"""Gera as OITO animacoes do frog-in-waiting (GeckoLib 4.8.3, Bedrock 1.8.0).

DECISAO QUE ESTE ARQUIVO CARREGA (ADR-017): o sapo deixa de vestir o sapo
vanilla. Corpo proprio quer dizer tambem MOVIMENTO proprio -- e movimento que
mente sobre o comportamento e pior do que movimento nenhum, porque o jogador
aprende errado e nenhum portao acusa.

POR QUE UM GERADOR, E NAO JSON ESCRITO A MAO
--------------------------------------------
1. A profundidade do enterro NAO E GOSTO: ela sai do proprio geo. Enterrado, o
   corpo desce ate a base dos olhos encostar em y=0. Escrito a mao, esse numero
   vira uma constante que sobrevive a proxima correcao do modelo -- e o sapo
   volta a boiar (ou some inteiro) sem um unico erro no log. Aqui ele e LIDO
   (ver profundidade_do_enterro) e conferido dos dois lados.
2. Nome de osso errado NAO da erro no GeckoLib: ele ignora o osso e o membro
   fica parado. Aqui isso reprova (conferir_ossos), contra o contrato E contra
   o geo de verdade.
3. As duracoes COPIAM os ticks que o servidor ja usa em HunterExamProfiles.
   Emerge 10 ticks = 0.5s, bocada 4 = 0.2s, agarrao 100 = 5.0s. Um emerge de
   0.2s num aviso de 0.5s promete a bocada antes da hora. Os numeros moram em
   TICKS_* abaixo, uma vez.

EIXOS -- CONFERIDOS no proprio geo, nao chutados
-------------------------------------------------
A convencao vanilla e y=0 no chao, -Z na FRENTE, +X a ESQUERDA do bicho. Este
gerador NAO confia nisso: conferir_eixos() le o geo e reprova se a cabeca nao
estiver a frente do corpo, se a mandibula nao apontar para a frente ou se
eye_left nao estiver do lado +X. Um geo com a frente invertida produziria um
sapo que morde para tras -- perfeitamente silencioso.

Derivado dessa convencao (e so dela):

    rotacao X positiva  -> focinho SOBE (ponto em -Z vai para +Y);
                           pe balanca para a FRENTE (ponto em -Y vai para -Z)
    rotacao X negativa  -> focinho DESCE; a MANDIBULA ABRE (a ponta esta em -Z)
    rotacao Z positiva  -> o lado esquerdo (+X) sobe; pe vai para a ESQUERDA
    posicao  -Y         -> agacha / afunda no chao

A VITIMA ESTA EM CIMA DA TOCA, e isso manda na direcao do ataque: o emerge e a
bocada sao para CIMA (rotacao X positiva na cabeca), nao para a frente. Um bote
horizontal passaria por baixo de quem esta pisando no sapo.

Regerar:  python art-source/enemies/frog_in_waiting/frog_in_waiting_animacoes.py
Exporta:  src/main/resources/assets/nenfoundation/animations/entity/frog_in_waiting.animation.json
"""
import json
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum.animacao import corrigir_sentido_de_z_em  # noqa: E402

# --------------------------------------------------------------- o contrato

GEO = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                   "geo", "entity", "frog_in_waiting.geo.json")
DESTINO = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "animations", "entity", "frog_in_waiting.animation.json")

OSSOS = (
    "root", "body", "head", "jaw", "eye_left", "eye_right", "throat",
    "leg_front_left", "foot_front_left", "leg_front_right", "foot_front_right",
    "leg_back_left", "foot_back_left", "leg_back_right", "foot_back_right",
)

CLIPES = (
    "burrowed", "emerge", "bite", "digest", "idle", "walk", "hurt", "death",
)

PE = {
    "leg_front_left": "foot_front_left",
    "leg_front_right": "foot_front_right",
    "leg_back_left": "foot_back_left",
    "leg_back_right": "foot_back_right",
}
# O pe contra-gira uma fracao da perna para a sola continuar mais ou menos
# paralela ao chao em vez de apontar para o jogador.
CONTRA_PE = -0.45

# Quanto do CRANIO sobra acima do chao no burrowed, alem dos olhos inteiros.
# O contrato pede "so o alto do cranio e os olhos": parar exatamente na base dos
# olhos deixaria a coroa da cabeca RENTE ao chao, e o que o jogador veria seriam
# dois olhos boiando sem bicho embaixo. 1 px de crosta e o que amarra os olhos a
# uma cabeca.
MARGEM_DE_CRANIO = 1.0

