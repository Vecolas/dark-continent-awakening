"""Gera a geometria autoral do Frog-In-Waiting (bedrock 1.12.0, carregada pelo GeckoLib 4).

POR QUE ESTE ARQUIVO EXISTE. O ADR-017 diz que mob vanilla e andaime: o sapo
emboscador ja funciona -- nasce enterrado, agarra quem pisa em cima, e solta
sempre -- vestindo a geometria e a textura do SAPO VANILLA. Por isso ele parece
pronto: os quatro gametests passam e ninguem ve nada errado no log. O unico
sinal do contrario e alguem abrir o jogo e reconhecer um sapo do pantano
aumentado. Este gerador produz o corpo proprio que substitui o emprestado.

Nao ha Blockbench aqui, entao a FONTE VERSIONADA e este Python: JSON gerado sem
gerador no git vira um arquivo que ninguem consegue corrigir depois.

ESTE MODULO E A UNICA TABELA DE CAIXAS. O gerador da textura importa CAIXAS
daqui de proposito: duas tabelas separadas divergem na primeira correcao de
modelo, e a divergencia nao da erro -- da face pintada no lugar errado, que so
aparece na tela.

ESTA ENTREGA NAO MUDA COMPORTAMENTO NENHUM. Os numeros que importam ao cliente
(10 ticks de emerge, 4 de mordida, 100 de agarrao) sao do servidor e continuam
la; aqui so existe o corpo que os desenha.

A HITBOX MANDA NO MODELO. A entidade e sized(1.4F, 1.0F) -- 22.4 x 16 px -- e as
validacoes abaixo reprovam se o modelo estourar isso. O piso das patas fica em
y=0.

AS TRES COISAS QUE A SILHUETA PRECISA ENTREGAR, e que sao validadas aqui porque
"ficou bonito" nao e criterio:

(a) LARGO, BAIXO E PESADO. Nao e sapo vanilla aumentado: a caixa envolvente usa
    22 dos 22.4 px de largura e so 14 dos 16 de altura. Quem olha de lado ve um
    bicho mais largo do que alto, agachado no chao.

(b) A BOCA E A ARMA. O osso `jaw` e um volume de 14 x 3 x 9 px -- ele atravessa
    o cranio inteiro em profundidade e ocupa 63% da largura do bicho. Nao e
    enfeite: e o osso que a lane de animacao abre, e uma mandibula fina nao teria
    o que abrir. `valida_boca` reprova se ela encolher.

(c) OS OLHOS SOBRAM DA TERRA. Enterrado, o sapo nao desenha o corpo -- so os
    olhos ficam de fora, e essa e a UNICA pista que o jogador tem antes de ser
    engolido. Por isso `eye_left` e `eye_right` sao volumes PROPRIOS acima da
    linha do cranio (base em y=11, topo do cranio em y=11), e `valida_olhos`
    reprova se alguem os afundar na cabeca: afundados, eles continuariam
    existindo, continuariam pintados, e a emboscada perderia o aviso sem que
    nada acusasse.

O osso `throat` tambem tem volume proprio, embaixo da mandibula: e ele que pulsa
durante a digestao (fase RECOVERY). Um `throat` sem cubo seria um osso que a
animacao move e ninguem ve.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

Regerar:  python art-source/enemies/frog_in_waiting/frog_in_waiting_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/frog_in_waiting.geo.json
"""
from collections import namedtuple
import json
import os
import re

IDENTIFICADOR = "geometry.frog_in_waiting"
UV_LARGURA, UV_ALTURA = 64, 64

# Limites que a entidade impoe: EnemyEntityTypes .sized(1.4F, 1.0F).
HITBOX_LARGURA_PX = 1.4 * 16.0
HITBOX_ALTURA_PX = 1.0 * 16.0

# Fracao minima da largura do bicho que a mandibula tem de ocupar para a leitura
# "a boca e a arma" sobreviver a uma correcao distraida de modelo.
BOCA_FRACAO_MINIMA = 0.55

Osso = namedtuple("Osso", "nome pai pivot rotacao")

