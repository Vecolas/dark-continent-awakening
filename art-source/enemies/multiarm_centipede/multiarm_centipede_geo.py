"""Geometria do Multiarm Centipede -- a formiga que ataca em SEQUENCIA.

O QUE O CORPO PRECISA CONTAR, e o que acontece se ele nao contar.

A ficha deste bicho cabe numa frase: varios bracos sao VARIAS JANELAS SEGUIDAS, e
o que o jogador aprende e esperar a ULTIMA. Um corpo que nao mostre os bracos
separados apaga essa frase inteira -- e nao apaga com erro, apaga com um mob que
ataca tres vezes e parece atacar uma so, longa e confusa.

Tres coisas o corpo tem de contar, e as tres tem consequencia mecanica:

  * BRACOS SAO OSSOS PROPRIOS, aos pares, um par por golpe da sequencia. O
    servidor abre tres janelas seguidas; se os seis bracos forem um bloco unico,
    eles sobem e descem juntos e o jogador nao consegue CONTAR a sequencia. Nao
    da erro: da um telegrafo que parece ruido. `valida_bracos_cumprem_a_sequencia`
    cobra os pares e cobra o alcance de cada um.
  * O CORPO SE ERGUE. O quadril e a cauda ficam no chao, atras, e o torax sobe
    ate a cabeca -- dois tercos da altura da hitbox estao acima do quadril. E
    dessa postura que sai o alcance dos bracos dianteiros; um corpo desenhado
    deitado teria os mesmos ossos e metade do alcance, e a caixa de golpe do
    servidor passaria a prometer o que o desenho nao entrega.
  * O ALCANCE CRESCE ao longo da sequencia. O par traseiro e o mais curto e o
    dianteiro e o mais longo, exatamente como as tres caixas de golpe do
    servidor. Quem recua um passo depois do primeiro golpe ainda esta dentro do
    ultimo -- e essa e a razao de o ultimo ter o telegrafo mais longo.

A HITBOX MANDA NO MODELO. A entidade e sized(1.6F, 2.2F) -- 25.6 x 35.2 px -- e a
bateria da biblioteca reprova se o modelo estourar isso. O piso fica em y=0, e
quem o encosta e a cauda e o par de pernas traseiras.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de AttackHitbox a frente e +Z, porque com yaw 0 o olhar
vanilla aponta para +Z. Misturar as duas ja custou um bug neste repositorio -- a
caixa do golpe ficou ATRAS do mob, que atacava, animava e nao encostava em
ninguem na frente. Por isso o alcance desenhado e medido aqui em px e comparado
com o MODULO do maxZ da caixa, e nunca com o sinal dele.

Regerar:  python art-source/enemies/multiarm_centipede/multiarm_centipede_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/multiarm_centipede.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "multiarm_centipede"
UV_LARGURA, UV_ALTURA = 128, 64

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(1.6F, 2.2F).
HITBOX = (1.6, 2.2)

# A SEQUENCIA, COPIADA DO SERVIDOR, com o metodo de origem ao lado.
#
# Cada entrada e (prefixo do par de bracos, maxZ da caixa daquele golpe em
# BLOCOS). Os tres numeros saem de MultiarmCentipedeTuning.sequencia(), e estao
# aqui para serem COBRADOS contra o desenho: caixa que chega mais longe do que o
# braco desenhado nao da erro nenhum -- da um jogador que leva pancada de um
# membro que, na tela, parou meio bloco antes dele. Pior ainda numa sequencia,
# porque ele nao sabe qual dos tres golpes o acertou.
#
# A ORDEM IMPORTA e e a ordem dos golpes: traseiro, medio, dianteiro. O alcance
# cresce, e e isso que impede o jogador de escapar do ultimo golpe recuando um
# passo depois do primeiro.
GOLPES_DA_SEQUENCIA = (
    ("arm_rear", 1.10),    # MultiarmCentipedeTuning.caixaDoBracoTraseiro()  maxZ
    ("arm_mid", 1.40),     # MultiarmCentipedeTuning.caixaDoBracoMedio()     maxZ
    ("arm_front", 1.70),   # MultiarmCentipedeTuning.caixaDoBracoDianteiro() maxZ
)

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# OS SEIS BRACOS PENDURAM NO TORAX, e cada um e um osso com cubo PROPRIO. Um
# unico osso "arms" com seis cubos seria mais barato de escrever e apagaria a
# ficha do bicho: os seis subiriam no mesmo quadro, a onda pelo corpo sumiria, e
# as tres janelas do servidor passariam a ter um desenho so. Nada nisso levanta
# excecao; o sintoma e "esse mob e confuso".
#
# As pernas traseiras penduram no QUADRIL, e nao no torax. Penduradas no torax o
# pai existe, o portao Java passa, e as pernas que sustentam o bicho no chao
# passariam a subir junto com o corpo erguido -- o mob flutuaria no proprio
# telegrafo.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    # o quadril e a ancora do corpo erguido: ele fica baixo e atras, e e em torno
    # dele que a investida inteira pivota.
    geo.Osso("hip", "root", (0, 7, 5)),
    geo.Osso("tail", "hip", (0, 6, 8)),
    geo.Osso("thorax", "hip", (0, 12, 3)),
    geo.Osso("head", "thorax", (0, 28, -4)),
    geo.Osso("mandible_left", "head", (2, 27, -8)),
    geo.Osso("mandible_right", "head", (-2, 27, -8)),
    # os tres pares, de cima para baixo. O ombro de cada par e o pivot, e e dele
    # que a regua de alcance mede.
    geo.Osso("arm_front_left", "thorax", (5, 28, -3)),
    geo.Osso("arm_front_right", "thorax", (-5, 28, -3)),
    geo.Osso("arm_mid_left", "thorax", (5, 24, -1)),
    geo.Osso("arm_mid_right", "thorax", (-5, 24, -1)),
    geo.Osso("arm_rear_left", "thorax", (5, 20, 1)),
    geo.Osso("arm_rear_right", "thorax", (-5, 20, 1)),
    geo.Osso("leg_rear_left", "hip", (6, 6, 5)),
    geo.Osso("leg_rear_right", "hip", (-6, 6, 5)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
#
# A folha e 128x64: os seis bracos sozinhos ja pedem 1872 px, e espremer num
# atlas menor obrigaria a encurtar os bracos -- que sao a unica coisa que este
# bicho tem para contar.
CAIXAS = (
    # o torax ERGUIDO: 17 px de coluna entre o quadril e a cabeca. E ele que
    # coloca os ombros altos o bastante para os bracos alcancarem o que as tres
    # caixas de golpe do servidor reivindicam.
    geo.Caixa("thorax", "thorax", 0, 0, -5, 12, -4, 10, 17, 6),
    # o quadril: largo e baixo, o contrapeso do corpo erguido. Ele e a peca mais
    # larga do bicho (12 px) e e ela que da a silhueta de "bicho apoiado atras".
    geo.Caixa("hip", "hip", 32, 0, -6, 5, 2, 12, 8, 6),
    # a cauda encosta no chao (y=0) de proposito: e o terceiro ponto de apoio, e
    # sem ele o corpo erguido leria como um bicho prestes a cair para tras.
    geo.Caixa("tail", "tail", 68, 0, -5, 0, 8, 10, 7, 6),
    geo.Caixa("head", "head", 100, 0, -5, 26, -8, 10, 8, 4),
    # as mandibulas sao a peca mais a FRENTE do bicho. Elas nao entram em caixa de
    # golpe nenhuma -- quem ataca sao os bracos -- e existem para a cabeca ter
    # leitura de predador de perto, onde a silhueta inteira nao cabe na tela.
    geo.Caixa("mandible_left", "mandible_left", 104, 23, 1, 26, -11, 3, 2, 3),
    geo.Caixa("mandible_right", "mandible_right", 104, 28, -4, 26, -11, 3, 2, 3),
    # OS TRES PARES. O comprimento cresce de tras para a frente, e e esse degrau
    # que a regua compara com as tres caixas do servidor.
    geo.Caixa("arm_front_left", "arm_front_left", 0, 23, 5, 3, -5, 3, 26, 3),
    geo.Caixa("arm_front_right", "arm_front_right", 12, 23, -8, 3, -5, 3, 26, 3),
    geo.Caixa("arm_mid_left", "arm_mid_left", 24, 23, 5, 2, -2, 3, 23, 3),
    geo.Caixa("arm_mid_right", "arm_mid_right", 36, 23, -8, 2, -2, 3, 23, 3),
    geo.Caixa("arm_rear_left", "arm_rear_left", 48, 23, 5, 1, 1, 3, 20, 3),
    geo.Caixa("arm_rear_right", "arm_rear_right", 60, 23, -8, 1, 1, 3, 20, 3),
    # pernas traseiras: curtas e grossas. Elas nao atacam; elas carregam o corpo
    # erguido, e e por isso que sao mais largas que qualquer braco.
    geo.Caixa("leg_rear_left", "leg_rear_left", 72, 23, 6, 0, 3, 4, 7, 4),
    geo.Caixa("leg_rear_right", "leg_rear_right", 88, 23, -10, 0, 3, 4, 7, 4),
)


# --------------------------------------------------- validacoes DO BICHO

def _pares_de_bracos(m):
    """Os prefixos de par que o modelo de fato tem, em ordem de declaracao."""
    return tuple(o.nome[: -len("_left")] for o in m.ossos if o.nome.endswith("_left")
                 and o.nome.startswith("arm_"))


def _alcance_do_par_em_px(m, prefixo):
    """Quanto o par alcanca a FRENTE do centro do bicho, quando gira para baixo.

    O braco pende do ombro; passado da horizontal, a ponta fica a
    (ombro_y - ponta_y) px a frente do pivot, e o pivot esta a pivot_z do centro.
    Essa e a distancia que o DESENHO promete.
    """
    ombro_x, ombro_y, ombro_z = m.osso(prefixo + "_left").pivot
    _, (ponta_y, _), _ = geo.volume(m.caixa(prefixo + "_left"))
    comprimento = ombro_y - ponta_y
    # -Z e a frente: a ponta vai de ombro_z para ombro_z - comprimento, e o que
    # interessa e o quanto isso passa do centro do bicho.
    return -(ombro_z - comprimento)


def valida_bracos_cumprem_a_sequencia(m):
    """Os bracos DESENHADOS tem de entregar as tres janelas que o servidor abre.

    Tres coisas sao cobradas, e nenhuma das tres levanta excecao em lugar nenhum
    do jogo:

    1. PARES SEPARADOS -- todo par citado pela sequencia existe como dois ossos
       proprios, cada um com cubo proprio. Um osso unico com seis cubos passa em
       todo portao da biblioteca e faz os seis bracos subirem no mesmo quadro: o
       jogador perde a capacidade de CONTAR os golpes, que e a unica coisa que
       este mob ensina.
    2. UM PAR POR JANELA -- o modelo tem pelo menos tantos pares quanto a
       sequencia tem golpes. Com menos pares, o mesmo membro teria de jogar dois
       golpes seguidos, e duas janelas com o mesmo desenho leem como uma so.
    3. ALCANCE -- o par que joga cada golpe alcanca pelo menos tao longe quanto a
       caixa daquele golpe reivindica. Caixa maior que o membro da um jogador que
       apanha de um braco que, na tela, parou antes dele -- e numa sequencia ele
       nem sabe qual dos golpes o acertou.
    """
    pares = _pares_de_bracos(m)
    if len(pares) < len(GOLPES_DA_SEQUENCIA):
        raise geo.ErroDeArte(
            "o modelo tem %d par(es) de bracos e a sequencia do servidor tem %d golpes (%s): o "
            "mesmo membro teria de jogar dois golpes seguidos, e duas janelas com o mesmo desenho "
            "leem como uma so -- o jogador perde a contagem, que e o que este mob ensina"
            % (len(pares), len(GOLPES_DA_SEQUENCIA), list(pares)))

    for prefixo, alcance_em_blocos in GOLPES_DA_SEQUENCIA:
        for lado in ("_left", "_right"):
            nome = prefixo + lado
            # m.osso e m.caixa ja levantam ErroDeArte com a lista de nomes
            # conhecidos; chamar os dois e o que prova que o braco e osso PROPRIO
            # com cubo PROPRIO, e nao um cubo pendurado no torax.
            m.osso(nome)
            if m.caixa(nome).osso != nome:
                raise geo.ErroDeArte(
                    "a caixa '%s' pendura no osso '%s': o braco deixa de ter osso proprio e o par "
                    "nao pode mais ser movido sozinho, entao a janela dele nao tem desenho"
                    % (nome, m.caixa(nome).osso))

        alcance_px = _alcance_do_par_em_px(m, prefixo)
        exigido_px = alcance_em_blocos * 16.0
        if alcance_px < exigido_px:
            raise geo.ErroDeArte(
                "o par '%s' alcanca %.1f px (%.2f blocos) a frente do centro e a caixa daquele "
                "golpe vai ate %.1f px (%.2f blocos): o jogador apanha de um braco que parou antes "
                "dele, e numa sequencia de tres ele nem sabe qual golpe o acertou"
                % (prefixo, alcance_px, alcance_px / 16.0, exigido_px, alcance_em_blocos))

    # O alcance tem de CRESCER na ordem da sequencia. Se ele encolher, recuar um
    # passo depois do primeiro golpe tiraria o jogador de todos os seguintes, e o
    # telegrafo longo do ultimo -- que existe para ser punido de perto -- passaria
    # a nunca ser visto. Nao da erro: da uma sequencia que o jogador aprende a
    # ignorar andando para tras.
    anterior = None
    for prefixo, _ in GOLPES_DA_SEQUENCIA:
        atual = _alcance_do_par_em_px(m, prefixo)
        if anterior is not None and atual <= anterior:
            raise geo.ErroDeArte(
                "o par '%s' alcanca %.1f px e o golpe anterior ja alcancava %.1f px: com o alcance "
                "encolhendo, recuar um passo depois do primeiro golpe tira o jogador de todos os "
                "seguintes, e o telegrafo longo do ultimo nunca chega a ser visto"
                % (prefixo, atual, anterior))
        anterior = atual

    print("alcance dos pares: %s" % "  ".join(
        "%s %.2f blocos (caixa %.2f)" % (p, _alcance_do_par_em_px(m, p) / 16.0, c)
        for p, c in GOLPES_DA_SEQUENCIA))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_bracos_cumprem_a_sequencia,))
