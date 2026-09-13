"""Gera a textura autoral da Spider Eagle (64 x 64), casada com spider_eagle.geo.json.

POR QUE ESTE ARQUIVO EXISTE. Mesma razao do gerador de geometria (ADR-017 e
ADR-007): o corpo emprestado do PHANTOM vanilla sai, e o binario que entra precisa
ter gerador no git -- PNG sem gerador e um arquivo que ninguem consegue corrigir.

A TABELA DE CAIXAS NAO MORA AQUI. Ela e importada de spider_eagle_geo.CAIXAS, de
proposito: se cada gerador tivesse a sua, os dois divergiriam na primeira correcao
de modelo e o sintoma nao seria erro nenhum -- seria uma face pintada no lugar
errado, que so aparece na tela.

A CLASSE `Pincel` E PARECIDA COM A DOS TRES MOBS IRMAOS, E ISSO E DELIBERADO. Ela
nao e conhecimento compartilhado: e o mesmo layout de caixa do Bedrock reescrito
para esta paleta. A unica coisa que NAO pode ser duplicada -- a tabela de caixas --
e importada. Se um quinto mob precisar do mesmo pincel, ele vira modulo; quatro
usos com paletas diferentes ainda nao pagam a indirecao, e o dia em que pagarem a
troca e mecanica.

Layout de caixa do Minecraft: para um cubo w x h x d a partir de (u,v),
    topo     (u+d,       v)     w x d
    base     (u+d+w,     v)     w x d
    direita  (u,         v+d)   d x h   (lado -X; a FRENTE fica na borda direita)
    frente   (u+d,       v+d)   w x h   (face -Z)
    esquerda (u+d+w,     v+d)   d x h   (lado +X; a FRENTE fica na borda esquerda)
    tras     (u+2d+w,    v+d)   w x h   (face +Z)

Todo pincel e preso a uma caixa e RECUSA pintar fora do retangulo dela: vazar para
a regiao do vizinho nao da erro, da a face errada no bicho errado.

LEITURA A DISTANCIA -- e para isso que a paleta existe, e cada item aqui e uma
coisa que o jogador precisa entender ANTES de estar dentro do raio de bote:

(a) POUSADA, ELA E PEDRA. O dorso e ocre de canion, mosqueado, sem saturacao
    nenhuma: a ave nasce POUSADA no alto, de dia, e quem escala a parede precisa
    ter de OLHAR para achar o ninho. Um dorso colorido entregaria a posicao dela
    antes de o jogador ter feito qualquer escolha, e o encounter inteiro -- roubar
    ovo sem matar a mae -- comeca por ela estar la sem ter sido notada.

(b) EM VOO, ELA E BARRIGA. O jogador ve esta ave DE BAIXO, e de baixo tudo que
    importa e claro contra o ceu: peito, ventre e a metade interna da asa sao o
    tom mais claro da folha. Contra esse claro sobram TRES pontos quase pretos --
    o gancho do bico, as quatro garras e as pontas das penas. Nao ha nenhum outro
    preto grande no bicho, de proposito: sao esses tres que dizem de que lado
    fica o perigo.

(c) A ASA ABERTA E O AVISO, ENTAO ELA TEM DE TER FORMA. Bordo de ataque escuro e
    continuo, ventre claro, e a PONTA (as primarias) quase preta. Com a asa
    dobrada so se ve o bordo escuro rente ao flanco; aberta, a ponta preta salta
    para fora e o gesto tem contorno. Se a ponta fosse da cor do braco, o bicho
    abriria a asa e a silhueta cresceria sem ninguem perceber o que mudou.

(d) O ABDOME NAO E DE AVE, E ELE PRECISA DENUNCIAR ISSO. Segmentado em faixas
    escuras, com uma seta clara no ventre e uma FIANDEIRA cor de seda na ponta.
    E o unico lugar do bicho que nao le como passaro -- e e o que explica, sem
    uma linha de texto, de onde veio o ninho de teia do canion.

Regerar:  python art-source/enemies/spider_eagle/spider_eagle_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/spider_eagle/adulto.png
"""
from PIL import Image
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spider_eagle_geo import CAIXAS, UV_LARGURA, UV_ALTURA  # noqa: E402  (fonte unica)

