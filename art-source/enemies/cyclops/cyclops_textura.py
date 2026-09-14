"""Folha do Cyclops -- pele de pedreira, couro cru, tora de madeira e UM olho.

A DECISAO DE LEITURA DESTA FOLHA CABE NUMA FRASE: **o unico contraste alto da
folha e o olho.** Tudo o mais -- pele, couro, madeira, unha -- vive numa faixa
estreita de luminancia media. O olho sai dessa faixa nas duas pontas, com uma
esclera quase branca e uma pupila quase preta encostadas uma na outra.

Isso nao e gosto: e a unica forma de o jogador achar o ponto fraco a vinte
blocos de distancia, no meio do mato, sem tutorial. Um segundo contraste alto
na folha -- dentes brancos, uma fivela metalica, uma tatuagem clara -- rouba o
olhar e o olho deixa de ser o que se acha primeiro. Isso NAO da erro nenhum,
nao aparece no atlas aberto num editor, e so se manifesta como "esse chefe e
confuso". Por isso `valida_contraste_unico_do_olho` mede a folha inteira e
reprova quem empatar com o olho.

O OLHO NAO E ENFEITE: ELE E A REGRA DESENHADA. O servidor chama de "eye" todo
impacto acima de 84% da altura da caixa de colisao e dentro de um cone frontal
estreito -- e paga multiplicador por isso. Se o olho for desenhado abaixo desse
limiar, a textura passa a prometer um critico que a regra recusa, e nao ha
portao que veja isso. Por isso a altura e conferida aqui contra o MESMO numero
que o Java usa.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

Regerar:  python art-source/enemies/cyclops/cyclops_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/cyclops/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import geometria as geo      # noqa: E402
from comum import textura as tex        # noqa: E402
from cyclops_geo import (ALTURA_MINIMA_DO_OLHO, CAIXAS, HITBOX, MOB,  # noqa: E402
                         UV_ALTURA, UV_LARGURA)

# Quantas vezes a amplitude de luminancia do olho tem de superar a da peca mais
# contrastada do resto do corpo.
#
# 1.5 e um degrau que se enxerga: com 1.2 duas regioes disputam o olhar e a
# leitura a distancia fica ambigua; com 3.0 o resto do bicho teria de ser
# chapado, e chapado le como plastico. O numero e um limite de LEITURA, nao de
# balanceamento -- ninguem vai gira-lo numa sessao de ajuste, e por isso ele mora
# aqui, ao lado do comentario que o explica.
FATOR_DE_CONTRASTE_DO_OLHO = 1.5

PALETA = tex.Paleta(
    PELE=(92, 104, 80),          # o flanco, visto de frente
    PELE_SOMBRA=(64, 74, 56),    # o que fica por baixo, atras e dentro da orbita
    PELE_LUZ=(118, 130, 102),    # o que pega luz de cima
    COURO=(78, 58, 42),          # tanga, cintas e a fenda da boca
    MADEIRA=(104, 80, 52),       # a tora do porrete
    MADEIRA_ESCURA=(70, 52, 34), # o veio e o lado de baixo da tora
    UNHA=(150, 146, 128),        # garras, dentes e as lascas cravadas na tora
    # --- os tres tons abaixo so podem aparecer dentro do olho ---
    ESCLERA=(236, 226, 190),     # o branco do olho: o ponto mais claro da folha
    IRIS=(176, 64, 40),          # o anel: o unico vermelho do bicho
    PUPILA=(28, 22, 26),         # o miolo: o ponto mais escuro da folha
)

# Os tons que a regua de contraste prende dentro do olho. Declarados uma vez, e
# nao repetidos na validacao: duas listas divergem, e a divergencia faria a
# regua deixar de vigiar justamente o tom que alguem acabou de espalhar.
TONS_DO_OLHO = ("ESCLERA", "IRIS", "PUPILA")

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def _pele(p, salpico):
    """O tratamento comum de toda peca de carne.

    Topo claro, base e traseira escuras: e a sombra que um corpo de quatro
    blocos precisa para nao ler como um bloco de cor chapada visto de cima.
    `salpico` e a semente -- semente repetida entre duas pecas faria as duas
    receberem o MESMO mosqueado, e um padrao repetido le como textura de
    parede, nao como pele.
    """
    p.tudo(PALETA.PELE)
    p.face("topo", PALETA.PELE_LUZ)
    p.face("base", PALETA.PELE_SOMBRA)
    p.face("tras", PALETA.PELE_SOMBRA)
    for face in tex.PAREDES:
        p.salpicar(face, PALETA.PELE_SOMBRA, 52, semente=salpico)
        p.salpicar(face, PALETA.PELE_LUZ, 30, semente=salpico + 7)


def pintar():
    # -------------------------------------------------------------- tronco
    torso = folha["torso"]
    _pele(torso, 3)
    # A cinta que atravessa o peito: ela existe para o tronco ter uma horizontal
    # e o olho nao ser a unica coisa com forma na silhueta de frente.
    for face in tex.PAREDES:
        torso.faixa_no_pe(face, 2, PALETA.COURO)
    torso.na_face("frente", 2, 4, 14, 2, PALETA.COURO)

    quadril = folha["hip"]
    _pele(quadril, 11)
    # A tanga: couro nas quatro paredes. Pintada so na frente, o bicho ficaria
    # visivelmente nu por tras -- e isso ninguem descobre sem girar a camera.
    for face in tex.PAREDES:
        quadril.faixa_no_pe(face, 5, PALETA.COURO)

    # -------------------------------------------------------------- cabeca
    cabeca = folha["head"]
    _pele(cabeca, 19)
    # A orbita: uma depressao escura em volta de onde a placa do olho se encaixa.
    # Ela e o que faz o olho parecer ENFIADO na cara em vez de colado nela.
    cabeca.na_face("frente", 3, 1, 8, 8, PALETA.PELE_SOMBRA, forca=3)
    # A sobrancelha unica, uma fiada acima da orbita.
    cabeca.na_face("frente", 2, 0, 10, 1, PALETA.COURO, forca=2)
    # A boca: fenda escura e quatro dentes. Os dentes usam UNHA, e nao um branco
    # proprio -- um segundo tom claro na cara disputaria o olhar com o olho, que
    # e exatamente o que esta folha nao pode ter.
    cabeca.na_face("frente", 3, 9, 8, 2, PALETA.COURO, forca=0)
    for dx in (4, 6, 8, 9):
        cabeca.ponto("frente", dx, 9, PALETA.UNHA)

    # ----------------------------------------------------------------- olho
    # A UNICA peca com contraste alto da folha inteira, e a unica pintada com
    # forca=0: ruido na pupila ou na esclera borraria a leitura a distancia, que
    # e a unica coisa que esta peca entrega.
    olho = folha["eye"]
    # As cinco faces que nao sao a da frente pertencem a orbita, e nao ao globo:
    # elas somem dentro da cara. Pintadas com tom de olho, vazariam o contraste
    # para fora do retangulo que a regua vigia.
    olho.tudo(PALETA.PELE_SOMBRA)
    olho.face("frente", PALETA.ESCLERA, forca=0)
    olho.na_face("frente", 1, 1, 4, 4, PALETA.IRIS, forca=0)
    olho.na_face("frente", 2, 2, 2, 2, PALETA.PUPILA, forca=0)

    # --------------------------------------------------------------- bracos
    for nome, semente in (("arm_left", 29), ("arm_right", 37)):
        braco = folha[nome]
        _pele(braco, semente)
        # Bracadeira de couro no ombro: e o que separa visualmente o braco do
        # tronco quando os dois estao na mesma cor e encostados.
        for face in tex.PAREDES:
            braco.faixa_no_topo(face, 3, PALETA.COURO)

    # -------------------------------------------------------------- porrete
    tora = folha["club"]
    tora.tudo(PALETA.MADEIRA)
    tora.face("topo", PALETA.MADEIRA_ESCURA)
    tora.face("base", PALETA.MADEIRA_ESCURA)
    tora.face("tras", PALETA.MADEIRA_ESCURA)
    # Veio da madeira em COLUNAS, e nao salpicado: salpico a distancia le como
    # sujeira; coluna le como tora. As colunas sao declaradas por face porque a
    # face 'frente' tem 5 px de largura e a 'direita' tem 7 -- uma lista unica
    # so funcionaria enquanto as duas tivessem o mesmo tamanho.
    for face, colunas in (("frente", (1, 3)), ("tras", (0, 2, 4)),
                          ("direita", (1, 4)), ("esquerda", (2, 5))):
        for dx in colunas:
            tora.coluna(face, dx, PALETA.MADEIRA_ESCURA, forca=2)
    # Lascas de pedra cravadas na ponta que bate. Elas ficam no PE da tora
    # porque e o pe que encosta no jogador, e um porrete com a ponta lisa nao
    # explica 14 de dano.
    for face in tex.PAREDES:
        tora.faixa_no_pe(face, 1, PALETA.UNHA)

    # ------------------------------------------------------- pernas e pes
    for nome, semente in (("leg_left", 43), ("leg_right", 53)):
        _pele(folha[nome], semente)

    for nome, semente in (("foot_left", 61), ("foot_right", 71)):
        pe = folha[nome]
        _pele(pe, semente)
        # Tres garras na FRENTE de cada pe. Pintadas na face 'frente' e nao na
        # 'base': na base elas nunca sao vistas, e o desenho teria custado a
        # mesma area de atlas para nada.
        for dx in (1, 3, 5):
            pe.na_face("frente", dx, 3, 2, 2, PALETA.UNHA, forca=0)


# ------------------------------------------ validacoes que ligam arte e regra

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
    a regua de contraste aprovaria qualquer coisa.
    """
    pincel = f[nome]
    for face in tex.FACES:
        x, y, largura, altura = pincel.faces()[face]
        for i in range(x, x + largura):
            for j in range(y, y + altura):
                yield f.px[i, j][:3]