# Ticks que o SERVIDOR usa. Copiados de HunterExamProfiles.frogSwallow() e
# frogGrabRules(); se um deles mudar la, muda aqui e regera.
TICKS_POR_SEGUNDO = 20.0
TICKS_EMERGE = 10    # frogSwallow().windupTicks -- o UNICO aviso
TICKS_BOCADA = 4     # frogSwallow().activeTicks
TICKS_AGARRAO = 100  # frogGrabRules().ticksMaximos

DUR_EMERGE = TICKS_EMERGE / TICKS_POR_SEGUNDO   # 0.5
DUR_BOCADA = TICKS_BOCADA / TICKS_POR_SEGUNDO   # 0.2
# O agarrao dura 100 ticks e o clipe REPETE em vez de congelar: 2.5s cabe duas
# vezes exatas dentro dele, entao a ultima pulsada termina junto com a soltura.
DUR_DIGEST = TICKS_AGARRAO / TICKS_POR_SEGUNDO / 2.0  # 2.5

# Pose em que o emerge TERMINA e a bocada COMECA. Mora aqui uma vez: escrita nos
# dois clipes, a primeira correcao em um deles produz um salto de pose entre o
# aviso e a mordida -- visivel, e sem nada que acuse.
FIM_DO_EMERGE = {
    "body_y": 0.8,
    "body_x": 9.0,
    "head_x": 14.0,
    "jaw_x": -26.0,
    "throat": 1.05,
    "olho": 1.2,
    "perna_frente": 12.0,
    "perna_tras": -20.0,
}

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
    """Acumula keyframes de um canal (rotation/position/scale) de um osso."""
    alvo = bones.setdefault(osso, {}).setdefault(canal, {})
    for t, v in pares:
        alvo[tempo(t)] = v


def perna(bones, nome, pares):
    """Keya a perna e DERIVA o pe dela.

    O pe escrito a mao fica com a fase certa hoje e errada na primeira correcao
    da perna -- e pe fora de fase nao da erro, da um bicho que arrasta a sola.
    """
    curva(bones, nome, "rotation", pares)
    curva(bones, PE[nome], "rotation",
          [(t, vetor(x=v[0] * CONTRA_PE)) for t, v in pares])


def piscada(bones, instantes, duracao=0.07, abertura=0.12):
    """Piscar e ESCALA em Y, nos dois olhos, sempre junto.

    Um olho piscando sozinho nao e estilo, e um erro de digitacao que ninguem
    revisa num JSON de 1200 linhas.
    """
    for olho in ("eye_left", "eye_right"):
        pares = [(0.0, escala())]
        for t in instantes:
            pares += [(t - duracao, escala()), (t, escala(y=abertura)),
                      (t + duracao, escala())]
        curva(bones, olho, "scale", pares)


# ------------------------------------------------------------------ clipes


def burrowed(fundo):
    """O ESTADO MAIS IMPORTANTE DESTE MOB.

    O corpo inteiro desce ABAIXO do chao ate a base dos olhos encostar em y=0;
    o que sobra na superficie e o alto do cranio e os olhos. E assim que
    "enterrado" se faz com modelo proprio, e e melhor do que a solucao antiga
    (esconder partes do modelo vanilla no cliente): aqui o que desaparece
    desaparece porque o BLOCO esta na frente, nao porque alguem lembrou de
    apagar um cubo.

    So os olhos se mexem, e minimamente. Enterrado, o sapo e uma pedra -- e
    qualquer movimento a mais entrega a emboscada de graca.
    """
    dur = 3.0
    b = {}
    curva(b, "body", "position", [(0.0, vetor(y=-fundo)), (dur, vetor(y=-fundo))])
    for olho in ("eye_left", "eye_right"):
        curva(b, olho, "position", [
            (0.0, vetor()), (1.4, vetor(y=0.15)), (2.2, vetor(y=0.05)),
            (dur, vetor())])
    piscada(b, [2.5])
    for olho in ("eye_left", "eye_right"):
        curva(b, olho, "scale", [(dur, escala())])
    return {"loop": True, "animation_length": dur, "bones": b}


