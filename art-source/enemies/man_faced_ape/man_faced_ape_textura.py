"""Gera as DUAS texturas autorais do Man-Faced Ape (64 x 64 cada), casadas com os geo.

POR QUE ESTE ARQUIVO EXISTE. Mesma razao do gerador de geometria (ADR-017 e
ADR-007): os corpos emprestados -- o ALDEAO e o PIGLIN vanilla -- saem, e os
binarios que entram precisam de gerador no git. PNG sem gerador e um arquivo que
ninguem consegue corrigir depois.

AS TABELAS DE CAIXAS NAO MORAM AQUI. Sao importadas de man_faced_ape_geo, de
proposito: se cada gerador tivesse a sua, os dois divergiriam na primeira
correcao de modelo, e o sintoma nao seria erro nenhum -- seria uma face pintada
no lugar errado, que so aparece na tela. Sao DOIS conjuntos porque sao dois
modelos, e os dois vem do MESMO modulo.

A CLASSE `Pincel` E PARECIDA COM A DO SAPO E A DO GREAT STAMP, E ISSO E
DELIBERADO. Ela nao e conhecimento compartilhado: e o mesmo layout de caixa do
Bedrock reescrito para esta paleta. A unica coisa que NAO pode ser duplicada --
a tabela de caixas -- e importada. (A diferenca util aqui: o pincel recebe a
FOLHA, porque este mob pinta duas, e `na_face` recorta em coordenada RELATIVA a
face, que e o que impede rosto pintado 2 px fora do lugar.)

Layout de caixa do Minecraft: para um cubo w x h x d a partir de (u,v),
    topo     (u+d,       v)     w x d
    base     (u+d+w,     v)     w x d
    direita  (u,         v+d)   d x h   (lado -X; a FRENTE fica na borda direita)
    frente   (u+d,       v+d)   w x h   (face -Z)
    esquerda (u+d+w,     v+d)   d x h   (lado +X; a FRENTE fica na borda esquerda)
    tras     (u+2d+w,    v+d)   w x h   (face +Z)

Todo pincel e preso a uma caixa e RECUSA pintar fora do retangulo dela: vazar
para a regiao do vizinho nao da erro, da a face errada no bicho errado.

A PALETA DO ROSTO E UMA SO, COMPARTILHADA PELAS DUAS FORMAS, e isso e a decisao
que carrega o mob inteiro: e a MESMA cara. PELE, OLHO_BRANCO, PUPILA, BOCA e
SOBRANCELHA sao usados nos dois desenhos. Se um dia alguem der ao macaco um tom
de pele proprio, a revelacao passa a ler como "trocou de bicho" em vez de "a cara
e a mesma, o corpo e que esta errado" -- e nenhum portao veria isso, porque duas
constantes de cor diferentes sao codigo perfeitamente valido.

LEITURA A DISTANCIA -- e para isso que o resto da paleta existe:

(a) O VIAJANTE TEM DE PARECER GENTE, E APAGADO. Pano surrado em marrom-cinza,
    nada saturado, nada que chame atencao. Um disfarce vistoso seria olhado, e
    ser olhado e exatamente o que faz o macaco PARAR -- ele se entregaria sozinho.

(b) O CAPUZ E UM VAZIO. A face da frente do capuz e quase preta com uma borda de
    pano em volta: de longe o jogador nao ve rosto, ve sombra. E so por isso que
    a cara humana pode estar pintada embaixo o tempo todo, pronta para o clipe de
    revelacao descobrir.

(c) AS MAOS SAO DE PELE, COM PELO. Elas sao o unico pedaco de corpo a mostra no
    disfarce e a unica pista antes dos 3.5 blocos. Pele clara contra pano escuro
    ja puxa o olho; os fios escuros salpicados nas costas da mao sao o detalhe
    que fecha a conta depois que o jogador ja desconfiou.

(d) REVELADO, O CONTRASTE E O SUSTO. Pelo quase preto no corpo inteiro e uma
    MASCARA de pele clara na cara -- e o unico ponto claro grande do bicho. E o
    contraste entre os dois que da o arrepio, e nao o formato.

Regerar:  python art-source/enemies/man_faced_ape/man_faced_ape_textura.py
Exporta:  .../textures/entity/man_faced_ape/revelado.png
          .../textures/entity/man_faced_ape_disfarce/humano.png
"""
from PIL import Image
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from man_faced_ape_geo import (  # noqa: E402  (fonte unica das caixas)
    CAIXAS_DISFARCE, CAIXAS_REVELADO, UV_LARGURA, UV_ALTURA)

