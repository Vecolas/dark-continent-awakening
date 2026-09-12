"""Gera as OITO animacoes do great stamp (GeckoLib 4.8.3, formato Bedrock 1.8.0).

DECISAO QUE ESTE ARQUIVO CARREGA (ADR-017): o great stamp deixa de vestir o
hoglin. Corpo proprio quer dizer tambem MOVIMENTO proprio -- e movimento que
mente sobre o comportamento e pior do que movimento nenhum, porque o jogador
aprende errado e nenhum portao acusa.

POR QUE UM GERADOR, E NAO JSON ESCRITO A MAO
--------------------------------------------
1. Marcha de quadrupede e uma cossenoide com fase. Escrever 17 keyframes por
   osso a mao produz erro de fase que ninguem le no diff -- aparece como um
   bicho mancando.
2. Nome de osso errado NAO da erro no GeckoLib: ele ignora o osso e o membro
   fica parado. Aqui isso reprova (ver conferir_ossos), e reprova DOS DOIS
   LADOS: osso que o geo tem e o contrato nao, e vice-versa.
3. As duracoes nao sao gosto: elas COPIAM os ticks que o servidor ja usa em
   HunterExamProfiles. Windup 18 ticks = 0.9s, active 20 = 1.0s, atordoamento
   40 = 2.0s. Um telegrafo de 0.4s num windup de 0.9s promete um bote que ainda
   vai demorar meio segundo. Os numeros moram em TICKS_* abaixo, uma vez.

EIXOS -- lidos do PROPRIO geo, nao chutados
-------------------------------------------
great_stamp.geo.json poe a cabeca e as presas em z NEGATIVO, a cauda em z
positivo e tusk_left em x POSITIVO. Entao, no espaco do arquivo (Y para cima,
frente = -Z, esquerda = +X):

    rotacao X negativa   -> focinho/testa DESCE, cauda SOBE, corpo inclina a frente
    rotacao X positiva   -> perna balanca para a FRENTE (o pe vai para -Z)
    rotacao Z positiva   -> pe vai para a ESQUERDA (+X); espelhar o par
    posicao  +Z          -> para TRAS;  -Y -> agacha

Isso e geometria do arquivo, nao convencao decorada: a mesma rotacao que baixa
o focinho (cuja ponta esta em -Z) levanta a cauda (cuja ponta esta em +Z).

Regerar:  python art-source/enemies/great_stamp/great_stamp_animacoes.py
Exporta:  src/main/resources/assets/nenfoundation/animations/entity/great_stamp.animation.json
"""
import json
import math
import os

# --------------------------------------------------------------- o contrato

GEO = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                   "geo", "entity", "great_stamp.geo.json")
DESTINO = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "animations", "entity", "great_stamp.animation.json")

OSSOS = (
    "root", "body", "chest", "neck", "head", "jaw", "tusk_left", "tusk_right",
    "leg_front_left", "hoof_front_left", "leg_front_right", "hoof_front_right",
    "leg_back_left", "hoof_back_left", "leg_back_right", "hoof_back_right",
    "tail",
)

CLIPES = (
    "idle", "walk", "run", "windup", "charge", "stagger", "hurt", "death",
)

# Ticks que o SERVIDOR usa. Copiados de HunterExamProfiles.greatStampCharge()
# e greatStampChargeRules(); se um deles mudar la, muda aqui e regera.
TICKS_POR_SEGUNDO = 20.0
TICKS_WINDUP = 18      # greatStampCharge().windupTicks
TICKS_ACTIVE = 20      # greatStampCharge().activeTicks
TICKS_ATORDOAMENTO = 40  # greatStampChargeRules().ticksDeAtordoamento

DUR_WINDUP = TICKS_WINDUP / TICKS_POR_SEGUNDO      # 0.9
DUR_CHARGE = TICKS_ACTIVE / TICKS_POR_SEGUNDO      # 1.0
DUR_STAGGER = TICKS_ATORDOAMENTO / TICKS_POR_SEGUNDO  # 2.0

