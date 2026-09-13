"""Gera as DUAS geometrias autorais do Kiriko (bedrock 1.12.0, GeckoLib 4).

POR QUE ESTE ARQUIVO EXISTE. Nao ha Blockbench no repositorio: a FONTE VERSIONADA
de um modelo e este Python. JSON gerado sem gerador no git vira um arquivo que
ninguem consegue corrigir depois -- e este mob tem DOIS.

ESTE MODULO E A UNICA TABELA DE CAIXAS -- das duas formas. O gerador da textura
importa CAIXAS_DISFARCE e CAIXAS_VERDADEIRO daqui de proposito: duas tabelas
separadas divergem na primeira correcao de modelo, e a divergencia nao da erro --
da face pintada no lugar errado, que so aparece na tela.

SAO DOIS MODELOS COM IDS PROPRIOS, como no man-faced ape, e nao um modelo com
metade dos ossos escondida. Osso invisivel continua carregado, continua animado e
continua ocupando atlas; e o dia em que alguem esquecer de esconde-lo, o viajante
aparece com bico por baixo do chapeu sem nada acusar. Dois ids se encaixam no
portao generico (CoerenciaDeGeckoLibTest) sem mudanca nenhuma: ele exige um
arquivo de animacao e uma pasta de textura por modelo, e duas formas sao dois de
cada.

O QUE ESTE MOB E, E POR QUE ISSO MANDA NA GEOMETRIA
---------------------------------------------------
O Kiriko nao ataca: ele AVALIA. Aparece em forma humana, observa como o jogador se
comporta e decide. Quem saca arma e reprovado -- ele se revela e parte para cima.
Quem espera e aprovado -- ele se revela, deixa a recompensa e vai embora. O
jogador vence NAO LUTANDO.

Isso inverte a regra que os seis irmaos deste repositorio seguem, e inverte junto
o criterio de desenho. Duas consequencias, e cada uma tem regua aqui:

(a) O DISFARCE NAO PODE TER PISTA. No man-faced ape as maos sao grandes demais de
    proposito: la o disfarce e caca, e caca justa precisa de contrapartida. Aqui
    nao. O Kiriko nao esta enganando para atacar -- esta observando, e quem entrega
    o disfarce e a TRANSFORMACAO, no momento em que ele decide mostra-la.
    `valida_disfarce_sem_pista` reprova qualquer pedaco de bicho que escape do
    contorno de roupa: mao maior que a manga, cubo com nome de bicho, ou qualquer
    volume que passe da aba do chapeu. Uma pista aqui nao levanta excecao nenhuma
    -- so ensina o jogador a atacar primeiro, que e exatamente o comportamento que
    este mob existe para reprovar.

(b) A FORMA VERDADEIRA E UMA CRIATURA QUE PENSA, e nao um monstro. A ficha diz
    "Magical Beast != monster", e isso tambem e desenho: postura ERETA, cabeca em
    cima dos ombros (e nao empurrada para a frente entre eles, como a do macaco),
    garras que NAO encostam no chao (ele anda em dois pes, nao nos nos), e uma cara
    com espaco para dois olhos que leem de frente. `valida_forma_que_pensa` mede
    cada um desses. Nenhum deles da erro se faltar: o que falta e o jogador olhar
    para o bicho e ver mais um monstro -- e entao ele ataca, e reprova, e a ficha
    inteira foi ignorada sem um unico teste vermelho.

A HITBOX MANDA NOS DOIS MODELOS, E E A MESMA.
---------------------------------------------
A entidade e sized(1.0F, 2.1F) -- 16 x 33.6 px -- e ela NAO muda quando o disfarce
cai. As duas silhuetas cabem nela de proposito: se a caixa mudasse, o jogador
notaria a troca pelo empurrao, pela mira que para de acertar ou pelo bloco em que
o bicho passa a nao caber. Ou seja: notaria ANTES de ver o corpo, e a revelacao
deixaria de ser a revelacao.

`valida_mesma_altura_total` vai alem e exige que as duas formas terminem na MESMA
altura, ao pixel. Mesma hitbox com alturas diferentes ainda entregaria a troca --
o vulto encolhe na tela mesmo com a caixa parada.

O CONTRASTE QUE SOBRA, ja que a altura e igual, e o unico que ainda pode existir:
o verdadeiro e mais LARGO e tem o TRONCO MAIS BAIXO (quadril 3 px abaixo do humano,
pernas curtas e fortes, torso comprido). O humano e estreito e de perna longa.
`valida_contraste_de_silhueta` mede os dois. Sem esse contraste a transformacao
vira troca de textura.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

Regerar:  python art-source/enemies/kiriko/kiriko_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/kiriko.geo.json
          src/main/resources/assets/nenfoundation/geo/entity/kiriko_disfarce.geo.json
"""
from collections import namedtuple
import json
import os
import re

UV_LARGURA, UV_ALTURA = 64, 64

# Limites que a entidade impoe: EnemyEntityTypes .sized(1.0F, 2.1F). A MESMA caixa
# serve as duas formas -- ver o cabecalho.
HITBOX_LARGURA_PX = 1.0 * 16.0
HITBOX_ALTURA_PX = 2.1 * 16.0

# Margens minimas do contraste entre as duas silhuetas. Nao sao botao de tuning:
# sao o piso para a transformacao ser LEGIVEL a distancia, agora que a altura
# total e identica por contrato.
VERDADEIRO_MAIS_LARGO_PX = 2.0
VERDADEIRO_COM_TRONCO_MAIS_BAIXO_PX = 2.0

