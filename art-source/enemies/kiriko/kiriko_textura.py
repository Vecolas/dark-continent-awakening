"""Gera as DUAS texturas autorais do Kiriko (64 x 64 cada), casadas com os geo.

POR QUE ESTE ARQUIVO EXISTE. Mesma razao do gerador de geometria (ADR-017 e
ADR-007): o binario que entra no repositorio precisa ter gerador no git. PNG sem
gerador e um arquivo que ninguem consegue corrigir depois.

AS TABELAS DE CAIXAS NAO MORAM AQUI. Sao importadas de kiriko_geo, de proposito:
se cada gerador tivesse a sua, os dois divergiriam na primeira correcao de modelo,
e o sintoma nao seria erro nenhum -- seria uma face pintada no lugar errado, que
so aparece na tela. Sao DOIS conjuntos porque sao dois modelos, e os dois vem do
MESMO modulo.

A CLASSE `Pincel` E PARECIDA COM A DOS SEIS MOBS IRMAOS, E ISSO E DELIBERADO. Ela
nao e conhecimento compartilhado: e o mesmo layout de caixa do Bedrock reescrito
para esta paleta. A unica coisa que NAO pode ser duplicada -- a tabela de caixas --
e importada.

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
a fita do chapeu na nuca e a unha do pe virada para tras.

Entao nada nas faces de cima e de baixo distingue frente de tras, e a recusa e
MECANICA, nao disciplina: `Pincel.na_face` REPROVA uma pintura de fiada parcial em
'topo' ou 'base'. O que sobra nelas e cor cheia, coluna (eixo X, garantido),
mosqueado, ou `fiada_espelhada`, que pinta a fiada e a irma dela de uma vez --
substituindo o palpite por um par.

AS DUAS FOLHAS SAO UM MOB SO, E MESMO ASSIM NAO COMPARTILHAM UM UNICO TOM
-------------------------------------------------------------------------
Aqui esta a diferenca com o man-faced ape, que vale a pena ler junto. La a paleta
do ROSTO e uma so nas duas formas, porque a piada do mob e "a cara e a mesma, o
corpo e que esta errado". Aqui e o oposto: o viajante e uma FABRICACAO INTEIRA, e
nada nele pode remeter ao bicho. Nenhum tom desta folha humana aparece na folha do
Kiriko verdadeiro, e `valida_paletas_disjuntas` cobra isso.

O motivo nao e estetico. Se um tom vazasse -- o ambar do olho no olho humano, o
ocre do bico na fivela do cinto -- o jogador atento reconheceria o bicho ANTES da
transformacao. E jogador que reconhece o bicho saca arma, e quem saca arma REPROVA
no julgamento. Seria um jogador punido por ter olhado com atencao.

LEITURA A DISTANCIA -- e para isso que as paletas existem
---------------------------------------------------------
(a) O VIAJANTE E GENTE, E E APAGADO. Casaco de estrada em oliva sujo, calca, bota,
    chapeu de palha. Nada saturado, nada que chame atencao, nada que assuste.
    Diferente do macaco, o ROSTO ESTA A MOSTRA e olhando para a frente: o chapeu
    comeca ACIMA do cranio, e nao ha sombra nenhuma sobre a cara. E preciso que o
    jogador VEJA que estao olhando para ele -- e isso que transforma "um NPC
    parado" em "alguem te avaliando", e e a unica dica que o encontro da.

(b) A FORMA VERDADEIRA E UMA CRIATURA QUE PENSA. Plumagem azul-ardosia fosca,
    peito creme, bico e garras cor de chifre, crista ocre. Nada de vermelho de
    ferida, nada de dente, nada de olho vazio. O CENTRO DA FOLHA SAO OS OLHOS:
    ambar, com pupila redonda e um brilho de 1 px, um par de cada lado do bico, na
    altura em que um rosto tem olhos. Olho com pupila le como alguem; olho chapado
    le como bicho. E essa a diferenca entre "Magical Beast" e "monster", e ela cabe
    em quatro pixels.

(c) O PEITO CREME E O QUE LIGA AS DUAS LEITURAS. Ele sobe pela garganta ate
    debaixo do bico: de longe, o que se ve da forma verdadeira e uma mancha clara
    na altura do peito e dois pontos ambar na altura do rosto. Sao esses dois
    marcadores que dizem "isto tem frente, e a frente esta virada para voce".

Regerar:  python art-source/enemies/kiriko/kiriko_textura.py
Exporta:  .../textures/entity/kiriko/verdadeiro.png
          .../textures/entity/kiriko_disfarce/humano.png
"""
from PIL import Image
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from kiriko_geo import (  # noqa: E402  (fonte unica das caixas)
    CAIXAS_DISFARCE, CAIXAS_VERDADEIRO, UV_LARGURA, UV_ALTURA)

