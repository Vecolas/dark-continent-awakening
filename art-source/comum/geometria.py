"""Geometria Bedrock 1.12.0: as tabelas, os portoes e o emissor do .geo.json.

ESTE MODULO NAO TEM TABELA DE CAIXAS -- ele e a FORMA de uma. Quem declara
OSSOS e CAIXAS e o arquivo do mob, porque cada linha daquela tabela carrega um
comentario dizendo por que a peca tem aquele tamanho, e esse comentario e o unico
lugar onde a decisao fica registrada.

O QUE ESTE MODULO COBRA, E POR QUE CADA COISA E COBRADA AQUI
-------------------------------------------------------------
Todos os defeitos abaixo tem a mesma assinatura: JSON perfeitamente valido, log
limpo, portao Java verde, e o sintoma so aparece na tela.

  `valida_ossos`            pai inexistente, duas raizes, caixa pendurada no
                            nada, osso sem volume. O GeckoLib nao reclama de
                            nenhum: ele so deixa o osso parado.
  `valida_pivots`           pivot de filho fora do volume do pai. O membro passa
                            a girar em torno de um ponto que nao existe no bicho,
                            e o relato que chega e "o clipe ficou estranho".
  `valida_uv`               sobreposicao e estouro no atlas, nome repetido,
                            tamanho fracionario. Sobreposicao pinta a face errada;
                            tamanho fracionario faz o portao Java (que arredonda
                            o `size`) medir um retangulo diferente deste gerador,
                            e as duas contas so discordam no dia em que uma delas
                            achar sobreposicao.
  `valida_faces_coplanares` duas caixas com a MESMA face no MESMO plano. Em jogo
                            isso cintila, e o jogador le como bug de video. E o
                            unico defeito de modelo que nenhum portao Java ve.
  `valida_hitbox`           modelo maior que a caixa de colisao. A silhueta passa
                            a prometer um alcance que o servidor nao entrega, e
                            quem calcula a distancia pelo desenho morre acertando
                            a conta.

O QUE ELE DELIBERADAMENTE NAO COBRA: se o bicho parece o bicho. Isso mora no
arquivo do mob, em validacoes como `valida_testa`, `valida_boca`,
`valida_envergadura`. A biblioteca as recebe em `Modelo.validar(extras=...)` e as
roda por ultimo, para que a mensagem que chega ao humano seja a mais especifica
que existir -- e nunca "o formato esta errado" quando o problema e o bicho.

Convencao de eixos: y=0 e o chao, -Z e a FRENTE, +X e o lado ESQUERDO. 16 px = 1
bloco. Ver o cabecalho de `comum/__init__.py` para o layout de caixa no atlas.
"""
from collections import namedtuple
import json
import math
import re

from . import (ErroDeArte, FORMATO_GEOMETRIA, PX_POR_BLOCO,
               caminho_geo, em_px, escrever_texto)

# `pai=None` so vale para a raiz e `rotacao=None` e o caso normal -- ver a nota
# sobre bind rotation em `_osso_json`. Os defaults existem para que a tabela do
# mob nao precise escrever `None` em toda linha; o que NAO tem default e o pivot,
# porque um pivot esquecido em (0,0,0) e um membro que gira em torno do calcanhar
# do bicho, e um default silencioso faria disso a opcao mais facil.
Osso = namedtuple("Osso", "nome pai pivot rotacao")
Osso.__new__.__defaults__ = (None,)

# `nome` NAO aparece no JSON. Ele existe para (a) as mensagens de recusa citarem
# a peca pelo nome que o humano usa e (b) o gerador de textura indexar os pinceis
# -- e e por isso que `valida_uv` reprova nome repetido.
Caixa = namedtuple("Caixa", "nome osso u v x y z w h d")


# ------------------------------------------------------------------ medidas