# Palavras que denunciam peca de bicho. Elas nao podem aparecer em cubo nenhum da
# forma humana -- ver `valida_disfarce_sem_pista`.
PALAVRAS_DE_BICHO = ("beak", "bico", "crest", "crista", "claw", "garra", "talon",
                     "tail", "cauda", "fang", "presa", "feather", "pena", "scale",
                     "escama", "horn", "chifre", "fur", "pelo")

Osso = namedtuple("Osso", "nome pai pivot rotacao")
Caixa = namedtuple("Caixa", "nome osso u v x y z w h d")

# ---------------------------------------------------------------------------
# FORMA HUMANA -- kiriko_disfarce
# ---------------------------------------------------------------------------

# Hierarquia CONGELADA pelo contrato da entrega: as animacoes, o GeoModel e o
# gametest escrevem contra estes nomes. Nome errado aqui nao da erro -- o GeckoLib
# so deixa o osso parado, e isso so aparece na tela.
#
# O pivot de um filho mora na JUNCAO com o pai, e `valida_pivots` cobra isso
# medindo: todo pivot cai DENTRO de um cubo do pai. `hat` gira na COROA da cabeca
# (0, 29, 0), que e onde o chapeu se apoia -- e nao no meio do cranio, senao o
# clipe `transform` afunda o chapeu na cara em vez de joga-lo para cima.
#
# NENHUM osso nasce com `rotation`. E decisao, nao esquecimento: a pose mora nas
# CAIXAS. Com bind rotation, todo valor escrito no .animation.json seria somado a
# um angulo invisivel -- duas fontes para a mesma pose, decididas em silencio.
OSSOS_DISFARCE = (
    Osso("root", None, (0, 0, 0), None),
    Osso("body", "root", (0, 12, 0), None),       # quadril
    Osso("head", "body", (0, 21, 0), None),       # base do pescoco
    Osso("hat", "head", (0, 29, 0), None),        # a coroa: e daqui que o chapeu sai
    Osso("arm_left", "body", (3.5, 21, 0), None),
    Osso("arm_right", "body", (-3.5, 21, 0), None),
    Osso("leg_left", "body", (2, 12, 0), None),
    Osso("leg_right", "body", (-2, 12, 0), None),
)

# A hierarquia que o contrato da entrega fixou, palavra por palavra. A repeticao e
# deliberada e TEM dono: `valida_contrato` compara as duas. Renomear um osso la em
# cima e mecanico; renomear e o contrato continuar batendo so acontece se alguem
# mudar os dois -- que e exatamente a conversa que precisa acontecer antes de uma
# animacao parar de tocar em silencio.
CONTRATO_DISFARCE = (
    ("root", None),
    ("body", "root"),
    ("head", "body"),
    ("hat", "head"),
    ("arm_left", "body"),
    ("arm_right", "body"),
    ("leg_left", "body"),
    ("leg_right", "body"),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) px a partir de (u,v) -- as validacoes
# abaixo reprovam sobreposicao e estouro do atlas.
#
# Todo tamanho e INTEIRO de proposito. O portao Java ARREDONDA `size` para calcular
# o retangulo do atlas; um size fracionario faria o portao medir um retangulo e
# este gerador medir outro, e as duas contas so discordariam no dia em que uma
# delas achasse sobreposicao.
CAIXAS_DISFARCE = (
    # Casaco de estrada. 8 px de largura e 4 de profundidade: ombro de gente, sem
    # nada que engrosse. A barra para em y=12, na altura do quadril.
    Caixa("body", "body", 32, 0, -4, 12, -2, 8, 9, 4),
    # Cabeca de gente, 8x8x8, como a de qualquer humanoide do jogo. E aqui esta a
    # diferenca com o macaco: o ROSTO FICA A MOSTRA. Este bicho nao esta se
    # escondendo debaixo de um capuz -- esta olhando para o jogador.
    Caixa("head", "head", 0, 0, -4, 21, -4, 8, 8, 8),
    # A ABA do chapeu de viagem: 12 x 12, a peca mais externa do disfarce em TODA
    # direcao. E ela que define o contorno que `valida_disfarce_sem_pista` cobra --
    # nada de corpo pode passar dela.
    Caixa("brim", "hat", 0, 16, -6, 29, -6, 12, 1, 12),
    # A copa, sobre a aba.
    Caixa("crown", "hat", 28, 29, -3.5, 30, -3.5, 7, 3, 7),
    # Mangas ao longo do corpo.
    Caixa("sleeve_left", "arm_left", 0, 45, 4, 13, -1.5, 2, 8, 3),
    Caixa("sleeve_right", "arm_right", 10, 45, -6, 13, -1.5, 2, 8, 3),
    # AS MAOS. Exatamente a secao da manga (2 x 3) -- ver `valida_disfarce_sem_pista`.
    # Mao maior que a manga e a pista do man-faced ape, e aqui ela seria um erro de
    # leitura: ensinaria o jogador a desconfiar, e desconfiar leva a atacar.
    Caixa("hand_left", "arm_left", 48, 16, 4, 10, -1.5, 2, 3, 3),
    Caixa("hand_right", "arm_right", 48, 22, -6, 10, -1.5, 2, 3, 3),
    # Pernas LONGAS (12 px de 33): e a perna que faz a silhueta humana ler como
    # alta e estreita, ja que a altura total e igual a do verdadeiro.
    Caixa("leg_left", "leg_left", 0, 29, 0.5, 0, -2, 3, 12, 4),
    Caixa("leg_right", "leg_right", 14, 29, -3.5, 0, -2, 3, 12, 4),
)

