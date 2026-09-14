"""Gera a textura autoral de `world_tree_leaves_luminous` (16 x 16).

POR QUE ESTE ARQUIVO EXISTE. Mesma razao dos geradores dos bichos (ADR-007):
PNG sem gerador e um arquivo que ninguem consegue corrigir depois. Quem quiser
mais dourado, menos buraco ou outro verde mexe aqui e regera -- e nao abre um
editor de imagem e pinta por cima, porque isso apaga a explicacao.

O QUE A TEXTURA PRECISA COMUNICAR, e por que ela nao e uma folha do vanilla
recolorida:

(a) DE LONGE ELA E FOLHA. Se o bloco luminoso lesse como "pedra brilhante"
    encravada na copa, a copa viraria um mosaico. O fundo e a mesma massa verde
    das outras tres folhas, com a mesma silhueta recortada.

(b) DE PERTO ELA E FOLHA COBERTA DE LIQUEN. Os pontos dourados sao ESPARSOS e
    AGRUPADOS -- manchas de dois a quatro pixels, como o glow lichen --, e nao
    uma chuva de pixels soltos. Ponto isolado em textura de 16x16 some na
    primeira mipmap: a 30 blocos a folha voltaria a ser verde lisa, e o jogador
    nao teria como saber de onde vem a luz.

(c) O DOURADO TEM HALO. Cada mancha tem um pixel mais claro no meio e um mais
    escuro na borda. Sem isso ela le como um furo amarelo, e nao como algo que
    emite.

ELA NAO LEVA TINT DE BIOMA, e o verde daqui e o motivo: as outras tres folhas
sao texturas do vanilla pintadas pelo bioma em runtime; esta traz a propria cor,
porque tingir de verde escuro um bloco que emite luz 15 apaga a unica coisa que
o distingue em tela. Ver WorldTreeColorHandlers.

DETERMINISTICO. Nenhum `random` sem semente: mesma execucao, mesmo PNG, bit a
bit. O portao Java nao regera este arquivo (ele nao fala Python), entao a
reprodutibilidade daqui e o que garante que o gerador continue explicando o
arquivo versionado.

Como rodar, da raiz do repositorio:

    python art-source/worldtree/folha_luminosa.py
"""

from PIL import Image
import os

LARGURA = 16
ALTURA = 16

DESTINO = os.path.join(
    "src", "main", "resources", "assets", "nenfoundation",
    "textures", "block", "world_tree_leaves_luminous.png")

# A PALETA VERDE E ESCURA DE PROPOSITO. O bloco emite luz 15: em jogo ele ja
# chega na tela com o brilho maximo, e uma textura clara estoura para branco e
# perde o desenho da folha. As outras tres folhas podem ser claras porque o
# bioma as escurece; esta nao passa por tint nenhum.
VERDE_FUNDO = (26, 48, 24)
VERDE_ESCURO = (34, 62, 30)
VERDE_MEIO = (46, 82, 38)
VERDE_CLARO = (62, 104, 48)

# Dourado quente, e nao amarelo puro: amarelo saturado le como enxofre.
OURO_BORDA = (138, 96, 24)
OURO = (206, 158, 52)
OURO_BRILHO = (246, 214, 122)


def sorteio(x, y, semente):
    """Hash inteiro, deterministico e sem dependencia de `random`."""
    valor = (x * 0x1F1F1F1F) ^ (y * 0x9E3779B1) ^ (semente * 0x85EBCA6B)
    valor &= 0xFFFFFFFF
    valor ^= valor >> 15
    valor = (valor * 0x2545F491) & 0xFFFFFFFF
    valor ^= valor >> 13
    return valor


def unidade(x, y, semente):
    return (sorteio(x, y, semente) & 0xFFFF) / 65535.0


