"""Geometria do Bat Scout -- a formiga quimera que AVISA (issue #121, peon).

O QUE O CORPO PRECISA CONTAR, e o que acontece se ele nao contar.

Este bicho nao e uma ameaca: HP 16, dano 3, armadura 0. O que ele faz e VER
primeiro e CONTAR. O alcance de percepcao dele e 32 -- o maior de qualquer peon
-- e a resposta que ele ensina e matar o batedor antes do relatorio. Nada disso
aparece numa ficha que o jogador leia; aparece na silhueta, e a silhueta deste
mob e a ASA.

Por isso a primeira decisao do modelo e esta: **a asa dobrada e a pose de
repouso, e a asa aberta e ANIMACAO.** Ela nao existe como caixa alternativa, e
essa e a diferenca inteira. Uma segunda caixa "asa aberta" precisaria ser
ligada e desligada por alguem, e o dia em que os dois estados ficarem ligados ao
mesmo tempo nao da erro nenhum: da um morcego com quatro asas. Uma pose de
animacao nao tem esse estado para esquecer.

A consequencia e medivel, e a regua `valida_envergadura_contra_a_hitbox` mede:
dobrada, a silhueta cabe FOLGADA dentro da caixa de colisao de 0.8 bloco;
aberta, ela NAO cabe -- e e justamente por nao caber que ela nao pode ser
repouso. Um modelo desenhado ja aberto prometeria, parado, um corpo muito maior
do que o servidor deixa acertar, e o jogador aprenderia uma mira que o jogo nao
paga.

A segunda coisa que o corpo conta e o OLHO. A regra de percepcao deste bicho
(`RegrasDeVisaoNoturna`, alimentada pelo trait `NIGHT_VISION` que
`ChimeraPeonDefinitions.batScout()` GARANTE) diz que o alcance dele **nao cai no
escuro**. Isso e invisivel: o jogador so consegue saber se o desenho disser
antes. Num bicho de 0.8 x 0.9 bloco o unico jeito de dizer "esse enxerga no
escuro" e o olho tomar a cara, e e a folha de textura que cobra isso.

A terceira e a CABECA NA FRENTE E NO EIXO. O cone de visao da entidade tem 80
graus de meia-abertura e e SIMETRICO em torno do olhar
(`BatScoutEntity.ABERTURA_DA_VISAO` -> `VisionCone.deGraus`). Uma cabeca pintada
de lado faria o desenho apontar para um lugar e a regra medir outro, e nada
acusaria isso.

A HITBOX MANDA NO MODELO. A entidade e sized(0.8F, 0.9F) -- 12.8 x 14.4 px -- e
a bateria da biblioteca reprova se o modelo PARADO estourar isso. O piso fica em
y=0: aqui ele e a ponta da asa dobrada e a garra do pe, que e como um morcego
pousado toca o chao.

A CAIXA DE VISIBILIDADE E DECLARADA, e nao herdada. O padrao da biblioteca sobra
meio bloco em volta do modelo PARADO -- e o modelo parado deste bicho e o de asa
fechada. Com a asa aberta a silhueta quase dobra de largura, e uma caixa apertada
nao da erro: faz o morcego SUMIR da tela quando a camera pega o angulo em que a
caixa sai do frustum, e some exatamente no quadro do bater de asa.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de `AttackHitbox.noMundo` a frente e +Z, porque com yaw 0
o olhar vanilla aponta para +Z. Misturar as duas ja custou um bug neste
repositorio: a caixa do golpe ficou ATRAS do mob, que atacava, animava e nao
encostava em ninguem na frente.

Regerar:  python art-source/enemies/bat_scout/bat_scout_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/bat_scout.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "bat_scout"
UV_LARGURA, UV_ALTURA = 64, 64

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(0.8F, 0.9F).
HITBOX = (0.8, 0.9)

# Envergadura MINIMA da asa aberta, em blocos.
#
# Limite de LEITURA, e nao botao de balanceamento: ninguem vai gira-lo numa
# sessao de ajuste. Ele existe porque a asa e a unica coisa que separa este mob,
# a vinte blocos, de um rato voando. Abaixo de um bloco e um quarto a silhueta
# aberta deixa de ler como asa e passa a ler como "bicho com dois pedacos
# soltos" -- e isso nao aparece em portao nenhum, so na tela.
ENVERGADURA_MINIMA_EM_BLOCOS = 1.25

# Caixa de visibilidade declarada, em blocos: largura, altura e deslocamento.
# Ela precisa caber a asa ABERTA, que e maior que o modelo parado. Ver o
# cabecalho.
BOUNDS = (2.0, 1.5, [0, 0.75, 0])

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# `wing_left_tip` pendura em `wing_left`, e NAO no corpo. Pendurada no corpo o
# pai existe, o portao Java passa, e a ponta fica parada no ar enquanto o braco
# da asa gira embaixo dela: a asa abre pela metade. E a asa aberta e o unico
# quadro que este bicho tem para ler de longe.
#
# As orelhas sao ossos PROPRIOS e nao um retangulo pintado na cara. Elas
# precisam reagir sozinhas -- deitar no windup, cair na morte -- e detalhe
# pintado nao reage. Alem disso, so um osso com cubo tem altura MEDIVEL.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 7, 1)),
    # o pescoco fica na quina da frente do corpo: e de la que a cabeca se
    # projeta, e o pivot precisa cair DENTRO do volume do pai
    geo.Osso("head", "body", (0, 10, -1)),
    geo.Osso("ear_left", "head", (1, 11, -3)),
    geo.Osso("ear_right", "head", (-1, 11, -3)),
    # os ombros ficam no alto do flanco: e ali que a asa dobrada se prende e
    # tambem o eixo em torno do qual ela abre
    geo.Osso("wing_left", "body", (2, 9, 0)),
    geo.Osso("wing_right", "body", (-2, 9, 0)),
    # o "punho" da asa, no pe da membrana dobrada
    geo.Osso("wing_left_tip", "wing_left", (2.5, 2, 0)),
    geo.Osso("wing_right_tip", "wing_right", (-2.5, 2, 0)),
    geo.Osso("foot_left", "body", (1, 4, 1)),
    geo.Osso("foot_right", "body", (-1, 4, 1)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
#
# A folha e 64x64 e nao 128x64: as dez caixas somam 932 px, menos de um quarto do
# atlas. Uma folha maior seria area reservada para nada, e area de atlas nao e
# de graca -- ela vai para a memoria de textura de todo cliente conectado.
CAIXAS = (
    # tronco pequeno e compacto: num morcego o corpo e quase so o ponto de onde
    # as asas saem, e inflar o tronco rouba a silhueta da asa
    geo.Caixa("body", "body", 0, 0, -2, 4, -1, 4, 6, 4),
    # cabeca do MESMO tamanho do tronco, de proposito. Ela e o painel onde o olho
    # precisa ser legivel a vinte blocos, e olho legivel exige cara grande -- num
    # bicho de 14 px de altura, cara grande e a unica forma de cara.
    geo.Caixa("head", "head", 16, 0, -2, 7, -5, 4, 4, 4),
    # orelhas: altas e finas. Elas sao o segundo sinal de leitura do bicho e a
    # razao de o modelo ir ate 14 px -- 0.4 px abaixo do teto da hitbox.
    geo.Caixa("ear_left", "ear_left", 0, 10, 0.5, 11, -4, 1, 3, 2),
    geo.Caixa("ear_right", "ear_right", 6, 10, -1.5, 11, -4, 1, 3, 2),
    # a asa DOBRADA: uma placa de 1 px de espessura colada no flanco, cobrindo o
    # corpo da altura do ombro ate quase o chao. Espessura 1 e deliberada -- uma
    # asa com volume nao e asa, e ainda empurraria a silhueta parada contra a
    # parede da hitbox.
    geo.Caixa("wing_left", "wing_left", 32, 0, 2, 2, -2, 1, 8, 6),
    geo.Caixa("wing_right", "wing_right", 46, 0, -3, 2, -2, 1, 8, 6),
    # a ponta dobrada, recolhida para a frente e para baixo: e ela que toca o
    # chao quando o bicho pousa, e e ela que descreve o maior arco quando a asa
    # abre
    geo.Caixa("wing_left_tip", "wing_left_tip", 24, 16, 2, 0, -1, 1, 2, 4),
    geo.Caixa("wing_right_tip", "wing_right_tip", 34, 16, -3, 0, -1, 1, 2, 4),
    # pes: pequenos e com garra. Eles existem porque um morcego pousado pendura,
    # e porque o clipe de ocio precisa de um ponto de apoio que nao seja a asa.
    geo.Caixa("foot_left", "foot_left", 12, 10, 0.5, 0, 0, 1, 4, 2),
    geo.Caixa("foot_right", "foot_right", 18, 10, -1.5, 0, 0, 1, 4, 2),
)


# --------------------------------------------------- validacoes DO BICHO
# As duas abaixo ligam o DESENHO a REGRA DO SERVIDOR. Nenhuma das divergencias
# que elas pegam levanta excecao em lugar nenhum.

def envergadura_aberta_em_px(m):
    """Quanto a asa ABERTA ocupa de ponta a ponta, em px do modelo.

    A conta e a do arco: a asa gira em torno do ombro, entao tudo que esta
    ABAIXO do pivot vira alcance LATERAL quando ela abre. O alcance de um lado e
    (pivot_y - pe_da_ponta), e a envergadura e o dobro disso mais a distancia
    entre os dois ombros.

    Ela e derivada do modelo e nao escrita a mao de proposito: escrita a mao, ela
    continuaria dizendo 22 px no dia em que alguem encurtasse a membrana, e a
    regua passaria a aprovar exatamente o que existe para reprovar.
    """
    ombro_x, ombro_y, _ = m.osso("wing_left").pivot
    _, (pe_da_ponta, _), _ = geo.volume(m.caixa("wing_left_tip"))
    alcance = ombro_y - pe_da_ponta
    return 2.0 * (ombro_x + alcance)


def valida_envergadura_contra_a_hitbox(m):
    """Dobrada a asa CABE na caixa de colisao; aberta ela NAO cabe -- e e o ponto.

    O servidor declara sized(0.8F, 0.9F) e essa caixa e o que o jogador consegue
    acertar. As duas metades desta regua cobram coisas opostas, e as duas falham
    em silencio:

    1. DOBRADA com folga. A silhueta parada tem de sobrar espaco dentro da
       hitbox. Sem folga, a primeira correcao de membrana estoura a caixa de
       colisao e o bicho passa a prometer, parado, um corpo maior do que o
       servidor deixa acertar.
    2. ABERTA fora da caixa. Se a asa aberta coubesse na hitbox, ela nao seria
       asa: seria um detalhe. E o que impede alguem de "simplificar" o modelo
       desenhando a asa ja aberta e dispensando a animacao e exatamente isto --
       aberta, ela nao cabe, e portanto nao pode ser pose de repouso.

    Nenhuma das duas levanta excecao em jogo. A primeira da uma mira que o jogo
    nao paga; a segunda da um morcego permanentemente de asas abertas, que o
    jogador le como bug de animacao.
    """
    (x0, x1), _, _ = m.limites()
    largura_dobrada = x1 - x0
    if largura_dobrada > m.hitbox_largura_px - 2.0:
        raise geo.ErroDeArte(
            "com as asas dobradas o modelo tem %.1f px de largura e a hitbox tem %.1f px: sobra "
            "menos de 2 px de folga, e a proxima correcao de membrana estoura a caixa de colisao "
            "sem que nada reprove" % (largura_dobrada, m.hitbox_largura_px))

    aberta = envergadura_aberta_em_px(m)
    if aberta <= m.hitbox_largura_px:
        raise geo.ErroDeArte(
            "a asa aberta alcanca %.1f px e a hitbox tem %.1f px: aberta, a asa CABE na caixa de "
            "colisao -- ou seja, ela nao e a silhueta deste bicho, e nada impede alguem de "
            "desenha-la aberta como pose de repouso" % (aberta, m.hitbox_largura_px))

    minimo = ENVERGADURA_MINIMA_EM_BLOCOS * 16.0
    if aberta < minimo:
        raise geo.ErroDeArte(
            "a asa aberta alcanca %.1f px (%.2f blocos) e o minimo de leitura e %.1f px (%.2f "
            "blocos): a vinte blocos a silhueta aberta deixa de ler como asa e passa a ler como "
            "dois pedacos soltos, e isso so aparece na tela"
            % (aberta, aberta / 16.0, minimo, ENVERGADURA_MINIMA_EM_BLOCOS))

    largura_dos_bounds = BOUNDS[0] * 16.0
    if aberta > largura_dos_bounds:
        raise geo.ErroDeArte(
            "a asa aberta alcanca %.1f px e a caixa de visibilidade declarada tem %.1f px: o "
            "morcego SOME da tela no angulo em que a caixa sai do frustum, e some exatamente no "
            "quadro do bater de asa" % (aberta, largura_dos_bounds))


def valida_cabeca_no_eixo_e_na_frente(m):
    """A cabeca esta centrada no eixo e e a peca mais a FRENTE do bicho.

    O cone de visao do servidor e SIMETRICO em torno do olhar
    (`VisionCone.deGraus`, alimentado por `BatScoutEntity.ABERTURA_DA_VISAO`).
    Uma cabeca fora do eixo faz o desenho apontar um lado e a regra medir o
    outro: o jogador que se aproxima pelo lado "cego" do desenho e visto assim
    mesmo, e nao ha erro nenhum para procurar.

    Ser a peca mais a frente importa por outro motivo, e ele e de combate: o
    focinho desenhado e o que promete o alcance da mordida, e o clipe de
    arremetida so cobre a diferenca a partir de onde o focinho ja chega. Se o
    corpo ou a asa passasse na frente da cabeca, a promessa passaria a sair de
    uma peca que nao morde.
    """
    (x0, x1), _, (z_cabeca, _) = geo.volume(m.caixa("head"))
    if x0 != -x1:
        raise geo.ErroDeArte(
            "a cabeca vai de x=%s a x=%s e nao esta centrada no eixo: o cone de visao do servidor "
            "e simetrico em torno do olhar, entao o desenho apontaria um lado e a regra mediria o "
            "outro" % (x0, x1))

    for caixa in m.caixas:
        if caixa.nome == "head":
            continue
        _, _, (z_outra, _) = geo.volume(caixa)
        if z_outra < z_cabeca:
            raise geo.ErroDeArte(
                "a caixa '%s' comeca em z=%s e a cabeca comeca em z=%s: alguma coisa passa na "
                "frente do focinho, e o alcance da mordida passaria a ser prometido por uma peca "
                "que nao morde" % (caixa.nome, z_outra, z_cabeca))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA),
                    hitbox_blocos=HITBOX, bounds=BOUNDS)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_envergadura_contra_a_hitbox,
                          valida_cabeca_no_eixo_e_na_frente))
    print("envergadura aberta: %.1f px (%.2f blocos), hitbox %.1f px"
          % (envergadura_aberta_em_px(MODELO), envergadura_aberta_em_px(MODELO) / 16.0,
             MODELO.hitbox_largura_px))
