"""Geometria do Scorpion Leader -- a formiga cuja ameaca nao termina no golpe.

O QUE O CORPO PRECISA CONTAR, e o que acontece se ele nao contar.

Esta formiga tem HP 120 e armadura 8 numa caixa de 1.5 x 1.6 bloco, e o dano
direto dela (12) e MENOR que o do guepardo (14). O preco real dela vem depois do
golpe, em veneno -- e veneno e a unica coisa de um encontro que o jogador nao ve
sair de lugar nenhum. A silhueta e o unico aviso que existe, e por isso ela e
feita de duas leituras opostas:

  * o CORPO e baixo, largo e chapado. Ele diz "blindado", que e o que armadura 8
    significa na pratica: bater de frente custa caro e nao resolve;
  * a CAUDA sobe acima de tudo e termina num ferrao apontado para a frente. Ela e
    a leitura de "isto envenena" a vinte blocos, e e o unico traco vertical da
    silhueta inteira.

Se a cauda nao for a peca mais alta, o bicho vira "mais uma formiga blindada": o
jogador nao aprende que existe um segundo tipo de dano, apanha do ferrao, ve 12
de dano na tela e culpa o balanceamento. Nada disso levanta excecao.

A CAUDA E UMA CORRENTE DE OSSOS, e nao um bloco so. Sao quatro elos
(tail_base -> tail_mid -> tail_tip -> stinger) porque o telegrafo do ferrao e um
movimento de CHICOTE: ela arma para tras, passa por cima do dorso e desce na
frente. Uma cauda de um osso so giraria em torno da raiz e leria como um mastro
batendo -- e o quadro em que o ferrao cruza o dorso, que e o unico quadro que
separa o ferrao do golpe de pinca, deixaria de existir.

DUAS REGUAS LIGAM ESTE ARQUIVO AO SERVIDOR, e nenhuma das duas divergencias
levanta excecao:

  * `valida_alcance_do_ferrao` -- o raio DESENHADO da cauda mais o arranco que a
    entidade aplica tem de cobrir o `maxZ` de ScorpionLeaderTuning.caixaDoFerrao();
  * `valida_alcance_da_pinca` -- a pinca DESENHADA tem de chegar tao longe quanto
    ScorpionLeaderTuning.caixaDaPinca() reivindica.

Caixa maior que o desenho da um jogador que apanha de uma peca que, na tela,
parou antes dele: dano certo, cooldown certo, log limpo, e a unica leitura que
ele tem quebrada.

A HITBOX MANDA NO MODELO. A entidade e sized(1.5F, 1.6F) -- 24 x 25.6 px -- e a
bateria da biblioteca reprova se o modelo estourar isso. O piso das pernas fica
em y=0.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de AttackHitbox a frente e +Z, porque com yaw 0 o olhar
vanilla aponta para +Z. Misturar as duas ja custou um bug neste repositorio -- a
caixa do golpe ficou ATRAS do mob, que atacava, animava e nao encostava em
ninguem na frente. Ver o comentario de DummyEnemyEntity.CAIXA_DO_GOLPE.

Regerar:  python art-source/enemies/scorpion_leader/scorpion_leader_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/scorpion_leader.geo.json
"""
import math
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "scorpion_leader"
UV_LARGURA, UV_ALTURA = 128, 64

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(1.5F, 1.6F).
HITBOX = (1.5, 1.6)

# ---------------------------------------------------------------------------
# Os numeros do SERVIDOR que este arquivo cobra. Cada um traz ao lado a constante
# de onde foi copiado -- e essa duplicacao e DECLARADA: sao as duas pontas de um
# portao que morde dos dois lados. Quem encolher a cauda aqui reprova aqui; quem
# esticar a caixa la reprova em ScorpionLeaderTuningTest.
# ---------------------------------------------------------------------------

