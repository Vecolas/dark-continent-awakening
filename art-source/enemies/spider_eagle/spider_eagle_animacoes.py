"""Gera as OITO animacoes da spider eagle (GeckoLib 4.8.3, Bedrock 1.8.0).

DECISAO QUE ESTE ARQUIVO CARREGA (ADR-017): a ave deixa de vestir o PHANTOM
vanilla. O comportamento nao muda uma linha -- os tres gametests que provam o
ninho parado, o aviso antes do bote e "quem recua deixa de ser alvo" continuam
provando a mesma coisa. O que muda e o corpo, e com ele o MOVIMENTO.

E neste mob o movimento nao e enfeite: a unica resposta que a spider eagle
ensina e RECUAR, e o jogador so tem como aprender isso OLHANDO para ela. O
aviso e a asa aberta. Se o aviso ler como ataque, a resposta ensinada vira
"lutar", e nenhum portao deste repositorio acusa -- o servidor continua
poupando quem recua, e o jogador continua morrendo por ter lido errado.

A ENVERGADURA E O PONTO DELICADO, E JA MORDEU UMA VEZ
------------------------------------------------------
O corpo emprestado tinha ~2,7 blocos de envergadura sobre uma hitbox de 1,2: a
silhueta MENTIA sobre o alcance, o tempo todo, inclusive parada. O contrato
desta entrega resolve isso pela raiz -- a asa nasce DOBRADA no geo, e quem abre
a asa e a ANIMACAO. Consequencia direta para esta lane:

    ASA ABERTA E DISPLAY, E DISPLAY E EXATAMENTE O AVISO.

Ou seja, a maior silhueta que a ave apresenta existe SO durante os 30 ticks em
que recuar ainda resolve. Fora deles ela e um bicho de 1,2 bloco. Por isso
`conferir_que_abrir_a_asa_muda_a_silhueta` mede a envergadura em pixels nos dois
estados e reprova se a diferenca nao der para ver: uma asa que "abre" 8% nao e
um aviso, e a falha seria completamente silenciosa.

TICKS: COPIADOS DO SERVIDOR
----------------------------
Aviso 30 ticks, mergulho 14/6/18. Saem de `HunterExamProfiles.spiderEagleNest()`
e `.spiderEagleDive()`; se mudarem la, mudam aqui e regera. Um aviso de 0,5s num
telegrafo de 1,5s promete o bote antes da hora, e o jogador aprende um relogio
que o jogo nao cumpre. `conferir_as_duracoes_do_servidor` reprova.

AS COSTURAS -- E O QUE O CONTROLLER FAZ COM ELAS
-------------------------------------------------
O Java registra UM controller so, com 4 ticks de transicao, e a descida inteira
dura 6. Uma transicao de 4 ticks atravessando uma fase de 6 comeria o bote em
mistura: quem levasse a pancada nunca veria a asa fechar.

A saida nao e encurtar a transicao -- e fazer com que nao haja o que misturar.
Quando o ultimo quadro de um clipe E o primeiro do seguinte, os 4 ticks de blend
viram um no-op e a troca acontece no quadro exato. Por isso esta cadeia e
conferida osso a osso, canal a canal, com o valor INTERPOLADO (que e o que o
jogador ve), e nao com a presenca de uma chave:

    warn(0)      == dive_windup(0)     a ave decide mergulhar
    windup(fim)  == dive(0)            ela se larga
    dive(0)      == dive(fim)          a descida repete sem tranco
    dive(fim)    == recover(0)         ela freia
    recover(fim) == fly(0)             volta ao cruzeiro

E as poses de costura nao sao escritas duas vezes: `quadro_em` LE o quadro do
clipe vizinho ja construido. Escrever a mao dos dois lados seria duas fontes
para a mesma verdade, e a divergencia apareceria como um tranco de um quadro --
tempo de sobra para incomodar e de menos para alguem conseguir descrever.

EIXOS -- CONFERIDOS no proprio geo, nao chutados
-------------------------------------------------
A convencao vanilla e y=0 no chao, -Z na FRENTE, +X a ESQUERDA do bicho.
`conferir_eixos` le o geo e reprova se o BICO nao estiver a frente e a CAUDA
atras -- que e exatamente como o contrato manda descobrir a frente deste mob.
Derivado dela, e so dela:

    rotacao X positiva  -> o bico SOBE (massa em -Z vai para +Y);
                           a CAUDA (massa em +Z) DESCE;
                           perna pendurada (massa em -Y) balanca para a FRENTE
    rotacao X negativa  -> o bico DESCE; o corpo APONTA PARA BAIXO;
                           o bico ABRE (a massa dele esta a frente da dobradica)
    rotacao Z positiva  -> o lado esquerdo (+X) SOBE: e a batida de asa
    posicao  -Z         -> projeta para a FRENTE (a cabeca no aviso)

O EIXO Y E DEDUZIDO, NAO OBSERVADO -- e esta e a unica suposicao desta lane.
X e Z acima foram confirmados em jogo nos mobs irmaos, e os dois concordam com a
mesma MAO (regra da mao direita, ciclo X:(Y,Z), Y:(Z,X), Z:(X,Y)). Dois eixos
fixam a mao; a mao fixa o terceiro. Logo:

    rotacao Y positiva  -> a asa ESQUERDA (+X) varre para a FRENTE (-Z)

Tudo que escreve Y aqui e ESPELHADO entre os dois lados (esquerda +v, direita
-v), como o PhantomModel vanilla e o modelo que sai de cena ja faziam: com o
sinal trocado o movimento continua simetrico. O que o sinal decide e se a asa
abre para FORA ou varre para TRAS -- e se em jogo ela abrir para o lado errado,
`SENTIDO_DE_Y = -1.0` inverte tudo num lugar so. Isso esta no relato como ponto
cego declarado, nao como certeza.

Regerar:  python art-source/enemies/spider_eagle/spider_eagle_animacoes.py
Exporta:  .../animations/entity/spider_eagle.animation.json
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum.animacao import corrigir_sentido_de_z_em  # noqa: E402

# --------------------------------------------------------------- o contrato

DIR_GEO = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "geo", "entity")
DIR_ANIM = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                        "animations", "entity")

MOB = "spider_eagle"

# A HIERARQUIA e o contrato, e ela e a UNICA lista de ossos deste arquivo: o
# conjunto de ossos sai daqui derivado. Listar os nomes de novo criaria duas
# fontes para a mesma verdade, e a divergencia se manifestaria como um membro
# parado -- sem erro nenhum.
#
# Ela nao e so nomenclatura: `wing_tip_*` ser filho de `wing_*` e o que faz a
# ponta HERDAR a rotacao do braco, e e essa heranca que a conta da abertura
# desconta. Um geo que pendurasse a ponta direto no `body` passaria no portao
# generico do Java (o pai existe) e abriria a asa com o dobro do angulo.
PAI = {
    "root": None,
    "body": "root",
    "head": "body",
    "beak": "head",
    "wing_left": "body",
    "wing_tip_left": "wing_left",
    "wing_right": "body",
    "wing_tip_right": "wing_right",
    "tail": "body",
    "leg_front_left": "body",
    "talon_front_left": "leg_front_left",
    "leg_front_right": "body",
    "talon_front_right": "leg_front_right",
    "leg_back_left": "body",
    "talon_back_left": "leg_back_left",
    "leg_back_right": "body",
    "talon_back_right": "leg_back_right",
    "spinner": "body",
}
OSSOS = tuple(PAI)

# `root` NAO E ANIMADO, e isso e decisao. Ele e a ancora que o renderer alinha
# com a hitbox; girar ou transladar a raiz move a silhueta inteira para fora da
# caixa de colisao, e num mob cuja promessa e "recue ate sair do raio" isso
# desloca a leitura de distancia do jogador sem deslocar nada no servidor.
# Quem precisa de peso vai no `body`, que tem o corpo inteiro pendurado.
NAO_ANIMADOS = ("root",)
ANIMAVEIS = tuple(o for o in OSSOS if o not in NAO_ANIMADOS)

# QUATRO PERNAS E UMA FIANDEIRA: e daqui que sai o lado aracnideo da ficha. Se
# a animacao nunca mexer nesses cinco ossos, o mob vira uma ave comum com pernas
# a mais -- ver `conferir_que_o_lado_aracnideo_aparece`.
PERNAS = tuple(o for o in ANIMAVEIS if o.startswith("leg_"))
GARRA = {p: next(o for o in ANIMAVEIS if PAI[o] == p) for p in PERNAS}
ASAS = ("wing_left", "wing_right")
PONTAS = {"wing_left": "wing_tip_left", "wing_right": "wing_tip_right"}

CLIPES = ("perch", "fly", "warn", "dive_windup", "dive", "recover",
          "hurt", "death")

# O TIPO DE REPETICAO MORA AQUI E SO AQUI. O codigo pede
# then(nome, Animation.LoopType.DEFAULT), e DEFAULT delega para este campo --
# entao este dicionario E o comportamento, nao a documentacao dele. Cada linha
# foi decidida, nao herdada:
LOOPS = {
    # pousada e voo sao os dois REPOUSOS da ave: repetem enquanto durarem.
    "perch": True,
    "fly": True,
    # o aviso repete porque a janela de recuo pode durar mais que o clipe -- e
    # um aviso que PARASSE de avisar enquanto a ave ainda esta avisando seria a
    # pior mentira que este mob consegue contar.
    "warn": True,
    # quem manda no tempo da subida e o servidor: 14 ticks e o piso, nao o teto.
    # hold_on_last_frame segura a ave no alto em vez de reiniciar a subida (loop)
    # ou devolve-la ao voo nivelado bem antes de se largar (false).
    "dive_windup": "hold_on_last_frame",
    # a descida repete em vez de congelar: uma ave parada no ar no meio do bote
    # le como travamento. Como o primeiro quadro E o ultimo, a repeticao nao
    # tem tranco -- ver `conferir_o_loop_fecha`.
    "dive": True,
    # a freada acontece UMA vez e termina no cruzeiro; repeti-la poria a ave
    # freando em loop enquanto ela ja voltou a voar.
    "recover": False,
    "hurt": False,
    # a entidade so some depois do clipe: voltar a pose viva no ultimo quadro e
    # um bug visual de meio segundo, e e o ultimo que o jogador ve.
    "death": "hold_on_last_frame",
}

# ------------------------------------------------------------------- ticks

TICKS_POR_SEGUNDO = 20.0
# Copiados do SERVIDOR. NestGuardRules da o aviso; AttackDefinition da o resto.
TICKS_DE_AVISO = 30
TICKS_WINDUP = 14
TICKS_ACTIVE = 6
TICKS_RECOVERY = 18

DUR_WARN = TICKS_DE_AVISO / TICKS_POR_SEGUNDO       # 1.50
DUR_WINDUP = TICKS_WINDUP / TICKS_POR_SEGUNDO       # 0.70
DUR_DIVE = TICKS_ACTIVE / TICKS_POR_SEGUNDO         # 0.30
DUR_RECOVER = TICKS_RECOVERY / TICKS_POR_SEGUNDO    # 0.90

DUR_PERCH = 3.0
DUR_FLY = 0.9
DUR_HURT = 0.25
DUR_DEATH = 1.3

# -------------------------------------------------------------- a hitbox

# sized(1.2F, 0.9F), em pixels de modelo. Nao e botao de tuning: e a caixa que o
# servidor usa para tudo, e a regua contra a qual a envergadura e julgada.
HITBOX_LARGURA_PX = 1.2 * 16.0
HITBOX_ALTURA_PX = 0.9 * 16.0

# --------------------------------------------------------------- as asas

# Ver o cabecalho: o Y e deduzido da mao dos eixos, nao observado em jogo.
SENTIDO_DE_Y = 1.0

# Fracao da dobra do geo que cada estado DESFAZ. 0 = como o geo nasce (dobrada),
# 1 = esticada; negativo aperta MAIS que o geo.
#
# A ordem destes numeros e a leitura inteira do mob, da menor silhueta para a
# maior: mergulho < pousada < morte < cruzeiro < AVISO.
ABERTA_MERGULHO = -0.25   # colada ao corpo: a ave virou um projetil
ABERTA_POUSADA = 0.0      # como o geo nasce
ABERTA_MORTE = 0.5        # meio aberta, sem forca: asa que cede, nao que voa
ABERTA_CRUZEIRO = 0.85    # punho levemente flexionado: e assim que um bicho voa
ABERTA_AVISO = 1.0        # envergadura maxima -- o display

# ONDE A SILHUETA SEPARA, E ONDE ELA NAO SEPARA
# ----------------------------------------------
# Este bloco existe porque a regua respondeu uma coisa que o texto do contrato
# nao antecipava, e a resposta mudou onde os portoes moram.
#
# Medindo (ver `envergadura`), a escada deste geo fica assim, em px:
#
#     mergulho 17,3  <  pousada 19,0  <<  cruzeiro 31,8  <  AVISO 32,4
#
# POUSADA -> AVISO da +71%, e e um abismo: e essa a diferenca que ensina, porque
# pousada e o estado em que o jogador encontra a ave ANTES de fazer besteira.
# CRUZEIRO -> AVISO da +2%, e isso nao e defeito de ajuste: a ponta dobra ~117
# graus em Y, e o canto traseiro dela ja esta praticamente no maximo de X quando
# a dobra passa de 90 graus. Depois disso, esticar mais quase nao alarga nada.
#
# A conclusao que os numeros impoem: entre VOANDO e AVISANDO, a silhueta nao e o
# que separa. Fechar o punho do cruzeiro ate forcar uma diferenca de envergadura
# produziria uma ave voando com a asa dobrada -- que le como bicho quebrado, e e
# um erro pior do que o que estaria corrigindo.
#
# Entao a leitura de "voando" contra "avisando" mora onde ela ja morava no
# contrato: AMPLITUDE e ANDAMENTO. 45 graus contra 16 (2,8x) e 1,5s contra 0,9s.
# E e ISSO que `conferir_o_aviso_e_amplo_e_lento` guarda, com margens de verdade,
# em vez de guardar 2% de envergadura com uma margem de mentira.
MARGEM_DA_ASA_ABERTA = 0.40   # quanto voar precisa alargar sobre estar pousada
MARGEM_DO_RECOLHIMENTO = 0.05  # quanto o bote precisa estreitar sobre o pouso
RAZAO_MINIMA_DE_AMPLITUDE = 2.0  # o aviso bate quantas vezes mais largo
RAZAO_MINIMA_DE_LENTIDAO = 1.4   # e quantas vezes mais devagar

# Batidas, em graus de rotacao Z do braco.
BATIDA_DE_CRUZEIRO = 16.0   # economica: a mesma do corpo que sai de cena
BATIDA_DE_AVISO = 45.0      # quase tres vezes mais larga -- e o "sai daqui"
BATIDA_DE_SUBIDA = 34.0     # forte, para ganhar altura depressa

# Periodo das batidas. O AVISO E O MAIS LENTO DE TODOS, de proposito: amplo e
# lento le como ameaca de display; amplo e rapido le como ataque comecando.
CICLO_CRUZEIRO = 0.9
CICLO_AVISO = 1.5   # uma batida so em 30 ticks
CICLO_SUBIDA = 0.35  # duas batidas em 14 ticks

# A ponta atrasa em relacao ao braco -- e esse atraso que faz uma asa parecer uma
# asa, e nao uma tabua com dobradica. Fracao do periodo.
ATRASO_DA_PONTA = 0.12

# ------------------------------------------------------- poses, em graus

CORPO_NO_AVISO = 12.0      # peito erguido, encarando o intruso
CORPO_NA_SUBIDA = 30.0     # bico para cima: ela esta ganhando altura
CORPO_NO_MERGULHO = -62.0  # apontada para baixo; o angulo que faz um bote
CORPO_NA_FREADA = 24.0     # peito jogado a frente, freando no ar

# A CABECA E LIDA SOMADA AO CORPO, e os dois numeros abaixo so fazem sentido
# assim. Na subida o corpo vai a +30 e o pescoco a -26: liquido +4, a ave sobe
# mas CONTINUA olhando para o intruso -- quem esta sendo mirado precisa ver que
# esta sendo mirado. No mergulho o corpo vai a -62 e o pescoco a +22: liquido
# -40, a cabeca desce MENOS que o corpo, e e ela que mira o bote.
#
# Escrever -20 na subida (o reflexo de "cabeca junto com o corpo") somaria +10 e
# poria a ave olhando para o ceu no exato momento em que ela escolhe o alvo. Nao
# daria erro nenhum.
CABECA_NA_SUBIDA = -26.0
CABECA_NO_MERGULHO = 22.0
CABECA_NO_AVISO = -8.0     # projetada para a frente e um pouco para baixo
PROJECAO_DA_CABECA = 1.6   # px para -Z: o pescoco se estende no aviso

BICO_NO_AVISO = -30.0      # escancarado: o grito faz parte do display
BICO_NO_MERGULHO = -14.0   # entreaberto; ela ja nao esta mais avisando
BICO_NA_SUBIDA = -20.0

CAUDA_NO_AVISO = 14.0      # baixa e aberta, contrapeso do peito erguido
CAUDA_NO_MERGULHO = -8.0   # alinhada com o corpo: leme, nao freio
CAUDA_NA_FREADA = 34.0     # baixada de vez: a cauda e o primeiro freio de ar

# Garras. No aviso elas se ESTENDEM (faz parte do "sai daqui"); no mergulho vao
# a frente, porque sao elas que chegam primeiro.
GARRAS_NO_AVISO = 30.0
GARRAS_NO_MERGULHO = 70.0
ABRIR_AS_GARRAS = 7.0      # afastamento lateral entre o par, em Z

# A garra CONTRA-GIRA a fracao da perna para continuar apontada para o chao em
# vez de girar junto e apontar para o jogador. Escrita a mao, ela fica com a fase
# certa hoje e errada na primeira correcao da perna.
CONTRA_GARRA = -0.45

FIANDEIRA_NO_AVISO = 16.0     # a fiandeira se abre: e a parte aracnidea do aviso
FIANDEIRA_RECOLHIDA = -8.0    # colada ao abdome em voo e no mergulho

# Fase que faz a cossenoide VALER A BASE em t=0. cos(2*pi*0.75) = 0, e a curva
# sobe a partir dai. Qualquer outra fase quebraria as costuras, porque o primeiro
# quadro de fly e de dive deixaria de ser a pose que o vizinho termina.
FASE_QUE_COMECA_NA_BASE = 0.75

# ------------------------------------------------ limites que os portoes usam

# O braco tem de JA apontar para fora no geo: a dobra mora na juncao com a
# ponta, que e onde o contrato pediu o pivot. Um braco dobrado exigiria compor
# duas rotacoes em cadeia para esticar a asa, e a conta exata de abertura desta
# lane deixaria de valer -- em silencio.
DOBRA_MAXIMA_DO_BRACO = 25.0
# Abaixo disto, a asa "aberta" e a asa "dobrada" tem a mesma silhueta e o aviso
# deixa de existir como aviso. Em px de envergadura.
GANHO_MINIMO_DA_ABERTURA = 0.25


# -------------------------------------------------------------- ferramentas


def tempo(t):
    """Chave de keyframe: string, sempre com decimal, sem zero sobrando."""
    texto = ("%.4f" % round(t, 4)).rstrip("0")
    return texto + "0" if texto.endswith(".") else texto


def num(v):
    v = round(v, 2)
    return int(v) if v == int(v) else v


def vetor(x=0.0, y=0.0, z=0.0):
    """Rotacao ou posicao: o neutro e zero."""
    return [num(x), num(y), num(z)]


def escala(x=1.0, y=1.0, z=1.0):
    """Escala: o neutro e UM. Vetor de escala com zero some com o osso."""
    return [num(x), num(y), num(z)]


NEUTRO = {"rotation": vetor, "position": vetor, "scale": escala}
CANAIS = tuple(NEUTRO)

# Os UNICOS ossos que algum clipe TRANSLADA. `aplicar` escreve `position` neutro
# so para estes: escrever zeros de posicao nos outros quinze seria ruido num
# arquivo cujo unico leitor humano e o diff, e a costura ja trata canal ausente
# como neutro. Quem transladar um osso novo reprova em
# `conferir_que_so_transladam_os_declarados` -- e ai acrescenta o nome AQUI, que
# e o que faz a pose de costura passar a incluir aquele canal.
OSSOS_QUE_TRANSLADAM = ("body", "head")


def curva(bones, osso, canal, pares):
    """Acumula keyframes de um canal de um osso.

    Escreve por CHAVE DE TEMPO, entao uma curva posterior sobrescreve a pose que
    `aplicar` deitou -- que e exatamente a ordem desejada.
    """
    alvo = bones.setdefault(osso, {}).setdefault(canal, {})
    for t, v in pares:
        alvo[tempo(t)] = v


def membro(bones, perna, pares):
    """Keya a perna e DERIVA a garra dela.

    So o eixo X contra-gira: Z carrega o afastamento do par, e contra-girar o
    afastamento viraria a garra para dentro da barriga.
    """
    curva(bones, perna, "rotation", pares)
    curva(bones, GARRA[perna], "rotation",
          [(t, vetor(x=v[0] * CONTRA_GARRA)) for t, v in pares])


def ciclo(duracao, periodo, amplitude, base=0.0, fase=FASE_QUE_COMECA_NA_BASE,
          amostras=8):
    """Cossenoide amostrada -- e a batida de asa inteira.

    Amostrar (em vez de escrever os extremos a mao) e o que mantem a fase certa
    quando alguem mexe no periodo, e o que mantem a interpolacao linear do
    formato 1.8.0 parecendo curva em vez de zigue-zague.
    """
    passo = periodo / amostras
    n = int(round(duracao / passo))
    return [(i * passo,
             base + amplitude * math.cos(2 * math.pi * (i * passo / periodo + fase)))
            for i in range(n + 1)]


# ---------------------------------------------------- quadros e interpolacao


def valor_em(quadros, t, neutro):
    """O valor que o JOGADOR ve em t, e nao a chave que existe em t.

    As costuras se conferem com isto, e nao com a presenca de um keyframe: um
    canal cuja ultima chave esta antes do fim SEGURA aquele valor ate o fim, e
    comparar chaves em vez de valores reprovaria um clipe correto (ou, pior,
    aprovaria um errado por comparar duas ausencias).
    """
    if not quadros:
        return neutro
    ordenados = sorted(((float(k), v) for k, v in quadros.items()), key=lambda kv: kv[0])
    if t <= ordenados[0][0]:
        return list(ordenados[0][1])
    if t >= ordenados[-1][0]:
        return list(ordenados[-1][1])
    for (ta, va), (tb, vb) in zip(ordenados, ordenados[1:]):
        if ta <= t <= tb:
            f = 0.0 if tb == ta else (t - ta) / (tb - ta)
            return [num(a + (b - a) * f) for a, b in zip(va, vb)]
    return list(ordenados[-1][1])


def quadro_em(clipe, t):
    """A pose COMPLETA do clipe em t: todo osso animavel, todo canal.

    Completa de proposito. Osso que um clipe nao cita nao fica onde estava: ele
    volta para o DEFAULT DO MODELO. Se a costura so comparasse os ossos que os
    dois clipes citam, um `dive` que esquecesse a fiandeira casaria com um
    `windup` que a recolhe -- e a fiandeira saltaria sozinha no quadro do bote.
    """
    pose = {}
    for osso in ANIMAVEIS:
        canais = clipe["bones"].get(osso, {})
        pose[osso] = {c: valor_em(canais.get(c, {}), t, NEUTRO[c]()) for c in CANAIS}
    return pose


def pose_neutra():
    return {osso: {c: NEUTRO[c]() for c in CANAIS} for osso in ANIMAVEIS}


def aplicar(bones, pose, t):
    """Deita a pose INTEIRA no instante t -- inclusive os zeros.

    Os zeros sao escritos de proposito. Um canal cuja primeira chave esta em
    t=0.12 vale aquele valor TAMBEM antes de 0.12: o clipe comecaria ja deslocado,
    e a costura com o clipe anterior quebraria sem nenhuma chave errada a vista.
    """
    for osso, canais in pose.items():
        for canal, valor in canais.items():
            if canal == "scale" and valor == escala():
                continue  # escala neutra nao precisa ser dita; ela nao "segura" nada
            if canal == "position" and valor == vetor() \
                    and osso not in OSSOS_QUE_TRANSLADAM:
                continue
            curva(bones, osso, canal, [(t, list(valor))])


# -------------------------------------------------------- a asa, em um lugar


def pose_das_asas(pose, abertura, dobra, batida=0.0, braco_x=0.0, braco_y=0.0,
                  braco_z=0.0, ponta_extra_z=0.0):
    """Escreve os QUATRO ossos de asa de uma vez, espelhados.

    `dobra` sao os dois angulos LIDOS DO GEO que esticam a ponta (ver
    `angulos_que_esticam`). `abertura` e a fracao deles que este estado desfaz.

    Espelhar aqui, num lugar so, e o que impede o erro que nao da erro: Z e Y
    invertidos entre os lados. Escritos a mao, um dos dois lados sobrevive a
    proxima correcao com o sinal antigo, e a ave passa a bater uma asa para cima
    e a outra para baixo -- o que, numa silhueta contra o ceu, le como bug de
    textura e nao como bug de sinal.
    """
    dobra_y, dobra_z = dobra
    for asa in ASAS:
        lado = 1.0 if asa.endswith("left") else -1.0
        pose[asa]["rotation"] = vetor(x=braco_x,
                                      y=braco_y * SENTIDO_DE_Y * lado,
                                      z=(braco_z + batida) * lado)
        pose[PONTAS[asa]]["rotation"] = vetor(
            y=dobra_y * abertura * SENTIDO_DE_Y * lado,
            z=(dobra_z * abertura + ponta_extra_z) * lado)
    return pose


def pose_das_pernas(pose, x_frente, x_tras, abrir=0.0):
    for perna in PERNAS:
        lado = 1.0 if perna.endswith("left") else -1.0
        x = x_frente if "front" in perna else x_tras
        pose[perna]["rotation"] = vetor(x=x, z=abrir * lado)
        pose[GARRA[perna]]["rotation"] = vetor(x=x * CONTRA_GARRA)
    return pose


# ------------------------------------------------------------- as poses-base


def pose_de_cruzeiro(m):
    p = pose_neutra()
    pose_das_asas(p, ABERTA_CRUZEIRO, m["dobra"])
    pose_das_pernas(p, m["recolhimento"], m["recolhimento"])
    p["spinner"]["rotation"] = vetor(x=FIANDEIRA_RECOLHIDA)
    return p


def pose_de_aviso(dobra):
    """A pose em que o aviso ORBITA. Tudo que le como ameaca de display esta aqui.

    Ela e tambem a pose em que `dive_windup` COMECA -- ver a cadeia de costuras
    no cabecalho. A ave nao volta ao neutro entre avisar e mergulhar: ela ja
    esta com a asa aberta e as garras para fora quando decide subir.
    """
    p = pose_neutra()
    p["body"]["rotation"] = vetor(x=CORPO_NO_AVISO)
    p["head"]["rotation"] = vetor(x=CABECA_NO_AVISO)
    p["head"]["position"] = vetor(z=-PROJECAO_DA_CABECA)
    p["beak"]["rotation"] = vetor(x=BICO_NO_AVISO)
    p["tail"]["rotation"] = vetor(x=CAUDA_NO_AVISO)
    pose_das_asas(p, ABERTA_AVISO, dobra, braco_x=-4.0, braco_y=10.0)
    pose_das_pernas(p, GARRAS_NO_AVISO, GARRAS_NO_AVISO * 0.55, ABRIR_AS_GARRAS)
    p["spinner"]["rotation"] = vetor(x=FIANDEIRA_NO_AVISO)
    return p


def pose_de_mergulho(dobra):
    """Projetil: asa colada, corpo apontado para baixo, garra a frente."""
    p = pose_neutra()
    p["body"]["rotation"] = vetor(x=CORPO_NO_MERGULHO)
    p["head"]["rotation"] = vetor(x=CABECA_NO_MERGULHO)
    p["beak"]["rotation"] = vetor(x=BICO_NO_MERGULHO)
    p["tail"]["rotation"] = vetor(x=CAUDA_NO_MERGULHO)
    # braco_z negativo BAIXA o lado +X: as duas asas caem coladas ao corpo.
    pose_das_asas(p, ABERTA_MERGULHO, dobra, braco_x=ASA_NO_MERGULHO[0],
                  braco_y=ASA_NO_MERGULHO[1], braco_z=ASA_NO_MERGULHO[2])
    pose_das_pernas(p, GARRAS_NO_MERGULHO, GARRAS_NO_MERGULHO * 0.6)
    p["spinner"]["rotation"] = vetor(x=FIANDEIRA_RECOLHIDA)
    return p


# ------------------------------------------------------------------- clipes


def perch(m):
    """Pousada no ninho. E o estado em que o jogador a ENCONTRA.

    Tres coisas, e so tres: asas DOBRADAS (a silhueta pequena -- e o contraste
    contra o qual o aviso vai ser lido), as quatro pernas apoiadas com o peso
    passando devagar de um par para o outro, e a cabeca varrendo o canion.

    A FIANDEIRA FICA QUIETA, e isso e afirmacao e nao esquecimento: ela e escrita
    em zero nos dois extremos. O ninho de teia ja esta feito; a fiandeira
    trabalhando aqui diria que a ave esta tecendo agora, que e uma coisa que o
    servidor nao faz. `conferir_a_fiandeira_esta_quieta_no_pouso` reprova quem
    "der vida" a ela depois.
    """
    dur = DUR_PERCH
    b = {}
    pose = pose_neutra()
    pose_das_asas(pose, ABERTA_POUSADA, m["dobra"])
    aplicar(b, pose, 0.0)
    aplicar(b, pose, dur)

    # Respiracao: uma por 3s. Um bicho pousado e vigiando nao esta ofegante.
    curva(b, "body", "position", [(t, vetor(y=v)) for t, v in ciclo(dur, 3.0, 0.35)])
    curva(b, "body", "rotation", [(t, vetor(x=v)) for t, v in ciclo(dur, 3.0, -1.2)])
    # A varredura: lenta e completa, uma so por volta do clipe. O sinal de Y aqui
    # nao importa -- ela olha para os dois lados.
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.9, vetor(x=3.0, y=-24.0)), (1.5, vetor(y=-4.0)),
        (2.2, vetor(x=2.0, y=20.0)), (dur, vetor())])
    curva(b, "beak", "rotation", [
        (0.0, vetor()), (1.2, vetor()), (1.32, vetor(x=-12.0)),
        (1.5, vetor(x=-3.0)), (1.62, vetor()), (dur, vetor())])
    # Um ajeitar de asa, uma vez por volta, sem abrir nada.
    for asa in ASAS:
        lado = 1.0 if asa.endswith("left") else -1.0
        curva(b, asa, "rotation", [
            (0.0, vetor()), (1.8, vetor()), (1.95, vetor(z=6.0 * lado)),
            (2.15, vetor(z=-2.0 * lado)), (2.3, vetor()), (dur, vetor())])
    # O peso passa do par da frente para o de tras e volta.
    for perna in PERNAS:
        amplitude = 1.8 if "front" in perna else -1.8
        membro(b, perna, [(t, vetor(x=v)) for t, v in ciclo(dur, dur, amplitude)])
    curva(b, "tail", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, dur, 2.5, 1.0)])
    return {"loop": LOOPS["perch"], "animation_length": dur, "bones": b}


def fly(m):
    """Cruzeiro: batida regular e economica, 0.9s por ciclo.

    O que faz esta asa parecer uma asa e o ATRASO DA PONTA: ela segue o braco com
    uma fracao de periodo de diferenca, em vez de virar junto. Sem isso a asa e
    uma tabua com dobradica -- e o problema nao aparece parado, so em movimento.

    As pernas ficam RECOLHIDAS sob o corpo, e o quanto e lido do geo: uma perna
    que continuasse pendurada em voo aumentaria a silhueta vertical do bicho
    exatamente onde a hitbox nao aumenta.
    """
    dur = DUR_FLY
    b = {}
    aplicar(b, pose_de_cruzeiro(m), 0.0)
    aplicar(b, pose_de_cruzeiro(m), dur)

    dobra_y, dobra_z = m["dobra"]
    for asa in ASAS:
        lado = 1.0 if asa.endswith("left") else -1.0
        curva(b, asa, "rotation", [(t, vetor(z=v * lado))
                                   for t, v in ciclo(dur, CICLO_CRUZEIRO,
                                                     BATIDA_DE_CRUZEIRO)])
        base_y = dobra_y * ABERTA_CRUZEIRO * SENTIDO_DE_Y * lado
        base_z = dobra_z * ABERTA_CRUZEIRO
        curva(b, PONTAS[asa], "rotation", [
            (t, vetor(y=base_y, z=(base_z + v) * lado))
            for t, v in ciclo(dur, CICLO_CRUZEIRO, BATIDA_DE_CRUZEIRO * 0.6, 0.0,
                              FASE_QUE_COMECA_NA_BASE + ATRASO_DA_PONTA)])
    # Corpo estavel: sobe e desce DUAS vezes por batida, quase nada. Um corpo que
    # oscilasse junto com a asa leria como planador em turbulencia.
    curva(b, "body", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, CICLO_CRUZEIRO / 2.0, 0.4)])
    curva(b, "body", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, CICLO_CRUZEIRO, 2.0)])
    # A cauda e LEME: ela corrige a guinada, nao acompanha a batida.
    curva(b, "tail", "rotation",
          [(t, vetor(x=v * 0.5, y=v)) for t, v in ciclo(dur, CICLO_CRUZEIRO, 4.0)])
    curva(b, "head", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, CICLO_CRUZEIRO / 2.0, -1.5)])
    return {"loop": LOOPS["fly"], "animation_length": dur, "bones": b}


def warn(m):
    """O AVISO. 30 ticks em que recuar ainda salva -- o clipe mais importante.

    Ele existe para dizer UMA coisa: sai daqui. E a diferenca entre "sai daqui" e
    "vou atacar" nao esta na pose, esta no ANDAMENTO. Amplo e LENTO le como
    display; amplo e RAPIDO le como o ataque ja comecando. Por isso a batida do
    aviso e a mais larga (45 graus) E a mais lenta (uma so em 1,5s) de todos os
    clipes deste arquivo, e `conferir_o_aviso_e_amplo_e_lento` reprova quem
    inverter qualquer um dos dois.

    A asa abre ate a envergadura MAXIMA aqui, e so aqui. Essa e a decisao da
    entrega inteira: a maior silhueta da ave existe exatamente durante a janela
    de recuo, e nao o tempo todo como no corpo emprestado.
    """
    dur = DUR_WARN
    b = {}
    base = pose_de_aviso(m["dobra"])
    aplicar(b, base, 0.0)
    aplicar(b, base, dur)

    dobra_y, dobra_z = m["dobra"]
    for asa in ASAS:
        lado = 1.0 if asa.endswith("left") else -1.0
        curva(b, asa, "rotation", [
            (t, vetor(x=-4.0, y=10.0 * SENTIDO_DE_Y * lado, z=v * lado))
            for t, v in ciclo(dur, CICLO_AVISO, BATIDA_DE_AVISO)])
        base_y = dobra_y * ABERTA_AVISO * SENTIDO_DE_Y * lado
        base_z = dobra_z * ABERTA_AVISO
        curva(b, PONTAS[asa], "rotation", [
            (t, vetor(y=base_y, z=(base_z + v) * lado))
            for t, v in ciclo(dur, CICLO_AVISO, BATIDA_DE_AVISO * 0.45, 0.0,
                              FASE_QUE_COMECA_NA_BASE + ATRASO_DA_PONTA)])
    # O corpo sobe com a batida: o peito se ergue e volta. Duas por clipe.
    curva(b, "body", "rotation",
          [(t, vetor(x=CORPO_NO_AVISO + v)) for t, v in ciclo(dur, CICLO_AVISO, 5.0)])
    curva(b, "body", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, CICLO_AVISO / 2.0, 0.8)])
    # A cabeca fica PROJETADA o clipe inteiro e balanca de leve: ela nao varre o
    # ambiente como no pouso, porque o aviso e PARA ALGUEM. Uma cabeca varrendo
    # aqui diria ao intruso que a ameaca nao e com ele.
    curva(b, "head", "rotation",
          [(t, vetor(x=CABECA_NO_AVISO + v)) for t, v in ciclo(dur, CICLO_AVISO, 6.0)])
    curva(b, "head", "position",
          [(t, vetor(z=-PROJECAO_DA_CABECA + v * 0.25))
           for t, v in ciclo(dur, CICLO_AVISO, -0.8)])
    # O grito acompanha o topo da batida.
    curva(b, "beak", "rotation", [
        (0.0, vetor(x=BICO_NO_AVISO)), (0.35, vetor(x=-12.0)),
        (0.75, vetor(x=BICO_NO_AVISO)), (1.1, vetor(x=-14.0)),
        (dur, vetor(x=BICO_NO_AVISO))])
    curva(b, "tail", "rotation",
          [(t, vetor(x=CAUDA_NO_AVISO + v)) for t, v in ciclo(dur, CICLO_AVISO, -6.0)])
    # As garras se estendem e se recolhem meio passo: garra parada le como pose,
    # garra que trabalha le como intencao.
    for perna in PERNAS:
        frente = "front" in perna
        lado = 1.0 if perna.endswith("left") else -1.0
        base_x = GARRAS_NO_AVISO if frente else GARRAS_NO_AVISO * 0.55
        curva(b, perna, "rotation", [
            (t, vetor(x=base_x + v, z=ABRIR_AS_GARRAS * lado))
            for t, v in ciclo(dur, CICLO_AVISO, 7.0 if frente else 4.0)])
        curva(b, GARRA[perna], "rotation", [
            (t, vetor(x=(base_x + v) * CONTRA_GARRA))
            for t, v in ciclo(dur, CICLO_AVISO, 7.0 if frente else 4.0)])
    # A FIANDEIRA TRABALHA -- e a unica parte do display que nao e de ave. E ela
    # que faz o ninho de teia do canion, e mostra-la aqui e o que diz ao jogador
    # que o lugar que ele invadiu e dela.
    curva(b, "spinner", "rotation", [
        (0.0, vetor(x=FIANDEIRA_NO_AVISO)), (0.4, vetor(x=FIANDEIRA_NO_AVISO + 9.0)),
        (0.8, vetor(x=FIANDEIRA_NO_AVISO - 4.0)),
        (1.15, vetor(x=FIANDEIRA_NO_AVISO + 6.0)), (dur, vetor(x=FIANDEIRA_NO_AVISO))])
    return {"loop": LOOPS["warn"], "animation_length": dur, "bones": b}


def dive(m):
    """A DESCIDA. 6 ticks -- o clipe mais curto e o unico que nao se pode ler.

    O primeiro quadro E o ultimo, de proposito, e isso resolve tres coisas de uma
    vez: a repeticao (LOOPS['dive'] e True) nao da tranco, a costura com o windup
    fecha, e a costura com o recover tambem. Quem mexer na pose de mergulho mexe
    nas tres ao mesmo tempo -- que e o unico jeito de elas nao divergirem.

    O que se mexe DENTRO dos 6 ticks e a garra: ela e lancada a frente no meio da
    descida e volta. Uma pose congelada por 6 ticks nao le como bote, le como a
    ave travada no ar; e esticar a garra e o gesto que diz o que vai acontecer.
    """
    dur = DUR_DIVE
    b = {}
    pose = pose_de_mergulho(m["dobra"])
    aplicar(b, pose, 0.0)
    aplicar(b, pose, dur)
    # O lance da garra: fora e de volta, dentro da janela em que o dano acontece.
    for perna in PERNAS:
        frente = "front" in perna
        base_x = GARRAS_NO_MERGULHO if frente else GARRAS_NO_MERGULHO * 0.6
        pico = 18.0 if frente else 9.0
        membro(b, perna, [
            (0.0, vetor(x=base_x)), (0.14, vetor(x=base_x + pico)),
            (dur, vetor(x=base_x))])
    # O corpo mergulha mais fundo no meio e volta: a ave ACELERA na queda.
    curva(b, "body", "rotation", [
        (0.0, vetor(x=CORPO_NO_MERGULHO)), (0.16, vetor(x=CORPO_NO_MERGULHO - 9.0)),
        (dur, vetor(x=CORPO_NO_MERGULHO))])
    return {"loop": LOOPS["dive"], "animation_length": dur, "bones": b}


def dive_windup(m, inicio, fim):
    """14 ticks: ela SOBE. E o segundo aviso, e o ultimo.

    Comeca exatamente onde o `warn` orbita (`inicio`) e termina exatamente na pose
    do `dive` (`fim`) -- os dois vem dos clipes vizinhos ja construidos, e nao de
    numeros repetidos aqui.

    hold_on_last_frame porque quem manda no tempo e o servidor: 14 ticks e o
    minimo da fase, nao o maximo. Em loop, a ave reiniciaria a subida do zero
    enquanto ainda esta subindo.

    A VIRADA acontece nos ultimos 4 ticks (0.5 -> 0.7), nao ao longo dos 14: uma
    ave que fosse inclinando para baixo o telegrafo inteiro estaria anunciando o
    bote desde o primeiro quadro, e os 14 ticks de subida deixariam de ser tempo
    de reagir para virar contagem regressiva.
    """
    dur = DUR_WINDUP
    virada = 0.5
    b = {}
    aplicar(b, inicio, 0.0)

    # Batida forte e RAPIDA: e assim que se ganha altura. Ela para na virada, com
    # a asa NO ALTO -- e de la que ela se fecha para o bote. Fechar a asa a partir
    # do meio da batida tiraria da virada o unico gesto que ela tem.
    #
    # A PONTA continua ABERTA ate a virada: e nos ultimos 4 ticks que ela se
    # recolhe. Sem esta chave a ponta interpolaria da abertura do aviso ate a
    # dobra do mergulho ao longo dos 14 ticks inteiros -- a asa fecharia devagar
    # durante a subida, e a maior silhueta da ave desapareceria no meio da janela
    # em que ela ainda serve para alguma coisa.
    dobra_y, dobra_z = m["dobra"]
    for asa in ASAS:
        lado = 1.0 if asa.endswith("left") else -1.0
        curva(b, asa, "rotation", [
            (t, vetor(x=-4.0, y=10.0 * SENTIDO_DE_Y * lado, z=v * lado))
            for t, v in ciclo(dur, CICLO_SUBIDA, BATIDA_DE_SUBIDA)
            if t < virada])
        curva(b, asa, "rotation", [
            (virada, vetor(x=-8.0, y=10.0 * SENTIDO_DE_Y * lado,
                           z=BATIDA_DE_SUBIDA * lado))])
        curva(b, PONTAS[asa], "rotation", [
            (virada, vetor(y=dobra_y * ABERTA_AVISO * SENTIDO_DE_Y * lado,
                           z=dobra_z * ABERTA_AVISO * lado))])
    curva(b, "body", "rotation", [
        (0.0, vetor(x=CORPO_NO_AVISO)), (0.18, vetor(x=CORPO_NA_SUBIDA)),
        (virada, vetor(x=CORPO_NA_SUBIDA - 4.0))])
    curva(b, "body", "position", [
        (0.0, vetor()), (0.18, vetor(y=1.2)), (virada, vetor(y=1.8))])
    curva(b, "head", "rotation", [
        (0.0, vetor(x=CABECA_NO_AVISO)), (0.18, vetor(x=CABECA_NA_SUBIDA)),
        (virada, vetor(x=CABECA_NA_SUBIDA))])
    curva(b, "head", "position", [
        (0.0, vetor(z=-PROJECAO_DA_CABECA)), (virada, vetor(z=-0.6))])
    curva(b, "beak", "rotation", [
        (0.0, vetor(x=BICO_NO_AVISO)), (0.2, vetor(x=BICO_NA_SUBIDA)),
        (virada, vetor(x=BICO_NA_SUBIDA))])
    curva(b, "tail", "rotation", [
        (0.0, vetor(x=CAUDA_NO_AVISO)), (0.2, vetor(x=CAUDA_NO_AVISO + 10.0)),
        (virada, vetor(x=2.0))])
    # As garras se recolhem para o mergulho: elas voltam a sair na descida. O Z
    # (o afastamento do par) FECHA junto -- no aviso as garras estao ABERTAS, que
    # e display, e no bote elas estao alinhadas, que e o que passa pelo ar.
    # Esquecer o Z aqui nao daria erro: daria uma costura rompida de 7 graus em
    # quatro pernas, no quadro exato em que a ave decide mergulhar.
    for perna in PERNAS:
        frente = "front" in perna
        lado = 1.0 if perna.endswith("left") else -1.0
        de = GARRAS_NO_AVISO if frente else GARRAS_NO_AVISO * 0.55
        membro(b, perna, [
            (0.0, vetor(x=de, z=ABRIR_AS_GARRAS * lado)),
            (0.22, vetor(x=de - 16.0, z=ABRIR_AS_GARRAS * 0.5 * lado)),
            (virada, vetor(x=de - 6.0))])
    curva(b, "spinner", "rotation", [
        (0.0, vetor(x=FIANDEIRA_NO_AVISO)), (0.25, vetor(x=4.0)),
        (virada, vetor(x=FIANDEIRA_RECOLHIDA))])
    # A VIRADA: a pose do mergulho chega inteira no ultimo quadro. Tudo que
    # `inicio` escreveu e que `fim` tambem escreve encontra aqui o seu par.
    aplicar(b, fim, dur)
    return {"loop": LOOPS["dive_windup"], "animation_length": dur, "bones": b}


def recover(m, inicio, fim):
    """18 ticks: ela freia e volta a subir. loop false -- acontece uma vez.

    A freada e um gesto de AR, nao de musculo: a asa abre de vez (envergadura de
    aviso), a cauda desce como um flap e o peito se joga a frente. O pico e cedo
    (0.28s) porque frear demora menos do que voltar a voar; o resto do clipe e a
    ave se reendireitando ate o cruzeiro.

    Termina EXATAMENTE no primeiro quadro de `fly`, que chega pronto em `fim`.
    Um `recover` que terminasse no neutro poria a ave fechando a asa no ultimo
    quadro para reabri-la no primeiro do voo -- uma piscada de um quadro, do tipo
    que ninguem consegue descrever e todo mundo estranha.
    """
    dur = DUR_RECOVER
    freada = 0.28
    b = {}
    aplicar(b, inicio, 0.0)

    dobra_y, dobra_z = m["dobra"]
    for asa in ASAS:
        lado = 1.0 if asa.endswith("left") else -1.0
        curva(b, asa, "rotation", [
            (0.0, inicio[asa]["rotation"]),
            # Asa levada a frente e aberta: e assim que uma ave para no ar.
            (freada, vetor(x=-10.0, y=22.0 * SENTIDO_DE_Y * lado, z=40.0 * lado)),
            (0.55, vetor(x=-4.0, y=8.0 * SENTIDO_DE_Y * lado, z=14.0 * lado))])
        curva(b, PONTAS[asa], "rotation", [
            (0.0, inicio[PONTAS[asa]]["rotation"]),
            (freada, vetor(y=dobra_y * ABERTA_AVISO * SENTIDO_DE_Y * lado,
                           z=(dobra_z * ABERTA_AVISO + 12.0) * lado)),
            (0.55, vetor(y=dobra_y * ABERTA_AVISO * SENTIDO_DE_Y * lado,
                         z=dobra_z * ABERTA_AVISO * lado))])
    curva(b, "body", "rotation", [
        (0.0, vetor(x=CORPO_NO_MERGULHO)), (0.14, vetor(x=-20.0)),
        (freada, vetor(x=CORPO_NA_FREADA)), (0.55, vetor(x=8.0))])
    curva(b, "body", "position", [
        (0.0, vetor()), (freada, vetor(y=-1.0)), (0.55, vetor(y=0.4))])
    curva(b, "head", "rotation", [
        (0.0, vetor(x=CABECA_NO_MERGULHO)), (0.14, vetor(x=16.0)),
        (freada, vetor(x=-14.0)), (0.55, vetor(x=-4.0))])
    curva(b, "beak", "rotation", [
        (0.0, vetor(x=BICO_NO_MERGULHO)), (freada, vetor(x=-22.0)),
        (0.6, vetor(x=-4.0))])
    curva(b, "tail", "rotation", [
        (0.0, vetor(x=CAUDA_NO_MERGULHO)), (freada, vetor(x=CAUDA_NA_FREADA)),
        (0.6, vetor(x=8.0))])
    # As garras recolhem DEPOIS da freada: elas sao a ultima coisa que ela guarda.
    for perna in PERNAS:
        frente = "front" in perna
        de = GARRAS_NO_MERGULHO if frente else GARRAS_NO_MERGULHO * 0.6
        membro(b, perna, [
            (0.0, vetor(x=de)), (freada, vetor(x=de - 22.0)),
            (0.62, vetor(x=m["recolhimento"] * 0.7))])
    curva(b, "spinner", "rotation", [
        (0.0, vetor(x=FIANDEIRA_RECOLHIDA)), (freada, vetor(x=6.0)),
        (0.62, vetor(x=FIANDEIRA_RECOLHIDA))])
    # E o cruzeiro chega inteiro no ultimo quadro.
    aplicar(b, fim, dur)
    return {"loop": LOOPS["recover"], "animation_length": dur, "bones": b}


def hurt():
    """Tranco de 5 ticks. NAO TOCA A ASA, e isso e decisao.

    O Java registra UM controller so. Num controller unico, osso que o clipe
    corrente nao cita volta para o DEFAULT DO MODELO -- e o default deste modelo
    e a asa DOBRADA. Um `hurt` que keyasse asa fecharia a asa de uma ave em pleno
    voo por um quarto de segundo, e pior: se o tranco pegasse durante o aviso,
    fecharia justamente o sinal que diz "voce ainda pode recuar".

    Deixando asa e pernas de fora, este clipe pode um dia subir para um SEGUNDO
    controller e sobrepor so a cabeca e o tronco, que e onde um tranco mora. Se
    ele for parar no mesmo controller do corpo, a asa vai fechar por 5 ticks --
    e ai a correcao e aqui, nao la. `conferir_que_o_hurt_nao_toca_a_asa` guarda
    essa decisao contra a proxima pessoa que achar o clipe "incompleto".

    PONTO CEGO DECLARADO: hoje ninguem pede este clipe. Levar dano nao passa por
    estado sincronizado neste mob (esta escrito no proprio SpiderEagleEntity), e
    ele existe para quando passar.
    """
    dur = DUR_HURT
    b = {}
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.06, vetor(x=-9.0, z=7.0)), (0.14, vetor(x=4.0, z=-3.0)),
        (dur, vetor())])
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.05, vetor(x=14.0, y=10.0)), (0.15, vetor(x=-5.0, y=-4.0)),
        (dur, vetor())])
    curva(b, "beak", "rotation", [
        (0.0, vetor()), (0.05, vetor(x=-32.0)), (0.16, vetor(x=-8.0)), (dur, vetor())])
    curva(b, "tail", "rotation", [
        (0.0, vetor()), (0.07, vetor(x=-16.0)), (0.17, vetor(x=5.0)), (dur, vetor())])
    curva(b, "spinner", "rotation", [
        (0.0, vetor()), (0.06, vetor(x=12.0)), (dur, vetor())])
    return {"loop": LOOPS["hurt"], "animation_length": dur, "bones": b}


def death(m):
    """A asa CEDE. 1.3s, hold_on_last_frame.

    A ordem importa mais que as poses: ultimo bater (0.15) -> a asa perde forca e
    abre sozinha (0.4) -> o corpo tomba girando (0.75) -> chega no chao (1.05) ->
    assenta (1.3). A asa termina MEIO aberta, nao dobrada e nao esticada: asa
    dobrada le como ave dormindo, asa esticada le como ave planando.

    A queda NAO e numero de gosto: sai do geo, em `queda_da_morte`. Escrita a
    mao, ela sobreviveria a proxima correcao do modelo e a ave morreria boiando
    (ou enterrada) sem um unico erro no log.

    PONTO CEGO DECLARADO: como o `hurt`, ninguem pede este clipe hoje.
    """
    dur = DUR_DEATH
    queda = m["queda"]
    b = {}
    dobra_y, dobra_z = m["dobra"]
    for asa in ASAS:
        lado = 1.0 if asa.endswith("left") else -1.0
        curva(b, asa, "rotation", [
            (0.0, vetor()),
            (0.15, vetor(z=20.0 * lado)),          # o ultimo bater
            (0.4, vetor(x=6.0, z=-14.0 * lado)),   # a forca vai embora
            (0.8, vetor(x=10.0, z=-26.0 * lado)),
            (dur, vetor(x=12.0, z=-30.0 * lado))])
        curva(b, PONTAS[asa], "rotation", [
            (0.0, vetor()),
            (0.4, vetor(y=dobra_y * ABERTA_MORTE * SENTIDO_DE_Y * lado,
                        z=dobra_z * ABERTA_MORTE * lado)),
            (dur, vetor(y=dobra_y * ABERTA_MORTE * SENTIDO_DE_Y * lado,
                        z=(dobra_z * ABERTA_MORTE - 10.0) * lado))])
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.15, vetor(x=12.0)), (0.4, vetor(x=-6.0, z=16.0)),
        (0.75, vetor(x=-18.0, z=44.0)), (1.05, vetor(x=-24.0, z=66.0)),
        (dur, vetor(x=-26.0, z=74.0))])
    curva(b, "body", "position", [
        (0.0, vetor()), (0.15, vetor(y=0.6)), (0.4, vetor(y=-queda * 0.3)),
        (0.75, vetor(x=1.0, y=-queda * 0.72)), (1.05, vetor(x=1.8, y=-queda * 0.95)),
        (dur, vetor(x=2.2, y=-queda))])
    # O pescoco vai ficando solto: a cabeca e a ultima coisa que para.
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.3, vetor(x=18.0)), (0.8, vetor(x=-12.0, z=-16.0)),
        (dur, vetor(x=-20.0, z=-26.0))])
    curva(b, "beak", "rotation", [
        (0.0, vetor()), (0.5, vetor(x=-8.0)), (0.95, vetor(x=-20.0)),
        (dur, vetor(x=-28.0))])
    curva(b, "tail", "rotation", [
        (0.0, vetor()), (0.35, vetor(x=-20.0)), (0.9, vetor(x=6.0)),
        (dur, vetor(x=14.0))])
    # As pernas ENCOLHEM -- e o gesto de aranha, e e o ultimo que ela faz.
    for perna in PERNAS:
        frente = "front" in perna
        alvo = 62.0 if frente else 54.0
        lado = 1.0 if perna.endswith("left") else -1.0
        membro(b, perna, [
            (0.0, vetor()), (0.25, vetor(x=alvo * 0.4, z=3.0 * lado)),
            (0.7, vetor(x=alvo * 0.85, z=6.0 * lado)),
            (dur, vetor(x=alvo, z=7.0 * lado))])
    curva(b, "spinner", "rotation", [
        (0.0, vetor()), (0.35, vetor(x=20.0)), (0.9, vetor(x=8.0)),
        (dur, vetor(x=4.0))])
    return {"loop": LOOPS["death"], "animation_length": dur, "bones": b}


# ------------------------------------------------------- leitura do geo


def carregar_geo():
    caminho = os.path.join(DIR_GEO, MOB + ".geo.json")
    if not os.path.exists(caminho):
        raise SystemExit(
            "%s nao existe. Este gerador LE o geo: a abertura da asa, o"
            " recolhimento das pernas e a queda da morte saem de la, e os eixos"
            " sao conferidos contra ele. Sem geo nao ha o que animar." % caminho)
    return json.load(open(caminho, encoding="utf-8"))["minecraft:geometry"][0]


def osso(geometria, nome):
    for b in geometria["bones"]:
        if b["name"] == nome:
            return b
    raise SystemExit("o geo nao tem o osso '%s'" % nome)


def caixas(geometria, nome):
    return osso(geometria, nome).get("cubes", [])


def pivot(geometria, nome):
    b = osso(geometria, nome)
    if "pivot" not in b:
        raise SystemExit("o osso '%s' nao declara pivot; toda rotacao daqui gira"
                         " em torno dele" % nome)
    return b["pivot"]


def centro(geometria, nome, eixo):
    cubos = caixas(geometria, nome)
    if not cubos:
        raise SystemExit("o osso '%s' nao tem cubo; nao da para ler o eixo" % nome)
    return sum(c["origin"][eixo] + c["size"][eixo] / 2.0 for c in cubos) / len(cubos)


def faixa(geometria, nomes, eixo):
    valores = []
    for nome in nomes:
        for c in caixas(geometria, nome):
            valores += [c["origin"][eixo], c["origin"][eixo] + c["size"][eixo]]
    if not valores:
        raise SystemExit("nenhum cubo em %s -- nao da para medir" % list(nomes))
    return min(valores), max(valores)


def cantos(geometria, nome):
    """Os 8 vertices de cada cubo do osso, em coordenadas do modelo."""
    pontos = []
    for c in caixas(geometria, nome):
        ox, oy, oz = c["origin"]
        sx, sy, sz = c["size"]
        for dx in (0.0, sx):
            for dy in (0.0, sy):
                for dz in (0.0, sz):
                    pontos.append((ox + dx, oy + dy, oz + dz))
    return pontos


def filhos(geometria, nome):
    return [b["name"] for b in geometria["bones"] if b.get("parent") == nome]


def subarvore_tem_volume(geometria, nome):
    if caixas(geometria, nome):
        return True
    return any(subarvore_tem_volume(geometria, f) for f in filhos(geometria, nome))


# -------------------------------------------------- a conta da rotacao


def girar(p, graus):
    """Aplica (rx, ry, rz) na ORDEM QUE O GECKOLIB APLICA: X, depois Y, depois Z.

    A ordem nao e detalhe. `RenderUtil.translateAndRotateMatrixForBone` empilha
    Z, depois Y, depois X -- e empilhar nessa ordem aplica ao PONTO na ordem
    inversa. Conferir a envergadura com a ordem trocada daria um numero plausivel
    e errado, que e o pior resultado possivel para uma regua.
    """
    rx, ry, rz = (math.radians(g) for g in graus)
    x, y, z = p
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    z, x = z * math.cos(ry) - x * math.sin(ry), z * math.sin(ry) + x * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return (x, y, z)


def em_volta(p, centro_da_rotacao, graus):
    d = [a - b for a, b in zip(p, centro_da_rotacao)]
    r = girar(d, graus)
    return tuple(a + b for a, b in zip(r, centro_da_rotacao))


def angulos_que_esticam(geometria, nome, pivot_do_osso):
    """(graus_y, graus_z) que poem a massa do osso sobre o eixo +X. EXATO.

    Com a ordem do GeckoLib e rx=0, o ponto sofre Ry e depois Rz. Entao:

        Ry(ty) leva (dx, dy, dz) para (r, dy, 0), com r = hypot(dx, dz) e
                ty = atan2(dz, dx)   -- e isso zera o Z exatamente;
        Rz(tz) leva (r, dy, 0)      para (h, 0, 0), com tz = -atan2(dy, r).

    Nao e aproximacao e nao e chute: sao duas equacoes com solucao fechada. E
    por isso que a abertura da asa pode ser LIDA do geo em vez de decorada -- e
    um angulo decorado e exatamente o que sobreviveria a proxima correcao do
    modelo abrindo a asa pela metade, sem erro nenhum.
    """
    massa = [centro(geometria, nome, e) for e in (0, 1, 2)]
    dx, dy, dz = (m - p for m, p in zip(massa, pivot_do_osso))
    ty = math.degrees(math.atan2(dz, dx))
    tz = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    return ty, tz


# -------------------------------------------------- numeros LIDOS do geo


def dobra_da_ponta(geometria):
    """Os dois angulos que ABREM a asa -- lidos da asa ESQUERDA do geo.

    Medidos na PONTA e so nela, porque e la que o contrato pos o pivot da
    juncao. O braco e conferido a parte (`conferir_que_o_braco_aponta_para_fora`):
    se a dobra estiver nele, esta conta abriria so metade da asa.

    O lado direito NAO e medido: ele e o espelho, e `conferir_a_simetria_das_asas`
    exige que o geo cumpra isso. Medir os dois e deixar cada um com o seu angulo
    esconderia uma assimetria de modelagem dentro de uma animacao "correta".
    """
    return angulos_que_esticam(geometria, "wing_tip_left",
                               pivot(geometria, "wing_tip_left"))


def envergadura(geometria, abertura, batida=0.0, braco=(0.0, 0.0, 0.0)):
    """Envergadura em px com a asa em um estado dado. Mede, nao estima.

    Toda a leitura deste mob depende deste numero, entao ele e calculado
    aplicando as MESMAS rotacoes que o clipe escreve, nos vertices de verdade,
    na ordem de verdade -- e nao por trigonometria de guardanapo sobre o
    comprimento da asa.
    """
    dobra_y, dobra_z = dobra_da_ponta(geometria)
    braco_x, braco_y, braco_z = braco
    rot_braco = (braco_x, braco_y * SENTIDO_DE_Y, braco_z + batida)
    rot_ponta = (0.0, dobra_y * abertura * SENTIDO_DE_Y, dobra_z * abertura)
    p_braco = pivot(geometria, "wing_left")
    p_ponta = pivot(geometria, "wing_tip_left")

    maior = 0.0
    for ponto in cantos(geometria, "wing_left"):
        maior = max(maior, abs(em_volta(ponto, p_braco, rot_braco)[0]))
    for ponto in cantos(geometria, "wing_tip_left"):
        # A ponta herda o braco: primeiro a rotacao local dela, depois a do pai.
        movido = em_volta(ponto, p_ponta, rot_ponta)
        maior = max(maior, abs(em_volta(movido, p_braco, rot_braco)[0]))
    return maior * 2.0


ASA_NO_MERGULHO = (10.0, -45.0, -38.0)
"""Rotacao do BRACO no mergulho (x, y, z), uma vez so.

