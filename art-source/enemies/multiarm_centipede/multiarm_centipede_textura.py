"""Folha do Multiarm Centipede -- quitina segmentada e UM par de bracos marcado.

A DECISAO DE LEITURA DESTA FOLHA CABE NUMA FRASE: **o unico contraste alto da
folha e o par de bracos DIANTEIRO.** Tudo o mais -- carapaca, ventre, mandibula,
perna, os outros quatro bracos -- vive numa faixa estreita de luminancia media. O
par dianteiro sai dessa faixa nas duas pontas, com faixas de aviso quase brancas
encostadas em faixas quase pretas.

ISSO NAO E GOSTO: E A REGRA DESENHADA. O servidor abre tres janelas seguidas, e o
golpe FINAL -- o de telegrafo mais longo, o de dano cheio, o unico com uma janela
de punicao de 26 ticks depois dele -- e jogado pelos bracos DIANTEIROS. O jogador
precisa achar esse par no meio de seis membros que se mexem ao mesmo tempo, a dez
blocos de distancia, sem tutorial. Se um segundo contraste alto aparecer na folha
-- uma mandibula branca, uma placa metalica no dorso, uma perna clara -- o olhar
se divide e o jogador passa a nao saber qual braco esta prestes a jogar o golpe
que importa. Nao da erro nenhum, nao aparece no atlas aberto num editor, e so se
manifesta como "essa sequencia e impossivel de ler".

Por isso `valida_par_dianteiro_e_o_unico_alto_contraste` mede a folha inteira e
reprova quem empatar com o par dianteiro -- e reprova tambem o VAZAMENTO, um tom
de aviso pintado fora dele.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

Regerar:  python art-source/enemies/multiarm_centipede/multiarm_centipede_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/multiarm_centipede/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import geometria as geo      # noqa: E402
from comum import textura as tex        # noqa: E402
from multiarm_centipede_geo import (CAIXAS, GOLPES_DA_SEQUENCIA, MOB,  # noqa: E402
                                    UV_ALTURA, UV_LARGURA)

# Quantas vezes a amplitude de luminancia do par dianteiro tem de superar a da
# peca mais contrastada do resto do corpo.
#
# 1.5 e um degrau que se enxerga: com 1.2 duas regioes disputam o olhar e, num
# bicho com seis membros em movimento, a duvida custa o golpe; com 3.0 o resto do
# corpo teria de ser chapado, e chapado le como plastico. O numero e um limite de
# LEITURA, nao de balanceamento -- ninguem vai gira-lo numa sessao de ajuste, e
# por isso ele mora aqui, ao lado do comentario que o explica.
FATOR_DE_CONTRASTE_DO_PAR_FINAL = 1.5

# O prefixo do par que joga o ULTIMO golpe da sequencia, DERIVADO da tabela do
# servidor em vez de escrito de novo. Escrito a mao aqui, ele continuaria dizendo
# "dianteiro" no dia em que o balanceamento trocasse a ordem dos golpes -- e a
# folha passaria a destacar o par errado, com o jogador aprendendo a vigiar um
# braco que nao e mais o perigoso.
PAR_DO_GOLPE_FINAL = GOLPES_DA_SEQUENCIA[-1][0]

PALETA = tex.Paleta(
    CARAPACA=(58, 66, 52),        # o flanco, visto de frente
    CARAPACA_LUZ=(88, 98, 76),    # o dorso, o que pega luz de cima
    CARAPACA_SOMBRA=(38, 44, 34), # o que fica por baixo, atras e entre os aneis
    VENTRE=(124, 120, 96),        # a barriga e a face de baixo dos aneis
    QUITINA=(78, 72, 58),         # os membros: bracos comuns, pernas, mandibula
    GARRA=(146, 140, 116),        # pontas: mandibula, garra de braco e de perna
    OLHO=(28, 24, 26),            # o aglomerado ocular da cabeca
    # --- os dois tons abaixo so podem aparecer no par de bracos do golpe final ---
    AVISO_CLARO=(232, 214, 140),  # a faixa clara: o ponto mais claro da folha
    AVISO_ESCURO=(30, 24, 22),    # a faixa escura: o ponto mais escuro da folha
)

# Os tons que a regua prende dentro do par final. Declarados uma vez, e nao
# repetidos na validacao: duas listas divergem, e a divergencia faria a regua
# deixar de vigiar justamente o tom que alguem acabou de espalhar.
TONS_DE_AVISO = ("AVISO_CLARO", "AVISO_ESCURO")

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def _carapaca(p, salpico):
    """O tratamento comum de toda peca de casco.

    Dorso claro, base e traseira escuras: e a sombra que um corpo erguido precisa
    para nao ler como um bloco de cor chapada visto de cima. `salpico` e a
    semente -- semente repetida entre duas pecas faria as duas receberem o MESMO
    mosqueado, e um padrao repetido le como textura de parede, nao como quitina.
    """
    p.tudo(PALETA.CARAPACA)
    p.face("topo", PALETA.CARAPACA_LUZ)
    p.face("base", PALETA.VENTRE)
    p.face("tras", PALETA.CARAPACA_SOMBRA)
    for face in tex.PAREDES:
        p.salpicar(face, PALETA.CARAPACA_SOMBRA, 54, semente=salpico)
        p.salpicar(face, PALETA.CARAPACA_LUZ, 28, semente=salpico + 7)


def _aneis(p, passo, primeira=1):
    """As juntas entre os segmentos, em fiadas horizontais nas quatro paredes.

    Aneis sao o que faz um corpo comprido ler como SEGMENTADO em vez de como um
    tubo. Pintados so na frente, o bicho ficaria visivelmente liso de lado -- e
    isso ninguem descobre sem girar a camera em volta dele.
    """
    for face in tex.PAREDES:
        _, _, _, altura = p.faces()[face]
        for dy in range(primeira, altura, passo):
            p.linha(face, dy, PALETA.CARAPACA_SOMBRA, forca=2)


def _membro(p, semente, garra_no_pe=2):
    """Braco comum ou perna: quitina fosca com a ponta clara.

    A ponta clara existe porque e ela que encosta no jogador. Membro de ponta lisa
    nao explica dano nenhum -- e este bicho tem seis deles.
    """
    p.tudo(PALETA.QUITINA)
    p.face("topo", PALETA.CARAPACA_SOMBRA)
    p.face("base", PALETA.GARRA)
    for face in tex.PAREDES:
        p.salpicar(face, PALETA.CARAPACA_SOMBRA, 46, semente=semente)
        p.faixa_no_pe(face, garra_no_pe, PALETA.GARRA)


def pintar():
    # --------------------------------------------------------------- tronco
    torax = folha["thorax"]
    _carapaca(torax, 3)
    _aneis(torax, 4)
    # Uma placa dorsal mais clara no meio da coluna: e a horizontal que o corpo
    # erguido precisa para nao ler como um poste quando visto de frente.
    torax.na_face("frente", 2, 6, 6, 3, PALETA.CARAPACA_LUZ, forca=3)

    quadril = folha["hip"]
    _carapaca(quadril, 11)
    _aneis(quadril, 3)

    cauda = folha["tail"]
    _carapaca(cauda, 19)
    _aneis(cauda, 2)
    # A cauda encosta no chao: a fiada de baixo das paredes fica suja de terra,
    # que e o tom de sombra. Sem ela a cauda parece flutuar sobre o bloco.
    for face in tex.PAREDES:
        cauda.faixa_no_pe(face, 1, PALETA.CARAPACA_SOMBRA)

    # --------------------------------------------------------------- cabeca
    cabeca = folha["head"]
    _carapaca(cabeca, 29)
    # O aglomerado ocular: dois blocos escuros, e NENHUM ponto claro dentro deles.
    # Um reflexo branco no olho seria o segundo contraste alto da folha, e ele
    # roubaria o olhar do par de bracos que joga o golpe final.
    cabeca.na_face("frente", 1, 2, 2, 2, PALETA.OLHO, forca=0)
    cabeca.na_face("frente", 7, 2, 2, 2, PALETA.OLHO, forca=0)
    # A placa frontal entre os olhos, clara: e ela que da direcao a cabeca quando
    # o bicho encara o alvo no fim do telegrafo.
    cabeca.na_face("frente", 4, 1, 2, 4, PALETA.CARAPACA_LUZ, forca=2)

    for nome, semente in (("mandible_left", 37), ("mandible_right", 41)):
        mandibula = folha[nome]
        mandibula.tudo(PALETA.QUITINA)
        mandibula.face("topo", PALETA.CARAPACA_SOMBRA)
        for face in tex.PAREDES:
            mandibula.faixa_no_pe(face, 1, PALETA.GARRA)
        mandibula.salpicar("frente", PALETA.CARAPACA_SOMBRA, 40, semente=semente)

    # ------------------------------------------------ bracos que nao decidem
    # Os quatro bracos dos golpes ENCADEADOS. Eles sao quitina comum de proposito:
    # marcar todos os seis apagaria a hierarquia e o jogador voltaria a ter seis
    # alvos de atencao onde a regra so tem um que importa.
    for nome, semente in (("arm_mid_left", 53), ("arm_mid_right", 59),
                          ("arm_rear_left", 61), ("arm_rear_right", 67)):
        _membro(folha[nome], semente)

    # -------------------------------------------------- o par do golpe final
    # A UNICA peca com contraste alto da folha inteira, e a unica pintada com
    # forca=0: ruido nas faixas borraria a leitura a distancia, que e a unica
    # coisa que esta peca entrega.
    for nome in (PAR_DO_GOLPE_FINAL + "_left", PAR_DO_GOLPE_FINAL + "_right"):
        braco = folha[nome]
        braco.tudo(PALETA.QUITINA)
        braco.face("topo", PALETA.CARAPACA_SOMBRA)
        for face in tex.PAREDES:
            _, _, _, altura = braco.faces()[face]
            # Faixas alternadas ao longo do membro inteiro. Duas fiadas coladas --
            # clara sobre escura -- porque e o ENCOSTO das duas que o olho acha de
            # longe; separadas, cada uma se dilui na quitina do meio.
            for dy in range(3, altura - 3, 5):
                braco.na_face(face, 0, dy, braco.faces()[face][2], 1,
                              PALETA.AVISO_CLARO, forca=0)
                braco.na_face(face, 0, dy + 1, braco.faces()[face][2], 1,
                              PALETA.AVISO_ESCURO, forca=0)
            # A ponta que encosta: escura, para a faixa clara mais baixa ficar
            # sendo a ultima coisa clara antes da garra.
            braco.faixa_no_pe(face, 2, PALETA.AVISO_ESCURO, forca=0)
        braco.face("base", PALETA.AVISO_CLARO, forca=0)

    # ----------------------------------------------------- pernas traseiras
    for nome, semente in (("leg_rear_left", 71), ("leg_rear_right", 73)):
        perna = folha[nome]
        perna.tudo(PALETA.QUITINA)
        perna.face("topo", PALETA.CARAPACA_SOMBRA)
        perna.face("base", PALETA.CARAPACA_SOMBRA)
        for face in tex.PAREDES:
            perna.salpicar(face, PALETA.CARAPACA_SOMBRA, 58, semente=semente)


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
    transparentes de proposito. Medi-los faria toda amplitude comecar do preto e a
    regua de contraste aprovaria qualquer coisa.
    """
    pincel = f[nome]
    for face in tex.FACES:
        x, y, largura, altura = pincel.faces()[face]
        for i in range(x, x + largura):
            for j in range(y, y + altura):
                yield f.px[i, j][:3]