def area_no_atlas(c):
    """Retangulo (x0, y0, x1, y1) que a caixa ocupa no atlas.

    Esta conta aparece em tres lugares do repositorio, em tres linguagens: aqui,
    em `textura.Pincel.faces` (que a fatia em seis) e no portao Java. Sao as
    pontas de um portao que morde dos dois lados; duplicacao declarada, nao
    esquecida.
    """
    return (c.u, c.v, c.u + 2 * c.d + 2 * c.w, c.v + c.d + c.h)


def volume(c):
    """Caixa envolvente do cubo: ((x0,x1),(y0,y1),(z0,z1))."""
    return ((c.x, c.x + c.w), (c.y, c.y + c.h), (c.z, c.z + c.d))


def limites(caixas):
    # `caixas` e parametro OBRIGATORIO de proposito. Um default `caixas=CAIXAS`
    # seria avaliado uma unica vez, na definicao, e a funcao passaria a medir para
    # sempre a tabela do momento do import. Isso nao da erro -- da uma regua que
    # aprova qualquer modelo, porque ela nunca ve a mudanca.
    if not caixas:
        raise ErroDeArte("nao da para medir um modelo sem caixa nenhuma")
    xs = [c.x for c in caixas] + [c.x + c.w for c in caixas]
    ys = [c.y for c in caixas] + [c.y + c.h for c in caixas]
    zs = [c.z for c in caixas] + [c.z + c.d for c in caixas]
    return (min(xs), max(xs)), (min(ys), max(ys)), (min(zs), max(zs))


def pixels_ocupados(caixas):
    return sum((2 * c.d + 2 * c.w) * (c.d + c.h) for c in caixas)


def achar(tabela, nome, o_que):
    encontrados = [item for item in tabela if item.nome == nome]
    if not encontrados:
        raise ErroDeArte("nao existe %s chamado '%s'. Nomes conhecidos: %s"
                         % (o_que, nome, sorted(item.nome for item in tabela)))
    return encontrados[0]


# --------------------------------------------------------------- validacoes
# Toda validacao recebe por parametro o que precisa medir. Nenhuma le global:
# regua que le global e regua que mede a tabela errada no dia em que houver duas.

def valida_ossos(ossos, caixas, exige_volume=True):
    nomes = [o.nome for o in ossos]
    repetidos = sorted({n for n in nomes if nomes.count(n) > 1})
    if repetidos:
        raise ErroDeArte("osso repetido: %s -- o segundo apaga o primeiro no JSON, "
                         "e as caixas do primeiro somem sem uma linha de log" % repetidos)
    raizes = [o for o in ossos if o.pai is None]
    if len(raizes) != 1 or raizes[0].nome != "root":
        raise ErroDeArte("a unica raiz tem de ser 'root', e ha %d raiz(es): %s. A raiz e a "
                         "ancora que o renderer alinha com a hitbox; duas ancoras deixam metade "
                         "do bicho fora da caixa de colisao"
                         % (len(raizes), [o.nome for o in raizes]))
    for o in ossos:
        if o.pai is not None and o.pai not in nomes:
            raise ErroDeArte("osso '%s' aponta para pai inexistente '%s': o GeckoLib nao levanta "
                             "erro, ele so deixa o osso parado no lugar" % (o.nome, o.pai))
    for o in ossos:
        visto, atual = [], o
        while atual.pai is not None:
            if atual.nome in visto:
                raise ErroDeArte("a hierarquia fecha um ciclo em '%s' (%s): o carregador entra em "
                                 "recursao e o mob nao aparece" % (o.nome, visto))
            visto.append(atual.nome)
            atual = achar(ossos, atual.pai, "osso")
    for c in caixas:
        if c.osso not in nomes:
            raise ErroDeArte("caixa '%s' pendurada em osso inexistente '%s': o volume nao entra em "
                             "lugar nenhum do bicho" % (c.nome, c.osso))
    if exige_volume:
        # A raiz e a unica dispensada: ela existe para o bicho inteiro poder subir
        # e descer. Osso sem cubo abaixo dela e osso que a animacao move e ninguem
        # ve -- o clipe roda, o valor muda e a tela fica igual.
        sem_cubo = [o.nome for o in ossos
                    if o.nome != "root" and not any(c.osso == o.nome for c in caixas)]
        if sem_cubo:
            raise ErroDeArte("ossos sem volume proprio: %s -- a animacao os move e nada aparece "
                             "na tela" % sem_cubo)


