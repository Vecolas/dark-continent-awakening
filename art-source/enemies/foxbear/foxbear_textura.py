"""Gera a textura autoral do Foxbear (128 x 64), casada com foxbear.geo.json.

POR QUE ESTE ARQUIVO EXISTE. Mesma razao do gerador de geometria (ADR-017 e
ADR-007): a geometria do URSO POLAR sai, e o binario que entra precisa ter gerador
no git -- PNG sem gerador e um arquivo que ninguem consegue corrigir depois.

A TABELA DE CAIXAS NAO MORA AQUI. Ela e importada de foxbear_geo.CAIXAS, de
proposito: se cada gerador tivesse a sua, os dois divergiriam na primeira correcao
de modelo e o sintoma nao seria erro nenhum -- seria uma face pintada no lugar
errado, que so aparece na tela.

A CLASSE `Pincel` E PARECIDA COM A DOS QUATRO MOBS IRMAOS, E ISSO E DELIBERADO.
Ela nao e conhecimento compartilhado: e o mesmo layout de caixa do Bedrock
reescrito para esta paleta. A unica coisa que NAO pode ser duplicada -- a tabela
de caixas -- e importada.

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
cai atras nao da para conferir fora do jogo: o layout acima fixa as colunas (elas
seguem o eixo X, e a frente esta ao lado da face frontal), mas nao as fiadas. Os
quatro mobs irmaos assumiram uma orientacao; ninguem verificou nenhuma delas, e um
palpite errado ali nao levanta excecao -- poe o nariz na nuca e a garra virada
para dentro, e so aparece para quem estiver olhando o bicho do angulo certo.

Entao aqui NADA nas faces de cima e de baixo distingue frente de tras. O nariz
mora inteiro na face FRONTAL, a ponta da garra tambem, a lingua e a sola sao
uniformes, e a listra do dorso corre ao longo do bicho -- padrao de COLUNA, que o
layout garante. `valida_faces_horizontais_sem_frente` reprova qualquer marca que
volte a depender do palpite: ela compara a cor-base de cada fiada com a da fiada
espelhada e exige que sejam iguais. Nao e cosmetico: e a diferenca entre um
detalhe que esta certo e um detalhe que TALVEZ esteja certo.

LEITURA A DISTANCIA -- e para isso que a paleta existe. O territorio deste bicho
tem 12 blocos de raio e o aviso dura 80 ticks: cada item abaixo e uma coisa que o
jogador precisa entender enquanto ainda pode dar meia-volta.

(a) DE LONGE ELE E UM URSO. Dorso escuro e mosqueado, sem saturacao: parado na
    floresta ele e uma massa marrom. Nao ha nada colorido nas costas de proposito
    -- o que o jogador precisa achar a 12 blocos e a CABECA, porque e a cabeca que
    diz para que lado ele esta olhando, e olhar e o comeco do aviso.

(b) DE PERTO ELE E UMA RAPOSA, E ISSO INCOMODA. A cabeca inteira e ruiva, clara
    contra o corpo, com mascara creme, orelhas de ponta preta e nariz preto grande
    na face frontal. O contraste entre cabeca acesa e corpo apagado e o que faz o
    bicho ler como hibrido, e nao como urso marrom.

(c) ERGUIDO, ELE E PEITO E GARRA. O clipe `rear_warn` gira o tronco: o que chega
    na tela deixa de ser o dorso e passa a ser o peito e a barriga. Por isso o
    babador do peito e a coisa mais CLARA da folha -- ele so existe para esse
    quadro -- e por isso as garras sao cor de chifre claro contra a pata escura,
    com a PONTA preta: erguidas na altura do peito creme, e a ponta preta que
    aparece. Uma garra preta inteira sumiria na pata; uma garra clara inteira
    sumiria no babador. Ela precisa das duas cores porque e vista sobre os dois
    fundos.

(d) A BOCA SO EXISTE ABERTA. A face de CIMA da mandibula e vermelha: ela nao
    aparece em nenhum quadro do `idle`, e aparece inteira no `bite`. Pintar a
    mandibula toda de creme nao daria erro nenhum -- daria um bote sem boca.

Regerar:  python art-source/enemies/foxbear/foxbear_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/foxbear/adulto.png
"""
from PIL import Image
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from foxbear_geo import CAIXAS, UV_LARGURA, UV_ALTURA  # noqa: E402  (fonte unica)

