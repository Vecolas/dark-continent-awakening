"""Gera a textura autoral do Frog-In-Waiting (64 x 64), casada com frog_in_waiting.geo.json.

POR QUE ESTE ARQUIVO EXISTE. Mesma razao do gerador de geometria (ADR-017 e
ADR-007): o corpo emprestado do sapo vanilla sai, e o binario que entra precisa
ter gerador no git -- PNG sem gerador e um arquivo que ninguem consegue
corrigir.

A TABELA DE CAIXAS NAO MORA AQUI. Ela e importada de frog_in_waiting_geo.CAIXAS,
de proposito: se cada gerador tivesse a sua, os dois divergiriam na primeira
correcao de modelo e o sintoma nao seria erro nenhum -- seria uma face pintada
no lugar errado, que so aparece na tela.

A CLASSE `Pincel` ABAIXO E PARECIDA COM A DO GREAT STAMP, E ISSO E DELIBERADO.
Ela nao e conhecimento compartilhado: e o mesmo layout de caixa do Bedrock
reescrito para esta paleta. A unica coisa que NAO pode ser duplicada -- a tabela
de caixas -- e importada. Se um dia um terceiro mob precisar do mesmo pincel,
ele vira modulo; dois usos nao pagam a indirecao.

Layout de caixa do Minecraft: para um cubo w x h x d a partir de (u,v),
    topo     (u+d,       v)     w x d
    base     (u+d+w,     v)     w x d
    direita  (u,         v+d)   d x h   (lado -X; a FRENTE fica na borda direita)
    frente   (u+d,       v+d)   w x h   (face -Z)
    esquerda (u+d+w,     v+d)   d x h   (lado +X; a FRENTE fica na borda esquerda)
    tras     (u+2d+w,    v+d)   w x h   (face +Z)

Todo pincel e preso a uma caixa e RECUSA pintar fora do retangulo dela: vazar
para a regiao do vizinho nao da erro, da a face errada no bicho errado.

LEITURA A DISTANCIA -- e para isso que a paleta existe, e cada item aqui e uma
coisa que o jogador precisa entender ANTES de morrer:

(a) ENTERRADO, O SAPO TEM DE PARECER CHAO. O dorso e lama sobre verde-pantano,
    salpicado: e o que faz o bicho sumir no terreno enquanto espera. Se o dorso
    fosse verde vivo, a emboscada nunca pegaria ninguem e o mob perderia o
    sentido inteiro.

(b) OS OLHOS SAO O AVISO, E O AVISO TEM DE GRITAR. Ambar quase fluorescente com
    pupila horizontal preta, pintada em CINCO faces (topo inclusive) -- porque
    quem se aproxima da toca ve o olho de cima, nao de frente. Este e o unico
    contraste alto da folha inteira, de proposito: dois pontos amarelos no lodo.

(c) A BOCA E VERMELHA E MOLHADA POR DENTRO. O teto da boca e o interior da
    mandibula sao a unica cor quente grande do bicho, e so aparecem quando o
    osso `jaw` abre. Fechado, o sapo e uma pedra de lodo; aberto, e uma ferida.

(d) A GARGANTA E PALIDA E COM PREGAS. As pregas existem para a pulsacao da
    digestao ser VISIVEL: um saco de cor chapada inchando nao le como engolir,
    le como bug de escala.

Regerar:  python art-source/enemies/frog_in_waiting/frog_in_waiting_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/frog_in_waiting/adulto.png
"""
from PIL import Image
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from frog_in_waiting_geo import CAIXAS, UV_LARGURA, UV_ALTURA  # noqa: E402  (fonte unica)

DORSO = (56, 64, 42)          # lombo encharcado de lama -- a cor do chao do pantano
COURO = (84, 96, 58)          # flanco, verde-pantano
VENTRE = (162, 168, 122)      # ventre palido
LAMA = (46, 42, 32)           # borrao de barro seco no lombo
MANCHA = (38, 48, 30)         # mosqueado do couro
LABIO = (30, 34, 24)          # a linha da boca, quase preta
BOCA = (132, 50, 56)          # interior molhado
BOCA_FUNDO = (84, 28, 36)     # o fundo da garganta, mais escuro
LINGUA = (188, 98, 104)       # a lingua, mais clara que o resto do interior
OLHO = (236, 180, 50)         # ambar -- o unico contraste alto da folha
OLHO_BRILHO = (255, 230, 140)
PUPILA = (14, 12, 10)
PALPEBRA = (62, 52, 26)
GULAR = (150, 158, 112)       # saco gular, palido
GULAR_PREGA = (100, 110, 74)  # as pregas que fazem a pulsacao ser legivel
PATA = (60, 66, 42)
MEMBRANA = (118, 124, 84)     # a pele entre os dedos
UNHA = (26, 28, 20)

