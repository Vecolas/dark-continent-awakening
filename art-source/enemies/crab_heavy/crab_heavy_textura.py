"""Folha do Crab Heavy -- placa fria em cima, ventre claro embaixo e atras.

A DECISAO DE LEITURA DESTA FOLHA CABE NUMA FRASE: **o ventre e a unica regiao
clara do bicho.** Carapaca, pincas, pernas e cabeca vivem numa faixa estreita de
luminancia baixa; o ventre salta dela inteiro, e nada mais na folha chega perto.

Isso nao e gosto, e a regra do servidor desenhada. `RegrasDeCarapacaOrientada`
cobra armadura cheia de quem bate de frente e quase nenhuma de quem acerta pelas
costas. O servidor sabe cobrar essa diferenca; o que ele nao consegue fazer e
CONTAR ao jogador que ela existe. Quem conta e esta folha: o jogador circula o
bicho, ve a mancha clara aparecer atras da placa escura e aprende onde bater --
sem tutorial, sem texto, sem uma linha de log.

Um segundo claro na folha -- uma junta pintada, um olho branco, uma faixa de
aviso na carapaca -- rouba o olhar e o ventre deixa de ser o que se acha
primeiro. Isso NAO da erro nenhum, nao aparece no atlas aberto num editor, e so
se manifesta como "esse bicho e confuso de ler". Por isso
`valida_ventre_e_o_unico_claro` mede a folha inteira e reprova quem empatar com
o ventre.

O OLHO E ESCURO DE PROPOSITO, e e a mesma decisao pelo avesso: o Cyclops usa o
olho como unico contraste alto porque o ponto fraco dele e o olho. Aqui o ponto
fraco e o ventre, e um olho claro competiria com ele. A folha so tem um lugar
para onde o olhar vai primeiro, e ele tem de ser o lugar que a regra paga.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

Regerar:  python art-source/enemies/crab_heavy/crab_heavy_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/crab_heavy/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import geometria as geo      # noqa: E402
from comum import textura as tex        # noqa: E402
from crab_heavy_geo import CAIXAS, MOB, UV_ALTURA, UV_LARGURA  # noqa: E402

# Quantos pontos de luminancia o ventre tem de estar ACIMA de tudo o mais.
#
# 40 e um degrau que se enxerga a dez blocos, no escuro de um ninho, com o bicho
# em movimento -- que e a condicao real em que o jogador precisa achar o alvo.
# Com 15 as duas regioes disputam o olhar e a leitura fica ambigua; com 90 o
# resto do corpo teria de ser quase preto, e preto chapado le como buraco e nao
# como carapaca.
#
# E um limite de LEITURA, e nao de balanceamento: girar este numero nao muda dano
# nenhum. Por isso ele mora aqui, ao lado do comentario que o explica.
DEGRAU_MINIMO_DE_LEITURA = 40.0

PALETA = tex.Paleta(
    # --- a placa: fria, escura, sem variacao de matiz ---
    CARAPACA=(54, 62, 74),        # o dorso, visto de cima e de frente
    CARAPACA_LUZ=(70, 80, 92),    # as cristas que pegam luz
    CARAPACA_SOMBRA=(36, 42, 52),  # a aba por baixo e a traseira da placa
    # --- as pincas: mesma familia da placa, porque elas TAMBEM sao armadura ---
    PINCA=(64, 68, 74),
    PINCA_SOMBRA=(44, 48, 54),
    # --- quitina comum: pernas, cabeca, o corpo escondido sob a placa ---
    QUITINA=(86, 76, 64),
    QUITINA_SOMBRA=(60, 52, 44),
    # --- o ventre: os UNICOS tons claros da folha ---
    VENTRE=(198, 172, 130),        # as placas do abdomen
    VENTRE_SOMBRA=(166, 142, 104),  # as juntas entre elas
    # --- o olho, escuro: ele nao pode disputar o olhar com o ventre ---
    OLHO=(26, 24, 28),
)

# O ruido do ventre e mais fraco que o do resto (4 contra 6) de proposito: o
# ruido desloca a luminancia pixel a pixel, e e o pixel MAIS ESCURO do ventre que
# a regua compara com o resto da folha. Ruido forte no ventre estreitaria o
# degrau sem mudar uma cor sequer.
FORCA_DO_VENTRE = 4

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def pintar():
    # ------------------------------------------------------------ carapaca
    placa = folha["carapaca"]
    placa.tudo(PALETA.CARAPACA)
    placa.face("topo", PALETA.CARAPACA_LUZ)
    placa.face("base", PALETA.CARAPACA_SOMBRA)
    placa.face("tras", PALETA.CARAPACA_SOMBRA)
    # Tres cristas correndo da frente para tras. Elas sao COLUNAS na face do topo
    # porque a coluna dessa face corre no eixo da profundidade; salpico no lugar
    # delas leria como sujeira, e crista transversal leria como segmento de
    # lagarta -- e este bicho precisa ler como UMA placa inteirica.
    for dx in (3, 11, 18):
        placa.coluna("topo", dx, PALETA.CARAPACA_LUZ, forca=2)
    # A aba: uma fiada escura no pe das quatro paredes. E o que faz a placa
    # parecer sobreposta ao corpo em vez de ser o corpo.
    for face in tex.PAREDES:
        placa.faixa_no_pe(face, 1, PALETA.CARAPACA_SOMBRA)
    for face in ("topo", "frente"):
        placa.salpicar(face, PALETA.CARAPACA_SOMBRA, 40, semente=3)
        placa.salpicar(face, PALETA.CARAPACA_LUZ, 26, semente=11)

    # ---------------------------------------------------------------- corpo
    # Ele fica inteiro debaixo da placa e quase nao aparece. Pintado assim mesmo:
    # face sem tinta nao da erro e nao aparece no atlas aberto num editor -- vira
    # um buraco por onde se ve o interior do bicho, de um angulo so.
    tronco = folha["body"]
    tronco.tudo(PALETA.QUITINA_SOMBRA)
    tronco.face("topo", PALETA.CARAPACA_SOMBRA)

    # --------------------------------------------------------------- ventre
    # A UNICA regiao clara da folha. Ela e o alvo, e por isso tem forma propria:
    # placas horizontais separadas por juntas. Chapada, a mancha leria como um
    # retangulo colado no bicho; segmentada, ela le como barriga.
    barriga = folha["ventre"]
    barriga.tudo(PALETA.VENTRE, forca=FORCA_DO_VENTRE)
    # As juntas entre as placas, na traseira (o que se ve por tras) e na base (o
    # que se ve de quem esta no chao).
    for dy in (2, 5, 8):
        barriga.linha("tras", dy, PALETA.VENTRE_SOMBRA, forca=FORCA_DO_VENTRE)
    for dy in (1, 4, 7):
        barriga.linha("base", dy, PALETA.VENTRE_SOMBRA, forca=FORCA_DO_VENTRE)
    # O topo do ventre encosta na carapaca e nunca e visto; ele leva o tom de
    # junta para que a borda entre placa e barriga nao pisque de claro quando a
    # camera passa rente.
    barriga.face("topo", PALETA.VENTRE_SOMBRA, forca=FORCA_DO_VENTRE)
    for lado in tex.LADOS:
        barriga.salpicar(lado, PALETA.VENTRE_SOMBRA, 34, semente=23, forca=FORCA_DO_VENTRE)

    # --------------------------------------------------------------- pincas
    for nome, semente in (("claw_left", 29), ("claw_right", 37)):
        pinca = folha[nome]
        pinca.tudo(PALETA.PINCA)
        pinca.face("base", PALETA.PINCA_SOMBRA)
        pinca.face("tras", PALETA.PINCA_SOMBRA)
        pinca.salpicar("topo", PALETA.PINCA_SOMBRA, 44, semente=semente)
    for nome in ("claw_left_tip", "claw_right_tip"):
        ponta = folha[nome]
        ponta.tudo(PALETA.PINCA_SOMBRA)
        # A aresta que fecha: uma fiada mais clara no topo da ponta. Ela e o
        # unico detalhe de forma da pinca, e existe para o jogador ver a garra
        # FECHAR no quadro do golpe -- sem ela o fechamento vira um borrao.
        ponta.faixa_no_topo("frente", 1, PALETA.PINCA)
        ponta.faixa_no_topo("tras", 1, PALETA.PINCA)

    # -------------------------------------------------------------- cabeca
    cabeca = folha["head"]
    cabeca.tudo(PALETA.QUITINA)
    cabeca.face("topo", PALETA.QUITINA_SOMBRA)
    cabeca.face("base", PALETA.QUITINA_SOMBRA)
    # Dois olhos ESCUROS, e nao um par claro. Ver o cabecalho: o unico claro da
    # folha e o ventre, e um olho claro roubaria dele o primeiro olhar.
    cabeca.ponto("frente", 1, 1, PALETA.OLHO)
    cabeca.ponto("frente", 4, 1, PALETA.OLHO)

    # -------------------------------------------------------------- pernas
    for nome, semente in (("leg_front_left", 43), ("leg_mid_left", 47),
                          ("leg_back_left", 53), ("leg_front_right", 59),
                          ("leg_mid_right", 61), ("leg_back_right", 67)):
        perna = folha[nome]
        perna.tudo(PALETA.QUITINA)
        # A ponta escura de cada pata. No PE da perna, que e o que encosta no
        # chao; pintada no topo ela ficaria escondida sob o corpo e a mesma area
        # de atlas teria custado o mesmo por nada.
        for face in tex.PAREDES:
            perna.faixa_no_pe(face, 2, PALETA.QUITINA_SOMBRA)
        perna.face("base", PALETA.QUITINA_SOMBRA)
        perna.salpicar("frente", PALETA.QUITINA_SOMBRA, 36, semente=semente)


# ------------------------------------------ validacao que liga arte e regra

def _luminancia(pixel):
    """Luminancia perceptual, a mesma conta que o olho humano faz de longe.

    Media simples dos canais mediria ERRADO: (0,255,0) e (255,0,0) tem a mesma
    media e brilhos completamente diferentes, e a regua aprovaria um verde
    berrante como se fosse tao discreto quanto um vermelho escuro.
    """
    return 0.299 * pixel[0] + 0.587 * pixel[1] + 0.114 * pixel[2]


def _pixels_das_faces(f, nome):
    """So os pixels das SEIS faces da caixa -- nunca os cantos do layout.

    O retangulo de uma caixa tem cantos que nenhuma face usa e que ficam
    transparentes de proposito. Medi-los faria toda amplitude comecar do preto e
    a regua aprovaria qualquer coisa.
    """
    pincel = f[nome]
    for face in tex.FACES:
        x, y, largura, altura = pincel.faces()[face]
        for i in range(x, x + largura):
            for j in range(y, y + altura):
                yield f.px[i, j][:3]


def valida_ventre_e_o_unico_claro(f):
    """O pixel mais ESCURO do ventre tem de superar o mais CLARO do resto.

    E a regra do servidor conferida em pixels. `RegrasDeCarapacaOrientada` paga
    quase nada a quem bate de frente e quase tudo a quem acerta pelas costas; o
    jogador so descobre isso se, ao circular o bicho, ele VIR alguma coisa mudar.
    O que muda e esta mancha.

    A comparacao e entre o EXTREMO de cada lado -- o ventre mais escuro contra o
    resto mais claro -- e nao entre medias. Media aprovaria uma folha em que o
    ventre e claro na maior parte e escurece justamente na borda traseira, que e
    a unica parte dele que o jogador ve enquanto decide se circula.

    Ela morde dos dois lados sem precisar de um segundo teste: um claro novo em
    qualquer outra peca derruba o degrau, e um ventre escurecido derruba tambem.
    Nenhuma das duas mudancas levanta excecao em lugar nenhum do jogo, nenhuma
    aparece no atlas aberto num editor, e as duas transformam o encontro num
    saco de pancada com armadura 8.
    """
    claros = list(map(_luminancia, _pixels_das_faces(f, "ventre")))
    ventre_mais_escuro = min(claros)

    pior_nome, pior_luz = None, -1.0
    for caixa in CAIXAS:
        if caixa.nome == "ventre":
            continue
        for pixel in _pixels_das_faces(f, caixa.nome):
            luz = _luminancia(pixel)
            if luz > pior_luz:
                pior_nome, pior_luz = caixa.nome, luz

    degrau = ventre_mais_escuro - pior_luz
    if degrau < DEGRAU_MINIMO_DE_LEITURA:
        raise geo.ErroDeArte(
            "o pixel mais escuro do ventre tem luminancia %.1f e o mais claro da caixa '%s' tem "
            "%.1f: o degrau e %.1f e o minimo legivel e %.1f. Com as duas regioes empatadas, o "
            "jogador circula o bicho, nao ve nada mudar, e a regra que paga por chegar pelas "
            "costas vira um numero que so o servidor conhece"
            % (ventre_mais_escuro, pior_nome, pior_luz, degrau, DEGRAU_MINIMO_DE_LEITURA))

    print("leitura do ventre: %.1f (mais escuro) contra '%s' %.1f (mais claro)  "
          "degrau %.1f, minimo %.1f"
          % (ventre_mais_escuro, pior_nome, pior_luz, degrau, DEGRAU_MINIMO_DE_LEITURA))


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_ventre_e_o_unico_claro,))