# Pares diagonais da marcha cruzada, como em QuadrupedModel: dianteira esquerda
# anda junto com traseira direita.
DIAGONAL_A = ("leg_front_left", "leg_back_right")
DIAGONAL_B = ("leg_front_right", "leg_back_left")
CASCO = {
    "leg_front_left": "hoof_front_left",
    "leg_front_right": "hoof_front_right",
    "leg_back_left": "hoof_back_left",
    "leg_back_right": "hoof_back_right",
}
# O casco contra-gira uma fracao da perna para nao apontar a sola para o jogador.
CONTRA_CASCO = -0.4

# -------------------------------------------------------------- ferramentas


def tempo(t):
    """Chave de keyframe: string, sempre com decimal, sem zero sobrando."""
    texto = ("%.4f" % round(t, 4)).rstrip("0")
    return texto + "0" if texto.endswith(".") else texto


def num(v):
    v = round(v, 2)
    return int(v) if v == int(v) else v


def vetor(x=0.0, y=0.0, z=0.0):
    return [num(x), num(y), num(z)]


def curva(bones, osso, canal, pares):
    """Acumula keyframes de um canal (rotation/position) de um osso."""
    alvo = bones.setdefault(osso, {}).setdefault(canal, {})
    for t, v in pares:
        alvo[tempo(t)] = v


def marcha(bones, duracao, ciclo, amplitude, fase_por_osso, amostras=8):
    """Cossenoide amostrada: e a marcha inteira, pernas e cascos.

    Amostrar a cossenoide (em vez de escrever extremos a mao) e o que mantem a
    fase certa quando o ciclo nao divide a duracao em numeros redondos.
    """
    passo = ciclo / amostras
    n = int(round(duracao / passo))
    for osso, (fase, escala) in fase_por_osso.items():
        pares_perna, pares_casco = [], []
        for i in range(n + 1):
            t = i * passo
            ang = amplitude * escala * math.cos(2 * math.pi * (t / ciclo + fase))
            pares_perna.append((t, vetor(x=ang)))
            pares_casco.append((t, vetor(x=ang * CONTRA_CASCO)))
        curva(bones, osso, "rotation", pares_perna)
        curva(bones, CASCO[osso], "rotation", pares_casco)


def balanco(duracao, ciclo, amplitude, base=0.0, fase=0.0, amostras=8):
    """Amostras de uma cossenoide simples, para bob de corpo e aceno de cabeca."""
    passo = ciclo / amostras
    n = int(round(duracao / passo))
    return [(i * passo, base + amplitude * math.cos(2 * math.pi * (i * passo / ciclo + fase)))
            for i in range(n + 1)]


# ------------------------------------------------------------------ clipes


def idle():
    """Respiracao lenta; o bicho parado ainda esta VIVO."""
    b = {}
    curva(b, "chest", "position", [
        (0.0, vetor()), (1.8, vetor(y=0.3)), (2.8, vetor(y=0.15)), (4.0, vetor())])
    curva(b, "chest", "rotation", [
        (0.0, vetor()), (1.8, vetor(x=-1.5)), (4.0, vetor())])
    curva(b, "neck", "rotation", [
        (0.0, vetor()), (1.2, vetor(x=1.5)), (2.6, vetor(x=-1.0)), (4.0, vetor())])
    curva(b, "head", "rotation", [
        (0.0, vetor()), (1.0, vetor(x=2.0, y=4.0)), (2.4, vetor(x=0.5, y=-3.0)),
        (3.2, vetor(x=1.5, y=2.0)), (4.0, vetor())])
    curva(b, "tail", "rotation", [
        (0.0, vetor()), (1.3, vetor(x=-2.0, y=7.0)), (2.7, vetor(x=1.0, y=-7.0)),
        (4.0, vetor())])
    return {"loop": True, "animation_length": 4.0, "bones": b}