img = Image.new("RGBA", (UV_LARGURA, UV_ALTURA), (0, 0, 0, 0))
px = img.load()


def ruido(x, y, forca):
    """Variacao deterministica: cor chapada le como plastico.

    O hash e embaralhado de proposito. Combinacao linear simples (x*7+y*13)
    produz FAIXAS DIAGONAIS regulares, que a distancia leem como listra e nao
    como pele -- e isso so aparece na tela.
    """
    h = (x * 374761393 + y * 668265263) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((h >> 7) % 5 - 2) * forca // 2


def _sorteio(x, y, semente):
    """0..255 deterministico por pixel, para decidir onde cai um borrao."""
    h = (x * 2654435761 + y * 40503 + semente * 2246822519) & 0xFFFFFFFF
    h = (h ^ (h >> 15)) * 2246822519 & 0xFFFFFFFF
    return (h >> 13) & 0xFF


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
        """Linhas contadas a partir do TOPO do retangulo da face."""
        x, y, w, h = self.faces()[nome]
        self.retangulo(x, y, w, min(linhas, h), cor, forca)

    def faixa_no_pe_da_face(self, nome, linhas, cor, forca=4):
        """Linhas contadas a partir da BASE do retangulo da face."""
        x, y, w, h = self.faces()[nome]
        n = min(linhas, h)
        self.retangulo(x, y + h - n, w, n, cor, forca)

    def linha_central_da_face(self, nome, altura, cor, forca=3):
        """Faixa horizontal no meio da face -- usada na pupila e nas pregas."""
        x, y, w, h = self.faces()[nome]
        n = min(altura, h)
        self.retangulo(x, y + (h - n) // 2, w, n, cor, forca)

    def salpicar(self, nome, cor, limiar, semente, forca=3):
        """Borroes de 1 px espalhados pela face, deterministicos.

        Mosqueado nao e enfeite: e o que faz o dorso ler como terra e nao como
        um bloco pintado -- a distancia, cor chapada denuncia o mob que deveria
        estar escondido.
        """
        x, y, w, h = self.faces()[nome]
        for i in range(x, x + w):
            for j in range(y, y + h):
                if _sorteio(i, j, semente) < limiar:
                    self.retangulo(i, j, 1, 1, cor, forca)


def pele(p, dorsal=DORSO, flanco=COURO, ventral=VENTRE, semente=1):
    """Couro de tronco: lama em cima, palido embaixo, mosqueado nos flancos."""
    p.tudo(flanco)
    p.face("topo", dorsal)
    p.face("base", ventral)
    for lado in ("direita", "esquerda"):
        p.faixa_no_topo_da_face(lado, 2, dorsal)
        p.faixa_no_pe_da_face(lado, 2, ventral)
    p.faixa_no_pe_da_face("frente", 2, ventral)
    p.faixa_no_pe_da_face("tras", 2, ventral)
    # o mosqueado vai por ultimo para cair POR CIMA das faixas
    p.salpicar("topo", LAMA, 74, semente)
    p.salpicar("topo", MANCHA, 52, semente + 7)
    for lado in ("direita", "esquerda", "tras"):
        p.salpicar(lado, MANCHA, 46, semente + 13)


pinceis = {c.nome: Pincel(c) for c in CAIXAS}

# --- tronco --------------------------------------------------------------
pele(pinceis["body"], semente=3)

# --- cranio --------------------------------------------------------------
# O cranio e a peca que fica de fora quando o sapo so afunda parcialmente, entao
# ele carrega o mosqueado mais forte da folha.
cabeca = pinceis["head"]
pele(cabeca, semente=11)
# O TETO DA BOCA. A face de baixo do cranio nao e couro: e carne molhada, e ela
# so aparece quando o osso `jaw` abre.
cabeca.face("base", BOCA, 5)
bx, by, bw, bh = cabeca.faces()["base"]
cabeca.retangulo(bx, by, bw, 2, BOCA_FUNDO, 3)          # o fundo da garganta escurece
cabeca.retangulo(bx + 2, by + 3, bw - 4, 1, BOCA_FUNDO, 2)  # a prega do ceu da boca
# A LINHA DA BOCA: uma fiada quase preta na borda de baixo de tudo que faz parede
# de boca. E ela que desenha o rasgo de ponta a ponta com a mandibula FECHADA.
for lado in ("direita", "esquerda", "frente"):
    cabeca.faixa_no_pe_da_face(lado, 1, LABIO, 2)
# narinas: dois pontos no alto do focinho
fx, fy, fw, fh = cabeca.faces()["frente"]
px[fx + 5, fy + 1] = (*LABIO, 255)
px[fx + fw - 6, fy + 1] = (*LABIO, 255)
# as orbitas: sombra no cranio embaixo de cada bulbo, para o olho nao parecer colado
tx, ty, tw, td = cabeca.faces()["topo"]
cabeca.retangulo(tx + 10, ty + 1, 5, 5, MANCHA, 2)
cabeca.retangulo(tx + 1, ty + 1, 5, 5, MANCHA, 2)

# --- a mandibula: a arma -------------------------------------------------
queixo = pinceis["jaw"]
queixo.tudo(COURO)
queixo.salpicar("frente", MANCHA, 40, 23)
for lado in ("direita", "esquerda", "frente"):
    queixo.faixa_no_topo_da_face(lado, 1, LABIO, 2)   # o labio de baixo fecha a linha da boca
queixo.face("base", VENTRE, 5)                        # o queixo por fora, palido
queixo.face("tras", BOCA_FUNDO, 4)                    # a entrada da garganta
# O INTERIOR DA MANDIBULA. E a maior mancha quente da folha: ela e o "aberto".
queixo.face("topo", BOCA, 5)
qx, qy, qw, qd = queixo.faces()["topo"]
queixo.retangulo(qx + qw // 2 - 2, qy, 4, qd - 1, LINGUA, 4)   # a lingua, do fundo a ponta
queixo.retangulo(qx, qy + qd - 2, qw, 2, BOCA_FUNDO, 3)        # sombra no fundo da boca

# --- os olhos: o unico aviso ---------------------------------------------
# Cinco faces com pupila, topo inclusive: quem chega na toca ve o olho DE CIMA.
for nome in ("eye_left", "eye_right"):
    olho = pinceis[nome]
    olho.tudo(OLHO, 3)
    olho.face("base", PALPEBRA, 2)                     # a raiz, onde o bulbo entra no cranio
    for lado in ("topo", "frente", "direita", "esquerda", "tras"):
        olho.faixa_no_topo_da_face(lado, 1, OLHO_BRILHO, 2)
        olho.linha_central_da_face(lado, 1, PUPILA, 0)  # pupila HORIZONTAL, como a de sapo

# --- a garganta: o que pulsa na digestao ---------------------------------
garganta = pinceis["throat"]
garganta.tudo(GULAR, 5)
garganta.face("topo", GULAR_PREGA, 3)                  # encosto na mandibula, em sombra
for lado in ("direita", "esquerda", "frente", "tras"):
    garganta.linha_central_da_face(lado, 1, GULAR_PREGA, 2)
    garganta.faixa_no_pe_da_face(lado, 1, GULAR_PREGA, 2)
gx, gy, gw, gd = garganta.faces()["base"]
garganta.retangulo(gx, gy + gd // 2, gw, 1, GULAR_PREGA, 2)  # a prega que abre quando incha

# --- patas ---------------------------------------------------------------
for nome in ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"):
    perna = pinceis[nome]
    perna.tudo(COURO)
    perna.face("topo", DORSO, 3)
    for lado in ("direita", "esquerda", "frente", "tras"):
        perna.faixa_no_topo_da_face(lado, 1, VENTRE, 3)  # encontro com o ventre
        perna.faixa_no_pe_da_face(lado, 1, PATA, 3)      # a canela escurece para o pe
    perna.salpicar("frente", MANCHA, 40, 31)

for nome in ("foot_front_left", "foot_front_right", "foot_back_left", "foot_back_right"):
    pe = pinceis[nome]
    pe.tudo(PATA, 4)
    # A MEMBRANA entre os dedos, com os dedos riscados por cima: e o que separa
    # um pe de sapo de um toco escuro qualquer, a distancia.
    for lado in ("topo", "base"):
        pe.face(lado, MEMBRANA, 4)
        fx, fy, fw, fd = pe.faces()[lado]
        for corte in range(1, fw, 2):
            pe.retangulo(fx + corte, fy, 1, fd, UNHA, 2)

destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "textures", "entity", "frog_in_waiting", "adulto.png")
os.makedirs(os.path.dirname(destino), exist_ok=True)
img.save(destino)
print("escrito", destino, img.size)
print("caixas pintadas:", len(pinceis))
