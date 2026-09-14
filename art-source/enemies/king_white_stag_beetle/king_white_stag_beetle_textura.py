"""Folha do King White Stag Beetle.

A CARAPACA TEM DE LER COMO CARAPACA A VINTE BLOCOS, e o ventre tem de ler como
OUTRA COISA no instante em que o bicho vira. Sao as duas unicas exigencias de
leitura deste mob, e as duas existem por causa da regra do servidor:

  * a casca e a superficie que NAO paga. Ela precisa ser uma cupula lisa, de cor
    chapada e clara, com UM contraste alto -- a costura escura que corre no meio
    dela. Casca salpicada de manchas leria como pelo ou rocha, e o jogador nao
    saberia que aquilo e uma armadura antes de gastar vinte espadadas
    descobrindo;

  * o ventre e a superficie que paga QUADRUPLO, durante 60 ticks, e o servidor
    nao avisa nada. O unico aviso que existe e a cor mudar quando o besouro rola.
    Ambar escuro contra marfim e uma troca que se le de longe e de relance, que e
    como ela vai ser lida.

O nome do bicho manda na paleta: King WHITE Stag Beetle. O branco e da casca, e e
ele que da ao ventre escuro todo o contraste de que ele precisa -- a paleta nao
tem um segundo claro competindo.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

Regerar:  python art-source/enemies/king_white_stag_beetle/king_white_stag_beetle_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/king_white_stag_beetle/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import textura as tex        # noqa: E402
from king_white_stag_beetle_geo import CAIXAS, MOB, UV_ALTURA, UV_LARGURA  # noqa: E402

PALETA = tex.Paleta(
    # a casca, e o bicho inteiro visto de longe
    CASCA=(226, 218, 202),
    # o flanco e a barriga da casca, um degrau abaixo -- volume, nao cor nova
    CASCA_SOMBRA=(188, 178, 158),
    # O UNICO contraste alto da folha: a costura no meio da casca e a borda dela.
    # Um segundo preto em outro lugar dividiria a silhueta em duas leituras.
    COSTURA=(54, 46, 40),
    # o ventre: a cor que o jogador precisa reconhecer no instante do tombo
    VENTRE=(126, 74, 38),
    # as placas do abdome, para o ventre nao ler como um retangulo chapado
    VENTRE_PLACA=(154, 100, 54),
    # quitina de cabeca, torax e pernas -- escura, para nao competir com a casca
    QUITINA=(74, 58, 46),
    # a pinca, polida e quase preta na ponta
    CHIFRE=(40, 33, 28),
)

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def pintar():
    # ------------------------------------------------------------- carapaca
    casca = folha["carapaca"]
    casca.tudo(PALETA.CASCA)
    # A base da casca e o que se ve por baixo quando ele vira, junto com o
    # ventre. Ela e sombra e nao ventre: se fosse pintada de ambar, o jogador
    # veria "barriga" numa regiao que o servidor cobra como carapaca.
    casca.face("base", PALETA.CASCA_SOMBRA)
    casca.face("tras", PALETA.CASCA_SOMBRA)
    # A COSTURA: duas colunas no topo, correndo da frente ao fundo. Ela e o unico
    # traco escuro grande da folha, e e o que faz a cupula ler como dois elitros
    # fechados em vez de uma pedra branca.
    for dx in (8, 9):
        casca.coluna("topo", dx, PALETA.COSTURA, forca=0)
    # A borda escura das quatro paredes: um besouro sem borda vira um seixo.
    for face in ("frente", "tras", "direita", "esquerda"):
        casca.faixa_no_pe(face, 1, PALETA.COSTURA)
    # Mosqueado discreto no topo. Denso demais, a casca deixaria de ler como
    # casca a distancia -- que e o unico lugar onde a leitura dela serve.
    casca.salpicar("topo", PALETA.CASCA_SOMBRA, 26, semente=11)

    # ---------------------------------------------------------------- torax
    # Quase invisivel embaixo da casca, e e por isso que ele e escuro: qualquer
    # claro aqui apareceria como uma segunda silhueta pela fresta.
    torax = folha["torax"]
    torax.tudo(PALETA.QUITINA)
    torax.face("topo", PALETA.COSTURA)

    # --------------------------------------------------------------- ventre
    ventre = folha["ventre"]
    ventre.tudo(PALETA.VENTRE)
    # A base e a face que fica para CIMA quando ele vira, entao e ela que carrega
    # o desenho. Pintar as placas so nas paredes daria um ventre liso justamente
    # no angulo em que o jogador olha para ele.
    ventre.face("base", PALETA.VENTRE)
    for dy in (1, 3, 5, 7):
        ventre.linha("base", dy, PALETA.VENTRE_PLACA, forca=2)
    for face in ("frente", "tras", "direita", "esquerda"):
        ventre.faixa_no_pe(face, 1, PALETA.VENTRE_PLACA)
    # O topo do ventre encosta no torax e nao se ve nunca: quitina, para que uma
    # fresta nao mostre ambar onde deveria haver corpo.
    ventre.face("topo", PALETA.QUITINA)

    # --------------------------------------------------------------- cabeca
    cabeca = folha["head"]
    cabeca.tudo(PALETA.QUITINA)
    cabeca.face("frente", PALETA.CHIFRE)
    cabeca.face("topo", PALETA.CHIFRE)
    # Os olhos compostos, um em cada flanco. Eles sao claros para a cabeca ter um
    # ponto de leitura, e sao PEQUENOS para nao virarem o foco: o foco e a pinca.
    for face in ("direita", "esquerda"):
        cabeca.na_face(face, 1, 1, 2, 2, PALETA.CASCA_SOMBRA, forca=0)

    # -------------------------------------------------------------- chifres
    for nome in ("horn_left", "horn_right"):
        chifre = folha[nome]
        chifre.tudo(PALETA.CHIFRE)
        # O dorso do chifre pega luz; a face de baixo nao. Sem esse degrau a pinca
        # vira um palito preto e o quadro em que ela fecha deixa de ser legivel.
        chifre.face("topo", PALETA.QUITINA)

    # --------------------------------------------------------------- pernas
    for nome in ("leg_front_left", "leg_front_right", "leg_mid_left",
                 "leg_mid_right", "leg_rear_left", "leg_rear_right"):
        perna = folha[nome]
        perna.tudo(PALETA.QUITINA)
        # A ponta escura e o que separa a perna do chao numa silhueta baixa. As
        # pernas sao tambem o que mais se mexe quando ele esta de costas, e e por
        # elas que o jogador ve que o bicho ainda esta vivo.
        perna.faixa_no_pe("frente", 2, PALETA.CHIFRE)
        perna.faixa_no_pe("tras", 2, PALETA.CHIFRE)
        perna.faixa_no_pe("direita", 2, PALETA.CHIFRE)
        perna.faixa_no_pe("esquerda", 2, PALETA.CHIFRE)
        perna.face("base", PALETA.CHIFRE)


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB)
