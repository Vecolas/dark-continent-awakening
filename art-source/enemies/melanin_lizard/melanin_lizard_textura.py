"""Folha do Melanin Lizard -- a pedra que tem um olho.

A PALETA INTEIRA E DE ROCHA, E ISSO E A MECANICA. Este bicho fica imovel e deixa
de ser alvo enquanto ninguem olha para ele de perto; a regra so e justa se a
textura tiver realmente enganado o jogador. Por isso nao ha nenhum tom saturado
de "lagarto" aqui: granito, sombra de granito, ventre empoeirado e liquen. Um
verde de reptil nesta folha nao daria erro nenhum -- daria um bicho que se ve de
longe e uma regra de camuflagem que parece trapaca quando finalmente pega
alguem.

HA UM UNICO CONTRASTE ALTO NA FOLHA, E ELE E O OLHO -- que e tambem o PONTO
FRACO que o servidor mede. Isso e decisao, nao coincidencia: a unica coisa que
salta na silhueta e a unica coisa que vale a pena acertar. Quem descobre o
lagarto descobre junto onde bater.

Se alguem mover o olho sem mover o `WeakPointResolver` (ou o contrario), a
textura passa a ensinar errado e NENHUM portao ve: o jogador mira no ambar,
leva dano comum, e culpa a propria mira. Por isso a altura do olho e conferida
abaixo contra o mesmo numero do perfil.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao, e a divergencia da face pintada no lugar errado.

Regerar:  python art-source/enemies/melanin_lizard/melanin_lizard_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/melanin_lizard/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import geometria as geo      # noqa: E402
from comum import textura as tex        # noqa: E402
from melanin_lizard_geo import CAIXAS, HITBOX, MOB, UV_ALTURA, UV_LARGURA  # noqa: E402

# Altura relativa a partir da qual o servidor chama a regiao de "olho".
# MelaninLizardTuning: new WeakPointResolver("olho", "corpo", 0.54D, 0.35D).
#
# O 0.54 foi MEDIDO no desenho, e nao escolhido: o olho ocupa as duas fiadas de
# cima da face lateral da cabeca, y 7..9 num bicho de 12.8 px de hitbox -- o pe
# dele fica em 0.547. O limiar tem de ficar logo ABAIXO disso. Um numero redondo
# copiado de outro mob (0.62, do great stamp) poria o limite acima do olho
# pintado: o ambar continuaria la, prometendo um acerto que a regra nao paga.
ALTURA_MINIMA_DO_OLHO = 0.54

# O olho, em coordenada de FACE: duas fiadas a partir do topo, duas colunas de
# largura. Estao aqui, e nao espalhados pela pintura, porque a validacao no fim
# do arquivo mede exatamente estes dois numeros.
OLHO_DY = 0
OLHO_LADO = 2

PALETA = tex.Paleta(
    ROCHA=(104, 100, 94),         # o flanco: granito seco, o tom que engana
    ROCHA_ESCURA=(74, 72, 68),    # o que se ve de cima, e a sombra sob o corpo
    ROCHA_CLARA=(140, 136, 126),  # o topo das placas, que pega luz
    VENTRE=(126, 116, 102),       # o que se ve de baixo, empoeirado
    LIQUEN=(96, 110, 78),         # o mosqueado que faz a pedra nao ler como plastico
    OLHO=(206, 158, 62),          # O UNICO contraste alto da folha, e o ponto fraco
    PUPILA=(22, 20, 18),          # a fenda vertical, que e o que faz o olho ser de reptil
    # Dentes e unhas. A primeira versao deste arquivo trazia (208, 200, 182) --
    # osso quase branco, o reflexo de quem pinta dente. A regua
    # `valida_um_unico_contraste_alto` reprovou na primeira execucao: aquele tom
    # estava a 99.8 de luminancia da rocha e o OLHO esta a 60.9, ou seja, os
    # dentes saltavam MAIS que o ponto fraco. Nada disso da erro; da um jogador
    # que acha a boca antes de achar o olho, e que por isso bate onde o
    # multiplicador nao existe.
    GARRA=(150, 144, 130),
)

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def pintar():
    # ------------------------------------------------------------- o tronco
    corpo = folha["corpo"]
    corpo.tudo(PALETA.ROCHA)
    corpo.face("topo", PALETA.ROCHA_ESCURA)
    corpo.face("base", PALETA.VENTRE)
    # Liquen so no DORSO. Ele e o detalhe que vende a pedra, e pedra so cria
    # liquen onde a chuva bate: salpicar o ventre tambem faria o bicho ler como
    # sujo por igual, que e como uma textura ruim parece.
    corpo.salpicar("topo", PALETA.LIQUEN, 52, semente=3)
    corpo.salpicar("topo", PALETA.ROCHA_CLARA, 34, semente=10)
    # Manchas nas quatro paredes: elas quebram a linha reta do flanco, que e o
    # que denuncia um bicho parado contra uma parede de blocos.
    for face in tex.PAREDES:
        corpo.salpicar(face, PALETA.ROCHA_ESCURA, 46, semente=17)

    # ------------------------------------------------------------- a cabeca
    cabeca = folha["cabeca"]
    cabeca.tudo(PALETA.ROCHA)
    cabeca.face("topo", PALETA.ROCHA_ESCURA)
    cabeca.face("base", PALETA.VENTRE)
    cabeca.salpicar("topo", PALETA.LIQUEN, 40, semente=5)
    # As narinas, na ponta do focinho. Ponto exato (forca 0): ruido numa narina
    # de 1 px a apaga.
    cabeca.ponto("frente", 1, 1, PALETA.ROCHA_ESCURA)
    cabeca.ponto("frente", 4, 1, PALETA.ROCHA_ESCURA)

    # O OLHO, nos DOIS lados, e com a frente do bicho em bordas OPOSTAS.
    #
    # Na face 'direita' a frente fica na borda DIREITA; na 'esquerda', na borda
    # ESQUERDA. Copiar a mesma coluna nos dois lados nao da erro: da um lagarto
    # visivelmente vesgo, com um olho no focinho e outro na nuca, e ninguem
    # descobre isso sem girar a camera em volta dele.
    _, _, largura_do_lado, _ = cabeca.faces()["direita"]
    cabeca.na_face("direita", largura_do_lado - OLHO_LADO, OLHO_DY,
                   OLHO_LADO, OLHO_LADO, PALETA.OLHO, forca=0)
    cabeca.na_face("esquerda", 0, OLHO_DY, OLHO_LADO, OLHO_LADO, PALETA.OLHO, forca=0)
    # A fenda da pupila fica do lado do FOCINHO em cada face, pelo mesmo motivo.
    cabeca.ponto("direita", largura_do_lado - 1, OLHO_DY, PALETA.PUPILA)
    cabeca.ponto("direita", largura_do_lado - 1, OLHO_DY + 1, PALETA.PUPILA)
    cabeca.ponto("esquerda", 0, OLHO_DY, PALETA.PUPILA)
    cabeca.ponto("esquerda", 0, OLHO_DY + 1, PALETA.PUPILA)

    # ---------------------------------------------------------- a mandibula
    mandibula = folha["mandibula"]
    mandibula.tudo(PALETA.ROCHA_ESCURA)
    mandibula.face("base", PALETA.VENTRE)
    # Dentes ALTERNADOS, e nao uma fiada cheia. Uma linha branca contínua seria
    # um segundo contraste alto na folha e roubaria o olho -- e o olho e o unico
    # ponto fraco que este bicho tem.
    for face in ("frente", "direita", "esquerda"):
        _, _, largura, _ = mandibula.faces()[face]
        for dx in range(0, largura, 2):
            mandibula.ponto(face, dx, 0, PALETA.GARRA)

    # -------------------------------------------------------------- a crista
    crista = folha["crista"]
    crista.tudo(PALETA.ROCHA_ESCURA)
    crista.face("topo", PALETA.ROCHA_CLARA)
    # As placas viram COLUNAS separadas, e nao salpico: a distancia, salpico le
    # como sujeira e coluna le como quina. A quina e o que faz a crista ler como
    # afloramento de rocha em vez de lombada de bicho.
    _, _, largura_do_topo, _ = crista.faces()["topo"]
    for dx in range(0, largura_do_topo, 2):
        crista.coluna("topo", dx, PALETA.ROCHA_ESCURA, forca=2)
    crista.salpicar("topo", PALETA.LIQUEN, 60, semente=23)

    # --------------------------------------------------------------- a cauda
    for nome in ("cauda", "ponta_da_cauda"):
        p = folha[nome]
        p.tudo(PALETA.ROCHA)
        p.face("topo", PALETA.ROCHA_ESCURA)
        p.face("base", PALETA.VENTRE)
        p.salpicar("topo", PALETA.LIQUEN, 44, semente=31)
    # Aneis escuros so na base da cauda: eles dao o comprimento ao olho, que e o
    # que faz a cauda ler como cauda e nao como um toco.
    cauda = folha["cauda"]
    for face in tex.LADOS:
        for dx in (0, 2, 4):
            cauda.coluna(face, dx, PALETA.ROCHA_ESCURA, forca=2)

    # -------------------------------------------------------------- as patas
    for nome in ("perna_frente_esq", "perna_frente_dir",
                 "perna_tras_esq", "perna_tras_dir"):
        p = folha[nome]
        p.tudo(PALETA.ROCHA_ESCURA)
        p.face("topo", PALETA.ROCHA)

    for nome in ("pata_frente_esq", "pata_frente_dir",
                 "pata_tras_esq", "pata_tras_dir"):
        p = folha[nome]
        p.tudo(PALETA.ROCHA_ESCURA)
        p.face("topo", PALETA.ROCHA)
        p.face("base", PALETA.VENTRE)
        # Unhas na ponta da pata: dois pontos, nao tres. Ver o comentario dos
        # dentes -- claro demais aqui e um segundo foco na folha.
        p.ponto("frente", 0, 0, PALETA.GARRA)
        p.ponto("frente", 2, 0, PALETA.GARRA)


def valida_olho_na_altura_do_ponto_fraco(f):
    """O olho pintado tem de cair na faixa que o servidor chama de ponto fraco.

    O resolver mede altura RELATIVA a caixa de colisao; a textura pinta pixels
    dentro de uma face. Os dois so concordam se alguem conferir, e a divergencia
    nao da erro nenhum: da um olho desenhado onde acertar nao vale mais, o que e
    PIOR do que nao ter olho, porque ensina errado.

    A segunda metade cobra o contrario: se o limiar descer tanto que o bicho
    inteiro vire ponto fraco, o multiplicador deixa de ser recompensa por mira e
    vira um desconto geral no HP -- e isso tambem passa calado.
    """
    cabeca = geo.achar(CAIXAS, "cabeca", "caixa")
    _, (_, topo_da_cabeca), _ = geo.volume(cabeca)
    altura_da_hitbox_px = HITBOX[1] * 16.0
    topo_do_olho = topo_da_cabeca - OLHO_DY
    pe_do_olho = topo_do_olho - OLHO_LADO
    limite = ALTURA_MINIMA_DO_OLHO * altura_da_hitbox_px
    if pe_do_olho < limite:
        raise geo.ErroDeArte(
            "o olho pintado vai de y=%s a y=%s e o servidor so chama de ponto fraco acima de %.2f "
            "px (WeakPointResolver %.2f de %.1f px): o desenho promete um acerto que a regra nao "
            "paga, e nada acusa isso"
            % (pe_do_olho, topo_do_olho, limite, ALTURA_MINIMA_DO_OLHO, altura_da_hitbox_px))
    _, (pe_do_corpo, _), _ = geo.volume(geo.achar(CAIXAS, "corpo", "caixa"))
    if pe_do_corpo > limite:
        raise geo.ErroDeArte(
            "o tronco comeca em y=%s, acima do limite de ponto fraco %.2f px: nao sobra regiao de "
            "corpo comum, e todo golpe no lagarto viraria critico"
            % (pe_do_corpo, limite))


def valida_um_unico_contraste_alto(f):
    """So o OLHO pode saltar da folha, e ele salta por um motivo mecanico.

    Um bicho cuja mecanica e passar despercebido nao pode ter dois focos. Se
    dente, unha ou liquen chegarem perto do contraste do olho, o jogador para de
    achar o olho primeiro -- e o olho e o ponto fraco. Isso nao reprova nenhum
    portao de formato: e leitura, e leitura so aparece na tela.

    A medida e a distancia de luminancia entre cada cor e a ROCHA, que e o fundo
    contra o qual tudo e visto.
    """
    def luminancia(cor):
        return 0.299 * cor[0] + 0.587 * cor[1] + 0.114 * cor[2]

    fundo = luminancia(PALETA.ROCHA)
    do_olho = abs(luminancia(PALETA.OLHO) - fundo)
    for nome, cor in PALETA:
        if nome in ("OLHO", "PUPILA"):
            continue
        distancia = abs(luminancia(cor) - fundo)
        if distancia >= do_olho:
            raise geo.ErroDeArte(
                "a cor '%s' esta a %.1f de luminancia da rocha e o olho esta a %.1f: a folha ganhou "
                "um segundo foco, o jogador para de achar o olho primeiro, e o olho e o ponto fraco "
                "deste bicho" % (nome, distancia, do_olho))


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_olho_na_altura_do_ponto_fraco,
                              valida_um_unico_contraste_alto))
