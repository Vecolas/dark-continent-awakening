"""Folha do Wolf Pack Hunter -- pelo de mato, ventre claro e olhos que contam.

A DECISAO DE LEITURA DESTA FOLHA CABE NUMA FRASE: **o jogador tem de CONTAR os
lobos antes de contar os golpes.** Tudo aqui serve a isso.

Um bicho de 14 px a dez blocos de distancia nao tem area para detalhe. O que
sobrevive a essa distancia e (a) o contorno e (b) o contraste entre o dorso e o
ventre. Por isso a folha tem dois campos grandes -- dorso escuro, ventre claro --
e um unico ponto de contraste alto: o par de olhos. Esse par e o que faz quatro
lobos no mato lerem como quatro bichos, e nao como uma mancha que se mexe.

UMA VARIANTE SO, E ISSO TEM UM CUSTO DECLARADO. O lider do bando nao e distinto
na textura. `Squad` promove o proximo quando o lider morre, e o jogador nao tem
como ver QUEM era o lider nem que a promocao aconteceu -- a mudanca e real no
servidor e invisivel na tela. Isso esta escrito aqui, e nao resolvido aqui: uma
segunda folha e uma decisao de escopo, nao um retoque de pincel.

O QUE A REGUA DESTE ARQUIVO COBRA. O ventre claro nao e gosto: ele e a metade de
baixo da silhueta, e e o que separa "lobo correndo" de "vulto". Se o ventre
empatar de luminancia com o dorso, o bicho vira um borrao uniforme a distancia --
nao ha erro, nao ha log, e o sintoma e "nao da para ver quantos sao".
`valida_degrau_de_dorso_e_ventre` mede a folha inteira e reprova o empate.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

Regerar:  python art-source/enemies/wolf_pack_hunter/wolf_pack_hunter_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/wolf_pack_hunter/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import geometria as geo      # noqa: E402
from comum import textura as tex        # noqa: E402
from wolf_pack_hunter_geo import (CAIXAS, MOB, UV_ALTURA, UV_LARGURA)  # noqa: E402

# Quantas vezes a luminancia media do VENTRE tem de superar a do DORSO.
#
# 2.2 e um degrau que se enxerga a dez blocos. Com 1.15 os dois campos empatam a
# distancia e a silhueta vira um vulto; com 4.0 o ventre teria de ser quase
# branco, e um lobo de barriga branca no meio do mato le como cachorro perdido --
# nao como predador de matilha.
#
# E limite de LEITURA, nao de balanceamento: ninguem vai gira-lo numa sessao de
# ajuste, e por isso ele mora aqui, ao lado do comentario que o explica.
DEGRAU_DE_VENTRE = 2.2

# As pecas do TRONCO -- as unicas que tem dorso e ventre de verdade. Declaradas
# uma vez, e nao repetidas dentro da regua: duas listas divergem, e a divergencia
# faria a regua medir um ventre que ja nao e o ventre.
#
# As patas ficam FORA de proposito: a base delas e a almofada preta, que e
# contato com o chao e nao contra-sombreado. Incluidas, elas puxariam a media do
# ventre para baixo e a regua reprovaria um desenho correto -- ou, pior,
# aprovaria um errado por compensacao.
PECAS_DO_TRONCO = ("body", "head", "tail")

PALETA = tex.Paleta(
    PELO=(84, 78, 70),            # o flanco, visto de frente
    PELO_DORSO=(52, 48, 44),      # o que se ve de cima: a faixa que corre no lombo
    PELO_VENTRE=(156, 148, 134),  # o que se ve de baixo e nas patas
    FOCINHO=(38, 35, 34),         # a ponta do focinho e as almofadas das patas
    GARRA=(206, 200, 186),        # garras e presas -- o unico claro fora do ventre
    OLHO=(214, 176, 64),          # a iris: o unico amarelo do bicho
    PUPILA=(22, 20, 20),          # o miolo: o ponto mais escuro da folha
)

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def _pelo(p, salpico, ventre=True):
    """O tratamento comum de toda peca coberta de pelo.

    Topo escuro e base clara: e o contra-sombreado que todo predador de campo
    aberto tem, e aqui ele nao e naturalismo -- e a unica coisa que da VOLUME a
    um corpo de 14 px visto contra o mato.

    `salpico` e a semente. Semente repetida entre duas pecas faria as duas
    receberem o MESMO mosqueado, e um padrao repetido le como textura de parede,
    nao como pelo.
    """
    p.tudo(PALETA.PELO)
    p.face("topo", PALETA.PELO_DORSO)
    p.face("tras", PALETA.PELO_DORSO)
    if ventre:
        p.face("base", PALETA.PELO_VENTRE)
    for face in tex.PAREDES:
        p.salpicar(face, PALETA.PELO_DORSO, 46, semente=salpico)
        p.salpicar(face, PALETA.PELO_VENTRE, 22, semente=salpico + 7)


def pintar():
    # -------------------------------------------------------------- tronco
    corpo = folha["body"]
    _pelo(corpo, 3)
    # A faixa do lombo: uma coluna escura que corre da nuca a cauda. Ela e o
    # unico traco longitudinal do bicho, e e o que faz o lobo visto de cima ler
    # como um corpo com direcao em vez de uma mancha.
    corpo.linha_central("topo", 3, PALETA.PELO_DORSO, forca=2)
    # O ventre claro sobe pelos flancos. Pintado so na base, ele desapareceria de
    # perfil -- e perfil e como se ve um lobo que esta cercando, nao atacando.
    for face in tex.LADOS:
        corpo.faixa_no_pe(face, 2, PALETA.PELO_VENTRE)
    corpo.faixa_no_pe("frente", 2, PALETA.PELO_VENTRE)

    # -------------------------------------------------------------- cabeca
    cabeca = folha["head"]
    _pelo(cabeca, 19)
    # A mascara escura em volta dos olhos. Ela existe para os olhos nao flutuarem
    # no meio de um campo de pelo medio: sem ela, o amarelo perde o contorno e o
    # par de pontos deixa de ler como um PAR.
    cabeca.na_face("frente", 0, 1, 4, 2, PALETA.PELO_DORSO, forca=2)
    # Os olhos: dois pixels de iris com um de pupila, com forca=0 -- ruido aqui
    # borraria a unica leitura que esta peca entrega.
    for dx in (0, 3):
        cabeca.ponto("frente", dx, 1, PALETA.OLHO)
        cabeca.ponto("frente", dx, 2, PALETA.PUPILA)
    # A garganta clara, para o ventre nao comecar so no peito.
    cabeca.faixa_no_pe("frente", 1, PALETA.PELO_VENTRE)

    # ----------------------------------------------------------- focinho
    bico = folha["snout"]
    bico.tudo(PALETA.PELO)
    bico.face("topo", PALETA.PELO_DORSO)
    bico.face("base", PALETA.PELO_VENTRE)
    # A ponta preta: a face da FRENTE inteira. E o que fecha a silhueta da
    # cabeca; ponta clara faria o focinho se dissolver no fundo do mato.
    bico.face("frente", PALETA.FOCINHO, forca=0)
    # As presas, na fiada de baixo das duas faces laterais. Na base elas nunca
    # seriam vistas, e o desenho teria custado a mesma area de atlas para nada.
    for face in tex.LADOS:
        bico.ponto(face, 0, 1, PALETA.GARRA)

    # ------------------------------------------------------------- orelhas
    for nome, semente in (("ear_left", 29), ("ear_right", 37)):
        orelha = folha[nome]
        orelha.tudo(PALETA.PELO_DORSO)
        # O interior claro, na face que aponta para a frente. Orelha chapada
        # escura some contra o dorso, e as orelhas sao o que se ve primeiro
        # quando o bando ainda esta atras do mato alto.
        orelha.na_face("frente", 0, 1, 1, 1, PALETA.PELO_VENTRE, forca=0)

    # -------------------------------------------------------------- cauda
    cauda = folha["tail"]
    _pelo(cauda, 43)
    # A ponta clara da cauda: o sinal mais alto do bando visto por tras, e a unica
    # peca que continua visivel quando o lobo ja virou as costas para recuar.
    cauda.faixa_no_pe("tras", 1, PALETA.PELO_VENTRE)

    # ------------------------------------------------------ pernas e patas
    for nome, semente in (("leg_front_left", 53), ("leg_front_right", 59),
                          ("leg_back_left", 61), ("leg_back_right", 67)):
        _pelo(folha[nome], semente)

    for nome, semente in (("paw_front_left", 71), ("paw_front_right", 73),
                          ("paw_back_left", 79), ("paw_back_right", 83)):
        pata = folha[nome]
        # As patas sao o campo CLARO por inteiro: e por elas que a leitura de
        # baixo funciona, e sao elas que a regua mede. `_pelo` com o topo escuro
        # aqui derrubaria a media e o degrau sumiria sem nada acusar.
        pata.tudo(PALETA.PELO_VENTRE)
        pata.face("base", PALETA.FOCINHO, forca=0)   # as almofadas
        for face in tex.PAREDES:
            pata.salpicar(face, PALETA.PELO, 30, semente=semente)
        # Tres garras na FRENTE de cada pata.
        for dx in (0, 1):
            pata.ponto("frente", dx, 0, PALETA.GARRA)


# ------------------------------------------ validacao que liga arte e regra

def _luminancia(pixel):
    """Luminancia perceptual, a mesma conta que o olho humano faz de longe.

    Media simples dos canais mediria ERRADO: (0,255,0) e (255,0,0) tem a mesma
    media e brilhos completamente diferentes, e a regua aprovaria um verde
    berrante como se fosse tao discreto quanto um vermelho escuro.
    """
    return 0.299 * pixel[0] + 0.587 * pixel[1] + 0.114 * pixel[2]


def _media_da_face(f, nomes, face):
    """Luminancia media de UMA face de cada caixa citada.

    Uma face, e nao a caixa inteira. Medir as seis misturaria o dorso com o
    ventre dentro da mesma media, e a regua passaria a comparar duas medias que
    ja contem as duas coisas -- ou seja, aprovaria qualquer desenho, inclusive um
    lobo chapado. Cantos do layout que nenhuma face usa ficam de fora por
    construcao: eles sao transparentes, e medi-los puxaria toda media ao preto.
    """
    valores = []
    for nome in nomes:
        x, y, largura, altura = f[nome].faces()[face]
        for i in range(x, x + largura):
            for j in range(y, y + altura):
                valores.append(_luminancia(f.px[i, j][:3]))
    if not valores:
        raise geo.ErroDeArte("nenhum pixel medido na face '%s' de %s: a regua nao mediu nada e "
                             "ficaria verde" % (face, list(nomes)))
    return sum(valores) / len(valores)


def valida_degrau_de_dorso_e_ventre(f):
    """O ventre tem de ser DEGRAU_DE_VENTRE vezes mais claro que o dorso.

    Este bicho tem 14 px e aparece em bando. A unica coisa que sobrevive a dez
    blocos de distancia e o contraste entre o campo escuro de cima e o campo
    claro de baixo: e ele que faz quatro lobos lerem como quatro bichos em vez de
    uma mancha que se mexe.

    Empatar os dois campos nao da erro nenhum, nao aparece no atlas aberto num
    editor, e nao reprova nenhum portao Java. O sintoma e o jogador nao conseguir
    contar quantos sao -- e contar quantos sao E o encontro: `SquadRules.matilha()`
    poe ate quatro, e `RegrasDeMatilha` faz o lobo sozinho RECUAR. Um jogador que
    nao consegue contar nao sabe se esta diante de um bicho fraco ou de um cerco.
    """
    dorso = _media_da_face(f, PECAS_DO_TRONCO, "topo")
    ventre = _media_da_face(f, PECAS_DO_TRONCO, "base")
    if ventre < dorso * DEGRAU_DE_VENTRE:
        raise geo.ErroDeArte(
            "o dorso tem luminancia media %.1f e o ventre %.1f; o exigido e %.1f (%.1fx). Com os "
            "dois campos empatados o lobo vira um vulto a dez blocos, e o jogador nao consegue "
            "CONTAR quantos sao -- que e a decisao inteira deste encontro"
            % (dorso, ventre, dorso * DEGRAU_DE_VENTRE, DEGRAU_DE_VENTRE))
    print("degrau: ventre %.1f  vs  dorso %.1f  (razao %.2fx, minimo %.2fx)"
          % (ventre, dorso, ventre / dorso, DEGRAU_DE_VENTRE))


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_degrau_de_dorso_e_ventre,))
