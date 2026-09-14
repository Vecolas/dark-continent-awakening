"""Clipes do Bubble Horse -- a representacao das fases que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, STAGGER, EXAUSTAO OU CARD. O servidor publica a
fase (`AttackPhase`), o cambaleio e a exaustao; o cliente escolhe o clipe
correspondente. Se a animacao e a regra discordarem, quem esta errado e este
arquivo.

AS TRES COISAS QUE ESTES CLIPES PRECISAM CONTAR
------------------------------------------------
1. O SALTO. Este bicho nao caminha -- ele se move em impulsos com pausa
   (`RegrasDeSaltoDeBolha`). Por isso `walk` NAO e um ciclo de passada: e um
   ciclo de PAUSA + ARCO, e a duracao dele e a do ciclo do servidor, cobrada por
   `valida_ciclo_do_salto`. Um clipe de passada continua sobre uma locomocao de
   impulso da um cavalo que patina, e o jogador perde a unica pista que ele tem
   para prever onde o proximo salto cai.
2. A PATADA. Ela e o recurso de quem foi encurralado, e nao um ataque procurado.
   O telegrafo e a EMPINADA: o corpo sai do lugar, e nao so muda de forma. Quem
   encurtar o windup na animacao sem encurtar no servidor entrega o pior dos dois
   mundos -- a pata ja desceu na tela e o dano so sai depois, e o jogador aprende
   que desviar nao funciona.
3. O ESTOURO NA MORTE. A condicao de card deste bicho e nao-letal: matar CANCELA
   a captura. Se a morte dele parecer a morte de qualquer mob, o jogador nunca
   descobre o que fez de errado -- ele so nao recebe um card que nao sabia que
   existia. Por isso as bolhas COLAPSAM no clipe de morte, e
   `valida_bolha_estoura_na_morte` cobra isso contra a propria regra de captura.

CONVENCAO DE ROTACAO, medida neste geo e nao decorada. Com -Z na frente:
  * rotacao X NEGATIVA num membro pendurado (perna) o joga para a FRENTE e para
    cima; X POSITIVA o joga para TRAS e para cima;
  * rotacao X POSITIVA no tronco e no pescoco inclina para a FRENTE (focinho para
    baixo); NEGATIVA empina.

ESCALA NUNCA E DERIVADA. `anim.derivar` zera os eixos que nao foram pedidos, e um
vetor de escala com zero NAO da erro -- ele SOME com o osso. Toda pulsacao de
bolha e escrita como curva propria, com base 1.0.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/bubble_horse/bubble_horse_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/bubble_horse.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import ErroDeArte, TICKS_POR_SEGUNDO   # noqa: E402
from comum import animacao as anim                # noqa: E402

MOB = "bubble_horse"

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType do
# Java VENCE o JSON, e cravar a repeticao la transformaria este dicionario em
# documentacao que discorda do comportamento.
#
# Esta e tambem a UNICA lista de clipes deste mob. Uma tupla CLIPES ao lado seria
# a mesma informacao escrita duas vezes, e alguem acrescentaria um clipe em so uma
# delas.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o telegrafo termina com o bicho EMPINADO e fica assim.
    # Voltando ao repouso no fim do clipe, ele desarmaria o golpe na tela enquanto
    # o servidor ainda esta em WINDUP -- o jogador leria "passou" e levaria a
    # patada mesmo assim.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    # hold_on_last_frame: o corpo fica caido e as bolhas ficam murchas ate a
    # entidade sumir. Um clipe de morte que volta ao repouso mostraria as bolhas
    # INTEIRAS no ultimo quadro -- ou seja, apagaria justamente o sinal de que o
    # card se perdeu.
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de BubbleHorseTuning, com o campo de origem ao lado.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=14,     # BubbleHorseTuning.WINDUP_DA_PATADA
                       active=4,      # BubbleHorseTuning.JANELA_DA_PATADA
                       recovery=16)}  # BubbleHorseTuning.RECUPERACAO_DA_PATADA

# O ciclo do salto, COPIADO de BubbleHorseTuning, com o campo ao lado. A soma
# destes dois e a duracao obrigatoria do clipe `walk` -- ver
# valida_ciclo_do_salto.
TICKS_DE_PAUSA = 12   # BubbleHorseTuning.TICKS_DE_PAUSA_ENTRE_SALTOS
TICKS_DE_ARCO = 8     # BubbleHorseTuning.TICKS_DE_ARCO_DO_SALTO

DUR_IDLE = 3.0
DUR_WALK = (TICKS_DE_PAUSA + TICKS_DE_ARCO) / TICKS_POR_SEGUNDO
DUR_WINDUP = 0.7      # 14 ticks
DUR_STRIKE = 0.3      # 6 ticks -- dois a mais que a janela, de proposito
DUR_RECOVERY = 0.85   # 17 ticks
DUR_STAGGER = 0.6
DUR_DEATH = 1.2

# Instante, dentro do ciclo, em que a pausa acaba e o impulso comeca.
FIM_DA_PAUSA = TICKS_DE_PAUSA / TICKS_POR_SEGUNDO

# Quanto a bolha fica no fim do clipe de morte. NAO pode ser zero: escala zero nao
# da erro, ela SOME com o osso -- e um osso que some nao e uma bolha que estoura,
# e sim uma bolha que nunca existiu. Oito por cento ainda deixa um resto visivel,
# que e o que se le como "murchou".
ESCALA_DA_BOLHA_MORTA = 0.08

# Arrasto da bolha de apoio atras da perna. Negativo e pequeno: a bolha fica para
# tras do movimento, que e o que um saco de ar preso a um tornozelo faz. Fator
# positivo faria a bolha ANTECIPAR a perna -- o movimento de quem empurra o chao,
# e este bicho nao empurra o chao, ele flutua sobre ele.
ARRASTO_DA_BOLHA = -0.4

# As bolhas, pelo papel que cada uma cumpre na silhueta.
BOLHAS_DE_APOIO = ("bubble_front_left", "bubble_front_right",
                   "bubble_back_left", "bubble_back_right")
PERNA_DA_BOLHA = {
    "bubble_front_left": "leg_front_left",
    "bubble_front_right": "leg_front_right",
    "bubble_back_left": "leg_back_left",
    "bubble_back_right": "leg_back_right",
}


def _pulsar(bones, osso, duracao, amplitude, fase=0.0):
    """Pulsacao de bolha: escala isotropica em torno de 1.0.

    Escrita como curva, e nunca derivada. `anim.derivar` zera os eixos que nao
    foram pedidos, e escala zero nao levanta erro -- ela apaga o osso da tela.
    """
    anim.curva(bones, osso, "scale",
               [(t, anim.escala(v, v, v)) for t, v in
                anim.cossenoide(duracao, duracao, amplitude, base=1.0, fase=fase)])


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # ---------------------------------------------------------------- ocio
    # Ocio de bicho que nao esta a vontade: respiracao curta, cabeca alta e
    # bolhas pulsando fora de fase. As bolhas sao o unico movimento grande do
    # repouso -- num corpo parado, e a pulsacao delas que conta ao jogador que o
    # bicho esta VIVO, e vivo e a unica forma em que ele vale card.
    ocio = {}
    anim.curva(ocio, "body", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 0.35)])
    anim.curva(ocio, "neck", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.2, fase=0.15)])
    anim.derivar(ocio, "neck", "head", "rotation", -0.7)
    anim.curva(ocio, "tail", "rotation",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE / 2.0, 6.0)])
    # Fases espalhadas: quatro bolhas pulsando juntas leem como uma engrenagem.
    for i, bolha in enumerate(BOLHAS_DE_APOIO):
        _pulsar(ocio, bolha, DUR_IDLE, 0.05, fase=i * 0.25)
    _pulsar(ocio, "bubble_croup", DUR_IDLE, 0.06, fase=0.1)
    _pulsar(ocio, "bubble_nape", DUR_IDLE, 0.04, fase=0.6)
    a.clipe("idle", DUR_IDLE, ocio)

    # ------------------------------------------------------------- locomocao
    # `walk` E O SALTO, e nao uma passada. A primeira metade (ate FIM_DA_PAUSA) e
    # a PAUSA: o bicho se agacha e as bolhas achatam. Depois vem o ARCO: impulso,
    # apice, queda. A duracao inteira e o ciclo que o servidor executa, e
    # `valida_ciclo_do_salto` cobra isso.
    #
    # Se este clipe virasse uma passada continua, o servidor continuaria dando
    # impulsos e a tela mostraria um trote -- o bicho deslizaria entre saltos, e o
    # jogador perderia a pista de QUANDO o proximo salto sai. Nada disso levanta
    # erro: o dano, a velocidade e o log ficam iguais.
    salto = {}
    apice = FIM_DA_PAUSA + (DUR_WALK - FIM_DA_PAUSA) * 0.45
    anim.curva(salto, "body", "position",
               [(0.0, anim.vetor()),
                (FIM_DA_PAUSA * 0.75, anim.vetor(y=-1.1)),   # agacha
                (FIM_DA_PAUSA, anim.vetor(y=-1.4)),          # carrega
                (apice, anim.vetor(y=2.6)),                  # apice do arco
                (DUR_WALK, anim.vetor())])
    anim.curva(salto, "body", "rotation",
               [(0.0, anim.vetor()),
                (FIM_DA_PAUSA, anim.vetor(x=6)),             # focinho baixo ao carregar
                (apice, anim.vetor(x=-9)),                   # empina no impulso
                (DUR_WALK, anim.vetor())])
    anim.derivar(salto, "body", "neck", "rotation", -0.55)
    anim.derivar(salto, "neck", "head", "rotation", -0.5)
    # As pernas RECOLHEM no ar em vez de alternarem: alternar e passada, e passada
    # e o que este bicho nao faz.
    for perna in ("leg_front_left", "leg_front_right"):
        anim.curva(salto, perna, "rotation",
                   [(0.0, anim.vetor()),
                    (FIM_DA_PAUSA, anim.vetor(x=16)),
                    (apice, anim.vetor(x=-34)),
                    (DUR_WALK, anim.vetor())])
    for perna in ("leg_back_left", "leg_back_right"):
        anim.curva(salto, perna, "rotation",
                   [(0.0, anim.vetor()),
                    (FIM_DA_PAUSA, anim.vetor(x=-20)),
                    (apice, anim.vetor(x=26)),
                    (DUR_WALK, anim.vetor())])
    for bolha, perna in PERNA_DA_BOLHA.items():
        anim.derivar(salto, perna, bolha, "rotation", ARRASTO_DA_BOLHA)
    # As bolhas de apoio ACHATAM ao carregar e ESTICAM no impulso. Isso nao e
    # enfeite: e o unico aviso que existe DENTRO da pausa, e a pausa e onde o
    # jogador decide se corta a linha do salto.
    for bolha in BOLHAS_DE_APOIO:
        anim.curva(salto, bolha, "scale",
                   [(0.0, anim.escala()),
                    (FIM_DA_PAUSA, anim.escala(1.28, 0.66, 1.28)),
                    (apice, anim.escala(0.82, 1.34, 0.82)),
                    (DUR_WALK, anim.escala())])
    anim.curva(salto, "bubble_croup", "scale",
               [(0.0, anim.escala()),
                (FIM_DA_PAUSA, anim.escala(0.92, 1.1, 0.92)),
                (apice, anim.escala(1.14, 0.9, 1.14)),
                (DUR_WALK, anim.escala())])
    anim.curva(salto, "tail", "rotation",
               [(0.0, anim.vetor()),
                (FIM_DA_PAUSA, anim.vetor(x=-14)),
                (apice, anim.vetor(x=22)),
                (DUR_WALK, anim.vetor())])
    a.clipe("walk", DUR_WALK, salto)

    # --------------------------------------------------------------- windup
    # A EMPINADA. O corpo sai do lugar: sobe, gira para tras e as dianteiras
    # deixam o chao. Silhueta que muda de LUGAR e o que se le de longe; mudanca de
    # forma some no meio do mato.
    #
    # A maior parte do curso fica na SEGUNDA metade (12 graus em 0.25 s, 34 no
    # fim): arranque lento e o que da ao jogador tempo de recuar antes do ponto de
    # nao-retorno -- e recuar e a resposta certa, porque quem continua batendo
    # perde o card.
    aviso = {}
    anim.curva(aviso, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.35, anim.vetor(x=-12)),
                (DUR_WINDUP, anim.vetor(x=-34))])
    anim.curva(aviso, "body", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=1.6, z=0.8))])
    # O pescoco COMPENSA o tronco: a cabeca continua encarando quem encurralou.
    # Sem a compensacao o focinho apontaria para o ceu, e o telegrafo deixaria de
    # dizer CONTRA QUEM o golpe vai.
    anim.curva(aviso, "neck", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=24))])
    anim.derivar(aviso, "neck", "head", "rotation", 0.35)
    for perna in ("leg_front_left", "leg_front_right"):
        anim.curva(aviso, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP * 0.35, anim.vetor(x=-28)),
                    (DUR_WINDUP, anim.vetor(x=-74))])
    for perna in ("leg_back_left", "leg_back_right"):
        anim.curva(aviso, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=14))])
    for bolha, perna in PERNA_DA_BOLHA.items():
        anim.derivar(aviso, perna, bolha, "rotation", ARRASTO_DA_BOLHA)
    # As bolhas dianteiras ESTUFAM no aviso: e o mesmo sinal do impulso, e e
    # deliberado que seja o mesmo -- este golpe e um salto que nao sai do lugar.
    for bolha in ("bubble_front_left", "bubble_front_right"):
        anim.curva(aviso, bolha, "scale",
                   [(0.0, anim.escala()), (DUR_WINDUP, anim.escala(1.18, 1.18, 1.18))])
    anim.curva(aviso, "bubble_croup", "scale",
               [(0.0, anim.escala()), (DUR_WINDUP, anim.escala(1.08, 0.94, 1.08))])
    anim.curva(aviso, "bubble_nape", "scale",
               [(0.0, anim.escala()), (DUR_WINDUP, anim.escala(1.1, 1.1, 1.1))])
    anim.curva(aviso, "tail", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-26))])
    a.clipe("windup", DUR_WINDUP, aviso)

    # --------------------------------------------------------------- strike
    # A JANELA QUE MACHUCA. O servidor gasta 4 ticks (0.2 s) e o clipe dura 0.3: o
    # excedente e permitido de proposito -- o servidor manda no fim e o Java corta
    # o clipe. Faltar e que nao pode.
    #
    # As dianteiras passam do ponto (+30) e voltam (+18). O exagero e o que separa
    # visualmente o golpe do telegrafo; sem ele os dois clipes leem como um
    # movimento continuo e o jogador nao consegue marcar onde o dano saiu.
    golpe = {}
    anim.curva(golpe, "body", "rotation",
               [(0.0, anim.vetor(x=-34)), (DUR_STRIKE * 0.55, anim.vetor(x=14)),
                (DUR_STRIKE, anim.vetor(x=8))])
    anim.curva(golpe, "body", "position",
               [(0.0, anim.vetor(y=1.6, z=0.8)), (DUR_STRIKE * 0.55, anim.vetor(y=-0.8, z=-0.6)),
                (DUR_STRIKE, anim.vetor(y=-0.4, z=-0.3))])
    anim.curva(golpe, "neck", "rotation",
               [(0.0, anim.vetor(x=24)), (DUR_STRIKE * 0.55, anim.vetor(x=-6)),
                (DUR_STRIKE, anim.vetor(x=-2))])
    anim.derivar(golpe, "neck", "head", "rotation", 0.35)
    for perna in ("leg_front_left", "leg_front_right"):
        anim.curva(golpe, perna, "rotation",
                   [(0.0, anim.vetor(x=-74)), (DUR_STRIKE * 0.55, anim.vetor(x=30)),
                    (DUR_STRIKE, anim.vetor(x=18))])
    for perna in ("leg_back_left", "leg_back_right"):
        anim.curva(golpe, perna, "rotation",
                   [(0.0, anim.vetor(x=14)), (DUR_STRIKE, anim.vetor(x=-8))])
    for bolha, perna in PERNA_DA_BOLHA.items():
        anim.derivar(golpe, perna, bolha, "rotation", ARRASTO_DA_BOLHA)
    # A bolha dianteira ACHATA no impacto: e o quadro que diz que a pata encostou
    # em alguma coisa. Sem ele, a pata desce e o dano sai sem que nada na tela
    # marque o instante.
    for bolha in ("bubble_front_left", "bubble_front_right"):
        anim.curva(golpe, bolha, "scale",
                   [(0.0, anim.escala(1.18, 1.18, 1.18)),
                    (DUR_STRIKE * 0.55, anim.escala(1.3, 0.62, 1.3)),
                    (DUR_STRIKE, anim.escala(1.12, 0.86, 1.12))])
    anim.curva(golpe, "bubble_croup", "scale",
               [(0.0, anim.escala(1.08, 0.94, 1.08)), (DUR_STRIKE, anim.escala(0.94, 1.06, 0.94))])
    anim.curva(golpe, "bubble_nape", "scale",
               [(0.0, anim.escala(1.1, 1.1, 1.1)), (DUR_STRIKE, anim.escala(0.96, 0.96, 0.96))])
    anim.curva(golpe, "tail", "rotation",
               [(0.0, anim.vetor(x=-26)), (DUR_STRIKE, anim.vetor(x=12))])
    a.clipe("strike", DUR_STRIKE, golpe)

    # -------------------------------------------------------------- recovery
    # A JANELA DE RESPOSTA -- e neste bicho ela e uma ARMADILHA, nao um convite.
    # Dezessete ticks de bicho aberto pedem uma punicao, e a punicao e exatamente
    # o que pode passar do ponto e matar o alvo que so vale vivo. Encurtar este
    # clipe nao mudaria o servidor; mudaria so a leitura, e o jogador acharia que
    # apanhou sem ter tido janela.
    volta = {}
    anim.curva(volta, "body", "rotation",
               [(0.0, anim.vetor(x=8)), (DUR_RECOVERY * 0.5, anim.vetor(x=4)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "body", "position",
               [(0.0, anim.vetor(y=-0.4, z=-0.3)), (DUR_RECOVERY * 0.5, anim.vetor(y=-0.9)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "neck", "rotation",
               [(0.0, anim.vetor(x=-2)), (DUR_RECOVERY * 0.5, anim.vetor(x=9)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "neck", "head", "rotation", -0.6)
    for perna in ("leg_front_left", "leg_front_right"):
        anim.curva(volta, perna, "rotation",
                   [(0.0, anim.vetor(x=18)), (DUR_RECOVERY * 0.5, anim.vetor(x=7)),
                    (DUR_RECOVERY, anim.vetor())])
    for perna in ("leg_back_left", "leg_back_right"):
        anim.curva(volta, perna, "rotation",
                   [(0.0, anim.vetor(x=-8)), (DUR_RECOVERY, anim.vetor())])
    for bolha, perna in PERNA_DA_BOLHA.items():
        anim.derivar(volta, perna, bolha, "rotation", ARRASTO_DA_BOLHA)
    for bolha in ("bubble_front_left", "bubble_front_right"):
        anim.curva(volta, bolha, "scale",
                   [(0.0, anim.escala(1.12, 0.86, 1.12)), (DUR_RECOVERY, anim.escala())])
    anim.curva(volta, "bubble_croup", "scale",
               [(0.0, anim.escala(0.94, 1.06, 0.94)), (DUR_RECOVERY, anim.escala())])
    anim.curva(volta, "bubble_nape", "scale",
               [(0.0, anim.escala(0.96, 0.96, 0.96)), (DUR_RECOVERY, anim.escala())])
    anim.curva(volta, "tail", "rotation",
               [(0.0, anim.vetor(x=12)), (DUR_RECOVERY, anim.vetor())])
    a.clipe("recovery", DUR_RECOVERY, volta)

    # --------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece. Neste bicho ela tem um segundo trabalho: ela e o aviso de
    # que o corpo esta cedendo. Quem cambaleia um Bubble Horse esta a poucos
    # golpes de mata-lo, e matar cancela o card.
    #
    # As bolhas AFUNDAM de um lado so: um cambaleio simetrico le como tique, e
    # assimetria le como perda de equilibrio.
    tropeco = {}
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.1, anim.vetor(x=-14, z=13)),
                (0.32, anim.vetor(x=7, z=-5)), (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "body", "position",
               [(0.0, anim.vetor()), (0.12, anim.vetor(y=-1.3)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "neck", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(x=-22, y=16)),
                (0.34, anim.vetor(x=10, y=-7)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "neck", "head", "rotation", -0.85, eixos=(0, 1))
    for perna in ("leg_front_left", "leg_back_left"):
        anim.curva(tropeco, perna, "rotation",
                   [(0.0, anim.vetor()), (0.14, anim.vetor(x=-17)),
                    (DUR_STAGGER, anim.vetor())])
    for perna in ("leg_front_right", "leg_back_right"):
        anim.curva(tropeco, perna, "rotation",
                   [(0.0, anim.vetor()), (0.14, anim.vetor(x=11)),
                    (DUR_STAGGER, anim.vetor())])
    for bolha, perna in PERNA_DA_BOLHA.items():
        anim.derivar(tropeco, perna, bolha, "rotation", ARRASTO_DA_BOLHA)
    for bolha in ("bubble_front_left", "bubble_back_left"):
        anim.curva(tropeco, bolha, "scale",
                   [(0.0, anim.escala()), (0.14, anim.escala(1.24, 0.68, 1.24)),
                    (DUR_STAGGER, anim.escala())])
    for bolha in ("bubble_front_right", "bubble_back_right"):
        anim.curva(tropeco, bolha, "scale",
                   [(0.0, anim.escala()), (0.14, anim.escala(0.88, 1.16, 0.88)),
                    (DUR_STAGGER, anim.escala())])
    anim.curva(tropeco, "bubble_croup", "scale",
               [(0.0, anim.escala()), (0.16, anim.escala(0.84, 0.84, 0.84)),
                (DUR_STAGGER, anim.escala())])
    anim.curva(tropeco, "bubble_nape", "scale",
               [(0.0, anim.escala()), (0.16, anim.escala(0.86, 0.86, 0.86)),
                (DUR_STAGGER, anim.escala())])
    anim.curva(tropeco, "tail", "rotation",
               [(0.0, anim.vetor()), (0.16, anim.vetor(y=24)), (DUR_STAGGER, anim.vetor())])
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # ------------------------------------------------------------------ morte
    # A MORTE DESTE BICHO E A PERDA DO CARD, e ela precisa ser LEGIVEL.
    #
    # A condicao de captura e `porEnfraquecimento()`: um quarto de vida, e NAO
    # PODE MORRER. Quem continuou batendo perdeu um premio que talvez nem soubesse
    # que existia -- e um jogador que nao consegue ver o que fez de errado nao
    # aprende nada com o erro. Por isso as bolhas COLAPSAM aqui, cedo (0.3 s) e
    # todas de uma vez: o estouro e o recibo.
    #
    # `valida_bolha_estoura_na_morte` cobra esse colapso. Sem ela, alguem
    # suavizaria as bolhas "para o clipe ficar mais bonito" e apagaria o unico
    # sinal que o mob tem.
    queda = {}
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.25, anim.vetor(x=-11, z=18)),
                (DUR_DEATH, anim.vetor(x=6, z=74))])
    anim.curva(queda, "body", "position",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.25, anim.vetor(y=0.8)),
                (DUR_DEATH, anim.vetor(y=-7.5))])
    anim.curva(queda, "neck", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.35, anim.vetor(x=-26)),
                (DUR_DEATH, anim.vetor(x=38))])
    anim.derivar(queda, "neck", "head", "rotation", 0.6)
    for perna in ("leg_front_left", "leg_front_right"):
        anim.curva(queda, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_DEATH * 0.35, anim.vetor(x=-40)),
                    (DUR_DEATH, anim.vetor(x=-58))])
    for perna in ("leg_back_left", "leg_back_right"):
        anim.curva(queda, perna, "rotation",
                   [(0.0, anim.vetor()), (DUR_DEATH * 0.35, anim.vetor(x=34)),
                    (DUR_DEATH, anim.vetor(x=52))])
    for bolha, perna in PERNA_DA_BOLHA.items():
        anim.derivar(queda, perna, bolha, "rotation", ARRASTO_DA_BOLHA)
    # O estouro: um estufo curto e a queda para ESCALA_DA_BOLHA_MORTA. O estufo
    # antes do colapso e o que faz ler como bolha e nao como fade-out.
    for bolha in BOLHAS_DE_APOIO + ("bubble_croup", "bubble_nape"):
        anim.curva(queda, bolha, "scale",
                   [(0.0, anim.escala()),
                    (DUR_DEATH * 0.18, anim.escala(1.26, 1.26, 1.26)),
                    (DUR_DEATH * 0.3, anim.escala(ESCALA_DA_BOLHA_MORTA,
                                                  ESCALA_DA_BOLHA_MORTA,
                                                  ESCALA_DA_BOLHA_MORTA)),
                    (DUR_DEATH, anim.escala(ESCALA_DA_BOLHA_MORTA,
                                            ESCALA_DA_BOLHA_MORTA,
                                            ESCALA_DA_BOLHA_MORTA))])
    anim.curva(queda, "tail", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=-34))])
    a.clipe("death", DUR_DEATH, queda)

    return a


# ------------------------------------------ validacoes que ligam arte e regra

def valida_ciclo_do_salto(a):
    """O clipe `walk` dura EXATAMENTE um ciclo de salto do servidor.

    O servidor alterna pausa e impulso com periodo TICKS_DE_PAUSA + TICKS_DE_ARCO
    (`RegrasDeSaltoDeBolha`). O clipe que repete durante a locomocao e este. Se as
    duas duracoes divergirem, a fase escorrega um pouco a cada volta e o bicho
    passa a aterrissar em quadros diferentes do clipe -- as vezes no agachamento,
    as vezes no apice.

    Nada disso da erro: a velocidade, o dano e o log ficam iguais. O que quebra e
    a unica coisa que o jogador tem para prever o salto, e prever o salto e a
    resposta que este mob ensina -- perseguir nao funciona.
    """
    duracao = a.clipes[a.nome_completo("walk")]["animation_length"]
    esperado = (TICKS_DE_PAUSA + TICKS_DE_ARCO) / TICKS_POR_SEGUNDO
    if abs(duracao - esperado) > 1e-9:
        raise ErroDeArte(
            "o clipe 'walk' dura %.3fs e o ciclo de salto do servidor dura %.3fs (pausa %d + arco "
            "%d ticks): a fase escorrega a cada volta, o bicho passa a aterrissar em quadros "
            "diferentes do clipe, e o jogador perde a unica pista que tem para prever onde o "
            "proximo salto cai" % (duracao, esperado, TICKS_DE_PAUSA, TICKS_DE_ARCO))


def valida_bolha_estoura_na_morte(a):
    """A morte TEM de colapsar todas as bolhas, e cedo.

    A condicao de card deste bicho e nao-letal (`CaptureCondition
    .porEnfraquecimento()`: um quarto de vida, e nao pode morrer). Matar cancela a
    captura -- e uma perda que o jogador nao ve e uma licao que ele nao aprende:
    ele simplesmente nao recebe um card que talvez nem soubesse que existia.

    O colapso das bolhas e o recibo. A regua cobra duas coisas: que TODA bolha
    termine o clipe murcha, e que o colapso aconteca na primeira metade -- um
    estouro no ultimo quadro acontece depois de o jogador ja ter desviado o olhar
    para o loot que nao caiu.
    """
    clipe = a.clipes[a.nome_completo("death")]
    fim = clipe["animation_length"]
    limite = ESCALA_DA_BOLHA_MORTA * 1.5
    for bolha in BOLHAS_DE_APOIO + ("bubble_croup", "bubble_nape"):
        quadros = clipe["bones"].get(bolha, {}).get("scale")
        if not quadros:
            raise ErroDeArte(
                "a bolha '%s' nao muda de escala no clipe de morte: ela fica INTEIRA no cadaver, e "
                "o unico sinal de que o card se perdeu some junto" % bolha)
        no_fim = anim.valor_em(quadros, fim, anim.escala())
        if max(no_fim) > limite:
            raise ErroDeArte(
                "a bolha '%s' termina a morte em escala %s e o limite e %.2f: a bolha sobrevive ao "
                "bicho, e a perda do card deixa de ter recibo na tela" % (bolha, no_fim, limite))
        na_metade = anim.valor_em(quadros, fim / 2.0, anim.escala())
        if max(na_metade) > limite:
            raise ErroDeArte(
                "a bolha '%s' ainda esta em %s na metade do clipe de morte: o estouro chega depois "
                "de o jogador ja ter olhado para o loot que nao caiu" % (bolha, na_metade))


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(
        ataques=ATAQUES,
        extras=(valida_ciclo_do_salto, valida_bolha_estoura_na_morte))
