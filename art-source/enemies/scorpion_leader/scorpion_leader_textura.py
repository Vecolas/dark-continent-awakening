"""Folha do Scorpion Leader.

A FOLHA TEM UM UNICO TRABALHO: fazer o jogador achar o FERRAO antes de tomar o
primeiro veneno.

Tudo aqui e escuro e fosco -- carapaca, pincas, pernas -- e existe um unico tom
quente na folha inteira, o ambar do ferrao. Isso nao e gosto: o veneno e um
segundo tipo de dano que nao sai de lugar nenhum que o jogador consiga apontar, e
a unica pista que ele tem e a ponta que brilha no alto da silhueta. Um segundo
ponto quente em qualquer outra peca dividiria essa leitura em duas, e a divisao
nao da erro -- da um jogador que olha para a garra errada.

E POR ISSO QUE AS PINCAS NAO LEVAM AMBAR. Elas sao o golpe que NAO envenena
(ScorpionLeaderTuning.pinca() nao aplica efeito nenhum), e pinta-las com a cor do
veneno ensinaria o oposto da regra: o jogador recuaria do golpe barato e entraria
no caro.

A segunda leitura e a ARMADURA. Armadura 8 e a mais alta da familia, e a carapaca
tem de dizer isso de longe: placas largas, chapadas, com uma costura escura no
meio e borda escura nas paredes. Casca salpicada leria como couro ou pelo, e couro
nao promete que bater de frente custa caro.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

Regerar:  python art-source/enemies/scorpion_leader/scorpion_leader_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/scorpion_leader/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import textura as tex        # noqa: E402
from scorpion_leader_geo import CAIXAS, MOB, UV_ALTURA, UV_LARGURA  # noqa: E402

PALETA = tex.Paleta(
    # a carapaca, e o bicho inteiro visto de longe: placa escura e fosca
    CASCA=(66, 60, 56),
    # o degrau de luz do dorso -- volume, nao cor nova
    CASCA_LUZ=(96, 88, 80),
    # O UNICO preto grande da folha: a costura da carapaca e a borda das paredes.
    # Um segundo preto em outro lugar dividiria a silhueta em duas leituras.
    COSTURA=(30, 27, 25),
    # quitina de cabeca, bracos e pernas -- um degrau abaixo da casca, para que a
    # carapaca continue sendo a silhueta
    QUITINA=(50, 45, 42),
    # a pinca: mais clara que o braco, porque o quadro em que ela fecha precisa
    # ser legivel. E ela NAO leva ambar: e o golpe que nao envenena.
    PINCA=(84, 76, 68),
    # O UNICO tom quente da folha inteira: o ferrao. Ele e a pista do veneno.
    VENENO=(206, 154, 44),
    # a sombra do ambar, so para a ponta nao ler como um retangulo chapado
    VENENO_FUNDO=(138, 92, 22),
    # os olhos: claros e minusculos, o unico ponto de leitura da cabeca
    OLHO=(214, 206, 190),
)

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)

PERNAS = ("leg_1_left", "leg_2_left", "leg_3_left", "leg_4_left",
          "leg_1_right", "leg_2_right", "leg_3_right", "leg_4_right")


def pintar():
    # ------------------------------------------------------------- carapaca
    casca = folha["carapaca"]
    casca.tudo(PALETA.CASCA)
    # O dorso e o que se ve de cima e de longe, e e ele que carrega a leitura de
    # placa. As duas colunas de costura correm da frente ao fundo: sem elas a
    # carapaca vira um seixo, e seixo nao promete armadura.
    casca.face("topo", PALETA.CASCA_LUZ)
    for dx in (6, 7):
        casca.coluna("topo", dx, PALETA.COSTURA, forca=0)
    # Tres fiadas transversais: as placas do abdome. Elas sao o que faz o corpo
    # ler como segmentado -- e segmentado e o que diz "artropode" antes de
    # qualquer outra coisa.
    for dy in (5, 9, 13):
        casca.linha("topo", dy, PALETA.COSTURA, forca=1)
    # A base fica no chao e quase nao se ve; escura, para que uma fresta entre o
    # corpo e o solo nao mostre uma barriga clara que nao existe.
    casca.face("base", PALETA.COSTURA)
    # A borda escura das quatro paredes: sem ela o corpo e um bloco, e bloco nao
    # tem volume a vinte blocos de distancia.
    for face in tex.PAREDES:
        casca.faixa_no_pe(face, 1, PALETA.COSTURA)

    # --------------------------------------------------------------- cabeca
    cabeca = folha["head"]
    cabeca.tudo(PALETA.QUITINA)
    cabeca.face("frente", PALETA.COSTURA)
    # Os olhos, um par no dorso da cabeca -- e do lado esquerdo e do direito a
    # coluna e DIFERENTE de proposito: na face 'direita' a frente do bicho fica na
    # borda direita, e copiar a mesma coluna nos dois lados daria um bicho vesgo
    # que so quem girar a camera descobre.
    cabeca.ponto("topo", 2, 1, PALETA.OLHO)
    cabeca.ponto("topo", 5, 1, PALETA.OLHO)
    cabeca.ponto("direita", 3, 1, PALETA.OLHO)
    cabeca.ponto("esquerda", 0, 1, PALETA.OLHO)

    # ---------------------------------------------------------------- cauda
    # Os tres elos ficam entre a casca e a quitina: eles precisam ler como
    # CONTINUACAO do corpo, e nao como um apendice colado. O que se destaca e so
    # a ponta.
    for nome in ("tail_base", "tail_mid", "tail_tip"):
        elo = folha[nome]
        elo.tudo(PALETA.CASCA)
        elo.face("topo", PALETA.CASCA_LUZ)
        for face in tex.PAREDES:
            elo.faixa_no_pe(face, 1, PALETA.COSTURA)

    # --------------------------------------------------------------- ferrao
    # A UNICA peca quente da folha. Ela e pequena, e e por isso que ela precisa ser
    # a mais clara: a vinte blocos, area pequena so e vista se o contraste for
    # alto. O fundo escuro na base do ferrao existe para a ponta nao ler como um
    # retangulo chapado.
    ferrao = folha["stinger"]
    ferrao.tudo(PALETA.VENENO)
    for face in tex.PAREDES:
        ferrao.faixa_no_topo(face, 1, PALETA.VENENO_FUNDO)
    ferrao.face("base", PALETA.VENENO_FUNDO)

    # --------------------------------------------------------------- bracos
    for nome in ("arm_left", "arm_right"):
        braco = folha[nome]
        braco.tudo(PALETA.QUITINA)
        braco.face("topo", PALETA.CASCA)

    # --------------------------------------------------------------- pincas
    # Claras o bastante para o fechar ser legivel, e SEM nenhum ambar: este e o
    # golpe que nao envenena, e uma pinca pintada com a cor do veneno ensinaria o
    # jogador a recuar do golpe barato e entrar no caro.
    for nome in ("claw_left", "claw_right"):
        pinca = folha[nome]
        pinca.tudo(PALETA.PINCA)
        pinca.face("topo", PALETA.CASCA_LUZ)
        # A ponta escura e o que separa a garra do fundo quando ela fecha na
        # frente do corpo, que e exatamente o quadro que precisa ser lido.
        pinca.faixa_no_pe("frente", 2, PALETA.COSTURA)
        pinca.faixa_no_pe("direita", 2, PALETA.COSTURA)
        pinca.faixa_no_pe("esquerda", 2, PALETA.COSTURA)

    # --------------------------------------------------------------- pernas
    for nome in PERNAS:
        perna = folha[nome]
        perna.tudo(PALETA.QUITINA)
        # A ponta preta separa a perna do chao numa silhueta que e quase toda
        # rente ao solo. Sem ela, oito pernas escuras sobre terra escura somem, e o
        # bicho passa a parecer que desliza.
        for face in tex.PAREDES:
            perna.faixa_no_pe(face, 1, PALETA.COSTURA)
        perna.face("base", PALETA.COSTURA)


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB)