def emerge(fundo):
    """10 ticks -- O UNICO AVISO antes da bocada.

    Comeca EXATAMENTE na pose de burrowed (por isso recebe o mesmo `fundo`):
    partir do neutro faria o sapo saltar do chao ja inteiro fora, e o meio
    segundo de aviso viraria um quadro.

    hold_on_last_frame: quem manda no tempo e o servidor. Se a fase WINDUP durar
    mais que 0.5s, o sapo segue com a boca armada em vez de voltar ao neutro e
    mentir que desistiu.
    """
    dur = DUR_EMERGE
    b = {}
    # O corpo sobe; a subida e desacelerada no fim, que e o que le como terra
    # sendo empurrada e nao como um elevador.
    curva(b, "body", "position", [
        (0.0, vetor(y=-fundo)), (0.12, vetor(y=-fundo * 0.62)),
        (0.28, vetor(y=-fundo * 0.22)), (0.4, vetor(y=0.5)),
        (dur, vetor(y=FIM_DO_EMERGE["body_y"]))])
    # X positivo = focinho para CIMA. A presa esta EM CIMA da toca.
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.12, vetor(x=3.0)), (0.28, vetor(x=7.0)),
        (dur, vetor(x=FIM_DO_EMERGE["body_x"]))])
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.12, vetor(x=4.0)), (0.28, vetor(x=10.0)),
        (dur, vetor(x=FIM_DO_EMERGE["head_x"]))])
    # A boca COMECA a abrir aqui e so termina na bocada: o jogador ve a fenda
    # antes de ver o buraco.
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (0.12, vetor(x=-6.0)), (0.28, vetor(x=-16.0)),
        (0.4, vetor(x=-23.0)), (dur, vetor(x=FIM_DO_EMERGE["jaw_x"]))])
    curva(b, "throat", "scale", [
        (0.0, escala()), (0.28, escala(1.02, 1.02, 1.02)),
        (dur, escala(FIM_DO_EMERGE["throat"], FIM_DO_EMERGE["throat"],
                     FIM_DO_EMERGE["throat"]))])
    # As traseiras empurram para cima (pe para TRAS, x negativo); as dianteiras
    # plantam a frente.
    perna(b, "leg_back_left", [
        (0.0, vetor()), (0.12, vetor(x=-8.0)), (0.28, vetor(x=-16.0)),
        (dur, vetor(x=FIM_DO_EMERGE["perna_tras"]))])
    perna(b, "leg_back_right", [
        (0.0, vetor()), (0.12, vetor(x=-8.0)), (0.28, vetor(x=-16.0)),
        (dur, vetor(x=FIM_DO_EMERGE["perna_tras"]))])
    perna(b, "leg_front_left", [
        (0.0, vetor()), (0.12, vetor(x=5.0)), (0.28, vetor(x=9.0)),
        (dur, vetor(x=FIM_DO_EMERGE["perna_frente"]))])
    perna(b, "leg_front_right", [
        (0.0, vetor()), (0.12, vetor(x=5.0)), (0.28, vetor(x=9.0)),
        (dur, vetor(x=FIM_DO_EMERGE["perna_frente"]))])
    # Olho ARREGALADO: a leitura de que ele ja escolheu quem vai comer.
    olho = FIM_DO_EMERGE["olho"]
    for lado in ("eye_left", "eye_right"):
        curva(b, lado, "scale", [
            (0.0, escala()), (0.16, escala(1.1, 1.1, 1.1)),
            (dur, escala(olho, olho, olho))])
    return {"loop": "hold_on_last_frame", "animation_length": dur, "bones": b}


def bite():
    """4 ticks. A boca abre por inteiro e fecha. Rapido e brutal.

    Comeca na pose em que o emerge parou -- FIM_DO_EMERGE -- e nao no neutro.
    Recomecar do neutro custaria um quadro de "voltar ao normal" bem no meio do
    bote, e 4 ticks nao tem quadro sobrando.

    NAO e loop: a mordida acontece uma vez. O que se repete depois e o digest.
    """
    dur = DUR_BOCADA
    b = {}
    p = FIM_DO_EMERGE
    curva(b, "body", "position", [
        (0.0, vetor(y=p["body_y"])), (0.06, vetor(y=1.6)),
        (0.13, vetor(y=0.2)), (dur, vetor())])
    curva(b, "body", "rotation", [
        (0.0, vetor(x=p["body_x"])), (0.06, vetor(x=13.0)),
        (0.13, vetor(x=-2.0)), (dur, vetor())])
    curva(b, "head", "rotation", [
        (0.0, vetor(x=p["head_x"])), (0.06, vetor(x=20.0)),
        (0.13, vetor(x=2.0)), (0.17, vetor(x=-3.0)), (dur, vetor())])
    # -62 graus: a gula inteira do bicho num numero. Ele engole um golem.
    curva(b, "jaw", "rotation", [
        (0.0, vetor(x=p["jaw_x"])), (0.06, vetor(x=-62.0)),
        (0.13, vetor(x=-4.0)), (0.17, vetor(x=-8.0)), (dur, vetor())])
    curva(b, "throat", "scale", [
        (0.0, escala(p["throat"], p["throat"], p["throat"])),
        (0.06, escala(1.12, 1.12, 1.12)), (0.13, escala(1.04, 1.04, 1.04)),
        (dur, escala())])
    perna(b, "leg_back_left", [
        (0.0, vetor(x=p["perna_tras"])), (0.06, vetor(x=-24.0)),
        (0.13, vetor(x=-6.0)), (dur, vetor())])
    perna(b, "leg_back_right", [
        (0.0, vetor(x=p["perna_tras"])), (0.06, vetor(x=-24.0)),
        (0.13, vetor(x=-6.0)), (dur, vetor())])
    perna(b, "leg_front_left", [
        (0.0, vetor(x=p["perna_frente"])), (0.06, vetor(x=18.0)),
        (0.13, vetor(x=4.0)), (dur, vetor())])
    perna(b, "leg_front_right", [
        (0.0, vetor(x=p["perna_frente"])), (0.06, vetor(x=18.0)),
        (0.13, vetor(x=4.0)), (dur, vetor())])
    for lado in ("eye_left", "eye_right"):
        curva(b, lado, "scale", [
            (0.0, escala(p["olho"], p["olho"], p["olho"])),
            (0.06, escala(1.15, 1.15, 1.15)),
            (0.13, escala(0.9, 0.9, 0.9)), (dur, escala())])
    return {"loop": False, "animation_length": dur, "bones": b}