def walk():
    """Marcha cruzada de quadrupede, ~1s por ciclo completo."""
    dur, ciclo = 1.0, 1.0
    b = {}
    marcha(b, dur, ciclo, 20.0, {
        DIAGONAL_A[0]: (0.0, 1.0), DIAGONAL_A[1]: (0.0, 0.95),
        DIAGONAL_B[0]: (0.5, 0.95), DIAGONAL_B[1]: (0.5, 1.0),
    })
    # Bob no dobro da frequencia da passada: o corpo sobe a cada apoio.
    curva(b, "body", "position",
          [(t, vetor(y=v)) for t, v in balanco(dur, ciclo / 2, 0.4, 0.0, 0.5)])
    curva(b, "chest", "rotation",
          [(t, vetor(x=v)) for t, v in balanco(dur, ciclo / 2, 1.2)])
    curva(b, "neck", "rotation",
          [(t, vetor(x=v)) for t, v in balanco(dur, ciclo / 2, 2.0, 1.0, 0.5)])
    curva(b, "head", "rotation",
          [(t, vetor(x=v)) for t, v in balanco(dur, ciclo / 2, 2.5, 1.5)])
    curva(b, "tail", "rotation", [
        (0.0, vetor()), (0.25, vetor(y=8.0)), (0.5, vetor()),
        (0.75, vetor(y=-8.0)), (1.0, vetor())])
    return {"loop": True, "animation_length": dur, "bones": b}


def run():
    """Mesma marcha, amplitude maior, corpo mais baixo e inclinado a frente."""
    dur, ciclo = 0.6, 0.6
    b = {}
    marcha(b, dur, ciclo, 32.0, {
        DIAGONAL_A[0]: (0.0, 1.0), DIAGONAL_A[1]: (0.0, 0.95),
        DIAGONAL_B[0]: (0.5, 0.95), DIAGONAL_B[1]: (0.5, 1.0),
    })
    curva(b, "body", "position",
          [(t, vetor(y=v)) for t, v in balanco(dur, ciclo / 2, 0.8, -0.8, 0.5)])
    # x negativo = frente do corpo baixa. Corre com o peito adiantado.
    curva(b, "body", "rotation",
          [(t, vetor(x=v)) for t, v in balanco(dur, ciclo / 2, 1.5, -5.0)])
    curva(b, "neck", "rotation",
          [(t, vetor(x=v)) for t, v in balanco(dur, ciclo / 2, 3.0, -8.0, 0.5)])
    curva(b, "head", "rotation",
          [(t, vetor(x=v)) for t, v in balanco(dur, ciclo / 2, 3.5, -6.0)])
    curva(b, "tail", "rotation", [
        (0.0, vetor(x=-10.0)), (0.15, vetor(x=-12.0, y=10.0)),
        (0.3, vetor(x=-10.0)), (0.45, vetor(x=-12.0, y=-10.0)),
        (0.6, vetor(x=-10.0))])
    return {"loop": True, "animation_length": dur, "bones": b}


