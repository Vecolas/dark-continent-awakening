"""Folha do Bubble Horse -- pelagem apagada de proposito, e bolhas que gritam.

A DECISAO DE LEITURA DESTA FOLHA CABE NUMA FRASE: **a pelagem e fundo, as bolhas
sao a figura.** O corpo inteiro vive numa faixa estreita de cinza-lilas de
luminancia media-baixa; as bolhas saem dessa faixa para cima, e so elas.

Isso nao e gosto, e sao duas regras do servidor que dependem disso:

  * A LOCOMOCAO E SALTO, E NAO PASSADA. O que o jogador precisa seguir com o olho
    sao as bolhas -- nos frames de voo elas sao a unica coisa cuja forma muda
    (elas esticam e achatam). Uma bolha que se confunde com o pelo a dez blocos
    faz o salto ficar imprevisivel, e prever o salto e a resposta que este mob
    ensina: perseguir nao funciona.
  * O CARD SO SAI VIVO. A morte colapsa as bolhas, e esse colapso e o recibo da
    perda. Se as bolhas nao forem o que o olho acha primeiro, o recibo chega e
    ninguem le.

`valida_bolha_se_separa_do_pelo` mede as duas pontas disso: a diferenca de
luminancia media entre a bolha mais escura e a peca de pelagem mais clara, e o
vazamento de qualquer tom de bolha para fora das bolhas.

NAO HA ALVO PINTADO, e a ausencia e deliberada. Este bicho nao tem
`WeakPointResolver`: nao ha regiao que pague multiplicador, porque a captura dele
e por ENFRAQUECIMENTO e nao por abate -- um alvo desenhado ensinaria o jogador a
acertar mais forte, que e exatamente o que perde o card. Marca de ponto fraco em
mob sem ponto fraco nao da erro nenhum: da um jogador mirando com capricho e
matando o premio.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

Regerar:  python art-source/enemies/bubble_horse/bubble_horse_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/bubble_horse/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import geometria as geo      # noqa: E402
from comum import textura as tex        # noqa: E402
from bubble_horse_geo import (BOLHAS, CAIXAS, MOB, PELAGEM,  # noqa: E402
                              UV_ALTURA, UV_LARGURA)

# Degrau MINIMO de luminancia media entre a bolha mais apagada e a peca de
# pelagem mais clara.
#
# 35 e o que se enxerga a dez blocos num bicho de 1.2 bloco: com 15 as duas
# regioes viram a mesma mancha quando o mip-map entra, e com 80 a pelagem teria
# de ser quase preta -- e um cavalo preto com bolhas brancas le como outro bicho,
# nao como este. O numero e um limite de LEITURA, nao de balanceamento: ninguem
# vai gira-lo numa sessao de ajuste, e por isso ele mora aqui, ao lado do
# comentario que o explica.
DEGRAU_MINIMO_DE_LUMINANCIA = 35.0

PALETA = tex.Paleta(
    PELO=(146, 142, 158),        # o flanco: cinza-lilas apagado, de proposito
    PELO_SOMBRA=(108, 104, 122),  # ventre, traseira e o que fica por baixo
    PELO_LUZ=(178, 176, 190),    # o que pega luz de cima
    CRINA=(74, 78, 108),         # crina, cauda e a linha do dorso
    FOCINHO=(96, 92, 108),       # focinho e narina
    OLHO=(36, 38, 54),           # o olho -- escuro, pequeno e SEM anel em volta
    # --- os tres tons abaixo so podem aparecer dentro das bolhas ---
    BOLHA=(176, 222, 240),       # o corpo da bolha
    BOLHA_LUZ=(232, 250, 254),   # o brilho no alto: o ponto mais claro da folha
    BOLHA_SOMBRA=(128, 186, 218),  # a barriga da bolha, onde ela encosta
)

# Os tons que a regua prende dentro das bolhas. Declarados uma vez, e nao
# repetidos na validacao: duas listas divergem, e a divergencia faria a regua
# deixar de vigiar justamente o tom que alguem acabou de espalhar.
TONS_DE_BOLHA = ("BOLHA", "BOLHA_LUZ", "BOLHA_SOMBRA")

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def _pelagem(p, salpico):
    """O tratamento comum de toda peca de carne.

    Topo claro, base e traseira escuras: e a sombra que um corpo claro precisa
    para nao ler como um bloco de cor chapada visto de cima. `salpico` e a
    semente -- semente repetida entre duas pecas faria as duas receberem o MESMO
    mosqueado, e um padrao repetido le como textura de parede, nao como pelo.
    """
    p.tudo(PALETA.PELO)
    p.face("topo", PALETA.PELO_LUZ)
    p.face("base", PALETA.PELO_SOMBRA)
    p.face("tras", PALETA.PELO_SOMBRA)
    for face in tex.PAREDES:
        p.salpicar(face, PALETA.PELO_SOMBRA, 44, semente=salpico)
        p.salpicar(face, PALETA.PELO_LUZ, 26, semente=salpico + 7)


def _bolha(p, semente):
    """Toda bolha se pinta igual, e isso e o ponto.

    Seis bolhas com tratamentos diferentes leriam como seis materiais; o jogador
    precisa entender na primeira olhada que a coisa redonda no pe e a mesma coisa
    redonda na garupa. O que varia entre elas e so o mosqueado, que vem da
    semente.
    """
    p.tudo(PALETA.BOLHA)
    # O brilho ocupa o topo inteiro: e ele que sobrevive ao mip-map quando o
    # jogador esta longe, e longe e onde a leitura precisa funcionar.
    p.face("topo", PALETA.BOLHA_LUZ)
    p.face("base", PALETA.BOLHA_SOMBRA)
    for face in tex.PAREDES:
        # A barriga escura de cada parede: sem ela a bolha le como cubo de gelo.
        p.faixa_no_pe(face, 1, PALETA.BOLHA_SOMBRA)
        # O reflexo alto, deslocado para um lado so. Centrado, ele leria como um
        # olho -- e este bicho nao pode ter nada que pareca um alvo.
        p.na_face(face, 1, 1, 2, 1, PALETA.BOLHA_LUZ, forca=0)
        p.salpicar(face, PALETA.BOLHA_SOMBRA, 30, semente=semente)


def pintar():
    # -------------------------------------------------------------- tronco
    tronco = folha["body"]
    _pelagem(tronco, 3)
    # A linha do dorso, em CRINA: ela e a unica marca escura do corpo e existe
    # para o tronco ter direcao legivel de cima, que e de onde o jogador ve o
    # bicho quando ele salta para longe.
    tronco.linha_central("topo", 1, PALETA.CRINA)

    # ------------------------------------------------------- pescoco e crina
    pescoco = folha["neck"]
    _pelagem(pescoco, 11)
    # A crina cai pela NUCA (face 'tras'), e nao pelo peito: pintada na frente ela
    # ficaria escondida atras da bolha da nuca em todo angulo util.
    pescoco.face("tras", PALETA.CRINA)
    pescoco.faixa_no_topo("direita", 1, PALETA.CRINA)
    pescoco.faixa_no_topo("esquerda", 1, PALETA.CRINA)

    # -------------------------------------------------------------- cabeca
    cabeca = folha["head"]
    _pelagem(cabeca, 19)
    # O focinho ocupa o PE da face da frente -- a frente da cabeca e a cara.
    cabeca.faixa_no_pe("frente", 2, PALETA.FOCINHO, forca=2)
    cabeca.ponto("frente", 1, 4, PALETA.OLHO)
    cabeca.ponto("frente", 3, 4, PALETA.OLHO)
    # Os olhos de verdade ficam nos LADOS: cavalo tem olho lateral, e e disso que
    # depende a leitura de "ele esta me vendo mesmo de lado".
    for face in tex.LADOS:
        cabeca.na_face(face, 1, 1, 2, 2, PALETA.OLHO, forca=0)
    # A testa escura, em CRINA, fechando a crina que vem do pescoco.
    cabeca.faixa_no_topo("tras", 2, PALETA.CRINA)

    # --------------------------------------------------------------- cauda
    cauda = folha["tail"]
    cauda.tudo(PALETA.CRINA)
    cauda.face("topo", PALETA.PELO_SOMBRA)
    for face in tex.PAREDES:
        cauda.salpicar(face, PALETA.PELO_SOMBRA, 60, semente=29)

    # -------------------------------------------------------------- pernas
    # Pernas mais ESCURAS que o tronco: elas sao finas, e uma perna clara sobre
    # fundo claro desaparece no quadro em que ela dobra -- que e o quadro que
    # anuncia o proximo salto.
    for nome, semente in (("leg_front_left", 37), ("leg_front_right", 41),
                          ("leg_back_left", 43), ("leg_back_right", 47)):
        perna = folha[nome]
        perna.tudo(PALETA.PELO_SOMBRA)
        perna.face("topo", PALETA.PELO)
        for face in tex.PAREDES:
            perna.salpicar(face, PALETA.CRINA, 34, semente=semente)

    # -------------------------------------------------------------- bolhas
    for i, nome in enumerate(BOLHAS):
        _bolha(folha[nome], 53 + i * 7)


# ------------------------------------------ validacoes que ligam arte e regra

def _luminancia(pixel):
    """Luminancia perceptual, a mesma conta que o olho humano faz de longe.

    Media simples dos canais mediria ERRADO: (0,255,0) e (255,0,0) tem a mesma
    media e brilhos completamente diferentes, e a regua aprovaria um verde
    berrante como se fosse tao discreto quanto um vermelho escuro.
    """
    return 0.299 * pixel[0] + 0.587 * pixel[1] + 0.114 * pixel[2]


def _pixels_das_faces(f, nome):
    """So os pixels das SEIS faces da caixa -- nunca os cantos do layout.

    O retangulo de uma caixa tem cantos que nenhuma face usa e que ficam
    transparentes de proposito. Medi-los faria toda media puxar para o preto, e a
    regua de separacao aprovaria qualquer coisa.
    """
    pincel = f[nome]
    for face in tex.FACES:
        x, y, largura, altura = pincel.faces()[face]
        for i in range(x, x + largura):
            for j in range(y, y + altura):
                yield f.px[i, j][:3]


def _media_de_luminancia(f, nome):
    valores = [_luminancia(p) for p in _pixels_das_faces(f, nome)]
    return sum(valores) / len(valores)


def valida_bolha_se_separa_do_pelo(f):
    """As bolhas tem de ser a FIGURA, e a pelagem o fundo -- e isso se mede.

    Duas coisas sao cobradas, e as duas quebram a mesma promessa:

    1. DEGRAU -- a bolha mais apagada tem de ficar DEGRAU_MINIMO_DE_LUMINANCIA
       acima da peca de pelagem mais clara. Empatadas, as duas regioes viram uma
       mancha so quando o mip-map entra, e o jogador perde as bolhas de vista
       exatamente nos frames de voo -- que sao os frames em que ele esta lendo
       para onde o salto vai. Perder o salto de vista transforma a resposta certa
       (antecipar) na resposta errada (perseguir).
    2. VAZAMENTO -- nenhum tom de bolha pode aparecer fora de uma bolha. Um
       respingo de BOLHA_LUZ no flanco poe um segundo ponto claro no corpo, e a
       silhueta passa a ter bolhas onde a geometria nao tem nenhuma.

    Nenhuma das duas levanta excecao em lugar nenhum do jogo, nenhuma aparece no
    atlas aberto num editor, e as duas so se manifestam como "esse bicho e
    confuso de acompanhar".
    """
    proibidos = {getattr(PALETA, nome): nome for nome in TONS_DE_BOLHA}
    for caixa in CAIXAS:
        if caixa.nome in BOLHAS:
            continue
        for pixel in _pixels_das_faces(f, caixa.nome):
            if pixel in proibidos:
                raise geo.ErroDeArte(
                    "o tom %s da bolha %s aparece na caixa '%s': a silhueta passa a ter bolha onde "
                    "a geometria nao tem nenhuma, e o jogador segue uma forma que nao existe"
                    % (proibidos[pixel], pixel, caixa.nome))

    bolha_mais_apagada = min(((_media_de_luminancia(f, n), n) for n in BOLHAS))
    pelo_mais_claro = max(((_media_de_luminancia(f, n), n) for n in PELAGEM))
    degrau = bolha_mais_apagada[0] - pelo_mais_claro[0]
    if degrau < DEGRAU_MINIMO_DE_LUMINANCIA:
        raise geo.ErroDeArte(
            "a bolha '%s' tem luminancia media %.1f e a pelagem '%s' tem %.1f: o degrau e %.1f e o "
            "minimo e %.1f. Com as duas regioes empatadas, o jogador perde as bolhas de vista no "
            "voo -- e perder o salto de vista troca a resposta certa (antecipar) pela errada "
            "(perseguir)" % (bolha_mais_apagada[1], bolha_mais_apagada[0], pelo_mais_claro[1],
                             pelo_mais_claro[0], degrau, DEGRAU_MINIMO_DE_LUMINANCIA))

    print("degrau: bolha '%s' %.1f  vs  pelagem '%s' %.1f  (degrau %.1f, minimo %.1f)"
          % (bolha_mais_apagada[1], bolha_mais_apagada[0], pelo_mais_claro[1],
             pelo_mais_claro[0], degrau, DEGRAU_MINIMO_DE_LUMINANCIA))


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_bolha_se_separa_do_pelo,))