def digest():
    """A vitima esta presa. 2.5s que cabem DUAS VEZES nos 100 ticks do agarrao.

    LOOP, e nao hold: congelar num quadro faria a digestao parecer travamento
    justo nos cinco segundos em que o jogador esta olhando o mob de perto e
    batendo nele para se soltar.

    Quem diz ao cliente que ha alguem preso e a fase RECOVERY, que o servidor
    publica enquanto digere -- estaAgarrando() e verdade so do servidor.
    """
    dur = DUR_DIGEST
    b = {}
    # Corpo PESADO: afundado, e cada engolida empurra o peso para baixo de novo.
    curva(b, "body", "position", [
        (0.0, vetor(y=-0.7)), (0.62, vetor(y=-1.1)), (1.25, vetor(y=-0.7)),
        (1.88, vetor(y=-1.0)), (dur, vetor(y=-0.7))])
    curva(b, "body", "rotation", [
        (0.0, vetor(x=-2.0)), (0.62, vetor(x=-4.0)), (1.25, vetor(x=-2.0)),
        (1.88, vetor(x=-3.5)), (dur, vetor(x=-2.0))])
    # A GARGANTA e o clipe: duas pulsadas lentas por volta, quatro no agarrao.
    curva(b, "throat", "scale", [
        (0.0, escala(1.02, 1.02, 1.02)), (0.45, escala(1.16, 1.2, 1.16)),
        (0.95, escala(1.04, 1.05, 1.04)), (1.25, escala(1.02, 1.02, 1.02)),
        (1.7, escala(1.14, 1.18, 1.14)), (2.2, escala(1.04, 1.05, 1.04)),
        (dur, escala(1.02, 1.02, 1.02))])
    curva(b, "throat", "position", [
        (0.0, vetor()), (0.45, vetor(y=-0.5)), (0.95, vetor(y=-0.15)),
        (1.7, vetor(y=-0.45)), (dur, vetor())])
    # Mandibula FECHADA, com o aperto que acompanha cada engolida. Boca aberta
    # aqui diria que a vitima pode sair andando.
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (0.45, vetor(x=3.0)), (0.95, vetor()),
        (1.7, vetor(x=2.5)), (dur, vetor())])
    # Pernas escoradas sob o peso: abertas (Z espelhado) e comprimidas.
    perna(b, "leg_front_left", [
        (0.0, vetor(x=9.0)), (0.62, vetor(x=11.0)), (1.25, vetor(x=9.0)),
        (1.88, vetor(x=10.5)), (dur, vetor(x=9.0))])
    perna(b, "leg_front_right", [
        (0.0, vetor(x=9.0)), (0.62, vetor(x=11.0)), (1.25, vetor(x=9.0)),
        (1.88, vetor(x=10.5)), (dur, vetor(x=9.0))])
    perna(b, "leg_back_left", [
        (0.0, vetor(x=10.0)), (0.62, vetor(x=13.0)), (1.25, vetor(x=10.0)),
        (1.88, vetor(x=12.0)), (dur, vetor(x=10.0))])
    perna(b, "leg_back_right", [
        (0.0, vetor(x=10.0)), (0.62, vetor(x=13.0)), (1.25, vetor(x=10.0)),
        (1.88, vetor(x=12.0)), (dur, vetor(x=10.0))])
    # Olhos MEIO CERRADOS o tempo todo -- e o que separa "digerindo" de
    # "procurando a proxima vitima".
    for lado in ("eye_left", "eye_right"):
        curva(b, lado, "scale", [
            (0.0, escala(y=0.5)), (0.45, escala(y=0.38)), (0.95, escala(y=0.52)),
            (1.7, escala(y=0.4)), (dur, escala(y=0.5))])
    return {"loop": True, "animation_length": dur, "bones": b}


