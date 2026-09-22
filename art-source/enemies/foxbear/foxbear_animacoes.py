"""Gera os DOZE clipes do foxbear (GeckoLib 4.8.3, Bedrock 1.8.0).

DECISAO QUE ESTE ARQUIVO CARREGA (ADR-017): o foxbear e O ULTIMO nome da divida
visual. Great Stamp, Frog-In-Waiting, Man-faced Ape e Spider Eagle ja trocaram o
corpo emprestado por um proprio; aqui o emprestimo era a GEOMETRIA DO URSO-POLAR,
e com ela vinha o esqueleto do `PolarBearModel` e as animacoes vanilla.

O comportamento nao muda uma linha. `FoxbearTerritory` continua decidindo tudo no
servidor, e os gametests que provam o territorio continuam provando a mesma coisa.
O que muda e o CORPO -- e neste mob o corpo carrega a unica coisa que o servidor
nao consegue dizer.

O QUE ESTE MOB ENSINA, E POR QUE ISSO E ANIMACAO E NAO CODIGO
--------------------------------------------------------------
Le `FoxbearTerritory.stateFor` e a maquina inteira cabe em quatro linhas:

    avancou  -> ENGAGE      (ele vem)
    recuou   -> RETURN_HOME (ele desiste)
    nem um nem outro -> WARN

Ou seja: o foxbear pune QUEM SE APROXIMA e poupa QUEM RECUA. Essa e uma regra
justa e completamente invisivel. O jogador nao le `stateFor`; ele olha para um
urso a doze blocos e decide. Se o aviso nao ler como aviso, a regra continua
certa no servidor e injusta na tela -- e nenhum portao deste repositorio acusa,
porque todos eles conferem o servidor.

Daqui sai a decisao que organiza os doze clipes:

    O AVISO TEM DOIS DEGRAUS, E O SEGUNDO TEM DE SER VISIVEL DE DOZE BLOCOS.

`warn` e o aviso de perto: corpo baixo, orelha para tras, rosnado, pata batendo
no chao. `rear_warn` e a escalada: o bicho ERGUE-SE nas traseiras e fica mais
alto do que em qualquer outro momento da vida dele. O primeiro fala com quem ja
esta perto; o segundo fala com quem ainda esta na borda do territorio -- e por
isso ele e medido em GRAUS DE ALTURA APARENTE a 12 blocos, nao em gosto
(`conferir_que_a_escalada_le_de_doze_blocos`).

O NUMERO 80 E DO SERVIDOR
--------------------------
`FoxbearEntity.TICKS_MAXIMOS_DE_AVISO` = 80. E a janela inteira do aviso: depois
dela o bicho desiste. Os dois clipes de aviso repetem, e o comprimento deles
DIVIDE 80 ticks em numero inteiro de voltas -- senao a ultima volta corta no
meio e o aviso termina com um tranco no quadro exato em que o foxbear estava
decidindo se vinha. `conferir_que_o_aviso_cabe_na_janela_do_servidor` reprova.

EIXOS -- CONFERIDOS no proprio geo, nao chutados
-------------------------------------------------
A convencao vanilla e y=0 no chao, -Z na FRENTE, +X a ESQUERDA do bicho.
`conferir_eixos` le o geo e reprova se a CABECA nao estiver a frente e a CAUDA
atras -- que e exatamente como o contrato manda descobrir a frente deste mob.
Derivado dela, e so dela:

    rotacao X positiva  -> a massa em -Z (o peito, a cabeca) SOBE; e o que
                           ERGUE O BICHO NAS TRASEIRAS;
                           a massa em +Z (a cauda) DESCE;
                           perna pendurada (massa em -Y) balanca para a FRENTE
    rotacao X negativa  -> o peito baixa; a MANDIBULA ABRE (a massa dela esta
                           a frente da propria dobradica)
    rotacao Z positiva  -> o lado esquerdo (+X) SOBE; e o rolamento da morte
    posicao  -Z         -> projeta para a FRENTE (o pescoco no aviso)
    posicao  -Y         -> AGACHA

O EIXO Y E DEDUZIDO, NAO OBSERVADO -- e a unica suposicao desta lane, herdada da
spider_eagle e pelo mesmo caminho: X e Z foram confirmados em jogo nos mobs
irmaos, os dois concordam com a mesma MAO (regra da mao direita, ciclo X:(Y,Z),
Y:(Z,X), Z:(X,Y)), e dois eixos fixam o terceiro. Logo:

    rotacao Y positiva  -> a ORELHA ESQUERDA (+X) gira para a FRENTE (-Z)

Tudo que escreve Y aqui e ESPELHADO entre os dois lados (esquerda +v, direita
-v). Se em jogo a orelha virar para o lado errado, `SENTIDO_DE_Y = -1.0` inverte
tudo num lugar so. Isso esta no relato como ponto cego declarado.

O QUE E LIDO DO GEO, E POR QUE NAO PODE SER DECORADO
------------------------------------------------------
Quatro numeros desta animacao NAO sao escolha de gosto -- eles sao consequencia
do modelo, e decorados sobrevivem a proxima correcao do geo em silencio:

  - `focinho_ate_o_chao`  quanto pescoco e cabeca tem de descer para o FOCINHO
                          chegar perto do chao no `idle_sniff`. Achado por
                          bisseccao sobre a cinematica de verdade, nao por
                          trigonometria de guardanapo. Um angulo decorado vira
                          "o bicho olhando para baixo", que nao e farejar.
  - `assentar`            quanto o corpo sobe ou desce para a pose ENCOSTAR no
                          chao. Uma funcao, tres usos: o bicho erguido nas
                          traseiras, o bicho deitado dormindo e o bicho tombado
                          morto. Escritos a mao, os tres ficariam certos hoje e
                          um deles boiaria (ou afundaria) na primeira correcao.
  - `alcance_da_garra`    ate onde a pata chega no `claw_swipe`, em px, contra a
                          FRENTE DA HITBOX. Um golpe que nao sai da silhueta nao
                          le como golpe.
  - `altura_do_clipe`     o topo da silhueta de cada clipe, medido no clipe e nao
                          numa pose escrita a parte. E dele que sai a escada
                          dormindo < avisando < parado < ERGUIDO.

Regerar:  python art-source/enemies/foxbear/foxbear_animacoes.py
Exporta:  .../animations/entity/foxbear.animation.json
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

MOB = "foxbear"

# A HIERARQUIA e o contrato, e ela e a UNICA lista de ossos deste arquivo: o
# conjunto de ossos sai daqui derivado. Listar os nomes de novo criaria duas
# fontes para a mesma verdade, e a divergencia se manifestaria como um membro
# parado -- sem erro nenhum.
#
# Ela nao e so nomenclatura. `leg_*` ser filho de `body` (e nao de `chest`) e o
# que faz as quatro pernas herdarem o mesmo agachamento; `paw_*` ser filho de
# `leg_*` e o que permite a pata contra-girar e continuar apoiada no chao
# enquanto a perna balanca. Um geo que pendurasse a pata no `body` passaria no
# portao generico do Java (o pai existe) e andaria com a sola virada para cima.
PAI = {
    "root": None,
    "body": "root",
    "chest": "body",
    "neck": "chest",
    "head": "neck",
    "jaw": "head",
    "ear_left": "head",
    "ear_right": "head",
    "leg_front_left": "body",
    "paw_front_left": "leg_front_left",
    "leg_front_right": "body",
    "paw_front_right": "leg_front_right",
    "leg_back_left": "body",
    "paw_back_left": "leg_back_left",
    "leg_back_right": "body",
    "paw_back_right": "leg_back_right",
    "tail": "body",
}
OSSOS = tuple(PAI)

# `root` NAO E ANIMADO, e isso e decisao. Ele e a ancora que o renderer alinha
# com a hitbox; girar ou transladar a raiz move a silhueta inteira para fora da
# caixa de colisao, e num mob cuja promessa e "recue e eu paro" isso desloca a
# leitura de distancia do jogador sem deslocar nada no servidor.
# Quem precisa de peso vai no `body`, que tem o corpo inteiro pendurado.
NAO_ANIMADOS = ("root",)
ANIMAVEIS = tuple(o for o in OSSOS if o not in NAO_ANIMADOS)

PERNAS = tuple(o for o in ANIMAVEIS if o.startswith("leg_"))
PATA = {p: next(o for o in ANIMAVEIS if PAI[o] == p) for p in PERNAS}
ORELHAS = ("ear_left", "ear_right")

# Pares diagonais da marcha cruzada, como em QuadrupedModel: dianteira esquerda
# anda junto com traseira direita.
DIAGONAL_A = ("leg_front_left", "leg_back_right")
DIAGONAL_B = ("leg_front_right", "leg_back_left")

CLIPES = ("idle", "idle_sniff", "walk", "run", "warn", "rear_warn",
          "claw_swipe", "bite", "short_charge", "hurt", "sleep", "death")

# O TIPO DE REPETICAO MORA AQUI E SO AQUI. O codigo pede
# then(nome, Animation.LoopType.DEFAULT), e DEFAULT delega para este campo --
# entao este dicionario E o comportamento, nao a documentacao dele. Cada linha
# foi decidida, nao herdada:
LOOPS = {
    # os tres REPOUSOS do bicho. Repetem enquanto o estado durar.
    "idle": True,
    "idle_sniff": True,
    "sleep": True,
    "walk": True,
    "run": True,
    # OS DOIS AVISOS REPETEM, e isso e o ponto: a janela do servidor e de 80
    # ticks e nenhum dos dois clipes dura tanto. Um aviso que PARASSE de avisar
    # enquanto o foxbear ainda esta avisando seria a pior mentira que este mob
    # consegue contar -- diria "desisti" no meio da unica janela em que recuar
    # ainda resolve.
    "warn": True,
    "rear_warn": True,
    # os tres golpes acontecem UMA vez. Em loop, o urso ficaria estapeando o ar
    # em cadencia de metronomo, que le como bug e nao como ataque.
    "claw_swipe": False,
    "bite": False,
    "short_charge": False,
    "hurt": False,
    # a entidade so some depois do clipe: voltar a pose viva no ultimo quadro e
    # um bug visual de meio segundo, e e o ultimo que o jogador ve.
    "death": "hold_on_last_frame",
}

# ------------------------------------------------------------------- ticks

TICKS_POR_SEGUNDO = 20.0

# COPIADOS DO SERVIDOR. Se mudarem la, mudam aqui e regera.
TICKS_MAXIMOS_DE_AVISO = 80      # FoxbearEntity.TICKS_MAXIMOS_DE_AVISO
RAIO_DO_TERRITORIO = 12.0        # FoxbearTerritory.TERRITORY_RADIUS, em blocos

JANELA_DO_AVISO = TICKS_MAXIMOS_DE_AVISO / TICKS_POR_SEGUNDO  # 4.0 s

DUR_IDLE = 4.0
DUR_SNIFF = 3.0
DUR_WALK = 1.1
DUR_RUN = 0.65
DUR_WARN = 1.0     # 4 voltas na janela de 80 ticks
DUR_REAR = 2.0     # 2 voltas na mesma janela: a escalada e mais lenta
DUR_SWIPE = 0.6
DUR_BITE = 0.4
DUR_CHARGE = 0.9
DUR_HURT = 0.25
DUR_SLEEP = 6.0
DUR_DEATH = 1.5

# -------------------------------------------------------------- a hitbox

# sized(1.4F, 1.35F), em pixels de modelo. Nao e botao de tuning: e a caixa que o
# servidor usa para tudo, e a regua contra a qual a silhueta e julgada.
HITBOX_LARGURA_PX = 1.4 * 16.0    # 22.4
HITBOX_ALTURA_PX = 1.35 * 16.0    # 21.6
# A frente da caixa de colisao, em z. E contra ela que o golpe de garra e medido.
FRENTE_DA_HITBOX = -HITBOX_LARGURA_PX / 2.0
RAIO_EM_PX = RAIO_DO_TERRITORIO * 16.0

# ------------------------------------------------------ eixos e espelhamento

# Ver o cabecalho: o Y e deduzido da mao dos eixos, nao observado em jogo.
SENTIDO_DE_Y = 1.0

# ------------------------------------------------------- poses, em graus

# RESPIRACAO. Um urso parado e um urso VIVO; um urso dormindo respira mais fundo
# e mais devagar. Os dois numeros existem em par de proposito: e o contraste
# entre eles que faz "dormindo" ler como dormindo e nao como parado deitado.
CICLO_RESPIRACAO = 2.0
CICLO_RESPIRACAO_DORMINDO = 3.0
# Os dois folegos sao o CURSO TOTAL do peito, em px -- de cheio a vazio. Medir os
# dois na mesma unidade e o que torna a comparacao do portao uma comparacao.
FOLEGO_PARADO = 0.35
FOLEGO_DORMINDO = 0.9

# MARCHA. A amplitude e a mesma familia do great_stamp -- quadrupede pesado --
# porque e a mesma leitura: passada curta e plantada, nao trote de cavalo.
PASSADA_DA_CAMINHADA = 20.0
PASSADA_DA_CORRIDA = 34.0
# O CORPO NA CORRIDA: baixo e ESTENDIDO. Os dois numeros sao pequenos de
# proposito, e o motivo e mecanico, nao estetico: a perna deste rig e um cubo
# rigido girando em torno do ombro, e tudo que baixa o tronco desce a pata junto.
# Um galope "dramatico" de tres pixels poe as quatro patas dentro do bloco a
# cada passada -- `conferir_que_nada_atravessa_o_chao` mede exatamente isso.
# A leitura de "ele vem" mora no RITMO e na cabeca baixa, nao na altura.
ABAIXAR_NA_CORRIDA = 0.4
INCLINAR_NA_CORRIDA = 4.0
# A PATA CONTRA-GIRA QUASE TUDO, E ISSO E FICHA DO BICHO E NAO AJUSTE FINO.
#
# O great_stamp usa -0.4 no casco, e copiar esse numero para ca foi o primeiro
# reflexo -- e estava errado por dois motivos que se reforcam.
#
# O primeiro e anatomico: urso e PLANTIGRADO. Ele pisa com a sola inteira, como
# uma pessoa; um ungulado pisa na ponta. Uma pata que acompanha a perna so 40% e
# o pe de um cavalo, nao o de um urso.
#
# O segundo e mecanico, e foi a regua que apontou: a pata deste geo tem sete
# pixels de profundidade, com esporao. Inclinada treze graus numa perna que ja
# esta recuada, a PONTA dela entra quase tres pixels dentro do bloco -- as quatro
# patas, a cada passada, o galope inteiro. `conferir_que_nada_atravessa_o_chao`
# reprovou com esse numero antes de qualquer olho humano ver o clipe.
#
# Sobra 15% de acompanhamento, que e o bastante para a sola rolar no fim da
# passada e a passada nao parecer feita de tabuas.
CONTRA_PATA = -0.85

# O AVISO DE PERTO. Corpo BAIXO e tenso: o urso se encolhe antes de crescer, e e
# esse encolhimento que faz o `rear_warn` parecer o dobro do que ele mede.
ABAIXAR_NO_AVISO = 1.2        # px para -Y
CORPO_NO_AVISO = -6.0         # peito adiantado, tensao para a frente
# O PESCOCO QUASE NAO DESCE, E ISSO FOI A GEOMETRIA QUE IMPOS. O reflexo era
# baixar a cabeca (-16 de pescoco), e o reflexo estava errado neste modelo: o
# queixo deste urso ja nasce a seis pixels do chao, e com o corpo agachado por
# cima a boca aberta do rosnado ENTRAVA no bloco -- durante os 80 ticks em que o
# jogador esta olhando o bicho de perto. Quem disse isso foi
# `conferir_que_nada_atravessa_o_chao`, medindo o queixo.
#
# O que o aviso pede e a cabeca PARA A FRENTE, nao para baixo -- e "para a
# frente" mora em `PROJECAO_DO_PESCOCO`, que e translacao e nao custa altura
# nenhuma. O liquido dos dois angulos abaixo e zero de proposito: o focinho
# aponta para o intruso, no nivel dele.
PESCOCO_NO_AVISO = -2.0
CABECA_NO_AVISO = 2.0
PROJECAO_DO_PESCOCO = 2.4     # px para -Z
MANDIBULA_NO_ROSNADO = -18.0  # negativo ABRE (ver eixos)
CAUDA_NO_AVISO = 12.0         # baixa e dura

# A ESCALADA. Ergue-se PARCIALMENTE: 48 graus, nao 90. Um urso de pe de verdade
# e outro mob; o que a diretriz pede e o bicho ganhando altura sobre o jogador,
# e e por isso que o portao que guarda este numero mede ALTURA APARENTE e nao o
# angulo -- o angulo e meio, a altura e o fim.
ERGUER_NAS_TRASEIRAS = 48.0
IMPULSO_DA_ESCALADA = 1.2      # px a mais de agachamento antes de subir
# As traseiras CONTRA-GIRAM quase tudo: perna de urso erguido fica em pe, nao
# sai voando para tras junto com o tronco. O que sobra (6 graus) e a flexao.
FLEXAO_DA_TRASEIRA = 6.0
DIANTEIRAS_NO_AR = 34.0       # recolhidas sob o peito, soltas
PESCOCO_ERGUIDO = -38.0       # desfaz o tronco e sobra olhando para BAIXO
CABECA_ERGUIDA = -16.0        # liquido -6 com o tronco a +48: encara de cima
MANDIBULA_ERGUIDA = -28.0

# A PATA QUE BATE NO CHAO -- a diretriz cita este gesto nominalmente.
# Ele so le como PANCADA se a descida for muito mais rapida que a subida; subir
# e descer no mesmo tempo e um PASSO, e passo nao avisa nada.
# `conferir_que_a_pata_BATE` guarda os dois lados.
LEVANTAR_A_PATA = 30.0
SUBIDA_DA_PATA = 0.34         # s -- devagar, ostensivo
DESCIDA_DA_PATA = 0.08        # s -- seco

# ORELHAS. Elas sao metade do vocabulario deste bicho e nao custam nada de
# geometria: para tras e ameaca, giradas e atencao, caidas e sono.
ORELHA_PARA_TRAS = 46.0       # magnitude; o sinal mora em `pose_das_orelhas`
ORELHA_ATENTA = 14.0
ORELHA_CAIDA = 40.0

# O GOLPE DE GARRA. Sobe alto e desce em arco. O `alcance_da_garra` mede o que
# estes dois numeros produzem em px -- eles nao sao a promessa, sao a tentativa.
GARRA_NO_ALTO = 80.0
GARRA_NA_PANCADA = 58.0        # o quadro que `alcance_da_garra` mede
# O ARCO NAO TERMINA NO AR NEM ATRAVESSANDO O CHAO: ele TERMINA PLANTADO, e o
# angulo em que a pata encosta e lido (`pernas_que_agacham`). A primeira versao
# levava a garra a -22 graus, varrendo por baixo da linha do chao -- e o portao
# mediu a pata dois pixels dentro do bloco no meio do golpe. Corrigir isso com
# um angulo menor teria sido adivinhar; o que resolve e dizer o que o gesto E:
# o urso desce a pata e ela POUSA. Um golpe que acaba plantado tambem le melhor
# -- e o peso caindo, e nao o braco parando no vazio.
TORCAO_DO_GOLPE = 12.0        # o tronco acompanha o braco; sem isso e so a perna

# A MORDIDA. Curta e seca: recua um pouco, abre, fecha, volta.
MANDIBULA_ABERTA = -36.0
AVANCO_DA_MORDIDA = 2.6       # px para -Z, no pescoco
MERGULHO_DA_MORDIDA = 0.6     # px a mais de agachamento no instante da dentada
INCLINACAO_DA_MORDIDA = 11.0

# A INVESTIDA CURTA. Agacha, toma impulso e corre -- e termina EXATAMENTE no
# primeiro quadro do `run`, porque e nele que ela desemboca.
AGACHAR_NA_INVESTIDA = 3.2
RECUO_DA_INVESTIDA = 1.6      # px para +Z: ele junta o corpo antes de soltar
CICLO_DA_INVESTIDA = 0.55     # passada mais curta que a do `run`: e um arranque

# O SONO. Deitado, sem forca nenhuma -- e "deitado" e uma medida, nao uma pose.
#
# A primeira versao desta lane escreveu os angulos a mao (82 graus na dianteira,
# -74 na traseira) e a escada de silhueta reprovou: o urso "deitado" media 0,85
# da altura do urso EM PE. O motivo e invisivel no arquivo -- a perna dobrada
# continuava sendo o ponto mais baixo do modelo, entao `assentar` apoiava o bicho
# NA PERNA e a barriga ficava boiando tres pixels acima do chao. Na tela: um urso
# agachado, nao um urso dormindo.
#
# Entao a dobra e PROCURADA: ela e a menor que poe a perna acima da barriga, que
# e o que "deitar sobre a perna" quer dizer. E o que encosta no chao passa a ser
# a barriga, que e quem tem de encostar. `conferir_que_o_sono_deita_de_verdade`
# guarda essa frase depois.
FOLGA_DA_BARRIGA_NO_CHAO = 1.5

# O FAREJO NAO E SO PESCOCO, E ISSO FOI A GEOMETRIA QUE DISSE.
#
# A primeira versao desta lane abaixava so pescoco e cabeca. A regua respondeu:
# com o pescoco no maximo, o focinho para a 4,6 px do chao -- porque neste modelo
# a perna tem 11 px e o pescoco inteiro alcanca 10,3. O bicho ficava "olhando
# para os proprios pes", que e precisamente o gesto errado, e nada no arquivo
# denunciaria isso.
#
# O que resolve e o que um urso de verdade faz: ele BAIXA A FRENTE. O tronco se
# inclina sobre o quadril e as dianteiras se estendem para segurar o peso. Este
# e o unico angulo escolhido do gesto; o resto -- quanto a dianteira precisa
# abrir para a pata continuar no chao, e quanto pescoco e cabeca ainda faltam --
# e LIDO do geo, em `dianteiras_que_seguram` e `focinho_ate_o_chao`.
CURVATURA_DO_FAREJO = 4.0

# ----------------------------------------- limites que os portoes usam

# A ESCADA DE SILHUETA, em fracao da altura de `idle`. Cada degrau tem uma
# consequencia propria, e por isso cada um tem a sua mensagem.
TETO_DO_SONO = 0.78        # dormindo tem de ser MUITO mais baixo que parado
TETO_DO_AVISO = 0.97       # avisando de perto ele se ENCOLHE
PISO_DA_ESCALADA = 1.22    # erguido ele CRESCE, e e disso que o mob vive
# E a escalada nao pode virar outro bicho: um urso que dobra de altura mente
# sobre o tamanho da caixa de colisao o tempo inteiro em que estiver avisando.
TETO_DA_ESCALADA_SOBRE_A_HITBOX = 1.9

# Quanto a escalada precisa CRESCER na tela de quem esta na borda do territorio.
# 12 blocos e o raio que o servidor usa para tudo; um aviso que so se le de tres
# blocos avisa depois que a decisao ja foi tomada.
GRAUS_MINIMOS_DE_ESCALADA = 1.5

# O modelo tem de caber na caixa que o servidor anuncia. Fora desta faixa quem
# reprova e o GEO, nao a animacao -- e a mensagem diz isso.
FAIXA_DA_ALTURA_PARADA = (0.80, 1.15)

# A patada: quanto a pata sobe (px) e quantas vezes mais rapida e a descida.
LEVANTADA_MINIMA_DA_PATA = 2.5
RAZAO_MINIMA_DA_PANCADA = 2.5

# As dianteiras tem de sair do chao de verdade na escalada.
DIANTEIRA_MINIMA_NO_AR = 5.0

# O golpe tem de passar da frente da hitbox -- e nao muito alem dela.
FOLGA_MINIMA_DA_GARRA = 1.0
FOLGA_MAXIMA_DA_GARRA = 12.0

# FAREJAR, EXPRESSO SEM NENHUM PIXEL ESCOLHIDO A MAO.
#
# "Perto do chao" so vira regua quando se diz PERTO EM RELACAO A QUE. A resposta
# que nao envelhece: em relacao a onde o focinho JA ESTA quando o bicho esta
# parado. Ele tem de fechar a maior parte dessa distancia -- e quanto for "a
# maior parte" e o unico numero desta conta, porque todo o resto sai do modelo.
#
# Escrever "o focinho desce ate y=2" seria um px escolhido a mao: no dia em que o
# geo ganhasse uma perna mais curta, o mesmo 2 px passaria a ser o focinho
# enterrado, e nada acusaria.
FRACAO_QUE_O_FOCINHO_FECHA = 0.65
# E, em cima disso, o gesto tem de ser VISIVEL EM PIXELS: a fracao sozinha
# aprovaria um bicho de focinho minusculo fechando 65% de quase nada. A regua
# dessa visibilidade tambem sai do modelo -- a espessura da propria pata, que e a
# menor coisa que o jogador consegue distinguir neste bicho. Se o focinho nao
# desce nem a altura de um pe, nao ha gesto.


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


NEUTRO = {"rotation": vetor, "position": vetor}
CANAIS = tuple(NEUTRO)

# Os UNICOS ossos que algum clipe TRANSLADA. `aplicar` escreve `position` neutro
# so para estes: escrever zeros de posicao nos outros treze seria ruido num
# arquivo cujo unico leitor humano e o diff, e a costura ja trata canal ausente
# como neutro. Quem transladar um osso novo reprova em
# `conferir_que_so_transladam_os_declarados` -- e ai acrescenta o nome AQUI, que
# e o que faz a pose de costura passar a incluir aquele canal.
OSSOS_QUE_TRANSLADAM = ("body", "chest", "neck")

# `scale` NAO E USADA POR NENHUM CLIPE, e isso e afirmado e conferido. A regua de
# silhueta deste arquivo faz cinematica direta sobre os vertices do geo e IGNORA
# escala; um clipe que passasse a escalar um osso continuaria sendo medido como
# se nao escalasse, e todos os portoes de altura ficariam verdes medindo outra
# coisa. `conferir_clipes` reprova quem introduzir o canal.


def curva(bones, osso, canal, pares):
    """Acumula keyframes de um canal de um osso.

    Escreve por CHAVE DE TEMPO, entao uma curva posterior sobrescreve a pose que
    `aplicar` deitou -- que e exatamente a ordem desejada.
    """
    alvo = bones.setdefault(osso, {}).setdefault(canal, {})
    for t, v in pares:
        alvo[tempo(t)] = v


def membro(bones, perna, pares, pata_inicial=None):
    """Keya a perna e DERIVA a pata dela.

    So o eixo X contra-gira: Z carrega o afastamento do par, e contra-girar o
    afastamento viraria a pata para dentro da barriga.

    `pata_inicial` existe para UM caso, e ele e uma costura: quando o clipe comeca
    numa pose em que a pata esta PLANA (o `warn`, onde ela esta apoiada no chao) e
    segue para um gesto em que ela rola como numa passada, o primeiro quadro tem
    de ser o da pose vizinha -- senao a derivacao aqui sobrescreve a costura e a
    pata salta 23 graus no quadro exato da troca. Nao da erro; o portao de loop e
    o de costura e que pegaram isso.
    """
    curva(bones, perna, "rotation", pares)
    patas = [(t, vetor(x=v[0] * CONTRA_PATA)) for t, v in pares]
    if pata_inicial is not None:
        patas[0] = (pares[0][0], list(pata_inicial))
    curva(bones, PATA[perna], "rotation", patas)


# Fase que faz a cossenoide VALER A BASE em t=0. cos(2*pi*0.75) = 0, e a curva
# sobe a partir dai. Qualquer outra fase quebraria as costuras, porque o primeiro
# quadro de um clipe deixaria de ser a pose que o vizinho termina.
FASE_QUE_COMECA_NA_BASE = 0.75


def ciclo(duracao, periodo, amplitude, base=0.0, fase=FASE_QUE_COMECA_NA_BASE,
          amostras=8, ate=None):
    """Cossenoide amostrada -- e a respiracao e a marcha inteiras.

    Amostrar (em vez de escrever os extremos a mao) e o que mantem a fase certa
    quando alguem mexe no periodo, e o que mantem a interpolacao linear do
    formato 1.8.0 parecendo curva em vez de zigue-zague.

    `ate` corta a amostragem antes do fim: os clipes que terminam numa COSTURA
    deixam o ultimo quadro para a pose do vizinho, em vez de disputa-lo.
    """
    passo = periodo / amostras
    n = int(round(duracao / passo))
    pares = [(i * passo,
              base + amplitude * math.cos(2 * math.pi * (i * passo / periodo + fase)))
             for i in range(n + 1)]
    return [(t, v) for t, v in pares if ate is None or t < ate - 1e-9]


def marcha(bones, duracao, periodo, amplitude, fase_por_osso, inicio=0.0,
           amostras=8, ate=None):
    """A marcha cruzada inteira: quatro pernas e quatro patas, de uma vez.

    Espelhar e defasar aqui, num lugar so, e o que impede o erro que nao da erro:
    um par diagonal com a fase trocada produz um quadrupede que anda com as duas
    pernas do mesmo lado juntas -- que le como "bug de animacao" e nao como o
    erro de fase que e.
    """
    passo = periodo / amostras
    n = int(round(duracao / passo))
    for osso, (fase, escala) in fase_por_osso.items():
        pares = []
        for i in range(n + 1):
            t = inicio + i * passo
            if ate is not None and t >= ate - 1e-9:
                continue
            ang = amplitude * escala * math.cos(
                2 * math.pi * (i * passo / periodo + fase))
            pares.append((t, vetor(x=ang)))
        membro(bones, osso, pares)


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
    dois clipes citam, um `short_charge` que esquecesse a orelha casaria com um
    `warn` que a poe para tras -- e a orelha saltaria sozinha no quadro em que o
    urso decide vir.
    """
    pose = {}
    for osso in ANIMAVEIS:
        canais = clipe["bones"].get(osso, {})
        pose[osso] = {c: valor_em(canais.get(c, {}), t, NEUTRO[c]())
                      for c in CANAIS}
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
            if canal == "position" and valor == vetor() \
                    and osso not in OSSOS_QUE_TRANSLADAM:
                continue
            curva(bones, osso, canal, [(t, list(valor))])