# ===========================================================================
# PALETA DO VIAJANTE -- nada aqui pode aparecer na folha do bicho
# ===========================================================================
PELE = (208, 170, 136)
PELE_SOMBRA = (162, 126, 98)
OLHO_BRANCO = (226, 220, 210)
PUPILA = (38, 30, 26)
BOCA = (120, 76, 66)
SOBRANCELHA = (74, 52, 38)
CABELO = (66, 46, 34)

PANO = (96, 88, 72)            # casaco de estrada, oliva sujo
PANO_CLARO = (120, 110, 90)    # o que a luz de cima pega
PANO_ESCURO = (66, 60, 50)
CALCA = (78, 70, 60)
BOTA = (52, 42, 34)
COURO = (94, 70, 46)           # cinto
FIVELA = (156, 136, 92)
PALHA = (186, 158, 104)        # o chapeu de viagem
PALHA_ESCURA = (140, 114, 72)
FITA = (88, 66, 48)            # a fita da copa

# ===========================================================================
# PALETA DO KIRIKO -- e ela nao encosta na de cima
# ===========================================================================
PENA = (62, 78, 96)            # azul-ardosia fosco
PENA_ESCURA = (38, 48, 62)
PENA_CLARA = (96, 114, 132)
PEITO = (196, 176, 132)        # peito e garganta: a mancha clara que se ve de longe
PEITO_SOMBRA = (154, 136, 100)
# Marfim FRIO, e nao ocre. A `valida_paletas_disjuntas` so pega tom IDENTICO, e um
# bico cor de palha ao lado de um chapeu de palha passaria por ela inteiro -- dois
# ocres vizinhos a 4 blocos sao o mesmo ocre, e o jogador atento ligaria as duas
# formas antes da transformacao. O marfim nao tem para onde ser confundido.
CHIFRE = (222, 206, 164)       # bico e garras
CHIFRE_ESCURO = (150, 134, 96)
CRISTA = (168, 78, 54)         # ocre queimado
CRISTA_ESCURA = (118, 52, 38)
OLHO_AMBAR = (232, 184, 86)
OLHO_BRILHO = (252, 234, 180)
PUPILA_BICHO = (22, 18, 16)
ESCAMA = (104, 92, 74)         # pes
ESCAMA_ESCURA = (70, 60, 48)

PALETA_HUMANA = (PELE, PELE_SOMBRA, OLHO_BRANCO, PUPILA, BOCA, SOBRANCELHA, CABELO,
                 PANO, PANO_CLARO, PANO_ESCURO, CALCA, BOTA, COURO, FIVELA,
                 PALHA, PALHA_ESCURA, FITA)
PALETA_BICHO = (PENA, PENA_ESCURA, PENA_CLARA, PEITO, PEITO_SOMBRA, CHIFRE,
                CHIFRE_ESCURO, CRISTA, CRISTA_ESCURA, OLHO_AMBAR, OLHO_BRILHO,
                PUPILA_BICHO, ESCAMA, ESCAMA_ESCURA)


