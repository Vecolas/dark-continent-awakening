"""Gera a textura autoral do Great Stamp (128 x 64), casada com great_stamp.geo.json.

POR QUE ESTE ARQUIVO EXISTE. Mesma razao do gerador de geometria (ADR-017 e
ADR-007): o corpo emprestado do hoglin sai, e o binario que entra precisa ter
gerador no git -- PNG sem gerador e um arquivo que ninguem consegue corrigir.

A TABELA DE CAIXAS NAO MORA AQUI. Ela e importada de great_stamp_geo.CAIXAS,
de proposito: se cada gerador tivesse a sua, os dois divergiriam na primeira
correcao de modelo e o sintoma nao seria erro nenhum -- seria uma face pintada
no lugar errado, que so aparece na tela.

Layout de caixa do Minecraft: para um cubo w x h x d a partir de (u,v),
    topo     (u+d,       v)     w x d
    base     (u+d+w,     v)     w x d
    direita  (u,         v+d)   d x h   (lado -X; a FRENTE fica na borda direita)
    frente   (u+d,       v+d)   w x h   (face -Z)
    esquerda (u+d+w,     v+d)   d x h   (lado +X; a FRENTE fica na borda esquerda)
    tras     (u+2d+w,    v+d)   w x h   (face +Z)

Todo pincel e preso a uma caixa e RECUSA pintar fora do retangulo dela: vazar
para a regiao do vizinho nao da erro, da a face errada no bicho errado.

LEITURA A DISTANCIA -- e para isso que a paleta existe:
(a) pesado e baixo: dorso escuro, ventre claro, cascos quase pretos;
(b) a TESTA e uma regiao distinta, em osso queimado, destacada do couro escuro
    e das presas. Nao e enfeite: o servidor chama de "forehead" tudo que for
    acertado acima de 62% da altura da caixa dentro de um cone frontal, e o que
    o jogador ve tem de coincidir com o que o servidor mede.

Regerar:  python art-source/enemies/great_stamp/great_stamp_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/great_stamp/adulto.png
"""
from PIL import Image
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from great_stamp_geo import CAIXAS, UV_LARGURA, UV_ALTURA  # noqa: E402  (fonte unica da tabela)

COURO = (86, 64, 50)         # flanco de javali
DORSO = (52, 39, 32)         # dorso, mais escuro
VENTRE = (128, 104, 84)      # ventre, mais claro
CERDA = (36, 28, 24)         # crista de cerdas do cachaco
TESTA = (186, 146, 76)       # a placa -- tem de saltar do couro E das presas
TESTA_BORDA = (116, 84, 40)
PRESA = (232, 224, 200)
PRESA_BASE = (168, 158, 134)
CASCO = (30, 26, 23)
FOCINHO = (112, 82, 74)
NARINA = (26, 20, 18)
OLHO = (22, 17, 15)
IRIS = (202, 128, 44)

img = Image.new("RGBA", (UV_LARGURA, UV_ALTURA), (0, 0, 0, 0))
px = img.load()


def ruido(x, y, forca):
    """Variacao deterministica: cor chapada le como plastico.

    O hash e embaralhado de proposito. Combinacao linear simples (x*7+y*13)
    produz FAIXAS DIAGONAIS regulares, que a distancia leem como listra e nao
    como pelo -- e isso so aparece na tela.
    """
    h = (x * 374761393 + y * 668265263) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((h >> 7) % 5 - 2) * forca // 2