# --- a rocha do canion: o que faz a ave pousada sumir na parede -------------
ROCHA = (132, 112, 86)
ROCHA_ESC = (94, 78, 58)
FLANCO = (154, 134, 104)

# --- o claro: tudo que o jogador ve de baixo -------------------------------
VENTRE = (214, 202, 178)
VENTRE_ESC = (170, 156, 132)

# --- as penas de voo: a ponta da asa e o que da contorno ao aviso ----------
PENA = (72, 60, 48)
PENA_BARRA = (44, 36, 30)

# --- a cara --------------------------------------------------------------
MASCARA = (52, 44, 36)        # a risca que sai do olho para a nuca
BICO = (228, 194, 88)         # chifre amarelo de rapina
CERA = (198, 158, 58)         # a cera, na raiz do bico
BICO_PONTA = (30, 26, 22)     # O GANCHO -- um dos tres pretos da folha
OLHO = (246, 204, 74)
OLHO_BRILHO = (255, 236, 168)
PUPILA = (14, 12, 10)

# --- o lado que nao e ave -------------------------------------------------
ABDOME = (80, 66, 58)
ABDOME_BARRA = (44, 36, 32)   # as faixas que segmentam o abdome
SEDA = (234, 232, 222)        # a fiandeira, cor de teia nova
SEDA_SOMBRA = (188, 184, 172)

# --- as pernas ------------------------------------------------------------
ESCAMA = (206, 174, 92)
ESCAMA_ESC = (150, 122, 58)
GARRA = (24, 22, 20)          # o terceiro preto: as unhas

img = Image.new("RGBA", (UV_LARGURA, UV_ALTURA), (0, 0, 0, 0))
px = img.load()