# ---------------------------------------------------------------------------
# FORMA VERDADEIRA -- kiriko
# ---------------------------------------------------------------------------

OSSOS_VERDADEIRO = (
    Osso("root", None, (0, 0, 0), None),
    Osso("body", "root", (0, 9, 0), None),        # quadril, 3 px abaixo do humano
    Osso("head", "body", (0, 22, 0), None),       # pescoco, EM CIMA do torso
    Osso("beak", "head", (0, 26, -3.5), None),    # a frente do cranio
    Osso("crest", "head", (0, 30, 0), None),      # a coroa do cranio
    Osso("arm_left", "body", (4, 21, 0), None),
    Osso("claw_left", "arm_left", (5.5, 8, 0), None),   # o pulso
    Osso("arm_right", "body", (-4, 21, 0), None),
    Osso("claw_right", "arm_right", (-5.5, 8, 0), None),
    Osso("leg_left", "body", (2, 9, 0), None),
    Osso("foot_left", "leg_left", (2, 2, 0), None),     # o tornozelo
    Osso("leg_right", "body", (-2, 9, 0), None),
    Osso("foot_right", "leg_right", (-2, 2, 0), None),
    Osso("tail", "body", (0, 15, 3.5), None),           # a raiz da cauda, nas costas
)

CONTRATO_VERDADEIRO = (
    ("root", None),
    ("body", "root"),
    ("head", "body"),
    ("beak", "head"),
    ("crest", "head"),
    ("arm_left", "body"),
    ("claw_left", "arm_left"),
    ("arm_right", "body"),
    ("claw_right", "arm_right"),
    ("leg_left", "body"),
    ("foot_left", "leg_left"),
    ("leg_right", "body"),
    ("foot_right", "leg_right"),
    ("tail", "body"),
)

CAIXAS_VERDADEIRO = (
    # TRONCO comprido (13 px) e largo (10), com o quadril em y=9 -- 3 px abaixo do
    # quadril humano. E esse rebaixamento, junto com a perna curta, que da o vulto
    # pesado sem mudar a altura total nem um pixel.
    # O torso e 1 px MAIS FUNDO que o cranio de cada lado (z -4..4 contra -3.5..3.5)
    # de proposito: assim a cabeca fica INTEIRA dentro da sombra dos ombros, que e o
    # que `valida_forma_que_pensa` chama de postura ereta. Cranio sobrando para a
    # frente, ainda que meio pixel, ja e a postura do man-faced ape.
    Caixa("torso", "body", 0, 0, -5, 9, -4, 10, 13, 8),
    # O CRANIO EM CIMA DOS OMBROS. Ele entra 1 px no torso (y=21, torso ate 22) para
    # nao abrir costura quando a cabeca gira no clipe `observe`. Cabeca em cima e o
    # que separa esta criatura do man-faced ape, cuja cabeca sai para a FRENTE do
    # peito -- postura de bicho que anda em quatro.
    Caixa("skull", "head", 0, 21, -4, 21, -3.5, 8, 9, 7),
    # O BICO. Sai 3.5 px a frente do cranio e ocupa as 4 colunas centrais da cara,
    # deixando 2 colunas de cada lado para os olhos -- ver `valida_forma_que_pensa`.
    Caixa("beak", "beak", 36, 0, -2, 24, -7, 4, 4, 5),
    # A CRISTA. Lamina fina de 2 px correndo da testa a nuca; e a peca mais alta do
    # bicho e o que fecha a altura total em 33, igual a do chapeu do viajante.
    Caixa("crest", "crest", 36, 9, -1, 29, -3, 2, 4, 5),
    # BRACOS LONGOS (13 px). Entram 1 px no torso para nao abrir costura no ombro.
    Caixa("arm_left", "arm_left", 0, 37, 4, 8, -1.5, 3, 13, 3),
    Caixa("arm_right", "arm_right", 12, 37, -7, 8, -1.5, 3, 13, 3),
    # AS GARRAS. Mais largas e mais fundas que o antebraco, abertas para fora, e
    # PARADAS EM y=3: elas nao encostam no chao. Bicho que apoia a mao no chao le
    # como quadrupede, e este anda em dois pes.
    Caixa("claw_left", "claw_left", 24, 40, 3.5, 3, -3, 4, 6, 4),
    Caixa("claw_right", "claw_right", 40, 40, -7.5, 3, -3, 4, 6, 4),
    # PERNAS CURTAS E FORTES. Estreitas em x (3, como as do humano) e FUNDAS em z
    # (5 contra 4): a forca mora na profundidade, que e o eixo em que sobra hitbox.
    # Largura era o eixo caro -- os bracos ja gastam ate 7.5 px de cada lado.
    Caixa("leg_left", "leg_left", 30, 27, 0.5, 2, -2.5, 3, 8, 5),
    Caixa("leg_right", "leg_right", 46, 27, -3.5, 2, -2.5, 3, 8, 5),
    # PES compridos, adiantados: 6 px de z contra 5 da perna, quase todo para a
    # frente. E o apoio de quem fica parado em pe observando.
    Caixa("foot_left", "foot_left", 24, 50, 1, 0, -4, 4, 3, 6),
    Caixa("foot_right", "foot_right", 44, 50, -5, 0, -4, 4, 3, 6),
    # A CAUDA. Sai das costas na altura do meio do torso e NAO encosta no chao: ela
    # equilibra a silhueta ereta, e nao a apoia.
    Caixa("tail", "tail", 36, 18, -2, 12, 3, 4, 5, 4),
)