# Hierarquia CONGELADA pelo contrato da entrega: as animacoes e o controller do
# GeckoLib escrevem contra estes nomes. Nome errado aqui nao da erro -- o
# GeckoLib so deixa o osso parado, e isso so aparece na tela.
# O pivot de um filho mora na JUNCAO com o pai, senao a animacao gira errado:
# `jaw` gira no fundo da boca (z=-2), nao no meio dela, senao abrir a boca
# afunda o queixo dentro do peito.
OSSOS = (
    Osso("root", None, (0, 0, 0), None),
    Osso("body", "root", (0, 7, 3), None),
    Osso("head", "body", (0, 8, -2), None),
    # dobradica no fundo da boca, na linha onde o labio de cima encontra o de baixo
    Osso("jaw", "head", (0, 6, -2), None),
    # base do bulbo, em cima do cranio: o olho gira/afunda sem sair da cabeca
    Osso("eye_left", "head", (4.5, 11, -8.5), None),
    Osso("eye_right", "head", (-4.5, 11, -8.5), None),
    # teto do saco gular, onde ele encosta na mandibula
    Osso("throat", "body", (0, 3, -5.5), None),
    Osso("leg_front_left", "body", (7, 5, -2.5), None),
    Osso("foot_front_left", "leg_front_left", (8.5, 2, -2.5), None),
    Osso("leg_front_right", "body", (-7, 5, -2.5), None),
    Osso("foot_front_right", "leg_front_right", (-8.5, 2, -2.5), None),
    Osso("leg_back_left", "body", (7, 5, 4.5), None),
    Osso("foot_back_left", "leg_back_left", (8.5, 2, 4.5), None),
    Osso("leg_back_right", "body", (-7, 5, 4.5), None),
    Osso("foot_back_right", "leg_back_right", (-8.5, 2, 4.5), None),
)

Caixa = namedtuple("Caixa", "nome osso u v x y z w h d")

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v) -- a
# validacao abaixo reprova sobreposicao e estouro do atlas.
CAIXAS = (
    # cranio: a peca MAIS LARGA do bicho (16 px, mais larga que o tronco de 14).
    # Sapo de verdade e cabeca; um cranio mais estreito que o corpo leria como
    # rato. Topo em y=11, que e o piso dos olhos.
    Caixa("head", "head", 0, 0, -8, 6, -11, 16, 5, 9),
    # tronco baixo e comprido, ventre a 4 px do chao
    Caixa("body", "body", 0, 14, -7, 4, -2, 14, 6, 10),
    # A MANDIBULA. Atravessa o cranio inteiro em profundidade (d=9, igual a da
    # cabeca) e tem 3 px de altura de carne. Ela abre para baixo pelo pivot em
    # z=-2; o vao que aparece entre ela e o teto da boca e a arma.
    Caixa("jaw", "jaw", 0, 30, -7, 3, -11, 14, 3, 9),
    # saco gular: encosta na mandibula em cima (y=3) e no chao embaixo (y=0).
    # E o unico volume que muda de tamanho na digestao.
    Caixa("throat", "throat", 0, 42, -4, 0, -8, 8, 3, 5),
    # OS OLHOS. Bulbos proprios ACIMA do cranio -- a unica coisa que sobra de
    # fora quando o bicho esta enterrado.
    Caixa("eye_left", "eye_left", 50, 0, 3, 11, -10, 3, 3, 3),
    Caixa("eye_right", "eye_right", 50, 6, -6, 11, -10, 3, 3, 3),
    # patas: curtas e abertas para FORA do tronco (x ate 10), o que alarga a
    # silhueta sem levantar o bicho do chao
    Caixa("leg_front_left", "leg_front_left", 48, 26, 7, 2, -4, 3, 3, 3),
    Caixa("leg_front_right", "leg_front_right", 48, 32, -10, 2, -4, 3, 3, 3),
    Caixa("leg_back_left", "leg_back_left", 48, 38, 7, 2, 3, 3, 3, 3),
    Caixa("leg_back_right", "leg_back_right", 26, 42, -10, 2, 3, 3, 3, 3),
    # pes dianteiros pequenos; traseiros GRANDES e espalmados -- e o que da o
    # "empurrao" do emerge sem precisar de perna de salto
    Caixa("foot_front_left", "foot_front_left", 48, 14, 6, 0, -5, 4, 2, 4),
    Caixa("foot_front_right", "foot_front_right", 48, 20, -10, 0, -5, 4, 2, 4),
    Caixa("foot_back_left", "foot_back_left", 0, 50, 6, 0, 2, 5, 2, 5),
    Caixa("foot_back_right", "foot_back_right", 20, 50, -11, 0, 2, 5, 2, 5),
)


def caixa(nome):
    return [c for c in CAIXAS if c.nome == nome][0]


