"""Geometria do Wolf Pack Hunter -- o lobo que so vence em BANDO (issue #119).

O QUE O CORPO PRECISA CONTAR, E O QUE ACONTECE SE ELE NAO CONTAR.

Este bicho tem 0.9 x 0.9 blocos -- 14.4 px de lado. Ele e o MENOR corpo da
familia de Greed Island, e a pequenez e a primeira metade da ficha: HP 26, dano
6, armadura 2. Um lobo sozinho perde. Um lobo desenhado grande prometeria o
contrario, e o jogador aprenderia a respeitar o individuo em vez de contar
quantos sao -- que e exatamente a leitura que este encontro precisa.

A segunda metade da ficha e a VELOCIDADE: 0.34, a maior das sete criaturas da
ilha. Velocidade alta num corpo atarracado le como bicho deslizando pelo chao, e
isso nenhum portao ve. Por isso a silhueta e de CORREDOR -- peito estreito (5 px
contra os 14 que caberiam) e patas longas (7 dos 14 px de altura). A regua
`valida_silhueta_de_corredor` cobra as duas coisas contra os limites declarados
em WolfPackHunterTuning.

A terceira e o FOCINHO, e ele e a peca que a regua mais importante mede. O
servidor reivindica uma caixa de mordida que vai de 0.2 a 0.95 blocos a frente
do centro do lobo (WolfPackHunterTuning.caixaDaMordida). O focinho DESENHADO
alcanca 0.44 bloco. A diferenca nao e descuido: ela e paga pelo avanco da
investida -- o lobo salta para dentro do golpe. A regua
`valida_focinho_alcanca_a_mordida` cobra a soma, e reprova nos dois sentidos:
caixa que comeca ADIANTE do focinho (mordida no ar) e caixa que termina alem do
que o focinho mais o salto alcancam (dano vindo de um lobo que, na tela, parou
antes do jogador).

A quarta e o ESPACAMENTO DO BANDO. `SquadRules.matilha()` manda os lobos ficarem
a 2.5 blocos uns dos outros no cerco. Se o corpo desenhado fosse mais largo que
esse espacamento, "espacamento respeitado" seria uma frase falsa: o cerco poria
lobo dentro de lobo, e o jogador leria travamento, nunca IA.
`valida_corpo_cabe_no_espacamento` mede isso.

A HITBOX MANDA NO MODELO. A entidade e sized(0.9F, 0.9F) -- 14.4 x 14.4 px -- e a
bateria da biblioteca reprova se o modelo estourar isso em qualquer eixo. O piso
das patas fica em y=0.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de AttackHitbox a frente e +Z, porque com yaw 0 o olhar
vanilla aponta para +Z. Misturar as duas ja custou um bug neste repositorio -- a
caixa do golpe ficou ATRAS do mob, que atacava, animava e nao encostava em
ninguem na frente. Por isso esta regua converte explicitamente, e o comentario
de WolfPackHunterTuning.caixaDaMordida diz a mesma coisa do outro lado.

Regerar:  python art-source/enemies/wolf_pack_hunter/wolf_pack_hunter_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/wolf_pack_hunter.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "wolf_pack_hunter"
UV_LARGURA, UV_ALTURA = 64, 64

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(0.9F, 0.9F).
HITBOX = (0.9, 0.9)

# ------------------------------------------------- os numeros que vem do Java
#
# Cada um destes esta escrito TAMBEM no Java, com o campo de origem ao lado.
# Duplicacao DECLARADA: a outra ponta e a regua que os cobra logo abaixo. Sem a
# regua, a duplicacao viraria divergencia na primeira sessao de balanceamento, e
# a divergencia nao da erro -- da um lobo cuja mordida nao bate com o desenho.

# WolfPackHunterTuning.caixaDaMordida(): minZ e maxZ, em BLOCOS, coordenada local.
MORDIDA_INICIO_EM_BLOCOS = 0.2
MORDIDA_FIM_EM_BLOCOS = 0.95

# WolfPackHunterTuning.AVANCO_DA_INVESTIDA: quanto o corpo do lobo viaja para a
# frente durante a janela que machuca. E o que paga a diferenca entre o focinho
# desenhado e o fim da caixa de mordida.
AVANCO_DA_INVESTIDA_EM_BLOCOS = 0.55

# SquadRules.matilha(): espacamento entre membros no cerco, em blocos.
ESPACAMENTO_DA_MATILHA_EM_BLOCOS = 2.5

# WolfPackHunterTuning.FRACAO_MINIMA_DE_PATA / FRACAO_MAXIMA_DE_PEITO -- os dois
# limites de LEITURA da silhueta de corredor.
FRACAO_MINIMA_DE_PATA = 0.45
FRACAO_MAXIMA_DE_PEITO = 0.45

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# `snout` e osso PROPRIO e nao um retangulo pintado na cara, por duas razoes
# mecanicas: (a) so um osso com cubo tem posicao MEDIVEL, e e a posicao dele que
# a regua compara com a caixa de mordida do servidor; (b) o focinho precisa
# abrir no windup e fechar no strike -- focinho pintado nao abre, e o windup de
# meio segundo deste bicho e curto demais para ser lido por outra coisa.
#
# As patas (`paw_*`) penduram na PERNA e nao no corpo. Penduradas no corpo o pai
# existe, o portao Java passa, e a pata fica parada no ar enquanto a canela gira
# embaixo dela -- num bicho de 14 px isso e metade da silhueta em movimento.
OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 7, 2)),
    geo.Osso("head", "body", (0, 11, -1)),
    geo.Osso("snout", "head", (0, 10, -5)),
    geo.Osso("ear_left", "head", (1, 13, -3)),
    geo.Osso("ear_right", "head", (-1, 13, -3)),
    geo.Osso("tail", "body", (0, 11, 5)),
    geo.Osso("leg_front_left", "body", (2, 7, 0)),
    geo.Osso("leg_front_right", "body", (-2, 7, 0)),
    geo.Osso("leg_back_left", "body", (2, 7, 4)),
    geo.Osso("leg_back_right", "body", (-2, 7, 4)),
    geo.Osso("paw_front_left", "leg_front_left", (2, 2, 0)),
    geo.Osso("paw_front_right", "leg_front_right", (-2, 2, 0)),
    geo.Osso("paw_back_left", "leg_back_left", (2, 2, 4)),
    geo.Osso("paw_back_right", "leg_back_right", (-2, 2, 4)),
)

# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
#
# A folha e 64x64 porque as quinze pecas somam 922 px de atlas -- 23% de 64x64.
# Uma folha maior seria area morta versionada; uma menor obrigaria a encolher as
# patas, e as patas sao metade da leitura de um corredor.
CAIXAS = (
    # tronco: 5 px de peito contra os 14 que a hitbox permitiria. O peito
    # estreito e o que separa "lobo" de "urso pequeno" a vinte blocos, e a
    # diferenca importa porque um urso pequeno nao explica um bando de quatro.
    geo.Caixa("body", "body", 0, 0, -2.5, 7, -1, 5, 5, 6),
    # cabeca baixa, na linha do lombo. Cabeca alta le como canideo domestico;
    # cabeca na linha do dorso le como predador em perseguicao, que e a unica
    # coisa que este bicho faz.
    geo.Caixa("head", "head", 22, 0, -2, 8, -5, 4, 5, 4),
    # focinho: a peca mais a FRENTE do modelo inteiro, e a regua cobra isso. Ele
    # e estreito (2 px) de proposito -- focinho largo aumentaria a area pintada
    # da cara e roubaria o contraste dos olhos.
    geo.Caixa("snout", "snout", 38, 0, -1, 9, -7, 2, 2, 2),
    # orelhas: 1 px de espessura, viradas para a frente. Elas sao o unico
    # detalhe acima da linha do dorso, e e por elas que se conta quantos lobos
    # ha no mato antes de ver os corpos.
    geo.Caixa("ear_left", "ear_left", 46, 0, 0.5, 12, -4, 1, 2, 2),
    geo.Caixa("ear_right", "ear_right", 52, 0, -1.5, 12, -4, 1, 2, 2),
    # cauda: curta e horizontal. Cauda longa sairia dos 14 px de comprimento que
    # a hitbox permite, e a bateria da biblioteca reprovaria -- corretamente: o
    # que passa da caixa de colisao e silhueta que promete alcance inexistente.
    geo.Caixa("tail", "tail", 38, 4, -1, 9, 5, 2, 2, 2),
    # pernas: 5 px de canela + 2 px de pata = 7 px, metade da altura do bicho.
    # Perna curta com velocidade 0.34 le como boneco deslizando pelo chao.
    geo.Caixa("leg_front_left", "leg_front_left", 0, 12, 1, 2, -1, 2, 5, 2),
    geo.Caixa("leg_front_right", "leg_front_right", 8, 12, -3, 2, -1, 2, 5, 2),
    geo.Caixa("leg_back_left", "leg_back_left", 16, 12, 1, 2, 3, 2, 5, 2),
    geo.Caixa("leg_back_right", "leg_back_right", 24, 12, -3, 2, 3, 2, 5, 2),
    # patas: mais compridas em z que a canela, e e o que faz a pisada aparecer.
    # Elas sao a unica peca que toca y=0 -- o piso do modelo mora aqui.
    geo.Caixa("paw_front_left", "paw_front_left", 32, 12, 1, 0, -1.5, 2, 2, 3),
    geo.Caixa("paw_front_right", "paw_front_right", 42, 12, -3, 0, -1.5, 2, 2, 3),
    geo.Caixa("paw_back_left", "paw_back_left", 0, 20, 1, 0, 2.5, 2, 2, 3),
    geo.Caixa("paw_back_right", "paw_back_right", 10, 20, -3, 0, 2.5, 2, 2, 3),
)


# --------------------------------------------------- validacoes DO BICHO
# As tres abaixo ligam o DESENHO a REGRA DO SERVIDOR. Nenhuma das divergencias
# que elas pegam levanta excecao em lugar nenhum do jogo.

def valida_focinho_alcanca_a_mordida(m):
    """A caixa de mordida do servidor tem de caber entre o focinho e o salto.

    O servidor reivindica de MORDIDA_INICIO a MORDIDA_FIM blocos a frente do
    centro do lobo. O desenho entrega |z| do focinho; o resto e pago pelo avanco
    da investida, que o lobo aplica ao entrar na janela ACTIVE.

    Duas divergencias, e as duas sao mudas:

    1. caixa comecando ADIANTE do focinho -- o lobo morderia a partir de um
       ponto que fica na frente da propria boca, e o jogador levaria dano sem que
       nada tivesse encostado nele;
    2. caixa terminando alem de focinho + salto -- o jogador apanha de um lobo
       que, na tela, parou antes dele. E a reclamacao mais dificil de diagnosticar
       que um mob corpo-a-corpo consegue gerar, porque o dano, o cooldown e o log
       estao todos certos.

    A conversao de eixo e explicita: na geometria a frente e -Z, na caixa de
    ataque a frente e +Z. Comparar os dois sem inverter o sinal daria uma regua
    que aprova exatamente o modelo errado.
    """
    _, _, (focinho_z0, _) = geo.volume(m.caixa("snout"))
    alcance_px = -focinho_z0                      # -Z e a frente: o sinal inverte AQUI
    inicio_px = MORDIDA_INICIO_EM_BLOCOS * 16.0
    fim_px = MORDIDA_FIM_EM_BLOCOS * 16.0
    salto_px = AVANCO_DA_INVESTIDA_EM_BLOCOS * 16.0

    if alcance_px <= 0:
        raise geo.ErroDeArte(
            "o focinho comeca em z=%s, ou seja, ATRAS do centro do lobo: o modelo esta virado ao "
            "contrario, e um mob virado ao contrario ataca, anima e nao encosta em quem esta na "
            "frente" % focinho_z0)
    if inicio_px > alcance_px:
        raise geo.ErroDeArte(
            "a caixa de mordida comeca a %.1f px (%.2f blocos) e o focinho desenhado so chega a "
            "%.1f px: a mordida comecaria na frente da propria boca, e o jogador levaria dano sem "
            "nada ter encostado nele"
            % (inicio_px, MORDIDA_INICIO_EM_BLOCOS, alcance_px))
    if fim_px > alcance_px + salto_px:
        raise geo.ErroDeArte(
            "a caixa de mordida termina a %.1f px (%.2f blocos) e o focinho alcanca %.1f px mais o "
            "salto de %.1f px = %.1f px: o jogador apanha de um lobo que, na tela, parou antes "
            "dele -- dano certo, cooldown certo, log limpo, e a unica leitura que ele tem quebrada"
            % (fim_px, MORDIDA_FIM_EM_BLOCOS, alcance_px, salto_px, alcance_px + salto_px))

    frente_das_outras = min(z0 for c in m.caixas if c.nome != "snout"
                            for (z0, _) in (geo.volume(c)[2],))
    if focinho_z0 >= frente_das_outras:
        raise geo.ErroDeArte(
            "o focinho comeca em z=%s e ha peca chegando a z=%s: o focinho nao e a peca mais a "
            "frente, entao o desenho morde com outra coisa que nao a boca -- e a caixa de mordida "
            "continua saindo do focinho" % (focinho_z0, frente_das_outras))


def valida_corpo_cabe_no_espacamento(m):
    """O corpo desenhado tem de caber, com folga, no espacamento do bando.

    `SquadRules.matilha()` manda os membros ficarem a ESPACAMENTO blocos uns dos
    outros, e `RegrasDeMatilha` cobra que a corda do cerco respeite isso. Nada
    disso, porem, sabe qual e o TAMANHO do bicho: o espacamento e uma distancia
    entre centros.

    Corpo mais largo que o espacamento nao da erro nenhum. Da um cerco em que
    "espacamento respeitado" poe lobo dentro de lobo, e o jogador le travamento
    -- nunca IA. A regua exige o dobro: dois lobos vizinhos precisam do proprio
    corpo INTEIRO entre os centros, e nao de meio corpo cada um encostado.
    """
    (x0, x1), _, (z0, z1) = m.limites()
    maior_lado_px = max(x1 - x0, z1 - z0)
    espacamento_px = ESPACAMENTO_DA_MATILHA_EM_BLOCOS * 16.0
    if maior_lado_px * 2.0 > espacamento_px:
        raise geo.ErroDeArte(
            "o lobo mede %.1f px no maior lado e o espacamento do bando e %.1f px (%.2f blocos): "
            "dois vizinhos no cerco ficariam a menos de um corpo de distancia, e o cerco poria "
            "lobo dentro de lobo -- o jogador le isso como travamento, nunca como IA"
            % (maior_lado_px, espacamento_px, ESPACAMENTO_DA_MATILHA_EM_BLOCOS))


def valida_silhueta_de_corredor(m):
    """Patas longas e peito estreito -- a leitura que a velocidade 0.34 exige.

    A ficha deste bicho e velocidade alta com corpo fraco. Velocidade alta num
    corpo atarracado le como bicho DESLIZANDO pelo chao, e nenhum portao ve isso:
    o mob anda, ataca, morre e passa em tudo.

    Duas medidas, contra os dois limites declarados em WolfPackHunterTuning:

    * a pata (canela + pe) ocupa pelo menos FRACAO_MINIMA_DE_PATA da altura --
      abaixo disso a passada nao tem curso para aparecer;
    * o peito ocupa no maximo FRACAO_MAXIMA_DE_PEITO da largura da hitbox --
      acima disso a silhueta de frente e um bloco, e um bloco nao corre.
    """
    _, (_, altura_px), _ = m.limites()
    _, (canela_y0, canela_y1), _ = geo.volume(m.caixa("leg_front_left"))
    _, (pata_y0, _), _ = geo.volume(m.caixa("paw_front_left"))
    pata_px = canela_y1 - min(canela_y0, pata_y0)
    if pata_px < FRACAO_MINIMA_DE_PATA * altura_px:
        raise geo.ErroDeArte(
            "a pata dianteira tem %.1f px de %.1f px de altura (%.0f%%) e o minimo e %.0f%%: com "
            "perna curta a passada nao tem curso, e um bicho de velocidade 0.34 passa a ler como "
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


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_focinho_alcanca_a_mordida,
                          valida_corpo_cabe_no_espacamento,
                          valida_silhueta_de_corredor))
