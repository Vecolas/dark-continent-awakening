"""Folha do Mosquito Officer.

ESTA FOLHA TEM UMA TAREFA SO: fazer a AGULHA ser a primeira coisa que o olho
encontra. O dreno deste oficial e invisivel -- a vida dela sobe e nada na tela
diz de onde veio -- e a unica explicacao que o jogador recebe e a silhueta mais a
cor. Por isso a paleta e quase inteira dessaturada e escura, e existe UM unico
contraste alto na folha: o vermelho na ponta da probocide.

Um segundo vermelho em qualquer outro lugar dividiria a leitura em dois, e a
divisao nao da erro nenhum -- da um bicho "bonito" de perto e ilegivel a vinte
blocos, que e a distancia em que o jogador precisa decidir se recua. Por isso o
abdome, que seria o candidato obvio a virar um saco de sangue, fica ESCURO: ele
carrega so as costuras num vermelho rebaixado, que a distancia le como sombra.

AS ASAS SAO A OUTRA LEITURA, e elas sao CLARAS de proposito. Contra um corpo
escuro, a membrana palida e o que faz a envergadura aparecer contra terra, folha
e pedra -- e a envergadura e o que diz "isto voa" antes de o bicho chegar. Asa
escura nao da erro: faz o oficial sumir no fundo em todo bioma que nao seja neve,
e o aviso de que ele esta vindo pelo alto desaparece junto.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

Regerar:  python art-source/enemies/mosquito_officer/mosquito_officer_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/mosquito_officer/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import textura as tex        # noqa: E402
from mosquito_officer_geo import CAIXAS, MOB, UV_ALTURA, UV_LARGURA  # noqa: E402

PALETA = tex.Paleta(
    # a quitina, e o bicho inteiro visto de longe: escura e fria, para a membrana
    # clara e a ponta vermelha terem contra o que brigar
    QUITINA=(52, 46, 54),
    # o degrau de luz do dorso -- volume, e nao cor nova
    QUITINA_CLARA=(84, 76, 88),
    # o que se ve por baixo; ela e mais escura que a quitina porque um bicho que
    # paira e visto de baixo o tempo todo, e barriga clara o faria flutuar
    VENTRE=(34, 30, 36),
    # a membrana da asa: clara, e a segunda leitura da silhueta
    MEMBRANA=(178, 180, 192),
    # as nervuras, para a asa nao ler como uma placa de papel
    NERVURA=(112, 114, 128),
    # a agulha: quase preta, para o vermelho da ponta ter onde encostar
    AGULHA=(26, 22, 26),
    # O UNICO CONTRASTE ALTO DA FOLHA. Ele mora na ponta da probocide e nos olhos,
    # que sao a mesma familia de cor e leem como um par -- "onde ela olha e onde
    # ela fura". Um terceiro lugar vermelho quebraria a leitura.
    SANGUE=(156, 36, 42),
    # o vermelho REBAIXADO das costuras do abdome: a distancia ele e sombra, e e
    # essa a intencao -- ele nao pode competir com a ponta
    SANGUE_FUNDO=(92, 26, 32),
)

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def pintar():
    # ----------------------------------------------------------------- torax
    torax = folha["thorax"]
    torax.tudo(PALETA.QUITINA)
    torax.face("topo", PALETA.QUITINA_CLARA)
    torax.face("base", PALETA.VENTRE)
    # Mosqueado discreto no dorso. Denso demais, o torax deixaria de ler como
    # quitina a distancia -- que e o unico lugar onde a leitura dele serve.
    torax.salpicar("topo", PALETA.QUITINA, 30, semente=7)
    # A linha escura que corre no meio do dorso e o que separa os dois pares de
    # asa visualmente; sem ela, o torax le como um bloco unico e a asa parece
    # colada por cima.
    torax.linha_central("topo", 1, PALETA.AGULHA, forca=0)

    # ---------------------------------------------------------------- abdome
    abdome = folha["abdomen"]
    abdome.tudo(PALETA.QUITINA)
    abdome.face("base", PALETA.VENTRE)
    # AS COSTURAS. Elas vao nas DUAS paredes laterais e na de baixo, porque o
    # abdome e visto de lado quando ela flanqueia e de baixo quando ela paira.
    # Pintar so um lado nao da erro: da um bicho visivelmente vesgo, e ninguem
    # descobre isso sem girar a camera em volta dele.
    for face in ("direita", "esquerda"):
        for dy in (1, 3, 5):
            abdome.linha(face, dy, PALETA.SANGUE_FUNDO, forca=2)
    abdome.linha_central("base", 1, PALETA.SANGUE_FUNDO, forca=2)
    # A ponta do abdome, escura: ela fecha a silhueta por tras.
    abdome.face("tras", PALETA.VENTRE)

    # ---------------------------------------------------------------- cabeca
    cabeca = folha["head"]
    cabeca.tudo(PALETA.QUITINA)
    cabeca.face("topo", PALETA.QUITINA_CLARA)
    cabeca.face("base", PALETA.VENTRE)
    # OS OLHOS COMPOSTOS, um em cada flanco, na MESMA familia de cor da ponta da
    # agulha. Eles sao a segunda metade do par que o jogador aprende: onde ela
    # olha e onde ela fura. Sao pequenos de proposito -- grandes, roubariam da
    # agulha o unico contraste que a folha tem para gastar.
    for face in ("direita", "esquerda"):
        cabeca.na_face(face, 1, 1, 2, 2, PALETA.SANGUE, forca=0)
    # A cara, escura, para a agulha sair de um fundo que nao compete com ela.
    cabeca.face("frente", PALETA.AGULHA)

    # ------------------------------------------------------------ proboscide
    # A PECA INTEIRA DO BICHO. Corpo quase preto, PONTA vermelha.
    agulha = folha["proboscis"]
    agulha.tudo(PALETA.AGULHA)
    # A face da ponta: um pixel, e o unico pixel totalmente saturado da folha.
    agulha.face("frente", PALETA.SANGUE, forca=0)
    # As duas ultimas colunas de cada flanco. Na face 'direita' a FRENTE do bicho
    # fica na borda direita; na 'esquerda', na borda esquerda. Copiar a mesma
    # coluna nos dois lados nao daria erro -- deixaria a marca no MEIO da agulha
    # de um lado e na ponta do outro, e so se ve isso girando a camera.
    _, _, largura_do_flanco, _ = agulha.faces()["direita"]
    for dx in (largura_do_flanco - 2, largura_do_flanco - 1):
        agulha.coluna("direita", dx, PALETA.SANGUE, forca=0)
    for dx in (0, 1):
        agulha.coluna("esquerda", dx, PALETA.SANGUE, forca=0)

    # ------------------------------------------------------------------ asas
    for nome in ("wing_left", "wing_right"):
        asa = folha[nome]
        asa.tudo(PALETA.MEMBRANA)
        # AS NERVURAS. Duas fiadas no dorso da asa e uma borda escura: sem elas a
        # asa le como uma placa de papel, e placa de papel nao le como voo.
        for dy in (1, 3):
            asa.linha("topo", dy, PALETA.NERVURA, forca=2)
        asa.faixa_no_pe("tras", 1, PALETA.NERVURA)
        asa.faixa_no_topo("frente", 1, PALETA.NERVURA)
        # A face de baixo e um degrau mais escura: a asa e vista de baixo o tempo
        # todo, e sem o degrau ela perde espessura e vira um decalque.
        asa.face("base", PALETA.NERVURA)

    # ---------------------------------------------------------------- bracos
    for nome in ("arm_left", "arm_right"):
        braco = folha[nome]
        braco.tudo(PALETA.QUITINA)
        # A ponta escura separa o braco do fundo numa silhueta de 1 px. Sem ela o
        # membro some contra qualquer coisa escura, e a articulacao de formiga --
        # que e o que distingue este bicho de uma mosca grande -- se perde.
        for face in ("frente", "tras", "direita", "esquerda"):
            braco.faixa_no_pe(face, 2, PALETA.AGULHA)
        braco.face("base", PALETA.AGULHA)

    # ---------------------------------------------------------------- pernas
    for nome in ("leg_left", "leg_right"):
        perna = folha[nome]
        perna.tudo(PALETA.QUITINA)
        # As pernas sao o que PENDE quando ela paira, e e por elas que o jogador
        # ve que o bicho esta no ar. A ponta escura da a elas um fim visivel.
        for face in ("frente", "tras", "direita", "esquerda"):
            perna.faixa_no_pe(face, 3, PALETA.AGULHA)
        perna.face("base", PALETA.AGULHA)
        # O joelho, um degrau claro no meio: sem ele a perna e um risco reto e a
        # dobra do clipe de recolher nao se le.
        for face in ("direita", "esquerda"):
            perna.linha(face, 5, PALETA.QUITINA_CLARA, forca=2)


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB)
