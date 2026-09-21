#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Sintese AUTORAL dos sons de Ren -- estouro, loop e fechamento (ADR-007).

POR QUE ELES EXISTEM. O ouvido classifica o efeito ANTES do olho: se o som for
eletrico, a aura vira eletricidade mesmo que o render esteja certo. E Nen
generico nao e eletricidade. Por isso nao ha nada aqui acima de ~3 kHz com
energia significativa, nenhuma serra, nenhuma distorcao de seno e nenhum
transiente seco -- os quatro ingredientes que produzem "faisca eletrica".

O QUE ESTES TRES SONS PRECISAM DIZER:

  ren_burst    -- PRESSAO SENDO LIBERADA. Ataque rapido mas nao seco, corpo
                  grave, e uma cauda de ar que se abre. Toca uma vez, na janela
                  de 0 a 220 ms da transicao TEN->REN, junto do flash de borda.
  ren_loop     -- PRESENCA. Zumbido grave quase subliminar mais um movimento
                  fino de ar por cima. Volume e pitch sobem com o output
                  EFETIVO, no codigo -- aqui ele e gravado neutro.
  ren_release  -- O FECHAMENTO. Uma succao curta: o ar volta para dentro e o
                  grave cai. Toca ao SAIR de Ren.

O LOOP E EMENDAVEL POR CONSTRUCAO, e isso e a parte que da errado quando nao se
presta atencao. Todo componente do loop e periodico no comprimento do arquivo:
os tons usam frequencias multiplas de 1/duracao, as modulacoes usam harmonicos
INTEIROS do mesmo periodo, e o ruido e gerado no dominio da frequencia -- o que
o torna periodico por definicao, ja que `irfft` devolve exatamente um periodo.
Nenhum envelope e aplicado ao loop: um envelope e justamente o que abre a
costura.

O QUE ISSO NAO RESOLVE, e esta declarado: o Vorbis acrescenta amostras de
priming na codificacao, entao um clique residual na emenda e possivel. Ele nao
foi medido em jogo. Ver docs/testing/o-que-nao-provamos.md.

Uso:
    python art-source/sons/aura.py [pasta-de-destino]

Destino padrao:
    src/main/resources/assets/nenfoundation/sounds/vfx/nen/
