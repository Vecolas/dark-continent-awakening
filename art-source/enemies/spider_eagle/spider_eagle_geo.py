"""Gera a geometria autoral da Spider Eagle (bedrock 1.12.0, carregada pelo GeckoLib 4).

POR QUE ESTE ARQUIVO EXISTE. O ADR-017 diz que mob vanilla e andaime: a ave do
canion ja funciona -- o ninho nao anda quando ela e salva longe dele, o aviso vem
antes do bote, e quem recua deixa de ser alvo -- vestindo a geometria e a textura
do PHANTOM vanilla. Por isso ela parece pronta: os tres gametests passam e o log
nao diz nada. O unico sinal do contrario e alguem abrir o jogo e reconhecer um
phantom cinza defendendo um ninho de teia. Este gerador produz o corpo proprio.

Nao ha Blockbench aqui, entao a FONTE VERSIONADA e este Python: JSON gerado sem
gerador no git vira um arquivo que ninguem consegue corrigir depois.

ESTE MODULO E A UNICA TABELA DE CAIXAS. O gerador da textura importa CAIXAS daqui
de proposito: duas tabelas separadas divergem na primeira correcao de modelo, e a
divergencia nao da erro -- da face pintada no lugar errado, que so aparece na tela.

ESTA ENTREGA NAO MUDA COMPORTAMENTO NENHUM. Os numeros que o cliente enxerga
(windup 14 ticks, active 6, recovery 18; aviso a 16 blocos, bote a 6, coleira 28)
sao do servidor e continuam la; aqui so existe o corpo que os desenha.

A ENVERGADURA E O PONTO DELICADO DESTE MOB, E JA MORDEU UMA VEZ.
-----------------------------------------------------------------
O corpo emprestado do phantom tinha ~2,7 blocos de envergadura sobre uma hitbox
de 1,2. A silhueta MENTIA sobre o alcance: quem visse a ave chegando julgaria
errado a distancia de recuo -- e recuar a tempo e a unica coisa que este mob pede
do jogador. Nada no repositorio reprova uma silhueta que mente; ela nao levanta
excecao, nao muda gametest, so mata gente que fez a conta certa pelo desenho
errado. Por isso, aqui:

  * a asa NASCE DOBRADA. O braco (`wing_*`) sai para o lado e a ponta
    (`wing_tip_*`) volta para tras, rente ao flanco. Em repouso o bicho ocupa
    19 px -- a propria largura da hitbox (19,2 px). A silhueta parada promete
    exatamente o que a coleira entrega.
  * quem ABRE a asa e a ANIMACAO, nao a geometria. Girar `wing_tip_*` 90 graus em
    torno do pivot da dobra estende a ponta para fora e leva a envergadura a
    31 px (~1,94 bloco). Asa aberta e DISPLAY, e display e exatamente o aviso: ele
    aparece, e some quando a ave desiste.

`valida_envergadura` reprova os dois lados disso: asa que ja nasce aberta (o aviso
perde o gesto, e a silhueta volta a mentir) e asa que abre de menos (o gesto
existe no arquivo e ninguem ve).

A HITBOX MANDA NO MODELO. A entidade e sized(1.2F, 0.9F) -- 19,2 x 14,4 px -- e as
validacoes abaixo reprovam se o modelo PARADO estourar isso. O piso das garras
fica em y=0, porque ela pousa no ninho.

AS TRES COISAS QUE A SILHUETA PRECISA ENTREGAR (ficha, secao 40 do plano: "ave +
elementos aracnideos"), validadas aqui porque "ficou bonito" nao e criterio:

(a) E AVE. Corpo fusiforme -- torax mais largo que cabeca e que abdome --, bico
    projetado a frente do cranio, cauda em leque que serve de leme, e asa em DUAS
    secoes. `valida_ave` cobra cada um.

(b) E ARANHA. QUATRO pernas finas sob o corpo, cada uma terminando numa garra mais
    larga que a propria perna, e uma FIANDEIRA (`spinner`) pendurada abaixo do
    abdome -- e ela que faz o ninho de teia do canion. `valida_aranha` reprova
    perna grossa (vira pata de ave), garra que nao abre (vira toco) e fiandeira
    afundada no abdome (vira osso que a animacao move e ninguem ve). E a
    COMBINACAO que da o desconforto: nem passaro, nem aranha.

(c) ELA DEFENDE UM LUGAR, E O JOGADOR A VE DE BAIXO. Quando ela mergulha, o que
    chega na tela e a barriga: por isso o bico projeta a frente de tudo e as
    garras sao a coisa mais baixa do modelo, espalhadas para fora das pernas.
    `valida_visivel_de_baixo` reprova quem recolher qualquer um dos dois.

E HA UMA FALHA MUDA QUE SO ESTE GERADOR PEGA: duas caixas com a MESMA face no
MESMO plano (dois topos em y=11, dois flancos em x=4). Em jogo isso nao e erro, e
cintilacao -- o famoso "z-fighting", que o jogador le como bug de driver e que
nenhum portao do repositorio enxerga, porque o JSON continua perfeitamente valido.
`valida_faces_coplanares` reprova. Caixa que so ENCOSTA na vizinha (faces opostas
no mesmo plano) e permitida de proposito: isso e como todo modelo do jogo e feito.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

Regerar:  python art-source/enemies/spider_eagle/spider_eagle_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/spider_eagle.geo.json
"""
from collections import namedtuple
import json
import os
import re