def windup():
    """O TELEGRAFO. 18 ticks para o jogador decidir sair da frente.

    Termina SEGURANDO a pose: quem manda no tempo da carga e o servidor, nao o
    clipe. Se a fase durar mais do que 0.9s, o stamp continua mirando em vez de
    voltar ao neutro e mentir que desistiu.
    """
    dur = DUR_WINDUP
    b = {}
    # Cabeca desce ate a TESTA apontar para a frente: -24 no pescoco e -22 na
    # cabeca somam ~46 graus de focinho para baixo, que e o que vira a placa da
    # testa para o alvo.
    curva(b, "neck", "rotation", [
        (0.0, vetor()), (0.2, vetor(x=-11.0)), (0.45, vetor(x=-19.0)),
        (0.7, vetor(x=-23.0)), (dur, vetor(x=-24.0))])
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.2, vetor(x=-9.0)), (0.45, vetor(x=-17.0)),
        (0.7, vetor(x=-21.0)), (dur, vetor(x=-22.0))])
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (0.45, vetor(x=-6.0)), (0.7, vetor(x=-10.0)),
        (dur, vetor(x=-8.0))])
    # Corpo agacha e recua: +Z e para tras, -Y e agachar. Tomar impulso.
    curva(b, "body", "position", [
        (0.0, vetor()), (0.2, vetor(y=-0.4, z=0.6)), (0.45, vetor(y=-0.8, z=1.1)),
        (0.7, vetor(y=-1.0, z=1.4)), (dur, vetor(y=-1.0, z=1.5))])
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.45, vetor(x=-2.0)), (dur, vetor(x=-3.0))])
    # A pata dianteira DIREITA raspa: planta a frente (+x) e arrasta para tras
    # (-x), duas vezes dentro dos 18 ticks.
    curva(b, "leg_front_right", "rotation", [
        (0.0, vetor()), (0.15, vetor(x=16.0)), (0.35, vetor(x=-14.0)),
        (0.55, vetor(x=16.0)), (0.75, vetor(x=-14.0)), (dur, vetor(x=-8.0))])
    curva(b, "hoof_front_right", "rotation", [
        (0.0, vetor()), (0.15, vetor(x=-10.0)), (0.35, vetor(x=8.0)),
        (0.55, vetor(x=-10.0)), (0.75, vetor(x=8.0)), (dur, vetor(x=4.0))])
    # A esquerda trava o peso; as traseiras recolhem sob o corpo para empurrar.
    curva(b, "leg_front_left", "rotation", [
        (0.0, vetor()), (0.45, vetor(x=6.0)), (dur, vetor(x=8.0))])
    curva(b, "leg_back_left", "rotation", [
        (0.0, vetor()), (0.45, vetor(x=-8.0)), (dur, vetor(x=-12.0))])
    curva(b, "leg_back_right", "rotation", [
        (0.0, vetor()), (0.45, vetor(x=-8.0)), (dur, vetor(x=-12.0))])
    curva(b, "tail", "rotation", [
        (0.0, vetor()), (0.3, vetor(x=-12.0)), (dur, vetor(x=-20.0))])
    return {"loop": "hold_on_last_frame", "animation_length": dur, "bones": b}


def charge():
    """20 ticks de corrida reta. LOOP: corrida mais longa repete, nao congela.

    A cabeca fica TRAVADA baixa -- e a leitura de que a testa e a arma, e de
    que ele nao vai desviar.
    """
    dur, ciclo = DUR_CHARGE, 0.5
    b = {}
    # Galope: as duas dianteiras quase juntas, as traseiras meio ciclo depois.
    marcha(b, dur, ciclo, 34.0, {
        "leg_front_left": (0.0, 1.0), "leg_front_right": (0.04, 0.9),
        "leg_back_left": (0.5, 0.92), "leg_back_right": (0.54, 1.0),
    })
    curva(b, "body", "position",
          [(t, vetor(y=v, z=0.0)) for t, v in balanco(dur, ciclo / 2, 0.7, -1.0, 0.5)])
    curva(b, "body", "rotation",
          [(t, vetor(x=v)) for t, v in balanco(dur, ciclo / 2, 1.5, -7.0)])
    # Trava, com micro-tranco de impacto: a cabeca nao busca alvo, ja escolheu.
    curva(b, "neck", "rotation",
          [(t, vetor(x=v)) for t, v in balanco(dur, ciclo / 2, 2.0, -26.0)])
    curva(b, "head", "rotation",
          [(t, vetor(x=v)) for t, v in balanco(dur, ciclo / 2, 2.0, -24.0, 0.5)])
    curva(b, "jaw", "rotation", [
        (0.0, vetor(x=-6.0)), (0.5, vetor(x=-9.0)), (dur, vetor(x=-6.0))])
    curva(b, "tail", "rotation", [
        (0.0, vetor(x=-22.0)), (0.25, vetor(x=-24.0, y=7.0)),
        (0.5, vetor(x=-22.0)), (0.75, vetor(x=-24.0, y=-7.0)),
        (dur, vetor(x=-22.0))])
    return {"loop": True, "animation_length": dur, "bones": b}


