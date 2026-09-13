#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Gera aura_ribbon_core.png -- 32x128 RGBA, autoral (ADR-007).

Centro branco, borda em fade horizontal, leve variacao ao longo do
comprimento. Tileavel em Y: toda modulacao longitudinal usa harmonicos
INTEIROS de 2*pi*y/128, entao a linha 127 encosta na linha 0 sem costura --
requisito porque a ribbon rola a coordenada V ao longo do tempo.

Sem PIL. Apenas zlib, struct e math (todos da biblioteca padrao).

Uso:
    python gerar_aura_ribbon_core.py [destino.png]

Destino padrao: ./aura_ribbon_core.png
No repositorio, ele vai para
src/main/resources/assets/nenfoundation/textures/vfx/nen/aura_ribbon_core.png
"""

import math
import struct
import sys
import zlib

LARGURA = 32
ALTURA = 128

# Paleta: o nucleo da ribbon e o ponto MAIS BRILHANTE da aura inteira
# (hierarquia de brilho de direcao-visual-da-aura.md secao 3). A borda puxa
# para o azul-claro da paleta (0.82, 0.92, 1.00 -> ~209,235,255), e um pouco
# alem, porque a borda da ribbon e o que some contra a neve se for branco puro.
COR_CENTRO = (255, 255, 255)
COR_BORDA = (176, 210, 255)

# Margem em pixels onde o alpha e forcado a zero nas duas colunas extremas.
# Sem ela, o filtro linear amostra a borda da textura contra o lado oposto
# (GL_REPEAT em U) e a ribbon ganha uma linha clara fantasma na lateral.
MARGEM_PX = 2.5


def suave(x):
    """smoothstep classico, em [0,1]."""
    x = max(0.0, min(1.0, x))
    return x * x * (3.0 - 2.0 * x)


def centro_do_nucleo(y):
    """Deriva lateral do nucleo ao longo do comprimento, em [0,1].

    E o que impede a ribbon de ser um TUBO NEON perfeitamente reto -- um dos
    modos de falha listados no documento de direcao visual. A amplitude e
    pequena de proposito: e uma ondulacao de filamento, nao um zigue-zague de
    raio eletrico.
    """
    t = 2.0 * math.pi * y / ALTURA
    return 0.5 + 0.055 * math.sin(t + 0.40) + 0.022 * math.sin(3.0 * t + 2.10)


def largura_relativa(y):
    """Modulacao da espessura do nucleo ao longo do comprimento."""
    t = 2.0 * math.pi * y / ALTURA
    return 1.0 + 0.22 * math.sin(2.0 * t + 1.10) + 0.10 * math.sin(5.0 * t + 4.00)


def ganho_longitudinal(y):
    """Variacao lenta de intensidade ao longo do comprimento."""
    t = 2.0 * math.pi * y / ALTURA
    lento = 0.5 + 0.5 * math.sin(2.0 * t + 0.60)
    fino = 0.5 + 0.5 * math.sin(7.0 * t + 1.90)
    return 0.80 + 0.15 * lento + 0.05 * fino


def pixel(x, y):
    """Um pixel RGBA da textura."""
    u = (x + 0.5) / LARGURA
    meia = 0.5 * largura_relativa(y)
    d = abs(u - centro_do_nucleo(y)) / meia
    d = max(0.0, min(1.0, d))

    # Duas contribuicoes com papeis diferentes, como a shell:
    #   corpo   -- largo e macio, da o volume do filamento
    #   nucleo  -- estreito e quente, da a linha clara do meio
    # Uma so das duas produz ou um borrao (fumaca) ou um traco duro (raio).
    corpo = (1.0 - d) ** 2.4
    nucleo = math.exp(-((d / 0.16) ** 2))

    alpha = 0.88 * corpo + 0.78 * nucleo
    alpha *= ganho_longitudinal(y)
    # Zera nas colunas extremas: ver MARGEM_PX.
    alpha *= suave(min(x, LARGURA - 1 - x) / MARGEM_PX)
    alpha = max(0.0, min(1.0, alpha))

    # A COR NUNCA VAI A PRETO, nem onde o alpha e zero. Alpha nao e
    # pre-multiplicado: o filtro linear mistura RGB ignorando A, e um RGB preto
    # na borda escureceria a lateral da ribbon -- defeito que so aparece de
    # perto e que ninguem associa a textura.
    t = d ** 1.3
    cor = tuple(
        int(round(COR_CENTRO[i] + (COR_BORDA[i] - COR_CENTRO[i]) * t))
        for i in range(3)
    )
    return cor[0], cor[1], cor[2], int(round(alpha * 255.0))


def bloco(tipo, dados):
    return (struct.pack(">I", len(dados)) + tipo + dados
            + struct.pack(">I", zlib.crc32(tipo + dados) & 0xFFFFFFFF))


def gerar():
    linhas = bytearray()
    for y in range(ALTURA):
        linhas.append(0)  # filtro None
        for x in range(LARGURA):
            linhas.extend(pixel(x, y))

    ihdr = struct.pack(">IIBBBBB", LARGURA, ALTURA, 8, 6, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n"
            + bloco(b"IHDR", ihdr)
            + bloco(b"IDAT", zlib.compress(bytes(linhas), 9))
            + bloco(b"IEND", b""))


def main():
    destino = sys.argv[1] if len(sys.argv) > 1 else "aura_ribbon_core.png"
    png = gerar()
    with open(destino, "wb") as arquivo:
        arquivo.write(png)
    print("%s  %dx%d RGBA  %d bytes" % (destino, LARGURA, ALTURA, len(png)))


if __name__ == "__main__":
    main()
