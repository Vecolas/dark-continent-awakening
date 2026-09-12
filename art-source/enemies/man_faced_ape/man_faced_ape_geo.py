"""Gera as DUAS geometrias autorais do Man-Faced Ape (bedrock 1.12.0, GeckoLib 4).

POR QUE ESTE ARQUIVO EXISTE. O ADR-017 diz que mob vanilla e andaime: o macaco
ja funciona -- disfarcado ele so avanca quando ninguem esta olhando, chegar perto
derruba o disfarce, e revelar chama o bando -- vestindo o ALDEAO e o PIGLIN
vanilla. Por isso ele parece pronto: os tres gametests passam e o log nao diz
nada. O unico sinal do contrario e alguem abrir o jogo e reconhecer um aldeao
cinza andando de lado na selva. Este gerador produz os corpos proprios.

Nao ha Blockbench aqui, entao a FONTE VERSIONADA e este Python: JSON gerado sem
gerador no git vira um arquivo que ninguem consegue corrigir depois.

ESTE MODULO E A UNICA TABELA DE CAIXAS -- das duas formas. O gerador da textura
importa CAIXAS_DISFARCE e CAIXAS_REVELADO daqui de proposito: duas tabelas
separadas divergem na primeira correcao de modelo, e a divergencia nao da erro --
da face pintada no lugar errado, que so aparece na tela.

SAO DOIS MODELOS COM IDS PROPRIOS, e nao um modelo com metade dos ossos
escondida. Esconder osso e o truque que acabou de sair do sapo: um osso invisivel
continua carregado, continua animado e continua ocupando atlas, e o dia em que
alguem esquecer de esconde-lo o macaco aparece com bracos de primata por baixo do
manto sem nada acusar. Dois ids se encaixam no portao generico sem mudanca
nenhuma -- ele exige um arquivo de animacao e uma pasta de textura por modelo, e
duas formas sao dois de cada.

ESTA ENTREGA NAO MUDA COMPORTAMENTO NENHUM. Os numeros que o cliente enxerga
(10 ticks de revelacao; golpe 8/4/12) sao do servidor e continuam la; aqui so
existe o corpo que os desenha.

A HITBOX MANDA NOS DOIS MODELOS, E E A MESMA. A entidade e sized(0.9F, 1.95F) --
14.4 x 31.2 px -- e ela NAO muda quando o disfarce cai. E de proposito que as
duas silhuetas caibam na mesma caixa: se a caixa mudasse, o jogador notaria a
troca pelo empurrao, pela mira que para de acertar ou pelo bloco que o bicho
passa a nao caber -- ou seja, notaria ANTES de ver o corpo, e a revelacao
deixaria de ser a revelacao.

O QUE AS SILHUETAS PRECISAM ENTREGAR, e que e validado aqui porque "ficou bonito"
nao e criterio:

(a) DE LONGE, O DISFARCE E GENTE. Ombros estreitos (11 px de vao de braco contra
    os 14 do macaco), bracos ao longo do corpo, capuz cobrindo o rosto. Nenhum
    traco de macaco: se o disfarce se entregasse a distancia, o mob inteiro --
    que e "ele parece gente ate nao parecer mais" -- perderia o sentido, e nada
    no repositorio reprovaria isso.

(b) A UNICA COISA ERRADA E SUTIL: as maos. Elas sao mais largas e mais fundas que
    a manga, e ficam de fora da barra da tunica. `valida_maos_do_disfarce`
    reprova quem "arrumar" a mao para caber na manga -- arrumada, o disfarce fica
    perfeito, e um disfarce perfeito nao tem contrapartida ensinavel.

(c) REVELADO, ELE E PESADO E BAIXO. Mais LARGO (14 px) e mais BAIXO (27 px) que a
    forma humana (12 x 31), bracos mais longos que as pernas, cabeca baixa e
    empurrada para a frente entre os ombros.

(d) O ROSTO E O MESMO ROSTO. O cranio tem a MESMA largura nas duas formas, e
    `valida_o_mesmo_rosto` reprova quem alargar so um dos dois: com cranios
    diferentes a revelacao le como "trocou de bicho", e nao como "a cara e a
    mesma, o corpo e que esta errado" -- que e o nome do mob.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

Regerar:  python art-source/enemies/man_faced_ape/man_faced_ape_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/man_faced_ape.geo.json
          src/main/resources/assets/nenfoundation/geo/entity/man_faced_ape_disfarce.geo.json
"""
from collections import namedtuple
import json
import os
import re