def stagger():
    """Bateu na parede. 40 ticks em que a TESTA fica alcancavel.

    Nao e loop: o clipe precisa ler como "ele esta aberto AGORA", e abertura
    que se repete para sempre deixa de ser janela.
    """
    dur = DUR_STAGGER
    b = {}
    # Recua e afunda; volta ao normal so no fim dos 40 ticks.
    curva(b, "body", "position", [
        (0.0, vetor()), (0.12, vetor(y=-0.6, z=2.2)), (0.3, vetor(y=-1.2, z=2.6)),
        (0.75, vetor(y=-1.0, z=2.0)), (1.4, vetor(y=-0.6, z=1.2)),
        (dur, vetor())])
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.12, vetor(x=5.0, z=4.0)), (0.3, vetor(x=3.0, z=-3.0)),
        (0.75, vetor(x=4.0, z=2.0)), (1.4, vetor(x=2.0)), (dur, vetor())])
    # Cabeca baixa e chacoalhando: a placa da testa fica na altura do jogador.
    curva(b, "neck", "rotation", [
        (0.0, vetor()), (0.12, vetor(x=-18.0)), (0.3, vetor(x=-22.0)),
        (0.75, vetor(x=-20.0)), (1.4, vetor(x=-16.0)), (dur, vetor(x=-6.0))])
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.12, vetor(x=-20.0, y=22.0)), (0.3, vetor(x=-16.0, y=-18.0)),
        (0.5, vetor(x=-19.0, y=14.0)), (0.75, vetor(x=-15.0, y=-11.0)),
        (1.0, vetor(x=-17.0, y=7.0)), (1.4, vetor(x=-13.0, y=-4.0)),
        (1.7, vetor(x=-10.0, y=2.0)), (dur, vetor())])
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (0.12, vetor(x=-14.0)), (0.75, vetor(x=-10.0)),
        (1.4, vetor(x=-6.0)), (dur, vetor())])
    # Dianteiras ABREM: +Z leva o pe para +X (esquerda), entao o par espelha.
    curva(b, "leg_front_left", "rotation", [
        (0.0, vetor()), (0.12, vetor(x=14.0, z=10.0)), (0.3, vetor(x=18.0, z=14.0)),
        (0.75, vetor(x=16.0, z=13.0)), (1.4, vetor(x=10.0, z=8.0)), (dur, vetor())])
    curva(b, "leg_front_right", "rotation", [
        (0.0, vetor()), (0.12, vetor(x=14.0, z=-10.0)), (0.3, vetor(x=18.0, z=-14.0)),
        (0.75, vetor(x=16.0, z=-13.0)), (1.4, vetor(x=10.0, z=-8.0)), (dur, vetor())])
    curva(b, "hoof_front_left", "rotation", [
        (0.0, vetor()), (0.3, vetor(x=-8.0)), (1.4, vetor(x=-5.0)), (dur, vetor())])
    curva(b, "hoof_front_right", "rotation", [
        (0.0, vetor()), (0.3, vetor(x=-8.0)), (1.4, vetor(x=-5.0)), (dur, vetor())])
    # Traseiras cravam no chao para segurar o tranco.
    curva(b, "leg_back_left", "rotation", [
        (0.0, vetor()), (0.3, vetor(x=-14.0)), (1.4, vetor(x=-8.0)), (dur, vetor())])
    curva(b, "leg_back_right", "rotation", [
        (0.0, vetor()), (0.3, vetor(x=-14.0)), (1.4, vetor(x=-8.0)), (dur, vetor())])
    curva(b, "tail", "rotation", [
        (0.0, vetor()), (0.3, vetor(x=-18.0, y=12.0)), (0.75, vetor(x=-14.0, y=-9.0)),
        (1.4, vetor(x=-8.0, y=5.0)), (dur, vetor())])
    return {"loop": False, "animation_length": dur, "bones": b}


