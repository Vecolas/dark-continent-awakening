"""Geometria do Melanin Lizard -- o lagarto que se confunde com a rocha e AGARRA.

O QUE A SILHUETA PRECISA ENTREGAR, e por que cada peca esta aqui.

Este bicho tem UMA mecanica que so funciona se o desenho colaborar: enquanto
ninguem olha para ele de perto, ele fica imovel e deixa de ser alvo. Um lagarto
alto, esguio e de dorso liso NAO sustenta isso -- o jogador que descobre a
camuflagem depois de passar ao lado dela sente que o jogo trapaceou. Entao a
forma e baixa, larga e cheia de quina: corpo rente ao chao, pernas ABERTAS para
os lados (sprawling, e nao sob o corpo), e uma CRISTA de placas nas costas que
e a unica coisa que aparece de cima -- que e de onde o jogador olha.

A HITBOX MANDA NO MODELO. A entidade e sized(1.6F, 0.8F) -- 25.6 x 12.8 px --
e ela e DEITADA: mais comprida que alta. A bateria da biblioteca reprova se o
modelo estourar qualquer um dos tres eixos, e neste bicho o eixo que apertou foi
o COMPRIMENTO: focinho e ponta da cauda somam 25 px dos 25.6 disponiveis. Nao
ha folga para "esticar um pouco a cauda" sem encurtar outra coisa.

O piso fica em y=0 -- e as PATAS que encostam nele, nao as pernas.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

Regerar:  python art-source/enemies/melanin_lizard/melanin_lizard_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/melanin_lizard.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "melanin_lizard"
UV_LARGURA, UV_ALTURA = 64, 64

# O literal da chamada Java, copiado como esta:
# EnemyEntityTypes.MELANIN_LIZARD -> .sized(1.6F, 0.8F)
HITBOX = (1.6, 0.8)

# ---------------------------------------------------------- numeros do bicho
#
# Os tres abaixo NAO sao gosto: sao numeros que o SERVIDOR tambem conhece, e a
# divergencia entre o desenho e a regra nao da erro nenhum -- da um bicho que
# promete com a silhueta o que a regra nao paga. As validacoes semanticas no fim
# deste arquivo os cobram.

# MelaninLizardTuning.CAIXA_DO_BOTE -> minZ e maxZ, em BLOCOS.
#
# Cuidado com o eixo: a caixa de ataque e matematica de MUNDO, onde a frente e
# +Z (yaw 0 olha para +Z). Aqui, na geometria Bedrock, a frente e -Z. Os dois
# numeros abaixo ja estao no sinal do MUNDO, e a comparacao converte o focinho.
# Misturar as duas convencoes poe a caixa de dano ATRAS do bicho, e isso nao da
# erro: ele ataca, anima, e quem apanha e quem estava pelas costas.
BOTE_PERTO_BLOCOS = 0.4
BOTE_LONGE_BLOCOS = 2.0

# MelaninLizardTuning.FRACAO_DE_ENCAIXE_DA_VITIMA = 0.3D.
# E a altura, em fracao da hitbox, onde o Java prende a vitima agarrada. Se ela
# cair fora da mandibula desenhada, o jogador preso flutua ao lado de uma boca
# aberta que nao esta segurando nada -- e nenhum portao ve isso.
FRACAO_DE_ENCAIXE_DA_VITIMA = 0.3

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: as animacoes escrevem contra estes nomes, e nome errado
# nao da erro -- o GeckoLib so deixa o osso parado.
#
# A MANDIBULA e osso proprio, pendurada na CABECA e com o pivot na dobradica de
# TRAS (z=-5). E isso que faz "abrir a boca" ser uma rotacao e nao uma translacao
# inventada: a massa da mandibula fica a frente da dobradica, entao ela abre
# sozinha quando a cabeca gira. Fundida a cabeca, o bote seria uma cabeca inteira
# girando, que le como cabecada e nao como mordida -- e este bicho AGARRA com a
# boca.
#
# As PATAS sao ossos proprios, e nao a ponta das pernas, porque elas sao o que
# encosta no chao: num bicho sprawling a pata fica plana enquanto a perna gira, e
# isso so existe se houver um osso para contra-girar (`anim.derivar`).
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 5, 0)),
    geo.Osso("head", "body", (0, 6, -5)),
    geo.Osso("jaw", "head", (0, 5, -5)),
    geo.Osso("crest", "body", (0, 8, 0)),
    geo.Osso("tail_base", "body", (0, 6, 7)),
    geo.Osso("tail_tip", "tail_base", (0, 5.5, 12)),
    geo.Osso("leg_front_left", "body", (4, 3, -3)),
    geo.Osso("foot_front_left", "leg_front_left", (5, 1, -3)),
    geo.Osso("leg_front_right", "body", (-4, 3, -3)),
    geo.Osso("foot_front_right", "leg_front_right", (-5, 1, -3)),
    geo.Osso("leg_back_left", "body", (4, 3, 4)),
    geo.Osso("foot_back_left", "leg_back_left", (5, 1, 4)),
    geo.Osso("leg_back_right", "body", (-4, 3, 4)),
    geo.Osso("foot_back_right", "leg_back_right", (-5, 1, 4)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
CAIXAS = (
    # O tronco: 8 de largura por 5 de altura num bicho de 12.8 px de hitbox. Ele
    # e CHATO de proposito -- um tronco quadrado leria como jacare de brinquedo,
    # e o que vende a rocha e a proporcao deitada.
    geo.Caixa("corpo", "body", 0, 0, -4, 3, -5, 8, 5, 12),
    # A cabeca e larga e curta, encaixada na frente do tronco. O topo dela (y=9)
    # fica ACIMA do tronco (y=8): e a unica parte da cabeca que aparece de cima,
    # e e por isso que o olho e pintado no alto dela.
    geo.Caixa("cabeca", "head", 40, 0, -3, 5, -11, 6, 4, 6),
    # A mandibula fecha no plano y=5, encostando na cabeca sem dividir volume com
    # ela -- faces OPOSTAS no mesmo plano sao o jeito normal de montar e as
    # normais contrarias resolvem a disputa; o que cintilaria seria a MESMA face.
    geo.Caixa("mandibula", "jaw", 0, 17, -3, 3, -10, 6, 2, 5),
    # A CRISTA: as placas do dorso. Ela e a peca que faz a camuflagem ser
    # honesta, porque e a unica silhueta que o jogador ve olhando de cima para um
    # bicho de 0.8 bloco de altura. Sem ela o lagarto vira uma lasca lisa no chao
    # e a regra de "nao e alvo" passa a parecer truque.
    geo.Caixa("crista", "crest", 0, 24, -2, 8, -4, 4, 2, 10),
    geo.Caixa("cauda", "tail_base", 40, 14, -2, 4, 7, 4, 3, 5),
    geo.Caixa("ponta_da_cauda", "tail_tip", 40, 10, -1, 4.5, 12, 2, 2, 2),
    # PERNAS ABERTAS: elas saem do flanco (x=4) para fora (x=6), e nao para baixo
    # sob o corpo. Isso e o que separa um lagarto de um cachorro na silhueta, e
    # custa 4 px de largura que a hitbox de 25.6 tem de sobra.
    geo.Caixa("perna_frente_esq", "leg_front_left", 28, 24, 4, 1, -4, 2, 2, 2),
    geo.Caixa("perna_frente_dir", "leg_front_right", 36, 24, -6, 1, -4, 2, 2, 2),
    geo.Caixa("perna_tras_esq", "leg_back_left", 44, 24, 4, 1, 3, 2, 2, 2),
    geo.Caixa("perna_tras_dir", "leg_back_right", 52, 24, -6, 1, 3, 2, 2, 2),
    # As patas tem 1 px de altura e sao mais largas que a perna: e o pe espalmado
    # que encosta no chao. Elas, e so elas, tocam y=0.
    geo.Caixa("pata_frente_esq", "foot_front_left", 0, 36, 3, 0, -5, 3, 1, 3),
    geo.Caixa("pata_frente_dir", "foot_front_right", 12, 36, -6, 0, -5, 3, 1, 3),
    geo.Caixa("pata_tras_esq", "foot_back_left", 24, 36, 3, 0, 2, 3, 1, 3),
    geo.Caixa("pata_tras_dir", "foot_back_right", 36, 36, -6, 0, 2, 3, 1, 3),
)


def valida_focinho_dentro_do_alcance_do_bote(m):
    """O focinho desenhado tem de cair DENTRO da caixa de dano do bote.

    Duas falhas moram aqui, e nenhuma das duas da erro:

    1. focinho ALEM do limite distante -- a silhueta promete alcance que o
       servidor nao paga, e quem calcula a distancia pelo desenho apanha sem
       entender por que;
    2. focinho AQUEM do limite proximo -- pior, e mais facil de cometer: o
       jogador encostado nos dentes NAO leva dano, porque a caixa comeca depois
       do focinho. O relato que chega e "o bicho as vezes morde e nao acerta".

    A conversao de sinal e o detalhe caro: a geometria Bedrock tem a frente em
    -Z e a caixa de ataque e matematica de MUNDO, onde a frente e +Z.
    """
    (_, _), (_, _), (z0, _) = m.limites()
    focinho_blocos = -z0 / 16.0          # -Z do modelo vira +Z do mundo
    if focinho_blocos > BOTE_LONGE_BLOCOS:
        raise geo.ErroDeArte(
            "o focinho chega a %.3f bloco a frente e a caixa do bote termina em %.3f: a silhueta "
            "promete um alcance que o servidor nao entrega, e quem mede a distancia pelo desenho "
            "apanha sem saber por que" % (focinho_blocos, BOTE_LONGE_BLOCOS))
    if focinho_blocos < BOTE_PERTO_BLOCOS:
        raise geo.ErroDeArte(
            "o focinho chega a %.3f bloco a frente e a caixa do bote so comeca em %.3f: quem "
            "encostar nos dentes fica no vao morto entre a boca e o dano, e o relato e 'o bicho "
            "morde e nao acerta'" % (focinho_blocos, BOTE_PERTO_BLOCOS))


def valida_boca_na_altura_de_encaixe_da_vitima(m):
    """A vitima agarrada tem de ficar DENTRO da mandibula desenhada.

    O Java prende o passageiro em `altura da hitbox * FRACAO_DE_ENCAIXE_DA_VITIMA`
    e nao sabe nada sobre onde a boca foi desenhada. Se alguem subir a cabeca do
    modelo, ou girar a fracao numa sessao de balanceamento, o agarrao continua
    funcionando perfeitamente -- com o jogador preso flutuando ao lado de uma
    boca que nao esta segurando nada. Nenhum portao ve isso, e o jogador nao tem
    vocabulario para reportar.
    """
    _, (y0, y1), _ = geo.volume(m.caixa("mandibula"))
    encaixe = FRACAO_DE_ENCAIXE_DA_VITIMA * m.hitbox_altura_px
    if not y0 <= encaixe <= y1:
        raise geo.ErroDeArte(
            "a vitima e presa a %.2f px de altura (%.2f de %.1f px de hitbox) e a mandibula vai de "
            "y=%s a y=%s: o agarrao funciona e o preso aparece flutuando fora da boca"
            % (encaixe, FRACAO_DE_ENCAIXE_DA_VITIMA, m.hitbox_altura_px, y0, y1))


def valida_crista_e_o_ponto_mais_alto(m):
    """A crista tem de ser o topo do bicho -- e e ela que a camuflagem mostra.

    Visto de cima, um lagarto de 0.8 bloco e quase so dorso. Se qualquer outra
    peca passar por cima da crista, o que o jogador ve de pe nao e mais a fileira
    de placas que a textura pinta como rocha: e um pedaco liso de couro. A regra
    de camuflagem continua valendo igual no servidor, e e ai que ela vira
    trapaca -- o bicho deixa de ser alvo sem nada na tela justificando.
    """
    _, (_, topo_da_crista), _ = geo.volume(m.caixa("crista"))
    for c in m.caixas:
        if c.nome == "crista":
            continue
        _, (_, topo), _ = geo.volume(c)
        if topo > topo_da_crista:
            raise geo.ErroDeArte(
                "a peca '%s' sobe ate y=%s e a crista para em y=%s: de cima o jogador deixa de ver "
                "as placas que a textura vende como rocha, e a camuflagem passa a parecer truque"
                % (c.nome, topo, topo_da_crista))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_focinho_dentro_do_alcance_do_bote,
                          valida_boca_na_altura_de_encaixe_da_vitima,
                          valida_crista_e_o_ponto_mais_alto))