UV_LARGURA, UV_ALTURA = 64, 64

# Limites que a entidade impoe: EnemyEntityTypes .sized(0.9F, 1.95F). A MESMA
# caixa serve as duas formas -- ver o cabecalho.
HITBOX_LARGURA_PX = 0.9 * 16.0
HITBOX_ALTURA_PX = 1.95 * 16.0

# Margens minimas do contraste entre as duas silhuetas. Nao sao botao de tuning:
# sao o piso para a troca ser LEGIVEL. Abaixo disso o macaco revelado tem o mesmo
# vulto do viajante, e a revelacao vira uma troca de textura.
REVELADO_MAIS_LARGO_PX = 1.0
REVELADO_MAIS_BAIXO_PX = 2.0

Osso = namedtuple("Osso", "nome pai pivot rotacao")
Caixa = namedtuple("Caixa", "nome osso u v x y z w h d")

# ---------------------------------------------------------------------------
# FORMA HUMANA -- man_faced_ape_disfarce
# ---------------------------------------------------------------------------

# Hierarquia CONGELADA pelo contrato da entrega: as animacoes e o GeoModel
# escrevem contra estes nomes. Nome errado aqui nao da erro -- o GeckoLib so
# deixa o osso parado, e isso so aparece na tela.
# O pivot de um filho mora na JUNCAO com o pai: `hood` gira na NUCA (z=4.5), e
# nao no meio da cabeca, senao o clipe de revelacao afunda o capuz no cranio em
# vez de joga-lo para tras.
OSSOS_DISFARCE = (
    Osso("root", None, (0, 0, 0), None),
    Osso("body", "root", (0, 12, 0), None),      # quadril
    Osso("head", "body", (0, 22, 0), None),      # base do pescoco
    Osso("hood", "head", (0, 25, 4.5), None),    # nuca: e daqui que o capuz cai
    Osso("arm_left", "body", (3.5, 22, 0), None),
    Osso("arm_right", "body", (-3.5, 22, 0), None),
    Osso("leg_left", "body", (2, 12, 0), None),
    Osso("leg_right", "body", (-2, 12, 0), None),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) px a partir de (u,v) -- as validacoes
# abaixo reprovam sobreposicao e estouro do atlas.
CAIXAS_DISFARCE = (
    # Cabeca de gente, 8x8x8, como a de qualquer humanoide do jogo. O rosto mora
    # na face da frente e fica quase todo DEBAixo do capuz: sobram 3 px de queixo.
    Caixa("head", "head", 0, 0, -4, 22, -4, 8, 8, 8),
    # O CAPUZ. Casca 0.5 px maior que o cranio em todo lado (nunca coplanar com
    # ele, senao as faces brigam) e cobrindo dos olhos para cima.
    Caixa("hood", "hood", 0, 16, -4.5, 25, -4.5, 9, 6, 9),
    # Tunica de viajante. 7 px de largura -- OMBRO ESTREITO e a metade da leitura
    # "isso e uma pessoa"; um tronco largo aqui ja entregaria o macaco.
    Caixa("body", "body", 36, 16, -3.5, 12, -2, 7, 10, 4),
    # Mangas finas, caidas ao longo do corpo.
    Caixa("sleeve_left", "arm_left", 28, 31, 3.5, 14, -1.5, 2, 8, 3),
    Caixa("sleeve_right", "arm_right", 38, 31, -5.5, 14, -1.5, 2, 8, 3),
    # AS MAOS -- a unica coisa errada. Mais largas (3 contra 2) e mais fundas
    # (4 contra 3) que a manga, penduradas ABAIXO da barra da tunica (y=12), onde
    # da para ve-las. Sao cubos do osso do braco de proposito: o contrato da forma
    # humana nao tem osso de mao, e inventar um aqui faria a hierarquia divergir
    # do arquivo de animacao.
    Caixa("hand_left", "arm_left", 0, 47, 3, 10, -2.5, 3, 4, 4),
    Caixa("hand_right", "arm_right", 14, 47, -6, 10, -2.5, 3, 4, 4),
    Caixa("leg_left", "leg_left", 0, 31, 0.5, 0, -2, 3, 12, 4),
    Caixa("leg_right", "leg_right", 14, 31, -3.5, 0, -2, 3, 12, 4),
)

# ---------------------------------------------------------------------------
# FORMA REVELADA -- man_faced_ape
# ---------------------------------------------------------------------------