def idle():
    """Parado fora da terra: so a garganta respira e o olho pisca.

    O CORPO NAO SE MEXE, de proposito. Sapo parado e parado -- dar a ele um
    balanco de respiracao no tronco faria a silhueta oscilar e estragaria o
    contraste com o walk, que e onde ele deve parecer vivo.
    """
    dur = 4.0
    b = {}
    curva(b, "throat", "scale", [
        (0.0, escala()), (0.9, escala(1.05, 1.08, 1.05)), (1.9, escala()),
        (2.8, escala(1.04, 1.07, 1.04)), (dur, escala())])
    curva(b, "throat", "position", [
        (0.0, vetor()), (0.9, vetor(y=-0.25)), (1.9, vetor()),
        (2.8, vetor(y=-0.2)), (dur, vetor())])
    piscada(b, [1.5, 3.3])
    for olho in ("eye_left", "eye_right"):
        curva(b, olho, "scale", [(dur, escala())])
    return {"loop": True, "animation_length": dur, "bones": b}


def walk():
    """SAPO NAO ANDA, PULA. Ciclo balistico de 0.8s.

    Por que keyframes explicitos e nao a cossenoide do great stamp: marcha de
    quadrupede E uma senoide, um pulo NAO E. O pulo tem agachamento, extensao,
    voo e absorcao, e cada um tem duracao propria. Amostrar um cosseno aqui
    produziria um sapo flutuando em marcha -- um mob mentindo sobre o que e.

    A ordem das fases mora nos comentarios de cada trecho porque e ela, e nao os
    numeros, que alguem vai precisar conferir depois.
    """
    dur = 0.8
    b = {}
    # 0.00 neutro | 0.12 agacha | 0.24 extende | 0.40 apice | 0.56 toca com as
    # dianteiras | 0.66 absorve | 0.80 neutro (== 0.00, o loop fecha).
    curva(b, "body", "position", [
        (0.0, vetor()), (0.12, vetor(y=-1.4)), (0.24, vetor(y=1.8)),
        (0.4, vetor(y=2.6)), (0.56, vetor(y=0.4)), (0.66, vetor(y=-0.9)),
        (dur, vetor())])
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.12, vetor(x=-3.0)), (0.24, vetor(x=8.0)),
        (0.4, vetor(x=3.0)), (0.56, vetor(x=-7.0)), (0.66, vetor(x=-2.0)),
        (dur, vetor())])
    # Traseiras: dobradas sob o quadril no agachamento (pe para a FRENTE, +X),
    # estendidas para tras no impulso (-X). E esse par que faz o pulo.
    for tras in ("leg_back_left", "leg_back_right"):
        perna(b, tras, [
            (0.0, vetor()), (0.12, vetor(x=26.0)), (0.24, vetor(x=-38.0)),
            (0.4, vetor(x=-25.0)), (0.56, vetor(x=-10.0)), (0.66, vetor(x=14.0)),
            (dur, vetor())])
    # Dianteiras: recolhidas no agachamento, esticadas para receber o chao no
    # apice, comprimidas na absorcao.
    for frente in ("leg_front_left", "leg_front_right"):
        perna(b, frente, [
            (0.0, vetor()), (0.12, vetor(x=10.0)), (0.24, vetor(x=22.0)),
            (0.4, vetor(x=30.0)), (0.56, vetor(x=18.0)), (0.66, vetor(x=-6.0)),
            (dur, vetor())])
    # A garganta leva o tranco do pouso -- e o que da PESO ao pulo.
    curva(b, "throat", "scale", [
        (0.0, escala()), (0.24, escala(0.95, 0.92, 0.95)), (0.4, escala()),
        (0.62, escala(1.08, 1.12, 1.08)), (dur, escala())])
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.24, vetor(x=6.0)), (0.4, vetor(x=2.0)),
        (0.6, vetor(x=-5.0)), (dur, vetor())])
    return {"loop": True, "animation_length": dur, "bones": b}


def hurt():
    """Tranco curto de 5 ticks. Nao interrompe leitura de fase nenhuma.

    NAO KEYA position do body, so rotacao. O motivo e o enterro: `position` do
    body e o canal que carrega a profundidade, e um hurt que o escrevesse
    arrancaria o sapo do chao por cinco ticks. Rotacao nao tem esse problema.
    (Isso REDUZ o estrago; nao o elimina -- ver o relato da entrega.)
    """
    dur = 0.25
    b = {}
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.06, vetor(x=6.0, z=7.0)), (0.13, vetor(x=-2.0, z=-3.0)),
        (dur, vetor())])
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.06, vetor(x=12.0, y=8.0)), (0.14, vetor(x=-5.0, y=-4.0)),
        (dur, vetor())])
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (0.05, vetor(x=-20.0)), (0.14, vetor(x=-6.0)), (dur, vetor())])
    curva(b, "throat", "scale", [
        (0.0, escala()), (0.06, escala(1.1, 1.12, 1.1)), (0.16, escala(0.98, 0.97, 0.98)),
        (dur, escala())])
    for frente in ("leg_front_left", "leg_front_right"):
        perna(b, frente, [(0.0, vetor()), (0.06, vetor(x=-9.0)), (dur, vetor())])
    for tras in ("leg_back_left", "leg_back_right"):
        perna(b, tras, [(0.0, vetor()), (0.06, vetor(x=8.0)), (dur, vetor())])
    return {"loop": False, "animation_length": dur, "bones": b}