Forma = namedtuple("Forma", "id ossos contrato caixas")

DISFARCE = Forma("kiriko_disfarce", OSSOS_DISFARCE, CONTRATO_DISFARCE, CAIXAS_DISFARCE)
VERDADEIRO = Forma("kiriko", OSSOS_VERDADEIRO, CONTRATO_VERDADEIRO, CAIXAS_VERDADEIRO)
FORMAS = (DISFARCE, VERDADEIRO)

# As pecas de ROUPA do disfarce. E o contorno delas que o corpo nao pode furar.
CHAPEU = ("brim", "crown")


# ------------------------------------------------------------------ utilidades

def caixa(forma, nome):
    return [c for c in forma.caixas if c.nome == nome][0]


def osso(forma, nome):
    return [o for o in forma.ossos if o.nome == nome][0]


def area_no_atlas(c):
    """Retangulo (x0, y0, x1, y1) que a caixa ocupa no atlas."""
    return (c.u, c.v, c.u + 2 * c.d + 2 * c.w, c.v + c.d + c.h)


def volume(c):
    """Caixa envolvente do cubo: ((x0,x1),(y0,y1),(z0,z1))."""
    return ((c.x, c.x + c.w), (c.y, c.y + c.h), (c.z, c.z + c.d))


def limites(forma):
    cs = forma.caixas
    xs = [c.x for c in cs] + [c.x + c.w for c in cs]
    ys = [c.y for c in cs] + [c.y + c.h for c in cs]
    zs = [c.z for c in cs] + [c.z + c.d for c in cs]
    return (min(xs), max(xs)), (min(ys), max(ys)), (min(zs), max(zs))


def largura(forma):
    (x0, x1), _, _ = limites(forma)
    return x1 - x0


def profundidade(forma):
    _, _, (z0, z1) = limites(forma)
    return z1 - z0


def altura(forma):
    _, (_, y1), _ = limites(forma)
    return y1


def espelho(nome):
    """Nome da peca espelhada, ou None se a peca e central."""
    if nome.endswith("_left"):
        return nome[:-len("_left")] + "_right"
    if nome.endswith("_right"):
        return nome[:-len("_right")] + "_left"
    return None


# ------------------------------------------------------------------ validacoes

def valida_ossos(forma):
    nomes = [o.nome for o in forma.ossos]
    if len(set(nomes)) != len(nomes):
        raise ValueError("%s: osso repetido" % forma.id)
    raizes = [o for o in forma.ossos if o.pai is None]
    if len(raizes) != 1 or raizes[0].nome != "root":
        raise ValueError("%s: a unica raiz tem de ser 'root'" % forma.id)
    for o in forma.ossos:
        if o.pai is not None and o.pai not in nomes:
            raise ValueError("%s: osso '%s' aponta para pai inexistente '%s'"
                             % (forma.id, o.nome, o.pai))
    for c in forma.caixas:
        if c.osso not in nomes:
            raise ValueError("%s: caixa '%s' pendurada em osso inexistente '%s'"
                             % (forma.id, c.nome, c.osso))
    # Osso sem cubo e osso que a animacao move e ninguem ve. Aqui TODOS tem volume
    # menos a raiz, que existe so para a entidade inteira se mover.
    sem_cubo = [o.nome for o in forma.ossos
                if o.nome != "root" and not any(c.osso == o.nome for c in forma.caixas)]
    if sem_cubo:
        raise ValueError("%s: ossos sem volume proprio: %s -- a animacao os move e nada aparece"
                         % (forma.id, sem_cubo))


def valida_contrato(forma):
    """A hierarquia e contrato entre as lanes, e contrato se confere."""
    declarado = tuple((o.nome, o.pai) for o in forma.ossos)
    if declarado != forma.contrato:
        faltando = [c for c in forma.contrato if c not in declarado]
        sobrando = [d for d in declarado if d not in forma.contrato]
        raise ValueError("%s: a hierarquia saiu do contrato da entrega. faltando: %s ; sobrando: "
                         "%s. As outras lanes escrevem contra estes nomes: um osso renomeado aqui "
                         "nao levanta excecao nenhuma, so deixa o membro parado na tela."
                         % (forma.id, faltando, sobrando))


def valida_pivots(forma):
    """Pivot de filho mora na JUNCAO com o pai -- e juncao se mede.

    Pivot fora do volume do pai nao da erro: da um membro que gira em torno de um
    ponto que nao existe no bicho, e o sintoma e um clipe 'meio quebrado' que
    ninguem sabe explicar. Aqui isso seria pior do que nos irmaos: `transform` e um
    clipe unico, visto uma vez por encontro, e quem o visse torto nao teria uma
    segunda chance de entender o que viu.
    """
    for o in forma.ossos:
        if o.pai is None:
            continue
        cubos_do_pai = [c for c in forma.caixas if c.osso == o.pai]
        if not cubos_do_pai:
            continue
        px, py, pz = o.pivot
        dentro = any(vx0 <= px <= vx1 and vy0 <= py <= vy1 and vz0 <= pz <= vz1
                     for (vx0, vx1), (vy0, vy1), (vz0, vz1)
                     in (volume(c) for c in cubos_do_pai))
        if not dentro:
            raise ValueError("%s: o pivot de '%s' %s cai fora de todo cubo do pai '%s': o osso "
                             "giraria em torno de um ponto que nao existe no bicho"
                             % (forma.id, o.nome, o.pivot, o.pai))