OSSOS_REVELADO = (
    Osso("root", None, (0, 0, 0), None),
    Osso("body", "root", (0, 9, 0), None),       # quadril
    Osso("head", "body", (0, 20, -3), None),     # peito: a cabeca sai para a FRENTE
    Osso("jaw", "head", (0, 20, -5), None),      # dobradica no fundo da boca
    Osso("ear_left", "head", (4, 23, -6), None),
    Osso("ear_right", "head", (-4, 23, -6), None),
    Osso("arm_left", "body", (4, 21, -1), None),
    Osso("hand_left", "arm_left", (5.5, 7, -2), None),
    Osso("arm_right", "body", (-4, 21, -1), None),
    Osso("hand_right", "arm_right", (-5.5, 7, -2), None),
    Osso("leg_left", "body", (2, 10, 0), None),
    Osso("foot_left", "leg_left", (2, 2, 0), None),
    Osso("leg_right", "body", (-2, 10, 0), None),
    Osso("foot_right", "leg_right", (-2, 2, 0), None),
)

CAIXAS_REVELADO = (
    # Tronco estreito no quadril (8 px) e a CINTA DE OMBRO por cima (10 px): o
    # afunilamento e o que le como primata, e nao como barril.
    Caixa("torso", "body", 0, 0, -4, 9, -3.5, 8, 12, 7),
    Caixa("chest", "body", 0, 19, -5, 15, -3, 10, 7, 6),
    # Cranio da MESMA largura do cranio humano (8 px) -- ver `valida_o_mesmo_rosto`.
    # Ele fica na frente do peito, nao em cima: cabeca baixa entre os ombros.
    Caixa("head", "head", 32, 19, -4, 20, -9, 8, 7, 6),
    # A MANDIBULA. Volume proprio, embaixo do cranio e PROJETADO 1 px alem dele:
    # e ela que faz a cara humana caber numa cabeca de bicho.
    Caixa("jaw", "jaw", 36, 32, -3.5, 17, -10, 7, 3, 5),
    Caixa("ear_left", "ear_left", 58, 0, 4, 22, -7, 1, 3, 2),
    Caixa("ear_right", "ear_right", 58, 6, -5, 22, -7, 1, 3, 2),
    # Bracos LONGOS (14 px) e adiantados, terminando em maos de apoio.
    Caixa("arm_left", "arm_left", 30, 0, 4, 7, -4, 3, 14, 4),
    Caixa("arm_right", "arm_right", 44, 0, -7, 7, -4, 3, 14, 4),
    # As maos pousam a FRENTE dos pes (z -7..-2): andar de nos e o que rende a
    # silhueta pesada sem precisar de perna comprida.
    Caixa("hand_left", "hand_left", 0, 32, 3, 3, -7, 4, 5, 5),
    Caixa("hand_right", "hand_right", 18, 32, -7, 3, -7, 4, 5, 5),
    # Pernas CURTAS (8 px) -- mais curtas que os bracos, e o validador cobra isso.
    Caixa("leg_left", "leg_left", 0, 42, 0.5, 2, -2, 3, 8, 4),
    Caixa("leg_right", "leg_right", 14, 42, -3.5, 2, -2, 3, 8, 4),
    Caixa("foot_left", "foot_left", 28, 42, 0.5, 0, -4, 4, 2, 6),
    Caixa("foot_right", "foot_right", 28, 51, -4.5, 0, -4, 4, 2, 6),
)

Forma = namedtuple("Forma", "id ossos caixas")

DISFARCE = Forma("man_faced_ape_disfarce", OSSOS_DISFARCE, CAIXAS_DISFARCE)
REVELADO = Forma("man_faced_ape", OSSOS_REVELADO, CAIXAS_REVELADO)
FORMAS = (DISFARCE, REVELADO)


# ------------------------------------------------------------------ utilidades

def caixa(forma, nome):
    return [c for c in forma.caixas if c.nome == nome][0]


def area_no_atlas(c):
    """Retangulo (x0, y0, x1, y1) que a caixa ocupa no atlas."""
    return (c.u, c.v, c.u + 2 * c.d + 2 * c.w, c.v + c.d + c.h)


def limites(forma):
    cs = forma.caixas
    xs = [c.x for c in cs] + [c.x + c.w for c in cs]
    ys = [c.y for c in cs] + [c.y + c.h for c in cs]
    zs = [c.z for c in cs] + [c.z + c.d for c in cs]
    return (min(xs), max(xs)), (min(ys), max(ys)), (min(zs), max(zs))


