"""Gera a textura autoral do Master of the Swamp (128 x 64), casada com o .geo.json.

POR QUE ESTE ARQUIVO EXISTE. Mesma razao do gerador de geometria (ADR-017 e
ADR-007): o binario que entra no repositorio precisa ter gerador no git -- PNG sem
gerador e um arquivo que ninguem consegue corrigir depois.

A TABELA DE CAIXAS NAO MORA AQUI. Ela e importada de master_of_the_swamp_geo.CAIXAS,
de proposito: se cada gerador tivesse a sua, os dois divergiriam na primeira
correcao de modelo e o sintoma nao seria erro nenhum -- seria uma face pintada no
lugar errado, que so aparece na tela.

A CLASSE `Pincel` E PARECIDA COM A DOS CINCO MOBS IRMAOS, E ISSO E DELIBERADO. Ela
nao e conhecimento compartilhado: e o mesmo layout de caixa do Bedrock reescrito
para esta paleta. A unica coisa que NAO pode ser duplicada -- a tabela de caixas --
e importada. O dia em que alguem quiser mexer no pincel de todos ao mesmo tempo e o
dia em que ele vira modulo; ate la, seis copias de 40 linhas custam menos que uma
indirecao que ninguem pediu.

Layout de caixa do Minecraft: para um cubo w x h x d a partir de (u,v),
    topo     (u+d,       v)     w x d
    base     (u+d+w,     v)     w x d
    direita  (u,         v+d)   d x h   (lado -X; a FRENTE fica na borda direita)
    frente   (u+d,       v+d)   w x h   (face -Z)
    esquerda (u+d+w,     v+d)   d x h   (lado +X; a FRENTE fica na borda esquerda)
    tras     (u+2d+w,    v+d)   w x h   (face +Z)

Todo pincel e preso a uma caixa e RECUSA pintar fora do retangulo dela: vazar para
a regiao do vizinho nao da erro, da a face errada no bicho errado.

O PONTO CEGO QUE ESTE ARQUIVO SE RECUSA A TER
---------------------------------------------
Nas faces de CIMA e de BAIXO, qual fiada da textura cai na FRENTE do bicho e qual
cai atras nao da para conferir fora do jogo: o layout acima fixa as COLUNAS (elas
seguem o eixo X) e nao as FIADAS. Um palpite errado ali nao levanta excecao -- poe
o olho na nuca e a garra virada para dentro.

Este mob e o pior lugar possivel para esse palpite: ele e visto principalmente DE
CIMA, atraves da agua. A face de cima nao e um detalhe dele, e quase tudo o que o
jogador ve antes de lancar a vara.

Entao aqui nada nas faces de cima e de baixo distingue frente de tras. Tudo o que
mora nelas e coluna (eixo X, garantido), mosqueado espelhado, cor uniforme ou fiada
pintada JUNTO COM a sua espelhada -- para isso existe `fiada_espelhada`, que
substitui o palpite por um par. `valida_faces_horizontais_sem_frente` reprova
qualquer marca que volte a depender do chute.

LEITURA A DISTANCIA -- e para isso que a paleta existe
------------------------------------------------------
O encontro inteiro e: achar o bicho na agua, fisgar, e decidir a cada segundo se
puxa ou acompanha. Cada item abaixo serve a um desses momentos.

(a) NA AGUA PARADA, ELE E FUNDO DE PANTANO. Dorso verde-barro mosqueado, sem
    saturacao nenhuma: ele e um encontro RARO, e achar o bicho e parte do
    encontro. Um dorso colorido entregaria a posicao antes de o jogador ter
    procurado. A unica linha continua em cima e a CRISTA clara -- e ela existe para
    o segundo depois, quando ele se mexe e precisa poder ser seguido.

(b) A BOCA E A MECANICA, ENTAO ELA E A COISA MAIS CLARA DA FOLHA. Os dentes sao o
    unico branco grande do bicho, com vao escuro entre eles e goela vermelha atras:
    de frente, a cara e uma armadilha e nao um peixe. Nada mais na folha chega
    perto desse valor, de proposito -- se a barriga fosse tao clara quanto o dente,
    a cara pararia de ser o que salta.

(c) FISGADO, ELE E LAMINA. `thrash` poe a cauda na tela; por isso o bordo de fuga
    da lamina caudal e claro e o resto dela e escuro e raiado. E o unico marcador
    que se move rapido, e e por ele que o jogador julga se o peixe ainda esta
    forte. Uma lamina inteira escura sumiria contra a agua justo no quadro em que o
    jogador precisa decidir.

(d) CANSADO, ELE E PERNA. Ele pousa no fundo sobre as quatro patas, e ai o que
    chega na tela sao as GARRAS: chifre claro contra quitina quase preta, com o vao
    entre os dedos aberto. E o quadro em que o bicho deixa de ser peixe -- e o
    quadro em que ele pode ser recolhido, entao ele precisa ser reconhecivel.

Regerar:  python art-source/enemies/master_of_the_swamp/master_of_the_swamp_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/master_of_the_swamp/adulto.png
"""
from PIL import Image
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from master_of_the_swamp_geo import CAIXAS, UV_LARGURA, UV_ALTURA  # noqa: E402  (fonte unica)

