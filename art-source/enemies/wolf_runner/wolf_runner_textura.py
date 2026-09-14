"""Folha do Wolf Runner -- pelo curto de corredor sobre quitina de formiga.

A DECISAO DE LEITURA DESTA FOLHA CABE NUMA FRASE: **o pelo e neutro e a quitina
e roxa.** Tudo que e mamifero -- flanco, ventre, canela, cauda -- vive numa
faixa estreita de cinza-terra; tudo que e inseto -- placa do dorso, antenas,
quelicera, aneis do flanco -- sai dessa faixa no MATIZ, e nao no brilho.

Isso nao e gosto, e a identidade da colonia. Um Wolf Runner sem marca insetoide
le como lobo comum: o mob nasce, corre, morde, dropa loot e passa em todo
portao, e o que se perde e a unica coisa que diz ao jogador que aquilo ali e
uma formiga quimera e nao fauna. Nada acusa isso -- nao ha excecao, nao ha log,
e o atlas aberto num editor parece perfeito.

Separar por MATIZ e nao por brilho e deliberado. Um contraste alto de
luminancia no dorso competiria com a silhueta, que e o que este bicho usa para
ser lido de longe enquanto contorna. O roxo aparece na cor sem roubar a forma.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

Regerar:  python art-source/enemies/wolf_runner/wolf_runner_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/wolf_runner/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import textura as tex        # noqa: E402
from wolf_runner_geo import (CAIXAS, MOB, UV_ALTURA, UV_LARGURA)  # noqa: E402

PALETA = tex.Paleta(
    PELO=(98, 92, 84),            # o flanco, visto de frente
    PELO_DORSO=(68, 64, 60),      # o que se ve de cima, e o que sombreia
    PELO_VENTRE=(136, 129, 116),  # o que se ve de baixo
    # --- o que e inseto: separado por MATIZ, nunca por brilho ---
    QUITINA=(86, 62, 98),         # placa, antenas, quelicera e os aneis
    QUITINA_LUZ=(126, 98, 140),   # a aresta da placa e a ponta da antena
    # --- os dois unicos pontos exatos da folha ---
    GARRA=(198, 192, 174),        # garras e dentes
    OLHO=(214, 172, 60),          # ambar: o bicho caca, e caçador tem olho claro
)


folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def _pelo(p, salpico, dorso=True):
    """O tratamento comum de toda peca de carne.

    Topo escuro e base clara: e a contra-sombra de qualquer bicho que corre no
    chao, e sem ela o corpo le como um bloco de cor chapada visto de cima --
    que e justamente o angulo em que o jogador ve uma matilha o contornando.

    `salpico` e a semente. Semente repetida entre duas pecas faria as duas
    receberem o MESMO mosqueado, e padrao repetido le como textura de parede,
    nao como pelo.
    """
    p.tudo(PALETA.PELO)
    if dorso:
        p.face("topo", PALETA.PELO_DORSO)
    p.face("base", PALETA.PELO_VENTRE)
    for face in tex.PAREDES:
        p.salpicar(face, PALETA.PELO_DORSO, 48, semente=salpico)
        p.salpicar(face, PALETA.PELO_VENTRE, 26, semente=salpico + 7)


def _quitina(p, salpico):
    """Peca de casca: lisa, escura por baixo, com a aresta de cima pegando luz.

    Salpico BAIXO de proposito. Quitina mosqueada le como pelo curto, e a folha
    inteira depende de as duas superficies nao se confundirem.
    """
    p.tudo(PALETA.QUITINA)
    p.face("topo", PALETA.QUITINA_LUZ)
    p.face("base", PALETA.QUITINA)
    for face in tex.PAREDES:
        p.salpicar(face, PALETA.QUITINA_LUZ, 16, semente=salpico)


def pintar():
    # -------------------------------------------------------------- tronco
    tronco = folha["body"]
    _pelo(tronco, 3)
    # Os ANEIS do flanco -- marca insetoide pintada. Colunas, e nao salpico:
    # salpico a distancia le como sujeira; coluna regular le como segmento de
    # exoesqueleto. Elas ficam nas duas laterais porque um bicho listrado de um
    # lado so e o defeito que ninguem descobre sem girar a camera.
    for face in ("direita", "esquerda"):
        for dx in (2, 4, 6):
            tronco.coluna(face, dx, PALETA.QUITINA, forca=2)
    # Faixa de ventre clara nas quatro paredes: ela e o que da uma horizontal a
    # silhueta de perfil, e perfil e como o flanqueador e visto.
    for face in tex.PAREDES:
        tronco.faixa_no_pe(face, 1, PALETA.PELO_VENTRE)

    # ------------------------------------------------------- placa do dorso
    placa = folha["plate"]
    _quitina(placa, 11)
    # Tres sulcos atravessando a placa. Eles existem para a peca nao ler como um
    # adesivo colado nas costas -- casca tem segmento, adesivo nao tem.
    for dy in (1, 2, 3):
        placa.linha("topo", dy, PALETA.QUITINA, forca=1)

    # -------------------------------------------------------------- pescoco
    pescoco = folha["neck"]
    _pelo(pescoco, 19)
    # Colar de quitina no alto do pescoco: e a transicao entre a cabeca de
    # inseto e o corpo de mamifero, e sem ela as duas metades leem como dois
    # bichos colados.
    for face in tex.PAREDES:
        pescoco.faixa_no_topo(face, 1, PALETA.QUITINA)

    # --------------------------------------------------------------- cabeca
    cabeca = folha["head"]
    _pelo(cabeca, 29)
    # A testa de quitina, e as duas orbitas escuras embaixo dela.
    cabeca.faixa_no_topo("frente", 1, PALETA.QUITINA)
    cabeca.na_face("frente", 1, 1, 1, 1, PALETA.PELO_DORSO, forca=0)
    cabeca.na_face("frente", 4, 1, 1, 1, PALETA.PELO_DORSO, forca=0)
    # Os olhos: um pixel exato cada, forca=0. Ruido num olho de 1 px apaga o
    # olho -- e olho e a unica coisa que diz para onde a cabeca aponta quando o
    # bicho esta agachado no aviso do bote.
    cabeca.ponto("frente", 1, 2, PALETA.OLHO)
    cabeca.ponto("frente", 4, 2, PALETA.OLHO)
    # Quitina tambem no alto da cabeca, continuando o colar.
    cabeca.faixa_no_topo("direita", 1, PALETA.QUITINA)
    cabeca.faixa_no_topo("esquerda", 1, PALETA.QUITINA)
    cabeca.face("topo", PALETA.QUITINA)

    # ------------------------------------------------------------ quelicera
    # A mandibula e INTEIRA de quitina, e os dentes sao GARRA -- o mesmo tom das
    # unhas, e nao um branco proprio. Um segundo claro na cara disputaria o
    # olhar com o olho, e o olho e o que aponta a direcao do bote.
    mandibula = folha["jaw"]
    _quitina(mandibula, 37)
    for dx in (0, 1, 2, 3):
        mandibula.ponto("frente", dx, 1, PALETA.GARRA)
    # As duas presas laterais, que e o que faz a peca ler como quelicera e nao
    # como queixo.
    mandibula.ponto("direita", 0, 1, PALETA.GARRA)
    mandibula.ponto("esquerda", 2, 1, PALETA.GARRA)

    # -------------------------------------------------------------- antenas
    for nome, semente in (("antenna_left", 43), ("antenna_right", 53)):
        antena = folha[nome]
        _quitina(antena, semente)
        # A ponta -- o pixel mais a frente de cada antena -- em tom claro. Sem
        # ela a antena some contra a cabeca escura e a marca de inseto depende
        # so da placa do dorso, que nao aparece de frente.
        antena.face("frente", PALETA.QUITINA_LUZ, forca=0)

    # --------------------------------------------------------------- cauda
    cauda = folha["tail"]
    _pelo(cauda, 61)
    # Ponta escura: leme so se le se a ponta tiver contorno proprio.
    for face in tex.PAREDES:
        cauda.faixa_no_pe(face, 1, PALETA.PELO_DORSO)
    cauda.face("tras", PALETA.PELO_DORSO)

    # ------------------------------------------------------- patas e garras
    for nome, semente in (("leg_front_left", 71), ("leg_front_right", 79),
                          ("leg_back_left", 89), ("leg_back_right", 97)):
        perna = folha[nome]
        # `dorso=False`: a canela e vista de lado e nunca de cima, e escurecer o
        # topo dela so gastaria area de atlas.
        _pelo(perna, semente, dorso=False)
        # A meia escura embaixo: ela separa a canela da pata no galope, que e
        # quando as duas passam uma na frente da outra.
        for face in tex.PAREDES:
            perna.faixa_no_pe(face, 1, PALETA.PELO_DORSO)

    for nome, semente in (("paw_front_left", 103), ("paw_front_right", 109),
                          ("paw_back_left", 113), ("paw_back_right", 127)):
        pata = folha[nome]
        _pelo(pata, semente, dorso=False)
        pata.face("base", PALETA.PELO_DORSO)
        # Duas garras na FRENTE de cada pata. Pintadas na 'frente' e nao na
        # 'base': na base elas nunca sao vistas, e o desenho teria custado a
        # mesma area de atlas para nada. Elas pagam o trait CLAWS do gene pool.
        pata.ponto("frente", 0, 1, PALETA.GARRA)
        pata.ponto("frente", 1, 1, PALETA.GARRA)


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB)