# --- o rosto: uma paleta so para as duas formas ---------------------------
PELE = (196, 156, 124)
PELE_SOMBRA = (148, 110, 86)
OLHO_BRANCO = (222, 216, 206)
PUPILA = (26, 20, 18)
BOCA = (92, 54, 48)
SOBRANCELHA = (56, 38, 30)
CABELO = (48, 36, 28)

# --- o viajante -----------------------------------------------------------
PANO = (84, 74, 63)           # tunica surrada, sem saturacao nenhuma
PANO_CLARO = (104, 93, 79)    # o que a luz de cima pega
PANO_ESCURO = (56, 49, 42)
SOMBRA_DO_CAPUZ = (20, 18, 17)  # o vazio onde deveria haver rosto
CALCA = (62, 55, 48)
BOTA = (40, 34, 29)
CINTO = (48, 38, 29)
FIVELA = (132, 112, 72)

# --- o macaco -------------------------------------------------------------
PELO = (46, 38, 34)
PELO_CLARO = (74, 62, 54)     # a capa dos ombros: e ela que engrossa o vulto
PELO_ESCURO = (28, 23, 21)
COURO = (72, 56, 48)          # palma, sola, calo
UNHA = (22, 18, 16)


def ruido(x, y, forca):
    """Variacao deterministica: cor chapada le como plastico.

    O hash e embaralhado de proposito. Combinacao linear simples (x*7+y*13)
    produz FAIXAS DIAGONAIS regulares, que a distancia leem como listra e nao
    como pano ou pelo -- e isso so aparece na tela.
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

    def salvar(self):
        os.makedirs(os.path.dirname(self.destino), exist_ok=True)
        self.img.save(self.destino)
        return self.destino


class Pincel:
    """Pincel preso a uma caixa DE UMA folha. Pintar fora do retangulo e erro."""

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

    # -- pintura em coordenada da FACE --------------------------------------
    # Tudo abaixo fala em (coluna, linha) DENTRO da face. E o que impede o erro
    # que nao da erro: um olho pintado 2 px fora cai no vizinho e o portao de
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

        Mosqueado nao e enfeite: e o que faz o pelo ler como pelo e o pano como
        pano gasto -- a distancia, cor chapada denuncia o modelo como plastico.
        """
        x, y, w, h = self.faces()[nome]
        for i in range(x, x + w):
            for j in range(y, y + h):
                if _sorteio(i, j, semente) < limiar:
                    self.retangulo(i, j, 1, 1, cor, forca)

    def sombrear(self, nome, dy, linhas, fator):
        """Escurece o que JA foi pintado -- a sombra do capuz sobre o rosto."""
        x, y, w, h = self.faces()[nome]
        linhas = min(linhas, h - dy)
        for i in range(x, x + w):
            for j in range(y + dy, y + dy + linhas):
                r, g, b, a = self.folha.px[i, j]
                self.folha.px[i, j] = (int(r * fator), int(g * fator), int(b * fator), a)


def pinceis(folha, caixas):
    return {c.nome: Pincel(folha, c) for c in caixas}