def largura(forma):
    (x0, x1), _, _ = limites(forma)
    return x1 - x0


def altura(forma):
    _, (_, y1), _ = limites(forma)
    return y1


# ------------------------------------------------------------- validacoes

def valida_ossos(forma):
    nomes = [o.nome for o in forma.ossos]
    if len(set(nomes)) != len(nomes):
        raise ValueError("%s: osso repetido" % forma.id)
    raizes = [o for o in forma.ossos if o.pai is None]
    if len(raizes) != 1 or raizes[0].nome != "root":
        raise ValueError("%s: a unica raiz tem de ser 'root'" % forma.id)
    for o in forma.ossos:
        if o.pai is not None and o.pai not in nomes:
            raise ValueError("%s: osso '%s' aponta para pai inexistente '%s'"
                             % (forma.id, o.nome, o.pai))
    for c in forma.caixas:
        if c.osso not in nomes:
            raise ValueError("%s: caixa '%s' pendurada em osso inexistente '%s'"
                             % (forma.id, c.nome, c.osso))
    # Osso sem cubo e osso que a animacao move e ninguem ve. Aqui TODOS tem
    # volume menos a raiz, que existe so para a entidade inteira se mover.
    sem_cubo = [o.nome for o in forma.ossos
                if o.nome != "root" and not any(c.osso == o.nome for c in forma.caixas)]
    if sem_cubo:
        raise ValueError("%s: ossos sem volume proprio: %s -- a animacao os move e nada aparece"
                         % (forma.id, sem_cubo))


def valida_uv(forma):
    """Sobreposicao de UV nao da erro: da textura errada na face. Entao reprova aqui."""
    nomes = [c.nome for c in forma.caixas]
    if len(set(nomes)) != len(nomes):
        raise ValueError("%s: caixa com nome repetido -- o gerador da textura indexa por nome"
                         % forma.id)
    for c in forma.caixas:
        x0, y0, x1, y1 = area_no_atlas(c)
        if x1 > UV_LARGURA or y1 > UV_ALTURA:
            raise ValueError("%s: caixa '%s' estoura o atlas: vai ate (%d,%d)"
                             % (forma.id, c.nome, x1, y1))
    for i, a in enumerate(forma.caixas):
        ax0, ay0, ax1, ay1 = area_no_atlas(a)
        for b in forma.caixas[i + 1:]:
            bx0, by0, bx1, by1 = area_no_atlas(b)
            if ax0 < bx1 and bx0 < ax1 and ay0 < by1 and by0 < ay1:
                raise ValueError("%s: caixas '%s' e '%s' se sobrepoem no atlas"
                                 % (forma.id, a.nome, b.nome))


def valida_hitbox(forma):
    (x0, x1), (y0, y1), (z0, z1) = limites(forma)
    if y0 != 0:
        raise ValueError("%s: o piso tem de ficar em y=0, esta em %s" % (forma.id, y0))
    if y1 > HITBOX_ALTURA_PX:
        raise ValueError("%s: modelo mais alto (%s) que a hitbox (%s)"
                         % (forma.id, y1, HITBOX_ALTURA_PX))
    if (x1 - x0) > HITBOX_LARGURA_PX or (z1 - z0) > HITBOX_LARGURA_PX:
        raise ValueError("%s: modelo de %s x %s px estoura a hitbox de %s px"
                         % (forma.id, x1 - x0, z1 - z0, HITBOX_LARGURA_PX))


def valida_maos_do_disfarce():
    """A UNICA pista do disfarce. Mao que cabe na manga e disfarce sem contrapartida.

    Reprova tambem a mao escondida DENTRO da tunica: pista que ninguem ve nao e
    pista, e nada no jogo acusaria -- o mob continuaria funcionando, so teria
    deixado de ser justo.
    """
    tunica = caixa(DISFARCE, "body")
    for mao, manga in (("hand_left", "sleeve_left"), ("hand_right", "sleeve_right")):
        m, s = caixa(DISFARCE, mao), caixa(DISFARCE, manga)
        if m.w <= s.w or m.d <= s.d:
            raise ValueError("a mao '%s' (%sx%s) nao e maior que a manga '%s' (%sx%s): sem a mao "
                             "grande o disfarce fica perfeito, e disfarce perfeito nao tem como "
                             "ser desmascarado" % (mao, m.w, m.d, manga, s.w, s.d))
        if m.y >= tunica.y:
            raise ValueError("a mao '%s' comeca em y=%s, na altura da tunica (base y=%s): "
                             "escondida na roupa, a unica pista do mob deixa de ser visivel"
                             % (mao, m.y, tunica.y))