def valida_paletas_disjuntas():
    """Nenhum tom em comum entre o viajante e o bicho -- ver o cabecalho.

    Duas constantes de cor iguais sao codigo perfeitamente valido: nada no
    repositorio veria isso. O que se veria e um jogador reconhecendo o Kiriko antes
    da transformacao, atacando, e sendo reprovado por ter olhado com atencao.
    """
    comuns = set(PALETA_HUMANA) & set(PALETA_BICHO)
    if comuns:
        raise ValueError("estes tons aparecem nas DUAS paletas: %s. O disfarce so funciona se nada "
                         "nele remeter ao bicho" % sorted(comuns))


def ruido(x, y, forca):
    """Variacao deterministica: cor chapada le como plastico.

    O hash e embaralhado de proposito. Combinacao linear simples (x*7+y*13) produz
    FAIXAS DIAGONAIS regulares, que a distancia leem como listra e nao como pano ou
    pena -- e isso so aparece na tela.
    """
    h = (x * 374761393 + y * 668265263) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((h >> 7) % 5 - 2) * forca // 2


def _sorteio(x, y, semente):
    """0..255 deterministico por pixel, para decidir onde cai um borrao."""
    h = (x * 2654435761 + y * 40503 + semente * 2246822519) & 0xFFFFFFFF
    h = (h ^ (h >> 15)) * 2246822519 & 0xFFFFFFFF
    return (h >> 13) & 0xFF


class Folha:
    """Uma das duas texturas. Existe porque este mob pinta DUAS."""

    def __init__(self, nome, destino):
        self.nome = nome
        self.destino = destino
        self.img = Image.new("RGBA", (UV_LARGURA, UV_ALTURA), (0, 0, 0, 0))
        self.px = self.img.load()
        self.pintado = set()

    def salvar(self):
        os.makedirs(os.path.dirname(self.destino), exist_ok=True)
        self.img.save(self.destino)
        return self.destino


class Pincel:
    """Pincel preso a uma caixa DE UMA folha. Pintar fora do retangulo e erro."""

    HORIZONTAIS = ("topo", "base")

    def __init__(self, folha, caixa):
        self.folha = folha
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
                self.folha.px[i, j] = (max(0, min(255, cor[0] + d)),
                                       max(0, min(255, cor[1] + d)),
                                       max(0, min(255, cor[2] + d)), 255)
                self.folha.pintado.add((i, j))

    # -- pintura em coordenada da FACE --------------------------------------
    # Tudo abaixo fala em (coluna, fiada) DENTRO da face. E o que impede o erro que
    # nao da erro: um olho pintado 2 px fora cai no vizinho e o portao de
    # sobreposicao nem pisca, porque continua dentro da mesma caixa.

    def na_face(self, nome, dx, dy, w, h, cor, forca=4):
        x, y, fw, fh = self.faces()[nome]
        if dx < 0 or dy < 0 or dx + w > fw or dy + h > fh:
            raise ValueError("caixa '%s', face '%s': (%d,%d)+%dx%d nao cabe em %dx%d"
                             % (self.c.nome, nome, dx, dy, w, h, fw, fh))
        if nome in self.HORIZONTAIS and (dy != 0 or h != fh):
            raise ValueError("caixa '%s': marca de fiada parcial na face '%s'. Em cima e embaixo, "
                             "qual fiada cai na FRENTE do bicho nao da para conferir fora do jogo "
                             "-- e o palpite errado poe a marca na nuca sem levantar erro. Use "
                             "cor cheia, coluna, mosqueado ou fiada_espelhada." % (self.c.nome, nome))
        self.retangulo(x + dx, y + dy, w, h, cor, forca)

    def face(self, nome, cor, forca=6):
        x, y, w, h = self.faces()[nome]
        self.retangulo(x, y, w, h, cor, forca)

    def tudo(self, cor, forca=6):
        for nome in self.faces():
            self.face(nome, cor, forca)

    def linha(self, nome, dy, cor, forca=3):
        """Uma fiada horizontal contada a partir do TOPO da face."""
        _, _, w, _ = self.faces()[nome]
        self.na_face(nome, 0, dy, w, 1, cor, forca)

    def faixa_no_topo(self, nome, fiadas, cor, forca=4):
        _, _, w, h = self.faces()[nome]
        self.na_face(nome, 0, 0, w, min(fiadas, h), cor, forca)

    def faixa_no_pe(self, nome, fiadas, cor, forca=4):
        _, _, w, h = self.faces()[nome]
        n = min(fiadas, h)
        self.na_face(nome, 0, h - n, w, n, cor, forca)

    def coluna(self, nome, dx, cor, forca=3):
        _, _, _, h = self.faces()[nome]
        self.na_face(nome, dx, 0, 1, h, cor, forca)

    def ponto(self, nome, dx, dy, cor):
        self.na_face(nome, dx, dy, 1, 1, cor, 0)

    def fiada_espelhada(self, nome, dy, cor, forca=3):
        """Pinta a fiada `dy` e a irma dela do outro lado da face, de uma vez.

        E o substituto do palpite nas faces de cima e de baixo: qualquer que seja a
        fiada que caia na frente do bicho, o desenho fica igual.
        """
        x, y, w, h = self.faces()[nome]
        if dy < 0 or dy >= h:
            raise ValueError("caixa '%s', face '%s': fiada %d fora de %d"
                             % (self.c.nome, nome, dy, h))
        self.retangulo(x, y + dy, w, 1, cor, forca)
        self.retangulo(x, y + (h - 1 - dy), w, 1, cor, forca)

    def borda(self, nome, cor, forca=3):
        """O contorno de 1 px da face -- simetrico em toda direcao, seguro em cima."""
        x, y, w, h = self.faces()[nome]
        self.retangulo(x, y, w, 1, cor, forca)
        self.retangulo(x, y + h - 1, w, 1, cor, forca)
        self.retangulo(x, y, 1, h, cor, forca)
        self.retangulo(x + w - 1, y, 1, h, cor, forca)

    def salpicar(self, nome, cor, limiar, semente, forca=3):
        """Borroes de 1 px espalhados pela face, deterministicos.

        Mosqueado nao e enfeite: e o que faz a pena ler como pena e o pano como pano
        gasto -- a distancia, cor chapada denuncia o modelo como plastico.
        """
        x, y, w, h = self.faces()[nome]
        for i in range(x, x + w):
            for j in range(y, y + h):
                if _sorteio(i, j, semente) < limiar:
                    self.retangulo(i, j, 1, 1, cor, forca)


