"""Geometria do Avian Commander -- a formiga quimera que COMANDA DE CIMA (rank SQUADRON_LEADER).

O QUE O CORPO PRECISA CONTAR, e o que acontece se ele nao contar.

Este bicho e lido quase sempre CONTRA O CEU, de baixo para cima e a distancia: a
regra do servidor manda ela ficar a `AvianCommanderTuning.ALTITUDE_DE_COMANDO`
blocos do chao enquanto comanda, e so desce no mergulho. Tudo que a silhueta faz
existe para servir essa leitura.

  * A ENVERGADURA E DOMINANTE, E ELA MORA NO MODELO -- NAO NA HITBOX. A caixa de
    colisao e 1.4 x 2.0 (22.4 x 32 px), e o modelo PARADO cabe nela: a asa NASCE
    DOBRADA, com o braco saindo para o lado e a ponta voltando para tras, rente ao
    flanco. Quem ABRE a asa e a animacao, e a asa aberta chega a quase 2.4 blocos.
    Isso e o mesmo desenho da Spider Eagle, e pela mesma razao: uma envergadura
    presa na geometria faria a silhueta PARADA prometer um alcance que o servidor
    nao entrega, e quem calcula a distancia de recuo pelo desenho morre acertando
    a conta. Nada no repositorio reprova uma silhueta que mente --
    `valida_envergadura` reprova aqui.
  * A GARRA E A ARMA, E NAO A ASA. O mergulho e o unico golpe dela, e a caixa de
    dano do servidor (`AvianCommanderTuning.caixaDoMergulho`) vai a 0.95 bloco a
    frente. `valida_garra_alcanca_o_mergulho` cobra que a garra DESENHADA mais o
    impulso do mergulho cubram isso -- senao o jogador apanha de uma garra que, na
    tela, parou antes dele.
  * A MARCA DE QUIMERA E OBRIGATORIA, e aqui ela e ESTRUTURA e nao pintura: duas
    antenas segmentadas saindo do cranio, acima da crista. Uma ave com antenas nao
    e uma ave -- e e exatamente esse desconforto de meio segundo que separa uma
    formiga quimera de "um passaro grande" no instante em que o jogador olha para
    cima. Feita so de textura, ela sumiria no mip-map a dez blocos, que e a
    distancia em que este mob e visto o tempo todo.
  * O VENTRE E A FACE PRINCIPAL. Enquanto ela comanda, a unica superficie que o
    jogador ve e a de baixo -- e e por isso que o gerador de textura tem uma regua
    propria para ela (`valida_marca_de_quimera_le_de_baixo`).

A HITBOX MANDA NO MODELO. A entidade e sized(1.4F, 2.0F) -- 22.4 x 32 px -- e a
bateria da biblioteca reprova se o modelo PARADO estourar isso. O piso das garras
fica em y=0: ela pousa, mesmo que raramente.

A CAIXA DE VISIBILIDADE E DECLARADA, e nao deduzida. O padrao da biblioteca mede
o modelo PARADO e sobra meio bloco; com a asa aberta pela animacao, a ave sairia
dessa caixa e SUMIRIA da tela justamente no quadro do telegrafo. Isso nao da erro
-- da um mob que pisca quando a camera chega num certo angulo.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de AttackHitbox a frente e +Z, porque com yaw 0 o olhar
vanilla aponta para +Z. Misturar as duas ja custou um bug neste repositorio -- a
caixa do golpe ficou ATRAS do mob, que atacava, animava e nao encostava em
ninguem na frente. Ver o comentario de AvianCommanderTuning.caixaDoMergulho.

Regerar:  python art-source/enemies/avian_commander/avian_commander_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/avian_commander.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "avian_commander"
UV_LARGURA, UV_ALTURA = 128, 64

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(1.4F, 2.0F).
HITBOX = (1.4, 2.0)

# --------------------------------------------------- numeros COPIADOS do Java
# Cada um esta aqui para ser COBRADO contra o desenho, e cada um tem o campo de
# origem escrito ao lado. Duplicacao DECLARADA: a outra ponta e o
# AvianCommanderTuningTest, e as duas juntas formam um portao que morde dos dois
# lados -- quem encolher a garra reprova aqui, quem esticar a caixa reprova la.

# AvianCommanderTuning.caixaDoMergulho(): maxZ = 0.95D.
ALCANCE_DA_CAIXA_DO_MERGULHO = 0.95
# AvianCommanderTuning.IMPULSO_DO_MERGULHO = 0.65D.
IMPULSO_DO_MERGULHO = 0.65

# ------------------------------------------------------- limiares DO DESENHO

# Teto da envergadura EM REPOUSO, em multiplos da largura da hitbox.
#
# UM. Nao e botao de tuning: e o ponto em que a silhueta parada passa a prometer
# um alcance que a caixa de colisao nao tem. A Spider Eagle usa 1.6 porque ela e
# um bicho de 1.2 que ATACA de perto; esta aqui e maior (1.4 de hitbox) e o golpe
# dela e um mergulho vertical, entao a promessa lateral tem de ser exata.
ENVERGADURA_MAXIMA_EM_REPOUSO = 1.0

# Quanto a asa ABERTA tem de crescer sobre a dobrada para o voo ser um gesto, e
# nao um detalhe. Abaixo disso o clipe existe no arquivo e ninguem ve na tela --
# e num bicho que passa o encontro inteiro no ar, a asa e a silhueta.
ABERTURA_MINIMA = 1.6

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# Os dois encadeamentos que mais importam, e `valida_pivots` cobra os dois
# medindo:
#   * `wing_*` gira no OMBRO (x = +-5, na parede do torax);
#   * `wing_tip_*` gira na DOBRA (x = +-11, z = 1, o canto externo-traseiro do
#     braco). Trocar um pelo outro nao da erro: da uma asa que abre arrancando a
#     ponta do lugar, ou um braco que gira em torno do nada.
#
# A CAUDA PENDURA NO ABDOME, e nao no torax. Pendurada no torax o pai existe, o
# portao Java passa, e o leme fica PARADO enquanto o abdome se inclina no
# mergulho -- e a cauda inclinando com o corpo e o unico quadro que conta ao
# jogador que ela ja escolheu a linha de descida.
#
# NENHUM osso nasce com `rotation`. E decisao, nao esquecimento: a pose dobrada
# esta nas CAIXAS, e nao numa rotacao de bind. Com bind rotation, todo valor que a
# lane de animacao escrevesse seria somado a um angulo invisivel no
# .animation.json -- duas fontes para a mesma pose, decididas em silencio.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 15, 0)),               # centro do torax
    geo.Osso("abdomen", "body", (0, 14, 4)),            # a juncao com o torax
    geo.Osso("head", "body", (0, 20, -4)),              # pescoco curto: a cabeca senta no torax
    geo.Osso("beak", "head", (0, 22, -7)),              # raiz do bico, dentro do cranio
    geo.Osso("crest", "head", (0, 27, -4)),             # a crista de quitina
    geo.Osso("antenna_left", "head", (1.5, 27, -6.5)),  # A MARCA DE QUIMERA
    geo.Osso("antenna_right", "head", (-1.5, 27, -6.5)),
    geo.Osso("wing_left", "body", (5, 18, -1)),         # OMBRO
    geo.Osso("wing_tip_left", "wing_left", (11, 18, 1)),  # A DOBRA
    geo.Osso("wing_right", "body", (-5, 18, -1)),
    geo.Osso("wing_tip_right", "wing_right", (-11, 18, 1)),
    geo.Osso("tail", "abdomen", (0, 11.5, 9)),          # leme, na ponta do abdome
    geo.Osso("leg_left", "body", (2.5, 10, 0)),
    geo.Osso("talon_left", "leg_left", (2.5, 2, 0)),
    geo.Osso("leg_right", "body", (-2.5, 10, 0)),
    geo.Osso("talon_right", "leg_right", (-2.5, 2, 0)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
#
# As origens laterais e verticais sao deslocadas entre pecas vizinhas de
# proposito (o torax em -5, o abdome em -4, as pernas em 1.5). Isso NAO e
# capricho: duas caixas com a mesma face no mesmo plano cintilam em jogo, e o
# jogador le z-fighting como bug de video. `valida_faces_coplanares` reprova.
CAIXAS = (
    # --- tronco: o TORAX e a peca mais larga, e e nele que as asas nascem -----
    geo.Caixa("thorax", "body", 0, 0, -5, 10, -5, 10, 10, 9),
    # O ABDOME e o pedaco de FORMIGA: mais estreito, pendurado mais baixo (y=9
    # contra 10) e segmentado na textura. E ele que da o desconforto de silhueta.
    geo.Caixa("abdomen", "abdomen", 38, 0, -4, 9, 4, 8, 7, 5),

    # --- cabeca --------------------------------------------------------------
    geo.Caixa("head", "head", 64, 0, -3, 20, -7, 6, 7, 6),
    # O BICO projeta 3 px a frente do cranio. Ele e o que aponta a linha do
    # mergulho: sem projecao, de baixo a cabeca vira uma bola e o jogador perde a
    # direcao do golpe que esta descendo em cima dele.
    geo.Caixa("beak", "beak", 64, 13, -1, 21, -10, 2, 3, 3),
    # A crista de quitina: ela e a peca que fecha o alto da silhueta e separa esta
    # comandante de um oficial alado qualquer visto de longe.
    geo.Caixa("crest", "crest", 38, 12, -2, 27, -5, 4, 2, 4),
    # AS ANTENAS -- a marca de quimera, em GEOMETRIA. Finas (1 px) e altas (4 px),
    # a frente da crista. Ver valida_marca_de_quimera.
    geo.Caixa("antenna_left", "antenna_left", 54, 12, 1, 27, -7, 1, 4, 1),
    geo.Caixa("antenna_right", "antenna_right", 58, 12, -2, 27, -7, 1, 4, 1),

    # --- asas, em DUAS secoes, NASCIDAS DOBRADAS -----------------------------
    # O braco sai para o LADO (w=6 > d=4) e para na propria largura da hitbox.
    geo.Caixa("wing_left", "wing_left", 0, 19, 5, 17, -3, 6, 2, 4),
    geo.Caixa("wing_right", "wing_right", 20, 19, -11, 17, -3, 6, 2, 4),
    # A ponta volta para TRAS (d=8 > w=3), rente ao flanco. E ela que a animacao
    # abre, e e dela que sai a envergadura dominante.
    geo.Caixa("wing_tip_left", "wing_tip_left", 88, 0, 8, 17, 1, 3, 2, 8),
    geo.Caixa("wing_tip_right", "wing_tip_right", 88, 10, -11, 17, 1, 3, 2, 8),

    # --- leme ----------------------------------------------------------------
    # Leque CHATO e LARGO (7 x 1): cauda de ave e superficie, nao volume. Mais
    # larga que o abdome de proposito -- e o que faz a curva ler como curva quando
    # ela vira no alto, que e o unico movimento que o jogador tem para prever de
    # que lado o mergulho vem.
    geo.Caixa("tail", "tail", 60, 19, -3.5, 11, 9, 7, 1, 3),

    # --- pernas e garras -----------------------------------------------------
    # Pernas curtas e finas: ela nao corre, e uma perna de corredor prometeria uma
    # perseguicao no chao que o servidor nunca executa.
    geo.Caixa("leg_left", "leg_left", 20, 25, 1.5, 2, -1, 2, 8, 2),
    geo.Caixa("leg_right", "leg_right", 28, 25, -3.5, 2, -1, 2, 8, 2),
    # AS GARRAS sao a arma, e por isso projetam 6 px A FRENTE do centro do bicho:
    # e essa projecao que `valida_garra_alcanca_o_mergulho` mede contra a caixa de
    # dano do servidor.
    geo.Caixa("talon_left", "talon_left", 40, 19, 0.5, 0, -6, 4, 2, 6),
    geo.Caixa("talon_right", "talon_right", 0, 25, -4.5, 0, -6, 4, 2, 6),
)

# As pecas que o jogador ve DE BAIXO enquanto ela comanda. Declaradas aqui porque
# o gerador de textura precisa exatamente desta particao para cobrar a marca no
# ventre -- e uma segunda lista escrita la divergiria na primeira peca nova,
# deixando a regua vigiando uma face a menos sem que nada acusasse.
VENTRE_DE_COMANDO = ("thorax", "abdomen", "wing_left", "wing_right",
                     "wing_tip_left", "wing_tip_right", "tail")

ASAS = ("wing_left", "wing_right", "wing_tip_left", "wing_tip_right")
ANTENAS = ("antenna_left", "antenna_right")
GARRAS = ("talon_left", "talon_right")


# --------------------------------------------------- validacoes DO BICHO

def valida_garra_alcanca_o_mergulho(m):
    """A garra DESENHADA mais o impulso cobrem a caixa de dano do servidor.

    O servidor reivindica ALCANCE_DA_CAIXA_DO_MERGULHO blocos a frente, e paga a
    diferenca entre o desenho e a caixa com IMPULSO_DO_MERGULHO -- o corpo viaja
    para a frente durante a janela que machuca. O que sobra para o DESENHO cobrir
    e a subtracao dos dois.

    Se a garra desenhada nao chegar la, o jogador leva 15 de dano de uma garra
    que, na tela, parou antes dele. Nao da erro: da um mob com alcance invisivel,
    e num bicho cuja unica defesa e sair de baixo do mergulho, essa mentira apaga
    a licao inteira. O relato que chega e "ele as vezes acerta de longe".

    A outra ponta desta regua e AvianCommanderTuningTest, que mede a caixa contra
    estes mesmos numeros. Quem encolher a garra reprova aqui; quem esticar a caixa
    reprova la.
    """
    _, _, (z0, _) = geo.volume(m.caixa("talon_left"))
    alcance_px = -z0
    exigido_px = (ALCANCE_DA_CAIXA_DO_MERGULHO - IMPULSO_DO_MERGULHO) * 16.0
    if alcance_px < exigido_px:
        raise geo.ErroDeArte(
            "a garra alcanca %.1f px (%.2f blocos) a frente do centro e a caixa do mergulho exige "
            "%.1f px (%.2f blocos = caixa %.2f menos impulso %.2f): o jogador apanha de uma garra "
            "que parou antes dele, e nada acusa"
            % (alcance_px, alcance_px / 16.0, exigido_px, exigido_px / 16.0,
               ALCANCE_DA_CAIXA_DO_MERGULHO, IMPULSO_DO_MERGULHO))


def _envergadura_dobrada(m):
    """Largura, em px, que o modelo PARADO ocupa -- e ela e a promessa da silhueta."""
    (x0, x1), _, _ = m.limites()
    return x1 - x0


def _envergadura_aberta(m):
    """Largura com as pontas giradas 90 graus em torno da dobra.

    A ponta se estende de `pivot_z` ate `pivot_z + d` quando dobrada; girada um
    quarto de volta em torno do pivot da dobra, esse mesmo comprimento passa a
    sair LATERALMENTE a partir de `pivot_x`. A conta e a mesma dos dois lados, e
    por isso a envergadura aberta e o dobro.
    """
    ponta = m.caixa("wing_tip_left")
    pivot_x, _, pivot_z = m.osso("wing_tip_left").pivot
    _, _, (z0, z1) = geo.volume(ponta)
    comprimento = max(z1 - pivot_z, pivot_z - z0)
    return 2.0 * (pivot_x + comprimento)


def valida_envergadura(m):
    """A asa nasce DOBRADA e abre pela ANIMACAO -- e as duas pontas sao medidas.

    Este numero ja mordeu uma vez neste repositorio, no corpo emprestado da Spider
    Eagle: uma envergadura de 2.7 blocos sobre uma hitbox de 1.2. A silhueta
    MENTIA sobre o alcance, e quem julgasse a distancia de recuo pelo desenho
    morria com a conta certa. Nada no repositorio reprova uma silhueta que mente:
    ela nao levanta excecao, nao muda gametest, so mata gente.

    Aqui a regua morde dos dois lados:

      * asa que ja nasce ABERTA na geometria -- a silhueta parada volta a mentir,
        e o gesto de abrir deixa de existir porque nao ha o que abrir;
      * asa que abre de MENOS -- o clipe existe no arquivo de animacao e ninguem
        ve na tela, e num mob que passa o encontro inteiro no ar a asa e a
        silhueta.
    """
    dobrada = _envergadura_dobrada(m)
    aberta = _envergadura_aberta(m)
    teto = ENVERGADURA_MAXIMA_EM_REPOUSO * m.hitbox_largura_px
    if dobrada > teto:
        raise geo.ErroDeArte(
            "a asa nasce com %s px de envergadura e o teto em repouso e %.1f px (%.1f x a hitbox "
            "de %.1f px): asa aberta na GEOMETRIA nao volta a fechar, e a silhueta parada mente "
            "sobre a distancia de recuo"
            % (dobrada, teto, ENVERGADURA_MAXIMA_EM_REPOUSO, m.hitbox_largura_px))
    if aberta < ABERTURA_MINIMA * dobrada:
        raise geo.ErroDeArte(
            "a asa aberta da %.1f px contra %s px dobrada (%.2fx, minimo %.2fx): o voo existe no "
            "arquivo de animacao e nao aparece na tela"
            % (aberta, dobrada, aberta / dobrada, ABERTURA_MINIMA))
    # A PONTA e quem abre. Braco comprido com ponta curta e uma asa que ja nasce
    # quase aberta: passa no teto por pouco e perde o gesto.
    braco, ponta = m.caixa("wing_left"), m.caixa("wing_tip_left")
    if braco.w <= braco.d:
        raise geo.ErroDeArte(
            "o braco da asa (%s de largura x %s de profundidade) nao sai para o LADO: sem isso a "
            "asa dobrada vira um toco colado no flanco" % (braco.w, braco.d))
    if ponta.d <= ponta.w:
        raise geo.ErroDeArte(
            "a ponta da asa (%s de largura x %s de profundidade) nao volta para TRAS: ela ja nasce "
            "estendida e nao sobra o que abrir" % (ponta.w, ponta.d))


def valida_marca_de_quimera(m):
    """A marca de quimera e ESTRUTURA, e nao pintura.

    Duas antenas segmentadas saindo do cranio, acima da crista. Uma ave com
    antenas nao e uma ave, e e esse desconforto de meio segundo que separa uma
    formiga quimera de "um passaro grande" no instante em que o jogador olha para
    cima -- que e o unico instante que este mob oferece.

    Feita so de textura, a marca sumiria no mip-map a dez blocos, e dez blocos e a
    distancia em que este bicho e visto o tempo todo (a altitude de comando e
    sete). Isso nao da erro: da um mob que perde a identidade exatamente na
    distancia de trabalho dele.

    A regua cobra tres coisas: que as antenas existam, que sejam FINAS (grossas,
    leem como chifre e o bicho vira um bode alado) e que passem do topo da
    crista -- escondidas atras dela, o osso existe e ninguem ve.
    """
    _, (_, topo_da_crista), _ = geo.volume(m.caixa("crest"))
    for nome in ANTENAS:
        antena = m.caixa(nome)
        if antena.w > 1 or antena.d > 1:
            raise geo.ErroDeArte(
                "a antena '%s' tem secao %s x %s px: grossa assim ela le como chifre, e o bicho "
                "vira um bode alado em vez de uma formiga quimera" % (nome, antena.w, antena.d))
        _, (_, topo), _ = geo.volume(antena)
        if topo <= topo_da_crista:
            raise geo.ErroDeArte(
                "a antena '%s' termina em y=%s e a crista vai ate y=%s: a antena fica escondida "
                "atras da crista, o osso existe e ninguem ve -- e a marca de quimera, que e "
                "obrigatoria, deixa de existir na tela" % (nome, topo, topo_da_crista))


def valida_ventre_existe(m):
    """Toda peca do ventre de comando existe e tem volume.

    A lista VENTRE_DE_COMANDO e consumida pelo gerador de TEXTURA para cobrar onde
    a marca de quimera e pintada. Um nome errado aqui nao daria erro la -- daria
    uma `ErroDeArte` de caixa inexistente no meio da pintura, ou pior, uma regua
    que varre uma peca a menos porque alguem renomeou um cubo e esqueceu da lista.
    """
    for nome in VENTRE_DE_COMANDO:
        m.caixa(nome)


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX,
                    # Declarada, e nao deduzida: com a asa aberta pela animacao a ave
                    # sai da caixa padrao e SOME da tela no quadro do telegrafo.
                    bounds=(3.5, 3.0, [0, 1.5, 0]))

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_garra_alcanca_o_mergulho,
                          valida_envergadura,
                          valida_marca_de_quimera,
                          valida_ventre_existe))