def valida_capuz():
    """O capuz cobre o rosto -- e NAO cobre o queixo.

    Cobrindo de menos, o disfarce mostra a cara de longe e o mob se entrega antes
    de o jogador ter o que observar. Cobrindo tudo, nao sobra rosto nenhum para o
    clipe de revelacao descobrir. Nenhum dos dois levanta erro.
    """
    capuz = caixa(DISFARCE, "hood")
    cranio = caixa(DISFARCE, "head")
    # O capuz PODE passar do alto da cabeca -- o que sobra e a ponta dele. O que
    # ele nao pode e ficar ABAIXO do alto do cranio.
    if capuz.y + capuz.h < cranio.y + cranio.h:
        raise ValueError("o capuz (topo y=%s) nao chega ao alto do cranio (y=%s): ele viraria "
                         "chapeu e o rosto ficaria a mostra"
                         % (capuz.y + capuz.h, cranio.y + cranio.h))
    if capuz.y <= cranio.y:
        raise ValueError("o capuz desce ate y=%s e o cranio comeca em y=%s: coberto ate o queixo, "
                         "nao sobra rosto para a revelacao descobrir" % (capuz.y, cranio.y))
    if capuz.w <= cranio.w or capuz.d <= cranio.d:
        raise ValueError("o capuz (%sx%s) nao e maior que o cranio (%sx%s): as faces ficam "
                         "coladas e brigam na tela" % (capuz.w, capuz.d, cranio.w, cranio.d))


def valida_bracos_do_macaco():
    """Braco mais longo que perna. E a leitura de primata, e ela e medivel."""
    braco = caixa(REVELADO, "arm_left").h + caixa(REVELADO, "hand_left").h
    perna = caixa(REVELADO, "leg_left").h + caixa(REVELADO, "foot_left").h
    if braco <= perna:
        raise ValueError("o braco tem %s px e a perna %s px: com braco curto o bicho revelado "
                         "le como pessoa musculosa, que e exatamente o que ele NAO pode ser "
                         "depois de revelado" % (braco, perna))


def valida_cabeca_baixa():
    """Cabeca baixa e ADIANTADA entre os ombros -- nao empoleirada num pescoco."""
    cranio = caixa(REVELADO, "head")
    ombro = caixa(REVELADO, "chest")
    tronco = caixa(REVELADO, "torso")
    folga = (cranio.y + cranio.h) - (ombro.y + ombro.h)
    if folga > cranio.h:
        raise ValueError("o cranio sobra %s px acima do ombro, mais que a propria altura dele "
                         "(%s): isso e pescoco, e pescoco le como gente" % (folga, cranio.h))
    if cranio.z >= tronco.z:
        raise ValueError("o cranio (frente z=%s) nao passa a frente do tronco (z=%s): sem a "
                         "cabeca empurrada para fora, a postura vira ereta" % (cranio.z, tronco.z))
    boca = caixa(REVELADO, "jaw")
    if boca.z >= cranio.z:
        raise ValueError("a mandibula (frente z=%s) nao projeta alem do cranio (z=%s): 'mandibula "
                         "proeminente' passa a ser so uma palavra no contrato" % (boca.z, cranio.z))


def valida_o_mesmo_rosto():
    """O nome do mob e cara de gente em corpo de macaco. O cranio e o MESMO cranio.

    Cranios de larguras diferentes nao dao erro: dao uma revelacao que le como
    'trocou de bicho' -- e a piada inteira do mob depende de ler como 'a cara e a
    mesma, o corpo e que esta errado'.
    """
    humano = caixa(DISFARCE, "head")
    macaco = caixa(REVELADO, "head")
    if humano.w != macaco.w:
        raise ValueError("o cranio humano tem %s px de largura e o do macaco %s: a revelacao "
                         "passaria a mostrar outro rosto, e nao o mesmo"
                         % (humano.w, macaco.w))


