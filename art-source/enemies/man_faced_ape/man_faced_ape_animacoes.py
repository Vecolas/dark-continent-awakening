"""Gera as DEZ animacoes do man-faced ape -- DUAS FORMAS, dois arquivos.

DECISAO QUE ESTE ARQUIVO CARREGA (ADR-017): o macaco deixa de vestir aldeao e
piglin vanilla. Esta entrega NAO muda comportamento nenhum -- os tres gametests
que provam o disfarce continuam provando a mesma coisa. O que muda e o corpo, e
com ele o MOVIMENTO, que e a parte do mob que o jogador le antes de ler qualquer
outra.

POR QUE DOIS ARQUIVOS, E NAO UM
--------------------------------
A arquitetura ja decidiu: dois modelos com ids proprios, e nao um modelo com
metade dos ossos escondida. A consequencia para esta lane e direta -- sao dois
conjuntos de ossos DIFERENTES e dois arquivos de animacao, e escrever o clipe de
uma forma contra o geo da outra e o erro facil daqui.

E ele e quase invisivel: {root, body, head, arm_*, leg_*} existe NAS DUAS formas.
Se este gerador so conferisse "o osso existe no geo", animar o humano contra o
geo do primata reprovaria por UM osso (`hood`) e passaria em todo o resto. Por
isso ha `conferir_ossos_exclusivos` (cada forma precisa citar osso que so ela
tem) e `conferir_que_trocar_os_pares_reprova`, que ALIMENTA o portao com os
pares trocados e exige que ele reprove. Portao que nunca reprovou e carimbo.

A COSTURA E O LUGAR PERIGOSO
-----------------------------
O servidor troca de MODELO no instante em que o reveal termina. Nesse quadro o
jogo descarta um esqueleto e monta outro, e nada no GeckoLib confere se as duas
poses combinam: se o humano termina de pe e o primata comeca agachado, o jogador
ve um salto -- e nenhum log diz nada.

Por isso a POSTURA mora aqui uma vez (`postura`), e ela e usada nos dois lados:
e o ultimo quadro de `man_faced_ape_disfarce.reveal` E o primeiro quadro de TODO
clipe de `man_faced_ape`. `conferir_a_costura` reprova se os dois lados
discordarem em qualquer osso comum. E o mesmo cuidado que o sapo tem com
FIM_DO_EMERGE, so que aqui a fronteira e entre dois MODELOS, nao entre dois
clipes.

CONSEQUENCIA QUE NAO E OBVIA: no primata, TODO clipe tem de keyar TODO osso
comum. Osso que um clipe nao cita volta para o default do MODELO (zero), nao
para a postura -- entao um `hurt` que esquecesse a cabeca endireitaria o macaco
por cinco ticks. `postura_base` deita a postura em todos eles antes de o clipe
escrever a sua parte.

TICKS: COPIADOS DO SERVIDOR
----------------------------
Reveal 10 ticks, golpe 8/4/12. Saem de HunterExamProfiles.manFacedApeDisguise()
e .manFacedApeStrike(); se mudarem la, mudam aqui e regera. Um golpe cuja janela
visivel nao coincide com os 4 ticks em que o dano acontece e um mob que MENTE
sobre quando bate -- ver `conferir_a_janela_do_golpe`, que e o portao mais util
deste arquivo.

EIXOS -- CONFERIDOS no proprio geo, nao chutados
-------------------------------------------------
A convencao vanilla e y=0 no chao, -Z na FRENTE, +X a ESQUERDA do bicho.
`conferir_eixos` le os dois geos e reprova se eles discordarem disso. Derivado
dela, e SO dela:

    rotacao X positiva  -> o rosto (massa em -Z) SOBE;
                           o topo do cranio (massa em +Y) vai para TRAS;
                           membro pendurado balanca para a FRENTE
    rotacao X negativa  -> o rosto DESCE; o tronco INCLINA PARA A FRENTE;
                           a MANDIBULA ABRE (a massa dela esta a frente do eixo)
    rotacao Z positiva  -> o lado esquerdo (+X) sobe; a mao pendurada vai para
                           FORA no braco esquerdo e para DENTRO no direito
    posicao  -Y         -> agacha

E por isso que abrir os bracos e `z` POSITIVO na esquerda e NEGATIVO na direita,
e nunca o mesmo sinal nos dois.

Regerar:  python art-source/enemies/man_faced_ape/man_faced_ape_animacoes.py
Exporta:  .../animations/entity/man_faced_ape.animation.json
          .../animations/entity/man_faced_ape_disfarce.animation.json
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum.animacao import corrigir_sentido_de_z_em  # noqa: E402

# --------------------------------------------------------------- o contrato

DIR_GEO = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "geo", "entity")
DIR_ANIM = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                        "animations", "entity")

ID_REVELADO = "man_faced_ape"
ID_DISFARCE = "man_faced_ape_disfarce"

# Hierarquias CONGELADAS pelo contrato da entrega. Nome errado nao da erro no
# GeckoLib: o osso simplesmente nao se mexe.
OSSOS_DISFARCE = (
    "root", "body", "head", "hood", "arm_left", "arm_right",
    "leg_left", "leg_right",
)
OSSOS_REVELADO = (
    "root", "body", "head", "jaw", "ear_left", "ear_right",
    "arm_left", "hand_left", "arm_right", "hand_right",
    "leg_left", "foot_left", "leg_right", "foot_right",
)

CLIPES_DISFARCE = ("idle", "walk", "stare", "reveal")
CLIPES_REVELADO = ("idle", "walk", "run", "strike", "hurt", "death")

# Ossos que existem NAS DUAS formas -- e que por isso atravessam a troca de
# modelo. Derivado, nao listado: listar de novo aqui seria a terceira fonte para
# uma verdade que ja tem duas.
COMUNS = tuple(o for o in OSSOS_DISFARCE if o in OSSOS_REVELADO and o != "root")

# Ossos que so uma das formas tem. Sao ELES que tornam a troca de pares
# detectavel; sem citar pelo menos um, um arquivo de animacao serve as duas
# formas por acidente.
EXCLUSIVOS_DISFARCE = ("hood",)
EXCLUSIVOS_REVELADO = ("jaw", "ear_left", "ear_right", "hand_left",
                       "hand_right", "foot_left", "foot_right")

# Extremidade que CONTRA-GIRA a fracao do membro, para a mao continuar apoiada e
# a sola continuar paralela ao chao em vez de apontar para o jogador.
EXTREMIDADE = {
    "arm_left": "hand_left", "arm_right": "hand_right",
    "leg_left": "foot_left", "leg_right": "foot_right",
}
CONTRA_MAO = -0.55
CONTRA_PE = -0.45

# ------------------------------------------------------------------- ticks

# Copiados do SERVIDOR. HunterExamProfiles.manFacedApeDisguise() da o reveal;
# .manFacedApeStrike() da os tres tempos do golpe.
TICKS_POR_SEGUNDO = 20.0
TICKS_REVEAL = 10
TICKS_WINDUP = 8
TICKS_ACTIVE = 4
TICKS_RECOVERY = 12

DUR_REVEAL = TICKS_REVEAL / TICKS_POR_SEGUNDO                       # 0.50
FIM_DO_WINDUP = TICKS_WINDUP / TICKS_POR_SEGUNDO                    # 0.40
FIM_DA_JANELA = (TICKS_WINDUP + TICKS_ACTIVE) / TICKS_POR_SEGUNDO   # 0.60
DUR_STRIKE = (TICKS_WINDUP + TICKS_ACTIVE
              + TICKS_RECOVERY) / TICKS_POR_SEGUNDO                 # 1.20

# ---------------------------------------------------------------- a postura

# A POSTURA DO PRIMATA, em graus. Ela e a pose em que o reveal TERMINA e em que
# todo clipe revelado COMECA -- ver `conferir_a_costura`.
#
# Os angulos sao de desenho e moram aqui uma vez. O agachamento (quanto o corpo
# desce) NAO esta nesta lista de proposito: ele e LIDO do geo, porque depende de
# quanto os nos dos dedos tem de folga ate o chao -- ver `agachamento`.
BODY_X = -22.0    # tronco inclinado para a FRENTE (X negativo, ver EIXOS)
# CABECA_X E POSITIVO, e isso NAO e engano de sinal -- e a conta que o geo
# obriga. O pescoco e filho do tronco, entao o angulo do rosto e a SOMA dos
# dois: com o tronco a -22, um +22 no pescoco deixaria o rosto exatamente
# nivelado. +14 deixa liquido -8 -- rosto adiantado, apontado um pouco para
# baixo, que e como um primata carrega a cabeca.
#
# Escrever -16 aqui (o reflexo de "cabeca baixa") somaria -38 e enfiaria o
# focinho no proprio peito. Nao daria erro nenhum: daria um macaco que anda
# olhando para os proprios pes.
CABECA_X = 14.0
BRACO_X = 12.0    # maos a frente dos ombros: e assim que o no do dedo planta
BRACO_Z = 16.0    # bracos abertos o bastante para passar do quadril
PERNA_X = 9.0     # joelho a frente
PERNA_Z = 5.0     # pernas arqueadas

# Fracao da folga das maos que o agachamento pode gastar. Gastar a folga inteira
# poria os nos dos dedos EXATAMENTE no chao -- e qualquer rotacao de braco os
# enfiaria dentro dele.
FRACAO_DO_AGACHAMENTO = 0.6
# Abaixo disto o agachamento nao le como agachamento, e a postura vira "de pe,
# meio curvado" -- que e o corpo que acabou de sair de cena.
AGACHAMENTO_MINIMO = 0.8

# Folga, em px, entre o capuz caido e a nuca. Sem ela o capuz para RASPANDO a
# cabeca e continua cobrindo parte do cranio no quadro em que o modelo troca.
MARGEM_DO_CAPUZ = 1.0
# Capuz que precise passar disto esta dobrando para dentro do pescoco.
ANGULO_MAXIMO_DO_CAPUZ = 150.0

# --------------------------------------------------------------- as marchas

CICLO_WALK_HUMANO = 1.0
CICLO_WALK_APE = 0.9
CICLO_RUN_APE = 0.55

# O que separa marcha de GENTE de marcha de BICHO, em dois numeros -- e os dois
# sao conferidos em `conferir_a_marcha`, porque "nao pode parecer bicho" e um
# criterio que some na primeira correcao distraida.
HUMANO_PERNA = 17.0   # a perna manda...
HUMANO_BRACO = 8.0    # ...e o braco so acompanha
APE_WALK_BRACO = 26.0  # no primata e o contrario: o braco longo PUXA o corpo
APE_WALK_PERNA = 20.0
APE_RUN_BRACO = 40.0
APE_RUN_PERNA = 32.0

# Fase que faz a cossenoide VALER A BASE em t=0. cos(2*pi*0.75) = 0, e a curva
# sobe a partir dai. Qualquer outra fase quebraria a costura, porque o primeiro
# quadro de walk/run deixaria de ser a postura.
FASE_QUE_COMECA_NA_BASE = 0.75


# -------------------------------------------------------------- ferramentas


def tempo(t):
    """Chave de keyframe: string, sempre com decimal, sem zero sobrando."""
    texto = ("%.4f" % round(t, 4)).rstrip("0")
    return texto + "0" if texto.endswith(".") else texto


def num(v):
    v = round(v, 2)
    return int(v) if v == int(v) else v


def vetor(x=0.0, y=0.0, z=0.0):
    """Rotacao ou posicao: o neutro e zero."""
    return [num(x), num(y), num(z)]


def escala(x=1.0, y=1.0, z=1.0):
    """Escala: o neutro e UM. Vetor de escala com zero some com o osso."""
    return [num(x), num(y), num(z)]


def curva(bones, osso, canal, pares):
    """Acumula keyframes de um canal (rotation/position/scale) de um osso.

    Escreve por CHAVE DE TEMPO, entao uma curva posterior sobrescreve a postura
    que `postura_base` deitou -- que e exatamente a ordem desejada.
    """
    alvo = bones.setdefault(osso, {}).setdefault(canal, {})
    for t, v in pares:
        alvo[tempo(t)] = v


def membro(bones, nome, pares):
    """Keya o braco/perna e DERIVA a mao/pe dele.

    A extremidade escrita a mao fica com a fase certa hoje e errada na primeira
    correcao do membro -- e mao fora de fase nao da erro, da um primata que
    arrasta o punho. So o eixo X contra-gira: Z carrega a abertura do braco, e
    contra-girar a abertura viraria o punho para dentro.
    """
    curva(bones, nome, "rotation", pares)
    filho = EXTREMIDADE[nome]
    fator = CONTRA_MAO if filho.startswith("hand") else CONTRA_PE
    curva(bones, filho, "rotation", [(t, vetor(x=v[0] * fator)) for t, v in pares])


def ciclo(duracao, periodo, amplitude, base=0.0, fase=FASE_QUE_COMECA_NA_BASE,
          amostras=8):
    """Cossenoide amostrada -- e a marcha inteira.

    Amostrar (em vez de escrever os extremos a mao) e o que mantem a fase certa
    quando alguem mexe no periodo, e o que mantem a interpolacao linear do
    formato 1.8.0 parecendo curva em vez de zigue-zague.
    """
    passo = periodo / amostras
    n = int(round(duracao / passo))
    return [(i * passo,
             base + amplitude * math.cos(2 * math.pi * (i * passo / periodo + fase)))
            for i in range(n + 1)]


def postura(agacha):
    """A postura do primata: rotacao por osso comum + a posicao do body.

    Devolve os dois separados porque so o BODY carrega o agachamento -- e
    espalhar a posicao pelos seis ossos criaria seis lugares para o mesmo
    numero.
    """
    rotacoes = {
        "body": vetor(x=BODY_X),
        "head": vetor(x=CABECA_X),
        "arm_left": vetor(x=BRACO_X, z=BRACO_Z),
        "arm_right": vetor(x=BRACO_X, z=-BRACO_Z),
        "leg_left": vetor(x=PERNA_X, z=PERNA_Z),
        "leg_right": vetor(x=PERNA_X, z=-PERNA_Z),
    }
    if set(rotacoes) != set(COMUNS):
        raise SystemExit(
            "a postura cobre %s e os ossos comuns as duas formas sao %s. Osso comum"
            " fora da postura volta para o default do modelo na troca -- o macaco"
            " endireita aquele membro por um quadro e nada acusa."
            % (sorted(rotacoes), sorted(COMUNS)))
    return rotacoes, vetor(y=-agacha)


def extremidades_da_postura():
    """Mao e pe da postura -- DERIVADOS, pela mesma conta que `membro` usa.

    Sem eles a postura ficaria pela metade, e o buraco seria pequeno demais para
    alguem ver e grande demais para ficar: o `idle` nao mexe as pernas, entao os
    pes dele ficariam no default (zero), enquanto walk, run, strike, hurt e death
    comecam com o pe em PERNA_X*CONTRA_PE. Toda transicao de idle para andar
    daria um tranco de 4 graus nos dois pes.

    Derivar (em vez de escrever -4.05 aqui) e o que mantem isso valido no dia em
    que PERNA_X ou CONTRA_PE mudarem.
    """
    rotacoes = {}
    for pai, filho in EXTREMIDADE.items():
        eh_mao = filho.startswith("hand")
        base = BRACO_X if eh_mao else PERNA_X
        fator = CONTRA_MAO if eh_mao else CONTRA_PE
        rotacoes[filho] = vetor(x=base * fator)
    return rotacoes


def postura_base(bones, agacha, duracao):
    """Deita a postura em TODO osso animavel, no inicio e no fim do clipe.

    E o que impede o buraco silencioso: osso que o clipe nao cita nao fica na
    postura, volta para o default do MODELO. Um `hurt` sem cabeca endireitaria o
    pescoco por cinco ticks; um `strike` sem pernas poria o macaco de pe no meio
    do golpe; um `idle` sem pes trancaria o tornozelo na primeira passada.
    """
    rotacoes, posicao = postura(agacha)
    rotacoes = dict(rotacoes, **extremidades_da_postura())
    for osso, valor in rotacoes.items():
        curva(bones, osso, "rotation", [(0.0, valor), (duracao, valor)])
    curva(bones, "body", "position", [(0.0, posicao), (duracao, posicao)])


# ------------------------------------------------- clipes da FORMA HUMANA

def humano_idle():
    """Pessoa esperando: respiracao contida e a cabeca que ainda se mexe.

    ESTE CLIPE EXISTE PARA CONTRASTAR COM `stare`. E a diferenca entre os dois
    que ensina o mob: aqui o corpo respira e a cabeca acompanha alguma coisa; la
    nao. Se alguem "melhorar" um dos dois ate ficarem parecidos, o mob perde a
    pista e nenhum teste acusa -- por isso `conferir_idle_vivo` reprova um idle
    sem movimento de tronco.
    """
    dur = 3.0
    b = {}
    # Um ciclo de respiracao por 3s (~20 por minuto): calmo. Dois seria uma
    # pessoa ofegante, e quem esta emboscando nao esta ofegante.
    curva(b, "body", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, 3.0, 0.25)])
    curva(b, "body", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, 3.0, -1.2)])
    # A cabeca VIVE: olha um pouco para um lado, volta, olha para o outro.
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.8, vetor(y=-7.0, x=1.0)), (1.5, vetor(y=-2.0)),
        (2.2, vetor(y=6.0, x=-1.0)), (dur, vetor())])
    curva(b, "hood", "rotation", [
        (0.0, vetor()), (0.9, vetor(x=1.5)), (2.3, vetor(x=-1.0)), (dur, vetor())])
    for lado, sinal in (("arm_left", 1.0), ("arm_right", -1.0)):
        curva(b, lado, "rotation", [
            (0.0, vetor()), (1.2, vetor(x=2.0, z=1.0 * sinal)),
            (2.4, vetor(x=-1.5)), (dur, vetor())])
    return {"loop": True, "animation_length": dur, "bones": b}


def humano_walk():
    """Caminhada de GENTE, 1.0s por ciclo completo (dois passos).

    O que a faz humana sao duas coisas, e as duas sao conferidas depois:
    (a) o TRONCO NAO INCLINA -- body.rotation.x fica em zero. Basta inclinar o
        peito para a frente e a silhueta vira bicho, por mais correta que a
        perna esteja;
    (b) o BRACO OSCILA MENOS QUE A PERNA, e em contrafase com ela. Braco com a
        amplitude da perna e marcha de quem esta carregando o proprio peso nos
        bracos -- que e o primata.
    """
    dur = CICLO_WALK_HUMANO
    b = {}
    # Perna esquerda e braco DIREITO na mesma fase: e a contrarrotacao que todo
    # bipede faz para nao girar o tronco a cada passo.
    for nome, fase, amplitude in (
            ("leg_left", 0.75, HUMANO_PERNA), ("leg_right", 0.25, HUMANO_PERNA),
            ("arm_left", 0.25, HUMANO_BRACO), ("arm_right", 0.75, HUMANO_BRACO)):
        curva(b, nome, "rotation",
              [(t, vetor(x=v)) for t, v in ciclo(dur, dur, amplitude, 0.0, fase)])
    # Sobe e desce DUAS vezes por ciclo: o corpo esta mais alto em cada apoio.
    curva(b, "body", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, dur / 2.0, 0.35)])
    # Transferencia de peso: o quadril rola para o lado que apoia. Z, nunca X --
    # X aqui e a inclinacao que transformaria a caminhada em marcha de bicho.
    curva(b, "body", "rotation",
          [(t, vetor(z=v)) for t, v in ciclo(dur, dur, 1.6)])
    # A cabeca fica NIVELADA: ela desfaz o rolamento do quadril.
    curva(b, "head", "rotation",
          [(t, vetor(z=v)) for t, v in ciclo(dur, dur, -1.1)])
    curva(b, "hood", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, dur / 2.0, 2.0)])
    return {"loop": True, "animation_length": dur, "bones": b}


def humano_stare():
    """O CLIPE QUE ENSINA O MOB -- e o unico que toca enquanto ele esta parado.

    Ele so fica parado quando alguem o encara. Entao este clipe E a pista, e a
    pista e a AUSENCIA de coisas: sem respiracao no tronco, sem cabeca que
    acompanhe nada, sem oscilacao nenhuma. Um corpo humano nunca faz isso.

    A UNICA coisa que se mexe e um tremor no ombro ESQUERDO, uma vez a cada 4s.
    A assimetria e o ponto e nao pode ser "corrigida": dois ombros tremendo
    juntos leem como calafrio, que e uma coisa que gente faz. Um ombro sozinho,
    seco, sem nada acompanhando, nao le como nada -- e e disso que o jogador
    desconfia antes de saber o que viu.

    body e head sao keyados em ZERO de proposito, e nao omitidos: o zero escrito
    e a afirmacao "este corpo nao respira", e ela aparece no diff. Omitidos,
    alguem acrescenta uma respiracao aqui achando que esta melhorando o clipe.
    `conferir_o_stare` reprova exatamente isso.
    """
    dur = 4.0
    b = {}
    for parado in ("body", "head", "hood", "arm_right", "leg_left", "leg_right"):
        curva(b, parado, "rotation", [(0.0, vetor()), (dur, vetor())])
    curva(b, "body", "position", [(0.0, vetor()), (dur, vetor())])
    # O tremor: 0.18s inteiro, comeca e termina no nada. Pequeno o bastante para
    # o jogador nao ter certeza, seco o bastante para nao parecer respiracao.
    curva(b, "arm_left", "rotation", [
        (0.0, vetor()), (2.52, vetor()), (2.58, vetor(x=1.8, z=-1.4)),
        (2.64, vetor(x=-1.1, z=0.8)), (2.7, vetor()), (dur, vetor())])
    return {"loop": True, "animation_length": dur, "bones": b}


def humano_reveal(agacha, capuz):
    """10 ticks. A TRANSICAO -- e o unico aviso que o jogador recebe.

    hold_on_last_frame, e nao loop nem false, por um motivo mecanico: o servidor
    troca o MODELO quando o WINDUP acaba, e a fase pode durar mais que 0.5s. Em
    loop, o macaco se retorceria de novo do zero; em false, ele voltaria a ser
    gente de pe bem no quadro anterior a virar primata.

    A forma do clipe nao e uma interpolacao: e RECOLHE (0.10), ROMPE (0.22),
    PASSA DO PONTO (0.35), ASSENTA (0.50). Sem o recolhimento inicial o corpo so
    incha, e inchar nao le como transformacao.

    O ultimo quadro E a postura do primata -- a mesma que todo clipe do outro
    modelo comeca. Ver `conferir_a_costura`.
    """
    dur = DUR_REVEAL
    rotacoes, posicao_do_body = postura(agacha)
    b = {}
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.1, vetor(x=-6.0)), (0.22, vetor(x=-14.0)),
        (0.35, vetor(x=-26.0)), (dur, rotacoes["body"])])
    curva(b, "body", "position", [
        (0.0, vetor()), (0.1, vetor(y=-0.6)), (0.22, vetor(y=0.5)),
        (0.35, vetor(y=-agacha * 1.6)), (dur, posicao_do_body)])
    # A CABECA NAO ACOMPANHA A QUEDA DO CORPO -- e esse o efeito.
    # O tronco desaba de 0 para -22 enquanto o pescoco sobe de 0 para +14. Somados,
    # o rosto quase nao se mexe: o corpo desmorona POR BAIXO de uma cabeca que
    # continua apontada para quem esta olhando. E a leitura mais desagradavel que
    # meio segundo de clipe consegue dar, e ela sai de graca da soma dos dois ossos.
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.1, vetor(x=6.0)), (0.22, vetor(x=2.0)),
        (0.35, vetor(x=10.0)), (dur, rotacoes["head"])])
    # O CAPUZ CAI PARA TRAS. O angulo nao e gosto: e o menor que tira o capuz de
    # cima do cranio, lido do geo por `angulo_do_capuz`.
    curva(b, "hood", "rotation", [
        (0.0, vetor()), (0.1, vetor(x=2.0)), (0.22, vetor(x=capuz * 0.3)),
        (0.35, vetor(x=capuz * 0.8)), (dur, vetor(x=capuz))])
    curva(b, "hood", "position", [
        (0.0, vetor()), (0.22, vetor(y=0.3, z=0.6)), (dur, vetor(y=-0.8, z=2.2))])
    for lado, sinal in (("arm_left", 1.0), ("arm_right", -1.0)):
        # Puxa para DENTRO antes de abrir: e o recolhimento que vende o resto.
        curva(b, lado, "rotation", [
            (0.0, vetor()), (0.1, vetor(x=-8.0, z=-4.0 * sinal)),
            (0.22, vetor(x=4.0, z=12.0 * sinal)),
            (0.35, vetor(x=16.0, z=34.0 * sinal)),   # o arranco, bem passado
            (dur, rotacoes[lado])])
        # OS OMBROS CRESCEM -- e a escala mora no BRACO, nao no body.
        # No body ela propagaria para cabeca e pernas e infliria a pessoa
        # inteira por igual, que le como balao e nao como transformacao. No
        # braco, ela cresce a partir do ombro: e exatamente o braco longo de
        # primata aparecendo.
        curva(b, lado, "scale", [
            (0.0, escala()), (0.1, escala(0.94, 0.96, 0.94)),
            (0.22, escala(1.1, 1.16, 1.1)), (dur, escala(1.3, 1.4, 1.3))])
    for lado, sinal in (("leg_left", 1.0), ("leg_right", -1.0)):
        curva(b, lado, "rotation", [
            (0.0, vetor()), (0.1, vetor(x=-5.0)),
            (0.22, vetor(x=4.0, z=3.0 * sinal)),
            (0.35, vetor(x=13.0, z=9.0 * sinal)), (dur, rotacoes[lado])])
    return {"loop": "hold_on_last_frame", "animation_length": dur, "bones": b}


# ---------------------------------------------- clipes da FORMA REVELADA

def ape_idle(agacha):
    """Primata pesado apoiado nos nos dos dedos.

    Tres coisas, e so tres: a respiracao PESADA no tronco (duas por 4s, contra
    uma por 3s do humano -- e esse contraste que diz que o corpo mudou), a
    cabeca varrendo devagar o ambiente, e as orelhas.

    AS ORELHAS SAO ASSIMETRICAS DE PROPOSITO, e isso e o oposto do que vale para
    olhos: bicho mexe uma orelha de cada vez, e mexer as duas juntas leria como
    um unico gesto de cabeca. Nao "corrigir" para o par.
    """
    dur = 4.0
    b = {}
    postura_base(b, agacha, dur)
    curva(b, "body", "position",
          [(t, vetor(y=-agacha + v)) for t, v in ciclo(dur, 2.0, 0.5)])
    curva(b, "body", "rotation",
          [(t, vetor(x=BODY_X + v)) for t, v in ciclo(dur, 2.0, -2.0)])
    # A varredura: lenta e completa, uma so por volta do clipe. Rapida, viraria
    # um bicho nervoso; o macaco revelado nao esta nervoso, esta escolhendo.
    curva(b, "head", "rotation", [
        (0.0, vetor(x=CABECA_X)), (1.1, vetor(x=CABECA_X + 2.0, y=-15.0)),
        (2.0, vetor(x=CABECA_X, y=-3.0)), (3.0, vetor(x=CABECA_X + 1.0, y=13.0)),
        (dur, vetor(x=CABECA_X))])
    curva(b, "ear_left", "rotation", [
        (0.0, vetor()), (1.05, vetor()), (1.15, vetor(x=-14.0, z=6.0)),
        (1.3, vetor(x=4.0)), (1.45, vetor()), (dur, vetor())])
    curva(b, "ear_right", "rotation", [
        (0.0, vetor()), (2.9, vetor()), (3.0, vetor(x=-12.0, z=-5.0)),
        (3.16, vetor(x=3.0)), (3.3, vetor()), (dur, vetor())])
    # A mandibula acompanha a respiracao: X negativo ABRE (a massa dela esta a
    # frente da dobradica -- conferido no geo).
    curva(b, "jaw", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, 2.0, -1.8, -1.8)])
    # Peso passando de uma mao para a outra, sem tirar nenhuma do chao.
    for lado, sinal, fase in (("arm_left", 1.0, 0.75), ("arm_right", -1.0, 0.25)):
        membro(b, lado, [(t, vetor(x=v, z=BRACO_Z * sinal))
                         for t, v in ciclo(dur, 4.0, 1.8, BRACO_X, fase)])
    return {"loop": True, "animation_length": dur, "bones": b}


def ape_walk(agacha):
    """Quadrupede-ish: o braco longo toca o chao a cada passada. 0.9s.

    Marcha DIAGONAL (braco esquerdo com perna direita), que e o que todo
    quadrupede faz em passo e o que mantem tres apoios no chao.

    O que a separa da caminhada humana, e o que e conferido depois: aqui o BRACO
    tem amplitude MAIOR que a perna. O primata nao balanca o braco, ele se puxa
    por ele.
    """
    dur = CICLO_WALK_APE
    b = {}
    postura_base(b, agacha, dur)
    for nome, sinal, fase, amplitude, base_x, base_z in (
            ("arm_left", 1.0, 0.75, APE_WALK_BRACO, BRACO_X, BRACO_Z),
            ("arm_right", -1.0, 0.25, APE_WALK_BRACO, BRACO_X, BRACO_Z),
            ("leg_left", 1.0, 0.25, APE_WALK_PERNA, PERNA_X, PERNA_Z),
            ("leg_right", -1.0, 0.75, APE_WALK_PERNA, PERNA_X, PERNA_Z)):
        membro(b, nome, [(t, vetor(x=v, z=base_z * sinal))
                         for t, v in ciclo(dur, dur, amplitude, base_x, fase)])
    # Sobe e desce duas vezes por ciclo, como o humano -- mas com o dobro da
    # amplitude, porque o peso e outro.
    curva(b, "body", "position",
          [(t, vetor(y=-agacha + v)) for t, v in ciclo(dur, dur / 2.0, 0.7)])
    curva(b, "body", "rotation",
          [(t, vetor(x=BODY_X + v, z=v * 0.5))
           for t, v in ciclo(dur, dur, 3.0)])
    # A cabeca BALANCA com a passada em vez de ficar nivelada: e o contrario do
    # que o humano faz, e e de proposito.
    curva(b, "head", "rotation",
          [(t, vetor(x=CABECA_X + v)) for t, v in ciclo(dur, dur / 2.0, 4.0)])
    curva(b, "jaw", "rotation", [(0.0, vetor(x=-2.0)), (dur, vetor(x=-2.0))])
    for orelha in ("ear_left", "ear_right"):
        curva(b, orelha, "rotation",
              [(t, vetor(x=v)) for t, v in ciclo(dur, dur / 2.0, 3.0, 2.0)])
    return {"loop": True, "animation_length": dur, "bones": b}


def ape_run(agacha):
    """Corrida: 0.55s. Mesma estrutura diagonal do walk, outro bicho.

    Nao e o walk acelerado. Tres coisas mudam de verdade -- o tronco desce mais
    (o macaco se joga para a frente), a cabeca fica BAIXA (linha do alvo, nao
    linha do horizonte) e a amplitude do braco quase dobra. Reaproveitar as
    mesmas amplitudes daria um walk tocado rapido, que le como bicho patinando.
    """
    dur = CICLO_RUN_APE
    b = {}
    postura_base(b, agacha, dur)
    tronco = BODY_X - 12.0   # mais fundo na corrida
    cabeca = CABECA_X - 8.0  # cabeca baixa, puxando para o alvo
    # AS BASES SAO AS DA POSTURA, e nao "a postura mais um pouco". Deslocar a base
    # aqui poria o primeiro quadro da corrida fora da costura -- e a corrida comeca
    # justamente quando o macaco acaba de se revelar. O que separa correr de andar e
    # a AMPLITUDE (40 contra 26) e o mergulho do tronco, nao um braco pendurado mais
    # a frente o tempo todo.
    for nome, sinal, fase, amplitude, base_x, base_z in (
            ("arm_left", 1.0, 0.75, APE_RUN_BRACO, BRACO_X, BRACO_Z),
            ("arm_right", -1.0, 0.25, APE_RUN_BRACO, BRACO_X, BRACO_Z),
            ("leg_left", 1.0, 0.25, APE_RUN_PERNA, PERNA_X, PERNA_Z),
            ("leg_right", -1.0, 0.75, APE_RUN_PERNA, PERNA_X, PERNA_Z)):
        membro(b, nome, [(t, vetor(x=v, z=base_z * sinal))
                         for t, v in ciclo(dur, dur, amplitude, base_x, fase)])
    # O t=0 tem de valer a POSTURA, e nao o tronco fundo da corrida: e a costura.
    # Por isso o mergulho entra como uma cossenoide COM BASE na postura, e nao
    # como um valor novo cravado no primeiro quadro.
    curva(b, "body", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, dur, (BODY_X - tronco) / 2.0,
                                             (BODY_X + tronco) / 2.0, 0.0)])
    curva(b, "body", "position",
          [(t, vetor(y=-agacha + v)) for t, v in ciclo(dur, dur / 2.0, 1.1)])
    curva(b, "head", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, dur, (CABECA_X - cabeca) / 2.0,
                                             (CABECA_X + cabeca) / 2.0, 0.0)])
    # Boca entreaberta: o bicho esta ofegante, e isso se ve antes do som.
    curva(b, "jaw", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, dur / 2.0, -4.0, -8.0)])
    for orelha in ("ear_left", "ear_right"):
        curva(b, orelha, "rotation", [(0.0, vetor()), (0.12, vetor(x=26.0)),
                                      (dur, vetor(x=26.0))])
    return {"loop": True, "animation_length": dur, "bones": b}


def ape_strike(agacha):
    """O GOLPE. 8 ticks de aviso, 4 de janela, 12 de recuperacao -- 1.2s.

    OS TRES TEMPOS SAO DO SERVIDOR, nao deste arquivo. O que este arquivo tem de
    garantir e que o desenho concorde com eles:

      0.00 -> 0.40  o braco SOBE. Sao os 8 ticks em que o jogador ainda pode sair.
      0.40 -> 0.60  o braco DESCE. Sao os 4 ticks em que o dano acontece.
      0.60 -> 1.20  recuperacao. 12 ticks em que bater nele e de graca.

    Um braco que chegasse ao topo em 0.5 prometeria o golpe depois da hora em que
    ele ja aconteceu; um que descesse em 0.3 prometeria antes. Nos dois casos o
    jogador aprende um tempo que o servidor nao cumpre, e nada no log diz nada.
    `conferir_a_janela_do_golpe` reprova os dois.

    UM BRACO SO, o direito. Um golpe com os dois seria outro movimento (um
    martelo de duas maos), e os 8 ticks de aviso precisam caber num membro que o
    jogador consiga seguir com o olho.

    loop false: o golpe acontece uma vez. E ele TERMINA NA POSTURA, nao no
    neutro -- senao o ultimo quadro poria o macaco de pe antes de o idle voltar.
    """
    dur = DUR_STRIKE
    b = {}
    postura_base(b, agacha, dur)
    # X crescente = a mao sobe pela FRENTE. Em 118 graus ela esta acima e a
    # frente da cabeca; em 22, ja varreu para baixo e para a frente. E essa
    # varredura, e nao a pose do topo, que e o golpe.
    curva(b, "arm_right", "rotation", [
        (0.0, vetor(x=BRACO_X, z=-BRACO_Z)),
        (0.16, vetor(x=48.0, z=-22.0)),
        (0.3, vetor(x=92.0, z=-26.0)),
        (FIM_DO_WINDUP, vetor(x=118.0, z=-28.0)),   # APICE: fim do aviso
        (0.5, vetor(x=66.0, z=-20.0)),
        (FIM_DA_JANELA, vetor(x=22.0, z=-12.0)),    # fim da janela de dano
        (0.78, vetor(x=2.0, z=-10.0)),              # inercia passa do ponto
        (1.0, vetor(x=BRACO_X + 3.0, z=-BRACO_Z)),
        (dur, vetor(x=BRACO_X, z=-BRACO_Z))])
    # O punho NAO contra-gira como no andar: aqui ele LIDERA. Derivar a mao da
    # formula do apoio a deixaria apontando para tras no momento do impacto.
    #
    # Mas ele SAI e VOLTA para a postura, e nao para o zero: fora do golpe esta
    # mao e um no de dedo apoiado no chao como qualquer outra.
    punho = extremidades_da_postura()["hand_right"]
    curva(b, "hand_right", "rotation", [
        (0.0, punho), (FIM_DO_WINDUP, vetor(x=-26.0)),
        (0.5, vetor(x=8.0)), (FIM_DA_JANELA, vetor(x=16.0)),
        (0.9, vetor(x=-4.0)), (dur, punho)])
    # O braco esquerdo PLANTA e recebe o peso: sem ele o macaco bate no ar.
    membro(b, "arm_left", [
        (0.0, vetor(x=BRACO_X, z=BRACO_Z)),
        (FIM_DO_WINDUP, vetor(x=BRACO_X + 14.0, z=BRACO_Z + 6.0)),
        (FIM_DA_JANELA, vetor(x=BRACO_X + 26.0, z=BRACO_Z + 2.0)),
        (0.85, vetor(x=BRACO_X + 8.0, z=BRACO_Z)),
        (dur, vetor(x=BRACO_X, z=BRACO_Z))])
    # O tronco desenrola no aviso e despenca na janela. E o corpo, e nao o braco,
    # que da PESO ao golpe.
    curva(b, "body", "rotation", [
        (0.0, vetor(x=BODY_X)), (0.16, vetor(x=BODY_X + 8.0)),
        (FIM_DO_WINDUP, vetor(x=-6.0)),
        (0.5, vetor(x=-26.0)), (FIM_DA_JANELA, vetor(x=-36.0)),
        (0.85, vetor(x=-14.0)), (dur, vetor(x=BODY_X))])
    curva(b, "body", "position", [
        (0.0, vetor(y=-agacha)), (FIM_DO_WINDUP, vetor(y=-agacha + 1.4)),
        (FIM_DA_JANELA, vetor(y=-agacha - 1.6)), (0.85, vetor(y=-agacha + 0.4)),
        (dur, vetor(y=-agacha))])
    # A cabeca e lida SOMADA ao tronco. No aviso o tronco sobe para -6 e o pescoco
    # vai a +20: liquido +14, o macaco ergue a cara. Na janela o tronco despenca
    # para -36 e o pescoco vai a -6: liquido -42, a cabeca DESCE junto com o braco.
    curva(b, "head", "rotation", [
        (0.0, vetor(x=CABECA_X)), (FIM_DO_WINDUP, vetor(x=20.0)),
        (0.52, vetor(x=4.0)), (FIM_DA_JANELA, vetor(x=-6.0)),
        (0.85, vetor(x=6.0)), (dur, vetor(x=CABECA_X))])
    # O rugido mora no aviso, nao no impacto: som e cara de aviso sao o aviso.
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (0.2, vetor(x=-18.0)), (FIM_DO_WINDUP, vetor(x=-38.0)),
        (FIM_DA_JANELA, vetor(x=-12.0)), (0.85, vetor(x=-4.0)), (dur, vetor())])
    # Orelhas COLADAS para tras no aviso -- a leitura de "vai vir".
    for orelha, sinal in (("ear_left", 1.0), ("ear_right", -1.0)):
        curva(b, orelha, "rotation", [
            (0.0, vetor()), (0.16, vetor(x=34.0, z=-6.0 * sinal)),
            (FIM_DA_JANELA, vetor(x=40.0, z=-8.0 * sinal)),
            (0.95, vetor(x=12.0)), (dur, vetor())])
    # A perna do lado que bate empurra; a outra segura o chao.
    membro(b, "leg_right", [
        (0.0, vetor(x=PERNA_X, z=-PERNA_Z)),
        (FIM_DO_WINDUP, vetor(x=PERNA_X - 12.0, z=-PERNA_Z)),
        (FIM_DA_JANELA, vetor(x=PERNA_X - 20.0, z=-PERNA_Z - 3.0)),
        (0.9, vetor(x=PERNA_X - 4.0, z=-PERNA_Z)),
        (dur, vetor(x=PERNA_X, z=-PERNA_Z))])
    membro(b, "leg_left", [
        (0.0, vetor(x=PERNA_X, z=PERNA_Z)),
        (FIM_DO_WINDUP, vetor(x=PERNA_X + 10.0, z=PERNA_Z)),
        (FIM_DA_JANELA, vetor(x=PERNA_X + 16.0, z=PERNA_Z + 4.0)),
        (0.9, vetor(x=PERNA_X + 5.0, z=PERNA_Z)),
        (dur, vetor(x=PERNA_X, z=PERNA_Z))])
    return {"loop": False, "animation_length": dur, "bones": b}


def ape_hurt(agacha):
    """Tranco de 5 ticks. Nao interrompe leitura de fase nenhuma.

    NAO KEYA position do body -- so rotacao. `position` e o canal que carrega o
    AGACHAMENTO, e um hurt que o escrevesse arrancaria o macaco da postura por
    cinco ticks, no meio de um golpe. Rotacao nao tem esse problema, porque a
    postura de rotacao ja esta deitada por `postura_base` e o tranco anda em
    volta dela. (Isso REDUZ o estrago; nao o elimina -- ver o relato.)
    """
    dur = 0.25
    b = {}
    postura_base(b, agacha, dur)
    curva(b, "body", "rotation", [
        (0.0, vetor(x=BODY_X)), (0.06, vetor(x=BODY_X + 7.0, z=8.0)),
        (0.14, vetor(x=BODY_X - 3.0, z=-3.0)), (dur, vetor(x=BODY_X))])
    curva(b, "head", "rotation", [
        (0.0, vetor(x=CABECA_X)), (0.06, vetor(x=CABECA_X + 15.0, y=9.0)),
        (0.15, vetor(x=CABECA_X - 5.0, y=-4.0)), (dur, vetor(x=CABECA_X))])
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (0.05, vetor(x=-24.0)), (0.15, vetor(x=-7.0)),
        (dur, vetor())])
    for orelha in ("ear_left", "ear_right"):
        curva(b, orelha, "rotation", [
            (0.0, vetor()), (0.05, vetor(x=32.0)), (0.16, vetor(x=8.0)),
            (dur, vetor())])
    for lado, sinal in (("arm_left", 1.0), ("arm_right", -1.0)):
        membro(b, lado, [
            (0.0, vetor(x=BRACO_X, z=BRACO_Z * sinal)),
            (0.06, vetor(x=BRACO_X - 10.0, z=(BRACO_Z + 5.0) * sinal)),
            (dur, vetor(x=BRACO_X, z=BRACO_Z * sinal))])
    for lado, sinal in (("leg_left", 1.0), ("leg_right", -1.0)):
        membro(b, lado, [
            (0.0, vetor(x=PERNA_X, z=PERNA_Z * sinal)),
            (0.06, vetor(x=PERNA_X + 8.0, z=PERNA_Z * sinal)),
            (dur, vetor(x=PERNA_X, z=PERNA_Z * sinal))])
    return {"loop": False, "animation_length": dur, "bones": b}


def ape_death(agacha, queda):
    """Cai de joelhos, o corpo tomba, o braco e o ultimo. 1.4s.

    A ORDEM IMPORTA MAIS QUE AS POSES, e e ela que alguem vai precisar conferir
    depois: tranco (0.15) -> joelhos cedem (0.40) -> tronco tomba (0.75) ->
    corpo no chao (1.10) -> assenta (1.40). O braco ESQUERDO larga cedo (0.30) e
    o DIREITO continua escorando ate 0.90, e so entao desaba. E esse atraso de
    meio segundo num membro so que faz a morte ler como morte, em vez de ler
    como desligar o bicho.

    A queda NAO e um numero de gosto: sai do geo, em `queda_da_morte`. Escrita a
    mao, ela sobreviveria a proxima correcao do modelo e o macaco morreria
    flutuando (ou enterrado) sem um unico erro no log.

    hold_on_last_frame: a entidade so some depois do clipe, e um corpo que volta
    a postura de pe no ultimo quadro e um bug visual de meio segundo.
    """
    dur = 1.4
    b = {}
    postura_base(b, agacha, dur)
    curva(b, "body", "rotation", [
        (0.0, vetor(x=BODY_X)), (0.15, vetor(x=BODY_X - 8.0, z=4.0)),
        (0.4, vetor(x=-38.0, z=14.0)), (0.75, vetor(x=-52.0, z=36.0)),
        (1.1, vetor(x=-58.0, z=62.0)), (dur, vetor(x=-60.0, z=74.0))])
    curva(b, "body", "position", [
        (0.0, vetor(y=-agacha)), (0.15, vetor(y=-agacha - 0.5)),
        (0.4, vetor(y=-agacha - queda * 0.35)),
        (0.75, vetor(x=1.0, y=-agacha - queda * 0.7)),
        (1.1, vetor(x=2.0, y=-agacha - queda * 0.94)),
        (dur, vetor(x=2.4, y=-agacha - queda))])
    # O pescoco vai ficando solto: o queixo sobe porque nada mais o segura.
    curva(b, "head", "rotation", [
        (0.0, vetor(x=CABECA_X)), (0.4, vetor(x=-4.0)),
        (0.9, vetor(x=18.0, z=-14.0)), (dur, vetor(x=26.0, z=-22.0))])
    # A boca abre DEVAGAR e por ultimo -- e a unica coisa que ainda se move
    # depois de o corpo parar.
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (0.5, vetor(x=-6.0)), (0.95, vetor(x=-15.0)),
        (dur, vetor(x=-26.0))])
    for orelha in ("ear_left", "ear_right"):
        curva(b, orelha, "rotation", [
            (0.0, vetor()), (0.3, vetor(x=22.0)), (0.8, vetor(x=10.0)),
            (dur, vetor(x=6.0))])
    # Braco esquerdo: larga cedo.
    membro(b, "arm_left", [
        (0.0, vetor(x=BRACO_X, z=BRACO_Z)), (0.3, vetor(x=-6.0, z=8.0)),
        (0.7, vetor(x=-22.0, z=3.0)), (dur, vetor(x=-26.0, z=2.0))])
    # Braco direito: ESCORA ate 0.9 -- ele ainda esta tentando.
    membro(b, "arm_right", [
        (0.0, vetor(x=BRACO_X, z=-BRACO_Z)),
        (0.55, vetor(x=BRACO_X + 4.0, z=-(BRACO_Z + 2.0))),
        (0.9, vetor(x=BRACO_X + 2.0, z=-BRACO_Z)),
        (1.15, vetor(x=-8.0, z=-6.0)), (dur, vetor(x=-24.0, z=-2.0))])
    for lado, sinal in (("leg_left", 1.0), ("leg_right", -1.0)):
        membro(b, lado, [
            (0.0, vetor(x=PERNA_X, z=PERNA_Z * sinal)),
            (0.25, vetor(x=34.0, z=(PERNA_Z + 4.0) * sinal)),
            (0.6, vetor(x=48.0, z=(PERNA_Z + 8.0) * sinal)),
            (dur, vetor(x=52.0, z=(PERNA_Z + 10.0) * sinal))])
    return {"loop": "hold_on_last_frame", "animation_length": dur, "bones": b}


# ------------------------------------------------------- leitura do geo

def carregar_geo(mob):
    caminho = os.path.join(DIR_GEO, mob + ".geo.json")
    if not os.path.exists(caminho):
        raise SystemExit(
            "%s nao existe. Este gerador LE o geo -- o agachamento, a queda da"
            " morte e o angulo do capuz saem de la, e os eixos sao conferidos"
            " contra ele. Sem os DOIS geos nao ha o que animar." % caminho)
    return json.load(open(caminho, encoding="utf-8"))["minecraft:geometry"][0]


def osso(geometria, nome):
    for b in geometria["bones"]:
        if b["name"] == nome:
            return b
    raise SystemExit("o geo nao tem o osso '%s'" % nome)


def caixas(geometria, nome):
    return osso(geometria, nome).get("cubes", [])


def pivot(geometria, nome):
    b = osso(geometria, nome)
    if "pivot" not in b:
        raise SystemExit("o osso '%s' nao declara pivot; toda rotacao daqui gira"
                         " em torno dele" % nome)
    return b["pivot"]


def faixa(geometria, nomes, eixo):
    valores = []
    for nome in nomes:
        for c in caixas(geometria, nome):
            valores += [c["origin"][eixo], c["origin"][eixo] + c["size"][eixo]]
    if not valores:
        raise SystemExit("nenhum cubo em %s -- nao da para medir" % list(nomes))
    return min(valores), max(valores)


def centro(geometria, nome, eixo):
    cubos = caixas(geometria, nome)
    if not cubos:
        raise SystemExit("o osso '%s' nao tem cubo; nao da para ler o eixo" % nome)
    return sum(c["origin"][eixo] + c["size"][eixo] / 2.0 for c in cubos) / len(cubos)


def filhos(geometria, nome):
    return [b["name"] for b in geometria["bones"] if b.get("parent") == nome]


def subarvore_tem_volume(geometria, nome):
    if caixas(geometria, nome):
        return True
    return any(subarvore_tem_volume(geometria, f) for f in filhos(geometria, nome))


# -------------------------------------------------- numeros LIDOS do geo

def agachamento(geometria):
    """Quanto o corpo desce na postura -- LIDO do geo, nunca decorado.

    A regra: o agachamento gasta no maximo uma fracao da FOLGA que os nos dos
    dedos tem ate o chao. Gastar a folga inteira poria a mao exatamente em y=0, e
    qualquer rotacao de braco (a passada do walk chega a inclinar 26 graus) a
    enfiaria dentro do bloco.

    O portao morde dos dois lados porque os dois erros sao silenciosos: macaco
    com a mao dentro do chao le como bug de colisao, e macaco sem agachamento
    nenhum le como a pessoa que ele acabou de deixar de ser.

    PONTO CEGO DECLARADO: a medida ignora a rotacao dos bracos, entao ela e um
    limite superior aproximado e nao uma prova de que a mao nunca atravessa o
    chao em nenhum quadro. Isso segue humano.
    """
    piso_das_maos, _ = faixa(
        geometria, ("arm_left", "hand_left", "arm_right", "hand_right"), 1)
    agacha = FRACAO_DO_AGACHAMENTO * piso_das_maos
    if agacha < AGACHAMENTO_MINIMO:
        raise SystemExit(
            "os bracos so tem %.1f px de folga ate o chao, o que da um agachamento"
            " de %.1f px (minimo %.1f). Com tao pouco, a postura de primata le como"
            " uma pessoa curvada -- que e exatamente o corpo que o reveal acabou de"
            " deixar para tras."
            % (piso_das_maos, agacha, AGACHAMENTO_MINIMO))
    return agacha


def queda_da_morte(geometria):
    """Quanto o body desce ao tombar -- LIDO do geo.

    Deitado de lado, o que fica na vertical e a LARGURA do torso; entao o pivot
    do corpo termina a meia largura do chao, e a queda e a diferenca entre a
    altura de pe e essa.

    PONTO CEGO DECLARADO: a conta ignora o rolamento em Z que acontece junto, e
    por isso e uma aproximacao. Ela impede o erro grosso (corpo boiando um bloco
    acima do chao, ou afundado nele), nao garante que cada quadro encoste certo.
    """
    altura_de_pe = pivot(geometria, "body")[1]
    menor_x, maior_x = faixa(geometria, ("body",), 0)
    deitado = (maior_x - menor_x) / 2.0
    queda = altura_de_pe - deitado
    if queda < 3.0:
        raise SystemExit(
            "o pivot do corpo esta a %.1f px e o torso deitado ocupa %.1f px de"
            " meia largura: a queda daria %.1f px. Abaixo de 3 px ninguem ve o"
            " macaco cair -- ele so muda de angulo."
            % (altura_de_pe, deitado, queda))
    if queda > altura_de_pe:
        raise SystemExit(
            "a queda calculada (%.1f px) passa da altura do pivot (%.1f px): o"
            " corpo terminaria abaixo do chao." % (queda, altura_de_pe))
    return queda


def angulo_do_capuz(geometria):
    """Menor rotacao X que tira o capuz de cima do cranio -- LIDA do geo.

    O capuz gira em torno do proprio pivot. Girado de theta, a massa dele vai
    parar em z = pivot_z + ry*sin(theta) + rz*cos(theta) (ver EIXOS). O clipe
    procura o menor theta que poe essa massa ATRAS da face traseira da cabeca,
    com uma folga, e ainda acrescenta uma margem para o capuz nao parar raspando.

    Decorar 95 graus aqui funcionaria hoje e mentiria no dia em que o capuz
    ficasse mais fundo: ele pararia meio caido, ainda cobrindo parte do rosto no
    quadro em que o modelo troca -- a revelacao mostraria menos do que promete, e
    nada acusaria.
    """
    pv = pivot(geometria, "hood")
    ry = centro(geometria, "hood", 1) - pv[1]
    rz = centro(geometria, "hood", 2) - pv[2]
    _, nuca = faixa(geometria, ("head",), 2)
    for graus in range(0, int(ANGULO_MAXIMO_DO_CAPUZ) + 1, 5):
        th = math.radians(graus)
        z = pv[2] + ry * math.sin(th) + rz * math.cos(th)
        if z >= nuca + MARGEM_DO_CAPUZ:
            return float(min(graus + 10, ANGULO_MAXIMO_DO_CAPUZ))
    raise SystemExit(
        "nenhuma rotacao ate %.0f graus poe o capuz atras da nuca (z=%.1f). Com a"
        " massa do capuz a %.1f px do pivot em Y e %.1f em Z, ele nao sai de cima"
        " da cabeca -- e o reveal mostraria um capuz caindo pela metade."
        % (ANGULO_MAXIMO_DO_CAPUZ, nuca, ry, rz))


# ------------------------------------------------------------------ portoes

def conferir_eixos(geometria, mob):
    """Os eixos se LEEM do geo. Decorados, produzem um macaco ao contrario."""
    if pivot(geometria, "head")[1] <= pivot(geometria, "body")[1]:
        raise SystemExit(
            "%s: a cabeca nao esta acima do corpo (head y=%.1f, body y=%.1f). Todo"
            " este gerador supoe +Y = CIMA."
            % (mob, pivot(geometria, "head")[1], pivot(geometria, "body")[1]))
    if centro(geometria, "arm_left", 0) <= 0:
        raise SystemExit(
            "%s: arm_left esta em x=%.1f, e o contrato supoe +X = ESQUERDA. Com o"
            " sinal trocado, abrir os bracos os CRUZA e o tombo da morte cai para o"
            " lado errado." % (mob, centro(geometria, "arm_left", 0)))

    if mob == ID_REVELADO:
        # A mandibula se mede contra o PROPRIO pivot (a dobradica), nao contra a
        # cabeca: as duas podem ocupar a mesma faixa de Z, e comparar uma com a
        # outra reprovaria um modelo correto. O que importa para abrir a boca e a
        # massa estar A FRENTE da dobradica.
        if centro(geometria, "jaw", 2) >= pivot(geometria, "jaw")[2]:
            raise SystemExit(
                "%s: a mandibula nao se estende a frente da propria dobradica (massa"
                " em z=%.1f, pivot em z=%.1f). Assim 'abrir a boca' (X negativo)"
                " FECHA a boca -- e so aparece para quem ver o macaco rugindo."
                % (mob, centro(geometria, "jaw", 2), pivot(geometria, "jaw")[2]))
        if centro(geometria, "ear_left", 0) <= 0:
            raise SystemExit(
                "%s: ear_left esta em x=%.1f e deveria estar em +X." % (
                    mob, centro(geometria, "ear_left", 0)))
    else:
        # PONTO CEGO DECLARADO: na forma humana nao ha mandibula, e nada aqui
        # prova sozinho que -Z e a frente. O que da para conferir e que o capuz
        # nao esta modelado NA FRENTE do rosto -- se estivesse, "cair para tras"
        # seria a direcao errada. O resto herda a convencao do outro geo, que a
        # mandibula prova, e a convencao vanilla do repositorio.
        if centro(geometria, "hood", 2) < centro(geometria, "head", 2):
            raise SystemExit(
                "%s: a massa do capuz (z=%.1f) esta A FRENTE da cabeca (z=%.1f)."
                " Capuz na frente do rosto nao 'cai para tras': o reveal jogaria"
                " o capuz por cima da cara em vez de tira-lo."
                % (mob, centro(geometria, "hood", 2), centro(geometria, "head", 2)))


def conferir_postura_nao_duplicada(geometria, mob):
    """Postura assada no geo + postura animada = postura aplicada DUAS vezes.

    E a mesma familia de erro que o projeto ja conhece no dano multiplicado em
    dois handlers: o resultado e plausivel demais para alguem notar sem medir.
    Se a outra lane precisar de uma rotacao assada em algum destes ossos, ela
    reprova aqui -- e ai as duas lanes conversam, que e o ponto.
    """
    for nome in COMUNS:
        r = osso(geometria, nome).get("rotation")
        if r and any(abs(v) > 1e-9 for v in r):
            raise SystemExit(
                "%s: o osso '%s' ja vem com rotation=%s no geo, e a postura do"
                " primata gira esse mesmo osso na animacao. Os dois se SOMAM, e o"
                " resultado fica plausivel demais para alguem notar sem medir."
                " Avise a lane de animacao antes de assar postura no modelo."
                % (mob, nome, r))


def conferir_ossos(animacoes, geometria, contrato, mob):
    """Osso errado nao da erro no GeckoLib: o membro so fica parado.

    Morde dos tres lados -- contrato, geo de verdade e animacao. Uma lane
    renomeando um osso sem avisar a outra reprova AQUI, e nao na tela de quem
    joga.
    """
    do_contrato = set(contrato)
    do_geo = {b["name"] for b in geometria["bones"]}
    if do_geo != do_contrato:
        raise SystemExit("%s: geo e contrato discordam de osso: %s"
                         % (mob, sorted(do_geo ^ do_contrato)))
    for nome, clipe in animacoes.items():
        desconhecidos = sorted(set(clipe["bones"]) - do_contrato)
        if desconhecidos:
            raise SystemExit(
                "%s move osso que nao existe nesta forma: %s. Se esses nomes sao da"
                " OUTRA forma, o par geo/animacao foi trocado."
                % (nome, desconhecidos))


def conferir_ossos_exclusivos(animacoes, exclusivos, mob):
    """Cada forma tem de CITAR osso que so ela tem.

    Sem isto, um arquivo que so mexesse em {body, head, arm_*, leg_*} serviria as
    duas formas por acidente -- e trocar os pares passaria pelo portao acima sem
    um unico aviso.
    """
    citados = set()
    for clipe in animacoes.values():
        citados |= set(clipe["bones"])
    faltando = sorted(set(exclusivos) - citados)
    if faltando:
        raise SystemExit(
            "%s nao anima nenhum de %s, que sao os ossos exclusivos desta forma."
            " Um arquivo assim serviria as DUAS formas, e trocar os pares deixaria"
            " de ser detectavel." % (mob, faltando))


def conferir_que_trocar_os_pares_reprova(disfarce, revelado, geo_d, geo_r):
    """ALIMENTA o portao com o erro que ele existe para pegar.

    Regua que nunca reprovou e carimbo. Aqui os pares sao trocados de proposito
    -- animacao humana contra geo de primata e vice-versa -- e o portao TEM de
    recusar os dois. Se um dia ele deixar passar, este teste quebra antes de
    alguem publicar um macaco com metade dos membros congelados.
    """
    for animacoes, geometria, contrato, rotulo in (
            (disfarce, geo_r, OSSOS_REVELADO, "humano contra geo de primata"),
            (revelado, geo_d, OSSOS_DISFARCE, "primata contra geo humano")):
        try:
            conferir_ossos(animacoes, geometria, contrato, "par trocado")
        except SystemExit:
            continue
        raise SystemExit(
            "o portao de ossos ACEITOU os pares trocados (%s). Ele nao esta"
            " protegendo nada: revise conferir_ossos e os ossos exclusivos."
            % rotulo)


def conferir_ossos_com_volume(animacoes, geometria, mob):
    """Osso sem cubo na subarvore e osso que a animacao move e ninguem ve."""
    movidos = set()
    for clipe in animacoes.values():
        movidos |= set(clipe["bones"])
    vazios = sorted(n for n in movidos if not subarvore_tem_volume(geometria, n))
    if vazios:
        raise SystemExit(
            "%s: estes ossos sao animados e nao tem cubo nenhum na subarvore: %s."
            " O clipe roda, o osso gira e a tela nao muda." % (mob, vazios))


def conferir_clipes(animacoes, mob, clipes):
    esperado = {"animation.%s.%s" % (mob, c) for c in clipes}
    if set(animacoes) != esperado:
        raise SystemExit("%s: chaves fora do contrato: %s"
                         % (mob, sorted(set(animacoes) ^ esperado)))
    for nome, clipe in animacoes.items():
        if clipe["loop"] not in (True, False, "hold_on_last_frame"):
            raise SystemExit("%s: loop invalido %r" % (nome, clipe["loop"]))
        fim = clipe["animation_length"]
        for nome_osso, canais in clipe["bones"].items():
            for canal, quadros in canais.items():
                if canal not in ("rotation", "position", "scale"):
                    raise SystemExit("%s/%s: canal invalido %s"
                                     % (nome, nome_osso, canal))
                for t, v in quadros.items():
                    if float(t) < 0 or float(t) > fim + 1e-9:
                        raise SystemExit("%s/%s: quadro %s fora de [0, %s]"
                                         % (nome, nome_osso, t, fim))
                    if canal == "scale" and min(v) <= 0:
                        raise SystemExit("%s/%s: escala %s some com o osso"
                                         % (nome, nome_osso, v))


def conferir_duracoes(disfarce, revelado):
    """As duracoes que o SERVIDOR manda nao podem divergir em silencio."""
    reveal = disfarce["animation.%s.reveal" % ID_DISFARCE]["animation_length"]
    if abs(reveal - DUR_REVEAL) > 1e-9:
        raise SystemExit("reveal dura %.3fs e o telegrafo do servidor da %.3fs"
                         % (reveal, DUR_REVEAL))
    strike = revelado["animation.%s.strike" % ID_REVELADO]["animation_length"]
    if abs(strike - DUR_STRIKE) > 1e-9:
        raise SystemExit(
            "strike dura %.3fs e o golpe do servidor da %.3fs (%d+%d+%d ticks)"
            % (strike, DUR_STRIKE, TICKS_WINDUP, TICKS_ACTIVE, TICKS_RECOVERY))
    # Ciclica cujo periodo nao divide a duracao da um salto a cada volta.
    for nome, periodo in (
            ("animation.%s.walk" % ID_DISFARCE, CICLO_WALK_HUMANO),
            ("animation.%s.walk" % ID_REVELADO, CICLO_WALK_APE),
            ("animation.%s.run" % ID_REVELADO, CICLO_RUN_APE)):
        fonte = disfarce if ID_DISFARCE in nome else revelado
        voltas = fonte[nome]["animation_length"] / periodo
        if abs(voltas - round(voltas)) > 1e-6:
            raise SystemExit("%s: %.3f voltas do ciclo de %.3fs -- o loop salta"
                             % (nome, voltas, periodo))


def conferir_a_janela_do_golpe(revelado):
    """O PORTAO MAIS UTIL DESTE ARQUIVO.

    O golpe tem 8 ticks de aviso e 4 de janela, e quem decide isso e o servidor.
    Se o braco nao chegar ao topo EXATAMENTE em 0.40 e nao estiver descendo ao
    longo de [0.40, 0.60], o desenho promete um tempo que o jogo nao cumpre --
    e o jogador aprende a se esquivar da animacao, nao do golpe.

    Nada no GeckoLib confere isso, e nenhum gametest ve a tela.
    """
    clipe = revelado["animation.%s.strike" % ID_REVELADO]
    quadros = clipe["bones"]["arm_right"]["rotation"]
    for marco, rotulo in ((FIM_DO_WINDUP, "fim do aviso"),
                          (FIM_DA_JANELA, "fim da janela de dano")):
        if tempo(marco) not in quadros:
            raise SystemExit(
                "strike nao tem keyframe de arm_right em %s (%s). Sem um quadro"
                " cravado ali, mexer em qualquer pose vizinha desloca o golpe sem"
                " que nada acuse." % (tempo(marco), rotulo))
    apice_t, apice_v = max(quadros.items(), key=lambda kv: kv[1][0])
    if apice_t != tempo(FIM_DO_WINDUP):
        raise SystemExit(
            "o braco chega ao topo em %s, e o aviso do servidor acaba em %s. O"
            " jogador le a subida como o tempo de sair -- e ela estaria terminando"
            " na hora errada." % (apice_t, tempo(FIM_DO_WINDUP)))
    fim = quadros[tempo(FIM_DA_JANELA)][0]
    if fim >= apice_v[0]:
        raise SystemExit(
            "de %s a %s o braco vai de %.1f a %.1f graus: ele nao esta DESCENDO na"
            " janela em que o dano acontece."
            % (tempo(FIM_DO_WINDUP), tempo(FIM_DA_JANELA), apice_v[0], fim))
    for t, v in quadros.items():
        if FIM_DO_WINDUP - 1e-9 < float(t) < FIM_DA_JANELA + 1e-9:
            continue
        if float(t) < FIM_DO_WINDUP and v[0] > apice_v[0] + 1e-9:
            raise SystemExit("strike: o braco passa do apice em %s, antes do aviso"
                             " terminar" % t)


def conferir_a_costura(disfarce, revelado, agacha):
    """A TROCA DE MODELO E A FRONTEIRA MAIS PERIGOSA DESTA ENTREGA.

    O ultimo quadro do reveal e o primeiro quadro de TODO clipe do primata tem de
    ser a MESMA pose, osso a osso. Se divergirem, o jogador ve um salto no exato
    quadro em que o mob se revela -- e o GeckoLib nao tem nada a dizer sobre isso,
    porque para ele sao dois modelos que nunca se encontraram.

    A escala fica FORA da comparacao de proposito: o braco humano cresce 1.3x
    para APROXIMAR o braco do primata, que ja nasce longo no proprio geo. Exigir
    escala igual aqui compararia duas coisas que nao sao a mesma.
    """
    rotacoes, posicao = postura(agacha)
    reveal = disfarce["animation.%s.reveal" % ID_DISFARCE]
    fim = tempo(reveal["animation_length"])
    for nome in COMUNS:
        tinha = reveal["bones"].get(nome, {}).get("rotation", {}).get(fim)
        if tinha != rotacoes[nome]:
            raise SystemExit(
                "o reveal termina com '%s' em %s e a postura do primata e %s. O"
                " modelo troca nesse quadro: a diferenca vira um salto."
                % (nome, tinha, rotacoes[nome]))
    if reveal["bones"]["body"]["position"].get(fim) != posicao:
        raise SystemExit(
            "o reveal termina com body.position=%s e a postura pede %s."
            % (reveal["bones"]["body"]["position"].get(fim), posicao))

    # Do lado do primata a conferencia inclui MAO e PE, que a forma humana nem tem:
    # a costura com o humano e so dos ossos comuns, mas a costura de um clipe do
    # primata para o SEGUINTE e de todos. Foi assim que o idle apareceu sem pes.
    completa = dict(rotacoes, **extremidades_da_postura())
    for nome_clipe, clipe in revelado.items():
        for nome, esperado in completa.items():
            tem = clipe["bones"].get(nome, {}).get("rotation", {}).get("0.0")
            if tem != esperado:
                raise SystemExit(
                    "%s comeca com '%s' em %s e a postura e %s. Osso que um clipe"
                    " nao poe na postura volta para o DEFAULT DO MODELO, nao para a"
                    " postura -- e aquele membro endireita sozinho."
                    % (nome_clipe, nome, tem, esperado))
        if clipe["bones"]["body"]["position"].get("0.0") != posicao:
            raise SystemExit(
                "%s comeca com body.position=%s e a postura pede %s."
                % (nome_clipe, clipe["bones"]["body"]["position"].get("0.0"),
                   posicao))


def conferir_o_stare(disfarce):
    """O stare e feito de AUSENCIA, e ausencia nao aparece num diff.

    Alguem vai abrir este clipe, achar que ele esta 'sem vida' e acrescentar uma
    respiracao. Esse alguem tera destruido a unica pista que o mob da antes da
    emboscada, e nenhum outro portao do repositorio olha para animacao.
    """
    clipe = disfarce["animation.%s.stare" % ID_DISFARCE]
    inquietos = set()
    for nome, canais in clipe["bones"].items():
        for canal, quadros in canais.items():
            neutro = escala() if canal == "scale" else vetor()
            if any(v != neutro for v in quadros.values()):
                inquietos.add(nome)
    if inquietos != {"arm_left"}:
        raise SystemExit(
            "no stare, quem se mexe e %s -- e tem de ser SO {'arm_left'}. Corpo"
            " parado demais e a pista inteira deste mob: ele so fica parado quando"
            " alguem o encara, e um corpo humano que nao respira e o que o jogador"
            " precisa estranhar antes de saber o que e." % sorted(inquietos))


def conferir_idle_vivo(disfarce):
    """idle e stare TEM de ser distinguiveis, senao o contraste some."""
    clipe = disfarce["animation.%s.idle" % ID_DISFARCE]
    corpo = clipe["bones"].get("body", {})
    vivo = any(v != vetor() for v in corpo.get("position", {}).values()) \
        or any(v != vetor() for v in corpo.get("rotation", {}).values())
    if not vivo:
        raise SystemExit(
            "o idle humano nao mexe o tronco. Sem respiracao ele vira um segundo"
            " stare, e a pista do mob deixa de ser um contraste para virar o estado"
            " normal do bicho.")


def conferir_a_marcha(disfarce, revelado):
    """'Nao pode parecer marcha de bicho' vira dois numeros conferiveis."""
    humano = disfarce["animation.%s.walk" % ID_DISFARCE]["bones"]
    inclinacao = max(abs(v[0]) for v in humano["body"]["rotation"].values())
    if inclinacao > 1e-9:
        raise SystemExit(
            "a caminhada humana inclina o tronco em %.1f graus (X). Basta inclinar"
            " o peito para a frente e a silhueta vira bicho, por mais correta que a"
            " perna esteja -- o peso do humano vai em Z (rolamento), nunca em X."
            % inclinacao)
    if HUMANO_BRACO >= HUMANO_PERNA:
        raise SystemExit(
            "no humano o braco oscila %.0f e a perna %.0f: braco com a amplitude da"
            " perna e marcha de quem carrega o proprio peso nos bracos, que e o"
            " primata." % (HUMANO_BRACO, HUMANO_PERNA))
    for nome, braco, perna in (("walk", APE_WALK_BRACO, APE_WALK_PERNA),
                               ("run", APE_RUN_BRACO, APE_RUN_PERNA)):
        if braco <= perna:
            raise SystemExit(
                "no primata o %s tem braco %.0f e perna %.0f: o braco longo e que"
                " PUXA o corpo, e com a perna mandando o macaco anda como gente"
                " agachada." % (nome, braco, perna))
        corpo = revelado["animation.%s.%s" % (ID_REVELADO, nome)]["bones"]["body"]
        if min(v[0] for v in corpo["rotation"].values()) > BODY_X + 1e-9:
            raise SystemExit("o %s do primata nao mantem o tronco inclinado" % nome)


# ------------------------------------------------------------- serializacao

def serializar(valor, recuo=0):
    """JSON com os vetores em UMA linha.

    json.dump(indent=2) quebra [0, -24, 0] em quatro linhas e o arquivo deixa de
    ser legivel num diff -- que e o unico lugar onde a outra pessoa vai conferir
    uma pose.
    """
    espaco = "  " * recuo
    if isinstance(valor, dict):
        if not valor:
            return "{}"
        itens = ",\n".join('%s  "%s": %s' % (espaco, chave, serializar(v, recuo + 1))
                           for chave, v in valor.items())
        return "{\n%s\n%s}" % (itens, espaco)
    if isinstance(valor, list):
        return "[%s]" % ", ".join(json.dumps(v) for v in valor)
    return json.dumps(valor)


def ordenar(clipe):
    """Keyframes em ordem de tempo: JSON nao garante ordem, diff humano exige."""
    for canais in clipe["bones"].values():
        for canal, quadros in list(canais.items()):
            canais[canal] = dict(sorted(quadros.items(), key=lambda kv: float(kv[0])))
    return clipe


def escrever(mob, animacoes):
    # SENTIDO DE Z. Este mob e um dos sete primeiros e nao passa por
    # `Animacoes.emitir`, onde a correcao mora para os demais -- mas a premissa
    # invertida era a MESMA, copiada de arquivo em arquivo. Chamar a funcao da
    # biblioteca em vez de repetir a negacao aqui e o que impede as duas copias
    # de divergirem no dia em que o sinal mudar.
    corrigir_sentido_de_z_em(mob, animacoes)
    destino = os.path.join(DIR_ANIM, mob + ".animation.json")
    os.makedirs(DIR_ANIM, exist_ok=True)
    texto = serializar({"format_version": "1.8.0", "animations": animacoes})
    json.loads(texto)  # o que sai daqui tem de voltar como JSON, sempre.
    with open(destino, "w", encoding="utf-8", newline="\n") as saida:
        saida.write(texto + "\n")
    return destino


def tabela(titulo, animacoes):
    print("\n%s" % titulo)
    print("  %-44s %6s  %-19s %s" % ("clipe", "dur", "loop", "ossos"))
    for nome, clipe in animacoes.items():
        print("  %-44s %5.2fs  %-19s %s"
              % (nome, clipe["animation_length"], str(clipe["loop"]),
                 ", ".join(sorted(clipe["bones"]))))


# ------------------------------------------------------------------- main

def main():
    geo_d = carregar_geo(ID_DISFARCE)
    geo_r = carregar_geo(ID_REVELADO)

    conferir_eixos(geo_d, ID_DISFARCE)
    conferir_eixos(geo_r, ID_REVELADO)
    conferir_postura_nao_duplicada(geo_r, ID_REVELADO)

    agacha = agachamento(geo_r)
    queda = queda_da_morte(geo_r)
    capuz = angulo_do_capuz(geo_d)

    disfarce = {
        "animation.%s.idle" % ID_DISFARCE: ordenar(humano_idle()),
        "animation.%s.walk" % ID_DISFARCE: ordenar(humano_walk()),
        "animation.%s.stare" % ID_DISFARCE: ordenar(humano_stare()),
        "animation.%s.reveal" % ID_DISFARCE: ordenar(humano_reveal(agacha, capuz)),
    }
    revelado = {
        "animation.%s.idle" % ID_REVELADO: ordenar(ape_idle(agacha)),
        "animation.%s.walk" % ID_REVELADO: ordenar(ape_walk(agacha)),
        "animation.%s.run" % ID_REVELADO: ordenar(ape_run(agacha)),
        "animation.%s.strike" % ID_REVELADO: ordenar(ape_strike(agacha)),
        "animation.%s.hurt" % ID_REVELADO: ordenar(ape_hurt(agacha)),
        "animation.%s.death" % ID_REVELADO: ordenar(ape_death(agacha, queda)),
    }

    conferir_clipes(disfarce, ID_DISFARCE, CLIPES_DISFARCE)
    conferir_clipes(revelado, ID_REVELADO, CLIPES_REVELADO)
    conferir_ossos(disfarce, geo_d, OSSOS_DISFARCE, ID_DISFARCE)
    conferir_ossos(revelado, geo_r, OSSOS_REVELADO, ID_REVELADO)
    conferir_ossos_exclusivos(disfarce, EXCLUSIVOS_DISFARCE, ID_DISFARCE)
    conferir_ossos_exclusivos(revelado, EXCLUSIVOS_REVELADO, ID_REVELADO)
    conferir_que_trocar_os_pares_reprova(disfarce, revelado, geo_d, geo_r)
    conferir_ossos_com_volume(disfarce, geo_d, ID_DISFARCE)
    conferir_ossos_com_volume(revelado, geo_r, ID_REVELADO)
    conferir_duracoes(disfarce, revelado)
    conferir_a_janela_do_golpe(revelado)
    conferir_a_costura(disfarce, revelado, agacha)
    conferir_o_stare(disfarce)
    conferir_idle_vivo(disfarce)
    conferir_a_marcha(disfarce, revelado)

    print("escrito", escrever(ID_DISFARCE, disfarce))
    print("escrito", escrever(ID_REVELADO, revelado))
    print("\nnumeros LIDOS do geo:")
    print("  agachamento do primata : %.2f px" % agacha)
    print("  queda da morte         : %.2f px" % queda)
    print("  angulo do capuz        : %.0f graus" % capuz)
    tabela("FORMA HUMANA (%s)" % ID_DISFARCE, disfarce)
    tabela("FORMA REVELADA (%s)" % ID_REVELADO, revelado)


if __name__ == "__main__":
    main()
