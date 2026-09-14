"""Folha do Radio Rat.

A PALETA INTEIRA E SURDA, MENOS UMA COR. Pelo cinza-terra, ventre claro, focinho
quase preto: um rato tem de sumir no chao de Greed Island, porque e isso que faz
dele um problema -- quem nao o ve, nao o mata, e o relatorio sai. O UNICO tom
saturado da folha e o ambar da travessa da antena, e ele existe para uma coisa
so: dizer ao jogador, a distancia e no meio de um bando, ONDE esta a peca que
denuncia.

Por isso `valida_alerta_so_na_travessa` morde dos dois lados. Se o ambar vazar
para o corpo, o unico ponto de contraste da folha deixa de apontar para a antena
e o bicho vira mais um mob colorido. Se a travessa DEIXAR de ser ambar, o
mensageiro se dissolve no bando e "matar o mensageiro" deixa de ser uma resposta
que alguem consegue executar. Nenhuma das duas da erro, e nenhuma aparece no
atlas aberto num editor -- as duas so aparecem em jogo, a distancia.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado.

Regerar:  python art-source/enemies/radio_rat/radio_rat_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/radio_rat/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import geometria as geo      # noqa: E402
from comum import textura as tex        # noqa: E402
from radio_rat_geo import CAIXAS, MOB, UV_ALTURA, UV_LARGURA  # noqa: E402

PALETA = tex.Paleta(
    PELO=(122, 106, 92),          # o flanco, e a cor que o bicho quer ser de longe
    DORSO=(88, 76, 66),           # o que se ve de cima, quando ele passa rente ao chao
    VENTRE=(168, 154, 138),       # o que se ve de baixo, e a linha que separa o perfil
    ORELHA_INTERNA=(196, 132, 132),  # a concha da orelha; a unica pele exposta
    FOCINHO=(58, 48, 44),         # focinho, solas e os pelos de guarda do dorso
    OLHO=(24, 20, 18),            # o unico preto contado da folha
    CAUDA=(146, 118, 112),        # a cauda e pelada, entao ela nao e da cor do pelo
    ANTENA=(62, 66, 72),          # o mastro: frio, para nao ler como parte do bicho
    ALERTA=(236, 176, 48),        # SO a travessa -- ver valida_alerta_so_na_travessa
)

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)

# Qual face de cada orelha e a de DENTRO, e de que lado dela fica a frente do
# bicho. Escrito como tabela porque e exatamente aqui que nasce o bicho vesgo: na
# face 'direita' a frente fica na borda DIREITA e na 'esquerda' ela fica na borda
# ESQUERDA, entao copiar o mesmo dx nos dois lados poe a concha de uma orelha
# virada para a frente e a da outra virada para tras. Isso nao da erro e nao
# aparece sem girar a camera em volta do bicho.
ORELHAS = (
    ("orelha_esquerda", "direita", "esquerda", 1),
    ("orelha_direita", "esquerda", "direita", 0),
)


def pintar():
    # --------------------------------------------------------------- tronco
    corpo = folha["corpo"]
    corpo.tudo(PALETA.PELO)
    corpo.face("topo", PALETA.DORSO)
    corpo.face("base", PALETA.VENTRE)
    # Pelos de guarda no dorso: salpico, e nao coluna. Coluna a distancia le como
    # listra -- e listra em Greed Island e outro bicho.
    corpo.salpicar("topo", PALETA.FOCINHO, 58, semente=3)
    # A linha do ventre da a volta nas quatro paredes. Pintada so na frente, o
    # rato fica com barriga de um lado so, visivel apenas de perfil invertido.
    for face in tex.PAREDES:
        corpo.faixa_no_pe(face, 1, PALETA.VENTRE)

    # --------------------------------------------------------------- cabeca
    cabeca = folha["cabeca"]
    cabeca.tudo(PALETA.PELO)
    cabeca.face("topo", PALETA.DORSO)
    cabeca.face("base", PALETA.VENTRE)
    # A cara cabe em 3x3 px. Nao ha espaco para desenhar um rato: ha espaco para
    # dois olhos e um focinho, e e isso que o cerebro completa.
    cabeca.ponto("frente", 0, 1, PALETA.OLHO)
    cabeca.ponto("frente", 2, 1, PALETA.OLHO)
    cabeca.ponto("frente", 1, 2, PALETA.FOCINHO)
    for face in tex.LADOS:
        cabeca.faixa_no_pe(face, 1, PALETA.VENTRE)

    # -------------------------------------------------------------- orelhas
    # As orelhas sao metade da silhueta deste bicho, entao elas precisam de
    # CONTRASTE INTERNO: fora escuro, dentro claro. Chapadas na cor do pelo elas
    # somem contra a cabeca justamente na distancia em que precisam ser lidas.
    for nome, dentro, fora, dx_da_frente in ORELHAS:
        orelha = folha[nome]
        orelha.tudo(PALETA.PELO)
        orelha.face(fora, PALETA.DORSO)
        orelha.face("topo", PALETA.DORSO)
        orelha.na_face(dentro, dx_da_frente, 0, 2, 2, PALETA.ORELHA_INTERNA, forca=2)

    # --------------------------------------------------------------- antena
    folha["antena"].tudo(PALETA.ANTENA, forca=2)
    # forca=0 no ambar, e nao por economia: o ruido mexeria na cor pixel a pixel e
    # a validacao abaixo -- que procura o tom EXATO -- passaria a aprovar uma
    # travessa com metade dos pixels fora do tom. Regua que nao reprova e carimbo.
    folha["antena_travessa"].tudo(PALETA.ALERTA, forca=0)

    # ---------------------------------------------------------------- patas
    for nome in ("pata_frente_esquerda", "pata_frente_direita",
                 "pata_tras_esquerda", "pata_tras_direita"):
        pata = folha[nome]
        pata.tudo(PALETA.PELO)
        pata.face("base", PALETA.FOCINHO)

    # ---------------------------------------------------------------- cauda
    cauda = folha["cauda"]
    cauda.tudo(PALETA.CAUDA, forca=4)
    # Um anel escuro por lado: e o que diferencia cauda pelada de galho.
    for face in tex.LADOS:
        cauda.ponto(face, 0, 0, PALETA.FOCINHO)


def valida_alerta_so_na_travessa(f):
    """A LIGACAO ENTRE A ARTE E A REGRA: o mensageiro tem de ser achavel.

    O Radio Rat nasce em grupo de ate quatro (SpawnCaps do radioRat, na
    GreedIslandProfiles) e a resposta que o jogo ensina e matar quem esta
    gritando antes do relatorio sair -- 20 ticks de windup, e so. Isso exige que
    o jogador ache a peca certa numa silhueta de 9.6 x 8 px, no meio de outras
    tres iguais, e o unico recurso que a folha tem para isso e um tom de alerta
    que aparece em UM lugar so.

    A regua morde dos dois lados de proposito:

      * ambar fora da travessa -- o contraste deixa de apontar para a antena, e o
        bicho vira mais um mob colorido;
      * travessa sem ambar -- o mensageiro se dissolve no bando, e a resposta que
        o encounter inteiro depende deixa de ser executavel.

    Nenhum dos dois da erro, nenhum aparece no atlas aberto num editor, e os dois
    so se manifestam em jogo, a distancia, que e onde este repositorio nao tem
    portao nenhum.
    """
    caixa = geo.achar(CAIXAS, "antena_travessa", "caixa")
    x0, y0, x1, y1 = geo.area_no_atlas(caixa)

    for x in range(f.largura):
        for y in range(f.altura):
            if f.px[x, y][:3] != PALETA.ALERTA:
                continue
            if not (x0 <= x < x1 and y0 <= y < y1):
                raise geo.ErroDeArte(
                    "o tom de alerta %s aparece em (%d,%d), fora do retangulo da travessa "
                    "(%d,%d)..(%d,%d): o unico contraste alto da folha deixa de apontar para a "
                    "antena, e achar o mensageiro no meio do bando vira sorte"
                    % (str(PALETA.ALERTA), x, y, x0, y0, x1, y1))

    pincel = f["antena_travessa"]
    for face in tex.FACES:
        fx, fy, fw, fh = pincel.faces()[face]
        for x in range(fx, fx + fw):
            for y in range(fy, fy + fh):
                if f.px[x, y][:3] != PALETA.ALERTA:
                    raise geo.ErroDeArte(
                        "a face '%s' da travessa tem %s em (%d,%d) e devia ter o tom de alerta "
                        "%s: de um angulo a antena perde o sinal, o mensageiro se confunde com o "
                        "resto do bando e 'matar quem grita' deixa de ser executavel"
                        % (face, str(f.px[x, y][:3]), x, y, str(PALETA.ALERTA)))


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_alerta_so_na_travessa,))