# -------------------------------------------------- poses espelhadas


def pose_das_pernas(pose, x_frente, x_tras, abrir=0.0):
    """As quatro pernas de uma vez, com a pata derivada.

    `abrir` afasta o par em Z (as dianteiras plantadas para fora, no aviso). Ele
    e espelhado; escrito a mao, um dos lados sobrevive a proxima correcao com o
    sinal antigo e o urso planta uma pata para fora e a outra para dentro.
    """
    for perna in PERNAS:
        lado = 1.0 if perna.endswith("left") else -1.0
        x = x_frente if "front" in perna else x_tras
        pose[perna]["rotation"] = vetor(x=x, z=abrir * lado)
        pose[PATA[perna]]["rotation"] = vetor(x=x * CONTRA_PATA)
    return pose


def pose_das_orelhas(pose, para_tras=0.0, girar=0.0, cair=0.0, x=0.0):
    """As duas orelhas de uma vez.

    O SINAL DE CADA GESTO MORA AQUI, e so aqui, porque cada um deles e uma frase:

      `para_tras`  ameaca. A orelha esquerda (+X) gira para TRAS, entao -Y.
      `girar`      atencao: uma orelha para um lado, a outra para o outro. E o
                   gesto do `idle_sniff`, e e ele que faz o bicho parecer vivo.
      `cair`       sono. A ponta (massa em +Y) tomba para FORA, entao -Z na
                   esquerda.
    """
    for orelha in ORELHAS:
        lado = 1.0 if orelha.endswith("left") else -1.0
        pose[orelha]["rotation"] = vetor(
            x=x,
            y=(-para_tras + girar) * SENTIDO_DE_Y * lado,
            z=-cair * lado)
    return pose


# ------------------------------------------------------------- as poses-base


def pose_de_aviso(geometria):
    """A pose em que o `warn` ORBITA -- e o primeiro quadro de `rear_warn` e de
    `short_charge`.

    O urso nao volta ao neutro entre avisar, crescer e vir: quando ele decide
    escalar, ja esta baixo, com a orelha para tras e a boca aberta. As tres
    coisas comecam do mesmo lugar porque sao o mesmo episodio.

    O AGACHAMENTO E LIDO. `ABAIXAR_NO_AVISO` diz quantos pixels; quanto a perna
    precisa abrir para aguentar isso sem enterrar a pata sai de
    `pernas_que_agacham`. E a mesma pata PLANA que vai bater no chao no clipe.
    """
    p = pose_neutra()
    p["body"]["rotation"] = vetor(x=CORPO_NO_AVISO)
    p["body"]["position"] = vetor(y=-ABAIXAR_NO_AVISO)
    p["neck"]["rotation"] = vetor(x=PESCOCO_NO_AVISO)
    p["neck"]["position"] = vetor(z=-PROJECAO_DO_PESCOCO)
    p["head"]["rotation"] = vetor(x=CABECA_NO_AVISO)
    p["jaw"]["rotation"] = vetor(x=MANDIBULA_NO_ROSNADO)
    p["tail"]["rotation"] = vetor(x=CAUDA_NO_AVISO)
    pose_das_orelhas(p, para_tras=ORELHA_PARA_TRAS, x=8.0)
    frente, tras = pernas_que_agacham(geometria, ABAIXAR_NO_AVISO,
                                      -CORPO_NO_AVISO, CONTRA_PATA_PLANA)
    for perna in PERNAS:
        lado = 1.0 if perna.endswith("left") else -1.0
        x = frente if "front" in perna else tras
        p[perna]["rotation"] = vetor(x=x, z=6.0 * lado)
        p[PATA[perna]]["rotation"] = vetor(x=num(x * CONTRA_PATA_PLANA))
    return p


def pose_erguida(geometria):
    """O topo da escalada: erguido nas traseiras, encarando de cima.

    A ALTURA DO CORPO E LIDA, NAO ESCRITA. Girar o tronco +48 graus em torno do
    pivot do `body` enterra as patas traseiras no chao -- e um urso enterrado ate
    o joelho nao le como ameaca, le como bug. `assentar` mede quanto falta e
    devolve; decorado, o numero ficaria certo hoje e errado na primeira correcao
    do comprimento da perna.
    """
    p = pose_neutra()
    p["body"]["rotation"] = vetor(x=ERGUER_NAS_TRASEIRAS)
    p["chest"]["rotation"] = vetor(x=6.0)
    p["neck"]["rotation"] = vetor(x=PESCOCO_ERGUIDO)
    p["head"]["rotation"] = vetor(x=CABECA_ERGUIDA)
    p["jaw"]["rotation"] = vetor(x=MANDIBULA_ERGUIDA)
    p["tail"]["rotation"] = vetor(x=-ERGUER_NAS_TRASEIRAS * 0.5)
    pose_das_orelhas(p, para_tras=ORELHA_PARA_TRAS, x=10.0)
    # As traseiras desfazem quase todo o tronco: perna de urso erguido fica em
    # pe. As dianteiras recolhem sob o peito e ficam SOLTAS.
    for perna in PERNAS:
        if "front" in perna:
            p[perna]["rotation"] = vetor(x=DIANTEIRAS_NO_AR)
            p[PATA[perna]]["rotation"] = vetor(x=DIANTEIRAS_NO_AR * CONTRA_PATA)
        else:
            x = -ERGUER_NAS_TRASEIRAS + FLEXAO_DA_TRASEIRA
            p[perna]["rotation"] = vetor(x=x)
            p[PATA[perna]]["rotation"] = vetor(x=x * CONTRA_PATA)
    p["body"]["position"] = vetor(y=assentar(geometria, p))
    return p


def pose_de_farejo(geometria, focinho):
    """A pose-base do `idle_sniff`: frente baixa, dianteiras estendidas, cara
    no chao.

    So UM angulo desta pose foi escolhido (`CURVATURA_DO_FAREJO`). A abertura da
    dianteira vem de `dianteiras_que_seguram`, o pescoco e a cabeca vem de
    `focinho_ate_o_chao`, e o assentamento final vem de `assentar`. Tres numeros
    lidos para um escrito -- e e essa proporcao que faz a pose sobreviver a
    proxima correcao do modelo.
    """
    pescoco, cabeca = focinho
    p = pose_neutra()
    p["body"]["rotation"] = vetor(x=-CURVATURA_DO_FAREJO)
    p["chest"]["rotation"] = vetor(x=-3.0)
    p["neck"]["rotation"] = vetor(x=pescoco)
    p["head"]["rotation"] = vetor(x=cabeca)
    p["tail"]["rotation"] = vetor(x=10.0)
    pose_das_orelhas(p, girar=ORELHA_ATENTA)
    frente, tras = pernas_que_agacham(geometria, 0.0, CURVATURA_DO_FAREJO)
    for perna in PERNAS:
        lado = 1.0 if perna.endswith("left") else -1.0
        x = frente if "front" in perna else tras - 6.0
        p[perna]["rotation"] = vetor(x=num(x), z=4.0 * lado)
        p[PATA[perna]]["rotation"] = vetor(x=num(x * CONTRA_PATA))
    p["body"]["position"] = vetor(y=assentar(geometria, p))
    return p


def pose_de_sono(geometria):
    """Deitado. As quatro pernas dobradas SOB o corpo, barriga no chao, cabeca
    apoiada.

    Nenhum dos tres angulos que definem esta pose esta escrito. A dobra da perna
    sai de `pernas_dobradas_sob_o_corpo`, a queda do corpo sai de
    `chao_da_barriga` e a inclinacao da cabeca sai de `cabeca_apoiada` -- e a
    ORDEM entre os tres e obrigatoria: a cabeca so sabe onde apoiar depois que o
    corpo desceu, e o corpo so sabe quanto descer depois que a perna saiu da
    frente.
    """
    frente, tras = pernas_dobradas_sob_o_corpo(geometria)
    p = pose_neutra()
    pose_das_orelhas(p, cair=ORELHA_CAIDA, x=-10.0)
    pose_das_pernas(p, frente, tras, abrir=4.0)
    # 1. o corpo desce ate a BARRIGA encostar -- e nao ate a perna encostar.
    p["body"]["position"] = vetor(y=num(-chao_da_barriga(geometria, p)))
    # 2. so agora a cabeca sabe onde esta o chao.
    pescoco, cabeca = cabeca_apoiada(geometria, p)
    p["neck"]["rotation"] = vetor(x=pescoco)
    p["head"]["rotation"] = vetor(x=cabeca)
    # 3. e a cauda, que deitada NAO desce: ela pousa atras.
    p["tail"]["rotation"] = vetor(x=cauda_apoiada(geometria, p))
    return p


# ------------------------------------------------------------------- clipes