IDENTIFICADOR = "geometry.spider_eagle"
UV_LARGURA, UV_ALTURA = 64, 64

# Limites que a entidade impoe: EnemyEntityTypes .sized(1.2F, 0.9F).
HITBOX_LARGURA_PX = 1.2 * 16.0
HITBOX_ALTURA_PX = 0.9 * 16.0

# Teto da envergadura EM REPOUSO, em multiplos da largura da hitbox. Nao e botao
# de tuning: e o ponto em que a silhueta parada passa a prometer um alcance que a
# coleira do ninho nao tem. Ver o cabecalho.
ENVERGADURA_MAXIMA_EM_REPOUSO = 1.6

# Quanto a asa ABERTA tem de crescer sobre a dobrada para o aviso ser um gesto, e
# nao um detalhe. Abaixo disso o clipe existe no arquivo e ninguem ve na tela.
ABERTURA_MINIMA = 1.4

Osso = namedtuple("Osso", "nome pai pivot rotacao")

# Hierarquia CONGELADA pelo contrato da entrega: as animacoes e o GeoModel
# escrevem contra estes nomes. Nome errado aqui nao da erro -- o GeckoLib so deixa
# o osso parado, e isso so aparece na tela.
#
# O pivot de um filho mora na JUNCAO com o pai, e `valida_pivots` cobra isso
# medindo: todo pivot cai DENTRO de um cubo do pai. Os dois que mais importam:
#   * `wing_*` gira no OMBRO (x=+-3,5, na parede do torax);
#   * `wing_tip_*` gira na DOBRA (x=+-9,5, z=1, o canto externo-traseiro do braco).
# Trocar um pelo outro nao da erro: da uma asa que abre arrancando a ponta do
# lugar, ou um braco que gira em torno do nada.
#
# NENHUM osso nasce com `rotation`. E decisao, nao esquecimento: a pose dobrada
# esta nas CAIXAS, e nao numa rotacao de bind. Com bind rotation, todo valor que a
# lane de animacao escrevesse seria somado a um angulo invisivel no .animation.json
# -- duas fontes para a mesma pose, decididas em silencio.
OSSOS = (
    Osso("root", None, (0, 0, 0), None),
    # centro do torax: e aqui que o corpo inteiro inclina no mergulho
    Osso("body", "root", (0, 9, -1), None),
    Osso("head", "body", (0, 10, -3), None),          # pescoco, na frente do torax
    Osso("beak", "head", (0, 10.5, -7), None),        # raiz do bico, dentro do cranio
    Osso("wing_left", "body", (3.5, 10, -1), None),   # OMBRO
    Osso("wing_tip_left", "wing_left", (9.5, 10, 1), None),    # A DOBRA
    Osso("wing_right", "body", (-3.5, 10, -1), None),
    Osso("wing_tip_right", "wing_right", (-9.5, 10, 1), None),
    Osso("tail", "body", (0, 8.5, 5), None),          # raiz do leque
    Osso("leg_front_left", "body", (3, 7, -2.5), None),
    Osso("talon_front_left", "leg_front_left", (3, 2, -2.5), None),
    Osso("leg_front_right", "body", (-3, 7, -2.5), None),
    Osso("talon_front_right", "leg_front_right", (-3, 2, -2.5), None),
    Osso("leg_back_left", "body", (2, 7, 4), None),
    Osso("talon_back_left", "leg_back_left", (2, 2, 4), None),
    Osso("leg_back_right", "body", (-2, 7, 4), None),
    Osso("talon_back_right", "leg_back_right", (-2, 2, 4), None),
    Osso("spinner", "body", (0, 6, 6), None),         # fim do abdome, por baixo
)