class Pincel:
    """Pincel preso a uma caixa. Pintar fora do retangulo dela e erro, nao aviso."""

    def __init__(self, caixa):
        self.c = caixa
        self.x0, self.y0 = caixa.u, caixa.v
        self.x1 = caixa.u + 2 * caixa.d + 2 * caixa.w
        self.y1 = caixa.v + caixa.d + caixa.h

    def faces(self):
        c = self.c
        return {
            "topo": (c.u + c.d, c.v, c.w, c.d),
            "base": (c.u + c.d + c.w, c.v, c.w, c.d),
            "direita": (c.u, c.v + c.d, c.d, c.h),
            "frente": (c.u + c.d, c.v + c.d, c.w, c.h),
            "esquerda": (c.u + c.d + c.w, c.v + c.d, c.d, c.h),
            "tras": (c.u + 2 * c.d + c.w, c.v + c.d, c.w, c.h),
        }

    def retangulo(self, x, y, w, h, cor, forca=6):
        if w <= 0 or h <= 0:
            return
        if x < self.x0 or y < self.y0 or x + w > self.x1 or y + h > self.y1:
            raise ValueError("pincel da caixa '%s' vazou para fora do retangulo dela: "
                             "(%d,%d)+%dx%d fora de (%d,%d)..(%d,%d)"
                             % (self.c.nome, x, y, w, h, self.x0, self.y0, self.x1, self.y1))
        for i in range(x, x + w):
            for j in range(y, y + h):
                d = ruido(i, j, forca)
                px[i, j] = (max(0, min(255, cor[0] + d)),
                            max(0, min(255, cor[1] + d)),
                            max(0, min(255, cor[2] + d)), 255)

    def tudo(self, cor, forca=6):
        for x, y, w, h in self.faces().values():
            self.retangulo(x, y, w, h, cor, forca)

    def face(self, nome, cor, forca=6):
        x, y, w, h = self.faces()[nome]
        self.retangulo(x, y, w, h, cor, forca)

    def faixa_no_topo_da_face(self, nome, linhas, cor, forca=4):
        """Linhas contadas a partir do TOPO do cubo (a primeira linha do retangulo)."""
        x, y, w, h = self.faces()[nome]
        self.retangulo(x, y, w, min(linhas, h), cor, forca)

    def faixa_no_pe_da_face(self, nome, linhas, cor, forca=4):
        """Linhas contadas a partir da BASE do cubo (a ultima linha do retangulo)."""
        x, y, w, h = self.faces()[nome]
        n = min(linhas, h)
        self.retangulo(x, y + h - n, w, n, cor, forca)


