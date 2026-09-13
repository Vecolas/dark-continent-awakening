"""Gera a geometria autoral do Master of the Swamp (bedrock 1.12.0, GeckoLib 4).

POR QUE ESTE ARQUIVO EXISTE. Os cinco irmaos entregues nasceram vestindo mob
vanilla e ganharam corpo proprio depois (ADR-017). Este nao: ele nasce sem
andaime nenhum, e o corpo dele comeca aqui. Nao ha Blockbench no repositorio,
entao a FONTE VERSIONADA e este Python -- JSON gerado sem gerador no git vira um
arquivo que ninguem consegue corrigir depois.

ESTE MODULO E A UNICA TABELA DE CAIXAS. O gerador da textura importa CAIXAS daqui
de proposito: duas tabelas separadas divergem na primeira correcao de modelo, e a
divergencia nao da erro -- da face pintada no lugar errado, que so aparece na
tela.

O QUE ESTE BICHO E, E POR QUE ISSO MANDA NA GEOMETRIA
-----------------------------------------------------
Ele nao e um inimigo que se mata: e um que se FISGA. O jogador lanca a isca, o
peixe morde, e o resto do encontro e um cabo de guerra -- puxar demais arrebenta a
linha, acompanhar cansa o peixe ate ele poder ser recolhido. Matar a pancada e
possivel e chato de proposito (60 de vida, armadura 4, e ele foge ao levar dano).

Tres consequencias diretas, e cada uma tem regua neste arquivo:

(a) A BOCA E A MECANICA, entao ela tem de LER DE FRENTE. O que fisga e o que o
    jogador precisa reconhecer antes de decidir puxar. `valida_boca` mede: as
    fileiras de dentes e as presas tem de ocupar pelo menos um quarto da area
    frontal da cabeca, tem de estar na PONTA do focinho, e as presas tem de sobrar
    para FORA da mandibula -- dente enterrado no queixo continua no arquivo,
    continua animado, e ninguem nunca o ve.

(b) ELE NAO E UM PEIXE GRANDE: e uma COISA ERRADA que vive no pantano. O que tira
    o bicho da categoria "peixe" sao as QUATRO pernas articuladas sob o corpo, com
    garra. `valida_pernas` reprova perna grossa (vira nadadeira), garra que nao
    abre (vira toco) e corpo encostando no chao (as pernas somem e sobra um peixe
    deitado).

(c) ELE POUSA NO FUNDO QUANDO CANSA. Por isso y=0 e o chao e as GARRAS sao o que
    encosta nele -- e nao a barriga. O clipe `caught` desce o bicho sobre as
    proprias pernas; se o corpo ja nascesse no chao, nao haveria para onde descer.

A HITBOX MANDA NO MODELO, E AQUI ELA NAO PRECISOU DE TETO NENHUM.
-----------------------------------------------------------------
A entidade e sized(2.4F, 1.6F) -- 38,4 x 25,6 px. O contrato permitia declarar um
teto e justificar um COMPRIMENTO maior que a largura de colisao, como os irmaos
fizeram com envergadura e chifre. Nao foi preciso: o bicho fecha em 37 x 24 x 24
px e cabe INTEIRO na caixa de colisao, em todos os eixos. `valida_hitbox` cobra os
tres lados, sem excecao e sem teto -- e uma regua mais dura de propria por nao ter
excecao para manter.

Isso importa para o cabo de guerra: a distancia entre o jogador e o peixe e medida
na hitbox, e um modelo que sobrasse dela faria a linha arrebentar "sem motivo"
para quem estivesse olhando o desenho.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

Regerar:  python art-source/enemies/master_of_the_swamp/master_of_the_swamp_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/master_of_the_swamp.geo.json
"""
from collections import namedtuple
import json
import os
import re

IDENTIFICADOR = "geometry.master_of_the_swamp"
UV_LARGURA, UV_ALTURA = 128, 64

# Limites que a entidade impoe: EnemyEntityTypes .sized(2.4F, 1.6F).
HITBOX_LARGURA_PX = 2.4 * 16.0
HITBOX_ALTURA_PX = 1.6 * 16.0

# Fracao minima da area frontal da cabeca que tem de ser DENTE. Nao e botao de
# tuning: e o ponto em que a boca deixa de ser reconhecivel de frente, e a boca e
# a unica coisa que este mob faz com o jogador.
DENTE_MINIMO_NA_CARA = 0.25

