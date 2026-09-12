"""Gera a textura do Foxbear para a GEOMETRIA EMPRESTADA do urso polar vanilla.

POR QUE ESTE ARQUIVO EXISTE. O Foxbear ja tem arte autoral -- foxbear.png, um
atlas 1254x1254 feito para um modelo GeckoLib que ainda nao existe no repositorio.
A entidade, enquanto isso, renderiza com a camada vanilla ModelLayers.POLAR_BEAR,
cuja UV e 128x64. Uma textura quadrada numa UV 2:1 nao da erro: da um bicho
manchado. Entao o renderer apontava para a textura do URSO POLAR, e o Foxbear
aparecia branco -- que em jogo le como "sem textura nenhuma".

Esta e a textura autoral que casa com a UV emprestada. Ela MORRE no dia em que o
modelo GeckoLib chegar; quem sobrevive e foxbear.png.

Os retangulos abaixo NAO foram chutados: saem de PolarBearModel.createBodyLayer(),
cubo por cubo, pela regra de layout de caixa do Minecraft -- para um cubo de
w x h x d em (u,v): topo (u+d, v, w x d), base (u+d+w, v, w x d), e as quatro
laterais na linha v+d, na ordem direita (u, d x h), frente (u+d, w x h),
esquerda (u+d+w, d x h), tras (u+2d+w, w x h).

Regerar:  python art-source/enemies/foxbear/emprestado.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/foxbear/emprestado.png
"""
from PIL import Image
import os

LARGURA, ALTURA = 128, 64

PELO = (107, 68, 48)        # dorso de urso
PELO_CLARO = (156, 90, 46)  # cabeca de raposa
CREME = (217, 203, 178)     # focinho, peito, ponta da cauda
ESCURO = (58, 42, 32)       # patas
NARIZ = (26, 20, 18)

img = Image.new("RGBA", (LARGURA, ALTURA), (0, 0, 0, 0))
px = img.load()


def ruido(x, y, forca=6):
    """Variacao deterministica: textura chapada le como plastico."""
    return ((x * 7 + y * 13) % 5 - 2) * forca // 2


def pintar(x0, y0, w, h, cor, forca=6):
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            if not (0 <= x < LARGURA and 0 <= y < ALTURA):
                continue
            d = ruido(x, y, forca)
            px[x, y] = (max(0, min(255, cor[0] + d)),
                        max(0, min(255, cor[1] + d)),
                        max(0, min(255, cor[2] + d)), 255)


def caixa(u, v, w, h, d, cor, forca=6):
    """As seis faces de um cubo, na ordem que o Minecraft baka."""
    pintar(u + d, v, w, d, cor, forca)              # topo
    pintar(u + d + w, v, w, d, cor, forca)          # base
    pintar(u, v + d, d, h, cor, forca)              # direita
    pintar(u + d, v + d, w, h, cor, forca)          # frente
    pintar(u + d + w, v + d, d, h, cor, forca)      # esquerda
    pintar(u + 2 * d + w, v + d, w, h, cor, forca)  # tras


# --- corpo: dois cubos, o traseiro e o dianteiro -------------------------
caixa(0, 19, 14, 14, 11, PELO)
caixa(39, 0, 12, 12, 10, PELO)
# peito claro na frente do cubo dianteiro (face frontal em u+d, v+d)
pintar(39 + 10 + 3, 0 + 10 + 5, 6, 7, CREME, 4)

# --- cabeca -------------------------------------------------------------
caixa(0, 0, 7, 7, 7, PELO_CLARO)
FRENTE_X, FRENTE_Y = 0 + 7, 0 + 7          # face frontal da cabeca
pintar(FRENTE_X + 1, FRENTE_Y + 3, 5, 4, CREME, 4)   # mascara clara
px[FRENTE_X + 1, FRENTE_Y + 2] = (*NARIZ, 255)       # olho esquerdo
px[FRENTE_X + 5, FRENTE_Y + 2] = (*NARIZ, 255)       # olho direito

# --- focinho ------------------------------------------------------------
caixa(0, 44, 5, 3, 3, CREME, 4)
pintar(0 + 3 + 1, 44 + 3 + 0, 3, 1, NARIZ, 0)        # nariz na frente do focinho

# --- orelhas (um cubo so, espelhado pelo modelo) ------------------------
caixa(26, 0, 2, 2, 1, PELO_CLARO, 4)
pintar(26 + 1, 0 + 1, 2, 2, CREME, 3)                # miolo claro

# --- pernas: traseiras e dianteiras -------------------------------------
for u, v, prof in ((50, 22, 8), (50, 40, 6)):
    caixa(u, v, 4, 10, prof, PELO)
    pintar(u + prof + 4, v, 4, prof, ESCURO, 3)      # face de baixo = a pata
    for dx, larg in ((0, prof), (prof, 4), (prof + 4, prof), (2 * prof + 4, 4)):
        pintar(u + dx, v + prof + 8, larg, 2, ESCURO, 3)  # dois ultimos aneis da perna

destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "textures", "entity", "foxbear", "emprestado.png")
os.makedirs(os.path.dirname(destino), exist_ok=True)
img.save(destino)
print("escrito", destino, img.size)
