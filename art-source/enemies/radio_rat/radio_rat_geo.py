"""Geometria do Radio Rat -- o rato que DENUNCIA.

O QUE ESTE BICHO PRECISA DIZER NUMA SILHUETA DE 9.6 x 8 PIXELS.

Ele nao machuca: ele conta aos outros onde o jogador esta, e a resposta que o
jogo ensina e matar o mensageiro antes do grito sair. Para isso funcionar, o
jogador tem de reconhecer o mensageiro ANTES de ter tempo de ler qualquer outra
coisa -- e com 9.6 x 8 px nao ha detalhe, so contorno. Por isso a silhueta gasta
o orcamento inteiro em duas coisas que se leem de longe:

  * ORELHAS grandes e chapadas, que dobram a largura da cabeca;
  * uma ANTENA com travessa no topo, que e a peca mais alta do bicho.

A antena e a peca mais alta DE PROPOSITO, e isso e cobrado por
`valida_antena_e_o_ponto_mais_alto`: o telegrafo do grito (20 ticks de windup, em
RadioRatTuning) e gasto levantando a antena, e uma antena escondida atras das
orelhas transformaria esses 20 ticks num aviso que ninguem ve. Nao da erro
nenhum: o servidor continua avisando, o jogador continua sem ler.

A HITBOX MANDA NO MODELO. A entidade e sized(0.6F, 0.5F) -- 9.6 x 8 px -- e e a
menor deste bestiario. A bateria da biblioteca reprova se o modelo estourar
isso, e aqui a folga vertical e ZERO: a travessa da antena termina exatamente em
y=8. Um pixel a mais e uma antena que o jogador ve e nao consegue acertar, e o
sintoma e "eu bati e nao pegou".

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO. 16 px = 1 bloco.

Regerar:  python art-source/enemies/radio_rat/radio_rat_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/radio_rat.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "radio_rat"
UV_LARGURA, UV_ALTURA = 32, 32

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(0.6F, 0.5F).
HITBOX = (0.6, 0.5)

# Quanto a travessa da antena tem de subir acima de QUALQUER outra peca.
#
# Nao e botao de balanceamento: e a distancia minima em que um contorno se separa
# de outro a 16 px de distancia da camera. Meio pixel e pouco e e o que cabe --
# o teto e a propria hitbox. Abaixo disso a antena encosta na linha das orelhas e
# o windup deixa de ser legivel de perfil, que e o angulo de onde se mata um rato.
ANTENA_ACIMA_DAS_ORELHAS_PX = 0.5

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: radio_rat.animation.json escreve contra estes nomes, e
# nome errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# A antena e osso proprio, e a travessa e osso FILHO dela. Fundir os dois numa
# peca so faria a barra girar em torno da base do mastro, e o chicote do grito
# (que e o unico quadro em que a travessa se mexe sozinha) sumiria. Pendurar a
# travessa na cabeca em vez do mastro e pior ainda: o pai existe, o portao Java
# passa, e a barra fica parada no ar enquanto o mastro sobe embaixo dela.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 3.5, 0)),
    geo.Osso("head", "body", (0, 3.5, -2)),
    # As orelhas pivotam na juncao com a cabeca, e nao no centro delas: orelha
    # que gira pelo meio abre para dentro do cranio.
    geo.Osso("ear_left", "head", (1.5, 5, -3.5)),
    geo.Osso("ear_right", "head", (-1.5, 5, -3.5)),
    geo.Osso("antenna", "head", (0, 5, -3.5)),
    geo.Osso("antenna_tip", "antenna", (0, 7, -3.5)),
    geo.Osso("leg_front_left", "body", (1.5, 2, -1)),
    geo.Osso("leg_front_right", "body", (-1.5, 2, -1)),
    geo.Osso("leg_back_left", "body", (1.5, 2, 1)),
    geo.Osso("leg_back_right", "body", (-1.5, 2, 1)),
    geo.Osso("tail", "body", (0, 3, 2)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
CAIXAS = (
    # O tronco e baixo e comprido: rato lido de perfil e uma linha horizontal, e
    # e o contraste com a vertical da antena que faz as duas se lerem.
    geo.Caixa("corpo", "body", 0, 0, -2, 2, -2, 4, 3, 4),
    # A cabeca e mais estreita que o corpo de proposito -- e o que sobra de
    # focinho quando nao ha pixel para um focinho separado.
    geo.Caixa("cabeca", "head", 16, 0, -1.5, 2, -5, 3, 3, 3),
    # As orelhas sao CHAPAS de 1 px: elas existem para a silhueta, nao para o
    # volume. Engrossa-las para 2 px consome 2 px da largura util e o bicho
    # comeca a encostar na parede da propria caixa de colisao.
    geo.Caixa("orelha_esquerda", "ear_left", 0, 8, 1.5, 4.5, -5, 1, 3, 3),
    geo.Caixa("orelha_direita", "ear_right", 8, 8, -2.5, 4.5, -5, 1, 3, 3),
    # O mastro. Fino porque a leitura vem da travessa, nao dele.
    geo.Caixa("antena", "antenna", 16, 8, -0.5, 5, -4, 1, 2, 1),
    # A travessa: a peca mais alta do bicho, e a unica em cor de alerta na folha.
    geo.Caixa("antena_travessa", "antenna_tip", 20, 8, -1.5, 7, -4, 3, 1, 1),
    geo.Caixa("pata_frente_esquerda", "leg_front_left", 0, 16, 1, 0, -1.5, 1, 2, 1),
    geo.Caixa("pata_frente_direita", "leg_front_right", 4, 16, -2, 0, -1.5, 1, 2, 1),
    geo.Caixa("pata_tras_esquerda", "leg_back_left", 8, 16, 1, 0, 0.5, 1, 2, 1),
    geo.Caixa("pata_tras_direita", "leg_back_right", 12, 16, -2, 0, 0.5, 1, 2, 1),
    geo.Caixa("cauda", "tail", 16, 16, -0.5, 2.5, 2, 1, 1, 2),
)


def valida_antena_e_o_ponto_mais_alto(m):
    """A LIGACAO ENTRE A ARTE E A REGRA: o telegrafo do grito precisa ser visto.

    O servidor gasta 20 ticks de windup antes de o relatorio sair
    (RadioRatTuning.GRITO_WINDUP_TICKS), e esses 20 ticks existem para uma coisa
    so: dar ao jogador a janela de matar o mensageiro antes do grito. O clipe de
    windup gasta a janela LEVANTANDO A ANTENA -- e se a antena nao for a peca
    mais alta do bicho, ela sobe atras da linha das orelhas e o aviso acontece
    fora da silhueta.

    Nada acusa isso. O servidor continua com a janela certa, o clipe continua
    tocando, o dano continua saindo na hora. O que se perde e a unica coisa que o
    jogador tinha para ler, e o relato que chega e "esse bicho grita do nada".
    """
    _, (_, topo_da_antena), _ = geo.volume(m.caixa("antena_travessa"))
    for c in m.caixas:
        if c.nome == "antena_travessa":
            continue
        _, (_, topo), _ = geo.volume(c)
        if topo_da_antena - topo < ANTENA_ACIMA_DAS_ORELHAS_PX:
            raise geo.ErroDeArte(
                "a travessa da antena termina em y=%s e '%s' chega a y=%s, ou seja %s px de "
                "diferenca, e o minimo e %s px: o windup levanta a antena atras da silhueta de "
                "'%s', o aviso de 20 ticks acontece onde nao se ve, e nenhum portao acusa isso"
                % (topo_da_antena, c.nome, topo, topo_da_antena - topo,
                   ANTENA_ACIMA_DAS_ORELHAS_PX, c.nome))


def valida_travessa_pendura_no_mastro(m):
    """A travessa pendura na ANTENA, nunca na cabeca.

    Pendurada na cabeca o pai existe, o portao Java passa e o `.geo.json` e
    valido: a barra simplesmente fica parada no ar enquanto o mastro sobe embaixo
    dela, e o grito perde o unico quadro em que a antena chicoteia.
    """
    geo.valida_encadeamento(
        m.ossos, (("antenna_tip", "antenna"),),
        "a travessa tem de acompanhar o mastro; pendurada na cabeca ela fica"
        " parada enquanto o mastro sobe, e o chicote do grito deixa de existir")


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_travessa_pendura_no_mastro,
                          valida_antena_e_o_ponto_mais_alto))
