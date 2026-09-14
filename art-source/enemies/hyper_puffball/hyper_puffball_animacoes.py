"""Clipes do Hyper Puffball -- o que o corpo mostra de um bicho que so tem UM evento.

NENHUM KEYFRAME AQUI APLICA DANO, STAGGER OU MORTE. O servidor publica a fase e o
cambaleio, e o cliente escolhe o clipe correspondente. Se a animacao e a regra
discordarem, quem esta errado e este arquivo.

O BICHO NAO ANDA, E MESMO ASSIM TEM 'walk'. A entidade referencia os sete clipes
por nome, e nome que nao existe nao da erro: o GeckoLib procura, nao acha, e deixa
o osso parado. O 'walk' aqui NAO e locomocao -- a MOVEMENT_SPEED do perfil e 0.0 e
nao ha goal que mande o fungo andar. Ele cobre o unico caso em que a posicao do
bicho muda mesmo assim: EMPURRAO -- correnteza, pistao, um mob passando por cima.
Por isso ele e curto e e um BALANCO, e nao uma marcha: um passo desenhado ali
ensinaria que o fungo caminha, e ele nunca caminha.

O 'death' E O ESTOURO. O servidor mata a entidade no mesmo ato em que aplica o
dano em area, entao nao existe clipe separado de explosao -- existe este. E por
isso ele dura 0.90 s: o vanilla remove a entidade em 20 ticks de deathTime (1.00 s)
e o que passar disso nunca chega na tela. Um clipe de 1.5 s nao daria erro nenhum:
daria um estouro que desaparece no meio, e a nuvem que o jogador precisa associar
ao dano some antes de abrir.

O ORCAMENTO DE ATAQUE E COPIADO DO SERVIDOR, com o metodo de origem ao lado. O
"ataque" do puffball e um AVISO que nao machuca ninguem -- ele incha, estufa e
desincha quando alguem chega perto. A regua da biblioteca continua valendo pelo
mesmo motivo de sempre: se o clipe acabar antes do orcamento, o fungo RELAXA na
tela enquanto o servidor ainda esta na fase, e o jogador aprende que ja pode
encostar.

Regerar (DEPOIS do geo, que este arquivo LE):
  python art-source/enemies/hyper_puffball/hyper_puffball_animacoes.py
Exporta:
  src/main/resources/assets/nenfoundation/animations/entity/hyper_puffball.animation.json
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import animacao as anim      # noqa: E402

MOB = "hyper_puffball"

# O loop mora AQUI, e nao no RawAnimation do Java: no GeckoLib 4.8.3 o LoopType do
# Java VENCE o JSON, e um .thenLoop() do lado de la transformaria este dicionario
# em documentacao que discorda do comportamento.
#
# Esta e tambem a UNICA lista de clipes. Uma lista separada seria a mesma
# informacao escrita duas vezes, e alguem acrescentaria um clipe em so uma delas.
LOOPS = {
    "idle": True,
    "walk": True,
    # hold_on_last_frame: o aviso termina INCHADO e fica assim. Voltando ao
    # repouso no fim do clipe, o fungo desinflaria na tela enquanto o servidor
    # ainda esta em WINDUP -- e a leitura viraria "passou o perigo" exatamente no
    # momento em que o jogador deveria estar recuando.
    "windup": "hold_on_last_frame",
    "strike": False,
    "recovery": False,
    "stagger": False,
    # hold_on_last_frame: o que sobra do fungo fica caido ate a entidade sumir.
    # Voltando ao repouso, o ultimo quadro antes do desaparecimento mostraria o
    # bicho inteiro de novo, e a leitura seria "ele sumiu", nao "ele estourou".
    "death": "hold_on_last_frame",
}

# Ticks COPIADOS de HyperPuffballTuning.aviso(), com o campo ao lado.
ATAQUES = {("windup", "strike", "recovery"): anim.Ataque(windup=12,     # AVISO_WINDUP_TICKS
                                                         active=4,      # AVISO_ACTIVE_TICKS
                                                         recovery=10)}  # AVISO_RECOVERY_TICKS

# Janela de morte do vanilla: LivingEntity remove a entidade quando deathTime
# chega a 20. Nao e numero deste bicho nem botao de balanceamento -- e do jogo.
TICKS_DE_MORTE_DO_VANILLA = 20

DUR_IDLE = 3.0
DUR_WALK = 0.5
DUR_WINDUP = 0.7     # cobre os 12 ticks (0.60 s) com folga
DUR_STRIKE = 0.25    # cobre os 4 ticks (0.20 s)
DUR_RECOVERY = 0.55  # cobre os 10 ticks (0.50 s)
DUR_STAGGER = 0.5
DUR_DEATH = 0.9      # abaixo de TICKS_DE_MORTE_DO_VANILLA / 20; ver valida_morte_cabe_na_janela


def valida_morte_cabe_na_janela_de_morte(a):
    """O estouro tem de caber nos ticks em que a entidade ainda existe.

    LIGACAO ARTE <-> REGRA. O estouro e instantaneo no servidor: o dano em area
    sai e a entidade morre no mesmo ato. O que o jogador VE do estouro e este
    clipe, e ele so tem os ticks de deathTime do vanilla para acontecer. Um clipe
    mais longo que essa janela nao da erro, nao aparece no log e nao reprova
    nenhum portao da biblioteca -- ele simplesmente e cortado no meio, e a nuvem
    que explica o dano nunca abre na tela.
    """
    clipe = a.clipes[a.nome_completo("death")]
    limite = TICKS_DE_MORTE_DO_VANILLA / 20.0
    if clipe["animation_length"] > limite + 1e-9:
        raise anim.ErroDeArte(
            "o clipe de morte dura %.2fs e a entidade some em %.2fs (deathTime %d ticks): o estouro "
            "e cortado no meio e o jogador nunca ve a nuvem que explica o dano que ele levou"
            % (clipe["animation_length"], limite, TICKS_DE_MORTE_DO_VANILLA))


def clipes(geometria):
    a = anim.Animacoes(MOB, geometria, LOOPS)

    # --------------------------------------------------------------- ocio
    # O fungo nao respira e nao balanca: ele ENCHE e esvazia, devagar. A amplitude
    # e minuscula (4% de escala) porque este e o unico movimento que ele tem em
    # repouso -- e porque ocio chamativo faria o jogador parar de distinguir o
    # ocio do aviso, que e a unica leitura que o salva.
    #
    # O talo NAO e citado aqui, de proposito: a peca presa no chao e o que diz
    # "isto nao vai a lugar nenhum", e ela so se mexe quando algo a empurra.
    ocio = {}
    anim.curva(ocio, "bulbo", "scale",
               [(t, anim.escala(v, v, v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 0.04, base=1.0)])
    anim.curva(ocio, "cupula", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 3.0, fase=0.12)])
    # O poro contra-gira de leve: a boca acompanha a cupula com atraso, que e o
    # que impede o topo inteiro de se mover como uma peca so de plastico.
    anim.derivar(ocio, "cupula", "poro", "rotation", -0.5)
    a.clipe("idle", DUR_IDLE, ocio)

    # ------------------------------------------------------------ empurrao
    # Nao e marcha -- ver o cabecalho. E o balanco de um saco pesado que acabou de
    # ser empurrado, e ele volta exatamente onde comecou porque repete.
    balanco = {}
    anim.curva(balanco, "talo", "rotation",
               [(t, anim.vetor(z=v)) for t, v in anim.cossenoide(DUR_WALK, DUR_WALK, 3.0)])
    # O bulbo atrasa e exagera o que o talo faz: massa em cima de base estreita.
    anim.derivar(balanco, "talo", "bulbo", "rotation", 1.8, eixos=(2,))
    anim.derivar(balanco, "bulbo", "cupula", "rotation", 0.6, eixos=(2,))
    a.clipe("walk", DUR_WALK, balanco)

    # -------------------------------------------------------------- windup
    # O AVISO, e ele e a UNICA coisa que o fungo faz antes de alguem se machucar.
    # Ele incha: o bulbo cresce mais em X e Z do que em Y (um saco enchendo fica
    # redondo, nao alto), o talo afunda sob o peso e a cupula sobe. Silhueta que
    # MUDA DE TAMANHO e o telegrafo mais legivel que um bicho imovel consegue dar
    # -- ele nao tem braco para erguer nem passo para recuar.
    aviso = {}
    anim.curva(aviso, "bulbo", "scale",
               [(0.0, anim.escala()), (DUR_WINDUP * 0.6, anim.escala(1.12, 1.06, 1.12)),
                (DUR_WINDUP, anim.escala(1.18, 1.1, 1.18))])
    anim.curva(aviso, "talo", "scale",
               [(0.0, anim.escala()), (DUR_WINDUP, anim.escala(1.06, 0.85, 1.06))])
    anim.curva(aviso, "cupula", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=1.5))])
    anim.curva(aviso, "poro", "position",
               [(0.0, anim.vetor()), (DUR_WINDUP, anim.vetor(y=1.0))])
    anim.curva(aviso, "poro", "scale",
               [(0.0, anim.escala()), (DUR_WINDUP, anim.escala(1.2, 1.0, 1.2))])
    a.clipe("windup", DUR_WINDUP, aviso)

    # -------------------------------------------------------------- strike
    # O ESTUFO. Ele NAO machuca ninguem -- o dano do puffball e zero na ficha, e
    # esta janela existe para o jogador associar "chegar perto" a "aquilo cuspiu".
    # O bulbo comprime e o poro dispara para cima: a inversao de forma entre o
    # windup (inchando) e o strike (comprimindo) e o que separa visualmente o
    # aviso do disparo, sem nenhuma mudanca de cor ou de som.
    estufo = {}
    anim.curva(estufo, "bulbo", "scale",
               [(0.0, anim.escala(1.18, 1.1, 1.18)), (DUR_STRIKE * 0.4, anim.escala(1.02, 1.24, 1.02)),
                (DUR_STRIKE, anim.escala(1.08, 1.06, 1.08))])
    anim.curva(estufo, "cupula", "position",
               [(0.0, anim.vetor(y=1.5)), (DUR_STRIKE * 0.4, anim.vetor(y=2.4)),
                (DUR_STRIKE, anim.vetor(y=1.8))])
    anim.curva(estufo, "poro", "position",
               [(0.0, anim.vetor(y=1.0)), (DUR_STRIKE * 0.4, anim.vetor(y=3.2)),
                (DUR_STRIKE, anim.vetor(y=2.0))])
    anim.curva(estufo, "poro", "scale",
               [(0.0, anim.escala(1.2, 1.0, 1.2)), (DUR_STRIKE * 0.4, anim.escala(1.7, 1.4, 1.7)),
                (DUR_STRIKE, anim.escala(1.3, 1.1, 1.3))])
    a.clipe("strike", DUR_STRIKE, estufo)

    # ------------------------------------------------------------ recovery
    # A JANELA EM QUE ENCOSTAR AINDA E SEGURO. Ela e a mais longa depois do
    # windup de proposito: e o tempo em que o jogador confere que nada aconteceu
    # e decide se arrisca bater. Encurta-la nao mudaria o servidor -- mudaria so a
    # leitura, e o fungo passaria a parecer sempre armado.
    volta = {}
    anim.curva(volta, "bulbo", "scale",
               [(0.0, anim.escala(1.08, 1.06, 1.08)), (DUR_RECOVERY * 0.45, anim.escala(0.96, 1.02, 0.96)),
                (DUR_RECOVERY, anim.escala())])
    anim.curva(volta, "talo", "scale",
               [(0.0, anim.escala(1.06, 0.85, 1.06)), (DUR_RECOVERY, anim.escala())])
    anim.curva(volta, "cupula", "position",
               [(0.0, anim.vetor(y=1.8)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "poro", "position",
               [(0.0, anim.vetor(y=2.0)), (DUR_RECOVERY, anim.vetor())])
    anim.curva(volta, "poro", "scale",
               [(0.0, anim.escala(1.3, 1.1, 1.3)), (DUR_RECOVERY, anim.escala())])
    a.clipe("recovery", DUR_RECOVERY, volta)

    # ------------------------------------------------------------- stagger
    # A INTERRUPCAO PRECISA SER VISIVEL, senao o stagger vira um numero que so o
    # servidor conhece -- e neste bicho ele e mais que leitura: bater nele e o que
    # arma o estouro, e o jogador precisa ver que o golpe CHEGOU. O saco afunda de
    # um lado e volta oscilando, como algo mole que levou pancada.
    tranco = {}
    anim.curva(tranco, "bulbo", "rotation",
               [(0.0, anim.vetor()), (0.1, anim.vetor(x=-16)), (0.26, anim.vetor(x=7)),
                (DUR_STAGGER, anim.vetor())])
    anim.curva(tranco, "bulbo", "scale",
               [(0.0, anim.escala()), (0.1, anim.escala(1.14, 0.86, 1.14)),
                (0.28, anim.escala(0.94, 1.08, 0.94)), (DUR_STAGGER, anim.escala())])
    anim.curva(tranco, "talo", "rotation",
               [(0.0, anim.vetor()), (0.12, anim.vetor(x=-5)), (DUR_STAGGER, anim.vetor())])
    anim.derivar(tranco, "bulbo", "cupula", "rotation", -0.7)
    anim.derivar(tranco, "cupula", "poro", "rotation", -0.5)
    a.clipe("stagger", DUR_STAGGER, tranco)

    # --------------------------------------------------------------- morte
    # O ESTOURO. Um inchaco rapido, a casca cedendo e o topo saindo: a cupula e o
    # poro sobem e giram enquanto o bulbo desaba para quase nada.
    #
    # A escala NUNCA chega a zero. Escala zero nao da erro -- ela SOME com o osso,
    # e o portao da biblioteca reprova por isso. O resto que fica no chao (0.15) e
    # tambem o que o jogador precisa ver para entender que ali havia um bicho.
    ruptura = {}
    anim.curva(ruptura, "bulbo", "scale",
               [(0.0, anim.escala()), (0.18, anim.escala(1.35, 1.3, 1.35)),
                (0.34, anim.escala(0.4, 0.22, 0.4)), (DUR_DEATH, anim.escala(0.2, 0.1, 0.2))])
    anim.curva(ruptura, "bulbo", "rotation",
               [(0.0, anim.vetor()), (0.34, anim.vetor(x=12)), (DUR_DEATH, anim.vetor(x=4))])
    anim.curva(ruptura, "talo", "scale",
               [(0.0, anim.escala()), (0.18, anim.escala(1.1, 0.9, 1.1)),
                (DUR_DEATH, anim.escala(0.9, 0.6, 0.9))])
    anim.curva(ruptura, "cupula", "position",
               [(0.0, anim.vetor()), (0.18, anim.vetor(y=2.0)), (0.34, anim.vetor(y=7.0)),
                (DUR_DEATH, anim.vetor(y=11.0))])
    anim.curva(ruptura, "cupula", "rotation",
               [(0.0, anim.vetor()), (0.34, anim.vetor(x=-28, z=14)),
                (DUR_DEATH, anim.vetor(x=-62, z=34))])
    anim.curva(ruptura, "poro", "position",
               [(0.0, anim.vetor()), (0.18, anim.vetor(y=3.0)), (0.34, anim.vetor(y=10.0)),
                (DUR_DEATH, anim.vetor(y=15.0))])
    anim.curva(ruptura, "poro", "scale",
               [(0.0, anim.escala()), (0.18, anim.escala(1.6, 1.3, 1.6)),
                (DUR_DEATH, anim.escala(0.5, 0.4, 0.5))])
    a.clipe("death", DUR_DEATH, ruptura)

    return a


if __name__ == "__main__":
    clipes(anim.carregar_geo(MOB)).emitir(ataques=ATAQUES,
                                          extras=(valida_morte_cabe_na_janela_de_morte,))