# --- o urso: o corpo, que a 12 blocos e so uma massa na mata ---------------
URSO = (84, 58, 40)
URSO_ESC = (56, 38, 26)
FLANCO = (112, 76, 48)

# --- a raposa: a cabeca, o unico lugar aceso do bicho ----------------------
RUIVO = (168, 92, 38)
RUIVO_ESC = (122, 64, 26)
MASCARA = (58, 38, 26)
ORELHA_INT = (206, 158, 118)
PONTA_ORELHA = (40, 30, 24)

# --- o claro: babador, garganta, ventre e ponta da cauda -------------------
# Tudo que o jogador ve quando o bicho se ergue mora nesta faixa.
CREME = (224, 210, 184)
CREME_SOMBRA = (176, 160, 134)

# --- as meias: perna e pata, escuras, para a garra ter contra o que brilhar -
MEIA = (44, 32, 26)
MEIA_CLARA = (70, 52, 40)
SOLA = (64, 48, 40)
COXIM = (30, 26, 24)

# --- a arma ---------------------------------------------------------------
GARRA = (216, 205, 182)       # chifre claro: e o que se ve contra a pata escura
GARRA_PONTA = (26, 22, 20)    # a ponta: e o que se ve contra o peito creme
NARIZ = (24, 20, 18)
BOCA = (126, 62, 60)          # so aparece no bote
BOCA_ESC = (88, 42, 42)
DENTE = (238, 232, 216)

# --- a cara ---------------------------------------------------------------
OLHO = (232, 168, 56)
OLHO_BRILHO = (252, 228, 164)
PUPILA = (14, 12, 10)

img = Image.new("RGBA", (UV_LARGURA, UV_ALTURA), (0, 0, 0, 0))
px = img.load()

# Cor ANTES do ruido, por pixel. Existe para valida_faces_horizontais_sem_frente
# poder comparar fiada com fiada: o ruido e deterministico mas nao e espelhado, e
# sem este registro a validacao acusaria diferenca em toda face mosqueada.
cor_base = {}

FACES_HORIZONTAIS = ("topo", "base")


def ruido(x, y, forca):
    """Variacao deterministica: cor chapada le como plastico.

    O hash e embaralhado de proposito. Combinacao linear simples (x*7+y*13)
    produz FAIXAS DIAGONAIS regulares, que a distancia leem como listra e nao como
    pelo -- e isso so aparece na tela.
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
    # Tudo abaixo fala em (coluna, linha) DENTRO da face. E o que impede o erro
    # que nao da erro: um olho pintado 2 px fora cai na face vizinha e o portao de
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
                             "qual. Use face/coluna, que seguem o eixo X."
                             % (metodo, nome, self.c.nome))

    def salpicar(self, nome, cor, limiar, semente, forca=3):
        """Borroes de 1 px espalhados pela face, deterministicos.

        Mosqueado nao e enfeite: e o que faz o dorso ler como pelo e nao como um
        bloco pintado -- a distancia, cor chapada denuncia um modelo, nao um bicho.

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


def contraluz(p, semente, dorsal=URSO_ESC, flanco=FLANCO, ventral=CREME_SOMBRA):
    """Escuro em cima, claro embaixo -- e o esquema que o corpo inteiro segue.

    Nao e estilo: de quatro, o bicho e visto de CIMA contra o chao da mata; erguido
    no aviso, ele e visto de BAIXO e de FRENTE. Um ventre escuro apagaria justo o
    quadro em que ele precisa ser lido.
    """
    p.tudo(flanco)
    p.face("topo", dorsal)
    p.face("base", ventral)
    for lado in PAREDES:
        p.faixa_no_topo(lado, 2, dorsal)
        p.faixa_no_pe(lado, 2, ventral)
    p.salpicar("topo", URSO_ESC, 70, semente)
    for lado in LADOS:
        p.salpicar(lado, URSO_ESC, 44, semente + 7)


