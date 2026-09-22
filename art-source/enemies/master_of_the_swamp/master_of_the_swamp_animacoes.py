"""Gera os SEIS clipes do master of the swamp (GeckoLib 4.8.3, Bedrock 1.8.0).

O QUE ESTE MOB E, E POR QUE ISSO MUDA A ANIMACAO
-------------------------------------------------
Ele nao e um inimigo que se mata: e um que se FISGA. O jogador lanca a vara, o
peixe morde, e comeca um cabo de guerra -- puxar demais arrebenta a linha,
acompanhar o peixe o cansa ate poder ser recolhido. Matar a pancada e possivel e
de proposito CHATO (60 de vida, armadura 4, e ele foge ao levar dano).

Consequencia direta, e ela e a entrega inteira desta lane:

    O JOGADOR TOMA A DECISAO DO JOGO OLHANDO PARA A ANIMACAO.

Enquanto a linha esta tensa, o servidor nao mostra numero nenhum. `thrash` e
`caught` sao a UNICA interface do cabo de guerra. Se `thrash` nao parecer custoso
de segurar, ninguem entende que puxar arrebenta. Se `caught` parecer morte,
ninguem tenta recolher -- e o premio do mob, que E a captura, nunca acontece.

E NENHUM PORTAO DESTE REPOSITORIO VE A TELA. Por isso os portoes daqui nao medem
"esta bonito": medem a ESCADA DE AGITACAO entre os clipes, em graus por segundo,
e mordem dos dois lados -- `caught` calmo demais vira peixe morto, agitado demais
vira peixe ainda lutando. Ver `conferir_a_escada_de_agitacao` e
`conferir_que_cansado_nao_le_como_morte`.

TICKS: COPIADOS DO SERVIDOR
----------------------------
12/4/16 saem de `HunterExamProfiles.masterOfTheSwampBite()`; os 200 ticks de
cansaco saem de `masterOfTheSwampFishing()` (RegrasDeFisgada.ticksParaCansar).
Se mudarem la, mudam aqui e regera. `conferir_as_duracoes_do_servidor` e
`conferir_que_o_debate_repete_na_luta` reprovam a divergencia.

Um detalhe que so um portao pega: a bocada tem de acontecer DENTRO da janela de 4
ticks. Uma boca que fecha na recuperacao entrega o dano antes de morder na tela,
e o jogador aprende que a mordida "pega do nada". Ver
`conferir_que_a_bocada_acontece_na_janela`.

EIXOS -- LIDOS do geo, nao decorados
--------------------------------------
Convencao vanilla: y=0 no chao, -Z na FRENTE, +X a ESQUERDA do bicho.
`conferir_eixos` le o geo e reprova se a CABECA nao estiver a frente e a CAUDA
atras -- que e exatamente como o contrato manda descobrir a frente deste mob.
Derivado dela, e so dela:

    rotacao X positiva  -> a cabeca SOBE (massa em -Z vai para +Y);
                           a CAUDA (massa em +Z) DESCE;
                           perna pendurada (massa em -Y) balanca para a FRENTE
    rotacao X negativa  -> a MANDIBULA ABRE (a massa dela esta a frente da
                           dobradica); a perna RECOLHE para tras
    rotacao Y positiva  -> a cabeca (massa em -Z) vai para -X = a DIREITA do
                           bicho; a cauda (massa em +Z) varre para +X = a
                           ESQUERDA. Ou seja: um yaw ja poe as duas pontas em
                           lados opostos, e e por isso que a `turn` AMPLIFICA a
                           cauda em vez de contra-gira-la
    rotacao Z positiva  -> o lado +X (esquerdo) SOBE: e o rolamento

O EIXO Y E DEDUZIDO DA MAO DOS EIXOS, NAO OBSERVADO -- mesma suposicao declarada
pela lane da spider eagle, e pelo mesmo motivo (regra da mao direita, ciclo
X:(Y,Z), Y:(Z,X), Z:(X,Y)). Se em jogo o peixe curvar para o lado errado,
`SENTIDO_DE_Y = -1.0` inverte tudo num lugar so.

O QUE ESTE GERADOR **NAO** LE DO GEO, E POR QUE
------------------------------------------------
Nenhum valor de pose sai de uma medida do geo. Isso e decisao, e e diferente do
gerador da spider eagle -- la a asa DOBRADA era uma afirmacao sobre COMPRIMENTO
("a envergadura em repouso mente sobre o alcance"), e comprimento se mede.

Aqui nao ha afirmacao de comprimento nenhuma: tudo que este mob faz e girar em
torno de juncoes que o contrato fixa, e o unico numero em pixels (a arrancada do
`escape`) e julgado contra a HITBOX, que e contrato e nao geo.

O que o geo ganha em troca e ser CONFERIDO em vez de copiado: o recolhimento das
pernas e um numero decidido aqui e conferido contra a geometria de la
(`conferir_que_as_pernas_recolhem_sem_atravessar_o_corpo`). Um valor derivado
ficaria silenciosamente certo e exigiria regerar a cada correcao do modelo; um
valor conferido fica RUIDOSAMENTE errado, que e o que se quer entre duas lanes
que nao se veem.

Regerar:  python art-source/enemies/master_of_the_swamp/master_of_the_swamp_animacoes.py
Exporta:  .../animations/entity/master_of_the_swamp.animation.json
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

MOB = "master_of_the_swamp"

# A HIERARQUIA e o contrato, e ela e a UNICA lista de ossos deste arquivo: o
# conjunto sai daqui derivado. Repetir os nomes criaria duas fontes para a mesma
# verdade, e a divergencia se manifestaria como um membro parado -- sem erro.
#
# `tail_fin` ser filho de `tail` e o que faz a onda do nado CRESCER para tras: a
# ponta herda o giro da cauda e soma o proprio. Pendurada no `body`, ela receberia
# so o proprio angulo, a onda morreria na juncao, e o portao generico do Java
# (que so confere que o pai existe) aprovaria.
PAI = {
    "root": None,
    "body": "root",
    "head": "body",
    "jaw": "head",
    "fin_left": "body",
    "fin_right": "body",
    "tail": "body",
    "tail_fin": "tail",
    "leg_front_left": "body",
    "claw_front_left": "leg_front_left",
    "leg_front_right": "body",
    "claw_front_right": "leg_front_right",
    "leg_back_left": "body",
    "claw_back_left": "leg_back_left",
    "leg_back_right": "body",
    "claw_back_right": "leg_back_right",
}
OSSOS = tuple(PAI)

# `root` NAO E ANIMADO, e isso e decisao. Ele e a ancora que o renderer alinha
# com a hitbox de 2.4x1.6; girar ou transladar a raiz move a silhueta inteira
# para fora da caixa de colisao. Num mob cuja mecanica e "a isca esta a 8 blocos
# e voce precisa saber onde o peixe esta", silhueta fora da caixa desloca a
# leitura de distancia do jogador sem deslocar nada no servidor.
NAO_ANIMADOS = ("root",)
ANIMAVEIS = tuple(o for o in OSSOS if o not in NAO_ANIMADOS)

PERNAS = tuple(o for o in ANIMAVEIS if o.startswith("leg_"))
GARRA = {p: next(o for o in ANIMAVEIS if PAI[o] == p) for p in PERNAS}
BARBATANAS = ("fin_left", "fin_right")

CLIPES = ("swim", "turn", "bite", "thrash", "caught", "escape")

# O TIPO DE REPETICAO MORA AQUI E SO AQUI. O codigo pede
# then(nome, Animation.LoopType.DEFAULT), e DEFAULT delega para este campo --
# entao este dicionario E o comportamento, nao a documentacao dele.
LOOPS = {
    # nadar e o repouso deste bicho: repete enquanto ele estiver na agua.
    "swim": True,
    # a curva dura o que o servidor quiser que dure; o clipe repete por baixo.
    "turn": True,
    # a bocada acontece UMA vez e termina no cruzeiro. Em loop, o peixe morderia
    # o vazio para sempre; com hold, ficaria de boca fechada e corpo travado.
    "bite": False,
    # o debate repete porque a luta dura 200 ticks e o clipe dura 25. E o que o
    # jogador olha enquanto decide se puxa ou acompanha.
    "thrash": True,
    # cansado repete: a janela de recolher tambem nao tem fim marcado, e um peixe
    # que PARASSE de respirar no fim do clipe viraria peixe morto na tela.
    "caught": True,
    # a fuga termina com o bicho estendido e ja longe. hold_on_last_frame porque
    # `false` devolveria o peixe a pose neutra do modelo no ultimo quadro -- uma
    # piscada de "ele voltou ao normal" no exato quadro em que ele some, e esse e
    # o ultimo que o jogador ve. Nao e loop, que e o que a ficha pede.
    "escape": "hold_on_last_frame",
}

# ------------------------------------------------------------------- ticks

TICKS_POR_SEGUNDO = 20.0

# Copiados do SERVIDOR: masterOfTheSwampBite() -> AttackDefinition("bite", 12, 4, 16, ...)
TICKS_WINDUP = 12
TICKS_ACTIVE = 4
TICKS_RECOVERY = 16

# Copiado do SERVIDOR: masterOfTheSwampFishing() -> RegrasDeFisgada(..., 200, ...)
TICKS_PARA_CANSAR = 200

DUR_WINDUP = TICKS_WINDUP / TICKS_POR_SEGUNDO                       # 0.60
DUR_ACTIVE = TICKS_ACTIVE / TICKS_POR_SEGUNDO                       # 0.20
DUR_BITE = (TICKS_WINDUP + TICKS_ACTIVE
            + TICKS_RECOVERY) / TICKS_POR_SEGUNDO                   # 1.60
DUR_LUTA = TICKS_PARA_CANSAR / TICKS_POR_SEGUNDO                    # 10.00

DUR_SWIM = 1.4
DUR_TURN = 1.0
DUR_THRASH = 1.25   # 8 voltas inteiras dentro dos 200 ticks de luta
DUR_CAUGHT = 3.0
DUR_ESCAPE = 0.8

# Periodos. O NADO E O MAIS LENTO DOS TRES QUE SE MEXEM, de proposito: um peixe
# de 2,4 blocos que bata a cauda depressa le como peixinho grande, nao como coisa
# pesada. `conferir_que_o_nado_e_pesado` reprova quem acelerar isso.
CICLO_NADO = DUR_SWIM
CICLO_CURVA = DUR_TURN

# -------------------------------------------------------------- a hitbox

# sized(2.4F, 1.6F), em pixels de modelo. Nao e botao de tuning: e a caixa que o
# servidor usa para tudo, e a unica regua em PIXEIS desta lane.
HITBOX_LARGURA_PX = 2.4 * 16.0   # 38.4
HITBOX_ALTURA_PX = 1.6 * 16.0    # 25.6

# Quanto de meia-caixa uma translacao de osso pode gastar. A arrancada do escape
# e a unica coisa que translada de verdade aqui, e ela existe para dar IMPULSO a
# leitura -- o deslocamento de verdade e a velocidade da entidade, que o servidor
# aplica. Um bone que ande meia caixa poe o modelo com metade do corpo fora da
# hitbox, e ai o peixe e atingivel onde ele nao esta.
FRACAO_MAXIMA_DA_CAIXA = 0.40

# --------------------------------------------------------------- os eixos

# Ver o cabecalho: o Y e deduzido da mao dos eixos, nao observado em jogo.
SENTIDO_DE_Y = 1.0

# ------------------------------------------------------- poses, em graus

# --- o cruzeiro (a pose-HUB: ver a cadeia de costuras em `costuras`) ---------

PERNA_RECOLHIDA = -72.0   # dobrada para tras, sob a barriga
GARRA_CONTRA = -0.45      # a garra CONTRA-GIRA a fracao da perna, para continuar
                          # apontada para baixo em vez de girar junto e apontar
                          # para o jogador. Escrita a mao, ela fica com a fase
                          # certa hoje e errada na primeira correcao da perna.
BARBATANA_CRUZEIRO = 14.0  # peitoral aberta e caida: e assim que um peixe plana

# --- a onda do nado ---------------------------------------------------------
# A ONDA CRESCE PARA TRAS, e essa e a leitura de "peso": num bicho enorme a
# cabeca fica estavel e quem trabalha e o terco final. Uma onda de amplitude
# igual do focinho a cauda le como enguia, nao como peixe grande.
ONDA_CORPO = 3.5
ONDA_CAUDA = 14.0
ONDA_PONTA = 22.0
ONDA_CABECA = -2.0   # contra-fase: a cabeca CORRIGE o rumo em vez de acompanhar

# O atraso que faz a onda VIAJAR. Sem ele os tres ossos giram juntos e a cauda
# vira uma tabua com dobradica -- defeito que nao aparece parado, so em
# movimento. Fracao do periodo.
ATRASO_DA_CAUDA = 0.14
ATRASO_DA_PONTA = 0.26

# --- a curva ----------------------------------------------------------------
ARCO_DA_CURVA = 20.0     # o corpo arqueia; e o que separa curva de nado
ROLAMENTO_DA_CURVA = 9.0  # ele deita para dentro da curva
VARRIDA_DA_CAUDA = 28.0  # a cauda AMPLIFICA o yaw do corpo: as duas pontas ja
                         # estao em lados opostos, e e a cauda jogada para FORA
                         # da curva que gera o momento. Contra-girar a cauda aqui
                         # produziria um peixe fazendo a curva de re.
VARRIDA_DA_PONTA = 20.0
OLHAR_NA_CURVA = 8.0     # a cabeca entra na curva antes do corpo
DOBRA_DA_INTERNA = 22.0  # a barbatana de DENTRO recolhe; a de fora abre

# --- a bocada ---------------------------------------------------------------
BOCA_FECHADA = 0.0
BOCA_ESCANCARADA = -58.0   # 12 ticks para chegar aqui: o aviso e a boca abrindo
BOCA_CERRADA = 2.0         # passa do fechado: e a bocada, nao um fechar de boca
RECUO_DA_BOCADA = 1.8      # px para +Z: ele PUXA para tras antes de dar o bote
AVANCO_DA_BOCADA = -3.2    # px para -Z: e o bote

# --- o debate (a luta) ------------------------------------------------------
# Base do bicho fisgado: rolado de lado, boca aberta, pernas espalhadas. E sobre
# ela que o ruido harmonico trabalha.
DEBATE = {
    "body": {"rotation": (-6.0, 22.0, 12.0), "position": (1.4, -0.3, 0.0)},
    "head": {"rotation": (-10.0, 14.0, -6.0)},
    "jaw": {"rotation": (-32.0, 0.0, 0.0)},
    "tail": {"rotation": (4.0, -20.0, 8.0)},
    "tail_fin": {"rotation": (0.0, -14.0, 0.0)},
    "fin_left": {"rotation": (-12.0, 0.0, -30.0)},
    "fin_right": {"rotation": (8.0, 0.0, -2.0)},
}
PERNA_NO_DEBATE_FRENTE = -24.0
PERNA_NO_DEBATE_TRAS = -52.0

# Amplitudes do debate, por osso e por eixo. Sao elas que fazem o clipe ser
# CUSTOSO DE SEGURAR -- e e a razao delas contra as do `caught` que o portao da
# escada guarda.
AGITO = {
    "body": {"rotation": (14.0, 30.0, 20.0), "position": (1.8, 1.0, 1.2)},
    "head": {"rotation": (16.0, 24.0, 12.0)},
    "jaw": {"rotation": (24.0, 0.0, 0.0)},
    "tail": {"rotation": (10.0, 48.0, 14.0)},
    "tail_fin": {"rotation": (8.0, 34.0, 10.0)},
    "fin_left": {"rotation": (14.0, 0.0, 20.0)},
    "fin_right": {"rotation": (14.0, 0.0, 20.0)},
}
AGITO_DA_PERNA = (20.0, 0.0, 8.0)

# --- cansado ----------------------------------------------------------------
# A POSE MAIS DELICADA DO ARQUIVO. Ela tem de ler como "agora da para pegar" e
# nao como "ele morreu": corpo mole e tombado, cauda caida, boca ENTREABERTA que
# continua respirando. Zero movimento aqui e um peixe morto, e um peixe morto nao
# convida ninguem a recolher a linha.
CANSADO = {
    "body": {"rotation": (4.0, 0.0, 9.0), "position": (0.0, -0.8, 0.0)},
    "head": {"rotation": (10.0, 6.0, -8.0)},
    "jaw": {"rotation": (-18.0, 0.0, 0.0)},
    "tail": {"rotation": (16.0, 6.0, 0.0)},
    "tail_fin": {"rotation": (10.0, 0.0, 0.0)},
    "fin_left": {"rotation": (6.0, 0.0, -14.0)},
    "fin_right": {"rotation": (6.0, 0.0, 10.0)},
}
PERNA_CANSADA_FRENTE = -34.0
PERNA_CANSADA_TRAS = -40.0

RESPIRO = {
    "body": {"rotation": (2.0, 2.5, 1.5), "position": (0.0, 0.5, 0.0)},
    "head": {"rotation": (2.5, 3.0, 0.0)},
    "jaw": {"rotation": (4.0, 0.0, 0.0)},
    "tail": {"rotation": (2.5, 5.0, 0.0)},
    "tail_fin": {"rotation": (0.0, 4.0, 0.0)},
    "fin_left": {"rotation": (0.0, 0.0, 3.0)},
    "fin_right": {"rotation": (0.0, 0.0, 3.0)},
}
RESPIRO_DA_PERNA = (3.0, 0.0, 0.0)

# --- a fuga -----------------------------------------------------------------
COILE_DA_FUGA = 34.0      # o corpo carrega para um lado antes de disparar
CHICOTE_DA_FUGA = 62.0    # e a cauda atravessa com tudo: UMA vez, nao um ciclo
ARRANCADA_PX = -7.0       # px para -Z ao longo dos 16 ticks
PERNA_NA_FUGA = -80.0     # coladas: o que passa pela agua nao tem aba aberta

# ------------------------------------------------ limites que os portoes usam

# A escada de agitacao, em graus por segundo. Cada razao guarda uma leitura:
RAZAO_DEBATE_SOBRE_NADO = 2.5     # lutar tem de ser visivelmente mais que nadar
RAZAO_DEBATE_SOBRE_CANSADO = 4.0  # e cansado, visivelmente MENOS que lutar
AGITACAO_MINIMA_DE_VIVO = 1.0     # abaixo disto, `caught` e um peixe morto
ABERTURA_MINIMA_CANSADA = 8.0     # a boca fica entreaberta o clipe inteiro
FOLGA_DA_BOCA_FECHADA = 6.0       # o que ainda conta como "boca fechada"
MINIMO_DE_VOLTAS_NA_LUTA = 4.0    # o debate tem de REPETIR durante os 200 ticks
RAZAO_DO_ARCO = 3.0               # quanto a curva arqueia mais que o nado
DOBRA_MINIMA_DA_PERNA = 45.0      # abaixo disto a perna nao recolheu, so inclinou
# Quanto da propria perna a ponta pode subir ACIMA da barriga ao recolher. Ela
# dobra ao longo do FLANCO, e nao pela linha do meio: encostar na altura da
# barriga e o que "sob o corpo" quer dizer. Passando de um quarto do comprimento,
# a ponta ja esta dentro do torso e a garra aparece saindo pela lateral.
TOLERANCIA_DE_ENCOSTE = 0.25

# Os ossos que CARREGAM a leitura de cada clipe. Sao eles que o portao do pedaco
# morto vigia: uma barbatana que pare por meio segundo ninguem nota, uma cauda
# que pare por meio segundo e um peixe que desistiu.
OSSOS_QUE_CARREGAM = ("body", "head", "jaw", "tail", "tail_fin")
# Quanto do percurso JUSTO (um quarto do total) cada quarto do clipe tem de
# carregar. Abaixo disso ha uma regiao em que o bicho praticamente para -- e num
# clipe que repete, ele para sempre na mesma hora.
FRACAO_MINIMA_DO_PERCURSO = 0.35

# Dois lados do "peixe ENORME": curto demais e a hitbox mente sobre o tamanho;
# comprido demais e a silhueta mente sobre o alcance.
COMPRIMENTO_MINIMO = 0.60
COMPRIMENTO_MAXIMO = 1.15


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

# O UNICO osso que algum clipe TRANSLADA. `aplicar` escreve `position` neutro so
# para ele: escrever zeros de posicao nos outros quatorze seria ruido num arquivo
# cujo unico leitor humano e o diff, e a costura ja trata canal ausente como
# neutro. Quem transladar um osso novo reprova em
# `conferir_que_so_translada_o_declarado` -- e ai acrescenta o nome AQUI, que e o
# que faz a pose de costura passar a incluir aquele canal.
OSSOS_QUE_TRANSLADAM = ("body",)


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
    afastamento viraria a garra para dentro da barriga. Os `pares` sao rotacoes
    ABSOLUTAS da perna -- a garra e uma fracao delas, nao do delta.
    """
    curva(bones, perna, "rotation", pares)
    curva(bones, GARRA[perna], "rotation",
          [(t, vetor(x=v[0] * GARRA_CONTRA)) for t, v in pares])


