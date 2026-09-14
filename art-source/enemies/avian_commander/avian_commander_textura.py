"""Folha do Avian Commander -- o dorso some no ceu, o VENTRE conta quem ela e.

A DECISAO DE LEITURA DESTA FOLHA CABE NUMA FRASE: **o jogador ve esta comandante
por baixo.** A regra do servidor manda ela ficar a
`AvianCommanderTuning.ALTITUDE_DE_COMANDO` blocos do chao enquanto comanda, e so
descer no mergulho -- ou seja, a superficie que trabalha e a de BAIXO, contra o
ceu, quase sempre em contraluz.

Disso saem as tres decisoes da folha, e nenhuma e gosto:

  * CONTRASSOMBRA INVERTIDA DE PROPOSITO. O dorso e escuro (ela some quando vista
    de cima, do alto de uma montanha) e o ventre e claro. E o que toda ave faz, e
    aqui serve para que a MARCA caiba sobre um fundo legivel.
  * A MARCA DE QUIMERA MORA NO VENTRE, E SO NELE. Ambar sobre a barriga clara:
    e o unico elemento de contraste alto da folha inteira, e ele esta exatamente
    na face que o jogador ve. Pintada no dorso, a marca existiria no atlas e nunca
    na tela -- e "marca de quimera obrigatoria" viraria uma linha de checklist que
    ninguem consegue conferir jogando.
  * NADA MAIS DISPUTA CONTRASTE. O olho e pequeno, a garra e clara mas minuscula,
    e a quitina fica numa faixa escura. Um segundo ponto de alto contraste faria o
    olho competir com a marca, e a dez blocos os dois viram uma mancha so.

`valida_marca_de_quimera_le_de_baixo` mede as tres pontas disso: que a marca
ESTEJA na base de toda peca do ventre de comando, que ela NAO esteja em nenhuma
face de topo, e que o degrau de luminancia entre ela e a barriga em volta sobreviva
a distancia.

A MARCA DE QUIMERA ESTRUTURAL -- as antenas -- vive no geo, e e cobrada la
(`valida_marca_de_quimera`). As duas metades sao deliberadas: forma para a
silhueta a vinte blocos, cor para a leitura a dez. Uma so das duas deixa o bicho
sem identidade em metade das distancias em que ele e visto.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

NAO HA ALVO PINTADO, e a ausencia e deliberada: este mob nao tem
`WeakPointResolver`. Marca de ponto fraco em mob sem ponto fraco nao da erro
nenhum -- da um jogador mirando com capricho numa regiao que nao paga nada, e
achando que o dano esta bugado.

Regerar:  python art-source/enemies/avian_commander/avian_commander_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/avian_commander/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import geometria as geo      # noqa: E402
from comum import textura as tex        # noqa: E402
from avian_commander_geo import (ANTENAS, CAIXAS, GARRAS, MOB,  # noqa: E402
                                 UV_ALTURA, UV_LARGURA, VENTRE_DE_COMANDO)

# Degrau MINIMO de luminancia entre a marca de quimera e a barriga em volta dela.
#
# 45 e o que sobrevive ao mip-map a dez blocos -- e dez blocos e a distancia de
# trabalho deste mob, porque a altitude de comando e sete. Com 20 a marca vira uma
# sujeira na barriga; com 100 a barriga teria de ser quase preta, e uma ave de
# ventre preto contra o ceu e uma silhueta sem detalhe nenhum, que e o oposto do
# que esta folha existe para entregar.
#
# E um limite de LEITURA, e nao de balanceamento: ninguem vai gira-lo numa sessao
# de ajuste, e por isso ele mora aqui, ao lado do comentario que o explica.
DEGRAU_MINIMO_DE_LUMINANCIA = 45.0

PALETA = tex.Paleta(
    PLUMA=(62, 68, 86),          # a pena do flanco: ardosia azulada
    PLUMA_LUZ=(92, 100, 120),    # o VENTRE -- e ele que o jogador ve
    PLUMA_SOMBRA=(40, 44, 58),   # o dorso: escuro para sumir visto de cima
    QUITINA=(86, 62, 48),        # o abdome, a crista, as antenas -- o lado formiga
    QUITINA_LUZ=(124, 94, 70),   # o bico e o alto da quitina
    QUITINA_SOMBRA=(58, 42, 34),  # pernas e o que fica por baixo da quitina
    GARRA=(206, 198, 186),       # a garra: clara, e pequena de proposito
    OLHO=(214, 230, 236),        # a esclera, palida
    PUPILA=(24, 20, 28),         # o unico preto contado da folha
    # --- o tom abaixo SO pode aparecer na base das pecas do ventre de comando ---
    MARCA=(232, 176, 66),        # a marca de quimera: ambar, o contraste alto
)

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)

# As pecas de pena. O complemento -- quitina -- e escrito por extenso porque a
# regua precisa citar a peca pelo nome quando reprova.
PENA = ("thorax", "head", "wing_left", "wing_right",
        "wing_tip_left", "wing_tip_right", "tail")
QUITINA = ("abdomen", "crest", "beak") + ANTENAS + GARRAS + ("leg_left", "leg_right")


def _pena(p, salpico):
    """O tratamento comum de toda peca emplumada: dorso escuro, ventre claro.

    `salpico` e a semente. Semente repetida entre duas pecas faria as duas
    receberem o MESMO mosqueado, e padrao repetido le como textura de parede --
    nao como pena.
    """
    p.tudo(PALETA.PLUMA)
    p.face("topo", PALETA.PLUMA_SOMBRA)
    p.face("base", PALETA.PLUMA_LUZ)
    p.face("tras", PALETA.PLUMA_SOMBRA)
    for face in tex.PAREDES:
        p.salpicar(face, PALETA.PLUMA_SOMBRA, 46, semente=salpico)
        p.salpicar(face, PALETA.PLUMA_LUZ, 22, semente=salpico + 7)


def _quitina(p, salpico, claro=False):
    """A quitina da formiga: faixa escura, com o topo iluminado."""
    p.tudo(PALETA.QUITINA_LUZ if claro else PALETA.QUITINA)
    p.face("topo", PALETA.QUITINA_LUZ)
    p.face("base", PALETA.QUITINA_SOMBRA)
    for face in tex.PAREDES:
        p.salpicar(face, PALETA.QUITINA_SOMBRA, 40, semente=salpico)


def _marcar_ventre(p):
    """A marca de quimera, na face de BAIXO, com pixel exato.

    `forca=0` em tudo: o ruido aqui seria o defeito. A regua compara o tom EXATO
    da paleta para saber se a marca esta onde devia e se ela nao vazou para o
    dorso -- com ruido, o mesmo pixel sairia com tres valores diferentes e a
    comparacao deixaria de significar alguma coisa.
    """
    _, _, largura, altura = p.faces()["base"]
    meio = altura // 2
    p.linha("base", meio, PALETA.MARCA, forca=0)
    if altura >= 3:
        # A segunda fiada faz a marca ler como FAIXA e nao como risco: um risco de
        # um pixel desaparece no primeiro nivel de mip-map, e o primeiro nivel de
        # mip-map e onde este bicho vive.
        p.linha("base", meio - 1, PALETA.MARCA, forca=0)
    if largura >= 5 and altura >= 5:
        # Os dois pontos nas pontas abrem a faixa num "V" -- a forma que se
        # reconhece contra o ceu mesmo quando a faixa vira uma linha borrada.
        p.ponto("base", 1, meio + 1, PALETA.MARCA)
        p.ponto("base", largura - 2, meio + 1, PALETA.MARCA)


def pintar():
    # -------------------------------------------------------------- tronco
    _pena(folha["thorax"], 3)
    # A linha do dorso, em quitina: e o unico sinal, visto de cima, de que aquele
    # passaro nao e um passaro.
    folha["thorax"].linha_central("topo", 2, PALETA.QUITINA_SOMBRA)

    # -------------------------------------------------------------- abdome
    abdome = folha["abdomen"]
    _quitina(abdome, 11)
    # Os segmentos: tres fiadas escuras nos flancos. Segmento e o que diz FORMIGA
    # num corpo que, de resto, e de ave.
    for face in tex.LADOS + ("tras",):
        for dy in (1, 3, 5):
            abdome.linha(face, dy, PALETA.QUITINA_SOMBRA)

    # -------------------------------------------------------------- cabeca
    cabeca = folha["head"]
    _pena(cabeca, 19)
    # Os olhos ficam nos LADOS: ave tem olho lateral, e e disso que depende a
    # leitura de "ela esta me vendo mesmo de lado" quando ela circula no alto.
    for face in tex.LADOS:
        cabeca.na_face(face, 1, 2, 3, 2, PALETA.OLHO, forca=0)
        cabeca.ponto(face, 2, 3, PALETA.PUPILA)
    # A mascara escura da testa, fechando a crista que vem de cima.
    cabeca.faixa_no_topo("frente", 2, PALETA.QUITINA_SOMBRA)

    # ------------------------------------------------------- bico e crista
    _quitina(folha["beak"], 23, claro=True)
    folha["beak"].faixa_no_pe("frente", 1, PALETA.QUITINA_SOMBRA)
    _quitina(folha["crest"], 29)
    folha["crest"].face("topo", PALETA.QUITINA_LUZ)

    # ------------------------------------------------------------- antenas
    for i, nome in enumerate(ANTENAS):
        antena = folha[nome]
        _quitina(antena, 31 + i * 6)
        # A ponta clara: uma antena inteira escura desaparece contra o dorso, e a
        # marca de quimera estrutural (ver avian_commander_geo.valida_marca_de_quimera)
        # deixa de aparecer justamente no unico angulo em que ela seria vista.
        antena.face("topo", PALETA.GARRA)

    # ---------------------------------------------------------------- asas
    for nome, semente in (("wing_left", 37), ("wing_right", 41),
                          ("wing_tip_left", 43), ("wing_tip_right", 47)):
        asa = folha[nome]
        _pena(asa, semente)
        # A borda de fuga escura: e ela que da contorno a asa aberta contra o ceu.
        # Sem contorno, a asa aberta e a asa dobrada viram a mesma mancha clara.
        asa.faixa_no_pe("tras", 1, PALETA.PLUMA_SOMBRA)

    # --------------------------------------------------------------- cauda
    cauda = folha["tail"]
    _pena(cauda, 53)
    for face in tex.LADOS:
        cauda.faixa_no_topo(face, 1, PALETA.PLUMA_SOMBRA)

    # ------------------------------------------------------ pernas e garras
    for nome, semente in (("leg_left", 59), ("leg_right", 61)):
        perna = folha[nome]
        perna.tudo(PALETA.QUITINA_SOMBRA)
        perna.face("topo", PALETA.QUITINA)
        for face in tex.PAREDES:
            perna.salpicar(face, PALETA.QUITINA, 30, semente=semente)
    for i, nome in enumerate(GARRAS):
        garra = folha[nome]
        _quitina(garra, 67 + i * 6)
        # A ponta da garra e clara e PEQUENA. Grande, ela viraria um segundo ponto
        # de contraste alto e brigaria com a marca de quimera a distancia.
        garra.faixa_no_pe("frente", 1, PALETA.GARRA, forca=0)

    # -------------------------------------------------- a marca de quimera
    # Por ultimo, e so no ventre: pintada antes, o salpicado das pecas passaria por
    # cima dela e a regua encontraria a marca faltando em metade das faces.
    for nome in VENTRE_DE_COMANDO:
        _marcar_ventre(folha[nome])


# ------------------------------------------ validacoes que ligam arte e regra

def _luminancia(pixel):
    """Luminancia perceptual, a mesma conta que o olho humano faz de longe.

    Media simples dos canais mediria ERRADO: (0,255,0) e (255,0,0) tem a mesma
    media e brilhos completamente diferentes, e a regua aprovaria um verde
    berrante como se fosse tao discreto quanto um vermelho escuro.
    """
    return 0.299 * pixel[0] + 0.587 * pixel[1] + 0.114 * pixel[2]


def _pixels_da_face(f, nome, face):
    """So os pixels de UMA face -- nunca os cantos do layout de caixa.

    O retangulo de uma caixa tem cantos que nenhuma face usa e que ficam
    transparentes de proposito. Medi-los faria toda media puxar para o preto, e a
    regua de separacao aprovaria qualquer coisa.
    """
    x, y, largura, altura = f[nome].faces()[face]
    for i in range(x, x + largura):
        for j in range(y, y + altura):
            yield f.px[i, j][:3]


def valida_marca_de_quimera_le_de_baixo(f):
    """A marca tem de estar onde o jogador olha -- e so la.

    A regra do servidor poe esta comandante a `ALTITUDE_DE_COMANDO` blocos do chao
    enquanto ela comanda. Disso decorre que a UNICA superficie dela que trabalha e
    a de baixo, e tres coisas sao cobradas:

    1. PRESENCA -- a marca aparece na base de toda peca de VENTRE_DE_COMANDO. Uma
       peca sem marca nao da erro: da um pedaco da barriga que nao diz nada, e o
       "V" que o jogador aprende a reconhecer fica pela metade num angulo so.
    2. AUSENCIA NO DORSO -- nenhum pixel da marca em face de topo. Marca no dorso
       existe no atlas e nunca na tela: ela seria uma linha de checklist cumprida
       que nenhum jogador consegue conferir, e o custo real e o contraste gasto
       fora do lugar.
    3. DEGRAU -- a marca tem de se separar da barriga em volta por
       DEGRAU_MINIMO_DE_LUMINANCIA. Empatadas, as duas viram uma mancha so quando
       o mip-map entra -- e o mip-map entra a partir de poucos blocos, que e onde
       este mob passa o encontro inteiro.

    Nenhuma das tres levanta excecao em lugar nenhum do jogo, nenhuma aparece no
    atlas aberto num editor, e as tres se manifestam do mesmo jeito: "esse bicho
    parece so um passaro grande".
    """
    marca = PALETA.MARCA

    for nome in VENTRE_DE_COMANDO:
        base = list(_pixels_da_face(f, nome, "base"))
        if marca not in base:
            raise geo.ErroDeArte(
                "a peca '%s' nao tem a marca de quimera na face de baixo: enquanto ela comanda, "
                "essa e a unica face que o jogador ve, e um pedaco da barriga sem marca deixa o "
                "sinal pela metade num angulo so" % nome)
        fundo = [p for p in base if p != marca]
        if not fundo:
            raise geo.ErroDeArte(
                "a face de baixo de '%s' e SO marca: sem barriga em volta nao ha contraste, e a "
                "marca deixa de ser marca para virar a cor da peca" % nome)
        degrau = abs(_luminancia(marca) - (sum(_luminancia(p) for p in fundo) / len(fundo)))
        if degrau < DEGRAU_MINIMO_DE_LUMINANCIA:
            raise geo.ErroDeArte(
                "na peca '%s' a marca tem luminancia %.1f e a barriga em volta tem %.1f: o degrau "
                "e %.1f e o minimo e %.1f. Empatadas, as duas viram uma mancha so no mip-map, e a "
                "marca de quimera some justamente na distancia em que este mob e visto"
                % (nome, _luminancia(marca),
                   sum(_luminancia(p) for p in fundo) / len(fundo),
                   degrau, DEGRAU_MINIMO_DE_LUMINANCIA))

    for caixa in CAIXAS:
        if marca in list(_pixels_da_face(f, caixa.nome, "topo")):
            raise geo.ErroDeArte(
                "o tom da marca de quimera %s aparece no TOPO da caixa '%s': ela comanda de sete "
                "blocos de altura, entao o dorso dela nunca e visto em jogo -- a marca existiria "
                "no atlas e em lugar nenhum da tela, e o contraste teria sido gasto fora do lugar"
                % (str(marca), caixa.nome))

    print("marca de quimera: luminancia %.1f, degrau minimo exigido %.1f, em %d pecas do ventre"
          % (_luminancia(marca), DEGRAU_MINIMO_DE_LUMINANCIA, len(VENTRE_DE_COMANDO)))


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_marca_de_quimera_le_de_baixo,))