def hurt():
    """Tranco curto de 5 ticks. Nao interrompe leitura de fase nenhuma."""
    dur = 0.25
    b = {}
    curva(b, "body", "position", [
        (0.0, vetor()), (0.08, vetor(y=-0.8, z=0.6)), (0.16, vetor(y=-0.3)),
        (dur, vetor())])
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.08, vetor(z=5.0)), (0.16, vetor(z=-2.0)), (dur, vetor())])
    curva(b, "neck", "rotation", [
        (0.0, vetor()), (0.08, vetor(x=9.0)), (0.16, vetor(x=-3.0)), (dur, vetor())])
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.08, vetor(x=11.0, y=6.0)), (0.16, vetor(x=-4.0, y=-3.0)),
        (dur, vetor())])
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (0.08, vetor(x=-16.0)), (0.16, vetor(x=-6.0)), (dur, vetor())])
    curva(b, "leg_front_left", "rotation", [
        (0.0, vetor()), (0.08, vetor(x=-7.0)), (dur, vetor())])
    curva(b, "leg_front_right", "rotation", [
        (0.0, vetor()), (0.08, vetor(x=-7.0)), (dur, vetor())])
    return {"loop": False, "animation_length": dur, "bones": b}


def death():
    """Tomba para um lado; a cabeca e a ultima coisa a cair.

    hold_on_last_frame: a entidade so some depois do clipe, e um corpo que
    volta a pose neutra no ultimo quadro vira um bug visual de meio segundo.
    """
    dur = 1.5
    b = {}
    # Rolar em +Z leva o corpo para a esquerda (+X); cai junto com o afundamento.
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.25, vetor(x=4.0, z=7.0)), (0.5, vetor(x=6.0, z=24.0)),
        (0.8, vetor(x=5.0, z=52.0)), (1.1, vetor(x=3.0, z=72.0)),
        (dur, vetor(x=2.0, z=78.0))])
    curva(b, "body", "position", [
        (0.0, vetor()), (0.25, vetor(y=-1.0)), (0.5, vetor(y=-2.8)),
        (0.8, vetor(y=-4.5, x=1.5)), (1.1, vetor(y=-5.5, x=2.5)),
        (dur, vetor(y=-6.0, x=3.0))])
    # Pernas cedem: dianteiras dobram primeiro.
    curva(b, "leg_front_left", "rotation", [
        (0.0, vetor()), (0.25, vetor(x=22.0)), (0.5, vetor(x=34.0)),
        (0.8, vetor(x=26.0)), (dur, vetor(x=20.0))])
    curva(b, "leg_front_right", "rotation", [
        (0.0, vetor()), (0.25, vetor(x=18.0)), (0.5, vetor(x=31.0)),
        (0.8, vetor(x=24.0)), (dur, vetor(x=18.0))])
    curva(b, "leg_back_left", "rotation", [
        (0.0, vetor()), (0.5, vetor(x=-14.0)), (0.8, vetor(x=-26.0)),
        (dur, vetor(x=-22.0))])
    curva(b, "leg_back_right", "rotation", [
        (0.0, vetor()), (0.5, vetor(x=-12.0)), (0.8, vetor(x=-24.0)),
        (dur, vetor(x=-20.0))])
    for casco in CASCO.values():
        curva(b, casco, "rotation", [
            (0.0, vetor()), (0.5, vetor(x=-10.0)), (dur, vetor(x=-6.0))])
    # A cabeca resiste ate 0.8s e so entao tomba -- e o que faz a morte ler
    # como morte, e nao como desligar o bicho.
    curva(b, "neck", "rotation", [
        (0.0, vetor()), (0.3, vetor(x=12.0)), (0.8, vetor(x=8.0)),
        (1.1, vetor(x=-14.0)), (dur, vetor(x=-26.0))])
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.3, vetor(x=10.0, z=-4.0)), (0.8, vetor(x=4.0, z=-8.0)),
        (1.1, vetor(x=-16.0, z=-14.0)), (dur, vetor(x=-28.0, z=-18.0))])
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (0.3, vetor(x=-18.0)), (0.8, vetor(x=-14.0)),
        (dur, vetor(x=-9.0))])
    curva(b, "tail", "rotation", [
        (0.0, vetor()), (0.3, vetor(x=-8.0)), (0.8, vetor(x=6.0)),
        (dur, vetor(x=14.0))])
    return {"loop": "hold_on_last_frame", "animation_length": dur, "bones": b}


