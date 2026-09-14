"""Clipes do Radio Rat -- a representacao das fases que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, RELATORIO OU STAGGER. O servidor publica a
fase, o cliente escolhe o clipe; se a animacao e a regra discordarem, quem esta
errado e este arquivo.

O QUE OS CLIPES DESTE BICHO TEM DE CONTAR. O Radio Rat nao briga: ele avisa. O
grito e o unico ato dele, e o servidor gasta 20 ticks de windup antes de o
relatorio sair -- uma janela que existe SO para o jogador poder matar o
mensageiro antes. Esses 20 ticks sao gastos aqui levantando a antena e abrindo
as orelhas, porque telegrafo que nao muda a silhueta e telegrafo que ninguem le.
E o ocio foi mantido de proposito MAIS FRACO que o windup: ocio chamativo faz o
jogador ignorar o aviso, e isso nao aparece em teste nenhum.

DOIS ORCAMENTOS SAIDOS DO SERVIDOR, e os dois estao cobrados abaixo:

  * windup + active + recovery -- conferido pela biblioteca contra a SOMA dos
    tres clipes encadeados;
  * a janela de cambaleio -- 20 ticks em que `cambaleando()` e verdadeiro e o
    cliente toca `stagger`. A biblioteca NAO cobra essa; ela e cobrada aqui, em
    `valida_cambaleio_cobre_a_janela_do_servidor`.

Convencao de eixos: y=0 e o chao, -Z e a FRENTE, +X e o lado ESQUERDO. Disso sai
a tabela de consequencia deste bicho:

  rotation x NEGATIVO em `head`/`antenna`  ->  focinho e antena SOBEM (empinar)
  rotation x POSITIVO em `leg_*`           ->  a pata recua (fim do passo)
  rotation y em `tail`                     ->  a cauda varre de um lado ao outro
  rotation z em `ear_*`                    ->  a orelha abre para fora do cranio
  rotation z em `body`                     ->  o bicho tomba de lado (morte)

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/radio_rat/radio_rat_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/radio_rat.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402

MOB = "radio_rat"

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType
# do Java VENCE o JSON. Esta e tambem a UNICA lista de clipes.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o telegrafo termina com a antena ERGUIDA e fica assim
    # ate o servidor trocar de fase. Voltando ao repouso no fim do clipe, o rato
    # desarmaria o grito na tela enquanto o servidor ainda esta em WINDUP -- o
    # jogador leria "passou", baixaria a arma, e o relatorio sairia mesmo assim.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS do servidor, com a origem ao lado.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=20,     # RadioRatTuning.GRITO_WINDUP_TICKS
                       active=4,      # RadioRatTuning.GRITO_ACTIVE_TICKS
                       recovery=16)}  # RadioRatTuning.GRITO_RECOVERY_TICKS

# Ticks em que StaggerState.cambaleando() continua verdadeiro depois de disparar.
# GreedIslandProfiles.radioRatStagger(): new StaggerRules(5.0F, 0.0F, 0.3F, 20).
TICKS_DE_CAMBALEIO = 20

DUR_IDLE = 2.0
DUR_WALK = 0.5
DUR_WINDUP = 1.0     # 20 ticks
DUR_STRIKE = 0.2     # 4 ticks
DUR_RECOVERY = 0.8   # 16 ticks
DUR_STAGGER = 1.0    # 20 ticks -- ver valida_cambaleio_cobre_a_janela_do_servidor
DUR_DEATH = 1.0

# Amplitude da antena no ocio e no windup, em graus. As duas moram juntas porque
# so fazem sentido comparadas: o aviso precisa ser MUITO maior que a respiracao,
# senao o jogador aprende a filtrar os dois como a mesma coisa.
ANTENA_NO_OCIO = 2.0
ANTENA_NO_WINDUP = 46.0


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # --------------------------------------------------------------- ocio
    # Farejar. Amplitudes pequenas e uma orelha girando mais rapido que o corpo:
    # e o que faz um bicho parado parecer vivo sem competir com o telegrafo.
    ocio = {}
    anim.curva(ocio, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 1.5)])
    anim.curva(ocio, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 3.0, fase=0.1)])
    # As orelhas piscam ao DOBRO da frequencia do corpo (periodo = metade da
    # duracao): duas voltas inteiras dentro do clipe, entao o loop fecha.
    anim.curva(ocio, "ear_left", "rotation",
               [(t, anim.vetor(z=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE / 2.0, 6.0)])
    # Espelhada, e nao copiada: as duas orelhas abrem para fora do cranio, e
    # copiar o mesmo sinal nos dois lados da um bicho visivelmente torto -- que
    # so aparece para quem girar a camera em volta dele.
    anim.derivar(ocio, "ear_left", "ear_right", "rotation", -1.0, eixos=(2,))
    anim.curva(ocio, "antenna", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, ANTENA_NO_OCIO, fase=0.25)])
    anim.derivar(ocio, "antenna", "antenna_tip", "rotation", -0.5)
    anim.curva(ocio, "tail", "rotation",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 8.0)])
    a.clipe("idle", DUR_IDLE, ocio)

    # ----------------------------------------------------------- locomocao
    # Passo curto e rapido: meio segundo por ciclo. As patas andam em DIAGONAL
    # (frente-esquerda com tras-direita), que e o que separa um quadrupede
    # correndo de um brinquedo de corda.
    marcha = {}
    anim.curva(marcha, "leg_front_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 26.0)])
    anim.derivar(marcha, "leg_front_left", "leg_back_right", "rotation", 1.0)
    anim.curva(marcha, "leg_front_right", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 26.0, fase=0.5)])
    anim.derivar(marcha, "leg_front_right", "leg_back_left", "rotation", 1.0)
    # O tronco quica DUAS vezes por passada (periodo = metade): um quique por
    # passada le como manco.
    anim.curva(marcha, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 3.0)])
    anim.derivar(marcha, "body", "head", "rotation", -0.6)
    anim.curva(marcha, "antenna", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 5.0, fase=0.15)])
    anim.derivar(marcha, "antenna", "antenna_tip", "rotation", -0.8)
    anim.curva(marcha, "tail", "rotation",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 14.0)])
    a.clipe("walk", DUR_WALK, marcha)

    # -------------------------------------------------------------- windup
    # O AVISO, e e o clipe mais importante deste bicho. Um segundo inteiro em que
    # a silhueta muda de forma: o rato empina nas patas traseiras, as orelhas
    # abrem e a antena sobe quase meio giro. Quem estiver olhando tem esse tempo
    # para matar o mensageiro; quem nao estiver, perde o alvo para o vizinho.
    aviso = {}
    anim.curva(aviso, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.6, anim.vetor(x=-12)),
                (DUR_WINDUP, anim.vetor(x=-18))])
    anim.curva(aviso, "body", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=0.5))])
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.5, anim.vetor(x=-14)),
                (DUR_WINDUP, anim.vetor(x=-26))])
    anim.curva(aviso, "antenna", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.7, anim.vetor(x=-34)),
                (DUR_WINDUP, anim.vetor(x=-ANTENA_NO_WINDUP))])
    anim.derivar(aviso, "antenna", "antenna_tip", "rotation", 0.5)
    anim.curva(aviso, "ear_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.4, anim.vetor(z=18)),
                (DUR_WINDUP, anim.vetor(z=24))])
    anim.derivar(aviso, "ear_left", "ear_right", "rotation", -1.0, eixos=(2,))
    # A cauda desce para contrabalancar o tronco empinado. Sem ela o bicho parece
    # tombar para tras em vez de se erguer.
    anim.curva(aviso, "tail", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=20))])
    anim.curva(aviso, "leg_front_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-40))])
    anim.derivar(aviso, "leg_front_left", "leg_front_right", "rotation", 1.0)
    a.clipe("windup", DUR_WINDUP, aviso)

    # -------------------------------------------------------------- strike
    # O GRITO SAI. Quatro ticks, e e nesta janela que o servidor entrega o
    # relatorio aos vizinhos -- e que a mordida de 2 acerta quem estiver colado.
    # Todos os ossos que o windup moveu continuam escritos aqui: osso que um
    # clipe nao cita volta para o default do modelo, e o rato desempinaria de
    # uma vez so no quadro em que o grito comeca.
    grito = {}
    anim.curva(grito, "body", "rotation",
               [(0.0, anim.vetor(x=-18)), (DUR_STRIKE * 0.4, anim.vetor(x=-24)),
                (DUR_STRIKE, anim.vetor(x=-6))])
    anim.curva(grito, "body", "position",
               [(0.0, anim.vetor(y=0.5)), (DUR_STRIKE * 0.4, anim.vetor(y=0.7)),
                (DUR_STRIKE, anim.vetor(y=0.5))])
    anim.curva(grito, "head", "rotation",
               [(0.0, anim.vetor(x=-26)), (DUR_STRIKE * 0.3, anim.vetor(x=-36)),
                (DUR_STRIKE, anim.vetor(x=-20))])
    # A antena chicoteia: sobe mais um pouco e volta. E o quadro em que a
    # travessa se mexe SOZINHA, e e por isso que ela e osso proprio.
    anim.curva(grito, "antenna", "rotation",
               [(0.0, anim.vetor(x=-ANTENA_NO_WINDUP)),
                (DUR_STRIKE * 0.3, anim.vetor(x=-58)),
                (DUR_STRIKE, anim.vetor(x=-40))])
    anim.derivar(grito, "antenna", "antenna_tip", "rotation", -1.4)
    anim.curva(grito, "ear_left", "rotation",
               [(0.0, anim.vetor(z=24)), (DUR_STRIKE * 0.3, anim.vetor(z=32)),
                (DUR_STRIKE, anim.vetor(z=22))])
    anim.derivar(grito, "ear_left", "ear_right", "rotation", -1.0, eixos=(2,))
    anim.curva(grito, "tail", "rotation",
               [(0.0, anim.vetor(x=20)), (DUR_STRIKE, anim.vetor(x=14))])
    anim.curva(grito, "leg_front_left", "rotation",
               [(0.0, anim.vetor(x=-40)), (DUR_STRIKE, anim.vetor(x=-28))])
    anim.derivar(grito, "leg_front_left", "leg_front_right", "rotation", 1.0)
    a.clipe("strike", DUR_STRIKE, grito)

    # ------------------------------------------------------------ recovery
    # A JANELA DE RESPOSTA, e ela e longa de proposito: 16 ticks em que o rato
    # esta de volta ao chao, sem nada armado, e quem chegou tarde ainda pune o
    # mensageiro antes do proximo grito. Encurtar este clipe nao mudaria o
    # servidor -- mudaria so a leitura, e o jogador acharia que foi punido sem
    # janela nenhuma.
    volta = {}
    anim.curva(volta, "body", "rotation",
               [(0.0, anim.vetor(x=-6)), (DUR_RECOVERY * 0.4, anim.vetor(x=4)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "body", "position",
               [(0.0, anim.vetor(y=0.5)), (DUR_RECOVERY * 0.3, anim.vetor()),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "head", "rotation",
               [(0.0, anim.vetor(x=-20)), (DUR_RECOVERY * 0.5, anim.vetor(x=6)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "antenna", "rotation",
               [(0.0, anim.vetor(x=-40)), (DUR_RECOVERY * 0.5, anim.vetor(x=8)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "antenna", "antenna_tip", "rotation", -0.6)
    anim.curva(volta, "ear_left", "rotation",
               [(0.0, anim.vetor(z=22)), (DUR_RECOVERY * 0.6, anim.vetor(z=4)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "ear_left", "ear_right", "rotation", -1.0, eixos=(2,))
    anim.curva(volta, "tail", "rotation",
               [(0.0, anim.vetor(x=14)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "leg_front_left", "rotation",
               [(0.0, anim.vetor(x=-28)), (DUR_RECOVERY * 0.4, anim.vetor(x=-6)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "leg_front_left", "leg_front_right", "rotation", 1.0)
    a.clipe("recovery", DUR_RECOVERY, volta)

    # ------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL. Ela e a resposta que este bicho ensina:
    # um golpe solido no meio do windup corta o relatorio antes de ele sair. Se o
    # cambaleio nao aparecesse, o jogador nao teria como aprender que interromper
    # funciona -- e o stagger viraria um numero que so o servidor conhece.
    #
    # A ANTENA E O QUE MAIS SE MEXE AQUI, de proposito: ela e a peca que estava
    # subindo, e ve-la desabar e a leitura de "o grito nao saiu".
    tropeco = {}
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.1, anim.vetor(x=22)), (0.3, anim.vetor(x=-8)),
                (0.6, anim.vetor(x=3)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "body", "position",
               [(0.0, anim.vetor()), (0.12, anim.vetor(z=0.8)), (0.6, anim.vetor()),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "head", "rotation",
               [(0.0, anim.vetor()), (0.14, anim.vetor(x=26)), (0.36, anim.vetor(x=-6)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "antenna", "rotation",
               [(0.0, anim.vetor()), (0.1, anim.vetor(x=34)), (0.3, anim.vetor(x=-10)),
                (0.7, anim.vetor(x=4)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "antenna", "antenna_tip", "rotation", -1.2)
    # Orelhas COLADAS para tras: e o unico gesto do repertorio em que elas
    # fecham, e por isso ele nao se confunde com nenhum outro clipe de longe.
    anim.curva(tropeco, "ear_left", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(z=-20)), (0.5, anim.vetor(z=-6)),
                (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "ear_left", "ear_right", "rotation", -1.0, eixos=(2,))
    anim.curva(tropeco, "tail", "rotation",
               [(0.0, anim.vetor()), (0.15, anim.vetor(x=-18)), (0.6, anim.vetor()),
                (DUR_STAGGER, anim.vetor())])
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # --------------------------------------------------------------- morte
    # hold_on_last_frame: o corpo fica tombado ate a entidade sumir. Um clipe de
    # morte que volta ao repouso mostra o rato de pe no ultimo quadro antes de
    # desaparecer, e a leitura vira "ele sumiu", nao "ele caiu".
    queda = {}
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(z=34)),
                (DUR_DEATH, anim.vetor(z=92))])
    anim.curva(queda, "body", "position",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(y=-1))])
    anim.curva(queda, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=18))])
    # A antena cai por ultimo e fica caida: e a confirmacao visual de que este
    # mensageiro nao denuncia mais ninguem.
    anim.curva(queda, "antenna", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=16)),
                (DUR_DEATH, anim.vetor(x=44))])
    anim.derivar(queda, "antenna", "antenna_tip", "rotation", -0.5)
    anim.curva(queda, "ear_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(z=-26))])
    anim.derivar(queda, "ear_left", "ear_right", "rotation", -1.0, eixos=(2,))
    anim.curva(queda, "leg_front_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=-30))])
    anim.derivar(queda, "leg_front_left", "leg_front_right", "rotation", 1.0)
    anim.derivar(queda, "leg_front_left", "leg_back_left", "rotation", 0.7)
    anim.derivar(queda, "leg_front_left", "leg_back_right", "rotation", 0.7)
    anim.curva(queda, "tail", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=-22))])
    a.clipe("death", DUR_DEATH, queda)

    return a


def valida_cambaleio_cobre_a_janela_do_servidor(a):
    """A LIGACAO ENTRE A ARTE E A REGRA, do lado do stagger.

    `StaggerState.cambaleando()` fica verdadeiro por 20 ticks depois de disparar,
    e durante esses 20 ticks o servidor publica CAMBALEANDO e o cliente toca este
    clipe. Se o clipe for mais curto, o rato termina a animacao e fica parado no
    ultimo quadro -- ou volta a pose neutra -- enquanto ainda esta interrompido.

    Nada acusa: a interrupcao continua valendo, o grito continua cortado, o log
    fica limpo. O que se perde e o jogador aprender que interromper FUNCIONA, e
    esse aprendizado e o bicho inteiro. A biblioteca cobra esta conta para os
    clipes de ATAQUE e nao para o de stagger; por isso ela mora aqui.
    """
    clipe = a.clipes[a.nome_completo("stagger")]
    duracao = clipe["animation_length"]
    exigido = TICKS_DE_CAMBALEIO / anim.TICKS_POR_SEGUNDO
    if duracao + 1e-9 < exigido:
        raise anim.ErroDeArte(
            "o clipe de stagger dura %.2fs e o servidor mantem o cambaleio por %.2fs (%d ticks, "
            "StaggerRules do radioRatStagger): o rato para de cambalear na tela enquanto ainda "
            "esta interrompido, e o jogador deixa de ver que interromper e a resposta"
            % (duracao, exigido, TICKS_DE_CAMBALEIO))


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(
        ataques=ATAQUES, extras=(valida_cambaleio_cobre_a_janela_do_servidor,))
