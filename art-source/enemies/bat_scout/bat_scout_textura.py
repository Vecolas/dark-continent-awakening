"""Folha do Bat Scout -- pelo escuro, membrana translucida e DOIS OLHOS enormes.

A DECISAO DE LEITURA DESTA FOLHA CABE NUMA FRASE: **o unico contraste alto da
folha e o olho, e o olho toma a cara.** Tudo o mais -- pelo, membrana, orelha,
garra -- vive numa faixa estreita de luminancia media. O olho sai dessa faixa nas
duas pontas, com uma esclera quase branca e uma pupila quase preta encostadas uma
na outra.

POR QUE O OLHO TEM DE SER GRANDE, e nao so contrastado. `ChimeraPeonDefinitions`
GARANTE o trait `NIGHT_VISION` a toda formiga desta familia, e
`RegrasDeVisaoNoturna` transforma esse trait numa consequencia mecanica: **o
alcance de percepcao deste bicho nao cai no escuro.** Um mob comum perde alcance
na penumbra; este nao perde nada.

Essa e uma regra que o jogador NUNCA vai ler num numero. Ele so pode aprende-la
por duas vias: apanhando repetidamente a noite ate desconfiar, ou olhando para o
bicho e reconhecendo, antes de chegar perto, um animal feito para enxergar no
escuro. A segunda via e o desenho -- e num corpo de 0.8 x 0.9 bloco a unica forma
de dizer "esse enxerga no escuro" e o olho TOMAR a cara. Um par de pontinhos de 1
px seria mecanicamente identico e leria como rato.

Por isso `valida_olho_de_visao_noturna` cobra tres coisas, e nenhuma delas
levanta excecao em lugar nenhum do jogo:

  1. AREA -- o olho ocupa pelo menos FRACAO_MINIMA_DO_OLHO da cara. Menos que
     isso e um morcego que enxerga no escuro sem parecer que enxerga, e a regra
     deixa de ter aviso.
  2. VAZAMENTO -- nenhum tom do olho aparece fora da cara. Um respingo de esclera
     numa garra ou numa membrana poe um segundo ponto claro na silhueta, e a
     vinte blocos o jogador passa a achar dois alvos onde ha um bicho.
  3. AMPLITUDE -- a diferenca entre o pixel mais claro e o mais escuro da cara
     supera a de qualquer outra peca por FATOR_DE_CONTRASTE_DO_OLHO. Empatado com
     a garra ou com o veio da membrana, o olho deixa de ser o que se acha
     primeiro.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

Regerar:  python art-source/enemies/bat_scout/bat_scout_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/bat_scout/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import geometria as geo      # noqa: E402
from comum import textura as tex        # noqa: E402
from bat_scout_geo import CAIXAS, MOB, UV_ALTURA, UV_LARGURA  # noqa: E402

# Fracao MINIMA da cara que o olho tem de ocupar.
#
# Limite de LEITURA, e nao botao de balanceamento: ninguem gira isto numa sessao
# de ajuste. A cara deste bicho tem 4x4 px -- 16 pixels no total, para tudo. Com
# menos de um terco deles no olho, a silhueta a distancia deixa de dizer "animal
# noturno" e passa a dizer "roedor", e a regra de visao noturna vira surpresa.
FRACAO_MINIMA_DO_OLHO = 0.30

# Quantas vezes a amplitude de luminancia da cara tem de superar a da peca mais
# contrastada do resto do corpo. 1.6 e um degrau que se enxerga; com 1.2 duas
# regioes disputam o olhar, e com 3.0 o resto do bicho teria de ser chapado --
# e chapado le como plastico.
FATOR_DE_CONTRASTE_DO_OLHO = 1.6

PALETA = tex.Paleta(
    PELO=(86, 74, 68),           # o corpo, visto de frente
    PELO_LUZ=(110, 96, 88),      # o que pega luz de cima
    PELO_SOMBRA=(58, 50, 47),    # o que fica por baixo, atras e dentro da orelha
    MEMBRANA=(104, 78, 84),      # a asa: rosada e fina, para ler diferente de pelo
    MEMBRANA_VEIO=(74, 54, 60),  # os veios e o lado de baixo da asa
    ORELHA=(128, 100, 100),      # a face interna da orelha
    GARRA=(150, 142, 130),       # o polegar da asa e as unhas dos pes
    # --- os dois tons abaixo so podem aparecer na FRENTE da cabeca ---
    ESCLERA=(238, 228, 186),     # o branco do olho: o ponto mais claro da folha
    PUPILA=(24, 20, 28),         # o miolo: o ponto mais escuro da folha
)

# Os tons que a regua prende dentro da cara. Declarados uma vez, e nao repetidos
# na validacao: duas listas divergem, e a divergencia faria a regua deixar de
# vigiar justamente o tom que alguem acabou de espalhar.
TONS_DO_OLHO = ("ESCLERA", "PUPILA")

# A UNICA face onde os tons do olho podem estar.
CAIXA_DA_CARA, FACE_DA_CARA = "head", "frente"

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def _pelo(p, salpico):
    """O tratamento comum de toda peca de pelo.

    Topo claro, base e traseira escuras: e a sombra que qualquer corpo precisa
    para nao ler como um bloco de cor chapada visto de cima. `salpico` e a
    semente -- semente repetida entre duas pecas faria as duas receberem o MESMO
    mosqueado, e padrao repetido le como textura de parede, nao como pelo.
    """
    p.tudo(PALETA.PELO)
    p.face("topo", PALETA.PELO_LUZ)
    p.face("base", PALETA.PELO_SOMBRA)
    p.face("tras", PALETA.PELO_SOMBRA)
    for face in tex.PAREDES:
        p.salpicar(face, PALETA.PELO_SOMBRA, 58, semente=salpico)
        p.salpicar(face, PALETA.PELO_LUZ, 28, semente=salpico + 7)


def _membrana(p, semente, veios):
    """A asa: base rosada, veios em FIADAS e a barra do braco no topo.

    Veio em fiada, e nao salpicado: salpico a distancia le como sujeira, e fiada
    le como membrana esticada entre dedos. A barra de pelo no topo existe para a
    asa aberta nao virar um retangulo solto -- ela e o braco, e o braco tem de
    aparecer preso ao corpo.
    """
    p.tudo(PALETA.MEMBRANA)
    p.face("base", PALETA.MEMBRANA_VEIO)
    p.face("tras", PALETA.MEMBRANA_VEIO)
    for face in tex.LADOS:
        p.faixa_no_topo(face, 1, PALETA.PELO)
        for dy in veios:
            p.linha(face, dy, PALETA.MEMBRANA_VEIO)
        p.salpicar(face, PALETA.MEMBRANA_VEIO, 26, semente=semente)
    p.face("topo", PALETA.PELO)


def pintar():
    # --------------------------------------------------------------- corpo
    _pelo(folha["body"], 3)

    # --------------------------------------------------------------- cabeca
    # A cara e um painel de 4x4 px, e dele 8 px sao olho. Essa proporcao e o mob:
    # o que sobra e uma sobrancelha e um focinho, e mais nada cabe.
    cabeca = folha["head"]
    _pelo(cabeca, 11)
    # Sobrancelha e focinho escuros: eles existem para o olho ter BORDA. Olho
    # claro encostando direto no pelo medio perde a silhueta a distancia.
    cabeca.linha("frente", 0, PALETA.PELO_SOMBRA)
    cabeca.linha("frente", 3, PALETA.PELO_SOMBRA)
    # Os dois olhos, com forca=0: ruido na esclera ou na pupila borraria a leitura
    # a distancia, que e a unica coisa que esta peca entrega.
    cabeca.na_face("frente", 0, 1, 2, 2, PALETA.ESCLERA, forca=0)
    cabeca.na_face("frente", 2, 1, 2, 2, PALETA.ESCLERA, forca=0)
    # As pupilas ficam nas colunas INTERNAS (1 e 2). Nas externas o bicho olharia
    # para os lados nos dois olhos ao mesmo tempo, e um predador que nunca encara
    # nada le como presa.
    cabeca.ponto("frente", 1, 2, PALETA.PUPILA)
    cabeca.ponto("frente", 2, 2, PALETA.PUPILA)

    # -------------------------------------------------------------- orelhas
    # A face INTERNA e declarada por orelha, e nao por uma lista unica: na face
    # 'direita' a frente do bicho fica na borda direita e na 'esquerda' na borda
    # esquerda. Pintar a mesma face nas duas orelhas nao da erro -- da um bicho
    # com uma orelha virada do avesso, e ninguem descobre isso sem girar a camera.
    for nome, interna, semente in (("ear_left", "direita", 19),
                                   ("ear_right", "esquerda", 23)):
        orelha = folha[nome]
        orelha.tudo(PALETA.PELO_SOMBRA)
        orelha.face(interna, PALETA.ORELHA)
        orelha.face("frente", PALETA.ORELHA)
        # A crista central: sem ela a orelha e um retangulo claro, e retangulo
        # claro na cabeca disputa o olhar com o olho.
        orelha.linha_central(interna, 1, PALETA.PELO_SOMBRA)
        orelha.salpicar(interna, PALETA.PELO_SOMBRA, 22, semente=semente)

    # ----------------------------------------------------------------- asas
    # Os veios ficam em fiadas IMPARES para nao encostarem na barra do braco
    # (fiada 0) nem na borda de baixo.
    _membrana(folha["wing_left"], 31, veios=(3, 5, 7))
    _membrana(folha["wing_right"], 41, veios=(3, 5, 7))

    for nome, semente in (("wing_left_tip", 47), ("wing_right_tip", 53)):
        ponta = folha[nome]
        _membrana(ponta, semente, veios=(1,))
        # O polegar da asa: a unica garra visivel de perfil quando ela abre. Ela
        # e pequena de proposito -- GARRA e o tom mais claro fora do olho, e uma
        # area grande dele disputaria o olhar.
        ponta.ponto("topo", 0, 0, PALETA.GARRA)
        ponta.ponto("frente", 0, 0, PALETA.GARRA)

    # ------------------------------------------------------------------ pes
    for nome, semente in (("foot_left", 61), ("foot_right", 67)):
        pe = folha[nome]
        pe.tudo(PALETA.PELO_SOMBRA)
        pe.salpicar("frente", PALETA.PELO, 30, semente=semente)
        # As unhas ficam no PE da peca, com forca=0: e o pe que encosta no galho,
        # e um morcego sem unha nao explica como ele fica pendurado.
        for face in tex.PAREDES:
            pe.faixa_no_pe(face, 1, PALETA.GARRA, forca=0)


# ------------------------------------------ validacao que liga arte e regra

def _luminancia(pixel):
    """Luminancia perceptual, a mesma conta que o olho humano faz de longe.

    Media simples dos canais mediria ERRADO: (0,255,0) e (255,0,0) tem a mesma
    media e brilhos completamente diferentes, e a regua aprovaria um verde
    berrante como se fosse tao discreto quanto um vermelho escuro.
    """
    return 0.299 * pixel[0] + 0.587 * pixel[1] + 0.114 * pixel[2]


def _pixels_da_face(f, caixa, face):
    """Os pixels de UMA face de UMA caixa."""
    x, y, largura, altura = f[caixa].faces()[face]
    for i in range(x, x + largura):
        for j in range(y, y + altura):
            yield f.px[i, j][:3]


def _pixels_das_faces(f, caixa):
    """So os pixels das SEIS faces -- nunca os cantos do layout.

    O retangulo de uma caixa tem cantos que nenhuma face usa e que ficam
    transparentes de proposito. Medi-los faria toda amplitude comecar do preto e
    a regua de contraste aprovaria qualquer coisa.
    """
    for face in tex.FACES:
        for pixel in _pixels_da_face(f, caixa, face):
            yield pixel


def valida_olho_de_visao_noturna(f):
    """O olho tem de TOMAR a cara, ficar so nela, e ser o unico contraste alto.

    As tres cobrancas estao juntas porque as tres sustentam a mesma promessa: o
    trait NIGHT_VISION e garantido a esta familia, `RegrasDeVisaoNoturna` faz o
    alcance de percepcao NAO cair no escuro, e o desenho e a unica coisa que
    avisa o jogador disso antes de ele apanhar. Separadas em tres funcoes, uma
    delas acabaria rodando sem as outras, e a promessa se quebra pelo elo que
    sobrou de fora.
    """
    # 1. AREA
    tons_do_olho = {getattr(PALETA, nome) for nome in TONS_DO_OLHO}
    pixels_da_cara = list(_pixels_da_face(f, CAIXA_DA_CARA, FACE_DA_CARA))
    do_olho = sum(1 for pixel in pixels_da_cara if pixel in tons_do_olho)
    fracao = do_olho / float(len(pixels_da_cara))
    if fracao < FRACAO_MINIMA_DO_OLHO:
        raise geo.ErroDeArte(
            "o olho ocupa %d de %d px da cara (%.0f%%) e o minimo de leitura e %.0f%%: o bicho "
            "continua enxergando no escuro pela regra e para de PARECER que enxerga, e o jogador "
            "so descobre a regra apanhando a noite sem saber por que"
            % (do_olho, len(pixels_da_cara), 100.0 * fracao, 100.0 * FRACAO_MINIMA_DO_OLHO))

    # 2. VAZAMENTO
    proibidos = {getattr(PALETA, nome): nome for nome in TONS_DO_OLHO}
    for caixa in CAIXAS:
        for face in tex.FACES:
            if caixa.nome == CAIXA_DA_CARA and face == FACE_DA_CARA:
                continue
            for pixel in _pixels_da_face(f, caixa.nome, face):
                if pixel in proibidos:
                    raise geo.ErroDeArte(
                        "o tom %s do olho %s aparece na face '%s' da caixa '%s': um segundo ponto "
                        "de contraste alto rouba o olhar, e a vinte blocos o jogador passa a achar "
                        "dois alvos onde ha um bicho"
                        % (proibidos[pixel], pixel, face, caixa.nome))

    # 3. AMPLITUDE
    luzes_da_cara = list(map(_luminancia, _pixels_das_faces(f, CAIXA_DA_CARA)))
    amplitude_da_cara = max(luzes_da_cara) - min(luzes_da_cara)
    pior_nome, pior_amplitude = None, 0.0
    for caixa in CAIXAS:
        if caixa.nome == CAIXA_DA_CARA:
            continue
        outras = list(map(_luminancia, _pixels_das_faces(f, caixa.nome)))
        amplitude = max(outras) - min(outras)
        if amplitude > pior_amplitude:
            pior_nome, pior_amplitude = caixa.nome, amplitude

    if amplitude_da_cara < pior_amplitude * FATOR_DE_CONTRASTE_DO_OLHO:
        raise geo.ErroDeArte(
            "a cara tem amplitude de luminancia %.1f e a caixa '%s' tem %.1f: o exigido e %.1f "
            "(%.1fx). Com duas regioes disputando o olhar, o olho deixa de ser o que se acha a "
            "distancia, e nada no jogo acusa isso"
            % (amplitude_da_cara, pior_nome, pior_amplitude,
               pior_amplitude * FATOR_DE_CONTRASTE_DO_OLHO, FATOR_DE_CONTRASTE_DO_OLHO))

    print("olho: %d de %d px da cara (%.0f%%)   contraste: cara %.1f  vs  '%s' %.1f "
          "(razao %.2fx, minimo %.2fx)"
          % (do_olho, len(pixels_da_cara), 100.0 * fracao, amplitude_da_cara, pior_nome,
             pior_amplitude, amplitude_da_cara / pior_amplitude, FATOR_DE_CONTRASTE_DO_OLHO))


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_olho_de_visao_noturna,))