"""

import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sintese as s  # noqa: E402


TAXA = 22050

# As sementes sao FIXAS e escritas aqui. Sem elas, duas execucoes produzem
# binarios diferentes -- e o custo nao e sonoro, e de historico: todo diff vira
# ruido e no dia em que um som mudar de verdade ninguem repara.
SEMENTE_ESTOURO = 4111
SEMENTE_LOOP = 4112
SEMENTE_FECHAMENTO = 4113


def _periodico(n, semente, cor):
    """Ruido colorido PERIODICO em `n` amostras.

    `irfft` de um espectro devolve exatamente um periodo do sinal, entao
    concatenar o resultado consigo mesmo nao produz descontinuidade. E isso que
    torna o loop emendavel sem crossfade -- e crossfade num loop de dois
    segundos e audivel como uma respiracao que nao existe.
    """
    rng = np.random.default_rng(semente)
    espectro = np.fft.rfft(rng.standard_normal(n))
    freq = np.fft.rfftfreq(n, 1.0 / TAXA)
    freq[0] = freq[1] if len(freq) > 1 else 1.0
    onda = np.fft.irfft(espectro * (freq ** (-cor)), n=n)
    return onda / (np.max(np.abs(onda)) or 1.0)


def estouro():
    """O estouro de pressao da ativacao de Ren.

    TRES CAMADAS COM PAPEIS DIFERENTES, e nenhuma delas sozinha serve:

      sub     -- 46 Hz caindo para 30. E o peso. Sozinho, e um baque de porta.
      corpo   -- harmonicos impares do sub, com desvio lento. E o que da a
                 sensacao de MASSA em movimento em vez de um golpe.
      ar      -- ruido escuro com abertura rapida. E o que diz "pressao", e nao
                 "impacto".

    O ATAQUE E DE 25 ms, e nao de 2. Um ataque instantaneo produz um CLIQUE, e
    clique le como percussao -- bateria, e nao energia. Vinte e cinco
    milissegundos e curto o bastante para continuar sendo um estouro.
    """
    duracao = 0.80
    n = int(duracao * TAXA)
    t = np.arange(n) / float(TAXA)

    p = np.linspace(0.0, 1.0, n) ** 1.5
    inst = 46.0 * (30.0 / 46.0) ** p
    fase = 2.0 * math.pi * np.cumsum(inst) / TAXA
    sub = np.sin(fase)
    corpo = 0.45 * np.sin(3.0 * fase) + 0.22 * np.sin(5.0 * fase)

    ar = _periodico(n, SEMENTE_ESTOURO, cor=1.15)
    # A cauda de ar ABRE e fecha; o grave so fecha. E a diferenca entre "algo
    # saiu daqui" e "algo bateu aqui".
    abertura = np.clip(t / 0.09, 0.0, 1.0) * np.exp(-3.1 * t)
    grave = np.exp(-4.4 * t) * np.clip(t / 0.025, 0.0, 1.0)

    return s.normalizar(1.00 * sub * grave
                        + 0.55 * corpo * grave
                        + 0.62 * ar * abertura)


def loop():
    """O zumbido de presenca enquanto Ren estiver ligado.

    DUAS ALTURAS, e nao uma. Um tom so le como aparelho ligado -- geladeira,
    transformador. Duas alturas proximas batendo entre si produzem um movimento
    lento que o ouvido le como algo VIVO. As duas sao multiplas exatas de
    1/duracao, senao a emenda estala.

    O MOVIMENTO FINO DE AR entra por cima em 18%: o suficiente para tirar a
    leitura de aparelho, pouco o bastante para nao virar chiado. Ele e ruido
    ESCURO; ruido claro aqui e o caminho mais curto para "eletricidade".

    GRAVADO NEUTRO. Volume e pitch sobem com o output efetivo no codigo, e nao
    aqui -- um loop ja gravado alto nao teria para onde crescer.
    """
    duracao = 2.4
    n = int(duracao * TAXA)
    t = np.arange(n) / float(TAXA)
    base = 1.0 / duracao  # a frequencia fundamental do LOOP, nao do som

    # 100 e 149 ciclos no arquivo: ~41.7 Hz e ~62.1 Hz. Nao formam intervalo
    # exato de proposito -- a leve desafinacao e o que produz o batimento.
    f1 = 100.0 * base
    f2 = 149.0 * base
    tom = (np.sin(2.0 * math.pi * f1 * t)
           + 0.62 * np.sin(2.0 * math.pi * f2 * t)
           + 0.18 * np.sin(2.0 * math.pi * 2.0 * f1 * t))

    # Modulacoes com harmonicos INTEIROS do periodo do arquivo: 1, 2 e 3 ciclos
    # ao longo do loop inteiro. Qualquer numero quebrado aqui abre a costura.
    respiracao = (1.0
                  + 0.16 * np.sin(2.0 * math.pi * 1.0 * base * t)
                  + 0.07 * np.sin(2.0 * math.pi * 3.0 * base * t + 1.3))

    ar = _periodico(n, SEMENTE_LOOP, cor=1.35)
    movimento = (1.0
                 + 0.35 * np.sin(2.0 * math.pi * 2.0 * base * t + 0.7)
                 + 0.15 * np.sin(2.0 * math.pi * 5.0 * base * t + 2.9))

    return s.normalizar(0.82 * tom * respiracao + 0.18 * ar * movimento)


def fechamento():
    """A succao curta ao sair de Ren.

    O DESENHO E O INVERSO DO ESTOURO, e isso e o ponto: no estouro o ar ABRE e o
    grave cai; aqui o ar FECHA -- entra devagar e colapsa -- e o grave desce
    junto. O ouvido reconhece a inversao mesmo sem conseguir nomea-la, e e ela
    que diz "acabou" em vez de "aconteceu de novo".

    CURTO: meio segundo. Um fechamento longo faz cada saida de Ren parecer um
    evento, e sair de Ren e a coisa mais comum que vai acontecer com ele.
    """
    duracao = 0.55
    n = int(duracao * TAXA)
    t = np.arange(n) / float(TAXA)

    p = np.linspace(0.0, 1.0, n) ** 0.8
    inst = 120.0 * (38.0 / 120.0) ** p
    fase = 2.0 * math.pi * np.cumsum(inst) / TAXA
    tom = np.sin(fase) + 0.3 * np.sin(2.0 * fase)

    ar = _periodico(n, SEMENTE_FECHAMENTO, cor=1.05)
    # A succao: sobe ate ~40% da duracao e colapsa. O expoente 2.2 na queda e o
    # que faz o fim parecer ENGOLIDO, e nao apenas silenciado.
    sobe = np.clip(t / (duracao * 0.40), 0.0, 1.0)
    cai = np.clip(1.0 - (t - duracao * 0.40) / (duracao * 0.60), 0.0, 1.0) ** 2.2
    succao = np.where(t < duracao * 0.40, sobe, cai)

    return s.normalizar(0.70 * tom * succao + 0.55 * ar * succao)


SONS = {
    'ren_burst.ogg': estouro,
    'ren_loop.ogg': loop,
    'ren_release.ogg': fechamento,
}


def main():
    padrao = os.path.join('src', 'main', 'resources', 'assets', 'nenfoundation',
                          'sounds', 'vfx', 'nen')
    destino = sys.argv[1] if len(sys.argv) > 1 else padrao
    for nome, gerar in SONS.items():
        caminho = os.path.join(destino, nome)
        s.escrever(caminho, gerar(), taxa=TAXA)
        print("%s  %.2fs  %d bytes" % (caminho, s.duracao_de(caminho),
                                       os.path.getsize(caminho)))


if __name__ == '__main__':
    main()
