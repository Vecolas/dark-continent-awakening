"""Clipes do Scorpion Leader -- as posturas que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, VENENO, STAGGER OU RECARGA. O servidor publica
a fase do ataque, o cambaleio e QUAL dos dois ataques esta em curso; o cliente
escolhe o clipe correspondente. Se a animacao e a regra discordarem, quem esta
errado e este arquivo.

ESTE MOB TEM DOIS ATAQUES, E ELES PRECISAM SER DOIS CLIPES DIFERENTES.

  * a PINCA e o golpe comum. Telegrafo curto (12 ticks), alcance curto, e ela
    NAO envenena;
  * o FERRAO e o golpe que envenena. Telegrafo longo (28 ticks), alcance maior, e
    ele cobra um preco que so aparece depois -- ScorpionLeaderTuning aplica
    MobEffects.POISON com duracao acumulavel ate um teto.

Se os dois usassem o mesmo par de clipes, o jogador veria o MESMO aviso para dois
golpes com consequencias diferentes. Ele nao aprenderia o que evitar, tomaria
veneno achando que tomou o golpe comum, veria 12 de dano na tela e culparia o
balanceamento. Isso nao levanta excecao em lugar nenhum: as fases estao certas, o
dano esta certo e o log esta limpo. `valida_o_ferrao_se_anuncia` cobra as duas
metades desse acordo -- que o aviso do ferrao mexe a cauda e o da pinca nao, e que
o chicote de fato leva a ponta para a FRENTE, que e onde a caixa de dano mora.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com a constante de origem ao lado. A
regua da biblioteca reprova se a soma dos tres clipes encadeados for menor que
windup + active + recovery: um clipe curto demais faz a formiga relaxar no meio do
golpe que ainda vai acertar -- dano certo, cooldown certo, log limpo, e a unica
leitura que o jogador tem quebrada.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/scorpion_leader/scorpion_leader_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/scorpion_leader.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402

MOB = "scorpion_leader"

# Graus de rotacao em X que a base da cauda tem de alcancar no clipe do ferrao.
#
# NAO e botao de balanceamento: 90 graus e a definicao de "a ponta cruzou para a
# frente". Com x positivo o topo do osso vai para -Z, que e a FRENTE da geometria
# -- a mesma frente que a caixa de dano do servidor cobre (em coordenada de
# mundo ela e +Z, porque com yaw 0 o olhar vanilla aponta para +Z).
#
# Abaixo de 90 o ferrao para em cima do dorso, e o jogador ve um golpe que nao
# chega nele levando veneno assim mesmo.
GRAUS_MINIMOS_DO_CHICOTE = 90.0

# Os quatro elos, do corpo ate a ponta. Eles sao citados juntos porque o
# telegrafo do ferrao so existe se TODOS se moverem: um elo parado no meio da
# cadeia quebra o chicote em duas metades que nao se explicam.
CAUDA = ("tail_base", "tail_mid", "tail_tip", "stinger")

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType do
# Java VENCE o JSON, e um atalho de repeticao do lado de la transformaria este
# dicionario em documentacao que discorda do comportamento.
#
# Esta e tambem a UNICA lista de clipes. Uma lista separada seria a mesma
# informacao escrita duas vezes, e alguem acrescentaria um clipe numa so.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame nos dois avisos: o telegrafo termina na pose ARMADA e
    # fica nela. Voltando ao repouso no fim do clipe, a formiga desarmaria na tela
    # enquanto o servidor ainda esta em WINDUP -- o jogador leria "passou", pararia
    # de recuar, e levaria o golpe que ele tinha acabado de ler como cancelado.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "sting_windup": "hold_on_last_frame",
    "sting": False,
    "sting_recovery": False,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de ScorpionLeaderTuning, com a constante ao lado.
ATAQUES = {
    ("windup", "strike", "recovery"):
        anim.Ataque(windup=12,      # WINDUP_DA_PINCA
                    active=5,       # JANELA_DA_PINCA
                    recovery=14),   # RECUPERACAO_DA_PINCA
    ("sting_windup", "sting", "sting_recovery"):
        anim.Ataque(windup=28,      # WINDUP_DO_FERRAO
                    active=4,       # JANELA_DO_FERRAO
                    recovery=20),   # RECUPERACAO_DO_FERRAO
}

DUR_IDLE = 3.0
DUR_WALK = 0.8
DUR_WINDUP = 0.6          # 12 ticks
DUR_STRIKE = 0.3          # 5 ticks, com folga
DUR_RECOVERY = 0.75       # 14 ticks, com folga
DUR_STING_WINDUP = 1.4    # 28 ticks
DUR_STING = 0.3           # 4 ticks, com folga
DUR_STING_RECOVERY = 1.0  # 20 ticks
DUR_STAGGER = 0.6
DUR_DEATH = 1.4

# Marcha de OITO pernas em dois grupos alternados. Escrever as oito em fase daria
# um bicho saltitando com o corpo inteiro -- nao da erro, e le como brinquedo de
# corda em vez de artropode.
PERNAS_A = ("leg_1_left", "leg_3_left", "leg_2_right", "leg_4_right")
PERNAS_B = ("leg_2_left", "leg_4_left", "leg_1_right", "leg_3_right")
PERNAS = PERNAS_A + PERNAS_B

# A pose ARMADA da cauda: ela recua sobre o dorso antes de chicotear. O sinal
# NEGATIVO em x joga o topo do osso para +Z, ou seja, para TRAS -- e e esse recuo
# que da curso ao golpe. Sem ele o ferrao sairia do repouso direto para a frente,
# e o aviso de 28 ticks nao teria nada para mostrar durante 28 ticks.
ARMADO = {"tail_base": -30, "tail_mid": -18, "tail_tip": -12, "stinger": -8}
# A pose do CHICOTE ja desfechado. Os elos passam de 90 graus somados, e o portao
# cobra isso na base -- ver valida_o_ferrao_se_anuncia.
CHICOTE = {"tail_base": 104, "tail_mid": 26, "tail_tip": 18, "stinger": 22}


def _pernas(bones, amplitude, fase_b=0.5, duracao=None, periodo=None):
    """Marcha alternada nos dois grupos. Usada no ocio e na caminhada."""
    duracao = duracao if duracao is not None else DUR_WALK
    periodo = periodo if periodo is not None else duracao
    for grupo, fase in ((PERNAS_A, 0.0), (PERNAS_B, fase_b)):
        for perna in grupo:
            anim.curva(bones, perna, "rotation",
                       [(t, anim.vetor(x=v)) for t, v in
                        anim.cossenoide(duracao, periodo, amplitude, fase=fase)])
    return bones


def _pose_da_cauda(bones, tabela, t):
    for osso, graus in tabela.items():
        anim.curva(bones, osso, "rotation", [(t, anim.vetor(x=graus))])
    return bones


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # --------------------------------------------------------------- ocio
    # Uma formiga blindada parada nao respira: ela ASSENTA. O corpo sobe e desce
    # meio pixel e a cauda oscila devagar, mantida no alto. A amplitude da cauda e
    # pequena de proposito -- ocio chamativo na cauda competiria com o aviso do
    # ferrao, e o aviso e a unica defesa que o jogador tem contra o veneno.
    ocio = {}
    anim.curva(ocio, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 0.4)])
    anim.curva(ocio, "head", "rotation",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 5.0, fase=0.15)])
    anim.curva(ocio, "tail_base", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.5, fase=0.1)])
    anim.derivar(ocio, "tail_base", "tail_mid", "rotation", 0.8)
    anim.derivar(ocio, "tail_base", "tail_tip", "rotation", -0.6)
    anim.derivar(ocio, "tail_base", "stinger", "rotation", -0.4)
    # As pincas abrem e fecham em torno do eixo Y, devagar. A direita e DERIVADA
    # da esquerda: escrita a mao ela ficaria com a fase certa hoje e errada na
    # primeira correcao, e a correcao nao da erro -- da um par de garras torto que
    # so aparece de frente.
    anim.curva(ocio, "claw_left", "rotation",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, 1.5, 5.0)])
    anim.derivar(ocio, "claw_left", "claw_right", "rotation", -1.0, eixos=(1,))
    _pernas(ocio, 2.0, duracao=DUR_IDLE, periodo=DUR_IDLE)
    a.clipe("idle", DUR_IDLE, ocio)

    # ----------------------------------------------------------- locomocao
    # Velocidade 0.27 e a mais baixa da familia: ela NAO alcanca ninguem correndo,
    # e a caminhada tem de mostrar isso. O corpo mal sobe, o passo e curto e a
    # cauda fica quase parada no alto -- uma cauda balancando na caminhada gastaria
    # o gesto que so o ferrao pode usar.
    marcha = {}
    anim.curva(marcha, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 0.5)])
    anim.curva(marcha, "body", "rotation",
               [(t, anim.vetor(z=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 2.0)])
    anim.curva(marcha, "tail_base", "rotation",
               [(t, anim.vetor(z=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 3.0, fase=0.25)])
    anim.curva(marcha, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 3.5, fase=0.25)])
    anim.curva(marcha, "arm_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 6.0)])
    anim.derivar(marcha, "arm_left", "arm_right", "rotation", 1.0)
    anim.derivar(marcha, "arm_left", "claw_left", "rotation", -0.5)
    anim.derivar(marcha, "arm_left", "claw_right", "rotation", -0.5)
    _pernas(marcha, 16.0)
    a.clipe("walk", DUR_WALK, marcha)

    # ------------------------------------------------- aviso da PINCA (curto)
    # Doze ticks. O corpo agacha, os bracos recuam e as garras ESCANCARAM. A cauda
    # nao entra aqui, e a ausencia e o conteudo do clipe: cauda parada no alto e a
    # frase "este e o golpe que nao envenena".
    aviso = {}
    anim.curva(aviso, "body", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=-0.8))])
    anim.curva(aviso, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-6))])
    anim.curva(aviso, "arm_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.6, anim.vetor(y=16)),
                (DUR_WINDUP, anim.vetor(y=24))])
    anim.derivar(aviso, "arm_left", "arm_right", "rotation", -1.0, eixos=(1,))
    anim.curva(aviso, "claw_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=34))])
    anim.derivar(aviso, "claw_left", "claw_right", "rotation", -1.0, eixos=(1,))
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=8))])
    a.clipe("windup", DUR_WINDUP, aviso)

    # --------------------------------------------------- janela da PINCA
    # Cinco ticks. As garras FECHAM e os bracos varrem para dentro. O fechar e o
    # quadro que separa o golpe do aviso. O deslocamento do corpo e minimo: quem
    # avanca e a entidade, e este ataque nem arranco tem -- o alcance dele sai
    # inteiro do desenho da pinca, e repetir o avanco aqui tiraria o modelo da
    # propria caixa de colisao.
    golpe = {}
    anim.curva(golpe, "body", "position",
               [(0.0, anim.vetor(y=-0.8)), (DUR_STRIKE * 0.4, anim.vetor(z=-0.6)),
                (DUR_STRIKE, anim.vetor(z=-0.3))])
    anim.curva(golpe, "body", "rotation",
               [(0.0, anim.vetor(x=-6)), (DUR_STRIKE, anim.vetor(x=4))])
    anim.curva(golpe, "arm_left", "rotation",
               [(0.0, anim.vetor(y=24)), (DUR_STRIKE * 0.45, anim.vetor(y=-18)),
                (DUR_STRIKE, anim.vetor(y=-10))])
    anim.derivar(golpe, "arm_left", "arm_right", "rotation", -1.0, eixos=(1,))
    anim.curva(golpe, "claw_left", "rotation",
               [(0.0, anim.vetor(y=34)), (DUR_STRIKE * 0.45, anim.vetor(y=-6)),
                (DUR_STRIKE, anim.vetor(y=-2))])
    anim.derivar(golpe, "claw_left", "claw_right", "rotation", -1.0, eixos=(1,))
    anim.curva(golpe, "head", "rotation",
               [(0.0, anim.vetor(x=8)), (DUR_STRIKE, anim.vetor(x=-4))])
    a.clipe("strike", DUR_STRIKE, golpe)

    # ------------------------------------------------ recuperacao da PINCA
    # Quatorze ticks: quase o triplo da janela. E a janela em que o jogador pune
    # sem risco, e encurta-la aqui nao mudaria o servidor -- mudaria so a leitura,
    # e o jogador acharia que foi punido sem ter tido vez.
    volta = {}
    anim.curva(volta, "body", "position",
               [(0.0, anim.vetor(z=-0.3)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "body", "rotation",
               [(0.0, anim.vetor(x=4)), (DUR_RECOVERY * 0.5, anim.vetor(x=-3)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "arm_left", "rotation",
               [(0.0, anim.vetor(y=-10)), (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "arm_left", "arm_right", "rotation", -1.0, eixos=(1,))
    anim.curva(volta, "claw_left", "rotation",
               [(0.0, anim.vetor(y=-2)), (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "claw_left", "claw_right", "rotation", -1.0, eixos=(1,))
    anim.curva(volta, "head", "rotation",
               [(0.0, anim.vetor(x=-4)), (DUR_RECOVERY, anim.vetor())])
    a.clipe("recovery", DUR_RECOVERY, volta)

    # -------------------------------------------------- aviso do FERRAO (longo)
    # Vinte e oito ticks -- mais que o DOBRO do aviso da pinca, e essa diferenca e
    # o preco do veneno. O corpo baixa, a cabeca encara, as garras abrem POUCO (a
    # informacao aqui nao e a garra) e a CAUDA recua sobre o dorso, elo por elo.
    # A ultima pose e segurada: e nela que o jogador decide se sai de perto.
    aviso_do_ferrao = {}
    anim.curva(aviso_do_ferrao, "body", "position",
               [(0.0, anim.vetor()), (DUR_STING_WINDUP, anim.vetor(y=-1.0))])
    anim.curva(aviso_do_ferrao, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_STING_WINDUP, anim.vetor(x=-4))])
    anim.curva(aviso_do_ferrao, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_STING_WINDUP, anim.vetor(x=10))])
    for osso, graus in ARMADO.items():
        anim.curva(aviso_do_ferrao, osso, "rotation",
                   [(0.0, anim.vetor()),
                    (DUR_STING_WINDUP * 0.45, anim.vetor(x=graus * 0.55)),
                    (DUR_STING_WINDUP, anim.vetor(x=graus))])
    anim.curva(aviso_do_ferrao, "claw_left", "rotation",
               [(0.0, anim.vetor()), (DUR_STING_WINDUP, anim.vetor(y=12))])
    anim.derivar(aviso_do_ferrao, "claw_left", "claw_right", "rotation", -1.0, eixos=(1,))
    # As pernas dianteiras plantam. Uma formiga que ferroa sem se firmar leria como
    # se o golpe nao tivesse peso, e peso e o que justifica o telegrafo longo.
    for i, perna in enumerate(("leg_1_left", "leg_1_right", "leg_2_left", "leg_2_right")):
        anim.curva(aviso_do_ferrao, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_STING_WINDUP, anim.vetor(x=-12 - 2 * i))])
    a.clipe("sting_windup", DUR_STING_WINDUP, aviso_do_ferrao)

    # ------------------------------------------------------ janela do FERRAO
    # Quatro ticks. A cauda desfecha: `tail_base` cruza os 90 graus e leva a ponta
    # para a FRENTE do bicho, que e onde ScorpionLeaderTuning.caixaDoFerrao() mora.
    # `valida_o_ferrao_se_anuncia` cobra esse cruzamento -- uma cauda que parasse
    # em cima do dorso envenenaria um jogador que viu o golpe passar por cima dele.
    ferroada = {}
    anim.curva(ferroada, "body", "position",
               [(0.0, anim.vetor(y=-1.0)), (DUR_STING, anim.vetor(y=-0.4, z=-0.5))])
    anim.curva(ferroada, "body", "rotation",
               [(0.0, anim.vetor(x=-4)), (DUR_STING, anim.vetor(x=6))])
    for osso in CAUDA:
        anim.curva(ferroada, osso, "rotation",
                   [(0.0, anim.vetor(x=ARMADO[osso])),
                    (DUR_STING * 0.7, anim.vetor(x=CHICOTE[osso])),
                    (DUR_STING, anim.vetor(x=CHICOTE[osso]))])
    anim.curva(ferroada, "head", "rotation",
               [(0.0, anim.vetor(x=10)), (DUR_STING, anim.vetor(x=-6))])
    a.clipe("sting", DUR_STING, ferroada)

    # ------------------------------------------------- recuperacao do FERRAO
    # Vinte ticks com a cauda ainda baixa: e a UNICA janela do encontro em que o
    # traco vertical some da silhueta, e e por isso que ela e a melhor hora de
    # bater. Devolver a cauda depressa demais apagaria a recompensa de ter lido o
    # telegrafo longo.
    volta_do_ferrao = {}
    anim.curva(volta_do_ferrao, "body", "position",
               [(0.0, anim.vetor(y=-0.4, z=-0.5)), (DUR_STING_RECOVERY, anim.vetor())])
    anim.curva(volta_do_ferrao, "body", "rotation",
               [(0.0, anim.vetor(x=6)), (DUR_STING_RECOVERY, anim.vetor())])
    for osso in CAUDA:
        anim.curva(volta_do_ferrao, osso, "rotation",
                   [(0.0, anim.vetor(x=CHICOTE[osso])),
                    (DUR_STING_RECOVERY * 0.65, anim.vetor(x=CHICOTE[osso] * 0.35)),
                    (DUR_STING_RECOVERY, anim.vetor())])
    anim.curva(volta_do_ferrao, "head", "rotation",
               [(0.0, anim.vetor(x=-6)), (DUR_STING_RECOVERY, anim.vetor())])
    for perna in ("leg_1_left", "leg_1_right", "leg_2_left", "leg_2_right"):
        anim.curva(volta_do_ferrao, perna, "rotation",
                   [(0.0, anim.vetor(x=-14)), (DUR_STING_RECOVERY, anim.vetor())])
    a.clipe("sting_recovery", DUR_STING_RECOVERY, volta_do_ferrao)

    # ------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece. Aqui ela e escrita em Z -- o bicho torce de LADO -- de
    # proposito: o aviso do ferrao e um movimento em X, e se as duas poses se
    # parecessem o jogador nao saberia se interrompeu ou se armou o golpe nela.
    tropeco = {}
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(z=-18)), (0.3, anim.vetor(z=11)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "head", "rotation",
               [(0.0, anim.vetor()), (0.16, anim.vetor(z=-22)), (0.34, anim.vetor(z=13)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "tail_base", "rotation",
               [(0.0, anim.vetor()), (0.14, anim.vetor(z=26)), (0.36, anim.vetor(z=-14)),
                (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "tail_base", "tail_mid", "rotation", 0.7, eixos=(2,))
    anim.derivar(tropeco, "tail_base", "tail_tip", "rotation", 0.5, eixos=(2,))
    anim.derivar(tropeco, "tail_base", "stinger", "rotation", 0.4, eixos=(2,))
    anim.curva(tropeco, "arm_left", "rotation",
               [(0.0, anim.vetor()), (0.15, anim.vetor(y=-20)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "arm_left", "arm_right", "rotation", -1.0, eixos=(1,))
    anim.curva(tropeco, "claw_left", "rotation",
               [(0.0, anim.vetor()), (0.18, anim.vetor(y=14)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "claw_left", "claw_right", "rotation", -1.0, eixos=(1,))
    for i, perna in enumerate(PERNAS):
        anim.curva(tropeco, perna, "rotation",
                   [(0.0, anim.vetor()), (0.14 + 0.015 * i, anim.vetor(x=-20)),
                    (DUR_STAGGER, anim.vetor())])
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # --------------------------------------------------------------- morte
    # hold_on_last_frame: ela termina com o corpo no chao e a CAUDA CAIDA para a
    # frente. A cauda caida e o fim da leitura que a cauda erguida abriu -- e o
    # unico sinal que diz ao jogador que o veneno parou de vir. Um clipe de morte
    # que voltasse ao repouso mostraria o bicho de pe no ultimo quadro antes de
    # sumir, e a leitura viraria "ele sumiu" em vez de "ele morreu".
    queda = {}
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.35, anim.vetor(z=22)),
                (DUR_DEATH, anim.vetor(z=64))])
    anim.curva(queda, "body", "position",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(y=-3.0))])
    anim.curva(queda, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=18))])
    for osso in CAUDA:
        anim.curva(queda, osso, "rotation",
                   [(0.0, anim.vetor()), (DUR_DEATH * 0.45, anim.vetor(x=-12)),
                    (DUR_DEATH, anim.vetor(x=58))])
    anim.curva(queda, "arm_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(y=-26))])
    anim.derivar(queda, "arm_left", "arm_right", "rotation", -1.0, eixos=(1,))
    anim.curva(queda, "claw_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(y=-16))])
    anim.derivar(queda, "claw_left", "claw_right", "rotation", -1.0, eixos=(1,))
    for i, perna in enumerate(PERNAS):
        anim.curva(queda, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_DEATH * (0.3 + 0.03 * i), anim.vetor(x=-24)),
                    (DUR_DEATH, anim.vetor(x=46))])
    a.clipe("death", DUR_DEATH, queda)

    return a


def valida_o_ferrao_se_anuncia(clipes_do_mob):
    """Os dois ataques tem de ser DOIS avisos, e o chicote tem de ir para a frente.

    ESTA REGUA E O CONTRATO ENTRE A ARTE E A REGRA DO SERVIDOR, e as tres metades
    dela pegam defeitos que nao levantam excecao nenhuma:

      * o aviso da PINCA nao pode mexer a cauda. Se mexesse, os dois telegrafos
        ficariam parecidos e o jogador nao teria como saber, ANTES do golpe, se o
        que vem cobra veneno ou nao. ScorpionLeaderTuning aplica POISON apenas na
        janela do ferrao -- a regra existiria, e seria invisivel;

      * o aviso do FERRAO tem de mexer os QUATRO elos e ser MAIS LONGO que o da
        pinca. O telegrafo longo e o preco do veneno: encurtado, o ataque mais
        caro do bicho passa a ser o mais barato de acertar, e nada no servidor
        muda;

      * na janela do ferrao, `tail_base` tem de cruzar GRAUS_MINIMOS_DO_CHICOTE.
        Com x positivo o topo do osso vai para -Z, a frente da geometria -- a
        mesma frente que a caixa de dano cobre em +Z de mundo. Uma cauda que
        parasse antes dos 90 graus envenenaria um jogador que viu o ferrao passar
        por cima dele sem descer, e ele culparia a propria leitura.
    """
    clipes = clipes_do_mob.clipes
    pinca = clipes[clipes_do_mob.nome_completo("windup")]
    ferrao = clipes[clipes_do_mob.nome_completo("sting_windup")]
    janela = clipes[clipes_do_mob.nome_completo("sting")]

    mexidos_na_pinca = sorted(osso for osso in CAUDA if osso in pinca["bones"])
    if mexidos_na_pinca:
        raise anim.ErroDeArte(
            "o aviso da pinca mexe %s: os dois telegrafos ficam parecidos, e o jogador deixa de "
            "poder saber ANTES do golpe qual dos dois cobra veneno. A regra do servidor continua "
            "aplicando POISON so na janela do ferrao -- ela apenas fica invisivel"
            % mexidos_na_pinca)

    faltando = sorted(osso for osso in CAUDA if osso not in ferrao["bones"])
    if faltando:
        raise anim.ErroDeArte(
            "o aviso do ferrao nao mexe %s: um elo parado no meio da cadeia quebra o chicote em "
            "duas metades que nao se explicam, e o unico aviso do golpe que envenena fica ilegivel"
            % faltando)

    if ferrao["animation_length"] <= pinca["animation_length"]:
        raise anim.ErroDeArte(
            "o aviso do ferrao dura %.2fs e o da pinca dura %.2fs: o telegrafo longo E o preco do "
            "veneno, e igualando os dois o ataque mais caro do bicho vira o mais barato de acertar "
            "-- sem que uma linha do servidor mude"
            % (ferrao["animation_length"], pinca["animation_length"]))

    fim = janela["animation_length"]
    pose = anim.valor_em(janela["bones"].get("tail_base", {}).get("rotation", {}), fim,
                         anim.vetor())
    if pose[0] < GRAUS_MINIMOS_DO_CHICOTE:
        raise anim.ErroDeArte(
            "a janela do ferrao termina com tail_base.rotation x=%s e o chicote so cruza para a "
            "frente a partir de %s graus: a caixa de dano fica na FRENTE do bicho, entao o jogador "
            "seria envenenado por um ferrao que, na tela, passou por cima dele sem descer"
            % (pose[0], GRAUS_MINIMOS_DO_CHICOTE))


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(ataques=ATAQUES,
                                          extras=(valida_o_ferrao_se_anuncia,))