Osso = namedtuple("Osso", "nome pai pivot rotacao")

# Hierarquia CONGELADA pelo contrato da entrega: as animacoes, o GeoModel e o
# gametest escrevem contra estes nomes. Nome errado aqui nao da erro -- o GeckoLib
# so deixa o osso parado, e isso so aparece na tela.
#
# O pivot de um filho mora na JUNCAO com o pai, e `valida_pivots` cobra isso
# medindo: todo pivot cai DENTRO de um cubo do pai. Os que mais importam:
#   * `jaw` gira na DOBRADICA, no fundo-baixo do cranio (0, 15, -7) -- e nao na
#     ponta do focinho: mandibula girando pela ponta abre a boca para tras;
#   * `tail_fin` gira no FIM da cauda (0, 14, 15), que e o que faz o rabo
#     chicotear em `thrash` em vez de girar inteiro como um remo;
#   * `claw_*` gira no tornozelo (y=2), que e onde a perna encontra a garra.
#
# NENHUM osso nasce com `rotation`. E decisao, nao esquecimento: a pose mora nas
# CAIXAS, e nao numa rotacao de bind. Com bind rotation, todo valor escrito no
# .animation.json seria somado a um angulo invisivel -- duas fontes para a mesma
# pose, decididas em silencio.
OSSOS = (
    Osso("root", None, (0, 0, 0), None),
    Osso("body", "root", (0, 14, 0), None),
    Osso("head", "body", (0, 15, -6), None),
    Osso("jaw", "head", (0, 15, -7), None),
    Osso("fin_left", "body", (6, 13, -2), None),
    Osso("fin_right", "body", (-6, 13, -2), None),
    Osso("tail", "body", (0, 14, 8), None),
    Osso("tail_fin", "tail", (0, 14, 15), None),
    Osso("leg_front_left", "body", (6, 8, -3), None),
    Osso("claw_front_left", "leg_front_left", (6, 2, -3), None),
    Osso("leg_front_right", "body", (-6, 8, -3), None),
    Osso("claw_front_right", "leg_front_right", (-6, 2, -3), None),
    Osso("leg_back_left", "body", (6, 8, 4), None),
    Osso("claw_back_left", "leg_back_left", (6, 2, 4), None),
    Osso("leg_back_right", "body", (-6, 8, 4), None),
    Osso("claw_back_right", "leg_back_right", (-6, 2, 4), None),
)

# A hierarquia que o contrato da entrega fixou, palavra por palavra. Ela esta
# repetida aqui de proposito, e a repeticao TEM dono: `valida_contrato` compara as
# duas. Renomear um osso la em cima e mecanico; renomear e o contrato continuar
# batendo so acontece se alguem mudar os dois -- que e exatamente a conversa que
# precisa acontecer antes de uma animacao parar de tocar em silencio.
CONTRATO = (
    ("root", None),
    ("body", "root"),
    ("head", "body"),
    ("jaw", "head"),
    ("fin_left", "body"),
    ("fin_right", "body"),
    ("tail", "body"),
    ("tail_fin", "tail"),
    ("leg_front_left", "body"),
    ("claw_front_left", "leg_front_left"),
    ("leg_front_right", "body"),
    ("claw_front_right", "leg_front_right"),
    ("leg_back_left", "body"),
    ("claw_back_left", "leg_back_left"),
    ("leg_back_right", "body"),
    ("claw_back_right", "leg_back_right"),
)