Caixa = namedtuple("Caixa", "nome osso u v x y z w h d")

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) px a partir de (u,v) -- as validacoes
# abaixo reprovam sobreposicao e estouro do atlas.
#
# Todo tamanho e INTEIRO de proposito, mesmo com origem em meio pixel. O portao
# Java arredonda `size` para calcular o retangulo do atlas; um size fracionario
# faria o portao medir um retangulo e este gerador medir outro, e as duas contas
# so discordariam no dia em que uma delas achasse sobreposicao.
CAIXAS = (
    # --- tronco fusiforme: o TORAX e a peca mais larga do bicho -------------
    # Ele carrega as asas e as pernas dianteiras. 8 px de largura contra 6 da
    # cabeca e 6 do abdome: e esse afunilamento nas duas pontas que le como ave.
    Caixa("thorax", "body", 0, 0, -4, 6, -4, 8, 6, 6),
    # O ABDOME e o pedaco ARACNIDEO: mais baixo que o torax (nasce em y=5, contra
    # 6), pendurado, e e dele que a fiandeira sai.
    Caixa("abdomen", "body", 28, 0, -3, 5, 1, 6, 6, 5),

    # --- cabeca -------------------------------------------------------------
    Caixa("head", "head", 0, 12, -3, 8, -8, 6, 5, 5),
    # O BICO. Projeta 2 px a frente do cranio e e a coisa mais a frente do modelo
    # inteiro -- e o que o jogador ve primeiro quando ela desce em cima dele.
    Caixa("beak", "beak", 0, 28, -1, 9, -10, 2, 3, 4),

    # --- asas, em DUAS secoes, NASCIDAS DOBRADAS ----------------------------
    # O braco sai para o LADO (w=6 > d=4) e para no ombro do proprio bicho.
    Caixa("wing_left", "wing_left", 0, 22, 3.5, 9, -3, 6, 2, 4),
    Caixa("wing_right", "wing_right", 20, 22, -9.5, 9, -3, 6, 2, 4),
    # A ponta volta para TRAS (d=6 > w=3), rente ao flanco, terminando antes do
    # leque da cauda. E ela que a animacao abre.
    Caixa("wing_tip_left", "wing_tip_left", 22, 12, 6.5, 9, 1, 3, 2, 6),
    Caixa("wing_tip_right", "wing_tip_right", 40, 12, -9.5, 9, 1, 3, 2, 6),

    # --- leme e fiandeira ---------------------------------------------------
    # Leque CHATO e LARGO (7 x 1): cauda de ave e superficie, nao volume. Ela e
    # mais larga que o abdome de proposito -- e o que faz a curva ler como curva.
    Caixa("tail", "tail", 40, 22, -3.5, 8, 5, 7, 1, 4),
    # A FIANDEIRA. Desce 2 px abaixo do abdome e sobra 1 px atras dele: e o unico
    # jeito de ela aparecer de baixo, que e de onde o jogador olha.
    Caixa("spinner", "spinner", 50, 0, -1.5, 3, 5, 3, 4, 2),

    # --- quatro pernas finas, sob o corpo -----------------------------------
    # 1 x 2 px de secao: finas o bastante para lerem como perna de aranha e nao
    # como coxa de ave. As dianteiras nascem no torax, as traseiras no abdome.
    Caixa("leg_front_left", "leg_front_left", 58, 12, 2.5, 1, -3.5, 1, 6, 2),
    Caixa("leg_front_right", "leg_front_right", 12, 28, -3.5, 1, -3.5, 1, 6, 2),
    Caixa("leg_back_left", "leg_back_left", 18, 28, 1.5, 1, 3, 1, 6, 2),
    Caixa("leg_back_right", "leg_back_right", 24, 28, -2.5, 1, 3, 1, 6, 2),

    # --- garras: a coisa mais baixa e a mais aberta do modelo ---------------
    # 3 px de largura contra 1 da perna. Elas ABREM, e e esse contorno dentado
    # contra o ceu que o jogador ve quando a ave passa por cima dele.
    Caixa("talon_front_left", "talon_front_left", 50, 6, 2, 0, -4, 3, 2, 3),
    Caixa("talon_front_right", "talon_front_right", 30, 28, -5, 0, -4, 3, 2, 3),
    Caixa("talon_back_left", "talon_back_left", 42, 28, 1, 0, 2.5, 3, 2, 3),
    Caixa("talon_back_right", "talon_back_right", 0, 36, -4, 0, 2.5, 3, 2, 3),
)

