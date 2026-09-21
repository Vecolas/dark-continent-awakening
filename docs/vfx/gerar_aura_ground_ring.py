#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Gera aura_ground_ring.png -- 128x128 RGBA, autoral (ADR-007).

O anel de pressao de Ren. A textura NAO e um circulo: ela e a FAIXA do anel
esticada num retangulo. O eixo X e a volta inteira (0 a 360 graus) e o eixo Y
atravessa a faixa, do raio interno (y=0) ao externo (y=127).

Tileavel em X, e isso e requisito e nao capricho: o anel fecha sobre si mesmo, e
qualquer descontinuidade entre a coluna 127 e a coluna 0 apareceria como uma
COSTURA -- a unica descontinuidade da forma, e por isso a primeira coisa que o
olho encontra. Toda modulacao longitudinal usa harmonicos INTEIROS de
2*pi*x/128.

ARCO IRREGULAR, SEM SIMBOLO E SEM RUNA. Isto e pressao de energia, nao magia: a
textura nao desenha glifo, pentagrama, relogio nem qualquer forma legivel. Os
cortes ao longo de X sao o que impede o anel de ser lido como marca de
invocacao; a modulacao de raio no renderer faz o resto.

Sem PIL. Apenas zlib, struct e math (todos da biblioteca padrao).

Uso:
    python gerar_aura_ground_ring.py [destino.png]

No repositorio, ele vai para
src/main/resources/assets/nenfoundation/textures/vfx/nen/aura_ground_ring.png
"""

import math
import struct
import sys
import zlib

LADO = 128

# A cor da faixa puxa para o mesmo azul-claro da ribbon, e a crista central e
# quase branca. A hierarquia de brilho manda: nucleo de ribbon > borda da shell
# > shell > halo > CHAO. O anel e o penultimo, entao ele nunca chega a branco
# puro -- se chegasse, competiria com a borda, que e quem carrega a leitura.
COR_CRISTA = (236, 246, 255)
COR_BORDA = (150, 192, 255)


def suave(x):
    """smoothstep classico, em [0,1]."""
    x = max(0.0, min(1.0, x))
    return x * x * (3.0 - 2.0 * x)


def presenca(x):
    """Quanto do anel existe nesta posicao da volta, em [0,1].

    E o que transforma um anel continuo num ARCO IRREGULAR. Tres harmonicos
    inteiros com fases diferentes produzem trechos fortes e trechos quase
    ausentes sem nenhum sorteio -- e sem sorteio nao ha costura.

    O piso de 0.18 existe de proposito: um anel que some por completo em algum
    angulo deixa de ser um anel e vira duas manchas soltas no chao.
    """
    t = 2.0 * math.pi * x / LADO
    a = 0.5 + 0.5 * math.sin(2.0 * t + 0.7)
    b = 0.5 + 0.5 * math.sin(5.0 * t + 2.3)
    c = 0.5 + 0.5 * math.sin(9.0 * t + 4.9)
    bruto = 0.52 * a + 0.31 * b + 0.17 * c
    return 0.18 + 0.82 * suave(bruto)


def centro_da_crista(x):
    """Onde a linha clara da faixa passa, em [0,1] no eixo Y.

    Ela NAO fica no meio exato. Uma crista perfeitamente centrada le como uma
    listra desenhada com regua; deslocada devagar ao longo da volta, ela le como
    energia que se acumula de um lado.
    """
    t = 2.0 * math.pi * x / LADO
    return 0.52 + 0.10 * math.sin(3.0 * t + 1.1) + 0.04 * math.sin(7.0 * t + 3.4)


def largura_da_crista(x):
    """Espessura relativa da crista ao longo da volta."""
    t = 2.0 * math.pi * x / LADO
    return 1.0 + 0.30 * math.sin(4.0 * t + 0.3) + 0.12 * math.sin(11.0 * t + 2.0)


def pixel(x, y):
    """Um pixel RGBA da faixa."""
    v = (y + 0.5) / LADO

    # A FAIXA MORRE NAS DUAS PONTAS. O raio interno some contra o corpo do
    # jogador e o externo se dissolve no chao; sem isso o anel teria duas bordas
    # cortadas com tesoura, que e a leitura de decalque colado.
    envelope = suave(min(v, 1.0 - v) / 0.22)

    d = abs(v - centro_da_crista(x)) / (0.30 * largura_da_crista(x))
    d = max(0.0, min(2.5, d))

    # Duas contribuicoes com papeis diferentes, como na shell e na ribbon:
    #   corpo  -- largo e fraco, da a presenca da faixa
    #   crista -- estreito e claro, da a linha do anel
    corpo = math.exp(-((d / 1.25) ** 2)) * 0.42
    crista = math.exp(-((d / 0.34) ** 2)) * 0.80

    alpha = (corpo + crista) * envelope * presenca(x)
    alpha = max(0.0, min(1.0, alpha))

    # A COR NUNCA VAI A PRETO, nem onde o alpha e zero: o alpha nao e
    # pre-multiplicado, e o filtro linear mistura RGB ignorando A. Um RGB preto
    # na borda escureceria a lateral do anel -- defeito que so aparece de perto.
    t = min(1.0, d / 1.6) ** 1.2
    cor = tuple(
        int(round(COR_CRISTA[i] + (COR_BORDA[i] - COR_CRISTA[i]) * t))
        for i in range(3)
    )
    return cor[0], cor[1], cor[2], int(round(alpha * 255.0))


def bloco(tipo, dados):
    return (struct.pack(">I", len(dados)) + tipo + dados
            + struct.pack(">I", zlib.crc32(tipo + dados) & 0xFFFFFFFF))


def gerar():
    linhas = bytearray()
    for y in range(LADO):
        linhas.append(0)  # filtro None
        for x in range(LADO):
            linhas.extend(pixel(x, y))

    ihdr = struct.pack(">IIBBBBB", LADO, LADO, 8, 6, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n"
            + bloco(b"IHDR", ihdr)
            + bloco(b"IDAT", zlib.compress(bytes(linhas), 9))
            + bloco(b"IEND", b""))


def main():
    destino = sys.argv[1] if len(sys.argv) > 1 else "aura_ground_ring.png"
    png = gerar()
    with open(destino, "wb") as arquivo:
        arquivo.write(png)
    print("%s  %dx%d RGBA  %d bytes" % (destino, LADO, LADO, len(png)))


if __name__ == "__main__":
    main()
