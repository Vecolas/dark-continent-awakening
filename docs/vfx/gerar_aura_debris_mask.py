#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Gera aura_debris_mask.png -- 16x16 RGBA, autoral (ADR-007).

A mascara de um FRAGMENTO levantado pela pressao de Ren. Ela e propositalmente
oposta a mascara de faisca: a faisca e um ponto macio que brilha; o fragmento e
uma lasca com CANTOS, porque ele precisa ser lido como materia arrancada do chao
-- e nao como mais uma particula de aura.

O sprite e minusculo em tela (0,02 a 0,08 bloco), entao qualquer detalhe interno
se perde. O que sobrevive a essa escala e a SILHUETA, e e nela que esta o
desenho: um quadrilatero irregular, levemente girado, com uma face mais clara e
duas mais escuras. Isso da a leitura de volume sem nenhum sombreamento de
verdade.

A cor sai BRANCA de proposito. Quem pinta o fragmento e o codigo, com a cor do
bloco sob os pes do jogador (ver SondagemDeChao) -- e uma mascara ja colorida
multiplicaria duas cores e deixaria todo detrito com um tom esverdeado que
ninguem consegue explicar.

Deterministico: nenhuma chamada a random. A forma vem de constantes escritas a
mao, e regerar o arquivo nao produz diff.

Sem PIL. Apenas zlib, struct e math (todos da biblioteca padrao).

Uso:
    python gerar_aura_debris_mask.py [destino.png]

No repositorio, ele vai para
src/main/resources/assets/nenfoundation/textures/particle/aura_debris.png
"""

import struct
import sys
import zlib

LADO = 16

# Os quatro cantos da lasca, em coordenada de textura [0,1]. Escritos a mao e
# propositalmente assimetricos: um quadrilatero regular le como "cubo de
# Minecraft em miniatura", que e a leitura errada -- o fragmento e um caco, e
# nao um bloco pequeno.
CANTOS = [
    (0.20, 0.30),
    (0.78, 0.18),
    (0.86, 0.72),
    (0.30, 0.84),
]

# A face clara e a metade superior-esquerda; as outras duas escurecem. Nao e
# iluminacao de verdade -- e o minimo que faz o olho ler tres dimensoes numa
# silhueta de seis pixels.
CLARO = 255
MEIO = 205
ESCURO = 150


def dentro(px, py):
    """Se o ponto esta dentro do quadrilatero.

    Teste de lado para cada aresta, com os cantos em ordem horaria. Um ponto
    dentro fica do mesmo lado de todas as quatro.
    """
    sinal = None
    for i in range(len(CANTOS)):
        ax, ay = CANTOS[i]
        bx, by = CANTOS[(i + 1) % len(CANTOS)]
        cruz = (bx - ax) * (py - ay) - (by - ay) * (px - ax)
        if abs(cruz) < 1e-9:
            continue
        atual = cruz > 0
        if sinal is None:
            sinal = atual
        elif sinal != atual:
            return False
    return True


def cobertura(x, y):
    """Fracao do pixel coberta, por amostragem 4x4.

    ANTI-ALIASING POR SUPERAMOSTRAGEM, e nao um filtro depois. Com 16 pixels de
    lado, uma borda dura perde a inclinacao das arestas -- e sem inclinacao a
    lasca vira um retangulo, que e exatamente a leitura de "bloco pequeno" que
    este desenho evita.
    """
    dentro_de = 0
    for sy in range(4):
        for sx in range(4):
            px = (x + (sx + 0.5) / 4.0) / LADO
            py = (y + (sy + 0.5) / 4.0) / LADO
            if dentro(px, py):
                dentro_de += 1
    return dentro_de / 16.0


def tom(x, y):
    """O cinza deste pixel: uma face clara, duas escuras."""
    px = (x + 0.5) / LADO
    py = (y + 0.5) / LADO
    # A diagonal separa a face iluminada da sombreada. O terceiro tom entra no
    # canto inferior direito, que e a face mais afastada da luz imaginaria.
    if px + py < 0.85:
        return CLARO
    if px + py > 1.35:
        return ESCURO
    return MEIO


def bloco(tipo, dados):
    return (struct.pack(">I", len(dados)) + tipo + dados
            + struct.pack(">I", zlib.crc32(tipo + dados) & 0xFFFFFFFF))


def gerar():
    linhas = bytearray()
    for y in range(LADO):
        linhas.append(0)  # filtro None
        for x in range(LADO):
            a = cobertura(x, y)
            c = tom(x, y)
            # A COR NAO VAI A PRETO onde o alpha e zero: alpha nao e
            # pre-multiplicado, e o filtro do atlas mistura RGB ignorando A.
            linhas.extend((c, c, c, int(round(a * 255.0))))

    ihdr = struct.pack(">IIBBBBB", LADO, LADO, 8, 6, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n"
            + bloco(b"IHDR", ihdr)
            + bloco(b"IDAT", zlib.compress(bytes(linhas), 9))
            + bloco(b"IEND", b""))


def main():
    destino = sys.argv[1] if len(sys.argv) > 1 else "aura_debris.png"
    png = gerar()
    with open(destino, "wb") as arquivo:
        arquivo.write(png)
    print("%s  %dx%d RGBA  %d bytes" % (destino, LADO, LADO, len(png)))


if __name__ == "__main__":
    main()