PERNAS = ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right")
GARRAS = ("talon_front_left", "talon_front_right", "talon_back_left", "talon_back_right")
ASAS = ("wing_left", "wing_right", "wing_tip_left", "wing_tip_right")


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


def envergadura_em_repouso():
    """Largura do bicho como ele NASCE: asa dobrada, que e a pose parada."""
    (x0, x1), _, _ = limites()
    return x1 - x0


def envergadura_aberta():
    """Largura com a ponta ESTENDIDA -- o que a animacao do aviso produz.

    A animacao gira `wing_tip_*` em torno do pivot da dobra ate a ponta, que
    nasce apontando para +Z, apontar para fora. O alcance da ponta e entao a
    distancia entre o pivot e o fim dela ao longo de Z; somada ao x do pivot, da
    a meia-envergadura aberta.
    """
    ponta = caixa("wing_tip_left")
    dobra = osso("wing_tip_left").pivot
    alcance = (ponta.z + ponta.d) - dobra[2]
    return 2 * (dobra[0] + alcance)


def largura_do_tronco():
    """Largura do bicho SEM as asas -- e ela que tem de caber na hitbox."""
    tronco = [c for c in CAIXAS if c.osso not in ASAS]
    (x0, x1), _, _ = limites(tronco)
    return x1 - x0


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
    # Osso sem cubo e osso que a animacao move e ninguem ve. Aqui TODOS tem
    # volume menos a raiz, que existe so para a ave inteira poder subir e descer.
    sem_cubo = [o.nome for o in OSSOS
                if o.nome != "root" and not any(c.osso == o.nome for c in CAIXAS)]
    if sem_cubo:
        raise ValueError("ossos sem volume proprio: %s -- a animacao os move e nada aparece"
                         % sem_cubo)
    # A asa tem de ser DUAS secoes encadeadas. Ponta pendurada no corpo, e nao no
    # braco, nao da erro: da uma asa que abre pela metade, com a ponta parada no
    # ar enquanto o braco gira embaixo dela.
    for ponta, braco in (("wing_tip_left", "wing_left"), ("wing_tip_right", "wing_right")):
        if osso(ponta).pai != braco:
            raise ValueError("'%s' devia pendurar em '%s' e pendura em '%s': sem as duas secoes "
                             "encadeadas a asa nao dobra nem abre" % (ponta, braco, osso(ponta).pai))
    for garra in GARRAS:
        perna = garra.replace("talon", "leg")
        if osso(garra).pai != perna:
            raise ValueError("'%s' devia pendurar em '%s' e pendura em '%s'"
                             % (garra, perna, osso(garra).pai))