Caixa = namedtuple("Caixa", "nome osso u v x y z w h d")

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) px a partir de (u,v) -- as validacoes
# abaixo reprovam sobreposicao e estouro do atlas.
#
# Todo tamanho e INTEIRO de proposito. O portao Java (CoerenciaDeGeckoLibTest)
# ARREDONDA `size` para calcular o retangulo do atlas; um size fracionario faria o
# portao medir um retangulo e este gerador medir outro, e as duas contas so
# discordariam no dia em que uma delas achasse sobreposicao.
CAIXAS = (
    # --- tronco: fusiforme e PESADO ----------------------------------------
    # 12 de largura contra 10 do cranio e 6 da cauda: e o afunilamento nas duas
    # pontas que faz o bicho ler como peixe, e nao como jacare.
    Caixa("trunk", "body", 0, 0, -6, 7, -6, 12, 14, 14),
    # A barriga PENDE entre as pernas. Ela e o que faz o bicho parecer pesado
    # demais para as proprias patas -- e e o que da a elas contra o que aparecer.
    Caixa("belly", "body", 0, 44, -5, 5, -4, 10, 2, 10),
    # A crista dorsal: e ela que corta a agua e e a unica parte que sobra acima do
    # dorso quando o bicho nada raso. Visto de cima, e o que o jogador persegue.
    Caixa("dorsal", "body", 38, 28, -2, 21, -4, 4, 3, 12),

    # --- cabeca: quase toda boca -------------------------------------------
    # O cranio e CHATO (6 px de altura contra 8 da boca inteira): a cabeca deste
    # bicho e uma tampa sobre uma armadilha, e nao um cranio com uma boca embaixo.
    Caixa("skull", "head", 52, 0, -5, 15, -18, 10, 6, 12),
    # A FILEIRA DE CIMA, rente a ponta do focinho (mesmo z do cranio).
    Caixa("teeth_upper", "head", 40, 44, -4, 13, -18, 8, 2, 7),
    # AS PRESAS DE CIMA descem POR FORA da mandibula (x 4..5, e o queixo vai ate
    # 4): e o unico jeito de aparecerem com a boca fechada.
    Caixa("fang_upper_left", "head", 108, 0, 4, 11, -18, 1, 4, 4),
    Caixa("fang_upper_right", "head", 118, 0, -5, 11, -18, 1, 4, 4),

    # --- mandibula ---------------------------------------------------------
    Caixa("jaw", "jaw", 0, 28, -4, 7, -17, 8, 4, 11),
    Caixa("teeth_lower", "jaw", 52, 20, -3, 11, -17, 6, 2, 6),
    # AS PRESAS DE BAIXO sobem por fora da bochecha (x 5..6, e o cranio vai ate 5).
    # Elas ficam mais atras que as de cima de proposito: presas cruzadas leem como
    # armadilha fechada, presas alinhadas leem como grade.
    Caixa("fang_lower_left", "jaw", 108, 8, 5, 13, -13, 1, 5, 3),
    Caixa("fang_lower_right", "jaw", 116, 8, -6, 13, -13, 1, 5, 3),
    # OS BARBILHOES. Penduram 4 px abaixo do queixo e moram na MANDIBULA: eles
    # varrem o fundo procurando a isca, e por isso balancam junto com a boca que
    # morde. Sao a peca que diz "agua parada e barro" sem uma linha de texto.
    Caixa("barbel_left", "jaw", 112, 44, 2, 3, -16, 1, 6, 1),
    Caixa("barbel_right", "jaw", 116, 44, -3, 3, -16, 1, 6, 1),

    # --- nadadeiras peitorais ----------------------------------------------
    # Chatas (2 px de altura) e largas: nadadeira e superficie, nao volume. Elas
    # sao o que a animacao `turn` inclina para a curva ler como curva.
    Caixa("fin_left", "fin_left", 96, 28, 6, 12, -5, 6, 2, 6),
    Caixa("fin_right", "fin_right", 96, 36, -12, 12, -5, 6, 2, 6),

    # --- cauda e leme ------------------------------------------------------
    Caixa("tail", "tail", 70, 28, -3, 10, 8, 6, 8, 7),
    # A LAMINA CAUDAL: 2 px de espessura por 18 de altura. Ela e a peca mais alta
    # do bicho e a que se ve primeiro quando ele esta fisgado e bate na agua --
    # `thrash` e, na tela, quase inteiro esta lamina.
    Caixa("tail_fin", "tail_fin", 96, 0, -1, 6, 15, 2, 18, 4),

    # --- quatro pernas de crustaceo, sob o corpo ---------------------------
    # 2 x 2 px de secao: finas o bastante para lerem como perna articulada, e nao
    # como nadadeira ventral. Elas entram 1 px no tronco para nao abrir costura.
    Caixa("leg_front_left", "leg_front_left", 108, 16, 5, 2, -4, 2, 6, 2),
    Caixa("leg_front_right", "leg_front_right", 116, 16, -7, 2, -4, 2, 6, 2),
    Caixa("leg_back_left", "leg_back_left", 120, 28, 5, 2, 3, 2, 6, 2),
    Caixa("leg_back_right", "leg_back_right", 120, 36, -7, 2, 3, 2, 6, 2),

    # --- garras: o unico ponto de apoio no fundo ---------------------------
    # 3 px de largura contra 2 da perna, e abertas para FORA: e esse contorno que
    # aparece quando o bicho pousa no fundo, cansado, e para de ser peixe.
    Caixa("claw_front_left", "claw_front_left", 76, 20, 5, 0, -5, 3, 2, 4),
    Caixa("claw_front_right", "claw_front_right", 70, 44, -8, 0, -5, 3, 2, 4),
    Caixa("claw_back_left", "claw_back_left", 84, 44, 5, 0, 2, 3, 2, 4),
    Caixa("claw_back_right", "claw_back_right", 98, 44, -8, 0, 2, 3, 2, 4),
)