def valida_simetria(forma):
    """Peca com par tem de ser o espelho do par, em cubo E em pivot.

    Uma tabela escrita a mao erra sinal. Bicho torto nao levanta excecao, nao muda
    gametest nenhum, e so aparece para quem girar a camera em volta dele.
    """
    for c in forma.caixas:
        par = espelho(c.nome)
        if par is None:
            continue
        outro = caixa(forma, par)
        if (c.w, c.h, c.d) != (outro.w, outro.h, outro.d):
            raise ValueError("%s: '%s' e '%s' tem tamanhos diferentes" % (forma.id, c.nome, par))
        if (c.y, c.z) != (outro.y, outro.z):
            raise ValueError("%s: '%s' e '%s' nao estao na mesma altura/profundidade"
                             % (forma.id, c.nome, par))
        if c.x != -(outro.x + outro.w):
            raise ValueError("%s: '%s' (x=%s) nao e o espelho de '%s' (x=%s, w=%s): o bicho sai "
                             "torto" % (forma.id, c.nome, c.x, par, outro.x, outro.w))
    for o in forma.ossos:
        par = espelho(o.nome)
        if par is None:
            continue
        outro = osso(forma, par)
        if (o.pivot[1], o.pivot[2]) != (outro.pivot[1], outro.pivot[2]) \
                or o.pivot[0] != -outro.pivot[0]:
            raise ValueError("%s: o pivot de '%s' %s nao espelha o de '%s' %s: os dois lados giram "
                             "de jeitos diferentes" % (forma.id, o.nome, o.pivot, par, outro.pivot))


def valida_uv(forma):
    """Sobreposicao de UV nao da erro: da textura errada na face. Entao reprova aqui."""
    nomes = [c.nome for c in forma.caixas]
    if len(set(nomes)) != len(nomes):
        raise ValueError("%s: caixa com nome repetido -- o gerador da textura indexa por nome"
                         % forma.id)
    for c in forma.caixas:
        if int(c.w) != c.w or int(c.h) != c.h or int(c.d) != c.d:
            raise ValueError("%s: a caixa '%s' tem tamanho fracionario %s: o portao Java arredonda "
                             "o size para medir o atlas e passaria a medir um retangulo diferente "
                             "deste gerador" % (forma.id, c.nome, (c.w, c.h, c.d)))
        x0, y0, x1, y1 = area_no_atlas(c)
        if x1 > UV_LARGURA or y1 > UV_ALTURA:
            raise ValueError("%s: caixa '%s' estoura o atlas: vai ate (%d,%d)"
                             % (forma.id, c.nome, x1, y1))
    for i, a in enumerate(forma.caixas):
        ax0, ay0, ax1, ay1 = area_no_atlas(a)
        for b in forma.caixas[i + 1:]:
            bx0, by0, bx1, by1 = area_no_atlas(b)
            if ax0 < bx1 and bx0 < ax1 and ay0 < by1 and by0 < ay1:
                raise ValueError("%s: caixas '%s' e '%s' se sobrepoem no atlas"
                                 % (forma.id, a.nome, b.nome))


def valida_faces_coplanares(forma):
    """Duas caixas com a MESMA face no MESMO plano cintilam em jogo.

    Nao e erro de JSON, nao aparece no log, e o jogador le como bug de driver. E o
    unico defeito de modelo que nenhum portao do repositorio enxerga.

    Face OPOSTA no mesmo plano (uma caixa encostando na outra) e permitida: e assim
    que todo modelo do jogo e montado, e as normais contrarias resolvem a disputa
    sozinhas. O que se reprova aqui e MESMA direcao com area em comum.
    """
    eixos = (("x", 0), ("y", 1), ("z", 2))
    for eixo, indice in eixos:
        outros = [i for _, i in eixos if i != indice]
        for lado in (0, 1):  # 0 = face minima, 1 = face maxima
            faces = []
            for c in forma.caixas:
                v = volume(c)
                faces.append((c.nome, v[indice][lado], v[outros[0]], v[outros[1]]))
            for i, (nome_a, plano_a, ra0, ra1) in enumerate(faces):
                for nome_b, plano_b, rb0, rb1 in faces[i + 1:]:
                    if plano_a != plano_b:
                        continue
                    if min(ra0[1], rb0[1]) - max(ra0[0], rb0[0]) <= 0:
                        continue
                    if min(ra1[1], rb1[1]) - max(ra1[0], rb1[0]) <= 0:
                        continue
                    raise ValueError(
                        "%s: '%s' e '%s' tem a mesma face %s=%s virada para o mesmo lado, com area "
                        "em comum: em jogo isso cintila, e o jogador le como bug de video"
                        % (forma.id, nome_a, nome_b, eixo, plano_a))


def valida_hitbox(forma):
    """A forma inteira cabe na caixa de colisao -- nos tres eixos, sem teto nem excecao."""
    (x0, x1), (y0, y1), (z0, z1) = limites(forma)
    if y0 != 0:
        raise ValueError("%s: o piso tem de ficar em y=0, esta em %s" % (forma.id, y0))
    if y1 > HITBOX_ALTURA_PX:
        raise ValueError("%s: modelo mais alto (%s px) que a hitbox (%s px)"
                         % (forma.id, y1, HITBOX_ALTURA_PX))
    if (x1 - x0) > HITBOX_LARGURA_PX:
        raise ValueError("%s: modelo com %s px de largura numa hitbox de %s px"
                         % (forma.id, x1 - x0, HITBOX_LARGURA_PX))
    if (z1 - z0) > HITBOX_LARGURA_PX:
        raise ValueError("%s: modelo com %s px de profundidade numa hitbox de %s px"
                         % (forma.id, z1 - z0, HITBOX_LARGURA_PX))


