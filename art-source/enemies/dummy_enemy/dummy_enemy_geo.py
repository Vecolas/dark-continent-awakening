"""Geometria do Boneco de Treino -- o inimigo descartavel da issue #138.

POR QUE ESTE BICHO EXISTE, e por que ele e FEIO DE PROPOSITO.

A issue #138 pede uma entidade descartavel que prove a framework inteira antes
de qualquer mob de conteudo: registra, nasce, anda, percebe, escolhe ataque,
causa dano, cambaleia e morre. O risco de um "dummy" e outro: no dia em que ele
ficar bonito, alguem o promove a conteudo, e o repositorio ganha um mob que
ninguem projetou.

Por isso a silhueta e um BONECO DE TREINO num poste -- saco de estopa, cinta de
couro e uma cruz de madeira. Ele nao se parece com nada do bestiario de Hunter x
Hunter, e essa e a garantia. Quem olhar para ele em jogo sabe na hora que esta
vendo uma ferramenta.

A HITBOX MANDA NO MODELO. A entidade e sized(0.8F, 1.9F) -- 12.8 x 30.4 px --
e a bateria da biblioteca reprova se o modelo estourar isso. O piso da cruz fica
em y=0.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

Regerar:  python art-source/enemies/dummy_enemy/dummy_enemy_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/dummy_enemy.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "dummy_enemy"
UV_LARGURA, UV_ALTURA = 64, 64

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(0.8F, 1.9F).
HITBOX = (0.8, 1.9)

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA pelo contrato de #114: as animacoes escrevem contra estes
# nomes, e nome errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# O poste e osso PROPRIO, e nao parte do corpo, porque e ele que nao se mexe: o
# saco balanca em torno da juncao com o poste, e essa juncao precisa existir como
# ponto. Fundir os dois faria o boneco inteiro girar a partir do chao, que e o
# movimento de uma arvore caindo e nao o de um saco apanhando.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("base", "root", (0, 1, 0)),
    geo.Osso("post", "base", (0, 2, 0)),
    geo.Osso("body", "post", (0, 12, 0)),
    geo.Osso("head", "body", (0, 20, 0)),
    # attack_origin semantico de #114: os bracos sao de onde o golpe sai, e sao
    # eles que a janela ACTIVE gira. Pendurados no poste em vez do corpo, eles
    # ficariam parados enquanto o saco balanca -- o pai existe, o portao Java
    # passa, e o golpe se descola do corpo que o desfere.
    geo.Osso("arm_left", "body", (4, 17, 0)),
    geo.Osso("arm_right", "body", (-4, 17, 0)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
CAIXAS = (
    # cruz de madeira: larga para o boneco nao tombar, e baixa para nao competir
    # com o saco na silhueta
    geo.Caixa("base", "base", 0, 0, -5, 0, -5, 10, 2, 10),
    geo.Caixa("post", "post", 40, 0, -2, 2, -2, 4, 10, 4),
    # o saco: a unica parte que o jogador aprende a acertar
    geo.Caixa("body", "body", 0, 14, -4, 12, -2, 8, 8, 4),
    geo.Caixa("head", "head", 24, 14, -3, 20, -3, 6, 6, 6),
    # tocos de braco: eles existem para o golpe ter de onde sair e para a
    # animacao de windup ter o que erguer
    geo.Caixa("arm_left", "arm_left", 0, 28, 4, 16, -1, 2, 2, 2),
    geo.Caixa("arm_right", "arm_right", 10, 28, -6, 16, -1, 2, 2, 2),
)


def valida_bracos_dentro_da_caixa(m):
    """Os bracos sao o que chega mais perto da parede da hitbox.

    A largura util e 12.8 px, ou seja 6.4 para cada lado, e os tocos terminam em
    6. A folga e de 0.4 px DE PROPOSITO: ela e o que garante que o boneco nao
    atravesse visualmente a propria caixa de colisao quando o windup abre os
    bracos. Alargar o toco em 1 px nao da erro nenhum -- da um braco que entra na
    parede, e isso so aparece para quem encosta o boneco num bloco.
    """
    for nome in ("arm_left", "arm_right"):
        c = m.caixa(nome)
        (x0, x1), _, _ = geo.volume(c)
        limite = m.hitbox_largura_px / 2.0
        if abs(x0) > limite or abs(x1) > limite:
            raise geo.ErroDeArte(
                "o braco '%s' vai de x=%s a x=%s e a meia-largura da hitbox e %.1f px: o toco "
                "atravessa a caixa de colisao, e o sintoma e um braco entrando no bloco vizinho"
                % (nome, x0, x1, limite))


def valida_saco_acima_do_poste(m):
    """O saco tem de comecar ACIMA do topo do poste.

    Se o corpo descer ate dentro do poste, as duas caixas ocupam o mesmo espaco e
    o balanco do saco passa a mostrar madeira por dentro da estopa. Nao e
    z-fighting (os planos nao coincidem), entao o portao de faces coplanares nao
    pisca -- so aparece na tela, e so quando o clipe de idle esta no extremo.
    """
    _, (_, topo_do_poste), _ = geo.volume(m.caixa("post"))
    _, (pe_do_saco, _), _ = geo.volume(m.caixa("body"))
    if pe_do_saco < topo_do_poste:
        raise geo.ErroDeArte(
            "o saco comeca em y=%s e o poste termina em y=%s: o balanco vai mostrar madeira "
            "por dentro da estopa, e nenhum portao de geometria ve isso"
            % (pe_do_saco, topo_do_poste))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_bracos_dentro_da_caixa, valida_saco_acima_do_poste))