def valida_par_dianteiro_e_o_unico_alto_contraste(f):
    """O par que joga o GOLPE FINAL e o unico contraste alto, e isso se mede.

    Duas coisas sao cobradas, e as duas quebram a mesma promessa:

    1. VAZAMENTO -- nenhum tom de aviso pode aparecer fora do par final. Uma
       faixa clara numa mandibula ou numa perna poe um segundo ponto de atencao no
       bicho, e o jogador passa a vigiar um membro que nao decide nada.
    2. AMPLITUDE -- a diferenca entre o pixel mais claro e o mais escuro do par
       final tem de superar a de qualquer outra peca por
       FATOR_DE_CONTRASTE_DO_PAR_FINAL. Empatado com o dorso ou com a cabeca, o par
       deixa de ser o que se acha primeiro no meio de seis membros em movimento.

    Nenhuma das duas levanta excecao em lugar nenhum do jogo, nenhuma aparece no
    atlas aberto num editor, e as duas so se manifestam como "essa sequencia e
    impossivel de ler".
    """
    marcados = {PAR_DO_GOLPE_FINAL + "_left", PAR_DO_GOLPE_FINAL + "_right"}
    proibidos = {getattr(PALETA, nome): nome for nome in TONS_DE_AVISO}
    for caixa in CAIXAS:
        if caixa.nome in marcados:
            continue
        for pixel in _pixels_das_faces(f, caixa.nome):
            if pixel in proibidos:
                raise geo.ErroDeArte(
                    "o tom de aviso %s %s aparece na caixa '%s': um segundo ponto de atencao no "
                    "bicho faz o jogador vigiar um membro que nao joga o golpe final, e a "
                    "sequencia deixa de ser legivel" % (proibidos[pixel], pixel, caixa.nome))

    luzes = []
    for nome in sorted(marcados):
        luzes += list(map(_luminancia, _pixels_das_faces(f, nome)))
    amplitude_do_par = max(luzes) - min(luzes)

    pior_nome, pior_amplitude = None, 0.0
    for caixa in CAIXAS:
        if caixa.nome in marcados:
            continue
        outras = list(map(_luminancia, _pixels_das_faces(f, caixa.nome)))
        amplitude = max(outras) - min(outras)
        if amplitude > pior_amplitude:
            pior_nome, pior_amplitude = caixa.nome, amplitude

    if amplitude_do_par < pior_amplitude * FATOR_DE_CONTRASTE_DO_PAR_FINAL:
        raise geo.ErroDeArte(
            "o par '%s' tem amplitude de luminancia %.1f e a caixa '%s' tem %.1f: o exigido e %.1f "
            "(%.1fx). Com duas regioes disputando o olhar, o jogador nao acha qual dos seis bracos "
            "joga o golpe final, e nada no jogo acusa isso"
            % (PAR_DO_GOLPE_FINAL, amplitude_do_par, pior_nome, pior_amplitude,
               pior_amplitude * FATOR_DE_CONTRASTE_DO_PAR_FINAL, FATOR_DE_CONTRASTE_DO_PAR_FINAL))

    print("contraste: par '%s' %.1f  vs  '%s' %.1f  (razao %.2fx, minimo %.2fx)"
          % (PAR_DO_GOLPE_FINAL, amplitude_do_par, pior_nome, pior_amplitude,
             amplitude_do_par / pior_amplitude, FATOR_DE_CONTRASTE_DO_PAR_FINAL))


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_par_dianteiro_e_o_unico_alto_contraste,))