PERNAS = ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right")
GARRAS = ("claw_front_left", "claw_front_right", "claw_back_left", "claw_back_right")
DENTES = ("teeth_upper", "teeth_lower",
          "fang_upper_left", "fang_upper_right", "fang_lower_left", "fang_lower_right")
CARA = ("skull", "jaw") + DENTES


# ------------------------------------------------------------------ utilidades

def caixa(nome):
    return [c for c in CAIXAS if c.nome == nome][0]


def osso(nome):
    return [o for o in OSSOS if o.nome == nome][0]


def area_no_atlas(c):
    """Retangulo (x0, y0, x1, y1) que a caixa ocupa no atlas."""
    return (c.u, c.v, c.u + 2 * c.d + 2 * c.w, c.v + c.d + c.h)


def volume(c):
    """Caixa envolvente do cubo: ((x0,x1),(y0,y1),(z0,z1))."""
    return ((c.x, c.x + c.w), (c.y, c.y + c.h), (c.z, c.z + c.d))


def limites(caixas=None):
    # `caixas=CAIXAS` como default seria avaliado UMA vez, na definicao: a funcao
    # passaria a medir para sempre a tabela do momento do import. Isso nao da erro
    # -- da uma regua que aprova qualquer modelo, porque ela nunca ve a mudanca.
    caixas = CAIXAS if caixas is None else caixas
    xs = [c.x for c in caixas] + [c.x + c.w for c in caixas]
    ys = [c.y for c in caixas] + [c.y + c.h for c in caixas]
    zs = [c.z for c in caixas] + [c.z + c.d for c in caixas]
    return (min(xs), max(xs)), (min(ys), max(ys)), (min(zs), max(zs))


def area_frontal(nomes):
    """Area projetada na cara do bicho (w x h) das caixas citadas."""
    return sum(caixa(n).w * caixa(n).h for n in nomes)


def espelho(nome):
    """Nome da peca espelhada, ou None se a peca e central."""
    if nome.endswith("_left"):
        return nome[:-len("_left")] + "_right"
    if nome.endswith("_right"):
        return nome[:-len("_right")] + "_left"
    return None


# ------------------------------------------------------------------ validacoes

def valida_ossos():
    nomes = [o.nome for o in OSSOS]
    if len(set(nomes)) != len(nomes):
        raise ValueError("osso repetido")
    raizes = [o for o in OSSOS if o.pai is None]
    if len(raizes) != 1 or raizes[0].nome != "root":
        raise ValueError("a unica raiz tem de ser 'root'")
    for o in OSSOS:
        if o.pai is not None and o.pai not in nomes:
            raise ValueError("osso '%s' aponta para pai inexistente '%s'" % (o.nome, o.pai))
    for c in CAIXAS:
        if c.osso not in nomes:
            raise ValueError("caixa '%s' pendurada em osso inexistente '%s'" % (c.nome, c.osso))
    # Osso sem cubo e osso que a animacao move e ninguem ve. Aqui TODOS tem volume
    # menos a raiz, que existe so para o bicho inteiro poder subir, descer e rolar.
    sem_cubo = [o.nome for o in OSSOS
                if o.nome != "root" and not any(c.osso == o.nome for c in CAIXAS)]
    if sem_cubo:
        raise ValueError("ossos sem volume proprio: %s -- a animacao os move e nada aparece"
                         % sem_cubo)


def valida_contrato():
    """A hierarquia e contrato entre quatro lanes, e contrato se confere."""
    declarado = tuple((o.nome, o.pai) for o in OSSOS)
    if declarado != CONTRATO:
        faltando = [c for c in CONTRATO if c not in declarado]
        sobrando = [d for d in declarado if d not in CONTRATO]
        raise ValueError("a hierarquia saiu do contrato da entrega. faltando: %s ; sobrando: %s. "
                         "As outras lanes escrevem contra estes nomes: um osso renomeado aqui nao "
                         "levanta excecao nenhuma, so deixa o membro parado na tela."
                         % (faltando, sobrando))