def valida_mesma_altura_total():
    """As duas formas terminam na MESMA altura, ao pixel.

    A hitbox ja e a mesma por contrato, mas hitbox igual com vulto de alturas
    diferentes ainda entrega a troca: o bicho encolhe na tela. E encolher e pior do
    que crescer -- o jogador le como 'o jogo bugou', e nao como 'ele se transformou'.
    """
    if altura(DISFARCE) != altura(VERDADEIRO):
        raise ValueError("o viajante tem %s px de altura e a forma verdadeira %s: a caixa de "
                         "colisao nao mudaria, mas o VULTO mudaria -- e e o vulto que o jogador ve"
                         % (altura(DISFARCE), altura(VERDADEIRO)))


def valida_contraste_de_silhueta():
    """Mesma altura, entao o contraste tem de vir da LARGURA e do TRONCO.

    Sem contraste nenhum a transformacao vira troca de textura, e o clipe mais
    importante do mob passa a nao comunicar nada. Nada disso levanta excecao: os
    gametests continuam verdes porque comportamento nao mudou.
    """
    lv, ld = largura(VERDADEIRO), largura(DISFARCE)
    if lv - ld < VERDADEIRO_MAIS_LARGO_PX:
        raise ValueError("a forma verdadeira tem %s px de largura e o viajante %s: menos de %s px "
                         "de diferenca e o mesmo vulto, e a transformacao deixa de ser visivel de "
                         "longe" % (lv, ld, VERDADEIRO_MAIS_LARGO_PX))

    quadril_humano = caixa(DISFARCE, "body").y
    quadril_bicho = caixa(VERDADEIRO, "torso").y
    if quadril_humano - quadril_bicho < VERDADEIRO_COM_TRONCO_MAIS_BAIXO_PX:
        raise ValueError("o quadril do bicho esta em y=%s e o do viajante em y=%s: com o tronco na "
                         "mesma altura o corpo comprido e a perna curta nao aparecem, e sobra um "
                         "humano com bico" % (quadril_bicho, quadril_humano))

    # A outra metade do "tronco mais baixo": ele tambem e mais COMPRIDO. Quadril
    # baixo com torso curto nao le como bicho pesado, le como pessoa agachada -- e
    # agachado o Kiriko viraria um humano encolhido, que e o contrario do que a
    # transformacao precisa mostrar.
    torso_bicho = caixa(VERDADEIRO, "torso").h
    torso_humano = caixa(DISFARCE, "body").h
    if torso_bicho - torso_humano < 2:
        raise ValueError("o torso do bicho tem %s px e o casaco do viajante %s: com troncos do "
                         "mesmo tamanho o quadril baixo le como pessoa agachada, e nao como corpo "
                         "de outra especie" % (torso_bicho, torso_humano))


def valida_disfarce_sem_pista():
    """O disfarce NAO tem pista. E o oposto da regra do man-faced ape, de proposito.

    La o disfarce e caca, e caca justa precisa de contrapartida visivel. Aqui o
    Kiriko esta OBSERVANDO -- quem entrega o disfarce e a transformacao, no momento
    em que ele decide mostra-la. Uma pista escondida no modelo nao da erro nenhum:
    ela ensina o jogador a desconfiar, desconfiar leva a sacar arma, e sacar arma e
    exatamente o comportamento que este mob existe para REPROVAR. O bug seria um
    jogador reprovado por ter lido bem o modelo.
    """
    # 1. Nada de peca de bicho no vocabulario do disfarce.
    for c in CAIXAS_DISFARCE:
        baixo = c.nome.lower()
        for palavra in PALAVRAS_DE_BICHO:
            if palavra in baixo:
                raise ValueError("o disfarce tem uma caixa chamada '%s': peca de bicho no corpo "
                                 "humano e pista, e este disfarce nao tem pista" % c.nome)

    # 2. A mao e a secao da manga. Mao maior e a pista do macaco -- aqui, erro.
    for mao, manga in (("hand_left", "sleeve_left"), ("hand_right", "sleeve_right")):
        m, s = caixa(DISFARCE, mao), caixa(DISFARCE, manga)
        if (m.w, m.d) != (s.w, s.d):
            raise ValueError("a mao '%s' tem secao %sx%s e a manga '%s' %sx%s: mao que nao cabe na "
                             "manga e a pista do man-faced ape. Este mob e o contrario -- aqui ela "
                             "ensinaria o jogador a atacar primeiro"
                             % (mao, m.w, m.d, manga, s.w, s.d))

    # 3. NADA de corpo escapa do contorno de roupa. A aba do chapeu e a peca mais
    #    externa em toda direcao horizontal, e a copa e o ponto mais alto. Um toco
    #    de bico, de cauda ou de garra quebraria isto antes de quebrar qualquer
    #    outra coisa.
    aba = caixa(DISFARCE, "brim")
    copa = caixa(DISFARCE, "crown")
    for c in CAIXAS_DISFARCE:
        if c.nome in CHAPEU:
            continue
        if c.x < aba.x or c.x + c.w > aba.x + aba.w:
            raise ValueError("a caixa '%s' vai de x=%s a %s e a aba do chapeu so cobre de %s a %s: "
                             "sobra corpo fora do contorno de roupa"
                             % (c.nome, c.x, c.x + c.w, aba.x, aba.x + aba.w))
        if c.z < aba.z or c.z + c.d > aba.z + aba.d:
            raise ValueError("a caixa '%s' vai de z=%s a %s e a aba do chapeu so cobre de %s a %s: "
                             "isso e um focinho, e focinho e pista"
                             % (c.nome, c.z, c.z + c.d, aba.z, aba.z + aba.d))
        if c.y + c.h > copa.y + copa.h:
            raise ValueError("a caixa '%s' passa do alto do chapeu (y=%s contra %s)"
                             % (c.nome, c.y + c.h, copa.y + copa.h))

    # 4. O rosto fica A MOSTRA. Sem capuz, sem sombra, sem nada por cima: a aba
    #    comeca ACIMA do cranio. Um chapeu que descesse sobre a cara transformaria
    #    este mob no man-faced ape, e a leitura 'ele esta te observando' morreria.
    cranio = caixa(DISFARCE, "head")
    if aba.y < cranio.y + cranio.h:
        raise ValueError("a aba desce ate y=%s e o cranio vai ate y=%s: o chapeu cobriria o rosto, "
                         "e o mob todo depende de o jogador VER que estao olhando para ele"
                         % (aba.y, cranio.y + cranio.h))