def valida_pivots(ossos, caixas):
    """O pivot de um filho mora na JUNCAO com o pai -- e junca se mede.

    Filho direto de `root` e dispensado: a raiz nao tem volume, e e nela que o
    bicho inteiro se apoia. Pai sem cubo tambem e dispensado, para que esta regua
    nao brigue com `exige_volume=False`.
    """
    for o in ossos:
        if o.pai is None or o.pai == "root":
            continue
        cubos_do_pai = [c for c in caixas if c.osso == o.pai]
        if not cubos_do_pai:
            continue
        px, py, pz = o.pivot
        dentro = any(vx0 <= px <= vx1 and vy0 <= py <= vy1 and vz0 <= pz <= vz1
                     for (vx0, vx1), (vy0, vy1), (vz0, vz1)
                     in (volume(c) for c in cubos_do_pai))
        if not dentro:
            raise ErroDeArte("o pivot de '%s' %s cai fora de todo cubo do pai '%s': o osso giraria "
                             "em torno de um ponto que nao existe no bicho, e o sintoma e um clipe "
                             "'meio quebrado' que ninguem sabe descrever" % (o.nome, o.pivot, o.pai))


def valida_uv(caixas, uv_largura, uv_altura):
    """Sobreposicao de UV nao da erro: da textura errada na face. Entao reprova aqui."""
    nomes = [c.nome for c in caixas]
    repetidos = sorted({n for n in nomes if nomes.count(n) > 1})
    if repetidos:
        raise ErroDeArte("caixa com nome repetido: %s -- o gerador da textura indexa os pinceis "
                         "por nome, e o segundo pincel passaria a pintar por cima do primeiro"
                         % repetidos)
    for c in caixas:
        if int(c.w) != c.w or int(c.h) != c.h or int(c.d) != c.d:
            raise ErroDeArte("a caixa '%s' tem tamanho fracionario %s: o portao Java arredonda o "
                             "size para medir o atlas e passaria a medir um retangulo diferente "
                             "deste gerador. Origem fracionaria pode; tamanho nao"
                             % (c.nome, (c.w, c.h, c.d)))
        if c.w <= 0 or c.h <= 0 or c.d <= 0:
            raise ErroDeArte("a caixa '%s' tem tamanho %s: cubo sem volume nao aparece e ainda "
                             "reserva area do atlas" % (c.nome, (c.w, c.h, c.d)))
        if int(c.u) != c.u or int(c.v) != c.v or c.u < 0 or c.v < 0:
            raise ErroDeArte("a caixa '%s' tem uv %s: o canto do layout e um pixel, e pixel e "
                             "inteiro e nao-negativo" % (c.nome, (c.u, c.v)))
        x0, y0, x1, y1 = area_no_atlas(c)
        if x1 > uv_largura or y1 > uv_altura:
            raise ErroDeArte("caixa '%s' estoura o atlas %dx%d: vai ate (%d,%d). O que sai da "
                             "folha vira amostragem da borda, e a face aparece esticada"
                             % (c.nome, uv_largura, uv_altura, x1, y1))
    for i, a in enumerate(caixas):
        ax0, ay0, ax1, ay1 = area_no_atlas(a)
        for b in caixas[i + 1:]:
            bx0, by0, bx1, by1 = area_no_atlas(b)
            if ax0 < bx1 and bx0 < ax1 and ay0 < by1 and by0 < ay1:
                raise ErroDeArte("caixas '%s' (%d,%d)..(%d,%d) e '%s' (%d,%d)..(%d,%d) se "
                                 "sobrepoem no atlas: as duas leem o mesmo pixel, e a segunda a "
                                 "ser pintada decide o que a primeira mostra"
                                 % (a.nome, ax0, ay0, ax1, ay1, b.nome, bx0, by0, bx1, by1))