def ciclo(duracao, periodo, amplitude, base=0.0, fase=0.75, amostras=8):
    """Cossenoide amostrada. `fase=0.75` faz a curva VALER A BASE em t=0.

    Amostrar (em vez de escrever os extremos a mao) e o que mantem a fase certa
    quando alguem mexe no periodo, e o que mantem a interpolacao linear do
    formato 1.8.0 parecendo curva em vez de zigue-zague.
    """
    passo = periodo / amostras
    n = int(round(duracao / passo))
    return [(i * passo,
             base + amplitude * math.cos(2 * math.pi * (i * passo / periodo + fase)))
            for i in range(n + 1)]


# ------------------------------------------------------- o ruido que nao e ruido


def ruido(duracao, amplitude, semente, amostras=24, harmonicos=3,
          pesos=(0.36, 0.34, 0.30)):
    """Movimento IRREGULAR que mesmo assim FECHA o loop. Exatamente.

    A soma de tres cossenoides com 1, 2 e 3 periodos inteiros dentro da duracao
    vale o MESMO em t=0 e em t=duracao, qualquer que seja a fase -- entao o clipe
    pode ser caotico e ainda assim repetir sem tranco. (Ver `conferir_o_loop_fecha`,
    que confere isso com o valor interpolado e nao com a presenca de uma chave.)

    Por que nao escrever os quadros a mao: um debate escrito a mao fica com todos
    os ossos batendo juntos, porque e assim que uma pessoa digita uma lista de
    tempos. Peixe fisgado nao tem compasso. Aqui a SEMENTE desloca as fases de
    cada osso e de cada eixo, e o resultado e que nada bate junto -- e continua
    deterministico, que e o que permite um portao medir amplitude.

    Nao ha `random` de proposito: dois geradores tem de produzir o mesmo arquivo,
    ou o diff deixa de significar alguma coisa.

    OS PESOS QUASE IGUAIS SAO CORRECAO DE UM ERRO QUE JA ACONTECEU AQUI. A
    primeira versao usava (0.55, 0.30, 0.15) -- a queda que soa "natural" -- e com
    ela o primeiro harmonico dominava: a cauda dava UMA varrida de 60 graus e
    passava a segunda metade do clipe parada dentro de 6 graus. Na tela isso e um
    peixe que se debate e desiste, repetindo oito vezes, no unico clipe cuja
    unica funcao e parecer custoso de segurar. Nenhum portao de pose via nada --
    a amplitude total estava certa, o loop fechava, a costura fechava. Foi
    `conferir_que_nenhum_loop_tem_pedaco_morto` que encontrou, e e ele que impede
    a proxima pessoa de "suavizar" estes pesos de volta.

    As FASES sao espalhadas pela razao aurea (0.6180) mais um passo por harmonico:
    duas fases proximas fazem os harmonicos cancelarem justamente na regiao onde
    eles deveriam se somar.
    """
    fases = [((semente + 1) * 0.6180 + k * 0.3333) % 1.0 for k in range(harmonicos)]
    total = sum(pesos[:harmonicos])
    passo = duracao / amostras
    saida = []
    for i in range(amostras + 1):
        t = i * passo
        v = sum(pesos[k] * math.cos(2 * math.pi * ((k + 1) * t / duracao + fases[k]))
                for k in range(harmonicos))
        saida.append((t, amplitude * v / total))
    return saida


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
    ordenados = sorted(((float(k), v) for k, v in quadros.items()),
                       key=lambda kv: kv[0])
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
    dois clipes citam, um `bite` que esquecesse as pernas casaria com um `swim`
    que as recolhe -- e as quatro pernas saltariam sozinhas no quadro do bote.
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
    t=0.12 vale aquele valor TAMBEM antes de 0.12: o clipe comecaria ja
    deslocado, e a costura com o clipe anterior quebraria sem nenhuma chave
    errada a vista.
    """
    for osso, canais in pose.items():
        for canal, valor in canais.items():
            if canal == "scale" and valor == escala():
                continue  # escala neutra nao "segura" nada; nao precisa ser dita
            if canal == "position" and valor == vetor() \
                    and osso not in OSSOS_QUE_TRANSLADAM:
                continue
            curva(bones, osso, canal, [(t, list(valor))])


def sobre(pose, osso, canal, x=0.0, y=0.0, z=0.0):
    """Um delta SOBRE a pose de referencia, no lugar de um absoluto.

    E o que permite um clipe comecar exatamente onde o vizinho esta sem repetir
    os numeros dele. Escrito como absoluto, o delta zero de um clipe deixaria de
    coincidir com a pose do vizinho no dia em que o vizinho mudasse -- e a
    costura romperia por um valor que ninguem digitou errado.
    """
    base = pose[osso][canal]
    return vetor(base[0] + x, base[1] + y, base[2] + z)


def valores(tabela, osso, canal):
    return tabela.get(osso, {}).get(canal, (0.0, 0.0, 0.0))


# ------------------------------------------------------------------- clipes


def swim():
    """Nado de corpo inteiro. 1,4s por batida -- UMA batida, e e lenta.

    A onda VIAJA: `body` lidera, `tail` atrasa 14% do periodo, `tail_fin` 26%.
    E ela CRESCE para tras (3,5 -> 14 -> 22 graus). As duas coisas juntas sao a
    leitura de PESO: num bicho de 2,4 blocos a cabeca tem de ficar parada e quem
    trabalha e o terco final. Amplitude igual do focinho a cauda le como enguia;
    batida rapida le como peixinho. `conferir_que_a_onda_cresce_para_tras` e
    `conferir_que_o_nado_e_pesado` reprovam os dois.

    As PERNAS ficam recolhidas sob o corpo o clipe inteiro, com uma deriva de 3
    graus para nao virarem enfeite colado. Perna aberta em nado le como bicho
    andando debaixo d'agua -- e este mob nada.

    ESTE CLIPE E A POSE-HUB do arquivo: `quadro_em(swim, 0)` e de onde `turn` e
    `bite` partem e para onde `bite` volta. Ele e construido primeiro por isso.
    """
    dur = DUR_SWIM
    b = {}
    pose = pose_neutra()
    for barbatana in BARBATANAS:
        lado = -1.0 if barbatana.endswith("left") else 1.0
        pose[barbatana]["rotation"] = vetor(z=BARBATANA_CRUZEIRO * lado)
    for perna in PERNAS:
        pose[perna]["rotation"] = vetor(x=PERNA_RECOLHIDA)
        pose[GARRA[perna]]["rotation"] = vetor(x=PERNA_RECOLHIDA * GARRA_CONTRA)
    aplicar(b, pose, 0.0)
    aplicar(b, pose, dur)

    curva(b, "body", "rotation",
          [(t, vetor(y=v * SENTIDO_DE_Y)) for t, v in ciclo(dur, CICLO_NADO, ONDA_CORPO)])
    # O corpo sobe e desce DUAS vezes por batida, quase nada: e a sustentacao, nao
    # a batida. Um corpo que oscilasse junto com a cauda leria como peixe leve.
    curva(b, "body", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, CICLO_NADO / 2.0, 0.35)])
    curva(b, "tail", "rotation", [
        (t, vetor(x=vx, y=vy * SENTIDO_DE_Y))
        for (t, vy), (_, vx) in zip(
            ciclo(dur, CICLO_NADO, ONDA_CAUDA, 0.0, 0.75 + ATRASO_DA_CAUDA),
            ciclo(dur, CICLO_NADO / 2.0, 2.0))])
    curva(b, "tail_fin", "rotation", [
        (t, vetor(y=v * SENTIDO_DE_Y))
        for t, v in ciclo(dur, CICLO_NADO, ONDA_PONTA, 0.0, 0.75 + ATRASO_DA_PONTA)])
    # A cabeca em CONTRA-FASE: ela corrige o rumo que a cauda desvia. Em fase com
    # o corpo, o peixe pareceria olhar para onde nao esta indo.
    curva(b, "head", "rotation",
          [(t, vetor(y=v * SENTIDO_DE_Y)) for t, v in ciclo(dur, CICLO_NADO, ONDA_CABECA)])
    # A boca abre de leve e fecha: e o bombeamento de agua pelas branquias. E o
    # unico sinal de que o bicho esta vivo quando ele esta so passando.
    curva(b, "jaw", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, CICLO_NADO, -2.5, -2.5)])
    for barbatana in BARBATANAS:
        lado = -1.0 if barbatana.endswith("left") else 1.0
        curva(b, barbatana, "rotation", [
            (t, vetor(z=(BARBATANA_CRUZEIRO + v) * lado))
            for t, v in ciclo(dur, CICLO_NADO, 5.0, 0.0, 0.75 + 0.2)])
    for perna in PERNAS:
        frente = "front" in perna
        membro(b, perna, [
            (t, vetor(x=v)) for t, v in ciclo(
                dur, CICLO_NADO, 3.0 if frente else -3.0, PERNA_RECOLHIDA,
                0.75 + (0.0 if frente else 0.1))])
    return {"loop": LOOPS["swim"], "animation_length": dur, "bones": b}


def turn(hub):
    """A curva. 1,0s, loop -- e ela e SIMETRICA, o que e uma decisao.

    A ficha pede "corpo arqueando lateralmente, cauda varrendo para o lado
    oposto, barbatana interna recolhida". O reflexo seria arquear para UM lado.
    Mas nada neste arquivo sabe para que lado o servidor esta virando: o clipe e
    pedido por estado, nao por sinal. Um arco fixo estaria errado METADE das
    vezes -- e um peixe inclinado para fora da curva le como bicho derrapando.

    Entao a curva vai e volta: arqueia para um lado, atravessa, arqueia para o
    outro. A frase da ficha continua cumprida nos dois extremos, porque quem
    decide qual barbatana recolhe e o SINAL do arco naquele instante, e nao um
    lado escolhido aqui (ver o `max(0, ...)`: a de dentro dobra, a de fora abre).

    A cauda AMPLIFICA o yaw do corpo em vez de contra-gira-lo. Ver os eixos no
    cabecalho: um yaw ja poe cabeca e cauda em lados opostos. Contra-girar a
    cauda a traria de volta para o lado da cabeca -- que e a pose do arranque (um
    C), e nao a de uma curva sustentada.
    """
    dur = DUR_TURN
    b = {}
    aplicar(b, hub, 0.0)
    aplicar(b, hub, dur)

    arco = ciclo(dur, CICLO_CURVA, 1.0)  # -1..1, e vale ZERO em t=0 e em t=dur
    curva(b, "body", "rotation", [
        (t, sobre(hub, "body", "rotation",
                  y=ARCO_DA_CURVA * c * SENTIDO_DE_Y, z=ROLAMENTO_DA_CURVA * c))
        for t, c in arco])
    curva(b, "head", "rotation", [
        (t, sobre(hub, "head", "rotation", y=OLHAR_NA_CURVA * c * SENTIDO_DE_Y))
        for t, c in arco])
    curva(b, "tail", "rotation", [
        (t, sobre(hub, "tail", "rotation", y=VARRIDA_DA_CAUDA * c * SENTIDO_DE_Y))
        for t, c in arco])
    curva(b, "tail_fin", "rotation", [
        (t, sobre(hub, "tail_fin", "rotation",
                  y=VARRIDA_DA_PONTA * c * SENTIDO_DE_Y))
        for t, c in arco])
    for barbatana in BARBATANAS:
        # A de DENTRO recolhe. "Dentro" e o lado para onde a cabeca esta indo, e
        # com Y positivo a cabeca vai para -X (a direita) -- entao c>0 recolhe a
        # direita. O max() e o que faz a outra ficar aberta em vez de meio dobrada.
        interna = 1.0 if barbatana.endswith("right") else -1.0
        lado = -1.0 if barbatana.endswith("left") else 1.0
        curva(b, barbatana, "rotation", [
            (t, sobre(hub, barbatana, "rotation",
                      z=-DOBRA_DA_INTERNA * max(0.0, c * interna) * lado))
            for t, c in arco])
    # A RESPIRACAO E A DERIVA DAS PERNAS ATRAVESSAM O CLIPE, e isso nao e enfeite.
    # Osso que este clipe CITA e nao move fica CONGELADO (nao volta ao default do
    # modelo): sem estas duas curvas, o peixe bombeia agua enquanto nada e para de
    # bombear no instante em que vira. Ninguem chamaria isso de bug -- so acharia
    # o bicho esquisito. `conferir_que_nenhum_loop_tem_pedaco_morto` reprova.
    curva(b, "jaw", "rotation", [
        (t, sobre(hub, "jaw", "rotation", x=v))
        for t, v in ciclo(dur, CICLO_CURVA, -2.5)])
    for perna in PERNAS:
        frente = "front" in perna
        membro(b, perna, [
            (t, sobre(hub, perna, "rotation", x=v))
            for t, v in ciclo(dur, CICLO_CURVA, 2.5 if frente else -2.5)])
    return {"loop": LOOPS["turn"], "animation_length": dur, "bones": b}


def bite(hub):
    """A bocada: 12 ticks de aviso, 4 de janela, 16 de recuperacao. 1,6s.

    O AVISO E A BOCA ABRINDO, e ele e LENTO de proposito. Sao 12 ticks em que o
    jogador ainda pode sair -- e uma boca que escancarasse em 2 ticks nao seria
    aviso nenhum, seria a mordida. `conferir_que_o_aviso_da_bocada_e_lento`
    reprova quem antecipar a abertura.

    A boca chega ao MAXIMO exatamente no fim do windup, e esta CERRADA no fim da
    janela ativa. Essa e a unica coisa deste clipe que o servidor pode
    contradizer em silencio: com o fecho na recuperacao, o dano sai antes da
    mordida aparecer, e o jogador aprende que a bocada "pega do nada".

    O corpo RECUA antes (1,8 px para +Z) e AVANCA no bote (-3,2 px). O recuo e
    metade do aviso: um bicho que so abre a boca parece bocejar; um que puxa o
    corpo para tras esta carregando.

    Comeca e termina no HUB (o primeiro quadro do nado), entao os 4 ticks de
    transicao do controller viram um no-op nas duas pontas.
    """
    dur = DUR_BITE
    janela = DUR_WINDUP + DUR_ACTIVE
    b = {}
    aplicar(b, hub, 0.0)

    # A boca: as chaves de 0.20 e 0.42 sao o que torna a abertura GRADUAL. Sem
    # elas a interpolacao linear de 0 a 0.60 daria a mesma coisa -- mas qualquer
    # ajuste posterior na curva passaria a ser invisivel, e o portao do aviso
    # lento estaria medindo uma reta que ninguem escolheu.
    curva(b, "jaw", "rotation", [
        (0.20, vetor(x=BOCA_ESCANCARADA * 0.30)),
        (0.42, vetor(x=BOCA_ESCANCARADA * 0.68)),
        (DUR_WINDUP, vetor(x=BOCA_ESCANCARADA)),
        (DUR_WINDUP + 0.10, vetor(x=BOCA_ESCANCARADA * 0.12)),
        (janela, vetor(x=BOCA_CERRADA)),
        (1.05, vetor(x=-4.0)),
        (1.35, vetor(x=-1.0))])
    curva(b, "body", "position", [
        (0.20, sobre(hub, "body", "position", z=RECUO_DA_BOCADA * 0.35)),
        (DUR_WINDUP, sobre(hub, "body", "position", z=RECUO_DA_BOCADA)),
        (DUR_WINDUP + 0.10, sobre(hub, "body", "position", z=AVANCO_DA_BOCADA * 0.8)),
        (janela, sobre(hub, "body", "position", z=AVANCO_DA_BOCADA)),
        (1.10, sobre(hub, "body", "position", z=AVANCO_DA_BOCADA * 0.45))])
    curva(b, "body", "rotation", [
        (0.25, sobre(hub, "body", "rotation", x=4.0)),
        (DUR_WINDUP, sobre(hub, "body", "rotation", x=6.0)),
        (janela, sobre(hub, "body", "rotation", x=-5.0)),
        (1.10, sobre(hub, "body", "rotation", x=-1.5))])
    curva(b, "head", "rotation", [
        (0.25, sobre(hub, "head", "rotation", x=3.0)),
        (DUR_WINDUP, sobre(hub, "head", "rotation", x=6.0)),
        (janela, sobre(hub, "head", "rotation", x=-4.0)),
        (1.10, sobre(hub, "head", "rotation", x=-1.0))])
    # A cauda CARREGA durante o aviso e descarrega no bote: e ela que empurra.
    curva(b, "tail", "rotation", [
        (0.30, sobre(hub, "tail", "rotation", y=10.0 * SENTIDO_DE_Y)),
        (DUR_WINDUP, sobre(hub, "tail", "rotation", y=20.0 * SENTIDO_DE_Y)),
        (janela, sobre(hub, "tail", "rotation", y=-14.0 * SENTIDO_DE_Y)),
        (1.10, sobre(hub, "tail", "rotation", y=-4.0 * SENTIDO_DE_Y))])
    curva(b, "tail_fin", "rotation", [
        (DUR_WINDUP, sobre(hub, "tail_fin", "rotation", y=16.0 * SENTIDO_DE_Y)),
        (janela + 0.06, sobre(hub, "tail_fin", "rotation", y=-18.0 * SENTIDO_DE_Y)),
        (1.20, sobre(hub, "tail_fin", "rotation", y=-3.0 * SENTIDO_DE_Y))])
    for barbatana in BARBATANAS:
        lado = -1.0 if barbatana.endswith("left") else 1.0
        curva(b, barbatana, "rotation", [
            (DUR_WINDUP, sobre(hub, barbatana, "rotation", z=-16.0 * lado)),
            (janela, sobre(hub, barbatana, "rotation", z=8.0 * lado)),
            (1.15, sobre(hub, barbatana, "rotation", z=2.0 * lado))])
    # AS PERNAS ABREM NO AVISO. E o lado crustaceo da ficha aparecendo no unico
    # momento em que o jogador esta perto o bastante para olhar: quatro pernas se
    # abrindo antes da boca chegar. Recolhidas o clipe inteiro, o bicho seria um
    # peixe grande com pernas de enfeite.
    for perna in PERNAS:
        frente = "front" in perna
        abre = -30.0 if frente else -46.0
        membro(b, perna, [
            (0.30, vetor(x=PERNA_RECOLHIDA + (PERNA_RECOLHIDA - abre) * -0.45)),
            (DUR_WINDUP, vetor(x=abre)),
            (janela, vetor(x=abre - 14.0)),
            (1.20, vetor(x=PERNA_RECOLHIDA + 6.0))])
    aplicar(b, hub, dur)
    return {"loop": LOOPS["bite"], "animation_length": dur, "bones": b}


def thrash():
    """O CLIPE DA LUTA. 25 ticks, repetindo 8 vezes dentro dos 200 de cansaco.

    E a unica interface do cabo de guerra. Enquanto ele roda, o jogador esta
    decidindo se puxa (e arrebenta a linha) ou se acompanha (e cansa o peixe), e
    ele decide OLHANDO. Entao este clipe tem uma exigencia que nenhuma pose
    resolve sozinha: tem de parecer CUSTOSO DE SEGURAR.

    O movimento e gerado por soma de tres harmonicos com fases proprias por osso
    e por eixo (ver `ruido`). Nao e enfeite tecnico -- e a diferenca entre um
    peixe se debatendo e um peixe balancando no compasso. Escrito a mao, todo
    osso bate junto, porque e assim que uma pessoa digita uma lista de tempos.

    E como cada harmonico cabe um numero INTEIRO de vezes na duracao, o caos
    fecha o loop exatamente -- sem o tranco a cada volta que um debate escrito a
    mao teria.
    """
    dur = DUR_THRASH
    b = {}
    base = pose_neutra()
    for osso, canais in DEBATE.items():
        for canal, valor in canais.items():
            base[osso][canal] = vetor(*valor)
    for perna in PERNAS:
        x = PERNA_NO_DEBATE_FRENTE if "front" in perna else PERNA_NO_DEBATE_TRAS
        lado = 1.0 if perna.endswith("left") else -1.0
        base[perna]["rotation"] = vetor(x=x, z=10.0 * lado)
        base[GARRA[perna]]["rotation"] = vetor(x=x * GARRA_CONTRA)
    aplicar(b, base, 0.0)
    aplicar(b, base, dur)

    semente = 0
    for indice, osso in enumerate(ANIMAVEIS):
        if osso in AGITO:
            for canal, amplitudes in AGITO[osso].items():
                eixos = []
                for eixo, amplitude in enumerate(amplitudes):
                    semente += 1
                    eixos.append(ruido(dur, amplitude, semente) if amplitude
                                 else [(t, 0.0) for t, _ in ruido(dur, 1.0, semente)])
                pares = []
                for i in range(len(eixos[0])):
                    t = eixos[0][i][0]
                    pares.append((t, sobre(base, osso, canal,
                                           x=eixos[0][i][1], y=eixos[1][i][1],
                                           z=eixos[2][i][1])))
                curva(b, osso, canal, pares)
    # AS PERNAS AGITAM, cada uma com a sua fase. Quatro pernas em fase seriam um
    # remo; quatro pernas fora de fase sao um bicho tentando se agarrar em nada --
    # e e a unica hora do arquivo em que o lado crustaceo domina a silhueta.
    for perna in PERNAS:
        semente += 1
        px = ruido(dur, AGITO_DA_PERNA[0], semente)
        semente += 1
        pz = ruido(dur, AGITO_DA_PERNA[2], semente)
        membro(b, perna, [
            (t, sobre(base, perna, "rotation", x=vx, z=vz))
            for (t, vx), (_, vz) in zip(px, pz)])
    return {"loop": LOOPS["thrash"], "animation_length": dur, "bones": b}


def caught():
    """Cansado e recolhivel. 3,0s, loop lento.

    O CLIPE MAIS FACIL DE ERRAR DO ARQUIVO, e o erro nao aparece em lugar nenhum
    a nao ser no comportamento do jogador. Ele tem de dizer "agora da para pegar"
    -- e as duas maneiras de falhar sao simetricas:

      movimento demais -> le como peixe ainda lutando, e ninguem tenta recolher;
      movimento de menos -> le como peixe MORTO, e ninguem tenta recolher.

    O que separa das duas coisas: a boca fica ENTREABERTA o clipe inteiro (um
    peixe exausto bombeia agua; um morto nao) e tudo respira devagar, com
    amplitude pequena mas nunca nula. `conferir_que_cansado_nao_le_como_morte`
    guarda os dois lados, e `conferir_a_escada_de_agitacao` guarda a razao contra
    o debate.

    NAO HA COSTURA com o `thrash`, e isso e deliberado -- ver `costuras`.
    """
    dur = DUR_CAUGHT
    b = {}
    base = pose_neutra()
    for osso, canais in CANSADO.items():
        for canal, valor in canais.items():
            base[osso][canal] = vetor(*valor)
    for perna in PERNAS:
        x = PERNA_CANSADA_FRENTE if "front" in perna else PERNA_CANSADA_TRAS
        lado = 1.0 if perna.endswith("left") else -1.0
        base[perna]["rotation"] = vetor(x=x, z=6.0 * lado)
        base[GARRA[perna]]["rotation"] = vetor(x=x * GARRA_CONTRA)
    aplicar(b, base, 0.0)
    aplicar(b, base, dur)

    # Dois harmonicos, nao tres: cansado nao tem a componente rapida. E o mesmo
    # mecanismo do debate com a pressa tirada -- que e literalmente o que
    # "exausto" significa aqui.
    semente = 500
    for osso in ANIMAVEIS:
        if osso in RESPIRO:
            for canal, amplitudes in RESPIRO[osso].items():
                eixos = []
                for amplitude in amplitudes:
                    semente += 1
                    eixos.append(ruido(dur, amplitude, semente, amostras=16,
                                       harmonicos=2))
                pares = []
                for i in range(len(eixos[0])):
                    t = eixos[0][i][0]
                    pares.append((t, sobre(base, osso, canal,
                                           x=eixos[0][i][1], y=eixos[1][i][1],
                                           z=eixos[2][i][1])))
                curva(b, osso, canal, pares)
    for perna in PERNAS:
        semente += 1
        membro(b, perna, [
            (t, sobre(base, perna, "rotation", x=v))
            for t, v in ruido(dur, RESPIRO_DA_PERNA[0], semente, amostras=16,
                              harmonicos=2)])
    return {"loop": LOOPS["caught"], "animation_length": dur, "bones": b}


def escape(inicio):
    """A fuga: 16 ticks. Carrega, chicoteia UMA vez, e some.

    Comeca exatamente no primeiro quadro do `thrash` (`inicio`, lido do clipe ja
    construido, e nao repetido aqui): a linha arrebenta NO MEIO do debate. Sem
    essa costura, os 4 ticks de transicao do controller comeriam um quarto de um
    clipe de 16 -- e o jogador que acabou de perder o peixe veria uma mistura, que
    e a pior coisa que se pode mostrar no momento em que se explica a derrota.

    UMA batida, e nao um ciclo. Um chicote que fosse e voltasse leria como mais um
    ciclo de debate; o que diz "acabou" e a assimetria: carrega para um lado
    (0,12s), atravessa tudo (0,26s), e depois so o corpo estendido indo embora.

    hold_on_last_frame: `false` devolveria o peixe a pose neutra no ultimo quadro,
    e esse e o ultimo quadro que o jogador ve dele.
    """
    dur = DUR_ESCAPE
    carga, chicote = 0.12, 0.26
    b = {}
    aplicar(b, inicio, 0.0)

    curva(b, "body", "rotation", [
        (carga, vetor(x=-2.0, y=COILE_DA_FUGA * SENTIDO_DE_Y, z=14.0)),
        (chicote, vetor(x=-4.0, y=-18.0 * SENTIDO_DE_Y, z=-8.0)),
        (0.46, vetor(x=-2.0, y=-4.0 * SENTIDO_DE_Y, z=-2.0)),
        (dur, vetor(x=-1.0))])
    curva(b, "body", "position", [
        (carga, vetor(z=1.2)),
        (chicote, vetor(z=ARRANCADA_PX * 0.42)),
        (0.46, vetor(z=ARRANCADA_PX * 0.74)),
        (dur, vetor(z=ARRANCADA_PX))])
    curva(b, "head", "rotation", [
        (carga, vetor(x=-4.0, y=16.0 * SENTIDO_DE_Y)),
        (chicote, vetor(x=-2.0, y=-8.0 * SENTIDO_DE_Y)),
        (dur, vetor())])
    # A boca FECHA na carga e fica fechada: o que atravessa a agua a essa
    # velocidade nao vai de boca aberta, e boca fechada e o sinal de que ele
    # parou de lutar contra a linha e passou a nadar.
    curva(b, "jaw", "rotation", [
        (carga, vetor(x=-8.0)), (chicote, vetor(x=BOCA_FECHADA)),
        (dur, vetor(x=BOCA_FECHADA))])
    curva(b, "tail", "rotation", [
        (carga, vetor(y=-CHICOTE_DA_FUGA * 0.75 * SENTIDO_DE_Y, z=-10.0)),
        (chicote, vetor(y=CHICOTE_DA_FUGA * SENTIDO_DE_Y, z=8.0)),
        (0.46, vetor(y=24.0 * SENTIDO_DE_Y)),
        (0.62, vetor(y=-10.0 * SENTIDO_DE_Y)),
        (dur, vetor())])
    curva(b, "tail_fin", "rotation", [
        (carga, vetor(y=-34.0 * SENTIDO_DE_Y)),
        (chicote + 0.05, vetor(y=44.0 * SENTIDO_DE_Y)),
        (0.52, vetor(y=16.0 * SENTIDO_DE_Y)),
        (0.68, vetor(y=-8.0 * SENTIDO_DE_Y)),
        (dur, vetor())])
    for barbatana in BARBATANAS:
        lado = -1.0 if barbatana.endswith("left") else 1.0
        curva(b, barbatana, "rotation", [
            (carga, vetor(z=(BARBATANA_CRUZEIRO + 18.0) * lado)),
            (chicote, vetor(z=4.0 * lado)),
            (dur, vetor(z=2.0 * lado))])
    # As pernas COLAM. E o ultimo pedaco de silhueta que ele guarda, e guardar e o
    # que diz que ele esta indo embora e nao voltando.
    for perna in PERNAS:
        membro(b, perna, [
            (carga, vetor(x=PERNA_RECOLHIDA * 0.6)),
            (chicote, vetor(x=PERNA_NA_FUGA)),
            (dur, vetor(x=PERNA_NA_FUGA))])
    return {"loop": LOOPS["escape"], "animation_length": dur, "bones": b}


# ------------------------------------------------------- leitura do geo


def carregar_geo():
    caminho = os.path.join(DIR_GEO, MOB + ".geo.json")
    if not os.path.exists(caminho):
        raise SystemExit(
            "%s nao existe. Este gerador CONFERE o geo: os eixos, a arvore de"
            " ossos, a simetria e o recolhimento das pernas sao julgados contra"
            " ele. Sem geo nao ha o que animar -- e rodar sem conferir seria"
            " escrever um arquivo que ninguem checou contra o modelo." % caminho)
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


def filhos(geometria, nome):
    return [b["name"] for b in geometria["bones"] if b.get("parent") == nome]


def subarvore_tem_volume(geometria, nome):
    if caixas(geometria, nome):
        return True
    return any(subarvore_tem_volume(geometria, f) for f in filhos(geometria, nome))


# ------------------------------------------------------ portoes de geometria


def conferir_a_hierarquia(geometria):
    """O geo tem de ter A ARVORE do contrato, e nao so os nomes dele.

    O portao do Java confere que todo `parent` existe. Ele NAO confere que
    `tail_fin` e filha de `tail`, nem que a garra e filha da perna -- e sao essas
    duas herancas que fazem a onda do nado crescer e a contra-rotacao da garra
    funcionar. Penduradas no `body`, as duas passariam no Java e animariam torto.
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