def area_no_atlas(c):
    """Retangulo (x0, y0, x1, y1) que a caixa ocupa no atlas."""
    return (c.u, c.v, c.u + 2 * c.d + 2 * c.w, c.v + c.d + c.h)


def valida_ossos():
    nomes = [o.nome for o in OSSOS]
    if len(set(nomes)) != len(nomes):
        raise ValueError("osso repetido")
    raizes = [o for o in OSSOS if o.pai is None]
    if len(raizes) != 1 or raizes[0].nome != "root":
        raise ValueError("a unica raiz tem de ser 'root'")
    for o in OSSOS:
        if o.pai is not None and o.pai not in nomes:
            raise ValueError("osso '%s' aponta para pai inexistente '%s'" % (o.nome, o.pai))
    for c in CAIXAS:
        if c.osso not in nomes:
            raise ValueError("caixa '%s' pendurada em osso inexistente '%s'" % (c.nome, c.osso))
    # Osso sem cubo e osso que a animacao move e ninguem ve. Aqui TODOS tem
    # volume menos a raiz, que existe so para a entidade inteira poder afundar.
    sem_cubo = [o.nome for o in OSSOS
                if o.nome != "root" and not any(c.osso == o.nome for c in CAIXAS)]
    if sem_cubo:
        raise ValueError("ossos sem volume proprio: %s -- a animacao os move e nada aparece"
                         % sem_cubo)


def valida_uv():
    """Sobreposicao de UV nao da erro: da textura errada na face. Entao reprova aqui."""
    for c in CAIXAS:
        x0, y0, x1, y1 = area_no_atlas(c)
        if x1 > UV_LARGURA or y1 > UV_ALTURA:
            raise ValueError("caixa '%s' estoura o atlas: vai ate (%d,%d)" % (c.nome, x1, y1))
    for i, a in enumerate(CAIXAS):
        ax0, ay0, ax1, ay1 = area_no_atlas(a)
        for b in CAIXAS[i + 1:]:
            bx0, by0, bx1, by1 = area_no_atlas(b)
            if ax0 < bx1 and bx0 < ax1 and ay0 < by1 and by0 < ay1:
                raise ValueError("caixas '%s' e '%s' se sobrepoem no atlas" % (a.nome, b.nome))


def limites():
    xs = [c.x for c in CAIXAS] + [c.x + c.w for c in CAIXAS]
    ys = [c.y for c in CAIXAS] + [c.y + c.h for c in CAIXAS]
    zs = [c.z for c in CAIXAS] + [c.z + c.d for c in CAIXAS]
    return (min(xs), max(xs)), (min(ys), max(ys)), (min(zs), max(zs))


def valida_hitbox():
    (x0, x1), (y0, y1), (z0, z1) = limites()
    if y0 != 0:
        raise ValueError("o piso das patas tem de ficar em y=0, esta em %s" % y0)
    if y1 > HITBOX_ALTURA_PX:
        raise ValueError("modelo mais alto (%s) que a hitbox (%s)" % (y1, HITBOX_ALTURA_PX))
    if (x1 - x0) > HITBOX_LARGURA_PX or (z1 - z0) > HITBOX_LARGURA_PX:
        raise ValueError("modelo mais largo/comprido que a hitbox de %s px" % HITBOX_LARGURA_PX)
    # LARGO E BAIXO nao e opiniao: se o bicho ficar mais alto do que largo, ele
    # deixou de ser este mob e virou outro -- e nenhum portao do repositorio ve isso.
    if y1 >= (x1 - x0):
        raise ValueError("o sapo ficou mais alto (%s) do que largo (%s): perdeu a leitura de "
                         "emboscador agachado" % (y1, x1 - x0))


def valida_boca():
    """A mandibula e a ARMA. Se ela encolher, a animacao de bocada abre um nada."""
    (x0, x1), _, _ = limites()
    boca = caixa("jaw")
    cranio = caixa("head")
    minimo = BOCA_FRACAO_MINIMA * (x1 - x0)
    if boca.w < minimo:
        raise ValueError("a mandibula tem %s px de largura e o minimo para ler como boca e "
                         "%.1f px (%.0f%% dos %s px do bicho)"
                         % (boca.w, minimo, BOCA_FRACAO_MINIMA * 100, x1 - x0))
    if boca.d < cranio.d:
        raise ValueError("a mandibula (%s px de profundidade) nao atravessa o cranio (%s px): "
                         "a boca abriria so na ponta do focinho" % (boca.d, cranio.d))
    if boca.y + boca.h > cranio.y:
        raise ValueError("a mandibula invade o cranio: em vez de abrir, ela atravessaria a "
                         "cabeca por dentro")


