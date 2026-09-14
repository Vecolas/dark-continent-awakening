"""Geometria do Bubble Horse -- o cavalo que SO VALE VIVO (issue #119, Greed Island).

O QUE O CORPO PRECISA CONTAR, e o que acontece se ele nao contar.

Este bicho nao luta: ele FOGE, em saltos flutuantes. A condicao de card dele e
`CaptureCondition.porEnfraquecimento()` -- um quarto de vida e NAO PODE MORRER --
e isso faz do encontro um problema de PARAR de bater na hora certa. Tudo que a
silhueta faz existe para servir essa leitura.

  * AS BOLHAS SAO MODELO, E NAO PARTICULA. Quatro delas sao o apoio no chao (o
    bicho nao tem casco) e duas carregam o volume do dorso e da nuca. Feitas de
    particula, elas sumiriam no LOD baixo e nos frames de voo -- que sao
    justamente os frames em que o jogador esta lendo para onde o proximo salto
    vai. Particula e acabamento; silhueta e geometria.
  * O CORPO E LEVE. Nove px de largura de tronco num bicho de 1.2 bloco: ele
    precisa ler como algo que se esquiva, e nao como algo que aguenta pancada.
    Um tronco pesado prometeria um corpo que sobrevive a um golpe a mais, e esse
    golpe a mais e exatamente o que perde o card.
  * A PATADA SAI DA FRENTE. O dano 3 e o recurso de quem foi encurralado: ele
    empina e desce as patas DIANTEIRAS. A caixa de dano do servidor vai a 1.0
    bloco a frente, e `valida_alcance_da_patada` cobra que a perna desenhada
    chegue la -- senao o jogador apanha de uma pata que, na tela, parou antes
    dele.

A HITBOX MANDA NO MODELO. A entidade e sized(1.2F, 1.8F) -- 19.2 x 28.8 px -- e a
bateria da biblioteca reprova se o modelo estourar isso. O piso das bolhas de
apoio fica em y=0.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de AttackHitbox a frente e +Z, porque com yaw 0 o olhar
vanilla aponta para +Z. Misturar as duas ja custou um bug neste repositorio -- a
caixa do golpe ficou ATRAS do mob, que atacava, animava e nao encostava em
ninguem na frente. Ver o comentario de BubbleHorseTuning.CAIXA_DA_PATADA.

Regerar:  python art-source/enemies/bubble_horse/bubble_horse_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/bubble_horse.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "bubble_horse"
UV_LARGURA, UV_ALTURA = 128, 64

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(1.2F, 1.8F).
HITBOX = (1.2, 1.8)

# Alcance, em BLOCOS, que a caixa de golpe do servidor reivindica a frente.
# BubbleHorseTuning.CAIXA_DA_PATADA: maxZ = 1.0D.
#
# Ele esta aqui para ser COBRADO contra o desenho. Caixa que chega mais longe do
# que a pata desenhada nao da erro nenhum: da um jogador que leva 3 de dano de uma
# pata que, na tela, parou meio bloco antes dele -- e num bicho cuja licao e
# distancia, uma pata com alcance invisivel e o pior defeito possivel.
ALCANCE_DA_PATADA_EM_BLOCOS = 1.0

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# CADA BOLHA E OSSO PROPRIO, e nao um retangulo pintado no corpo. Tres razoes, e
# as tres sao mecanicas: (a) as bolhas de apoio precisam ACHATAR na aterrissagem
# e ESTUFAR no impulso, e retangulo pintado nao reage; (b) so um osso com cubo
# tem altura e alcance MEDIVEIS, e e isso que as reguas deste arquivo comparam
# com os numeros do servidor; (c) a bolha da garupa e da nuca migrariam de lugar
# na primeira correcao de textura sem que nenhuma regua percebesse.
#
# As bolhas de apoio penduram na PERNA, e nao no corpo. Penduradas no corpo o pai
# existe, o portao Java passa, e a bolha fica PARADA no ar enquanto a perna dobra
# -- e a perna dobrando sobre uma bolha parada e o unico quadro que conta ao
# jogador que o proximo salto ja comecou.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 16.5, 0)),
    geo.Osso("neck", "body", (0, 19, -4.5)),
    geo.Osso("head", "neck", (0, 24.5, -5)),
    geo.Osso("bubble_nape", "neck", (0, 23, -4.5)),
    geo.Osso("tail", "body", (0, 18.5, 4.5)),
    geo.Osso("bubble_croup", "body", (0, 19.5, 4)),
    geo.Osso("leg_front_left", "body", (2.5, 14, -2.5)),
    geo.Osso("leg_front_right", "body", (-2.5, 14, -2.5)),
    geo.Osso("leg_back_left", "body", (2.5, 14, 2.5)),
    geo.Osso("leg_back_right", "body", (-2.5, 14, 2.5)),
    geo.Osso("bubble_front_left", "leg_front_left", (2.5, 5, -2.5)),
    geo.Osso("bubble_front_right", "leg_front_right", (-2.5, 5, -2.5)),
    geo.Osso("bubble_back_left", "leg_back_left", (2.5, 5, 2.5)),
    geo.Osso("bubble_back_right", "leg_back_right", (-2.5, 5, 2.5)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
#
# TODAS as origens laterais sao meio-pixel deslocadas entre pecas vizinhas (o
# tronco em -4.5, as pernas em -4, as bolhas em -5). Isso NAO e capricho: duas
# caixas com a mesma face no mesmo plano cintilam em jogo, e o jogador le
# z-fighting como bug de video. `valida_faces_coplanares` reprova; o meio pixel e
# o que faz nao acontecer.
CAIXAS = (
    # tronco curto e estreito -- um corpo de fuga, nao de briga
    geo.Caixa("body", "body", 0, 0, -4.5, 13, -5, 9, 7, 10),
    # pescoco QUASE vertical: o bicho carrega a cabeca alta porque ele passa a
    # vida olhando para tras. Pescoco horizontal leria como animal pastando.
    geo.Caixa("neck", "neck", 62, 0, -2, 19, -6, 4, 6, 3),
    # cabeca pequena, no alto da hitbox: ela e o que aparece primeiro por cima do
    # mato, e e o sinal de que o bicho ja viu o jogador.
    geo.Caixa("head", "head", 40, 0, -2.5, 23, -9.5, 5, 5, 5),
    # bolha da nuca: encosta o pescoco na cabeca e fecha a silhueta de perfil
    geo.Caixa("bubble_nape", "bubble_nape", 56, 18, -3, 21, -7.5, 6, 5, 5),
    # cauda curta -- ela existe para o traseiro ter direcao legivel de longe
    geo.Caixa("tail", "tail", 108, 0, -1, 17, 5, 2, 2, 4),
    # a MAIOR bolha, na garupa: e ela que da ao bicho a silhueta redonda que se
    # reconhece a vinte blocos, e e ela que o jogador ve subir no salto.
    geo.Caixa("bubble_croup", "bubble_croup", 78, 0, -3.5, 19, 1, 7, 7, 7),
    # pernas finas que NAO tocam o chao: ver valida_bolhas_sustentam_o_bicho
    geo.Caixa("leg_front_left", "leg_front_left", 0, 18, 1, 4, -4, 3, 10, 3),
    geo.Caixa("leg_front_right", "leg_front_right", 14, 18, -4, 4, -4, 3, 10, 3),
    geo.Caixa("leg_back_left", "leg_back_left", 28, 18, 1, 4, 1, 3, 10, 3),
    geo.Caixa("leg_back_right", "leg_back_right", 42, 18, -4, 4, 1, 3, 10, 3),
    # as quatro bolhas de apoio: o "casco" deste bicho
    geo.Caixa("bubble_front_left", "bubble_front_left", 80, 18, 0, 0, -5, 5, 5, 5),
    geo.Caixa("bubble_front_right", "bubble_front_right", 102, 18, -5, 0, -5, 5, 5, 5),
    geo.Caixa("bubble_back_left", "bubble_back_left", 0, 32, 0, 0, 0, 5, 5, 5),
    geo.Caixa("bubble_back_right", "bubble_back_right", 22, 32, -5, 0, 0, 5, 5, 5),
)

# As bolhas, por papel. Declaradas aqui porque o gerador de TEXTURA precisa
# exatamente desta particao para cobrar a separacao de luminancia -- e uma
# segunda lista escrita la divergiria na primeira bolha nova, deixando a regua
# vigiando uma peca a menos sem que nada acusasse.
BOLHAS_DE_APOIO = ("bubble_front_left", "bubble_front_right",
                   "bubble_back_left", "bubble_back_right")
BOLHAS_DE_VOLUME = ("bubble_croup", "bubble_nape")
BOLHAS = BOLHAS_DE_APOIO + BOLHAS_DE_VOLUME

# Tudo que e carne. O complemento de BOLHAS, escrito por extenso porque a regua
# de luminancia precisa citar o nome da peca que empatou.
PELAGEM = ("body", "neck", "head", "tail",
           "leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right")


# --------------------------------------------------- validacoes DO BICHO
# As duas abaixo ligam o DESENHO a REGRA DO SERVIDOR. Nenhuma das divergencias
# que elas pegam levanta excecao em lugar nenhum.

def valida_alcance_da_patada(m):
    """A pata DIANTEIRA desenhada alcanca tao longe quanto a caixa de golpe.

    A perna gira em torno do ombro; quando ela passa da horizontal, a ponta da
    bolha de apoio fica a (ombro_y - bolha_y) px a frente do pivo, e o pivo esta a
    (-pivo_z) px a frente do centro do bicho. A soma e a distancia que o DESENHO
    promete.

    Se a caixa de dano do servidor reivindicar mais do que isso, o jogador leva
    pancada de uma pata que, na tela, parou antes dele. Nao da erro: da um mob com
    alcance invisivel. Num bicho cuja unica defesa do jogador e ler distancia,
    essa mentira apaga a licao inteira -- e o relato que chega e "ele acerta de
    longe as vezes".
    """
    _, ombro_y, ombro_z = m.osso("leg_front_left").pivot
    _, (bolha_y0, _), _ = geo.volume(m.caixa("bubble_front_left"))
    alcance_px = (-ombro_z) + (ombro_y - bolha_y0)
    exigido_px = ALCANCE_DA_PATADA_EM_BLOCOS * 16.0
    if alcance_px < exigido_px:
        raise geo.ErroDeArte(
            "a pata dianteira alcanca %.1f px (%.2f blocos) a partir do centro e a caixa de golpe "
            "do servidor vai ate %.1f px (%.2f blocos): o jogador apanha de uma pata que parou "
            "antes dele, e nada acusa"
            % (alcance_px, alcance_px / 16.0, exigido_px, ALCANCE_DA_PATADA_EM_BLOCOS))


def valida_bolhas_sustentam_o_bicho(m):
    """SO as bolhas tocam o chao -- nenhuma perna chega a y=0.

    Este bicho nao caminha: a locomocao dele e impulso com pausa
    (`RegrasDeSaltoDeBolha`), e o servidor nunca produz um passo continuo. Uma
    perna desenhada apoiada no chao prometeria uma passada que o servidor nao
    executa, e o resultado e um cavalo que parece patinar -- que o jogador le como
    bug de animacao, e nao como a mecanica que ele precisa aprender a prever.

    A regua cobra os dois lados: as quatro bolhas de apoio TEM de encostar em
    y=0 (senao o bicho flutua sem nada sob ele), e nenhuma outra peca pode.
    """
    for nome in BOLHAS_DE_APOIO:
        _, (y0, _), _ = geo.volume(m.caixa(nome))
        if y0 != 0:
            raise geo.ErroDeArte(
                "a bolha de apoio '%s' comeca em y=%s e nao em y=0: o bicho fica pendurado no ar "
                "sem nada que explique onde ele se apoia, e o salto passa a parecer voo" % (nome, y0))
    for caixa in m.caixas:
        if caixa.nome in BOLHAS_DE_APOIO:
            continue
        _, (y0, _), _ = geo.volume(caixa)
        if y0 <= 0:
            raise geo.ErroDeArte(
                "a peca '%s' encosta o chao (y=%s) e so as bolhas de apoio podem: perna no chao "
                "promete uma passada que o servidor nunca executa -- a locomocao dele e impulso "
                "com pausa -- e o sintoma e um cavalo que parece patinar" % (caixa.nome, y0))


def valida_bolhas_estao_na_silhueta(m):
    """As bolhas tem de ser volume de verdade, e nao enfeite colado.

    O criterio e grosseiro de proposito: a soma do volume das bolhas nao pode ser
    menor que a do tronco. Bolha menor que isso deixa de mudar a silhueta a
    distancia, e o bicho volta a ler como 'um potro qualquer' -- que e exatamente
    a leitura que faz o jogador trata-lo como mob comum e mata-lo sem perceber
    que perdeu o card.
    """
    def volume_de(nome):
        c = m.caixa(nome)
        return c.w * c.h * c.d

    bolhas = sum(volume_de(n) for n in BOLHAS)
    tronco = volume_de("body")
    if bolhas < tronco:
        raise geo.ErroDeArte(
            "as bolhas somam %d px cubicos e o tronco sozinho tem %d: as bolhas deixaram de ser a "
            "silhueta do bicho, e a distancia ele volta a ler como um potro comum -- que e a "
            "leitura que faz o jogador mata-lo sem saber que perdeu o card" % (bolhas, tronco))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_alcance_da_patada,
                          valida_bolhas_sustentam_o_bicho,
                          valida_bolhas_estao_na_silhueta))