def valida_pivots():
    """Pivot de filho mora na JUNCAO com o pai -- e junca se mede.

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
    unico defeito de modelo que nenhum portao do repositorio enxerga.

    Face OPOSTA no mesmo plano (uma caixa encostando na outra) e permitida: e
    assim que todo modelo do jogo e montado, e as normais contrarias resolvem a
    disputa sozinhas. O que se reprova aqui e MESMA direcao com area em comum.
    """
    eixos = (("x", 0), ("y", 1), ("z", 2))
    for eixo, indice in eixos:
        outros = [i for _, i in eixos if i != indice]
        for lado in (0, 1):  # 0 = face minima, 1 = face maxima
            faces = []
            for c in CAIXAS:
                v = volume(c)
                faces.append((c.nome, v[indice][lado],
                              v[outros[0]], v[outros[1]]))
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
    """O bicho PARADO cabe na hitbox. A asa aberta e o unico que pode sobrar."""
    (x0, x1), (y0, y1), (z0, z1) = limites()
    if y0 != 0:
        raise ValueError("o piso das garras tem de ficar em y=0, esta em %s" % y0)
    if y1 > HITBOX_ALTURA_PX:
        raise ValueError("modelo mais alto (%s) que a hitbox (%s)" % (y1, HITBOX_ALTURA_PX))
    if (x1 - x0) > HITBOX_LARGURA_PX:
        raise ValueError("a ave PARADA tem %s px de largura e a hitbox tem %s: a silhueta passa a "
                         "prometer um alcance que a coleira do ninho nao tem"
                         % (x1 - x0, HITBOX_LARGURA_PX))
    if (z1 - z0) > HITBOX_LARGURA_PX:
        raise ValueError("modelo de %s px de comprimento numa hitbox de %s px"
                         % (z1 - z0, HITBOX_LARGURA_PX))


def valida_envergadura():
    """A regua deste mob. Ver o cabecalho -- este numero ja mordeu uma vez."""
    dobrada = envergadura_em_repouso()
    aberta = envergadura_aberta()
    teto = ENVERGADURA_MAXIMA_EM_REPOUSO * HITBOX_LARGURA_PX
    if dobrada > teto:
        raise ValueError("a asa nasce com %s px de envergadura e o teto em repouso e %.1f px "
                         "(%.1f x a hitbox): asa aberta na GEOMETRIA nao volta a fechar, e a "
                         "silhueta parada mente sobre a distancia de recuo"
                         % (dobrada, teto, ENVERGADURA_MAXIMA_EM_REPOUSO))
    if dobrada <= largura_do_tronco():
        raise ValueError("a envergadura dobrada (%s px) nao passa do tronco (%s px): a asa sumiu "
                         "dentro do corpo e o bicho deixou de ser uma ave"
                         % (dobrada, largura_do_tronco()))
    if aberta < ABERTURA_MINIMA * dobrada:
        raise ValueError("a asa aberta da %s px contra %s px dobrada (%.2fx, minimo %.2fx): o "
                         "aviso existe no arquivo de animacao e nao aparece na tela"
                         % (aberta, dobrada, float(aberta) / dobrada, ABERTURA_MINIMA))
    # A ponta e quem abre. Braco comprido com ponta curta e uma asa que ja nasce
    # quase aberta -- passa no teste acima por pouco e perde o gesto.
    braco, ponta = caixa("wing_left"), caixa("wing_tip_left")
    if braco.w <= braco.d:
        raise ValueError("o braco da asa (%s x %s) nao sai para o LADO: sem isso a asa dobrada "
                         "vira um toco colado no flanco" % (braco.w, braco.d))
    if ponta.d <= ponta.w:
        raise ValueError("a ponta da asa (%s x %s) nao volta para TRAS: ela ja nasce estendida e "
                         "nao sobra o que abrir" % (ponta.w, ponta.d))


