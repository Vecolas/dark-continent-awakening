"""Clipes do King White Stag Beetle -- a representacao das posturas que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, STAGGER, VIRAGEM OU RECOMPENSA. O servidor
publica a fase do ataque, o cambaleio e o contador da janela de costas; o cliente
escolhe o clipe correspondente. Se a animacao e a regra discordarem, quem esta
errado e este arquivo.

O CLIPE `capsized` NAO E DECORACAO. Ele e a outra metade de uma regra do
servidor. KingWhiteStagBeetleTuning diz que, de costas, o ventre paga acima de
0.54 da altura da caixa de colisao -- e o ventre DESENHADO so chega la porque
este clipe gira `body` em 180 graus. Um `capsized` que girasse 90, ou que
esquecesse o giro, deixaria a regra pagando no alto enquanto o desenho continua
mostrando a barriga no chao: multiplicador certo, log limpo, e o jogador mirando
onde a tela manda e levando dano comum. `valida_giro_do_tombo` cobra os dois
lados desse acordo.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com a constante de origem ao lado. A
regua da biblioteca reprova se a soma dos tres clipes encadeados for menor que
windup + active + recovery: um clipe curto demais faz o besouro relaxar no meio
da investida que ainda vai acertar -- dano certo, cooldown certo, log limpo, e a
unica leitura que o jogador tem quebrada.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/king_white_stag_beetle/king_white_stag_beetle_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/king_white_stag_beetle.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402

MOB = "king_white_stag_beetle"

# Graus de giro do tombo. NAO e botao de balanceamento: 180 e a definicao de
# "de costas", e e o numero de que a altura espelhada do ventre depende.
GIRO_DO_TOMBO = 180.0

# Altura relativa a partir da qual o servidor paga ventre com o bicho virado.
# KingWhiteStagBeetleTuning.ALTURA_MINIMA_DO_VENTRE_DE_COSTAS = 0.54D.
ALTURA_MINIMA_DO_VENTRE_DE_COSTAS = 0.54
# O literal de EnemyEntityTypes .sized(1.6F, 1.4F) -- a altura, em px.
HITBOX_ALTURA_PX = 1.4 * 16.0

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType do
# Java VENCE o JSON, e um atalho de repeticao do lado de la transformaria este
# dicionario em documentacao que discorda do comportamento.
#
# Esta e tambem a UNICA lista de clipes. Uma lista separada seria a mesma
# informacao escrita duas vezes, e alguem acrescentaria um clipe numa so.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o aviso termina com o bicho EMPINADO e fica assim. E a
    # pose que o servidor chama de WINDUP, e e nela que o ventre paga e que o
    # tranco derruba. Voltando ao repouso no fim do clipe, o besouro desarmaria a
    # investida na tela enquanto o servidor ainda esta em WINDUP -- o jogador
    # leria "passou", pararia de bater no ventre exposto e levaria a chifrada.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    # capsized repete: a janela dura 60 ticks e o clipe dura 24. Sem repetir, o
    # besouro ficaria imovel nos ultimos 36 ticks e a janela pareceria ter
    # acabado antes da hora.
    "capsized": True,
    "righting": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de KingWhiteStagBeetleTuning, com a constante ao lado.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=26,     # WINDUP_DA_INVESTIDA
                       active=8,      # JANELA_DA_INVESTIDA
                       recovery=22)}  # RECUPERACAO_DA_INVESTIDA

DUR_IDLE = 3.0
DUR_WALK = 0.9
DUR_WINDUP = 1.3     # 26 ticks
DUR_STRIKE = 0.4     # 8 ticks
DUR_RECOVERY = 1.1   # 22 ticks
DUR_STAGGER = 0.6
DUR_CAPSIZED = 1.2
# 0.9 s sao os 18 ticks de KingWhiteStagBeetleTuning.TICKS_PARA_LEVANTAR. Os dois
# numeros sao o MESMO instante visto dos dois lados: o servidor troca a leitura
# de "de costas" para "levantando" quando faltam 18, e o clipe leva exatamente
# esse tempo para pousar o bicho de pe. Um clipe mais longo terminaria com o
# besouro ainda de lado no tick em que ele volta a atacar.
DUR_RIGHTING = 0.9
DUR_DEATH = 1.4

PERNAS_TRIPE_A = ("leg_front_left", "leg_mid_right", "leg_rear_left")
PERNAS_TRIPE_B = ("leg_front_right", "leg_mid_left", "leg_rear_right")
PERNAS = PERNAS_TRIPE_A + PERNAS_TRIPE_B


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # -------------------------------------------------------------- ocio
    # Um besouro parado nao respira: ele VIBRA. O corpo sobe e desce um terco de
    # pixel e as antenas-chifre abrem e fecham devagar. A amplitude e minuscula
    # porque o ocio nao pode competir com o aviso da investida -- ocio chamativo
    # faz o jogador ignorar o telegrafo, e o telegrafo e a janela do encontro.
    ocio = {}
    anim.curva(ocio, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 0.35)])
    anim.curva(ocio, "carapaca", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 1.2, fase=0.12)])
    anim.curva(ocio, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.0, fase=0.2)])
    # A pinca abre e fecha em torno do eixo Y. Os dois chifres sao espelhados: o
    # direito e DERIVADO do esquerdo, e nao escrito a mao. Escrito a mao ele
    # ficaria com a fase certa hoje e errada na primeira correcao, e a correcao
    # nao da erro -- da uma pinca torta que so aparece de frente.
    anim.curva(ocio, "horn_left", "rotation",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, 1.5, 3.5)])
    anim.derivar(ocio, "horn_left", "horn_right", "rotation", -1.0, eixos=(1,))
    for i, perna in enumerate(PERNAS):
        anim.curva(ocio, perna, "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(DUR_IDLE, DUR_IDLE, 1.5, fase=0.12 * i)])
    a.clipe("idle", DUR_IDLE, ocio)

    # ----------------------------------------------------------- locomocao
    # MARCHA DE TRIPE, que e como inseto anda: tres pernas no chao e tres no ar,
    # alternando. Escrever as seis em fase daria um bicho saltitando com as seis
    # juntas -- nao da erro, e le como brinquedo de corda em vez de besouro.
    marcha = {}
    anim.curva(marcha, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 0.4)])
    anim.curva(marcha, "body", "rotation",
               [(t, anim.vetor(z=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 2.5)])
    anim.curva(marcha, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 3.0, fase=0.25)])
    for perna in PERNAS_TRIPE_A:
        anim.curva(marcha, perna, "rotation",
                   [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 17.0)])
    for perna in PERNAS_TRIPE_B:
        anim.curva(marcha, perna, "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(DUR_WALK, DUR_WALK, 17.0, fase=0.5)])
    a.clipe("walk", DUR_WALK, marcha)

    # -------------------------------------------------------------- windup
    # O AVISO, e ele e A JANELA. Um segundo e trezentos milissegundos empinando: o
    # corpo levanta a frente, as pernas dianteiras saem do chao, a casca abre um
    # dedo e a pinca escancara. E o unico momento em que a silhueta deixa de ser
    # "casca" -- e e por isso que ele e legivel de longe.
    #
    # O sinal de x NEGATIVO e o que levanta a frente. A convencao vem do boneco de
    # treino, onde `body` rotation x = -22 e descrito como o tronco RECUANDO: no
    # formato Bedrock, x negativo joga o topo para tras, que num bicho horizontal e
    # exatamente empinar. Trocar o sinal aqui nao da erro -- da um besouro que
    # enfia a cara no chao para avisar que vai investir.
    aviso = {}
    anim.curva(aviso, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.55, anim.vetor(x=-34)),
                (DUR_WINDUP, anim.vetor(x=-46))])
    anim.curva(aviso, "body", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=1.0, z=1.5))])
    anim.curva(aviso, "carapaca", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-7))])
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-12))])
    anim.curva(aviso, "horn_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.5, anim.vetor(y=14)),
                (DUR_WINDUP, anim.vetor(y=22))])
    anim.derivar(aviso, "horn_left", "horn_right", "rotation", -1.0, eixos=(1,))
    # As dianteiras saem do chao, as do meio ficam a meio caminho e as TRASEIRAS
    # ficam plantadas: sao elas que seguram o bicho em pe. Levantar as seis
    # deixaria o besouro flutuando na pose que o encontro inteiro usa.
    for perna in ("leg_front_left", "leg_front_right"):
        anim.curva(aviso, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-38))])
    for perna in ("leg_mid_left", "leg_mid_right"):
        anim.curva(aviso, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-16))])
    for perna in ("leg_rear_left", "leg_rear_right"):
        anim.curva(aviso, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=8))])
    a.clipe("windup", DUR_WINDUP, aviso)

    # -------------------------------------------------------------- strike
    # A JANELA QUE MACHUCA, e ela e curta. O corpo desaba para a frente e a pinca
    # FECHA -- o fechar e o que separa visualmente o golpe do aviso, e e o quadro
    # que o jogador aprende a reconhecer. O deslocamento em z e pequeno de
    # proposito: quem avanca de verdade e a entidade, com o arranco que o servidor
    # aplica. Repetir o avanco aqui somaria dois movimentos e o modelo sairia da
    # propria caixa de colisao.
    golpe = {}
    anim.curva(golpe, "body", "rotation",
               [(0.0, anim.vetor(x=-46)), (DUR_STRIKE * 0.5, anim.vetor(x=16)),
                (DUR_STRIKE, anim.vetor(x=8))])
    anim.curva(golpe, "body", "position",
               [(0.0, anim.vetor(y=1.0, z=1.5)), (DUR_STRIKE * 0.5, anim.vetor(z=-1.5)),
                (DUR_STRIKE, anim.vetor(z=-0.8))])
    anim.curva(golpe, "carapaca", "rotation",
               [(0.0, anim.vetor(x=-7)), (DUR_STRIKE, anim.vetor(x=2))])
    anim.curva(golpe, "head", "rotation",
               [(0.0, anim.vetor(x=-12)), (DUR_STRIKE * 0.5, anim.vetor(x=10)),
                (DUR_STRIKE, anim.vetor(x=4))])
    anim.curva(golpe, "horn_left", "rotation",
               [(0.0, anim.vetor(y=22)), (DUR_STRIKE * 0.45, anim.vetor(y=-12)),
                (DUR_STRIKE, anim.vetor(y=-6))])
    anim.derivar(golpe, "horn_left", "horn_right", "rotation", -1.0, eixos=(1,))
    for perna in PERNAS:
        anim.curva(golpe, perna, "rotation",
                   [(0.0, anim.vetor(x=-38 if perna.startswith("leg_front") else 0)),
                    (DUR_STRIKE, anim.vetor(x=14))])
    a.clipe("strike", DUR_STRIKE, golpe)

    # ------------------------------------------------------------ recovery
    # A JANELA DE RESPOSTA. Ela e longa de proposito: e o tempo em que o jogador
    # pune sem risco. Encurta-la aqui nao mudaria o servidor -- mudaria so a
    # leitura, e o jogador acharia que foi punido sem janela.
    volta = {}
    anim.curva(volta, "body", "rotation",
               [(0.0, anim.vetor(x=8)), (DUR_RECOVERY * 0.45, anim.vetor(x=-5)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "body", "position",
               [(0.0, anim.vetor(z=-0.8)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "carapaca", "rotation",
               [(0.0, anim.vetor(x=2)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "head", "rotation",
               [(0.0, anim.vetor(x=4)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "horn_left", "rotation",
               [(0.0, anim.vetor(y=-6)), (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "horn_left", "horn_right", "rotation", -1.0, eixos=(1,))
    for perna in PERNAS:
        anim.curva(volta, perna, "rotation",
                   [(0.0, anim.vetor(x=14)), (DUR_RECOVERY * 0.5, anim.vetor(x=-4)),
                    (DUR_RECOVERY, anim.vetor())])
    a.clipe("recovery", DUR_RECOVERY, volta)

    # ------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece. Aqui ela tambem precisa ser DIFERENTE do tombo: o mesmo
    # golpe que interrompe pode derrubar, e se as duas poses se parecessem o
    # jogador nao saberia qual das duas coisas ele conseguiu.
    tropeco = {}
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(z=-16)), (0.3, anim.vetor(z=10)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "head", "rotation",
               [(0.0, anim.vetor()), (0.16, anim.vetor(z=-20)), (0.34, anim.vetor(z=12)),
                (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "body", "carapaca", "rotation", -0.5, eixos=(2,))
    for i, perna in enumerate(PERNAS):
        anim.curva(tropeco, perna, "rotation",
                   [(0.0, anim.vetor()), (0.14 + 0.02 * i, anim.vetor(x=-22)),
                    (DUR_STAGGER, anim.vetor())])
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # -------------------------------------------------------------- tombado
    # DE COSTAS. O giro de 180 graus em `body` e a peca que a regra do servidor
    # depende -- ver valida_giro_do_tombo. Ele fica FIXO nas duas pontas do clipe
    # porque o clipe repete: uma ponta em 180 e a outra em qualquer outro valor
    # daria um tranco por volta, e o portao de loop reprovaria.
    #
    # O que se mexe sao as pernas, remando no ar, e o ventre pulsando. Sem esse
    # movimento a pose leria como "morto" -- e morto e outra coisa, com outro
    # clipe e outra consequencia.
    tombado = {}
    anim.curva(tombado, "body", "rotation",
               [(0.0, anim.vetor(z=GIRO_DO_TOMBO)),
                (DUR_CAPSIZED / 2.0, anim.vetor(z=GIRO_DO_TOMBO)),
                (DUR_CAPSIZED, anim.vetor(z=GIRO_DO_TOMBO))])
    anim.curva(tombado, "ventre", "position",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_CAPSIZED, DUR_CAPSIZED, 0.4)])
    anim.curva(tombado, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_CAPSIZED, DUR_CAPSIZED / 2.0, 7.0)])
    anim.curva(tombado, "horn_left", "rotation",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_CAPSIZED, DUR_CAPSIZED / 2.0, 12.0)])
    anim.derivar(tombado, "horn_left", "horn_right", "rotation", -1.0, eixos=(1,))
    for i, perna in enumerate(PERNAS):
        anim.curva(tombado, perna, "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(DUR_CAPSIZED, DUR_CAPSIZED / 3.0, 26.0,
                                    fase=0.16 * i)])
    a.clipe("capsized", DUR_CAPSIZED, tombado)

    # ------------------------------------------------------------ levantando
    # O AVISO DE QUE A JANELA ACABA. Ele dura os mesmos 18 ticks que o servidor
    # reserva, e TERMINA EM ZERO -- ver valida_giro_do_tombo. Terminar em qualquer
    # outro angulo deixaria o besouro de lado no tick em que ele volta a atacar:
    # o servidor o trataria como de pe, a investida sairia, e o modelo estaria
    # deitado. Nenhuma excecao, e um bicho visivelmente quebrado.
    endireitar = {}
    anim.curva(endireitar, "body", "rotation",
               [(0.0, anim.vetor(z=GIRO_DO_TOMBO)),
                (DUR_RIGHTING * 0.25, anim.vetor(z=GIRO_DO_TOMBO + 24)),
                (DUR_RIGHTING * 0.5, anim.vetor(z=GIRO_DO_TOMBO - 36)),
                (DUR_RIGHTING * 0.8, anim.vetor(z=38)),
                (DUR_RIGHTING, anim.vetor())])
    anim.curva(endireitar, "head", "rotation",
               [(0.0, anim.vetor(x=6)), (DUR_RIGHTING * 0.6, anim.vetor(x=-14)),
                (DUR_RIGHTING, anim.vetor())])
    for i, perna in enumerate(PERNAS):
        anim.curva(endireitar, perna, "rotation",
                   [(0.0, anim.vetor(x=26)),
                    (DUR_RIGHTING * (0.3 + 0.05 * i), anim.vetor(x=-30)),
                    (DUR_RIGHTING, anim.vetor())])
    a.clipe("righting", DUR_RIGHTING, endireitar)

    # --------------------------------------------------------------- morte
    # hold_on_last_frame: ele termina DE COSTAS, com as pernas recolhidas -- a
    # pose de besouro morto que todo mundo reconhece. Um clipe de morte que volta
    # ao repouso mostraria o bicho de pe no ultimo quadro antes de sumir, e a
    # leitura viraria "ele sumiu" em vez de "ele morreu".
    #
    # Ele termina no MESMO angulo do tombo, e as pernas e que separam os dois: de
    # costas vivo elas remam, morto elas ficam dobradas para dentro. Se as duas
    # poses fossem identicas, o jogador pararia de bater num bicho vivo.
    queda = {}
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(z=54)),
                (DUR_DEATH * 0.7, anim.vetor(z=142)),
                (DUR_DEATH, anim.vetor(z=GIRO_DO_TOMBO))])
    anim.curva(queda, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=18))])
    anim.curva(queda, "horn_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(y=-16))])
    anim.derivar(queda, "horn_left", "horn_right", "rotation", -1.0, eixos=(1,))
    for i, perna in enumerate(PERNAS):
        anim.curva(queda, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_DEATH * (0.4 + 0.04 * i), anim.vetor(x=-18)),
                    (DUR_DEATH, anim.vetor(x=52))])
    a.clipe("death", DUR_DEATH, queda)

    return a


def valida_giro_do_tombo(clipes_do_mob):
    """O tombo tem de girar 180 graus, e o levantar tem de desfazer o giro.

    ESTA REGUA E O CONTRATO ENTRE A ARTE E A REGRA DO SERVIDOR, e as duas metades
    dela pegam defeitos que nao levantam excecao nenhuma:

      * `KingWhiteStagBeetleTuning.ALTURA_MINIMA_DO_VENTRE_DE_COSTAS` paga
        multiplicador acima de 0.54 da altura da caixa de colisao. O ventre
        DESENHADO so chega la depois de girar 180 graus em torno do pivo de
        `body`. Um clipe que girasse 90, ou 175, ou que esquecesse o giro, deixaria
        a regra pagando no alto enquanto a barriga continua no chao: o jogador
        mira onde a tela manda e leva dano comum, e nao ha nada para procurar;

      * `righting` termina no tick em que o servidor devolve o bicho para de pe.
        Terminar em qualquer angulo que nao seja zero deixaria o besouro deitado
        enquanto o servidor ja o trata como em pe -- a investida sairia com o
        modelo de lado, e de novo sem uma linha de log.

    A altura espelhada tambem e recalculada aqui, a partir do geo lido do disco,
    porque o numero que interessa nao e "girou 180": e "girou 180 EM TORNO DO
    PIVO CERTO". O gerador de geometria cobra a mesma coisa do outro lado; sao as
    duas pontas de um portao que morde dos dois lados.
    """
    geometria = clipes_do_mob.geometria
    tombo = clipes_do_mob.clipes[clipes_do_mob.nome_completo("capsized")]
    quadros = tombo["bones"].get("body", {}).get("rotation", {})
    if not quadros:
        raise anim.ErroDeArte(
            "o clipe 'capsized' nao gira 'body': o servidor publica a janela de costas, paga "
            "multiplicador no alto da caixa de colisao, e a tela mostra o besouro de pe -- "
            "multiplicador certo, log limpo, jogador mirando no lugar errado")
    for chave, valor in quadros.items():
        if abs(valor[2]) != GIRO_DO_TOMBO:
            raise anim.ErroDeArte(
                "'capsized' tem body.rotation z=%s em t=%s e o tombo e de %s graus: girado pela "
                "metade, o ventre desenhado nao sobe ate onde o servidor paga"
                % (valor[2], chave, GIRO_DO_TOMBO))

    # O ventre espelhado tem de cair acima do limiar -- a mesma conta do geo,
    # feita de novo a partir do arquivo escrito. Se o geo mudar e o clipe nao,
    # esta e a regua que acusa.
    pivo_y = anim.pivot_de(geometria, "body")[1]
    ventre_y0, ventre_y1 = anim.faixa_de(geometria, ("ventre",), 1)
    virado_y0 = 2 * pivo_y - ventre_y1
    limite = ALTURA_MINIMA_DO_VENTRE_DE_COSTAS * HITBOX_ALTURA_PX
    if virado_y0 < limite:
        raise anim.ErroDeArte(
            "girado em torno do pivo y=%s, o ventre comeca em y=%.1f px e o servidor so paga "
            "acima de %.1f px: o giro esta certo e o pivo nao, e o efeito e o mesmo -- um ventre "
            "exposto que a regra nao reconhece" % (pivo_y, virado_y0, limite))

    levantar = clipes_do_mob.clipes[clipes_do_mob.nome_completo("righting")]
    fim = levantar["animation_length"]
    pose = anim.valor_em(levantar["bones"].get("body", {}).get("rotation", {}), fim,
                         anim.vetor())
    if pose != anim.vetor():
        raise anim.ErroDeArte(
            "'righting' termina com body.rotation em %s e nao no repouso: no tick seguinte o "
            "servidor devolve o besouro para de pe e manda ele investir, e o modelo sai deitado"
            % pose)


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(ataques=ATAQUES, extras=(valida_giro_do_tombo,))