def rosto_humano(p, nome_da_face, topo, alto_da_testa=True):
    """Pinta A CARA -- a mesma nas duas formas, por isso e uma funcao so.

    `topo` e a linha em que a testa comeca dentro da face; abaixo dela vem
    sobrancelha, olhos, nariz e queixo, nesta ordem, uma fiada cada. A funcao
    RECUSA uma face que nao tenha as 6 fiadas: um rosto espremido nao da erro,
    da uma carinha de bloco que so aparece na tela.
    """
    _, _, largura, altura = p.faces()[nome_da_face]
    if largura < 8 or altura - topo < 6:
        raise ValueError("caixa '%s': a face '%s' tem %dx%d e o rosto precisa de 8 colunas e 6 "
                         "fiadas a partir da linha %d" % (p.c.nome, nome_da_face, largura,
                                                          altura, topo))
    if alto_da_testa:
        p.na_face(nome_da_face, 1, topo, 6, 1, PELE, 3)          # testa
    p.na_face(nome_da_face, 1, topo + 1, 6, 1, SOBRANCELHA, 2)   # sobrancelha corrida
    # OS OLHOS. Duas fiadas de branco com a pupila no meio: e a unica coisa da
    # cara que o jogador reconhece a 3 blocos, e a razao de a mascara ser clara.
    p.na_face(nome_da_face, 1, topo + 2, 2, 1, OLHO_BRANCO, 2)
    p.na_face(nome_da_face, 5, topo + 2, 2, 1, OLHO_BRANCO, 2)
    p.ponto(nome_da_face, 2, topo + 2, PUPILA)
    p.ponto(nome_da_face, 5, topo + 2, PUPILA)
    # nariz: duas fiadas de sombra no meio, estreitas
    p.na_face(nome_da_face, 3, topo + 3, 2, 1, PELE_SOMBRA, 2)
    p.ponto(nome_da_face, 3, topo + 4, PELE_SOMBRA)
    p.ponto(nome_da_face, 4, topo + 4, PELE_SOMBRA)
    # boca
    p.na_face(nome_da_face, 2, topo + 5, 4, 1, BOCA, 2)


# ===========================================================================
# FORMA HUMANA -- textures/entity/man_faced_ape_disfarce/humano.png
# ===========================================================================

viajante = Folha("man_faced_ape_disfarce",
                 os.path.join("src", "main", "resources", "assets", "nenfoundation",
                              "textures", "entity", "man_faced_ape_disfarce", "humano.png"))
d = pinceis(viajante, CAIXAS_DISFARCE)

# --- a tunica -------------------------------------------------------------
tunica = d["body"]
tunica.tudo(PANO)
tunica.face("topo", PANO_CLARO, 4)          # ombro, onde a luz bate
tunica.face("base", PANO_ESCURO, 3)         # por baixo da barra
for lado in ("frente", "tras", "direita", "esquerda"):
    tunica.faixa_no_pe(lado, 2, PANO_ESCURO)   # a barra em sombra
    tunica.linha(lado, 5, CINTO, 2)            # o cinto, na altura da cintura
    tunica.salpicar(lado, PANO_ESCURO, 34, 5)  # pano gasto, sem brilho
tunica.ponto("frente", 3, 5, FIVELA)           # a fivela: o unico ponto claro da roupa
tunica.na_face("frente", 3, 0, 1, 5, PANO_ESCURO, 2)   # a costura do peito

# --- a cabeca: a cara ja esta aqui, so nao da para ver --------------------
cabeca = d["head"]
cabeca.tudo(PELE)
cabeca.face("topo", CABELO, 3)
cabeca.face("tras", CABELO, 3)
for lado in ("direita", "esquerda"):
    cabeca.faixa_no_topo(lado, 3, CABELO)       # cabelo descendo pelas tempora
cabeca.faixa_no_topo("frente", 1, CABELO)       # a linha do cabelo
rosto_humano(cabeca, "frente", 1)
cabeca.faixa_no_pe("frente", 1, PELE_SOMBRA, 2)  # o queixo -- e SO ISTO que fica de fora
# A SOMBRA DO CAPUZ. Da testa para cima o capuz cobre de verdade (a casca e um
# volume, nao um truque de pintura); esta sombra e para o clipe de revelacao, em
# que o capuz cai e o rosto aparece saindo do escuro.
cabeca.sombrear("frente", 0, 5, 0.55)
for lado in ("direita", "esquerda"):
    cabeca.sombrear(lado, 0, 5, 0.55)