Ela e usada pelo clipe E pelas duas reguas que medem o mergulho. Repetida em
cada lugar, uma das copias sobreviveria a proxima correcao e a regua passaria a
medir uma asa que o clipe nao desenha -- um verde que nao prova nada.

O Y GRANDE NAO E EXAGERO, E FOI O PORTAO QUE O EXIGIU. A primeira versao deste
mergulho so BAIXAVA a asa (z=-30, y=-16), que e o gesto obvio de "fechar a asa".
`conferir_a_escada_de_silhueta` reprovou: baixar uma asa que sai reta do corpo
encurta a envergadura pelo cosseno do angulo, e o proprio deslocamento vertical
do braco devolve quase tudo -- a ave mergulhava com 19,1 px de envergadura
contra 19,0 px parada, ou seja, MAIOR. Na tela isso seria uma ave que se larga
sem nunca recolher a asa, e a pose que deveria dizer "a janela fechou" diria a
mesma coisa que a pose de repouso.

O que encurta de verdade e VARRER PARA TRAS (Y), nao baixar (Z). Os tres numeros
existem juntos: o Z cola a asa ao corpo, o Y a recolhe, e o X inclina o bordo.
"""


def escada_de_silhueta(geometria):
    """A envergadura de cada estado que ENSINA alguma coisa, em px, em ordem.

    `death` fica de FORA de proposito: nada esta sendo ensinado a um jogador que
    ja matou a ave, e prender a asa da morte a uma ordem faria um portao sobre
    uma coisa que nao tem consequencia nenhuma.
    """
    return (
        ("mergulho", envergadura(geometria, ABERTA_MERGULHO, braco=ASA_NO_MERGULHO)),
        ("pousada", envergadura(geometria, ABERTA_POUSADA)),
        ("cruzeiro", envergadura(geometria, ABERTA_CRUZEIRO)),
        ("aviso", envergadura(geometria, ABERTA_AVISO)),
    )


def recolhimento_das_pernas(geometria):
    """Quanto a perna dobra para tras em voo -- LIDO do geo.

    A regra: recolhida, a ponta da garra nao pode ficar abaixo da barriga. Uma
    perna pendurada em voo aumenta a silhueta vertical exatamente onde a hitbox
    nao aumenta -- e neste mob, que ensina distancia, silhueta que nao
    corresponde a caixa e o erro que a entrega inteira existe para corrigir.

    Decorar -95 aqui funcionaria hoje e atravessaria a garra pela barriga no dia
    em que a perna ficasse mais curta. PONTO CEGO DECLARADO: a conta trata a
    perna como um segmento reto do pivot ate o ponto mais baixo, e ignora a
    contra-rotacao da garra; ela e um limite util, nao uma prova por quadro.
    """
    barriga, _ = faixa(geometria, ("body",), 1)
    angulos = []
    for perna in PERNAS:
        py = pivot(geometria, perna)[1]
        baixo, _ = faixa(geometria, (perna, GARRA[perna]), 1)
        comprimento = py - baixo
        if comprimento <= 0.0:
            raise SystemExit(
                "a perna '%s' nao desce abaixo do proprio pivot (pivot y=%.1f,"
                " ponto mais baixo y=%.1f). Este gerador supoe perna PENDURADA:"
                " sem isso, 'recolher' gira para o lado errado." % (perna, py, baixo))
        alvo = max(-1.0, min(1.0, (py - barriga) / comprimento))
        angulos.append(-(math.degrees(math.acos(alvo)) + 12.0))
    # A MAIS COMPRIDA manda: um angulo por perna deixaria as quatro recolhidas em
    # alturas diferentes, que le como bicho desmontado.
    recolhe = min(angulos)
    if not -150.0 <= recolhe <= -55.0:
        raise SystemExit(
            "o recolhimento calculado deu %.1f graus, fora de [-150, -55]. Abaixo"
            " de 55 a perna continua pendurada em voo; alem de 150 ela dobra para"
            " dentro do proprio corpo." % recolhe)
    return recolhe


def queda_da_morte(geometria):
    """Quanto o body desce ao tombar -- LIDO do geo.

    Deitado de lado, o que fica na vertical e a LARGURA do torso; entao o pivot
    do corpo termina a meia largura do chao, e a queda e a diferenca.

    PONTO CEGO DECLARADO: a conta ignora o rolamento em Z que acontece junto, e
    por isso e aproximacao. Ela impede o erro grosso (corpo boiando ou enterrado),
    nao garante que cada quadro encoste certo.
    """
    altura = pivot(geometria, "body")[1]
    menor_x, maior_x = faixa(geometria, ("body",), 0)
    deitado = (maior_x - menor_x) / 2.0
    queda = altura - deitado
    if queda < 2.0:
        raise SystemExit(
            "o pivot do corpo esta a %.1f px e o torso deitado ocupa %.1f px de"
            " meia largura: a queda daria %.1f px. Abaixo de 2 px ninguem ve a ave"
            " cair -- ela so muda de angulo." % (altura, deitado, queda))
    if queda > altura:
        raise SystemExit(
            "a queda calculada (%.1f px) passa da altura do pivot (%.1f px): o"
            " corpo terminaria abaixo do chao." % (queda, altura))
    return queda


# ------------------------------------------------------------------ portoes


def conferir_eixos(geometria):
    """Os eixos se LEEM do geo. Decorados, produzem uma ave ao contrario."""
    if pivot(geometria, "head")[1] < pivot(geometria, "body")[1]:
        raise SystemExit(
            "a cabeca esta abaixo do corpo (head y=%.1f, body y=%.1f). Todo este"
            " gerador supoe +Y = CIMA."
            % (pivot(geometria, "head")[1], pivot(geometria, "body")[1]))
    # A FRENTE SE DESCOBRE COM O BICO E A CAUDA, que e como o contrato manda.
    bico = centro(geometria, "beak", 2)
    cauda = centro(geometria, "tail", 2)
    if not bico < cauda:
        raise SystemExit(
            "o bico esta em z=%.1f e a cauda em z=%.1f: a frente nao e -Z. Com o"
            " eixo invertido, o mergulho aponta a ave para CIMA e o aviso projeta"
            " a cabeca para tras -- tudo sem um unico erro no log." % (bico, cauda))
    if centro(geometria, "beak", 2) >= pivot(geometria, "beak")[2]:
        raise SystemExit(
            "o bico nao se estende a frente da propria dobradica (massa em z=%.1f,"
            " pivot em z=%.1f). Assim 'abrir o bico' (X negativo) FECHA o bico, e"
            " isso so aparece para quem ver a ave gritando."
            % (centro(geometria, "beak", 2), pivot(geometria, "beak")[2]))
    if centro(geometria, "wing_left", 0) <= 0:
        raise SystemExit(
            "wing_left esta em x=%.1f, e o contrato supoe +X = ESQUERDA. Com o"
            " sinal trocado as asas se CRUZAM ao abrir."
            % centro(geometria, "wing_left", 0))
    if centro(geometria, "spinner", 1) > centro(geometria, "body", 1):
        raise SystemExit(
            "a fiandeira esta ACIMA do centro do corpo (y=%.1f contra %.1f). Ela e"
            " a fiandeira do abdome: modelada em cima, o flare do aviso a joga por"
            " cima do dorso." % (centro(geometria, "spinner", 1),
                                 centro(geometria, "body", 1)))


def conferir_a_hierarquia(geometria):
    """O geo tem de ter A ARVORE do contrato, e nao so os nomes dele.

    O portao generico do Java confere que todo `parent` existe. Ele NAO confere
    que a ponta e filha do braco -- e e essa heranca que a conta da abertura
    desconta. Pendurada no `body`, a ponta receberia a rotacao de abertura sem
    receber a do braco, e a asa abriria torta. Em silencio.
    """
    real = {b["name"]: b.get("parent") for b in geometria["bones"]}
    if real != PAI:
        diferencas = sorted(k for k in set(real) | set(PAI)
                            if real.get(k, "<ausente>") != PAI.get(k, "<ausente>"))
        raise SystemExit(
            "geo e contrato discordam da arvore de ossos em %s. No geo: %s. No"
            " contrato: %s."
            % (diferencas, {k: real.get(k, "<ausente>") for k in diferencas},
               {k: PAI.get(k, "<ausente>") for k in diferencas}))


def conferir_a_simetria_das_asas(geometria):
    """A animacao espelha; o geo tem de ser espelhado.

    Se o geo tiver a asa direita com outro comprimento ou outro angulo, espelhar
    a animacao produz uma ave que abre uma asa mais que a outra -- e o relato de
    bug vira "a animacao esta torta", que e o lugar errado para procurar.
    """
    for esquerda, direita in (("wing_left", "wing_right"),
                              ("wing_tip_left", "wing_tip_right")):
        e = [centro(geometria, esquerda, i) for i in range(3)]
        d = [centro(geometria, direita, i) for i in range(3)]
        if abs(e[0] + d[0]) > 0.51 or abs(e[1] - d[1]) > 0.51 or abs(e[2] - d[2]) > 0.51:
            raise SystemExit(
                "'%s' esta em %s e '%s' em %s: as duas asas nao sao espelhos. A"
                " animacao escreve esquerda +v e direita -v; com o geo assimetrico"
                " isso abre uma asa mais que a outra." % (esquerda, e, direita, d))


def conferir_que_o_braco_aponta_para_fora(geometria):
    """A DOBRA MORA NA JUNCAO, que e onde o contrato pos o pivot.

    Se o braco tambem nascer dobrado, a conta exata de `angulos_que_esticam`
    deixa de bastar: esticar a asa passaria a exigir compor a rotacao do braco
    com a da ponta, e a abertura desta lane abriria so um pedaco -- sem erro.
    Quem reprovar aqui NAO deve mexer neste arquivo sozinho: as duas lanes
    conversam, porque o remendo de um lado quebra o do outro.
    """
    for asa in ASAS:
        ty, tz = angulos_que_esticam(geometria, asa, pivot(geometria, asa))
        # O braco esquerdo aponta para +X e o direito para -X; medir o direito
        # contra +X daria ~180 graus de "dobra" que nao existe.
        if asa.endswith("right"):
            ty, tz = -ty, -tz
            ty = ty - 180.0 if ty > 0 else ty + 180.0
        fora = math.hypot(ty, tz)
        if fora > DOBRA_MAXIMA_DO_BRACO:
            raise SystemExit(
                "'%s' esta a %.1f graus do eixo lateral (y=%.1f, z=%.1f), acima do"
                " limite de %.1f. A abertura desta lane so desdobra a JUNCAO"
                " (wing_tip_*): com o braco dobrado tambem, a asa 'aberta' fica"
                " aberta pela metade e nada acusa."
                % (asa, fora, ty, tz, DOBRA_MAXIMA_DO_BRACO))


def conferir_que_abrir_a_asa_muda_a_silhueta(geometria):
    """O PORTAO MAIS UTIL DESTE ARQUIVO.

    A entrega inteira se apoia numa frase: a asa nasce dobrada e quem abre e o
    aviso. Se a asa aberta e a asa dobrada tiverem quase a mesma envergadura, essa
    frase fica verdadeira no texto e falsa na tela -- e todos os outros portoes
    continuam verdes, porque cada um deles confere uma coisa que continua certa.

    Ele morde dos DOIS lados de proposito: envergadura demais em repouso e o bug
    que o corpo emprestado tinha (a silhueta mentindo sobre o alcance o tempo
    todo); ganho de menos na abertura e o aviso deixando de avisar.
    """
    dobrada = envergadura(geometria, ABERTA_POUSADA)
    aberta = envergadura(geometria, ABERTA_AVISO)
    if dobrada <= 0.0:
        raise SystemExit("a asa dobrada mede 0 px de envergadura.")
    ganho = (aberta - dobrada) / dobrada
    if ganho < GANHO_MINIMO_DA_ABERTURA:
        raise SystemExit(
            "a asa dobrada mede %.1f px e aberta %.1f px: %.0f%% de ganho, e o"
            " minimo e %.0f%%. Uma asa que 'abre' tao pouco nao e um aviso: o"
            " jogador nao tem como ver a diferenca entre a ave em repouso e a ave"
            " mandando ele sair, e a unica resposta que este mob ensina deixa de"
            " ser ensinavel." % (dobrada, aberta, ganho * 100.0,
                                 GANHO_MINIMO_DA_ABERTURA * 100.0))
    if dobrada > HITBOX_LARGURA_PX * 1.6:
        raise SystemExit(
            "a asa DOBRADA ja mede %.1f px de envergadura sobre uma hitbox de"
            " %.1f px (%.2fx). O contrato pede no maximo 1.6x em repouso: acima"
            " disso a silhueta volta a mentir sobre o alcance o tempo todo, que e"
            " exatamente o bug do corpo emprestado."
            % (dobrada, HITBOX_LARGURA_PX, dobrada / HITBOX_LARGURA_PX))


def conferir_a_escada_de_silhueta(geometria):
    """A ordem dos quatro estados, medida em px, com a margem de cada degrau.

    Cada degrau tem uma consequencia propria, e por isso cada um tem a sua
    mensagem: reprovar com "a escada esta errada" mandaria a proxima pessoa
    procurar em quatro lugares de uma vez.
    """
    largura = dict(escada_de_silhueta(geometria))

    if largura["mergulho"] > largura["pousada"] * (1.0 - MARGEM_DO_RECOLHIMENTO):
        raise SystemExit(
            "no mergulho a ave mede %.1f px e parada mede %.1f px: ela nao"
            " RECOLHE ao se largar. Asa aberta e o sinal de que ainda da para"
            " recuar; mostra-lo na descida diz 'ainda da tempo' no momento exato"
            " em que nao da mais, e pune quem obedeceu a pista certa."
            % (largura["mergulho"], largura["pousada"]))

    ganho = largura["cruzeiro"] / largura["pousada"] - 1.0
    if ganho < MARGEM_DA_ASA_ABERTA:
        raise SystemExit(
            "pousada a ave mede %.1f px e voando %.1f px: so %.0f%% a mais, e o"
            " minimo e %.0f%%. Bicho que voa abre a asa -- com os dois parecidos,"
            " a asa deixa de significar voo e o pouso deixa de ser a silhueta"
            " pequena contra a qual o aviso vai ser lido."
            % (largura["pousada"], largura["cruzeiro"], ganho * 100.0,
               MARGEM_DA_ASA_ABERTA * 100.0))

    # O ULTIMO DEGRAU NAO TEM MARGEM, E ISSO E DECLARADO. Ver o bloco "onde a
    # silhueta separa" la em cima: nesta geometria o aviso so alarga ~2% sobre o
    # cruzeiro, e exigir mais aqui forcaria uma ave voando de asa dobrada. O que
    # separa voar de avisar esta em `conferir_o_aviso_e_amplo_e_lento`; aqui so
    # se exige que o aviso nao seja MENOR, que seria a contradicao direta.
    if largura["aviso"] < largura["cruzeiro"]:
        raise SystemExit(
            "avisando a ave mede %.1f px e voando %.1f px: o aviso e a MENOR das"
            " duas silhuetas. 'As asas abrem ate a envergadura maxima' e a frase"
            " que esta entrega existe para cumprir."
            % (largura["aviso"], largura["cruzeiro"]))


def conferir_o_aviso_e_amplo_e_lento(animacoes):
    """AMPLO E LENTO -- E ESTE E O PORTAO QUE CARREGA A LEITURA DO MOB.

    A escada de silhueta prova que a ave AVISANDO nao se parece com a ave
    POUSADA. Ela nao prova -- e nesta geometria nao pode provar -- que a ave
    avisando nao se parece com a ave VOANDO: sao 2% de envergadura de diferenca.
    Quem separa esses dois sao a amplitude e o andamento, e e aqui que eles sao
    exigidos com margem.

      (a) o aviso bate mais LARGO que o cruzeiro, por RAZAO_MINIMA_DE_AMPLITUDE;
      (b) o aviso bate mais DEVAGAR que o cruzeiro, por RAZAO_MINIMA_DE_LENTIDAO;
      (c) e ele e o extremo dos dois lados contra TODOS os outros clipes.

    (b) e o mais facil de perder e o mais caro. 'Mais ameacador' e o instinto
    errado para um mob cuja ameaca e um pedido de recuo: acelerar a batida do
    aviso faz o clipe ler como investida comecando, e o jogador aprende a fugir
    tarde ou a lutar cedo. Nenhum gametest ve isso, porque nenhum gametest ve a
    tela.
    """
    largura = BATIDA_DE_AVISO / BATIDA_DE_CRUZEIRO
    if largura < RAZAO_MINIMA_DE_AMPLITUDE:
        raise SystemExit(
            "o aviso bate %.0f graus e o cruzeiro %.0f: %.2fx, e o minimo e %.2fx."
            " Com as duas batidas parecidas, quem ve a ave voar ja viu o aviso --"
            " e a silhueta nao vem socorrer, porque entre voar e avisar ela muda"
            " so 2%%." % (BATIDA_DE_AVISO, BATIDA_DE_CRUZEIRO, largura,
                          RAZAO_MINIMA_DE_AMPLITUDE))
    lentidao = CICLO_AVISO / CICLO_CRUZEIRO
    if lentidao < RAZAO_MINIMA_DE_LENTIDAO:
        raise SystemExit(
            "o aviso leva %.2fs por batida e o cruzeiro %.2fs: %.2fx, e o minimo e"
            " %.2fx. Amplo e RAPIDO le como ataque comecando: a ave estaria"
            " prometendo o bote durante a janela em que recuar ainda salva."
            % (CICLO_AVISO, CICLO_CRUZEIRO, lentidao, RAZAO_MINIMA_DE_LENTIDAO))
    if not BATIDA_DE_AVISO > max(BATIDA_DE_CRUZEIRO, BATIDA_DE_SUBIDA):
        raise SystemExit(
            "a batida do aviso (%.0f) nao e a mais larga (cruzeiro %.0f, subida"
            " %.0f)." % (BATIDA_DE_AVISO, BATIDA_DE_CRUZEIRO, BATIDA_DE_SUBIDA))
    if not CICLO_AVISO > max(CICLO_CRUZEIRO, CICLO_SUBIDA):
        raise SystemExit(
            "o ciclo do aviso (%.2fs) nao e o mais LENTO (cruzeiro %.2fs, subida"
            " %.2fs). A subida vem DEPOIS do aviso: se ela fosse mais lenta, o"
            " telegrafo desaceleraria bem quando a ameaca se tornou real."
            % (CICLO_AVISO, CICLO_CRUZEIRO, CICLO_SUBIDA))
    # E o clipe tem de conter um numero inteiro de batidas, senao o loop salta.
    voltas = animacoes["animation.%s.warn" % MOB]["animation_length"] / CICLO_AVISO
    if abs(voltas - round(voltas)) > 1e-6:
        raise SystemExit("o aviso tem %.3f batidas: o loop salta a cada volta"
                         % voltas)


def conferir_o_mergulho_e_o_mais_inclinado(animacoes):
    """A ave tem de estar mais apontada para baixo no bote do que em qualquer
    outro momento. E o unico clipe em que ela esta CAINDO."""
    fundos = {}
    for clipe in ("perch", "fly", "warn", "dive_windup", "dive", "recover"):
        corpo = animacoes["animation.%s.%s" % (MOB, clipe)]["bones"]["body"]
        fundos[clipe] = min(v[0] for v in corpo["rotation"].values())
    mergulho = fundos["dive"]
    piores = {k: v for k, v in fundos.items() if k != "dive" and v <= mergulho}
    if piores:
        raise SystemExit(
            "no mergulho o corpo chega a %.1f graus e estes clipes descem tanto ou"
            " mais: %s. O bote deixa de ser o momento mais inclinado da ave, e a"
            " pose que deveria significar 'ela se largou' passa a acontecer fora"
            " do bote." % (mergulho, piores))


def conferir_as_duracoes_do_servidor(animacoes):
    """As duracoes que o SERVIDOR manda nao podem divergir em silencio."""
    for clipe, esperado, origem in (
            ("warn", DUR_WARN, "NestGuardRules.ticksDeAviso=%d" % TICKS_DE_AVISO),
            ("dive_windup", DUR_WINDUP, "windupTicks=%d" % TICKS_WINDUP),
            ("dive", DUR_DIVE, "activeTicks=%d" % TICKS_ACTIVE),
            ("recover", DUR_RECOVER, "recoveryTicks=%d" % TICKS_RECOVERY)):
        real = animacoes["animation.%s.%s" % (MOB, clipe)]["animation_length"]
        if abs(real - esperado) > 1e-9:
            raise SystemExit(
                "%s dura %.3fs e o servidor da %.3fs (%s). Um telegrafo com o"
                " tempo errado ensina ao jogador um relogio que o jogo nao cumpre."
                % (clipe, real, esperado, origem))
    voltas = animacoes["animation.%s.fly" % MOB]["animation_length"] / CICLO_CRUZEIRO
    if abs(voltas - round(voltas)) > 1e-6:
        raise SystemExit("o voo tem %.3f batidas: o loop salta a cada volta" % voltas)


def conferir_os_loops_do_contrato(animacoes):
    """O campo `loop` E o comportamento, porque o codigo usa LoopType.DEFAULT.

    Ele nao e documentacao de uma decisao tomada no Java: ele e a decisao. Este
    portao existe para que mudar um deles exija mudar LOOPS -- onde cada linha
    tem o motivo escrito ao lado -- em vez de mudar um literal no meio do JSON.
    """
    for clipe in CLIPES:
        real = animacoes["animation.%s.%s" % (MOB, clipe)]["loop"]
        if real != LOOPS[clipe]:
            raise SystemExit("%s declara loop=%r e o contrato pede %r"
                             % (clipe, real, LOOPS[clipe]))
        if real not in (True, False, "hold_on_last_frame"):
            raise SystemExit("%s: loop invalido %r" % (clipe, real))


def conferir_clipes(animacoes):
    esperado = {"animation.%s.%s" % (MOB, c) for c in CLIPES}
    if set(animacoes) != esperado:
        raise SystemExit("chaves fora do contrato: %s"
                         % sorted(set(animacoes) ^ esperado))
    for nome, clipe in animacoes.items():
        fim = clipe["animation_length"]
        if not clipe["bones"]:
            raise SystemExit("%s nao move osso nenhum" % nome)
        for nome_osso, canais in clipe["bones"].items():
            for canal, quadros in canais.items():
                if canal not in CANAIS:
                    raise SystemExit("%s/%s: canal invalido %s"
                                     % (nome, nome_osso, canal))
                if not quadros:
                    raise SystemExit("%s/%s/%s: canal sem keyframe"
                                     % (nome, nome_osso, canal))
                for t, v in quadros.items():
                    if float(t) < 0 or float(t) > fim + 1e-9:
                        raise SystemExit("%s/%s: quadro %s fora de [0, %s]"
                                         % (nome, nome_osso, t, fim))
                    if canal == "scale" and min(v) <= 0:
                        raise SystemExit("%s/%s: escala %s some com o osso"
                                         % (nome, nome_osso, v))


def conferir_ossos(animacoes, geometria):
    """Osso errado nao da erro no GeckoLib: o membro so fica parado."""
    do_geo = {b["name"] for b in geometria["bones"]}
    if do_geo != set(OSSOS):
        raise SystemExit("geo e contrato discordam de osso: %s"
                         % sorted(do_geo ^ set(OSSOS)))
    for nome, clipe in animacoes.items():
        desconhecidos = sorted(set(clipe["bones"]) - set(OSSOS))
        if desconhecidos:
            raise SystemExit("%s move osso que nao existe: %s" % (nome, desconhecidos))
        proibidos = sorted(set(clipe["bones"]) & set(NAO_ANIMADOS))
        if proibidos:
            raise SystemExit(
                "%s anima %s. A raiz e a ancora que o renderer alinha com a"
                " hitbox: mexer nela desloca a silhueta inteira para fora da caixa"
                " de colisao, e num mob que ensina distancia isso e o bug que a"
                " entrega existe para corrigir." % (nome, proibidos))


def conferir_ossos_com_volume(animacoes, geometria):
    """Osso sem cubo na subarvore e osso que a animacao move e ninguem ve."""
    movidos = set()
    for clipe in animacoes.values():
        movidos |= set(clipe["bones"])
    vazios = sorted(n for n in movidos if not subarvore_tem_volume(geometria, n))
    if vazios:
        raise SystemExit(
            "estes ossos sao animados e nao tem cubo nenhum na subarvore: %s. O"
            " clipe roda, o osso gira e a tela nao muda." % vazios)


def conferir_o_loop_fecha(animacoes):
    """Clipe que repete tem de TERMINAR onde comecou, canal a canal.

    Um loop cujas pontas nao batem da um tranco a cada volta -- uma vez por
    segundo, para sempre. E o tipo de defeito que ninguem reporta porque ninguem
    consegue descrever: "tem alguma coisa estranha na ave".
    """
    for clipe in CLIPES:
        if LOOPS[clipe] is not True:
            continue
        animacao = animacoes["animation.%s.%s" % (MOB, clipe)]
        fim = animacao["animation_length"]
        inicio, final = quadro_em(animacao, 0.0), quadro_em(animacao, fim)
        for nome in ANIMAVEIS:
            for canal in CANAIS:
                if inicio[nome][canal] != final[nome][canal]:
                    raise SystemExit(
                        "%s repete, mas '%s'.%s vale %s no comeco e %s no fim. O"
                        " tranco acontece a cada %.2fs, para sempre."
                        % (clipe, nome, canal, inicio[nome][canal],
                           final[nome][canal], fim))


def costuras(animacoes):
    """A cadeia do episodio, em pares (de, t_de, para, t_para, por que)."""
    def clipe(nome):
        return animacoes["animation.%s.%s" % (MOB, nome)]

    return (
        (clipe("warn"), 0.0, clipe("dive_windup"), 0.0,
         "a ave decide mergulhar: ela ja esta de asa aberta e garra para fora"),
        (clipe("dive_windup"), DUR_WINDUP, clipe("dive"), 0.0,
         "ela se larga -- e a transicao de 4 ticks atravessa uma fase de 6"),
        (clipe("dive"), DUR_DIVE, clipe("recover"), 0.0,
         "a descida acaba e a freada comeca"),
        (clipe("recover"), DUR_RECOVER, clipe("fly"), 0.0,
         "ela volta ao cruzeiro"),
    )


def conferir_a_costura(animacoes):
    """A COSTURA E A FRONTEIRA MAIS PERIGOSA DESTA ENTREGA.

    O controller e UM SO, com 4 ticks de transicao, e a descida dura 6. Se as
    poses vizinhas forem iguais, a mistura vira um no-op e a troca acontece no
    quadro exato. Se nao forem, quase metade do bote e gasta interpolando -- e o
    jogador que levou a pancada nunca viu a ave fechar a asa.

    Compara o VALOR interpolado, osso a osso, canal a canal, com os ossos
    ausentes valendo o default do MODELO -- que e o que o GeckoLib faz de fato.
    """
    for antes, t_antes, depois, t_depois, motivo in costuras(animacoes):
        a, b = quadro_em(antes, t_antes), quadro_em(depois, t_depois)
        for nome in ANIMAVEIS:
            for canal in CANAIS:
                if a[nome][canal] != b[nome][canal]:
                    raise SystemExit(
                        "costura rompida (%s): '%s'.%s vale %s de um lado e %s do"
                        " outro. Osso que um clipe nao cita volta para o DEFAULT DO"
                        " MODELO, nao para a pose anterior."
                        % (motivo, nome, canal, a[nome][canal], b[nome][canal]))


def conferir_que_a_costura_reprova(animacoes):
    """ALIMENTA o portao com o erro que ele existe para pegar.

    Regua que nunca reprovou e carimbo. Aqui uma pose de costura e estragada de
    proposito -- um grau num osso -- e o portao TEM de recusar. Se um dia ele
    deixar passar, este teste quebra antes de alguem publicar uma ave que gasta
    metade do bote em mistura.
    """
    estragado = json.loads(json.dumps(animacoes))
    clipe = estragado["animation.%s.dive" % MOB]
    chave = tempo(0.0)
    original = clipe["bones"]["body"]["rotation"][chave]
    clipe["bones"]["body"]["rotation"][chave] = [original[0] + 1.0, 0, 0]
    try:
        conferir_a_costura(estragado)
    except SystemExit:
        return
    raise SystemExit(
        "a costura ACEITOU um mergulho deslocado de 1 grau. O portao nao esta"
        " protegendo nada: revise conferir_a_costura e quadro_em.")


def conferir_que_o_hurt_nao_toca_a_asa(animacoes):
    """O `hurt` deixar a asa de fora e DECISAO, e ela e invisivel no arquivo.

    Com um controller so, osso que o clipe corrente nao cita volta ao default do
    modelo -- que aqui e a asa DOBRADA. Um `hurt` com asa fecharia a asa de uma
    ave em voo por 5 ticks; se o tranco pegasse durante o aviso, fecharia
    exatamente o sinal que diz "voce ainda pode recuar", e a pancada seguinte
    puniria quem tinha lido certo.

    Sem este portao, a proxima pessoa abre o clipe, acha que faltou asa e
    "completa" -- destruindo o motivo sem nunca ve-lo escrito.
    """
    citados = set(animacoes["animation.%s.hurt" % MOB]["bones"])
    asas = citados & (set(ASAS) | set(PONTAS.values()) | set(PERNAS)
                      | set(GARRA.values()))
    if asas:
        raise SystemExit(
            "o hurt move %s. Ele e curto e pode vir a rodar num segundo"
            " controller; tocar asa ou perna ali fecha a asa de uma ave em voo"
            " por 5 ticks." % sorted(asas))


def conferir_que_o_lado_aracnideo_aparece(animacoes):
    """QUATRO PERNAS E UMA FIANDEIRA -- ou e so uma ave com ossos a mais.

    O lado aracnideo da ficha nao existe no comportamento (o servidor nao tem
    nada de aranha): ele existe SO na silhueta e no movimento. Se nenhum clipe
    mexer na fiandeira, ela e um cubo colado embaixo do bicho, e o mob some
    dentro da categoria "ave grande".
    """
    mexe = set()
    for nome, clipe in animacoes.items():
        for osso, canais in clipe["bones"].items():
            if any(v != NEUTRO[c]() for c, q in canais.items() for v in q.values()):
                mexe.add(osso)
    faltando = sorted((set(PERNAS) | {"spinner"}) - mexe)
    if faltando:
        raise SystemExit(
            "nenhum clipe mexe em %s. Sao eles que carregam o 'elementos"
            " aracnideos' da ficha -- parados, o mob e uma ave com ossos a mais."
            % faltando)
    aviso = animacoes["animation.%s.warn" % MOB]["bones"].get("spinner", {})
    if not any(v != vetor() for v in aviso.get("rotation", {}).values()):
        raise SystemExit(
            "a fiandeira nao trabalha no AVISO. O aviso e o unico momento em que o"
            " jogador olha a ave de perto por 1,5s: se a parte aracnidea nao"
            " aparecer ali, ela nao aparece em lugar nenhum.")


def conferir_que_so_transladam_os_declarados(animacoes):
    """Quem translada um osso novo tem de dizer isso em OSSOS_QUE_TRANSLADAM.

    E o fecho de `aplicar`: a pose de costura so escreve `position` neutro para
    os ossos dessa lista. Um clipe que passasse a transladar a cauda sem entrar
    na lista teria a primeira chave de posicao no meio do clipe -- e uma chave de
    posicao no meio vale TAMBEM antes dela, entao a cauda comecaria deslocada e a
    costura com o clipe vizinho quebraria sem nenhuma chave errada a vista.
    """
    fora = set()
    for nome, clipe in animacoes.items():
        for osso, canais in clipe["bones"].items():
            if "position" in canais and osso not in OSSOS_QUE_TRANSLADAM:
                fora.add("%s/%s" % (nome, osso))
    if fora:
        raise SystemExit(
            "estes clipes transladam osso fora de OSSOS_QUE_TRANSLADAM: %s."
            " Acrescente o osso aquela tupla -- e ai a pose de costura passa a"
            " escrever o zero de posicao dele." % sorted(fora))


def conferir_a_fiandeira_esta_quieta_no_pouso(animacoes):
    """No pouso a fiandeira e feita de AUSENCIA, e ausencia nao aparece num diff.

    O ninho de teia ja esta feito. Uma fiandeira trabalhando na pousada diria que
    a ave esta tecendo AGORA -- uma coisa que o servidor nao faz e que nenhum
    gametest desmente. E o contraste com o aviso, onde ela trabalha, e o que faz
    a fiandeira significar alguma coisa quando trabalha.
    """
    canais = animacoes["animation.%s.perch" % MOB]["bones"].get("spinner", {})
    inquietos = [(c, t, v) for c, q in canais.items() for t, v in q.items()
                 if v != NEUTRO[c]()]
    if inquietos:
        raise SystemExit(
            "a fiandeira se mexe na pousada: %s. Ela tem de ficar em zero ali --"
            " e o zero e escrito de proposito, para a decisao aparecer no arquivo."
            % inquietos)


# ------------------------------------------------------------- serializacao


def serializar(valor, recuo=0):
    """JSON com os vetores em UMA linha.

    json.dump(indent=2) quebra [0, -24, 0] em quatro linhas e o arquivo deixa de
    ser legivel num diff -- que e o unico lugar onde a outra pessoa vai conferir
    uma pose.
    """
    espaco = "  " * recuo
    if isinstance(valor, dict):
        if not valor:
            return "{}"
        itens = ",\n".join('%s  "%s": %s' % (espaco, chave, serializar(v, recuo + 1))
                           for chave, v in valor.items())
        return "{\n%s\n%s}" % (itens, espaco)
    if isinstance(valor, list):
        return "[%s]" % ", ".join(json.dumps(v) for v in valor)
    return json.dumps(valor)


def ordenar(clipe):
    """Keyframes em ordem de tempo: JSON nao garante ordem, diff humano exige."""
    for canais in clipe["bones"].values():
        for canal, quadros in list(canais.items()):
            canais[canal] = dict(sorted(quadros.items(), key=lambda kv: float(kv[0])))
    clipe["bones"] = dict(sorted(clipe["bones"].items(),
                                 key=lambda kv: ANIMAVEIS.index(kv[0])))
    return clipe


def escrever(animacoes):
    # SENTIDO DE Z. Este mob e um dos sete primeiros e nao passa por
    # `Animacoes.emitir`, onde a correcao mora para os demais -- mas a premissa
    # invertida era a MESMA, copiada de arquivo em arquivo. Chamar a funcao da
    # biblioteca em vez de repetir a negacao aqui e o que impede as duas copias
    # de divergirem no dia em que o sinal mudar.
    corrigir_sentido_de_z_em("spider_eagle", animacoes)
    destino = os.path.join(DIR_ANIM, MOB + ".animation.json")
    os.makedirs(DIR_ANIM, exist_ok=True)
    texto = serializar({"format_version": "1.8.0", "animations": animacoes})
    json.loads(texto)  # o que sai daqui tem de voltar como JSON, sempre.
    with open(destino, "w", encoding="utf-8", newline="\n") as saida:
        saida.write(texto + "\n")
    return destino


def tabela(animacoes):
    print("\n  %-34s %6s  %-19s %s" % ("clipe", "dur", "loop", "ossos"))
    for clipe in CLIPES:
        a = animacoes["animation.%s.%s" % (MOB, clipe)]
        print("  %-34s %5.2fs  %-19s %s"
              % ("animation.%s.%s" % (MOB, clipe), a["animation_length"],
                 str(a["loop"]), ", ".join(sorted(a["bones"]))))


# ------------------------------------------------------------------- main

def medir(geometria):
    """Tudo que este gerador LE do geo, num lugar so.

    Um dicionario, e nao seis parametros: estes quatro numeros atravessam quase
    todos os clipes, e uma assinatura de seis argumentos e onde alguem troca a
    ordem de dois floats sem que nada reclame.
    """
    return {
        "dobra": dobra_da_ponta(geometria),
        "recolhimento": recolhimento_das_pernas(geometria),
        "queda": queda_da_morte(geometria),
    }


def main():
    geometria = carregar_geo()

    # Os portoes de GEOMETRIA vem primeiro, antes de qualquer clipe existir: eles
    # conferem as premissas de que todas as contas abaixo dependem, e falhar aqui
    # produz uma mensagem sobre o MODELO em vez de uma sobre a animacao.
    conferir_a_hierarquia(geometria)
    conferir_eixos(geometria)
    conferir_a_simetria_das_asas(geometria)
    conferir_que_o_braco_aponta_para_fora(geometria)
    conferir_que_abrir_a_asa_muda_a_silhueta(geometria)
    conferir_a_escada_de_silhueta(geometria)

    m = medir(geometria)

    # A ORDEM DE CONSTRUCAO E A CADEIA DE COSTURAS. Os vizinhos leem o quadro um
    # do outro em vez de repetir a pose: e o que impede duas fontes para a mesma
    # verdade num lugar onde a divergencia custa um tranco de um quadro.
    clipe_fly = ordenar(fly(m))
    clipe_warn = ordenar(warn(m))
    clipe_dive = ordenar(dive(m))
    clipe_windup = ordenar(dive_windup(
        m, quadro_em(clipe_warn, 0.0), quadro_em(clipe_dive, 0.0)))
    clipe_recover = ordenar(recover(
        m, quadro_em(clipe_dive, DUR_DIVE), quadro_em(clipe_fly, 0.0)))

    animacoes = {
        "animation.%s.perch" % MOB: ordenar(perch(m)),
        "animation.%s.fly" % MOB: clipe_fly,
        "animation.%s.warn" % MOB: clipe_warn,
        "animation.%s.dive_windup" % MOB: clipe_windup,
        "animation.%s.dive" % MOB: clipe_dive,
        "animation.%s.recover" % MOB: clipe_recover,
        "animation.%s.hurt" % MOB: ordenar(hurt()),
        "animation.%s.death" % MOB: ordenar(death(m)),
    }

    conferir_clipes(animacoes)
    conferir_os_loops_do_contrato(animacoes)
    conferir_ossos(animacoes, geometria)
    conferir_ossos_com_volume(animacoes, geometria)
    conferir_que_so_transladam_os_declarados(animacoes)
    conferir_as_duracoes_do_servidor(animacoes)
    conferir_o_aviso_e_amplo_e_lento(animacoes)
    conferir_o_mergulho_e_o_mais_inclinado(animacoes)
    conferir_o_loop_fecha(animacoes)
    conferir_a_costura(animacoes)
    conferir_que_a_costura_reprova(animacoes)
    conferir_que_o_hurt_nao_toca_a_asa(animacoes)
    conferir_que_o_lado_aracnideo_aparece(animacoes)
    conferir_a_fiandeira_esta_quieta_no_pouso(animacoes)

    print("escrito", escrever(animacoes))
    print("\nnumeros LIDOS do geo:")
    print("  abertura da ponta      : y=%.1f  z=%.1f graus" % m["dobra"])
    print("  recolhimento da perna  : %.1f graus" % m["recolhimento"])
    print("  queda da morte         : %.2f px" % m["queda"])
    # A ESCADA E IMPRESSA PARA SER OLHADA. Os portoes exigem a ordem e as margens
    # minimas; nenhum deles prova que o jogador distingue os degraus na tela. Essa
    # parte segue humana -- ver o relato e docs/testing/o-que-nao-provamos.md.
    escada = escada_de_silhueta(geometria)
    print("\nenvergadura por estado (hitbox = %.1f px de largura):"
          % HITBOX_LARGURA_PX)
    anterior = None
    for rotulo, valor in escada:
        delta = "" if anterior is None else "  %+.0f%% sobre o anterior" % (
            (valor / anterior - 1.0) * 100.0)
        print("  %-9s %6.1f px   %.2fx a hitbox%s"
              % (rotulo, valor, valor / HITBOX_LARGURA_PX, delta))
        anterior = valor
    largura = dict(escada)
    print("  AVISO sobre POUSADA: %+.0f%%  <- e este o degrau que ensina"
          % ((largura["aviso"] / largura["pousada"] - 1.0) * 100.0))
    print("  morte (fora da escada): %.1f px"
          % envergadura(geometria, ABERTA_MORTE))
    print("\naviso contra cruzeiro, onde a leitura de verdade mora:")
    print("  amplitude : %.0f contra %.0f graus  (%.2fx)"
          % (BATIDA_DE_AVISO, BATIDA_DE_CRUZEIRO,
             BATIDA_DE_AVISO / BATIDA_DE_CRUZEIRO))
    print("  andamento : %.2f contra %.2f s/batida  (%.2fx mais lento)"
          % (CICLO_AVISO, CICLO_CRUZEIRO, CICLO_AVISO / CICLO_CRUZEIRO))
    tabela(animacoes)


if __name__ == "__main__":
    main()