# --- o barril: o dorso, que e tudo que se ve de longe ----------------------
barril = pinceis["body"]
contraluz(barril, 3)
# A LISTRA DORSAL. Duas colunas escuras correndo do ombro a garupa: e ela que diz,
# de cima, para que lado o bicho esta apontado -- e a coluna segue o eixo X, que o
# layout fixa, entao ela nao depende de palpite nenhum.
for dx in (5, 6):
    barril.coluna("topo", dx, URSO_ESC, 2)
barril.salpicar("tras", URSO_ESC, 52, 5)

# --- a cernelha: o cupim de ombro e o BABADOR -----------------------------
cernelha = pinceis["chest"]
contraluz(cernelha, 11)
cernelha.salpicar("topo", URSO_ESC, 84, 13)      # o cupim, em sombra
cernelha.face("base", CREME, 4)                  # entre as dianteiras: visto de baixo
# O BABADOR. Ele existe para UM quadro: o do `rear_warn`, quando o peito vira a
# tela inteira. Em V, estreitando para baixo -- borda reta leria como babador de
# desenho animado, e este bicho nao e fofo.
cernelha.na_face("frente", 4, 1, 6, 5, CREME, 3)
cernelha.na_face("frente", 5, 6, 4, 2, CREME, 3)
cernelha.na_face("frente", 6, 8, 2, 2, CREME, 3)

# --- o pescoco: onde o urso vira raposa -----------------------------------
pescoco = pinceis["neck"]
pescoco.tudo(RUIVO_ESC, 5)
pescoco.face("topo", URSO, 4)                    # por cima ele ainda e dorso
pescoco.face("base", CREME, 4)                   # a garganta
pescoco.face("tras", URSO, 4)                    # onde ele entra no peito
for lado in PAREDES:
    pescoco.faixa_no_pe(lado, 2, CREME, 3)       # a garganta sobe pelos flancos

# --- a cabeca: o unico lugar aceso do bicho -------------------------------
cranio = pinceis["head"]
cranio.tudo(RUIVO, 5)
cranio.face("topo", RUIVO, 4)
cranio.salpicar("topo", MASCARA, 52, 17)
cranio.face("tras", RUIVO_ESC, 4)                # a nuca, contra o pescoco
cranio.face("base", CREME, 4)                    # o queixo, visto de baixo
cranio.faixa_no_pe("frente", 3, CREME, 3)        # a mascara clara da cara

# OS OLHOS. Ambar com pupila preta: o unico ponto saturado da folha inteira, e
# servem a uma coisa so -- quem esta sendo encarado precisa VER que esta sendo
# encarado, ou os 80 ticks de aviso passam sem ninguem entender que foram um aviso.
# Na face 'direita' a FRENTE fica na borda direita; na 'esquerda', na esquerda --
# por isso as duas colunas abaixo sao espelhadas em vez de repetidas. Copiar a
# mesma coluna nos dois lados nao da erro: da um bicho visivelmente vesgo por um
# lado so, e ninguem descobre isso sem girar a camera em volta dele.
for lado in LADOS:
    _, _, fundura, _ = cranio.faces()[lado]
    cranio.faixa_no_pe(lado, 2, CREME, 3)        # a bochecha clara
    # As colunas saem da PROFUNDIDADE da face, e nao de numeros escritos a mao: o
    # cranio ja mudou de tamanho uma vez nesta entrega, e numero cravado aqui teria
    # passado a pintar o olho na nuca sem nada acusar.
    if lado == "direita":
        inicio_do_olho, pupila, brilho = fundura - 3, fundura - 2, fundura - 3
        inicio_da_mascara = fundura - 6
    else:
        inicio_do_olho, pupila, brilho = 1, 1, 2
        inicio_da_mascara = fundura - 3
    cranio.na_face(lado, inicio_do_olho, 1, 2, 2, OLHO, 2)
    cranio.ponto(lado, brilho, 1, OLHO_BRILHO)
    cranio.ponto(lado, pupila, 2, PUPILA)
    cranio.na_face(lado, inicio_da_mascara, 3, 3, 1, MASCARA, 2)  # a risca ate a nuca

# --- o focinho: fino, claro, e o NARIZ na face frontal --------------------
focinho = pinceis["muzzle"]
focinho.tudo(CREME, 4)
focinho.face("topo", RUIVO, 4)                   # o cano do nariz, uniforme
focinho.face("base", CREME_SOMBRA, 3)
focinho.face("tras", RUIVO, 4)                   # onde ele entra no cranio
for lado in LADOS:
    focinho.faixa_no_topo(lado, 1, RUIVO, 3)     # o cano desce pelos lados
