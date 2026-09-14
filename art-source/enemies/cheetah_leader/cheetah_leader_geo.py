"""Geometria do Cheetah Leader -- a formiga quimera que CHEGA primeiro.

O QUE O CORPO PRECISA CONTAR, E O QUE ACONTECE SE ELE NAO CONTAR.

Este bicho tem 1.2 x 1.5 blocos -- 19.2 x 24 px. A ficha dele e velocidade 0.46
com armadura 3: ele e o mais rapido da colonia e um dos mais faceis de ferir.
Essa troca e a coisa inteira, e ela precisa estar na SILHUETA antes de estar no
combate. Um guepardo desenhado encorpado prometeria couro que a armadura 3 nao
paga, e o jogador aprenderia a recuar de um bicho que ele deveria estar
perseguindo de volta durante a fadiga.

As quatro decisoes do desenho, e o que cada uma impede:

1. PATAS ALTAS. Canela de 12 px mais pata de 2 px = 14 px dos 23 px de altura.
   Perna curta com velocidade 0.46 le como boneco DESLIZANDO pelo chao, e isso
   nenhum portao ve: o mob anda, ataca, morre e passa em tudo.
   `valida_silhueta_de_corredor` cobra a fracao.

2. PEITO ESTREITO. 8 px dos 19.2 px que a hitbox permitiria. De frente, um peito
   largo vira um bloco, e bloco nao corre.

3. CAUDA LONGA, EM DOIS SEGMENTOS. Ela e 6 px contra os 7 px do tronco -- quase
   um corpo de cauda. Nao e enfeite: e o unico membro que continua se mexendo
   quando o bicho freia, e e por ele que o jogador enxerga a virada. Cauda curta
   nao da erro; da um felino que muda de direcao como um carrinho.

4. A MARCA DE QUIMERA. A `carapaca` e uma placa de quitina sobre a nuca e os
   ombros, com osso proprio. Sem ela este modelo le como guepardo comum, e a
   colonia perde a identidade visual que faz o jogador reconhecer uma formiga
   antes de ler o nome dela. Placa PINTADA na pele nao serve: so um osso com
   cubo tem silhueta, e e a silhueta que sobrevive a dez blocos de distancia.

A HITBOX E MAIS LARGA QUE O BICHO, E ISSO ESTA DECLARADO. A entidade e
sized(1.2F, 1.5F) e o corpo desenhado usa 8 dos 19.2 px de largura. A sobra e
deliberada -- um felino de peito largo le como urso -- mas ela tem um preco que
nenhuma regua daqui mede: um tiro que passa ao lado do desenho ainda acerta a
caixa de colisao. Isso e humano, e esta escrito aqui em vez de ficar implicito.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de AttackHitbox a frente e +Z, porque com yaw 0 o olhar
vanilla aponta para +Z. Misturar as duas ja custou um bug neste repositorio -- a
caixa do golpe ficou ATRAS do mob, que atacava, animava e nao encostava em
ninguem na frente. Por isso `valida_bote_alcanca_a_caixa` converte o sinal
explicitamente, e o comentario de CheetahLeaderTuning.caixaDoBote diz a mesma
coisa do outro lado.

Regerar:  python art-source/enemies/cheetah_leader/cheetah_leader_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/cheetah_leader.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "cheetah_leader"
UV_LARGURA, UV_ALTURA = 64, 32

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(1.2F, 1.5F).
HITBOX = (1.2, 1.5)

# ------------------------------------------------- os numeros que vem do Java
#
# Cada um destes esta escrito TAMBEM no Java, com o campo de origem ao lado.
# Duplicacao DECLARADA: a outra ponta e a regua que os cobra logo abaixo, e
# CheetahLeaderTuningTest cobra o mesmo do lado de la. Sem as duas pontas, a
# duplicacao viraria divergencia na primeira sessao de balanceamento -- e a
# divergencia nao da erro, da um bote cujo alcance nao bate com o desenho.

# CheetahLeaderTuning.caixaDoBote(): minZ e maxZ, em BLOCOS, coordenada local.
BOTE_INICIO_EM_BLOCOS = 0.25
BOTE_FIM_EM_BLOCOS = 1.05

# CheetahLeaderTuning.AVANCO_DO_BOTE: quanto o corpo viaja para a frente durante
# a janela que machuca. E o que paga a diferenca entre a ponta do focinho
# desenhado e o fim da caixa do bote.
AVANCO_DO_BOTE_EM_BLOCOS = 0.55

# SquadRules.esquadrao(): espacamento entre membros do esquadrao, em blocos.
ESPACAMENTO_DO_ESQUADRAO_EM_BLOCOS = 3.0

# ------------------------------------------------- os limites de LEITURA
#
# Estes dois NAO vao para o Java, e a ausencia e de proposito. Eles medem o
# DESENHO, nao o servidor: nenhum consumidor em Java leria qualquer um deles, e
# uma constante declarada la sem consumidor e exatamente o "numero que foi para a
# config e ficou orfao" que o CLAUDE.md lista. Eles moram aqui, ao lado do
# comentario que os explica e da regua que os cobra.

# A pata (canela + pe) ocupa pelo menos esta fracao da altura do modelo.
FRACAO_MINIMA_DE_PATA = 0.5

# O peito ocupa no maximo esta fracao da largura da hitbox.
FRACAO_MAXIMA_DE_PEITO = 0.45

# A cauda mede pelo menos esta fracao do comprimento do tronco.
FRACAO_MINIMA_DE_CAUDA = 0.7

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# `snout` e osso PROPRIO e nao um retangulo pintado na cara por duas razoes
# mecanicas: (a) so um osso com cubo tem posicao MEDIVEL, e e a posicao dele que
# a regua compara com a caixa do bote do servidor; (b) a boca precisa abrir no
# windup de 12 ticks -- boca pintada nao abre, e 0.6 s e curto demais para ser
# lido por outra coisa que nao mudanca de contorno.
#
# `tail_tip` pendura na `tail`, e nao no corpo. Pendurada no corpo o pai existe,
# o portao Java passa, e a ponta fica rigida enquanto a base gira -- o que apaga
# exatamente a leitura de virada que a cauda existe para dar.
#
# As patas (`paw_*`) penduram na PERNA pelo mesmo motivo: penduradas no corpo,
# elas ficam paradas no ar enquanto a canela gira embaixo delas.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 16, 0)),
    geo.Osso("carapaca", "body", (0, 20, 0)),
    geo.Osso("head", "body", (0, 19, -3)),
    geo.Osso("snout", "head", (0, 17, -7)),
    geo.Osso("ear_left", "head", (1.5, 21, -5)),
    geo.Osso("ear_right", "head", (-1.5, 21, -5)),
    geo.Osso("tail", "body", (0, 19, 4)),
    geo.Osso("tail_tip", "tail", (0, 19, 7)),
    geo.Osso("leg_front_left", "body", (2, 14, -1)),
    geo.Osso("leg_front_right", "body", (-2, 14, -1)),
    geo.Osso("leg_back_left", "body", (2, 14, 2)),
    geo.Osso("leg_back_right", "body", (-2, 14, 2)),
    geo.Osso("paw_front_left", "leg_front_left", (2, 2, -1)),
    geo.Osso("paw_front_right", "leg_front_right", (-2, 2, -1)),
    geo.Osso("paw_back_left", "leg_back_left", (2, 2, 2)),
    geo.Osso("paw_back_right", "leg_back_right", (-2, 2, 2)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
#
# A folha e 64x32 -- e nao 64x64 -- porque as dezesseis pecas somam 1682 px, 82%
# de 64x32. Numa folha 64x64 a mesma arte ocuparia 41% e os outros 59% seriam
# area morta VERSIONADA: peso de download e de memoria de atlas por nada.
CAIXAS = (
    # tronco: 7 px de comprimento e 8 px de peito. Curto de proposito -- o
    # comprimento do bicho esta na CAUDA e no PESCOCO, nao na barriga, e e isso
    # que separa um felino de um cachorro grande na silhueta de perfil.
    geo.Caixa("body", "body", 0, 0, -4, 13, -3, 8, 7, 7),
    # a placa de quitina: A MARCA DE QUIMERA. Ela cobre a nuca e os ombros, que
    # e onde o olho cai primeiro quando o bicho vem correndo de frente. Posta na
    # garupa ela so apareceria de costas -- e de costas o jogador ja perdeu.
    geo.Caixa("carapaca", "carapaca", 40, 9, -3.5, 20, -2, 7, 2, 5),
    # cabeca baixa, na linha do lombo. Cabeca alta le como felino domestico;
    # cabeca na linha do dorso le como predador em perseguicao, que e a unica
    # coisa que este bicho faz.
    geo.Caixa("head", "head", 30, 0, -2.5, 16, -7, 5, 5, 4),
    # focinho: a peca mais a FRENTE do modelo inteiro, e a regua cobra isso.
    # Estreito (3 px) para nao roubar area da cara -- a cara e onde moram os
    # olhos e a lagrima, que sao o unico contraste alto do bicho.
    geo.Caixa("snout", "snout", 48, 0, -1.5, 16, -9, 3, 2, 2),
    # orelhas: 1 px de espessura, pequenas e arredondadas. Elas sao o unico
    # detalhe acima da placa, e sao elas que dizem para que lado a cabeca esta
    # virada quando o corpo inteiro ja passou de perfil.
    geo.Caixa("ear_left", "ear_left", 58, 0, 1, 21, -6, 1, 2, 2),
    geo.Caixa("ear_right", "ear_right", 48, 4, -2, 21, -6, 1, 2, 2),
    # cauda em dois segmentos, 3 px cada. Dois, e nao um: um unico cubo longo
    # gira em bloco e le como vara presa na garupa. E dois e o minimo -- com tres
    # a cauda passaria dos 19.2 px de comprimento que a hitbox permite, e a
    # bateria da biblioteca reprovaria, corretamente.
    geo.Caixa("tail", "tail", 54, 4, -1, 18, 4, 2, 2, 3),
    geo.Caixa("tail_tip", "tail_tip", 30, 9, -1, 18, 7, 2, 2, 3),
    # pernas: 12 px de canela. Elas sao mais da metade da altura do bicho, e sao
    # a razao de a passada ter curso para aparecer numa corrida de 0.46.
    geo.Caixa("leg_front_left", "leg_front_left", 0, 14, 1, 2, -2, 2, 12, 2),
    geo.Caixa("leg_front_right", "leg_front_right", 8, 14, -3, 2, -2, 2, 12, 2),
    geo.Caixa("leg_back_left", "leg_back_left", 16, 14, 1, 2, 1, 2, 12, 2),
    geo.Caixa("leg_back_right", "leg_back_right", 24, 14, -3, 2, 1, 2, 12, 2),
    # patas dianteiras compridas (5 px em z) e traseiras curtas (3 px): e o
    # desequilibrio que faz a pisada de um felino em corrida ler como avanco e
    # nao como marcha. Elas sao as unicas pecas que tocam y=0 -- o piso do
    # modelo mora aqui.
    geo.Caixa("paw_front_left", "paw_front_left", 32, 16, 1, 0, -4, 2, 2, 5),
    geo.Caixa("paw_front_right", "paw_front_right", 46, 16, -3, 0, -4, 2, 2, 5),
    geo.Caixa("paw_back_left", "paw_back_left", 32, 23, 1, 0, 1, 2, 2, 3),
    geo.Caixa("paw_back_right", "paw_back_right", 42, 23, -3, 0, 1, 2, 2, 3),
)


# --------------------------------------------------- validacoes DO BICHO
#
# As duas primeiras ligam o DESENHO a REGRA DO SERVIDOR; a terceira e a quarta
# medem LEITURA. Nenhuma das divergencias que elas pegam levanta excecao em lugar
# nenhum do jogo.

def valida_bote_alcanca_a_caixa(m):
    """A caixa do bote tem de caber entre a ponta do focinho e o avanco. [ARTE <-> SERVIDOR]

    O servidor reivindica de BOTE_INICIO a BOTE_FIM blocos a frente do centro do
    bicho (CheetahLeaderTuning.caixaDoBote). O desenho entrega |z| do focinho, que
    e a peca mais a frente do modelo; o resto e pago pelo avanco que o guepardo
    aplica ao entrar na janela ACTIVE.

    Tres divergencias, e as tres sao mudas:

    1. focinho atras do centro -- o modelo esta virado ao contrario, e um mob
       virado ao contrario ataca, anima e nao encosta em quem esta na frente;
    2. caixa comecando ADIANTE do focinho -- o bote sairia de um ponto na frente
       da propria boca, e o jogador levaria dano sem que nada tivesse encostado
       nele;
    3. caixa terminando alem de focinho + avanco -- o jogador apanha de um bicho
       que, na tela, parou antes dele. E a reclamacao mais dificil de
       diagnosticar que um mob corpo-a-corpo consegue gerar, porque o dano, o
       cooldown e o log estao todos certos.

    A conversao de eixo e explicita: na geometria a frente e -Z, na caixa de
    ataque a frente e +Z. Comparar os dois sem inverter o sinal daria uma regua
    que aprova exatamente o modelo errado.
    """
    _, _, (focinho_z0, _) = geo.volume(m.caixa("snout"))
    alcance_px = -focinho_z0                      # -Z e a frente: o sinal inverte AQUI
    inicio_px = BOTE_INICIO_EM_BLOCOS * 16.0
    fim_px = BOTE_FIM_EM_BLOCOS * 16.0
    avanco_px = AVANCO_DO_BOTE_EM_BLOCOS * 16.0

    if alcance_px <= 0:
        raise geo.ErroDeArte(
            "o focinho comeca em z=%s, ou seja, ATRAS do centro do bicho: o modelo esta virado ao "
            "contrario, e um mob virado ao contrario ataca, anima e nao encosta em quem esta na "
            "frente -- quem apanha e quem estiver pelas costas" % focinho_z0)
    if inicio_px > alcance_px:
        raise geo.ErroDeArte(
            "a caixa do bote comeca a %.1f px (%.2f blocos) e o focinho desenhado so chega a "
            "%.1f px: o golpe comecaria na frente da propria boca, e o jogador levaria dano sem "
            "nada ter encostado nele"
            % (inicio_px, BOTE_INICIO_EM_BLOCOS, alcance_px))
    if fim_px > alcance_px + avanco_px:
        raise geo.ErroDeArte(
            "a caixa do bote termina a %.1f px (%.2f blocos) e o focinho alcanca %.1f px mais o "
            "avanco de %.1f px = %.1f px: o jogador apanha de um guepardo que, na tela, parou "
            "antes dele -- dano certo, cooldown certo, log limpo, e a unica leitura que ele tem "
            "quebrada"
            % (fim_px, BOTE_FIM_EM_BLOCOS, alcance_px, avanco_px, alcance_px + avanco_px))

    frente_das_outras = min(geo.volume(c)[2][0] for c in m.caixas if c.nome != "snout")
    if focinho_z0 >= frente_das_outras:
        raise geo.ErroDeArte(
            "o focinho comeca em z=%s e ha peca chegando a z=%s: o focinho nao e a peca mais a "
            "frente, entao o desenho encosta com outra coisa que nao a boca -- e a caixa do bote "
            "continua saindo do focinho" % (focinho_z0, frente_das_outras))


def valida_corpo_cabe_no_espacamento(m):
    """O corpo desenhado tem de caber, com folga, no espacamento do esquadrao. [ARTE <-> SERVIDOR]

    `SquadRules.esquadrao()` manda os membros ficarem a ESPACAMENTO blocos uns
    dos outros, e este bicho e o LIDER: e ele quem ocupa o centro do grupo que
    chega depois. Nada em `SquadRules`, porem, sabe qual e o TAMANHO do bicho --
    o espacamento e uma distancia entre CENTROS.

    Corpo mais longo que o espacamento nao da erro nenhum. Da um grupo em que
    "espacamento respeitado" poe formiga dentro de formiga, e o jogador le
    travamento -- nunca IA. A regua exige o dobro: dois vizinhos precisam do
    proprio corpo INTEIRO entre os centros, e nao de meio corpo cada um
    encostado.
    """
    (x0, x1), _, (z0, z1) = m.limites()
    maior_lado_px = max(x1 - x0, z1 - z0)
    espacamento_px = ESPACAMENTO_DO_ESQUADRAO_EM_BLOCOS * 16.0
    if maior_lado_px * 2.0 > espacamento_px:
        raise geo.ErroDeArte(
            "o guepardo mede %.1f px no maior lado e o espacamento do esquadrao e %.1f px "
            "(%.2f blocos): dois vizinhos ficariam a menos de um corpo de distancia, e a formacao "
            "poria formiga dentro de formiga -- o jogador le isso como travamento, nunca como IA"
            % (maior_lado_px, espacamento_px, ESPACAMENTO_DO_ESQUADRAO_EM_BLOCOS))


def valida_silhueta_de_corredor(m):
    """Patas altas e peito estreito -- a leitura que a velocidade 0.46 exige.

    A ficha deste bicho e a maior velocidade da colonia com armadura 3.
    Velocidade alta num corpo atarracado le como bicho DESLIZANDO pelo chao, e
    nenhum portao ve isso: o mob anda, ataca, morre e passa em tudo.

    Duas medidas, contra os dois limites declarados no topo deste arquivo:

    * a pata (canela + pe) ocupa pelo menos FRACAO_MINIMA_DE_PATA da altura --
      abaixo disso a passada nao tem curso para aparecer, e o arranque de 30
      ticks passa sem que nada na tela mude;
    * o peito ocupa no maximo FRACAO_MAXIMA_DE_PEITO da largura da hitbox --
      acima disso a silhueta de frente e um bloco, e bloco nao corre.
    """
    _, (_, altura_px), _ = m.limites()
    _, (canela_y0, canela_y1), _ = geo.volume(m.caixa("leg_front_left"))
    _, (pata_y0, _), _ = geo.volume(m.caixa("paw_front_left"))
    pata_px = canela_y1 - min(canela_y0, pata_y0)
    if pata_px < FRACAO_MINIMA_DE_PATA * altura_px:
        raise geo.ErroDeArte(
            "a pata dianteira tem %.1f px de %.1f px de altura (%.0f%%) e o minimo e %.0f%%: com "
            "perna curta a passada nao tem curso, e um bicho de velocidade 0.46 passa a ler como "
            "boneco deslizando pelo chao"
            % (pata_px, altura_px, 100.0 * pata_px / altura_px, 100.0 * FRACAO_MINIMA_DE_PATA))

    (peito_x0, peito_x1), _, _ = geo.volume(m.caixa("body"))
    peito_px = peito_x1 - peito_x0
    if peito_px > FRACAO_MAXIMA_DE_PEITO * m.hitbox_largura_px:
        raise geo.ErroDeArte(
            "o peito tem %.1f px e a hitbox tem %.1f px de largura (%.0f%%), acima do maximo de "
            "%.0f%%: de frente a silhueta vira um bloco, e bloco nao corre -- o jogador deixa de "
            "reconhecer o que o bicho faz antes de ele chegar"
            % (peito_px, m.hitbox_largura_px, 100.0 * peito_px / m.hitbox_largura_px,
               100.0 * FRACAO_MAXIMA_DE_PEITO))


def valida_cauda_de_equilibrio(m):
    """A cauda mede quase um tronco, e ela e o que mostra a virada.

    O arranque deste bicho termina numa FADIGA, e a fadiga so e uma resposta do
    jogador se ele conseguir ver a virada acontecer. O corpo inteiro muda pouco
    entre correr e frear; a cauda muda muito, porque ela e o unico membro com
    curso livre.

    Cauda curta nao da erro: da um felino que muda de direcao como um carrinho, e
    o defeito e daqueles que ninguem consegue descrever -- so diz que "esta
    estranho".
    """
    _, _, (tronco_z0, tronco_z1) = geo.volume(m.caixa("body"))
    _, _, (base_z0, _) = geo.volume(m.caixa("tail"))
    _, _, (_, ponta_z1) = geo.volume(m.caixa("tail_tip"))
    tronco_px = tronco_z1 - tronco_z0
    cauda_px = ponta_z1 - base_z0
    if cauda_px < FRACAO_MINIMA_DE_CAUDA * tronco_px:
        raise geo.ErroDeArte(
            "a cauda mede %.1f px e o tronco %.1f px (%.0f%%), abaixo do minimo de %.0f%%: sem "
            "cauda longa a virada no fim do arranque nao aparece, e a fadiga -- que e a resposta "
            "do jogador -- deixa de ser legivel na tela"
            % (cauda_px, tronco_px, 100.0 * cauda_px / tronco_px,
               100.0 * FRACAO_MINIMA_DE_CAUDA))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_bote_alcanca_a_caixa,
                          valida_corpo_cabe_no_espacamento,
                          valida_silhueta_de_corredor,
                          valida_cauda_de_equilibrio))
