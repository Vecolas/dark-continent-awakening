"""Geometria do Mosquito Officer -- a formiga que DRENA (familia quimera, rank OFFICER).

O QUE O CORPO PRECISA CONTAR, e o que acontece se ele nao contar.

Este bicho tem UMA leitura, e ela e a PROBOSCIDE. Tudo que ele faz em jogo --
chegar pelo lado, encostar, e ficar mais forte com o que tira -- so e
interpretavel se o jogador reconhecer, a distancia, que aquilo ali SUGA. Um
oficial alado sem probocide visivel e um mob que cura do nada: o dreno acontece,
a barra de vida dele sobe, e nao ha uma unica peca no desenho que explique de
onde veio.

A segunda coisa que o corpo conta e que ele e FRACO. HP 55 e armadura 2 sao os
menores entre os oficiais, e a razao e de desenho: um oficial que ja nasce forte
nao precisa drenar, e a mecanica inteira vira decoracao. Por isso o torax e fino
(4 px), o abdome e mais fino ainda (3 px) e nao ha uma unica placa de armadura na
silhueta. Engrossar o corpo aqui nao daria erro nenhum -- daria um bicho que
PARECE aguentar pancada e morre em quatro golpes, e o jogador aprenderia a nao
confiar no que ve.

A terceira sao as ASAS. Elas sao a peca mais larga do modelo de proposito: a
entidade usa FlyingMoveControl e chega pelo FLANCO, e a silhueta precisa dizer
"isto voa" antes de estar em cima de quem olha. Asa curta nao da erro: da um
oficial que aparece de lado no ar sem nunca ter avisado que podia.

A HITBOX MANDA NO MODELO. A entidade e sized(0.9F, 1.4F) -- 14.4 x 22.4 px -- e a
bateria da biblioteca reprova se o modelo estourar isso. O piso dos pes fica em
y=0: ela pousa para atacar, e um modelo que flutuasse sobre o proprio piso
deixaria uma sombra que nao encosta em nada.

A PROBOSCIDE PASSA DA PAREDE DA CAIXA DE COLISAO, e isso e deliberado -- e a
mesma decisao do porrete do Cyclops. A caixa de colisao tem 0.45 de meia-espessura
e a ponta da probocide chega a 0.625 blocos a frente do eixo. E ela que promete o
alcance do golpe, e por isso `valida_proboscide_alcanca_a_picada` cobra que a
caixa de dano NAO chegue mais longe do que o desenho.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de AttackHitbox a frente e +Z, porque com yaw 0 o olhar
vanilla aponta para +Z. Misturar as duas ja custou um bug neste repositorio -- a
caixa do golpe ficou ATRAS do mob, que atacava, animava e nao encostava em
ninguem na frente.

Regerar:  python art-source/enemies/mosquito_officer/mosquito_officer_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/mosquito_officer.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "mosquito_officer"
UV_LARGURA, UV_ALTURA = 64, 64

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(0.9F, 1.4F).
HITBOX = (0.9, 1.4)

# --------------------------------------------------------------------------
# OS TRES NUMEROS ABAIXO SAO DO SERVIDOR. Eles estao copiados aqui para serem
# COBRADOS contra o desenho, e cada um tem o metodo de origem escrito ao lado.
# Nenhuma das divergencias que eles pegam levanta excecao em lugar nenhum.

# Alcance, em BLOCOS, que a caixa da picada reivindica a frente.
# MosquitoOfficerTuning.caixaDaPicada(): maxZ = 0.6D.
ALCANCE_DA_PICADA_EM_BLOCOS = 0.6

# Altura, em BLOCOS, do piso e do teto da caixa da picada.
# MosquitoOfficerTuning.caixaDaPicada(): minY = 0.2D, maxY = 1.3D.
PISO_DA_PICADA_EM_BLOCOS = 0.2
TETO_DA_PICADA_EM_BLOCOS = 1.3

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# `proboscis` pendura na CABECA, e nao no torax. Pendurada no torax o pai existe,
# o portao Java passa, e a probocide fica apontando para frente enquanto a cabeca
# se inclina para picar -- o unico quadro que o jogador tem para ler o golpe sai
# com a agulha fora do eixo da cara.
#
# As asas penduram no TORAX e nao no abdome, porque e o torax que carrega o
# musculo de voo em qualquer inseto e, o que importa aqui, porque o abdome INCHA
# no clipe de dreno: asa pendurada nele cresceria junto, e o bicho ganharia
# envergadura ao sugar sangue sem que nada acusasse.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("thorax", "root", (0, 12, 0)),
    geo.Osso("abdomen", "thorax", (0, 12, 2)),
    geo.Osso("head", "thorax", (0, 18, -1)),
    geo.Osso("proboscis", "head", (0, 19, -3)),
    geo.Osso("wing_left", "thorax", (2, 17, -1)),
    geo.Osso("wing_right", "thorax", (-2, 17, -1)),
    geo.Osso("arm_left", "thorax", (2, 16, -2)),
    geo.Osso("arm_right", "thorax", (-2, 16, -2)),
    geo.Osso("leg_left", "thorax", (1, 12, 1)),
    geo.Osso("leg_right", "thorax", (-1, 12, 1)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
#
# A folha e 64x64 e cabe com folga: o bicho e magro, e magro custa pouco atlas.
# Subir para 128x128 aqui so daria area vazia -- e area vazia numa folha e um
# convite a alguem pintar detalhe onde nenhuma face le.
CAIXAS = (
    # o torax: 4 px de largura. E a peca mais grossa do bicho, e ainda assim ela
    # e mais fina que a cabeca de um zumbi. E isso que diz "corpo fraco".
    geo.Caixa("thorax", "thorax", 0, 0, -2, 12, -3, 4, 6, 6),
    # o abdome: 3 px, mais fino que o torax, e pendurado ATRAS e ABAIXO. Ele e o
    # deposito -- e o clipe de dreno incha exatamente esta caixa.
    geo.Caixa("abdomen", "abdomen", 20, 0, -1.5, 7, 0, 3, 6, 4),
    # a cabeca: pequena, porque a leitura deste bicho nao esta na cara. Grande,
    # ela roubaria o contraste da probocide, que e a unica coisa que precisa ser
    # lida a vinte blocos.
    geo.Caixa("head", "head", 34, 0, -2, 18, -3, 4, 4, 4),
    # A PROBOSCIDE. 7 px de agulha, 1 px de secao, centrada no eixo por origem
    # fracionaria (-0.5). Secao 1x1 e deliberada: engrossa-la para 2 px a faria
    # ler como chifre, e chifre e outra promessa -- chifre empala, probocide suga.
    geo.Caixa("proboscis", "proboscis", 34, 8, -0.5, 17.5, -10, 1, 1, 7),
    # as asas: a peca mais LARGA do modelo, 5 px para cada lado. Elas encostam na
    # parede da hitbox de proposito -- ver valida_asas_sao_a_silhueta.
    geo.Caixa("wing_left", "wing_left", 0, 16, 2, 16, -2, 5, 1, 5),
    geo.Caixa("wing_right", "wing_right", 0, 22, -7, 16, -2, 5, 1, 5),
    # os bracos: 1 px. Eles nao sao arma nenhuma -- o golpe sai da probocide --
    # e existem para a silhueta ter articulacao de formiga e nao de mosca.
    geo.Caixa("arm_left", "arm_left", 20, 16, 2, 10, -3, 1, 6, 1),
    geo.Caixa("arm_right", "arm_right", 24, 16, -3, 10, -3, 1, 6, 1),
    # as pernas: longas e finas, do chao ate o torax. Elas sao o que faz a pose
    # de pouso ler como pouso, e nao como um bicho enfiado no chao.
    geo.Caixa("leg_left", "leg_left", 0, 28, 1, 0, 1, 2, 12, 2),
    geo.Caixa("leg_right", "leg_right", 8, 28, -3, 0, 1, 2, 12, 2),
)


# --------------------------------------------------- validacoes DO BICHO
# As duas abaixo ligam o DESENHO a REGRA DO SERVIDOR. Nenhuma das divergencias
# que elas pegam levanta excecao em lugar nenhum.

def valida_proboscide_alcanca_e_le_a_picada(m):
    """A probocide DESENHADA tem de cobrir a caixa de dano, e tem de ser LEGIVEL.

    Sao tres cobrancas, e as tres nascem do mesmo acordo: a caixa da picada e
    invisivel, e a probocide e a unica coisa que o jogador tem para estimar onde
    e quando ela vale.

      * EM Z -- a ponta da agulha tem de chegar tao longe quanto `maxZ`. Se a
        caixa reivindicar mais do que o desenho, o jogador leva picada de uma
        agulha que, na tela, parou meio bloco antes dele. Nao da erro: da um mob
        com alcance invisivel, e essa e a reclamacao mais dificil de diagnosticar
        que um bicho corpo-a-corpo consegue gerar;

      * EM Y -- a agulha tem de estar DENTRO da faixa de altura que a caixa
        cobre. Desenhada acima do teto ou abaixo do piso, ela aponta para uma
        altura em que a picada nunca acontece: o bicho encosta, a animacao fecha,
        e nada acerta. Fase certa, cooldown certo, log limpo.

      * NA LEITURA -- a agulha tem de estar no eixo e se destacar da cara por uma
        margem. O dreno deste oficial e INVISIVEL: a vida dela sobe e nada na tela
        explica por que. A unica explicacao que o jogador recebe e a silhueta.
        Fora do eixo, a agulha le como perna ou antena; empatada com a cara, ela
        some de perfil raso -- que e exatamente o angulo em que ela chega, porque
        a entidade ataca vindo pelo flanco.

    A folga de Z e permitida numa direcao so. Probocide MAIS longa que a caixa e
    aceitavel -- quem manda no dano e o servidor, e a agulha sobrando le como
    agulha. O contrario e que mente.
    """
    (x0, x1), (agulha_y0, agulha_y1), (agulha_z0, _) = geo.volume(m.caixa("proboscis"))
    alcance_px = -agulha_z0
    exigido_px = ALCANCE_DA_PICADA_EM_BLOCOS * 16.0
    if alcance_px < exigido_px:
        raise geo.ErroDeArte(
            "a probocide alcanca %.1f px (%.3f blocos) a frente do eixo e a caixa da picada vai "
            "ate %.1f px (%.2f blocos): o jogador apanha de uma agulha que parou antes dele, e "
            "nada acusa"
            % (alcance_px, alcance_px / 16.0, exigido_px, ALCANCE_DA_PICADA_EM_BLOCOS))

    piso_px = PISO_DA_PICADA_EM_BLOCOS * 16.0
    teto_px = TETO_DA_PICADA_EM_BLOCOS * 16.0
    if agulha_y0 < piso_px or agulha_y1 > teto_px:
        raise geo.ErroDeArte(
            "a probocide vai de y=%.1f a y=%.1f px e a caixa da picada so cobre de %.1f a %.1f px "
            "(MosquitoOfficerTuning.caixaDaPicada minY/maxY): a agulha aponta para uma altura em "
            "que o golpe nao existe, e o bicho encosta sem acertar nada"
            % (agulha_y0, agulha_y1, piso_px, teto_px))

    if x0 != -x1:
        raise geo.ErroDeArte(
            "a probocide vai de x=%s a x=%s e nao esta centrada no eixo: fora do eixo ela le como "
            "perna ou antena, e o dreno passa a nao ter explicacao nenhuma na tela" % (x0, x1))

    # Margem minima, em px, entre a ponta da agulha e a peca seguinte do corpo.
    # LIMITE DE LEITURA, e nao botao de balanceamento: abaixo disso a agulha some
    # de perfil raso, e perfil raso e como ela chega.
    margem_minima = 4.0
    for caixa in m.caixas:
        if caixa.nome == "proboscis":
            continue
        _, _, (z0, _) = geo.volume(caixa)
        if z0 - agulha_z0 < margem_minima:
            raise geo.ErroDeArte(
                "a caixa '%s' comeca em z=%s e a probocide em z=%s, com menos de %.0f px de "
                "margem: a agulha deixa de se destacar de perfil raso, que e o angulo em que ela "
                "chega -- o oficial voa pelo flanco" % (caixa.nome, z0, agulha_z0, margem_minima))


def valida_asas_sao_a_silhueta(m):
    """UMA asa tem de ser mais larga que qualquer peca do corpo, e o par tem de caber.

    As duas metades sao a mesma decisao vista dos dois lados.

    A entidade voa (FlyingMoveControl) e chega pelo FLANCO. Quem decide se a
    silhueta de longe diz "isto voa" nao e a envergadura ponta a ponta -- essa
    mede o vao entre as duas asas e continua grande mesmo com dois cotocos
    colados no torax. O que se le a vinte blocos e a PECA: uma asa mais estreita
    que o proprio corpo desaparece contra ele, a silhueta vira a de um bicho
    terrestre, e o jogador so descobre que o oficial voa quando ele ja chegou.

    E o par nao pode estourar a caixa de colisao: a asa entraria no bloco vizinho,
    e isso so aparece para quem encurrala o bicho contra uma parede.
    """
    asas = tuple(c for c in m.caixas if c.nome.startswith("wing_"))
    corpo = tuple(c for c in m.caixas if not c.nome.startswith("wing_"))
    if not asas:
        raise geo.ErroDeArte(
            "o modelo nao tem nenhuma caixa 'wing_*': o molde garante o trait WINGS e a entidade "
            "usa navegacao de voo, entao o dado existiria e o corpo o desmentiria -- sem erro")
    mais_larga_do_corpo = max((geo.volume(c)[0][1] - geo.volume(c)[0][0], c.nome) for c in corpo)
    for asa in asas:
        (x0, x1), _, _ = geo.volume(asa)
        if (x1 - x0) <= mais_larga_do_corpo[0]:
            raise geo.ErroDeArte(
                "a asa '%s' tem %s px de largura e '%s' tem %s px: a asa nao se destaca do proprio "
                "corpo, a silhueta de longe le como bicho terrestre, e o jogador so descobre que o "
                "oficial voa quando ele ja chegou"
                % (asa.nome, x1 - x0, mais_larga_do_corpo[1], mais_larga_do_corpo[0]))

    (envergadura_x0, _), _, _ = geo.volume(m.caixa("wing_right"))
    (_, envergadura_x1), _, _ = geo.volume(m.caixa("wing_left"))
    envergadura = envergadura_x1 - envergadura_x0
    if envergadura > m.hitbox_largura_px:
        raise geo.ErroDeArte(
            "a envergadura e de %s px e a hitbox tem %.1f px de largura: a asa atravessa a caixa "
            "de colisao e entra no bloco vizinho, e isso so aparece para quem encurrala o bicho "
            "contra uma parede" % (envergadura, m.hitbox_largura_px))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_proboscide_alcanca_e_le_a_picada,
                          valida_asas_sao_a_silhueta))