# O NARIZ, inteiro na face FRONTAL -- que e a unica face cuja orientacao o layout
# de caixa garante. Duas fiadas pretas e a de baixo creme: labio.
focinho.face("frente", CREME, 3)
focinho.na_face("frente", 0, 0, 3, 2, NARIZ, 2)

# --- a mandibula: a boca que so existe no bote ----------------------------
mandibula = pinceis["jaw"]
mandibula.tudo(CREME_SOMBRA, 4)
# A face de CIMA e a boca. Ela nao aparece em quadro nenhum do idle e aparece
# inteira no `bite`. Uniforme de proposito: nada aqui distingue frente de tras.
mandibula.face("topo", BOCA, 4)
for dx in (1, 2):
    mandibula.coluna("topo", dx, BOCA_ESC, 3)    # a lingua, ao longo do eixo X
mandibula.face("base", CREME_SOMBRA, 3)
mandibula.face("frente", CREME, 3)               # o queixo
mandibula.face("tras", BOCA_ESC, 3)              # o fundo da boca
for lado in LADOS:
    _, _, fundura, _ = mandibula.faces()[lado]
    borda_da_frente = fundura - 1 if lado == "direita" else 0
    mandibula.linha(lado, 0, MASCARA, 2)         # a linha do labio
    mandibula.ponto(lado, borda_da_frente, 0, DENTE)   # a presa, na ponta

# --- as orelhas: grandes, eretas, de ponta preta --------------------------
for nome in ("ear_left", "ear_right"):
    orelha = pinceis[nome]
    orelha.tudo(RUIVO_ESC, 4)
    orelha.face("frente", ORELHA_INT, 3)         # o interior, que aponta para a frente
    for dx in (0, 2):
        orelha.coluna("frente", dx, RUIVO_ESC, 3)   # a borda da concha
    orelha.face("tras", RUIVO, 4)                # o dorso da orelha
    orelha.face("base", RUIVO_ESC, 3)            # a raiz, no cranio
    # A PONTA PRETA. E ela que faz a orelha ter contorno contra o ceu e contra a
    # folhagem: sem isso, uma orelha ruiva sobre um fundo de floresta some.
    orelha.face("topo", PONTA_ORELHA, 2)
    for parede in PAREDES:
        orelha.faixa_no_topo(parede, 1, PONTA_ORELHA, 2)

# --- a cauda: escura na raiz, ruiva no meio, de ponta clara ---------------
# A cauda e a unica peca do bicho que atravessa a paleta inteira de cima a baixo.
# Nao e enfeite: e um marcador que se move. Na mata fechada, a 12 blocos, a ponta
# clara balancando e o que diz ONDE ele esta e para que lado esta voltado -- e
# saber isso e o que permite ao jogador nao entrar no territorio.
raiz = pinceis["tail"]
raiz.tudo(RUIVO, 5)
raiz.face("topo", URSO_ESC, 4)                   # a raiz, contra a garupa
raiz.face("base", RUIVO, 4)                      # por baixo ela segue na escova
for parede in PAREDES:
    raiz.faixa_no_topo(parede, 2, URSO_ESC, 4)   # o escuro desce da garupa
    raiz.salpicar(parede, RUIVO_ESC, 46, 23)     # densidade: pelo, nao cilindro

escova = pinceis["tail_brush"]
escova.tudo(RUIVO, 5)
escova.face("topo", RUIVO, 4)                    # onde ela entra na raiz
escova.face("base", CREME, 3)                    # a PONTA, vista de baixo
for parede in PAREDES:
    escova.faixa_no_pe(parede, 2, CREME, 3)      # a ponta clara
    escova.salpicar(parede, RUIVO_ESC, 40, 27)

# --- as pernas: meias escuras ---------------------------------------------
# Elas sao o fundo contra o qual a garra clara aparece. Perna da cor do corpo
# apagaria a garra, e a garra e o que o jogador precisa ler quando o bicho sobe.
for nome in ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"):
    perna = pinceis[nome]
    perna.tudo(MEIA, 5)
    perna.face("topo", MEIA_CLARA, 3)            # a raiz, enterrada no barril
    perna.face("base", MEIA, 3)
    for parede in PAREDES:
        perna.faixa_no_topo(parede, 2, FLANCO, 4)    # onde a meia comeca, no flanco
        perna.linha(parede, 2, MEIA_CLARA, 3)        # a borda da meia
        perna.salpicar(parede, MEIA_CLARA, 30, 29)