def valida_olho_na_altura_do_ponto_fraco(f):
    """O olho pintado tem de cair na faixa que o servidor chama de ponto fraco.

    O resolver mede altura RELATIVA a caixa de colisao; a textura pinta pixels
    dentro de uma face. Os dois so concordam por construcao se alguem conferir --
    e a divergencia nao da erro nenhum: da um olho desenhado onde bater nao vale
    mais, o que e pior do que nao ter olho, porque ENSINA errado.

    A regua e frouxa de um lado de proposito: o limiar do servidor (0.84) fica
    ABAIXO da base do olho desenhado. Isso e assumido -- o impacto que o servidor
    mede e onde o traco ENTRA na caixa de colisao, e nao o pixel que o jogador
    mirou, entao exigir o pixel exato faria o critico quase nunca pagar e o olho
    viraria uma promessa vazia. O que a regua proibe e o contrario: olho pintado
    ABAIXO do limiar, que promete um acerto que a regra recusa.
    """
    _, (olho_y0, olho_y1), _ = geo.volume(geo.achar(CAIXAS, "eye", "caixa"))
    altura_da_hitbox_px = HITBOX[1] * 16.0
    limite = ALTURA_MINIMA_DO_OLHO * altura_da_hitbox_px
    if olho_y0 < limite:
        raise geo.ErroDeArte(
            "o olho pintado vai de y=%s a y=%s e o servidor so chama de ponto fraco acima de "
            "%.1f px (WeakPointResolver %.2f de %.1f px): o desenho promete um critico que a "
            "regra nao paga, e nada acusa isso"
            % (olho_y0, olho_y1, limite, ALTURA_MINIMA_DO_OLHO, altura_da_hitbox_px))


