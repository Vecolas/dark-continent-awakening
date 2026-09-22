"""Textura autoral do anel de Greed Island (16x16).

POR QUE AUTORAL. O anel e o item que ABRE a ilha, e ele nasce do mesmo principio
do card: referenciar arte vanilla e legitimo, extrair arte da obra nao e
(ADR-007). Este arquivo desenha o anel pixel a pixel a partir de uma descricao
-- aro de metal dourado com uma pedra verde --, e nao copia imagem nenhuma.

O QUE O CANONE DIZ, e o que e escolha nossa. Canone: todo jogador PRECISA do
anel para entrar em Greed Island, e e ele que habilita as palavras-chave do
jogo. A aparencia detalhada nao e descrita em texto que se possa citar com
seguranca; aro dourado e pedra verde vem do material de divulgacao e de
merchandising, e por isso estao registrados aqui como ESCOLHA DE ARTE e nao como
fidelidade canonica.

A PEDRA E O QUE FAZ O ANEL LER COMO ANEL a 16 px. Um aro sozinho vira um "O"
cinzento e se perde entre uma argola e uma rosquinha no inventario; o ponto de
cor quebra a simetria e da frente ao item. Foi a primeira coisa validada abaixo.

Deterministico: nenhuma chamada a random sem semente, nenhum timestamp. Rodar
duas vezes produz bytes identicos -- a PNG e binario versionado, e sem isso todo
diff vira ruido e a mudanca de verdade passa despercebida.

Regerar:  python art-source/items/greed_island_ring_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/item/greed_island_ring.png
"""
import os

from PIL import Image

LARGURA = ALTURA = 16
DESTINO = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "textures", "item", "greed_island_ring.png")

# A borda escura existe pelo mesmo motivo do card: sem ela o dourado encosta no
# fundo claro do inventario e a silhueta some.
BORDA = (40, 30, 14, 255)
OURO = (214, 172, 72, 255)
OURO_CLARO = (246, 218, 130, 255)
OURO_SOMBRA = (158, 120, 44, 255)
PEDRA = (46, 150, 92, 255)
PEDRA_CLARA = (128, 226, 160, 255)
PEDRA_SOMBRA = (22, 92, 58, 255)

# Centro e raios do aro, em px. O aro e ELIPTICO (mais largo que alto) de
# proposito: um circulo perfeito a 16 px le como moeda vista de frente, e o
# anel precisa parecer estar de pe.
CENTRO_X, CENTRO_Y = 8, 9
RAIO_X, RAIO_Y = 5.0, 4.2
ESPESSURA = 1.5


def _distancia_eliptica(x, y):
    """1.0 na linha do aro; <1 dentro do furo; >1 fora do metal."""
    dx = (x + 0.5 - CENTRO_X) / RAIO_X
    dy = (y + 0.5 - CENTRO_Y) / RAIO_Y
    return (dx * dx + dy * dy) ** 0.5


def desenhar():
    img = Image.new("RGBA", (LARGURA, ALTURA), (0, 0, 0, 0))
    px = img.load()

    meia = ESPESSURA / (2.0 * RAIO_X)
    for y in range(ALTURA):
        for x in range(LARGURA):
            d = _distancia_eliptica(x, y)
            if d > 1.0 + meia * 2.2 or d < 1.0 - meia * 2.2:
                continue
            if d > 1.0 + meia or d < 1.0 - meia:
                px[x, y] = BORDA
            elif y < CENTRO_Y:
                # A luz vem de cima-esquerda, como no resto dos itens do mod.
                px[x, y] = OURO_CLARO if x <= CENTRO_X else OURO
            else:
                px[x, y] = OURO_SOMBRA if x > CENTRO_X else OURO

    # A pedra, montada no topo do aro. Ela INVADE o metal de proposito: uma pedra
    # pousada ao lado leria como duas coisas soltas.
    topo_y = 2
    for dy in range(0, 4):
        for dx in range(-2, 3):
            x, y = CENTRO_X + dx, topo_y + dy
            if abs(dx) + abs(dy - 1) > 2:
                continue
            if abs(dx) + abs(dy - 1) == 2:
                px[x, y] = PEDRA_SOMBRA
            else:
                px[x, y] = PEDRA
    # UM brilho, e um so. Dois pontos especulares a 16 px competem e a pedra
    # perde o volume em vez de ganhar.
    px[CENTRO_X - 1, topo_y + 1] = PEDRA_CLARA
    return img


def validar(img):
    """Quatro reguas que o olho nao pega num icone de 16 px."""
    px = img.load()
    opacos = [(x, y) for y in range(ALTURA) for x in range(LARGURA) if px[x, y][3] > 0]
    if len(opacos) < 40:
        raise ValueError("so %d pixels opacos: o anel ficaria quase invisivel no inventario "
                         "e ninguem reportaria isso como bug" % len(opacos))

    # O FURO precisa existir, senao o anel vira moeda -- e uma moeda dourada com
    # uma pedra em cima nao le como anel em lugar nenhum.
    furo = sum(1 for y in range(ALTURA) for x in range(LARGURA)
               if px[x, y][3] == 0 and _distancia_eliptica(x, y) < 0.6)
    if furo < 8:
        raise ValueError("o furo do aro tem so %d px vazios: sem furo visivel a 16 px o item "
                         "le como moeda, nao como anel" % furo)

    # A PEDRA e o unico ponto de cor; sem ela o item vira um "O" dourado.
    verdes = sum(1 for _x, _y in opacos if px[_x, _y] in (PEDRA, PEDRA_CLARA, PEDRA_SOMBRA))
    if verdes < 6:
        raise ValueError("so %d px de pedra: e ela que da FRENTE ao anel e o separa de uma "
                         "argola qualquer" % verdes)

    for canto in ((0, 0), (15, 0), (0, 15), (15, 15)):
        if px[canto][3] != 0:
            raise ValueError("o canto %s ficou opaco: a silhueta encostaria na borda do slot "
                             "e o item pareceria cortado" % (canto,))
    return img


if __name__ == "__main__":
    imagem = validar(desenhar())
    os.makedirs(os.path.dirname(DESTINO), exist_ok=True)
    imagem.save(DESTINO)
    print("escrito %s (%dx%d)" % (DESTINO, LARGURA, ALTURA))
