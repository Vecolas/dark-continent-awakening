"""Folha do Boneco de Treino.

ELE E FEIO DE PROPOSITO, e a paleta e onde isso se decide. Estopa crua, madeira
seca, cinta de couro e um alvo pintado no peito: nada disso pertence ao bestiario
de Hunter x Hunter, e essa e a garantia de que ninguem promove o dummy a
conteudo por engano.

O ALVO NO PEITO NAO E ENFEITE. Ele marca, na tela, a mesma regiao que o servidor
chama de ponto fraco -- o jogador ve onde bater porque a textura conta a mesma
coisa que o WeakPointResolver mede. Se alguem mover o alvo sem mover o resolver,
a textura passa a mentir, e nao ha portao que veja isso: por isso a altura do
alvo e conferida abaixo contra o mesmo numero do perfil.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao, e a divergencia nao da erro -- da face pintada no
lugar errado.

Regerar:  python art-source/enemies/dummy_enemy/dummy_enemy_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/dummy_enemy/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import geometria as geo      # noqa: E402
from comum import textura as tex        # noqa: E402
from dummy_enemy_geo import CAIXAS, HITBOX, MOB, UV_ALTURA, UV_LARGURA  # noqa: E402

# Altura relativa a partir da qual o servidor chama a regiao de "target".
# HunterExamProfiles: new WeakPointResolver("target", "body", 0.45D, 0.4D).
#
# O 0.45 NAO foi escolhido no vazio: ele e o alvo pintado, medido. O anel ocupa
# y 14..18 num boneco de 30.4 px de hitbox, ou seja 0.46 da altura. A primeira
# versao deste arquivo trazia 0.55, copiado de outro mob, e a regua abaixo
# reprovou na primeira execucao -- que e exatamente para isso que ela existe.
# Com 0.55 o desenho prometeria um acerto que a regra nao paga: o jogador
# miraria no vermelho, levaria dano comum, e nao haveria erro para procurar.
ALVO_ALTURA_MINIMA = 0.45

PALETA = tex.Paleta(
    ESTOPA=(178, 156, 112),      # o saco, visto de frente
    ESTOPA_SOMBRA=(140, 120, 84),  # o que fica por baixo e atras
    MADEIRA=(112, 84, 54),       # poste e cruz
    MADEIRA_CLARA=(146, 112, 74),  # o topo da cruz, que pega luz
    COURO=(84, 58, 40),          # as cintas que prendem a estopa
    ALVO=(168, 62, 48),          # o anel do alvo, no peito
    ALVO_CENTRO=(232, 214, 186),  # o miolo do alvo
)

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def pintar():
    # ------------------------------------------------------------- madeira
    # As colunas do veio sao declaradas POR CAIXA, e nao numa lista unica: a
    # cruz tem 10 px de face e o poste tem 4, e uma coluna em dx=5 no poste cai
    # na face vizinha. O portao da biblioteca pega isso -- mas o certo e nao
    # tentar, porque a lista unica so funciona enquanto as caixas tiverem o
    # mesmo tamanho, e isso ninguem garante.
    for nome, colunas in (("base", (0, 3, 6, 9)), ("post", (0, 3))):
        p = folha[nome]
        p.tudo(PALETA.MADEIRA)
        p.face("topo", PALETA.MADEIRA_CLARA)
        # Veio da madeira: colunas alternadas, e nao salpico. Salpico a
        # distancia le como sujeira; coluna le como tabua.
        for dx in colunas:
            for face in ("frente", "tras", "direita", "esquerda"):
                p.coluna(face, dx, PALETA.MADEIRA_CLARA, forca=2)

    # --------------------------------------------------------------- saco
    saco = folha["body"]
    saco.tudo(PALETA.ESTOPA)
    saco.face("tras", PALETA.ESTOPA_SOMBRA)
    saco.face("base", PALETA.ESTOPA_SOMBRA)
    # As cintas de couro: uma no alto e uma no pe, nas QUATRO faces laterais.
    # Pintar so na frente daria um boneco visivelmente vesgo por tras, e isso
    # nao aparece sem girar a camera em volta dele.
    for face in ("frente", "tras", "direita", "esquerda"):
        saco.faixa_no_topo(face, 1, PALETA.COURO)
        saco.faixa_no_pe(face, 1, PALETA.COURO)
    # O alvo, so na FRENTE: um anel de 4x4 com miolo claro.
    saco.na_face("frente", 2, 2, 4, 4, PALETA.ALVO, forca=0)
    saco.na_face("frente", 3, 3, 2, 2, PALETA.ALVO_CENTRO, forca=0)

    # -------------------------------------------------------------- cabeca
    cabeca = folha["head"]
    cabeca.tudo(PALETA.ESTOPA)
    cabeca.face("tras", PALETA.ESTOPA_SOMBRA)
    cabeca.face("topo", PALETA.ESTOPA_SOMBRA)
    # A "cara": duas cruzes de linha costurada. Nao sao olhos -- sao pontos de
    # costura, e e o que mantem o boneco lendo como objeto e nao como bicho.
    for dx in (1, 4):
        cabeca.ponto("frente", dx, 2, PALETA.COURO)
        cabeca.ponto("frente", dx, 3, PALETA.COURO)
    cabeca.faixa_no_pe("frente", 1, PALETA.COURO)
    cabeca.faixa_no_pe("direita", 1, PALETA.COURO)
    cabeca.faixa_no_pe("esquerda", 1, PALETA.COURO)
    cabeca.faixa_no_pe("tras", 1, PALETA.COURO)

    # -------------------------------------------------------------- bracos
    for nome in ("arm_left", "arm_right"):
        p = folha[nome]
        p.tudo(PALETA.ESTOPA_SOMBRA)
        p.face("topo", PALETA.ESTOPA)
        p.salpicar("frente", PALETA.COURO, 200, semente=7)


def valida_alvo_na_altura_do_ponto_fraco(f):
    """O alvo pintado tem de cair na faixa que o servidor chama de ponto fraco.

    O resolver mede altura RELATIVA a caixa de colisao; a textura pinta pixels
    dentro de uma face. Os dois so concordam por construcao se alguem conferir --
    e a divergencia nao da erro nenhum: da um alvo desenhado onde bater nao vale
    mais, o que e pior do que nao ter alvo, porque ENSINA errado.
    """
    corpo = geo.achar(CAIXAS, "body", "caixa")
    (_, _), (y0, y1), _ = geo.volume(corpo)
    altura_da_hitbox_px = HITBOX[1] * 16.0
    # O anel comeca 2 px abaixo do topo da face e tem 4 px: em coordenada de
    # mundo isso e o trecho de cima do saco.
    topo_do_alvo = y1 - 2
    pe_do_alvo = y1 - 6
    limite = ALVO_ALTURA_MINIMA * altura_da_hitbox_px
    if pe_do_alvo < limite:
        raise geo.ErroDeArte(
            "o alvo pintado vai de y=%s a y=%s e o servidor so chama de ponto fraco acima de "
            "%.1f px (WeakPointResolver %.2f de %.1f px): o desenho promete um acerto que a "
            "regra nao paga, e nada acusa isso" % (pe_do_alvo, topo_do_alvo, limite,
                                                   ALVO_ALTURA_MINIMA, altura_da_hitbox_px))
    if y0 > limite:
        raise geo.ErroDeArte(
            "o saco inteiro (y=%s..%s) esta acima do limite de ponto fraco %.1f px: nao sobra "
            "regiao de corpo comum, e todo golpe no boneco viraria critico" % (y0, y1, limite))


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_alvo_na_altura_do_ponto_fraco,))