def idle():
    """Parado, peso nas quatro patas. O estado em que o jogador o ENCONTRA.

    Tres coisas: respiracao no peito, cabeca oscilando POUCO, e a orelha que se
    mexe sozinha de vez em quando. A oscilacao pequena e deliberada -- e contra
    esta silhueta parada que os dois avisos vao ser lidos, e uma idle agitada
    gasta antecipadamente o contraste de que a escalada vive.
    """
    dur = DUR_IDLE
    b = {}
    aplicar(b, pose_neutra(), 0.0)
    aplicar(b, pose_neutra(), dur)

    curva(b, "chest", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, CICLO_RESPIRACAO,
                                             FOLEGO_PARADO / 2.0)])
    curva(b, "chest", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, CICLO_RESPIRACAO, -1.4)])
    curva(b, "body", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, CICLO_RESPIRACAO, 0.15)])
    curva(b, "neck", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, dur, 2.0)])
    # A cabeca varre devagar e VOLTA: uma volta completa por clipe.
    curva(b, "head", "rotation", [
        (0.0, vetor()), (1.1, vetor(x=2.0, y=9.0)), (2.0, vetor(x=-1.0, y=1.0)),
        (2.9, vetor(x=1.5, y=-8.0)), (dur, vetor())])
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (2.3, vetor()), (2.45, vetor(x=-7.0)),
        (2.65, vetor(x=-2.0)), (2.8, vetor()), (dur, vetor())])
    # O tique da orelha: acontece UMA vez, fora do compasso da respiracao. E o
    # detalhe que faz o bicho parecer atento sem parecer agitado.
    for orelha in ORELHAS:
        lado = 1.0 if orelha.endswith("left") else -1.0
        curva(b, orelha, "rotation", [
            (0.0, vetor()), (1.5, vetor()),
            (1.62, vetor(x=-6.0, y=ORELHA_ATENTA * SENTIDO_DE_Y * lado)),
            (1.8, vetor(x=-2.0, y=ORELHA_ATENTA * 0.3 * SENTIDO_DE_Y * lado)),
            (1.95, vetor()), (dur, vetor())])
    # O peso passa devagar de um par de pernas para o outro.
    for perna in PERNAS:
        amplitude = 1.4 if "front" in perna else -1.4
        membro(b, perna, [(t, vetor(x=v)) for t, v in ciclo(dur, dur, amplitude)])
    curva(b, "tail", "rotation",
          [(t, vetor(x=v * 0.4, y=v)) for t, v in ciclo(dur, dur, 6.0)])
    return {"loop": LOOPS["idle"], "animation_length": dur, "bones": b}


def idle_sniff(base, focinho):
    """ELE FAREJA. A variacao que faz o bicho parecer vivo quando ninguem olha.

    O gesto inteiro e UM: o focinho chega PERTO DO CHAO. Nao "a cabeca desce" --
    perto do chao, medido em px, por `conferir_que_o_focinho_chega_ao_chao`. A
    diferenca entre as duas coisas e a diferenca entre um urso farejando e um
    urso olhando para os proprios pes, e ela e invisivel no arquivo: os dois sao
    "neck com X negativo".

    Por isso NENHUM angulo da descida esta escrito aqui. A pose-base inteira vem
    de `pose_de_farejo`, que a mediu no geo -- tronco, dianteiras, pescoco e
    cabeca. Se o modelo mudar de perna ou de pescoco, a pose muda junto.

    Em cima dela, tres coisas pequenas: o focinho VARRE em Y, a mandibula treme
    na cadencia da farejada, e as orelhas GIRAM em oposicao -- o bicho esta
    ouvindo enquanto cheira, e e essa a frase que a orelha diz neste clipe.
    """
    dur = DUR_SNIFF
    pescoco, cabeca = focinho
    b = {}
    aplicar(b, base, 0.0)
    aplicar(b, base, dur)

    # A VARREDURA: o focinho vai para um lado e volta pelo outro, uma volta por
    # clipe. E ela que diz "procurando", em vez de "encontrou".
    curva(b, "head", "rotation", [
        (0.0, vetor(x=cabeca)),
        (0.7, vetor(x=cabeca + 3.0, y=-19.0)),
        (1.5, vetor(x=cabeca - 2.0, y=2.0)),
        (2.3, vetor(x=cabeca + 3.0, y=17.0)),
        (dur, vetor(x=cabeca))])
    # O pescoco sobe e desce pouco: cheirar nao e ficar parado com a cara no chao.
    curva(b, "neck", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, 1.5, 4.0, pescoco)])
    # A farejada propriamente: seis inspiracoes curtas no clipe.
    curva(b, "jaw", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, 0.5, -4.0, -4.0)])
    # AS ORELHAS GIRAM EM OPOSICAO -- uma escuta a frente, a outra atras. E o
    # gesto mais barato deste arquivo e o que mais diz.
    for orelha in ORELHAS:
        lado = 1.0 if orelha.endswith("left") else -1.0
        curva(b, orelha, "rotation", [
            (t, vetor(y=(ORELHA_ATENTA + v) * SENTIDO_DE_Y * lado))
            for t, v in ciclo(dur, 1.5, 16.0 * lado)])
    curva(b, "chest", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, 1.0, 0.2)])
    curva(b, "tail", "rotation",
          [(t, vetor(x=base["tail"]["rotation"][0] + v * 0.3, y=v))
           for t, v in ciclo(dur, dur, 7.0)])
    # As pernas passam o peso de um par para o outro SEM sair da pose-base: o
    # valor de partida e lido dela, e nao reescrito. Reescrito, ele ficaria certo
    # hoje e romperia o assentamento do chao na primeira correcao da curvatura --
    # e o urso fareja com a pata enterrada, sem erro nenhum.
    for perna in PERNAS:
        amplitude = 1.2 if "front" in perna else -1.2
        parte_de = base[perna]["rotation"]
        membro(b, perna, [(t, vetor(x=parte_de[0] + v, z=parte_de[2]))
                          for t, v in ciclo(dur, dur, amplitude)])
    return {"loop": LOOPS["idle_sniff"], "animation_length": dur, "bones": b}


def walk():
    """Marcha cruzada de quadrupede pesado, 1.1s por volta completa.

    Pesado quer dizer: passada curta, corpo subindo pouco e cabeca acompanhando o
    apoio. Um urso que balanca muito le como bicho leve, e leve e a leitura
    errada para o mob que vai investir em cima do jogador.
    """
    dur, periodo = DUR_WALK, DUR_WALK
    b = {}
    aplicar(b, pose_neutra(), 0.0)
    aplicar(b, pose_neutra(), dur)

    marcha(b, dur, periodo, PASSADA_DA_CAMINHADA, {
        DIAGONAL_A[0]: (0.0, 1.0), DIAGONAL_A[1]: (0.0, 0.92),
        DIAGONAL_B[0]: (0.5, 0.92), DIAGONAL_B[1]: (0.5, 1.0),
    })
    # Bob no dobro da frequencia da passada: o corpo sobe a cada apoio.
    curva(b, "body", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, periodo / 2.0, 0.4)])
    curva(b, "body", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, periodo, 1.5)])
    curva(b, "chest", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, periodo / 2.0, 1.2)])
    curva(b, "neck", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, periodo / 2.0, 2.2, -2.0)])
    curva(b, "head", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, periodo / 2.0, 2.5, 1.0)])
    curva(b, "jaw", "rotation", [(0.0, vetor()), (dur, vetor())])
    for orelha in ORELHAS:
        lado = 1.0 if orelha.endswith("left") else -1.0
        curva(b, orelha, "rotation",
              [(t, vetor(x=v, y=ORELHA_ATENTA * 0.4 * SENTIDO_DE_Y * lado))
               for t, v in ciclo(dur, periodo / 2.0, -3.0)])
    curva(b, "tail", "rotation", [
        (0.0, vetor()), (periodo * 0.25, vetor(x=3.0, y=9.0)),
        (periodo * 0.5, vetor()), (periodo * 0.75, vetor(x=3.0, y=-9.0)),
        (dur, vetor())])
    return {"loop": LOOPS["walk"], "animation_length": dur, "bones": b}


def run():
    """Galope curto: corpo ESTENDIDO, cabeca BAIXA. 0.65s por volta.

    O contrato pede as duas coisas nesta ordem, e a ordem importa: e a cabeca
    baixa que diz que ele nao vai desviar. Um urso correndo de cabeca erguida le
    como um urso passando por ali; de cabeca baixa, le como um urso vindo.

    As duas dianteiras batem quase juntas e as traseiras meio ciclo depois -- e
    galope, e nao a marcha cruzada do `walk`. Sao dois andamentos diferentes de
    proposito: o jogador precisa distinguir "ele anda" de "ele vem" pelo RITMO,
    que e o que se le de doze blocos, e nao pela velocidade da entidade, que nao
    se le de lugar nenhum.
    """
    dur, periodo = DUR_RUN, DUR_RUN
    b = {}
    base = pose_neutra()
    base["body"]["position"] = vetor(y=-ABAIXAR_NA_CORRIDA)
    base["body"]["rotation"] = vetor(x=-INCLINAR_NA_CORRIDA)
    base["neck"]["rotation"] = vetor(x=-22.0)
    base["head"]["rotation"] = vetor(x=10.0)
    base["jaw"]["rotation"] = vetor(x=-8.0)
    base["tail"]["rotation"] = vetor(x=-14.0)
    pose_das_orelhas(base, para_tras=ORELHA_PARA_TRAS * 0.7, x=6.0)
    aplicar(b, base, 0.0)
    aplicar(b, base, dur)

    marcha(b, dur, periodo, PASSADA_DA_CORRIDA, {
        "leg_front_left": (0.0, 1.0), "leg_front_right": (0.05, 0.9),
        "leg_back_left": (0.5, 0.92), "leg_back_right": (0.55, 1.0),
    })
    curva(b, "body", "position",
          [(t, vetor(y=-ABAIXAR_NA_CORRIDA + v))
           for t, v in ciclo(dur, periodo / 2.0, 0.55)])
    curva(b, "body", "rotation",
          [(t, vetor(x=-INCLINAR_NA_CORRIDA + v))
           for t, v in ciclo(dur, periodo / 2.0, 1.8)])
    curva(b, "chest", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, periodo / 2.0, 2.0)])
    curva(b, "neck", "rotation",
          [(t, vetor(x=-22.0 + v)) for t, v in ciclo(dur, periodo / 2.0, 3.0)])
    curva(b, "head", "rotation",
          [(t, vetor(x=10.0 + v)) for t, v in ciclo(dur, periodo / 2.0, 3.0, 0.0,
                                                    FASE_QUE_COMECA_NA_BASE + 0.5)])
    curva(b, "tail", "rotation", [
        (0.0, vetor(x=-14.0)), (periodo * 0.25, vetor(x=-16.0, y=8.0)),
        (periodo * 0.5, vetor(x=-14.0)), (periodo * 0.75, vetor(x=-16.0, y=-8.0)),
        (dur, vetor(x=-14.0))])
    return {"loop": LOOPS["run"], "animation_length": dur, "bones": b}


def warn(geometria):
    """O AVISO DE PERTO. 1s em loop, dentro de uma janela de 80 ticks.

    Corpo baixo e tenso, cabeca a frente, orelhas para tras, rosnado -- e A PATA
    QUE BATE NO CHAO, que a diretriz cita nominalmente e que e o unico gesto
    ACUSTICO desta lista mesmo sem som: uma pancada que a silhueta faz.

    O ENCOLHIMENTO E O PONTO. Ele se abaixa aqui para poder crescer em
    `rear_warn`; se o `warn` ja fosse grande, a escalada nao teria de onde subir.
    `conferir_a_escada_de_silhueta` guarda os dois lados dessa conta.
    """
    dur = DUR_WARN
    b = {}
    base = pose_de_aviso(geometria)
    aplicar(b, base, 0.0)
    aplicar(b, base, dur)

    # A tensao respira curto e rapido -- o oposto da idle.
    curva(b, "body", "position",
          [(t, vetor(y=-ABAIXAR_NO_AVISO + v)) for t, v in ciclo(dur, dur / 2.0, 0.3)])
    curva(b, "chest", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, dur / 2.0, -1.8)])
    # O rosnado pulsa: boca que fica parada aberta le como pose, nao como som.
    curva(b, "jaw", "rotation", [
        (0.0, vetor(x=MANDIBULA_NO_ROSNADO)), (0.22, vetor(x=-24.0)),
        (0.45, vetor(x=-13.0)), (0.68, vetor(x=-22.0)),
        (dur, vetor(x=MANDIBULA_NO_ROSNADO))])
    # A cabeca oscila pouco e NAO varre: o aviso e PARA ALGUEM. Uma cabeca
    # varrendo aqui diria ao intruso que a ameaca nao e com ele.
    curva(b, "head", "rotation",
          [(t, vetor(x=CABECA_NO_AVISO + v)) for t, v in ciclo(dur, dur, 3.0)])
    curva(b, "neck", "rotation",
          [(t, vetor(x=PESCOCO_NO_AVISO + v)) for t, v in ciclo(dur, dur, -3.0)])
    curva(b, "neck", "position",
          [(t, vetor(z=-PROJECAO_DO_PESCOCO - v)) for t, v in ciclo(dur, dur, 0.5)])
    # A orelha nao fica parada para tras: ela TRAVA para tras e tenta voltar.
    for orelha in ORELHAS:
        lado = 1.0 if orelha.endswith("left") else -1.0
        curva(b, orelha, "rotation", [
            (t, vetor(x=8.0, y=(-ORELHA_PARA_TRAS + v) * SENTIDO_DE_Y * lado))
            for t, v in ciclo(dur, dur, 6.0)])

    # A PATADA. Sobe devagar (ostensiva) e desce SECA -- e a razao entre os dois
    # tempos que faz a diferenca entre uma pancada e um passo.
    #
    # A PATA FICA PLANA o gesto inteiro (`CONTRA_PATA_PLANA`), e por isso ela e
    # escrita aqui em vez de sair de `membro`. Com a contra-rotacao parcial de uma
    # passada, este geo mal levanta a pata do chao: o calcanhar desce quase tanto
    # quanto a ponta sobe, e a "patada" vira um tremor de um pixel e meio. Foi
    # `conferir_que_a_pata_BATE` que disse isso, medindo.
    subida = SUBIDA_DA_PATA
    batida = subida + DESCIDA_DA_PATA
    apoiada = base["leg_front_right"]["rotation"][0]
    atras = base["leg_back_right"]["rotation"][0]

    def dianteira(perna, pares):
        lado = 1.0 if perna.endswith("left") else -1.0
        curva(b, perna, "rotation",
              [(t, vetor(x=x, z=6.0 * lado)) for t, x in pares])
        curva(b, PATA[perna], "rotation",
              [(t, vetor(x=num(x * CONTRA_PATA_PLANA))) for t, x in pares])

    dianteira("leg_front_right", [
        (0.0, apoiada), (0.06, apoiada),
        (subida, apoiada + LEVANTAR_A_PATA),
        (batida, apoiada),                       # a pancada: ela volta ao chao
        (batida + 0.09, apoiada + 3.0),          # e quica de leve
        (dur, apoiada)])
    # A dianteira esquerda TRAVA o peso enquanto a direita bate.
    dianteira("leg_front_left", [
        (0.0, apoiada), (subida, apoiada + 3.0),
        (batida, apoiada - 2.0), (dur, apoiada)])
    # O IMPACTO MORA NO CORPO, e nao na pata enterrada. Uma pata que atravessa o
    # bloco para "sentir peso" e um bug; um tronco que afunda meio pixel no quadro
    # exato da pancada e o peso.
    curva(b, "body", "position", [
        (batida, vetor(y=-ABAIXAR_NO_AVISO - 0.35)),
        (batida + 0.12, vetor(y=-ABAIXAR_NO_AVISO + 0.15))])
    # As traseiras absorvem a pancada.
    for perna in ("leg_back_left", "leg_back_right"):
        lado = 1.0 if perna.endswith("left") else -1.0
        pares = [(0.0, atras), (subida, atras - 2.0), (batida, atras + 3.0),
                 (dur, atras)]
        curva(b, perna, "rotation",
              [(t, vetor(x=x, z=6.0 * lado)) for t, x in pares])
        curva(b, PATA[perna], "rotation",
              [(t, vetor(x=num(x * CONTRA_PATA_PLANA))) for t, x in pares])
    curva(b, "tail", "rotation",
          [(t, vetor(x=CAUDA_NO_AVISO + v)) for t, v in ciclo(dur, dur, -3.0)])
    return {"loop": LOOPS["warn"], "animation_length": dur, "bones": b}


