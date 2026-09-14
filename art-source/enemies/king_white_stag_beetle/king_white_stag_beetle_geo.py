"""Geometria do King White Stag Beetle -- o besouro cuja carapaca NAO paga.

O QUE O CORPO PRECISA CONTAR, e o que acontece se ele nao contar.

Este bicho tem armadura 9 e HP 90 numa caixa de 1.6 x 1.4 bloco: ele e LARGO e
BAIXO, e nao alto. A silhueta e o primeiro aviso que o jogador recebe, e ela
precisa dizer "casca", nao "bicho". Um besouro alto e estreito leria como aranha
ou escaravelho de brinquedo, e o jogador atacaria de frente esperando que
funcionasse -- que e exatamente o que este encontro existe para negar.

A segunda coisa que o corpo conta e O VENTRE, e essa e a licao inteira. O
servidor so paga multiplicador em duas posturas: empinado (o aviso da investida)
e DE COSTAS. Na segunda, o modelo gira 180 graus em torno do pivo de `body`, e a
placa do ventre -- que estava colada no chao -- passa a ser a parte ALTA da caixa
de colisao. E por isso que o pivo tem de estar exatamente no meio da altura do
modelo, e e por isso que a placa tem de ficar baixa o bastante para, espelhada,
cair acima do limiar que KingWhiteStagBeetleTuning cobra. As duas coisas sao
medidas por `valida_ventre_no_alto_quando_virado`, e nenhuma das duas falhas
levanta excecao: pivo fora do meio afunda o bicho no chao quando ele vira, e
placa alta demais faz o ventre desenhado prometer um critico que a regra recusa.

A terceira e A PINCA. Os chifres existem no modelo porque o alcance da investida
precisa ter de onde sair: a caixa de dano do servidor vai ate 1.0 bloco a frente
do centro, e sem uma pinca desenhada que chegue la o jogador apanharia de nada.
`valida_alcance_dos_chifres` cobra exatamente isso.

A HITBOX MANDA NO MODELO. A entidade e sized(1.6F, 1.4F) -- 25.6 x 22.4 px -- e
a bateria da biblioteca reprova se o modelo estourar isso. O piso das pernas fica
em y=0.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de AttackHitbox a frente e +Z, porque com yaw 0 o olhar
vanilla aponta para +Z. Misturar as duas ja custou um bug neste repositorio -- a
caixa do golpe ficou ATRAS do mob, que atacava, animava e nao encostava em
ninguem na frente. Ver o comentario de DummyEnemyEntity.CAIXA_DO_GOLPE.

Regerar:  python art-source/enemies/king_white_stag_beetle/king_white_stag_beetle_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/king_white_stag_beetle.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "king_white_stag_beetle"
UV_LARGURA, UV_ALTURA = 128, 64

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(1.6F, 1.4F).
HITBOX = (1.6, 1.4)

# Altura RELATIVA a partir da qual o servidor chama a regiao de "ventre" com o
# bicho DE COSTAS.
# KingWhiteStagBeetleTuning.ALTURA_MINIMA_DO_VENTRE_DE_COSTAS = 0.54D.
#
# Ele esta aqui para ser COBRADO contra o desenho depois do giro de 180 graus.
# Placa desenhada abaixo desse limiar promete um critico que a regra recusa, e
# isso e pior do que nao ter ventre nenhum, porque ENSINA errado.
ALTURA_MINIMA_DO_VENTRE_DE_COSTAS = 0.54

# Alcance, em BLOCOS, que a caixa da investida reivindica a frente.
# KingWhiteStagBeetleTuning.caixaDaInvestida(): maxZ = 1.0D.
ALCANCE_DA_INVESTIDA_EM_BLOCOS = 1.0

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# `body` e o PIVO DO GIRO, e nao so a raiz do tronco. Tudo pendura nele -- casca,
# ventre, cabeca e as seis pernas -- porque o clipe de costas gira UM osso e o
# bicho inteiro tem de acompanhar. Pendurar as pernas em `root` faria o corpo
# virar e as pernas ficarem plantadas no chao, de pe, embaixo de uma carapaca de
# cabeca para baixo: o pai existe, o portao Java passa, e o que aparece na tela
# e um bicho partido ao meio.
#
# `carapaca` e `ventre` sao ossos PROPRIOS, e nao dois retangulos pintados no
# torax. Tres razoes, e as tres sao mecanicas: (a) so um osso com cubo tem altura
# MEDIVEL, e e a altura espelhada dele que a regua compara com o limiar do
# WeakPointResolver; (b) a casca precisa abrir um pouco no aviso e o ventre
# precisa pulsar de costas, e peca pintada nao se mexe; (c) pintados, os dois
# migrariam de lugar na primeira correcao de textura sem que nenhuma regua
# percebesse.
#
# Os chifres penduram em `head`, e nao no corpo. Pendurados no corpo eles ficariam
# PARADOS enquanto a cabeca desce na investida -- e o fechar da pinca e o unico
# quadro que o jogador tem para ler o golpe.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 9, 0)),
    geo.Osso("carapaca", "body", (0, 11, 1)),
    geo.Osso("ventre", "body", (0, 5, 1)),
    geo.Osso("head", "body", (0, 9, -5)),
    geo.Osso("horn_left", "head", (4, 9, -10)),
    geo.Osso("horn_right", "head", (-4, 9, -10)),
    # As seis pernas em tres pares. Elas existem separadas -- e nao como um bloco
    # so -- porque a marcha de inseto e um TRIPE alternado, e tripe alternado
    # precisa de tres ossos de um lado que andem com tres do outro em contrafase.
    geo.Osso("leg_front_left", "body", (8, 5, -2)),
    geo.Osso("leg_front_right", "body", (-8, 5, -2)),
    geo.Osso("leg_mid_left", "body", (8, 5, 2)),
    geo.Osso("leg_mid_right", "body", (-8, 5, 2)),
    geo.Osso("leg_rear_left", "body", (8, 5, 6)),
    geo.Osso("leg_rear_right", "body", (-8, 5, 6)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
CAIXAS = (
    # torax: o corpo por baixo da casca. Mais estreito que a carapaca em todos os
    # eixos, de proposito -- e a casca que tem de ser a silhueta, e ela so e a
    # silhueta se sobrar borda dela por fora do corpo.
    geo.Caixa("torax", "body", 62, 0, -8, 5, -5, 16, 7, 12),
    # carapaca: a peca mais larga e a mais alta. Ela e o bicho visto de cima e de
    # longe, e e ela que tem de ler como CASCA -- uma unica cupula lisa, sem
    # recorte que a quebre em partes.
    geo.Caixa("carapaca", "carapaca", 0, 0, -9, 11, -4, 18, 7, 12),
    # ventre: a placa baixa e CHATA. Fina (2 px) porque ela e uma superficie e nao
    # um volume: o que importa dela e onde ela cai depois do giro de 180 graus.
    geo.Caixa("ventre", "ventre", 0, 21, -7, 3, -3, 14, 2, 9),
    # cabeca: pequena, para que os chifres dominem a frente. Uma cabeca grande
    # competiria com a pinca justamente no quadro em que a pinca e a informacao.
    geo.Caixa("head", "head", 48, 21, -5, 7, -10, 10, 4, 5),
    # os chifres: 7 px de pinca cada, saindo 17 px a frente do centro do bicho.
    # Esse 17 e o numero que valida_alcance_dos_chifres compara com a caixa de
    # dano do servidor.
    geo.Caixa("horn_left", "horn_left", 80, 21, 3, 7, -17, 3, 3, 7),
    geo.Caixa("horn_right", "horn_right", 102, 21, -6, 7, -17, 3, 3, 7),
    # pernas: finas e curtas, fora da linha do torax. Elas sao o que chega mais
    # perto da parede da hitbox -- ver valida_pernas_dentro_da_caixa.
    geo.Caixa("leg_front_left", "leg_front_left", 0, 34, 8, 0, -4, 3, 5, 3),
    geo.Caixa("leg_front_right", "leg_front_right", 14, 34, -11, 0, -4, 3, 5, 3),
    geo.Caixa("leg_mid_left", "leg_mid_left", 28, 34, 8, 0, 0, 3, 5, 3),
    geo.Caixa("leg_mid_right", "leg_mid_right", 42, 34, -11, 0, 0, 3, 5, 3),
    geo.Caixa("leg_rear_left", "leg_rear_left", 56, 34, 8, 0, 4, 3, 5, 3),
    geo.Caixa("leg_rear_right", "leg_rear_right", 70, 34, -11, 0, 4, 3, 5, 3),
)


# --------------------------------------------------- validacoes DO BICHO
# As duas primeiras ligam o DESENHO a REGRA DO SERVIDOR. Nenhuma das divergencias
# que elas pegam levanta excecao em lugar nenhum.

def valida_ventre_no_alto_quando_virado(m):
    """Espelhado pelo giro de 180 graus, o ventre tem de cair ACIMA do limiar.

    Esta e a regua mais importante deste arquivo, porque ela e a unica coisa que
    liga a arte a mecanica inteira do bicho.

    O servidor mede altura RELATIVA na caixa de colisao e so chama de "ventre"
    o que estiver acima de ALTURA_MINIMA_DO_VENTRE_DE_COSTAS. Com o besouro de pe,
    a placa do ventre esta no CHAO -- ou seja, embaixo do limiar, como tem de
    estar. Com ele virado, o clipe `capsized` gira `body` em 180 graus e a placa
    sobe. Onde ela vai parar depende de UM numero: a altura do pivo de `body`.

    Duas coisas sao cobradas aqui, e as duas sao silenciosas:

      * o pivo tem de estar no MEIO da altura do modelo. Fora do meio, o giro
        de 180 graus desloca o bicho inteiro: para cima ele flutua, para baixo ele
        afunda no chao. Nao ha erro -- ha um besouro virado enterrado ate a
        carapaca, e o jogador batendo no ar por cima dele;
      * a placa ESPELHADA tem de ficar acima do limiar, e a carapaca espelhada
        tem de ficar abaixo. A primeira metade garante que o ventre desenhado
        pague; a segunda garante que sobre regiao comum, senao todo golpe no bicho
        virado seria critico e o ponto fraco deixaria de ser um ponto.
    """
    _, pivo_y, _ = m.osso("body").pivot
    (_, _), (modelo_y0, modelo_y1), _ = m.limites()
    if 2 * pivo_y != modelo_y0 + modelo_y1:
        raise geo.ErroDeArte(
            "o pivo de 'body' esta em y=%s e o modelo vai de y=%s a y=%s: o meio seria %.1f. "
            "Girado 180 graus em torno de um pivo fora do meio, o bicho virado sai do lugar -- "
            "flutua ou afunda no chao -- e nenhum portao ve isso"
            % (pivo_y, modelo_y0, modelo_y1, (modelo_y0 + modelo_y1) / 2.0))

    limite = ALTURA_MINIMA_DO_VENTRE_DE_COSTAS * m.hitbox_altura_px
    _, (ventre_y0, ventre_y1), _ = geo.volume(m.caixa("ventre"))
    virado_y0 = 2 * pivo_y - ventre_y1
    virado_y1 = 2 * pivo_y - ventre_y0
    if virado_y0 < limite:
        raise geo.ErroDeArte(
            "de costas o ventre ocupa y=%s..%s e o servidor so paga acima de %.1f px "
            "(WeakPointResolver %.2f de %.1f px): o desenho promete um critico que a regra "
            "recusa, e nao ha erro nenhum para procurar"
            % (virado_y0, virado_y1, limite, ALTURA_MINIMA_DO_VENTRE_DE_COSTAS,
               m.hitbox_altura_px))

    _, (casca_y0, _), _ = geo.volume(m.caixa("carapaca"))
    casca_virada_y1 = 2 * pivo_y - casca_y0
    if casca_virada_y1 >= limite:
        raise geo.ErroDeArte(
            "de costas a carapaca sobe ate y=%s, no ou acima do limiar de %.1f px: nao sobra "
            "regiao comum embaixo, todo golpe no bicho virado viraria critico, e o ponto fraco "
            "deixa de ser um ponto" % (casca_virada_y1, limite))


def valida_alcance_dos_chifres(m):
    """A pinca DESENHADA tem de alcancar tao longe quanto a caixa da investida.

    A caixa de dano do servidor e local e mede a partir do centro da entidade. A
    ponta do chifre tambem: ela esta a `-z` px do eixo z=0 do modelo. Essa e a
    distancia que o desenho promete.

    Se a caixa reivindicar mais do que isso, o jogador leva 12 de dano de um
    chifre que, na tela, parou antes dele. Nao da erro: da um mob com alcance
    invisivel, que e a reclamacao mais dificil de diagnosticar que um bicho
    corpo-a-corpo consegue gerar.

    A regua tambem cobra que os chifres sejam a peca mais a FRENTE. Chifre atras
    da cabeca nao e pinca: e enfeite, e o quadro em que ela fecha -- o unico
    quadro que telegrafa o golpe -- deixa de ser visivel.
    """
    _, _, (ponta_z, _) = geo.volume(m.caixa("horn_left"))
    alcance_px = -ponta_z
    exigido_px = ALCANCE_DA_INVESTIDA_EM_BLOCOS * 16.0
    if alcance_px < exigido_px:
        raise geo.ErroDeArte(
            "a pinca alcanca %.1f px (%.2f blocos) a frente do centro e a caixa da investida vai "
            "ate %.1f px (%.2f blocos): o jogador apanha de um chifre que parou antes dele, e nada "
            "acusa" % (alcance_px, alcance_px / 16.0, exigido_px, ALCANCE_DA_INVESTIDA_EM_BLOCOS))
    _, _, (cabeca_z, _) = geo.volume(m.caixa("head"))
    if ponta_z >= cabeca_z:
        raise geo.ErroDeArte(
            "o chifre comeca em z=%s e a cabeca comeca em z=%s: a pinca fica atras da cara e o "
            "quadro em que ela fecha, que e o unico telegrafo do golpe, some" % (ponta_z, cabeca_z))


def valida_pernas_dentro_da_caixa(m):
    """As pernas sao o que chega mais perto da parede da hitbox.

    A largura util e 25.6 px, ou seja 12.8 para cada lado, e as pernas terminam em
    11. A folga de 1.8 px e maior que a dos outros mobs de proposito: este bicho
    GIRA em torno do eixo z, e o giro leva as pernas para fora na horizontal antes
    de leva-las para cima. Alargar a perna em 2 px nao da erro -- da uma perna que
    entra no bloco vizinho no meio do tombo, e isso so aparece para quem virou o
    besouro encostado numa parede.
    """
    limite = m.hitbox_largura_px / 2.0
    for c in m.caixas:
        if not c.nome.startswith("leg_"):
            continue
        (x0, x1), _, _ = geo.volume(c)
        if abs(x0) > limite or abs(x1) > limite:
            raise geo.ErroDeArte(
                "a perna '%s' vai de x=%s a x=%s e a meia-largura da hitbox e %.1f px: o membro "
                "atravessa a caixa de colisao, e o sintoma e uma perna dentro do bloco vizinho"
                % (c.nome, x0, x1, limite))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_ventre_no_alto_quando_virado,
                          valida_alcance_dos_chifres,
                          valida_pernas_dentro_da_caixa))
