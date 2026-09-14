"""Clipes do Crab Heavy -- a representacao das fases que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, AGARRAO, STAGGER OU RECOMPENSA. O servidor
publica a fase (`AttackPhase`) e o cambaleio; o cliente escolhe o clipe
correspondente. Se a animacao e a hitbox discordarem, quem esta errado e este
arquivo.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com o campo de origem ao lado. A
regua da biblioteca reprova se a soma dos tres clipes encadeados for menor que
windup + active + recovery.

A FORMA DO TELEGRAFO E A FICHA DO BICHO. Ele arma por 16 ticks, fecha em 6 e
demora 20 para se recompor -- e a recuperacao longa e o convite: e nela que o
jogador contorna. Um caranguejo que se recompusesse depressa continuaria
funcionando e tiraria do encontro a unica janela que ele oferece para chegar as
costas, que e a resposta que este mob inteiro existe para ensinar.

O QUE ESTE ARQUIVO TEM DE PROVAR ALEM DISSO: que a PINCA DESENHADA alcanca tao
longe quanto a caixa de dano do servidor reivindica. A caixa vai a 1.5 bloco da
origem; a ponta parada esta a 11 px (0.69 bloco). A diferenca so existe porque o
golpe AVANCA a pinca, e um avanco encurtado aqui -- ou apagado numa correcao de
pose -- entregaria um mob que machuca a um palmo de distancia de uma pinca que,
na tela, parou antes do jogador. Nada acusa isso. `valida_alcance_da_pincada`
acusa.

CONVENCAO DE ROTACAO E DE POSICAO, medida neste geo e nao decorada. Com -Z na
frente:
  * position z NEGATIVO leva o osso para a FRENTE;
  * rotacao X POSITIVA no corpo inclina para a FRENTE (focinho para baixo);
  * rotacao X NEGATIVA numa perna a joga para a frente do bicho;
  * rotacao Z afasta ou aproxima a peca do eixo -- e o que abre as pincas.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/crab_heavy/crab_heavy_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/crab_heavy.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402

MOB = "crab_heavy"

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType
# do Java VENCE o JSON, e cravar a repeticao la transformaria este dicionario em
# documentacao que discorda do comportamento. O portao `oLoopMoraNoArquivoDeAnimacao`
# reprova os atalhos do lado de la.
#
# Esta e tambem a UNICA lista de clipes deste mob. Uma tupla CLIPES ao lado seria
# a mesma informacao escrita duas vezes, e alguem acrescentaria um clipe em so
# uma delas.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o aviso termina com as pincas ABERTAS no alto e fica
    # assim. Voltando ao repouso no fim do clipe, o caranguejo desarmaria o
    # agarrao na tela enquanto o servidor ainda esta em WINDUP -- o jogador leria
    # "passou" e seria agarrado mesmo assim. Este golpe tira CONTROLE, e nao so
    # vida: ler errado aqui custa mais caro do que ler errado num golpe comum.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de CrabHeavyTuning, com o campo de origem ao lado.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=16,     # CrabHeavyTuning.PINCADA_WINDUP_TICKS
                       active=6,      # CrabHeavyTuning.PINCADA_ACTIVE_TICKS
                       recovery=20)}  # CrabHeavyTuning.PINCADA_RECOVERY_TICKS

# Alcance, em BLOCOS, que a caixa de golpe do servidor reivindica a frente.
# CrabHeavyTuning.ALCANCE_DA_PINCADA = 1.5D, que e o maxZ de CAIXA_DA_PINCADA.
#
# Ele esta aqui para ser COBRADO contra o desenho em movimento -- ver
# valida_alcance_da_pincada.
ALCANCE_DA_PINCADA_EM_BLOCOS = 1.5

DUR_IDLE = 3.0
DUR_WALK = 0.9
DUR_WINDUP = 0.8      # 16 ticks
DUR_STRIKE = 0.35     # 7 ticks -- um a mais que a janela, de proposito
DUR_RECOVERY = 1.0    # 20 ticks
DUR_STAGGER = 0.65
DUR_DEATH = 1.5

# Quanto a ponta da pinca acompanha o braco. Menor que 1 de proposito: a ponta
# ARRASTA atras do braco, que e o que um membro pesado faz. Igual a 1 os dois
# andariam colados e o golpe leria como um bloco unico deslizando.
ARRASTO_DA_PONTA = 0.4

# Avanco do braco da pinca no golpe, em px do modelo, para a FRENTE (z negativo).
#
# Ele e o que fecha a conta do alcance: 11 px de ponta parada + 10 px de braco +
# o arrasto da ponta cobrem a caixa de 1.5 bloco do servidor. Encurtar este
# numero nao da erro nenhum -- passa a existir uma faixa em que o servidor
# machuca e a pinca, na tela, parou antes.
AVANCO_DA_PINCA_PX = 10.0


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # ---------------------------------------------------------------- ocio
    # Respiracao LENTA e curta: a carapaca sobe e desce 0.5 px em tres segundos,
    # e mais nada se mexe com amplitude. Um ocio agitado num bicho de 60 de vida
    # competiria com o telegrafo, e o telegrafo e a unica leitura que importa
    # aqui. As pincas respiram fora de fase com o corpo -- peso solto sempre
    # chega atrasado.
    ocio = {}
    anim.curva(ocio, "carapaca", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 0.5)])
    anim.curva(ocio, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 1.2)])
    anim.derivar(ocio, "body", "ventre", "rotation", -0.5)
    anim.derivar(ocio, "body", "head", "rotation", -0.8)
    anim.curva(ocio, "claw_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.4, fase=0.3)])
    # A pinca direita espelha a esquerda: fator -1. Copiar a curva a mao nos dois
    # lados nao da erro -- da um bicho visivelmente torto, e so quem girar a
    # camera em volta dele descobre.
    anim.derivar(ocio, "claw_left", "claw_right", "rotation", -1.0)
    anim.derivar(ocio, "claw_left", "claw_left_tip", "rotation", ARRASTO_DA_PONTA)
    anim.derivar(ocio, "claw_right", "claw_right_tip", "rotation", ARRASTO_DA_PONTA)
    a.clipe("idle", DUR_IDLE, ocio)

    # ------------------------------------------------------------ locomocao
    # TRIPE ALTERNADO, que e como um artropode de seis pernas anda de verdade:
    # frente-esquerda, meio-direita e tras-esquerda sobem juntas enquanto as
    # outras tres ficam no chao. Sem isso -- com as seis em fase -- o bicho
    # pulsa como uma agua-viva, e o defeito nao aparece em portao nenhum.
    #
    # O ciclo e curto (0.9 s) e a amplitude e pequena (10 graus): velocidade 0.21
    # num corpo largo e baixo le como peso quando a passada e miuda e frequente.
    # Passada longa num caranguejo leria como caminhada de bipede.
    marcha = {}
    anim.curva(marcha, "leg_front_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 10.0)])
    for companheira in ("leg_mid_right", "leg_back_left"):
        anim.derivar(marcha, "leg_front_left", companheira, "rotation", 1.0)
    for oposta in ("leg_mid_left", "leg_front_right", "leg_back_right"):
        anim.derivar(marcha, "leg_front_left", oposta, "rotation", -1.0)
    # O corpo balanca de lado (y) no MESMO periodo da passada: e o gingado que
    # diz "seis pernas". No dobro do periodo ele mancaria.
    anim.curva(marcha, "body", "rotation",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 3.0)])
    anim.derivar(marcha, "body", "carapaca", "rotation", -0.4, eixos=(1,))
    anim.derivar(marcha, "body", "ventre", "rotation", -0.7, eixos=(1,))
    anim.derivar(marcha, "body", "head", "rotation", 0.6, eixos=(1,))
    # O solavanco vertical tem periodo METADE do ciclo: sao duas fases de apoio
    # por volta, e uma so faria o caranguejo mancar.
    anim.curva(marcha, "carapaca", "position",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 0.4)])
    anim.curva(marcha, "claw_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 6.0, fase=0.5)])
    anim.derivar(marcha, "claw_left", "claw_right", "rotation", -1.0)
    anim.derivar(marcha, "claw_left", "claw_left_tip", "rotation", ARRASTO_DA_PONTA)
    anim.derivar(marcha, "claw_right", "claw_right_tip", "rotation", ARRASTO_DA_PONTA)
    a.clipe("walk", DUR_WALK, marcha)

    # --------------------------------------------------------------- windup
    # O AVISO, e ele muda a silhueta de LUGAR e nao so de forma: o corpo AFUNDA
    # (as pernas dobram), a carapaca inclina para a frente e as duas pincas sobem
    # e ABREM. Silhueta que muda de lugar e o que se le de longe.
    #
    # Afundar tem uma segunda funcao, e ela e a ficha inteira do bicho: com o
    # corpo baixo, a carapaca fica entre o jogador e tudo o mais. Quem estiver na
    # frente no fim deste clipe esta olhando para a placa, e e exatamente isso que
    # o servidor vai cobrar dele.
    #
    # A maior parte do curso acontece na SEGUNDA metade: arranque lento e o que da
    # tempo de decidir antes do ponto de nao-retorno.
    aviso = {}
    anim.curva(aviso, "body", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.35, anim.vetor(y=-0.6)),
                (DUR_WINDUP, anim.vetor(y=-2.0))])
    anim.curva(aviso, "carapaca", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.35, anim.vetor(x=3)),
                (DUR_WINDUP, anim.vetor(x=11))])
    anim.curva(aviso, "ventre", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-6))])
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=7))])
    # As pincas ABREM: rotacao Z afasta cada uma do eixo, e o par e espelhado.
    # Elas tambem recuam (z positivo) para ter curso -- um braco ja esticado no
    # fim do aviso nao tem para onde ir no golpe, e o golpe leria como um tranco.
    anim.curva(aviso, "claw_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.35, anim.vetor(z=6, x=-4)),
                (DUR_WINDUP, anim.vetor(z=24, x=-16))])
    anim.curva(aviso, "claw_left", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(z=2.0, y=1.0))])
    anim.derivar(aviso, "claw_left", "claw_right", "rotation", -1.0, eixos=(0, 1, 2))
    anim.derivar(aviso, "claw_left", "claw_right", "position", -1.0, eixos=(0,))
    anim.derivar(aviso, "claw_left", "claw_left_tip", "rotation", 0.8, eixos=(2,))
    anim.derivar(aviso, "claw_right", "claw_right_tip", "rotation", 0.8, eixos=(2,))
    # As pernas plantam: as seis dobram para fora. Um caranguejo que arma o golpe
    # com as pernas soltas continua deslizando, e o telegrafo passa a apontar para
    # um lugar diferente daquele de onde o golpe sai.
    for perna in ("leg_front_left", "leg_mid_left", "leg_back_left"):
        anim.curva(aviso, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(z=-9))])
    for perna in ("leg_front_right", "leg_mid_right", "leg_back_right"):
        anim.curva(aviso, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(z=9))])
    a.clipe("windup", DUR_WINDUP, aviso)

    # --------------------------------------------------------------- strike
    # A JANELA QUE MACHUCA E QUE AGARRA. O servidor gasta 6 ticks (0.30 s) e o
    # clipe dura 0.35: o excedente e permitido de proposito -- o servidor manda no
    # fim e o Java corta o clipe. Faltar e que nao pode.
    #
    # As pincas FECHAM e AVANCAM ao mesmo tempo. O avanco e o que cobre a caixa de
    # dano do servidor (ver valida_alcance_da_pincada); o fechamento e o que
    # explica, na tela, por que o jogador parou de andar.
    golpe = {}
    anim.curva(golpe, "claw_left", "position",
               [(0.0, anim.vetor(z=2.0, y=1.0)),
                (DUR_STRIKE * 0.45, anim.vetor(z=-AVANCO_DA_PINCA_PX)),
                (DUR_STRIKE, anim.vetor(z=-AVANCO_DA_PINCA_PX * 0.7))])
    anim.derivar(golpe, "claw_left", "claw_right", "position", 1.0, eixos=(1, 2))
    anim.derivar(golpe, "claw_left", "claw_left_tip", "position", ARRASTO_DA_PONTA, eixos=(2,))
    anim.derivar(golpe, "claw_right", "claw_right_tip", "position", ARRASTO_DA_PONTA, eixos=(2,))
    # De 24 graus abertos a -6: passar do fechado e o exagero que separa o golpe
    # do telegrafo. Sem ele os dois clipes leem como um movimento continuo e o
    # jogador nao consegue marcar onde o agarrao aconteceu.
    anim.curva(golpe, "claw_left", "rotation",
               [(0.0, anim.vetor(z=24, x=-16)), (DUR_STRIKE * 0.45, anim.vetor(z=-6, x=4)),
                (DUR_STRIKE, anim.vetor(z=0, x=1))])
    anim.derivar(golpe, "claw_left", "claw_right", "rotation", -1.0, eixos=(0, 1, 2))
    anim.derivar(golpe, "claw_left", "claw_left_tip", "rotation", 0.8, eixos=(2,))
    anim.derivar(golpe, "claw_right", "claw_right_tip", "rotation", 0.8, eixos=(2,))
    # O corpo inteiro entra no golpe -- z negativo e para a frente nesta
    # geometria. Sem esse avanco, um bicho de 22 px de largura parece bater so
    # com os bracos.
    anim.curva(golpe, "body", "position",
               [(0.0, anim.vetor(y=-2.0)), (DUR_STRIKE * 0.45, anim.vetor(y=-1.2, z=-2.5)),
                (DUR_STRIKE, anim.vetor(y=-1.4, z=-1.6))])
    anim.curva(golpe, "carapaca", "rotation",
               [(0.0, anim.vetor(x=11)), (DUR_STRIKE * 0.45, anim.vetor(x=-4)),
                (DUR_STRIKE, anim.vetor(x=-1))])
    anim.derivar(golpe, "carapaca", "ventre", "rotation", -0.5)
    anim.derivar(golpe, "carapaca", "head", "rotation", 0.7)
    a.clipe("strike", DUR_STRIKE, golpe)

    # -------------------------------------------------------------- recovery
    # A JANELA DE RESPOSTA, e e a mais longa do bicho: 1.0 s em que o jogador
    # contorna. Encurtar este clipe nao mudaria o servidor -- mudaria so a
    # leitura, e o jogador acharia que foi punido sem ter tido janela.
    #
    # O caranguejo fica com as pincas CAIDAS na primeira metade e so depois se
    # recompoe. E nessa metade que a carapaca esta inclinada para a frente e o
    # ventre aparece mais por tras: o quadro em que contornar e obviamente a
    # resposta certa.
    volta = {}
    anim.curva(volta, "claw_left", "position",
               [(0.0, anim.vetor(z=-AVANCO_DA_PINCA_PX * 0.7)),
                (DUR_RECOVERY * 0.5, anim.vetor(z=-2.0, y=-1.0)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "claw_left", "claw_right", "position", 1.0, eixos=(1, 2))
    anim.derivar(volta, "claw_left", "claw_left_tip", "position", ARRASTO_DA_PONTA, eixos=(2,))
    anim.derivar(volta, "claw_right", "claw_right_tip", "position", ARRASTO_DA_PONTA, eixos=(2,))
    anim.curva(volta, "claw_left", "rotation",
               [(0.0, anim.vetor(z=0, x=1)), (DUR_RECOVERY * 0.5, anim.vetor(z=4, x=14)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "claw_left", "claw_right", "rotation", -1.0, eixos=(0, 1, 2))
    anim.derivar(volta, "claw_left", "claw_left_tip", "rotation", 0.8, eixos=(2,))
    anim.derivar(volta, "claw_right", "claw_right_tip", "rotation", 0.8, eixos=(2,))
    anim.curva(volta, "body", "position",
               [(0.0, anim.vetor(y=-1.4, z=-1.6)), (DUR_RECOVERY * 0.5, anim.vetor(y=-2.2)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "carapaca", "rotation",
               [(0.0, anim.vetor(x=-1)), (DUR_RECOVERY * 0.5, anim.vetor(x=9)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "carapaca", "ventre", "rotation", -0.5)
    anim.derivar(volta, "carapaca", "head", "rotation", 0.7)
    for perna in ("leg_front_left", "leg_mid_left", "leg_back_left"):
        anim.curva(volta, perna, "rotation",
                   [(0.0, anim.vetor(z=-9)), (DUR_RECOVERY, anim.vetor())])
    for perna in ("leg_front_right", "leg_mid_right", "leg_back_right"):
        anim.curva(volta, perna, "rotation",
                   [(0.0, anim.vetor(z=9)), (DUR_RECOVERY, anim.vetor())])
    a.clipe("recovery", DUR_RECOVERY, volta)

    # --------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece -- e quem chegou pelas costas nao recebe confirmacao
    # nenhuma de que chegar pelas costas era a resposta.
    #
    # Quem mais se mexe e a CARAPACA, e isso nao e estetica: o cambaleio deste
    # bicho quase sempre vem de um golpe que passou pela placa, e a reacao tem de
    # acontecer onde o golpe entrou. As pernas de um lado escorregam -- um
    # artropode que perde o equilibrio perde o apoio, e nao o prumo.
    tropeco = {}
    anim.curva(tropeco, "carapaca", "rotation",
               [(0.0, anim.vetor()), (0.1, anim.vetor(x=-13, z=10)),
                (0.32, anim.vetor(x=6, z=-4)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "carapaca", "ventre", "rotation", -0.6, eixos=(0, 2))
    anim.derivar(tropeco, "carapaca", "head", "rotation", 0.8, eixos=(0, 2))
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(z=9, y=7)),
                (0.36, anim.vetor(z=-3, y=-3)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "body", "position",
               [(0.0, anim.vetor()), (0.12, anim.vetor(y=-1.6)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "claw_left", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(x=-22, z=13)),
                (0.36, anim.vetor(x=7, z=-4)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "claw_left", "claw_right", "rotation", -1.0, eixos=(0, 1, 2))
    anim.derivar(tropeco, "claw_left", "claw_left_tip", "rotation", 0.7, eixos=(2,))
    anim.derivar(tropeco, "claw_right", "claw_right_tip", "rotation", 0.7, eixos=(2,))
    for perna in ("leg_front_left", "leg_mid_left", "leg_back_left"):
        anim.curva(tropeco, perna, "rotation",
                   [(0.0, anim.vetor()), (0.14, anim.vetor(z=-15, x=8)),
                    (DUR_STAGGER, anim.vetor())])
    for perna in ("leg_front_right", "leg_mid_right", "leg_back_right"):
        anim.curva(tropeco, perna, "rotation",
                   [(0.0, anim.vetor()), (0.14, anim.vetor(z=6, x=-5)),
                    (DUR_STAGGER, anim.vetor())])
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # ------------------------------------------------------------------ morte
    # hold_on_last_frame: o corpo fica caido ate a entidade sumir. Um clipe de
    # morte que volta ao repouso mostra o bicho INTEIRO no ultimo quadro antes de
    # desaparecer, e a leitura vira "ele sumiu", nao "ele caiu".
    #
    # Ele tomba de LADO e o ventre fica virado para quem esta em pe. E o epilogo
    # certo para um mob cujo encontro inteiro foi sobre achar o ventre: o cadaver
    # mostra, sem uma linha de texto, o que era para ter sido atingido.
    #
    # O corpo desce 5 px enquanto gira: sem a descida, o modelo pivota no ar e
    # metade dele atravessa o chao.
    queda = {}
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(z=22)),
                (DUR_DEATH, anim.vetor(z=88))])
    anim.curva(queda, "body", "position",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(y=-1.5)),
                (DUR_DEATH, anim.vetor(y=-5.0))])
    anim.curva(queda, "carapaca", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.45, anim.vetor(x=-9, z=6)),
                (DUR_DEATH, anim.vetor(x=-3, z=12))])
    anim.derivar(queda, "carapaca", "ventre", "rotation", -0.7, eixos=(0, 2))
    anim.derivar(queda, "carapaca", "head", "rotation", 1.2, eixos=(0, 2))
    anim.curva(queda, "claw_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.4, anim.vetor(x=-26, z=18)),
                (DUR_DEATH, anim.vetor(x=-41, z=27))])
    anim.derivar(queda, "claw_left", "claw_right", "rotation", -1.0, eixos=(0, 1, 2))
    # A ponta cai ALEM do braco: fator maior que 1 e o que um peso solto faz.
    anim.derivar(queda, "claw_left", "claw_left_tip", "rotation", 1.3, eixos=(2,))
    anim.derivar(queda, "claw_right", "claw_right_tip", "rotation", 1.3, eixos=(2,))
    # As pernas se RECOLHEM, e nao se espalham: artropode morto encolhe.
    for perna in ("leg_front_left", "leg_mid_left", "leg_back_left"):
        anim.curva(queda, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(z=26, x=12)),
                    (DUR_DEATH, anim.vetor(z=44, x=18))])
    for perna in ("leg_front_right", "leg_mid_right", "leg_back_right"):
        anim.curva(queda, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(z=-26, x=12)),
                    (DUR_DEATH, anim.vetor(z=-44, x=18))])
    a.clipe("death", DUR_DEATH, queda)

    return a


# ------------------------------------------ validacao que liga arte e regra

def valida_alcance_da_pincada(a):
    """A pinca DESENHADA, no quadro mais avancado do golpe, cobre a caixa de dano.

    A caixa de `CrabHeavyTuning.CAIXA_DA_PINCADA` vai a 1.5 bloco da origem do
    bicho. A ponta parada chega a 11 px (0.69 bloco), e a diferenca e coberta
    pelo AVANCO do braco mais o arrasto da ponta -- ou seja, por keyframes deste
    arquivo.

    Isso torna o alcance uma propriedade da ANIMACAO, e nao do modelo parado, e
    por isso a regua mora aqui: o gerador de geometria nao tem como saber quanto
    a pinca avanca.

    O que ela impede: alguem suavizar o golpe numa passagem de polimento, tirar
    2 px do avanco, e criar uma faixa em que o servidor machuca e agarra enquanto
    a pinca, na tela, parou antes do jogador. Nao ha erro nisso, nao ha log, e a
    reclamacao que chega -- "esse bicho tem alcance invisivel" -- e a mais dificil
    de diagnosticar que um mob corpo-a-corpo consegue gerar.

    Ela tambem morde do outro lado: se o avanco crescer muito alem do necessario,
    a pinca passa a atravessar o jogador sem que o servidor cobre nada, e o mob
    ensina um alcance que ele nao tem. Por isso ha teto, e nao so piso.
    """
    # -Z e a frente na geometria; +Z e a frente na caixa do servidor. As duas
    # convencoes medem a MESMA direcao fisica, e e por isso que o sinal e
    # invertido aqui de proposito, e nao por descuido.
    ponta_parada_px = -anim.faixa_de(a.geometria,
                                     ("claw_left_tip", "claw_right_tip"), 2)[0]

    golpe = a.clipes[a.nome_completo("strike")]["bones"]
    avanco_px = 0.0
    for osso in ("claw_left", "claw_left_tip"):
        quadros = golpe.get(osso, {}).get("position")
        if not quadros:
            raise anim.ErroDeArte(
                "o clipe 'strike' nao move a posicao de '%s': sem avanco, a pinca desenhada chega "
                "a %.1f px e a caixa do servidor reivindica %.1f px -- o bicho machuca de longe "
                "com uma pinca que parou perto" % (osso, ponta_parada_px,
                                                   ALCANCE_DA_PINCADA_EM_BLOCOS * 16.0))
        avanco_px += -min(v[2] for v in quadros.values())

    alcance_px = ponta_parada_px + avanco_px
    exigido_px = ALCANCE_DA_PINCADA_EM_BLOCOS * 16.0
    if alcance_px < exigido_px:
        raise anim.ErroDeArte(
            "a pinca alcanca %.1f px (%.2f bloco) no quadro mais avancado e a caixa de golpe do "
            "servidor vai ate %.1f px (%.2f bloco): o jogador e agarrado por uma pinca que, na "
            "tela, parou antes dele, e nada acusa"
            % (alcance_px, alcance_px / 16.0, exigido_px, ALCANCE_DA_PINCADA_EM_BLOCOS))
    if alcance_px > exigido_px * 1.5:
        raise anim.ErroDeArte(
            "a pinca alcanca %.1f px (%.2f bloco) e a caixa do servidor so vai ate %.1f px (%.2f "
            "bloco): a pinca atravessa quem esta longe demais para ser atingido, e o mob passa a "
            "ensinar um alcance que ele nao tem"
            % (alcance_px, alcance_px / 16.0, exigido_px, ALCANCE_DA_PINCADA_EM_BLOCOS))

    print("alcance da pinca: %.1f px parada + %.1f px de avanco = %.1f px "
          "(caixa do servidor: %.1f px)"
          % (ponta_parada_px, avanco_px, alcance_px, exigido_px))


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(ataques=ATAQUES,
                                          extras=(valida_alcance_da_pincada,))