def valida_forma_que_pensa():
    """Magical Beast != monster, e isso e desenho -- entao e medivel.

    Cada regra abaixo separa esta criatura de um bicho disforme. Quebrar qualquer
    uma nao produz erro: produz um jogador que olha e ve um monstro, e entao ataca,
    e entao reprova -- com a ficha inteira funcionando e ignorada.
    """
    torso = caixa(VERDADEIRO, "torso")
    cranio = caixa(VERDADEIRO, "skull")
    bico = caixa(VERDADEIRO, "beak")
    crista = caixa(VERDADEIRO, "crest")
    rabo = caixa(VERDADEIRO, "tail")

    # POSTURA ERETA: a cabeca mora EM CIMA do torso, e nao empurrada para a frente
    # entre os ombros (que e a cabeca do man-faced ape, e e postura de quadrupede).
    if cranio.y + cranio.h <= torso.y + torso.h:
        raise ValueError("o cranio termina em y=%s e o torso em y=%s: a cabeca nao esta em cima "
                         "dos ombros, e a postura deixa de ser ereta"
                         % (cranio.y + cranio.h, torso.y + torso.h))
    if cranio.z < torso.z:
        raise ValueError("o cranio comeca em z=%s, a frente do torso (z=%s): cabeca empurrada para "
                         "fora e postura de bicho que anda em quatro -- e a de um monstro, nao a "
                         "de alguem que esta avaliando voce" % (cranio.z, torso.z))

    # ELE ANDA EM DOIS PES. Garra no chao le como quadrupede, na hora.
    for nome in ("claw_left", "claw_right"):
        garra = caixa(VERDADEIRO, nome)
        if garra.y <= 0:
            raise ValueError("a garra '%s' encosta no chao (y=%s): o bicho passa a andar nos nos, "
                             "e a silhueta vira a do man-faced ape revelado" % (nome, garra.y))
        antebraco = caixa(VERDADEIRO, nome.replace("claw", "arm"))
        if garra.w <= antebraco.w or garra.d <= antebraco.d:
            raise ValueError("a garra '%s' (%sx%s) nao e maior que o antebraco (%sx%s): sem abrir, "
                             "ela vira um toco e a mao deixa de ler como mao que pega coisas"
                             % (nome, garra.w, garra.d, antebraco.w, antebraco.d))

    # A CARA TEM ONDE POR OLHOS. O bico ocupa o meio; o que sobra de cada lado e o
    # espaco dos olhos, e olho de 1 coluna some a 3 blocos.
    sobra = (cranio.w - bico.w) / 2.0
    if sobra < 2:
        raise ValueError("o bico tem %s px numa cara de %s: sobram %s colunas de cada lado para o "
                         "olho, e olho estreito demais le como risco. A ficha pede olhos de quem "
                         "PENSA, e isso comeca em haver espaco para eles"
                         % (bico.w, cranio.w, sobra))
    if bico.z >= cranio.z:
        raise ValueError("o bico (frente z=%s) nao passa do cranio (z=%s): 'bico proeminente' vira "
                         "so uma palavra no contrato" % (bico.z, cranio.z))
    if bico.z + bico.d <= cranio.z:
        raise ValueError("o bico termina em z=%s e o cranio comeca em %s: ele nao entra na cabeca, "
                         "e sobra um vao por onde se ve o interior do cranio quando ele abre a boca"
                         % (bico.z + bico.d, cranio.z))

    # A CRISTA E O ALTO DO BICHO -- e e ela que fecha a altura contra o chapeu.
    if crista.y + crista.h != altura(VERDADEIRO):
        raise ValueError("a crista termina em y=%s e o bicho em %s: outra peca virou o ponto mais "
                         "alto, e a crista deixa de ser o remate da silhueta"
                         % (crista.y + crista.h, altura(VERDADEIRO)))
    if crista.w > 3:
        raise ValueError("a crista tem %s px de largura: grossa assim ela le como capacete, e nao "
                         "como crista" % crista.w)

    # A CAUDA EQUILIBRA, NAO APOIA.
    if rabo.z < torso.z + torso.d - 1:
        raise ValueError("a cauda comeca em z=%s e o torso termina em %s: ela nasce dentro do "
                         "corpo e nao aparece" % (rabo.z, torso.z + torso.d))
    if rabo.y <= 0:
        raise ValueError("a cauda encosta no chao: ela passa a ser um terceiro apoio, e a postura "
                         "ereta some")

    # PERNAS FORTES. Na largura nao dava (os bracos gastam o eixo x inteiro), entao
    # a forca mora na PROFUNDIDADE -- e a regua mede a secao, nao o palpite.
    perna = caixa(VERDADEIRO, "leg_left")
    perna_humana = caixa(DISFARCE, "leg_left")
    if perna.w * perna.d <= perna_humana.w * perna_humana.d:
        raise ValueError("a perna do bicho tem secao %sx%s e a do viajante %sx%s: 'pernas fortes' "
                         "precisa aparecer em algum eixo, e neste modelo o eixo disponivel e z"
                         % (perna.w, perna.d, perna_humana.w, perna_humana.d))