def rear_warn(geometria, inicio, alto):
    """A ESCALADA. 2s em loop: ele SOBE, encara de cima, e desce para subir de novo.

    E o clipe de que este mob vive. Ele existe para dizer uma frase inteira -- "a
    proxima e de verdade" -- para alguem que pode estar a doze blocos de
    distancia. Por isso e o unico clipe medido em GRAUS DE ALTURA APARENTE na
    borda do territorio, e nao em graus de rotacao
    (`conferir_que_a_escalada_le_de_doze_blocos`).

    A ORDEM DENTRO DO CLIPE E A LEITURA:
       0.00  a pose do `warn`, inteira -- a escalada nao passa pelo neutro
       0.30  ja esta em cima: a subida e RAPIDA, porque hesitacao nao ameaca
       0.30..1.55  fica. Oscila, a pata dianteira trabalha no ar, a boca abre
       2.00  volta a pose do `warn`, que e tambem o primeiro quadro

    Descer no fim, em vez de segurar a pose, e decisao: o urso erguido volta ao
    chao e SOBE DE NOVO enquanto a janela do servidor durar. Segurar a pose por
    80 ticks viraria estatua; repetir a subida e o que le como "ele esta
    insistindo", que e literalmente o que o servidor esta fazendo.
    """
    dur = DUR_REAR
    subiu, vai_descer = 0.30, 1.55
    # O AGACHAMENTO DE IMPULSO TAMBEM E MEDIDO. Antes de subir ele afunda mais um
    # pouco, e "mais um pouco" tem de vir com a perna que o sustenta: escrito a
    # mao (16 graus na dianteira, que era a primeira versao), o corpo descia e a
    # perna ESTICAVA, e as quatro patas entravam quatro pixels no bloco no quadro
    # em que o urso toma impulso. `conferir_que_nada_atravessa_o_chao` mediu.
    fundo_frente, fundo_tras = pernas_que_agacham(
        geometria, ABAIXAR_NO_AVISO + IMPULSO_DA_ESCALADA,
        -CORPO_NO_AVISO, CONTRA_PATA_PLANA)
    b = {}
    aplicar(b, inicio, 0.0)

    # O tronco. A posicao em Y vem da pose ERGUIDA, que leu do geo quanto o corpo
    # tem de subir para as traseiras encostarem no chao.
    curva(b, "body", "rotation", [
        (0.0, inicio["body"]["rotation"]),
        (0.12, vetor(x=CORPO_NO_AVISO)),              # afunda para tomar impulso
        (subiu, alto["body"]["rotation"]),
        (0.9, vetor(x=ERGUER_NAS_TRASEIRAS + 3.0)),
        (vai_descer, alto["body"]["rotation"]),
        (1.85, vetor(x=CORPO_NO_AVISO))])
    curva(b, "body", "position", [
        (0.0, inicio["body"]["position"]),
        (0.12, vetor(y=-ABAIXAR_NO_AVISO - IMPULSO_DA_ESCALADA)),
        (subiu, alto["body"]["position"]),
        (vai_descer, alto["body"]["position"]),
        (1.85, vetor(y=-ABAIXAR_NO_AVISO - IMPULSO_DA_ESCALADA * 0.6))])
    curva(b, "chest", "rotation", [
        (0.0, inicio["chest"]["rotation"]), (subiu, alto["chest"]["rotation"]),
        (vai_descer, alto["chest"]["rotation"]), (1.85, vetor(x=-2.0))])
    curva(b, "chest", "position", [(0.0, vetor()), (dur, vetor())])
    # A cabeca ENCARA DE CIMA o tempo todo em que ele estiver em cima. Ela oscila
    # de leve, e a oscilacao e lenta: em cima ele nao tem pressa.
    curva(b, "neck", "rotation", [
        # O QUEIXO SOBE AO TOMAR IMPULSO, e nao desce. O reflexo de "agachar e
        # baixar a cabeca" enfiou o queixo tres pixels dentro do bloco neste geo,
        # e antes disso ja estava errado de leitura: quem vai se erguer olha para
        # cima primeiro.
        (0.0, inicio["neck"]["rotation"]), (0.12, vetor(x=PESCOCO_NO_AVISO + 8.0)),
        (subiu, alto["neck"]["rotation"]),
        (0.9, vetor(x=PESCOCO_ERGUIDO + 5.0)),
        (vai_descer, alto["neck"]["rotation"]),
        (1.85, vetor(x=PESCOCO_NO_AVISO + 4.0))])
    curva(b, "neck", "position", [
        (0.0, inicio["neck"]["position"]), (subiu, vetor(z=-0.5)),
        (vai_descer, vetor(z=-0.5)), (1.85, vetor(z=-PROJECAO_DO_PESCOCO))])
    curva(b, "head", "rotation", [
        (0.0, inicio["head"]["rotation"]), (subiu, alto["head"]["rotation"]),
        (0.75, vetor(x=CABECA_ERGUIDA - 4.0, y=5.0)),
        (1.2, vetor(x=CABECA_ERGUIDA - 2.0, y=-5.0)),
        (vai_descer, alto["head"]["rotation"]),
        (1.85, vetor(x=CABECA_NO_AVISO + 2.0))])
    # O urso erguido RUGE: a boca abre mais do que no aviso de perto.
    curva(b, "jaw", "rotation", [
        (0.0, inicio["jaw"]["rotation"]), (0.2, vetor(x=MANDIBULA_ERGUIDA - 6.0)),
        (0.6, vetor(x=MANDIBULA_ERGUIDA)), (1.0, vetor(x=-18.0)),
        (1.3, vetor(x=MANDIBULA_ERGUIDA)),
        (1.85, vetor(x=MANDIBULA_NO_ROSNADO))])
    for orelha in ORELHAS:
        lado = 1.0 if orelha.endswith("left") else -1.0
        curva(b, orelha, "rotation", [
            (0.0, inicio[orelha]["rotation"]), (subiu, alto[orelha]["rotation"]),
            (0.95, vetor(x=6.0, y=-(ORELHA_PARA_TRAS - 8.0) * SENTIDO_DE_Y * lado)),
            (vai_descer, alto[orelha]["rotation"]),
            (1.85, vetor(x=8.0, y=-ORELHA_PARA_TRAS * SENTIDO_DE_Y * lado))])

    # AS DIANTEIRAS. Elas saem do chao e FICAM soltas -- e o gesto que diz que o
    # bicho esta apoiado so nas traseiras. Elas trabalham no ar: um par de patas
    # congelado la em cima le como modelo travado.
    for perna in ("leg_front_left", "leg_front_right"):
        lado = 1.0 if perna.endswith("left") else -1.0
        membro(b, perna, [
            (0.0, inicio[perna]["rotation"]),
            (0.12, vetor(x=fundo_frente, z=6.0 * lado)),
            (subiu, alto[perna]["rotation"]),
            (0.75, vetor(x=DIANTEIRAS_NO_AR + 14.0 * lado, z=4.0 * lado)),
            (1.15, vetor(x=DIANTEIRAS_NO_AR - 10.0 * lado, z=-2.0 * lado)),
            (vai_descer, alto[perna]["rotation"]),
            (1.85, vetor(x=fundo_frente, z=6.0 * lado))],
            inicio[PATA[perna]]["rotation"])
    for perna in ("leg_back_left", "leg_back_right"):
        lado = 1.0 if perna.endswith("left") else -1.0
        membro(b, perna, [
            (0.0, inicio[perna]["rotation"]),
            (0.12, vetor(x=fundo_tras, z=6.0 * lado)),
            (subiu, alto[perna]["rotation"]),
            (0.9, vetor(x=alto[perna]["rotation"][0] + 4.0)),
            (vai_descer, alto[perna]["rotation"]),
            (1.85, vetor(x=fundo_tras, z=6.0 * lado))],
            inicio[PATA[perna]]["rotation"])
    curva(b, "tail", "rotation", [
        (0.0, inicio["tail"]["rotation"]), (subiu, alto["tail"]["rotation"]),
        (vai_descer, alto["tail"]["rotation"]),
        (1.85, vetor(x=CAUDA_NO_AVISO + 2.0))])

    # E o aviso de perto volta INTEIRO no ultimo quadro: e ele que fecha o loop e
    # e ele que costura com o `warn` dos dois lados.
    aplicar(b, inicio, dur)
    return {"loop": LOOPS["rear_warn"], "animation_length": dur, "bones": b}


def claw_swipe(geometria, corrida):
    """Golpe de pata: ergue a dianteira e desce em arco. 0.6s, uma vez so.

    COMECA E TERMINA NO PRIMEIRO QUADRO DO `run`, e isso e o que faz o golpe
    existir no jogo em vez de so no arquivo. Quem pede este clipe e o ENGAGE, e
    no ENGAGE o urso esta perseguindo -- o vizinho dos dois lados e a corrida.
    Um golpe que comecasse do neutro faria o urso parar de correr, bater e voltar
    a correr, com dois trancos de 4 ticks que ninguem consegue descrever.

    O QUE MEDE ESTE CLIPE NAO E O ANGULO, E O ALCANCE: `alcance_da_garra` conta
    em px quanto a pata passa da frente da hitbox. Um golpe que nao sai da
    silhueta nao le como golpe; um que sai demais promete um alcance que o
    servidor nao entrega.
    """
    dur = DUR_SWIPE
    b = {}
    aplicar(b, corrida, 0.0)

    # O TRONCO TORCE JUNTO, E O SENTIDO DA TORCAO NAO E DECORACAO.
    #
    # A garra e a DIREITA, que mora em -X. Uma guinada Y positiva leva +X para -Z
    # (a frente), ou seja, joga o ombro ESQUERDO a frente e o direito para TRAS.
    # Entao o sinal correto e: +TORCAO no armar (ombro direito recuado, ganhando
    # curso) e -TORCAO no golpe (ombro direito lancado a frente).
    #
    # Trocar os dois faria o urso bater com a garra direita torcendo o corpo para
    # longe do golpe. Continuaria um movimento; so nao seria uma pancada. E o
    # alcance mediria menos -- que foi exatamente como este erro apareceu, em
    # `conferir_que_o_golpe_sai_da_silhueta`, e nao a olho.
    armar, bate, solta = 0.15, 0.3, 0.44
    plantado = pernas_que_agacham(geometria, ABAIXAR_NA_CORRIDA,
                                  INCLINAR_NA_CORRIDA)[0]
    curva(b, "body", "rotation", [
        (0.0, corrida["body"]["rotation"]),
        (armar, vetor(x=-2.0, y=TORCAO_DO_GOLPE, z=-8.0)),
        (bate, vetor(x=-6.0, y=-TORCAO_DO_GOLPE - 4.0, z=4.0)),
        (solta, vetor(x=-5.0, y=-4.0, z=2.0))])
    curva(b, "body", "position", [
        (0.0, corrida["body"]["position"]), (armar, vetor(y=0.4, z=0.5)),
        (bate, vetor(y=-ABAIXAR_NA_CORRIDA, z=-1.0)),
        (solta, vetor(y=-ABAIXAR_NA_CORRIDA))])
    curva(b, "chest", "rotation", [
        (0.0, vetor()), (armar, vetor(x=4.0, y=6.0)),
        (bate, vetor(x=-6.0, y=-8.0)), (solta, vetor(x=-2.0))])
    curva(b, "neck", "rotation", [
        (0.0, corrida["neck"]["rotation"]), (armar, vetor(x=-8.0, y=8.0)),
        (bate, vetor(x=-6.0, y=-6.0)), (solta, vetor(x=-12.0))])
    curva(b, "head", "rotation", [
        (0.0, corrida["head"]["rotation"]), (armar, vetor(x=6.0, y=6.0)),
        (bate, vetor(x=6.0, y=-5.0)), (solta, vetor(x=8.0))])
    curva(b, "jaw", "rotation", [
        (0.0, corrida["jaw"]["rotation"]), (armar, vetor(x=-16.0)),
        (bate, vetor(x=-26.0)), (solta, vetor(x=-10.0))])
    for orelha in ORELHAS:
        lado = 1.0 if orelha.endswith("left") else -1.0
        curva(b, orelha, "rotation", [
            (0.0, corrida[orelha]["rotation"]),
            (armar, vetor(x=10.0, y=-ORELHA_PARA_TRAS * SENTIDO_DE_Y * lado)),
            (solta, vetor(x=8.0,
                          y=-ORELHA_PARA_TRAS * 0.8 * SENTIDO_DE_Y * lado))])

    # A DIANTEIRA DIREITA E A ARMA. Ela sobe alto (armar), CORTA passando a frente
    # do corpo (bate) e o arco continua ate o fim (solta) -- um golpe que para no
    # alvo le como empurrao. O quadro do `bate` e o que `alcance_da_garra` mede.
    membro(b, "leg_front_right", [
        (0.0, corrida["leg_front_right"]["rotation"]),
        (armar, vetor(x=GARRA_NO_ALTO, z=-14.0)),
        (bate, vetor(x=GARRA_NA_PANCADA, z=6.0)),
        (solta, vetor(x=plantado, z=8.0)),
        (0.52, vetor(x=plantado * 0.5, z=2.0))],
        corrida[PATA["leg_front_right"]]["rotation"])
    # A esquerda PLANTA: alguem tem de segurar o peso durante o golpe.
    membro(b, "leg_front_left", [
        (0.0, corrida["leg_front_left"]["rotation"]),
        (armar, vetor(x=24.0, z=8.0)), (bate, vetor(x=34.0, z=10.0)),
        (solta, vetor(x=22.0, z=4.0))], corrida[PATA["leg_front_left"]]["rotation"])
    for perna in ("leg_back_left", "leg_back_right"):
        lado = 1.0 if perna.endswith("left") else -1.0
        membro(b, perna, [
            (0.0, corrida[perna]["rotation"]),
            (armar, vetor(x=-18.0, z=4.0 * lado)),
            (bate, vetor(x=-24.0, z=6.0 * lado)),
            (solta, vetor(x=-14.0, z=2.0 * lado))], corrida[PATA[perna]]["rotation"])
    curva(b, "tail", "rotation", [
        (0.0, corrida["tail"]["rotation"]), (armar, vetor(x=-18.0, y=10.0)),
        (bate, vetor(x=-8.0, y=-14.0)), (solta, vetor(x=-14.0))])

    aplicar(b, corrida, dur)
    return {"loop": LOOPS["claw_swipe"], "animation_length": dur, "bones": b}


def bite(geometria, corrida):
    """Mordida curta e seca. 0.4s, uma vez so.

    Oito ticks de acao. A leitura inteira esta em TRES quadros: recua e abre
    (0.10), estende e FECHA (0.20), solta (0.28). O fechamento e o unico
    instante que importa, e por isso ele e um quadro sozinho -- uma mandibula que
    fecha ao longo de quatro ticks le como bocejo.

    Como o `claw_swipe`, comeca e termina no primeiro quadro do `run`.
    """
    dur = DUR_BITE
    # As pernas CRAVAM, e o angulo em que elas cravam e lido: o mergulho do corpo
    # para a frente afunda o ombro, e uma perna escrita a mao afunda com ele. Era
    # o caso ate `conferir_que_nada_atravessa_o_chao` medir 2,5 px de tornozelo
    # dentro da terra no quadro exato da dentada.
    frente, tras = pernas_que_agacham(
        geometria, ABAIXAR_NA_CORRIDA + MERGULHO_DA_MORDIDA, INCLINACAO_DA_MORDIDA)
    b = {}
    aplicar(b, corrida, 0.0)

    curva(b, "body", "position", [
        (0.0, corrida["body"]["position"]),
        (0.1, vetor(y=-ABAIXAR_NA_CORRIDA, z=0.8)),
        (0.2, vetor(y=-ABAIXAR_NA_CORRIDA - MERGULHO_DA_MORDIDA, z=-1.4)),
        (0.3, vetor(y=-ABAIXAR_NA_CORRIDA - 0.3, z=-0.4))])
    curva(b, "body", "rotation", [
        (0.0, corrida["body"]["rotation"]), (0.1, vetor(x=-3.0)),
        (0.2, vetor(x=-INCLINACAO_DA_MORDIDA)), (0.3, vetor(x=-6.0))])
    curva(b, "neck", "rotation", [
        (0.0, corrida["neck"]["rotation"]), (0.1, vetor(x=-12.0)),
        (0.2, vetor(x=-30.0)), (0.3, vetor(x=-24.0))])
    curva(b, "neck", "position", [
        (0.0, vetor()), (0.1, vetor(z=0.8)),
        (0.2, vetor(z=-AVANCO_DA_MORDIDA)), (0.3, vetor(z=-0.8))])
    curva(b, "head", "rotation", [
        (0.0, corrida["head"]["rotation"]), (0.1, vetor(x=16.0)),
        (0.2, vetor(x=22.0)), (0.3, vetor(x=14.0))])
    # ABRE, FECHA, entreabre. O zero em 0.20 e a mordida.
    curva(b, "jaw", "rotation", [
        (0.0, corrida["jaw"]["rotation"]), (0.1, vetor(x=MANDIBULA_ABERTA)),
        (0.2, vetor()), (0.28, vetor(x=-4.0)), (0.34, vetor(x=-10.0))])
    for orelha in ORELHAS:
        lado = 1.0 if orelha.endswith("left") else -1.0
        curva(b, orelha, "rotation", [
            (0.0, corrida[orelha]["rotation"]),
            (0.1, vetor(x=12.0, y=-ORELHA_PARA_TRAS * SENTIDO_DE_Y * lado)),
            (0.3, vetor(x=8.0,
                        y=-ORELHA_PARA_TRAS * 0.8 * SENTIDO_DE_Y * lado))])
    # As pernas so CRAVAM: uma mordida nao e um passo, e o clipe nao pode roubar
    # a passada da corrida que continua em volta dele.
    for perna in PERNAS:
        dianteira = "front" in perna
        lado = 1.0 if perna.endswith("left") else -1.0
        membro(b, perna, [
            (0.0, corrida[perna]["rotation"]),
            (0.2, vetor(x=frente if dianteira else tras, z=3.0 * lado)),
            (0.3, vetor(x=frente * 0.6 if dianteira else tras * 0.6))],
            corrida[PATA[perna]]["rotation"])
    curva(b, "tail", "rotation", [
        (0.0, corrida["tail"]["rotation"]), (0.2, vetor(x=-20.0)),
        (0.3, vetor(x=-15.0))])

    aplicar(b, corrida, dur)
    return {"loop": LOOPS["bite"], "animation_length": dur, "bones": b}


def short_charge(geometria, aviso, corrida):
    """A INVESTIDA CURTA. 0.9s: abaixa, toma impulso, corre.

    E a unica ponte entre "ele esta avisando" e "ele esta vindo", e por isso ela
    e costurada dos DOIS lados: comeca no primeiro quadro do `warn` e termina no
    primeiro quadro do `run`.

    Isso nao e elegancia. Se o comeco nao fosse a pose do aviso, o urso passaria
    pelo neutro entre avisar e investir -- um quadro de "ele relaxou" no exato
    instante em que o servidor acabou de decidir ENGAGE. O jogador leria
    "desistiu" e pararia de recuar, que e o unico erro que este mob pune.

    A ORDEM: 0.00 a pose do aviso -> 0.22 o agachamento MAIS FUNDO (ele junta o
    corpo e recua, e essa e a ultima chance de sair) -> 0.35 o arranque ->
    0.35..0.90 uma passada curta e rapida que desemboca no `run`.
    """
    dur = DUR_CHARGE
    coil, arranque = 0.22, 0.35
    # O AGACHAMENTO MAIS FUNDO DE TODO O ARQUIVO, e por isso o que mais precisava
    # ser medido: o corpo desce mais de tres pixels e nenhuma perna escrita a mao
    # aguenta isso sem enfiar o tornozelo no bloco.
    fundo_frente, fundo_tras = pernas_que_agacham(
        geometria, AGACHAR_NA_INVESTIDA, -CORPO_NO_AVISO, CONTRA_PATA_PLANA)
    b = {}
    aplicar(b, aviso, 0.0)

    curva(b, "body", "position", [
        (0.0, aviso["body"]["position"]),
        (coil, vetor(y=-AGACHAR_NA_INVESTIDA, z=RECUO_DA_INVESTIDA)),
        (arranque, vetor(y=-ABAIXAR_NA_CORRIDA, z=-2.0))])
    curva(b, "body", "rotation", [
        (0.0, aviso["body"]["rotation"]), (coil, vetor(x=CORPO_NO_AVISO)),
        (arranque, vetor(x=-INCLINAR_NA_CORRIDA))])
    curva(b, "chest", "rotation", [
        (0.0, vetor()), (coil, vetor(x=3.0)), (arranque, vetor(x=-4.0))])
    curva(b, "neck", "rotation", [
        (0.0, aviso["neck"]["rotation"]), (coil, vetor(x=-8.0)),
        (arranque, vetor(x=-26.0))])
    curva(b, "neck", "position", [
        (0.0, aviso["neck"]["position"]), (coil, vetor(z=0.6)),
        (arranque, vetor(z=-1.0))])
    curva(b, "head", "rotation", [
        (0.0, aviso["head"]["rotation"]), (coil, vetor(x=2.0)),
        (arranque, vetor(x=14.0))])
    curva(b, "jaw", "rotation", [
        (0.0, aviso["jaw"]["rotation"]), (coil, vetor(x=-8.0)),
        (arranque, vetor(x=-22.0)), (0.6, vetor(x=-8.0))])
    for orelha in ORELHAS:
        lado = 1.0 if orelha.endswith("left") else -1.0
        curva(b, orelha, "rotation", [
            (0.0, aviso[orelha]["rotation"]),
            (arranque, vetor(x=6.0,
                             y=-ORELHA_PARA_TRAS * 0.7 * SENTIDO_DE_Y * lado))])
    # As pernas ENROLAM sob o corpo e soltam. As traseiras empurram; as
    # dianteiras estendem para a frente.
    for perna in PERNAS:
        frente = "front" in perna
        lado = 1.0 if perna.endswith("left") else -1.0
        membro(b, perna, [
            (0.0, aviso[perna]["rotation"]),
            (coil, vetor(x=fundo_frente if frente else fundo_tras, z=4.0 * lado)),
            (arranque, vetor(x=-18.0 if frente else 16.0))],
            aviso[PATA[perna]]["rotation"])
    curva(b, "tail", "rotation", [
        (0.0, aviso["tail"]["rotation"]), (coil, vetor(x=18.0)),
        (arranque, vetor(x=-16.0))])

    # A PASSADA. Periodo mais curto que o do `run`: arranque e mais rapido que
    # cruzeiro. Como a fase zero desta marcha E a fase zero da corrida, a ultima
    # amostra cairia exatamente sobre a pose de costura -- e por isso ela e
    # cortada (`ate=dur`): quem escreve o ultimo quadro e o vizinho, sozinho.
    marcha(b, dur - arranque, CICLO_DA_INVESTIDA, PASSADA_DA_CORRIDA, {
        "leg_front_left": (0.0, 1.0), "leg_front_right": (0.05, 0.9),
        "leg_back_left": (0.5, 0.92), "leg_back_right": (0.55, 1.0),
    }, inicio=arranque, ate=dur)
    curva(b, "body", "position",
          [(t + arranque, vetor(y=-ABAIXAR_NA_CORRIDA + v)) for t, v in ciclo(
              dur - arranque, CICLO_DA_INVESTIDA / 2.0, 0.55, ate=dur - arranque)
           if t > 0.0])
    curva(b, "body", "rotation",
          [(t + arranque, vetor(x=-INCLINAR_NA_CORRIDA + v)) for t, v in ciclo(
              dur - arranque, CICLO_DA_INVESTIDA / 2.0, 2.0, ate=dur - arranque)
           if t > 0.0])
    curva(b, "neck", "rotation",
          [(t + arranque, vetor(x=-24.0 + v)) for t, v in ciclo(
              dur - arranque, CICLO_DA_INVESTIDA / 2.0, 3.0, ate=dur - arranque)
           if t > 0.0])

    aplicar(b, corrida, dur)
    return {"loop": LOOPS["short_charge"], "animation_length": dur, "bones": b}