def valida_pivots():
    """Pivot de filho mora na JUNCAO com o pai -- e juncao se mede.

    Pivot fora do volume do pai nao da erro: da um membro que gira em torno de um
    ponto que nao existe no bicho, e o sintoma e um clipe 'meio quebrado' que
    ninguem sabe explicar.
    """
    for o in OSSOS:
        if o.pai is None or o.pai == "root":
            continue
        cubos_do_pai = [c for c in CAIXAS if c.osso == o.pai]
        if not cubos_do_pai:
            continue
        px, py, pz = o.pivot
        dentro = any(vx0 <= px <= vx1 and vy0 <= py <= vy1 and vz0 <= pz <= vz1
                     for (vx0, vx1), (vy0, vy1), (vz0, vz1)
                     in (volume(c) for c in cubos_do_pai))
        if not dentro:
            raise ValueError("o pivot de '%s' %s cai fora de todo cubo do pai '%s': o osso giraria "
                             "em torno de um ponto que nao existe no bicho"
                             % (o.nome, o.pivot, o.pai))


def valida_simetria():
    """Peca com par tem de ser o espelho do par, em cubo E em pivot.

    Uma tabela escrita a mao erra sinal. Bicho torto nao levanta excecao, nao muda
    gametest nenhum, e so aparece para quem girar a camera em volta dele -- e neste
    mob, que o jogador ve de cima e de frente, pode nunca aparecer.
    """
    for c in CAIXAS:
        par = espelho(c.nome)
        if par is None:
            continue
        outro = caixa(par)
        if (c.w, c.h, c.d) != (outro.w, outro.h, outro.d):
            raise ValueError("'%s' e '%s' tem tamanhos diferentes" % (c.nome, par))
        if (c.y, c.z) != (outro.y, outro.z):
            raise ValueError("'%s' e '%s' nao estao na mesma altura/profundidade" % (c.nome, par))
        if c.x != -(outro.x + outro.w):
            raise ValueError("'%s' (x=%s) nao e o espelho de '%s' (x=%s, w=%s): o bicho sai torto"
                             % (c.nome, c.x, par, outro.x, outro.w))
    for o in OSSOS:
        par = espelho(o.nome)
        if par is None:
            continue
        outro = osso(par)
        if (o.pivot[1], o.pivot[2]) != (outro.pivot[1], outro.pivot[2]) \
                or o.pivot[0] != -outro.pivot[0]:
            raise ValueError("o pivot de '%s' %s nao espelha o de '%s' %s: os dois lados giram "
                             "de jeitos diferentes" % (o.nome, o.pivot, par, outro.pivot))


def valida_uv():
    """Sobreposicao de UV nao da erro: da textura errada na face. Entao reprova aqui."""
    nomes = [c.nome for c in CAIXAS]
    if len(set(nomes)) != len(nomes):
        raise ValueError("caixa com nome repetido -- o gerador da textura indexa por nome")
    for c in CAIXAS:
        if int(c.w) != c.w or int(c.h) != c.h or int(c.d) != c.d:
            raise ValueError("a caixa '%s' tem tamanho fracionario %s: o portao Java arredonda o "
                             "size para medir o atlas e passaria a medir um retangulo diferente "
                             "deste gerador" % (c.nome, (c.w, c.h, c.d)))
        x0, y0, x1, y1 = area_no_atlas(c)
        if x1 > UV_LARGURA or y1 > UV_ALTURA:
            raise ValueError("caixa '%s' estoura o atlas: vai ate (%d,%d)" % (c.nome, x1, y1))
    for i, a in enumerate(CAIXAS):
        ax0, ay0, ax1, ay1 = area_no_atlas(a)
        for b in CAIXAS[i + 1:]:
            bx0, by0, bx1, by1 = area_no_atlas(b)
            if ax0 < bx1 and bx0 < ax1 and ay0 < by1 and by0 < ay1:
                raise ValueError("caixas '%s' e '%s' se sobrepoem no atlas" % (a.nome, b.nome))