def valida_ave():
    """Corpo fusiforme, bico projetado, leque de leme. Sem isso e um bicho generico."""
    torax, abdome, cranio = caixa("thorax"), caixa("abdomen"), caixa("head")
    if torax.w <= cranio.w or torax.w <= abdome.w:
        raise ValueError("o torax (%s px) nao e a peca mais larga (cabeca %s, abdome %s): sem o "
                         "afunilamento nas duas pontas o corpo deixa de ser fusiforme"
                         % (torax.w, cranio.w, abdome.w))
    bico = caixa("beak")
    if bico.z >= cranio.z:
        raise ValueError("o bico (frente z=%s) nao projeta alem do cranio (z=%s): 'bico' vira uma "
                         "palavra no contrato" % (bico.z, cranio.z))
    leque = caixa("tail")
    if leque.w <= leque.h * 4:
        raise ValueError("a cauda tem %s x %s px: grossa demais para ler como leque, e leme e "
                         "superficie, nao volume" % (leque.w, leque.h))
    if leque.w <= abdome.w:
        raise ValueError("a cauda (%s px) nao e mais larga que o abdome (%s px): o leme some "
                         "atras do corpo e a curva deixa de ser legivel"
                         % (leque.w, abdome.w))


def valida_aranha():
    """QUATRO pernas finas, garras que abrem e uma fiandeira que aparece."""
    if len(PERNAS) != 4 or len(GARRAS) != 4:
        raise ValueError("sao quatro pernas e quatro garras -- e daqui que sai o lado aracnideo")
    torax = caixa("thorax")
    for nome in PERNAS:
        perna = caixa(nome)
        if perna.w > 2 or perna.d > 2:
            raise ValueError("a perna '%s' tem secao %s x %s px: grossa assim ela le como coxa de "
                             "ave, e o bicho perde o lado de aranha" % (nome, perna.w, perna.d))
        if perna.y + perna.h > torax.y + torax.h:
            raise ValueError("a perna '%s' sobe acima do torax: ela deixaria de ficar SOB o corpo"
                             % nome)
    for nome in GARRAS:
        garra = caixa(nome)
        perna = caixa(nome.replace("talon", "leg"))
        if garra.w <= perna.w:
            raise ValueError("a garra '%s' (%s px) nao e mais larga que a perna (%s px): sem abrir, "
                             "ela vira um toco e o contorno dentado some"
                             % (nome, garra.w, perna.w))
    abdome, fiandeira = caixa("abdomen"), caixa("spinner")
    if fiandeira.y >= abdome.y:
        raise ValueError("a fiandeira comeca em y=%s e o abdome em y=%s: afundada no corpo, ela "
                         "continua carregada, continua animada, e ninguem nunca a ve"
                         % (fiandeira.y, abdome.y))
    if fiandeira.z + fiandeira.d <= abdome.z + abdome.d:
        raise ValueError("a fiandeira acaba em z=%s, dentro do abdome (z=%s): ela tem de sobrar no "
                         "FIM do abdome, que e de onde a teia sai"
                         % (fiandeira.z + fiandeira.d, abdome.z + abdome.d))
    leque = caixa("tail")
    if fiandeira.y + fiandeira.h > leque.y:
        raise ValueError("a fiandeira (topo y=%s) entra na cauda (base y=%s): vista de baixo, o "
                         "leque passaria a esconder a unica peca que explica o ninho de teia"
                         % (fiandeira.y + fiandeira.h, leque.y))


