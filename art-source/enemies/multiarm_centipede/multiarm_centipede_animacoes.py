"""Clipes do Multiarm Centipede -- a representacao das fases que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, STAGGER OU RECOMPENSA. O servidor publica a
fase (`AttackPhase`) e o cambaleio; o cliente escolhe o clipe correspondente. Se
a animacao e a hitbox discordarem, quem esta errado e este arquivo.

O PROBLEMA QUE ESTE MOB TRAZ, E QUE NENHUM ANTERIOR TINHA. O servidor abre TRES
janelas seguidas com orcamentos DIFERENTES -- 12, 11 e 24 ticks de aviso -- e o
cliente tem UM unico clipe de `windup` para as tres. Isso nao e descuido: o
contrato de clipes deste mob e fixo (idle, walk, windup, strike, recovery,
stagger, death), e inventar um clipe por golpe multiplicaria o arquivo por tres
para mostrar o mesmo gesto.

A saida tem duas metades, e as duas sao cobradas por regua:

  * O CLIPE DE AVISO CABE NO GOLPE MAIS CURTO. Com 0.55 s (11 ticks) ele termina
    dentro dos 11 ticks do golpe mais rapido da sequencia, entao TODO golpe mostra
    o gesto inteiro. Um clipe mais longo que isso seria cortado no meio pelos
    golpes encadeados, e o jogador veria um braco que comeca a subir e nunca
    chega -- ele aprenderia que o aviso "as vezes" acontece.
  * O AVISO SEGURA O ULTIMO QUADRO. `hold_on_last_frame` faz os 24 ticks do golpe
    final terminarem com os bracos ARMADOS, e nao com o bicho relaxando no meio do
    proprio telegrafo. Sem isso, o golpe mais perigoso da sequencia seria o unico
    cujo aviso desaparece antes de o dano sair.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com o metodo de origem ao lado, e a
regua da biblioteca o compara com a SOMA dos tres clipes encadeados. Ele usa o
golpe FINAL, que e o mais longo dos tres: cobrir o maior cobre os outros, e os
menores sao cortados pelo servidor, que e o que a biblioteca permite de proposito.

CONVENCAO DE ROTACAO, medida neste geo e nao decorada. Com -Z na frente:
  * rotacao X NEGATIVA num membro pendurado (braco, perna) o joga para a FRENTE e
    para cima; X POSITIVA o joga para TRAS e para cima;
  * rotacao X POSITIVA no torax e na cabeca inclina para a FRENTE (olhar para
    baixo); NEGATIVA joga para tras.
Os bracos armam para TRAS (X positivo) e varrem para a FRENTE (X negativo).

A ONDA E O BICHO. Em todo clipe os tres pares se movem com DEFASAGEM -- dianteiro,
medio, traseiro -- e nunca no mesmo quadro. Seis bracos em fase leem como um
bloco unico se mexendo, que e exatamente a leitura que a geometria separou os
ossos para evitar. A defasagem nao e enfeite: e o que faz uma centopeia parecer
uma centopeia, e e o que permite ao jogador perceber que ha MAIS de um braco
mesmo quando a sequencia inteira sai em tres segundos.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/multiarm_centipede/multiarm_centipede_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/multiarm_centipede.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402

MOB = "multiarm_centipede"

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
    # hold_on_last_frame: ver o cabecalho. Os tres golpes da sequencia tem avisos
    # de 12, 11 e 24 ticks e um clipe so; segurar o ultimo quadro e o que faz o
    # aviso de 24 ticks terminar com os bracos no alto em vez de relaxados.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks de aviso de CADA golpe, copiados de MultiarmCentipedeTuning.sequencia().
# Eles estao aqui para a regua `valida_aviso_cabe_no_golpe_mais_curto` poder
# cobrar o clipe contra o MENOR deles -- que e o unico que aperta.
AVISOS_DA_SEQUENCIA = (12,   # MultiarmCentipedeTuning.WINDUP_DO_BRACO_TRASEIRO
                       11,   # MultiarmCentipedeTuning.WINDUP_DO_BRACO_MEDIO
                       24)   # MultiarmCentipedeTuning.WINDUP_DO_BRACO_DIANTEIRO

# Orcamento do golpe FINAL -- o mais longo dos tres, e por isso o que a soma dos
# clipes encadeados precisa cobrir.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=24,     # MultiarmCentipedeTuning.WINDUP_DO_BRACO_DIANTEIRO
                       active=5,      # MultiarmCentipedeTuning.JANELA_DO_BRACO_DIANTEIRO
                       recovery=26)}  # MultiarmCentipedeTuning.RECUPERACAO_DO_BRACO_DIANTEIRO

DUR_IDLE = 3.2
DUR_WALK = 1.0
DUR_WINDUP = 0.55    # 11 ticks -- o aviso mais curto da sequencia; ver a regua
DUR_STRIKE = 0.3     # 6 ticks -- um a mais que a janela de 5, de proposito
DUR_RECOVERY = 2.0   # 40 ticks -- sobra sobre os 26 do servidor; ele manda no fim
DUR_STAGGER = 1.2
DUR_DEATH = 1.8

# Os tres pares, da frente para tras. Esta tupla e a ordem da ONDA, e nao a ordem
# dos golpes: a onda percorre o corpo sempre no mesmo sentido, enquanto a
# sequencia de golpes vai do par traseiro ao dianteiro. Sao duas coisas
# diferentes de proposito -- a onda e locomocao e respiracao, e a sequencia e
# combate.
PARES = ("arm_front", "arm_mid", "arm_rear")

# Defasagem, em fracao de ciclo, entre um par e o seguinte. Um oitavo de ciclo e
# o menor degrau que ainda se enxerga a dez blocos; zero faria os seis bracos
# subirem juntos e apagaria a unica leitura que a geometria separou os ossos para
# entregar.
DEFASAGEM_DA_ONDA = 0.125

# Arrasto das mandibulas atras da cabeca. Negativo e pequeno: elas ficam para tras
# do movimento do cranio, que e o que uma peca articulada solta faz. Fator
# positivo as faria ANTECIPAR a cabeca -- o movimento de quem ja sabia.
ARRASTO_DA_MANDIBULA = -0.45


def _bracos(prefixo):
    return (prefixo + "_left", prefixo + "_right")


def _onda(bones, duracao, amplitude, base=0.0, fase_inicial=0.0):
    """Escreve a mesma cossenoide nos tres pares, com defasagem crescente.

    Escrita a mao, par por par, a onda ficaria com a fase certa hoje e errada na
    primeira vez que alguem mudasse a duracao do clipe -- e uma onda fora de fase
    nao da erro: da seis bracos que se mexem sem formar movimento nenhum.
    """
    for indice, prefixo in enumerate(PARES):
        fase = fase_inicial + indice * DEFASAGEM_DA_ONDA
        esquerdo, direito = _bracos(prefixo)
        anim.curva(bones, esquerdo, "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(duracao, duracao, amplitude, base=base, fase=fase)])
        # O lado direito e DERIVADO do esquerdo, e nao escrito de novo: duas
        # curvas independentes divergem na primeira correcao, e a divergencia
        # aparece como um bicho visivelmente torto de um lado so -- que ninguem
        # descobre sem girar a camera em volta dele.
        anim.derivar(bones, esquerdo, direito, "rotation", 1.0)


def _degrau(bones, pares_de_tempo_por_indice, canal="rotation"):
    """Mesma sequencia de keyframes nos tres pares, deslocada no TEMPO.

    `pares_de_tempo_por_indice` recebe o indice do par e devolve a lista de
    (t, vetor). E assim que o telegrafo e o golpe mantem a onda: os seis bracos
    fazem o mesmo gesto, e nao no mesmo instante.
    """
    for indice, prefixo in enumerate(PARES):
        esquerdo, direito = _bracos(prefixo)
        anim.curva(bones, esquerdo, canal, pares_de_tempo_por_indice(indice))
        anim.derivar(bones, esquerdo, direito, canal, 1.0)


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # ---------------------------------------------------------------- ocio
    # Respiracao lenta (3.2 s) com a onda correndo pelos bracos. Amplitude
    # pequena: ocio chamativo competiria com o telegrafo, e neste mob o telegrafo
    # e o unico texto que o jogador tem para ler.
    ocio = {}
    anim.curva(ocio, "thorax", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.2)])
    anim.curva(ocio, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 3.0, fase=0.1)])
    for mandibula in ("mandible_left", "mandible_right"):
        anim.derivar(ocio, "head", mandibula, "rotation", ARRASTO_DA_MANDIBULA)
    _onda(ocio, DUR_IDLE, 5.0)
    # A cauda varre o chao devagar, no eixo Y: e o unico movimento horizontal do
    # ocio, e e ele que impede a silhueta parada de ler como estatua.
    anim.curva(ocio, "tail", "rotation",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 6.0, fase=0.25)])
    anim.curva(ocio, "hip", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 0.35)])
    a.clipe("idle", DUR_IDLE, ocio)

    # ------------------------------------------------------------ locomocao
    # Um ciclo por segundo, e os bracos REMAM: eles sao a locomocao deste bicho,
    # e nao enfeite de tronco. Amplitude grande (16 graus) com a onda defasada e o
    # que faz um corpo erguido de 2.2 blocos andar como centopeia em vez de
    # deslizar como boneco.
    marcha = {}
    _onda(marcha, DUR_WALK, 16.0)
    anim.curva(marcha, "leg_rear_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 14.0)])
    # A perna direita e o contrario da esquerda: fator -1. Escrita a mao, ela
    # ficaria meio quadro fora de fase na primeira correcao da esquerda, e o
    # sintoma e um bicho que manca sem que ninguem saiba dizer por que.
    anim.derivar(marcha, "leg_rear_left", "leg_rear_right", "rotation", -1.0)
    anim.curva(marcha, "hip", "rotation",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 5.0)])
    anim.derivar(marcha, "hip", "tail", "rotation", -1.4, eixos=(1,))
    anim.curva(marcha, "thorax", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 3.2, fase=0.25)])
    anim.derivar(marcha, "thorax", "head", "rotation", -0.8)
    for mandibula in ("mandible_left", "mandible_right"):
        anim.derivar(marcha, "head", mandibula, "rotation", ARRASTO_DA_MANDIBULA)
    # O solavanco vertical tem periodo METADE do ciclo: sao duas pisadas por
    # ciclo, e uma so faria o bicho mancar.
    anim.curva(marcha, "hip", "position",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 0.7)])
    a.clipe("walk", DUR_WALK, marcha)

    # --------------------------------------------------------------- windup
    # O AVISO. Meio segundo em que os seis bracos armam para TRAS enquanto o
    # torax se joga para tras e o corpo se ergue mais -- tres sinais que mudam a
    # silhueta de LUGAR, e nao so de forma. Silhueta que muda de lugar e o que se
    # le de longe.
    #
    # O par que abre a onda e o DIANTEIRO, e ele arma MAIS ALTO que os outros dois
    # (fator 1.0, 0.82, 0.66). Isso nao e estetica: o golpe final e justamente o
    # dos bracos dianteiros, e o degrau de altura e o que permite ao jogador ver,
    # no meio da sequencia, que o par grande ainda nao jogou.
    FATOR_POR_PAR = (1.0, 0.82, 0.66)
    ARMADO = 62.0
    aviso = {}
    _degrau(aviso, lambda i: [
        (0.0, anim.vetor()),
        (DUR_WINDUP * (0.35 + i * 0.08), anim.vetor(x=ARMADO * FATOR_POR_PAR[i] * 0.35)),
        (DUR_WINDUP, anim.vetor(x=ARMADO * FATOR_POR_PAR[i]))])
    anim.curva(aviso, "thorax", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.4, anim.vetor(x=-5)),
                (DUR_WINDUP, anim.vetor(x=-16))])
    # A cabeca ENCARA o alvo durante o aviso: ela inclina para a frente enquanto o
    # torax vai para tras. Sem isso o bicho armaria olhando para o ceu, e o
    # jogador perderia a unica pista de PARA ONDE o golpe vai sair.
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=13))])
    for mandibula in ("mandible_left", "mandible_right"):
        anim.derivar(aviso, "head", mandibula, "rotation", -1.2)
    anim.curva(aviso, "hip", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=0.9, z=0.6))])
    anim.curva(aviso, "leg_rear_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=11))])
    anim.derivar(aviso, "leg_rear_left", "leg_rear_right", "rotation", 1.0)
    anim.curva(aviso, "tail", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-9))])
    a.clipe("windup", DUR_WINDUP, aviso)

    # --------------------------------------------------------------- strike
    # A JANELA QUE MACHUCA. O servidor gasta no maximo 5 ticks (0.25 s) e o clipe
    # dura 0.3: o excedente e permitido de proposito -- o servidor manda no fim e o
    # Java corta. Faltar e que nao pode.
    #
    # Os bracos passam do ponto (-78) e voltam um pouco (-56). O exagero e o que
    # separa visualmente o golpe do telegrafo; sem ele os dois clipes leem como um
    # movimento continuo e o jogador nao consegue marcar onde o dano saiu -- que
    # numa sequencia de tres e a diferenca entre aprender e adivinhar.
    golpe = {}
    _degrau(golpe, lambda i: [
        (0.0, anim.vetor(x=62.0 * (1.0, 0.82, 0.66)[i])),
        (DUR_STRIKE * (0.5 + i * 0.08), anim.vetor(x=-78)),
        (DUR_STRIKE, anim.vetor(x=-56))])
    anim.curva(golpe, "thorax", "rotation",
               [(0.0, anim.vetor(x=-16)), (DUR_STRIKE * 0.55, anim.vetor(x=26)),
                (DUR_STRIKE, anim.vetor(x=19))])
    anim.derivar(golpe, "thorax", "head", "rotation", 0.45)
    for mandibula in ("mandible_left", "mandible_right"):
        anim.derivar(golpe, "head", mandibula, "rotation", -1.1)
    # O corpo inteiro entra no golpe: z NEGATIVO e para a frente nesta geometria.
    # Sem esse avanco, um bicho de seis bracos pareceria bater com os membros
    # soltos, e o empurrao que o servidor aplica nao teria desenho nenhum.
    anim.curva(golpe, "hip", "position",
               [(0.0, anim.vetor(y=0.9, z=0.6)), (DUR_STRIKE * 0.55, anim.vetor(y=-0.4, z=-2.4)),
                (DUR_STRIKE, anim.vetor(y=-0.2, z=-1.6))])
    anim.curva(golpe, "leg_rear_left", "rotation",
               [(0.0, anim.vetor(x=11)), (DUR_STRIKE, anim.vetor(x=-8))])
    anim.derivar(golpe, "leg_rear_left", "leg_rear_right", "rotation", 1.0)
    anim.curva(golpe, "tail", "rotation",
               [(0.0, anim.vetor(x=-9)), (DUR_STRIKE, anim.vetor(x=6))])
    a.clipe("strike", DUR_STRIKE, golpe)

    # -------------------------------------------------------------- recovery
    a.clipe("recovery", DUR_RECOVERY, _recuperacao())

    # --------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, e neste mob ela e a informacao mais cara
    # do encontro: stagger no meio da sequencia CANCELA o resto dela no servidor.
    # Se a tela nao mostrar o corte, o jogador que acabou de interromper dois
    # golpes continuaria recuando deles -- e a recompensa por interromper viraria
    # invisivel, sem nada no log.
    #
    # Os bracos DESABAM, e nao balancam: um cambaleio com a onda ainda correndo
    # leria como mais um gesto da sequencia. A onda para aqui, de proposito.
    tropeco = {}
    _degrau(tropeco, lambda i: [
        (0.0, anim.vetor()),
        (0.14 + i * 0.04, anim.vetor(x=-34 - i * 5)),
        (0.44 + i * 0.04, anim.vetor(x=12)),
        (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "thorax", "rotation",
               [(0.0, anim.vetor()), (0.16, anim.vetor(x=-26, z=9)), (0.46, anim.vetor(x=11, z=-4)),
                (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "thorax", "head", "rotation", 1.2)
    for mandibula in ("mandible_left", "mandible_right"):
        anim.derivar(tropeco, "head", mandibula, "rotation", -0.9)
    anim.curva(tropeco, "hip", "rotation",
               [(0.0, anim.vetor()), (0.18, anim.vetor(y=14)), (0.5, anim.vetor(y=-6)),
                (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "hip", "tail", "rotation", -1.3, eixos=(1,))
    # Um pe atras para nao cair: o passo de recuperacao e o que faz o cambaleio
    # ler como perda de equilibrio e nao como tique.
    anim.curva(tropeco, "leg_rear_left", "rotation",
               [(0.0, anim.vetor()), (0.2, anim.vetor(x=17)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "leg_rear_left", "leg_rear_right", "rotation", -0.7)
    anim.curva(tropeco, "hip", "position",
               [(0.0, anim.vetor()), (0.2, anim.vetor(y=-1.4, z=1.2)),
                (DUR_STAGGER, anim.vetor())])
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # ------------------------------------------------------------------ morte
    # hold_on_last_frame: o corpo fica caido ate a entidade sumir. Um clipe de
    # morte que volta ao repouso mostra o bicho ERGUIDO no ultimo quadro antes de
    # desaparecer, e a leitura vira "ele sumiu", nao "ele caiu".
    #
    # O torax desaba para a FRENTE e o quadril desce 5 px: sem a descida, o modelo
    # pivota no ar e metade do corpo atravessa o chao.
    queda = {}
    anim.curva(queda, "hip", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(x=10, z=14)),
                (DUR_DEATH, anim.vetor(x=24, z=52))])
    anim.curva(queda, "hip", "position",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(y=-1.4)),
                (DUR_DEATH, anim.vetor(y=-5))])
    anim.curva(queda, "thorax", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.35, anim.vetor(x=34)),
                (DUR_DEATH, anim.vetor(x=62))])
    anim.derivar(queda, "thorax", "head", "rotation", 0.55)
    for mandibula in ("mandible_left", "mandible_right"):
        anim.derivar(queda, "head", mandibula, "rotation", -0.8)
    # Os bracos ENCOLHEM para dentro, que e o que um artropode morto faz -- e e o
    # oposto do desabar do cambaleio. As duas quedas precisam ser distinguiveis:
    # confundir "cambaleou" com "morreu" faz o jogador parar de atacar cedo demais.
    _degrau(queda, lambda i: [
        (0.0, anim.vetor()),
        (DUR_DEATH * (0.4 + i * 0.07), anim.vetor(x=46 + i * 6)),
        (DUR_DEATH, anim.vetor(x=74 + i * 5))])
    anim.curva(queda, "leg_rear_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=33)),
                (DUR_DEATH, anim.vetor(x=58))])
    anim.derivar(queda, "leg_rear_left", "leg_rear_right", "rotation", 1.0)
    anim.curva(queda, "tail", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=-22))])
    a.clipe("death", DUR_DEATH, queda)

    return a


def _recuperacao():
    """A JANELA DE RESPOSTA -- o clipe mais longo do mob depois do ocio.

    Ela existe separada porque e a peca que ENSINA: o golpe final gasta 26 ticks
    aqui, e e neles que o jogador pune. Os bracos caem pesados, o torax fica
    dobrado para a frente na primeira metade e so depois o bicho se ergue de novo.

    Encurtar este clipe nao mudaria o servidor -- mudaria so a leitura, e o
    jogador acharia que apanhou sem ter tido janela. E o contrario tambem custa:
    um clipe que termina antes dos 26 ticks faria o bicho voltar a postura de
    ataque enquanto ainda esta indefeso, e o jogador pararia de punir.
    """
    volta = {}
    _degrau(volta, lambda i: [
        (0.0, anim.vetor(x=-56)),
        (DUR_RECOVERY * (0.3 + i * 0.05), anim.vetor(x=-22 + i * 4)),
        (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "thorax", "rotation",
               [(0.0, anim.vetor(x=19)), (DUR_RECOVERY * 0.45, anim.vetor(x=25)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "thorax", "head", "rotation", 0.5)
    for mandibula in ("mandible_left", "mandible_right"):
        anim.derivar(volta, "head", mandibula, "rotation", -0.7)
    anim.curva(volta, "hip", "position",
               [(0.0, anim.vetor(y=-0.2, z=-1.6)), (DUR_RECOVERY * 0.45, anim.vetor(y=-1.1)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "leg_rear_left", "rotation",
               [(0.0, anim.vetor(x=-8)), (DUR_RECOVERY * 0.45, anim.vetor(x=13)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "leg_rear_left", "leg_rear_right", "rotation", 1.0)
    anim.curva(volta, "tail", "rotation",
               [(0.0, anim.vetor(x=6)), (DUR_RECOVERY, anim.vetor())])
    return volta


# ------------------------------------------ validacao que liga arte e regra

def valida_aviso_cabe_no_golpe_mais_curto(a):
    """O clipe de aviso tem de terminar DENTRO do windup mais curto da sequencia.

    Este mob tem tres janelas seguidas com orcamentos diferentes e UM clipe de
    aviso para as tres. O clipe e cortado pelo servidor quando a fase muda; se ele
    for mais longo que o golpe mais rapido, o aviso daquele golpe aparece pela
    METADE -- um braco que comeca a subir e nunca chega ao alto.

    Isso nao levanta erro em lugar nenhum: o dano sai, a fase sai, o log fica
    limpo. O que quebra e a unica coisa que a sequencia ensina, porque o jogador
    passa a ver um aviso que "as vezes" acontece e aprende a nao confiar nele.

    A regua tambem cobra `hold_on_last_frame`. Sem ele, o golpe FINAL -- que tem o
    aviso mais longo, 24 ticks contra os 11 do clipe -- terminaria com o bicho
    relaxado, e o golpe mais perigoso da sequencia seria o unico sem telegrafo no
    instante em que o dano sai.
    """
    clipe = a.clipes[a.nome_completo("windup")]
    duracao_em_ticks = clipe["animation_length"] * 20.0
    menor_aviso = min(AVISOS_DA_SEQUENCIA)
    if duracao_em_ticks > menor_aviso + 1e-9:
        raise anim.ErroDeArte(
            "o clipe de aviso dura %.2f ticks e o golpe mais curto da sequencia avisa por %d "
            "ticks (%s): nesse golpe o jogador ve um braco que comeca a subir e nunca chega ao "
            "alto, e aprende que o aviso 'as vezes' acontece"
            % (duracao_em_ticks, menor_aviso, list(AVISOS_DA_SEQUENCIA)))
    if a.loops["windup"] != "hold_on_last_frame":
        raise anim.ErroDeArte(
            "o aviso repete com %r e precisa de 'hold_on_last_frame': o golpe final avisa por %d "
            "ticks contra os %.0f do clipe, e sem segurar o ultimo quadro ele termina com o bicho "
            "relaxado -- o golpe mais perigoso da sequencia seria o unico sem telegrafo no "
            "instante em que o dano sai"
            % (a.loops["windup"], max(AVISOS_DA_SEQUENCIA), duracao_em_ticks))
    print("aviso: clipe %.1f ticks  <=  menor golpe %d ticks  (maior %d, segurado no ultimo quadro)"
          % (duracao_em_ticks, menor_aviso, max(AVISOS_DA_SEQUENCIA)))



if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(ataques=ATAQUES,
                                          extras=(valida_aviso_cabe_no_golpe_mais_curto,))
