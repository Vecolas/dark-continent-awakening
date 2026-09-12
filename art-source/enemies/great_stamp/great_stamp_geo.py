"""Gera a geometria autoral do Great Stamp (bedrock 1.12.0, carregada pelo GeckoLib 4).

POR QUE ESTE ARQUIVO EXISTE. O ADR-017 diz que mob vanilla e andaime: o Great
Stamp funcionava vestindo a geometria do HOGLIN e por isso parecia pronto. Este
gerador produz o corpo proprio que substitui o emprestado. Nao ha Blockbench
aqui, entao a FONTE VERSIONADA e este Python -- binario ou JSON gerado sem
gerador no git vira um arquivo que ninguem consegue corrigir depois.

ESTE MODULO E A UNICA TABELA DE CAIXAS. O gerador da textura importa CAIXAS
daqui de proposito: duas tabelas separadas divergem na primeira correcao, e a
divergencia nao da erro -- da textura borrada, que so aparece na tela.

A HITBOX MANDA NO MODELO. A entidade e sized(1.9F, 1.55F) -- 30.4 x 24.8 px --
e as validacoes abaixo reprovam se o modelo estourar isso. O piso dos cascos
fica em y=0.

A TESTA E PONTO FRACO MEDIDO PELO SERVIDOR:
HunterExamProfiles usa WeakPointResolver("forehead", "body", 0.62, 0.5), ou
seja, acima de 62% de 24.8 px = 15.4 px e dentro de um cone frontal de 60 graus.
A placa da testa fica em y 16..22, inteira dentro dessa faixa, e o focinho em
y 10..16, inteiro FORA dela. O que o jogador ve coincide com o que o servidor
mede -- se alguem mexer nas alturas, mexeu na regra de dano tambem.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

Regerar:  python art-source/enemies/great_stamp/great_stamp_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/great_stamp.geo.json
"""
from collections import namedtuple
import json
import os
import re

IDENTIFICADOR = "geometry.great_stamp"
UV_LARGURA, UV_ALTURA = 128, 64

# Limites que a entidade impoe: EnemyEntityTypes .sized(1.9F, 1.55F).
HITBOX_LARGURA_PX = 1.9 * 16.0
HITBOX_ALTURA_PX = 1.55 * 16.0

# Altura relativa a partir da qual o servidor chama a regiao de "forehead".
# HunterExamProfiles: new WeakPointResolver("forehead", "body", 0.62D, 0.5D).
TESTA_ALTURA_MINIMA = 0.62

Osso = namedtuple("Osso", "nome pai pivot rotacao")

# Hierarquia CONGELADA pelo contrato da entrega: as animacoes escrevem contra
# estes nomes. Nome errado aqui nao da erro -- o GeckoLib so deixa o osso parado.
# O pivot de um filho mora na JUNCAO com o pai, senao a animacao gira errado.
OSSOS = (
    Osso("root", None, (0, 0, 0), None),
    Osso("body", "root", (0, 15, 9), None),
    Osso("chest", "body", (0, 16, 5), None),
    Osso("neck", "chest", (0, 18, -4), None),
    Osso("head", "neck", (0, 17, -8), None),
    Osso("jaw", "head", (0, 12, -7), None),
    # Presas: pegas nao animadas, curvadas para cima e para a frente pela
    # rotacao de base (-30 em X inclina o topo para -Z, levando a ponta de
    # z=-12 para z=-15) e abertas para fora pelos 12 graus em Z.
    Osso("tusk_left", "head", (6, 11, -12), (-30, 0, -12)),
    Osso("tusk_right", "head", (-6, 11, -12), (-30, 0, 12)),
    Osso("leg_front_left", "body", (5.5, 9, -1.5), None),
    Osso("hoof_front_left", "leg_front_left", (5.5, 2, -1.5), None),
    Osso("leg_front_right", "body", (-5.5, 9, -1.5), None),
    Osso("hoof_front_right", "leg_front_right", (-5.5, 2, -1.5), None),
    Osso("leg_back_left", "body", (5.5, 9, 9.5), None),
    Osso("hoof_back_left", "leg_back_left", (5.5, 2, 9.5), None),
    Osso("leg_back_right", "body", (-5.5, 9, 9.5), None),
    Osso("hoof_back_right", "leg_back_right", (-5.5, 2, 9.5), None),
    Osso("tail", "body", (0, 18, 13), None),
)