def pinceis(folha, caixas):
    return {c.nome: Pincel(folha, c) for c in caixas}


def valida_faces_pintadas(folha, caixas):
    """Toda face de todo cubo esta pintada, e nada foi pintado fora de uma face.

    Face esquecida nao levanta erro: sai TRANSPARENTE em jogo, e o jogador ve um
    buraco no bicho -- que le como falha de video, e nao como face nao pintada.
    O canto morto do layout de caixa (o quadrado d x d no alto a esquerda) nao e
    face nenhuma: pintar la e desperdicio de atlas que ninguem nota.
    """
    de_face = set()
    for c in caixas:
        p = Pincel(folha, c)
        for nome, (x, y, w, h) in p.faces().items():
            for i in range(x, x + w):
                for j in range(y, y + h):
                    de_face.add((i, j))

    faltando = de_face - folha.pintado
    if faltando:
        culpados = set()
        for c in caixas:
            p = Pincel(folha, c)
            for nome, (x, y, w, h) in p.faces().items():
                if any((i, j) in faltando
                       for i in range(x, x + w) for j in range(y, y + h)):
                    culpados.add(c.nome + "." + nome)
        raise ValueError("%s: %d px de face nao foram pintados (%s): em jogo isso e um buraco "
                         "transparente no bicho" % (folha.nome, len(faltando), sorted(culpados)))

    sobrando = folha.pintado - de_face
    if sobrando:
        raise ValueError("%s: %d px pintados fora de qualquer face (canto morto do layout de "
                         "caixa): atlas gasto a toa" % (folha.nome, len(sobrando)))


# ===========================================================================
# FORMA HUMANA -- textures/entity/kiriko_disfarce/humano.png
# ===========================================================================