def hurt():
    """Tranco de 5 ticks. NAO TOCA PERNA NEM PATA, e isso e decisao.

    O Java registra UM controller so. Num controller unico, osso que o clipe
    corrente nao cita volta para o DEFAULT DO MODELO -- e o default e o urso
    parado. Um `hurt` que keyasse as quatro pernas poria um urso em plena
    corrida com as patas na pose de bind por cinco ticks; um `hurt` que as
    keyasse em ZERO faria exatamente a mesma coisa, com a diferenca de estar
    escrito. Nao ha terceira opcao dentro de um controller so.

    Entao a decisao e: este clipe cobre TRONCO E CABECA, que e onde um tranco
    mora, e fica pronto para subir a um SEGUNDO controller que sobreponha o
    flinch sem destruir a passada. Enquanto ele estiver no mesmo controller da
    locomocao, as pernas vao saltar por cinco ticks -- e isso esta no relato como
    pendencia para a lane do Java, nao como descuido desta.

    `conferir_que_o_hurt_nao_toca_a_perna` guarda a decisao contra a proxima
    pessoa que abrir o clipe e achar que faltou perna.
    """
    dur = DUR_HURT
    b = {}
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.06, vetor(x=6.0, z=7.0)), (0.14, vetor(x=-3.0, z=-3.0)),
        (dur, vetor())])
    curva(b, "body", "position", [
        (0.0, vetor()), (0.06, vetor(y=-1.0, z=0.9)), (0.14, vetor(y=-0.3)),
        (dur, vetor())])
    curva(b, "chest", "rotation", [
        (0.0, vetor()), (0.06, vetor(x=-5.0)), (0.16, vetor(x=2.0)), (dur, vetor())])
    curva(b, "neck", "rotation", [
        (0.0, vetor()), (0.05, vetor(x=10.0)), (0.15, vetor(x=-4.0)), (dur, vetor())])
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.05, vetor(x=12.0, y=9.0)), (0.15, vetor(x=-5.0, y=-4.0)),
        (dur, vetor())])
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (0.05, vetor(x=-34.0)), (0.16, vetor(x=-9.0)), (dur, vetor())])
    for orelha in ORELHAS:
        lado = 1.0 if orelha.endswith("left") else -1.0
        curva(b, orelha, "rotation", [
            (0.0, vetor()),
            (0.05, vetor(x=12.0, y=-ORELHA_PARA_TRAS * SENTIDO_DE_Y * lado)),
            (0.16, vetor(x=4.0,
                         y=-ORELHA_PARA_TRAS * 0.4 * SENTIDO_DE_Y * lado)),
            (dur, vetor())])
    curva(b, "tail", "rotation", [
        (0.0, vetor()), (0.06, vetor(x=-14.0)), (0.16, vetor(x=5.0)), (dur, vetor())])
    return {"loop": LOOPS["hurt"], "animation_length": dur, "bones": b}


def sleep(deitado):
    """Dormindo. 6s em loop, e a respiracao e o clipe inteiro.

    A regra deste clipe e a de menos: o unico movimento e o folego, e ele e LENTO
    (3s por ciclo, contra 2s parado) e AMPLO (0.9 px, contra 0.35). Sao esses
    dois numeros em par que fazem "dormindo" ler como dormindo -- amplitude
    sozinha le como ofegante, e lentidao sozinha le como estatua.
    `conferir_que_o_sono_respira_fundo_e_devagar` guarda os dois.

    As orelhas CAEM e nao se mexem. Um urso dormindo com orelha atenta esta
    fingindo, e nao ha nada no servidor que corresponda a isso.
    """
    dur = DUR_SLEEP
    b = {}
    aplicar(b, deitado, 0.0)
    aplicar(b, deitado, dur)

    # A RESPIRACAO DEITADA SO VAI PARA CIMA, e o corpo NAO acompanha.
    #
    # Parado, o peito sobe e desce em torno do repouso. Deitado, o repouso E o
    # chao: metade de uma oscilacao simetrica vira torax dentro do bloco -- e foi
    # assim que este clipe reprovou, com o folego mais amplo de todo o arquivo
    # empurrando o urso 2,3 px para dentro da terra duas vezes por volta.
    #
    # Entao o folego deitado e uma meia-onda apoiada no zero: a caixa toracica se
    # ENCHE a partir do repouso, que e o que ela faz de verdade. E o `body` fica
    # quieto -- ele esta no chao, e chao nao afunda.
    curva(b, "chest", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, CICLO_RESPIRACAO_DORMINDO,
                                             FOLEGO_DORMINDO / 2.0,
                                             FOLEGO_DORMINDO / 2.0)])
    curva(b, "chest", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, CICLO_RESPIRACAO_DORMINDO,
                                             1.5, 1.5)])
    # A cabeca acompanha o folego e NADA MAIS. Ela esta APOIADA, e o angulo dela
    # foi medido contra o chao com o corpo ja deitado: a oscilacao parte dali e e
    # pequena o bastante para nao furar o bloco. Escrever um angulo aqui seria a
    # segunda fonte de verdade que poria o focinho dentro da terra.
    apoio_pescoco = deitado["neck"]["rotation"][0]
    apoio_cabeca = deitado["head"]["rotation"][0]
    # Tambem meia-onda, e para CIMA: a cabeca esta apoiada no limite do chao, e
    # meia oscilacao simetrica a enfiaria nele junto com o peito.
    curva(b, "neck", "rotation",
          [(t, vetor(x=apoio_pescoco + v))
           for t, v in ciclo(dur, CICLO_RESPIRACAO_DORMINDO, 0.7, 0.7)])
    curva(b, "head", "rotation",
          [(t, vetor(x=apoio_cabeca + v))
           for t, v in ciclo(dur, CICLO_RESPIRACAO_DORMINDO, 0.5, 0.5)])
    # Um suspiro por clipe -- a mandibula abre uma vez, devagar, e fecha.
    curva(b, "jaw", "rotation", [
        (0.0, vetor()), (3.4, vetor()), (3.9, vetor(x=-9.0)),
        (4.6, vetor(x=-3.0)), (5.1, vetor()), (dur, vetor())])
    curva(b, "tail", "rotation",
          [(t, vetor(x=deitado["tail"]["rotation"][0] + v * 0.4, y=v))
           for t, v in ciclo(dur, dur, 4.0)])
    return {"loop": LOOPS["sleep"], "animation_length": dur, "bones": b}


def death(geometria):
    """As pernas cedem, ele tomba, a cabeca por ultimo. 1.5s, hold_on_last_frame.

    A ORDEM importa mais que as poses: as dianteiras dobram primeiro, o tronco
    rola para a esquerda, o corpo chega ao chao e SO ENTAO a cabeca larga. Uma
    cabeca que tomba junto com o corpo le como desligar o bicho; uma cabeca que
    resiste meio segundo le como morte.

    O CLIPE E ESCRITO COMO UMA SEQUENCIA DE POSES, E CADA UMA E ASSENTADA.
    A versao anterior escrevia a queda como fracoes do valor final (22%, 60%,
    90%) -- e os numeros do meio nao correspondiam a nada: no quadro em que as
    pernas ja tinham cedido mas o tronco ainda nao tinha rolado, o corpo estava
    dois pixels dentro do chao. Com `assentar` chamado POR ESTAGIO, cada quadro
    chave encosta no chao pela pose que ele realmente tem, e a interpolacao anda
    entre duas poses que ja estao no lugar certo.
    """
    dur = DUR_DEATH
    o = ORELHA_CAIDA
    estagios = (
        # (t, rotacoes, deriva em X)   -- o urso tomba para a ESQUERDA (+Z rola +X)
        (0.0, {}, 0.0),
        (0.2, {"body": vetor(x=8.0, z=6.0), "chest": vetor(x=5.0),
               "neck": vetor(x=14.0), "head": vetor(x=11.0, z=-4.0),
               "jaw": vetor(x=-30.0), "tail": vetor(x=-10.0),
               "leg_front_left": vetor(x=26.0), "leg_front_right": vetor(x=22.0),
               "leg_back_left": vetor(x=-8.0), "leg_back_right": vetor(x=-6.0)},
         0.0),
        (0.5, {"body": vetor(x=6.0, z=26.0), "chest": vetor(x=2.0),
               "neck": vetor(x=9.0), "head": vetor(x=4.0, z=-9.0),
               "jaw": vetor(x=-20.0), "tail": vetor(x=-16.0),
               "leg_front_left": vetor(x=40.0), "leg_front_right": vetor(x=36.0),
               "leg_back_left": vetor(x=-20.0), "leg_back_right": vetor(x=-18.0)},
         0.6),
        (0.85, {"body": vetor(x=5.0, z=52.0), "chest": vetor(x=-1.0),
                "neck": vetor(x=4.0), "head": vetor(x=-6.0, z=-13.0),
                "jaw": vetor(x=-16.0), "tail": vetor(x=-6.0),
                "leg_front_left": vetor(x=36.0), "leg_front_right": vetor(x=32.0),
                "leg_back_left": vetor(x=-30.0), "leg_back_right": vetor(x=-28.0)},
         1.3),
        (1.15, {"body": vetor(x=4.0, z=68.0), "chest": vetor(x=-3.0),
                "neck": vetor(x=-12.0), "head": vetor(x=-18.0, z=-16.0),
                "jaw": vetor(x=-20.0), "tail": vetor(x=4.0),
                "leg_front_left": vetor(x=30.0), "leg_front_right": vetor(x=27.0),
                "leg_back_left": vetor(x=-30.0), "leg_back_right": vetor(x=-27.0)},
         1.8),
        (dur, {"body": vetor(x=3.0, z=76.0), "chest": vetor(x=-4.0),
               "neck": vetor(x=-24.0), "head": vetor(x=-26.0, z=-20.0),
               "jaw": vetor(x=-24.0), "tail": vetor(x=12.0),
               "leg_front_left": vetor(x=26.0), "leg_front_right": vetor(x=24.0),
               "leg_back_left": vetor(x=-24.0), "leg_back_right": vetor(x=-22.0)},
         2.2),
    )

    # A ORELHA CAI ao longo do tombo. Ela e escrita aqui, em cima dos estagios, em
    # vez de dentro de cada um: espelhar a mao em seis lugares e onde um dos lados
    # sobrevive a proxima correcao com o sinal antigo.
    orelha_por_t = {0.0: (0.0, 0.0), 0.2: (6.0, -o * 0.2), 0.5: (2.0, -o * 0.5),
                    0.85: (-2.0, -o * 0.7), 1.15: (-6.0, -o * 0.8),
                    dur: (-8.0, -o * 0.8)}

    poses = []
    for t, rotacoes, deriva in estagios:
        p = pose_neutra()
        for osso, rot in rotacoes.items():
            p[osso]["rotation"] = list(rot)
            if osso in PATA:
                p[PATA[osso]]["rotation"] = vetor(x=num(rot[0] * CONTRA_PATA))
        x_orelha, z_orelha = orelha_por_t[t]
        for orelha in ORELHAS:
            lado = 1.0 if orelha.endswith("left") else -1.0
            p[orelha]["rotation"] = vetor(x=x_orelha, z=z_orelha * lado)
        # CADA ESTAGIO ENCOSTA NO CHAO PELA PROPRIA POSE.
        p["body"]["position"] = vetor(x=deriva, y=num(assentar(geometria, p)))
        poses.append((t, p))

    b = {}
    for t, pose in poses:
        aplicar(b, pose, t)
    return {"loop": LOOPS["death"], "animation_length": dur, "bones": b}


# ------------------------------------------------------- leitura do geo


def carregar_geo():
    caminho = os.path.join(DIR_GEO, MOB + ".geo.json")
    if not os.path.exists(caminho):
        raise SystemExit(
            "%s nao existe. Este gerador LE o geo: a descida do focinho, a altura"
            " do bicho erguido, a queda da morte e o alcance da garra saem de la,"
            " e os eixos sao conferidos contra ele. Sem geo nao ha o que animar --"
            " e um arquivo de animacao sem modelo reprova em"
            " CoerenciaDeGeckoLibTest antes de chegar a tela de alguem."
            % caminho)
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


def subarvore(geometria, nome):
    saida = [nome]
    for f in filhos(geometria, nome):
        saida += subarvore(geometria, f)
    return saida


def subarvore_tem_volume(geometria, nome):
    return any(caixas(geometria, n) for n in subarvore(geometria, nome))


# -------------------------------------------------- a conta da rotacao