Caixa = namedtuple("Caixa", "nome osso u v x y z w h d")

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v) -- a
# validacao abaixo reprova sobreposicao e estouro do atlas.
CAIXAS = (
    # tronco: cernelha alta na frente, garupa mais baixa atras, ventre a 8 px do
    # chao -- e o que faz o bicho ler como PESADO E BAIXO, e nao como gado.
    Caixa("chest", "chest", 0, 0, -9, 8, -5, 18, 16, 10),
    Caixa("body", "body", 56, 0, -8, 8, 5, 16, 14, 8),
    # o cachaco fecha o degrau entre a cernelha (topo 24) e o cranio (topo 22)
    Caixa("neck", "neck", 82, 26, -5, 12, -9, 10, 10, 5),
    # cabeca baixa e larga, e a PLACA DA TESTA como volume proprio na frente dela
    Caixa("head", "head", 0, 26, -7, 12, -13, 14, 10, 7),
    Caixa("forehead", "head", 80, 43, -6, 16, -15, 12, 6, 2),
    Caixa("jaw", "jaw", 42, 26, -6, 9, -14, 12, 3, 8),
    Caixa("tusk_left", "tusk_left", 104, 0, 5, 11, -13, 2, 6, 2),
    Caixa("tusk_right", "tusk_right", 112, 0, -7, 11, -13, 2, 6, 2),
    # pernas curtas e grossas
    Caixa("leg_front_left", "leg_front_left", 0, 43, 3, 2, -4, 5, 7, 5),
    Caixa("leg_front_right", "leg_front_right", 20, 43, -8, 2, -4, 5, 7, 5),
    Caixa("leg_back_left", "leg_back_left", 40, 43, 3, 2, 7, 5, 7, 5),
    Caixa("leg_back_right", "leg_back_right", 60, 43, -8, 2, 7, 5, 7, 5),
    Caixa("hoof_front_left", "hoof_front_left", 104, 14, 2.5, 0, -4.5, 6, 2, 6),
    Caixa("hoof_front_right", "hoof_front_right", 0, 55, -8.5, 0, -4.5, 6, 2, 6),
    Caixa("hoof_back_left", "hoof_back_left", 24, 55, 2.5, 0, 6.5, 6, 2, 6),
    Caixa("hoof_back_right", "hoof_back_right", 48, 55, -8.5, 0, 6.5, 6, 2, 6),
    Caixa("tail", "tail", 104, 8, -2, 16, 13, 4, 4, 2),
)


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
        raise ValueError("o piso dos cascos tem de ficar em y=0, esta em %s" % y0)
    if y1 > HITBOX_ALTURA_PX:
        raise ValueError("modelo mais alto (%s) que a hitbox (%s)" % (y1, HITBOX_ALTURA_PX))
    if (x1 - x0) > HITBOX_LARGURA_PX or (z1 - z0) > HITBOX_LARGURA_PX:
        raise ValueError("modelo mais largo/comprido que a hitbox de %s px" % HITBOX_LARGURA_PX)


def valida_testa():
    """A placa da testa tem de cair inteira na faixa que o servidor chama de ponto fraco."""
    testa = [c for c in CAIXAS if c.nome == "forehead"][0]
    piso = TESTA_ALTURA_MINIMA * HITBOX_ALTURA_PX
    if testa.y < piso:
        raise ValueError("a testa comeca em y=%s, abaixo do piso de ponto fraco (%.2f px)"
                         % (testa.y, piso))
    focinho = [c for c in CAIXAS if c.nome == "jaw"][0]
    if focinho.y + focinho.h > piso:
        raise ValueError("o focinho invade a faixa de ponto fraco: o jogador acertaria "
                         "a testa mirando o nariz")


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
                "visible_bounds_width": 3,
                "visible_bounds_height": 2,
                "visible_bounds_offset": [0, 1, 0],
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
    valida_testa()

    texto = _achatar_numeros(json.dumps(geometria(), indent=2))
    json.loads(texto)  # o JSON gerado tem de continuar valido depois do achatamento

    destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                           "geo", "entity", "great_stamp.geo.json")
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    with open(destino, "w", encoding="utf-8", newline="\n") as f:
        f.write(texto + "\n")

    (x0, x1), (y0, y1), (z0, z1) = limites()
    print("escrito", destino)
    print("ossos: %d   caixas: %d" % (len(OSSOS), len(CAIXAS)))
    print("caixa envolvente: x %s..%s  y %s..%s  z %s..%s (px)" % (x0, x1, y0, y1, z0, z1))
    print("hitbox: %.1f x %.1f px   testa a partir de %.1f px"
          % (HITBOX_LARGURA_PX, HITBOX_ALTURA_PX, TESTA_ALTURA_MINIMA * HITBOX_ALTURA_PX))


if __name__ == "__main__":
    main()