# --- as patas: largas, com sola -------------------------------------------
for nome in ("paw_front_left", "paw_front_right", "paw_back_left", "paw_back_right"):
    pata = pinceis[nome]
    pata.tudo(MEIA, 4)
    pata.face("topo", MEIA_CLARA, 3)             # enterrada na perna
    # A SOLA. Simetrica em profundidade de proposito: coxim central e quatro
    # dedos nos cantos, que leem igual de qualquer orientacao de fiada. As medidas
    # saem da FACE, e nao de numeros escritos a mao -- a pata traseira e 1 px mais
    # comprida que a dianteira, e um coxim de tamanho fixo sairia do centro nela.
    pata.face("base", SOLA, 3)
    _, _, larga, funda = pata.faces()["base"]
    pata.na_face("base", 1, 1, larga - 2, funda - 2, COXIM, 2)
    for dx, dy in ((0, 0), (larga - 1, 0), (0, funda - 1), (larga - 1, funda - 1)):
        pata.ponto("base", dx, dy, COXIM)
    for parede in PAREDES:
        pata.faixa_no_pe(parede, 1, SOLA, 3)

# --- as garras: tres unhas por pata, em uma caixa so ----------------------
# O volume e uma barra; quem faz as TRES unhas e a textura, nas colunas 0, 2 e 4 --
# e coluna, nas faces de cima e de baixo, segue o eixo X, que o layout fixa. A
# PONTA preta mora na face frontal, que tambem e inequivoca.
for nome in ("claw_front_left", "claw_front_right", "claw_back_left", "claw_back_right"):
    unhas = pinceis[nome]
    unhas.tudo(MEIA, 3)                          # o vao entre as unhas e pata
    for face in ("topo", "base"):
        unhas.face(face, MEIA, 3)
        for dx in (0, 2, 4):
            unhas.coluna(face, dx, GARRA, 2)
    unhas.face("frente", MEIA, 3)
    for dx in (0, 2, 4):
        unhas.coluna("frente", dx, GARRA_PONTA, 2)
    unhas.face("tras", MEIA, 3)
    for lado in LADOS:
        unhas.face(lado, GARRA, 2)


def valida_sem_buraco():
    """Nenhum pixel transparente DENTRO do retangulo de uma caixa.

    Face esquecida nao da erro e nao aparece no atlas aberto no editor -- aparece
    como um buraco por onde se ve o interior do bicho, e so de um angulo. E a
    falha mais barata de cometer aqui: basta pintar cinco faces de seis.
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
    frente do bicho. Uma marca que dependa disso e um detalhe que TALVEZ esteja
    certo -- e como ela nao levanta excecao, ninguem descobre que estava errada
    ate alguem olhar o mob do angulo exato.

    A comparacao e feita na COR-BASE, antes do ruido: o ruido e deterministico mas
    nao e espelhado, e compara-lo aqui acusaria toda face mosqueada.
    """
    for c in CAIXAS:
        p = pinceis[c.nome]
        for nome in FACES_HORIZONTAIS:
            x, y, w, h = p.faces()[nome]
            for i in range(x, x + w):
                for j in range(y, y + h):
                    espelho = y + (h - 1) - (j - y)
                    if cor_base[(i, j)] != cor_base[(i, espelho)]:
                        raise ValueError(
                            "caixa '%s', face '%s': a fiada %d e a %d tem cores diferentes em "
                            "x=%d. Isso faz a face de cima (ou de baixo) apontar para um lado do "
                            "bicho, e qual lado e exatamente o que nao da para conferir fora do "
                            "jogo." % (c.nome, nome, j - y, espelho - y, i))


valida_sem_buraco()
valida_faces_horizontais_sem_frente()

destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "textures", "entity", "foxbear", "adulto.png")
os.makedirs(os.path.dirname(destino), exist_ok=True)
img.save(destino)
print("escrito", destino, img.size)
print("caixas pintadas:", len(pinceis))