viajante = Folha("kiriko_disfarce",
                 os.path.join("src", "main", "resources", "assets", "nenfoundation",
                              "textures", "entity", "kiriko_disfarce", "humano.png"))
d = pinceis(viajante, CAIXAS_DISFARCE)

# --- o casaco de estrada --------------------------------------------------
casaco = d["body"]
casaco.tudo(PANO)
casaco.face("topo", PANO_CLARO, 4)          # ombro, onde a luz bate
casaco.face("base", PANO_ESCURO, 3)         # por baixo da barra
for lado in ("frente", "tras", "direita", "esquerda"):
    casaco.faixa_no_topo(lado, 1, PANO_CLARO)   # a gola
    casaco.faixa_no_pe(lado, 2, PANO_ESCURO)    # a barra, em sombra
    casaco.linha(lado, 5, COURO, 2)             # o cinto, na altura da cintura
    casaco.salpicar(lado, PANO_ESCURO, 32, 5)   # pano gasto, sem brilho
casaco.ponto("frente", 3, 5, FIVELA)            # a fivela: o unico ponto claro da roupa
casaco.na_face("frente", 3, 1, 1, 4, PANO_ESCURO, 2)   # a abertura do casaco

# --- a cabeca: E AQUI QUE O MOB INTEIRO ACONTECE --------------------------
# O rosto fica a MOSTRA, sem capuz e sem sombra. O jogador precisa ver que estao
# olhando para ele -- e so isso que transforma "um NPC parado na estrada" em
# "alguem te avaliando", e e a unica dica que o encontro da antes de decidir.
cabeca = d["head"]
cabeca.tudo(PELE)
cabeca.face("topo", CABELO, 3)
cabeca.face("tras", CABELO, 3)
for lado in ("direita", "esquerda"):
    cabeca.faixa_no_topo(lado, 3, CABELO)       # cabelo descendo pelas temporas
cabeca.faixa_no_topo("frente", 1, CABELO)       # a linha do cabelo
# testa
cabeca.na_face("frente", 1, 1, 6, 1, PELE, 3)
# sobrancelha corrida
cabeca.na_face("frente", 1, 2, 6, 1, SOBRANCELHA, 2)
# OS OLHOS. Sao a coisa que o jogador reconhece a 3 blocos, e eles olham de volta.
cabeca.na_face("frente", 1, 3, 2, 1, OLHO_BRANCO, 2)
cabeca.na_face("frente", 5, 3, 2, 1, OLHO_BRANCO, 2)
cabeca.ponto("frente", 2, 3, PUPILA)
cabeca.ponto("frente", 5, 3, PUPILA)
# nariz
cabeca.na_face("frente", 3, 4, 2, 1, PELE_SOMBRA, 2)
cabeca.ponto("frente", 3, 5, PELE_SOMBRA)
cabeca.ponto("frente", 4, 5, PELE_SOMBRA)
# boca e queixo
cabeca.na_face("frente", 2, 6, 4, 1, BOCA, 2)
cabeca.faixa_no_pe("frente", 1, PELE_SOMBRA, 2)

# --- o chapeu de viagem: aba e copa ---------------------------------------
# A aba comeca ACIMA do cranio (o geo cobra isso): ela nao faz sombra na cara.
aba = d["brim"]
aba.tudo(PALHA)
aba.face("base", PALHA_ESCURA, 3)               # por baixo, onde a luz nao chega
aba.borda("topo", PALHA_ESCURA)                 # a beirada gasta -- simetrica, ver o cabecalho
aba.borda("base", PALHA_ESCURA)
for lado in ("frente", "tras", "direita", "esquerda"):
    aba.face(lado, PALHA_ESCURA, 3)             # o corte da palha, visto de lado
aba.salpicar("topo", PALHA_ESCURA, 26, 7)       # a trama da palha

copa = d["crown"]
copa.tudo(PALHA)
copa.face("topo", PALHA, 4)
copa.face("base", PALHA_ESCURA, 3)
for lado in ("frente", "tras", "direita", "esquerda"):
    copa.faixa_no_pe(lado, 1, FITA)             # A FITA, rente a aba, dando a volta
    copa.salpicar(lado, PALHA_ESCURA, 26, 9)