# --- a agua parada: o dorso, que e tudo que se ve antes de o bicho se mexer ---
LIMO = (78, 86, 54)           # flanco
LIMO_ESC = (46, 54, 34)       # dorso
LIMO_CLARO = (108, 114, 74)   # onde o flanco vira ventre
BARRO = (92, 76, 48)          # as manchas: fundo de pantano, nao pele de peixe

# --- o claro de baixo: barriga, garganta, papada ---------------------------
VENTRE = (176, 172, 138)
VENTRE_SOMBRA = (138, 134, 106)

# --- a boca: o unico branco grande da folha --------------------------------
DENTE = (240, 235, 216)
DENTE_RAIZ = (178, 170, 142)  # onde ele some na gengiva
DENTE_VAO = (38, 36, 28)      # o vao entre os dentes: sem ele e uma barra branca
GOELA = (124, 58, 58)
GOELA_ESC = (76, 34, 36)

# --- a cara ----------------------------------------------------------------
OLHO = (228, 208, 110)
OLHO_BRILHO = (252, 242, 194)
PUPILA = (16, 14, 12)
BARBILHAO = (164, 152, 118)   # os fios que varrem o fundo atras da isca

# --- as nadadeiras: membrana e raio ----------------------------------------
MEMBRANA = (126, 130, 90)
RAIO = (58, 62, 42)
BORDO = (198, 196, 164)       # o bordo de fuga da lamina caudal

# --- o que nao e peixe: perna, garra ---------------------------------------
QUITINA = (54, 48, 36)
QUITINA_CLARA = (88, 78, 56)
GARRA = (198, 184, 148)       # chifre claro contra a quitina escura
GARRA_VAO = (32, 28, 22)

img = Image.new("RGBA", (UV_LARGURA, UV_ALTURA), (0, 0, 0, 0))
px = img.load()

# Cor ANTES do ruido, por pixel. Existe para valida_faces_horizontais_sem_frente
# poder comparar fiada com fiada: o ruido e deterministico mas nao e espelhado, e
# sem este registro a validacao acusaria toda face mosqueada.
cor_base = {}

FACES_HORIZONTAIS = ("topo", "base")


