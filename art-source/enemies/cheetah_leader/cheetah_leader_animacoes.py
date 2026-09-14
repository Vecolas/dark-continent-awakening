"""Clipes do Cheetah Leader -- a representacao das fases que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, STAGGER, VELOCIDADE OU RECOMPENSA. O servidor
publica a fase (`AttackPhase`), o cambaleio e a fase do arranque; o cliente
escolhe o clipe e o RITMO correspondentes. Se a animacao e a regra discordarem,
quem esta errado e este arquivo.

O RITMO DA PASSADA NAO MORA AQUI, E ISSO E DELIBERADO. O guepardo tem tres
velocidades -- arranque, normal e fadiga -- e elas NAO sao tres clipes de
caminhada. Sao o MESMO clipe tocado em velocidades diferentes, pelo
`setAnimationSpeedHandler` de CheetahLeaderEntity, que le o multiplicador de
`RegrasDeArranque` a partir da fase sincronizada. Tres clipes seriam tres
verdades sobre a mesma passada: girar o multiplicador numa sessao de
balanceamento mudaria a velocidade do corpo e nao a das patas, e o bicho passaria
a patinar sem que nada acusasse.

O TELEGRAFO E CURTO, PORQUE A AMEACA DESTE BICHO NAO E O GOLPE. O ciclope avisa
por 30 ticks; este guepardo avisa por 12 (0.6 s). O que machuca nele e ter
CHEGADO -- o bote e so o que acontece depois. Um windup longo daria ao jogador
tempo de tratar a chegada como um duelo comum, e o arranque inteiro se apagaria
sem nada acusar.

Meio segundo, porem, e curto demais para ser lido pelo corpo todo. Por isso o
aviso acontece em TRES pecas que mudam de LUGAR na silhueta: o corpo afunda (ela
se agacha), a boca ABRE, e a PLACA DE QUITINA se levanta na nuca. A placa e a
marca de quimera, e po-la no telegrafo e o que faz a marca trabalhar em vez de
so existir: a dez blocos, cor e detalhe somem; contorno que muda, nao.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com o campo de origem ao lado. A
regua da biblioteca reprova se a soma dos tres clipes encadeados for menor que
windup + active + recovery. Clipe mais curto faz o bicho RELAXAR no meio do golpe
que ainda vai acertar: dano certo, cooldown certo, log limpo, e a unica coisa que
o jogador tem para ler quebrada.

E HA UMA SEGUNDA REGUA, PROPRIA DESTE ARQUIVO. A biblioteca cobra a duracao do
ATAQUE contra o servidor, e nao cobra a do CAMBALEIO -- e o cambaleio deste mob
dura 35 ticks (`ChimeraProfiles.cheetahLeaderStagger()`). Um clipe de stagger
mais curto que a janela devolve o guepardo a pose neutra enquanto ele ainda esta
interrompido, e o jogador le "ele se recuperou" e recua de um bicho que nao podia
revidar -- desperdicando a unica janela em que um bicho de armadura 3 fica parado
na frente dele. `valida_cambaleio_cobre_a_janela` fecha isso.

CONVENCAO DE ROTACAO, medida neste geo e nao decorada. Com -Z na frente:
  * rotacao X NEGATIVA num membro pendurado (perna, pata) o joga para a FRENTE;
    X POSITIVA o joga para TRAS;
  * rotacao X POSITIVA na cabeca e no focinho abaixa o nariz; NEGATIVA o levanta;
  * rotacao X POSITIVA no corpo levanta a garupa e abaixa o peito;
  * rotacao X NEGATIVA na cauda e na carapaca as levanta.
O guepardo se agacha no aviso (garupa alta, peito baixo: X positivo no corpo) e
se estica no bote (X negativo).

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/cheetah_leader/cheetah_leader_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/cheetah_leader.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import ErroDeArte, TICKS_POR_SEGUNDO     # noqa: E402
from comum import animacao as anim                  # noqa: E402

MOB = "cheetah_leader"

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType
# do Java VENCE o JSON, e cravar a repeticao la transformaria este dicionario em
# documentacao que discorda do comportamento.
#
# Esta e tambem a UNICA lista de clipes deste mob, e ela e um CONTRATO com
# CheetahLeaderEntity: os sete nomes aparecem la como literais
# "animation.cheetah_leader.<clipe>". Nome trocado nao da erro -- o GeckoLib
# procura, nao acha, e o osso fica parado.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o aviso termina com o guepardo AGACHADO, a boca aberta
    # e a placa erguida, e fica assim. Voltando ao repouso no fim do clipe, ele
    # desarmaria o bote na tela enquanto o servidor ainda esta em WINDUP -- num
    # telegrafo de 12 ticks isso e a diferenca entre um aviso e nenhum.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de CheetahLeaderTuning.bote(), com o campo de origem ao lado.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=12,      # CheetahLeaderTuning.WINDUP_DO_BOTE
                       active=5,       # CheetahLeaderTuning.JANELA_DO_BOTE
                       recovery=14)}   # CheetahLeaderTuning.RECUPERACAO_DO_BOTE

# Ticks COPIADOS de ChimeraProfiles.cheetahLeaderStagger(): o quarto campo de
# StaggerRules(30.0F, 4.0F, 0.5F, 35). E cobrado por
# `valida_cambaleio_cobre_a_janela`, logo abaixo.
TICKS_DE_CAMBALEIO = 35

DUR_IDLE = 2.6
DUR_WALK = 0.5       # passada curta e rapida; o RITMO vem da fase do arranque
DUR_WINDUP = 0.6     # 12 ticks
DUR_STRIKE = 0.3     # 6 ticks -- um a mais que a janela, de proposito
DUR_RECOVERY = 0.75  # 15 ticks
DUR_STAGGER = 1.8    # 36 ticks -- um a mais que a janela de cambaleio do servidor
DUR_DEATH = 1.2

# Arrasto da orelha atras da cabeca. Negativo e pequeno: a orelha fica para TRAS
# do movimento. Fator positivo faria a orelha ANTECIPAR o pescoco, que e o
# movimento de quem ja sabia -- e este bicho reage a distancia, nao adivinha.
ARRASTO_DA_ORELHA = -0.4

# Arrasto do focinho. Menor ainda: o focinho e preso na cara, nao pendurado nela.
ARRASTO_DO_FOCINHO = -0.15

# Arrasto da ponta da cauda atras da base. Este e o MAIOR de todos, e e a
# decisao de leitura deste bicho: a ponta chega meio quadro depois da base, e e
# esse atraso que transforma uma cauda em um chicote de equilibrio. Com fator
# pequeno a cauda vira uma vara rigida, e a virada no fim do arranque some.
ARRASTO_DA_PONTA_DA_CAUDA = -0.75

# Contra-rotacao da pata em relacao a canela. Negativa: a pata volta contra o
# balanco da perna, que e o que faz o pe "assentar" em vez de chutar o ar.
CONTRA_ROTACAO_DA_PATA = -0.5

PERNAS = ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right")


def _patas_derivadas(bones):
    """As quatro patas SEMPRE saem da canela, nunca escritas a mao.

    Escritas a mao elas ficam com a fase certa hoje e errada na primeira
    correcao da perna, e a correcao nao da erro: da uma pata arrastando meio
    quadro atras da canela, em quatro membros ao mesmo tempo.
    """
    for perna in PERNAS:
        anim.derivar(bones, perna, perna.replace("leg_", "paw_"), "rotation",
                     CONTRA_ROTACAO_DA_PATA)
    return bones


def _quatro_patas(bones, dianteira_esquerda_graus, fase_cruzada=0.5, duracao=None,
                  periodo=None):
    """A passada cruzada das quatro pernas, com as patas SEMPRE derivadas.

    Cruzada porque e o que um quadrupede faz: dianteira esquerda anda com a
    traseira direita. Escrever as quatro em fase daria um felino saltitando com
    as quatro juntas, e isso nao levanta erro -- so faz um predador parecer um
    coelho.
    """
    duracao = duracao if duracao is not None else DUR_WALK
    periodo = periodo if periodo is not None else duracao
    for perna, fase in (("leg_front_left", 0.0), ("leg_back_right", 0.0),
                        ("leg_front_right", fase_cruzada), ("leg_back_left", fase_cruzada)):
        anim.curva(bones, perna, "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(duracao, periodo, dianteira_esquerda_graus, fase=fase)])
    return _patas_derivadas(bones)


def _cabeca_completa(bones):
    """Orelhas e focinho SEMPRE derivados da cabeca, em todo clipe.

    A cabeca deste bicho e pequena (5 px numa silhueta de 23), e o que a torna
    legivel e o conjunto: cabeca que se mexe com orelha parada le como adesivo
    colado no bicho -- e isso nao aparece em portao nenhum, so na tela.
    """
    anim.derivar(bones, "head", "ear_left", "rotation", ARRASTO_DA_ORELHA, eixos=(0, 1))
    anim.derivar(bones, "head", "ear_right", "rotation", ARRASTO_DA_ORELHA, eixos=(0, 1))
    anim.derivar(bones, "head", "snout", "rotation", ARRASTO_DO_FOCINHO, eixos=(0, 1))
    return bones


def _cauda_completa(bones, eixos=(0, 1)):
    """A ponta da cauda SEMPRE sai da base, com atraso.

    Escrita a mao, a ponta acerta a fase de hoje e erra a da proxima correcao --
    e a cauda deste bicho e a peca que mostra a virada. Uma ponta fora de fase
    nao da erro: da um chicote que bate antes do braco.
    """
    return anim.derivar(bones, "tail", "tail_tip", "rotation",
                        ARRASTO_DA_PONTA_DA_CAUDA, eixos=eixos)


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # ---------------------------------------------------------------- ocio
    # Respiracao lenta e a cabeca varrendo o horizonte. O varrimento em Y nao e
    # enfeite: um lider parado que NAO olha em volta le como estatua, e estatua
    # nao explica por que os outros chegaram atras dele.
    ocio = {}
    anim.curva(ocio, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 1.2)])
    anim.curva(ocio, "head", "rotation",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 9.0, fase=0.2)])
    _cabeca_completa(ocio)
    # A cauda varre devagar mesmo parada: e ela que diz "vivo" a vinte blocos,
    # onde nem a respiracao do tronco nem o piscar da cara chegam.
    anim.curva(ocio, "tail", "rotation",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 12.0)])
    _cauda_completa(ocio)
    # Amplitude minuscula nas patas: o peso passa de um par ao outro sem passada.
    _quatro_patas(ocio, 1.5, duracao=DUR_IDLE, periodo=DUR_IDLE)
    a.clipe("idle", DUR_IDLE, ocio)

    # ------------------------------------------------------------ locomocao
    # UM clipe para as tres velocidades. O ciclo de 0.5 s e o da velocidade
    # NORMAL (0.46); o arranque e a fadiga tocam este mesmo clipe mais rapido e
    # mais devagar, pelo multiplicador que o servidor publica. Escrever aqui uma
    # passada "de arranque" criaria uma segunda fonte para o mesmo ritmo.
    marcha = {}
    _quatro_patas(marcha, 34.0)
    anim.curva(marcha, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 3.5)])
    # O solavanco vertical tem periodo METADE do ciclo: sao duas pisadas por
    # ciclo, e uma so faria o guepardo mancar.
    anim.curva(marcha, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 0.6)])
    anim.derivar(marcha, "body", "head", "rotation", -0.6)
    _cabeca_completa(marcha)
    anim.curva(marcha, "tail", "rotation",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 16.0)])
    _cauda_completa(marcha)
    a.clipe("walk", DUR_WALK, marcha)

    # --------------------------------------------------------------- windup
    # DOZE TICKS DE AVISO, e eles tem de caber na silhueta. Tres coisas mudam de
    # LUGAR: o corpo afunda 2 px, a boca abre, e a PLACA se levanta na nuca.
    #
    # A maior parte do curso acontece na SEGUNDA metade. Arranque lento e o que
    # da ao jogador tempo de decidir antes do ponto de nao-retorno -- e com 12
    # ticks de orcamento, esse tempo e tudo que ha.
    aviso = {}
    anim.curva(aviso, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.4, anim.vetor(x=4)),
                (DUR_WINDUP, anim.vetor(x=13))])
    anim.curva(aviso, "body", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.4, anim.vetor(y=-0.5)),
                (DUR_WINDUP, anim.vetor(y=-2.0))])
    # A cabeca ENCARA o alvo enquanto o corpo se agacha: ela sobe contra a
    # inclinacao do tronco. Cabeca acompanhando o tronco esconderia a boca atras
    # do peito, e a boca e um terco do aviso.
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-16))])
    anim.derivar(aviso, "head", "ear_left", "rotation", ARRASTO_DA_ORELHA)
    anim.derivar(aviso, "head", "ear_right", "rotation", ARRASTO_DA_ORELHA)
    # A boca ABRE -- escrita a mao e nao derivada, porque abrir a boca e o unico
    # movimento deste clipe que nao e arrasto de outra peca.
    anim.curva(aviso, "snout", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.4, anim.vetor(x=8)),
                (DUR_WINDUP, anim.vetor(x=28))])
    # A PLACA DE QUITINA se ergue. Ela e a marca de quimera, e e aqui que ela
    # trabalha: a dez blocos o jogador nao ve a boca abrir, mas ve o contorno do
    # dorso crescer. Placa parada faria a marca ser so um adorno de atlas.
    anim.curva(aviso, "carapaca", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.5, anim.vetor(x=-5)),
                (DUR_WINDUP, anim.vetor(x=-15))])
    anim.curva(aviso, "tail", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-26))])
    _cauda_completa(aviso, eixos=(0,))
    # As quatro pernas dobram JUNTAS: e o agachamento, e nao uma passada.
    anim.curva(aviso, "leg_front_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=16))])
    anim.curva(aviso, "leg_front_right", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=16))])
    anim.curva(aviso, "leg_back_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-20))])
    anim.curva(aviso, "leg_back_right", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-20))])
    _patas_derivadas(aviso)
    a.clipe("windup", DUR_WINDUP, aviso)

    # --------------------------------------------------------------- strike
    # A JANELA QUE MACHUCA. O servidor gasta 5 ticks (0.25 s) e o clipe dura
    # 0.3: o excedente e permitido de proposito -- o servidor manda no fim e o
    # Java corta o clipe. Faltar e que nao pode.
    #
    # O corpo se ESTICA e avanca: z NEGATIVO e para a frente nesta geometria.
    # Esse avanco nao e enfeite -- ele e a metade visual do impulso que o
    # servidor aplica (CheetahLeaderTuning.AVANCO_DO_BOTE), e e ele que faz a
    # caixa do bote chegar onde o focinho desenhado sozinho nao chegaria. Tirar o
    # avanco da tela sem tirar do servidor deixa o jogador levando dano de um
    # bicho parado.
    bote = {}
    anim.curva(bote, "body", "rotation",
               [(0.0, anim.vetor(x=13)), (DUR_STRIKE * 0.5, anim.vetor(x=-16)),
                (DUR_STRIKE, anim.vetor(x=-7))])
    anim.curva(bote, "body", "position",
               [(0.0, anim.vetor(y=-2.0)), (DUR_STRIKE * 0.5, anim.vetor(y=1.2, z=-3.0)),
                (DUR_STRIKE, anim.vetor(y=0.5, z=-1.8))])
    anim.curva(bote, "head", "rotation",
               [(0.0, anim.vetor(x=-16)), (DUR_STRIKE * 0.5, anim.vetor(x=14)),
                (DUR_STRIKE, anim.vetor(x=6))])
    anim.derivar(bote, "head", "ear_left", "rotation", ARRASTO_DA_ORELHA)
    anim.derivar(bote, "head", "ear_right", "rotation", ARRASTO_DA_ORELHA)
    # A boca FECHA no meio da janela. Fechando depois do fim do clipe, ela
    # ficaria aberta durante a recuperacao inteira, e a leitura viraria "ele
    # ainda esta atacando" -- justamente na janela em que o jogador deveria punir.
    anim.curva(bote, "snout", "rotation",
               [(0.0, anim.vetor(x=28)), (DUR_STRIKE * 0.5, anim.vetor(x=-4)),
                (DUR_STRIKE, anim.vetor())])
    # A placa baixa junto com a boca: o aviso inteiro se desfaz no mesmo instante
    # em que o golpe sai, e e isso que separa "vai atacar" de "atacou".
    anim.curva(bote, "carapaca", "rotation",
               [(0.0, anim.vetor(x=-15)), (DUR_STRIKE * 0.5, anim.vetor(x=5)),
                (DUR_STRIKE, anim.vetor(x=1))])
    anim.curva(bote, "tail", "rotation",
               [(0.0, anim.vetor(x=-26)), (DUR_STRIKE * 0.6, anim.vetor(x=16)),
                (DUR_STRIKE, anim.vetor(x=10))])
    _cauda_completa(bote, eixos=(0,))
    anim.curva(bote, "leg_front_left", "rotation",
               [(0.0, anim.vetor(x=16)), (DUR_STRIKE * 0.5, anim.vetor(x=-36)),
                (DUR_STRIKE, anim.vetor(x=-14))])
    anim.curva(bote, "leg_front_right", "rotation",
               [(0.0, anim.vetor(x=16)), (DUR_STRIKE * 0.5, anim.vetor(x=-36)),
                (DUR_STRIKE, anim.vetor(x=-14))])
    anim.curva(bote, "leg_back_left", "rotation",
               [(0.0, anim.vetor(x=-20)), (DUR_STRIKE * 0.5, anim.vetor(x=28)),
                (DUR_STRIKE, anim.vetor(x=11))])
    anim.curva(bote, "leg_back_right", "rotation",
               [(0.0, anim.vetor(x=-20)), (DUR_STRIKE * 0.5, anim.vetor(x=28)),
                (DUR_STRIKE, anim.vetor(x=11))])
    _patas_derivadas(bote)
    a.clipe("strike", DUR_STRIKE, bote)

    # -------------------------------------------------------------- recovery
    # A JANELA EM QUE O JOGADOR PUNE, e ela dura mais que o aviso. Num bicho de
    # armadura 3, essa janela e o pagamento do bote: sem ela, chegar primeiro
    # seria vantagem sem preco, e o encontro viraria uma perseguicao que so
    # termina quando o jogador morre.
    #
    # Encurtar este clipe nao mudaria o servidor -- mudaria so a leitura, e o
    # jogador acharia que apanhou sem ter tido janela.
    volta = {}
    anim.curva(volta, "body", "rotation",
               [(0.0, anim.vetor(x=-7)), (DUR_RECOVERY * 0.5, anim.vetor(x=6)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "body", "position",
               [(0.0, anim.vetor(y=0.5, z=-1.8)), (DUR_RECOVERY * 0.5, anim.vetor(y=-0.7)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "head", "rotation",
               [(0.0, anim.vetor(x=6)), (DUR_RECOVERY * 0.5, anim.vetor(x=-5)),
                (DUR_RECOVERY, anim.vetor())])
    _cabeca_completa(volta)
    anim.curva(volta, "carapaca", "rotation",
               [(0.0, anim.vetor(x=1)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "tail", "rotation",
               [(0.0, anim.vetor(x=10)), (DUR_RECOVERY * 0.5, anim.vetor(x=-6)),
                (DUR_RECOVERY, anim.vetor())])
    _cauda_completa(volta, eixos=(0,))
    anim.curva(volta, "leg_front_left", "rotation",
               [(0.0, anim.vetor(x=-14)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "leg_front_right", "rotation",
               [(0.0, anim.vetor(x=-14)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "leg_back_left", "rotation",
               [(0.0, anim.vetor(x=11)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "leg_back_right", "rotation",
               [(0.0, anim.vetor(x=11)), (DUR_RECOVERY, anim.vetor())])
    _patas_derivadas(volta)
    a.clipe("recovery", DUR_RECOVERY, volta)

    # --------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece. Neste bicho ela vale o dobro: interromper o lider e a
    # unica coisa que atrasa a chegada dele, e um jogador que nao ve o cambaleio
    # nao aprende que interromper foi o que funcionou.
    #
    # Este clipe dura 1.8 s contra os 1.75 s de janela do servidor. A folga e de
    # proposito, e `valida_cambaleio_cobre_a_janela` a cobra.
    tropeco = {}
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.14, anim.vetor(x=-22, y=19)),
                (0.5, anim.vetor(x=10, y=-9)), (1.1, anim.vetor(y=3)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "body", "position",
               [(0.0, anim.vetor()), (0.14, anim.vetor(y=-1.6)),
                (0.6, anim.vetor(y=-0.5)), (DUR_STAGGER, anim.vetor())])
    # A cabeca e o que mais se mexe: ela e pequena, mas e a peca mais alta que
    # muda de lugar, e num bicho que ja esta longe e ela que chega ao olho.
    anim.curva(tropeco, "head", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(x=-28, y=-23)),
                (0.45, anim.vetor(x=13, y=11)), (1.0, anim.vetor(y=-5)),
                (DUR_STAGGER, anim.vetor())])
    # As orelhas ACHATAM: fator maior que o arrasto normal, e nos dois eixos. E o
    # sinal que um felino da, e o unico que nao precisa de cor para ser lido.
    anim.derivar(tropeco, "head", "ear_left", "rotation", -1.1, eixos=(0, 1))
    anim.derivar(tropeco, "head", "ear_right", "rotation", -1.1, eixos=(0, 1))
    anim.derivar(tropeco, "head", "snout", "rotation", ARRASTO_DO_FOCINHO, eixos=(0, 1))
    # A placa cai. Erguida ela significa ameaca; caida, significa que a ameaca
    # parou -- e e o unico sinal do cambaleio que continua legivel de costas.
    anim.curva(tropeco, "carapaca", "rotation",
               [(0.0, anim.vetor()), (0.2, anim.vetor(x=9)), (0.8, anim.vetor(x=3)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "tail", "rotation",
               [(0.0, anim.vetor()), (0.2, anim.vetor(x=30, y=-16)),
                (0.7, anim.vetor(x=14, y=8)), (DUR_STAGGER, anim.vetor())])
    _cauda_completa(tropeco)
    # Um passo atras com as dianteiras e uma travada com as traseiras: o
    # cambaleio le como perda de equilibrio e nao como tique.
    anim.curva(tropeco, "leg_front_left", "rotation",
               [(0.0, anim.vetor()), (0.18, anim.vetor(x=26)), (0.6, anim.vetor(x=-8)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "leg_front_right", "rotation",
               [(0.0, anim.vetor()), (0.18, anim.vetor(x=17)), (0.6, anim.vetor(x=-5)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "leg_back_left", "rotation",
               [(0.0, anim.vetor()), (0.24, anim.vetor(x=-21)), (0.65, anim.vetor(x=7)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "leg_back_right", "rotation",
               [(0.0, anim.vetor()), (0.24, anim.vetor(x=-13)), (0.65, anim.vetor(x=5)),
                (DUR_STAGGER, anim.vetor())])
    _patas_derivadas(tropeco)
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # ------------------------------------------------------------------ morte
    # hold_on_last_frame: o corpo fica caido ate a entidade sumir. Um clipe de
    # morte que volta ao repouso mostra o bicho DE PE no ultimo quadro antes de
    # desaparecer, e a leitura vira "ele sumiu", nao "ele caiu". Num lider isso
    # importa o dobro: quem nao ve o lider cair nao sabe que o esquadrao acabou
    # de perder o comando.
    #
    # O corpo desce 8 px enquanto tomba para o lado: sem a descida, o modelo
    # pivota no ar e metade do bicho atravessa o chao.
    queda = {}
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(z=28, x=9)),
                (DUR_DEATH, anim.vetor(z=90, x=15))])
    anim.curva(queda, "body", "position",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(y=-2.2)),
                (DUR_DEATH, anim.vetor(y=-8))])
    anim.curva(queda, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.45, anim.vetor(x=-16, y=21)),
                (DUR_DEATH, anim.vetor(x=23, y=9))])
    _cabeca_completa(queda)
    anim.curva(queda, "carapaca", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.4, anim.vetor(x=7)),
                (DUR_DEATH, anim.vetor(x=2))])
    anim.curva(queda, "tail", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=24, y=22)),
                (DUR_DEATH, anim.vetor(x=7, y=6))])
    _cauda_completa(queda)
    anim.curva(queda, "leg_front_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=-38)),
                (DUR_DEATH, anim.vetor(x=-54))])
    anim.curva(queda, "leg_front_right", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=-25)),
                (DUR_DEATH, anim.vetor(x=-40))])
    anim.curva(queda, "leg_back_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=29)),
                (DUR_DEATH, anim.vetor(x=45))])
    anim.curva(queda, "leg_back_right", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=19)),
                (DUR_DEATH, anim.vetor(x=33))])
    _patas_derivadas(queda)
    a.clipe("death", DUR_DEATH, queda)

    return a


# ------------------------------------------ validacao que liga arte e regra

def valida_cambaleio_cobre_a_janela(clipes_do_mob):
    """O clipe de stagger dura pelo menos a janela que o servidor cobra. [ARTE <-> SERVIDOR]

    A biblioteca cobra a duracao do ATAQUE contra o orcamento do servidor, e nao
    cobra a do CAMBALEIO -- porque stagger nao e ataque e nao passa por
    `AttackDefinition`. Mas a falha e a MESMA, e igualmente muda: o servidor
    segura o guepardo interrompido por TICKS_DE_CAMBALEIO ticks; se o clipe
    acabar antes, o GeckoLib devolve os ossos a pose neutra e o bicho fica de pe,
    parado, no meio de uma interrupcao que ainda esta valendo.

    O que o jogador le e "ele se recuperou", e ele recua de um bicho que nao
    podia revidar -- desperdicando a unica janela em que um guepardo de armadura
    3 fica parado ao alcance dele. Nada disso levanta excecao: o stagger
    acontece, o cooldown acontece, o log fica limpo.

    Clipe MAIS LONGO e permitido de proposito, pela mesma razao do ataque: o
    servidor manda no fim, e o Java troca de clipe quando a janela acaba.
    """
    nome = clipes_do_mob.nome_completo("stagger")
    duracao = clipes_do_mob.clipes[nome]["animation_length"]
    exigido = TICKS_DE_CAMBALEIO / TICKS_POR_SEGUNDO
    if duracao + 1e-9 < exigido:
        raise ErroDeArte(
            "%s dura %.2fs e o servidor segura o cambaleio por %.2fs (%d ticks, "
            "ChimeraProfiles.cheetahLeaderStagger): o clipe acaba antes, o guepardo volta a pose "
            "neutra no meio da interrupcao, e o jogador recua de um bicho que nao podia revidar"
            % (nome, duracao, exigido, TICKS_DE_CAMBALEIO))


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(ataques=ATAQUES,
                                          extras=(valida_cambaleio_cobre_a_janela,))