def girar(p, graus):
    """Aplica (rx, ry, rz) na ORDEM QUE O GECKOLIB APLICA: X, depois Y, depois Z.

    A ordem nao e detalhe. `RenderUtil.translateAndRotateMatrixForBone` empilha
    Z, depois Y, depois X -- e empilhar nessa ordem aplica ao PONTO na ordem
    inversa. Medir a altura com a ordem trocada daria um numero plausivel e
    errado, que e o pior resultado possivel para uma regua.
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


def transformar(geometria, pose):
    """CINEMATICA DIRETA: onde cada vertice do modelo esta, nesta pose.

    E o unico jeito honesto de medir a silhueta de um bicho hierarquico. A conta
    de guardanapo -- "a perna tem 9 px, entao 48 graus levantam 6,7" -- ignora
    que o osso herda o pai, e e exatamente por ignorar isso que ela erra sempre
    para o lado otimista.

    A rotacao de BIND do geo (o campo `rotation` do osso) entra somada a da
    animacao, que e o que o GeckoLib faz. Um osso que nasce torto no modelo e
    medido torto aqui.
    """
    resultado = {}

    def desce(nome, do_pai):
        b = osso(geometria, nome)
        piv = b.get("pivot", [0.0, 0.0, 0.0])
        bind = b.get("rotation", [0.0, 0.0, 0.0])
        anim = pose.get(nome, {}).get("rotation", [0.0, 0.0, 0.0])
        rot = [g + a for g, a in zip(bind, anim)]
        desloca = pose.get(nome, {}).get("position", [0.0, 0.0, 0.0])

        def aqui(p):
            q = em_volta(p, piv, rot)
            return do_pai((q[0] + desloca[0], q[1] + desloca[1], q[2] + desloca[2]))

        resultado[nome] = [aqui(p) for p in cantos(geometria, nome)]
        for f in filhos(geometria, nome):
            desce(f, aqui)

    desce("root", lambda p: p)
    return resultado


def pontos(geometria, pose, ossos=None):
    todos = transformar(geometria, pose)
    if ossos is None:
        return [p for lista in todos.values() for p in lista]
    escolhidos = []
    for nome in ossos:
        for n in subarvore(geometria, nome):
            escolhidos += todos[n]
    return escolhidos


def topo(geometria, pose):
    """O ponto mais ALTO da silhueta nesta pose. E a regua do mob inteiro."""
    return max(p[1] for p in pontos(geometria, pose))


def chao(geometria, pose, ossos=None):
    return min(p[1] for p in pontos(geometria, pose, ossos))


def frente(geometria, pose, ossos=None):
    """O ponto mais a FRENTE (menor z). Usado para medir o alcance da garra."""
    return min(p[2] for p in pontos(geometria, pose, ossos))


def assentar(geometria, pose, alvo=0.0):
    """Quanto o corpo tem de SUBIR para a pose encostar no chao.

    UMA funcao, TRES usos: o bicho erguido nas traseiras, o bicho deitado
    dormindo e o bicho tombado morto. Cada um deles escrito a mao ficaria certo
    hoje e errado na primeira correcao do geo -- e "errado" aqui quer dizer um
    urso boiando dois pixels acima do chao, que ninguem reporta e todo mundo
    estranha.

    Ela pressupoe que o `body` carrega tudo que tem volume: transladar o body
    move a silhueta inteira. `conferir_a_hierarquia` garante isso.
    """
    return alvo - chao(geometria, pose)


def altura_do_clipe(geometria, clipe, amostras=24):
    """O topo da silhueta AO LONGO do clipe -- medido no clipe, nao numa pose.

    Medir uma pose escrita a parte seria a segunda fonte de verdade classica: a
    pose diria que o urso sobe, o clipe poderia nunca chegar nela, e todos os
    portoes de altura ficariam verdes medindo um numero que o jogador nao ve.
    """
    fim = clipe["animation_length"]
    return max(topo(geometria, quadro_em(clipe, fim * i / amostras))
               for i in range(amostras + 1))


# -------------------------------------------------- numeros LIDOS do geo


# Como o angulo total se reparte entre pescoco e cabeca. Um urso que abaixasse
# so o pescoco fareja de cara para a frente; so a cabeca, fareja com o pescoco
# reto. A proporcao e de gosto; o TOTAL nao e -- ele e procurado no geo.
REPARTICAO_DO_FOCINHO = (0.58, 0.42)
ANGULO_MAXIMO_DO_PESCOCO = -140.0
ABERTURA_MAXIMA_DA_DIANTEIRA = 85.0


DOBRA_MAXIMA_DA_PERNA = 140.0


def chao_da_barriga(geometria, pose):
    """O ponto mais baixo do TRONCO -- e so dele -- nesta pose.

    `assentar` olha o modelo inteiro e apoia o bicho no que estiver mais baixo.
    Para dormir isso e errado, e erra calado: com a perna dobrada pendurando um
    pixel abaixo da barriga, o urso "deitado" fica apoiado na perna e a barriga
    boia. Le como urso agachado, e todos os outros portoes continuam verdes.
    """
    movidos = transformar(geometria, pose)
    return min(p[1] for nome in ("body", "chest") for p in movidos[nome])


def altura_da_pata(geometria):
    """A espessura da pata, lida do geo. E a tolerancia natural do recolhimento."""
    return max(c["origin"][1] + c["size"][1] for c in caixas(geometria, "paw_front_left"))


def pernas_dobradas_sob_o_corpo(geometria):
    """(dianteira, traseira): a MENOR dobra que tira a perna de baixo da barriga.

    Deitar quer dizer que o peso passou da perna para o tronco. Enquanto a perna
    dobrada pendurar abaixo da barriga, o bicho continua apoiado NELA -- e a
    diferenca entre as duas coisas, na tela, e a diferenca entre um urso dormindo
    e um urso agachado. Nenhum portao de pose pega isso; quem pegou foi a escada
    de silhueta, medindo um urso "deitado" com 85% da altura do urso em pe.

    O ALVO E A LINHA DA BARRIGA, sem folga nenhuma, e a folga foi tirada de
    proposito depois de existir: com uma tolerancia do tamanho da pata, a perna
    parava um pata-de-espessura ABAIXO da barriga -- e como o corpo desce ate a
    barriga encostar, essa diferenca virava exatamente uma pata enterrada no
    bloco, o clipe inteiro. Uma folga que so aparece somada a outra medida e o
    tipo de numero que passa despercebido em revisao e reprova na tela.

    A busca e uma VARREDURA e nao uma bisseccao: a curva COMECA DESCENDO (a pata e
    um pe comprido, e nos primeiros graus o calcanhar desce mais do que a ponta
    sobe), e uma bisseccao sobre uma curva que nao e monotona devolve um angulo
    plausivel e errado, calada.

    A dianteira dobra para a FRENTE e a traseira para TRAS, que e como um
    quadrupede recolhe as pernas; as duas sao procuradas separadamente porque os
    pivots estao em z diferentes e a pata e comprida para um lado so.
    """
    alvo = chao_da_barriga(geometria, pose_neutra())
    saida = []
    for perna, sentido in (("leg_front_left", 1.0), ("leg_back_left", -1.0)):
        def altura(angulo, perna=perna):
            p = pose_neutra()
            for lado in (perna, perna.replace("left", "right")):
                p[lado]["rotation"] = vetor(x=angulo)
                p[PATA[lado]]["rotation"] = vetor(x=angulo * CONTRA_PATA)
            return chao(geometria, p, (perna,))

        passos = int(DOBRA_MAXIMA_DA_PERNA * 2.0)  # meio grau de resolucao
        dobra, melhor = None, None
        for i in range(passos + 1):
            angulo = DOBRA_MAXIMA_DA_PERNA * sentido * i / passos
            aqui = altura(angulo)
            melhor = aqui if melhor is None else max(melhor, aqui)
            if aqui >= alvo:
                dobra = angulo
                break
        if dobra is None:
            raise SystemExit(
                "dobrando '%s' ate %.0f graus, a pata so chega a %.1f px e a linha"
                " da barriga esta em %.1f. O"
                " urso nao consegue recolher a perna para debaixo do corpo:"
                " deitado, ele vai ficar apoiado nela, que e um urso agachado."
                % (perna, DOBRA_MAXIMA_DA_PERNA * sentido, melhor, alvo))
        saida.append(num(dobra))
    return saida[0], saida[1]


# A PATA PLANA. `CONTRA_PATA` (-0.4) e estilo: a pata acompanha a perna em parte,
# que e o que faz uma passada parecer uma passada. Mas quando a pata precisa
# ESTAR NO CHAO -- agachado, ou no instante em que ela bate -- ela tem de ficar
# HORIZONTAL, e ai a contra-rotacao e total. Sem isso, a pata deste geo (que e um
# pe comprido, com calcanhar) crava um canto no bloco a cada grau que a perna
# gira, e a conta de "quanto a perna aguenta" deixa de fechar.
CONTRA_PATA_PLANA = -1.0
ABERTURA_MAXIMA_DO_AGACHAMENTO = 75.0


def pernas_que_agacham(geometria, queda, inclinacao=0.0, contra=None):
    """(dianteira, traseira) que sustentam o corpo `queda` px mais baixo e
    `inclinacao` graus mais inclinado para a frente.

    Agachar nao e transladar o corpo para baixo: e DOBRAR a perna. Num modelo de
    cubos rigidos, "dobrar" e girar -- e girar encurta o alcance vertical da perna
    pelo cosseno do angulo. Esta funcao inverte essa conta e devolve o angulo.

    Ela existe porque a alternativa e o erro mais silencioso deste arquivo. O
    `warn` abaixa o corpo dois pixels, e dois pixels escritos direto na posicao do
    `body` poem as quatro patas DENTRO do bloco, o clipe inteiro, nos 80 ticks em
    que o jogador esta olhando o bicho de perto. Nada reclama: o urso avisa com as
    patas enterradas ate o tornozelo.

    A INCLINACAO CONTA, e ignora-la e o erro que ela ja cometeu uma vez: baixar o
    peito girando o tronco sobre o quadril afunda o ombro -- e com ele o pivot das
    duas pernas da FRENTE, que sao filhas do `body` -- sem mexer um pixel no
    quadril. Uma versao desta funcao que so olhasse `queda` devolveria a dianteira
    curta pela altura exata da inclinacao, e o urso avisaria com as patas da
    frente enterradas. As traseiras, no mesmo gesto, nao sentem nada: o pivot do
    `body` esta em cima delas.

    A dianteira abre para a FRENTE e a traseira para TRAS -- e a base larga de um
    quadrupede tenso, e e por isso que o mesmo gesto que resolve a geometria
    resolve tambem a leitura.
    """
    if contra is None:
        contra = CONTRA_PATA
    saida = []
    for perna, sentido in (("leg_front_left", 1.0), ("leg_back_left", -1.0)):
        def sob_o_chao(angulo, perna=perna):
            p = pose_neutra()
            p["body"]["position"] = vetor(y=-queda)
            p["body"]["rotation"] = vetor(x=-inclinacao)
            for lado in (perna, perna.replace("left", "right")):
                p[lado]["rotation"] = vetor(x=angulo)
                p[PATA[lado]]["rotation"] = vetor(x=angulo * contra)
            return chao(geometria, p, (perna,))

        lo, hi = 0.0, ABERTURA_MAXIMA_DO_AGACHAMENTO * sentido
        if sob_o_chao(0.0) >= 0.0:
            saida.append(0.0)
            continue
        if sob_o_chao(hi) < 0.0:
            raise SystemExit(
                "para abaixar o corpo %.1f px e inclina-lo %.1f graus, '%s' teria"
                " de abrir mais de %.0f graus. O agachamento pedido nao cabe no"
                " comprimento desta perna: baixe o valor, ou converse com a lane"
                " do geo." % (queda, inclinacao, perna,
                              ABERTURA_MAXIMA_DO_AGACHAMENTO))
        for _ in range(60):
            meio = (lo + hi) / 2.0
            if sob_o_chao(meio) < 0.0:
                lo = meio
            else:
                hi = meio
        saida.append(num((lo + hi) / 2.0))
    return saida[0], saida[1]


ALTURA_DO_QUEIXO_APOIADO = 0.4
TOMBO_MAXIMO_DA_CABECA = -70.0
LEVANTE_MAXIMO_DA_CAUDA = -85.0


def cauda_apoiada(geometria, deitado):
    """O angulo em que a cauda DEITA em vez de furar o chao.

    Deitado, o corpo desce oito pixels -- e a cauda deste geo desce quase ate o
    jarrete. Escrever "cauda baixa" (o reflexo: X positivo) enfia quatro pixels
    dela dentro do bloco. O que a cauda de um bicho deitado faz e o contrario:
    ela se levanta um pouco e POUSA atras dele.

    Mesma varredura da cabeca, e pelo mesmo motivo: o maior angulo que ainda nao
    fura o chao. Nao ha bisseccao aqui porque nao ha monotonia depois do fundo do
    arco.
    """
    passos = int(LEVANTE_MAXIMO_DA_CAUDA * -4.0)
    melhor = 0.0
    for i in range(passos + 1):
        angulo = LEVANTE_MAXIMO_DA_CAUDA * i / passos
        p = {osso: dict(canais) for osso, canais in deitado.items()}
        p["tail"] = dict(deitado["tail"])
        p["tail"]["rotation"] = vetor(x=angulo)
        if chao(geometria, p, ("tail",)) >= ALTURA_DO_QUEIXO_APOIADO:
            return num(angulo)
        melhor = angulo
    raise SystemExit(
        "a cauda nao sai do chao nem levantada %.0f graus com o corpo deitado."
        " Ou a cauda modelada desce demais, ou o corpo esta deitando fundo demais."
        % melhor)


def cabeca_apoiada(geometria, deitado):
    """(pescoco_x, cabeca_x) que poem o QUEIXO no chao, com o corpo ja deitado.

    A maior inclinacao que ainda nao fura o chao -- varredura, e nao bisseccao,
    pelo mesmo motivo do focinho: passado o fundo do arco a cara volta a subir, e
    uma bisseccao devolveria calada um urso dormindo de cara para o ceu.

    Zero e uma resposta legitima: neste modelo o corpo desce tanto ao deitar que
    a mandibula ja chega ao chao sozinha. Se a varredura devolver zero, e porque a
    cabeca ja esta apoiada -- e forcar angulo ali enterraria o focinho.
    """
    a, b = REPARTICAO_DO_FOCINHO

    def queixo(total):
        p = {osso: dict(canais) for osso, canais in deitado.items()}
        p["neck"] = dict(deitado["neck"])
        p["head"] = dict(deitado["head"])
        p["neck"]["rotation"] = vetor(x=total * a)
        p["head"]["rotation"] = vetor(x=total * b)
        return chao(geometria, p, ("head",))

    # A BUSCA VAI PARA OS DOIS LADOS, e isso nao e generalidade gratuita: neste
    # geo o queixo do urso ja esta ABAIXO do chao quando o corpo deita, porque o
    # tronco desce oito pixels e a cabeca desce junto. A versao que so procurava
    # para baixo devolveu zero, calada -- e zero, aqui, e um urso dormindo com o
    # focinho dentro da terra. Quando o queixo nao cabe, a cabeca SOBE ate caber.
    if queixo(0.0) < ALTURA_DO_QUEIXO_APOIADO:
        passos = int(-TOMBO_MAXIMO_DA_CABECA * 4.0)
        for i in range(passos + 1):
            total = -TOMBO_MAXIMO_DA_CABECA * i / passos
            if queixo(total) >= ALTURA_DO_QUEIXO_APOIADO:
                return num(total * a), num(total * b)
        raise SystemExit(
            "deitado, o queixo nao sai do chao nem com a cabeca erguida %.0f"
            " graus. O corpo esta deitando fundo demais para o pescoco deste"
            " modelo." % -TOMBO_MAXIMO_DA_CABECA)

    passos = int(TOMBO_MAXIMO_DA_CABECA * -4.0)
    melhor = (0.0, 0.0)
    for i in range(passos + 1):
        total = TOMBO_MAXIMO_DA_CABECA * i / passos
        if queixo(total) < ALTURA_DO_QUEIXO_APOIADO:
            break
        melhor = (num(total * a), num(total * b))
    return melhor


def altura_do_focinho_parado(geometria):
    """Onde o focinho esta quando o bicho esta em pe. E a regua do farejo."""
    return chao(geometria, pose_neutra(), ("head",))


def dianteiras_que_seguram(geometria, curvatura):
    """Quanto a dianteira abre para a PATA CONTINUAR NO CHAO com o tronco baixo.

    Inclinar o tronco sobre o quadril afunda o ombro, e com ele o pivot das duas
    pernas da frente -- que sao filhas do `body`. Sem compensar, o urso fareja com
    meia pata dentro do bloco. E a compensacao NAO e "gire a perna o mesmo tanto":
    a pata e um pe comprido, o calcanhar dela desce quando a perna vai a frente, e
    o angulo que zera a conta e maior do que a intuicao diz.

    Por isso ele e PROCURADO, na cinematica de verdade, e nao escrito. Um angulo
    decorado aqui ficaria certo neste geo e enterraria a pata no dia em que a
    perna mudasse de comprimento -- sem erro nenhum, como sempre.
    """
    def sob_o_chao(abertura):
        p = pose_neutra()
        p["body"]["rotation"] = vetor(x=-curvatura)
        for perna in ("leg_front_left", "leg_front_right"):
            p[perna]["rotation"] = vetor(x=abertura)
            p[PATA[perna]]["rotation"] = vetor(x=abertura * CONTRA_PATA)
        return chao(geometria, p, ("leg_front_left", "leg_front_right"))

    lo, hi = 0.0, ABERTURA_MAXIMA_DA_DIANTEIRA
    if sob_o_chao(hi) < 0.0:
        raise SystemExit(
            "com o tronco a %.0f graus a pata dianteira continua abaixo do chao"
            " mesmo com a perna aberta a %.0f. O tronco esta inclinando demais"
            " para o comprimento desta perna: baixe CURVATURA_DO_FAREJO ou"
            " converse com a lane do geo."
            % (curvatura, ABERTURA_MAXIMA_DA_DIANTEIRA))
    for _ in range(60):
        meio = (lo + hi) / 2.0
        if sob_o_chao(meio) < 0.0:
            lo = meio
        else:
            hi = meio
    return (lo + hi) / 2.0


def focinho_ate_o_chao(geometria, base):
    """(pescoco_x, cabeca_x) que poem o FOCINHO perto do chao. Achado, nao escrito.

    Nao ha formula fechada e NAO HA MONOTONIA: passado o fundo do arco, continuar
    girando o pescoco levanta o focinho de volta. Uma bisseccao aqui "convergiria"
    para o limite do intervalo e devolveria um urso com a cara virada para cima --
    e devolveria calada, que e o pior jeito de errar. Entao a busca e uma
    VARREDURA, e ela para no PRIMEIRO angulo que chega: o menor que resolve, e nao
    o maior que ainda cabe.

    Reprovar aqui e uma mensagem para a LANE DO GEO, nao para esta: quer dizer que
    o pescoco modelado nao alcanca, e nenhum angulo desta lane conserta.
    """
    a, b = REPARTICAO_DO_FOCINHO
    de_pe = altura_do_focinho_parado(geometria)
    alvo = de_pe * (1.0 - FRACAO_QUE_O_FOCINHO_FECHA)

    def baixo(total):
        p = {osso: dict(canais) for osso, canais in base.items()}
        p["neck"] = dict(base["neck"])
        p["head"] = dict(base["head"])
        p["neck"]["rotation"] = vetor(x=total * a)
        p["head"]["rotation"] = vetor(x=total * b)
        return chao(geometria, p, ("head",))

    passos = int(ANGULO_MAXIMO_DO_PESCOCO * -4.0)  # 0.25 grau de resolucao
    melhor = None
    for i in range(passos + 1):
        total = ANGULO_MAXIMO_DO_PESCOCO * i / passos
        altura = baixo(total)
        if melhor is None or altura < melhor[0]:
            melhor = (altura, total)
        if altura <= alvo:
            return num(total * a), num(total * b)

    raise SystemExit(
        "o focinho para a %.1f px do chao no melhor angulo possivel (%.0f graus de"
        " pescoco+cabeca, com o tronco ja inclinado %.0f). Parado ele esta a %.1f"
        " px, e farejar pede fechar %.0f%% dessa distancia -- ou seja, chegar a"
        " %.1f px. O modelo nao alcanca: isto e uma correcao do GEO (pescoco mais"
        " longo, ou pivot do pescoco mais a frente), e nenhum angulo desta lane"
        " conserta." % (melhor[0], melhor[1], CURVATURA_DO_FAREJO, de_pe,
                        FRACAO_QUE_O_FOCINHO_FECHA * 100.0, alvo))


def alcance_da_garra(geometria, clipe, amostras=24):
    """Quanto a pata dianteira direita passa da FRENTE DA HITBOX, em px.

    Positivo = a garra sai da caixa de colisao, que e o que faz um golpe ler como
    golpe. Medido no clipe, ao longo do clipe -- e nao no angulo, que e so o
    meio.
    """
    fim = clipe["animation_length"]
    mais_a_frente = min(
        frente(geometria, quadro_em(clipe, fim * i / amostras),
               ("leg_front_right",))
        for i in range(amostras + 1))
    return FRENTE_DA_HITBOX - mais_a_frente


# ------------------------------------------------------------------ portoes


def conferir_a_hierarquia(geometria):
    """O geo tem de ter A ARVORE do contrato, e nao so os nomes dele.

    O portao generico do Java confere que todo `parent` existe. Ele NAO confere
    que `chest` pendura o pescoco e que `leg_*` pendura a pata -- e e essa
    heranca que a cinematica deste arquivo desconta. Uma perna pendurada no
    `chest` em vez do `body` receberia o agachamento duas vezes, e o urso
    avisando ficaria com as patas enterradas. Em silencio.
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
    """Os eixos se LEEM do geo. Decorados, produzem um urso ao contrario."""
    if pivot(geometria, "head")[1] < pivot(geometria, "body")[1]:
        raise SystemExit(
            "a cabeca esta abaixo do corpo (head y=%.1f, body y=%.1f). Todo este"
            " gerador supoe +Y = CIMA -- inclusive `assentar`, que poria o urso"
            " morto flutuando."
            % (pivot(geometria, "head")[1], pivot(geometria, "body")[1]))
    # A FRENTE SE DESCOBRE COM A CABECA E A CAUDA, que e como o contrato manda.
    cabeca = centro(geometria, "head", 2)
    cauda = centro(geometria, "tail", 2)
    if not cabeca < cauda:
        raise SystemExit(
            "a cabeca esta em z=%.1f e a cauda em z=%.1f: a frente nao e -Z. Com o"
            " eixo invertido, erguer nas traseiras joga o bicho para TRAS, o golpe"
            " de garra bate atras dele e o aviso projeta a cabeca para longe do"
            " jogador -- tudo sem um unico erro no log." % (cabeca, cauda))
    if centro(geometria, "jaw", 2) >= pivot(geometria, "jaw")[2]:
        raise SystemExit(
            "a mandibula nao se estende a frente da propria dobradica (massa em"
            " z=%.1f, pivot em z=%.1f). Assim 'abrir a boca' (X negativo) FECHA a"
            " boca, e o rosnado do aviso vira um urso de bico fechado."
            % (centro(geometria, "jaw", 2), pivot(geometria, "jaw")[2]))
    if centro(geometria, "ear_left", 0) <= 0 or centro(geometria, "ear_right", 0) >= 0:
        raise SystemExit(
            "ear_left esta em x=%.1f e ear_right em x=%.1f, e o contrato supoe"
            " +X = ESQUERDA. Com o sinal trocado as orelhas se CRUZAM ao ir para"
            " tras." % (centro(geometria, "ear_left", 0),
                        centro(geometria, "ear_right", 0)))
    if centro(geometria, "ear_left", 1) <= centro(geometria, "head", 1):
        raise SystemExit(
            "a orelha esquerda esta em y=%.1f e o centro da cabeca em %.1f: a"
            " orelha nao esta em CIMA da cabeca. `cair` (o sono) tomba a ponta"
            " para fora supondo massa acima do pivot; modelada embaixo, ela"
            " tomba para dentro do cranio."
            % (centro(geometria, "ear_left", 1), centro(geometria, "head", 1)))
    for esquerda, direita in (("leg_front_left", "leg_front_right"),
                              ("leg_back_left", "leg_back_right")):
        if centro(geometria, esquerda, 0) <= 0 or centro(geometria, direita, 0) >= 0:
            raise SystemExit(
                "'%s' esta em x=%.1f e '%s' em x=%.1f: o par nao esta espelhado em"
                " torno de x=0. A animacao escreve esquerda +v e direita -v; com o"
                " geo assim o urso planta uma pata para fora e a outra para dentro."
                % (esquerda, centro(geometria, esquerda, 0), direita,
                   centro(geometria, direita, 0)))
    if not centro(geometria, "leg_front_left", 2) < centro(geometria, "leg_back_left", 2):
        raise SystemExit(
            "a perna 'da frente' esta em z=%.1f e a 'de tras' em z=%.1f: elas estao"
            " trocadas. A marcha cruzada defasa os pares pelo NOME; trocados, o"
            " urso anda com as duas pernas do mesmo lado juntas."
            % (centro(geometria, "leg_front_left", 2),
               centro(geometria, "leg_back_left", 2)))
    for perna, pata in PATA.items():
        if centro(geometria, pata, 1) >= centro(geometria, perna, 1):
            raise SystemExit(
                "'%s' esta em y=%.1f e '%s' em y=%.1f: a pata nao esta ABAIXO da"
                " perna. A contra-rotacao da pata supoe que ela e o pe; invertida,"
                " ela gira a sola para o jogador a cada passada."
                % (pata, centro(geometria, pata, 1), perna,
                   centro(geometria, perna, 1)))