# ------------------------------------------------------------------ geracao

def _osso_json(forma, o):
    d = {"name": o.nome}
    if o.pai is not None:
        d["parent"] = o.pai
    d["pivot"] = list(o.pivot)
    if o.rotacao is not None:
        d["rotation"] = list(o.rotacao)
    cubos = [{"origin": [c.x, c.y, c.z], "size": [c.w, c.h, c.d], "uv": [c.u, c.v]}
             for c in forma.caixas if c.osso == o.nome]
    if cubos:
        d["cubes"] = cubos
    return d


def geometria(forma):
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                # O portao generico confere ESTA string contra o nome do arquivo:
                # errada, o modelo nao e achado e o mob some da tela sem log.
                "identifier": "geometry." + forma.id,
                "texture_width": UV_LARGURA,
                "texture_height": UV_ALTURA,
                # A caixa de visibilidade cobre o bicho com os bracos ABERTOS no
                # `transform`: apertada demais, ele sumiria da tela justo no quadro
                # em que o jogador esta descobrindo o que ele e.
                "visible_bounds_width": 2.5,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 1.25, 0],
            },
            "bones": [_osso_json(forma, o) for o in forma.ossos],
        }],
    }


def _achatar_numeros(texto):
    """Poe os vetores numericos numa linha so -- o JSON e gerado, mas e lido em review."""
    return re.sub(r"\[\s+([-\d.,\se]+?)\s*\]",
                  lambda m: "[" + ", ".join(m.group(1).replace(",", " ").split()) + "]",
                  texto)


def escreve(forma):
    texto = _achatar_numeros(json.dumps(geometria(forma), indent=2))
    json.loads(texto)  # o JSON gerado tem de continuar valido depois do achatamento

    destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                           "geo", "entity", forma.id + ".geo.json")
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    with open(destino, "w", encoding="utf-8", newline="\n") as f:
        f.write(texto + "\n")
    return destino


def main():
    for forma in FORMAS:
        valida_ossos(forma)
        valida_contrato(forma)
        valida_pivots(forma)
        valida_simetria(forma)
        valida_uv(forma)
        valida_faces_coplanares(forma)
        valida_hitbox(forma)
    valida_mesma_altura_total()
    valida_contraste_de_silhueta()
    valida_disfarce_sem_pista()
    valida_forma_que_pensa()

    for forma in FORMAS:
        destino = escreve(forma)
        (x0, x1), (y0, y1), (z0, z1) = limites(forma)
        ocupado = sum((2 * c.d + 2 * c.w) * (c.d + c.h) for c in forma.caixas)
        print("escrito", destino)
        print("  ossos: %d   caixas: %d" % (len(forma.ossos), len(forma.caixas)))
        print("  envolvente: x %s..%s  y %s..%s  z %s..%s (px)" % (x0, x1, y0, y1, z0, z1))
        print("  %s larg x %s alt x %s prof   (hitbox %.1f x %.1f x %.1f)"
              % (x1 - x0, y1, z1 - z0, HITBOX_LARGURA_PX, HITBOX_ALTURA_PX, HITBOX_LARGURA_PX))
        print("  atlas %dx%d: %d de %d px ocupados (%.0f%%)"
              % (UV_LARGURA, UV_ALTURA, ocupado, UV_LARGURA * UV_ALTURA,
                 100.0 * ocupado / (UV_LARGURA * UV_ALTURA)))
        for o in forma.ossos:
            cubos = [c.nome for c in forma.caixas if c.osso == o.nome]
            print("    %-12s pai=%-12s pivot=%-16s %s"
                  % (o.nome, o.pai, o.pivot, ", ".join(cubos) if cubos else "(so transporta)"))

    print("as duas formas medem %s px de altura -- a mesma, ao pixel" % altura(DISFARCE))
    print("contraste: o verdadeiro e %.1f px mais largo, %.1f px mais fundo, e tem o quadril "
          "%.1f px mais baixo"
          % (largura(VERDADEIRO) - largura(DISFARCE),
             profundidade(VERDADEIRO) - profundidade(DISFARCE),
             caixa(DISFARCE, "body").y - caixa(VERDADEIRO, "torso").y))
    print("perna a mostra (chao ate o quadril): %s px no viajante, %s px no bicho"
          % (caixa(DISFARCE, "body").y, caixa(VERDADEIRO, "torso").y))
    print("torso: %s px no viajante, %s px no bicho"
          % (caixa(DISFARCE, "body").h, caixa(VERDADEIRO, "torso").h))


if __name__ == "__main__":
    main()