# --- mangas ---------------------------------------------------------------
for nome in ("sleeve_left", "sleeve_right"):
    manga = d[nome]
    manga.tudo(PANO)
    manga.face("topo", PANO_CLARO, 4)
    manga.face("base", PANO_ESCURO, 2)
    for lado in ("frente", "tras", "direita", "esquerda"):
        manga.faixa_no_pe(lado, 1, PANO_ESCURO)   # o punho
        manga.salpicar(lado, PANO_ESCURO, 28, 11)

# --- as maos: mao de gente, do tamanho da manga --------------------------
# Sem pista nenhuma, de proposito -- ver `valida_disfarce_sem_pista` no geo.
for nome in ("hand_left", "hand_right"):
    mao = d[nome]
    mao.tudo(PELE, 4)
    mao.face("base", PELE_SOMBRA, 3)              # a palma
    for lado in ("frente", "tras", "direita", "esquerda"):
        mao.faixa_no_topo(lado, 1, PELE_SOMBRA, 2)   # os nos dos dedos
    mao.coluna("frente", 1, PELE_SOMBRA, 2)          # o vao entre os dedos

# --- pernas: calca e bota -------------------------------------------------
for nome in ("leg_left", "leg_right"):
    perna = d[nome]
    perna.tudo(CALCA)
    perna.face("topo", PANO_ESCURO, 2)            # por baixo do casaco
    perna.face("base", BOTA, 2)                   # a sola
    for lado in ("frente", "tras", "direita", "esquerda"):
        perna.faixa_no_pe(lado, 4, BOTA)          # o cano da bota
        perna.linha(lado, 8, COURO, 2)            # a correia da bota
        perna.salpicar(lado, PANO_ESCURO, 26, 13)

# ===========================================================================
# FORMA VERDADEIRA -- textures/entity/kiriko/verdadeiro.png
# ===========================================================================

kiriko = Folha("kiriko",
               os.path.join("src", "main", "resources", "assets", "nenfoundation",
                            "textures", "entity", "kiriko", "verdadeiro.png"))
k = pinceis(kiriko, CAIXAS_VERDADEIRO)

# --- o tronco: plumagem escura e O PEITO CLARO ---------------------------
# O peito creme e a mancha que se enxerga de longe. Ele diz "isto tem frente" --
# e, com os olhos, e o par de marcadores que faz o bicho parecer virado PARA voce.
torso = k["torso"]
torso.tudo(PENA)
torso.face("topo", PENA_ESCURA, 3)
torso.face("base", PENA_ESCURA, 3)
for lado in ("tras", "direita", "esquerda"):
    torso.faixa_no_topo(lado, 3, PENA_CLARA)         # a capa dos ombros
    torso.salpicar(lado, PENA_ESCURA, 36, 21)
# O BABADOR, e nao a frente inteira. Peito claro de borda a borda vira uma laje
# bege: de longe ela come o bico, o pescoco e os bracos, e sobra um vulto sem
# forma. Com moldura de pluma o claro vira MANCHA -- uma coisa com contorno, na
# altura do peito, que o olho encontra e reconhece.
torso.na_face("frente", 1, 2, 8, 9, PEITO, 4)
# As dobras da penugem AFUNILAM para baixo. Fiadas escuras de ponta a ponta leem
# como costela -- tres barras paralelas do mesmo comprimento viram uma escada, e
# escada num peito le como caixa toracica exposta, que e leitura de monstro.
torso.na_face("frente", 2, 4, 6, 1, PEITO_SOMBRA, 2)
torso.na_face("frente", 3, 7, 4, 1, PEITO_SOMBRA, 2)
torso.na_face("frente", 1, 9, 8, 2, PEITO_SOMBRA, 3)   # o ventre, mais fundo
torso.na_face("frente", 0, 0, 10, 2, PENA_ESCURA, 3)   # a clavicula, ainda emplumada

