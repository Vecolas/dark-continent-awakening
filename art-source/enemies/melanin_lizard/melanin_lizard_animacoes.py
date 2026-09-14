"""Clipes do Melanin Lizard -- a representacao das fases que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, AGARRAO OU CAMUFLAGEM. O servidor publica a
fase, o cambaleio e o estado de camuflagem; o cliente escolhe o clipe. Se a
animacao e a regra discordarem, quem esta errado e este arquivo.

O CLIPE QUE NAO SE MEXE E O MAIS IMPORTANTE DESTE BICHO. `camouflage` e uma pose
ESTATICA de propria vontade: corpo prensado no chao, pernas abertas, nada
oscilando. Enquanto ele esta camuflado o servidor recusa-o como alvo, e a unica
coisa que sustenta essa recusa na tela e o bicho parecer pedra. Um "ocio
discreto" ali -- uma respiracao de meio grau -- nao daria erro nenhum e faria a
regra parecer trapaca, porque o jogador veria um animal vivo que ele nao
consegue mirar. A validacao `valida_camuflagem_imovel` reprova qualquer
movimento nesse clipe.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com o campo de origem ao lado. A
regua da biblioteca reprova se a soma dos tres clipes encadeados for menor que
windup + active + recovery.

EIXOS -- herdados da tabela CONFERIDA EM JOGO no foxbear, que e quadrupede como
este e usa a mesma convencao (y=0 no chao, -Z a FRENTE, +X a ESQUERDA):

    rotacao X positiva  -> a massa em -Z (peito, cabeca) SOBE; a cauda desce;
                           perna pendurada balanca para a FRENTE
    rotacao X negativa  -> o peito baixa; a MANDIBULA ABRE, porque a massa dela
                           esta a frente da propria dobradica (pivot em z=-5)
    rotacao Z positiva  -> o lado esquerdo (+X) SOBE; num membro pendurado, ele
                           abre para FORA no lado esquerdo
    rotacao Y positiva  -> o que esta em +X gira para a FRENTE (-Z)
    posicao  -Z         -> projeta para a FRENTE;  posicao -Y -> AGACHA

Isto e ponto cego declarado: X e Z foram confirmados em jogo no foxbear, Y e
deduzido pela mesma mao. Se em jogo a cauda varrer para o lado errado, o sinal
esta num lugar so.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/melanin_lizard/melanin_lizard_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/melanin_lizard.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402

MOB = "melanin_lizard"

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType
# do Java VENCE o JSON. Esta e tambem a UNICA lista de clipes.
LOOPS = {
    "idle": True,
    "walk": True,
    # A pedra. Ver o cabecalho: ela repete porque tem de durar o tempo que o
    # jogador levar para passar ao lado, e ela nao muda nada entre as voltas --
    # que e o unico jeito de um loop de pose estatica nao dar tranco.
    "camouflage": True,
    # hold_on_last_frame: o telegrafo termina com a boca ABERTA e fica assim.
    # Voltando ao repouso no fim do clipe, o lagarto fecharia a boca na tela
    # enquanto o servidor ainda esta em WINDUP -- o jogador leria "passou" e
    # seria agarrado mesmo assim.
    "windup": "hold_on_last_frame",
    "strike": False,
    # RECOVERY repete de proposito, e e o unico clipe de fase que repete. Ele e a
    # pose de quem esta SEGURANDO alguem: o servidor publica RECOVERY durante o
    # agarrao inteiro, que dura ate MelaninLizardTuning.AGARRAO.ticksMaximos
    # (80 ticks) -- quatro vezes a recuperacao de um bote no vazio. Sem repetir,
    # o lagarto congelaria com a vitima na boca depois de um segundo.
    "recovery": True,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de MelaninLizardTuning, com o campo ao lado.
ATAQUES = {("windup", "strike", "recovery"): anim.Ataque(
    windup=12,    # MelaninLizardTuning.BOTE_WINDUP_TICKS
    active=5,     # MelaninLizardTuning.BOTE_ACTIVE_TICKS
    recovery=18)}  # MelaninLizardTuning.BOTE_RECOVERY_TICKS

DUR_IDLE = 3.0
DUR_WALK = 0.8
DUR_CAMUFLAGEM = 2.0
DUR_WINDUP = 0.6     # 12 ticks
DUR_STRIKE = 0.25    # 5 ticks
DUR_RECOVERY = 1.0   # 20 ticks: o servidor gasta 18, e a sobra e do servidor cortar
DUR_STAGGER = 0.5
DUR_DEATH = 1.2

PERNAS_ESQ = ("leg_front_left", "leg_back_left")
PERNAS_DIR = ("leg_front_right", "leg_back_right")
PATA_DA_PERNA = {
    "leg_front_left": "foot_front_left",
    "leg_front_right": "foot_front_right",
    "leg_back_left": "foot_back_left",
    "leg_back_right": "foot_back_right",
}

# Quanto a boca tem de estar aberta no fim do WINDUP, em graus (o angulo e
# negativo porque X negativo abre). Abaixo disso o telegrafo deixa de existir:
# este bicho nao tira vida com o bote, ele tira CONTROLE, e a unica coisa que o
# jogador tem para ler antes de perder o controle e a boca abrindo.
ABERTURA_MINIMA_NO_WINDUP = 30.0


def _patas_seguem_as_pernas(bones, fator, eixos=(0,)):
    """A pata contra-gira a perna para continuar espalmada no chao.

    Escrita a mao, ela fica com a fase certa hoje e errada na primeira correcao
    da perna -- e a correcao nao da erro: da um pe que arrasta meio quadro atras
    da canela.
    """
    for perna, pata in PATA_DA_PERNA.items():
        if perna in bones and "rotation" in bones[perna]:
            anim.derivar(bones, perna, pata, "rotation", fator, eixos=eixos)


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # ---------------------------------------------------------------- ocio
    # Ele respira e VARRE a cabeca devagar. A amplitude e pequena (1 grau no
    # tronco) porque ocio chamativo faz o jogador ignorar o telegrafo -- e neste
    # bicho o telegrafo e a unica defesa contra perder o controle do personagem.
    ocio = {}
    anim.curva(ocio, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 1.0)])
    anim.curva(ocio, "head", "rotation",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 6.0, fase=0.25)])
    # A boca entreabre e fecha na respiracao: base negativa mantem a fenda visivel
    # o tempo todo, que e o que lembra ao jogador que este bicho morde.
    anim.curva(ocio, "jaw", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 1.5, base=-1.5)])
    anim.curva(ocio, "tail_base", "rotation",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 5.0, fase=0.5)])
    anim.derivar(ocio, "tail_base", "tail_tip", "rotation", 1.4, eixos=(1,))
    # As placas do dorso acompanham o folego com atraso -- e o que impede a
    # crista de ler como um bloco colado nas costas.
    anim.derivar(ocio, "body", "crest", "rotation", 0.4)
    a.clipe("idle", DUR_IDLE, ocio)

    # ------------------------------------------------------------ locomocao
    # Marcha SPRAWLING: o tronco ondula em Y e as patas se alternam na DIAGONAL
    # (frente-esquerda com tras-direita). Um quadrupede de pernas sob o corpo
    # nao ondula; um lagarto so anda ondulando, e e por isso que o corpo escreve
    # Y aqui e o pescoco contra-gira para a cabeca continuar apontando em frente.
    marcha = {}
    anim.curva(marcha, "body", "rotation",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 5.0)])
    anim.curva(marcha, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 0.4)])
    anim.derivar(marcha, "body", "head", "rotation", -0.6, eixos=(1,))
    anim.derivar(marcha, "body", "tail_base", "rotation", -1.5, eixos=(1,))
    anim.derivar(marcha, "tail_base", "tail_tip", "rotation", 1.4, eixos=(1,))
    anim.curva(marcha, "leg_front_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 26.0)])
    anim.curva(marcha, "leg_front_right", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 26.0, fase=0.5)])
    anim.derivar(marcha, "leg_front_left", "leg_back_right", "rotation", 1.0)
    anim.derivar(marcha, "leg_front_right", "leg_back_left", "rotation", 1.0)
    _patas_seguem_as_pernas(marcha, -0.55)
    a.clipe("walk", DUR_WALK, marcha)

    # ----------------------------------------------------------- camuflagem
    # A PEDRA. Duas chaves com o MESMO valor: a pose entra pela transicao do
    # GeckoLib e depois nao muda mais. Ver o cabecalho para o porque de nao
    # haver respiracao aqui.
    pedra = {}
    for instante in (0.0, DUR_CAMUFLAGEM):
        # Prensado contra o chao: 1 px de agachamento num bicho de 12.8 px de
        # hitbox e o suficiente para a barriga encostar e as pernas sumirem na
        # silhueta vista de lado.
        anim.curva(pedra, "body", "position", [(instante, anim.vetor(y=-1.0))])
        for perna in PERNAS_ESQ:
            anim.curva(pedra, perna, "rotation", [(instante, anim.vetor(z=24.0))])
        for perna in PERNAS_DIR:
            anim.curva(pedra, perna, "rotation", [(instante, anim.vetor(z=-24.0))])
    # As patas contra-giram por inteiro: aberta a perna, o pe continua plano no
    # chao. Sem isso o bicho parado apoia a silhueta na QUINA das patas, e a
    # leitura de "pedra assentada" vira "bicho de pontinha de pe".
    _patas_seguem_as_pernas(pedra, -1.0, eixos=(2,))
    a.clipe("camouflage", DUR_CAMUFLAGEM, pedra)

    # -------------------------------------------------------------- windup
    # O AVISO. O peito SOBE, a boca ABRE e as dianteiras se armam. O gesto muda a
    # silhueta de lugar e nao so de forma -- um lagarto rente ao chao que de
    # repente tem altura e a unica coisa que le a distancia.
    aviso = {}
    anim.curva(aviso, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=16))])
    anim.curva(aviso, "body", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=1.0))])
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=10))])
    anim.curva(aviso, "jaw", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.6, anim.vetor(x=-34)),
                (DUR_WINDUP, anim.vetor(x=-46))])
    anim.curva(aviso, "crest", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-7))])
    anim.curva(aviso, "tail_base", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-9))])
    anim.derivar(aviso, "tail_base", "tail_tip", "rotation", 1.2)
    for perna in ("leg_front_left", "leg_front_right"):
        anim.curva(aviso, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=18))])
    _patas_seguem_as_pernas(aviso, -0.6)
    a.clipe("windup", DUR_WINDUP, aviso)

    # -------------------------------------------------------------- strike
    # O BOTE. Curto e com exagero: o corpo passa do ponto para a frente e a boca
    # fecha PASSANDO de fechada. O exagero e o que separa visualmente o golpe do
    # telegrafo -- sem ele os dois clipes parecem o mesmo movimento mais lento.
    bote = {}
    anim.curva(bote, "body", "rotation",
               [(0.0, anim.vetor(x=16)), (DUR_STRIKE * 0.6, anim.vetor(x=-12)),
                (DUR_STRIKE, anim.vetor(x=-6))])
    anim.curva(bote, "body", "position",
               [(0.0, anim.vetor(y=1.0)), (DUR_STRIKE * 0.6, anim.vetor(y=-0.5, z=-2.0)),
                (DUR_STRIKE, anim.vetor(y=0.0, z=-1.2))])
    anim.curva(bote, "head", "rotation",
               [(0.0, anim.vetor(x=10)), (DUR_STRIKE * 0.6, anim.vetor(x=-14)),
                (DUR_STRIKE, anim.vetor(x=-8))])
    anim.curva(bote, "jaw", "rotation",
               [(0.0, anim.vetor(x=-46)), (DUR_STRIKE * 0.6, anim.vetor(x=3)),
                (DUR_STRIKE, anim.vetor())])
    anim.curva(bote, "crest", "rotation",
               [(0.0, anim.vetor(x=-7)), (DUR_STRIKE, anim.vetor(x=2))])
    anim.curva(bote, "tail_base", "rotation",
               [(0.0, anim.vetor(x=-9)), (DUR_STRIKE * 0.6, anim.vetor(x=10)),
                (DUR_STRIKE, anim.vetor(x=4))])
    anim.derivar(bote, "tail_base", "tail_tip", "rotation", 1.2)
    for perna in ("leg_front_left", "leg_front_right"):
        anim.curva(bote, perna, "rotation",
                   [(0.0, anim.vetor(x=18)), (DUR_STRIKE * 0.6, anim.vetor(x=-16)),
                    (DUR_STRIKE, anim.vetor(x=-7))])
    _patas_seguem_as_pernas(bote, -0.6)
    a.clipe("strike", DUR_STRIKE, bote)

    # ------------------------------------------------------------ recovery
    # SEGURANDO. Nao e "descansando": e o lagarto de boca travada, tremendo com o
    # peso de quem se debate. Todos os periodos dividem a duracao, entao o clipe
    # fecha e pode repetir pelo agarrao inteiro.
    segurando = {}
    anim.curva(segurando, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_RECOVERY, DUR_RECOVERY / 2.0, 2.5, base=-4.0)])
    anim.curva(segurando, "body", "position",
               [(t, anim.vetor(z=v)) for t, v in
                anim.cossenoide(DUR_RECOVERY, DUR_RECOVERY / 4.0, 0.4, base=-0.8)])
    anim.derivar(segurando, "body", "head", "rotation", 0.6)
    # A boca NAO reabre aqui. Ela aperta: a oscilacao inteira fica no lado
    # fechado (base -1.2, amplitude 1.2), entao o angulo nunca passa de zero. Uma
    # boca que reabre no meio do agarrao diria ao jogador que ele ja esta livre.
    anim.curva(segurando, "jaw", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_RECOVERY, DUR_RECOVERY / 2.0, 1.2, base=-1.2)])
    anim.curva(segurando, "tail_base", "rotation",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_RECOVERY, DUR_RECOVERY, 7.0)])
    anim.derivar(segurando, "tail_base", "tail_tip", "rotation", 1.4, eixos=(1,))
    for perna in ("leg_front_left", "leg_front_right"):
        anim.curva(segurando, perna, "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(DUR_RECOVERY, DUR_RECOVERY / 2.0, 5.0, base=-4.0)])
    _patas_seguem_as_pernas(segurando, -0.5)
    a.clipe("recovery", DUR_RECOVERY, segurando)

    # ------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece. O bicho e jogado de lado e a boca se abre -- e a boca
    # abrindo aqui e a informacao: quem estava agarrado foi solto.
    tropeco = {}
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.10, anim.vetor(x=-8, z=18)),
                (0.28, anim.vetor(x=2, z=-6)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "head", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(y=16)),
                (0.30, anim.vetor(y=-6)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "jaw", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(x=-22)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "tail_base", "rotation",
               [(0.0, anim.vetor()), (0.14, anim.vetor(y=-20)),
                (0.32, anim.vetor(y=8)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "tail_base", "tail_tip", "rotation", 1.3, eixos=(1,))
    anim.derivar(tropeco, "body", "crest", "rotation", -0.5, eixos=(2,))
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # --------------------------------------------------------------- morte
    # hold_on_last_frame: ele fica TOMBADO ate a entidade sumir. Um clipe de
    # morte que volta ao repouso mostra o bicho de pe no ultimo quadro antes de
    # desaparecer, e a leitura vira "ele sumiu", nao "ele morreu".
    #
    # Tombar para o lado, e nao afundar: um lagarto morto de barriga para baixo
    # e indistinguivel de um lagarto camuflado, e a leitura de "matei" e
    # justamente o que a captura NAO-LETAL deste bicho precisa que seja obvia.
    queda = {}
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.35, anim.vetor(z=40)),
                (DUR_DEATH, anim.vetor(z=92))])
    anim.curva(queda, "body", "position",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.35, anim.vetor(y=-1.0)),
                (DUR_DEATH, anim.vetor(y=-2.0))])
    anim.curva(queda, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=-18))])
    anim.curva(queda, "jaw", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=-16)),
                (DUR_DEATH, anim.vetor(x=-13))])
    anim.curva(queda, "crest", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=6))])
    anim.curva(queda, "tail_base", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(y=24))])
    anim.derivar(queda, "tail_base", "tail_tip", "rotation", 1.2, eixos=(1,))
    for perna in PERNAS_ESQ:
        anim.curva(queda, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(z=30))])
    for perna in PERNAS_DIR:
        anim.curva(queda, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(z=-30))])
    _patas_seguem_as_pernas(queda, -0.4, eixos=(2,))
    a.clipe("death", DUR_DEATH, queda)

    return a


# ------------------------------------------------------- validacoes do bicho

def valida_camuflagem_imovel(a):
    """O clipe de camuflagem NAO pode ter movimento nenhum.

    Enquanto camuflado o servidor recusa este bicho como alvo
    (`RegrasDeCamuflagemDeRocha.podeSerAlvo`). A unica coisa que sustenta essa
    recusa na tela e ele parecer pedra. Um gesto de meio grau aqui nao da erro,
    nao reprova nenhum portao de formato e faz a regra virar trapaca: o jogador
    ve um animal vivo que a mira dele atravessa.
    """
    clipe = a.clipes[a.nome_completo("camouflage")]
    for osso, canais in clipe["bones"].items():
        for canal, quadros in canais.items():
            valores = list(quadros.values())
            if any(v != valores[0] for v in valores):
                raise anim.ErroDeArte(
                    "camouflage move '%s'.%s entre %s e %s: enquanto o servidor recusa este bicho "
                    "como alvo, a tela tem de mostrar pedra. Um animal que respira e nao pode ser "
                    "mirado le como trapaca, e nada acusa isso"
                    % (osso, canal, valores[0], valores[-1]))


def valida_boca_aberta_no_fim_do_windup(a):
    """No ultimo quadro do WINDUP a boca tem de estar escancarada.

    O que este bote tira do jogador nao e vida, e CONTROLE: ele prende. O unico
    aviso que existe antes disso e a boca abrindo, e o WINDUP e a unica fase em
    que recuar ainda funciona. Um windup de boca fechada continua durando os 12
    ticks que o servidor cobra -- o relogio fica certo, o dano fica certo, e o
    jogador perde o personagem sem ter tido o que ler.
    """
    clipe = a.clipes[a.nome_completo("windup")]
    quadros = clipe["bones"].get("jaw", {}).get("rotation", {})
    if not quadros:
        raise anim.ErroDeArte(
            "o clipe windup nao mexe na mandibula: o telegrafo deste bicho E a boca abrindo, e sem "
            "ele o agarrao vira perda de controle sem aviso")
    abertura = anim.valor_em(quadros, clipe["animation_length"], anim.vetor())[0]
    if abertura > -ABERTURA_MINIMA_NO_WINDUP:
        raise anim.ErroDeArte(
            "no fim do windup a mandibula esta em %s grau(s) e o minimo legivel e %s (X negativo "
            "abre): o aviso dura os ticks certos e nao mostra nada, e o jogador perde o controle "
            "do personagem sem ter tido o que ler" % (abertura, -ABERTURA_MINIMA_NO_WINDUP))


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(
        ataques=ATAQUES,
        extras=(valida_camuflagem_imovel, valida_boca_aberta_no_fim_do_windup))
