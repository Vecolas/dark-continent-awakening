"""Clipes do Bat Scout -- e o lugar onde a ASA ABERTA existe.

NENHUM KEYFRAME AQUI APLICA DANO, STAGGER OU RECOMPENSA. O servidor publica a
fase (`AttackPhase`) e o cambaleio; o cliente escolhe o clipe correspondente. Se
a animacao e a hitbox discordarem, quem esta errado e este arquivo.

A DECISAO QUE ESTE ARQUIVO CARREGA: a asa dobrada e a asa aberta sao POSES, e
nao caixas alternativas. O modelo tem UMA asa, desenhada dobrada no repouso
(ver `bat_scout_geo.py`), e e a rotacao daqui que a abre. A alternativa --
duas caixas, uma ligada por vez -- nao da erro nenhum no dia em que as duas
ficarem ligadas: da um morcego com quatro asas, e nenhum portao deste
repositorio ve isso. Uma pose nao tem estado para esquecer.

Isso so vale enquanto alguem medir, e por isso `valida_asa_dobrada_e_aberta`
existe. Ela cobra o DEGRAU: `idle` tem de manter a asa fechada e `walk`,
`windup` e `strike` tem de abri-la alem do limiar. Sem o degrau os dois estados
viram o mesmo estado -- o morcego pousado com a asa meio aberta e o morcego
voando com a asa meio aberta --, e a leitura de "esse ai voa" desaparece sem que
uma linha de log mude.

O MAPA CLIPE <-> ESTADO E O DO CONTROLADOR DA ENTIDADE, e nao uma escolha livre:

    idle      no chao E parado        -> POUSADO, asa fechada
    walk      no AR ou se deslocando  -> VOANDO, asa batendo
    windup    AttackPhase.WINDUP publicado pelo servidor
    strike    AttackPhase.ACTIVE
    recovery  AttackPhase.RECOVERY
    stagger   campo CAMBALEANDO publicado pelo servidor
    death     deathTime > 0

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com o campo de origem ao lado. A
regua da biblioteca reprova se a soma dos tres clipes encadeados for menor que
windup + active + recovery -- senao o morcego relaxa no meio da mordida que
ainda vai acertar.

E A ARREMETIDA E COBRADA CONTRA O ALCANCE DA MORDIDA. Este bicho e minusculo: o
focinho desenhado chega a 5 px do centro dele, e a caixa de golpe do servidor
reivindica bem mais que isso. Quem cobre a diferenca e o AVANCO do corpo no
clipe de `strike`. Se ele encolher, o jogador leva 3 de dano de um morcego que,
na tela, parou antes dele -- e essa e a reclamacao mais dificil de diagnosticar
que um corpo-a-corpo consegue gerar, porque nao ha erro nenhum para procurar.

CONVENCAO DE ROTACAO, medida neste geo e nao decorada. Com -Z na frente:
  * rotacao X NEGATIVA num membro pendurado o joga para a FRENTE e para cima;
    X POSITIVA o joga para TRAS e para cima;
  * rotacao X POSITIVA na cabeca inclina o focinho para BAIXO;
  * rotacao Z POSITIVA na asa esquerda (lado +X) a abre para FORA; a asa direita
    e sempre DERIVADA dela com fator -1, porque escrita a mao ela ficaria com a
    fase certa hoje e meio quadro atras na primeira correcao.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/bat_scout/bat_scout_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/bat_scout.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402

MOB = "bat_scout"

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType
# do Java VENCE o JSON, e cravar a repeticao la transformaria este dicionario em
# documentacao que discorda do comportamento.
#
# Esta e tambem a UNICA lista de clipes deste mob. Uma tupla CLIPES ao lado seria
# a mesma informacao escrita duas vezes, e alguem acrescentaria um clipe em so
# uma delas.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o telegrafo termina com o morcego ARMADO no alto e fica
    # assim. Voltando ao repouso no fim do clipe, ele desarmaria a mordida na
    # tela enquanto o servidor ainda esta em WINDUP -- o jogador leria "passou" e
    # levaria o dano mesmo assim.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de BatScoutTuning.mordida(), com o campo de origem ao lado.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=12,     # BatScoutTuning.MORDIDA_WINDUP_TICKS
                       active=3,      # BatScoutTuning.MORDIDA_ACTIVE_TICKS
                       recovery=14)}  # BatScoutTuning.MORDIDA_RECOVERY_TICKS

# Alcance, em BLOCOS, que a caixa de golpe do servidor reivindica a frente.
# BatScoutTuning.ALCANCE_DA_MORDIDA -> caixaDaMordida().maxZ().
#
# Ele esta aqui para ser COBRADO contra o desenho mais o avanco do clipe. Ver
# valida_arremetida_cobre_a_mordida.
ALCANCE_DA_MORDIDA_EM_BLOCOS = 0.55

# Limiares da leitura de asa, em graus. Nao sao balanceamento: sao o DEGRAU entre
# "pousado" e "voando", e e o degrau que o jogador le. Mora aqui, ao lado do
# comentario que o explica, e nao na biblioteca -- a biblioteca nao pode ter
# numero de bicho.
ASA_DOBRADA_MAXIMA = 25.0
ASA_ABERTA_MINIMA = 60.0

CLIPES_DE_ASA_DOBRADA = ("idle",)
CLIPES_DE_ASA_ABERTA = ("walk", "windup", "strike")

DUR_IDLE = 2.4
DUR_WALK = 0.5       # 10 ticks por batida: morcego bate depressa, e isso e a ficha
DUR_WINDUP = 0.6     # 12 ticks
DUR_STRIKE = 0.2     # 4 ticks -- um a mais que a janela ativa, de proposito
DUR_RECOVERY = 0.7   # 14 ticks
DUR_STAGGER = 0.45
DUR_DEATH = 0.9

# Avanco do corpo no golpe, em px do modelo. Z NEGATIVO e para a FRENTE nesta
# geometria. E este numero, somado ao focinho desenhado, que paga o alcance da
# mordida do servidor -- ver valida_arremetida_cobre_a_mordida.
ARREMETIDA_PX = 5.0

# Angulo de repouso da asa dobrada e angulo de cruzeiro da asa aberta.
ASA_FECHADA = 6.0
ASA_CRUZEIRO = 52.0

# Arrasto da ponta da asa em relacao ao braco dela. Positivo e menor que 1: a
# ponta acompanha, mas fecha um pouco atras -- e o que faz a membrana ler como
# pano esticado e nao como placa rigida.
ARRASTO_DA_PONTA = 0.45


def _asa(bones, pares):
    """Escreve a asa esquerda e DERIVA a direita e as duas pontas.

    A direita nunca e escrita a mao. Escrita a mao ela fica com a fase certa hoje
    e meio quadro atras na primeira correcao da esquerda, e isso ninguem consegue
    descrever: chega como "tem alguma coisa estranha nesse morcego".
    """
    anim.curva(bones, "wing_left", "rotation", pares)
    anim.derivar(bones, "wing_left", "wing_right", "rotation", -1.0, eixos=(2,))
    anim.derivar(bones, "wing_left", "wing_left_tip", "rotation", ARRASTO_DA_PONTA, eixos=(2,))
    anim.derivar(bones, "wing_right", "wing_right_tip", "rotation", ARRASTO_DA_PONTA, eixos=(2,))


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # ----------------------------------------------------------------- ocio
    # POUSADO. Respiracao lenta, asa fechada colada no corpo, orelhas varrendo
    # devagar. Ocio chamativo competiria com o unico quadro que importa neste
    # bicho -- o instante em que ele ABRE a asa e sai --, e esse instante e a
    # deixa do jogador para tentar alcanca-lo antes do relatorio.
    ocio = {}
    anim.curva(ocio, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 0.3)])
    anim.curva(ocio, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 3.0, fase=0.15)])
    # As orelhas sao DERIVADAS da cabeca com fator negativo: elas ficam para tras
    # do movimento do pescoco, que e o arrasto de uma membrana fina.
    anim.derivar(ocio, "head", "ear_left", "rotation", -0.6)
    anim.derivar(ocio, "head", "ear_right", "rotation", -0.6)
    _asa(ocio, [(t, anim.vetor(z=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, ASA_FECHADA, base=ASA_FECHADA)])
    anim.curva(ocio, "foot_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.0, fase=0.4)])
    anim.derivar(ocio, "foot_left", "foot_right", "rotation", 1.0)
    a.clipe("idle", DUR_IDLE, ocio)

    # ------------------------------------------------------------- locomocao
    # VOANDO. Meio segundo por batida: um morcego bate depressa, e uma batida
    # lenta num bicho de 0.9 bloco le como passaro grande -- ou seja, como outro
    # mob. A asa vai de 19 a 85 graus: e o curso inteiro, e e o que garante o
    # degrau contra o clipe de ocio.
    voo = {}
    _asa(voo, [(t, anim.vetor(z=v)) for t, v in
               anim.cossenoide(DUR_WALK, DUR_WALK, 33.0, base=ASA_CRUZEIRO)])
    # O corpo sobe no fim da descida da asa, e nao junto com ela: a fase de um
    # quarto e o que faz a batida parecer EMPURRAR o ar em vez de acompanhar o
    # corpo.
    anim.curva(voo, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 0.6, fase=0.25)])
    anim.curva(voo, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 2.5, fase=0.5)])
    anim.derivar(voo, "head", "ear_left", "rotation", -0.5)
    anim.derivar(voo, "head", "ear_right", "rotation", -0.5)
    # Pes recolhidos para TRAS (X positivo num membro pendurado). Pes pendurados
    # em voo leem como bicho caindo, e nao como bicho voando.
    anim.curva(voo, "foot_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 4.0, base=35.0)])
    anim.derivar(voo, "foot_left", "foot_right", "rotation", 1.0)
    a.clipe("walk", DUR_WALK, voo)

    # ---------------------------------------------------------------- windup
    # O AVISO, e ele dura 0.6 s. O morcego RECUA no ar enquanto abre a asa ate
    # o maximo: a silhueta muda de LUGAR e de TAMANHO ao mesmo tempo, que e o que
    # se le de longe. Encurtar isto na animacao sem encurtar no servidor entrega
    # o pior dos dois mundos -- a pose ja passou e o dano so sai depois.
    aviso = {}
    _asa(aviso, [(0.0, anim.vetor(z=ASA_CRUZEIRO)),
                 (DUR_WINDUP * 0.35, anim.vetor(z=72.0)),
                 (DUR_WINDUP, anim.vetor(z=95.0))])
    # Recua (Z POSITIVO) e sobe: e o gesto de quem vai cair em cima.
    anim.curva(aviso, "body", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.35, anim.vetor(y=0.8, z=1.2)),
                (DUR_WINDUP, anim.vetor(y=1.6, z=2.2))])
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=12.0))])
    # As orelhas DEITAM no aviso -- e o unico sinal que sobra de perfil, quando a
    # asa aberta esconde o corpo inteiro.
    anim.curva(aviso, "ear_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=44.0))])
    anim.derivar(aviso, "ear_left", "ear_right", "rotation", 1.0)
    anim.curva(aviso, "foot_left", "rotation",
               [(0.0, anim.vetor(x=35.0)), (DUR_WINDUP, anim.vetor(x=-18.0))])
    anim.derivar(aviso, "foot_left", "foot_right", "rotation", 1.0)
    a.clipe("windup", DUR_WINDUP, aviso)

    # ---------------------------------------------------------------- strike
    # A JANELA QUE MACHUCA, e a arremetida que paga o alcance. O corpo avanca
    # ARREMETIDA_PX para a FRENTE (z negativo) e o focinho desenhado cobre o
    # resto -- valida_arremetida_cobre_a_mordida cobra a soma contra a caixa do
    # servidor.
    #
    # O servidor gasta 3 ticks (0.15 s) e o clipe dura 0.2: o excedente e
    # permitido de proposito -- o servidor manda no fim e o Java corta o clipe.
    # Faltar e que nao pode.
    golpe = {}
    anim.curva(golpe, "body", "position",
               [(0.0, anim.vetor(y=1.6, z=2.2)),
                (DUR_STRIKE * 0.6, anim.vetor(y=-0.4, z=-ARREMETIDA_PX)),
                (DUR_STRIKE, anim.vetor(y=-0.2, z=-ARREMETIDA_PX + 1.5))])
    # A asa fecha CONTRA o corpo no mergulho: ela comeca no maximo e passa do
    # ponto para baixo. O exagero e o que separa visualmente o golpe do
    # telegrafo; sem ele os dois clipes leem como um movimento continuo e o
    # jogador nao consegue marcar onde o dano saiu.
    _asa(golpe, [(0.0, anim.vetor(z=95.0)),
                 (DUR_STRIKE * 0.6, anim.vetor(z=22.0)),
                 (DUR_STRIKE, anim.vetor(z=38.0))])
    anim.curva(golpe, "head", "rotation",
               [(0.0, anim.vetor(x=12.0)), (DUR_STRIKE * 0.6, anim.vetor(x=-14.0)),
                (DUR_STRIKE, anim.vetor(x=-8.0))])
    anim.derivar(golpe, "head", "ear_left", "rotation", 1.4)
    anim.derivar(golpe, "head", "ear_right", "rotation", 1.4)
    anim.curva(golpe, "foot_left", "rotation",
               [(0.0, anim.vetor(x=-18.0)), (DUR_STRIKE, anim.vetor(x=-36.0))])
    anim.derivar(golpe, "foot_left", "foot_right", "rotation", 1.0)
    a.clipe("strike", DUR_STRIKE, golpe)

    # -------------------------------------------------------------- recovery
    # A JANELA DE RESPOSTA. Este bicho tem 16 de vida e armadura 0: 0.7 s de
    # recuperacao no ar e tempo de sobra para uma flecha, e e de proposito.
    # Encurtar este clipe nao mudaria o servidor -- mudaria so a leitura, e o
    # jogador acharia que apanhou sem ter tido janela.
    volta = {}
    anim.curva(volta, "body", "position",
               [(0.0, anim.vetor(y=-0.2, z=-ARREMETIDA_PX + 1.5)),
                (DUR_RECOVERY * 0.5, anim.vetor(y=0.6, z=-1.0)),
                (DUR_RECOVERY, anim.vetor())])
    _asa(volta, [(0.0, anim.vetor(z=38.0)),
                 (DUR_RECOVERY * 0.5, anim.vetor(z=58.0)),
                 (DUR_RECOVERY, anim.vetor(z=ASA_CRUZEIRO))])
    anim.curva(volta, "head", "rotation",
               [(0.0, anim.vetor(x=-8.0)), (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "head", "ear_left", "rotation", -0.6)
    anim.derivar(volta, "head", "ear_right", "rotation", -0.6)
    anim.curva(volta, "foot_left", "rotation",
               [(0.0, anim.vetor(x=-36.0)), (DUR_RECOVERY, anim.vetor(x=35.0))])
    anim.derivar(volta, "foot_left", "foot_right", "rotation", 1.0)
    a.clipe("recovery", DUR_RECOVERY, volta)

    # --------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece. Aqui ela e a asa FECHANDO de uma vez e o corpo caindo:
    # e a mesma coisa que `RegrasDeVooDeBatedor` decide do lado de la
    # (MovimentoDeVoo.CAIR), e as duas leituras precisam contar a mesma historia.
    # Um stagger animado como tremidinha, com a regra mandando cair, entrega um
    # bicho que despenca sem motivo aparente.
    tropeco = {}
    _asa(tropeco, [(0.0, anim.vetor(z=ASA_CRUZEIRO)), (0.1, anim.vetor(z=14.0)),
                   (0.26, anim.vetor(z=30.0)), (DUR_STAGGER, anim.vetor(z=ASA_FECHADA))])
    anim.curva(tropeco, "body", "position",
               [(0.0, anim.vetor()), (0.12, anim.vetor(y=-1.6)), (0.3, anim.vetor(y=-0.9)),
                (DUR_STAGGER, anim.vetor(y=-1.2))])
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(z=-18.0)), (0.3, anim.vetor(z=9.0)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "head", "rotation",
               [(0.0, anim.vetor()), (0.1, anim.vetor(x=-26.0, y=12.0)),
                (0.28, anim.vetor(x=10.0, y=-5.0)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "head", "ear_left", "rotation", -1.2)
    anim.derivar(tropeco, "head", "ear_right", "rotation", -1.2)
    anim.curva(tropeco, "foot_left", "rotation",
               [(0.0, anim.vetor(x=35.0)), (0.12, anim.vetor(x=-20.0)),
                (DUR_STAGGER, anim.vetor(x=5.0))])
    anim.derivar(tropeco, "foot_left", "foot_right", "rotation", 1.0)
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # ----------------------------------------------------------------- morte
    # hold_on_last_frame: o corpo fica caido ate a entidade sumir. Um clipe de
    # morte que volta ao repouso mostra o morcego de asas abertas no ultimo
    # quadro antes de desaparecer, e a leitura vira "ele fugiu", nao "ele caiu"
    # -- num bicho cuja resposta INTEIRA e mata-lo antes do relatorio, confundir
    # "caiu" com "fugiu" e o pior desfecho possivel.
    #
    # O corpo desce 4 px enquanto tomba: sem a descida o modelo pivota no ar e
    # metade dele atravessa o chao.
    queda = {}
    _asa(queda, [(0.0, anim.vetor(z=ASA_CRUZEIRO)), (DUR_DEATH * 0.3, anim.vetor(z=34.0)),
                 (DUR_DEATH, anim.vetor(z=ASA_FECHADA))])
    anim.curva(queda, "body", "position",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(y=-1.5)),
                (DUR_DEATH, anim.vetor(y=-4.0))])
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.35, anim.vetor(x=22.0, z=-14.0)),
                (DUR_DEATH, anim.vetor(x=76.0, z=-26.0))])
    anim.curva(queda, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.4, anim.vetor(x=-18.0)),
                (DUR_DEATH, anim.vetor(x=-42.0))])
    anim.derivar(queda, "head", "ear_left", "rotation", 1.1)
    anim.derivar(queda, "head", "ear_right", "rotation", 1.1)
    anim.curva(queda, "foot_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=-52.0))])
    anim.derivar(queda, "foot_left", "foot_right", "rotation", 1.0)
    a.clipe("death", DUR_DEATH, queda)

    return a


# ------------------------------------------ validacoes que ligam arte e regra

def _abertura_maxima(a, clipe):
    """O maior |rotacao Z| que a asa esquerda alcanca num clipe, em graus."""
    quadros = a.clipes[a.nome_completo(clipe)]["bones"].get("wing_left", {}).get("rotation")
    if not quadros:
        raise anim.ErroDeArte(
            "o clipe '%s' nao move a asa: este bicho e a asa dele, e um clipe que a deixa parada "
            "mostra um morcego congelado enquanto o corpo inteiro se mexe" % clipe)
    return max(abs(v[2]) for v in quadros.values())


def valida_asa_dobrada_e_aberta(a):
    """O degrau entre POUSADO e VOANDO tem de existir, e ele se mede em graus.

    Esta e a regua do requisito central deste mob: a asa aberta e uma POSE, e
    poses so se distinguem se houver distancia entre elas. Com `idle` e `walk`
    na mesma abertura, o modelo continua correto, os portoes continuam verdes, o
    GeckoLib continua tocando os dois clipes -- e o jogador perde o unico sinal
    que diz que este bicho voa e que ele acabou de SAIR.

    A segunda metade cobra o inverso e e a que impede o atalho: se `idle` abrisse
    a asa, alguem poderia desenhar a asa ja aberta no geo e apagar a animacao
    inteira. O modelo parado nao cabe na hitbox de asa aberta
    (`valida_envergadura_contra_a_hitbox`), entao esse atalho e proibido nos dois
    arquivos, por dois motivos diferentes.
    """
    for clipe in CLIPES_DE_ASA_DOBRADA:
        abertura = _abertura_maxima(a, clipe)
        if abertura > ASA_DOBRADA_MAXIMA:
            raise anim.ErroDeArte(
                "o clipe '%s' abre a asa ate %.1f graus e o teto de asa dobrada e %.1f: pousado "
                "com a asa aberta, o morcego perde o degrau que diz 'ele saiu voando', e nada no "
                "jogo acusa isso" % (clipe, abertura, ASA_DOBRADA_MAXIMA))
    for clipe in CLIPES_DE_ASA_ABERTA:
        abertura = _abertura_maxima(a, clipe)
        if abertura < ASA_ABERTA_MINIMA:
            raise anim.ErroDeArte(
                "o clipe '%s' abre a asa so ate %.1f graus e o minimo de asa aberta e %.1f: a "
                "silhueta de voo fica igual a de pouso, e a locomocao deste bicho deixa de ler "
                "como voo" % (clipe, abertura, ASA_ABERTA_MINIMA))


def valida_arremetida_cobre_a_mordida(a):
    """Focinho DESENHADO + avanco do clipe tem de alcancar a caixa do servidor.

    O Cyclops cobra isto no porrete, que e uma peca estatica; aqui a peca
    estatica nao basta. O focinho deste bicho chega a cinco pixels do centro
    dele -- 0.31 bloco -- e a caixa de golpe do servidor reivindica 0.55. Quem
    paga a diferenca e a ARREMETIDA do clipe de `strike`, e por isso ela e
    medida aqui e nao no gerador de geometria: ela nao existe na geometria.

    Encurtar o avanco nao levanta excecao: o dano sai, o cooldown sai, o log fica
    limpo, e o jogador leva 3 de dano de um morcego que, na tela, parou antes
    dele. Num bicho de dano baixo isso e ainda pior -- ninguem associa a mordida
    invisivel a nada, e o relato que chega e "as vezes ele acerta de longe".
    """
    # A medida sai do GEO lido do disco, e nao de um literal repetido aqui: um
    # literal continuaria dizendo 5 px no dia em que alguem encurtasse o focinho,
    # e a regua passaria a aprovar exatamente o que existe para reprovar.
    focinho_z, _ = anim.faixa_de(a.geometria, ("head",), 2)
    focinho_px = max(0.0, -focinho_z)

    quadros = a.clipes[a.nome_completo("strike")]["bones"].get("body", {}).get("position")
    if not quadros:
        raise anim.ErroDeArte(
            "o clipe 'strike' nao move o corpo: sem arremetida o alcance da mordida passa a ser "
            "so o focinho desenhado, e a caixa do servidor acerta de onde nao ha bicho")
    avanco_px = max(0.0, -min(v[2] for v in quadros.values()))

    alcance_px = focinho_px + avanco_px
    exigido_px = ALCANCE_DA_MORDIDA_EM_BLOCOS * 16.0
    if alcance_px < exigido_px:
        raise anim.ErroDeArte(
            "o focinho desenhado chega a %.1f px e a arremetida avanca %.1f px (total %.1f px, "
            "%.2f blocos); a caixa de golpe do servidor vai ate %.1f px (%.2f blocos): o jogador "
            "leva a mordida de um morcego que, na tela, parou antes dele"
            % (focinho_px, avanco_px, alcance_px, alcance_px / 16.0,
               exigido_px, ALCANCE_DA_MORDIDA_EM_BLOCOS))

    print("mordida: focinho %.1f px + arremetida %.1f px = %.1f px (%.2f blocos), "
          "caixa do servidor %.2f blocos"
          % (focinho_px, avanco_px, alcance_px, alcance_px / 16.0,
             ALCANCE_DA_MORDIDA_EM_BLOCOS))


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(
        ataques=ATAQUES,
        extras=(valida_asa_dobrada_e_aberta, valida_arremetida_cobre_a_mordida))
