"""Clipes do Avian Commander -- a representacao das fases que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, STAGGER, ALTITUDE OU ORDEM. O servidor publica
a fase (`AttackPhase`), o cambaleio e a POSTURA (`PosturaDeComando`); o cliente
escolhe o clipe correspondente. Se a animacao e a regra discordarem, quem esta
errado e este arquivo.

AS TRES COISAS QUE ESTES CLIPES PRECISAM CONTAR
------------------------------------------------
1. ELA ESTA NO ALTO, E PARADA NAO E O MESMO QUE MORTA. `idle` e o posto de
   comando: corpo quase im?vel e ASA BATENDO. Um clipe de ocio sem batida deixaria
   uma ave suspensa no ar sem nada que explique por que ela nao cai, e o jogador
   le isso como travamento -- nunca como "ela esta te observando". Por isso o
   controlador Java escolhe entre `idle` e `walk` pela POSTURA, e nao so pela
   velocidade (ver AvianCommanderEntity.registerControllers).
2. O MERGULHO E UM COMPROMISSO, E A SUBIDA E O PRECO. `windup` fecha a asa e
   aponta o corpo para baixo -- silhueta que MUDA, e nao so pose que muda. E
   `recovery` e a SUBIDA: ela dura
   `AvianCommanderTuning.TICKS_DE_SUBIDA_APOS_MERGULHO`, que e a janela em que o
   servidor a proibe de comandar. `valida_subida_cobre_a_janela_do_jogador` cobra
   isso: um clipe mais curto poria a comandante de volta no alto NA TELA enquanto
   o servidor ainda a mantem calada, e o jogador leria "acabou" antes de acabar --
   e perderia a unica janela que o encontro oferece.
3. A ASA FECHA NO GOLPE E ABRE NA SUBIDA. A envergadura e a silhueta deste bicho:
   fechada, ela e um projetil; aberta, e uma comandante. `valida_asa_fecha_no_golpe`
   cobra a diferenca -- sem ela, alguem suaviza a asa "para o clipe ficar mais
   bonito" e os dois estados passam a ter a mesma leitura a dez blocos, que e a
   distancia em que este mob vive.

CONVENCAO DE ROTACAO, medida neste geo e nao decorada. Com -Z na frente:
  * rotacao X POSITIVA no tronco inclina para a FRENTE (bico para baixo);
    NEGATIVA empina -- e empinar e o gesto de quem esta subindo;
  * rotacao X NEGATIVA num membro pendurado (perna) o joga para a FRENTE;
  * rotacao Z nas asas e a BATIDA, e os dois lados levam sinais opostos. Sinais
    iguais dariam as duas asas subindo para o mesmo lado do mundo, que le como
    um bicho quebrado e nao como uma ave.

O SENTIDO DE Y DA ABERTURA E DEDUZIDO DA MAO DOS EIXOS, E NAO OBSERVADO EM JOGO.
A ponta da asa nasce apontando para TRAS (+Z a partir da dobra); girada em Y ela
passa a apontar para FORA (+X, no lado esquerdo). O sinal esta em `SENTIDO_DE_Y`,
isolado numa constante justamente porque e a unica coisa aqui que nao da para
provar sem abrir o jogo -- e trocado, a asa abriria PARA DENTRO do corpo, o que e
imediatamente visivel e nao levanta erro nenhum. Ver
docs/testing/o-que-nao-provamos.md.

ESCALA NUNCA E DERIVADA. `anim.derivar` zera os eixos que nao foram pedidos, e um
vetor de escala com zero NAO da erro -- ele SOME com o osso.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/avian_commander/avian_commander_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/avian_commander.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import ErroDeArte, TICKS_POR_SEGUNDO   # noqa: E402
from comum import animacao as anim                # noqa: E402

MOB = "avian_commander"

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType do
# Java VENCE o JSON, e cravar a repeticao la transformaria este dicionario em
# documentacao que discorda do comportamento.
#
# Esta e tambem a UNICA lista de clipes deste mob. Uma tupla CLIPES ao lado seria
# a mesma informacao escrita duas vezes, e alguem acrescentaria um clipe em so uma
# delas.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o telegrafo termina com a asa FECHADA e o bico apontado
    # para baixo, e fica assim. Voltando ao repouso no fim do clipe, ela desarmaria
    # o mergulho na tela enquanto o servidor ainda esta em WINDUP -- o jogador
    # leria "passou" e levaria as garras mesmo assim.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    # hold_on_last_frame: o corpo fica caido e a asa aberta no chao ate a entidade
    # sumir. Um clipe de morte que volta ao repouso mostraria a comandante em pose
    # de voo no ultimo quadro, em cima do proprio cadaver.
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de AvianCommanderTuning, com o campo de origem ao lado.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=18,     # AvianCommanderTuning.WINDUP_DO_MERGULHO
                       active=6,      # AvianCommanderTuning.JANELA_DO_MERGULHO
                       recovery=24)}  # AvianCommanderTuning.RECUPERACAO_DO_MERGULHO

# A janela em que o servidor a proibe de COMANDAR depois do mergulho. Copiado de
# AvianCommanderTuning.TICKS_DE_SUBIDA_APOS_MERGULHO. E um numero diferente da
# recuperacao do ATAQUE de proposito: um acaba quando ela pode atacar de novo, o
# outro quando ela pode mandar de novo.
TICKS_DE_SUBIDA_APOS_MERGULHO = 30

DUR_IDLE = 2.0
DUR_WALK = 0.6
DUR_WINDUP = 0.9    # 18 ticks
DUR_STRIKE = 0.4    # 8 ticks -- dois a mais que a janela, de proposito
DUR_RECOVERY = TICKS_DE_SUBIDA_APOS_MERGULHO / TICKS_POR_SEGUNDO   # 1.50
DUR_STAGGER = 0.6
DUR_DEATH = 1.4

# Ver o cabecalho: o Y e deduzido da mao dos eixos, nao observado em jogo.
SENTIDO_DE_Y = 1.0

# Abertura da ponta da asa, em graus, por estado. Zero e como o geo nasce
# (dobrada, rente ao flanco); 90 e a ponta estendida para fora.
#
# A ESCADA E A LEITURA INTEIRA DO MOB, da menor silhueta para a maior:
#     golpe < posto de comando < cruzeiro < SUBIDA
# O golpe e o menor porque ali ela e um projetil, e a subida e a maior porque e
# la que ela precisa ser vista -- a subida e a janela do jogador, e uma janela
# que nao se enxerga nao e janela.
ABERTURA_NO_GOLPE = 18.0
ABERTURA_NO_POSTO = 62.0
ABERTURA_EM_CRUZEIRO = 78.0
ABERTURA_NA_SUBIDA = 90.0

# Quanto a asa do golpe tem de ficar ABAIXO da asa de cruzeiro para que os dois
# estados tenham silhuetas diferentes a dez blocos. Nao e botao de tuning: e o
# limite de LEITURA, e dez blocos e a distancia em que este mob vive (a altitude
# de comando e sete).
DEGRAU_MINIMO_DA_ASA = 30.0

ASAS = ("wing_left", "wing_right")
PONTAS = {"wing_left": "wing_tip_left", "wing_right": "wing_tip_right"}
PERNAS = ("leg_left", "leg_right")
GARRA_DA_PERNA = {"leg_left": "talon_left", "leg_right": "talon_right"}
ANTENAS = ("antenna_left", "antenna_right")

# Sinal da batida por asa: opostos, sempre. Iguais, as duas asas sobem para o
# mesmo lado do mundo e o bicho le como quebrado, nao como ave.
SINAL_DA_BATIDA = {"wing_left": 1.0, "wing_right": -1.0}
# Sinal da abertura por ponta: espelhado, pela mesma razao.
SINAL_DA_ABERTURA = {"wing_tip_left": 1.0, "wing_tip_right": -1.0}


def _abrir(bones, graus, duracao, tremor=0.0, periodo=None):
    """Poe as duas pontas na abertura dada, com um tremor opcional.

    Escrito como curva de dois quadros mesmo quando e constante: um canal com um
    keyframe so segura o valor ate o fim e funciona, mas o diff de um clipe cujo
    canal tem uma chave a mais ou a menos deixa de dizer o que mudou.
    """
    ciclo = periodo or duracao
    for ponta, sinal in SINAL_DA_ABERTURA.items():
        if tremor <= 0.0:
            anim.curva(bones, ponta, "rotation",
                       [(0.0, anim.vetor(y=graus * sinal * SENTIDO_DE_Y)),
                        (duracao, anim.vetor(y=graus * sinal * SENTIDO_DE_Y))])
            continue
        anim.curva(bones, ponta, "rotation",
                   [(t, anim.vetor(y=v * sinal * SENTIDO_DE_Y)) for t, v in
                    anim.cossenoide(duracao, ciclo, tremor, base=graus)])


def _bater(bones, amplitude, duracao, periodo, base=0.0, fase=0.0):
    """A batida das asas: rotacao Z com sinais opostos nos dois lados."""
    for asa, sinal in SINAL_DA_BATIDA.items():
        anim.curva(bones, asa, "rotation",
                   [(t, anim.vetor(z=v * sinal)) for t, v in
                    anim.cossenoide(duracao, periodo, amplitude, base=base * sinal, fase=fase)])


def _pernas(bones, graus, duracao, garra=0.0):
    """As pernas recolhidas (ou estendidas) pelo clipe inteiro."""
    for perna in PERNAS:
        anim.curva(bones, perna, "rotation",
                   [(0.0, anim.vetor(x=graus)), (duracao, anim.vetor(x=graus))])
        anim.curva(bones, GARRA_DA_PERNA[perna], "rotation",
                   [(0.0, anim.vetor(x=garra)), (duracao, anim.vetor(x=garra))])


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # ---------------------------------------------------------------- ocio
    # O POSTO DE COMANDO. O corpo quase nao se move -- ela esta OLHANDO --, e o
    # que se move e a asa. Uma ave suspensa no ar sem batida nao tem explicacao
    # visual para nao cair, e o jogador le isso como travamento; a batida lenta e
    # profunda e o que diz "ela esta parada porque quer".
    #
    # As antenas varrem devagar e fora de fase com a asa: sao a marca de quimera, e
    # marca parada vira enfeite. Fora de fase porque duas oscilacoes sincronizadas
    # leem como engrenagem.
    posto = {}
    anim.curva(posto, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 0.6)])
    anim.curva(posto, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.0, base=-3.0)])
    _bater(posto, 14.0, DUR_IDLE, DUR_IDLE / 2.0)
    _abrir(posto, ABERTURA_NO_POSTO, DUR_IDLE, tremor=4.0, periodo=DUR_IDLE / 2.0)
    # A cabeca contra-gira o tronco: o bico continua apontado para o campo mesmo
    # quando o corpo balanca. Sem a compensacao, o olhar sobe e desce junto com a
    # respiracao e ela deixa de parecer que esta vigiando alguma coisa.
    anim.derivar(posto, "body", "head", "rotation", -0.8)
    anim.derivar(posto, "head", "beak", "rotation", -0.3)
    for i, antena in enumerate(ANTENAS):
        anim.curva(posto, antena, "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(DUR_IDLE, DUR_IDLE, 9.0, fase=0.25 + i * 0.2)])
    anim.curva(posto, "crest", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.5, fase=0.1)])
    anim.curva(posto, "abdomen", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 3.0, fase=0.5)])
    anim.derivar(posto, "abdomen", "tail", "rotation", -0.6)
    _pernas(posto, 34.0, DUR_IDLE, garra=-18.0)
    a.clipe("idle", DUR_IDLE, posto)

    # ------------------------------------------------------------- locomocao
    # CRUZEIRO. Batida forte e rapida, corpo levemente inclinado para a frente, e
    # a asa mais aberta que no posto: ela esta indo a algum lugar.
    #
    # As pernas ficam RECOLHIDAS, e nao alternando: passada num bicho que voa e o
    # defeito que faz um mob alado parecer andar no ar.
    voo = {}
    anim.curva(voo, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 0.9)])
    anim.curva(voo, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 3.0, base=6.0)])
    _bater(voo, 36.0, DUR_WALK, DUR_WALK)
    _abrir(voo, ABERTURA_EM_CRUZEIRO, DUR_WALK, tremor=6.0, periodo=DUR_WALK)
    anim.derivar(voo, "body", "head", "rotation", -0.9)
    anim.derivar(voo, "head", "beak", "rotation", -0.2)
    for i, antena in enumerate(ANTENAS):
        anim.curva(voo, antena, "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(DUR_WALK, DUR_WALK, 14.0, base=12.0, fase=0.3 + i * 0.15)])
    anim.curva(voo, "crest", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 3.0, base=4.0)])
    anim.curva(voo, "abdomen", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 4.0, base=-3.0, fase=0.5)])
    anim.derivar(voo, "abdomen", "tail", "rotation", -0.7)
    _pernas(voo, 52.0, DUR_WALK, garra=-26.0)
    a.clipe("walk", DUR_WALK, voo)

    # --------------------------------------------------------------- windup
    # O COMPROMISSO. A asa FECHA, o corpo aponta para baixo e as garras comecam a
    # abrir. Silhueta que muda de FORMA, e nao so de pose: a dezoito ticks de
    # distancia e sete blocos de altura, e a forma que chega.
    #
    # A maior parte do curso fica na SEGUNDA metade (um terco em 0.35 s, tudo no
    # fim): arranque lento e o que da ao jogador tempo de sair de baixo antes do
    # ponto de nao-retorno -- e sair de baixo e a resposta certa.
    aviso = {}
    anim.curva(aviso, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.35, anim.vetor(x=10)),
                (DUR_WINDUP, anim.vetor(x=46))])
    anim.curva(aviso, "body", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=1.2))])
    for asa, sinal in SINAL_DA_BATIDA.items():
        anim.curva(aviso, asa, "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP * 0.35, anim.vetor(z=-14 * sinal)),
                    (DUR_WINDUP, anim.vetor(z=-40 * sinal))])
    for ponta, sinal in SINAL_DA_ABERTURA.items():
        anim.curva(aviso, ponta, "rotation",
                   [(0.0, anim.vetor(y=ABERTURA_EM_CRUZEIRO * sinal * SENTIDO_DE_Y)),
                    (DUR_WINDUP, anim.vetor(y=ABERTURA_NO_GOLPE * sinal * SENTIDO_DE_Y))])
    # O pescoco COMPENSA o tronco: a cabeca continua encarando quem esta embaixo.
    # Sem a compensacao o bico apontaria para o chao a frente do alvo, e o telegrafo
    # deixaria de dizer CONTRA QUEM o mergulho vai.
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-22))])
    anim.derivar(aviso, "head", "beak", "rotation", 0.3)
    anim.curva(aviso, "crest", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-16))])
    for antena in ANTENAS:
        # As antenas se deitam para tras: e o sinal de que ela ja decidiu.
        anim.curva(aviso, antena, "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=48))])
    anim.curva(aviso, "abdomen", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-14))])
    anim.derivar(aviso, "abdomen", "tail", "rotation", 1.4)
    for perna in PERNAS:
        anim.curva(aviso, perna, "rotation",
                   [(0.0, anim.vetor(x=52)), (DUR_WINDUP * 0.35, anim.vetor(x=30)),
                    (DUR_WINDUP, anim.vetor(x=-46))])
        anim.curva(aviso, GARRA_DA_PERNA[perna], "rotation",
                   [(0.0, anim.vetor(x=-26)), (DUR_WINDUP, anim.vetor(x=-52))])
    a.clipe("windup", DUR_WINDUP, aviso)

    # --------------------------------------------------------------- strike
    # A JANELA QUE MACHUCA. O servidor gasta 6 ticks (0.3 s) e o clipe dura 0.4: o
    # excedente e permitido de proposito -- o servidor manda no fim e o Java corta
    # o clipe. Faltar e que nao pode.
    #
    # As garras passam do ponto e voltam. O exagero e o que separa visualmente o
    # golpe do telegrafo; sem ele os dois clipes leem como um movimento continuo e
    # o jogador nao consegue marcar onde o dano saiu.
    golpe = {}
    anim.curva(golpe, "body", "rotation",
               [(0.0, anim.vetor(x=46)), (DUR_STRIKE * 0.5, anim.vetor(x=16)),
                (DUR_STRIKE, anim.vetor(x=24))])
    anim.curva(golpe, "body", "position",
               [(0.0, anim.vetor(y=1.2)), (DUR_STRIKE * 0.5, anim.vetor(y=-1.4)),
                (DUR_STRIKE, anim.vetor(y=-0.8))])
    for asa, sinal in SINAL_DA_BATIDA.items():
        anim.curva(golpe, asa, "rotation",
                   [(0.0, anim.vetor(z=-40 * sinal)), (DUR_STRIKE * 0.5, anim.vetor(z=-8 * sinal)),
                    (DUR_STRIKE, anim.vetor(z=-18 * sinal))])
    _abrir(golpe, ABERTURA_NO_GOLPE, DUR_STRIKE)
    anim.curva(golpe, "head", "rotation",
               [(0.0, anim.vetor(x=-22)), (DUR_STRIKE * 0.5, anim.vetor(x=4)),
                (DUR_STRIKE, anim.vetor(x=-6))])
    anim.derivar(golpe, "head", "beak", "rotation", 0.3)
    anim.curva(golpe, "crest", "rotation",
               [(0.0, anim.vetor(x=-16)), (DUR_STRIKE, anim.vetor(x=-4))])
    for antena in ANTENAS:
        anim.curva(golpe, antena, "rotation",
                   [(0.0, anim.vetor(x=48)), (DUR_STRIKE, anim.vetor(x=30))])
    anim.curva(golpe, "abdomen", "rotation",
               [(0.0, anim.vetor(x=-14)), (DUR_STRIKE, anim.vetor(x=8))])
    anim.derivar(golpe, "abdomen", "tail", "rotation", 1.2)
    for perna in PERNAS:
        anim.curva(golpe, perna, "rotation",
                   [(0.0, anim.vetor(x=-46)), (DUR_STRIKE * 0.5, anim.vetor(x=-74)),
                    (DUR_STRIKE, anim.vetor(x=-58))])
        anim.curva(golpe, GARRA_DA_PERNA[perna], "rotation",
                   [(0.0, anim.vetor(x=-52)), (DUR_STRIKE * 0.5, anim.vetor(x=-18)),
                    (DUR_STRIKE, anim.vetor(x=-34))])
    a.clipe("strike", DUR_STRIKE, golpe)

    # -------------------------------------------------------------- recovery
    # A SUBIDA -- E ELA E A JANELA DO JOGADOR, nao um enfeite de fim de golpe.
    #
    # Dura TICKS_DE_SUBIDA_APOS_MERGULHO, que e o tempo em que o servidor a proibe
    # de comandar. Nesse intervalo o esquadrao fica com o alvo velho e sem
    # reagrupar. Se este clipe acabar antes, ela aparece de volta no posto de
    # comando NA TELA enquanto ainda esta calada -- e o jogador, que estava
    # contando os segundos pela silhueta, para de contar cedo demais.
    #
    # A asa vai a ABERTURA_NA_SUBIDA: e aqui que a envergadura e maxima, porque e
    # aqui que ela precisa ser vista.
    subida = {}
    anim.curva(subida, "body", "rotation",
               [(0.0, anim.vetor(x=24)), (DUR_RECOVERY * 0.3, anim.vetor(x=-26)),
                (DUR_RECOVERY, anim.vetor(x=-4))])
    anim.curva(subida, "body", "position",
               [(0.0, anim.vetor(y=-0.8)), (DUR_RECOVERY * 0.45, anim.vetor(y=1.6)),
                (DUR_RECOVERY, anim.vetor())])
    # Tres batidas largas na subida: e o esforco que explica por que ela nao pode
    # comandar enquanto sobe.
    _bater(subida, 44.0, DUR_RECOVERY, DUR_RECOVERY / 3.0)
    for ponta, sinal in SINAL_DA_ABERTURA.items():
        anim.curva(subida, ponta, "rotation",
                   [(0.0, anim.vetor(y=ABERTURA_NO_GOLPE * sinal * SENTIDO_DE_Y)),
                    (DUR_RECOVERY * 0.25,
                     anim.vetor(y=ABERTURA_NA_SUBIDA * sinal * SENTIDO_DE_Y)),
                    (DUR_RECOVERY, anim.vetor(y=ABERTURA_NO_POSTO * sinal * SENTIDO_DE_Y))])
    anim.curva(subida, "head", "rotation",
               [(0.0, anim.vetor(x=-6)), (DUR_RECOVERY * 0.3, anim.vetor(x=18)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(subida, "head", "beak", "rotation", -0.3)
    anim.curva(subida, "crest", "rotation",
               [(0.0, anim.vetor(x=-4)), (DUR_RECOVERY, anim.vetor())])
    for i, antena in enumerate(ANTENAS):
        anim.curva(subida, antena, "rotation",
                   [(0.0, anim.vetor(x=30)), (DUR_RECOVERY * (0.4 + i * 0.1), anim.vetor(x=-16)),
                    (DUR_RECOVERY, anim.vetor())])
    anim.curva(subida, "abdomen", "rotation",
               [(0.0, anim.vetor(x=8)), (DUR_RECOVERY * 0.3, anim.vetor(x=16)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(subida, "abdomen", "tail", "rotation", 1.3)
    for perna in PERNAS:
        anim.curva(subida, perna, "rotation",
                   [(0.0, anim.vetor(x=-58)), (DUR_RECOVERY * 0.4, anim.vetor(x=10)),
                    (DUR_RECOVERY, anim.vetor(x=40))])
        anim.curva(subida, GARRA_DA_PERNA[perna], "rotation",
                   [(0.0, anim.vetor(x=-34)), (DUR_RECOVERY, anim.vetor(x=-22))])
    a.clipe("recovery", DUR_RECOVERY, subida)

    # --------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece. Neste mob ela tem um segundo trabalho: interromper a
    # comandante CALA as ordens (RegrasDeComandoAereo poe ela em RECOLHER), e o
    # jogador precisa de um sinal de que aquele acerto valeu alguma coisa para o
    # bando inteiro -- e nao so para a barra de vida dela.
    #
    # As asas perdem a simetria: um cambaleio simetrico le como tique, e assimetria
    # le como perda de controle do voo.
    tropeco = {}
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.1, anim.vetor(x=-18, z=22)),
                (0.34, anim.vetor(x=9, z=-9)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "body", "position",
               [(0.0, anim.vetor()), (0.14, anim.vetor(y=-2.2)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "wing_left", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(z=-34)),
                (0.36, anim.vetor(z=12)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "wing_right", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(z=16)),
                (0.36, anim.vetor(z=-8)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "wing_tip_left", "rotation",
               [(0.0, anim.vetor(y=ABERTURA_NO_POSTO * SENTIDO_DE_Y)),
                (0.16, anim.vetor(y=ABERTURA_NA_SUBIDA * SENTIDO_DE_Y)),
                (DUR_STAGGER, anim.vetor(y=ABERTURA_NO_POSTO * SENTIDO_DE_Y))])
    anim.curva(tropeco, "wing_tip_right", "rotation",
               [(0.0, anim.vetor(y=-ABERTURA_NO_POSTO * SENTIDO_DE_Y)),
                (0.16, anim.vetor(y=-ABERTURA_NO_GOLPE * SENTIDO_DE_Y)),
                (DUR_STAGGER, anim.vetor(y=-ABERTURA_NO_POSTO * SENTIDO_DE_Y))])
    anim.curva(tropeco, "head", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(x=-26, y=20)),
                (0.36, anim.vetor(x=12, y=-8)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "head", "beak", "rotation", -0.4, eixos=(0, 1))
    anim.curva(tropeco, "crest", "rotation",
               [(0.0, anim.vetor()), (0.14, anim.vetor(x=-22)), (DUR_STAGGER, anim.vetor())])
    for i, antena in enumerate(ANTENAS):
        anim.curva(tropeco, antena, "rotation",
                   [(0.0, anim.vetor()), (0.12 + i * 0.04, anim.vetor(x=-40, z=18 - i * 36)),
                    (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "abdomen", "rotation",
               [(0.0, anim.vetor()), (0.16, anim.vetor(x=20)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "abdomen", "tail", "rotation", -0.8)
    for perna in PERNAS:
        anim.curva(tropeco, perna, "rotation",
                   [(0.0, anim.vetor(x=34)), (0.16, anim.vetor(x=-14)),
                    (DUR_STAGGER, anim.vetor(x=34))])
        anim.curva(tropeco, GARRA_DA_PERNA[perna], "rotation",
                   [(0.0, anim.vetor(x=-18)), (0.16, anim.vetor(x=-44)),
                    (DUR_STAGGER, anim.vetor(x=-18))])
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # ------------------------------------------------------------------ morte
    # ELA CAI. A asa abre de um lado e cede do outro, o corpo rola, e as antenas
    # -- a marca de quimera -- ficam caidas no ultimo quadro.
    #
    # A queda importa: num mob que o jogador passou o encontro inteiro tentando
    # trazer para baixo, a morte precisa TERMINAR no chao. Uma morte que some no ar
    # apaga o recibo da unica tatica que o encontro ensina.
    queda = {}
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.25, anim.vetor(x=-20, z=26)),
                (DUR_DEATH, anim.vetor(x=14, z=82))])
    anim.curva(queda, "body", "position",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.2, anim.vetor(y=1.0)),
                (DUR_DEATH, anim.vetor(y=-12.0))])
    anim.curva(queda, "wing_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(z=-46)),
                (DUR_DEATH, anim.vetor(z=-12))])
    anim.curva(queda, "wing_right", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(z=18)),
                (DUR_DEATH, anim.vetor(z=34))])
    for ponta, sinal in SINAL_DA_ABERTURA.items():
        anim.curva(queda, ponta, "rotation",
                   [(0.0, anim.vetor(y=ABERTURA_NO_POSTO * sinal * SENTIDO_DE_Y)),
                    (DUR_DEATH * 0.35, anim.vetor(y=ABERTURA_NA_SUBIDA * sinal * SENTIDO_DE_Y)),
                    (DUR_DEATH, anim.vetor(y=ABERTURA_EM_CRUZEIRO * sinal * SENTIDO_DE_Y))])
    anim.curva(queda, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.35, anim.vetor(x=-30)),
                (DUR_DEATH, anim.vetor(x=42))])
    anim.derivar(queda, "head", "beak", "rotation", 0.4)
    anim.curva(queda, "crest", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=26))])
    for antena in ANTENAS:
        # Caidas no ultimo quadro: a marca de quimera termina murcha, e e ela que
        # diz que o que morreu foi uma formiga, e nao um passaro.
        anim.curva(queda, antena, "rotation",
                   [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(x=-28)),
                    (DUR_DEATH, anim.vetor(x=72))])
    anim.curva(queda, "abdomen", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=-24))])
    anim.derivar(queda, "abdomen", "tail", "rotation", 1.5)
    for perna in PERNAS:
        anim.curva(queda, perna, "rotation",
                   [(0.0, anim.vetor(x=40)), (DUR_DEATH * 0.35, anim.vetor(x=-52)),
                    (DUR_DEATH, anim.vetor(x=-72))])
        anim.curva(queda, GARRA_DA_PERNA[perna], "rotation",
                   [(0.0, anim.vetor(x=-22)), (DUR_DEATH, anim.vetor(x=-8))])
    a.clipe("death", DUR_DEATH, queda)

    return a


# ------------------------------------------ validacoes que ligam arte e regra

def valida_subida_cobre_a_janela_do_jogador(a):
    """O clipe `recovery` dura, no minimo, a janela em que ela nao pode comandar.

    O servidor a proibe de comandar por
    `AvianCommanderTuning.TICKS_DE_SUBIDA_APOS_MERGULHO` ticks depois do mergulho.
    Essa janela e o unico preco do golpe dela: enquanto ela sobe, o esquadrao fica
    com o alvo velho e sem reagrupar, e quem sobreviveu ao mergulho tem tempo de
    matar um membro ou sair do cerco.

    Se o clipe acabar antes, a comandante aparece de volta em pose de posto de
    comando NA TELA enquanto o servidor ainda a mantem calada. Nada acusa: o dano
    sai certo, a ordem continua bloqueada, o log fica limpo. O que quebra e a
    unica leitura que o jogador tem para saber quanto tempo ainda tem -- e ele
    para de contar cedo demais, o que e pior do que nao ter contador nenhum.
    """
    duracao = a.clipes[a.nome_completo("recovery")]["animation_length"]
    exigido = TICKS_DE_SUBIDA_APOS_MERGULHO / TICKS_POR_SEGUNDO
    if duracao + 1e-9 < exigido:
        raise ErroDeArte(
            "o clipe 'recovery' dura %.2fs e a janela de subida do servidor dura %.2fs (%d ticks, "
            "AvianCommanderTuning.TICKS_DE_SUBIDA_APOS_MERGULHO): ela volta ao posto de comando na "
            "tela enquanto ainda esta proibida de comandar, e o jogador para de contar a janela "
            "antes de ela acabar"
            % (duracao, exigido, TICKS_DE_SUBIDA_APOS_MERGULHO))


def valida_asa_fecha_no_golpe(a):
    """A asa do GOLPE tem de ler diferente da asa de CRUZEIRO, a dez blocos.

    A envergadura e a silhueta deste bicho, e a silhueta e tudo o que chega da
    altitude de comando. Fechada, ela e um projetil; aberta, e uma comandante.
    Se os dois estados tiverem quase a mesma abertura, o jogador perde a unica
    pista de que o mergulho ja comecou -- e a resposta certa (sair de baixo) passa
    a depender de reflexo em vez de leitura.

    Isso nunca levanta excecao. O clipe roda, o dano sai, o cooldown sai; o que
    some e a diferenca entre "ela esta patrulhando" e "ela esta vindo".
    """
    golpe = a.clipes[a.nome_completo("strike")]
    cruzeiro = a.clipes[a.nome_completo("walk")]
    for ponta in ("wing_tip_left", "wing_tip_right"):
        quadros_golpe = golpe["bones"].get(ponta, {}).get("rotation")
        quadros_cruzeiro = cruzeiro["bones"].get(ponta, {}).get("rotation")
        if not quadros_golpe or not quadros_cruzeiro:
            raise ErroDeArte(
                "a ponta '%s' nao tem rotacao em 'strike' ou em 'walk': sem os dois estados nao ha "
                "o que comparar, e a asa deixa de contar em que fase ela esta" % ponta)
        abertura_golpe = abs(anim.valor_em(quadros_golpe, 0.0, anim.vetor())[1])
        abertura_cruzeiro = abs(anim.valor_em(quadros_cruzeiro, 0.0, anim.vetor())[1])
        degrau = abertura_cruzeiro - abertura_golpe
        if degrau < DEGRAU_MINIMO_DA_ASA:
            raise ErroDeArte(
                "a ponta '%s' abre %.1f graus no golpe e %.1f em cruzeiro: o degrau e %.1f e o "
                "minimo e %.1f. Com os dois estados empatados, a silhueta para de dizer se ela "
                "esta patrulhando ou vindo, e sair de baixo vira reflexo em vez de leitura"
                % (ponta, abertura_golpe, abertura_cruzeiro, degrau, DEGRAU_MINIMO_DA_ASA))


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(
        ataques=ATAQUES,
        extras=(valida_subida_cobre_a_janela_do_jogador, valida_asa_fecha_no_golpe))
