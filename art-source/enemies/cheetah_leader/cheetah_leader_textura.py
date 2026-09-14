"""Folha do Cheetah Leader -- pelagem de savana, e uma placa que NAO e pelagem.

A DECISAO DE LEITURA DESTA FOLHA CABE NUMA FRASE: **o jogador tem de reconhecer
uma FORMIGA QUIMERA antes de reconhecer um felino.** Tudo aqui serve a isso.

Um guepardo pintado com pelo malhado e nada mais le como guepardo -- e um
guepardo comum nao explica por que ele lidera um esquadrao, por que ele nasce de
uma colonia nem por que ele aparece no bestiario ao lado de um escorpiao de dois
metros. A colonia inteira perde identidade visual quando UM dos bichos dela
parece fauna local, porque o jogador deixa de ter uma pista comum para agrupa-los.

Por isso a folha tem TRES campos, e nao dois:

* o pelo malhado -- o que diz "felino", em tons quentes (vermelho acima de azul);
* o ventre claro -- o contra-sombreado que da volume a um corpo de 23 px;
* a QUITINA da carapaca -- o unico campo FRIO (azul acima de vermelho) do bicho.

O terceiro e a marca de quimera, e ele e cobrado por
`valida_marca_de_quimera`: area minima e um degrau de luminancia contra o dorso.
Nao e capricho de paleta -- a quitina pintada em tom de pelo nao da erro, nao
aparece no atlas aberto num editor, e o sintoma e "esse mob parece um bicho
normal", que ninguem reporta como bug.

A LAGRIMA E O UNICO PRETO DA CARA, E ELA TEM FUNCAO. A faixa escura que desce do
olho ate o focinho e o que amarra os dois olhos num PAR a dez blocos; sem ela o
amarelo da iris flutua no meio do pelo malhado e as manchas competem com os olhos
pelo mesmo contraste. Um bicho cujo rosto nao tem foco nao "olha" para o jogador,
e este aqui precisa olhar: ele e o que chega primeiro.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

UMA VARIANTE SO, E ISSO TEM UM CUSTO DECLARADO. Nao ha folha distinta para a
formiga que foi PROMOVIDA a lider depois que este guepardo morre: `Squad` promove
o proximo, e o jogador nao tem como ver que a promocao aconteceu. A mudanca e
real no servidor e invisivel na tela. Isso esta escrito aqui, e nao resolvido
aqui: uma segunda folha e uma decisao de escopo, nao um retoque de pincel.

Regerar:  python art-source/enemies/cheetah_leader/cheetah_leader_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/cheetah_leader/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import geometria as geo      # noqa: E402
from comum import textura as tex        # noqa: E402
from cheetah_leader_geo import (CAIXAS, MOB, UV_ALTURA, UV_LARGURA)  # noqa: E402

# ------------------------------------------------------ limiares de LEITURA
#
# Nenhum destes tres vai para o Java, e a ausencia e de proposito: eles medem a
# FOLHA, e nao ha consumidor Java para nenhum deles. Declarados la, seriam
# constantes orfas -- o numero que foi para a config e nunca voltou.

# Quanto a luminancia media do VENTRE tem de superar a do DORSO.
#
# 1.7 e um degrau que se enxerga a dez blocos. Com 1.15 os dois campos empatam a
# distancia e a silhueta vira um vulto; com 3.0 o ventre teria de ser quase
# branco, e um felino de barriga branca no meio do mato le como animal
# domestico -- nao como o que chega primeiro.
DEGRAU_DE_VENTRE = 1.7

# Quanto a luminancia media da QUITINA tem de se afastar da do DORSO, em valor
# absoluto (0..255). Vinte e o ponto em que o olho ainda separa os dois campos a
# dez blocos; abaixo disso a placa vira uma mancha grande de pelo e a marca de
# quimera deixa de existir sem que nada acuse.
DEGRAU_DA_QUITINA = 20.0

# Fracao MINIMA da area de atlas do tronco que a carapaca tem de ocupar.
#
# Marca pequena demais para ser vista a dez blocos e marca que so existe no
# atlas. 0.12 e um oitavo do tronco -- o suficiente para a placa quebrar a linha
# do dorso na silhueta, que e onde ela trabalha.
FRACAO_MINIMA_DA_MARCA = 0.12

# As pecas do TRONCO com pelagem -- as unicas que tem dorso e ventre de verdade.
#
# `body` fica de FORA de proposito, e a razao e geometrica: o topo do tronco
# esta coberto pela carapaca no modelo 3D. Medi-lo faria a regua avaliar uma face
# que ninguem ve, e um desenho poderia passar (ou reprovar) por causa de pixels
# que o jogo nunca desenha.
PECAS_COM_PELAGEM = ("head", "tail", "tail_tip")

# As pecas que formam o tronco para efeito de AREA. Aqui `body` entra: a conta e
# de proporcao de superficie, e a superficie do tronco existe mesmo quando parte
# dela esta coberta.
PECAS_DO_TRONCO = ("body", "head", "carapaca")

# As faces que recebem malha. A `base` fica de fora: o ventre e limpo em felino
# malhado, e salpicar a base derrubaria a media que `valida_degrau_de_dorso_e_ventre`
# mede -- a regua passaria a comparar dois campos malhados e aprovaria qualquer
# desenho.
FACES_MALHADAS = ("topo", "frente", "tras", "direita", "esquerda")

PALETA = tex.Paleta(
    PELO=(176, 146, 96),          # o flanco, visto de frente
    PELO_DORSO=(142, 114, 72),    # o que se ve de cima
    PELO_VENTRE=(222, 208, 178),  # o que se ve de baixo e as patas
    MANCHA=(58, 44, 32),          # a malha: o padrao que diz "felino de savana"
    QUITINA=(48, 60, 76),         # a placa: o UNICO campo frio do bicho
    QUITINA_BRILHO=(104, 122, 138),  # a crista da placa, onde a luz bate
    LAGRIMA=(28, 24, 22),         # a faixa do olho ao focinho, e o nariz
    OLHO=(226, 176, 60),          # a iris: o unico amarelo do bicho
    PUPILA=(18, 16, 16),          # o miolo: o ponto mais escuro da folha
    GARRA=(214, 206, 186),        # garras e presas -- o unico claro fora do ventre
    COXIM=(42, 36, 34),           # as almofadas das patas: contato com o chao
)

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def _pelo(p, salpico, ventre=True):
    """O tratamento comum de toda peca coberta de pelagem.

    Topo escuro e base clara: e o contra-sombreado que todo predador de campo
    aberto tem, e aqui ele nao e naturalismo -- e a unica coisa que da VOLUME a
    um corpo de 23 px visto contra o capim.

    `salpico` e a semente. Semente repetida entre duas pecas faria as duas
    receberem a MESMA malha, e um padrao repetido le como textura de parede, nao
    como pelo.
    """
    p.tudo(PALETA.PELO)
    p.face("topo", PALETA.PELO_DORSO)
    p.face("tras", PALETA.PELO_DORSO)
    if ventre:
        p.face("base", PALETA.PELO_VENTRE)
    for face in FACES_MALHADAS:
        # Duas camadas com sementes diferentes: uma de malha e uma de sombra. Uma
        # camada so produz pontos de tamanho unico, que a distancia leem como
        # chuvisco de televisao em vez de pelagem malhada.
        p.salpicar(face, PALETA.MANCHA, 42, semente=salpico)
        p.salpicar(face, PALETA.PELO_DORSO, 26, semente=salpico + 7)


def pintar():
    # -------------------------------------------------------------- tronco
    corpo = folha["body"]
    _pelo(corpo, 3)
    # O ventre claro sobe pelos flancos. Pintado so na base, ele desapareceria de
    # perfil -- e perfil e como se ve um guepardo que esta CORRENDO, que e a
    # pose em que este bicho passa a maior parte do encontro.
    for face in tex.LADOS:
        corpo.faixa_no_pe(face, 2, PALETA.PELO_VENTRE)
    corpo.faixa_no_pe("frente", 2, PALETA.PELO_VENTRE)

    # ------------------------------------------------ a marca de quimera
    placa = folha["carapaca"]
    # `tudo` com forca baixa: quitina e lisa. O ruido que faz pelo parecer pelo
    # faz placa parecer pedra porosa, e pedra porosa nao le como casca de inseto.
    placa.tudo(PALETA.QUITINA, forca=2)
    # A crista, com forca=0. Ela e uma linha reta de UM pixel, e ruido numa linha
    # de um pixel a apaga: o pixel claro vira um pixel medio e a crista some.
    placa.linha_central("topo", 1, PALETA.QUITINA_BRILHO, forca=0)
    placa.linha("frente", 0, PALETA.QUITINA_BRILHO, forca=0)
    placa.linha("tras", 0, PALETA.QUITINA_BRILHO, forca=0)
    for face in tex.LADOS:
        placa.linha(face, 0, PALETA.QUITINA_BRILHO, forca=0)

    # -------------------------------------------------------------- cabeca
    cabeca = folha["head"]
    _pelo(cabeca, 19)
    # A mascara escura em volta dos olhos. Ela existe para os olhos nao flutuarem
    # no meio de um campo malhado: sem ela, o amarelo perde o contorno e o par de
    # pontos deixa de ler como um PAR -- vira mais duas manchas.
    cabeca.na_face("frente", 0, 1, 5, 1, PALETA.PELO_DORSO, forca=2)
    # Cada olho tem DOIS pixels: iris fora, pupila dentro. Um pixel so daria um
    # ponto amarelo chapado, e ponto chapado nao tem direcao -- o bicho pareceria
    # olhar para lugar nenhum. Com a pupila na coluna interna, os dois olhos
    # convergem para a frente, que e para onde ele corre.
    #
    # A ordem importa: a iris e pintada primeiro e a pupila por cima. Invertida,
    # a iris cobriria a pupila e os olhos ficariam chapados de amarelo -- sem
    # erro nenhum, so sem foco.
    cabeca.na_face("frente", 0, 1, 2, 1, PALETA.OLHO, forca=0)
    cabeca.na_face("frente", 3, 1, 2, 1, PALETA.OLHO, forca=0)
    cabeca.ponto("frente", 1, 1, PALETA.PUPILA)
    cabeca.ponto("frente", 3, 1, PALETA.PUPILA)
    # A LAGRIMA desce da borda EXTERNA de cada olho ate a linha da boca. Duas
    # fiadas, em coluna. Ela e o que amarra os dois olhos num par a dez blocos:
    # sem ela o amarelo flutua no meio da malha e as manchas competem com os
    # olhos pelo mesmo contraste.
    for dx in (0, 4):
        cabeca.ponto("frente", dx, 2, PALETA.LAGRIMA)
        cabeca.ponto("frente", dx, 3, PALETA.LAGRIMA)
    # A garganta clara, para o ventre nao comecar so no peito.
    cabeca.faixa_no_pe("frente", 1, PALETA.PELO_VENTRE)

    # ----------------------------------------------------------- focinho
    bico = folha["snout"]
    bico.tudo(PALETA.PELO)
    bico.face("topo", PALETA.PELO_DORSO)
    bico.face("base", PALETA.PELO_VENTRE)
    # O nariz preto: a face da FRENTE inteira. E o que fecha a silhueta da
    # cabeca; ponta clara faria o focinho se dissolver no capim seco.
    bico.face("frente", PALETA.LAGRIMA, forca=0)
    # As presas, na fiada de baixo das duas faces laterais. Na base elas nunca
    # seriam vistas, e o desenho teria custado a mesma area de atlas para nada.
    for face in tex.LADOS:
        bico.ponto(face, 0, 1, PALETA.GARRA)

    # ------------------------------------------------------------- orelhas
    for nome in ("ear_left", "ear_right"):
        orelha = folha[nome]
        orelha.tudo(PALETA.PELO_DORSO)
        # O interior claro, na face que aponta para a frente. Orelha chapada
        # escura some contra a placa, e as orelhas sao o que diz para que lado a
        # cabeca esta virada quando o corpo ja passou de perfil.
        orelha.na_face("frente", 0, 1, 1, 1, PALETA.PELO_VENTRE, forca=0)

    # -------------------------------------------------------------- cauda
    base_da_cauda = folha["tail"]
    _pelo(base_da_cauda, 43)
    ponta = folha["tail_tip"]
    _pelo(ponta, 53)
    # A ponta ANELADA: duas fiadas escuras na ponta da cauda. E o sinal mais alto
    # do bicho visto por tras, e a unica peca que continua legivel quando ele ja
    # virou as costas -- que e exatamente o que acontece quando a fadiga o obriga
    # a desengajar.
    for face in tex.PAREDES:
        ponta.faixa_no_pe(face, 1, PALETA.MANCHA)
    ponta.face("tras", PALETA.MANCHA, forca=2)

    # ------------------------------------------------------ pernas e patas
    for nome, semente in (("leg_front_left", 61), ("leg_front_right", 67),
                          ("leg_back_left", 71), ("leg_back_right", 73)):
        _pelo(folha[nome], semente)

    for nome, semente in (("paw_front_left", 79), ("paw_front_right", 83),
                          ("paw_back_left", 89), ("paw_back_right", 97)):
        pata = folha[nome]
        # As patas sao o campo CLARO por inteiro: e por elas que a leitura de
        # baixo funciona, e elas ficam fora da regua do ventre justamente porque
        # a base delas e o coxim preto -- contato com o chao, nao
        # contra-sombreado. Incluidas na regua, puxariam a media do ventre para
        # baixo e reprovariam um desenho correto.
        pata.tudo(PALETA.PELO_VENTRE)
        pata.face("base", PALETA.COXIM, forca=0)
        for face in tex.PAREDES:
            pata.salpicar(face, PALETA.PELO, 30, semente=semente)
        # Duas garras na FRENTE de cada pata. Garra de guepardo nao recolhe, e
        # e por isso que ela aparece na folha em vez de so no nome do trait.
        for dx in (0, 1):
            pata.ponto("frente", dx, 0, PALETA.GARRA)


# ------------------------------------------ validacoes que ligam arte e regra

def _luminancia(pixel):
    """Luminancia perceptual, a mesma conta que o olho humano faz de longe.

    Media simples dos canais mediria ERRADO: (0,255,0) e (255,0,0) tem a mesma
    media e brilhos completamente diferentes, e a regua aprovaria um verde
    berrante como se fosse tao discreto quanto um vermelho escuro.
    """
    return 0.299 * pixel[0] + 0.587 * pixel[1] + 0.114 * pixel[2]


def _media_da_face(f, nomes, face):
    """Luminancia media e canais medios de UMA face de cada caixa citada.

    Uma face, e nao a caixa inteira. Medir as seis misturaria o dorso com o
    ventre dentro da mesma media, e a regua passaria a comparar duas medias que
    ja contem as duas coisas -- ou seja, aprovaria qualquer desenho, inclusive um
    bicho chapado. Cantos do layout que nenhuma face usa ficam de fora por
    construcao: eles sao transparentes, e medi-los puxaria toda media ao preto.

    @return (luminancia_media, vermelho_medio, azul_medio)
    """
    luz, vermelho, azul = [], [], []
    for nome in nomes:
        x, y, largura, altura = f[nome].faces()[face]
        for i in range(x, x + largura):
            for j in range(y, y + altura):
                pixel = f.px[i, j][:3]
                luz.append(_luminancia(pixel))
                vermelho.append(pixel[0])
                azul.append(pixel[2])
    if not luz:
        raise geo.ErroDeArte("nenhum pixel medido na face '%s' de %s: a regua nao mediu nada e "
                             "ficaria verde" % (face, list(nomes)))
    n = float(len(luz))
    return sum(luz) / n, sum(vermelho) / n, sum(azul) / n


def valida_degrau_de_dorso_e_ventre(f):
    """O ventre tem de ser DEGRAU_DE_VENTRE vezes mais claro que o dorso.

    Este bicho passa o encontro inteiro de PERFIL, correndo. A unica coisa que
    sobrevive a dez blocos nessa pose e o contraste entre o campo escuro de cima
    e o campo claro de baixo: e ele que faz o corpo ler como um animal com volume
    em vez de uma mancha que desliza.

    Empatar os dois campos nao da erro nenhum, nao aparece no atlas aberto num
    editor, e nao reprova nenhum portao Java. O sintoma e o jogador nao conseguir
    julgar a DISTANCIA do bicho -- e distancia e a decisao inteira deste
    encontro, porque e dela que sai a escolha entre recuar e punir a fadiga.
    """
    dorso, _, _ = _media_da_face(f, PECAS_COM_PELAGEM, "topo")
    ventre, _, _ = _media_da_face(f, PECAS_COM_PELAGEM, "base")
    if ventre < dorso * DEGRAU_DE_VENTRE:
        raise geo.ErroDeArte(
            "o dorso tem luminancia media %.1f e o ventre %.1f; o exigido e %.1f (%.1fx). Com os "
            "dois campos empatados o guepardo vira um vulto a dez blocos, e o jogador nao consegue "
            "julgar a distancia -- que e de onde sai a escolha entre recuar e punir a fadiga"
            % (dorso, ventre, dorso * DEGRAU_DE_VENTRE, DEGRAU_DE_VENTRE))
    print("degrau: ventre %.1f  vs  dorso %.1f  (razao %.2fx, minimo %.2fx)"
          % (ventre, dorso, ventre / dorso, DEGRAU_DE_VENTRE))


def valida_marca_de_quimera(f):
    """A carapaca tem de ser grande, fria e clara/escura o bastante para APARECER.

    O bicho e uma formiga quimera (`ChimeraProfiles.cheetahLeader()` publica
    `EnemyFaction.CHIMERA_ANT`), e a faccao e uma regra de servidor: ela decide
    quem ele ataca, com quem ele forma esquadrao e em que pagina do bestiario ele
    entra. Nada disso aparece na tela. O que aparece e a placa.

    Uma placa pintada em tom de pelo nao da erro: o mod carrega, o mob nasce, a
    faccao continua certa, e o jogador le "um guepardo". Tres medidas fecham
    isso, e as tres reprovam por razoes diferentes:

    1. AREA -- a placa ocupa pelo menos FRACAO_MINIMA_DA_MARCA da superficie do
       tronco. Menor que isso ela nao quebra a linha do dorso na silhueta, e
       silhueta e o que sobrevive a dez blocos;
    2. DEGRAU DE LUMINANCIA -- a placa se afasta do dorso em pelo menos
       DEGRAU_DA_QUITINA. Empatada, ela vira uma sela de pelo;
    3. TEMPERATURA -- a placa e FRIA (azul acima de vermelho) e a pelagem e
       QUENTE (vermelho acima de azul). Esta e a medida que pega o caso que as
       outras duas deixam passar: uma placa marrom-escura tem area e tem degrau, e
       ainda assim le como couro molhado. Quitina nao e couro, e a diferenca esta
       no matiz, nao no brilho.
    """
    area_da_marca = sum(_area_no_atlas(c) for c in CAIXAS if c.nome == "carapaca")
    area_do_tronco = sum(_area_no_atlas(c) for c in CAIXAS if c.nome in PECAS_DO_TRONCO)
    fracao = area_da_marca / float(area_do_tronco)
    if fracao < FRACAO_MINIMA_DA_MARCA:
        raise geo.ErroDeArte(
            "a carapaca ocupa %.1f%% da superficie do tronco e o minimo e %.1f%%: uma marca desse "
            "tamanho nao quebra a linha do dorso na silhueta, e o bicho volta a ler como guepardo "
            "comum -- a colonia perde a pista visual que agrupa as formigas"
            % (100.0 * fracao, 100.0 * FRACAO_MINIMA_DA_MARCA))

    quitina, quitina_r, quitina_b = _media_da_face(f, ("carapaca",), "topo")
    dorso, dorso_r, dorso_b = _media_da_face(f, PECAS_COM_PELAGEM, "topo")
    if abs(quitina - dorso) < DEGRAU_DA_QUITINA:
        raise geo.ErroDeArte(
            "a quitina tem luminancia media %.1f e o dorso %.1f -- diferenca de %.1f, abaixo do "
            "minimo de %.1f: a placa empata com o pelo e vira uma sela, nao uma carapaca"
            % (quitina, dorso, abs(quitina - dorso), DEGRAU_DA_QUITINA))
    if quitina_b <= quitina_r:
        raise geo.ErroDeArte(
            "a quitina tem vermelho medio %.1f e azul medio %.1f: ela e QUENTE, e placa quente le "
            "como couro molhado sobre o lombo. Casca de inseto e fria -- azul acima de vermelho -- "
            "e e o matiz, e nao o brilho, que separa as duas coisas a dez blocos"
            % (quitina_r, quitina_b))
    if dorso_r <= dorso_b:
        raise geo.ErroDeArte(
            "o dorso tem vermelho medio %.1f e azul medio %.1f: a pelagem ficou FRIA, e ai a placa "
            "deixa de contrastar com ela por temperatura -- as duas viram o mesmo material e a "
            "marca de quimera some sem nada acusar" % (dorso_r, dorso_b))
    print("marca: %.1f%% do tronco  |  quitina %.1f vs dorso %.1f (degrau %.1f, minimo %.1f)"
          % (100.0 * fracao, quitina, dorso, abs(quitina - dorso), DEGRAU_DA_QUITINA))


def _area_no_atlas(c):
    """Area de superficie da caixa, em px. Mesma conta de `geo.area_no_atlas`.

    Usa o retangulo do layout de caixa e nao a soma das seis faces de proposito:
    e o retangulo que a biblioteca mede, e usar duas contas diferentes para a
    mesma grandeza faria as duas so discordarem no dia em que uma delas achasse
    um problema.
    """
    x0, y0, x1, y1 = geo.area_no_atlas(c)
    return (x1 - x0) * (y1 - y0)


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_degrau_de_dorso_e_ventre, valida_marca_de_quimera))
