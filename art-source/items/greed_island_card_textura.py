"""Textura autoral do card de Greed Island (16x16).

POR QUE AUTORAL e nao uma referencia a textura vanilla. Os itens anteriores deste
repositorio apontam para {@code minecraft:item/book} e similares, e isso e
legitimo -- referenciar nao e extrair (ADR-007). O card, porem, e o simbolo de um
sistema inteiro: reusar o livro faria o premio de Greed Island parecer uma nota
de campo, e a leitura mais barata do jogador ("isto e outro livro") apagaria a
diferenca entre observar e CAPTURAR.

Deterministico: nenhuma chamada a random sem semente, nenhum timestamp. Rodar
duas vezes produz bytes identicos -- a PNG e binario versionado, e sem isso todo
diff vira ruido e a mudanca de verdade passa despercebida.

Regerar:  python art-source/items/greed_island_card_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/item/greed_island_card.png
"""
import os

from PIL import Image

LARGURA = ALTURA = 16
DESTINO = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "textures", "item", "greed_island_card.png")

# A moldura escura e o que faz o card LER como card a 16 px: sem ela a silhueta
# se mistura com o fundo do inventario e o item vira um borrao claro.
BORDA = (28, 26, 40, 255)
PAPEL = (222, 214, 190, 255)
PAPEL_SOMBRA = (196, 186, 158, 255)
SELO = (58, 104, 128, 255)
SELO_CLARO = (96, 158, 184, 255)
DOURADO = (198, 160, 74, 255)


def ruido(x, y):
    """Hash da coordenada, embaralhado.

    Combinacao linear simples produz faixas diagonais regulares, que a 16 px
    leem como listra e nao como fibra de papel -- e isso so aparece na tela.
    """
    h = (x * 374761393 + y * 668265263) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return (h ^ (h >> 16)) & 0xFF


def desenhar():
    img = Image.new("RGBA", (LARGURA, ALTURA), (0, 0, 0, 0))
    px = img.load()

    # Corpo do card: retangulo de 10x14 centrado, com moldura de 1 px.
    x0, x1 = 3, 12
    y0, y1 = 1, 14
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            borda = x in (x0, x1) or y in (y0, y1)
            if borda:
                px[x, y] = BORDA
            else:
                px[x, y] = PAPEL if ruido(x, y) > 96 else PAPEL_SOMBRA

    # Selo central: o losango que marca a conversao. Ele e o unico contraste alto
    # da folha -- dois contrastes altos a 16 px competem e nenhum vence.
    centro_x, centro_y = 7, 7
    for dy in range(-3, 4):
        for dx in range(-3, 4):
            if abs(dx) + abs(dy) <= 3:
                px[centro_x + dx, centro_y + dy] = SELO
            if abs(dx) + abs(dy) <= 1:
                px[centro_x + dx, centro_y + dy] = SELO_CLARO

    # Cantos dourados: a leitura de "carta de colecao" a distancia.
    for x, y in ((x0 + 1, y0 + 1), (x1 - 1, y0 + 1), (x0 + 1, y1 - 1), (x1 - 1, y1 - 1)):
        px[x, y] = DOURADO
    return img


def validar(img):
    """Duas reguas que o olho nao pega num icone de 16 px."""
    px = img.load()
    opacos = sum(1 for y in range(ALTURA) for x in range(LARGURA) if px[x, y][3] > 0)
    if opacos < 100:
        raise ValueError("so %d pixels opacos: o card ficaria quase invisivel no inventario "
                         "e ninguem reportaria isso como bug" % opacos)
    if px[0, 0][3] != 0:
        raise ValueError("o canto (0,0) ficou opaco: a silhueta encostaria na borda do slot "
                         "e o item pareceria cortado")
    return img


if __name__ == "__main__":
    imagem = validar(desenhar())
    os.makedirs(os.path.dirname(DESTINO), exist_ok=True)
    imagem.save(DESTINO)
    print("escrito %s (%dx%d)" % (DESTINO, LARGURA, ALTURA))
