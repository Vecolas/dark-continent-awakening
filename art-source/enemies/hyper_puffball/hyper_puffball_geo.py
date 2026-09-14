"""Geometria do Hyper Puffball -- o fungo que NAO anda.

O QUE O CORPO PRECISA CONTAR. A ficha do bicho e velocidade 0.0 e dano 0: ele
nunca persegue e nunca bate. Toda a ameaca dele esta em UM evento -- o estouro de
esporos de quem o machucou de perto. Entao a silhueta tem de dizer duas coisas de
longe, antes de qualquer texto de tutorial:

  1. "isto nao vem atras de voce" -- nao ha perna, pata nem casco; o volume se
     apoia num talo curto e largo, que le como coisa PRESA no chao;
  2. "isto esta cheio" -- um saco inflado, com uma cupula e um poro no topo, que
     e por onde a nuvem sai quando ele abre.

UMA CAIXA SO NAO BASTA. Um cubo unico de 16 px le exatamente como um bloco a
distancia, e bloco nao e bicho: o jogador passa por cima sem registrar que ali ha
uma criatura, e a primeira vez que ele descobre e levando o estouro. Sao quatro
pecas -- talo, bulbo, cupula, poro -- e a escada de larguras (10 -> 16 -> 12 -> 4)
e o que quebra a leitura de cubo.

A HITBOX MANDA NO MODELO. A entidade e sized(1.2F, 1.2F) -- 19.2 x 19.2 px -- e a
bateria da biblioteca reprova se o modelo estourar isso. O piso do talo fica em
y=0.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO. 16 px = 1 bloco.

Regerar:  python art-source/enemies/hyper_puffball/hyper_puffball_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/hyper_puffball.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "hyper_puffball"
UV_LARGURA, UV_ALTURA = 64, 64

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(1.2F, 1.2F).
HITBOX = (1.2, 1.2)

# --------------------------------------------------------- numeros do SERVIDOR
# Copiados, com a origem ao lado. Eles nao sao decididos aqui -- eles sao
# CONFERIDOS aqui, porque a arte promete ao jogador coisas que so o servidor paga.

# GreedIslandProfiles.hyperPuffball(): new EnemyAttributes(18, 0.0F, 0, 0, 12, 1.0F)
#                                                               ^ movementSpeed
VELOCIDADE_DO_PERFIL = 0.0

# HyperPuffballTuning.RAIO_DO_ESTOURO, em blocos.
RAIO_DO_ESTOURO = 2.5

# Quao longe do CENTRO do fungo fica quem esta encostado nele: meia largura do
# modelo mais o corpo de quem bateu. Um bloco e a medida grosseira de um jogador
# colado. Nao e botao de balanceamento -- e a conta que liga o desenho ao raio.
MARGEM_DE_TOQUE_PX = 16.0

# Nomes que declaram membro de locomocao. Se um deles aparecer na hierarquia, o
# modelo passou a prometer deslocamento.
PALAVRAS_DE_LOCOMOCAO = ("leg", "perna", "pata", "foot", "garra", "casco", "asa")

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: as animacoes escrevem contra estes nomes, e nome errado
# nao da erro -- o GeckoLib so deixa o osso parado.
#
# O talo e osso proprio, e nao parte do bulbo, porque e ele que NAO se mexe: o
# saco incha e balanca em torno da juncao com o talo, e essa juncao precisa
# existir como ponto. Fundido ao bulbo, o inchaco levantaria o bicho inteiro do
# chao -- um fungo flutuando meio pixel, que ninguem sabe descrever e todo mundo
# acha estranho.
#
# A cupula e o poro sao separados porque o estouro e no TOPO: a nuvem sai por
# ali, e os dois precisam poder subir e abrir sem arrastar o saco junto.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("talo", "root", (0, 0, 0)),
    geo.Osso("bulbo", "talo", (0, 2, 0)),
    geo.Osso("cupula", "bulbo", (0, 16, 0)),
    geo.Osso("poro", "cupula", (0, 18, 0)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
CAIXAS = (
    # o talo: curto e mais estreito que o bulbo, para o saco parecer APOIADO e
    # nao plantado. 2 px de altura e o minimo que ainda se ve de longe.
    geo.Caixa("talo", "talo", 0, 44, -5, 0, -5, 10, 2, 10),
    # o bulbo: a peca que o jogador acerta, e a unica que ocupa a hitbox inteira
    # em largura (16 de 19.2 px). Ele e quase cubico de proposito -- um saco de
    # esporos cheio e redondo, e o que tira a leitura de bloco e a escada de
    # larguras das outras tres pecas, nao um bulbo achatado.
    geo.Caixa("bulbo", "bulbo", 0, 0, -8, 2, -8, 16, 14, 16),
    # a cupula: o ombro do fungo. Ela existe para o topo nao ser uma tampa plana
    # de 16 px, que a distancia le como caixote.
    geo.Caixa("cupula", "cupula", 0, 30, -6, 16, -6, 12, 2, 12),
    # o poro: a boca por onde a nuvem sai. Pequeno e no centro, porque e ele que
    # o jogador tem de associar ao estouro depois de ver o primeiro.
    geo.Caixa("poro", "poro", 48, 30, -2, 18, -2, 4, 1, 4),
)


def valida_sem_membro_de_locomocao(m):
    """Velocidade 0.0 no servidor proibe perna no modelo.

    LIGACAO ARTE <-> REGRA. O perfil da MOVEMENT_SPEED 0.0: o fungo nao anda, e
    nao existe goal que o mande andar. Um modelo com perna, pata ou casco promete
    o contrario, e a promessa nao da erro nenhum -- da um jogador esperando o
    bicho se aproximar, lendo a imobilidade como pathfinding quebrado, e indo
    embora sem nunca descobrir que o perigo era chegar perto.

    A regua e o NOME do osso de proposito: quem for desenhar uma perna vai
    chama-la de perna, e e ai que ela morde.
    """
    if VELOCIDADE_DO_PERFIL != 0.0:
        raise geo.ErroDeArte(
            "VELOCIDADE_DO_PERFIL vale %s e este arquivo foi escrito para um bicho parado: se o "
            "servidor passou a mover o hyper puffball, esta regua deixou de valer e o modelo "
            "precisa de membro -- confira GreedIslandProfiles.hyperPuffball()" % VELOCIDADE_DO_PERFIL)
    for o in m.ossos:
        for palavra in PALAVRAS_DE_LOCOMOCAO:
            if palavra in o.nome:
                raise geo.ErroDeArte(
                    "o osso '%s' cita '%s', e a MOVEMENT_SPEED do perfil e %s: o modelo promete um "
                    "deslocamento que o servidor nunca paga, e o jogador le o bicho parado como bug "
                    "e nao como a ficha dele" % (o.nome, palavra, VELOCIDADE_DO_PERFIL))


def valida_estouro_alcanca_quem_encostou(m):
    """O raio do estouro tem de passar da silhueta, com folga para quem encostou.

    LIGACAO ARTE <-> REGRA. O servidor mede o estouro do CENTRO da entidade
    (HyperPuffballTuning.RAIO_DO_ESTOURO); a arte decide de onde esse centro fica
    longe. Um bulbo maior -- ou um raio menor numa sessao de balanceamento --
    produz o pior resultado possivel: o fungo estoura, a nuvem aparece, e quem
    estava colado nele nao leva nada. Nao ha erro, nao ha log, e a leitura que o
    bicho inteiro existe para ensinar some.
    """
    (x0, x1), _, (z0, z1) = m.limites()
    meia_largura = max(x1 - x0, z1 - z0) / 2.0
    raio_px = RAIO_DO_ESTOURO * 16.0
    if raio_px - meia_largura < MARGEM_DE_TOQUE_PX:
        raise geo.ErroDeArte(
            "o modelo tem %.1f px de meia largura e o estouro alcanca %.1f px (raio %.2f blocos): "
            "sobram %.1f px alem da silhueta e quem esta encostado no bicho fica a cerca de %.0f px "
            "do centro. O fungo estouraria sem atingir quem o machucou, e nada acusa isso"
            % (meia_largura, raio_px, RAIO_DO_ESTOURO, raio_px - meia_largura, MARGEM_DE_TOQUE_PX))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_sem_membro_de_locomocao, valida_estouro_alcanca_quem_encostou))