def valida_faces_coplanares():
    """Duas caixas com a MESMA face no MESMO plano cintilam em jogo.

    Nao e erro de JSON, nao aparece no log, e o jogador le como bug de driver. E o
    unico defeito de modelo que nenhum portao do repositorio enxerga -- e neste
    bicho ele seria pior que nos irmaos, porque ele e visto atraves da agua, onde
    cintilacao ja e o que o jogador espera do jogo.

    Face OPOSTA no mesmo plano (uma caixa encostando na outra) e permitida: e assim
    que todo modelo do jogo e montado, e as normais contrarias resolvem a disputa
    sozinhas. O que se reprova aqui e MESMA direcao com area em comum.
    """
    eixos = (("x", 0), ("y", 1), ("z", 2))
    for eixo, indice in eixos:
        outros = [i for _, i in eixos if i != indice]
        for lado in (0, 1):  # 0 = face minima, 1 = face maxima
            faces = []
            for c in CAIXAS:
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
                        "'%s' e '%s' tem a mesma face %s=%s virada para o mesmo lado, com area em "
                        "comum: em jogo isso cintila, e o jogador le como bug de video"
                        % (nome_a, nome_b, eixo, plano_a))


def valida_hitbox():
    """O bicho inteiro cabe na caixa de colisao. Sem teto e sem excecao -- ver o cabecalho."""
    (x0, x1), (y0, y1), (z0, z1) = limites()
    if y0 != 0:
        raise ValueError("as garras tem de pousar em y=0, e a peca mais baixa esta em %s: o clipe "
                         "`caught` nao teria para onde descer" % y0)
    if y1 > HITBOX_ALTURA_PX:
        raise ValueError("modelo mais alto (%s px) que a hitbox (%s px)" % (y1, HITBOX_ALTURA_PX))
    if (x1 - x0) > HITBOX_LARGURA_PX:
        raise ValueError("modelo com %s px de largura numa hitbox de %s px"
                         % (x1 - x0, HITBOX_LARGURA_PX))
    if (z1 - z0) > HITBOX_LARGURA_PX:
        raise ValueError("modelo com %s px de comprimento numa hitbox de %s px: a distancia do "
                         "cabo de guerra e medida na hitbox, e a linha arrebentaria 'sem motivo' "
                         "para quem estivesse olhando o desenho" % (z1 - z0, HITBOX_LARGURA_PX))


def valida_boca():
    """A boca e a mecanica. Se ela nao le de frente, o mob nao comunica o que faz."""
    cranio, queixo = caixa("skull"), caixa("jaw")
    cara = area_frontal(CARA)
    dente = area_frontal(DENTES)
    if dente < DENTE_MINIMO_NA_CARA * cara:
        raise ValueError("dente ocupa %d de %d px da cara (%.0f%%, minimo %.0f%%): de frente este "
                         "bicho vira um peixe grande, e o jogador nao tem como saber que a boca e "
                         "o que importa"
                         % (dente, cara, 100.0 * dente / cara, 100 * DENTE_MINIMO_NA_CARA))

    # A ponta do focinho: dente enterrado no meio da cabeca nao aparece de frente.
    frente = min(c.z for c in CAIXAS)
    if caixa("teeth_upper").z != frente:
        raise ValueError("a fileira de cima comeca em z=%s e a ponta do bicho esta em z=%s: os "
                         "dentes ficam atras do cranio e a cara vira uma parede"
                         % (caixa("teeth_upper").z, frente))
    if caixa("teeth_lower").z > queixo.z + 1:
        raise ValueError("a fileira de baixo esta %s px recuada no queixo: com a boca fechada nao "
                         "sobra dente nenhum embaixo" % (caixa("teeth_lower").z - queixo.z))

    # As duas fileiras se encontram sem se atravessar: elas leem como uma boca
    # fechada sobre a isca, e nao como um bloco de dente.
    cima, baixo = caixa("teeth_upper"), caixa("teeth_lower")
    if baixo.y + baixo.h != cima.y:
        raise ValueError("as fileiras nao se encontram (baixo termina em y=%s, cima comeca em "
                         "y=%s): ou sobra um vao no meio da cara, ou uma atravessa a outra"
                         % (baixo.y + baixo.h, cima.y))
    if cima.y + cima.h != cranio.y:
        raise ValueError("a fileira de cima nao encosta no cranio: sobra um vao por onde se ve o "
                         "interior da cabeca")
    if queixo.y + queixo.h != baixo.y:
        raise ValueError("a fileira de baixo nao encosta no queixo")

    # As presas sobram para FORA: e o que faz a boca ter contorno com ela fechada.
    for nome in ("fang_upper_left", "fang_lower_left"):
        presa = caixa(nome)
        vizinho = queixo if nome.startswith("fang_upper") else cranio
        if presa.x < vizinho.x + vizinho.w:
            raise ValueError("a presa '%s' (x=%s) nasce dentro de '%s' (ate x=%s): ela fica "
                             "enterrada, continua no arquivo, continua animada, e ninguem a ve"
                             % (nome, presa.x, vizinho.nome, vizinho.x + vizinho.w))
    if caixa("fang_lower_left").z < caixa("fang_upper_left").z + caixa("fang_upper_left").d:
        raise ValueError("as presas de cima e de baixo se cruzam em profundidade: em vez de "
                         "armadilha fechada, a boca le como grade")

    # Os barbilhoes tem de SOBRAR abaixo do queixo, ou nao existem na tela.
    for nome in ("barbel_left", "barbel_right"):
        fio = caixa(nome)
        if queixo.y - fio.y < 3:
            raise ValueError("o barbilhao '%s' sobra so %s px abaixo do queixo: de longe ele some "
                             "no contorno da cabeca" % (nome, queixo.y - fio.y))
        if fio.y <= 0:
            raise ValueError("o barbilhao '%s' encosta no chao: ele varre o fundo, nao se apoia "
                             "nele" % nome)