def conferir_eixos(geometria):
    """Os eixos se LEEM do geo. Decorados, produzem um peixe nadando de re."""
    if pivot(geometria, "head")[1] < pivot(geometria, "body")[1] - 0.001:
        raise SystemExit(
            "a cabeca esta abaixo do corpo (head y=%.1f, body y=%.1f). Todo este"
            " gerador supoe +Y = CIMA."
            % (pivot(geometria, "head")[1], pivot(geometria, "body")[1]))
    # A FRENTE SE DESCOBRE COM A CABECA E A CAUDA, que e como o contrato manda.
    cabeca = centro(geometria, "head", 2)
    cauda = centro(geometria, "tail", 2)
    if not cabeca < cauda:
        raise SystemExit(
            "a cabeca esta em z=%.1f e a cauda em z=%.1f: a frente nao e -Z. Com o"
            " eixo invertido, a arrancada do escape empurra o peixe para TRAS e a"
            " onda do nado viaja da cauda para a cabeca -- tudo sem um unico erro"
            " no log." % (cabeca, cauda))
    if centro(geometria, "tail_fin", 2) <= centro(geometria, "tail", 2):
        raise SystemExit(
            "tail_fin (z=%.1f) nao esta ATRAS de tail (z=%.1f). A onda do nado"
            " supoe que a ponta e o ultimo segmento; invertida, o atraso dela vira"
            " adianto e a cauda parece empurrar a cabeca."
            % (centro(geometria, "tail_fin", 2), centro(geometria, "tail", 2)))
    if centro(geometria, "jaw", 2) >= pivot(geometria, "jaw")[2]:
        raise SystemExit(
            "a mandibula nao se estende a frente da propria dobradica (massa em"
            " z=%.1f, pivot em z=%.1f). Assim 'abrir a boca' (X negativo) FECHA a"
            " boca, e o aviso de 12 ticks da bocada vira um bicho de boca cerrada."
            % (centro(geometria, "jaw", 2), pivot(geometria, "jaw")[2]))
    if centro(geometria, "fin_left", 0) <= 0:
        raise SystemExit(
            "fin_left esta em x=%.1f, e o contrato supoe +X = ESQUERDA. Com o"
            " sinal trocado as barbatanas se CRUZAM, e na curva recolhe a de fora."
            % centro(geometria, "fin_left", 0))
    fundo_da_perna, _ = faixa(geometria, PERNAS + tuple(GARRA.values()), 1)
    barriga, _ = faixa(geometria, ("body",), 1)
    if fundo_da_perna >= barriga:
        raise SystemExit(
            "as pernas nao PENDURAM abaixo da barriga (barriga y=%.1f, ponto mais"
            " baixo da perna y=%.1f). Este gerador supoe perna pendurada: sem"
            " isso, 'recolher' (X negativo) gira para o lado errado e as quatro"
            " pernas apontam para a FRENTE durante o nado inteiro."
            % (barriga, fundo_da_perna))