# --- o capuz: um vazio com borda de pano ----------------------------------
capuz = d["hood"]
capuz.tudo(PANO_ESCURO)
capuz.face("topo", PANO, 4)
capuz.face("base", SOMBRA_DO_CAPUZ, 2)          # por dentro, onde a cabeca entra
capuz.face("frente", PANO_ESCURO, 3)
capuz.na_face("frente", 1, 1, 7, 4, SOMBRA_DO_CAPUZ, 2)   # A BOCA DO CAPUZ
for lado in ("direita", "esquerda"):
    capuz.faixa_no_pe(lado, 1, PANO, 3)         # a aba, que pega luz na ponta
    capuz.salpicar(lado, PANO_ESCURO, 30, 9)
capuz.salpicar("tras", PANO_ESCURO, 30, 11)

# --- mangas ---------------------------------------------------------------
for nome in ("sleeve_left", "sleeve_right"):
    manga = d[nome]
    manga.tudo(PANO)
    manga.face("topo", PANO_CLARO, 4)
    manga.face("base", PANO_ESCURO, 2)
    for lado in ("frente", "tras", "direita", "esquerda"):
        manga.faixa_no_pe(lado, 1, PANO_ESCURO)   # o punho da manga
        manga.salpicar(lado, PANO_ESCURO, 30, 13)

# --- AS MAOS: a unica coisa errada ---------------------------------------
for nome in ("hand_left", "hand_right"):
    mao = d[nome]
    mao.tudo(PELE, 4)
    mao.face("topo", PELE_SOMBRA, 3)              # costas da mao, na sombra da manga
    mao.face("base", PELE_SOMBRA, 3)              # palma
    for lado in ("frente", "tras", "direita", "esquerda"):
        mao.faixa_no_topo(lado, 1, PELE_SOMBRA, 2)   # os nos dos dedos
    mao.coluna("frente", 1, PELE_SOMBRA, 2)          # os vaos entre os dedos
    # OS FIOS. Pelo escuro nas costas e nos lados da mao -- a distancia nao se ve,
    # e a 3 blocos e a segunda coisa que nao fecha.
    for lado in ("topo", "frente", "direita", "esquerda"):
        mao.salpicar(lado, CABELO, 26, 17, 2)

# --- pernas ---------------------------------------------------------------
for nome in ("leg_left", "leg_right"):
    perna = d[nome]
    perna.tudo(CALCA)
    perna.face("topo", PANO_ESCURO, 2)            # por baixo da tunica
    perna.face("base", BOTA, 2)                   # a sola
    for lado in ("frente", "tras", "direita", "esquerda"):
        perna.faixa_no_pe(lado, 3, BOTA)          # a bota
        perna.salpicar(lado, PANO_ESCURO, 28, 19)

# ===========================================================================
# FORMA REVELADA -- textures/entity/man_faced_ape/revelado.png
# ===========================================================================

macaco = Folha("man_faced_ape",
               os.path.join("src", "main", "resources", "assets", "nenfoundation",
                            "textures", "entity", "man_faced_ape", "revelado.png"))
r = pinceis(macaco, CAIXAS_REVELADO)

# --- tronco ---------------------------------------------------------------
tronco = r["torso"]
tronco.tudo(PELO)
tronco.face("topo", PELO_ESCURO, 3)
tronco.face("base", PELO_ESCURO, 3)
tronco.faixa_no_pe("frente", 5, PELO_CLARO)       # o ventre, mais claro
for lado in ("frente", "tras", "direita", "esquerda"):
    tronco.salpicar(lado, PELO_ESCURO, 58, 23)

# --- a cinta de ombro: e ela que engrossa o vulto -------------------------
ombro = r["chest"]
ombro.tudo(PELO)
ombro.face("topo", PELO_CLARO, 4)                 # a capa, vista de cima
for lado in ("frente", "tras", "direita", "esquerda"):
    ombro.faixa_no_topo(lado, 2, PELO_CLARO)      # e a capa descendo pelos lados
    ombro.salpicar(lado, PELO_ESCURO, 52, 29)

