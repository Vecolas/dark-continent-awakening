"""Geometria do Wolf Runner -- a formiga quimera que FLANQUEIA (issue #121).

O QUE O CORPO PRECISA CONTAR, e o que acontece se ele nao contar.

A primeira coisa e VELOCIDADE, e ela se le pela silhueta antes de qualquer
numero de ficha. Peito estreito, patas longas, corpo comprido e cauda de leme.
Um corpo atarracado com HP 28 e velocidade 0.38 promete um bicho que segura a
linha, e o jogador trata o encontro como um cerco frontal -- exatamente a leitura
errada para um mob cujo unico truque e CHEGAR pelo lado.

A segunda e que ele e FORMIGA QUIMERA, e nao lobo. Sem nenhuma marca insetoide o
bicho le como lobo comum, a colonia perde identidade visual, e nada no
repositorio acusa isso: o mob nasce, anda, morde e passa em todo portao. As
marcas sao TRES e sao ossos, nao pintura -- placa de quitina no dorso
(`plate`) e um par de antenas (`antenna_left`, `antenna_right`) --, mais a
quelicera pintada na mandibula. Osso, e nao pintura, porque a issue #120 proibe
skeleton procedural em runtime: trait visivel precisa existir como osso, e um
desenho pintado migraria de lugar na primeira correcao de textura sem que regua
nenhuma percebesse.

A terceira e o FOCINHO, e essa e a unica que tem regra do servidor do outro
lado. A caixa de mordida reivindica 1.0 bloco a frente; o focinho desenhado
alcanca 0.5, e a diferenca e paga pelo avanco do bote. As duas pontas dessa
conta sao cobradas -- aqui, por `valida_focinho_alcanca_a_mordida`, e la, por
`WolfRunnerTuningTest`.

A HITBOX MANDA NO MODELO. A entidade e sized(1.0F, 0.9F) -- 16.0 x 14.4 px -- e
a bateria da biblioteca reprova se o modelo estourar isso. O bicho e compacto de
verdade: os 16 px de comprimento vao da ponta da antena (z=-8) a ponta da cauda
(z=+8), sem folga nenhuma. O piso das patas fica em y=0.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de AttackHitbox a frente e +Z, porque com yaw 0 o olhar
vanilla aponta para +Z. Misturar as duas ja custou um bug neste repositorio -- a
caixa do golpe ficou ATRAS do mob, que atacava, animava e nao encostava em
ninguem na frente. Ver o comentario de DummyEnemyEntity.CAIXA_DO_GOLPE.

Regerar:  python art-source/enemies/wolf_runner/wolf_runner_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/wolf_runner.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "wolf_runner"
UV_LARGURA, UV_ALTURA = 64, 64

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(1.0F, 0.9F).
HITBOX = (1.0, 0.9)

# Alcance, em BLOCOS, que a caixa de mordida do servidor reivindica a frente.
# WolfRunnerTuning.caixaDaMordida(): maxZ = 1.0D.
ALCANCE_DA_CAIXA_DE_MORDIDA_EM_BLOCOS = 1.0

# Quanto o corpo VIAJA para a frente durante a janela que machuca.
# WolfRunnerTuning.AVANCO_DA_INVESTIDA = 0.5D.
#
# Ele esta aqui para ser somado ao focinho desenhado e COBRADO contra a caixa.
# Caixa que chega mais longe do que focinho + avanco nao da erro nenhum: da um
# jogador que apanha de uma boca que, na tela, parou antes dele.
AVANCO_DA_INVESTIDA_EM_BLOCOS = 0.5

# O gene pool desta familia, copiado de ChimeraPeonDefinitions.wolfRunner():
#   traitsPossiveis  = {SPEED, LEAP, CLAWS, TAIL}
#   traitsGarantidos = {SPEED, LEAP}
#
# Duplicacao DECLARADA. A outra ponta e o Java; as duas juntas formam um portao
# que morde dos dois lados -- quem acrescentar um trait la sem dar osso a ele
# reprova aqui, e quem apagar o osso daqui reprova aqui tambem. A regra que as
# liga e a da issue #120: trait visivel precisa de osso no modelo, porque
# skeleton procedural em runtime esta proibido.
OSSOS_QUE_PAGAM_O_TRAIT = {
    # correr e saltar sao as duas coisas que este bicho faz, e as duas saem das
    # mesmas quatro patas. Sem elas o molde garantiria SPEED e LEAP num corpo
    # que nao tem com que correr nem com que saltar.
    "SPEED": ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"),
    "LEAP": ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"),
    "CLAWS": ("paw_front_left", "paw_front_right", "paw_back_left", "paw_back_right"),
    "TAIL": ("tail",),
}

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# `neck` existe separado de `body` porque o telegrafo do bote e um AGACHAMENTO:
# o corpo desce e a cabeca continua encarando o alvo. Com a cabeca pendurada
# direto no tronco, os dois desceriam juntos e o bicho olharia para o chao no
# unico quadro em que o jogador precisa saber para onde ele vai pular.
#
# As patas penduram em `body` e nao em `neck`: pendurada no pescoco, a pata
# dianteira acompanharia a cabeca no agachamento e o bicho ajoelharia de frente
# em vez de agachar atras. O pai existiria, o portao Java passaria, e o
# telegrafo apontaria para o lugar errado.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 9, 1)),
    geo.Osso("plate", "body", (0, 12, 1)),
    geo.Osso("neck", "body", (0, 10, -3)),
    geo.Osso("head", "neck", (0, 10, -4)),
    geo.Osso("jaw", "head", (0, 9, -6)),
    geo.Osso("antenna_left", "head", (2, 11, -6)),
    geo.Osso("antenna_right", "head", (-2, 11, -6)),
    geo.Osso("tail", "body", (0, 10, 4)),
    geo.Osso("leg_front_left", "body", (3, 7, -1)),
    geo.Osso("leg_front_right", "body", (-3, 7, -1)),
    geo.Osso("leg_back_left", "body", (3, 7, 3)),
    geo.Osso("leg_back_right", "body", (-3, 7, 3)),
    geo.Osso("paw_front_left", "leg_front_left", (3, 2, -1)),
    geo.Osso("paw_front_right", "leg_front_right", (-3, 2, -1)),
    geo.Osso("paw_back_left", "leg_back_left", (3, 2, 2)),
    geo.Osso("paw_back_right", "leg_back_right", (-3, 2, 2)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
#
# A folha e 64x64 e nao 128x64: o bicho inteiro cabe em 1362 px de atlas, e uma
# folha grande demais nao da erro -- so gasta memoria de textura por mob, e sao
# vinte e tres mobs.
CAIXAS = (
    # tronco: 6 px de largura para 8 de comprimento. Estreito e comprido e a
    # silhueta de corredor; quadrado seria a de um bicho que segura a linha.
    geo.Caixa("body", "body", 0, 0, -3, 6, -3, 6, 6, 8),
    # a placa de quitina do dorso -- marca insetoide numero 1. Ela fica em cima
    # do lombo porque e a unica vista de cima, e de cima e como o jogador ve uma
    # matilha que o esta contornando.
    geo.Caixa("plate", "plate", 0, 29, -2, 12, -1, 4, 1, 4),
    # pescoco baixo e curto: a cabeca sai a FRENTE do peito, e nao acima dele.
    # Cabeca alta le como canideo de postura ereta, que anda; cabeca baixa le
    # como bicho em corrida.
    geo.Caixa("neck", "neck", 0, 16, -2, 7, -5, 4, 4, 3),
    geo.Caixa("head", "head", 28, 0, -3, 8, -7, 6, 5, 4),
    # a mandibula, projetada a frente da cara: e ela que define o alcance
    # DESENHADO do focinho, e valida_focinho_alcanca_a_mordida mede exatamente
    # esta caixa.
    geo.Caixa("jaw", "jaw", 48, 0, -2, 7, -8, 4, 2, 3),
    # antenas -- marca insetoide numero 2. Elas ficam FORA da largura da cabeca
    # (x 3..4) de proposito: enfiadas dentro do volume da cara, so a ponta
    # apareceria, e a leitura de inseto sumiria justamente a distancia.
    geo.Caixa("antenna_left", "antenna_left", 48, 5, 3, 11, -8, 1, 1, 4),
    geo.Caixa("antenna_right", "antenna_right", 16, 29, -4, 11, -8, 1, 1, 4),
    # cauda: leme de curva. Ela e o que faz o contorno ler como contorno -- um
    # corredor sem cauda parece deslizar de lado.
    geo.Caixa("tail", "tail", 40, 24, -1, 9, 5, 2, 2, 3),
    # patas: 5 px de canela para 2 de pata, num bicho de 13 px. Metade da altura
    # e perna, e e isso que promete o salto.
    geo.Caixa("leg_front_left", "leg_front_left", 14, 16, 2, 2, -2, 2, 5, 3),
    geo.Caixa("leg_front_right", "leg_front_right", 24, 16, -4, 2, -2, 2, 5, 3),
    geo.Caixa("leg_back_left", "leg_back_left", 34, 16, 2, 2, 1, 2, 5, 3),
    geo.Caixa("leg_back_right", "leg_back_right", 44, 16, -4, 2, 1, 2, 5, 3),
    geo.Caixa("paw_front_left", "paw_front_left", 0, 24, 2, 0, -3, 2, 2, 3),
    geo.Caixa("paw_front_right", "paw_front_right", 10, 24, -4, 0, -3, 2, 2, 3),
    geo.Caixa("paw_back_left", "paw_back_left", 20, 24, 2, 0, 1, 2, 2, 3),
    geo.Caixa("paw_back_right", "paw_back_right", 30, 24, -4, 0, 1, 2, 2, 3),
)


# --------------------------------------------------- validacoes DO BICHO
# As duas abaixo ligam o DESENHO a REGRA DO SERVIDOR. Nenhuma das divergencias
# que elas pegam levanta excecao em lugar nenhum.

def valida_focinho_alcanca_a_mordida(m):
    """O focinho DESENHADO mais o avanco do bote cobrem a caixa de mordida.

    A caixa de dano do servidor vai ate 1.0 bloco a frente do centro do bicho. A
    mandibula desenhada chega a 0.5, porque o modelo inteiro tem de caber nos
    16 px de comprimento da hitbox; a diferenca e paga pelo AVANCO da investida,
    que e o deslocamento que o corpo ganha durante a janela ACTIVE.

    Se a soma nao fechar, o jogador leva 7 de dano de uma boca que, na tela,
    parou antes dele. Dano certo, cooldown certo, log limpo -- e a unica leitura
    que um mob corpo-a-corpo oferece quebrada. E a reclamacao mais dificil de
    diagnosticar que este tipo de bicho consegue gerar.
    """
    _, _, (focinho_z0, _) = geo.volume(m.caixa("jaw"))
    alcance_desenhado = -focinho_z0 / 16.0
    alcance_total = alcance_desenhado + AVANCO_DA_INVESTIDA_EM_BLOCOS
    if alcance_total + 1e-9 < ALCANCE_DA_CAIXA_DE_MORDIDA_EM_BLOCOS:
        raise geo.ErroDeArte(
            "o focinho desenhado alcanca %.3f bloco (z=%s px) e o avanco do bote paga mais %.2f, "
            "somando %.3f -- a caixa de mordida do servidor reivindica %.2f: o jogador apanha de "
            "uma boca que parou antes dele, e nada acusa"
            % (alcance_desenhado, focinho_z0, AVANCO_DA_INVESTIDA_EM_BLOCOS, alcance_total,
               ALCANCE_DA_CAIXA_DE_MORDIDA_EM_BLOCOS))

    # E o inverso tambem e defeito. Focinho MUITO mais longo que a caixa desenha
    # uma boca que atravessa o alvo sem machucar, e o jogador aprende que a
    # mordida "as vezes nao pega" -- o que ele nao consegue distinguir de lag.
    if alcance_desenhado > ALCANCE_DA_CAIXA_DE_MORDIDA_EM_BLOCOS:
        raise geo.ErroDeArte(
            "o focinho desenhado alcanca %.3f bloco e a caixa de mordida so vai a %.2f: a boca "
            "atravessa o alvo sem machucar, e o jogador le isso como lag"
            % (alcance_desenhado, ALCANCE_DA_CAIXA_DE_MORDIDA_EM_BLOCOS))


def valida_ossos_para_os_traits(m):
    """Todo trait do gene pool desta familia tem osso com volume para mostra-lo.

    O molde `ChimeraPeonDefinitions.wolfRunner()` sorteia entre SPEED, LEAP,
    CLAWS e TAIL, e garante SPEED e LEAP. A issue #120 proibe skeleton
    procedural em runtime, entao cada um desses traits precisa ja existir como
    osso -- nao ha como fazer nascer uma cauda no momento do sorteio.

    Um trait sem osso nao da erro nenhum: a identidade guarda TAIL, o NBT salva
    TAIL, a colonia conta TAIL, e o bicho aparece sem cauda. O jogador descreve
    isso como "as vezes ele tem cauda e as vezes nao", que e a pior descricao de
    bug que existe, porque manda a proxima pessoa procurar aleatoriedade.
    """
    nomes = {o.nome for o in m.ossos}
    com_volume = {c.osso for c in m.caixas}
    for trait in sorted(OSSOS_QUE_PAGAM_O_TRAIT):
        for osso in OSSOS_QUE_PAGAM_O_TRAIT[trait]:
            if osso not in nomes:
                raise geo.ErroDeArte(
                    "o gene pool desta familia pode sortear %s e o modelo nao tem o osso '%s': a "
                    "identidade guardaria o trait, a colonia o contaria, e o bicho apareceria sem "
                    "ele -- o jogador descreve isso como aleatoriedade" % (trait, osso))
            if osso not in com_volume:
                raise geo.ErroDeArte(
                    "o osso '%s', que paga o trait %s, nao tem cubo nenhum: ele existe na "
                    "hierarquia, a animacao o move, e a tela nao muda" % (osso, trait))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_focinho_alcanca_a_mordida,
                          valida_ossos_para_os_traits))