# A MASSA DE FOLHA, EM MANCHAS -- e nao pixel a pixel.
#
# Ruido por pixel produz chuvisco, que e o mesmo defeito que a copa levou tres
# tentativas para matar (secao 1.2 de docs/worldtree/copa-e-folhagem.md). Aqui a
# correlacao vem de somar dois "blocos" de 2x2 e 4x4: o resultado tem grumos do
# tamanho de uma folha, e nao granulado de TV.
def tom_do_pixel(x, y):
    grosso = unidade(x // 4, y // 4, 11)
    medio = unidade(x // 2, y // 2, 23)
    fino = unidade(x, y, 37)
    return grosso * 0.5 + medio * 0.34 + fino * 0.16


# OS BURACOS SAO O QUE FAZ LER COMO FOLHA. Um quadrado cheio le como bloco de
# musgo; o recorte e metade da leitura.
#
# O LIMIAR E UNIFORME, E ISSO FOI UM CONSERTO. A primeira versao concentrava
# buraco na BORDA do quadro -- parecia razoavel, "a folha se desfaz nas pontas".
# O bloco LADRILHA: duas bordas vizinhas encostam, e o resultado foi uma GRADE
# preta de um pixel atravessando a copa inteira, a cada 16 blocos. Em tela isso
# le como emenda de chunk, que e o artefato mais caro de diagnosticar desta
# trilha. So aparece ao ladrilhar a textura -- olhar o quadro sozinho nao mostra.
def e_buraco(x, y):
    return tom_do_pixel(x, y) < 0.30


# AS MANCHAS DOURADAS SAO POSICOES FIXAS, e nao um sorteio por pixel.
#
# Escritas a mao para garantir o que o sorteio nao garante: que elas estejam
# espalhadas pelo quadro (e nao amontoadas num canto), que nenhuma encoste na
# borda -- o bloco ladrilha, e mancha na borda vira listra continua entre blocos
# vizinhos -- e que a contagem caia na faixa que o portao mede.
#
# Cada tupla e (x, y, tamanho). Tamanho 1 = um pixel de ouro com halo; 2 = par.
MANCHAS = [
    (4, 3, 2),
    (11, 4, 1),
    (7, 8, 1),
    (13, 10, 1),
    (3, 11, 2),
    (10, 13, 1),
]


def pixels_da_mancha(mx, my, tamanho):
    """O miolo da mancha: um pixel, ou um par na diagonal."""
    if tamanho == 1:
        return [(mx, my)]
    return [(mx, my), (mx + 1, my + 1)]


def gerar():
    img = Image.new("RGBA", (LARGURA, ALTURA), (0, 0, 0, 0))
    pix = img.load()

    # 1. a massa verde
    for y in range(ALTURA):
        for x in range(LARGURA):
            if e_buraco(x, y):
                continue
            tom = tom_do_pixel(x, y)
            if tom < 0.34:
                cor = VERDE_FUNDO
            elif tom < 0.55:
                cor = VERDE_ESCURO
            elif tom < 0.78:
                cor = VERDE_MEIO
            else:
                cor = VERDE_CLARO
            pix[x, y] = cor + (255,)

    # 2. o halo escuro de cada mancha, ANTES do miolo -- assim o miolo sempre
    #    fica por cima e nenhuma mancha perde o centro para o halo da vizinha.
    #
    # DUAS DIRECOES, E NAO AS QUATRO. Com as quatro, toda mancha virava um SINAL
    # DE MAIS perfeito: seis cruzes identicas espalhadas pelo quadro, que leem
    # como florzinha desenhada, e nao como liquen. Duas direcoes, escolhidas pelo
    # indice da mancha, dao seis formas diferentes com o mesmo custo.
    lados = ((-1, 0), (0, -1), (1, 0), (0, 1))
    miolos = set()
    for mx, my, tamanho in MANCHAS:
        miolos.update(pixels_da_mancha(mx, my, tamanho))
    for indice, (mx, my, tamanho) in enumerate(MANCHAS):
        escolhidos = (lados[indice % 4], lados[(indice + 1 + indice // 4) % 4])
        for cx, cy in pixels_da_mancha(mx, my, tamanho):
            for dx, dy in escolhidos:
                x, y = cx + dx, cy + dy
                if not (0 <= x < LARGURA and 0 <= y < ALTURA):
                    continue
                if (x, y) in miolos:
                    continue
                # O HALO NAO PREENCHE BURACO. Se preenchesse, a mancha fecharia
                # o recorte da folha e viraria um disco solido.
                if pix[x, y][3] == 0:
                    continue
                pix[x, y] = OURO_BORDA + (255,)

    # 3. o miolo dourado, com um pixel de brilho no primeiro de cada mancha
    for mx, my, tamanho in MANCHAS:
        for indice, (x, y) in enumerate(pixels_da_mancha(mx, my, tamanho)):
            pix[x, y] = (OURO_BRILHO if indice == 0 else OURO) + (255,)

    return img


if __name__ == "__main__":
    destino = os.path.abspath(DESTINO)
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    imagem = gerar()
    # `optimize=True` para o PNG sair sempre igual: sem ele o zlib pode variar
    # o encode entre versoes e o arquivo mudaria sem a imagem mudar.
    imagem.save(destino, "PNG", optimize=True)

    opacos = sum(1 for y in range(ALTURA) for x in range(LARGURA)
                 if imagem.getpixel((x, y))[3] > 0)
    nucleo = sum(1 for y in range(ALTURA) for x in range(LARGURA)
                 if imagem.getpixel((x, y))[:3] in (OURO, OURO_BRILHO))
    halo = sum(1 for y in range(ALTURA) for x in range(LARGURA)
               if imagem.getpixel((x, y))[:3] == OURO_BORDA)
    print("escrito:", destino)
    print("  opacos   : %d de %d (%.0f%% -- o resto e recorte de folha)"
          % (opacos, LARGURA * ALTURA, 100.0 * opacos / (LARGURA * ALTURA)))
    print("  ouro     : %d nucleo + %d halo = %.0f%% dos opacos, em %d manchas"
          % (nucleo, halo, 100.0 * (nucleo + halo) / opacos, len(MANCHAS)))