# --- a cabeca: A MASCARA --------------------------------------------------
cranio = r["head"]
cranio.tudo(PELO)
cranio.face("topo", PELO_ESCURO, 3)
cranio.salpicar("tras", PELO_ESCURO, 52, 31)
# A cara ocupa as colunas 1..6 de 8: sobra uma fiada de pelo de cada lado, e e
# essa moldura escura que faz a pele clara SALTAR. Rosto que vaza ate a borda
# perde a moldura e vira mancha.
cranio.na_face("frente", 1, 1, 6, 6, PELE, 3)
cranio.faixa_no_topo("frente", 1, PELO, 2)        # a testa peluda, baixa
rosto_humano(cranio, "frente", 1, alto_da_testa=False)
# a pele dobra a esquina: uma coluna de cara em cada lado do cranio
cranio.coluna("direita", 5, PELE_SOMBRA, 2)       # na 'direita' a FRENTE fica na borda direita
cranio.coluna("esquerda", 0, PELE_SOMBRA, 2)      # na 'esquerda' a FRENTE fica na borda esquerda

# --- a mandibula ----------------------------------------------------------
queixo = r["jaw"]
queixo.tudo(PELE, 4)
queixo.face("base", PELO_ESCURO, 3)               # por baixo do queixo, sombra
queixo.face("tras", PELO_ESCURO, 3)               # a garganta, escondida pelo peito
queixo.face("topo", BOCA, 3)                      # o interior, so visivel quando o osso abre
queixo.linha("frente", 0, BOCA, 2)                # A LINHA DA BOCA, de ponta a ponta
queixo.faixa_no_pe("frente", 1, PELE_SOMBRA, 2)   # o queixo em sombra
for lado in ("direita", "esquerda"):
    queixo.linha(lado, 0, BOCA, 2)                # a boca continua pelos cantos

# --- orelhas --------------------------------------------------------------
for nome, externa in (("ear_left", "esquerda"), ("ear_right", "direita")):
    orelha = r[nome]
    orelha.tudo(PELO, 3)
    orelha.face(externa, PELE_SOMBRA, 3)          # a concha, de pele

# --- bracos ---------------------------------------------------------------
for nome in ("arm_left", "arm_right"):
    braco = r[nome]
    braco.tudo(PELO)
    braco.face("topo", PELO_CLARO, 3)             # a capa dos ombros continua no braco
    for lado in ("frente", "tras", "direita", "esquerda"):
        braco.faixa_no_topo(lado, 2, PELO_CLARO)
        braco.faixa_no_pe(lado, 2, PELO_ESCURO)   # o antebraco escurece ate o pulso
        braco.salpicar(lado, PELO_ESCURO, 54, 37)

# --- maos: o bicho anda nos nos -------------------------------------------
for nome in ("hand_left", "hand_right"):
    mao = r[nome]
    mao.tudo(PELO)
    mao.face("topo", PELO_ESCURO, 3)
    mao.face("base", COURO, 4)                    # a palma
    mao.face("frente", COURO, 4)                  # OS NOS, que e o que toca o chao
    for dedo in range(1, 4):
        mao.coluna("frente", dedo, UNHA, 2)       # os vaos entre os dedos
    mao.salpicar("tras", PELO_ESCURO, 50, 41)

# --- pernas e pes ---------------------------------------------------------
for nome in ("leg_left", "leg_right"):
    perna = r[nome]
    perna.tudo(PELO)
    perna.face("topo", PELO_ESCURO, 3)
    for lado in ("frente", "tras", "direita", "esquerda"):
        perna.faixa_no_pe(lado, 1, PELO_ESCURO)
        perna.salpicar(lado, PELO_ESCURO, 54, 43)

for nome in ("foot_left", "foot_right"):
    pe = r[nome]
    pe.tudo(PELO)
    pe.face("base", COURO, 4)                     # a sola
    pe.faixa_no_pe("frente", 1, COURO, 3)         # os dedos aparecendo na frente
    for dedo in range(1, 4):
        pe.coluna("base", dedo, UNHA, 2)

# ===========================================================================

for folha in (viajante, macaco):
    print("escrito", folha.salvar(), folha.img.size)
print("caixas pintadas: %d no disfarce, %d no revelado" % (len(d), len(r)))