def death():
    """Tomba de lado em 1.2s; a GARGANTA e a ultima coisa a parar.

    hold_on_last_frame: a entidade so some depois do clipe, e um corpo que volta
    a pose neutra no ultimo quadro vira um bug visual de meio segundo.

    A ordem importa mais que as poses: corpo cai (0.2-0.85), pernas encolhem
    (0.2-0.9), olhos fecham (0.7-1.1) e a garganta ainda pulsa uma vez em 0.75,
    depois de tudo o resto ja ter desistido. E isso que faz a morte ler como
    morte, e nao como desligar o bicho.
    """
    dur = 1.2
    b = {}
    # Z positivo rola para a ESQUERDA (+X); a posicao acompanha o tombo.
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.2, vetor(x=3.0, z=10.0)), (0.5, vetor(x=2.0, z=38.0)),
        (0.85, vetor(z=68.0)), (dur, vetor(x=-1.0, z=82.0))])
    curva(b, "body", "position", [
        (0.0, vetor()), (0.2, vetor(y=-0.6)), (0.5, vetor(x=1.2, y=-2.2)),
        (0.85, vetor(x=2.2, y=-3.4)), (dur, vetor(x=2.6, y=-3.8))])
    # Encolhem para DENTRO: a dianteira recua (x negativo), a traseira avanca
    # (x positivo) -- as duas na direcao do centro do bicho.
    for frente in ("leg_front_left", "leg_front_right"):
        perna(b, frente, [
            (0.0, vetor()), (0.25, vetor(x=-16.0)), (0.6, vetor(x=-28.0)),
            (dur, vetor(x=-31.0))])
    for tras in ("leg_back_left", "leg_back_right"):
        perna(b, tras, [
            (0.0, vetor()), (0.25, vetor(x=14.0)), (0.6, vetor(x=26.0)),
            (dur, vetor(x=29.0))])
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.2, vetor(x=5.0)), (0.5, vetor(x=-15.0, z=-8.0)),
        (0.85, vetor(x=-22.0, z=-12.0)), (dur, vetor(x=-26.0, z=-14.0))])
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (0.35, vetor(x=-5.0)), (0.7, vetor(x=-11.0)),
        (dur, vetor(x=-16.0))])
    curva(b, "throat", "scale", [
        (0.0, escala()), (0.25, escala(1.08, 1.1, 1.08)), (0.5, escala()),
        (0.75, escala(1.05, 1.06, 1.05)), (0.95, escala()), (dur, escala())])
    for olho in ("eye_left", "eye_right"):
        curva(b, olho, "scale", [
            (0.0, escala()), (0.7, escala(y=0.6)), (1.1, escala(y=0.12)),
            (dur, escala(y=0.12))])
    return {"loop": "hold_on_last_frame", "animation_length": dur, "bones": b}


# ------------------------------------------------------------------ portoes


def carregar_geo():
    if not os.path.exists(GEO):
        raise SystemExit(
            "%s nao existe. Este gerador LE o geo -- a profundidade do enterro e"
            " os eixos saem de la. Sem ele nao ha o que animar." % GEO)
    geo = json.load(open(GEO, encoding="utf-8"))
    return geo["minecraft:geometry"][0]


def caixas(geometria, osso):
    for b in geometria["bones"]:
        if b["name"] == osso:
            return b.get("cubes", [])
    raise SystemExit("o geo nao tem o osso '%s'" % osso)


def faixa_y(geometria, ossos):
    """(menor y, maior y) dos cubos destes ossos, em px."""
    valores = []
    for osso in ossos:
        for c in caixas(geometria, osso):
            valores += [c["origin"][1], c["origin"][1] + c["size"][1]]
    if not valores:
        raise SystemExit("nenhum cubo em %s -- nao da para medir altura" % list(ossos))
    return min(valores), max(valores)


def centro_z(geometria, osso):
    cubos = caixas(geometria, osso)
    if not cubos:
        raise SystemExit("o osso '%s' nao tem cubo; nao da para ler o eixo Z" % osso)
    return sum(c["origin"][2] + c["size"][2] / 2.0 for c in cubos) / len(cubos)