def pele(p, dorsal=DORSO, flanco=COURO, ventral=VENTRE, crista=False):
    """Couro de tronco: escuro em cima, claro embaixo, com a crista opcional."""
    p.tudo(flanco)
    p.face("topo", dorsal)
    p.face("base", ventral)
    for lado in ("direita", "esquerda"):
        p.faixa_no_topo_da_face(lado, 3, dorsal)
        p.faixa_no_pe_da_face(lado, 3, ventral)
    p.faixa_no_pe_da_face("frente", 3, ventral)
    p.faixa_no_pe_da_face("tras", 3, ventral)
    if crista:
        x, y, w, h = p.faces()["topo"]
        p.retangulo(x + w // 2 - 1, y, 2, h, CERDA, 3)


pinceis = {c.nome: Pincel(c) for c in CAIXAS}

# --- tronco --------------------------------------------------------------
pele(pinceis["chest"], crista=True)
pele(pinceis["body"], crista=True)
pele(pinceis["neck"], crista=True)

# cicatrizes de briga nos flancos do peitoral -- um bruto que ja levou pancada
peito = pinceis["chest"]
for lado in ("direita", "esquerda"):
    fx, fy, fw, fh = peito.faces()[lado]
    peito.retangulo(fx + 2, fy + 5, 5, 1, VENTRE, 2)
    peito.retangulo(fx + 4, fy + 8, 4, 1, VENTRE, 2)

# --- cabeca --------------------------------------------------------------
cabeca = pinceis["head"]
pele(cabeca, crista=True)
# a faixa de baixo da face frontal e o unico pedaco de cara que a placa da testa
# nao cobre: e o alto do focinho. Ele entra pelo MEIO, com couro sobrando dos
# dois lados -- focinho da borda a borda le como cara chapada, nao como bicho.
fx, fy, fw, fh = cabeca.faces()["frente"]
cabeca.retangulo(fx + 2, fy + fh - 3, fw - 4, 3, FOCINHO, 4)
# olhos altos na lateral, junto da frente da cabeca (javali enxerga de lado).
dx, dy, dw, dh = cabeca.faces()["direita"]
cabeca.retangulo(dx + dw - 3, dy + 2, 2, 2, OLHO, 0)
px[dx + dw - 2, dy + 2] = (*IRIS, 255)
ex, ey, ew, eh = cabeca.faces()["esquerda"]
cabeca.retangulo(ex + 1, ey + 2, 2, 2, OLHO, 0)
px[ex + 1, ey + 2] = (*IRIS, 255)

# --- a placa da testa: o alvo -------------------------------------------
testa = pinceis["forehead"]
testa.tudo(TESTA, 5)
tx, ty, tw, th = testa.faces()["frente"]
# moldura escura: e o que separa a placa do couro a distancia
testa.retangulo(tx, ty, tw, 1, TESTA_BORDA, 2)
testa.retangulo(tx, ty + th - 1, tw, 1, TESTA_BORDA, 2)
testa.retangulo(tx, ty, 1, th, TESTA_BORDA, 2)
testa.retangulo(tx + tw - 1, ty, 1, th, TESTA_BORDA, 2)
# duas marcas de impacto no meio da placa
testa.retangulo(tx + 3, ty + 2, 4, 1, TESTA_BORDA, 2)
testa.retangulo(tx + 5, ty + 1, 1, 4, TESTA_BORDA, 2)
testa.faixa_no_pe_da_face("topo", 1, TESTA_BORDA, 2)

# --- mandibula e focinho -------------------------------------------------
queixo = pinceis["jaw"]
queixo.tudo(COURO)
queixo.face("base", FOCINHO, 4)
qx, qy, qw, qh = queixo.faces()["frente"]
queixo.retangulo(qx + 1, qy, qw - 2, qh, FOCINHO, 4)  # o disco do focinho, sem tocar as bordas
px[qx + 3, qy + 1] = (*NARINA, 255)
px[qx + qw - 4, qy + 1] = (*NARINA, 255)
for lado in ("direita", "esquerda"):
    queixo.faixa_no_pe_da_face(lado, 1, FOCINHO, 3)

# --- presas --------------------------------------------------------------
for nome in ("tusk_left", "tusk_right"):
    presa = pinceis[nome]
    presa.tudo(PRESA, 3)
    for lado in ("direita", "esquerda", "frente", "tras"):
        presa.faixa_no_pe_da_face(lado, 2, PRESA_BASE, 3)  # a raiz, onde some na gengiva

# --- pernas e cascos -----------------------------------------------------
for nome in ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"):
    perna = pinceis[nome]
    perna.tudo(COURO)
    perna.face("topo", DORSO, 3)
    for lado in ("direita", "esquerda", "frente", "tras"):
        perna.faixa_no_topo_da_face(lado, 2, VENTRE, 3)   # encontro com o ventre
        perna.faixa_no_pe_da_face(lado, 3, DORSO, 3)      # canela escurecendo para o casco

for nome in ("hoof_front_left", "hoof_front_right", "hoof_back_left", "hoof_back_right"):
    casco = pinceis[nome]
    casco.tudo(CASCO, 4)
    casco.face("base", CASCO, 2)
    cx, cy, cw, ch = casco.faces()["frente"]
    casco.retangulo(cx + cw // 2, cy, 1, ch, (0, 0, 0), 0)  # a fenda entre as unhas

# --- cauda ---------------------------------------------------------------
rabo = pinceis["tail"]
rabo.tudo(COURO)
rabo.face("topo", DORSO, 3)
rabo.face("tras", CERDA, 3)   # o tufo da ponta

destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "textures", "entity", "great_stamp", "adulto.png")
os.makedirs(os.path.dirname(destino), exist_ok=True)
img.save(destino)
print("escrito", destino, img.size)
print("caixas pintadas:", len(pinceis))