def conferir_a_simetria(geometria):
    """A animacao espelha; o geo tem de ser espelhado.

    Com o geo assimetrico, espelhar a animacao produz um peixe que abre uma
    barbatana mais que a outra -- e o relato de bug vira "a animacao esta torta",
    que e o lugar errado para procurar.
    """
    pares = [("fin_left", "fin_right")]
    for frente in ("front", "back"):
        pares.append(("leg_%s_left" % frente, "leg_%s_right" % frente))
        pares.append(("claw_%s_left" % frente, "claw_%s_right" % frente))
    for esquerda, direita in pares:
        e = [centro(geometria, esquerda, i) for i in range(3)]
        d = [centro(geometria, direita, i) for i in range(3)]
        if abs(e[0] + d[0]) > 0.51 or abs(e[1] - d[1]) > 0.51 \
                or abs(e[2] - d[2]) > 0.51:
            raise SystemExit(
                "'%s' esta em %s e '%s' em %s: os dois nao sao espelhos. A"
                " animacao escreve esquerda +v e direita -v; com o geo assimetrico"
                " isso move um lado mais que o outro." % (esquerda, e, direita, d))


def conferir_que_o_peixe_e_enorme(geometria):
    """A silhueta contra a HITBOX -- e ela morde dos dois lados.

    O contrato pediu sized(2.4F, 1.6F) porque a ficha diz "peixe ENORME", e a
    hitbox e o que o servidor usa para tudo (mira, alcance da isca, colisao). Um
    modelo pequeno dentro dessa caixa e uma caixa que mente sobre o tamanho: o
    jogador acerta o vazio ao lado do bicho. Um modelo maior que a caixa mente na
    outra direcao: a cauda atravessa o barco e nao colide com nada.
    """
    menor, maior = faixa(geometria, [n for n in OSSOS if caixas(geometria, n)], 2)
    comprimento = maior - menor
    razao = comprimento / HITBOX_LARGURA_PX
    if razao < COMPRIMENTO_MINIMO:
        raise SystemExit(
            "o modelo mede %.1f px de comprimento numa hitbox de %.1f px (%.2fx),"
            " abaixo do minimo de %.2fx. A caixa mente sobre o tamanho: o jogador"
            " acerta o vazio em volta de um peixe que a ficha chama de ENORME."
            % (comprimento, HITBOX_LARGURA_PX, razao, COMPRIMENTO_MINIMO))
    if razao > COMPRIMENTO_MAXIMO:
        raise SystemExit(
            "o modelo mede %.1f px de comprimento numa hitbox de %.1f px (%.2fx),"
            " acima do maximo de %.2fx. A silhueta passa da caixa: a cauda aparece"
            " onde nao ha colisao nenhuma, e nada acusa."
            % (comprimento, HITBOX_LARGURA_PX, razao, COMPRIMENTO_MAXIMO))