def centro_x(geometria, osso):
    cubos = caixas(geometria, osso)
    if not cubos:
        raise SystemExit("o osso '%s' nao tem cubo; nao da para ler o eixo X" % osso)
    return sum(c["origin"][0] + c["size"][0] / 2.0 for c in cubos) / len(cubos)


def pivot(geometria, osso):
    for b in geometria["bones"]:
        if b["name"] == osso:
            return b["pivot"]
    raise SystemExit("o geo nao tem o osso '%s'" % osso)


def conferir_eixos(geometria):
    """Os eixos se LEEM do geo. Decorados, eles produzem um sapo ao contrario.

    As tres afirmacoes abaixo sao as unicas de que cada rotacao deste arquivo
    depende. Se o geo mudar de convencao, isto reprova ANTES de alguem ver um
    sapo mordendo para tras.
    """
    if centro_z(geometria, "head") >= centro_z(geometria, "body"):
        raise SystemExit(
            "a cabeca nao esta a frente do corpo em Z (head=%.1f, body=%.1f). Todas"
            " as rotacoes deste gerador supoem -Z = FRENTE."
            % (centro_z(geometria, "head"), centro_z(geometria, "body")))
    # A mandibula se mede contra o PROPRIO pivot (a dobradica), e nao contra a
    # cabeca: neste geo as duas ocupam a mesma faixa de Z, e comparar uma com a
    # outra reprovaria um modelo correto. O que importa para abrir a boca e que
    # a massa da mandibula esteja A FRENTE da dobradica.
    if centro_z(geometria, "jaw") >= pivot(geometria, "jaw")[2]:
        raise SystemExit(
            "a mandibula nao se estende a frente da propria dobradica (massa em"
            " z=%.1f, pivot em z=%.1f). Assim, 'abrir a boca' (rotacao X negativa)"
            " FECHA a boca -- e ninguem ve isso sem olhar o mob mordendo."
            % (centro_z(geometria, "jaw"), pivot(geometria, "jaw")[2]))
    if centro_x(geometria, "eye_left") <= 0:
        raise SystemExit(
            "eye_left esta em x=%.1f, e o contrato supoe +X = ESQUERDA. Com o"
            " sinal trocado, o tombo da morte cai para o lado errado."
            % centro_x(geometria, "eye_left"))


def profundidade_do_enterro(geometria):
    """Quanto o body desce no burrowed -- LIDO do geo, nunca decorado.

    A regra: o corpo desce ate faltar MARGEM_DE_CRANIO para a base dos OLHOS. O
    que fica abaixo de y=0 some por OCLUSAO -- e o bloco na frente, nao um cubo
    escondido no cliente. Sobram na superficie a coroa da cabeca e os olhos
    inteiros, que e exatamente o que o contrato pede.

    O portao morde dos dois lados, porque os dois erros sao silenciosos: sapo
    cuja pista visivel deixa de ser o par de olhos vira um calombo sem leitura,
    e sapo que so encosta o queixo no chao nao esta enterrado, esta agachado.
    """
    base_olhos, topo_olhos = faixa_y(geometria, ("eye_left", "eye_right"))
    fundo = base_olhos - MARGEM_DE_CRANIO
    corpo_baixo, corpo_alto = faixa_y(
        geometria, [o for o in OSSOS if o != "root" and caixas(geometria, o)])
    altura = corpo_alto - corpo_baixo
    visivel = corpo_alto - fundo
    # A REGRA INTEIRA depende de os olhos serem o ponto mais alto do bicho. Se
    # o geo ganhar uma crista ou um dorso acima deles, descer ate a linha dos
    # olhos deixa ESSA peca de fora, e a pista da emboscada passa a ser um
    # calombo no chao em vez de dois olhos observando.
    if corpo_alto > topo_olhos + 1e-9:
        raise SystemExit(
            "os olhos nao sao o ponto mais alto do modelo (algo sobe ate %.1f px e"
            " o topo dos olhos e %.1f). Enterrar pela linha dos olhos deixaria essa"
            " peca de fora e a pista da emboscada deixaria de ser um par de olhos."
            % (corpo_alto, topo_olhos))
    if visivel > 0.35 * altura:
        raise SystemExit(
            "enterrado, %.1f px de %.1f ficariam acima do chao (%.0f%%). Isso nao e"
            " um sapo enterrado, e um sapo agachado -- o jogador ve o bicho inteiro"
            " e a emboscada deixa de existir."
            % (visivel, altura, 100.0 * visivel / altura))
    return fundo