def valida_pernas():
    """QUATRO pernas finas com garra. E delas que vem o desconforto do bicho."""
    if len(PERNAS) != 4 or len(GARRAS) != 4:
        raise ValueError("sao quatro pernas e quatro garras -- e o que tira o bicho da categoria "
                         "'peixe grande'")
    tronco = caixa("trunk")
    for nome in PERNAS:
        perna = caixa(nome)
        if perna.w > 2 or perna.d > 2:
            raise ValueError("a perna '%s' tem secao %s x %s px: grossa assim ela le como "
                             "nadadeira ventral, e o lado de crustaceo some"
                             % (nome, perna.w, perna.d))
        if perna.y + perna.h <= tronco.y:
            raise ValueError("a perna '%s' nao chega ao tronco: sobra um vao entre o corpo e a "
                             "pata, e o bicho parece desmontado" % nome)
        if perna.y >= tronco.y:
            raise ValueError("a perna '%s' comeca dentro do tronco: nao sobra perna para ver"
                             % nome)
    for nome in GARRAS:
        garra = caixa(nome)
        perna = caixa(nome.replace("claw", "leg"))
        if garra.w <= perna.w:
            raise ValueError("a garra '%s' (%s px) nao e mais larga que a perna (%s px): sem abrir "
                             "ela vira um toco" % (nome, garra.w, perna.w))
        if garra.y != 0:
            raise ValueError("a garra '%s' nao encosta no chao (y=%s): o bicho cansado pousaria no "
                             "fundo pela barriga" % (nome, garra.y))
    piso_do_corpo = min(c.y for c in CAIXAS if c.osso not in PERNAS + GARRAS)
    if piso_do_corpo <= 0:
        raise ValueError("ha corpo encostando no chao: as pernas deixam de ser o que sustenta o "
                         "bicho e a silhueta vira um peixe deitado")


def valida_peixe():
    """Fusiforme, com leme e nadadeiras. Sem isso ele deixa de ser um peixe com pernas."""
    tronco, cranio, rabo = caixa("trunk"), caixa("skull"), caixa("tail")
    if tronco.w <= cranio.w or tronco.w <= rabo.w:
        raise ValueError("o tronco (%s px) nao e a peca mais larga (cranio %s, cauda %s): sem o "
                         "afunilamento nas duas pontas o corpo deixa de ser fusiforme"
                         % (tronco.w, cranio.w, rabo.w))
    lamina = caixa("tail_fin")
    if lamina.h < 4 * lamina.w:
        raise ValueError("a lamina caudal tem %s x %s px: grossa demais para ler como leme, e "
                         "`thrash` e quase inteiro esta peca na tela" % (lamina.w, lamina.h))
    if lamina.h <= rabo.h:
        raise ValueError("a lamina (%s px) nao passa da cauda (%s px): o leme some atras do corpo"
                         % (lamina.h, rabo.h))
    peitoral = caixa("fin_left")
    if peitoral.h > 2:
        raise ValueError("a nadadeira peitoral tem %s px de espessura: nadadeira e superficie, "
                         "nao volume" % peitoral.h)
    if peitoral.x < tronco.x + tronco.w:
        raise ValueError("a nadadeira peitoral nasce dentro do tronco: ela nao aparece de cima, "
                         "que e de onde o jogador olha a agua")
    crista = caixa("dorsal")
    if crista.y != tronco.y + tronco.h:
        raise ValueError("a crista dorsal nao encosta no dorso: ou flutua, ou afunda nele")
    if crista.y + crista.h != max(c.y + c.h for c in CAIXAS if c.osso == "body"):
        raise ValueError("a crista nao e a peca mais alta do corpo: nadando raso, o que aparece "
                         "na superficie passa a ser o dorso liso, e nao a crista")