def conferir_que_as_pernas_recolhem_sem_atravessar_o_corpo(geometria):
    """PERNA_RECOLHIDA e decidido aqui e CONFERIDO contra o geo de la.

    A ficha pede "pernas recolhidas sob o corpo" durante o nado, e isso e duas
    afirmacoes, nao uma:

      recolhidas -- a perna dobrou de verdade (>= 45 graus), em vez de so
                    inclinar e continuar remando embaixo do peixe;
      SOB o corpo -- a ponta subiu na direcao da barriga MAS parou abaixo dela.
                    Passando do teto, a perna entra no volume do corpo e o
                    jogador ve garra saindo pela lateral -- e isso le como bug de
                    modelagem, nao como bug de angulo.

    Um valor derivado do geo ficaria certo em silencio e exigiria regerar a cada
    correcao do modelo. Este fica RUIDOSAMENTE errado, que e o que se quer entre
    duas lanes que nao se veem.
    """
    if abs(PERNA_RECOLHIDA) < DOBRA_MINIMA_DA_PERNA:
        raise SystemExit(
            "PERNA_RECOLHIDA vale %.1f graus, e o minimo e %.1f. Abaixo disso a"
            " perna nao recolheu: ela continua pendurada, e o bicho nada com"
            " quatro remos abertos." % (PERNA_RECOLHIDA, DOBRA_MINIMA_DA_PERNA))
    barriga, _ = faixa(geometria, ("body",), 1)
    for perna in PERNAS:
        py, pz = pivot(geometria, perna)[1], pivot(geometria, perna)[2]
        baixo, _ = faixa(geometria, (perna, GARRA[perna]), 1)
        comprimento = py - baixo
        if comprimento <= 0.0:
            raise SystemExit(
                "a perna '%s' nao desce abaixo do proprio pivot (pivot y=%.1f,"
                " ponto mais baixo y=%.1f). Este gerador supoe perna PENDURADA."
                % (perna, py, baixo))
        angulo = math.radians(abs(PERNA_RECOLHIDA))
        ponta_y = py - comprimento * math.cos(angulo)
        ponta_z = pz + comprimento * math.sin(angulo)
        if ponta_y > barriga + comprimento * TOLERANCIA_DE_ENCOSTE:
            raise SystemExit(
                "recolhida a %.1f graus, a ponta de '%s' sobe ate y=%.2f, mais de"
                " %.0f%% da propria perna acima da barriga (y=%.2f): ela entra"
                " DENTRO do corpo. Em jogo isso aparece como garra saindo pela"
                " lateral do peixe, e le como erro de modelagem -- o lugar errado"
                " para procurar."
                % (PERNA_RECOLHIDA, perna, ponta_y,
                   TOLERANCIA_DE_ENCOSTE * 100.0, barriga))
        if ponta_y < barriga - comprimento * 0.5:
            raise SystemExit(
                "recolhida a %.1f graus, a ponta de '%s' fica em y=%.2f, mais de"
                " meia perna abaixo da barriga (y=%.2f). Isso nao e 'sob o corpo',"
                " e perna pendurada com o joelho torto."
                % (PERNA_RECOLHIDA, perna, ponta_y, barriga))
        if ponta_z <= pz:
            raise SystemExit(
                "recolhida, a ponta de '%s' fica em z=%.2f, que nao esta ATRAS do"
                " pivot (z=%.2f). O sinal de PERNA_RECOLHIDA esta invertido: ela"
                " esta dobrando para a frente." % (perna, ponta_z, pz))


