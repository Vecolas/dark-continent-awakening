"""Clipes do Cyclops -- a representacao das fases que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, STAGGER OU RECOMPENSA. O servidor publica a
fase (`AttackPhase`) e o cambaleio; o cliente escolhe o clipe correspondente. Se
a animacao e a hitbox discordarem, quem esta errado e este arquivo.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com o metodo de origem ao lado. A
regua da biblioteca reprova se a soma dos tres clipes encadeados for menor que
windup + active + recovery.

A FORMA DO TELEGRAFO E A FICHA DO BICHO. Ele e lento: 30 ticks de aviso, 5 de
janela, 25 de recuperacao. Quem encurtar o windup na animacao sem encurtar no
servidor entrega o pior dos dois mundos -- o porrete ja caiu na tela e o dano so
sai meio segundo depois, e o jogador aprende que desviar nao funciona. Quem
encurtar o CLIPE inteiro faz o gigante RELAXAR no meio do golpe que ainda vai
acertar: dano certo, cooldown certo, log limpo, e a unica leitura que o jogador
tem quebrada. E por isso que `valida_duracao_de_ataque` existe.

CONVENCAO DE ROTACAO, medida neste geo e nao decorada. Com -Z na frente:
  * rotacao X NEGATIVA num membro pendurado (braco, porrete) o joga para a
    FRENTE e para cima; X POSITIVA o joga para TRAS e para cima;
  * rotacao X POSITIVA no torso e na cabeca inclina para a FRENTE (olhar para
    baixo); NEGATIVA joga para tras.
O ciclope arma o porrete para TRAS (X positivo grande) e desce em cima do alvo
(X negativo). O arco completo -- de +118 a -46 -- e o que faz o golpe ler como
peso, e nao como tapa.

O OLHO E ANIMADO EM TODO CLIPE. Ele e o unico ponto de leitura que este mob tem;
um olho parado num corpo que se mexe le como adesivo colado no bicho. Ele nunca
se move sozinho -- e sempre DERIVADO da cabeca, com fator negativo pequeno, que e
o arrasto de um globo dentro da orbita. Escrito a mao ele ficaria com a fase
certa hoje e errada na primeira correcao do pescoco.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/cyclops/cyclops_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/cyclops.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402

MOB = "cyclops"

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
    # hold_on_last_frame: o telegrafo termina com o porrete ARMADO no alto e fica
    # assim. Voltando ao repouso no fim do clipe, o gigante desarmaria o golpe na
    # tela enquanto o servidor ainda esta em WINDUP -- o jogador leria "passou" e
    # levaria a pancada mesmo assim. Este e o clipe mais longo do mob (1.5 s), e e
    # exatamente nele que o erro seria mais caro.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de CyclopsTuning.porrete(), com o campo de origem ao lado.
ATAQUES = {("windup", "strike", "recovery"):
           anim.Ataque(windup=30,     # CyclopsTuning.WINDUP_DO_PORRETE
                       active=5,      # CyclopsTuning.JANELA_DO_PORRETE
                       recovery=25)}  # CyclopsTuning.RECUPERACAO_DO_PORRETE

DUR_IDLE = 3.0
DUR_WALK = 1.2
DUR_WINDUP = 1.5     # 30 ticks
DUR_STRIKE = 0.3     # 6 ticks -- um a mais que a janela, de proposito (ver abaixo)
DUR_RECOVERY = 1.3   # 26 ticks
DUR_STAGGER = 0.7
DUR_DEATH = 1.6

# Arrasto do globo ocular dentro da orbita. Negativo e pequeno: o olho fica para
# tras do movimento da cabeca. Fator positivo faria o olho ANTECIPAR o pescoco,
# que e o movimento de quem ja sabia -- e este bicho enxerga mal, essa e a ficha.
ARRASTO_DO_OLHO = -0.35


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # ---------------------------------------------------------------- ocio
    # Respiracao LENTA (3 s por ciclo) e de amplitude pequena. Ocio chamativo num
    # mob de 4.2 blocos competiria com o telegrafo, e o telegrafo e a unica coisa
    # que o jogador precisa ler neste bicho.
    ocio = {}
    anim.curva(ocio, "torso", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 1.6)])
    anim.curva(ocio, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.2, fase=0.12)])
    anim.derivar(ocio, "head", "eye", "rotation", ARRASTO_DO_OLHO)
    # Os bracos pendem e balancam atras do tronco: fator negativo pequeno e o
    # arrasto de peso solto. O porrete arrasta ainda mais, porque e o que ele e.
    anim.curva(ocio, "arm_right", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.6, fase=0.28)])
    anim.derivar(ocio, "arm_right", "arm_left", "rotation", 0.8)
    anim.derivar(ocio, "arm_right", "club", "rotation", -0.55)
    anim.curva(ocio, "hip", "position",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 0.4)])
    a.clipe("idle", DUR_IDLE, ocio)

    # ------------------------------------------------------------ locomocao
    # Passada LONGA e lenta: 1.2 s por ciclo completo, com amplitude grande nas
    # pernas (18 graus). Velocidade 0.24 num corpo de 4.2 blocos so le como peso
    # se a passada for longa; passada curta e rapida num gigante le como boneco
    # deslizando, e nenhum portao ve isso.
    marcha = {}
    anim.curva(marcha, "leg_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 18.0)])
    anim.derivar(marcha, "leg_left", "leg_right", "rotation", -1.0)
    # Os pes NUNCA sao escritos a mao: derivados, eles continuam com a fase certa
    # quando alguem mexer na perna. A mao, a primeira correcao da perna deixa o pe
    # meio quadro atras da canela, e isso ninguem consegue descrever.
    anim.derivar(marcha, "leg_left", "foot_left", "rotation", -0.45)
    anim.derivar(marcha, "leg_right", "foot_right", "rotation", -0.45)
    anim.curva(marcha, "arm_right", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 11.0, fase=0.5)])
    anim.derivar(marcha, "arm_right", "arm_left", "rotation", -1.0)
    anim.derivar(marcha, "arm_right", "club", "rotation", -0.4)
    anim.curva(marcha, "hip", "rotation",
               [(t, anim.vetor(y=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 4.0)])
    anim.curva(marcha, "torso", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 2.4, fase=0.25)])
    anim.derivar(marcha, "torso", "head", "rotation", -0.9)
    anim.derivar(marcha, "head", "eye", "rotation", ARRASTO_DO_OLHO)
    # O solavanco vertical tem periodo METADE do ciclo: sao duas pisadas por
    # ciclo, e uma so faria o gigante mancar.
    anim.curva(marcha, "hip", "position",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK / 2.0, 0.9)])
    a.clipe("walk", DUR_WALK, marcha)

    # --------------------------------------------------------------- windup
    # O AVISO, e ele dura um segundo e meio. O porrete sobe para TRAS enquanto o
    # tronco se joga para tras e o joelho dobra: tres sinais que mudam a silhueta
    # de LUGAR, e nao so de forma. Silhueta que muda de lugar e o que se le de
    # longe; mudanca de forma some no meio das folhas.
    #
    # A maior parte do curso acontece na SEGUNDA metade (30 graus em 0.45 s, 118
    # no fim): arranque lento e o que da ao jogador tempo de decidir antes de o
    # ponto de nao-retorno chegar.
    aviso = {}
    anim.curva(aviso, "arm_right", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.3, anim.vetor(x=30)),
                (DUR_WINDUP, anim.vetor(x=118))])
    anim.derivar(aviso, "arm_right", "club", "rotation", 0.22)
    anim.curva(aviso, "arm_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-26))])
    anim.curva(aviso, "torso", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.3, anim.vetor(x=-3)),
                (DUR_WINDUP, anim.vetor(x=-12))])
    # A cabeca ENCARA o alvo durante o aviso -- ela inclina para a frente
    # enquanto o tronco vai para tras. O olho arrasta atras dela.
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=9))])
    anim.derivar(aviso, "head", "eye", "rotation", ARRASTO_DO_OLHO)
    anim.curva(aviso, "leg_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=9))])
    anim.derivar(aviso, "leg_left", "leg_right", "rotation", 1.0)
    anim.derivar(aviso, "leg_left", "foot_left", "rotation", -1.0)
    anim.derivar(aviso, "leg_right", "foot_right", "rotation", -1.0)
    anim.curva(aviso, "hip", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=-1.8))])
    a.clipe("windup", DUR_WINDUP, aviso)

    # --------------------------------------------------------------- strike
    # A JANELA QUE MACHUCA. O servidor gasta 5 ticks (0.25 s) e o clipe dura 0.3:
    # o excedente e permitido de proposito -- o servidor manda no fim e o Java
    # corta o clipe. Faltar e que nao pode.
    #
    # O porrete passa do ponto (-46) e volta um pouco (-28). O exagero e o que
    # separa visualmente o golpe do telegrafo; sem ele os dois clipes leem como
    # um movimento continuo e o jogador nao consegue marcar onde o dano saiu.
    golpe = {}
    anim.curva(golpe, "arm_right", "rotation",
               [(0.0, anim.vetor(x=118)), (DUR_STRIKE * 0.6, anim.vetor(x=-46)),
                (DUR_STRIKE, anim.vetor(x=-28))])
    anim.derivar(golpe, "arm_right", "club", "rotation", 0.16)
    anim.curva(golpe, "arm_left", "rotation",
               [(0.0, anim.vetor(x=-26)), (DUR_STRIKE * 0.6, anim.vetor(x=22)),
                (DUR_STRIKE, anim.vetor(x=14))])
    anim.curva(golpe, "torso", "rotation",
               [(0.0, anim.vetor(x=-12)), (DUR_STRIKE * 0.6, anim.vetor(x=24)),
                (DUR_STRIKE, anim.vetor(x=18))])
    anim.derivar(golpe, "torso", "head", "rotation", 0.5)
    anim.derivar(golpe, "head", "eye", "rotation", ARRASTO_DO_OLHO)
    # O corpo inteiro entra no golpe: z NEGATIVO e para a frente nesta geometria.
    # Sem esse avanco, um gigante de 4.2 blocos parece bater com o braco solto.
    anim.curva(golpe, "hip", "position",
               [(0.0, anim.vetor(y=-1.8)), (DUR_STRIKE * 0.6, anim.vetor(y=-0.6, z=-2.2)),
                (DUR_STRIKE, anim.vetor(y=-0.6, z=-1.4))])
    anim.curva(golpe, "leg_left", "rotation",
               [(0.0, anim.vetor(x=9)), (DUR_STRIKE, anim.vetor(x=-6))])
    anim.derivar(golpe, "leg_left", "leg_right", "rotation", 1.0)
    anim.derivar(golpe, "leg_left", "foot_left", "rotation", -1.0)
    anim.derivar(golpe, "leg_right", "foot_right", "rotation", -1.0)
    a.clipe("strike", DUR_STRIKE, golpe)

    # -------------------------------------------------------------- recovery
    # A JANELA DE RESPOSTA, e ela e a mais longa depois do aviso: 1.3 s em que o
    # jogador pune. Encurtar este clipe nao mudaria o servidor -- mudaria so a
    # leitura, e o jogador acharia que apanhou sem ter tido janela.
    #
    # O gigante fica DOBRADO sobre o porrete na primeira metade e so depois se
    # endireita. E nessa metade que o olho fica baixo, e e por isso que ela existe.
    volta = {}
    anim.curva(volta, "arm_right", "rotation",
               [(0.0, anim.vetor(x=-28)), (DUR_RECOVERY * 0.55, anim.vetor(x=-14)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "arm_right", "club", "rotation", 0.16)
    anim.curva(volta, "arm_left", "rotation",
               [(0.0, anim.vetor(x=14)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "torso", "rotation",
               [(0.0, anim.vetor(x=18)), (DUR_RECOVERY * 0.55, anim.vetor(x=22)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "torso", "head", "rotation", 0.5)
    anim.derivar(volta, "head", "eye", "rotation", ARRASTO_DO_OLHO)
    anim.curva(volta, "hip", "position",
               [(0.0, anim.vetor(y=-0.6, z=-1.4)), (DUR_RECOVERY * 0.55, anim.vetor(y=-1.2)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "leg_left", "rotation",
               [(0.0, anim.vetor(x=-6)), (DUR_RECOVERY * 0.55, anim.vetor(x=12)),
                (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "leg_left", "leg_right", "rotation", 1.0)
    anim.derivar(volta, "leg_left", "foot_left", "rotation", -1.0)
    anim.derivar(volta, "leg_right", "foot_right", "rotation", -1.0)
    a.clipe("recovery", DUR_RECOVERY, volta)

    # --------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece -- e o ponto fraco, que e o jeito barato de disparar o
    # stagger, deixa de ensinar qualquer coisa.
    #
    # A cabeca e o que mais se mexe, e isso nao e estetica: quem cambaleia este
    # bicho acertou o OLHO, e a reacao tem de acontecer onde o golpe entrou.
    tropeco = {}
    anim.curva(tropeco, "head", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(x=-32, y=14)),
                (0.34, anim.vetor(x=13, y=-6)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "head", "eye", "rotation", -0.9, eixos=(0, 1))
    anim.curva(tropeco, "torso", "rotation",
               [(0.0, anim.vetor()), (0.16, anim.vetor(x=-18)), (0.4, anim.vetor(x=8)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "hip", "rotation",
               [(0.0, anim.vetor()), (0.18, anim.vetor(y=13)), (0.42, anim.vetor(y=-5)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "arm_right", "rotation",
               [(0.0, anim.vetor()), (0.16, anim.vetor(x=-34)), (0.42, anim.vetor(x=10)),
                (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "arm_right", "club", "rotation", -0.7)
    anim.curva(tropeco, "arm_left", "rotation",
               [(0.0, anim.vetor()), (0.16, anim.vetor(x=28)), (0.42, anim.vetor(x=-9)),
                (DUR_STAGGER, anim.vetor())])
    # Um pe atras para nao cair: o passo de recuperacao e o que faz o cambaleio
    # ler como perda de equilibrio e nao como tique.
    anim.curva(tropeco, "leg_left", "rotation",
               [(0.0, anim.vetor()), (0.2, anim.vetor(x=15)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "leg_left", "leg_right", "rotation", -0.6)
    anim.derivar(tropeco, "leg_left", "foot_left", "rotation", -0.8)
    anim.derivar(tropeco, "leg_right", "foot_right", "rotation", -0.8)
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # ------------------------------------------------------------------ morte
    # hold_on_last_frame: o corpo fica caido ate a entidade sumir. Um clipe de
    # morte que volta ao repouso mostra o gigante DE PE no ultimo quadro antes de
    # desaparecer, e a leitura vira "ele sumiu", nao "ele caiu".
    #
    # O quadril desce 6 px enquanto gira: sem a descida, o modelo pivota no ar e
    # metade do corpo atravessa o chao.
    queda = {}
    anim.curva(queda, "hip", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(x=16)),
                (DUR_DEATH, anim.vetor(x=82))])
    anim.curva(queda, "hip", "position",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.3, anim.vetor(y=-1.5)),
                (DUR_DEATH, anim.vetor(y=-6))])
    anim.curva(queda, "torso", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.4, anim.vetor(x=-22)),
                (DUR_DEATH, anim.vetor(x=-9))])
    anim.derivar(queda, "torso", "head", "rotation", 1.3)
    anim.derivar(queda, "head", "eye", "rotation", ARRASTO_DO_OLHO)
    anim.curva(queda, "arm_right", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.45, anim.vetor(x=-38)),
                (DUR_DEATH, anim.vetor(x=-66))])
    # O porrete cai da mao antes do corpo: fator maior que 1 o joga alem do
    # braco, que e o que um peso solto faz.
    anim.derivar(queda, "arm_right", "club", "rotation", -1.25)
    anim.curva(queda, "arm_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=44)),
                (DUR_DEATH, anim.vetor(x=61))])
    anim.curva(queda, "leg_left", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=-19)),
                (DUR_DEATH, anim.vetor(x=-33))])
    anim.derivar(queda, "leg_left", "leg_right", "rotation", 0.7)
    anim.derivar(queda, "leg_left", "foot_left", "rotation", -0.5)
    anim.derivar(queda, "leg_right", "foot_right", "rotation", -0.5)
    a.clipe("death", DUR_DEATH, queda)

    return a


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(ataques=ATAQUES)