def ruido(x, y, forca):
    """Variacao deterministica: cor chapada le como plastico.

    O hash e embaralhado de proposito. Combinacao linear simples (x*7+y*13)
    produz FAIXAS DIAGONAIS regulares, que a distancia leem como listra e nao como
    pena ou rocha -- e isso so aparece na tela.
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

    def linha(self, nome, dy, cor, forca=3):
        """Uma fiada horizontal contada a partir do TOPO da face."""
        _, _, w, _ = self.faces()[nome]
        self.na_face(nome, 0, dy, w, 1, cor, forca)

    def faixa_no_topo(self, nome, linhas, cor, forca=4):
        _, _, w, h = self.faces()[nome]
        self.na_face(nome, 0, 0, w, min(linhas, h), cor, forca)

    def faixa_no_pe(self, nome, linhas, cor, forca=4):
        _, _, w, h = self.faces()[nome]
        n = min(linhas, h)
        self.na_face(nome, 0, h - n, w, n, cor, forca)

    def coluna(self, nome, dx, cor, forca=3):
        _, _, _, h = self.faces()[nome]
        self.na_face(nome, dx, 0, 1, h, cor, forca)

    def ponto(self, nome, dx, dy, cor):
        self.na_face(nome, dx, dy, 1, 1, cor, 0)

    def salpicar(self, nome, cor, limiar, semente, forca=3):
        """Borroes de 1 px espalhados pela face, deterministicos.

        Mosqueado nao e enfeite: e o que faz o dorso ler como rocha e nao como um
        bloco pintado -- a distancia, cor chapada denuncia a ave que devia estar
        confundida com a parede do canion.
        """
        x, y, w, h = self.faces()[nome]
        for i in range(x, x + w):
            for j in range(y, y + h):
                if _sorteio(i, j, semente) < limiar:
                    self.retangulo(i, j, 1, 1, cor, forca)


pinceis = {c.nome: Pincel(c) for c in CAIXAS}

LADOS = ("direita", "esquerda")
PAREDES = ("frente", "tras", "direita", "esquerda")


def contraluz(p, semente, dorsal=ROCHA, flanco=FLANCO, ventral=VENTRE):
    """Escuro em cima, claro embaixo -- e o esquema que o bicho inteiro segue.

    Nao e estilo: pousada ela e vista de CIMA contra a rocha, e em voo ela e vista
    de BAIXO contra o ceu. Um dorso claro a entregaria parada; um ventre escuro a
    esconderia justo no mergulho, que e o momento em que ela precisa ser vista.
    """
    p.tudo(flanco)
    p.face("topo", dorsal)
    p.face("base", ventral)
    for lado in PAREDES:
        p.faixa_no_topo(lado, 2, dorsal)
        p.faixa_no_pe(lado, 2, ventral)
    p.salpicar("topo", ROCHA_ESC, 70, semente)
    for lado in LADOS:
        p.salpicar(lado, ROCHA_ESC, 44, semente + 7)


# --- o torax: a peca que carrega a asa ------------------------------------
torax = pinceis["thorax"]
contraluz(torax, 3)
# o peito, que e o que chega primeiro quando ela desce: claro e liso, sem mosqueado
torax.faixa_no_pe("frente", 4, VENTRE, 3)
torax.salpicar("tras", ROCHA_ESC, 52, 5)

# --- o abdome: a parte que nao e ave --------------------------------------
abdome = pinceis["abdomen"]
abdome.tudo(ABDOME, 5)
abdome.face("topo", ABDOME, 4)
# AS FAIXAS. Duas no dorso e duas em cada flanco: e a segmentacao que faz o
# abdome ler como corpo de aranha e nao como rabadilha de passaro.
for faixa in (1, 3):
    abdome.linha("topo", faixa, ABDOME_BARRA, 2)
for lado in PAREDES:
    abdome.linha(lado, 1, ABDOME_BARRA, 2)
    abdome.linha(lado, 4, ABDOME_BARRA, 2)
    abdome.faixa_no_pe(lado, 1, VENTRE_ESC, 2)
abdome.face("tras", ABDOME_BARRA, 3)          # a face onde a fiandeira nasce
# A SETA DO VENTRE. Clara, apontando para tras: e a unica marca de desenho do
# bicho, e ela so aparece para quem esta EMBAIXO dele.
abdome.face("base", VENTRE, 4)
abdome.na_face("base", 1, 1, 4, 1, ABDOME_BARRA, 2)
abdome.na_face("base", 2, 2, 2, 1, ABDOME_BARRA, 2)
abdome.ponto("base", 2, 3, ABDOME_BARRA)
abdome.ponto("base", 3, 3, ABDOME_BARRA)

# --- a cabeca -------------------------------------------------------------
cranio = pinceis["head"]
cranio.tudo(ROCHA, 5)
cranio.face("topo", ROCHA_ESC, 4)             # a calota, em sombra
cranio.salpicar("topo", MASCARA, 60, 11)
cranio.face("tras", ROCHA_ESC, 4)             # a nuca
cranio.face("base", VENTRE, 4)                # a garganta -- vista de baixo
cranio.faixa_no_topo("frente", 2, MASCARA, 2)  # a sobrancelha corrida da rapina
cranio.faixa_no_pe("frente", 2, VENTRE, 3)     # o papo claro

# OS OLHOS. Ficam nos LADOS, como os de ave, e nao na frente. Amarelos com pupila
# preta: sao o unico ponto saturado da folha, e servem a uma coisa so -- quem
# esta sendo encarado precisa ver que esta sendo encarado, ou o aviso nao e aviso.
# Na face 'direita' a FRENTE fica na borda direita; na 'esquerda', na esquerda --
# por isso as duas colunas abaixo sao espelhadas em vez de repetidas. Copiar a
# mesma coluna nos dois lados nao da erro: da uma ave visivelmente vesga por um
# lado so, e ninguem descobre isso sem girar a camera em volta dela.
for lado in LADOS:
    dianteira = 4 if lado == "direita" else 0     # coluna encostada no bico
    traseira = 3 if lado == "direita" else 1      # coluna encostada na nuca
    inicio_da_risca = 0 if lado == "direita" else 1
    cranio.na_face(lado, min(dianteira, traseira), 1, 2, 2, OLHO, 2)
    cranio.ponto(lado, traseira, 1, OLHO_BRILHO)  # o brilho, no canto de tras
    cranio.ponto(lado, dianteira, 2, PUPILA)      # a pupila, olhando para a frente
    cranio.na_face(lado, inicio_da_risca, 3, 4, 1, MASCARA, 2)  # a risca ate a nuca

# --- o bico: o primeiro dos tres pretos -----------------------------------
bico = pinceis["beak"]
bico.tudo(BICO, 4)
bico.face("tras", CERA, 3)                     # a cera, onde ele entra no cranio
# O GANCHO. Frente inteira preta, e as duas fiadas da PONTA pretas por cima e por
# baixo: de baixo -- que e de onde o jogador olha no mergulho -- ele e um bico de
# rapina virado para ele, e nao um cone amarelo.
bico.face("frente", BICO_PONTA, 2)
bico.na_face("topo", 0, 0, 2, 2, BICO_PONTA, 2)
bico.na_face("base", 0, 0, 2, 2, BICO_PONTA, 2)
bico.na_face("base", 0, 2, 2, 2, CERA, 2)
for lado, borda_da_frente, narina in (("direita", 3, 0), ("esquerda", 0, 3)):
    bico.coluna(lado, borda_da_frente, BICO_PONTA, 2)
    bico.ponto(lado, narina, 1, CERA)          # a narina, na raiz

# --- as asas: braco e ponta ----------------------------------------------
# O braco e rocha por cima e claro por baixo, como o resto do bicho. O que muda
# aqui e o BORDO DE ATAQUE: escuro e continuo, ele desenha a linha da asa contra
# o ceu sem depender de sombra.
for nome, face_externa in (("wing_left", "esquerda"), ("wing_right", "direita")):
    asa = pinceis[nome]
    asa.tudo(ROCHA, 5)
    asa.face("topo", ROCHA, 5)
    asa.salpicar("topo", ROCHA_ESC, 56, 17)
    asa.face("frente", ROCHA_ESC, 3)           # BORDO DE ATAQUE
    asa.face("tras", PENA, 3)                  # bordo de fuga, das penas
    asa.face("base", VENTRE, 4)                # a face que o jogador ve
    asa.na_face("base", 0, 2, 6, 2, VENTRE_ESC, 3)   # sombra sob as secundarias
    asa.linha("topo", 3, PENA, 2)              # a barra que separa braco de ponta
    asa.face(face_externa, PENA, 3)            # o punho, onde a ponta se encaixa

# A PONTA. Quase preta nas duas faces do fim: dobrada ela e uma risca escura rente
# ao flanco, aberta ela salta para fora e o aviso ganha contorno. Se ela fosse da
# cor do braco, a asa abriria e ninguem veria o que mudou.
for nome, face_interna in (("wing_tip_left", "direita"), ("wing_tip_right", "esquerda")):
    ponta = pinceis[nome]
    ponta.tudo(PENA, 4)
    ponta.face("frente", ROCHA_ESC, 3)         # o punho, colado no braco
    ponta.face("tras", PENA_BARRA, 2)          # o fim das primarias
    ponta.na_face("topo", 0, 3, 3, 3, PENA_BARRA, 3)
    # por baixo, a metade de dentro e clara e a de fora e preta: a mao da ave
    ponta.face("base", VENTRE, 4)
    ponta.na_face("base", 0, 3, 3, 3, PENA_BARRA, 3)
    for lado in LADOS:
        ponta.faixa_no_pe(lado, 1, VENTRE_ESC, 2)
    ponta.salpicar(face_interna, PENA_BARRA, 40, 19)

# --- a cauda: o leme ------------------------------------------------------
leque = pinceis["tail"]
leque.tudo(ROCHA, 4)
leque.face("topo", ROCHA, 4)
leque.linha("topo", 1, PENA, 2)                # a barra do meio
leque.na_face("topo", 0, 3, 7, 1, PENA_BARRA, 2)   # a barra terminal, escura
leque.face("base", VENTRE, 4)                  # por baixo, clara
leque.na_face("base", 0, 3, 7, 1, PENA_BARRA, 2)
leque.face("tras", PENA_BARRA, 2)              # o fim do leque
for lado in LADOS:
    leque.face(lado, ROCHA_ESC, 3)

# --- a fiandeira: o que explica o ninho -----------------------------------
fiandeira = pinceis["spinner"]
fiandeira.tudo(ABDOME_BARRA, 4)
fiandeira.face("topo", ABDOME, 3)              # onde ela entra no abdome
# A BOCA DA FIANDEIRA, cor de teia nova: fica na face de BAIXO e na de TRAS, que
# sao as duas unicas de onde ela e vista -- de baixo em voo, de tras no ninho.
fiandeira.face("base", SEDA, 3)
fiandeira.ponto("base", 1, 0, SEDA_SOMBRA)
fiandeira.face("tras", SEDA_SOMBRA, 3)
fiandeira.coluna("tras", 1, SEDA, 2)
for lado in LADOS:
    fiandeira.faixa_no_pe(lado, 1, SEDA_SOMBRA, 2)

# --- as quatro pernas: finas e ARTICULADAS --------------------------------
# Os aneis escuros no quadril, no joelho e no tornozelo sao o que faz uma coluna
# de 1 px ler como perna com juntas, e nao como um palito.
for nome in ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"):
    perna = pinceis[nome]
    perna.tudo(ESCAMA, 4)
    perna.face("topo", ABDOME_BARRA, 2)        # a raiz, na sombra do corpo
    perna.face("base", ESCAMA_ESC, 3)
    for lado in PAREDES:
        perna.linha(lado, 0, ESCAMA_ESC, 2)    # quadril
        perna.linha(lado, 2, ESCAMA_ESC, 2)    # joelho
        perna.linha(lado, 5, GARRA, 2)         # tornozelo, ja quase da cor da unha

# --- as garras: o terceiro preto ------------------------------------------
for nome in ("talon_front_left", "talon_front_right", "talon_back_left", "talon_back_right"):
    garra = pinceis[nome]
    garra.tudo(ESCAMA, 4)
    garra.face("tras", ESCAMA_ESC, 3)
    # AS UNHAS. Tres riscas pretas correndo da frente para tras, no topo e na
    # sola: e este contorno dentado que aparece contra o ceu quando ela passa por
    # cima, e e a unica coisa que diz que a ave que vem descendo nao vai so passar.
    for face in ("topo", "base"):
        garra.na_face(face, 0, 0, 1, 2, GARRA, 2)
        garra.na_face(face, 2, 0, 1, 2, GARRA, 2)
        garra.ponto(face, 1, 0, GARRA)
    garra.face("frente", GARRA, 2)             # as pontas, vistas de frente
    for lado, borda_da_frente in (("direita", 2), ("esquerda", 0)):
        garra.coluna(lado, borda_da_frente, GARRA, 2)

def valida_sem_buraco():
    """Nenhum pixel transparente DENTRO do retangulo de uma caixa.

    Face esquecida nao da erro e nao aparece no atlas aberto no editor -- aparece
    como um buraco por onde se ve o interior do bicho, e so de um angulo. E a
    falha mais barata de cometer aqui: basta pintar cinco faces de seis.
    """
    for c in CAIXAS:
        p = pinceis[c.nome]
        for nome, (x, y, w, h) in ((n, v) for n, v in p.faces().items()):
            for i in range(x, x + w):
                for j in range(y, y + h):
                    if px[i, j][3] != 255:
                        raise ValueError("a face '%s' da caixa '%s' ficou sem tinta em (%d,%d): "
                                         "em jogo isso e um buraco no bicho, visto de um angulo so"
                                         % (nome, c.nome, i, j))


valida_sem_buraco()

destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "textures", "entity", "spider_eagle", "adulto.png")
os.makedirs(os.path.dirname(destino), exist_ok=True)
img.save(destino)
print("escrito", destino, img.size)
print("caixas pintadas:", len(pinceis))
