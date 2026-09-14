"""Clipes do Wolf Pack Hunter -- a representacao das fases que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, STAGGER OU RECOMPENSA. O servidor publica a
fase (`AttackPhase`) e o cambaleio; o cliente escolhe o clipe correspondente. Se
a animacao e a hitbox discordarem, quem esta errado e este arquivo.

A FORMA DO TELEGRAFO E A FICHA DO BICHO, E ELA E O OPOSTO DA DO CICLOPE. O
gigante avisa por um segundo e meio; este lobo avisa por MEIO SEGUNDO (10
ticks). Isso e deliberado: a ameaca deste mob nunca foi o golpe individual --
sao os outros tres chegando enquanto voce olha para este. Um windup longo daria
ao jogador tempo de tratar cada lobo como um duelo, e o encontro inteiro se
apagaria sem nada acusar.

Meio segundo, porem, e curto demais para ser lido pelo corpo todo. Por isso o
aviso acontece em DUAS pecas que mudam de lugar na silhueta: o quadril afunda
(o lobo se agacha) e o focinho ABRE. Num bicho de 14 px, cor e detalhe somem a
dez blocos; mudanca de contorno, nao.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com o campo de origem ao lado. A
regua da biblioteca reprova se a soma dos tres clipes encadeados for menor que
windup + active + recovery. Clipe mais curto faz o lobo RELAXAR no meio da
mordida que ainda vai acertar: dano certo, cooldown certo, log limpo, e a unica
coisa que o jogador tem para ler quebrada.

E HA UMA SEGUNDA REGUA, PROPRIA DESTE ARQUIVO. A biblioteca cobra a duracao do
ATAQUE contra o servidor, e nao cobra a do CAMBALEIO -- e o cambaleio deste mob
dura 25 ticks (`GreedIslandProfiles.wolfPackHunterStagger()`). Um clipe de
stagger mais curto que a janela devolve o lobo a pose neutra enquanto ele ainda
esta interrompido, e o jogador le "ele se recuperou" e recua de um bicho que
nao podia revidar. `valida_cambaleio_cobre_a_janela` fecha isso.

CONVENCAO DE ROTACAO, medida neste geo e nao decorada. Com -Z na frente:
  * rotacao X NEGATIVA num membro pendurado (perna, pata) o joga para a FRENTE;
    X POSITIVA o joga para TRAS;
  * rotacao X POSITIVA na cabeca e no focinho abaixa o nariz; NEGATIVA o levanta;
  * rotacao X POSITIVA no corpo levanta a garupa e abaixa o peito.
O lobo se agacha no aviso (peito baixo, garupa alta: X positivo no corpo) e se
estica na mordida (X negativo).

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/wolf_pack_hunter/wolf_pack_hunter_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/wolf_pack_hunter.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import ErroDeArte, TICKS_POR_SEGUNDO     # noqa: E402
from comum import animacao as anim                  # noqa: E402

MOB = "wolf_pack_hunter"

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType
# do Java VENCE o JSON, e cravar a repeticao la transformaria este dicionario em
# documentacao que discorda do comportamento.
#
# Esta e tambem a UNICA lista de clipes deste mob, e ela e um CONTRATO com
# WolfPackHunterEntity: os sete nomes aparecem la como literais
# "animation.wolf_pack_hunter.<clipe>". Nome trocado nao da erro -- o GeckoLib
# procura, nao acha, e o osso fica parado.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o aviso termina com o lobo AGACHADO e o focinho aberto,
    # e fica assim. Voltando ao repouso no fim do clipe, ele desarmaria a mordida
    # na tela enquanto o servidor ainda esta em WINDUP -- num telegrafo de meio
    # segundo isso e a diferenca entre um aviso e nenhum.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de WolfPackHunterTuning.mordida(), com o campo de origem ao lado.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=10,     # WolfPackHunterTuning.WINDUP_DA_MORDIDA
                       active=4,      # WolfPackHunterTuning.JANELA_DA_MORDIDA
                       recovery=8)}   # WolfPackHunterTuning.RECUPERACAO_DA_MORDIDA

# Ticks COPIADOS de GreedIslandProfiles.wolfPackHunterStagger(): o quarto campo
# de StaggerRules(12.0F, 1.0F, 0.4F, 25). E cobrado por
# `valida_cambaleio_cobre_a_janela`, logo abaixo.
TICKS_DE_CAMBALEIO = 25

DUR_IDLE = 2.4
DUR_WALK = 0.6       # trote rapido: velocidade 0.34 e a maior das sete criaturas
DUR_WINDUP = 0.5     # 10 ticks
DUR_STRIKE = 0.25    # 5 ticks -- um a mais que a janela, de proposito
DUR_RECOVERY = 0.45  # 9 ticks
DUR_STAGGER = 1.3    # 26 ticks -- um a mais que a janela de cambaleio do servidor
DUR_DEATH = 1.1

# Arrasto da orelha atras da cabeca. Negativo e pequeno: a orelha fica para tras
# do movimento. Fator positivo faria a orelha ANTECIPAR o pescoco, que e o
# movimento de quem ja sabia -- e este bicho reage, nao antecipa.
ARRASTO_DA_ORELHA = -0.45

# Arrasto do focinho. Menor ainda: o focinho e preso na cara, nao pendurado nela.
ARRASTO_DO_FOCINHO = -0.18


def _quatro_patas(bones, dianteira_esquerda_graus, fase_cruzada=0.5, duracao=None,
                  periodo=None):
    """A passada cruzada das quatro pernas, com as patas SEMPRE derivadas.

    Cruzada porque e o que um quadrupede faz: dianteira esquerda anda com a
    traseira direita. Escrever as quatro a mao daria um lobo saltitando com as
    quatro juntas, e isso nao levanta erro -- so faz um predador parecer um
    coelho.

    As patas nunca sao escritas a mao. Escritas, elas ficam com a fase certa hoje
    e errada na primeira correcao da canela, e a correcao nao da erro: da uma
    pata arrastando meio quadro atras da perna.
    """
    duracao = duracao if duracao is not None else DUR_WALK
    periodo = periodo if periodo is not None else duracao
    anim.curva(bones, "leg_front_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(duracao, periodo, dianteira_esquerda_graus)])
    anim.curva(bones, "leg_back_right", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(duracao, periodo, dianteira_esquerda_graus)])
    anim.curva(bones, "leg_front_right", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(duracao, periodo, dianteira_esquerda_graus,
                                fase=fase_cruzada)])
    anim.curva(bones, "leg_back_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(duracao, periodo, dianteira_esquerda_graus,
                                fase=fase_cruzada)])
    for perna in ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"):
        anim.derivar(bones, perna, perna.replace("leg_", "paw_"), "rotation", -0.55)
    return bones


def _cabeca_completa(bones):
    """Orelhas e focinho SEMPRE derivados da cabeca, em todo clipe.

    Num corpo de 14 px, a cabeca e quase metade da silhueta. Cabeca que se mexe
    com orelha parada le como adesivo colado no bicho -- e isso nao aparece em
    portao nenhum, so na tela.
    """
    anim.derivar(bones, "head", "ear_left", "rotation", ARRASTO_DA_ORELHA)
    anim.derivar(bones, "head", "ear_right", "rotation", ARRASTO_DA_ORELHA)
    anim.derivar(bones, "head", "snout", "rotation", ARRASTO_DO_FOCINHO)
    return bones


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # ---------------------------------------------------------------- ocio
    # Respiracao lenta e a cabeca varrendo de um lado ao outro. O varrimento em Y
    # nao e enfeite: um lobo parado que NAO olha em volta le como estatua, e
    # estatua nao explica por que ele chamou os outros tres.
    ocio = {}
    anim.curva(ocio, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 1.4)])
    anim.curva(ocio, "head", "rotation",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 9.0, fase=0.15)])
    _cabeca_completa(ocio)
    anim.curva(ocio, "tail", "rotation",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 7.0)])
    # Amplitude minuscula nas patas: o peso passa de um par ao outro sem passada.
    _quatro_patas(ocio, 2.0, duracao=DUR_IDLE, periodo=DUR_IDLE)
    a.clipe("idle", DUR_IDLE, ocio)

    # ------------------------------------------------------------ locomocao
    # Passada CURTA e rapida: 0.6 s por ciclo. Velocidade 0.34 com passada longa
    # daria um lobo que anda em camera lenta enquanto atravessa o terreno, e
    # ninguem consegue descrever esse defeito -- so diz que "esta estranho".
    marcha = {}
    _quatro_patas(marcha, 26.0)
    anim.curva(marcha, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 3.0)])
    anim.derivar(marcha, "body", "head", "rotation", -0.7)
    _cabeca_completa(marcha)
    # O solavanco vertical tem periodo METADE do ciclo: sao duas pisadas por
    # ciclo, e uma so faria o lobo mancar.
    anim.curva(marcha, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 0.5)])
    anim.curva(marcha, "tail", "rotation",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 12.0)])
    a.clipe("walk", DUR_WALK, marcha)

    # --------------------------------------------------------------- windup
    # MEIO SEGUNDO DE AVISO, e ele tem de caber na silhueta. Tres coisas mudam de
    # LUGAR (nao de forma): o corpo afunda 1.5 px, a garupa sobe, e o focinho
    # abre. Mudanca de contorno e o que se le a dez blocos no meio do mato; cor e
    # detalhe, nao.
    #
    # A maior parte do curso acontece na SEGUNDA metade. Arranque lento e o que da
    # ao jogador tempo de decidir antes do ponto de nao-retorno -- e com 10 ticks
    # de orcamento, esse tempo e tudo que ha.
    aviso = {}
    anim.curva(aviso, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.4, anim.vetor(x=4)),
                (DUR_WINDUP, anim.vetor(x=15))])
    anim.curva(aviso, "body", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.4, anim.vetor(y=-0.4)),
                (DUR_WINDUP, anim.vetor(y=-1.5))])
    # A cabeca ENCARA o alvo enquanto o corpo se agacha: ela sobe contra a
    # inclinacao do tronco. Cabeca acompanhando o tronco esconderia o focinho
    # atras do peito, e o focinho e metade do aviso.
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-18))])
    anim.derivar(aviso, "head", "ear_left", "rotation", ARRASTO_DA_ORELHA)
    anim.derivar(aviso, "head", "ear_right", "rotation", ARRASTO_DA_ORELHA)
    # O focinho ABRE -- escrito a mao e nao derivado, porque abrir a boca e o
    # unico movimento deste clipe que nao e arrasto de outra peca.
    anim.curva(aviso, "snout", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.4, anim.vetor(x=6)),
                (DUR_WINDUP, anim.vetor(x=26))])
    anim.curva(aviso, "tail", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-22))])
    # As quatro pernas dobram JUNTAS: e o agachamento, e nao uma passada.
    _quatro_patas(aviso, 0.0, duracao=DUR_WINDUP, periodo=DUR_WINDUP)
    anim.curva(aviso, "leg_front_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=14))])
    anim.curva(aviso, "leg_front_right", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=14))])
    anim.curva(aviso, "leg_back_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-16))])
    anim.curva(aviso, "leg_back_right", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-16))])
    for perna in ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"):
        anim.derivar(aviso, perna, perna.replace("leg_", "paw_"), "rotation", -0.55)
    a.clipe("windup", DUR_WINDUP, aviso)

    # --------------------------------------------------------------- strike
    # A JANELA QUE MACHUCA. O servidor gasta 4 ticks (0.2 s) e o clipe dura 0.25:
    # o excedente e permitido de proposito -- o servidor manda no fim e o Java
    # corta o clipe. Faltar e que nao pode.
    #
    # O corpo se ESTICA e avanca: z NEGATIVO e para a frente nesta geometria. Esse
    # avanco nao e enfeite -- ele e a metade visual do salto que o servidor aplica
    # (WolfPackHunterTuning.AVANCO_DA_INVESTIDA), e e ele que faz a caixa de
    # mordida chegar onde o focinho desenhado sozinho nao chegaria. Tirar o avanco
    # da tela sem tirar do servidor deixa o jogador levando dano de um lobo parado.
    mordida = {}
    anim.curva(mordida, "body", "rotation",
               [(0.0, anim.vetor(x=15)), (DUR_STRIKE * 0.5, anim.vetor(x=-14)),
                (DUR_STRIKE, anim.vetor(x=-6))])
    anim.curva(mordida, "body", "position",
               [(0.0, anim.vetor(y=-1.5)), (DUR_STRIKE * 0.5, anim.vetor(y=0.8, z=-2.6)),
                (DUR_STRIKE, anim.vetor(y=0.4, z=-1.6))])
    anim.curva(mordida, "head", "rotation",
               [(0.0, anim.vetor(x=-18)), (DUR_STRIKE * 0.5, anim.vetor(x=16)),
                (DUR_STRIKE, anim.vetor(x=8))])
    anim.derivar(mordida, "head", "ear_left", "rotation", ARRASTO_DA_ORELHA)
    anim.derivar(mordida, "head", "ear_right", "rotation", ARRASTO_DA_ORELHA)
    # O focinho FECHA no meio da janela. Fechar depois do fim do clipe deixaria a
    # boca aberta durante a recuperacao inteira, e a leitura viraria "ele ainda
    # esta mordendo" -- justamente na janela em que o jogador deveria punir.
    anim.curva(mordida, "snout", "rotation",
               [(0.0, anim.vetor(x=26)), (DUR_STRIKE * 0.5, anim.vetor(x=-4)),
                (DUR_STRIKE, anim.vetor())])
    anim.curva(mordida, "tail", "rotation",
               [(0.0, anim.vetor(x=-22)), (DUR_STRIKE, anim.vetor(x=10))])
    anim.curva(mordida, "leg_front_left", "rotation",
               [(0.0, anim.vetor(x=14)), (DUR_STRIKE * 0.5, anim.vetor(x=-30)),
                (DUR_STRIKE, anim.vetor(x=-12))])
    anim.curva(mordida, "leg_front_right", "rotation",
               [(0.0, anim.vetor(x=14)), (DUR_STRIKE * 0.5, anim.vetor(x=-30)),
                (DUR_STRIKE, anim.vetor(x=-12))])
    anim.curva(mordida, "leg_back_left", "rotation",
               [(0.0, anim.vetor(x=-16)), (DUR_STRIKE * 0.5, anim.vetor(x=24)),
                (DUR_STRIKE, anim.vetor(x=9))])
    anim.curva(mordida, "leg_back_right", "rotation",
               [(0.0, anim.vetor(x=-16)), (DUR_STRIKE * 0.5, anim.vetor(x=24)),
                (DUR_STRIKE, anim.vetor(x=9))])
    for perna in ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"):
        anim.derivar(mordida, perna, perna.replace("leg_", "paw_"), "rotation", -0.55)
    a.clipe("strike", DUR_STRIKE, mordida)

    # -------------------------------------------------------------- recovery
    # A JANELA EM QUE O JOGADOR PUNE, e ela dura quase o dobro do aviso. Num mob
    # de bando, essa janela e o que impede o cerco de virar uma trituradora: os
    # quatro lobos nao podem estar todos em ACTIVE ao mesmo tempo, e a
    # recuperacao longa e o que espaca os golpes no tempo.
    #
    # Encurtar este clipe nao mudaria o servidor -- mudaria so a leitura, e o
    # jogador acharia que apanhou sem ter tido janela.
    volta = {}
    anim.curva(volta, "body", "rotation",
               [(0.0, anim.vetor(x=-6)), (DUR_RECOVERY * 0.5, anim.vetor(x=6)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "body", "position",
               [(0.0, anim.vetor(y=0.4, z=-1.6)), (DUR_RECOVERY * 0.5, anim.vetor(y=-0.5)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "head", "rotation",
               [(0.0, anim.vetor(x=8)), (DUR_RECOVERY * 0.5, anim.vetor(x=-5)),
                (DUR_RECOVERY, anim.vetor())])
    _cabeca_completa(volta)
    anim.curva(volta, "tail", "rotation",
               [(0.0, anim.vetor(x=10)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "leg_front_left", "rotation",
               [(0.0, anim.vetor(x=-12)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "leg_front_right", "rotation",
               [(0.0, anim.vetor(x=-12)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "leg_back_left", "rotation",
               [(0.0, anim.vetor(x=9)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "leg_back_right", "rotation",
               [(0.0, anim.vetor(x=9)), (DUR_RECOVERY, anim.vetor())])
    for perna in ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"):
        anim.derivar(volta, perna, perna.replace("leg_", "paw_"), "rotation", -0.55)
    a.clipe("recovery", DUR_RECOVERY, volta)

    # --------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece -- e num bando de quatro, o jogador que interrompeu UM
    # lobo precisa saber QUAL, ou a interrupcao deixa de ser uma tatica.
    #
    # Este clipe dura 1.3 s contra os 1.25 s de janela do servidor. A folga e de
    # proposito, e `valida_cambaleio_cobre_a_janela` a cobra: clipe mais curto
    # devolve o lobo a pose neutra enquanto ele ainda esta interrompido, e o
    # jogador recua de um bicho que nao podia revidar.
    tropeco = {}
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.15, anim.vetor(x=-20, y=17)),
                (0.45, anim.vetor(x=9, y=-8)), (0.9, anim.vetor(y=3)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "body", "position",
               [(0.0, anim.vetor()), (0.15, anim.vetor(y=-1.2)),
                (0.5, anim.vetor(y=-0.4)), (DUR_STAGGER, anim.vetor())])
    # A cabeca e o que mais se mexe. Num bicho de 14 px, a cabeca e a unica peca
    # com area suficiente para a reacao aparecer de longe.
    anim.curva(tropeco, "head", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(x=-26, y=-21)),
                (0.4, anim.vetor(x=12, y=10)), (0.85, anim.vetor(y=-5)),
                (DUR_STAGGER, anim.vetor())])
    # As orelhas ACHATAM: fator maior que o arrasto normal, e nos dois eixos. E o
    # sinal que um canideo da, e o unico que nao precisa de cor para ser lido.
    anim.derivar(tropeco, "head", "ear_left", "rotation", -1.1, eixos=(0, 1))
    anim.derivar(tropeco, "head", "ear_right", "rotation", -1.1, eixos=(0, 1))
    anim.derivar(tropeco, "head", "snout", "rotation", ARRASTO_DO_FOCINHO)
    anim.curva(tropeco, "tail", "rotation",
               [(0.0, anim.vetor()), (0.2, anim.vetor(x=28)), (0.6, anim.vetor(x=14)),
                (DUR_STAGGER, anim.vetor())])
    # Um passo atras com as dianteiras e uma travada com as traseiras: o
    # cambaleio le como perda de equilibrio e nao como tique.
    anim.curva(tropeco, "leg_front_left", "rotation",
               [(0.0, anim.vetor()), (0.18, anim.vetor(x=22)), (0.55, anim.vetor(x=-7)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "leg_front_right", "rotation",
               [(0.0, anim.vetor()), (0.18, anim.vetor(x=14)), (0.55, anim.vetor(x=-4)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "leg_back_left", "rotation",
               [(0.0, anim.vetor()), (0.22, anim.vetor(x=-18)), (0.6, anim.vetor(x=6)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "leg_back_right", "rotation",
               [(0.0, anim.vetor()), (0.22, anim.vetor(x=-11)), (0.6, anim.vetor(x=4)),
                (DUR_STAGGER, anim.vetor())])
    for perna in ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"):
        anim.derivar(tropeco, perna, perna.replace("leg_", "paw_"), "rotation", -0.55)
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # ------------------------------------------------------------------ morte
    # hold_on_last_frame: o corpo fica caido ate a entidade sumir. Um clipe de
    # morte que volta ao repouso mostra o lobo DE PE no ultimo quadro antes de
    # desaparecer, e a leitura vira "ele sumiu", nao "ele caiu". Num bando de
    # quatro isso importa o dobro: quem nao ve o lobo cair nao sabe quantos
    # restam.
    #
    # O corpo desce 6 px enquanto tomba para o lado: sem a descida, o modelo
    # pivota no ar e metade do bicho atravessa o chao.
    queda = {}
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(z=26, x=8)),
                (DUR_DEATH, anim.vetor(z=88, x=14))])
    anim.curva(queda, "body", "position",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(y=-1.6)),
                (DUR_DEATH, anim.vetor(y=-6))])
    anim.curva(queda, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.45, anim.vetor(x=-14, y=19)),
                (DUR_DEATH, anim.vetor(x=21, y=8))])
    _cabeca_completa(queda)
    anim.curva(queda, "tail", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=20)),
                (DUR_DEATH, anim.vetor(x=6))])
    anim.curva(queda, "leg_front_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=-34)),
                (DUR_DEATH, anim.vetor(x=-49))])
    anim.curva(queda, "leg_front_right", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=-22)),
                (DUR_DEATH, anim.vetor(x=-37))])
    anim.curva(queda, "leg_back_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=26)),
                (DUR_DEATH, anim.vetor(x=41))])
    anim.curva(queda, "leg_back_right", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=17)),
                (DUR_DEATH, anim.vetor(x=30))])
    for perna in ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"):
        anim.derivar(queda, perna, perna.replace("leg_", "paw_"), "rotation", -0.55)
    a.clipe("death", DUR_DEATH, queda)

    return a


# ------------------------------------------ validacao que liga arte e regra

def valida_cambaleio_cobre_a_janela(clipes_do_mob):
    """O clipe de stagger tem de durar pelo menos a janela que o servidor cobra.

    A biblioteca cobra a duracao do ATAQUE contra o orcamento do servidor, e nao
    cobra a do CAMBALEIO -- porque stagger nao e ataque e nao passa por
    `AttackDefinition`. Mas a falha e a MESMA, e igualmente muda: o servidor
    segura o lobo interrompido por TICKS_DE_CAMBALEIO ticks; se o clipe acabar
    antes, o GeckoLib devolve os ossos a pose neutra e o lobo fica de pe, parado,
    no meio de uma interrupcao que ainda esta valendo.

    O que o jogador le e "ele se recuperou", e ele recua de um bicho que nao
    podia revidar -- desperdicando exatamente a janela que interromper comprou.
    Nada disso levanta excecao: o stagger acontece, o cooldown acontece, o log
    fica limpo.

    Clipe MAIS LONGO e permitido de proposito, pela mesma razao do ataque: o
    servidor manda no fim, e o Java troca de clipe quando a janela acaba.
    """
    nome = clipes_do_mob.nome_completo("stagger")
    duracao = clipes_do_mob.clipes[nome]["animation_length"]
    exigido = TICKS_DE_CAMBALEIO / TICKS_POR_SEGUNDO
    if duracao + 1e-9 < exigido:
        raise ErroDeArte(
            "%s dura %.2fs e o servidor segura o cambaleio por %.2fs (%d ticks, "
            "GreedIslandProfiles.wolfPackHunterStagger): o clipe acaba antes, o lobo volta a pose "
            "neutra no meio da interrupcao, e o jogador recua de um bicho que nao podia revidar"
            % (nome, duracao, exigido, TICKS_DE_CAMBALEIO))


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(ataques=ATAQUES,
                                          extras=(valida_cambaleio_cobre_a_janela,))
