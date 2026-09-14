"""Folha do Hyper Puffball.

A PALETA E O AVISO. Este bicho nao tem garra, nao tem dente e nao se mexe: a
unica coisa que pode ensinar o jogador a nao encostar nele antes de ele aprender
na pele e a COR. Por isso a folha tem exatamente um contraste alto -- os poros
roxos contra a casca ocre -- e nada mais disputa atencao com eles. Casca pastel,
verrugas um degrau mais escuras, talo terroso: tudo que nao e o gatilho e
deliberadamente sem graca.

OS POROS NAO SAO ENFEITE. O servidor nao pergunta de que lado veio o golpe:
RegrasDeEstouro.decidir() olha vida, distancia e "ja estourou", e mais nada. Nao
ha cone, nao ha frente, nao ha costas. Entao a marca que anuncia o gatilho
tambem nao pode ter frente -- ela existe nas quatro paredes e no topo, e a
validacao abaixo cobra isso. Pintada so na frente, ela nao daria erro nenhum: daria
um jogador que se aproxima por tras, ve uma bola inofensiva, bate, e leva um
estouro que a tela nunca prometeu.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao, e a divergencia nao da erro -- da face pintada no
lugar errado.

Regerar:  python art-source/enemies/hyper_puffball/hyper_puffball_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/hyper_puffball/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import textura as tex        # noqa: E402
from hyper_puffball_geo import CAIXAS, MOB, UV_ALTURA, UV_LARGURA  # noqa: E402

# Quantos pixels de marca uma face precisa ter para contar como marcada.
#
# Oito e o tamanho que ainda se le a uns dez blocos, que e onde a decisao de
# chegar perto e tomada. Uma marca de 4 px (um unico poro) existe no atlas,
# passa em qualquer portao, e simplesmente nao e vista -- que e o mesmo que nao
# existir, so que mais dificil de descobrir.
MARCA_MINIMA_PX = 8

# As faces por onde o fungo e alcancavel de fora. A base encosta no chao e nunca
# e vista; e a unica dispensada da marca.
FACES_EXPOSTAS = ("frente", "tras", "direita", "esquerda", "topo")

PALETA = tex.Paleta(
    CASCA=(198, 188, 152),        # o saco, sob luz
    CASCA_SOMBRA=(150, 140, 108),  # o que fica por baixo e no vinco da cupula
    VERRUGA=(172, 158, 118),      # o mosqueado que faz a casca ler como fungo
    TALO=(124, 112, 88),          # o pe preso no chao
    ESPORO=(110, 72, 126),        # O GATILHO -- o unico contraste alto da folha
    ESPORO_CLARO=(176, 128, 190),  # a boca do poro, por onde a nuvem sai
)

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)

# Onde os poros ficam em cada parede do bulbo (coluna, linha), em coordenada DA
# FACE. Tres por parede, desalinhados na vertical: alinhados, eles leem como uma
# faixa pintada; espalhados, leem como respiros.
POROS_NA_PAREDE = ((2, 4), (7, 7), (12, 3))
# E nos cantos do topo, que e o que se ve de cima -- e de cima e como se olha um
# bicho de 1.2 bloco de altura.
POROS_NO_TOPO = ((1, 1), (13, 1), (1, 13), (13, 13))
LADO_DO_PORO = 2


def pintar():
    # ---------------------------------------------------------------- talo
    talo = folha["talo"]
    talo.tudo(PALETA.TALO)
    # O topo do talo fica debaixo do bulbo e so aparece quando o saco incha e
    # sobe. Pintado com a cor da casca em sombra, esse vislumbre le como "o bicho
    # esta esticando"; pintado igual ao resto do talo, le como um buraco.
    talo.face("topo", PALETA.CASCA_SOMBRA)
    for face in tex.PAREDES:
        talo.faixa_no_pe(face, 1, PALETA.CASCA_SOMBRA)

    # --------------------------------------------------------------- bulbo
    bulbo = folha["bulbo"]
    bulbo.tudo(PALETA.CASCA)
    # A base encosta no chao: escura, e sem marca. Pintar a base clara faz o
    # fungo parecer flutuando meio pixel acima do bloco, de lado.
    bulbo.face("base", PALETA.CASCA_SOMBRA)
    # O mosqueado entra ANTES dos poros. Salpicado por cima, ele comeria pixels
    # da marca e a validacao de contagem reprovaria -- corretamente, porque na
    # tela a marca ficaria roida e deixaria de ler a distancia.
    for face in tex.PAREDES:
        bulbo.salpicar(face, PALETA.VERRUGA, 64, semente=11)
        # A sombra do pe do saco: dois pixels que assentam o volume no chao.
        bulbo.faixa_no_pe(face, 2, PALETA.CASCA_SOMBRA)
    bulbo.salpicar("topo", PALETA.VERRUGA, 80, semente=17)

    # OS POROS, por ultimo e com forca=0: a cor precisa sair EXATA da paleta para
    # a validacao conseguir conta-los. Com ruido, cada pixel viraria um tom
    # proprio e a regua nao teria o que medir.
    for face in tex.PAREDES:
        for dx, dy in POROS_NA_PAREDE:
            bulbo.na_face(face, dx, dy, LADO_DO_PORO, LADO_DO_PORO, PALETA.ESPORO, forca=0)
    for dx, dy in POROS_NO_TOPO:
        bulbo.na_face("topo", dx, dy, LADO_DO_PORO, LADO_DO_PORO, PALETA.ESPORO, forca=0)

    # -------------------------------------------------------------- cupula
    cupula = folha["cupula"]
    cupula.tudo(PALETA.CASCA_SOMBRA)
    # O topo da cupula e a area que se abre: roxo inteiro, porque e dali que a
    # nuvem sai no clipe de morte. E o segundo maior bloco de contraste da folha,
    # e ele fica embaixo do poro para os dois lerem como uma coisa so.
    cupula.face("topo", PALETA.ESPORO)
    for face in tex.PAREDES:
        cupula.faixa_no_topo(face, 1, PALETA.ESPORO)

    # ---------------------------------------------------------------- poro
    poro = folha["poro"]
    poro.tudo(PALETA.ESPORO)
    poro.face("topo", PALETA.ESPORO_CLARO)


def valida_marca_do_gatilho_em_toda_volta(f):
    """A marca do gatilho existe em todas as faces expostas do bulbo.

    LIGACAO ARTE <-> REGRA. O estouro nao tem angulo: RegrasDeEstouro.decidir()
    recebe vida, distancia e o marcador de "ja estourou", e nenhum cosseno de
    frente -- diferente de um WeakPointResolver, que tem cone e por isso pode ter
    marca so de um lado. Aqui a regra e igual em volta, e a pintura tem de ser
    igual em volta.

    O erro que esta regua pega e o mais barato de cometer nesta lane: pintar a
    face 'frente' e considerar o bicho pintado. Nao da erro, nao aparece no atlas
    aberto num editor, e so se descobre girando a camera em volta do mob -- ou,
    em jogo, levando um estouro que a tela nao anunciou.
    """
    pincel = f["bulbo"]
    for nome in FACES_EXPOSTAS:
        x, y, largura, altura = pincel.faces()[nome]
        marcados = 0
        for i in range(x, x + largura):
            for j in range(y, y + altura):
                if f.px[i, j][:3] == PALETA.ESPORO:
                    marcados += 1
        if marcados < MARCA_MINIMA_PX:
            raise tex.ErroDeArte(
                "a face '%s' do bulbo tem %d px de marca de gatilho e o minimo e %d: o estouro "
                "dispara igual de todos os lados, entao um lado sem marca e um jogador que se "
                "aproxima por ali sem nenhum aviso na tela" % (nome, marcados, MARCA_MINIMA_PX))


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_marca_do_gatilho_em_toda_volta,))