# ------------------------------------------------------------------ portoes


def conferir_ossos(animacoes):
    """Osso errado nao da erro no GeckoLib: o membro so fica parado.

    Morde dos dois lados -- contra o contrato E contra o geo de verdade, se ele
    existir. Uma lane renomeando um osso sem avisar a outra reprova AQUI, e nao
    na tela de quem joga.
    """
    do_contrato = set(OSSOS)
    if os.path.exists(GEO):
        geo = json.load(open(GEO, encoding="utf-8"))
        do_geo = {b["name"] for g in geo["minecraft:geometry"] for b in g["bones"]}
        if do_geo != do_contrato:
            raise SystemExit("geo e contrato discordam de osso: %s"
                             % sorted(do_geo ^ do_contrato))
    for nome, clipe in animacoes.items():
        desconhecidos = sorted(set(clipe["bones"]) - do_contrato)
        if desconhecidos:
            raise SystemExit("%s move osso que nao existe: %s" % (nome, desconhecidos))


def conferir_clipes(animacoes):
    esperado = {"animation.great_stamp." + c for c in CLIPES}
    if set(animacoes) != esperado:
        raise SystemExit("chaves fora do contrato: %s" % sorted(set(animacoes) ^ esperado))
    for nome, clipe in animacoes.items():
        if clipe["loop"] not in (True, False, "hold_on_last_frame"):
            raise SystemExit("%s: loop invalido %r" % (nome, clipe["loop"]))
        fim = clipe["animation_length"]
        for osso, canais in clipe["bones"].items():
            for canal, quadros in canais.items():
                if canal not in ("rotation", "position", "scale"):
                    raise SystemExit("%s/%s: canal invalido %s" % (nome, osso, canal))
                for t in quadros:
                    if float(t) < 0 or float(t) > fim + 1e-9:
                        raise SystemExit("%s/%s: quadro %s fora de [0, %s]"
                                         % (nome, osso, t, fim))


def serializar(valor, recuo=0):
    """JSON com os vetores em UMA linha.

    json.dump(indent=2) quebra [0, -24, 0] em quatro linhas e o arquivo deixa
    de ser legivel num diff -- que e o unico lugar onde a outra pessoa vai
    conferir uma pose.
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


def main():
    animacoes = {"animation.great_stamp." + nome: ordenar(fabrica())
                 for nome, fabrica in (
                     ("idle", idle), ("walk", walk), ("run", run),
                     ("windup", windup), ("charge", charge), ("stagger", stagger),
                     ("hurt", hurt), ("death", death))}
    conferir_clipes(animacoes)
    conferir_ossos(animacoes)

    os.makedirs(os.path.dirname(DESTINO), exist_ok=True)
    texto = serializar({"format_version": "1.8.0", "animations": animacoes})
    json.loads(texto)  # o que sai daqui tem de voltar como JSON, sempre.
    with open(DESTINO, "w", encoding="utf-8", newline="\n") as saida:
        saida.write(texto + "\n")

    print("escrito", DESTINO)
    for nome, clipe in animacoes.items():
        print("  %-34s %4.2fs  loop=%-18s ossos=%d"
              % (nome, clipe["animation_length"], str(clipe["loop"]), len(clipe["bones"])))


if __name__ == "__main__":
    main()