# ScorpionLeaderTuning.caixaDoFerrao(): maxZ = 1.05D.
ALCANCE_DA_CAIXA_DO_FERRAO_EM_BLOCOS = 1.05
# ScorpionLeaderTuning.AVANCO_DO_FERRAO = 0.55D -- o arranco que a entidade
# aplica UMA vez no primeiro tick da janela ativa. Ele e a parte do alcance que o
# desenho nao paga; sem ele, a caixa teria de caber inteira dentro do raio da
# cauda, e a cauda teria de ser mais longa que a propria hitbox.
AVANCO_DO_FERRAO_EM_BLOCOS = 0.55
# ScorpionLeaderTuning.caixaDaPinca(): maxZ = 0.85D. A pinca NAO tem arranco: ela
# e o golpe curto e sem veneno, e o alcance dela sai inteiro do desenho.
ALCANCE_DA_CAIXA_DA_PINCA_EM_BLOCOS = 0.85

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# A CAUDA PENDURA EM CADEIA, elo por elo. Pendurar `stinger` direto em `body`
# faria o pai existir, o portao Java passar, e o ferrao ficar PARADO enquanto os
# elos giram embaixo dele: o chicote perde a ponta, e a ponta e a informacao.
#
# As pincas penduram no braco, e nao no corpo. Penduradas no corpo elas abririam
# no lugar certo e na hora errada -- o braco avanca antes da pinca fechar, e e
# esse meio quadro de atraso que faz a garra ler como garra.
#
# As oito pernas penduram no corpo e tem pivot na BORDA dele (x = +-7), que e
# onde a junta existe de verdade. Pivot no centro faria cada perna girar em torno
# do meio do bicho, e o passo apareceria como a perna deslizando pelo chao.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 6, 0)),
    geo.Osso("head", "body", (0, 6, -7)),
    geo.Osso("arm_left", "body", (7, 6, -5)),
    geo.Osso("arm_right", "body", (-7, 6, -5)),
    geo.Osso("claw_left", "arm_left", (8, 6, -9)),
    geo.Osso("claw_right", "arm_right", (-8, 6, -9)),
    geo.Osso("tail_base", "body", (0, 8, 7)),
    geo.Osso("tail_mid", "tail_base", (0, 14, 6)),
    geo.Osso("tail_tip", "tail_mid", (0, 19, 5)),
    geo.Osso("stinger", "tail_tip", (0, 21, 2)),
    geo.Osso("leg_1_left", "body", (7, 5, -5)),
    geo.Osso("leg_2_left", "body", (7, 5, -1)),
    geo.Osso("leg_3_left", "body", (7, 5, 3)),
    geo.Osso("leg_4_left", "body", (7, 5, 7)),
    geo.Osso("leg_1_right", "body", (-7, 5, -5)),
    geo.Osso("leg_2_right", "body", (-7, 5, -1)),
    geo.Osso("leg_3_right", "body", (-7, 5, 3)),
    geo.Osso("leg_4_right", "body", (-7, 5, 7)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
CAIXAS = (
    # carapaca: larga (14) e BAIXA (5). A altura e a decisao: um torax alto leria
    # como aranha, e aranha nao promete armadura. Ela tambem e a peca mais
    # comprida, para que a silhueta de perfil seja uma placa, e nao um corpo.
    geo.Caixa("carapaca", "body", 0, 0, -7, 4, -7, 14, 5, 15),
    # cabeca: pequena e encaixada na frente da carapaca. Pequena de proposito --
    # quem tem de dominar a frente sao as pincas, e uma cabeca grande disputaria
    # o quadro em que elas fecham.
    geo.Caixa("head", "head", 58, 0, -4, 4, -11, 8, 4, 4),
    # os quatro elos da cauda, afinando para cima. O afinamento nao e enfeite: e
    # o que faz a ponta ler como PONTA numa silhueta de vinte blocos de distancia.
    geo.Caixa("tail_base", "tail_base", 82, 0, -3, 9, 3, 6, 6, 6),
    geo.Caixa("tail_mid", "tail_mid", 106, 0, -2, 15, 3, 4, 5, 5),
    geo.Caixa("tail_tip", "tail_tip", 0, 20, -2, 20, 1, 4, 4, 5),
    # ferrao: a peca mais a frente da cauda, curvada por cima do dorso. E ele que
    # `valida_alcance_do_ferrao` mede, e e ele que a textura pinta de veneno.
    geo.Caixa("stinger", "stinger", 18, 20, -1, 17, -2, 2, 4, 4),
    # bracos: finos e curtos, so o bastante para a pinca ter de onde sair.
    geo.Caixa("arm_left", "arm_left", 30, 20, 7, 5, -10, 3, 3, 6),
    geo.Caixa("arm_right", "arm_right", 48, 20, -10, 5, -10, 3, 3, 6),
    # pincas: a peca mais a frente do bicho inteiro (z = -14). Esse -14 e o numero
    # que valida_alcance_da_pinca compara com a caixa de dano do golpe comum.
    geo.Caixa("claw_left", "claw_left", 66, 20, 6, 4, -14, 5, 5, 5),
    geo.Caixa("claw_right", "claw_right", 86, 20, -11, 4, -14, 5, 5, 5),
    # oito pernas, em quatro pares. Curtas e finas: elas sustentam um corpo que
    # tem de parecer colado no chao, e perna alta desmentiria a armadura.
    geo.Caixa("leg_1_left", "leg_1_left", 0, 30, 7, 0, -6, 2, 4, 2),
    geo.Caixa("leg_2_left", "leg_2_left", 8, 30, 7, 0, -2, 2, 4, 2),
    geo.Caixa("leg_3_left", "leg_3_left", 16, 30, 7, 0, 2, 2, 4, 2),
    geo.Caixa("leg_4_left", "leg_4_left", 24, 30, 7, 0, 6, 2, 4, 2),
    geo.Caixa("leg_1_right", "leg_1_right", 32, 30, -9, 0, -6, 2, 4, 2),
    geo.Caixa("leg_2_right", "leg_2_right", 40, 30, -9, 0, -2, 2, 4, 2),
    geo.Caixa("leg_3_right", "leg_3_right", 48, 30, -9, 0, 2, 2, 4, 2),
    geo.Caixa("leg_4_right", "leg_4_right", 56, 30, -9, 0, 6, 2, 4, 2),
)


def raio_do_chicote(m):
    """Distancia do pivot da cauda ao ponto mais distante do ferrao, em px.

    E esta a medida que vira ALCANCE quando a cauda desenrola para a frente: o
    ferrao descreve um arco em torno de `tail_base`, e nenhum ponto dele pode
    passar do raio. Medir pela ponta do cubo -- e nao pelo centro -- e o que
    impede a regua de aprovar uma cauda curta por meio pixel.
    """
    px, py, pz = m.osso("tail_base").pivot
    (x0, x1), (y0, y1), (z0, z1) = geo.volume(m.caixa("stinger"))
    return max(math.sqrt((x - px) ** 2 + (y - py) ** 2 + (z - pz) ** 2)
               for x in (x0, x1) for y in (y0, y1) for z in (z0, z1))


# --------------------------------------------------- validacoes DO BICHO
# As duas primeiras ligam o DESENHO a REGRA DO SERVIDOR. A terceira e a leitura:
# ela nao tem numero no Java, e e a unica coisa que impede o bicho de virar "mais
# uma formiga blindada".

def valida_alcance_do_ferrao(m):
    """A cauda desenhada, mais o arranco, tem de cobrir a caixa do ferrao.

    A caixa de dano do servidor e LOCAL e mede a partir do centro da entidade. O
    ferrao alcanca, na tela, o raio do chicote MENOS a distancia em que o pivot da
    cauda esta atras do centro -- porque o arco se abre de tras para a frente. O
    resto do alcance e pago pelo arranco de ScorpionLeaderTuning.AVANCO_DO_FERRAO,
    aplicado UMA vez no primeiro tick da janela ativa.

    Se a soma nao cobrir o `maxZ` da caixa, o jogador leva veneno de um ferrao
    que, na tela, parou antes dele -- e veneno e justamente o dano que ele nao
    consegue rastrear ate a origem. Nao ha erro para procurar: a fase esta certa,
    o cooldown esta certo e o log esta limpo.

    A regua tambem cobra o inverso, que e o defeito mais facil de cometer aqui:
    caixa MUITO menor que o desenho. Um ferrao que atravessa visivelmente o alvo
    sem encostar ensina o jogador a ignorar o telegrafo mais caro do bicho.
    """
    _, _, pivo_z = m.osso("tail_base").pivot
    alcance_px = raio_do_chicote(m) - pivo_z
    alcance_blocos = alcance_px / 16.0
    coberto = alcance_blocos + AVANCO_DO_FERRAO_EM_BLOCOS
    if coberto + 1e-9 < ALCANCE_DA_CAIXA_DO_FERRAO_EM_BLOCOS:
        raise geo.ErroDeArte(
            "o ferrao desenhado alcanca %.3f bloco a frente do centro e o arranco paga mais "
            "%.2f, total %.3f; a caixa do ferrao vai ate %.2f: o jogador seria envenenado por "
            "uma ponta que parou antes dele, e veneno e justamente o dano que ele nao consegue "
            "rastrear" % (alcance_blocos, AVANCO_DO_FERRAO_EM_BLOCOS, coberto,
                          ALCANCE_DA_CAIXA_DO_FERRAO_EM_BLOCOS))
    if coberto > ALCANCE_DA_CAIXA_DO_FERRAO_EM_BLOCOS + 0.5:
        raise geo.ErroDeArte(
            "o ferrao desenhado mais o arranco chegam a %.3f bloco e a caixa so vai ate %.2f: "
            "sobra mais de meio bloco de ponta que atravessa o alvo sem encostar, e o jogador "
            "aprende a ignorar o telegrafo mais caro do bicho"
            % (coberto, ALCANCE_DA_CAIXA_DO_FERRAO_EM_BLOCOS))


def valida_alcance_da_pinca(m):
    """A pinca DESENHADA tem de chegar tao longe quanto a caixa do golpe comum.

    O golpe de pinca e o ataque que NAO envenena, e por isso ele nao ganha
    arranco nenhum: o alcance dele sai inteiro do desenho. A ponta da pinca esta a
    `-z` px do eixo z=0 do modelo, e essa e a distancia que a silhueta promete.

    Caixa maior que a pinca da um jogador que apanha de uma garra que parou antes
    dele -- e como este e o golpe comum, ele acontece muitas vezes por encontro, o
    que transforma um erro de meio bloco na reclamacao mais dificil de diagnosticar
    que um bicho corpo-a-corpo consegue gerar.

    A regua tambem cobra que a pinca seja a peca mais a FRENTE. Pinca atras da
    cabeca nao e pinca: e enfeite, e o quadro em que ela fecha deixa de ser
    visivel justamente quando ele e a unica leitura do golpe curto.
    """
    _, _, (ponta_z, _) = geo.volume(m.caixa("claw_left"))
    alcance_px = -ponta_z
    exigido_px = ALCANCE_DA_CAIXA_DA_PINCA_EM_BLOCOS * 16.0
    if alcance_px < exigido_px:
        raise geo.ErroDeArte(
            "a pinca alcanca %.1f px (%.2f bloco) a frente do centro e a caixa do golpe comum vai "
            "ate %.1f px (%.2f bloco): o jogador apanha de uma garra que parou antes dele, muitas "
            "vezes por encontro, e nada acusa"
            % (alcance_px, alcance_px / 16.0, exigido_px, ALCANCE_DA_CAIXA_DA_PINCA_EM_BLOCOS))
    _, _, (cabeca_z, _) = geo.volume(m.caixa("head"))
    if ponta_z >= cabeca_z:
        raise geo.ErroDeArte(
            "a pinca comeca em z=%s e a cabeca comeca em z=%s: a garra fica atras da cara e o "
            "quadro em que ela fecha, que e a unica leitura do golpe curto, some"
            % (ponta_z, cabeca_z))


def valida_a_cauda_e_a_leitura(m):
    """A cauda tem de ser a peca mais ALTA, e o ferrao tem de passar do dorso.

    Esta e a unica regua deste arquivo sem numero do lado do Java, e ela existe
    porque a mecanica inteira do bicho depende de uma leitura: o veneno e um
    segundo tipo de dano, e o jogador so pode aprender a evita-lo se souber, ANTES
    do golpe, qual dos dois ataques esta vindo.

    Duas coisas sao cobradas:

      * a ponta da cauda e o ponto mais alto do modelo. Se a carapaca ou a pinca
        subissem acima dela, a silhueta pararia de ter um traco vertical e o bicho
        leria como "mais uma formiga blindada" -- e o jogador so descobriria o
        veneno depois de tomar o primeiro;
      * o ferrao termina A FRENTE do pivot da cauda, ou seja, curvado por cima do
        dorso. Um ferrao apontado para tras deixaria o telegrafo do chicote
        comecando fora da tela do jogador que esta de frente, que e exatamente
        quem precisa le-lo.
    """
    _, (_, topo_do_modelo), _ = m.limites()
    _, (_, topo_da_cauda), _ = geo.volume(m.caixa("tail_tip"))
    if topo_da_cauda < topo_do_modelo:
        raise geo.ErroDeArte(
            "a cauda termina em y=%s e o modelo sobe ate y=%s: outra peca e o ponto alto da "
            "silhueta, a cauda deixa de ser o traco vertical que anuncia veneno, e o jogador so "
            "descobre o segundo tipo de dano depois de toma-lo"
            % (topo_da_cauda, topo_do_modelo))
    _, _, pivo_z = m.osso("tail_base").pivot
    _, _, (ferrao_z0, _) = geo.volume(m.caixa("stinger"))
    if ferrao_z0 >= pivo_z:
        raise geo.ErroDeArte(
            "o ferrao comeca em z=%s e a cauda nasce em z=%s: a ponta fica ATRAS do bicho, o "
            "chicote arma fora do campo de visao de quem esta de frente, e o telegrafo mais caro "
            "do encontro nao chega a ser visto" % (ferrao_z0, pivo_z))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_alcance_do_ferrao,
                          valida_alcance_da_pinca,
                          valida_a_cauda_e_a_leitura))
