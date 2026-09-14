"""Clipes do Boneco de Treino -- a representacao das fases que o SERVIDOR publica.

NENHUM KEYFRAME AQUI APLICA DANO, STAGGER OU RECOMPENSA. O servidor publica a
fase e o cliente escolhe o clipe correspondente; se a animacao e a hitbox
discordarem, quem esta errado e este arquivo.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com o metodo de origem ao lado. A
regua da biblioteca reprova se a soma dos tres clipes encadeados for menor que
windup + active + recovery: um clipe curto demais faz o boneco RELAXAR no meio
do golpe que ainda vai acertar -- dano certo, cooldown certo, log limpo, e a
unica leitura que o jogador tem quebrada.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/dummy_enemy/dummy_enemy_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/dummy_enemy.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402

MOB = "dummy_enemy"

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType
# do Java VENCE o JSON, e um .thenLoop() do lado de la transformaria este
# dicionario em documentacao que discorda do comportamento.
#
# Esta e tambem a UNICA lista de clipes. Uma lista separada seria a mesma
# informacao escrita duas vezes, e alguem acrescentaria um clipe numa so.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o telegrafo termina com os bracos ERGUIDOS e fica
    # assim. Voltando ao repouso no fim do clipe, o boneco desarmaria o golpe na
    # tela enquanto o servidor ainda esta em WINDUP -- o jogador leria "passou" e
    # levaria a pancada mesmo assim.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de HunterExamProfiles.dummyEnemyStrike(), com o metodo ao lado.
ATAQUES = {("windup", "strike", "recovery"): anim.Ataque(windup=10,    # windupTicks
                                                         active=4,     # activeTicks
                                                         recovery=12)}  # recoveryTicks

DUR_IDLE = 2.4
DUR_WALK = 0.8
DUR_WINDUP = 0.5    # 10 ticks
DUR_STRIKE = 0.2    # 4 ticks
DUR_RECOVERY = 0.6  # 12 ticks
DUR_STAGGER = 0.5
DUR_DEATH = 1.0


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # -------------------------------------------------------------- ocio
    # O boneco pendurado num poste nao respira: ele OSCILA. A amplitude e
    # pequena (2 graus) porque o movimento tem de ser percebido sem competir
    # com o telegrafo -- ocio chamativo faz o jogador ignorar o windup.
    ocio = {}
    anim.curva(ocio, "body", "rotation",
               [(t, anim.vetor(z=v)) for t, v in anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.0)])
    anim.curva(ocio, "head", "rotation",
               [(t, anim.vetor(z=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 3.0, fase=0.08)])
    # Os bracos seguem o saco com atraso: fator negativo pequeno da o arrasto
    # de um peso solto, que e o que a estopa e.
    anim.derivar(ocio, "body", "arm_left", "rotation", -0.6, eixos=(2,))
    anim.derivar(ocio, "body", "arm_right", "rotation", -0.6, eixos=(2,))
    a.clipe("idle", DUR_IDLE, ocio)

    # ----------------------------------------------------------- locomocao
    # O boneco ANDA porque a issue #138 exige que a framework prove locomocao.
    # Sem pernas, quem anda e a cruz: ela pivota alternadamente, como alguem
    # arrastando um movel pesado. E feio, e e assumido -- ver o cabecalho.
    marcha = {}
    anim.curva(marcha, "base", "rotation",
               [(t, anim.vetor(z=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 9.0)])
    anim.curva(marcha, "post", "rotation",
               [(t, anim.vetor(z=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 5.0, fase=0.12)])
    anim.curva(marcha, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 6.0, fase=0.25)])
    anim.derivar(marcha, "body", "head", "rotation", -0.5)
    anim.curva(marcha, "arm_left", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 14.0, fase=0.5)])
    anim.derivar(marcha, "arm_left", "arm_right", "rotation", -1.0)
    a.clipe("walk", DUR_WALK, marcha)

    # -------------------------------------------------------------- windup
    # O AVISO. Meio segundo de bracos subindo e tronco recuando -- o recuo e o
    # que faz o golpe ser lido antes de sair, porque a silhueta muda de lugar e
    # nao so de forma.
    aviso = {}
    anim.curva(aviso, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-22))])
    anim.curva(aviso, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(x=-10))])
    anim.curva(aviso, "arm_left", "rotation",
               [(0.0, anim.vetor()), (DUR_WINDUP * 0.7, anim.vetor(x=-70)),
                (DUR_WINDUP, anim.vetor(x=-96))])
    anim.derivar(aviso, "arm_left", "arm_right", "rotation", 1.0)
    a.clipe("windup", DUR_WINDUP, aviso)

    # -------------------------------------------------------------- strike
    # A JANELA QUE MACHUCA, e ela e curta. Os bracos descem de -96 ao repouso e
    # passam do ponto: o exagero e o que separa visualmente o golpe do telegrafo.
    golpe = {}
    anim.curva(golpe, "arm_left", "rotation",
               [(0.0, anim.vetor(x=-96)), (DUR_STRIKE * 0.6, anim.vetor(x=34)),
                (DUR_STRIKE, anim.vetor(x=18))])
    anim.derivar(golpe, "arm_left", "arm_right", "rotation", 1.0)
    anim.curva(golpe, "body", "rotation",
               [(0.0, anim.vetor(x=-22)), (DUR_STRIKE * 0.6, anim.vetor(x=16)),
                (DUR_STRIKE, anim.vetor(x=10))])
    anim.curva(golpe, "body", "position",
               [(0.0, anim.vetor()), (DUR_STRIKE * 0.6, anim.vetor(z=-1.5)),
                (DUR_STRIKE, anim.vetor(z=-0.5))])
    a.clipe("strike", DUR_STRIKE, golpe)

    # ------------------------------------------------------------ recovery
    # A JANELA DE RESPOSTA. Ela e a mais longa das tres de proposito: e o tempo
    # em que o jogador pune o boneco, e encurta-la aqui nao mudaria o servidor --
    # mudaria so a leitura, e o jogador acharia que foi punido sem janela.
    volta = {}
    anim.curva(volta, "arm_left", "rotation",
               [(0.0, anim.vetor(x=18)), (DUR_RECOVERY, anim.vetor())])
    anim.derivar(volta, "arm_left", "arm_right", "rotation", 1.0)
    anim.curva(volta, "body", "rotation",
               [(0.0, anim.vetor(x=10)), (DUR_RECOVERY * 0.5, anim.vetor(x=-4)),
                (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "body", "position",
               [(0.0, anim.vetor(z=-0.5)), (DUR_RECOVERY, anim.vetor())])
    a.clipe("recovery", DUR_RECOVERY, volta)

    # ------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece. O saco chicoteia para tras e volta oscilando.
    tropeco = {}
    anim.curva(tropeco, "body", "rotation",
               [(0.0, anim.vetor()), (0.1, anim.vetor(x=-34)), (0.28, anim.vetor(x=14)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "head", "rotation",
               [(0.0, anim.vetor()), (0.14, anim.vetor(x=-40)), (0.32, anim.vetor(x=16)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tropeco, "post", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(x=-8)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tropeco, "body", "arm_left", "rotation", -0.8)
    anim.derivar(tropeco, "body", "arm_right", "rotation", -0.8)
    a.clipe("stagger", DUR_STAGGER, tropeco)

    # --------------------------------------------------------------- morte
    # hold_on_last_frame: o corpo fica caido ate a entidade sumir. Um clipe de
    # morte que volta ao repouso mostra o boneco de pe no ultimo quadro antes de
    # desaparecer, e a leitura vira "ele sumiu", nao "ele caiu".
    queda = {}
    anim.curva(queda, "base", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.35, anim.vetor(x=18)),
                (DUR_DEATH, anim.vetor(x=88))])
    anim.curva(queda, "body", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH * 0.5, anim.vetor(x=-16)),
                (DUR_DEATH, anim.vetor(x=6))])
    anim.curva(queda, "head", "rotation",
               [(0.0, anim.vetor()), (DUR_DEATH, anim.vetor(x=24))])
    anim.derivar(queda, "body", "arm_left", "rotation", -1.2)
    anim.derivar(queda, "body", "arm_right", "rotation", -1.2)
    a.clipe("death", DUR_DEATH, queda)

    return a


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(ataques=ATAQUES)