# ---------------------------------------------------------- portoes de clipe


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


def conferir_ossos(animacoes, geometria):
    """Osso errado nao da erro no GeckoLib: o membro so fica parado."""
    do_geo = {b["name"] for b in geometria["bones"]}
    if do_geo != set(OSSOS):
        raise SystemExit("geo e contrato discordam de osso: %s"
                         % sorted(do_geo ^ set(OSSOS)))
    for nome, clipe in animacoes.items():
        desconhecidos = sorted(set(clipe["bones"]) - set(OSSOS))
        if desconhecidos:
            raise SystemExit("%s move osso que nao existe: %s"
                             % (nome, desconhecidos))
        proibidos = sorted(set(clipe["bones"]) & set(NAO_ANIMADOS))
        if proibidos:
            raise SystemExit(
                "%s anima %s. A raiz e a ancora que o renderer alinha com a"
                " hitbox: mexer nela desloca a silhueta inteira para fora da caixa"
                " de colisao, e num mob que se pesca pela distancia da isca isso"
                " desloca a leitura do jogador sem deslocar nada no servidor."
                % (nome, proibidos))


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


def conferir_que_so_translada_o_declarado(animacoes):
    """Quem translada um osso novo tem de dizer isso em OSSOS_QUE_TRANSLADAM.

    E o fecho de `aplicar`: a pose de costura so escreve `position` neutro para
    os ossos dessa lista. Um clipe que passasse a transladar a cabeca sem entrar
    na lista teria a primeira chave de posicao no meio do clipe -- e uma chave de
    posicao no meio vale TAMBEM antes dela, entao a cabeca comecaria deslocada e a
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


def conferir_que_ninguem_sai_da_caixa(animacoes):
    """Translacao de osso e emprestimo contra a hitbox, e o limite e dela.

    O deslocamento de verdade da fuga e a velocidade da entidade -- o servidor
    aplica, o cliente interpola, e a hitbox vai junto. A translacao do `body` so
    da o IMPULSO da leitura. Um osso que ande meia caixa poe metade do peixe fora
    da hitbox, e ai ele e atingivel onde ele visivelmente nao esta.
    """
    limite = HITBOX_LARGURA_PX / 2.0 * FRACAO_MAXIMA_DA_CAIXA
    for nome, clipe in animacoes.items():
        for osso, canais in clipe["bones"].items():
            for t, v in canais.get("position", {}).items():
                if max(abs(c) for c in v) > limite + 1e-9:
                    raise SystemExit(
                        "%s move '%s' para %s em t=%s, e o limite e %.1f px"
                        " (%.0f%% de meia hitbox de %.1f px)."
                        % (nome, osso, v, t, limite, FRACAO_MAXIMA_DA_CAIXA * 100,
                           HITBOX_LARGURA_PX))


def conferir_as_duracoes_do_servidor(animacoes):
    """As duracoes que o SERVIDOR manda nao podem divergir em silencio."""
    real = animacoes["animation.%s.bite" % MOB]["animation_length"]
    if abs(real - DUR_BITE) > 1e-9:
        raise SystemExit(
            "bite dura %.3fs e o servidor da %.3fs (AttackDefinition %d/%d/%d)."
            " Um telegrafo com o tempo errado ensina ao jogador um relogio que o"
            " jogo nao cumpre."
            % (real, DUR_BITE, TICKS_WINDUP, TICKS_ACTIVE, TICKS_RECOVERY))
    for clipe, periodo in (("swim", CICLO_NADO), ("turn", CICLO_CURVA)):
        voltas = animacoes["animation.%s.%s" % (MOB, clipe)]["animation_length"] / periodo
        if abs(voltas - round(voltas)) > 1e-6:
            raise SystemExit("%s tem %.3f ciclos: o loop salta a cada volta"
                             % (clipe, voltas))


def conferir_que_a_bocada_acontece_na_janela(animacoes):
    """A BOCA TEM DE FECHAR DENTRO DOS 4 TICKS EM QUE O DANO SAI.

    E o unico ponto deste arquivo que o servidor pode contradizer em silencio: o
    `AttackDefinition` entrega o dano na janela ativa, e se a boca ainda estiver
    aberta ali, o jogador leva a mordida ANTES de ver a mordida. Ele aprende que o
    bicho "pega do nada" -- e a resposta que isso ensina (nao chegar perto nunca)
    e a que mata a pesca.

    Do outro lado: a boca tem de estar ESCANCARADA no fim do windup, porque sao os
    12 ticks em que sair ainda resolve.
    """
    clipe = animacoes["animation.%s.bite" % MOB]
    quadros = clipe["bones"]["jaw"]["rotation"]
    janela = DUR_WINDUP + DUR_ACTIVE

    no_aviso = valor_em(quadros, DUR_WINDUP, vetor())[0]
    if no_aviso > BOCA_ESCANCARADA * 0.9:
        raise SystemExit(
            "no fim do aviso (t=%.2f) a boca esta em %.1f graus, e o contrato pede"
            " pelo menos %.1f. O aviso de 12 ticks existe para ser VISTO: uma boca"
            " meio aberta nao diz que a mordida vem."
            % (DUR_WINDUP, no_aviso, BOCA_ESCANCARADA * 0.9))

    no_fim_da_janela = valor_em(quadros, janela, vetor())[0]
    if no_fim_da_janela < -FOLGA_DA_BOCA_FECHADA:
        raise SystemExit(
            "no fim da janela ativa (t=%.2f) a boca ainda esta a %.1f graus de"
            " aberta. O dano sai nesses 4 ticks: com a boca aberta ali, o jogador"
            " leva a mordida antes de ve-la, e a bocada vira dano do nada."
            % (janela, no_fim_da_janela))

    # E o maximo de abertura tem de estar NO fim do aviso, nao antes nem depois.
    passo = 0.02
    n = int(round(clipe["animation_length"] / passo))
    aberturas = [(valor_em(quadros, i * passo, vetor())[0], i * passo)
                 for i in range(n + 1)]
    _, quando = min(aberturas)
    if abs(quando - DUR_WINDUP) > 0.06:
        raise SystemExit(
            "a boca chega ao maximo em t=%.2f e o fim do aviso e t=%.2f. Antes, o"
            " aviso acaba cedo e o jogador acha que ja passou; depois, a boca"
            " escancara junto com o dano e o aviso deixa de ser aviso."
            % (quando, DUR_WINDUP))


def conferir_que_o_aviso_da_bocada_e_lento(animacoes):
    """A boca abre DEVAGAR, e isso e o aviso inteiro.

    Uma boca que escancara em dois ticks e a mordida, nao o telegrafo dela. Este
    portao exige que a abertura seja progressiva: no meio do windup ela tem de
    estar entre um terco e dois tercos do maximo. Um degrau tardio ("fechada,
    fechada, ESCANCARADA") passaria nos outros dois portoes desta bocada.
    """
    quadros = animacoes["animation.%s.bite" % MOB]["bones"]["jaw"]["rotation"]
    meio = valor_em(quadros, DUR_WINDUP / 2.0, vetor())[0] / BOCA_ESCANCARADA
    if not 0.30 <= meio <= 0.72:
        raise SystemExit(
            "na metade do aviso (t=%.2f) a boca esta a %.0f%% do maximo, e o"
            " contrato pede entre 30%% e 72%%. Fora disso a abertura nao e"
            " progressiva: ou ela ja escancarou (e os 12 ticks de aviso valem 6),"
            " ou ela abre de uma vez no fim (e o aviso vira a propria mordida)."
            % (DUR_WINDUP / 2.0, meio * 100.0))


def amplitude(clipe, osso, canal, eixo):
    quadros = clipe["bones"].get(osso, {}).get(canal, {})
    if not quadros:
        return 0.0
    valores = [v[eixo] for v in quadros.values()]
    return max(valores) - min(valores)


def conferir_que_a_onda_cresce_para_tras(animacoes):
    """A ONDA CRESCE E VIAJA -- e as duas coisas juntas sao a leitura de PESO.

    Cresce: `body` < `tail` < `tail_fin`. Amplitude igual do focinho a cauda le
    como enguia; maior na frente le como bicho sendo arrastado pela cabeca.

    Viaja: os atrasos sao estritamente crescentes. Sem atraso, os tres ossos
    giram no mesmo instante e a cauda inteira vira uma tabua com dobradica. Esse
    defeito nao aparece numa captura de tela: so em movimento.
    """
    nado = animacoes["animation.%s.swim" % MOB]
    escada = [("body", amplitude(nado, "body", "rotation", 1)),
              ("tail", amplitude(nado, "tail", "rotation", 1)),
              ("tail_fin", amplitude(nado, "tail_fin", "rotation", 1))]
    for (a, va), (bb, vb) in zip(escada, escada[1:]):
        if not va < vb:
            raise SystemExit(
                "no nado, '%s' varre %.1f graus e '%s' varre %.1f: a onda nao"
                " cresce para tras. Um peixe de 2,4 blocos com a mesma amplitude"
                " do focinho a cauda le como enguia." % (a, va, bb, vb))
    if not 0.0 < ATRASO_DA_CAUDA < ATRASO_DA_PONTA:
        raise SystemExit(
            "os atrasos da onda sao %.2f (cauda) e %.2f (ponta): ela nao VIAJA."
            " Sem atraso crescente os tres ossos giram juntos e a cauda vira uma"
            " tabua com dobradica -- defeito que so aparece em movimento."
            % (ATRASO_DA_CAUDA, ATRASO_DA_PONTA))


def conferir_que_a_curva_arqueia_o_corpo(animacoes):
    """O que separa `turn` de `swim` e o CORPO, nao a cauda.

    As duas movem a cauda em Y. Se o corpo nao arquear, a curva e um nado com a
    cauda um pouco mais larga -- e o jogador nao tem como ver que o bicho virou.
    """
    curva_ = amplitude(animacoes["animation.%s.turn" % MOB], "body", "rotation", 1)
    nado = amplitude(animacoes["animation.%s.swim" % MOB], "body", "rotation", 1)
    if nado <= 0.0 or curva_ < nado * RAZAO_DO_ARCO:
        raise SystemExit(
            "na curva o corpo arqueia %.1f graus e no nado %.1f (%.2fx), e o"
            " minimo e %.2fx. Sem o arco, virar e nadar sao o mesmo clipe com a"
            " cauda um pouco maior." % (curva_, nado,
                                        curva_ / nado if nado else 0.0,
                                        RAZAO_DO_ARCO))


def agitacao(clipe):
    """Graus por segundo, medios, sobre os ossos que o clipe move.

    E a unica regua desta lane que mede LEITURA em vez de pose, e ela existe
    porque a decisao do cabo de guerra e tomada no olho: o jogador compara o
    quanto o bicho se mexe agora com o quanto ele se mexia ha cinco segundos.
    Media (e nao maximo) de proposito: um unico osso agitado nao faz um peixe
    parecer que esta lutando.
    """
    total = 0.0
    for osso in clipe["bones"]:
        total += max(amplitude(clipe, osso, "rotation", e) for e in range(3))
    return total / len(clipe["bones"]) / clipe["animation_length"]


def conferir_a_escada_de_agitacao(animacoes):
    """A ESCADA QUE CARREGA A MECANICA DO MOB.

    Nadar < debater, e cansado << debater. Os dois degraus sao a unica maneira que
    o jogador tem de saber em que ponto do cabo de guerra ele esta -- o servidor
    nao mostra barra nenhuma.

    O degrau de baixo e o caro: se `caught` nao for visivelmente mais calmo que
    `thrash`, o jogador continua acompanhando o peixe depois dos 200 ticks e nunca
    recolhe. O premio do mob e a captura, e ela simplesmente nao acontece.
    """
    a = {c: agitacao(animacoes["animation.%s.%s" % (MOB, c)]) for c in CLIPES}
    if a["thrash"] < a["swim"] * RAZAO_DEBATE_SOBRE_NADO:
        raise SystemExit(
            "o debate agita %.1f graus/s e o nado %.1f (%.2fx), e o minimo e"
            " %.2fx. Um peixe fisgado que se mexa como um peixe passando nao"
            " parece custoso de segurar, e 'puxar arrebenta a linha' vira uma"
            " regra que so existe no servidor."
            % (a["thrash"], a["swim"], a["thrash"] / a["swim"],
               RAZAO_DEBATE_SOBRE_NADO))
    if a["thrash"] < a["caught"] * RAZAO_DEBATE_SOBRE_CANSADO:
        raise SystemExit(
            "o debate agita %.1f graus/s e o cansado %.1f (%.2fx), e o minimo e"
            " %.2fx. Sem essa queda, o jogador nao tem como ver que os 200 ticks"
            " passaram -- ele continua acompanhando o peixe e nunca recolhe, e a"
            " captura, que E o premio deste mob, nao acontece."
            % (a["thrash"], a["caught"], a["thrash"] / a["caught"],
               RAZAO_DEBATE_SOBRE_CANSADO))
    if a["escape"] <= a["swim"]:
        raise SystemExit(
            "a fuga agita %.1f graus/s e o nado %.1f: a arrancada e mais mansa que"
            " o cruzeiro. O clipe que diz 'voce perdeu o peixe' tem de ser o"
            " gesto mais forte dos 16 ticks dele." % (a["escape"], a["swim"]))


def conferir_que_cansado_nao_le_como_morte(animacoes):
    """O portao de DOIS LADOS de `caught`, e ele e o mais util do arquivo.

    O outro degrau da escada ja garante que cansado e mais calmo que o debate.
    Este garante que ele nao e calmo DEMAIS -- porque o fundo do poco nao e
    "manso", e "morto", e um peixe morto nao convida ninguem a recolher a linha.

    Duas exigencias, e as duas falham caladas:
      (a) o clipe tem de se mexer: agitacao acima de um minimo;
      (b) a boca tem de ficar ENTREABERTA o clipe inteiro -- e nao escancarada
          como numa bocada. Bicho exausto bombeia agua; bicho morto nao.
    """
    clipe = animacoes["animation.%s.caught" % MOB]
    ritmo = agitacao(clipe)
    if ritmo < AGITACAO_MINIMA_DE_VIVO:
        raise SystemExit(
            "cansado agita %.2f graus/s, abaixo do minimo de %.2f. Esse e o peixe"
            " MORTO: parado, ele nao le como 'agora da para pegar', le como 'ja"
            " acabou' -- e ninguem recolhe a linha de um bicho que ja acabou."
            % (ritmo, AGITACAO_MINIMA_DE_VIVO))
    boca = [v[0] for v in clipe["bones"]["jaw"]["rotation"].values()]
    if max(boca) > -ABERTURA_MINIMA_CANSADA:
        raise SystemExit(
            "no clipe de cansado a boca chega a %.1f graus (fechada). Ela tem de"
            " ficar entreaberta o tempo todo: a respiracao e o que separa exausto"
            " de morto, e e ela que diz ao jogador que ainda ha peixe para pegar."
            % max(boca))
    if min(boca) < BOCA_ESCANCARADA * 0.6:
        raise SystemExit(
            "no clipe de cansado a boca chega a %.1f graus, quase a abertura da"
            " BOCADA (%.1f). Boca escancarada le como ataque, nao como exaustao."
            % (min(boca), BOCA_ESCANCARADA))


def conferir_que_o_debate_repete_na_luta(animacoes):
    """O debate tem de REPETIR varias vezes dentro dos 200 ticks de cansaco.

    A luta dura `RegrasDeFisgada.ticksParaCansar`. Um clipe tao longo que rodasse
    uma ou duas vezes nesse intervalo nao leria como "ele esta se debatendo": cada
    gesto apareceria uma vez so, e o jogador leria como uma animacao de estado, e
    nao como esforco continuo. Se o servidor mudar os 200 ticks, isto reprova.
    """
    dur = animacoes["animation.%s.thrash" % MOB]["animation_length"]
    voltas = DUR_LUTA / dur
    if voltas < MINIMO_DE_VOLTAS_NA_LUTA:
        raise SystemExit(
            "o debate dura %.2fs e a luta dura %.2fs (%d ticks): so %.1f voltas, e"
            " o minimo e %.1f. Com tao poucas repeticoes, cada gesto aparece uma"
            " vez e o clipe le como pose de estado em vez de esforco."
            % (dur, DUR_LUTA, TICKS_PARA_CANSAR, voltas, MINIMO_DE_VOLTAS_NA_LUTA))


def conferir_que_o_nado_e_pesado(animacoes):
    """Peso e ANDAMENTO: o nado tem de ser o mais lento dos que se mexem.

    "Tem de parecer PESADO -- ele e enorme" e a unica instrucao de leitura que a
    ficha da para o `swim`, e ela nao mora numa pose: mora no periodo. O instinto
    errado e acelerar a batida para o bicho "ter vida" -- e um peixe de 2,4 blocos
    batendo cauda depressa le como peixe pequeno visto de perto.
    """
    if not CICLO_NADO > max(CICLO_CURVA, DUR_THRASH):
        raise SystemExit(
            "o nado leva %.2fs por ciclo, e curva (%.2fs) ou debate (%.2fs) sao"
            " mais lentos. O bicho mais pesado do arquivo tem de ser o que se move"
            " mais devagar quando esta so passando."
            % (CICLO_NADO, CICLO_CURVA, DUR_THRASH))
    a = {c: agitacao(animacoes["animation.%s.%s" % (MOB, c)]) for c in CLIPES}
    mais_mansos = {c: v for c, v in a.items()
                   if c not in ("swim", "caught") and v <= a["swim"]}
    if mais_mansos:
        raise SystemExit(
            "o nado agita %.1f graus/s e estes clipes agitam tanto ou menos: %s."
            " Nadar e o REPOUSO deste bicho: qualquer coisa que ele faca de"
            " proposito tem de se mexer mais do que ele passando."
            % (a["swim"], {c: round(v, 1) for c, v in mais_mansos.items()}))


def conferir_que_as_pernas_aparecem(animacoes):
    """QUATRO PERNAS E QUATRO GARRAS -- ou e um peixe com ossos a mais.

    "multiplas pernas / crustacean-like" e metade da ficha, e ela nao existe no
    comportamento: o servidor nao tem nada de crustaceo. Ela existe SO na silhueta
    e no movimento. Se nenhum clipe mexer nas pernas, elas viram cubos colados na
    barriga e o mob some dentro da categoria "peixe grande".

    E no DEBATE elas tem de se mexer especificamente, porque e o clipe que o
    jogador olha por dez segundos seguidos.
    """
    mexe = set()
    for clipe in animacoes.values():
        for osso, canais in clipe["bones"].items():
            if any(v != NEUTRO[c]() for c, q in canais.items() for v in q.values()):
                mexe.add(osso)
    faltando = sorted((set(PERNAS) | set(GARRA.values())) - mexe)
    if faltando:
        raise SystemExit(
            "nenhum clipe mexe em %s. Sao eles que carregam o 'multiplas pernas /"
            " crustacean-like' da ficha -- parados, o mob e um peixe com ossos a"
            " mais." % faltando)
    debate = animacoes["animation.%s.thrash" % MOB]["bones"]
    paradas = sorted(p for p in PERNAS
                     if len(set(tuple(v) for v in
                                debate.get(p, {}).get("rotation", {}).values())) < 2)
    if paradas:
        raise SystemExit(
            "no debate estas pernas nao se mexem: %s. E o clipe que o jogador olha"
            " por dez segundos seguidos; perna parada ali e perna parada sempre."
            % paradas)


def percurso(clipe, osso, canal, de, ate, passo=0.02):
    """Quanto o osso ANDA no intervalo -- soma dos deslocamentos, nao amplitude.

    Amplitude nao distingue "varreu 60 graus e parou" de "varreu 60 graus o tempo
    todo": as duas medem 60. Percurso distingue, e e por isso que o portao abaixo
    mede isto e nao aquilo.
    """
    quadros = clipe["bones"].get(osso, {}).get(canal, {})
    if not quadros:
        return 0.0
    n = max(1, int(round((ate - de) / passo)))
    andado = 0.0
    anterior = valor_em(quadros, de, NEUTRO[canal]())
    for i in range(1, n + 1):
        atual = valor_em(quadros, de + (ate - de) * i / n, NEUTRO[canal]())
        andado += sum(abs(a - b) for a, b in zip(atual, anterior))
        anterior = atual
    return andado


def conferir_que_nenhum_loop_tem_pedaco_morto(animacoes):
    """UM CLIPE QUE REPETE NAO PODE TER UM QUARTO PARADO.

    ESTE PORTAO JA PEGOU UM BUG DE VERDADE, e por isso a mensagem dele e longa: o
    debate saiu da primeira versao com os harmonicos cancelando na segunda metade,
    e a cauda varria 60 graus e depois ficava 0,6s dentro de 6 graus. Oito vezes
    por luta, o peixe se debatia e desistia -- exatamente a leitura oposta a que o
    clipe existe para produzir.

    Nada mais via: a amplitude total estava certa (o portao da escada de agitacao
    passava), o loop fechava, a costura com a fuga fechava. O defeito estava na
    DISTRIBUICAO do movimento no tempo, e nenhuma medida de pose enxerga isso.

    Mede PERCURSO por quarto, e nao amplitude: "varreu e parou" e "varreu o tempo
    todo" tem a mesma amplitude. E vale tambem para as cossenoides do nado e da
    curva de graca -- uma cossenoide anda a mesma distancia em cada quarto, entao
    o portao nao cobra nada delas que elas ja nao cumpram. Quem for reprovado aqui
    escreveu um clipe com uma regiao morta, e nao um clipe suave.
    """
    for clipe in CLIPES:
        if LOOPS[clipe] is not True:
            continue
        animacao = animacoes["animation.%s.%s" % (MOB, clipe)]
        fim = animacao["animation_length"]
        for osso in OSSOS_QUE_CARREGAM:
            total = percurso(animacao, osso, "rotation", 0.0, fim)
            # CONGELADO CONTA COMO MORTO, e este ramo tambem ja pegou um bug: a
            # `turn` nascera fixando a mandibula na pose do nado. Um osso que o
            # clipe CITA e nao move nao volta ao default do modelo -- ele fica
            # preso -- entao o peixe respirava nadando e parava de respirar ao
            # virar. Ninguem chamaria isso de bug; so estranharia o bicho.
            if total <= 0.0:
                raise SystemExit(
                    "em '%s', o osso '%s' esta CITADO e PARADO o clipe inteiro."
                    " Osso citado nao volta ao default do modelo: ele congela. Num"
                    " clipe que repete, isso e um pedaco do bicho que morre"
                    " enquanto o resto se mexe." % (clipe, osso))
            justo = total / 4.0
            for q in range(4):
                andado = percurso(animacao, osso, "rotation",
                                  fim * q / 4.0, fim * (q + 1) / 4.0)
                if andado < justo * FRACAO_MINIMA_DO_PERCURSO:
                    raise SystemExit(
                        "em '%s', o osso '%s' anda %.1f graus no %do quarto do"
                        " clipe e %.1f no clipe inteiro -- %.0f%% da parte justa,"
                        " e o minimo e %.0f%%. Esse quarto e um PEDACO MORTO: o"
                        " clipe repete para sempre, entao o bicho para na mesma"
                        " hora toda vez. Amplitude nao ve isso, e nenhum outro"
                        " portao daqui ve."
                        % (clipe, osso, andado, q + 1, total,
                           andado / justo * 100.0,
                           FRACAO_MINIMA_DO_PERCURSO * 100.0))


def conferir_o_loop_fecha(animacoes):
    """Clipe que repete tem de TERMINAR onde comecou, canal a canal.

    Um loop cujas pontas nao batem da um tranco a cada volta -- oito vezes por
    luta, para sempre. E o tipo de defeito que ninguem reporta porque ninguem
    consegue descrever: "tem alguma coisa estranha no peixe".
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
    """A cadeia de transicoes, em pares (de, t_de, para, t_para, por que).

    O QUE NAO ESTA AQUI TAMBEM E DECISAO. Duas transicoes ficam de fora de
    proposito, e as duas seriam PIORES costuradas:

      bite(fim) -> thrash : e o instante em que o anzol pega. O tranco da
                            transicao E o tranco da fisgada; costurar isso
                            produziria um peixe que passa de morder a se debater
                            sem nada acontecer entre as duas coisas.
      thrash -> caught    : e o bicho AFROUXANDO. Uma mistura de 4 ticks e
                            exatamente o que "ir ficando mole" parece. Costurado
                            no quadro exato, ele desliga a luta como um
                            interruptor, e o jogador perde a unica pista de que o
                            cansaco e gradual.

    Costura nao e virtude por si: ela existe para a transicao nao COMER um clipe
    curto. Onde a transicao e o proprio gesto, costurar apaga o gesto.
    """
    def clipe(nome):
        return animacoes["animation.%s.%s" % (MOB, nome)]

    return (
        (clipe("swim"), 0.0, clipe("bite"), 0.0,
         "ele comete a bocada a partir do cruzeiro"),
        (clipe("bite"), DUR_BITE, clipe("swim"), 0.0,
         "e volta a nadar"),
        (clipe("swim"), 0.0, clipe("turn"), 0.0,
         "a curva comeca do cruzeiro"),
        (clipe("thrash"), 0.0, clipe("escape"), 0.0,
         "a linha arrebenta no meio do debate -- e a fuga tem 16 ticks para ser"
         " lida inteira"),
    )


def conferir_a_costura(animacoes):
    """A COSTURA E A FRONTEIRA MAIS PERIGOSA DESTA ENTREGA.

    O controller e um so, com transicao de poucos ticks, e a fuga inteira dura 16.
    Se as poses vizinhas forem iguais, a mistura vira um no-op e a troca acontece
    no quadro exato. Se nao forem, um quarto da fuga e gasto interpolando -- e o
    jogador que acabou de perder o peixe ve uma mistura no lugar da arrancada.

    Compara o VALOR interpolado, osso a osso, canal a canal, com os ossos ausentes
    valendo o default do MODELO -- que e o que o GeckoLib faz de fato.
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
    proposito -- um grau num osso -- e o portao TEM de recusar.
    """
    estragado = json.loads(json.dumps(animacoes))
    clipe = estragado["animation.%s.escape" % MOB]
    chave = tempo(0.0)
    original = clipe["bones"]["tail"]["rotation"][chave]
    clipe["bones"]["tail"]["rotation"][chave] = [original[0], original[1] + 1.0,
                                                 original[2]]
    try:
        conferir_a_costura(estragado)
    except SystemExit:
        return
    raise SystemExit(
        "a costura ACEITOU uma fuga deslocada de 1 grau. O portao nao esta"
        " protegendo nada: revise conferir_a_costura e quadro_em.")


def conferir_que_a_escada_reprova(animacoes):
    """ALIMENTA o portao da escada com um `caught` agitado.

    O degrau entre debater e cansar e a mecanica inteira do mob, e ele e um
    numero: se a regua que o guarda parar de morder, ninguem percebe -- os outros
    quinze portoes continuam verdes, porque cada um confere uma coisa que continua
    certa.
    """
    estragado = json.loads(json.dumps(animacoes))
    clipe = estragado["animation.%s.caught" % MOB]
    for canais in clipe["bones"].values():
        for canal, quadros in canais.items():
            if canal != "rotation":
                continue
            for chave in quadros:
                quadros[chave] = [v * 12.0 for v in quadros[chave]]
    try:
        conferir_a_escada_de_agitacao(estragado)
    except SystemExit:
        return
    raise SystemExit(
        "a escada ACEITOU um peixe 'cansado' se debatendo doze vezes mais forte."
        " O portao nao esta protegendo nada: revise agitacao e"
        " conferir_a_escada_de_agitacao.")


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
            canais[canal] = dict(sorted(quadros.items(),
                                        key=lambda kv: float(kv[0])))
    clipe["bones"] = dict(sorted(clipe["bones"].items(),
                                 key=lambda kv: ANIMAVEIS.index(kv[0])))
    return clipe


def escrever(animacoes):
    # SENTIDO DE Z. Este mob e um dos sete primeiros e nao passa por
    # `Animacoes.emitir`, onde a correcao mora para os demais -- mas a premissa
    # invertida era a MESMA, copiada de arquivo em arquivo. Chamar a funcao da
    # biblioteca em vez de repetir a negacao aqui e o que impede as duas copias
    # de divergirem no dia em que o sinal mudar.
    corrigir_sentido_de_z_em("master_of_the_swamp", animacoes)
    destino = os.path.join(DIR_ANIM, MOB + ".animation.json")
    os.makedirs(DIR_ANIM, exist_ok=True)
    texto = serializar({"format_version": "1.8.0", "animations": animacoes})
    json.loads(texto)  # o que sai daqui tem de voltar como JSON, sempre.
    with open(destino, "w", encoding="utf-8", newline="\n") as saida:
        saida.write(texto + "\n")
    return destino


def tabela(animacoes):
    print("\n  %-40s %7s  %-19s %s" % ("clipe", "dur", "loop", "ossos"))
    for clipe in CLIPES:
        a = animacoes["animation.%s.%s" % (MOB, clipe)]
        print("  %-40s %6.2fs  %-19s %s"
              % ("animation.%s.%s" % (MOB, clipe), a["animation_length"],
                 str(a["loop"]), ", ".join(sorted(a["bones"]))))


# ------------------------------------------------------------------- main


def main():
    geometria = carregar_geo()

    # Os portoes de GEOMETRIA vem primeiro, antes de qualquer clipe existir: eles
    # conferem as premissas de que todas as poses abaixo dependem, e falhar aqui
    # produz uma mensagem sobre o MODELO em vez de uma sobre a animacao.
    conferir_a_hierarquia(geometria)
    conferir_eixos(geometria)
    conferir_a_simetria(geometria)
    conferir_que_o_peixe_e_enorme(geometria)
    conferir_que_as_pernas_recolhem_sem_atravessar_o_corpo(geometria)

    # A ORDEM DE CONSTRUCAO E A CADEIA DE COSTURAS. Os vizinhos leem o quadro um
    # do outro em vez de repetir a pose: e o que impede duas fontes para a mesma
    # verdade num lugar onde a divergencia custa um tranco de um quadro.
    clipe_swim = ordenar(swim())
    hub = quadro_em(clipe_swim, 0.0)
    clipe_thrash = ordenar(thrash())

    animacoes = {
        "animation.%s.swim" % MOB: clipe_swim,
        "animation.%s.turn" % MOB: ordenar(turn(hub)),
        "animation.%s.bite" % MOB: ordenar(bite(hub)),
        "animation.%s.thrash" % MOB: clipe_thrash,
        "animation.%s.caught" % MOB: ordenar(caught()),
        "animation.%s.escape" % MOB: ordenar(
            escape(quadro_em(clipe_thrash, 0.0))),
    }

    conferir_clipes(animacoes)
    conferir_os_loops_do_contrato(animacoes)
    conferir_ossos(animacoes, geometria)
    conferir_ossos_com_volume(animacoes, geometria)
    conferir_que_so_translada_o_declarado(animacoes)
    conferir_que_ninguem_sai_da_caixa(animacoes)
    conferir_as_duracoes_do_servidor(animacoes)
    conferir_que_a_bocada_acontece_na_janela(animacoes)
    conferir_que_o_aviso_da_bocada_e_lento(animacoes)
    conferir_que_a_onda_cresce_para_tras(animacoes)
    conferir_que_a_curva_arqueia_o_corpo(animacoes)
    conferir_que_o_debate_repete_na_luta(animacoes)
    conferir_que_o_nado_e_pesado(animacoes)
    conferir_a_escada_de_agitacao(animacoes)
    conferir_que_cansado_nao_le_como_morte(animacoes)
    conferir_que_as_pernas_aparecem(animacoes)
    conferir_que_nenhum_loop_tem_pedaco_morto(animacoes)
    conferir_o_loop_fecha(animacoes)
    conferir_a_costura(animacoes)
    conferir_que_a_costura_reprova(animacoes)
    conferir_que_a_escada_reprova(animacoes)

    print("escrito", escrever(animacoes))

    # A ESCADA E IMPRESSA PARA SER OLHADA. Os portoes exigem as razoes minimas;
    # nenhum deles prova que o jogador LE os degraus na tela. Essa parte segue
    # humana -- ver o relato e docs/testing/o-que-nao-provamos.md.
    print("\nagitacao por clipe (graus/s medios, por osso movido):")
    for clipe in CLIPES:
        a = animacoes["animation.%s.%s" % (MOB, clipe)]
        print("  %-8s %7.1f" % (clipe, agitacao(a)))
    a = {c: agitacao(animacoes["animation.%s.%s" % (MOB, c)]) for c in CLIPES}
    print("  debate sobre nado    : %.2fx  (minimo %.2fx)"
          % (a["thrash"] / a["swim"], RAZAO_DEBATE_SOBRE_NADO))
    print("  debate sobre cansado : %.2fx  (minimo %.2fx)  <- e este que decide"
          " se a captura acontece"
          % (a["thrash"] / a["caught"], RAZAO_DEBATE_SOBRE_CANSADO))
    print("\na onda do nado, em graus varridos:")
    for osso in ("body", "tail", "tail_fin"):
        print("  %-9s %5.1f" % (osso, amplitude(clipe_swim, osso, "rotation", 1)))
    print("\na luta: %d ticks / %.2fs de debate = %.1f voltas"
          % (TICKS_PARA_CANSAR, DUR_THRASH, DUR_LUTA / DUR_THRASH))
    tabela(animacoes)


if __name__ == "__main__":
    main()