def conferir_que_o_modelo_cabe_na_hitbox(geometria):
    """O bicho PARADO tem de ter o tamanho que o servidor anuncia.

    Este portao nao e sobre animacao, e a mensagem diz isso: quem reprova aqui e
    o GEO. Ele esta neste arquivo porque e aqui que existe a cinematica que sabe
    medir, e porque toda a escada de silhueta abaixo e expressa em fracao desta
    altura -- uma base errada aprovaria uma escada errada inteira.
    """
    parado = topo(geometria, pose_neutra())
    baixo, alto = FAIXA_DA_ALTURA_PARADA
    if not baixo * HITBOX_ALTURA_PX <= parado <= alto * HITBOX_ALTURA_PX:
        raise SystemExit(
            "parado, o modelo mede %.1f px de altura e a hitbox declara %.1f px"
            " (sized(1.4F, 1.35F)): %.2fx, fora de [%.2f, %.2f]. Silhueta que nao"
            " corresponde a caixa de colisao e o bug que esta migracao inteira"
            " existe para corrigir -- o jogador mira onde ve, e o servidor"
            " responde onde a caixa esta."
            % (parado, HITBOX_ALTURA_PX, parado / HITBOX_ALTURA_PX, baixo, alto))


def conferir_a_escada_de_silhueta(geometria, animacoes):
    """A ESCADA: dormindo < avisando < parado < ERGUIDO. Em px, medida nos clipes.

    Cada degrau tem uma consequencia propria, e por isso cada um tem a sua
    mensagem: reprovar com "a escada esta errada" mandaria a proxima pessoa
    procurar em quatro lugares de uma vez.
    """
    def alt(clipe):
        return altura_do_clipe(geometria, animacoes["animation.%s.%s" % (MOB, clipe)])

    parado = alt("idle")
    if alt("sleep") > parado * TETO_DO_SONO:
        raise SystemExit(
            "dormindo o urso mede %.1f px e parado %.1f px (%.2fx, teto %.2f). Um"
            " bicho deitado tem de LER como deitado a distancia: se ele mal baixa,"
            " o jogador nao distingue 'dormindo' de 'parado' e perde a unica"
            " chance que este mob da de passar sem acordar nada."
            % (alt("sleep"), parado, alt("sleep") / parado, TETO_DO_SONO))
    if alt("warn") > parado * TETO_DO_AVISO:
        raise SystemExit(
            "avisando de perto o urso mede %.1f px e parado %.1f px (%.2fx, teto"
            " %.2f). O `warn` tem de ENCOLHER o bicho: e desse encolhimento que a"
            " escalada tira a altura toda. Um aviso ja grande faz o `rear_warn`"
            " parecer um passo, e a escalada deixa de escalar."
            % (alt("warn"), parado, alt("warn") / parado, TETO_DO_AVISO))
    if alt("rear_warn") < parado * PISO_DA_ESCALADA:
        raise SystemExit(
            "erguido o urso mede %.1f px e parado %.1f px (%.2fx, piso %.2f). A"
            " escalada nao escala: 'ergue-se parcialmente nas traseiras e encara"
            " de cima' e a frase inteira que este clipe existe para cumprir, e sem"
            " altura ela e so uma pose."
            % (alt("rear_warn"), parado, alt("rear_warn") / parado,
               PISO_DA_ESCALADA))
    teto = HITBOX_ALTURA_PX * TETO_DA_ESCALADA_SOBRE_A_HITBOX
    if alt("rear_warn") > teto:
        raise SystemExit(
            "erguido o urso mede %.1f px sobre uma hitbox de %.1f px (%.2fx, teto"
            " %.2f). A escalada e display e display pode passar da caixa -- mas"
            " passar TANTO devolve o bug do corpo emprestado: a silhueta passa a"
            " mentir sobre o tamanho do bicho durante os 80 ticks em que o jogador"
            " esta justamente decidindo a que distancia e seguro ficar."
            % (alt("rear_warn"), HITBOX_ALTURA_PX,
               alt("rear_warn") / HITBOX_ALTURA_PX,
               TETO_DA_ESCALADA_SOBRE_A_HITBOX))


def conferir_que_a_escalada_le_de_doze_blocos(geometria, animacoes):
    """O PORTAO QUE LIGA O SERVIDOR A TELA.

    `FoxbearTerritory.TERRITORY_RADIUS` e 12 blocos. E dessa distancia que o
    jogador entra no territorio, e e la que a decisao de recuar ainda e barata.
    Um aviso que so se le de tres blocos avisa DEPOIS que a decisao foi tomada --
    e o servidor, que continua certo, vai punir alguem que nunca teve o dado.

    Entao a escalada e medida em GRAUS DE ALTURA APARENTE na borda do raio, e
    nao em graus de rotacao do tronco. Os dois nao sao a mesma coisa: 48 graus de
    tronco num urso de pescoco curto podem nao dar um pixel de diferenca la.

    O `warn` NAO passa por aqui, e isso e declarado: ele e o aviso de perto, e o
    trabalho dele (orelha, rosnado, patada) e de leitura proxima por natureza.
    Quem fala com quem esta longe e a escalada.
    """
    parado = altura_do_clipe(geometria, animacoes["animation.%s.idle" % MOB])
    erguido = altura_do_clipe(geometria, animacoes["animation.%s.rear_warn" % MOB])
    graus = math.degrees(math.atan2(erguido - parado, RAIO_EM_PX))
    if graus < GRAUS_MINIMOS_DE_ESCALADA:
        raise SystemExit(
            "a escalada cresce %.1f px sobre o urso parado, o que a %.0f blocos"
            " (%.0f px) da %.2f graus de altura aparente -- abaixo do minimo de"
            " %.2f. Na borda do territorio o jogador nao ve diferenca nenhuma"
            " entre o urso parado e o urso avisando, e o servidor vai puni-lo por"
            " uma pista que nunca chegou."
            % (erguido - parado, RAIO_DO_TERRITORIO, RAIO_EM_PX, graus,
               GRAUS_MINIMOS_DE_ESCALADA))


def conferir_que_as_dianteiras_saem_do_chao(geometria, animacoes):
    """'Patas dianteiras no ar' e a metade do `rear_warn` que se pode medir.

    Ele reprova o erro obvio e completamente silencioso: girar o tronco sem
    contra-girar as traseiras. O urso sobe, a silhueta cresce, TODOS os portoes
    de altura ficam verdes -- e as patas dianteiras continuam raspando o chao,
    porque o que subiu foi o dorso. Na tela isso le como o bicho se espreguicando.
    """
    clipe = animacoes["animation.%s.rear_warn" % MOB]
    alto = quadro_em(clipe, 0.9)  # o meio do trecho em que ele fica erguido
    for perna in ("leg_front_left", "leg_front_right"):
        altura = chao(geometria, alto, (perna,))
        if altura < DIANTEIRA_MINIMA_NO_AR:
            raise SystemExit(
                "no alto do `rear_warn` a '%s' chega a %.1f px do chao, e o minimo"
                " e %.1f. Erguido nas traseiras quer dizer APOIADO nas traseiras:"
                " com a dianteira raspando, o bicho nao esta ameacando, esta se"
                " espreguicando." % (perna, altura, DIANTEIRA_MINIMA_NO_AR))
    # E o contrario tambem: as traseiras tem de continuar NO chao.
    for perna in ("leg_back_left", "leg_back_right"):
        altura = chao(geometria, alto, (perna,))
        if altura > 1.5:
            raise SystemExit(
                "no alto do `rear_warn` a '%s' esta a %.1f px do chao: o urso"
                " erguido esta FLUTUANDO. `assentar` mede a pose inteira; se a"
                " traseira ficou no ar, quem esta encostando o chao e outra coisa"
                " (a cauda, o focinho) e a pose esta errada." % (perna, altura))


def conferir_que_a_pata_BATE(geometria, animacoes):
    """A PATADA DO AVISO -- o gesto que a diretriz cita nominalmente.

    Ele morde dos dois lados, porque ha dois jeitos de perde-lo e nenhum deles da
    erro:

      (a) a pata mal sai do chao -> nao ha patada, ha um tremor;
      (b) ela sobe e desce no mesmo tempo -> ha um PASSO, e passo nao avisa nada.

    (b) e o que se perde sozinho. Interpolacao linear entre duas chaves simetricas
    e o caminho de menor esforco, e produz exatamente o gesto errado.
    """
    clipe = animacoes["animation.%s.warn" % MOB]
    fim = clipe["animation_length"]
    amostras = 60
    alturas = [(fim * i / amostras,
                chao(geometria, quadro_em(clipe, fim * i / amostras),
                     ("leg_front_right",)))
               for i in range(amostras + 1)]
    t_alto, alto = max(alturas, key=lambda par: par[1])
    baixo = min(h for _, h in alturas)
    if alto - baixo < LEVANTADA_MINIMA_DA_PATA:
        raise SystemExit(
            "no `warn` a pata dianteira direita varia %.1f px de altura (de %.1f a"
            " %.1f), e o minimo e %.1f. 'Bate a pata no chao' precisa de uma pata"
            " que SAIA do chao; com essa amplitude o gesto que a diretriz cita"
            " nominalmente nao existe na tela."
            % (alto - baixo, baixo, alto, LEVANTADA_MINIMA_DA_PATA))
    # O tempo entre o ponto mais alto e o PRIMEIRO fundo depois dele: a pancada.
    #
    # Primeiro fundo, e nao o menor valor do clipe. A diferenca parece
    # burocratica e nao e: o corpo respira durante o aviso, e a respiracao leva a
    # pata a quase meio pixel ABAIXO de onde ela aterrissou, la pelos 0,9s.
    # Procurar o minimo global encontraria esse vale -- e mediria a pancada como
    # se ela durasse meio segundo, reprovando uma patada que esta correta.
    depois = [(t, h) for t, h in alturas if t > t_alto]
    if not depois:
        raise SystemExit(
            "no `warn` a pata dianteira direita termina o clipe no ponto mais"
            " alto: ela sobe e nunca desce. Nao ha pancada nenhuma.")
    t_baixo = depois[-1][0]
    for (t, h), (_, seguinte) in zip(depois, depois[1:]):
        if seguinte > h:
            t_baixo = t
            break
    descida = t_baixo - t_alto
    subida = t_alto if t_alto > 0 else fim
    if descida <= 0 or subida / descida < RAZAO_MINIMA_DA_PANCADA:
        raise SystemExit(
            "no `warn` a pata sobe em %.2fs e desce em %.2fs (%.2fx, minimo"
            " %.2fx). Subir e descer no mesmo tempo e um PASSO. O que faz uma"
            " pancada e a assimetria: ostensiva na subida, seca na descida -- e"
            " ela nao aparece em nenhum diff, so na tela."
            % (subida, descida, (subida / descida) if descida > 0 else 0.0,
               RAZAO_MINIMA_DA_PANCADA))


def conferir_que_o_golpe_sai_da_silhueta(geometria, animacoes):
    """O `claw_swipe` tem de PASSAR da frente da hitbox -- e nao muito alem.

    Curto demais, o golpe acontece dentro do proprio corpo e le como o urso se
    sacudindo. Longo demais, ele promete um alcance que o `MeleeAttackGoal` nao
    entrega, e o jogador aprende a recuar meio bloco a menos do que precisa.
    """
    alcance = alcance_da_garra(
        geometria, animacoes["animation.%s.claw_swipe" % MOB])
    if alcance < FOLGA_MINIMA_DA_GARRA:
        raise SystemExit(
            "no auge do golpe a garra passa %.1f px da frente da hitbox (%.1f px),"
            " e o minimo e %.1f. Um golpe que nao sai da silhueta nao le como"
            " golpe: le como o bicho se sacudindo, e o jogador nao aprende a"
            " recuar do que nao viu vir." % (alcance, FRENTE_DA_HITBOX,
                                             FOLGA_MINIMA_DA_GARRA))
    if alcance > FOLGA_MAXIMA_DA_GARRA:
        raise SystemExit(
            "no auge do golpe a garra passa %.1f px da frente da hitbox, e o teto"
            " e %.1f. A animacao esta prometendo um alcance que o servidor nao"
            " tem: quem recuar ate a borda do que VIU vai continuar sendo"
            " atingido -- ou, pior, vai parar de recuar cedo demais."
            % (alcance, FOLGA_MAXIMA_DA_GARRA))


def conferir_que_o_focinho_chega_ao_chao(geometria, animacoes):
    """`idle_sniff` e uma promessa em uma palavra: ele FAREJA.

    Os angulos vem de `focinho_ate_o_chao`, que ja mede -- mas mede uma POSE. Este
    portao mede o CLIPE, que e o que o jogador ve, e por isso ele pega o erro que
    a bisseccao nao pega: uma curva posterior sobrescrevendo o pescoco e
    levantando a cara do bicho sem que nada mais mude.
    """
    clipe = animacoes["animation.%s.idle_sniff" % MOB]
    parado = altura_do_focinho_parado(geometria)
    alvo = parado * (1.0 - FRACAO_QUE_O_FOCINHO_FECHA)
    fim = clipe["animation_length"]
    amostras = 30
    menor = min(chao(geometria, quadro_em(clipe, fim * i / amostras), ("head",))
                for i in range(amostras + 1))
    if menor > alvo + 1.0:
        raise SystemExit(
            "no `idle_sniff` o focinho nao passa de %.1f px do chao, e farejar"
            " pede %.1f (fechar %.0f%% dos %.1f px que ele tem parado). Alguma"
            " curva do clipe esta levantando a cabeca depois que a pose-base a"
            " abaixou -- e 'a cabeca meio baixa' le como o urso olhando para os"
            " proprios pes, que nao e o gesto."
            % (menor, alvo, FRACAO_QUE_O_FOCINHO_FECHA * 100.0, parado))
    minimo = altura_da_pata(geometria)
    if parado - menor < minimo:
        raise SystemExit(
            "entre `idle` e `idle_sniff` o focinho desce %.1f px, e o minimo e"
            " %.1f (a espessura da pata deste modelo -- a menor coisa que da para"
            " distinguir nele). As duas idles ficam iguais, e a variacao que faz o"
            " bicho parecer vivo quando ninguem esta olhando deixa de existir."
            % (parado - menor, minimo))


def conferir_que_o_sono_respira_fundo_e_devagar(animacoes):
    """Dormir e um ANDAMENTO, nao uma pose.

    Os dois numeros existem em par: amplitude sem lentidao le como ofegante,
    lentidao sem amplitude le como estatua. Nenhum dos dois erros da erro, e os
    dois destroem a mesma coisa -- a unica leitura que o `sleep` oferece.
    """
    if FOLEGO_DORMINDO <= FOLEGO_PARADO:
        raise SystemExit(
            "o folego dormindo (%.2f px) nao e mais AMPLO que o parado (%.2f px)."
            % (FOLEGO_DORMINDO, FOLEGO_PARADO))
    if CICLO_RESPIRACAO_DORMINDO <= CICLO_RESPIRACAO:
        raise SystemExit(
            "o ciclo dormindo (%.2fs) nao e mais LENTO que o parado (%.2fs)."
            % (CICLO_RESPIRACAO_DORMINDO, CICLO_RESPIRACAO))
    for clipe, periodo in (("sleep", CICLO_RESPIRACAO_DORMINDO),
                           ("idle", CICLO_RESPIRACAO)):
        voltas = animacoes["animation.%s.%s" % (MOB, clipe)]["animation_length"] \
            / periodo
        if abs(voltas - round(voltas)) > 1e-6:
            raise SystemExit(
                "%s tem %.3f respiracoes: o loop salta a cada volta, uma vez por"
                " clipe, para sempre." % (clipe, voltas))


def conferir_que_o_sono_deita_de_verdade(geometria, animacoes):
    """Deitado, quem encosta no chao tem de ser A BARRIGA.

    Este e o portao que separa dormir de agachar, e ele existe porque a diferenca
    entre os dois nao aparece em pose nenhuma: as duas tem as quatro pernas
    dobradas e a cabeca baixa. O que muda e ONDE o peso esta. Se a perna recolhida
    ainda pendurar abaixo do tronco, `assentar` apoia o bicho NELA, a barriga fica
    boiando, e o resultado e um urso agachado com cara de sono -- com o clipe
    rodando, a respiracao certa e todos os outros portoes verdes.

    A escada de silhueta pega o caso grosseiro (um urso "deitado" com 85% da
    altura do urso em pe). Este pega o caso fino, que e o que sobra depois de
    alguem "quase" consertar o grosseiro.
    """
    clipe = animacoes["animation.%s.sleep" % MOB]
    fim = clipe["animation_length"]
    amostras = 24
    barriga = min(chao_da_barriga(geometria, quadro_em(clipe, fim * i / amostras))
                  for i in range(amostras + 1))
    if barriga > FOLGA_DA_BARRIGA_NO_CHAO:
        raise SystemExit(
            "dormindo, a barriga do urso nunca chega a menos de %.1f px do chao (o"
            " limite e %.1f). Ele nao esta deitado: esta apoiado em outra coisa --"
            " quase sempre a perna recolhida, que continua pendurando abaixo do"
            " tronco. Na tela isso e um urso agachado, e nada mais acusa."
            % (barriga, FOLGA_DA_BARRIGA_NO_CHAO))