def ruido(x, y, forca):
    """Variacao deterministica: cor chapada le como plastico.

    O hash e embaralhado de proposito. Combinacao linear simples (x*7+y*13) produz
    FAIXAS DIAGONAIS regulares, que a distancia leem como listra e nao como pele
    molhada -- e isso so aparece na tela.
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

    # -- pintura crua -------------------------------------------------------

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
                cor_base[(i, j)] = cor
                px[i, j] = (max(0, min(255, cor[0] + d)),
                            max(0, min(255, cor[1] + d)),
                            max(0, min(255, cor[2] + d)), 255)

    # -- pintura em coordenada da FACE --------------------------------------
    # Tudo abaixo fala em (coluna, fiada) DENTRO da face. E o que impede o erro que
    # nao da erro: um olho pintado 2 px fora cai na face vizinha e o portao de
    # sobreposicao nem pisca, porque continua dentro da mesma caixa.

    def na_face(self, nome, dx, dy, w, h, cor, forca=4):
        x, y, fw, fh = self.faces()[nome]
        if dx < 0 or dy < 0 or dx + w > fw or dy + h > fh:
            raise ValueError("caixa '%s', face '%s': (%d,%d)+%dx%d nao cabe em %dx%d"
                             % (self.c.nome, nome, dx, dy, w, h, fw, fh))
        self.retangulo(x + dx, y + dy, w, h, cor, forca)

    def face(self, nome, cor, forca=6):
        x, y, w, h = self.faces()[nome]
        self.retangulo(x, y, w, h, cor, forca)

    def tudo(self, cor, forca=6):
        for nome in self.faces():
            self.face(nome, cor, forca)

    def coluna(self, nome, dx, cor, forca=3):
        """Uma coluna inteira. Nas faces de cima e de baixo a coluna segue o eixo X,
        que o layout de caixa fixa -- por isso ela e segura ali, e a FIADA nao e."""
        _, _, _, h = self.faces()[nome]
        self.na_face(nome, dx, 0, 1, h, cor, forca)

    def linha(self, nome, dy, cor, forca=3):
        """Uma fiada horizontal contada a partir do TOPO da face.

        So para as quatro paredes: nelas a fiada segue o eixo Y, que e inequivoco.
        """
        self._recusa_horizontal(nome, "linha")
        _, _, w, _ = self.faces()[nome]
        self.na_face(nome, 0, dy, w, 1, cor, forca)

    def fiada_espelhada(self, nome, dy, cor, forca=3):
        """A fiada dy E a fiada espelhada dela, sempre as duas.

        E o unico jeito de uma FIADA existir nas faces de cima e de baixo sem
        depender do palpite de qual ponta e a frente: pintando o par, a marca fica
        igual nas duas leituras possiveis. Os raios das nadadeiras usam isto -- raio
        so de um lado seria um detalhe que TALVEZ esta certo.
        """
        _, _, w, h = self.faces()[nome]
        for fiada in (dy, h - 1 - dy):
            self.na_face(nome, 0, fiada, w, 1, cor, forca)

    def faixa_no_topo(self, nome, linhas, cor, forca=4):
        self._recusa_horizontal(nome, "faixa_no_topo")
        _, _, w, h = self.faces()[nome]
        self.na_face(nome, 0, 0, w, min(linhas, h), cor, forca)

    def faixa_no_pe(self, nome, linhas, cor, forca=4):
        self._recusa_horizontal(nome, "faixa_no_pe")
        _, _, w, h = self.faces()[nome]
        n = min(linhas, h)
        self.na_face(nome, 0, h - n, w, n, cor, forca)

    def ponto(self, nome, dx, dy, cor):
        self.na_face(nome, dx, dy, 1, 1, cor, 0)

    def _recusa_horizontal(self, nome, metodo):
        if nome in FACES_HORIZONTAIS:
            raise ValueError("'%s' foi chamado na face '%s' da caixa '%s': em cima e embaixo, "
                             "'primeira fiada' quer dizer 'a frente OU a tras', e ninguem sabe "
                             "qual. Use face/coluna/fiada_espelhada."
                             % (metodo, nome, self.c.nome))

    def salpicar(self, nome, cor, limiar, semente, forca=3):
        """Borroes de 1 px espalhados pela face, deterministicos.

        Mosqueado nao e enfeite: e o que faz o dorso ler como couro molhado e barro,
        e nao como um bloco pintado. A distancia, cor chapada denuncia um modelo.

        Nas faces de cima e de baixo o sorteio e ESPELHADO em profundidade, para o
        mosqueado nao virar, sozinho, a marca que distingue frente de tras.
        """
        x, y, w, h = self.faces()[nome]
        horizontal = nome in FACES_HORIZONTAIS
        for i in range(x, x + w):
            for j in range(y, y + h):
                jj = min(j - y, y + h - 1 - j) + y if horizontal else j
                if _sorteio(i, jj, semente) < limiar:
                    self.retangulo(i, j, 1, 1, cor, forca)


pinceis = {c.nome: Pincel(c) for c in CAIXAS}

LADOS = ("direita", "esquerda")
PAREDES = ("frente", "tras", "direita", "esquerda")


def agua_parada(p, semente, dorsal=LIMO_ESC, flanco=LIMO, ventral=VENTRE):
    """Escuro em cima, claro embaixo -- e o esquema que o corpo inteiro segue.

    Nao e estilo: e contra-sombreamento de peixe. De cima ele e visto contra o fundo
    escuro do pantano; quando rola em `thrash`, e visto de baixo contra a superficie
    clara. As duas leituras precisam funcionar, e sao leituras opostas.
    """
    p.tudo(flanco)
    p.face("topo", dorsal)
    p.face("base", ventral)
    for parede in PAREDES:
        p.faixa_no_topo(parede, 2, dorsal)
        p.faixa_no_pe(parede, 2, ventral)
    p.salpicar("topo", BARRO, 58, semente)
    p.salpicar("topo", LIMO_ESC, 70, semente + 3)
    for lado in LADOS:
        p.salpicar(lado, LIMO_CLARO, 40, semente + 7)


# --- o tronco: o dorso, que e tudo que se ve da margem --------------------
tronco = pinceis["trunk"]
agua_parada(tronco, 3)
# A LINHA LATERAL, orgao de peixe e marcador de leitura: uma fiada clara no meio do
# flanco, na PAREDE (onde a fiada e inequivoca). Ela e o que da comprimento ao bicho
# visto de lado -- sem ela, um corpo de 14 px de altura le como bloco.
for lado in LADOS:
    tronco.linha(lado, 6, LIMO_CLARO, 2)
    tronco.linha(lado, 7, LIMO_ESC, 2)
# A espinha, em coluna (eixo X), correndo do cranio a garupa.
for dx in (5, 6):
    tronco.coluna("topo", dx, LIMO_ESC, 2)

# --- a barriga: pende entre as pernas ------------------------------------
barriga = pinceis["belly"]
barriga.tudo(VENTRE_SOMBRA, 4)
barriga.face("base", VENTRE, 4)              # o que se ve quando ele rola
barriga.face("topo", LIMO_ESC, 3)            # enterrada no tronco
for parede in PAREDES:
    barriga.faixa_no_pe(parede, 1, VENTRE, 3)

# --- a crista dorsal: o que corta a agua ---------------------------------
# Ela e a unica linha continua do dorso, e existe para o bicho poder ser SEGUIDO
# quando se mexe. Escura nos lados, com o bordo claro em cima -- em coluna, que e o
# que o layout garante.
crista = pinceis["dorsal"]
crista.tudo(LIMO_ESC, 4)
crista.face("base", LIMO_ESC, 3)             # a raiz, no dorso
crista.face("topo", LIMO_ESC, 3)
for dx in (1, 2):
    crista.coluna("topo", dx, BORDO, 2)
for lado in LADOS:
    crista.faixa_no_topo(lado, 1, BORDO, 2)
    crista.salpicar(lado, BARRO, 46, 9)

# --- o cranio: uma tampa sobre uma armadilha -----------------------------
cranio = pinceis["skull"]
agua_parada(cranio, 11)
cranio.face("base", GOELA_ESC, 3)            # o ceu da boca, visto com ela aberta
cranio.face("tras", LIMO, 4)                 # onde ele entra no tronco
for lado in LADOS:
    cranio.faixa_no_pe(lado, 2, VENTRE_SOMBRA, 3)   # a bochecha, descendo para a boca
# OS OLHOS. Pequenos, altos e amarelos: o unico ponto saturado da folha inteira. Eles
# nao servem para o bicho ser bonito -- servem para o jogador saber que foi NOTADO,
# que e o momento em que a isca deixa de funcionar.
# Na face 'direita' a FRENTE fica na borda direita; na 'esquerda', na borda esquerda
# -- por isso as duas colunas sao espelhadas, e nao repetidas. Copiar a mesma coluna
# nos dois lados nao da erro: da um bicho vesgo de um lado so, e ninguem descobre
# isso sem girar a camera em volta dele. As medidas saem da PROFUNDIDADE da face, e
# nao de numeros escritos a mao: o cranio ja tem 12 px e pode encolher.
for lado in LADOS:
    _, _, fundura, _ = cranio.faces()[lado]
    if lado == "direita":
        olho, brilho, pupila = fundura - 4, fundura - 4, fundura - 3
    else:
        olho, brilho, pupila = 2, 3, 2
    cranio.na_face(lado, olho, 1, 2, 2, OLHO, 2)
    cranio.ponto(lado, brilho, 1, OLHO_BRILHO)
    cranio.ponto(lado, pupila, 2, PUPILA)

# --- a fileira de cima: o branco que faz a cara ler ----------------------
# O vao escuro entre os dentes e o que separa "quatro dentes" de "uma barra branca".
# Ele mora em COLUNA, inclusive na face de baixo -- que e a face que aparece quando a
# boca abre, e coluna segue o eixo X, que o layout fixa.
cima = pinceis["teeth_upper"]
cima.tudo(DENTE, 3)
cima.face("topo", DENTE_RAIZ, 3)             # a gengiva, enterrada no cranio
cima.face("tras", GOELA_ESC, 3)              # a goela, atras da fileira
for face in ("frente", "base"):
    for dx in (1, 3, 5, 7):
        cima.coluna(face, dx, DENTE_VAO, 2)
for lado in LADOS:
    cima.faixa_no_topo(lado, 1, DENTE_RAIZ, 2)

# --- a mandibula: a goela mora na face de cima dela ----------------------
queixo = pinceis["jaw"]
queixo.tudo(VENTRE_SOMBRA, 4)
queixo.face("base", VENTRE, 4)               # a papada, vista de baixo
queixo.face("frente", VENTRE, 3)             # o queixo
queixo.face("tras", GOELA_ESC, 3)
# A face de CIMA e o chao da boca. Ela nao aparece em quadro nenhum do `swim` e
# aparece inteira no `bite`. A lingua vai em coluna (eixo X): nada aqui distingue
# frente de tras, porque nada aqui PODE distinguir.
queixo.face("topo", GOELA, 4)
for dx in (3, 4):
    queixo.coluna("topo", dx, GOELA_ESC, 3)
for lado in LADOS:
    queixo.faixa_no_topo(lado, 1, LIMO, 3)   # o labio, onde o couro encontra a boca
    queixo.salpicar(lado, VENTRE, 36, 13)

# --- a fileira de baixo --------------------------------------------------
baixo = pinceis["teeth_lower"]
baixo.tudo(DENTE, 3)
baixo.face("base", DENTE_RAIZ, 3)            # a gengiva, enterrada no queixo
baixo.face("tras", GOELA_ESC, 3)
for face in ("frente", "topo"):
    for dx in (1, 3, 5):
        baixo.coluna(face, dx, DENTE_VAO, 2)
for lado in LADOS:
    baixo.faixa_no_pe(lado, 1, DENTE_RAIZ, 2)

# --- as presas: o contorno da boca FECHADA -------------------------------
# Elas sao o unico dente que aparece sem o bicho abrir a boca. A raiz escurece para a
# presa nao ler como um palito colado no rosto.
for nome in ("fang_upper_left", "fang_upper_right"):
    presa = pinceis[nome]
    presa.tudo(DENTE, 3)
    presa.face("topo", DENTE_RAIZ, 3)        # a raiz, enfiada na maxila
    presa.face("base", DENTE, 3)             # a ponta, vista de baixo
    for parede in PAREDES:
        presa.faixa_no_topo(parede, 1, DENTE_RAIZ, 2)

for nome in ("fang_lower_left", "fang_lower_right"):
    presa = pinceis[nome]
    presa.tudo(DENTE, 3)
    presa.face("base", DENTE_RAIZ, 3)        # a raiz, enfiada na mandibula
    presa.face("topo", DENTE, 3)             # a ponta, que sobe pela bochecha
    for parede in PAREDES:
        presa.faixa_no_pe(parede, 1, DENTE_RAIZ, 2)

# --- os barbilhoes: os fios que acham a isca -----------------------------
for nome in ("barbel_left", "barbel_right"):
    fio = pinceis[nome]
    fio.tudo(BARBILHAO, 3)
    fio.face("topo", VENTRE_SOMBRA, 2)       # onde ele sai do queixo
    fio.face("base", BARBILHAO, 2)
    for parede in PAREDES:
        fio.faixa_no_pe(parede, 2, VENTRE, 2)   # a ponta, mais clara

# --- as peitorais: superficie, nao volume --------------------------------
# Os raios vao em FIADA ESPELHADA: numa nadadeira eles correm para fora, ao longo do
# eixo X, e fiada nas faces de cima e de baixo e exatamente o que nao se pode
# adivinhar. Pintando o par, a marca fica igual nas duas leituras possiveis.
for nome in ("fin_left", "fin_right"):
    nadadeira = pinceis[nome]
    nadadeira.tudo(MEMBRANA, 4)
    for face in ("topo", "base"):
        nadadeira.face(face, MEMBRANA, 4)
        nadadeira.fiada_espelhada(face, 1, RAIO, 2)
    for parede in PAREDES:
        nadadeira.faixa_no_topo(parede, 1, RAIO, 2)      # o bordo, de cima
    nadadeira.face("direita", MEMBRANA, 3)
    nadadeira.face("esquerda", MEMBRANA, 3)

# --- a cauda -------------------------------------------------------------
rabo = pinceis["tail"]
agua_parada(rabo, 17)
for lado in LADOS:
    rabo.linha(lado, 4, LIMO_CLARO, 2)       # a linha lateral continua ate aqui

# --- a lamina caudal: o que se ve quando ele briga -----------------------
# Escura e raiada, com o BORDO DE FUGA claro. O bordo e a coisa que se move mais
# rapido na tela inteira durante o cabo de guerra, e e por ele que o jogador julga se
# o peixe ainda tem forca. Uma lamina toda escura sumiria contra a agua justo ai.
lamina = pinceis["tail_fin"]
lamina.tudo(MEMBRANA, 4)
lamina.face("topo", RAIO, 3)
lamina.face("base", RAIO, 3)
lamina.face("frente", MEMBRANA, 3)           # a raiz, onde ela entra na cauda
lamina.face("tras", BORDO, 3)                # o bordo de fuga, visto de tras
for lado in LADOS:
    _, _, fundura, altura = lamina.faces()[lado]
    lamina.face(lado, MEMBRANA, 4)
    for dy in range(0, altura, 3):
        lamina.linha(lado, dy, RAIO, 2)      # os raios da nadadeira
    # O bordo de fuga fica na borda ESQUERDA da face 'direita' e na direita da
    # 'esquerda' -- a frente e que e fixa pelo layout, entao o fundo sai dela.
    borda = 0 if lado == "direita" else fundura - 1
    lamina.coluna(lado, borda, BORDO, 2)

# --- as pernas: quitina escura, o fundo contra o qual a garra aparece ----
for nome in ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"):
    perna = pinceis[nome]
    perna.tudo(QUITINA, 4)
    perna.face("topo", QUITINA_CLARA, 3)     # a raiz, enterrada no tronco
    perna.face("base", QUITINA, 3)
    for parede in PAREDES:
        perna.faixa_no_topo(parede, 1, VENTRE_SOMBRA, 3)   # onde ela sai da barriga
        perna.linha(parede, 3, QUITINA_CLARA, 2)           # a articulacao do meio

# --- as garras: o quadro do bicho cansado --------------------------------
# Chifre claro sobre quitina quase preta, com o vao entre os dedos aberto em COLUNA
# (eixo X) -- tanto na sola quanto em cima, onde o layout garante a orientacao.
for nome in ("claw_front_left", "claw_front_right", "claw_back_left", "claw_back_right"):
    garra = pinceis[nome]
    garra.tudo(GARRA, 3)
    for face in ("topo", "base"):
        garra.face(face, GARRA, 3)
        garra.coluna(face, 1, GARRA_VAO, 2)  # o vao entre os dois dedos
    garra.face("frente", GARRA, 3)
    garra.coluna("frente", 1, GARRA_VAO, 2)
    garra.face("tras", QUITINA, 3)           # atras ela ainda e perna
    for lado in LADOS:
        garra.face(lado, GARRA, 3)
        garra.faixa_no_topo(lado, 1, QUITINA, 2)


def valida_sem_buraco():
    """Nenhum pixel transparente DENTRO do retangulo de uma caixa.

    Face esquecida nao da erro e nao aparece no atlas aberto no editor -- aparece
    como um buraco por onde se ve o interior do bicho, e so de um angulo. E a falha
    mais barata de cometer aqui: basta pintar cinco faces de seis.
    """
    for c in CAIXAS:
        p = pinceis[c.nome]
        for nome, (x, y, w, h) in p.faces().items():
            for i in range(x, x + w):
                for j in range(y, y + h):
                    if px[i, j][3] != 255:
                        raise ValueError("a face '%s' da caixa '%s' ficou sem tinta em (%d,%d): "
                                         "em jogo isso e um buraco no bicho, visto de um angulo so"
                                         % (nome, c.nome, i, j))


def valida_faces_horizontais_sem_frente():
    """Nada em cima nem embaixo pode distinguir frente de tras.

    Ver o cabecalho: fora do jogo nao da para saber qual fiada dessas faces cai na
    frente do bicho. Uma marca que dependa disso e um detalhe que TALVEZ esta certo
    -- e como ela nao levanta excecao, ninguem descobre que estava errada ate alguem
    olhar o mob do angulo exato. Neste mob o angulo exato e o mais comum que existe:
    de cima, atraves da agua.

    A comparacao e feita na COR-BASE, antes do ruido: o ruido e deterministico mas
    nao e espelhado, e compara-lo aqui acusaria toda face mosqueada.
    """
    for c in CAIXAS:
        p = pinceis[c.nome]
        for nome in FACES_HORIZONTAIS:
            x, y, w, h = p.faces()[nome]
            for i in range(x, x + w):
                for j in range(y, y + h):
                    reflexo = y + (h - 1) - (j - y)
                    if cor_base[(i, j)] != cor_base[(i, reflexo)]:
                        raise ValueError(
                            "caixa '%s', face '%s': a fiada %d e a %d tem cores diferentes em "
                            "x=%d. Isso faz a face de cima (ou de baixo) apontar para um lado do "
                            "bicho, e qual lado e exatamente o que nao da para conferir fora do "
                            "jogo." % (c.nome, nome, j - y, reflexo - y, i))


def valida_dente_e_o_mais_claro():
    """O dente tem de ser o branco da folha -- e nao 'um dos claros'.

    A boca e a mecanica inteira deste mob. Se a barriga, a garra ou o bordo da cauda
    subirem ate o valor do dente, a cara para de saltar e ninguem percebe: a folha
    continua bonita, e o bicho passa a ler como peixe grande.
    """
    def brilho(cor):
        return (cor[0] * 299 + cor[1] * 587 + cor[2] * 114) // 1000

    limite = brilho(DENTE)
    for nome, cor in (("VENTRE", VENTRE), ("GARRA", GARRA), ("BORDO", BORDO),
                      ("OLHO", OLHO), ("BARBILHAO", BARBILHAO), ("MEMBRANA", MEMBRANA)):
        if brilho(cor) >= limite - 20:
            raise ValueError("a cor %s (brilho %d) chegou perto do DENTE (%d): a boca deixa de "
                             "ser a coisa que salta na cara do bicho"
                             % (nome, brilho(cor), limite))


valida_sem_buraco()
valida_faces_horizontais_sem_frente()
valida_dente_e_o_mais_claro()

destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "textures", "entity", "master_of_the_swamp", "adulto.png")
os.makedirs(os.path.dirname(destino), exist_ok=True)
img.save(destino)
print("escrito", destino, img.size)
print("caixas pintadas:", len(pinceis))
print("pixels com tinta: %d de %d" % (len(cor_base), UV_LARGURA * UV_ALTURA))