def valida_faces_coplanares(caixas):
    """Duas caixas com a MESMA face no MESMO plano cintilam em jogo.

    Nao e erro de JSON, nao aparece no log, e o jogador le como bug de driver.

    Face OPOSTA no mesmo plano (uma caixa encostando na outra) e permitida de
    proposito: e assim que todo modelo do jogo e montado, e as normais contrarias
    resolvem a disputa sozinhas. O que se reprova aqui e MESMA direcao com area
    em comum.
    """
    eixos = (("x", 0), ("y", 1), ("z", 2))
    for eixo, indice in eixos:
        outros = [i for _, i in eixos if i != indice]
        for lado in (0, 1):  # 0 = face minima, 1 = face maxima
            faces = []
            for c in caixas:
                v = volume(c)
                faces.append((c.nome, v[indice][lado], v[outros[0]], v[outros[1]]))
            for i, (nome_a, plano_a, ra0, ra1) in enumerate(faces):
                for nome_b, plano_b, rb0, rb1 in faces[i + 1:]:
                    if plano_a != plano_b:
                        continue
                    if min(ra0[1], rb0[1]) - max(ra0[0], rb0[0]) <= 0:
                        continue
                    if min(ra1[1], rb1[1]) - max(ra1[0], rb1[0]) <= 0:
                        continue
                    raise ErroDeArte(
                        "'%s' e '%s' tem a mesma face %s=%s virada para o mesmo lado, com area em "
                        "comum: em jogo isso cintila, e o jogador le como bug de video"
                        % (nome_a, nome_b, eixo, plano_a))


def valida_hitbox(caixas, largura_px, altura_px, rotulo="a hitbox"):
    """O modelo PARADO cabe na caixa de colisao que a entidade Java declara.

    `rotulo` existe para a mensagem continuar ensinando: a biblioteca sabe o que
    mediu e qual era o limite, mas so o arquivo do mob sabe por que aquele limite
    e aquele -- 'a coleira do ninho', 'o alcance da carga'. O motivo pertence ao
    bicho; a medida, a biblioteca.
    """
    (x0, x1), (y0, y1), (z0, z1) = limites(caixas)
    if y0 != 0:
        raise ErroDeArte("o piso do modelo tem de ficar em y=0 e esta em %s: o bicho flutua ou "
                         "afunda no chao, e o renderer nao corrige isso" % y0)
    if y1 > altura_px:
        raise ErroDeArte("o modelo tem %s px de altura e %s tem %s px: a cabeca passa por cima de "
                         "quem mira na caixa de colisao, e o tiro que parece acertar erra"
                         % (y1, rotulo, altura_px))
    if (x1 - x0) > largura_px:
        raise ErroDeArte("o modelo tem %s px de largura e %s tem %s px: a silhueta parada promete "
                         "um alcance que o servidor nao entrega"
                         % (x1 - x0, rotulo, largura_px))
    if (z1 - z0) > largura_px:
        raise ErroDeArte("o modelo tem %s px de comprimento e %s tem %s px de lado: o bicho "
                         "atravessa paredes que a colisao dele nao atravessa"
                         % (z1 - z0, rotulo, largura_px))


def valida_encadeamento(ossos, pares, motivo):
    """Filho tem de pendurar no pai DECLARADO, e nao em qualquer ancestral.

    `pares` e uma sequencia de (filho, pai). Pendurar a ponta da asa no corpo em
    vez de no braco nao da erro -- o pai existe, o portao Java passa -- e da uma
    asa que abre pela metade, com a ponta parada no ar enquanto o braco gira
    embaixo dela. Quem sabe quais encadeamentos importam e o mob; `motivo` e a
    frase dele.
    """
    for filho, pai in pares:
        atual = achar(ossos, filho, "osso").pai
        if atual != pai:
            raise ErroDeArte("'%s' devia pendurar em '%s' e pendura em '%s': %s"
                             % (filho, pai, atual, motivo))