def valida_contraste_unico_do_olho(f):
    """O olho e o UNICO contraste alto da folha, e isso se mede.

    Duas coisas sao cobradas, e as duas quebram a mesma promessa:

    1. VAZAMENTO -- nenhum tom do olho pode aparecer fora do retangulo do olho.
       Um respingo de esclera num dente ou numa fivela poe um segundo ponto
       claro na cara, e o jogador passa a achar dois alvos onde a regra so paga
       um.
    2. AMPLITUDE -- a diferenca entre o pixel mais claro e o mais escuro do olho
       tem de superar a de qualquer outra peca por FATOR_DE_CONTRASTE_DO_OLHO.
       Empatada com o porrete ou com os dentes, a mancha do olho deixa de ser o
       que se acha primeiro a vinte blocos.

    Nenhuma das duas levanta excecao em lugar nenhum do jogo, nenhuma aparece no
    atlas aberto num editor, e as duas so se manifestam como "esse chefe e
    confuso de ler".
    """
    proibidos = {getattr(PALETA, nome): nome for nome in TONS_DO_OLHO}
    for caixa in CAIXAS:
        if caixa.nome == "eye":
            continue
        for pixel in _pixels_das_faces(f, caixa.nome):
            if pixel in proibidos:
                raise geo.ErroDeArte(
                    "o tom %s do olho %s aparece na caixa '%s': um segundo ponto de contraste alto "
                    "rouba o olhar, e o olho deixa de ser o que o jogador acha primeiro"
                    % (proibidos[pixel], pixel, caixa.nome))

    luzes = list(map(_luminancia, _pixels_das_faces(f, "eye")))
    amplitude_do_olho = max(luzes) - min(luzes)
    pior_nome, pior_amplitude = None, 0.0
    for caixa in CAIXAS:
        if caixa.nome == "eye":
            continue
        outras = list(map(_luminancia, _pixels_das_faces(f, caixa.nome)))
        amplitude = max(outras) - min(outras)
        if amplitude > pior_amplitude:
            pior_nome, pior_amplitude = caixa.nome, amplitude

    if amplitude_do_olho < pior_amplitude * FATOR_DE_CONTRASTE_DO_OLHO:
        raise geo.ErroDeArte(
            "o olho tem amplitude de luminancia %.1f e a caixa '%s' tem %.1f: o exigido e %.1f "
            "(%.1fx). Com duas regioes disputando o olhar, o ponto fraco deixa de ser o que se "
            "acha a distancia, e nada no jogo acusa isso"
            % (amplitude_do_olho, pior_nome, pior_amplitude,
               pior_amplitude * FATOR_DE_CONTRASTE_DO_OLHO, FATOR_DE_CONTRASTE_DO_OLHO))

    print("contraste: olho %.1f  vs  '%s' %.1f  (razao %.2fx, minimo %.2fx)"
          % (amplitude_do_olho, pior_nome, pior_amplitude,
             amplitude_do_olho / pior_amplitude, FATOR_DE_CONTRASTE_DO_OLHO))


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_olho_na_altura_do_ponto_fraco,
                              valida_contraste_unico_do_olho))
