"""Geometria do Crab Heavy -- a formiga quimera que SEGURA a linha (rank PEON).

O QUE O CORPO PRECISA CONTAR, E O QUE ACONTECE SE ELE NAO CONTAR.

A ficha deste bicho e armadura 8 com uma condicao: a placa vale DE FRENTE. Quem
bate na carapaca paga o preco inteiro; quem chega por tras acerta o ventre e paga
quase nada. Isso esta escrito em `RegrasDeCarapacaOrientada`, roda no servidor e
nao aparece em lugar nenhum da tela -- a nao ser aqui.

Entao o corpo tem duas obrigacoes, e as duas sao mecanicas e nao estilo:

  1. A CARAPACA DOMINA A SILHUETA. Ela e a peca mais larga, a mais alta e a que
     cobre a frente inteira. Um caranguejo cuja carapaca fosse so mais uma caixa
     entre outras nao ensinaria nada: o jogador bateria de frente para sempre,
     levaria 9 de dano por pincada e nunca entenderia por que o bicho "nao
     morre". Nao ha erro nisso -- ha um peon gordo.

  2. O VENTRE SOBRA POR TRAS. A carapaca termina ANTES do abdomen, e os ultimos
     pixels do bicho, vistos de costas, sao ventre. E isso que o jogador procura
     quando circula. Um ventre escondido embaixo da carapaca deixaria a regra do
     servidor perfeitamente funcional e perfeitamente invisivel, que e a pior
     combinacao possivel: a resposta existe e ninguem consegue descobrir qual e.

A HITBOX MANDA NO MODELO. A entidade e `.sized(1.4F, 1.6F)` -- 22.4 x 25.6 px --
e a bateria da biblioteca reprova se o modelo estourar isso. O modelo parado
ocupa 22 x 24 x 22 px: ele PREENCHE a caixa de colisao de proposito. Sobrar
muito espaco dentro da hitbox nao da erro nenhum; da flecha que acerta o ar ao
lado do bicho e conta como acerto, e o jogador aprende que a mira dele mente.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de `AttackHitbox.noMundo` a frente e +Z, porque com yaw 0
o olhar vanilla aponta para +Z. Misturar as duas ja custou um bug neste
repositorio -- a caixa do golpe ficou ATRAS do mob, que atacava, animava e nao
encostava em ninguem na frente.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

Regerar:  python art-source/enemies/crab_heavy/crab_heavy_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/crab_heavy.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "crab_heavy"
UV_LARGURA, UV_ALTURA = 128, 64

# O literal da chamada Java, copiado como esta:
# EnemyEntityTypes.CRAB_HEAVY ... .sized(1.4F, 1.6F).
HITBOX = (1.4, 1.6)

# Quantos pixels de ventre precisam sobrar ATRAS da carapaca.
#
# Ele esta aqui para ser COBRADO contra o desenho, e o numero nao e de gosto: tres
# pixels e o menor degrau que ainda se enxerga na silhueta de costas a uns dez
# blocos, que e a distancia em que o jogador decide se circula. Menos que isso e
# uma borda que some no contorno e o abdomen vira "parte da concha".
#
# Ele nao e botao de balanceamento: girar este numero nao muda dano nenhum. E um
# limite de LEITURA, e mora ao lado do comentario que o explica.
EXPOSICAO_MINIMA_DO_VENTRE_PX = 3

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# `carapaca` e `ventre` sao ossos SEPARADOS, e nao duas regioes pintadas na mesma
# caixa. Tres razoes, e as tres sao mecanicas: (a) so um osso com cubo tem
# posicao MEDIVEL, e e a posicao dele que as reguas deste arquivo comparam com a
# regra do servidor; (b) a carapaca precisa reagir sozinha -- afundar no
# cambaleio, erguer no aviso -- e regiao pintada nao reage; (c) uma divisao so de
# textura migraria de lugar na primeira correcao de folha sem que nenhuma regua
# percebesse.
#
# `claw_left_tip` pendura em `claw_left`, e nao no corpo. Pendurado no corpo o pai
# existe, o portao Java passa, e a ponta da pinca fica PARADA no ar enquanto o
# braco avanca -- que e o unico quadro em que o jogador ve o agarrao comecar.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 7, -1)),
    geo.Osso("carapaca", "body", (0, 15, 0)),
    geo.Osso("ventre", "body", (0, 9, 3)),
    geo.Osso("head", "body", (0, 12, -6)),
    geo.Osso("claw_left", "body", (7, 11, -4)),
    geo.Osso("claw_left_tip", "claw_left", (7.5, 10.5, -8)),
    geo.Osso("claw_right", "body", (-7, 11, -4)),
    geo.Osso("claw_right_tip", "claw_right", (-7.5, 10.5, -8)),
    geo.Osso("leg_front_left", "body", (8, 7, -3.5)),
    geo.Osso("leg_mid_left", "body", (8, 7, 0.5)),
    geo.Osso("leg_back_left", "body", (8, 7, 3)),
    geo.Osso("leg_front_right", "body", (-8, 7, -3.5)),
    geo.Osso("leg_mid_right", "body", (-8, 7, 0.5)),
    geo.Osso("leg_back_right", "body", (-8, 7, 3)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
#
# A folha e 128x64 porque so a carapaca ja pede 74x23: ela e a maior peca do
# bicho por uma decisao de leitura, e nao por acaso.
CAIXAS = (
    # A CARAPACA. 22 px de largura -- exatamente a hitbox -- e o teto do modelo.
    # Ela e mais larga que o corpo e mais comprida que ele de proposito: e a aba
    # que sobra dos dois lados que le como "placa", e nao como "costas".
    geo.Caixa("carapaca", "carapaca", 0, 0, -11, 16, -7, 22, 8, 15),
    # O corpo, escondido debaixo da carapaca. Ele existe para as pernas e as
    # pincas terem onde pendurar; quase nada dele aparece de cima.
    geo.Caixa("body", "body", 74, 0, -8, 7, -6, 16, 8, 9),
    # O VENTRE. Ele desce 1 px abaixo do corpo e vai 3 px ALEM da carapaca: essa
    # sobra e a unica coisa que o jogador tem para mirar, e ela e cobrada por
    # valida_carapaca_orientada.
    geo.Caixa("ventre", "ventre", 0, 23, -7, 6, 3, 14, 10, 8),
    # As pincas: braco grosso e ponta movel. A ponta e caixa propria porque ela
    # avanca sozinha no golpe -- e o curso dela e o que a regua de alcance do
    # gerador de animacao mede contra a caixa de dano do servidor.
    geo.Caixa("claw_left", "claw_left", 44, 23, 5, 8, -8, 5, 5, 7),
    geo.Caixa("claw_right", "claw_right", 68, 23, -10, 8, -8, 5, 5, 7),
    geo.Caixa("claw_left_tip", "claw_left_tip", 112, 23, 5, 9, -11, 5, 3, 3),
    geo.Caixa("claw_right_tip", "claw_right_tip", 112, 29, -10, 9, -11, 5, 3, 3),
    # A cabeca e PEQUENA e fica embaixo da aba da carapaca. Cabeca grande neste
    # bicho competiria com a carapaca pelo olhar, e a carapaca e a informacao.
    geo.Caixa("head", "head", 92, 23, -3, 9, -10, 6, 5, 4),
    # Seis pernas curtas, 7 px. Curtas de proposito: perna alta levantaria o
    # ventre para longe do chao e daria ao jogador um alvo por BAIXO, que e um
    # angulo que a regra do servidor nao sabe pagar -- o resolver dela mede
    # cosseno horizontal, e nao altura.
    geo.Caixa("leg_front_left", "leg_front_left", 0, 41, 8, 0, -5, 3, 7, 3),
    geo.Caixa("leg_mid_left", "leg_mid_left", 12, 41, 8, 0, -1, 3, 7, 3),
    geo.Caixa("leg_back_left", "leg_back_left", 24, 41, 8, 0, 2, 3, 7, 3),
    geo.Caixa("leg_front_right", "leg_front_right", 36, 41, -11, 0, -5, 3, 7, 3),
    geo.Caixa("leg_mid_right", "leg_mid_right", 48, 41, -11, 0, -1, 3, 7, 3),
    geo.Caixa("leg_back_right", "leg_back_right", 60, 41, -11, 0, 2, 3, 7, 3),
)

# A caixa de visibilidade e DECLARADA, e nao deduzida do modelo parado.
#
# O padrao da biblioteca mede a pose parada. A pinca deste bicho avanca 10 px no
# golpe e a ponta derivada vai junto: medida no parado, a caixa ficaria apertada
# exatamente no quadro em que o membro sai mais do corpo. Isso nao da erro -- faz
# o mob SUMIR da tela quando a camera chega no angulo em que a caixa deixa o
# frustum, e some no quadro do telegrafo, que e o unico que precisava ser visto.
BOUNDS = (3.0, 2.5, [0, 1.25, 0])


# --------------------------------------------------- validacao DO BICHO
# Ela liga o DESENHO a REGRA DO SERVIDOR. Nenhuma das divergencias que ela pega
# levanta excecao em lugar nenhum.

def valida_carapaca_orientada(m):
    """A carapaca domina a frente, e o ventre sobra atras dela.

    As duas metades desta regua sao as duas metades de
    `RegrasDeCarapacaOrientada`: a placa cheia de frente e a placa quase nula
    pelas costas. O servidor ja sabe cobrar a diferenca; o que ele nao consegue
    fazer e CONTAR ao jogador que a diferenca existe.

    Se a carapaca nao for a peca mais larga e mais alta, o bicho le como um
    caranguejo generico e o jogador nunca forma a hipotese "a frente e dura".
    Se o ventre nao sobrar por tras, ele forma a hipotese e nao acha onde
    aplica-la. Os dois casos rodam, compilam, spawnam, dropam loot e passam em
    todos os portoes; os dois transformam o encontro num saco de pancada com
    armadura 8.
    """
    carapaca = m.caixa("carapaca")
    (cx0, cx1), (_, cy1), (_, cz1) = geo.volume(carapaca)

    (mx0, mx1), (_, my1), _ = m.limites()
    if (cx1 - cx0) < (mx1 - mx0):
        raise geo.ErroDeArte(
            "a carapaca tem %s px de largura e o bicho inteiro tem %s px: a placa nao e a peca "
            "que define a silhueta, e o jogador nao tem por que supor que a frente e dura. Ele "
            "bate de frente para sempre, leva 9 por pincada e nao descobre a resposta"
            % (cx1 - cx0, mx1 - mx0))
    if cy1 < my1:
        raise geo.ErroDeArte(
            "o topo da carapaca esta em y=%s e o topo do modelo em y=%s: alguma peca passa por "
            "cima da placa e rouba a leitura de 'bicho encouracado' que este mob inteiro depende "
            "de entregar" % (cy1, my1))

    _, _, (_, vz1) = geo.volume(m.caixa("ventre"))
    sobra = vz1 - cz1
    if sobra < EXPOSICAO_MINIMA_DO_VENTRE_PX:
        raise geo.ErroDeArte(
            "a carapaca termina em z=%s e o ventre em z=%s: sobram %s px de ventre por tras e o "
            "minimo legivel e %s px. O servidor continua cobrando quase nada de quem acerta pelas "
            "costas, e ninguem descobre isso: a regra fica perfeita e invisivel, que e pior do que "
            "nao existir" % (cz1, vz1, sobra, EXPOSICAO_MINIMA_DO_VENTRE_PX))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA),
                    hitbox_blocos=HITBOX, bounds=BOUNDS)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_carapaca_orientada,))