# ------------------------------------------------------------------ emissao

def _osso_json(o, caixas):
    d = {"name": o.nome}
    if o.pai is not None:
        d["parent"] = o.pai
    d["pivot"] = list(o.pivot)
    # `rotation` de bind e omitido quando nao existe, e o normal e nao existir.
    # A pose de repouso mora nas CAIXAS. Com bind rotation, todo valor que a lane
    # de animacao escrevesse seria somado a um angulo invisivel no
    # .animation.json -- duas fontes para a mesma pose, decididas em silencio.
    if o.rotacao is not None:
        d["rotation"] = list(o.rotacao)
    cubos = [{"origin": [c.x, c.y, c.z], "size": [c.w, c.h, c.d], "uv": [c.u, c.v]}
             for c in caixas if c.osso == o.nome]
    if cubos:
        d["cubes"] = cubos
    return d


def _achatar_numeros(texto):
    """Poe os vetores numericos numa linha so -- o JSON e gerado, mas e lido em review.

    `json.dumps(indent=2)` quebra [0, 15, 9] em quatro linhas, e o arquivo deixa
    de ser legivel num diff -- que e o unico lugar onde a outra pessoa vai
    conferir um pivot.
    """
    return re.sub(r"\[\s+([-\d.,\se]+?)\s*\]",
                  lambda m: "[" + ", ".join(m.group(1).replace(",", " ").split()) + "]",
                  texto)


def _meio_bloco_acima(px):
    """Arredonda px para cima, em blocos, no degrau de meio bloco."""
    return math.ceil((px / PX_POR_BLOCO) * 2.0) / 2.0