def valida_visivel_de_baixo():
    """O jogador ve esta ave DE BAIXO -- e de baixo ela tem de parecer perigosa."""
    bico = caixa("beak")
    mais_a_frente = min(c.z for c in CAIXAS if c.nome != "beak")
    if bico.z >= mais_a_frente:
        raise ValueError("o bico comeca em z=%s e ha peca mais a frente dele (z=%s): quem esta "
                         "embaixo veria a barriga chegar antes da arma"
                         % (bico.z, mais_a_frente))
    for nome in GARRAS:
        garra = caixa(nome)
        if garra.y != 0:
            raise ValueError("a garra '%s' nao encosta no chao (y=%s): a ave nao pousaria no "
                             "proprio ninho, e de baixo a silhueta perderia o contorno dentado"
                             % (nome, garra.y))
    piso_do_corpo = min(c.y for c in CAIXAS if c.osso not in PERNAS + GARRAS)
    if piso_do_corpo <= 0:
        raise ValueError("ha corpo encostando no chao: as garras deixam de ser a coisa mais baixa "
                         "e o contorno visto de baixo vira um borrao")


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
                # A caixa de visibilidade acompanha a asa ABERTA, e nao a dobrada:
                # apertada demais, a ave sumiria da tela no quadro exato do aviso,
                # que e o unico quadro em que ela precisa ser vista.
                "visible_bounds_width": 3,
                "visible_bounds_height": 1.5,
                "visible_bounds_offset": [0, 0.75, 0],
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
    valida_pivots()
    valida_uv()
    valida_faces_coplanares()
    valida_hitbox()
    valida_envergadura()
    valida_ave()
    valida_aranha()
    valida_visivel_de_baixo()

    texto = _achatar_numeros(json.dumps(geometria(), indent=2))
    json.loads(texto)  # o JSON gerado tem de continuar valido depois do achatamento

    destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                           "geo", "entity", "spider_eagle.geo.json")
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    with open(destino, "w", encoding="utf-8", newline="\n") as f:
        f.write(texto + "\n")

    (x0, x1), (y0, y1), (z0, z1) = limites()
    ocupado = sum((2 * c.d + 2 * c.w) * (c.d + c.h) for c in CAIXAS)
    dobrada, aberta = envergadura_em_repouso(), envergadura_aberta()
    print("escrito", destino)
    print("ossos: %d   caixas: %d" % (len(OSSOS), len(CAIXAS)))
    print("caixa envolvente (asa dobrada): x %s..%s  y %s..%s  z %s..%s (px)"
          % (x0, x1, y0, y1, z0, z1))
    print("parada: %s px de largura x %s de altura x %s de comprimento  (hitbox %.1f x %.1f)"
          % (x1 - x0, y1, z1 - z0, HITBOX_LARGURA_PX, HITBOX_ALTURA_PX))
    print("tronco sem asas: %s px de largura" % largura_do_tronco())
    print("envergadura DOBRADA: %s px (%.2f bloco, %.2fx a hitbox)"
          % (dobrada, dobrada / 16.0, dobrada / HITBOX_LARGURA_PX))
    print("envergadura ABERTA:  %s px (%.2f bloco, %.2fx a hitbox, %.2fx a dobrada)"
          % (aberta, aberta / 16.0, aberta / HITBOX_LARGURA_PX, float(aberta) / dobrada))
    print("atlas %dx%d: %d de %d px ocupados (%.0f%%)"
          % (UV_LARGURA, UV_ALTURA, ocupado, UV_LARGURA * UV_ALTURA,
             100.0 * ocupado / (UV_LARGURA * UV_ALTURA)))


if __name__ == "__main__":
    main()