# ------------------------------------------------------------------ geracao

def _osso_json(o):
    d = {"name": o.nome}
    if o.pai is not None:
        d["parent"] = o.pai
    d["pivot"] = list(o.pivot)
    if o.rotacao is not None:
        d["rotation"] = list(o.rotacao)
    cubos = [{"origin": [c.x, c.y, c.z], "size": [c.w, c.h, c.d], "uv": [c.u, c.v]}
             for c in CAIXAS if c.osso == o.nome]
    if cubos:
        d["cubes"] = cubos
    return d


def geometria():
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                # O portao generico confere ESTA string contra o nome do arquivo:
                # errada, o modelo nao e achado e o mob some da tela sem log.
                "identifier": IDENTIFICADOR,
                "texture_width": UV_LARGURA,
                "texture_height": UV_ALTURA,
                # A caixa de visibilidade cobre o bicho com a cauda LEVANTADA no
                # `thrash`: apertada demais, ele sumiria da tela justo no quadro em
                # que o jogador esta decidindo se puxa ou acompanha.
                "visible_bounds_width": 3,
                "visible_bounds_height": 2.5,
                "visible_bounds_offset": [0, 1.25, 0],
            },
            "bones": [_osso_json(o) for o in OSSOS],
        }],
    }


def _achatar_numeros(texto):
    """Poe os vetores numericos numa linha so -- o JSON e gerado, mas e lido em review."""
    return re.sub(r"\[\s+([-\d.,\se]+?)\s*\]",
                  lambda m: "[" + ", ".join(m.group(1).replace(",", " ").split()) + "]",
                  texto)


def main():
    valida_ossos()
    valida_contrato()
    valida_pivots()
    valida_simetria()
    valida_uv()
    valida_faces_coplanares()
    valida_hitbox()
    valida_boca()
    valida_pernas()
    valida_peixe()

    texto = _achatar_numeros(json.dumps(geometria(), indent=2))
    json.loads(texto)  # o JSON gerado tem de continuar valido depois do achatamento

    destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                           "geo", "entity", "master_of_the_swamp.geo.json")
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    with open(destino, "w", encoding="utf-8", newline="\n") as f:
        f.write(texto + "\n")

    (x0, x1), (y0, y1), (z0, z1) = limites()
    ocupado = sum((2 * c.d + 2 * c.w) * (c.d + c.h) for c in CAIXAS)
    print("escrito", destino)
    print("ossos: %d   caixas: %d" % (len(OSSOS), len(CAIXAS)))
    print("caixa envolvente: x %s..%s  y %s..%s  z %s..%s (px)" % (x0, x1, y0, y1, z0, z1))
    print("bicho: %s larg x %s alt x %s comp   hitbox: %.1f x %.1f x %.1f"
          % (x1 - x0, y1 - y0, z1 - z0, HITBOX_LARGURA_PX, HITBOX_ALTURA_PX, HITBOX_LARGURA_PX))
    print("comprimento: %.2f bloco (%.0f%% da hitbox)"
          % ((z1 - z0) / 16.0, 100.0 * (z1 - z0) / HITBOX_LARGURA_PX))
    print("dente na cara: %d de %d px (%.0f%%, minimo %.0f%%)"
          % (area_frontal(DENTES), area_frontal(CARA),
             100.0 * area_frontal(DENTES) / area_frontal(CARA), 100 * DENTE_MINIMO_NA_CARA))
    print("atlas %dx%d: %d de %d px ocupados (%.0f%%)"
          % (UV_LARGURA, UV_ALTURA, ocupado, UV_LARGURA * UV_ALTURA,
             100.0 * ocupado / (UV_LARGURA * UV_ALTURA)))
    for o in OSSOS:
        cubos = [c.nome for c in CAIXAS if c.osso == o.nome]
        print("  %-18s pai=%-16s pivot=%-14s %s"
              % (o.nome, o.pai, o.pivot, ", ".join(cubos) if cubos else "(so transporta)"))


if __name__ == "__main__":
    main()