# --- o cranio: OS OLHOS SAO O MOB ----------------------------------------
# Face da frente: 8 colunas x 9 fiadas. O bico cobre as colunas 2..5; o que sobra
# de cada lado (0..1 e 6..7) e o espaco dos olhos, e o geo cobra que ele exista.
cranio = k["skull"]
cranio.tudo(PENA)
cranio.face("topo", PENA_ESCURA, 3)
for lado in ("direita", "esquerda", "tras"):
    cranio.salpicar(lado, PENA_ESCURA, 34, 25)
cranio.faixa_no_topo("frente", 2, PENA_ESCURA)       # testa e arco da sobrancelha
cranio.na_face("frente", 2, 2, 4, 4, PENA_ESCURA, 2)  # o vao atras do bico
# O PAR DE OLHOS. Ambar de 2x2, pupila redonda de 1 px e um brilho de 1 px. E este
# quadrado de quatro pixels que separa "Magical Beast" de "monster": olho chapado
# le como bicho, olho com pupila le como alguem que esta te olhando de volta.
for coluna, pupila, brilho in ((0, 1, 0), (6, 6, 7)):
    cranio.na_face("frente", coluna, 2, 2, 2, OLHO_AMBAR, 2)
    cranio.ponto("frente", pupila, 3, PUPILA_BICHO)
    cranio.ponto("frente", brilho, 2, OLHO_BRILHO)
cranio.na_face("frente", 0, 4, 2, 2, PENA, 3)        # a bochecha esquerda
cranio.na_face("frente", 6, 4, 2, 2, PENA, 3)        # a direita
# A FIADA ESCURA EMBAIXO DO BICO. Sem ela a garganta clara encosta no bico claro e
# o bico DESAPARECE de frente: sao dois ocres vizinhos, e o olho nao separa dois
# ocres vizinhos a 4 blocos. A peca mais importante da cara sumiria sem nada
# acusar -- o modelo continuaria certo, o atlas continuaria certo.
cranio.na_face("frente", 0, 6, 8, 1, PENA_ESCURA, 2)
cranio.faixa_no_pe("frente", 2, PEITO)               # A GARGANTA, que puxa o peito para cima
for lado in ("direita", "esquerda"):
    cranio.faixa_no_pe(lado, 2, PEITO_SOMBRA, 2)     # a garganta dobra a esquina

# --- o bico ---------------------------------------------------------------
bico = k["beak"]
bico.tudo(CHIFRE, 4)
bico.face("base", CHIFRE_ESCURO, 3)                  # a mandibula de baixo
bico.coluna("topo", 1, CHIFRE_ESCURO, 2)             # a quilha, por cima
bico.coluna("topo", 2, CHIFRE_ESCURO, 2)
for lado in ("direita", "esquerda"):
    bico.faixa_no_pe(lado, 1, CHIFRE_ESCURO, 2)      # A LINHA DA BOCA, de lado a lado
# A PONTA. De frente o bico e um quadrado de 4x4 -- chapado, ele le como focinho
# cortado. As colunas de fora em sombra estreitam o que se enxerga para 2 px, e o
# bico volta a AFUNILAR. E o unico jeito de um cubo ter ponta.
bico.face("frente", CHIFRE, 3)
bico.coluna("frente", 0, CHIFRE_ESCURO, 2)
bico.coluna("frente", 3, CHIFRE_ESCURO, 2)
bico.faixa_no_pe("frente", 1, CHIFRE_ESCURO, 2)      # a linha da boca fecha na ponta

# --- a crista -------------------------------------------------------------
crista = k["crest"]
crista.tudo(CRISTA, 4)
crista.face("base", CRISTA_ESCURA, 3)
for lado in ("direita", "esquerda"):
    crista.faixa_no_pe(lado, 1, CRISTA_ESCURA, 2)    # a raiz, onde ela sai do cranio
    crista.salpicar(lado, CRISTA_ESCURA, 30, 27)