def valida_as_duas_silhuetas():
    """As duas formas cabem na MESMA hitbox -- e mesmo assim tem vultos diferentes.

    As duas metades desta regra sao igualmente mudas se quebrarem: silhuetas
    identicas transformam a revelacao numa troca de textura, e uma forma que
    estoure a caixa vira um bicho que atravessa parede -- nos dois casos os
    gametests continuam verdes, porque comportamento nao mudou.
    """
    dl, rl = largura(DISFARCE), largura(REVELADO)
    da, ra = altura(DISFARCE), altura(REVELADO)
    if rl - dl < REVELADO_MAIS_LARGO_PX:
        raise ValueError("o macaco tem %s px de largura e o viajante %s: menos de %s px de "
                         "diferenca e o mesmo vulto, e a revelacao deixa de ser visivel de longe"
                         % (rl, dl, REVELADO_MAIS_LARGO_PX))
    if da - ra < REVELADO_MAIS_BAIXO_PX:
        raise ValueError("o macaco tem %s px de altura e o viajante %s: o revelado precisa "
                         "AGACHAR pelo menos %s px, senao ele so troca de pele"
                         % (ra, da, REVELADO_MAIS_BAIXO_PX))
    # As duas caibam na MESMA caixa e o chao seja o mesmo: quem desenha um modelo
    # flutuando 1 px nao ve erro nenhum, ve um bicho 'meio estranho'.
    for forma in FORMAS:
        valida_hitbox(forma)


# ------------------------------------------------------------------ geracao

def _osso_json(forma, o):
    d = {"name": o.nome}
    if o.pai is not None:
        d["parent"] = o.pai
    d["pivot"] = list(o.pivot)
    if o.rotacao is not None:
        d["rotation"] = list(o.rotacao)
    cubos = [{"origin": [c.x, c.y, c.z], "size": [c.w, c.h, c.d], "uv": [c.u, c.v]}
             for c in forma.caixas if c.osso == o.nome]
    if cubos:
        d["cubes"] = cubos
    return d


def geometria(forma):
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                # O portao generico confere ESTA string contra o nome do arquivo:
                # errada, o modelo nao e achado e o mob some da tela sem log.
                "identifier": "geometry." + forma.id,
                "texture_width": UV_LARGURA,
                "texture_height": UV_ALTURA,
                "visible_bounds_width": 2,
                "visible_bounds_height": 2.5,
                "visible_bounds_offset": [0, 1.25, 0],
            },
            "bones": [_osso_json(forma, o) for o in forma.ossos],
        }],
    }


def _achatar_numeros(texto):
    """Poe os vetores numericos numa linha so -- o JSON e gerado, mas e lido em review."""
    return re.sub(r"\[\s+([-\d.,\se]+?)\s*\]",
                  lambda m: "[" + ", ".join(m.group(1).replace(",", " ").split()) + "]",
                  texto)


def escreve(forma):
    texto = _achatar_numeros(json.dumps(geometria(forma), indent=2))
    json.loads(texto)  # o JSON gerado tem de continuar valido depois do achatamento

    destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                           "geo", "entity", forma.id + ".geo.json")
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    with open(destino, "w", encoding="utf-8", newline="\n") as f:
        f.write(texto + "\n")
    return destino


def main():
    for forma in FORMAS:
        valida_ossos(forma)
        valida_uv(forma)
        valida_hitbox(forma)
    valida_maos_do_disfarce()
    valida_capuz()
    valida_bracos_do_macaco()
    valida_cabeca_baixa()
    valida_o_mesmo_rosto()
    valida_as_duas_silhuetas()

    for forma in FORMAS:
        destino = escreve(forma)
        (x0, x1), (y0, y1), (z0, z1) = limites(forma)
        ocupado = sum((2 * c.d + 2 * c.w) * (c.d + c.h) for c in forma.caixas)
        print("escrito", destino)
        print("  ossos: %d   caixas: %d" % (len(forma.ossos), len(forma.caixas)))
        print("  envolvente: x %s..%s  y %s..%s  z %s..%s (px)" % (x0, x1, y0, y1, z0, z1))
        print("  %s px de largura x %s de altura x %s de profundidade  (hitbox %.1f x %.1f)"
              % (x1 - x0, y1, z1 - z0, HITBOX_LARGURA_PX, HITBOX_ALTURA_PX))
        print("  atlas %dx%d: %d de %d px ocupados (%.0f%%)"
              % (UV_LARGURA, UV_ALTURA, ocupado, UV_LARGURA * UV_ALTURA,
                 100.0 * ocupado / (UV_LARGURA * UV_ALTURA)))
    print("contraste das silhuetas: revelado e %.1f px mais largo e %.1f px mais baixo"
          % (largura(REVELADO) - largura(DISFARCE), altura(DISFARCE) - altura(REVELADO)))


if __name__ == "__main__":
    main()