def conferir_ossos(animacoes, geometria):
    """Osso errado nao da erro no GeckoLib: o membro so fica parado.

    Morde dos dois lados -- contra o contrato E contra o geo de verdade. Uma
    lane renomeando um osso sem avisar a outra reprova AQUI, e nao na tela de
    quem joga.
    """
    do_contrato = set(OSSOS)
    do_geo = {b["name"] for b in geometria["bones"]}
    if do_geo != do_contrato:
        raise SystemExit("geo e contrato discordam de osso: %s"
                         % sorted(do_geo ^ do_contrato))
    for nome, clipe in animacoes.items():
        desconhecidos = sorted(set(clipe["bones"]) - do_contrato)
        if desconhecidos:
            raise SystemExit("%s move osso que nao existe: %s" % (nome, desconhecidos))


def conferir_clipes(animacoes):
    esperado = {"animation.frog_in_waiting." + c for c in CLIPES}
    if set(animacoes) != esperado:
        raise SystemExit("chaves fora do contrato: %s"
                         % sorted(set(animacoes) ^ esperado))
    for nome, clipe in animacoes.items():
        if clipe["loop"] not in (True, False, "hold_on_last_frame"):
            raise SystemExit("%s: loop invalido %r" % (nome, clipe["loop"]))
        fim = clipe["animation_length"]
        for osso, canais in clipe["bones"].items():
            for canal, quadros in canais.items():
                if canal not in ("rotation", "position", "scale"):
                    raise SystemExit("%s/%s: canal invalido %s" % (nome, osso, canal))
                for t, v in quadros.items():
                    if float(t) < 0 or float(t) > fim + 1e-9:
                        raise SystemExit("%s/%s: quadro %s fora de [0, %s]"
                                         % (nome, osso, t, fim))
                    if canal == "scale" and min(v) <= 0:
                        raise SystemExit("%s/%s: escala %s some com o osso"
                                         % (nome, osso, v))


def conferir_duracoes(animacoes):
    """As duracoes que o SERVIDOR manda nao podem divergir em silencio."""
    def dur(nome):
        return animacoes["animation.frog_in_waiting." + nome]["animation_length"]

    if abs(dur("emerge") - DUR_EMERGE) > 1e-9:
        raise SystemExit("emerge dura %.3fs e o windup do servidor da %.3fs"
                         % (dur("emerge"), DUR_EMERGE))
    if abs(dur("bite") - DUR_BOCADA) > 1e-9:
        raise SystemExit("bite dura %.3fs e a fase ACTIVE da %.3fs"
                         % (dur("bite"), DUR_BOCADA))
    voltas = TICKS_AGARRAO / TICKS_POR_SEGUNDO / dur("digest")
    if abs(voltas - round(voltas)) > 1e-6:
        raise SystemExit(
            "digest de %.3fs nao cabe um numero inteiro de vezes nos %d ticks do"
            " agarrao (%.3f voltas): a ultima pulsada seria cortada no meio."
            % (dur("digest"), TICKS_AGARRAO, voltas))


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


def main():
    geometria = carregar_geo()
    conferir_eixos(geometria)
    fundo = profundidade_do_enterro(geometria)

    animacoes = {
        "animation.frog_in_waiting.burrowed": ordenar(burrowed(fundo)),
        "animation.frog_in_waiting.emerge": ordenar(emerge(fundo)),
        "animation.frog_in_waiting.bite": ordenar(bite()),
        "animation.frog_in_waiting.digest": ordenar(digest()),
        "animation.frog_in_waiting.idle": ordenar(idle()),
        "animation.frog_in_waiting.walk": ordenar(walk()),
        "animation.frog_in_waiting.hurt": ordenar(hurt()),
        "animation.frog_in_waiting.death": ordenar(death()),
    }
    conferir_clipes(animacoes)
    # SENTIDO DE Z. Este mob e um dos sete primeiros e nao passa por
    # `Animacoes.emitir`, onde a correcao mora para os demais -- mas a premissa
    # invertida era a MESMA, copiada de arquivo em arquivo. Chamar a funcao da
    # biblioteca em vez de repetir a negacao aqui e o que impede as duas copias
    # de divergirem no dia em que o sinal mudar.
    corrigir_sentido_de_z_em("frog_in_waiting", animacoes)
    conferir_ossos(animacoes, geometria)
    conferir_duracoes(animacoes)

    os.makedirs(os.path.dirname(DESTINO), exist_ok=True)
    texto = serializar({"format_version": "1.8.0", "animations": animacoes})
    json.loads(texto)  # o que sai daqui tem de voltar como JSON, sempre.
    with open(DESTINO, "w", encoding="utf-8", newline="\n") as saida:
        saida.write(texto + "\n")

    print("escrito", DESTINO)
    print("  enterro lido do geo: %.1f px" % fundo)
    for nome, clipe in animacoes.items():
        print("  %-38s %4.2fs  loop=%-18s ossos=%d"
              % (nome, clipe["animation_length"], str(clipe["loop"]),
                 len(clipe["bones"])))


if __name__ == "__main__":
    main()
