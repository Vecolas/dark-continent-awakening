"""Clipes do Mosquito Officer -- a representacao das posturas que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, DRENO, STAGGER OU CURA. O servidor publica a
fase do ataque e o cambaleio; o cliente escolhe o clipe correspondente. Se a
animacao e a regra discordarem, quem esta errado e este arquivo.

AS ASAS SAO A OUTRA METADE DE UMA REGRA DO SERVIDOR. A entidade nasce com
FlyingMoveControl e FlyingPathNavigation e NUNCA pousa por vontade propria: ela
paira, flanqueia e pica no ar. Um clipe que deixe as asas paradas mostra um bicho
POUSADO enquanto o servidor o trata como voando -- e o jogador que le "pousado"
tenta prende-lo no chao, cerca a saida por terra e toma a picada por cima. Isso
nao levanta excecao nenhuma: o voo continua funcionando, a IA continua certa, e
so a leitura quebra. `valida_voo_e_telegrafo` cobra as duas pontas desse acordo --
asa batendo em todo clipe vivo, e asa PARADA na morte, que e o unico momento em
que ela de fato deixa de voar.

O WINDUP E O UNICO AVISO QUE ESTE BICHO DA. Ele dura 14 ticks e termina com
`hold_on_last_frame`: a agulha fica ESTENDIDA na pose final, e e essa pose que o
jogador tem para decidir se recua. Um windup que terminasse com a probocide
recolhida seria um telegrafo que nao mostra nada -- fase certa, duracao certa,
log limpo, e o aviso invisivel. A segunda metade de `valida_voo_e_telegrafo` cobra
exatamente isso.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com a constante de origem ao lado. A
regua da biblioteca reprova se a soma dos tres clipes encadeados for menor que
windup + active + recovery: um clipe curto demais faz o oficial relaxar no meio da
picada que ainda vai acertar -- dano certo, cooldown certo, log limpo, e a unica
leitura que o jogador tem quebrada.

O QUE ESTE ARQUIVO DELIBERADAMENTE NAO MOSTRA: o dreno. A vida dela sobe quando a
picada conecta, e nenhum destes sete clipes incha o abdome para anunciar isso. A
razao e que `recovery` toca IGUAL quando a picada erra -- a fase e do relogio do
ataque, e nao do acerto -- entao um abdome inchando ali mentiria em toda picada
perdida, e mentira de animacao e pior do que silencio: o jogador aprenderia a
contar um dreno que nao aconteceu. O sinal do dreno precisa de um estado que o
servidor publique, e esse estado ainda nao existe.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/mosquito_officer/mosquito_officer_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/mosquito_officer.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402

MOB = "mosquito_officer"

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType do
# Java VENCE o JSON, e um atalho de repeticao do lado de la transformaria este
# dicionario em documentacao que discorda do comportamento.
#
# Esta e tambem a UNICA lista de clipes. Uma lista separada seria a mesma
# informacao escrita duas vezes, e alguem acrescentaria um clipe numa so.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o aviso termina com a agulha ESTENDIDA e fica assim. E
    # a pose que o servidor chama de WINDUP. Voltando ao repouso no fim do clipe,
    # o oficial recolheria a probocide na tela enquanto o servidor ainda esta em
    # WINDUP -- o jogador leria "passou", pararia de recuar e tomaria a picada.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de MosquitoOfficerTuning, com a constante ao lado.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=14,     # WINDUP_DA_PICADA
                       active=4,      # JANELA_DA_PICADA
                       recovery=18)}  # RECUPERACAO_DA_PICADA

DUR_IDLE = 1.2
DUR_WALK = 0.8
DUR_WINDUP = 0.7      # 14 ticks
DUR_STRIKE = 0.2      # 4 ticks
DUR_RECOVERY = 0.9    # 18 ticks
DUR_STAGGER = 0.5
DUR_DEATH = 1.1

ASAS = ("wing_left", "wing_right")
PERNAS = ("leg_left", "leg_right")
BRACOS = ("arm_left", "arm_right")

# Deslocamento MINIMO, em px, que a probocide precisa ter avancado no ultimo
# quadro do windup. LIMITE DE LEITURA e nao botao de balanceamento: abaixo de um
# pixel a extensao nao se distingue do repouso a distancia de combate, e o aviso
# deixa de avisar. Ele e cobrado por valida_voo_e_telegrafo.
AVANCO_MINIMO_DA_AGULHA_NO_AVISO = 1.0


def _bater_asas(bones, duracao, periodo, amplitude):
    """A batida, escrita uma vez.

    A asa direita e DERIVADA da esquerda, e nao escrita a mao. Escrita a mao ela
    fica com a fase certa hoje e errada na primeira correcao, e a correcao nao da
    erro -- da um bicho batendo uma asa de cada vez, que le como asa quebrada.
    """
    anim.curva(bones, "wing_left", "rotation",
               [(t, anim.vetor(z=v)) for t, v in
                anim.cossenoide(duracao, periodo, amplitude, amostras=4)])
    anim.derivar(bones, "wing_left", "wing_right", "rotation", -1.0, eixos=(2,))
    return bones


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # ---------------------------------------------------------------- ocio
    # PAIRANDO. Ela nao fica parada: fica SUSPENSA. O corpo sobe e desce meio
    # pixel enquanto as asas batem seis vezes por segundo na tela -- rapido o
    # bastante para ler como zumbido, lento o bastante para o olho ver que ha asa.
    #
    # A amplitude do corpo e minuscula de proposito: o ocio nao pode competir com
    # o windup. Ocio chamativo faz o jogador ignorar o telegrafo, e o telegrafo e
    # a unica janela que este encontro oferece.
    ocio = {}
    anim.curva(ocio, "thorax", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, 0.6, 0.5)])
    anim.curva(ocio, "abdomen", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 3.0, fase=0.15)])
    anim.curva(ocio, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.5, fase=0.3)])
    _bater_asas(ocio, DUR_IDLE, 0.2, 34.0)
    # As pernas PENDEM. Sao elas que dizem que o bicho esta no ar e nao apoiado:
    # perna rigida em pose de pouso faria a silhueta parada ler como pousada, que
    # e exatamente o que as asas estao ali para desmentir.
    for i, perna in enumerate(PERNAS):
        anim.curva(ocio, perna, "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(DUR_IDLE, DUR_IDLE, 4.0, base=6.0, fase=0.12 * i)])
    anim.curva(ocio, "arm_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 5.0, base=-8.0, fase=0.2)])
    anim.derivar(ocio, "arm_left", "arm_right", "rotation", 1.0)
    a.clipe("idle", DUR_IDLE, ocio)

    # ------------------------------------------------------------- locomocao
    # CRUZEIRO. O clipe de "caminhada" deste bicho e voo em deslocamento: o torax
    # INCLINA para a frente e as asas batem mais forte. Reaproveitar a pose de
    # ocio aqui nao daria erro -- daria um mob que atravessa o terreno na postura
    # de quem esta parado, e a leitura de "ela esta vindo" se perderia.
    cruzeiro = {}
    anim.curva(cruzeiro, "thorax", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 3.0, base=-10.0)])
    anim.curva(cruzeiro, "thorax", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_WALK, 0.4, 0.6)])
    anim.curva(cruzeiro, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 2.0, base=6.0, fase=0.25)])
    anim.curva(cruzeiro, "abdomen", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 4.0, base=8.0, fase=0.4)])
    _bater_asas(cruzeiro, DUR_WALK, 0.16, 42.0)
    for i, perna in enumerate(PERNAS):
        anim.curva(cruzeiro, perna, "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(DUR_WALK, DUR_WALK, 7.0, base=18.0, fase=0.2 * i)])
    anim.curva(cruzeiro, "arm_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 4.0, base=-16.0)])
    anim.derivar(cruzeiro, "arm_left", "arm_right", "rotation", 1.0)
    a.clipe("walk", DUR_WALK, cruzeiro)

    # --------------------------------------------------------------- windup
    # O AVISO, e ele e A JANELA. Setecentos milissegundos: o corpo RECUA meio
    # pixel, a cabeca baixa, as pernas recolhem e a agulha AVANCA. O recuo existe
    # para o avanco do strike ter de onde sair -- sem ele o golpe comeca ja no
    # lugar onde termina, e o quadro de impacto some.
    #
    # O sinal de z NEGATIVO e a frente na GEOMETRIA (convencao Bedrock), e por
    # isso o recuo e z positivo e o avanco e z negativo. Trocar os dois nao da
    # erro: da um oficial que ataca para tras.
    aviso = {}
    anim.curva(aviso, "thorax", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.6, anim.vetor(x=-12)),
                (DUR_WINDUP, anim.vetor(x=-16))])
    anim.curva(aviso, "thorax", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=0.6, z=0.8))])
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.5, anim.vetor(x=12)),
                (DUR_WINDUP, anim.vetor(x=20))])
    # A AGULHA. Ela avanca 1.8 px e cai 8 graus: e o unico sinal que o jogador
    # recebe, e ele precisa sobreviver a um quadro congelado.
    anim.curva(aviso, "proboscis", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.45, anim.vetor(z=-0.7)),
                (DUR_WINDUP, anim.vetor(z=-1.8))])
    anim.curva(aviso, "proboscis", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=8))])
    anim.curva(aviso, "abdomen", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=14))])
    _bater_asas(aviso, DUR_WINDUP, 0.14, 46.0)
    for perna in PERNAS:
        anim.curva(aviso, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-26))])
    anim.curva(aviso, "arm_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=18))])
    anim.derivar(aviso, "arm_left", "arm_right", "rotation", 1.0)
    a.clipe("windup", DUR_WINDUP, aviso)

    # --------------------------------------------------------------- strike
    # A JANELA QUE MACHUCA, e ela e MUITO curta: quatro ticks. O corpo inteiro
    # mergulha para a frente e a agulha crava. O deslocamento e pequeno em
    # numeros porque quem avanca de verdade e a entidade; repetir o avanco aqui
    # somaria dois movimentos e o modelo sairia da propria caixa de colisao.
    picada = {}
    anim.curva(picada, "thorax", "rotation",
               [(0.0, anim.vetor(x=-16)), (DUR_STRIKE * 0.5, anim.vetor(x=10)),
                (DUR_STRIKE, anim.vetor(x=6))])
    anim.curva(picada, "thorax", "position",
               [(0.0, anim.vetor(y=0.6, z=0.8)), (DUR_STRIKE * 0.5, anim.vetor(z=-2.2)),
                (DUR_STRIKE, anim.vetor(z=-1.8))])
    anim.curva(picada, "head", "rotation",
               [(0.0, anim.vetor(x=20)), (DUR_STRIKE, anim.vetor(x=28))])
    anim.curva(picada, "proboscis", "position",
               [(0.0, anim.vetor(z=-1.8)), (DUR_STRIKE * 0.4, anim.vetor(z=-3.5)),
                (DUR_STRIKE, anim.vetor(z=-3.2))])
    anim.curva(picada, "proboscis", "rotation",
               [(0.0, anim.vetor(x=8)), (DUR_STRIKE, anim.vetor(x=2))])
    anim.curva(picada, "abdomen", "rotation",
               [(0.0, anim.vetor(x=14)), (DUR_STRIKE, anim.vetor(x=-4))])
    _bater_asas(picada, DUR_STRIKE, 0.1, 30.0)
    for perna in PERNAS:
        anim.curva(picada, perna, "rotation",
                   [(0.0, anim.vetor(x=-26)), (DUR_STRIKE, anim.vetor(x=-6))])
    anim.curva(picada, "arm_left", "rotation",
               [(0.0, anim.vetor(x=18)), (DUR_STRIKE, anim.vetor(x=34))])
    anim.derivar(picada, "arm_left", "arm_right", "rotation", 1.0)
    a.clipe("strike", DUR_STRIKE, picada)

    # ------------------------------------------------------------- recovery
    # A JANELA DE RESPOSTA, e ela e a mais longa das tres: dezoito ticks. Este
    # oficial tem armadura 2 e 55 de vida, e a recuperacao e onde o jogador cobra
    # isso. Encurta-la aqui nao mudaria o servidor -- mudaria so a leitura, e o
    # jogador acharia que foi punido sem janela.
    volta = {}
    anim.curva(volta, "thorax", "rotation",
               [(0.0, anim.vetor(x=6)), (DUR_RECOVERY * 0.4, anim.vetor(x=-6)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "thorax", "position",
               [(0.0, anim.vetor(z=-1.8)), (DUR_RECOVERY * 0.5, anim.vetor(z=0.6)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "head", "rotation",
               [(0.0, anim.vetor(x=28)), (DUR_RECOVERY * 0.5, anim.vetor(x=-6)),
                (DUR_RECOVERY, anim.vetor())])
    # A agulha VOLTA ao repouso. Ela tem de voltar: presa estendida, a silhueta
    # ficaria no telegrafo para sempre e o aviso deixaria de significar aviso.
    anim.curva(volta, "proboscis", "position",
               [(0.0, anim.vetor(z=-3.2)), (DUR_RECOVERY * 0.6, anim.vetor(z=-0.6)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "proboscis", "rotation",
               [(0.0, anim.vetor(x=2)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "abdomen", "rotation",
               [(0.0, anim.vetor(x=-4)), (DUR_RECOVERY * 0.45, anim.vetor(x=10)),
                (DUR_RECOVERY, anim.vetor())])
    _bater_asas(volta, DUR_RECOVERY, 0.18, 36.0)
    for i, perna in enumerate(PERNAS):
        anim.curva(volta, perna, "rotation",
                   [(0.0, anim.vetor(x=-6)),
                    (DUR_RECOVERY * (0.4 + 0.1 * i), anim.vetor(x=16)),
                    (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "arm_left", "rotation",
               [(0.0, anim.vetor(x=34)), (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "arm_left", "arm_right", "rotation", 1.0)
    a.clipe("recovery", DUR_RECOVERY, volta)

    # -------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece. Num bicho que voa ela e uma PERDA DE ALTURA: o corpo cai,
    # rola de lado e as asas FALHAM -- batem fora de compasso e voltam.
    #
    # A falha e escrita a mao, e nao com cossenoide, de proposito: uma batida
    # regular com amplitude menor leria como "ela desacelerou", e desacelerar e
    # outra coisa. O que precisa aparecer aqui e descompasso.
    tropeco = {}
    anim.curva(tropeco, "thorax", "rotation",
               [(0.0, anim.vetor()), (0.1, anim.vetor(z=-22, x=8)),
                (0.26, anim.vetor(z=14, x=-4)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "thorax", "position",
               [(0.0, anim.vetor()), (0.14, anim.vetor(y=-1.6)),
                (0.34, anim.vetor(y=-0.6)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "head", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(z=-26)),
                (0.3, anim.vetor(z=16)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "abdomen", "rotation",
               [(0.0, anim.vetor()), (0.16, anim.vetor(x=-18)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "wing_left", "rotation",
               [(0.0, anim.vetor()), (0.08, anim.vetor(z=-38)),
                (0.18, anim.vetor(z=6)), (0.3, anim.vetor(z=-30)),
                (0.4, anim.vetor(z=10)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "wing_left", "wing_right", "rotation", -1.0, eixos=(2,))
    for i, perna in enumerate(PERNAS):
        anim.curva(tropeco, perna, "rotation",
                   [(0.0, anim.vetor()), (0.12 + 0.04 * i, anim.vetor(x=-30)),
                    (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "arm_left", "rotation",
               [(0.0, anim.vetor()), (0.14, anim.vetor(x=40)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "arm_left", "arm_right", "rotation", 1.0)
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # ---------------------------------------------------------------- morte
    # hold_on_last_frame: ela termina CAIDA DE LADO, com as pernas recolhidas --
    # a pose de inseto morto que todo mundo reconhece. Um clipe de morte que
    # voltasse ao repouso mostraria o bicho pairando no ultimo quadro antes de
    # sumir, e a leitura viraria "ele fugiu" em vez de "ele morreu".
    #
    # AS ASAS NAO APARECEM AQUI, e a ausencia e a regra. Este e o unico clipe em
    # que ela deixa de voar, e e a unica diferenca inequivoca entre "caiu" e
    # "esta baixando para picar". Asa batendo num cadaver nao levanta erro
    # nenhum: faz o jogador continuar recuando de um bicho que ja morreu.
    queda = {}
    anim.curva(queda, "thorax", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(z=26, x=14)),
                (DUR_DEATH * 0.7, anim.vetor(z=62, x=6)),
                (DUR_DEATH, anim.vetor(z=78))])
    anim.curva(queda, "thorax", "position",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.35, anim.vetor(y=-1.0)),
                (DUR_DEATH, anim.vetor(y=-5.0))])
    anim.curva(queda, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=-20))])
    anim.curva(queda, "proboscis", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=22))])
    anim.curva(queda, "abdomen", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=30))])
    for i, perna in enumerate(PERNAS):
        anim.curva(queda, perna, "rotation",
                   [(0.0, anim.vetor()),
                    (DUR_DEATH * (0.4 + 0.08 * i), anim.vetor(x=-24)),
                    (DUR_DEATH, anim.vetor(x=58))])
    anim.curva(queda, "arm_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=64))])
    anim.derivar(queda, "arm_left", "arm_right", "rotation", 1.0)
    a.clipe("death", DUR_DEATH, queda)

    return a


def valida_voo_e_telegrafo(clipes_do_mob):
    """As asas batem enquanto ela vive, e a agulha avanca no aviso.

    ESTA REGUA E O CONTRATO ENTRE A ARTE E A REGRA DO SERVIDOR, e as duas metades
    dela pegam defeitos que nao levantam excecao nenhuma:

      * A ENTIDADE VOA. Ela e construida com FlyingMoveControl e
        FlyingPathNavigation e nao tem clipe de pouso: todo estado vivo acontece
        no ar. Um clipe que esqueca as asas mostra um bicho pousado enquanto o
        servidor o trata como voando, e o jogador que le "pousado" cerca o chao e
        toma a picada por cima. A regua morde dos DOIS lados: `death` e o unico
        clipe em que as asas tem de estar PARADAS, porque cair e a unica coisa que
        distingue um cadaver de um oficial baixando para picar;

      * O WINDUP E O UNICO AVISO. Ele dura os 14 ticks de
        `MosquitoOfficerTuning.WINDUP_DA_PICADA` e termina com
        `hold_on_last_frame`, entao a pose FINAL dele e o que o jogador ve
        congelado enquanto decide se recua. Se a probocide terminar recolhida, o
        aviso existe no relogio do servidor e nao existe na tela -- fase certa,
        duracao certa, log limpo, e o telegrafo invisivel.
    """
    vivos = [c for c in LOOPS if c != "death"]
    for nome in vivos:
        clipe = clipes_do_mob.clipes[clipes_do_mob.nome_completo(nome)]
        paradas = [asa for asa in ASAS if asa not in clipe["bones"]]
        if paradas:
            raise anim.ErroDeArte(
                "o clipe '%s' nao move %s: a entidade voa com FlyingMoveControl e nunca pousa, "
                "entao a tela mostraria um bicho pousado enquanto o servidor o trata como no ar -- "
                "e quem le 'pousado' cerca o chao e toma a picada por cima" % (nome, paradas))

    morte = clipes_do_mob.clipes[clipes_do_mob.nome_completo("death")]
    batendo = [asa for asa in ASAS if asa in morte["bones"]]
    if batendo:
        raise anim.ErroDeArte(
            "o clipe 'death' move %s: asa batendo num cadaver e a unica coisa que faz um oficial "
            "morto parecer um oficial baixando para picar, e o jogador continua recuando de algo "
            "que ja morreu" % batendo)

    aviso = clipes_do_mob.clipes[clipes_do_mob.nome_completo("windup")]
    fim = aviso["animation_length"]
    agulha = anim.valor_em(aviso["bones"].get("proboscis", {}).get("position", {}),
                           fim, anim.vetor())
    # -Z e a frente na geometria Bedrock: avancar e ficar MAIS NEGATIVO.
    if agulha[2] > -AVANCO_MINIMO_DA_AGULHA_NO_AVISO:
        raise anim.ErroDeArte(
            "o windup termina com proboscis.position z=%s e o aviso exige pelo menos %.1f px de "
            "avanco: como o clipe segura o ultimo quadro, essa e a pose que o jogador ve enquanto "
            "decide se recua -- agulha recolhida e um telegrafo que nao mostra nada"
            % (agulha[2], AVANCO_MINIMO_DA_AGULHA_NO_AVISO))


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(ataques=ATAQUES, extras=(valida_voo_e_telegrafo,))
