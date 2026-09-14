"""Geometria do Cyclops -- o gigante de UM olho (issue #119, familia Greed Island).

O QUE O CORPO PRECISA CONTAR, e o que acontece se ele nao contar.

O bicho tem 4.2 blocos. A escala nao e enfeite: e o primeiro aviso que o jogador
recebe, e ela chega antes de qualquer numero de ficha. Um ciclope do tamanho de
um zumbi teria HP 120 e dano 14 sem NENHUM sinal disso na tela -- e o relato que
chega e "esse mob mata do nada".

A segunda coisa que o corpo conta e o OLHO UNICO, e essa e a licao do encontro
inteiro. O cone de visao da entidade e de 60 graus de meia-abertura, METADE do
que um mob comum enxerga (o boneco de treino usa 75). Circular por fora funciona;
correr de frente nao. Para que essa licao seja legivel, o olho precisa ser:

  * UNICO -- dois olhos leriam como gigante comum, e a silhueta passaria a
    prometer visao normal enquanto a regra entrega meia;
  * no EIXO -- o cone do servidor e simetrico em torno do olhar; um olho pintado
    de lado faria o desenho apontar para um lugar e a regra medir outro, e nada
    acusaria isso;
  * NA FRENTE e NO ALTO -- e onde o WeakPointResolver paga o multiplicador.

A terceira e o PORRETE. Ele existe no modelo porque o alcance do golpe precisa
ter de onde sair: a caixa de dano do servidor vai ate 3.0 blocos a frente, e sem
uma arma desenhada que chegue la o jogador apanharia de nada -- aprendendo uma
distancia que o desenho nao cumpre. A regua `valida_alcance_do_porrete` cobra
exatamente isso.

A HITBOX MANDA NO MODELO. A entidade e sized(1.8F, 4.2F) -- 28.8 x 67.2 px -- e a
bateria da biblioteca reprova se o modelo estourar isso. O piso dos pes fica em
y=0.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de AttackHitbox a frente e +Z, porque com yaw 0 o olhar
vanilla aponta para +Z. Misturar as duas ja custou um bug neste repositorio -- a
caixa do golpe ficou ATRAS do mob, que atacava, animava e nao encostava em
ninguem na frente. Ver o comentario de DummyEnemyEntity.CAIXA_DO_GOLPE.

Regerar:  python art-source/enemies/cyclops/cyclops_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/cyclops.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "cyclops"
UV_LARGURA, UV_ALTURA = 128, 128

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(1.8F, 4.2F).
HITBOX = (1.8, 4.2)

# Alcance, em BLOCOS, que a caixa de golpe do servidor reivindica a frente.
# CyclopsTuning.caixaDoPorrete(): maxZ = 3.0D.
#
# Ele esta aqui para ser COBRADO contra o desenho. Caixa que chega mais longe do
# que o porrete desenhado nao da erro nenhum: da um jogador que leva 14 de dano
# de um porrete que, na tela, parou meio bloco antes dele.
ALCANCE_DO_GOLPE_EM_BLOCOS = 3.0

# Altura relativa a partir da qual o servidor chama a regiao de "eye".
# CyclopsTuning.ALTURA_MINIMA_DO_OLHO = 0.84D.
ALTURA_MINIMA_DO_OLHO = 0.84

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# `eye` e osso PROPRIO, e nao um retangulo pintado na cara. Tres razoes, e as
# tres sao mecanicas: (a) so um osso com cubo tem altura MEDIVEL, e e a altura
# dele que a regua compara com o limiar do WeakPointResolver; (b) o olho precisa
# reagir sozinho -- apertar no cambaleio, arregalar no windup -- e osso pintado
# nao reage; (c) um olho pintado migraria de lugar na primeira correcao de
# textura sem que nenhuma regua percebesse.
#
# `club` pendura em `arm_right`, e nao no torso. Pendurado no torso o pai existe,
# o portao Java passa, e o porrete fica PARADO no ar enquanto o braco desce --
# que e o unico quadro que o jogador tem para ler o golpe.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("hip", "root", (0, 32, 0)),
    geo.Osso("torso", "hip", (0, 36, 0)),
    geo.Osso("head", "torso", (0, 54, 0)),
    geo.Osso("eye", "head", (0, 61, -7)),
    geo.Osso("arm_left", "torso", (9, 53, 0)),
    geo.Osso("arm_right", "torso", (-9, 53, 0)),
    geo.Osso("club", "arm_right", (-11.5, 33, -5)),
    geo.Osso("leg_left", "hip", (5, 30, 0)),
    geo.Osso("leg_right", "hip", (-5, 30, 0)),
    geo.Osso("foot_left", "leg_left", (5, 6, 0)),
    geo.Osso("foot_right", "leg_right", (-5, 6, 0)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
#
# A folha e 128x128 e nao 128x64: so o torso, a cabeca e o porrete ja pedem
# 60x30 + 56x26 + 24x37. Espremer num atlas menor obrigaria a encolher a cabeca,
# e a cabeca e onde mora o unico sinal de leitura que este mob tem.
CAIXAS = (
    # tronco: ombros largos, quadril estreito. A silhueta de cima e o que o
    # jogador ve primeiro quando ele aparece por cima de uma arvore.
    geo.Caixa("torso", "torso", 0, 0, -9, 36, -6, 18, 18, 12),
    geo.Caixa("hip", "hip", 30, 70, -9, 28, -5, 18, 8, 10),
    # cabeca GRANDE de proposito: ela e o painel onde o olho precisa ser legivel
    # a vinte blocos, e olho legivel exige cara grande.
    geo.Caixa("head", "head", 60, 0, -7, 54, -7, 14, 12, 14),
    # o olho: placa de 1 px de profundidade colada na testa. Profundidade 1 e
    # deliberada -- um olho esferico (d=6) empurraria a cara para fora da
    # hitbox e ainda brigaria por area de atlas com a cabeca inteira.
    geo.Caixa("eye", "eye", 86, 70, -3, 58, -8, 6, 6, 1),
    # bracos: 5 px de largura cada. Eles sao o que chega mais perto da parede da
    # hitbox -- ver valida_bracos_dentro_da_caixa.
    geo.Caixa("arm_left", "arm_left", 24, 32, 9, 32, -5, 5, 22, 10),
    geo.Caixa("arm_right", "arm_right", 54, 32, -14, 32, -5, 5, 22, 10),
    # o porrete: 30 px de tora, pendurado na mao direita e deslocado para a
    # FRENTE (z de -12 a -5) para nao atravessar o proprio pe do bicho.
    geo.Caixa("club", "club", 0, 32, -14, 2, -12, 5, 30, 7),
    # pernas e pes: os pes sao mais estreitos que a perna em x de proposito, para
    # o porrete poder descer rente ao corpo sem entrar dentro do pe.
    geo.Caixa("leg_left", "leg_left", 84, 32, 2, 5, -4, 7, 23, 8),
    geo.Caixa("leg_right", "leg_right", 0, 70, -9, 5, -4, 7, 23, 8),
    geo.Caixa("foot_left", "foot_left", 0, 102, 1, 0, -6, 8, 5, 11),
    geo.Caixa("foot_right", "foot_right", 38, 102, -9, 0, -6, 8, 5, 11),
)


# --------------------------------------------------- validacoes DO BICHO
# As tres abaixo ligam o DESENHO a REGRA DO SERVIDOR. Nenhuma das divergencias
# que elas pegam levanta excecao em lugar nenhum.

def valida_olho_no_eixo_e_na_frente(m):
    """O olho unico tem de estar centrado no eixo e ser a peca mais a FRENTE.

    O servidor decide ponto fraco com um cone SIMETRICO em torno do olhar
    (WeakPointResolver.cossenoMinimo). Um olho pintado 2 px para a esquerda nao
    da erro: faz o desenho apontar um lado e a regra pagar o outro, e o jogador
    que mirar no olho leva dano comum sem nada para procurar.

    Ser a peca mais a frente da cabeca e o que garante que o olho apareca de
    perfil raso -- afundado atras da testa, ele some justamente no angulo em que
    o jogador esta decidindo se circula ou avanca.
    """
    olho = m.caixa("eye")
    (x0, x1), _, (z0, _) = geo.volume(olho)
    if x0 != -x1:
        raise geo.ErroDeArte(
            "o olho vai de x=%s a x=%s e nao esta centrado no eixo: o cone de ponto fraco do "
            "servidor e simetrico em torno do olhar, entao o desenho passaria a apontar um lado "
            "e a regra a pagar o outro" % (x0, x1))
    _, _, (cabeca_z0, _) = geo.volume(m.caixa("head"))
    if z0 >= cabeca_z0:
        raise geo.ErroDeArte(
            "o olho comeca em z=%s e a testa comeca em z=%s: o olho fica afundado atras da cara e "
            "some de perfil raso, que e exatamente o angulo em que o jogador decide se circula ou "
            "avanca" % (z0, cabeca_z0))


def valida_olho_acima_do_limiar_do_ponto_fraco(m):
    """A base do olho DESENHADO tem de ficar acima do limiar que o servidor cobra.

    O resolver mede altura relativa a caixa de colisao e so chama de "eye" o que
    estiver acima de ALTURA_MINIMA_DO_OLHO. Olho desenhado abaixo desse limiar
    promete um acerto que a regra nao paga -- e isso e pior do que nao ter olho
    nenhum, porque ENSINA errado.

    A regua tambem cobra o inverso: tem de sobrar corpo comum abaixo do limiar,
    senao todo golpe no bicho viraria critico e o ponto fraco deixaria de ser um
    ponto.
    """
    _, (olho_y0, olho_y1), _ = geo.volume(m.caixa("eye"))
    limite = ALTURA_MINIMA_DO_OLHO * m.hitbox_altura_px
    if olho_y0 < limite:
        raise geo.ErroDeArte(
            "o olho vai de y=%s a y=%s e o servidor so paga ponto fraco acima de %.1f px "
            "(WeakPointResolver %.2f de %.1f px): o desenho promete um critico que a regra recusa, "
            "e nao ha erro nenhum para procurar"
            % (olho_y0, olho_y1, limite, ALTURA_MINIMA_DO_OLHO, m.hitbox_altura_px))
    _, (corpo_y0, _), _ = geo.volume(m.caixa("torso"))
    if corpo_y0 > limite:
        raise geo.ErroDeArte(
            "o torso comeca em y=%s, acima do limiar de %.1f px: nao sobra regiao de corpo comum e "
            "todo golpe no bicho viraria critico" % (corpo_y0, limite))


def valida_alcance_do_porrete(m):
    """O porrete DESENHADO tem de alcancar tao longe quanto a caixa de golpe.

    O braco gira em torno do ombro; quando ele passa da horizontal, a ponta do
    porrete fica a (ombro_y - ponta_y) px a frente do pivo, e o pivo esta sobre o
    eixo z=0 do bicho. Essa e a distancia que o desenho promete.

    Se a caixa de dano do servidor reivindicar mais do que isso, o jogador leva
    pancada de um porrete que, na tela, parou antes dele. Nao da erro: da um mob
    com alcance invisivel, que e a reclamacao mais dificil de diagnosticar que um
    mob corpo-a-corpo consegue gerar.
    """
    _, ombro_y, _ = m.osso("arm_right").pivot
    _, (ponta_y, _), _ = geo.volume(m.caixa("club"))
    alcance_px = ombro_y - ponta_y
    exigido_px = ALCANCE_DO_GOLPE_EM_BLOCOS * 16.0
    if alcance_px < exigido_px:
        raise geo.ErroDeArte(
            "o porrete alcanca %.1f px (%.2f blocos) a partir do ombro e a caixa de golpe do "
            "servidor vai ate %.1f px (%.2f blocos): o jogador apanha de um porrete que parou "
            "antes dele, e nada acusa"
            % (alcance_px, alcance_px / 16.0, exigido_px, ALCANCE_DO_GOLPE_EM_BLOCOS))


def valida_bracos_dentro_da_caixa(m):
    """Os bracos sao o que chega mais perto da parede da hitbox.

    A largura util e 28.8 px, ou seja 14.4 para cada lado, e os bracos terminam
    em 14. A folga de 0.4 px e a mesma do boneco de treino e existe pelo mesmo
    motivo: alargar o braco em 1 px nao da erro nenhum -- da um braco que entra
    no bloco vizinho quando o windup o abre, e isso so aparece para quem encurrala
    o bicho contra uma parede.
    """
    for nome in ("arm_left", "arm_right"):
        c = m.caixa(nome)
        (x0, x1), _, _ = geo.volume(c)
        limite = m.hitbox_largura_px / 2.0
        if abs(x0) > limite or abs(x1) > limite:
            raise geo.ErroDeArte(
                "o braco '%s' vai de x=%s a x=%s e a meia-largura da hitbox e %.1f px: o membro "
                "atravessa a caixa de colisao, e o sintoma e um braco dentro do bloco vizinho"
                % (nome, x0, x1, limite))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_olho_no_eixo_e_na_frente,
                          valida_olho_acima_do_limiar_do_ponto_fraco,
                          valida_alcance_do_porrete,
                          valida_bracos_dentro_da_caixa))