# --- bracos ---------------------------------------------------------------
for nome in ("arm_left", "arm_right"):
    braco = k[nome]
    braco.tudo(PENA)
    braco.face("topo", PENA_CLARA, 3)                # a capa do ombro continua no braco
    for lado in ("frente", "tras", "direita", "esquerda"):
        braco.faixa_no_topo(lado, 3, PENA_CLARA)
        braco.faixa_no_pe(lado, 3, PENA_ESCURA)      # o antebraco escurece ate o pulso
        braco.salpicar(lado, PENA_ESCURA, 34, 29)

# --- garras: mao que PEGA coisas, e nao pata que apoia --------------------
for nome in ("claw_left", "claw_right"):
    garra = k[nome]
    garra.tudo(PENA)
    garra.face("topo", PENA_ESCURA, 3)
    garra.face("base", CHIFRE, 4)                    # a palma, de chifre claro
    # So as PONTAS sao claras. Mao clara inteira vira um bloco bege na altura do
    # joelho, e de longe le como 'o bicho esta segurando alguma coisa' -- que e
    # exatamente a leitura errada num mob que nao carrega arma nenhuma.
    for lado in ("frente", "tras", "direita", "esquerda"):
        garra.faixa_no_pe(lado, 2, CHIFRE)           # os dedos
        garra.faixa_no_topo(lado, 1, PENA_ESCURA, 2)
    for dedo in (1, 2, 3):
        garra.coluna("base", dedo, CHIFRE_ESCURO, 2)  # os vaos entre os dedos
    garra.na_face("frente", 1, 5, 1, 1, CHIFRE_ESCURO, 2)
    garra.na_face("frente", 2, 5, 1, 1, CHIFRE_ESCURO, 2)

# --- pernas ---------------------------------------------------------------
for nome in ("leg_left", "leg_right"):
    perna = k[nome]
    perna.tudo(PENA)
    perna.face("topo", PENA_ESCURA, 3)
    for lado in ("frente", "tras", "direita", "esquerda"):
        perna.faixa_no_pe(lado, 2, ESCAMA)           # a canela, onde a pena acaba
        perna.salpicar(lado, PENA_ESCURA, 34, 31)

# --- pes ------------------------------------------------------------------
for nome in ("foot_left", "foot_right"):
    pe = k[nome]
    pe.tudo(ESCAMA)
    pe.face("base", ESCAMA_ESCURA, 3)                # a sola
    pe.face("frente", CHIFRE, 3)                     # OS DEDOS, virados para a frente
    for dedo in (1, 2, 3):
        pe.coluna("frente", dedo, CHIFRE_ESCURO, 2)  # os vaos entre eles
    pe.fiada_espelhada("topo", 0, ESCAMA_ESCURA)     # as escamas do dorso do pe
    for lado in ("direita", "esquerda", "tras"):
        pe.salpicar(lado, ESCAMA_ESCURA, 40, 33)

# --- a cauda --------------------------------------------------------------
rabo = k["tail"]
rabo.tudo(PENA)
rabo.face("topo", PENA_ESCURA, 3)
rabo.face("tras", PENA_CLARA, 4)                     # a ponta, clara: e o que se ve por tras
for lado in ("frente", "direita", "esquerda", "base"):
    rabo.salpicar(lado, PENA_ESCURA, 34, 35)
for lado in ("direita", "esquerda"):
    rabo.linha(lado, 1, PENA_CLARA, 2)               # as barras da cauda
    rabo.linha(lado, 3, PENA_CLARA, 2)

# ===========================================================================

valida_paletas_disjuntas()
valida_faces_pintadas(viajante, CAIXAS_DISFARCE)
valida_faces_pintadas(kiriko, CAIXAS_VERDADEIRO)

for folha in (viajante, kiriko):
    print("escrito", folha.salvar(), folha.img.size,
          "-- %d px de face pintados" % len(folha.pintado))
print("caixas pintadas: %d no disfarce, %d na forma verdadeira" % (len(d), len(k)))
print("paletas: %d tons no viajante, %d no bicho, 0 em comum"
      % (len(set(PALETA_HUMANA)), len(set(PALETA_BICHO))))