def conferir_que_o_aviso_cabe_na_janela_do_servidor(animacoes):
    """Os dois avisos repetem DENTRO de 80 ticks -- e tem de caber inteiros.

    `FoxbearEntity.TICKS_MAXIMOS_DE_AVISO` e 80. Se o clipe nao dividir a janela,
    a ultima volta corta no meio e o aviso termina com um tranco no quadro exato
    em que o servidor estava decidindo se o foxbear vem ou desiste. E o pior
    lugar possivel para um tranco: e o unico quadro que o jogador estava lendo.
    """
    for clipe in ("warn", "rear_warn"):
        dur = animacoes["animation.%s.%s" % (MOB, clipe)]["animation_length"]
        voltas = JANELA_DO_AVISO / dur
        if abs(voltas - round(voltas)) > 1e-6:
            raise SystemExit(
                "`%s` dura %.2fs e a janela do servidor e %.2fs (%d ticks): cabem"
                " %.3f voltas. Escolha uma duracao que divida a janela, ou o aviso"
                " termina cortado no meio."
                % (clipe, dur, JANELA_DO_AVISO, TICKS_MAXIMOS_DE_AVISO, voltas))
    # E a escalada e mais LENTA que o aviso de perto. Amplo e lento le como
    # display; amplo e rapido le como o ataque ja comecando -- e aqui isso seria
    # uma promessa falsa, porque o servidor ainda esta em WARN.
    if DUR_REAR <= DUR_WARN:
        raise SystemExit(
            "a escalada (%.2fs) nao e mais lenta que o aviso de perto (%.2fs). Um"
            " display rapido le como investida comecando: o urso prometeria o"
            " ataque durante a janela em que recuar ainda resolve."
            % (DUR_REAR, DUR_WARN))


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
                    raise SystemExit(
                        "%s/%s: canal '%s'. Este mob so usa %s -- em particular"
                        " NAO usa `scale`, e a regua de silhueta deste arquivo"
                        " ignora escala. Um osso escalado continuaria sendo medido"
                        " como se nao fosse, e toda a escada de altura ficaria"
                        " verde medindo outra coisa."
                        % (nome, nome_osso, canal, list(CANAIS)))
                if not quadros:
                    raise SystemExit("%s/%s/%s: canal sem keyframe"
                                     % (nome, nome_osso, canal))
                for t, v in quadros.items():
                    if float(t) < 0 or float(t) > fim + 1e-9:
                        raise SystemExit("%s/%s: quadro %s fora de [0, %s]"
                                         % (nome, nome_osso, t, fim))
                    if len(v) != 3:
                        raise SystemExit("%s/%s: vetor %s nao tem tres eixos"
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
            raise SystemExit("%s move osso que nao existe: %s"
                             % (nome, desconhecidos))
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


# Os clipes que escrevem a POSE INTEIRA. `hurt` fica de fora, e so ele -- ver a
# docstring dele. Esta tupla e a lista de excecoes ao contrario: quem entrar nela
# esta prometendo que o clipe cita todos os 16 ossos animaveis.
CLIPES_DE_CORPO_INTEIRO = tuple(c for c in CLIPES if c != "hurt")


def conferir_que_o_corpo_inteiro_e_escrito(animacoes):
    """Com UM controller, osso que o clipe nao cita volta ao DEFAULT DO MODELO.

    Nao para onde estava: para a pose de bind. Entao um `claw_swipe` que
    esquecesse a orelha faria a orelha saltar para a frente no meio do golpe --
    exatamente quando ela deveria estar para tras -- e nada acusaria, porque o
    clipe roda, o golpe acontece e a orelha "some" por meio segundo.

    Por isso todo clipe de estado escreve os DEZESSEIS ossos animaveis. A unica
    excecao e `hurt`, e ela e decisao com motivo escrito.
    """
    for clipe in CLIPES_DE_CORPO_INTEIRO:
        citados = set(animacoes["animation.%s.%s" % (MOB, clipe)]["bones"])
        faltando = sorted(set(ANIMAVEIS) - citados)
        if faltando:
            raise SystemExit(
                "`%s` nao cita %s. Com um controller so, esses ossos voltam para a"
                " pose de BIND enquanto o clipe roda -- nao para onde estavam."
                " Ou o clipe os escreve, ou ele entra na lista de excecoes com o"
                " motivo ao lado, como o `hurt`." % (clipe, faltando))


def conferir_que_o_hurt_nao_toca_a_perna(animacoes):
    """O `hurt` deixar perna e pata de fora e DECISAO, e ela e invisivel no arquivo.

    Ver a docstring de `hurt`: dentro de um controller so, keyar as pernas e
    keya-las em zero dao o mesmo resultado ruim, e a versao parcial e a unica que
    fica CERTA no dia em que o flinch subir para um segundo controller.

    Sem este portao, a proxima pessoa abre o clipe, acha que faltou perna e
    "completa" -- destruindo o motivo sem nunca ve-lo escrito.
    """
    citados = set(animacoes["animation.%s.hurt" % MOB]["bones"])
    membros = citados & (set(PERNAS) | set(PATA.values()))
    if membros:
        raise SystemExit(
            "o `hurt` move %s. Ele dura 5 ticks e pode pegar o urso em plena"
            " corrida; tocar perna ali rouba a passada. Ele cobre tronco e cabeca"
            " de proposito, para poder subir a um segundo controller e sobrepor o"
            " flinch sem destruir a locomocao." % sorted(membros))


def conferir_que_as_orelhas_falam(animacoes):
    """AS ORELHAS SAO METADE DO VOCABULARIO DESTE BICHO.

    A diretriz cita orelha em tres clipes por nome -- girando no `idle_sniff`,
    para tras no `warn`, caidas no `sleep`. Nenhum deles custa geometria e
    qualquer um deles se perde sem deixar rastro: o clipe continua rodando, o
    bicho continua se mexendo, e a frase que a orelha dizia simplesmente nao e
    dita. Este portao exige que ela diga cada uma das tres, e que as tres sejam
    DIFERENTES entre si -- tres poses parecidas nao sao tres frases.
    """
    def pose_da_orelha(clipe, t):
        return quadro_em(animacoes["animation.%s.%s" % (MOB, clipe)],
                         t)["ear_left"]["rotation"]

    frases = {
        "idle_sniff": pose_da_orelha("idle_sniff", 0.4),
        "warn": pose_da_orelha("warn", 0.5),
        "sleep": pose_da_orelha("sleep", 3.0),
    }
    for clipe, valor in frases.items():
        if valor == vetor():
            raise SystemExit(
                "no `%s` a orelha esquerda esta na pose neutra. A diretriz cita a"
                " orelha nesse clipe por nome, e ela e o gesto mais barato deste"
                " arquivo: perde-la nao quebra nada e apaga uma frase inteira."
                % clipe)
    nomes = sorted(frases)
    for i in range(len(nomes)):
        for j in range(i + 1, len(nomes)):
            a, b = frases[nomes[i]], frases[nomes[j]]
            if max(abs(x - y) for x, y in zip(a, b)) < 10.0:
                raise SystemExit(
                    "a orelha faz quase a mesma coisa em `%s` (%s) e `%s` (%s).  As"
                    " tres poses sao tres FRASES diferentes -- atencao, ameaca e"
                    " sono -- e parecidas elas deixam de distinguir os estados."
                    % (nomes[i], a, nomes[j], b))


def conferir_o_loop_fecha(animacoes):
    """Clipe que repete tem de TERMINAR onde comecou, canal a canal.

    Um loop cujas pontas nao batem da um tranco a cada volta -- uma vez por
    segundo, para sempre. E o tipo de defeito que ninguem reporta porque ninguem
    consegue descrever: "tem alguma coisa estranha no urso".
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
        (clipe("warn"), 0.0, clipe("rear_warn"), 0.0,
         "o aviso ESCALA: ele ja esta baixo, de orelha para tras, quando sobe"),
        (clipe("rear_warn"), DUR_REAR, clipe("warn"), 0.0,
         "ele desce da escalada e continua avisando, sem passar pelo neutro"),
        (clipe("warn"), 0.0, clipe("short_charge"), 0.0,
         "ENGAGE: ele para de avisar e vem, e nao pode relaxar no caminho"),
        (clipe("short_charge"), DUR_CHARGE, clipe("run"), 0.0,
         "a investida curta desemboca na corrida"),
        (clipe("run"), 0.0, clipe("claw_swipe"), 0.0,
         "ele bate CORRENDO: o vizinho do golpe dos dois lados e a corrida"),
        (clipe("claw_swipe"), DUR_SWIPE, clipe("run"), 0.0,
         "e volta a correr"),
        (clipe("run"), 0.0, clipe("bite"), 0.0,
         "a mordida sai da mesma corrida"),
        (clipe("bite"), DUR_BITE, clipe("run"), 0.0,
         "e devolve a corrida"),
    )


def conferir_a_costura(animacoes):
    """A COSTURA E A FRONTEIRA MAIS PERIGOSA DESTA ENTREGA.

    O controller e UM SO, com transicao de alguns ticks, e o `bite` inteiro dura
    oito. Se as poses vizinhas forem iguais, a mistura vira um no-op e a troca
    acontece no quadro exato. Se nao forem, quase metade da mordida e gasta
    interpolando -- e quem levou a dentada nunca viu a boca fechar.

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
    deixar passar, este teste quebra antes de alguem publicar um urso que gasta
    metade da mordida em mistura.
    """
    estragado = json.loads(json.dumps(animacoes))
    clipe = estragado["animation.%s.bite" % MOB]
    chave = tempo(0.0)
    original = clipe["bones"]["head"]["rotation"][chave]
    clipe["bones"]["head"]["rotation"][chave] = [original[0] + 1.0,
                                                 original[1], original[2]]
    try:
        conferir_a_costura(estragado)
    except SystemExit:
        return
    raise SystemExit(
        "a costura ACEITOU uma mordida deslocada de 1 grau. O portao nao esta"
        " protegendo nada: revise conferir_a_costura e quadro_em.")


def conferir_que_a_escada_reprova(geometria, animacoes):
    """ALIMENTA a escada de silhueta com o erro que ela existe para pegar.

    Uma escalada que nao escala e o defeito mais caro que este arquivo pode ter,
    e o mais facil de nao notar: o clipe roda, o urso se mexe, todos os outros
    portoes ficam verdes. Aqui o `rear_warn` e achatado de proposito -- o tronco
    volta a zero -- e a escada TEM de recusar.
    """
    estragado = json.loads(json.dumps(animacoes))
    corpo = estragado["animation.%s.rear_warn" % MOB]["bones"]["body"]["rotation"]
    for chave in corpo:
        corpo[chave] = [0, 0, 0]
    try:
        conferir_a_escada_de_silhueta(geometria, estragado)
    except SystemExit:
        return
    raise SystemExit(
        "a escada de silhueta ACEITOU um `rear_warn` com o tronco achatado em"
        " zero. O portao nao esta protegendo nada: revise altura_do_clipe e"
        " transformar.")


def conferir_que_o_apoio_reprova(geometria, animacoes):
    """ALIMENTA o portao das traseiras com os DOIS erros que ele existe para pegar.

    Regua que nunca reprovou e carimbo -- e este portao e o mais facil de virar
    um. A primeira versao dele foi alimentada com um `rear_warn` de dianteiras
    zeradas e NAO reprovou, com toda a razao: com o tronco a 48 graus o ombro sobe
    tanto que a pata dianteira nao alcanca o chao em angulo nenhum. Aquele teste
    nao estava provando nada, e so a tentativa de estragar mostrou isso.

    Os dois casos abaixo sao os que de fato acontecem:

      (a) alguem baixa `ERGUER_NAS_TRASEIRAS` "so um pouco" e a escalada degenera
          num bicho inclinado para tras, de pata dianteira raspando o chao;
      (b) alguem tira a contra-rotacao das traseiras e o urso passa a girar
          INTEIRO em torno do quadril -- as traseiras saem do chao e ele fica
          flutuando com a mesma altura de silhueta que o portao de altura aprova.
    """
    def recusa(estrago, o_que):
        quebrado = json.loads(json.dumps(animacoes))
        estrago(quebrado["animation.%s.rear_warn" % MOB]["bones"])
        try:
            conferir_que_as_dianteiras_saem_do_chao(geometria, quebrado)
        except SystemExit:
            return
        raise SystemExit(
            "o portao das traseiras ACEITOU %s. Ele nao esta protegendo nada:"
            " revise conferir_que_as_dianteiras_saem_do_chao." % o_que)

    def achatar(bones):
        for quadro in bones["body"]["rotation"].values():
            quadro[0] = quadro[0] * 0.1

    def soltar_as_traseiras(bones):
        for perna in ("leg_back_left", "leg_back_right"):
            for quadro in bones[perna]["rotation"].values():
                quadro[0] = 0

    recusa(achatar, "uma escalada de 5 graus, com a pata dianteira no chao")
    recusa(soltar_as_traseiras, "um urso erguido com as traseiras no ar")


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


def conferir_que_nada_atravessa_o_chao(geometria, animacoes):
    """Nenhum clipe enterra o bicho.

    Um urso com meia pata dentro do bloco nao da erro, nao aparece no log e e a
    primeira coisa que um jogador nota. A tolerancia existe porque pata de
    quadrupede encosta no chao de verdade e a interpolacao linear passa um pouco
    do ponto; ela e pequena de proposito.
    """
    # A TOLERANCIA E A ESPESSURA DA PROPRIA PATA, lida do geo -- e nao um numero
    # escolhido. Ela nao e zero porque este rig NAO CONSEGUE ser zero: a perna e
    # um cubo rigido girando em torno do ombro, sem joelho, e qualquer gesto que
    # baixe o tronco desce a pata junto. Exigir zero obrigaria a um urso que nunca
    # agacha, nunca corre baixo e nunca deita -- ou seja, a desistir da metade da
    # entrega para deixar uma regua feliz.
    #
    # O que a tolerancia diz, entao, e exatamente isto: no pior quadro, o que
    # entra no bloco e A SOLA, nunca o tornozelo. E ESSE limite que separa "pata
    # assentando no chao", que ninguem nota, de "urso enterrado ate o meio da
    # perna", que e a primeira coisa que um jogador ve.
    #
    # PONTO CEGO DECLARADO: dentro dessa faixa, esta lane nao prova que o pe
    # encosta BEM. Isso segue humano.
    tolerancia = -altura_da_pata(geometria)
    for clipe in CLIPES:
        animacao = animacoes["animation.%s.%s" % (MOB, clipe)]
        fim = animacao["animation_length"]
        amostras = 24
        for i in range(amostras + 1):
            t = fim * i / amostras
            baixo = chao(geometria, quadro_em(animacao, t))
            if baixo < tolerancia:
                raise SystemExit(
                    "`%s` em t=%.2fs poe o ponto mais baixo do modelo em y=%.1f px"
                    " -- %.1f px dentro do bloco, e a sola inteira tem %.1f px. Nao"
                    " e a pata assentando: e o tornozelo dentro da terra. Nas poses"
                    " paradas quem mede o chao e `assentar` ou `pernas_que_agacham`,"
                    " e uma curva posterior sobrescrevendo a posicao do corpo desfaz"
                    " os dois; nas passadas, o culpado costuma ser o tronco baixo"
                    " demais para o comprimento da perna."
                    % (clipe, t, baixo, -baixo, -tolerancia))


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
    corrigir_sentido_de_z_em("foxbear", animacoes)
    destino = os.path.join(DIR_ANIM, MOB + ".animation.json")
    os.makedirs(DIR_ANIM, exist_ok=True)
    texto = serializar({"format_version": "1.8.0", "animations": animacoes})
    json.loads(texto)  # o que sai daqui tem de voltar como JSON, sempre.
    with open(destino, "w", encoding="utf-8", newline="\n") as saida:
        saida.write(texto + "\n")
    return destino


def tabela(geometria, animacoes):
    print("\n  %-32s %6s  %-19s %3s  %6s  %s"
          % ("clipe", "dur", "loop", "n", "topo", "ossos"))
    for clipe in CLIPES:
        a = animacoes["animation.%s.%s" % (MOB, clipe)]
        print("  %-32s %5.2fs  %-19s %3d  %5.1fpx  %s"
              % ("animation.%s.%s" % (MOB, clipe), a["animation_length"],
                 str(a["loop"]), len(a["bones"]),
                 altura_do_clipe(geometria, a),
                 ", ".join(sorted(a["bones"]))))


# ------------------------------------------------------------------- main


def medir(geometria):
    """Tudo que este gerador LE do geo antes de existir clipe, num lugar so.

    Um dicionario, e nao uma lista de parametros: estes numeros atravessam varios
    clipes, e uma assinatura de seis argumentos e onde alguem troca a ordem de
    dois floats sem que nada reclame.
    """
    base_do_farejo = pose_neutra()
    base_do_farejo["body"]["rotation"] = vetor(x=-CURVATURA_DO_FAREJO)
    focinho = focinho_ate_o_chao(geometria, base_do_farejo)
    return {"focinho": focinho, "farejo": pose_de_farejo(geometria, focinho)}


def main():
    geometria = carregar_geo()

    # Os portoes de GEOMETRIA vem primeiro, antes de qualquer clipe existir: eles
    # conferem as premissas de que todas as contas abaixo dependem, e falhar aqui
    # produz uma mensagem sobre o MODELO em vez de uma sobre a animacao.
    conferir_a_hierarquia(geometria)
    conferir_eixos(geometria)
    conferir_que_o_modelo_cabe_na_hitbox(geometria)

    m = medir(geometria)

    # A ORDEM DE CONSTRUCAO E A CADEIA DE COSTURAS. Os vizinhos leem o quadro um
    # do outro em vez de repetir a pose: e o que impede duas fontes para a mesma
    # verdade num lugar onde a divergencia custa um tranco de um quadro.
    clipe_warn = ordenar(warn(geometria))
    clipe_run = ordenar(run())
    pose_do_aviso = quadro_em(clipe_warn, 0.0)
    pose_da_corrida = quadro_em(clipe_run, 0.0)

    animacoes = {
        "animation.%s.idle" % MOB: ordenar(idle()),
        "animation.%s.idle_sniff" % MOB: ordenar(idle_sniff(m["farejo"], m["focinho"])),
        "animation.%s.walk" % MOB: ordenar(walk()),
        "animation.%s.run" % MOB: clipe_run,
        "animation.%s.warn" % MOB: clipe_warn,
        "animation.%s.rear_warn" % MOB: ordenar(
            rear_warn(geometria, pose_do_aviso, pose_erguida(geometria))),
        "animation.%s.claw_swipe" % MOB: ordenar(
            claw_swipe(geometria, pose_da_corrida)),
        "animation.%s.bite" % MOB: ordenar(bite(geometria, pose_da_corrida)),
        "animation.%s.short_charge" % MOB: ordenar(
            short_charge(geometria, pose_do_aviso, pose_da_corrida)),
        "animation.%s.hurt" % MOB: ordenar(hurt()),
        "animation.%s.sleep" % MOB: ordenar(sleep(pose_de_sono(geometria))),
        "animation.%s.death" % MOB: ordenar(death(geometria)),
    }

    conferir_clipes(animacoes)
    conferir_os_loops_do_contrato(animacoes)
    conferir_ossos(animacoes, geometria)
    conferir_ossos_com_volume(animacoes, geometria)
    conferir_que_o_corpo_inteiro_e_escrito(animacoes)
    conferir_que_o_hurt_nao_toca_a_perna(animacoes)
    conferir_que_so_transladam_os_declarados(animacoes)
    conferir_que_o_aviso_cabe_na_janela_do_servidor(animacoes)
    conferir_que_o_sono_respira_fundo_e_devagar(animacoes)
    conferir_que_o_sono_deita_de_verdade(geometria, animacoes)
    conferir_o_loop_fecha(animacoes)
    conferir_a_costura(animacoes)
    conferir_que_a_costura_reprova(animacoes)
    conferir_a_escada_de_silhueta(geometria, animacoes)
    conferir_que_a_escada_reprova(geometria, animacoes)
    conferir_que_a_escalada_le_de_doze_blocos(geometria, animacoes)
    conferir_que_as_dianteiras_saem_do_chao(geometria, animacoes)
    conferir_que_o_apoio_reprova(geometria, animacoes)
    conferir_que_a_pata_BATE(geometria, animacoes)
    conferir_que_o_golpe_sai_da_silhueta(geometria, animacoes)
    conferir_que_o_focinho_chega_ao_chao(geometria, animacoes)
    conferir_que_as_orelhas_falam(animacoes)
    conferir_que_nada_atravessa_o_chao(geometria, animacoes)

    print("escrito", escrever(animacoes))

    print("\nnumeros LIDOS do geo:")
    print("  focinho farejando      : pescoco %.1f  cabeca %.1f graus" % m["focinho"])
    print("  corpo erguido          : %+.2f px (assentar)"
          % pose_erguida(geometria)["body"]["position"][1])
    print("  corpo deitado          : %+.2f px (assentar)"
          % pose_de_sono(geometria)["body"]["position"][1])
    print("  alcance da garra       : %+.2f px alem da frente da hitbox"
          % alcance_da_garra(geometria,
                             animacoes["animation.%s.claw_swipe" % MOB]))

    # A ESCADA E IMPRESSA PARA SER OLHADA. Os portoes exigem a ordem e as margens
    # minimas; nenhum deles prova que o jogador distingue os degraus na tela. Essa
    # parte segue humana -- ver o relato e docs/testing/o-que-nao-provamos.md.
    parado = altura_do_clipe(geometria, animacoes["animation.%s.idle" % MOB])
    print("\nescada de silhueta (hitbox = %.1f px de altura):" % HITBOX_ALTURA_PX)
    for clipe in ("sleep", "warn", "idle", "rear_warn"):
        alt = altura_do_clipe(geometria,
                              animacoes["animation.%s.%s" % (MOB, clipe)])
        print("  %-10s %6.1f px   %.2fx o parado   %.2fx a hitbox"
              % (clipe, alt, alt / parado, alt / HITBOX_ALTURA_PX))
    erguido = altura_do_clipe(geometria,
                              animacoes["animation.%s.rear_warn" % MOB])
    print("  a ESCALADA cresce %.1f px = %.2f graus de altura aparente a %.0f"
          " blocos" % (erguido - parado,
                       math.degrees(math.atan2(erguido - parado, RAIO_EM_PX)),
                       RAIO_DO_TERRITORIO))

    tabela(geometria, animacoes)


if __name__ == "__main__":
    main()
