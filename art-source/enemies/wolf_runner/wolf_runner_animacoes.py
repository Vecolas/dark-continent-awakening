"""Clipes do Wolf Runner -- a representacao das fases que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, IMPULSO OU STAGGER. O servidor publica a fase
(`AttackPhase`) e o cambaleio; o cliente escolhe o clipe correspondente. Se a
animacao e a hitbox discordarem, quem esta errado e este arquivo.

O CLIPE QUE CARREGA O BICHO E O `windup`, E ELE E O TELEGRAFO DO SALTO. O Wolf
Runner fecha distancia com um bote: no ultimo tick do aviso o servidor aplica um
impulso unico e o corpo viaja. Sem um agachamento visivel antes disso, o salto
vira TELEPORTE -- o bicho estava a quatro blocos e agora esta em cima do
jogador, com dano certo, cooldown certo e log limpo. Por isso
`valida_telegrafo_do_salto` cobra duas coisas que a regua generica da biblioteca
nao cobra:

  1. o `windup` SOZINHO tem de durar os ticks do aviso do bote. A regua da
     biblioteca soma windup + strike + recovery, e uma soma generosa aprovaria
     um aviso de dois ticks com uma recuperacao longa -- que e exatamente o
     telefone quebrado que este mob nao pode ter;
  2. o corpo tem de DESCER no aviso e SUBIR no golpe. Agachar e o unico sinal
     que um quadrupede tem para dizer "vou pular", e ele precisa existir na
     tela, nao so no comentario.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com o campo de origem ao lado. Ele
carrega o BOTE, que e o golpe mais longo dos dois; a mordida curta
(10 + 4 + 10 = 24 ticks) cabe folgada dentro do mesmo encadeamento, e e por isso
que ela nao aparece aqui -- duas entradas para os mesmos tres clipes seriam duas
reguas medindo a mesma coisa, e a mais frouxa venceria em silencio.

CONVENCAO DE ROTACAO, medida neste geo e nao decorada. Com -Z na frente:
  * rotacao X NEGATIVA num membro pendurado (pata, cauda) o joga para a FRENTE;
    X POSITIVA o joga para TRAS;
  * rotacao X POSITIVA no tronco e na cabeca inclina para a FRENTE (focinho para
    baixo); NEGATIVA levanta o focinho;
  * posicao Z NEGATIVA e para a frente; posicao Y NEGATIVA e agachar.

AS ANTENAS NUNCA SAO ESCRITAS A MAO. Elas sao sempre DERIVADAS da cabeca, com
fator negativo: antena e apendice leve e chega atrasada ao movimento. Escritas a
mao, ficariam com a fase certa hoje e errada na primeira correcao do pescoco --
e antena fora de fase le como erro de render, nao como inseto.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/wolf_runner/wolf_runner_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/wolf_runner.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402
from comum import ErroDeArte            # noqa: E402

MOB = "wolf_runner"

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType
# do Java VENCE o JSON, e cravar a repeticao la transformaria este dicionario em
# documentacao que discorda do comportamento.
#
# Esta e tambem a UNICA lista de clipes deste mob, e ela e o contrato com
# WolfRunnerEntity: os sete nomes abaixo sao os sete que o controller pede.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o aviso termina AGACHADO e fica assim ate o servidor
    # trocar de fase. Voltando ao repouso no fim do clipe, o bicho se levantaria
    # na tela enquanto o servidor ainda esta em WINDUP -- o jogador leria "ele
    # desistiu" e tomaria o bote na cara.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de WolfRunnerTuning, com o campo de origem ao lado.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=16,     # WolfRunnerTuning.WINDUP_DO_BOTE
                       active=5,      # WolfRunnerTuning.JANELA_DO_BOTE
                       recovery=14)}  # WolfRunnerTuning.RECUPERACAO_DO_BOTE

# WolfRunnerTuning.WINDUP_DO_BOTE, isolado: e este numero, e nao a soma dos
# tres, que valida_telegrafo_do_salto cobra contra o clipe de aviso.
TICKS_DO_AVISO_DO_BOTE = 16
TICKS_POR_SEGUNDO = 20.0

DUR_IDLE = 2.4
DUR_WALK = 0.6       # passada curta e rapida: velocidade 0.38 e a maior dos peoes
DUR_WINDUP = 0.8     # 16 ticks
DUR_STRIKE = 0.3     # 6 ticks -- um a mais que a janela, de proposito
DUR_RECOVERY = 0.75  # 15 ticks
DUR_STAGGER = 0.5
DUR_DEATH = 1.2

# Quanto o corpo DESCE no fim do aviso, em px do modelo. Negativo e agachar.
#
# 1.8 px num bicho de 13 px de altura e um agachamento de 14% -- visivel de
# longe sem o corpo atravessar o chao. Zero aqui nao daria erro nenhum: daria o
# salto sem aviso, que e a unica coisa que este arquivo existe para impedir.
AGACHAMENTO_DO_AVISO = -1.8

# Quanto o corpo SOBE no auge do bote, em px do modelo.
ALTURA_DO_BOTE = 3.2

# Arrasto da antena atras da cabeca. Negativo e pequeno: apendice leve chega
# atrasado. Fator positivo faria a antena ANTECIPAR o pescoco, que e o
# movimento de quem ja sabia -- e antena nao decide nada.
ARRASTO_DA_ANTENA = -0.45


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # ---------------------------------------------------------------- ocio
    # Respiracao curta e nervosa (2.4 s). Um corredor parado nao descansa: ele
    # espera. A cauda e o que mais se mexe, porque e ela que diz que o bicho
    # esta pronto para virar.
    ocio = {}
    anim.curva(ocio, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 1.4)])
    anim.curva(ocio, "neck", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.0, fase=0.15)])
    anim.derivar(ocio, "neck", "head", "rotation", -0.6)
    anim.derivar(ocio, "head", "antenna_left", "rotation", ARRASTO_DA_ANTENA)
    anim.derivar(ocio, "head", "antenna_right", "rotation", ARRASTO_DA_ANTENA)
    anim.curva(ocio, "tail", "rotation",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 9.0, fase=0.3)])
    anim.curva(ocio, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 0.3)])
    a.clipe("idle", DUR_IDLE, ocio)

    # ------------------------------------------------------------ locomocao
    # Ciclo de 0.6 s -- metade do de um bicho pesado. Amplitude grande nas
    # patas (26 graus): passada longa e rapida e o que le como corrida. Passada
    # curta com o mesmo deslocamento le como boneco deslizando, e nenhum portao
    # ve isso.
    marcha = {}
    anim.curva(marcha, "leg_front_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 26.0)])
    # Galope, e nao trote: o par dianteiro anda JUNTO e o traseiro vem meio
    # ciclo depois. Alternar em diagonal (fator -1 entre esquerda e direita)
    # daria o passo de um cavalo de carga, e este bicho persegue.
    anim.derivar(marcha, "leg_front_left", "leg_front_right", "rotation", 1.0)
    anim.curva(marcha, "leg_back_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 30.0, fase=0.5)])
    anim.derivar(marcha, "leg_back_left", "leg_back_right", "rotation", 1.0)
    # As patas NUNCA sao escritas a mao: derivadas, elas continuam com a fase
    # certa quando alguem mexer na canela. A mao, a primeira correcao da perna
    # deixa a pata meio quadro atras dela, e isso ninguem consegue descrever.
    anim.derivar(marcha, "leg_front_left", "paw_front_left", "rotation", -0.5)
    anim.derivar(marcha, "leg_front_right", "paw_front_right", "rotation", -0.5)
    anim.derivar(marcha, "leg_back_left", "paw_back_left", "rotation", -0.5)
    anim.derivar(marcha, "leg_back_right", "paw_back_right", "rotation", -0.5)
    # O lombo ondula: num galope o tronco e mola, e sem isso as patas parecem
    # penduradas num caixote.
    anim.curva(marcha, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 5.0, fase=0.25)])
    anim.derivar(marcha, "body", "neck", "rotation", -0.8)
    anim.derivar(marcha, "neck", "head", "rotation", -0.5)
    anim.derivar(marcha, "head", "antenna_left", "rotation", ARRASTO_DA_ANTENA)
    anim.derivar(marcha, "head", "antenna_right", "rotation", ARRASTO_DA_ANTENA)
    anim.curva(marcha, "tail", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 7.0, fase=0.5)])
    # Duas batidas por ciclo: o galope tem duas suspensoes, e uma so faria o
    # bicho mancar.
    anim.curva(marcha, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 0.8)])
    a.clipe("walk", DUR_WALK, marcha)

    # --------------------------------------------------------------- windup
    # O AVISO DO BOTE. Ele dura 0.8 s e tem tres sinais que mudam a silhueta de
    # LUGAR, e nao so de forma: o corpo DESCE, as patas traseiras dobram e a
    # cauda sobe. Silhueta que muda de lugar e o que se le a vinte blocos;
    # mudanca de forma some no meio do mato.
    #
    # A cabeca NAO desce junto: o pescoco contra-gira para o focinho continuar
    # apontado para o alvo. E ele que diz PARA ONDE o bote vai, e um bicho que
    # olha para o chao no quadro do aviso nao diz nada.
    #
    # A maior parte do curso acontece na SEGUNDA metade: arranque lento e o que
    # da ao jogador tempo de sair do caminho antes do ponto de nao-retorno.
    aviso = {}
    anim.curva(aviso, "body", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.35, anim.vetor(y=-0.5)),
                (DUR_WINDUP, anim.vetor(y=AGACHAMENTO_DO_AVISO))])
    anim.curva(aviso, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-9))])
    anim.curva(aviso, "neck", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=13))])
    anim.derivar(aviso, "neck", "head", "rotation", 0.4)
    anim.derivar(aviso, "head", "antenna_left", "rotation", ARRASTO_DA_ANTENA)
    anim.derivar(aviso, "head", "antenna_right", "rotation", ARRASTO_DA_ANTENA)
    # As traseiras comprimem (X positivo as joga para tras, dobrando o jarrete);
    # as dianteiras so plantam.
    anim.curva(aviso, "leg_back_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.35, anim.vetor(x=8)),
                (DUR_WINDUP, anim.vetor(x=34))])
    anim.derivar(aviso, "leg_back_left", "leg_back_right", "rotation", 1.0)
    anim.derivar(aviso, "leg_back_left", "paw_back_left", "rotation", -0.7)
    anim.derivar(aviso, "leg_back_left", "paw_back_right", "rotation", -0.7)
    anim.curva(aviso, "leg_front_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-12))])
    anim.derivar(aviso, "leg_front_left", "leg_front_right", "rotation", 1.0)
    anim.derivar(aviso, "leg_front_left", "paw_front_left", "rotation", -0.4)
    anim.derivar(aviso, "leg_front_left", "paw_front_right", "rotation", -0.4)
    # Cauda ereta: e o sinal que se enxerga por cima do mato quando o corpo ja
    # esta baixo demais para aparecer.
    anim.curva(aviso, "tail", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=32))])
    a.clipe("windup", DUR_WINDUP, aviso)

    # --------------------------------------------------------------- strike
    # O BOTE. O servidor gasta 5 ticks (0.25 s) e o clipe dura 0.3: o excedente
    # e permitido de proposito -- o servidor manda no fim e o Java corta o
    # clipe. Faltar e que nao pode.
    #
    # O corpo SOBE e vai para a FRENTE (z negativo). Esse par e o que distingue
    # um salto de um empurrao: so subir le como pulo no lugar, so avancar le
    # como escorregao.
    golpe = {}
    anim.curva(golpe, "body", "position",
               [(0.0, anim.vetor(y=AGACHAMENTO_DO_AVISO)),
                (DUR_STRIKE * 0.45, anim.vetor(y=ALTURA_DO_BOTE, z=-2.6)),
                (DUR_STRIKE, anim.vetor(y=ALTURA_DO_BOTE * 0.6, z=-3.4))])
    anim.curva(golpe, "body", "rotation",
               [(0.0, anim.vetor(x=-9)), (DUR_STRIKE * 0.45, anim.vetor(x=14)),
                (DUR_STRIKE, anim.vetor(x=6))])
    anim.curva(golpe, "neck", "rotation",
               [(0.0, anim.vetor(x=13)), (DUR_STRIKE * 0.45, anim.vetor(x=-6)),
                (DUR_STRIKE, anim.vetor(x=2))])
    anim.derivar(golpe, "neck", "head", "rotation", 0.5)
    anim.derivar(golpe, "head", "antenna_left", "rotation", ARRASTO_DA_ANTENA)
    anim.derivar(golpe, "head", "antenna_right", "rotation", ARRASTO_DA_ANTENA)
    # A mandibula abre no auge e fecha no fim: e o unico quadro que diz que o
    # bote MORDE, e nao so empurra.
    anim.curva(golpe, "jaw", "rotation",
               [(0.0, anim.vetor()), (DUR_STRIKE * 0.45, anim.vetor(x=26)),
                (DUR_STRIKE, anim.vetor(x=4))])
    # As traseiras estendem (a mola solta); as dianteiras vao a frente para
    # receber o chao.
    anim.curva(golpe, "leg_back_left", "rotation",
               [(0.0, anim.vetor(x=34)), (DUR_STRIKE * 0.45, anim.vetor(x=-26)),
                (DUR_STRIKE, anim.vetor(x=-14))])
    anim.derivar(golpe, "leg_back_left", "leg_back_right", "rotation", 1.0)
    anim.derivar(golpe, "leg_back_left", "paw_back_left", "rotation", -0.6)
    anim.derivar(golpe, "leg_back_left", "paw_back_right", "rotation", -0.6)
    anim.curva(golpe, "leg_front_left", "rotation",
               [(0.0, anim.vetor(x=-12)), (DUR_STRIKE * 0.45, anim.vetor(x=-38)),
                (DUR_STRIKE, anim.vetor(x=-20))])
    anim.derivar(golpe, "leg_front_left", "leg_front_right", "rotation", 1.0)
    anim.derivar(golpe, "leg_front_left", "paw_front_left", "rotation", -0.5)
    anim.derivar(golpe, "leg_front_left", "paw_front_right", "rotation", -0.5)
    anim.curva(golpe, "tail", "rotation",
               [(0.0, anim.vetor(x=32)), (DUR_STRIKE, anim.vetor(x=-18))])
    a.clipe("strike", DUR_STRIKE, golpe)

    # -------------------------------------------------------------- recovery
    # A JANELA EM QUE O JOGADOR PUNE, e num mob de 28 de vida ela e a defesa do
    # jogador inteira. O bicho POUSA: o corpo afunda abaixo do repouso antes de
    # voltar, porque uma aterrissagem sem absorcao le como o bicho tendo
    # flutuado ate o chao.
    #
    # Encurtar este clipe nao mudaria o servidor -- mudaria so a leitura, e o
    # jogador acharia que apanhou sem ter tido janela.
    volta = {}
    anim.curva(volta, "body", "position",
               [(0.0, anim.vetor(y=ALTURA_DO_BOTE * 0.6, z=-3.4)),
                (DUR_RECOVERY * 0.3, anim.vetor(y=-1.4, z=-1.0)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "body", "rotation",
               [(0.0, anim.vetor(x=6)), (DUR_RECOVERY * 0.3, anim.vetor(x=-7)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "neck", "rotation",
               [(0.0, anim.vetor(x=2)), (DUR_RECOVERY * 0.3, anim.vetor(x=11)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "neck", "head", "rotation", 0.4)
    anim.derivar(volta, "head", "antenna_left", "rotation", ARRASTO_DA_ANTENA)
    anim.derivar(volta, "head", "antenna_right", "rotation", ARRASTO_DA_ANTENA)
    anim.curva(volta, "jaw", "rotation",
               [(0.0, anim.vetor(x=4)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "leg_front_left", "rotation",
               [(0.0, anim.vetor(x=-20)), (DUR_RECOVERY * 0.3, anim.vetor(x=18)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "leg_front_left", "leg_front_right", "rotation", 1.0)
    anim.derivar(volta, "leg_front_left", "paw_front_left", "rotation", -0.5)
    anim.derivar(volta, "leg_front_left", "paw_front_right", "rotation", -0.5)
    anim.curva(volta, "leg_back_left", "rotation",
               [(0.0, anim.vetor(x=-14)), (DUR_RECOVERY * 0.3, anim.vetor(x=22)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "leg_back_left", "leg_back_right", "rotation", 1.0)
    anim.derivar(volta, "leg_back_left", "paw_back_left", "rotation", -0.6)
    anim.derivar(volta, "leg_back_left", "paw_back_right", "rotation", -0.6)
    anim.curva(volta, "tail", "rotation",
               [(0.0, anim.vetor(x=-18)), (DUR_RECOVERY * 0.45, anim.vetor(x=9)),
                (DUR_RECOVERY, anim.vetor())])
    a.clipe("recovery", DUR_RECOVERY, volta)

    # --------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece -- e interromper o bote, que e a defesa que este mob
    # ensina, deixa de ter recompensa na tela.
    #
    # O bicho perde o eixo: o quadril gira em Y, a cabeca desvia e uma pata
    # atravessa para nao cair. Um tremor simetrico leria como tique.
    tropeco = {}
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.1, anim.vetor(x=-16, y=15)),
                (0.28, anim.vetor(x=7, y=-7)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "body", "position",
               [(0.0, anim.vetor()), (0.1, anim.vetor(y=-1.0, z=0.8)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "neck", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(x=-22, y=-18)),
                (0.32, anim.vetor(x=9, y=8)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "neck", "head", "rotation", -0.8, eixos=(0, 1))
    anim.derivar(tropeco, "head", "antenna_left", "rotation", -1.1, eixos=(0, 1))
    anim.derivar(tropeco, "head", "antenna_right", "rotation", -1.1, eixos=(0, 1))
    anim.curva(tropeco, "leg_front_left", "rotation",
               [(0.0, anim.vetor()), (0.14, anim.vetor(x=24)),
                (0.34, anim.vetor(x=-8)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "leg_front_left", "leg_front_right", "rotation", -0.6)
    anim.derivar(tropeco, "leg_front_left", "paw_front_left", "rotation", -0.7)
    anim.derivar(tropeco, "leg_front_right", "paw_front_right", "rotation", -0.7)
    anim.curva(tropeco, "leg_back_left", "rotation",
               [(0.0, anim.vetor()), (0.14, anim.vetor(x=-18)),
                (0.34, anim.vetor(x=6)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "leg_back_left", "leg_back_right", "rotation", -0.6)
    anim.derivar(tropeco, "leg_back_left", "paw_back_left", "rotation", -0.7)
    anim.derivar(tropeco, "leg_back_right", "paw_back_right", "rotation", -0.7)
    anim.curva(tropeco, "tail", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(y=-26)),
                (0.34, anim.vetor(y=12)), (DUR_STAGGER, anim.vetor())])
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # ------------------------------------------------------------------ morte
    # hold_on_last_frame: o corpo fica caido ate a entidade sumir. Um clipe de
    # morte que volta ao repouso mostra o bicho DE PE no ultimo quadro antes de
    # desaparecer, e a leitura vira "ele sumiu", nao "ele caiu".
    #
    # O corpo desce 4.5 px enquanto tomba de lado (rotacao Z): sem a descida, o
    # modelo pivota no ar e metade dele atravessa o chao.
    queda = {}
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(x=-11, z=26)),
                (DUR_DEATH, anim.vetor(x=-4, z=84))])
    anim.curva(queda, "body", "position",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(y=-1.2)),
                (DUR_DEATH, anim.vetor(y=-4.5))])
    anim.curva(queda, "neck", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.45, anim.vetor(x=19)),
                (DUR_DEATH, anim.vetor(x=31))])
    anim.derivar(queda, "neck", "head", "rotation", 0.7)
    # A antena cai ALEM da cabeca: fator maior que 1 e o que um apendice sem
    # musculo faz quando o bicho para de segura-lo.
    anim.derivar(queda, "head", "antenna_left", "rotation", -1.3)
    anim.derivar(queda, "head", "antenna_right", "rotation", -1.3)
    anim.curva(queda, "jaw", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=17))])
    anim.curva(queda, "leg_front_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.4, anim.vetor(x=-27)),
                (DUR_DEATH, anim.vetor(x=-44))])
    anim.derivar(queda, "leg_front_left", "leg_front_right", "rotation", 0.7)
    anim.derivar(queda, "leg_front_left", "paw_front_left", "rotation", -0.4)
    anim.derivar(queda, "leg_front_right", "paw_front_right", "rotation", -0.4)
    anim.curva(queda, "leg_back_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.4, anim.vetor(x=21)),
                (DUR_DEATH, anim.vetor(x=39))])
    anim.derivar(queda, "leg_back_left", "leg_back_right", "rotation", 0.7)
    anim.derivar(queda, "leg_back_left", "paw_back_left", "rotation", -0.4)
    anim.derivar(queda, "leg_back_right", "paw_back_right", "rotation", -0.4)
    anim.curva(queda, "tail", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=-29))])
    a.clipe("death", DUR_DEATH, queda)

    return a


# ------------------------------------------ validacao que liga arte e regra

def valida_telegrafo_do_salto(a):
    """O aviso do bote existe na tela, dura o que o servidor gasta, e AGACHA.

    Duas coisas sao cobradas, e as duas fecham a mesma falha muda -- um salto
    que chega sem ter sido anunciado:

    1. DURACAO ISOLADA. `valida_duracao_de_ataque` soma os tres clipes e
       aprovaria um aviso de dois ticks compensado por uma recuperacao longa. O
       aviso e a unica parte que o jogador usa para DECIDIR, e ele precisa durar
       sozinho os ticks que o servidor gasta em WINDUP.
    2. SENTIDO DO MOVIMENTO. O corpo tem de terminar o aviso mais BAIXO do que
       comecou, e o golpe tem de leva-lo mais ALTO do que o agachamento. Sem o
       par, o impulso do servidor continua saindo igual e o bicho atravessa
       quatro blocos sem nada na silhueta dizendo que ia fazer isso -- dano
       certo, cooldown certo, log limpo, e o jogador jurando que o mob teleporta.
    """
    aviso = a.clipes[a.nome_completo("windup")]
    exigido = TICKS_DO_AVISO_DO_BOTE / TICKS_POR_SEGUNDO
    if aviso["animation_length"] + 1e-9 < exigido:
        raise ErroDeArte(
            "o clipe de aviso dura %.2fs e o servidor gasta %.2fs em WINDUP (%d ticks): o bicho "
            "termina o agachamento antes de saltar, e o quadro que o jogador usa para decidir "
            "desaparece no meio do proprio aviso"
            % (aviso["animation_length"], exigido, TICKS_DO_AVISO_DO_BOTE))

    golpe = a.clipes[a.nome_completo("strike")]
    neutro = anim.vetor()
    inicio = anim.valor_em(aviso["bones"].get("body", {}).get("position", {}), 0.0, neutro)
    fim = anim.valor_em(aviso["bones"].get("body", {}).get("position", {}),
                        aviso["animation_length"], neutro)
    if fim[1] >= inicio[1]:
        raise ErroDeArte(
            "o corpo termina o aviso em y=%s e comecou em y=%s: nao ha agachamento nenhum, e o "
            "bote passa a sair sem aviso na tela -- o servidor continua aplicando o impulso, e o "
            "jogador le teleporte" % (fim[1], inicio[1]))

    auge = min(anim.valor_em(golpe["bones"].get("body", {}).get("position", {}), t, neutro)[1]
               for t in (0.0, golpe["animation_length"]))
    topo = max(anim.valor_em(golpe["bones"].get("body", {}).get("position", {}), t, neutro)[1]
               for t in (0.0, golpe["animation_length"] * 0.45, golpe["animation_length"]))
    if topo <= fim[1]:
        raise ErroDeArte(
            "o corpo chega no maximo a y=%s durante o bote e o agachamento terminou em y=%s: o "
            "salto nao sai do chao na tela, e o deslocamento que o servidor aplica vira um "
            "escorregao sem causa visivel" % (topo, fim[1]))
    if auge > fim[1]:
        raise ErroDeArte(
            "o bote comeca em y=%s e o aviso terminou em y=%s: o clipe de golpe nao parte de onde "
            "o agachamento parou, e o bicho salta um quadro antes de ter agachado" % (auge, fim[1]))


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(ataques=ATAQUES,
                                          extras=(valida_telegrafo_do_salto,))
