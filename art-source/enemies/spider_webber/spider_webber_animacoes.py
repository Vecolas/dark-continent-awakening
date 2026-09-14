"""Clipes da Spider Webber -- a representacao das posturas que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, IMOBILIZACAO, STAGGER OU SOLTURA. O servidor
publica a fase do ataque e o cambaleio; o cliente escolhe o clipe correspondente.
Se a animacao e a regra discordarem, quem esta errado e este arquivo.

O CLIPE `windup` NAO E DECORACAO -- ELE E A METADE VISIVEL DE UMA REGRA.
SpiderWebberTuning gasta 30 ticks de aviso antes de a teia existir, e esse aviso
e a UNICA chance que o jogador tem de sair da area ou de fechar a distancia para
dentro da zona morta. Se o clipe nao ERGUER o abdome, o servidor continua
avisando e a tela nao: a teia parece sair do nada, o telegrafo de um segundo e
meio vira um numero que so o servidor conhece, e o relato que chega e "essa
aranha prende sem avisar". Nao ha excecao, nao ha log.
`valida_telegrafo_da_fiandeira` cobra os tres lados desse acordo -- o abdome
sobe, a pose SEGURA ate o servidor mandar, e o disparo COMECA de onde o aviso
terminou.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com a constante de origem ao lado. A
regua da biblioteca reprova se a soma dos tres clipes encadeados for menor que
windup + active + recovery: um clipe curto demais faz a aranha relaxar no meio do
lancamento que ainda vai prender -- dano certo, cooldown certo, log limpo, e a
unica leitura que o jogador tem quebrada.

SOBRE O SINAL DO GIRO. No formato Bedrock, rotacao x NEGATIVA levanta o que esta
em -Z (e assim que o besouro empina). O abdome desta aranha mora em +Z, entao
quem o levanta e o x POSITIVO. Trocar o sinal aqui nao da erro: enfia o abdome no
chao exatamente no quadro que deveria ser o aviso.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/spider_webber/spider_webber_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/spider_webber.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402

MOB = "spider_webber"

# ------------------------------------------------------------------ posturas
#
# Estes angulos nao sao balanceamento: sao a POSE, e a pose e o contrato com o
# telegrafo do servidor.

# Quanto o abdome sobe no aviso, em graus. E a pose que o servidor chama de
# WINDUP, e e a unica coisa que o jogador ve antes da teia.
ABDOME_NO_AVISO = 55.0
# A fiandeira abre junto: ela e a peca que produz o fio, e o quadro em que ela
# escancara e o que separa "a aranha esta reparando em mim" de "a aranha vai
# atirar".
FIANDEIRA_NO_AVISO = 24.0
# O disparo: o abdome chicoteia para a frente e a fiandeira fecha.
ABDOME_NO_DISPARO = 34.0
FIANDEIRA_NO_DISPARO = -10.0

# Giro minimo, em graus, que o aviso PRECISA ter para ser lido de longe.
#
# Limite de LEITURA com conta atras: a aranha tem 1.3 bloco de altura e o abdome
# ocupa 8 dos 19 px do modelo. Abaixo de uns quarenta graus o deslocamento da
# ponta do abdome fica menor que a espessura da propria peca, e a distancia de
# combate -- de tres a nove blocos -- isso nao e um movimento, e um tremor.
GIRO_MINIMO_DO_AVISO = 40.0

# A faixa em que este mob opera, em blocos. COPIADA do servidor, com o nome da
# constante ao lado -- e a mesma duplicacao declarada que o orcamento de ataque
# usa, e existe pela mesma razao: a recusa precisa dizer a distancia em que o
# defeito aparece, e uma mensagem que so acusa nao ensina nada.
# SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA = 3.5D.
# SpiderWebberTuning.ALCANCE_MAXIMO_DA_TEIA = 9.0D.
ALCANCE_MINIMO_DA_TEIA_EM_BLOCOS = 3.5
ALCANCE_MAXIMO_DA_TEIA_EM_BLOCOS = 9.0

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType do
# Java VENCE o JSON, e um atalho de repeticao do lado de la transformaria este
# dicionario em documentacao que discorda do comportamento.
#
# Esta e tambem a UNICA lista de clipes. Uma lista separada seria a mesma
# informacao escrita duas vezes, e alguem acrescentaria um clipe em so uma delas.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o aviso termina com o abdome ERGUIDO e fica assim. E a
    # pose que o servidor chama de WINDUP. Voltando ao repouso no fim do clipe, a
    # aranha desarmaria o lancamento na tela enquanto o servidor ainda esta
    # avisando -- o jogador leria "passou", pararia de recuar e levaria a teia.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de SpiderWebberTuning, com a constante ao lado.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=30,     # WINDUP_DA_TEIA
                       active=6,      # JANELA_DA_TEIA
                       recovery=20)}  # RECUPERACAO_DA_TEIA

DUR_IDLE = 3.0
DUR_WALK = 0.8
DUR_WINDUP = 1.5     # 30 ticks
DUR_STRIKE = 0.3     # 6 ticks
DUR_RECOVERY = 1.1   # 22 ticks, uma folga sobre os 20 do servidor
DUR_STAGGER = 0.6
DUR_DEATH = 1.4

# MARCHA DE ARACNIDEO: quatro patas no chao e quatro no ar, alternando em
# diagonal. Escrever as oito em fase daria um bicho saltitando com tudo junto --
# nao da erro, e le como brinquedo de corda em vez de aranha.
GRUPO_A = ("front_left", "third_left", "second_right", "rear_right")
GRUPO_B = ("second_left", "rear_left", "front_right", "third_right")
PATAS = GRUPO_A + GRUPO_B


def perna(sufixo):
    return "leg_" + sufixo


def pe(sufixo):
    return "foot_" + sufixo


def derivar_pes(bones, fator=-0.65):
    """O tarso NUNCA e escrito a mao: ele e derivado do femur.

    Escrito a mao ele fica com a fase certa hoje e errada na primeira correcao da
    perna, e a correcao nao da erro -- da um pe que arrasta meio quadro atras do
    joelho, e o relato que chega e 'o clipe ficou estranho'.
    """
    for sufixo in PATAS:
        anim.derivar(bones, perna(sufixo), pe(sufixo), "rotation", fator)


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # -------------------------------------------------------------- ocio
    # Uma aranha parada nao respira: ela PULSA. O abdome sobe e desce um grau e a
    # fiandeira se abre e fecha devagar. A amplitude e minuscula porque o ocio nao
    # pode competir com o aviso da teia -- ocio chamativo faz o jogador ignorar o
    # telegrafo, e o telegrafo e a janela do encontro.
    ocio = {}
    anim.curva(ocio, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 0.3)])
    anim.curva(ocio, "abdomen", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.0, fase=0.15)])
    # A fiandeira se mexe SOZINHA no ocio, e isso e deliberado: ela e a peca que o
    # jogador precisa aprender a olhar, e uma peca que so se move no aviso passa
    # despercebida justamente na primeira vez.
    anim.curva(ocio, "fiandeira", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE / 2.0, 3.5)])
    anim.curva(ocio, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.5, fase=0.25)])
    for i, sufixo in enumerate(PATAS):
        anim.curva(ocio, perna(sufixo), "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(DUR_IDLE, DUR_IDLE, 1.6, fase=0.1 * i)])
    derivar_pes(ocio)
    a.clipe("idle", DUR_IDLE, ocio)

    # ----------------------------------------------------------- locomocao
    # Os dois grupos andam em CONTRAFASE -- e a marcha alternada de um aracnideo.
    # O corpo balanca em z junto, porque uma aranha andando joga o peso de um lado
    # para o outro; sem esse balanco a marcha le como patinacao.
    marcha = {}
    anim.curva(marcha, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 0.4)])
    anim.curva(marcha, "body", "rotation",
               [(t, anim.vetor(z=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 2.2)])
    anim.curva(marcha, "abdomen", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 3.0, fase=0.2)])
    anim.curva(marcha, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 2.5, fase=0.3)])
    for sufixo in GRUPO_A:
        anim.curva(marcha, perna(sufixo), "rotation",
                   [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 20.0)])
    for sufixo in GRUPO_B:
        anim.curva(marcha, perna(sufixo), "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(DUR_WALK, DUR_WALK, 20.0, fase=0.5)])
    derivar_pes(marcha)
    a.clipe("walk", DUR_WALK, marcha)

    # -------------------------------------------------------------- windup
    # O AVISO, E ELE E A JANELA. Um segundo e meio erguendo o abdome sobre as
    # costas e escancarando a fiandeira. E o unico momento em que a silhueta deixa
    # de ser "aranha parada", e e por isso que ele precisa ser legivel de longe --
    # de tres a nove blocos, que e a faixa inteira em que este mob opera.
    #
    # As quatro patas dianteiras abaixam e as traseiras firmam: ela ANCORA para
    # atirar. Levantar as oito deixaria a aranha flutuando na pose que o encontro
    # inteiro usa.
    aviso = {}
    anim.curva(aviso, "abdomen", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.45, anim.vetor(x=ABDOME_NO_AVISO * 0.7)),
                (DUR_WINDUP, anim.vetor(x=ABDOME_NO_AVISO))])
    anim.curva(aviso, "fiandeira", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.5, anim.vetor(x=FIANDEIRA_NO_AVISO * 0.5)),
                (DUR_WINDUP, anim.vetor(x=FIANDEIRA_NO_AVISO))])
    anim.curva(aviso, "body", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=-0.8))])
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-10))])
    for sufixo in ("front_left", "front_right", "second_left", "second_right"):
        anim.curva(aviso, perna(sufixo), "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=14))])
    for sufixo in ("third_left", "third_right", "rear_left", "rear_right"):
        anim.curva(aviso, perna(sufixo), "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-8))])
    derivar_pes(aviso)
    a.clipe("windup", DUR_WINDUP, aviso)

    # -------------------------------------------------------------- strike
    # A JANELA QUE PRENDE, e ela e curta: seis ticks. O abdome chicoteia para a
    # frente e a fiandeira FECHA -- o fechar e o que separa visualmente o disparo
    # do aviso, e e o quadro que o jogador aprende a reconhecer.
    #
    # Ela COMECA exatamente onde o aviso terminou. Recomecar do repouso faria o
    # abdome despencar e subir de novo entre o ultimo tick do aviso e o primeiro
    # do disparo: um tranco de um quadro que o jogador le como "ela cancelou",
    # justamente no tick em que a teia sai.
    #
    # O corpo nao avanca aqui: a teia e uma AREA resolvida pelo servidor, e nao um
    # pulo. Um avanco no clipe somaria um movimento que o servidor nao fez, e o
    # modelo sairia da propria caixa de colisao.
    disparo = {}
    anim.curva(disparo, "abdomen", "rotation",
               [(0.0, anim.vetor(x=ABDOME_NO_AVISO)),
                (DUR_STRIKE * 0.4, anim.vetor(x=ABDOME_NO_DISPARO - 6)),
                (DUR_STRIKE, anim.vetor(x=ABDOME_NO_DISPARO))])
    anim.curva(disparo, "fiandeira", "rotation",
               [(0.0, anim.vetor(x=FIANDEIRA_NO_AVISO)),
                (DUR_STRIKE * 0.35, anim.vetor(x=FIANDEIRA_NO_DISPARO)),
                (DUR_STRIKE, anim.vetor(x=FIANDEIRA_NO_DISPARO))])
    anim.curva(disparo, "body", "position",
               [(0.0, anim.vetor(y=-0.8)), (DUR_STRIKE * 0.4, anim.vetor(y=0.4)),
                (DUR_STRIKE, anim.vetor(y=0.2))])
    anim.curva(disparo, "head", "rotation",
               [(0.0, anim.vetor(x=-10)), (DUR_STRIKE, anim.vetor(x=6))])
    for sufixo in PATAS:
        base = 14 if sufixo.startswith(("front", "second")) else -8
        anim.curva(disparo, perna(sufixo), "rotation",
                   [(0.0, anim.vetor(x=base)), (DUR_STRIKE, anim.vetor(x=base * 0.3))])
    derivar_pes(disparo)
    a.clipe("strike", DUR_STRIKE, disparo)

    # ------------------------------------------------------------ recovery
    # A JANELA DE RESPOSTA, e ela e longa de proposito: e o tempo em que o jogador
    # fecha a distancia sem risco e entra na zona morta, que e a unica resposta que
    # este encontro oferece. Encurta-la aqui nao mudaria o servidor -- mudaria so a
    # leitura, e o jogador acharia que foi punido sem janela.
    #
    # Ela TERMINA no repouso, porque o clipe seguinte e `idle` ou `walk`: terminar
    # com o abdome meio erguido faria a aranha estalar de volta no primeiro quadro
    # do ocio, e um estalo por ataque e o tipo de defeito que ninguem reporta
    # porque ninguem consegue descrever.
    volta = {}
    anim.curva(volta, "abdomen", "rotation",
               [(0.0, anim.vetor(x=ABDOME_NO_DISPARO)),
                (DUR_RECOVERY * 0.5, anim.vetor(x=-6)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "fiandeira", "rotation",
               [(0.0, anim.vetor(x=FIANDEIRA_NO_DISPARO)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "body", "position",
               [(0.0, anim.vetor(y=0.2)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "head", "rotation",
               [(0.0, anim.vetor(x=6)), (DUR_RECOVERY, anim.vetor())])
    for sufixo in PATAS:
        base = 14 * 0.3 if sufixo.startswith(("front", "second")) else -8 * 0.3
        anim.curva(volta, perna(sufixo), "rotation",
                   [(0.0, anim.vetor(x=base)), (DUR_RECOVERY * 0.45, anim.vetor(x=-base * 0.5)),
                    (DUR_RECOVERY, anim.vetor())])
    derivar_pes(volta)
    a.clipe("recovery", DUR_RECOVERY, volta)

    # ------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece. Ela tambem precisa ser DIFERENTE do aviso: o mesmo golpe
    # que interrompe e o que rasga a teia, e se as duas poses se parecessem o
    # jogador nao saberia qual das duas coisas ele conseguiu.
    #
    # O eixo aqui e z -- ela e jogada de LADO --, e o aviso e todo em x. Dois
    # eixos diferentes e o que torna as duas leituras impossiveis de confundir a
    # distancia.
    tropeco = {}
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(z=-18)), (0.3, anim.vetor(z=11)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "abdomen", "rotation",
               [(0.0, anim.vetor()), (0.14, anim.vetor(z=14)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "head", "rotation",
               [(0.0, anim.vetor()), (0.16, anim.vetor(z=-22)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "fiandeira", "rotation",
               [(0.0, anim.vetor()), (0.18, anim.vetor(x=-14)), (DUR_STAGGER, anim.vetor())])
    for i, sufixo in enumerate(PATAS):
        anim.curva(tropeco, perna(sufixo), "rotation",
                   [(0.0, anim.vetor()), (0.12 + 0.015 * i, anim.vetor(x=-24)),
                    (DUR_STAGGER, anim.vetor())])
    derivar_pes(tropeco)
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # --------------------------------------------------------------- morte
    # hold_on_last_frame: ela termina com as OITO PATAS RECOLHIDAS para dentro --
    # a pose de aranha morta que todo mundo reconhece sem precisar de legenda. Um
    # clipe de morte que volta ao repouso mostraria o bicho de pe no ultimo quadro
    # antes de sumir, e a leitura viraria "ele sumiu" em vez de "ele morreu".
    #
    # O abdome CAI, e cair e o oposto de subir: a ultima coisa que o jogador ve e
    # a fiandeira encostando no chao, que e a promessa de que nao vem mais teia.
    queda = {}
    anim.curva(queda, "body", "position",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.35, anim.vetor(y=-1.5)),
                (DUR_DEATH, anim.vetor(y=-4.0))])
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.4, anim.vetor(z=16)),
                (DUR_DEATH, anim.vetor(z=24))])
    anim.curva(queda, "abdomen", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(x=12)),
                (DUR_DEATH, anim.vetor(x=-18))])
    anim.curva(queda, "fiandeira", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=-22))])
    anim.curva(queda, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=22))])
    for i, sufixo in enumerate(PATAS):
        anim.curva(queda, perna(sufixo), "rotation",
                   [(0.0, anim.vetor()), (DUR_DEATH * (0.3 + 0.03 * i), anim.vetor(x=-30)),
                    (DUR_DEATH, anim.vetor(x=58))])
    # As patas recolhem MAIS que o femur: o fator positivo aqui inverte o sinal do
    # derivado padrao de proposito, e e ele que fecha o tarso para dentro em vez
    # de deixar a pata esticada para cima como um inseto de brinquedo.
    derivar_pes(queda, fator=0.8)
    a.clipe("death", DUR_DEATH, queda)

    return a


def valida_telegrafo_da_fiandeira(clipes_do_mob):
    """O aviso tem de ERGUER o abdome, SEGURAR a pose e EMENDAR no disparo.

    ESTA REGUA E O CONTRATO ENTRE A ARTE E A REGRA DO SERVIDOR, e as tres metades
    dela pegam defeitos que nao levantam excecao nenhuma:

      * `SpiderWebberTuning.WINDUP_DA_TEIA` gasta 30 ticks avisando, e esse aviso
        e a unica janela em que o jogador pode sair da area ou entrar na zona
        morta. Um clipe que nao erga o abdome deixa o servidor avisando e a tela
        muda: a teia parece sair do nada, e o telegrafo vira um numero interno;

      * o loop tem de ser `hold_on_last_frame`. Repetindo, a aranha abaixaria e
        levantaria o abdome durante o aviso, e o jogador leria "passou" no meio do
        vale -- pararia de recuar e levaria a teia;

      * `strike` tem de COMECAR na pose em que `windup` terminou. Comecando do
        repouso, o abdome despenca e sobe de novo entre o ultimo tick do aviso e o
        primeiro do disparo: um tranco de um quadro, no tick exato em que a teia
        sai, que se le como cancelamento.

    A quarta cobranca e a emenda do outro lado: `recovery` tem de terminar no
    repouso, porque o clipe seguinte e `idle` ou `walk`. Terminar com o abdome
    meio erguido produz um estalo por ataque -- o tipo de defeito que ninguem
    reporta porque ninguem consegue descrever.
    """
    if LOOPS["windup"] != "hold_on_last_frame":
        raise anim.ErroDeArte(
            "o clipe 'windup' esta declarado como %r e precisa ser 'hold_on_last_frame': "
            "repetindo, o abdome desce e sobe durante o aviso, e o jogador le 'passou' no meio "
            "do vale -- para de recuar e leva a teia" % (LOOPS["windup"],))

    aviso = clipes_do_mob.clipes[clipes_do_mob.nome_completo("windup")]
    quadros = aviso["bones"].get("abdomen", {}).get("rotation", {})
    if not quadros:
        raise anim.ErroDeArte(
            "o clipe 'windup' nao gira 'abdomen': o servidor gasta 30 ticks avisando e a tela nao "
            "muda nada. A teia parece sair do nada, e o unico telegrafo do mob vira um numero que "
            "so o servidor conhece")

    fim_do_aviso = anim.valor_em(quadros, aviso["animation_length"], anim.vetor())
    if fim_do_aviso[0] < GIRO_MINIMO_DO_AVISO:
        raise anim.ErroDeArte(
            "'windup' termina com abdomen.rotation x=%s e o minimo legivel e %s graus: de %.1f a "
            "%.1f blocos, que e a faixa inteira em que este mob opera, um giro menor que esse nao "
            "e um movimento -- e um tremor"
            % (fim_do_aviso[0], GIRO_MINIMO_DO_AVISO, ALCANCE_MINIMO_DA_TEIA_EM_BLOCOS,
               ALCANCE_MAXIMO_DA_TEIA_EM_BLOCOS))

    disparo = clipes_do_mob.clipes[clipes_do_mob.nome_completo("strike")]
    for osso in ("abdomen", "fiandeira"):
        pose_no_aviso = anim.valor_em(aviso["bones"].get(osso, {}).get("rotation", {}),
                                      aviso["animation_length"], anim.vetor())
        pose_no_disparo = anim.valor_em(disparo["bones"].get(osso, {}).get("rotation", {}),
                                        0.0, anim.vetor())
        if pose_no_aviso != pose_no_disparo:
            raise anim.ErroDeArte(
                "'windup' termina com %s.rotation em %s e 'strike' comeca em %s: entre o ultimo "
                "tick do aviso e o primeiro do disparo o osso da um tranco de um quadro, no tick "
                "exato em que a teia sai -- e o jogador le isso como cancelamento"
                % (osso, pose_no_aviso, pose_no_disparo))

    recuperacao = clipes_do_mob.clipes[clipes_do_mob.nome_completo("recovery")]
    fim = recuperacao["animation_length"]
    for osso in ("abdomen", "fiandeira"):
        pose = anim.valor_em(recuperacao["bones"].get(osso, {}).get("rotation", {}), fim,
                             anim.vetor())
        if pose != anim.vetor():
            raise anim.ErroDeArte(
                "'recovery' termina com %s.rotation em %s e nao no repouso: o clipe seguinte e "
                "'idle' ou 'walk', e a aranha estala de volta no primeiro quadro do ocio -- um "
                "estalo por ataque, que ninguem consegue descrever" % (osso, pose))


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(ataques=ATAQUES,
                                          extras=(valida_telegrafo_da_fiandeira,))