def valida_olhos():
    """Enterrado, SO os olhos sobram. Afundados no cranio, a emboscada perde o aviso."""
    cranio = caixa("head")
    topo_do_cranio = cranio.y + cranio.h
    for nome in ("eye_left", "eye_right"):
        olho = caixa(nome)
        if olho.y < topo_do_cranio:
            raise ValueError("'%s' comeca em y=%s, abaixo do topo do cranio (y=%s): enterrado o "
                             "sapo nao teria o que mostrar" % (nome, olho.y, topo_do_cranio))
        if olho.z < cranio.z or olho.z + olho.d > cranio.z + cranio.d:
            raise ValueError("'%s' fica fora da planta do cranio: o olho flutuaria" % nome)
    esquerdo, direito = caixa("eye_left"), caixa("eye_right")
    if esquerdo.x <= 0 or direito.x + direito.w >= 0:
        raise ValueError("os olhos tem de ficar um de cada lado do plano central")


def valida_garganta():
    """O `throat` pulsa na digestao. Sem volume proprio EMBAIXO da boca, nada pulsa."""
    garganta = caixa("throat")
    boca = caixa("jaw")
    cranio = caixa("head")
    if garganta.y + garganta.h > boca.y:
        raise ValueError("a garganta (topo em y=%s) invade a mandibula (base em y=%s)"
                         % (garganta.y + garganta.h, boca.y))
    if garganta.z < cranio.z or garganta.z + garganta.d > cranio.z + cranio.d:
        raise ValueError("a garganta nao fica embaixo da cabeca: ela pulsaria no lugar errado")


def _osso_json(o):
    d = {"name": o.nome}
    if o.pai is not None:
        d["parent"] = o.pai
    d["pivot"] = list(o.pivot)
    if o.rotacao is not None:
        d["rotation"] = list(o.rotacao)
    cubos = [{"origin": [c.x, c.y, c.z], "size": [c.w, c.h, c.d], "uv": [c.u, c.v]}
             for c in CAIXAS if c.osso == o.nome]
    if cubos:
        d["cubes"] = cubos
    return d


def geometria():
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": IDENTIFICADOR,
                "texture_width": UV_LARGURA,
                "texture_height": UV_ALTURA,
                "visible_bounds_width": 2,
                "visible_bounds_height": 1.5,
                "visible_bounds_offset": [0, 0.75, 0],
            },
            "bones": [_osso_json(o) for o in OSSOS],
        }],
    }


def _achatar_numeros(texto):
    """Poe os vetores numericos numa linha so -- o JSON e gerado, mas e lido em review."""
    return re.sub(r"\[\s+([-\d.,\se]+?)\s*\]",
                  lambda m: "[" + ", ".join(m.group(1).replace(",", " ").split()) + "]",
                  texto)


def main():
    valida_ossos()
    valida_uv()
    valida_hitbox()
    valida_boca()
    valida_olhos()
    valida_garganta()

    texto = _achatar_numeros(json.dumps(geometria(), indent=2))
    json.loads(texto)  # o JSON gerado tem de continuar valido depois do achatamento

    destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                           "geo", "entity", "frog_in_waiting.geo.json")
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    with open(destino, "w", encoding="utf-8", newline="\n") as f:
        f.write(texto + "\n")

    (x0, x1), (y0, y1), (z0, z1) = limites()
    ocupado = sum((2 * c.d + 2 * c.w) * (c.d + c.h) for c in CAIXAS)
    print("escrito", destino)
    print("ossos: %d   caixas: %d" % (len(OSSOS), len(CAIXAS)))
    print("caixa envolvente: x %s..%s  y %s..%s  z %s..%s (px)" % (x0, x1, y0, y1, z0, z1))
    print("largura %s px x altura %s px x profundidade %s px  (hitbox %.1f x %.1f)"
          % (x1 - x0, y1, z1 - z0, HITBOX_LARGURA_PX, HITBOX_ALTURA_PX))
    print("atlas %dx%d: %d de %d px ocupados (%.0f%%)"
          % (UV_LARGURA, UV_ALTURA, ocupado, UV_LARGURA * UV_ALTURA,
             100.0 * ocupado / (UV_LARGURA * UV_ALTURA)))


if __name__ == "__main__":
    main()