class Modelo:
    """Um mob inteiro do lado da geometria: tabelas, portoes e o arquivo.

    `mob` e a unica identidade declarada. O `identifier` do Bedrock e DERIVADO
    dele -- "geometry." + mob -- porque o portao Java exige exatamente isso, e
    declarar os dois criaria duas fontes para o mesmo nome; a divergencia faria o
    modelo nao ser achado e o mob sumir da tela sem uma linha de log.

    `hitbox_blocos` e o literal da chamada Java (`sized(1.9F, 1.55F)`), copiado
    como esta, para que quem mudar a entidade ache este arquivo procurando pelo
    numero. A conversao em pixels e nossa.
    """

    def __init__(self, mob, ossos, caixas, uv, hitbox_blocos,
                 bounds=None, exige_volume=True):
        self.mob = mob
        self.identificador = "geometry." + mob
        self.ossos = tuple(ossos)
        self.caixas = tuple(caixas)
        self.uv_largura, self.uv_altura = uv
        self.hitbox_largura_px = em_px(hitbox_blocos[0])
        self.hitbox_altura_px = em_px(hitbox_blocos[1])
        self.bounds = bounds
        self.exige_volume = exige_volume

    # -- consulta -----------------------------------------------------------

    def caixa(self, nome):
        return achar(self.caixas, nome, "caixa")

    def osso(self, nome):
        return achar(self.ossos, nome, "osso")

    def caixas_de(self, osso):
        return tuple(c for c in self.caixas if c.osso == osso)

    def limites(self, caixas=None):
        return limites(self.caixas if caixas is None else caixas)

    # -- portoes ------------------------------------------------------------

    def validar(self, extras=()):
        """Roda a bateria do FORMATO e, por ultimo, as validacoes do BICHO.

        A ordem importa para a mensagem, nao para a corretude: quem recebe a
        recusa tem de ler primeiro a coisa mais generica que quebrou. Uma caixa
        que estoura o atlas E deixa o bicho mais alto que a hitbox deve falar de
        atlas, porque e o que a pessoa vai consertar primeiro.
        """
        valida_ossos(self.ossos, self.caixas, self.exige_volume)
        valida_pivots(self.ossos, self.caixas)
        valida_uv(self.caixas, self.uv_largura, self.uv_altura)
        valida_faces_coplanares(self.caixas)
        valida_hitbox(self.caixas, self.hitbox_largura_px, self.hitbox_altura_px)
        for extra in extras:
            extra(self)
        return self

    # -- saida --------------------------------------------------------------

    def _bounds(self):
        if self.bounds is not None:
            return self.bounds
        # A caixa de visibilidade apertada nao da erro: faz o mob SUMIR da tela
        # quando a camera chega no angulo em que a caixa sai do frustum -- e some
        # exatamente quando um membro se estende, que costuma ser o quadro do
        # telegrafo. Por isso o padrao ja sobra meio bloco, e quem anima um membro
        # que sai muito do corpo (uma asa que abre) declara o proprio.
        (x0, x1), (_, y1), (z0, z1) = self.limites()
        largura = _meio_bloco_acima(max(x1 - x0, z1 - z0)) + 0.5
        altura = _meio_bloco_acima(y1) + 0.5
        return (largura, altura, [0, round(altura / 2.0, 2), 0])

    def json(self):
        largura, altura, deslocamento = self._bounds()
        return {
            "format_version": FORMATO_GEOMETRIA,
            "minecraft:geometry": [{
                "description": {
                    "identifier": self.identificador,
                    "texture_width": self.uv_largura,
                    "texture_height": self.uv_altura,
                    "visible_bounds_width": largura,
                    "visible_bounds_height": altura,
                    "visible_bounds_offset": deslocamento,
                },
                "bones": [_osso_json(o, self.caixas) for o in self.ossos],
            }],
        }

    def texto(self):
        texto = _achatar_numeros(json.dumps(self.json(), indent=2))
        # O JSON gerado tem de continuar valido DEPOIS do achatamento: este
        # re-parse e o portao contra a propria regex acima.
        json.loads(texto)
        return texto

    def escrever(self, destino=None, raiz="."):
        return escrever_texto(destino or caminho_geo(self.mob, raiz), self.texto())

    # -- relato -------------------------------------------------------------

    def resumo(self, destino):
        """O que o humano le no terminal.

        Existe porque nada aqui prova que o bicho parece o bicho. As medidas
        impressas sao para serem OLHADAS -- ver docs/testing/o-que-nao-provamos.md.
        """
        (x0, x1), (y0, y1), (z0, z1) = self.limites()
        ocupado = pixels_ocupados(self.caixas)
        folha = self.uv_largura * self.uv_altura
        linhas = [
            "escrito %s" % destino,
            "ossos: %d   caixas: %d" % (len(self.ossos), len(self.caixas)),
            "caixa envolvente: x %s..%s  y %s..%s  z %s..%s (px)"
            % (x0, x1, y0, y1, z0, z1),
            "parado: %s x %s x %s px  (hitbox %.1f x %.1f px)"
            % (x1 - x0, y1, z1 - z0, self.hitbox_largura_px, self.hitbox_altura_px),
            "atlas %dx%d: %d de %d px ocupados (%.0f%%)"
            % (self.uv_largura, self.uv_altura, ocupado, folha, 100.0 * ocupado / folha),
        ]
        return "\n".join(linhas)

    def emitir(self, extras=(), destino=None, raiz=".", silencioso=False):
        """Valida, escreve e relata -- nesta ordem, sempre.

        Validar ANTES de produzir texto e o que impede meio arquivo escrito: uma
        recusa no meio da escrita deixaria no disco um .geo.json truncado que o
        jogo carrega parcialmente, e o sintoma seria um mob sem pernas.
        """
        self.validar(extras)
        caminho = self.escrever(destino, raiz)
        if not silencioso:
            print(self.resumo(caminho))
        return caminho
