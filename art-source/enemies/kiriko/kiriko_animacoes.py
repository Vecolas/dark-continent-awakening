"""Gera os DEZ clipes do kiriko -- DUAS FORMAS, dois arquivos (Bedrock 1.8.0).

O QUE ESTE MOB E, E POR QUE ISSO VIRA A LANE INTEIRA DE ANIMACAO
-----------------------------------------------------------------
Todos os outros seis mobs deste repositorio atacam. O kiriko NAO: ele te AVALIA.
Aparece em forma humana, observa como voce se comporta durante 200 ticks e
decide. Quem saca arma ou fere um bicho pacifico na frente dele e REPROVADO --
ele revela a forma verdadeira e parte para cima. Quem espera em paz e APROVADO --
ele revela a forma verdadeira, reconhece o jogador e vai embora.

    O JOGADOR VENCE ESTE ENCONTRO NAO LUTANDO.

E o servidor nao tem como dizer isso. Nao ha barra de julgamento na tela, nao ha
numero de pontuacao, nao ha mensagem durante a observacao. A UNICA coisa que
informa o jogador de que ele esta sendo medido -- em vez de esperado por uma
emboscada -- e o clipe `observe`. E a unica coisa que diferencia "voce passou" de
"voce vai morrer", no instante em que as duas revelacoes mostram o MESMO corpo, e
o clipe que vem depois: `approve` ou `strike`.

Consequencia direta: se `observe` ler como ameaca, o jogador saca a arma e
reprova por causa da animacao. Se `approve` ler como ataque, o jogador ataca
primeiro e transforma uma aprovacao em luta. Nos dois casos a ficha do mob foi
ignorada -- e NENHUM portao deste repositorio ve a tela.

Por isso os portoes daqui nao medem "esta bonito". Eles medem afirmacoes:
`conferir_que_observe_e_atencao` (a cabeca lidera, o corpo nao, e o olhar PARA em
vez de varrer) e `conferir_que_approve_nao_e_ataque_nem_fuga` (o tronco nunca se
inclina para a frente, os bracos abrem para FORA e UMA vez so, e nada no clipe se
move mais depressa que o golpe).

POR QUE DOIS ARQUIVOS, E O ERRO FACIL DAQUI
--------------------------------------------
O contrato fixou dois ids proprios -- `kiriko` e `kiriko_disfarce` -- e nao um
modelo com metade dos ossos escondida. Sao dois conjuntos de ossos e dois
arquivos de animacao, e escrever o clipe de uma forma contra o geo da outra e o
erro mais facil desta lane.

Ele e quase invisivel porque {root, body, head, arm_*, leg_*} existe NAS DUAS
formas: um portao que so perguntasse "o osso existe no geo?" reprovaria o arquivo
humano contra o geo do bicho por UM osso (`hat`) e aprovaria todo o resto. Por
isso ha `conferir_ossos_exclusivos` (cada forma tem de CITAR osso que so ela tem)
e `conferir_que_trocar_os_pares_reprova`, que ALIMENTA o portao com os pares
trocados e exige que ele recuse os dois. Regua que nunca reprovou e carimbo.

A COSTURA, E POR QUE AQUI ELA E MAIS BARATA QUE NO MACACO
-----------------------------------------------------------
O renderer troca de MODELO quando `estaTransformando()` desliga. Nesse quadro o
jogo joga fora um esqueleto e monta outro, e nada no GeckoLib confere se as duas
poses combinam. Por isso a POSTURA do bicho mora aqui uma vez (`postura_comum`) e
e ao mesmo tempo o ULTIMO quadro de `kiriko_disfarce.transform` e o PRIMEIRO
quadro de TODO clipe de `kiriko`. `conferir_a_costura` reprova a divergencia.

A diferenca em relacao ao man-faced ape e de desenho, e ela e deliberada: o
macaco termina AGACHADO nos nos dos dedos, entao a costura dele atravessa uma
mudanca de estatura inteira. O kiriko termina ERETO. A ficha pede "postura ereta"
e "andar de bicho grande e consciente -- nao arrastado, nao bestial", e o
contrato manda as duas formas caberem na MESMA `sized(1.0F, 2.1F)`. Somadas, as
tres coisas dizem que o que muda na revelacao e o CORPO, nao a postura -- e e por
isso que a postura do kiriko fica perto de zero e a do macaco nao.

Isso nao torna a costura desnecessaria: torna a divergencia mais dificil de ver a
olho nu, que e pior. Um osso fora da postura aqui e um membro que endireita por
um quadro no exato instante em que o jogador esta olhando para a revelacao.

CONSEQUENCIA QUE NAO E OBVIA: TODO clipe keya TODO osso animavel, nas duas
formas. Osso que um clipe nao cita volta para o default do MODELO, nao para a
postura nem para a pose anterior. Um `hurt` que esquecesse a crista aprumaria a
crista por cinco ticks; um `observe` que esquecesse os bracos nao poderia
AFIRMAR que as maos estao quietas, que e metade do que aquele clipe existe para
dizer. `postura_base` e `base_humana` deitam a pose inteira antes de o clipe
escrever a parte dele, e `conferir_que_todo_clipe_cita_todo_osso_animavel` exige
isso de todos os dez. O unico osso que nenhum clipe toca e `root` -- ele e a
ancora que o renderer alinha com a hitbox, e girar a raiz move a silhueta para
fora da caixa em que o mob de fato e acertado.

TICKS: DE ONDE VEM CADA UM -- E O QUE ESTE ARQUIVO INVENTA
------------------------------------------------------------
  strike 10/5/14  COPIADO do servidor: HunterExamProfiles.kirikoStrike().
                  Se mudar la, muda aqui e regera. Um golpe cuja janela visivel
                  nao coincide com os 5 ticks em que o dano acontece e um mob que
                  MENTE sobre quando bate. Ver `conferir_a_janela_do_golpe`.
  observe 200     COPIADO do servidor: kirikoJulgamento().ticksDeObservacao. Nao
                  e a duracao do clipe (o clipe repete): e a janela que ele tem de
                  preencher, e e contra ela que `conferir_a_janela_de_observacao`
                  julga o comprimento do loop.
  transform 24    ESTE ARQUIVO E A FONTE, e isso e uma divida com a lane da
                  entidade. O contrato congelado NAO tem um numero de ticks de
                  transformacao em lugar nenhum -- kirikoJulgamento() nao carrega
                  um, e kirikoStrike() so fala do golpe. Entao TICKS_TRANSFORM
                  abaixo e a unica fonte, e a entidade precisa segurar
                  `estaTransformando()` por exatamente 24 ticks. Segurando menos,
                  a revelacao e cortada no meio e o corpo humano some antes de
                  acabar de se desfazer; segurando mais, o humano congela no
                  ultimo quadro, de pe, esperando -- e nenhuma das duas coisas da
                  erro no log. Esta declarado no relato desta entrega.

EIXOS -- CONFERIDOS no proprio geo, nao chutados
-------------------------------------------------
Convencao vanilla: y=0 no chao, -Z na FRENTE, +X a ESQUERDA do bicho.
`conferir_eixos` le os dois geos e reprova se eles discordarem disso -- e aqui ha
com o que conferir de verdade, porque a forma verdadeira tem bico (massa a
frente), crista (massa acima) e cauda (massa atras). Derivado dessa leitura, e so
dela:

    rotacao X positiva  -> o rosto (massa em -Z) SOBE; o BICO aponta para cima;
                           o topo do cranio e a CRISTA (massa em +Y) vao para
                           TRAS; a CAUDA (massa em +Z) DESCE; membro pendurado
                           balanca para a FRENTE
    rotacao X negativa  -> o rosto DESCE; o tronco INCLINA PARA A FRENTE; a
                           CRISTA se levanta para a frente (alerta); a CAUDA SOBE
    rotacao Y positiva  -> a cabeca (massa em -Z) varre para -X = a DIREITA do
                           bicho. E o eixo do "virar para ir embora"
    rotacao Z positiva  -> o lado esquerdo (+X) sobe; a mao pendurada vai para
                           FORA no braco esquerdo e para DENTRO no direito

E por isso que ABRIR OS BRACOS no `approve` e z POSITIVO na esquerda e NEGATIVO
na direita, e nunca o mesmo sinal nos dois -- com o mesmo sinal os bracos nao
abrem, eles varrem para o mesmo lado, que e um gesto de enxotar.

O EIXO Y E DEDUZIDO DA MAO DOS EIXOS, NAO OBSERVADO em jogo -- mesma suposicao
declarada pelas lanes da spider eagle e do master of the swamp, e pelo mesmo
motivo (regra da mao direita, ciclo X:(Y,Z), Y:(Z,X), Z:(X,Y)). Se em jogo o
kiriko virar para o lado errado ao ir embora, `SENTIDO_DE_Y = -1.0` inverte tudo
num lugar so.

O QUE ESTE GERADOR LE DO GEO, E POR QUE ESSES QUATRO
------------------------------------------------------
Nenhum numero de pose sai de medida. O que sai de medida sao as quatro
afirmacoes que a revelacao FAZ sobre os dois corpos -- e afirmacao sobre corpo se
mede no corpo, senao ela sobrevive a proxima correcao do modelo mentindo:

  ESCALA_DO_CORPO   "o corpo cresce". Lida da razao entre as alturas dos DOIS
                    geos, em torno do pivot do body. Decorada, ela fica certa
                    hoje e no dia em que o bicho ficar 4 px mais alto o humano
                    para de crescer ate ele -- e o modelo troca com um salto de
                    tamanho, que e exatamente o que a hitbox unica existe para
                    evitar.
  ESCALA_DO_BRACO   "os bracos alongam". Lida da razao entre os comprimentos de
                    braco dos dois geos, descontada a escala que o body ja
                    aplica (o braco e filho do body: as duas se MULTIPLICAM).
                    Se a forma verdadeira nao tiver braco proporcionalmente mais
                    longo que a humana, este numero reprova e diz isso -- porque
                    ai a batida de "os bracos alongam" nao tem o que mostrar.
  QUEDA_DO_CHAPEU   "o chapeu cai". Lida da distancia que o chapeu precisa
                    descer para o fundo dele passar do fundo da cabeca. Decorada,
                    um chapeu mais fundo pararia raspando o cranio: a revelacao
                    mostraria menos do que promete e nada acusaria.
  QUEDA_DA_MORTE    quanto o corpo desce ao tombar. Escrita a mao, o kiriko
                    morreria flutuando (ou enterrado) sem um erro no log.

E a HITBOX e a quinta regua, mas ela e CONTRATO e nao geo: `sized(1.0F, 2.1F)`.
`conferir_que_a_forma_cabe_na_caixa` morde dos dois lados nos dois geos, porque
os dois erros sao silenciosos -- forma maior que a caixa e um bicho acertavel
onde ele nao esta, e forma muito menor e um bicho que nao e acertado onde ele
parece estar.

Regerar:  python art-source/enemies/kiriko/kiriko_animacoes.py
Exporta:  .../animations/entity/kiriko.animation.json
          .../animations/entity/kiriko_disfarce.animation.json
"""
import json
import math
import os

# --------------------------------------------------------------- o contrato

DIR_GEO = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                       "geo", "entity")
DIR_ANIM = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                        "animations", "entity")

ID_VERDADEIRO = "kiriko"
ID_DISFARCE = "kiriko_disfarce"

# AS HIERARQUIAS SAO O CONTRATO, e sao a UNICA lista de ossos deste arquivo: os
# conjuntos saem daqui derivados. Repetir os nomes criaria duas fontes para a
# mesma verdade, e a divergencia se manifestaria como um membro parado -- sem
# erro, sem log, sem nada.
#
# `claw_*` filho de `arm_*` e `foot_*` filho de `leg_*` e o que faz a garra
# CONTRA-GIRAR uma fracao do membro e continuar apontada para baixo. Penduradas
# no `body`, elas receberiam so o proprio angulo, girariam soltas no ar durante a
# passada, e o portao generico do Java (que so confere que o pai existe)
# aprovaria.
PAI_DISFARCE = {
    "root": None,
    "body": "root",
    "head": "body",
    "hat": "head",
    "arm_left": "body",
    "arm_right": "body",
    "leg_left": "body",
    "leg_right": "body",
}
PAI_VERDADEIRO = {
    "root": None,
    "body": "root",
    "head": "body",
    "beak": "head",
    "crest": "head",
    "arm_left": "body",
    "claw_left": "arm_left",
    "arm_right": "body",
    "claw_right": "arm_right",
    "leg_left": "body",
    "foot_left": "leg_left",
    "leg_right": "body",
    "foot_right": "leg_right",
    "tail": "body",
}

OSSOS_DISFARCE = tuple(PAI_DISFARCE)
OSSOS_VERDADEIRO = tuple(PAI_VERDADEIRO)

# `root` NAO E ANIMADO, nas duas formas, e isso e decisao e nao esquecimento. Ele
# e a ancora que o renderer alinha com a hitbox de 1.0x2.1; girar ou transladar a
# raiz move a silhueta inteira para fora da caixa de colisao. Num mob cujo
# encontro inteiro depende de o jogador conseguir ler a distancia ate ele antes
# de decidir se saca alguma coisa, silhueta fora da caixa e pior que um bug de
# arte: e uma informacao errada bem no momento da decisao.
NAO_ANIMADOS = ("root",)
ANIMAVEIS_DISFARCE = tuple(o for o in OSSOS_DISFARCE if o not in NAO_ANIMADOS)
ANIMAVEIS_VERDADEIRO = tuple(o for o in OSSOS_VERDADEIRO if o not in NAO_ANIMADOS)

# Ossos que existem NAS DUAS formas -- e que por isso atravessam a troca de
# modelo. Derivado, nao listado: uma terceira lista para uma verdade que ja tem
# duas e como a divergencia entra.
COMUNS = tuple(o for o in ANIMAVEIS_DISFARCE if o in OSSOS_VERDADEIRO)

# Ossos que so uma das formas tem. Sao ELES que tornam a troca de pares
# detectavel; sem citar pelo menos um, um arquivo de animacao serve as duas
# formas por acidente.
EXCLUSIVOS_DISFARCE = tuple(o for o in ANIMAVEIS_DISFARCE
                            if o not in OSSOS_VERDADEIRO)
EXCLUSIVOS_VERDADEIRO = tuple(o for o in ANIMAVEIS_VERDADEIRO
                              if o not in OSSOS_DISFARCE)

CLIPES_DISFARCE = ("idle", "walk", "observe", "transform")
CLIPES_VERDADEIRO = ("idle", "walk", "strike", "approve", "hurt", "death")

# O TIPO DE REPETICAO MORA AQUI E SO AQUI. O codigo pede
# then(nome, Animation.LoopType.DEFAULT), e DEFAULT delega para o campo "loop" do
# clipe carregado -- entao este dicionario E o comportamento, e nao a
# documentacao dele.
LOOPS_DISFARCE = {
    # o viajante parado: e o repouso do disfarce e repete enquanto ele esperar.
    "idle": True,
    # caminhada: repete enquanto o servidor estiver movendo a entidade.
    "walk": True,
    # a avaliacao dura 200 ticks e o clipe dura 5s. Ele TEM de repetir: e o
    # estado, nao um gesto. Ver `conferir_a_janela_de_observacao`.
    "observe": True,
    # A REVELACAO acontece UMA vez, e termina no corpo que o proximo modelo
    # comeca. hold_on_last_frame, e nao loop nem false, pelo mesmo motivo
    # mecanico do macaco: em loop ele se transformaria de novo do zero; em false
    # ele voltaria a ser gente de pe no quadro anterior a virar bicho -- e esse e
    # o quadro em que o modelo troca.
    "transform": "hold_on_last_frame",
}
LOOPS_VERDADEIRO = {
    "idle": True,
    "walk": True,
    # o golpe acontece uma vez e TERMINA NA POSTURA, para o idle voltar sem salto.
    "strike": False,
    # A APROVACAO termina com o bicho ja virado para ir embora. `false` devolveria
    # o kiriko a pose neutra no ultimo quadro -- uma piscada de "ele se
    # desvirou" bem no quadro em que o jogador esta entendendo que passou.
    "approve": "hold_on_last_frame",
    "hurt": False,
    # o corpo caido fica caido. `false` poria o cadaver de pe por um quadro.
    "death": "hold_on_last_frame",
}

# Extremidade que CONTRA-GIRA a fracao do membro, para a garra continuar apontada
# para baixo e a sola continuar paralela ao chao em vez de apontar para o jogador.
EXTREMIDADE = {
    "arm_left": "claw_left", "arm_right": "claw_right",
    "leg_left": "foot_left", "leg_right": "foot_right",
}
CONTRA_GARRA = -0.45
CONTRA_PE = -0.40

# ------------------------------------------------------------------- ticks

TICKS_POR_SEGUNDO = 20.0

# COPIADOS do servidor: kirikoStrike() -> AttackDefinition("strike", 10, 5, 14, ...)
TICKS_WINDUP = 10
TICKS_ACTIVE = 5
TICKS_RECOVERY = 14

# COPIADO do servidor: kirikoJulgamento() -> RegrasDeJulgamento(200, ...)
TICKS_DE_OBSERVACAO = 200

# INVENTADO AQUI -- ver o cabecalho. Nao existe contrapartida no contrato
# congelado, entao esta constante e a fonte e a entidade a copia.
TICKS_TRANSFORM = 24

FIM_DO_WINDUP = TICKS_WINDUP / TICKS_POR_SEGUNDO                     # 0.50
FIM_DA_JANELA = (TICKS_WINDUP + TICKS_ACTIVE) / TICKS_POR_SEGUNDO    # 0.75
DUR_STRIKE = (TICKS_WINDUP + TICKS_ACTIVE
              + TICKS_RECOVERY) / TICKS_POR_SEGUNDO                  # 1.45
DUR_TRANSFORM = TICKS_TRANSFORM / TICKS_POR_SEGUNDO                  # 1.20
JANELA_DE_OBSERVACAO = TICKS_DE_OBSERVACAO / TICKS_POR_SEGUNDO       # 10.00

DUR_IDLE_HUMANO = 3.0
DUR_WALK_HUMANO = 1.0
DUR_OBSERVE = 5.0
DUR_IDLE_KIRIKO = 4.0
DUR_WALK_KIRIKO = 0.95
DUR_APPROVE = 1.5
DUR_HURT = 0.25
DUR_DEATH = 1.4

# --------------------------------------------------------------- a hitbox

# sized(1.0F, 2.1F), em pixels de modelo. Nao e botao de tuning: e a caixa que o
# servidor usa para tudo, e AS DUAS FORMAS CABEM NELA de proposito -- se a caixa
# mudasse entre as formas, o disfarce entregaria a si mesmo antes do corpo.
HITBOX_LARGURA_PX = 1.0 * 16.0   # 16.0
HITBOX_ALTURA_PX = 2.1 * 16.0    # 33.6

# Quanto da caixa o modelo pode DEIXAR DE OCUPAR. Um modelo muito menor que a
# propria hitbox e um bicho que e acertado no ar acima da cabeca -- e num mob que
# o jogador esta tentando NAO atacar, isso e pior que o contrario: um golpe
# acidental no vazio ainda conta como agressao.
OCUPACAO_MINIMA_DA_CAIXA = 0.80

# --------------------------------------------------------------- os eixos

# Ver o cabecalho: o Y e deduzido da mao dos eixos, nao observado em jogo.
SENTIDO_DE_Y = 1.0

# ------------------------------------------------------- a postura do bicho

# A POSTURA DA FORMA VERDADEIRA, em graus. Ela e a pose em que o `transform`
# TERMINA e em que todo clipe de `kiriko` COMECA -- ver `conferir_a_costura`.
#
# ELA E QUASE ERETA, E ISSO E A FICHA E NAO ECONOMIA. "Magical Beast != monster"
# e a frase que a secao 41 usa, e postura e onde essa frase vira imagem: um bicho
# curvado, com o peito abaixo da linha dos ombros, le como predador em posicao de
# bote qualquer que seja a animacao por cima. Os 3 graus de inclinacao existem
# para ele nao ficar rigido como um poste; nao para ele parecer pronto para
# atacar.
BODY_X = -3.0
# CABECA_X E POSITIVO e isso NAO e engano de sinal -- e a conta que a hierarquia
# obriga. O pescoco e filho do tronco, entao o angulo do rosto e a SOMA dos dois:
# com o tronco a -3, um +6 no pescoco deixa liquido +3. O rosto fica LEVEMENTE
# ACIMA da horizontal, que e como se carrega a cabeca quando se esta olhando para
# alguem de igual para igual.
#
# Escrever -6 aqui (o reflexo de "cabeca baixa") somaria -9 e daria um bicho que
# olha para o chao. Nao daria erro nenhum: daria um Magical Beast encabulado.
CABECA_X = 6.0
BRACO_X = 5.0     # bracos um pouco a frente do plano do tronco
BRACO_Z = 9.0     # e afastados do corpo: e a massa do bicho que nao cabe colada
PERNA_X = 3.0     # quase reta -- ele esta de pe, nao agachado
PERNA_Z = 3.0
# CAUDA_X NEGATIVO LEVANTA a cauda (a massa dela esta em +Z, ver EIXOS). Cauda
# arrastando no chao le como reptil pesado; o kiriko a carrega alta.
CAUDA_X = -12.0
# CRISTA_X NEGATIVO joga a massa (que esta em +Y) para a FRENTE: crista erguida e
# adiantada, que e a leitura de ALERTA. Ela ABAIXA (X positivo) no `strike`, e e
# essa inversao que avisa o golpe antes do braco se mexer.
CRISTA_X = -7.0
# O bico neutro nao aponta para lugar nenhum. Ele e UM osso, e nao uma mandibula:
# nao ha boca para abrir aqui, so um focinho para apontar. Chamar isto de "abrir
# a boca" seria prometer uma articulacao que o modelo nao tem.
BICO_X = 0.0

# O KIRIKO NAO AGACHA. O zero esta escrito, e nao omitido, porque ele e uma
# afirmacao: este bicho termina a revelacao com o quadril na mesma altura em que
# o humano estava. E o que permite as duas formas dividirem a hitbox sem que a
# troca de modelo mexa a silhueta verticalmente, e e o oposto do man-faced ape,
# que desce para os nos dos dedos. Omitido, alguem "melhora" a postura com um
# agachamento e desfaz a decisao sem saber que existia uma.
AGACHAMENTO = 0.0

# --------------------------------------------------------------- as marchas

# O que separa marcha de GENTE de marcha de BICHO, em quatro numeros -- e os
# quatro sao conferidos em `conferir_a_marcha`.
#
# ATENCAO: A REGRA AQUI E O INVERSO DA DO MAN-FACED APE, e copiar aquele portao
# para ca enforcaria a coisa errada. No macaco o BRACO tem de oscilar MAIS que a
# perna, porque ele se puxa pelos bracos longos. No kiriko a PERNA manda, nos
# dois corpos: a ficha pede "andar de bicho grande e consciente -- nao arrastado,
# nao bestial", e braco com amplitude de perna e exatamente o quadrupede-ish que
# ele nao e.
HUMANO_PERNA = 17.0
HUMANO_BRACO = 8.0
KIRIKO_PERNA = 21.0
KIRIKO_BRACO = 11.0

# Fase que faz a cossenoide VALER A BASE em t=0. cos(2*pi*0.75) = 0, e a curva
# sobe a partir dai. Qualquer outra fase quebraria a costura, porque o primeiro
# quadro de walk deixaria de ser a postura.
FASE_QUE_COMECA_NA_BASE = 0.75

# ---------------------------------------------- limites que os portoes usam

# --- observe ---------------------------------------------------------------
# "corpo imovel, maos quietas, cabeca acompanhando" vira quatro numeros.
OBSERVE_CORPO_MAXIMO = 2.5    # acima disto o corpo respira DEMAIS e vira idle
OBSERVE_BRACO_MAXIMO = 2.0    # acima disto as maos nao estao quietas
OBSERVE_CABECA_SOBRE_BRACO = 4.0  # a cabeca tem de liderar, e com folga
# Um varrimento largo demais le como vigia de horizonte -- alguem procurando
# QUALQUER COISA, que e o contrario de alguem medindo VOCE. E ele e tambem a
# quantidade que passaria do jogador no dia em que alguem acrescentar uma camada
# de head-tracking no renderer: ver o ponto cego declarado no relato.
OBSERVE_YAW_MAXIMO = 16.0
# O olhar tem de PARAR. Varrer sem parar e um radar; parar e fixar e um olhar.
OBSERVE_FRACAO_PARADA = 0.55
OBSERVE_LIMIAR_DE_PARADO = 2.0   # graus: abaixo disto o trecho conta como parado

# --- approve ---------------------------------------------------------------
# O tronco NUNCA passa da postura para a frente. Inclinar o peito na direcao de
# quem esta olhando e o que transforma qualquer gesto em investida, por mais
# amigavel que a mao esteja.
APPROVE_TOLERANCIA_DO_TRONCO = 0.5
# Os bracos abrem para FORA (Z), nao para a FRENTE (X). Em X eles viram o arco do
# golpe; a razao e o que separa "reconhecimento" de "bote".
APPROVE_RAZAO_FORA_SOBRE_FRENTE = 2.0
# UMA vez. Duas viram bater de asas, que em bicho e display de ameaca.
APPROVE_ABERTURAS = 1
# Ele VIRA para ir embora -- mas o MODELO so COMECA a virada; quem termina e a
# entidade. Um modelo que girasse 180 graus ficaria de costas dentro da propria
# hitbox, e o corpo desenhado passaria a discordar da caixa que o servidor usa.
APPROVE_YAW_MAXIMO = 55.0
# A virada e o gesto todo acontecem no ultimo terco, e nunca antes.
APPROVE_INICIO_DA_VIRADA = 2.0 / 3.0
# NADA no approve pode se mexer tao depressa quanto o golpe. E a regua certa para
# "nao pode parecer fuga": fuga e o mesmo corpo se movendo na velocidade do
# combate, so que para longe.
APPROVE_FRACAO_DA_VELOCIDADE_DO_GOLPE = 0.6

# --- transform -------------------------------------------------------------
# A revelacao NAO e um susto. Dois numeros seguram isso: nada pode passar da pose
# final (o corpo nao "quica" ao assentar, que e o que da o arranco), e o quadro
# mais rapido do clipe nao pode estar no comeco -- transformacao que estala nos
# primeiros seis ticks e jump scare, e este mob nao assusta, ele decide.
TRANSFORM_TOLERANCIA_DE_OVERSHOOT = 0.5
TRANSFORM_INICIO_PROTEGIDO = 0.25

# --- crescimento (lido do geo, limitado aqui) --------------------------------
# Abaixo disto "o corpo cresce" nao se ve, e a revelacao perde a metade fisica.
CRESCIMENTO_MINIMO = 1.06
# Acima disto o corpo humano se deforma antes de trocar, e a leitura vira balao
# inflando em vez de criatura largando um disfarce.
CRESCIMENTO_MAXIMO = 1.80
# O braco tem de alongar MAIS que o resto do corpo, senao "os bracos alongam" nao
# e uma batida da revelacao, e so o corpo inteiro ficando maior.
VANTAGEM_MINIMA_DO_BRACO = 1.02

# --- o chapeu ---------------------------------------------------------------
# Folga, em px, entre o fundo do chapeu caido e o fundo da cabeca. Sem ela o
# chapeu para RASPANDO o queixo e ainda cobre parte do cranio no quadro em que o
# modelo troca.
MARGEM_DO_CHAPEU = 1.5
# Abaixo disto ninguem ve o chapeu sair: ele so muda de angulo.
QUEDA_MINIMA_DO_CHAPEU = 2.0
# O tombo do chapeu enquanto cai. Ele nao precisa ser lido do geo porque nao
# afirma nada sobre o corpo: e so o objeto girando ao cair.
TOMBO_DO_CHAPEU = 112.0
RECUO_DO_CHAPEU = 3.0   # px para +Z: ele cai para TRAS, e nao em cima do rosto

# --- a morte ----------------------------------------------------------------
QUEDA_MINIMA_DA_MORTE = 3.0


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
    return [num(x), num(SENTIDO_DE_Y * y), num(z)]


def escala(x=1.0, y=1.0, z=1.0):
    """Escala: o neutro e UM. Vetor de escala com zero some com o osso."""
    return [num(x), num(y), num(z)]


def curva(bones, osso, canal, pares):
    """Acumula keyframes de um canal (rotation/position/scale) de um osso.

    Escreve por CHAVE DE TEMPO, entao uma curva posterior sobrescreve a pose que
    `postura_base`/`base_humana` deitaram -- que e exatamente a ordem desejada.
    """
    alvo = bones.setdefault(osso, {}).setdefault(canal, {})
    for t, v in pares:
        alvo[tempo(t)] = v


def membro(bones, nome, pares):
    """Keya o braco/perna e DERIVA a garra/pe dele.

    A extremidade escrita a mao fica com a fase certa hoje e errada na primeira
    correcao do membro -- e garra fora de fase nao da erro, da um bicho que
    arrasta a ponta da mao pelo chao. So o eixo X contra-gira: Z carrega a
    abertura do braco, e contra-girar a abertura viraria a garra para dentro.
    """
    curva(bones, nome, "rotation", pares)
    filho = EXTREMIDADE[nome]
    fator = CONTRA_GARRA if filho.startswith("claw") else CONTRA_PE
    curva(bones, filho, "rotation", [(t, vetor(x=v[0] * fator)) for t, v in pares])


def ciclo(duracao, periodo, amplitude, base=0.0, fase=FASE_QUE_COMECA_NA_BASE,
          amostras=8):
    """Cossenoide amostrada -- e a marcha e a respiracao inteiras.

    Amostrar (em vez de escrever os extremos a mao) e o que mantem a fase certa
    quando alguem mexe no periodo, e o que mantem a interpolacao linear do
    formato 1.8.0 parecendo curva em vez de zigue-zague. Tambem e o que FECHA o
    loop: com periodo que divide a duracao, o ultimo quadro vale o primeiro.
    """
    passo = periodo / amostras
    n = int(round(duracao / passo))
    return [(i * passo,
             base + amplitude * math.cos(2 * math.pi * (i * passo / periodo + fase)))
            for i in range(n + 1)]


# ----------------------------------------------------------------- a postura

def postura_comum():
    """Os ossos que as DUAS formas tem -- e so eles. E a pose da costura.

    Devolver exatamente COMUNS e conferido aqui dentro: osso comum que ficasse de
    fora da postura voltaria para o default do modelo no quadro da troca, e
    aquele membro endireitaria sozinho bem no meio da revelacao.
    """
    rotacoes = {
        "body": vetor(x=BODY_X),
        "head": vetor(x=CABECA_X),
        "arm_left": vetor(x=BRACO_X, z=BRACO_Z),
        "arm_right": vetor(x=BRACO_X, z=-BRACO_Z),
        "leg_left": vetor(x=PERNA_X, z=PERNA_Z),
        "leg_right": vetor(x=PERNA_X, z=-PERNA_Z),
    }
    if set(rotacoes) != set(COMUNS):
        raise SystemExit(
            "a postura comum cobre %s e os ossos comuns as duas formas sao %s."
            % (sorted(rotacoes), sorted(COMUNS)))
    return rotacoes, vetor(y=-AGACHAMENTO)


def postura_completa():
    """A postura comum MAIS o que so a forma verdadeira tem.

    As extremidades sao DERIVADAS pela mesma conta que `membro` usa. Escrever
    -2.25 aqui funcionaria hoje e mentiria no dia em que PERNA_X ou CONTRA_PE
    mudassem -- e o buraco seria pequeno demais para alguem ver e grande demais
    para ficar: o `idle` quase nao mexe as pernas, entao os pes dele ficariam
    parados num valor e os do `walk` comecariam noutro.
    """
    rotacoes, posicao = postura_comum()
    rotacoes = dict(rotacoes)
    rotacoes["beak"] = vetor(x=BICO_X)
    rotacoes["crest"] = vetor(x=CRISTA_X)
    rotacoes["tail"] = vetor(x=CAUDA_X)
    for pai, filho in EXTREMIDADE.items():
        eh_garra = filho.startswith("claw")
        base = BRACO_X if eh_garra else PERNA_X
        fator = CONTRA_GARRA if eh_garra else CONTRA_PE
        rotacoes[filho] = vetor(x=base * fator)
    if set(rotacoes) != set(ANIMAVEIS_VERDADEIRO):
        raise SystemExit(
            "a postura completa cobre %s e os ossos animaveis do kiriko sao %s."
            % (sorted(rotacoes), sorted(ANIMAVEIS_VERDADEIRO)))
    return rotacoes, posicao


def postura_base(bones, duracao):
    """Deita a postura inteira em TODO osso animavel, no inicio e no fim.

    E o que impede o buraco silencioso: osso que o clipe nao cita nao fica na
    postura, volta para o default do MODELO. Um `hurt` sem crista aprumaria a
    crista por cinco ticks; um `strike` sem cauda deixaria a cauda no chao no
    meio do golpe.
    """
    rotacoes, posicao = postura_completa()
    for osso, valor in rotacoes.items():
        curva(bones, osso, "rotation", [(0.0, valor), (duracao, valor)])
    curva(bones, "body", "position", [(0.0, posicao), (duracao, posicao)])


def base_humana(bones, duracao):
    """Deita o REPOUSO humano (zero) em todo osso animavel do disfarce.

    O default do modelo humano ja e zero, entao isto parece redundante -- e nao
    e. Ele existe para que cada clipe AFIRME a pose inteira em vez de herda-la:
    e o que permite ao `observe` dizer, no arquivo e no diff, que as pernas estao
    plantadas e as maos quietas. Omitido, "quieto" viraria "nao mencionado", e
    `conferir_que_observe_e_atencao` nao teria o que medir.
    """
    for osso in ANIMAVEIS_DISFARCE:
        curva(bones, osso, "rotation", [(0.0, vetor()), (duracao, vetor())])
    curva(bones, "body", "position", [(0.0, vetor()), (duracao, vetor())])


# ------------------------------------------------- clipes da FORMA HUMANA

def humano_idle():
    """Viajante parado: respiracao calma e o peso trocando de pe devagar.

    ESTE CLIPE EXISTE PARA CONTRASTAR COM `observe`, e o contraste e o oposto do
    que o man-faced ape faz. Ali o disfarce parado e um corpo que NAO respira, e a
    pista e uma ausencia assustadora. Aqui o kiriko nao esta escondendo que esta
    ali: as duas poses respiram. O que muda entre elas e PARA ONDE vai a atencao
    -- no idle o corpo se ocupa de si (peso, respiracao, uma olhada solta), no
    observe o corpo para e a cabeca fica em voce.

    Se alguem "melhorar" um dos dois ate ficarem parecidos, o mob perde a unica
    pista que da antes de decidir, e nenhum outro portao do repositorio olha para
    animacao.
    """
    dur = DUR_IDLE_HUMANO
    b = {}
    base_humana(b, dur)
    # Uma respiracao por 3s (~20 por minuto): calmo. Duas seria alguem ofegante, e
    # quem esta avaliando nao esta ofegante.
    curva(b, "body", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, dur, 0.22)])
    # O PESO TROCA DE PE: rolamento em Z, uma vez por ciclo, DEFASADO da
    # respiracao. Em fase, o peito e o quadril viram um so movimento e o corpo le
    # como bloco; defasados, lem como duas coisas acontecendo no mesmo corpo, que
    # e o que faz um idle parecer vivo em vez de embalado.
    respiro = ciclo(dur, dur, -0.9)
    peso = ciclo(dur, dur, 1.4, 0.0, 0.5)
    curva(b, "body", "rotation",
          [(t, vetor(x=rx, z=pz)) for (t, rx), (_, pz) in zip(respiro, peso)])
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.7, vetor(y=-8.0, x=1.0)), (1.4, vetor(y=-2.0)),
        (2.1, vetor(y=7.0, x=-1.0)), (2.6, vetor(y=3.0)), (dur, vetor())])
    # O chapeu atrasa um quadro atras da cabeca: e o que faz um chapeu parecer
    # apoiado em vez de colado.
    curva(b, "hat", "rotation", [
        (0.0, vetor()), (0.85, vetor(x=1.6)), (2.25, vetor(x=-1.1)),
        (dur, vetor())])
    for lado, sinal in (("arm_left", 1.0), ("arm_right", -1.0)):
        curva(b, lado, "rotation", [
            (0.0, vetor()), (1.1, vetor(x=2.2, z=1.2 * sinal)),
            (2.2, vetor(x=-1.6)), (dur, vetor())])
    # As pernas recebem o peso, e recebem NA MESMA FASE do quadril (0.5). Fase
    # propria aqui daria um corpo cujo quadril rola para um lado enquanto as
    # pernas se preparam para o outro.
    for lado, sinal in (("leg_left", 1.0), ("leg_right", -1.0)):
        curva(b, lado, "rotation",
              [(t, vetor(z=-v * 0.35 * sinal))
               for t, v in ciclo(dur, dur, 1.4, 0.0, 0.5)])
    return {"loop": LOOPS_DISFARCE["idle"], "animation_length": dur, "bones": b}


def humano_walk():
    """Caminhada de GENTE, 1.0s por ciclo completo (dois passos).

    O que a faz humana sao duas coisas, e as duas sao conferidas depois:
    (a) o TRONCO NAO INCLINA -- body.rotation.x fica em zero exato. Basta
        inclinar o peito para a frente e a silhueta vira bicho, por mais correta
        que a perna esteja;
    (b) o BRACO OSCILA MENOS QUE A PERNA, e em contrafase com ela.
    """
    dur = DUR_WALK_HUMANO
    b = {}
    base_humana(b, dur)
    # Perna esquerda e braco DIREITO na mesma fase: e a contrarrotacao que todo
    # bipede faz para nao girar o tronco a cada passo.
    for nome, fase, amplitude in (
            ("leg_left", 0.75, HUMANO_PERNA), ("leg_right", 0.25, HUMANO_PERNA),
            ("arm_left", 0.25, HUMANO_BRACO), ("arm_right", 0.75, HUMANO_BRACO)):
        curva(b, nome, "rotation",
              [(t, vetor(x=v)) for t, v in ciclo(dur, dur, amplitude, 0.0, fase)])
    # Sobe e desce DUAS vezes por ciclo: o corpo esta mais alto em cada apoio.
    curva(b, "body", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, dur / 2.0, 0.35)])
    curva(b, "body", "rotation",
          [(t, vetor(z=v)) for t, v in ciclo(dur, dur, 1.6)])
    # A cabeca fica NIVELADA: ela desfaz o rolamento do quadril.
    curva(b, "head", "rotation",
          [(t, vetor(z=v)) for t, v in ciclo(dur, dur, -1.1)])
    curva(b, "hat", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, dur / 2.0, 2.0)])
    return {"loop": LOOPS_DISFARCE["walk"], "animation_length": dur, "bones": b}


def humano_observe():
    """O CLIPE QUE DEFINE O MOB. Ele esta te avaliando.

    Toda a mecanica do encontro acontece enquanto este clipe toca: 200 ticks em
    que o servidor conta pontos e nao mostra nada. Se o jogador ler ameaca aqui,
    ele saca uma arma e REPROVA por causa da animacao -- e o unico mob do
    repositorio que se vence nao lutando vira mais um bicho que ataca.

    A DIFERENCA ENTRE ATENCAO E AMEACA CABE EM TRES ESCOLHAS:

    1. O CORPO PARA, MAS NAO MORRE. Ele continua respirando, bem pouco. Corpo
       absolutamente imovel e o truque do man-faced ape, e la ele serve para
       assustar -- um corpo humano que nao respira e a pista de que aquilo nao e
       humano. Aqui seria a leitura errada: o kiriko nao esta emboscando ninguem.
    2. A CABECA LIDERA, E LIDERA EM X. O gesto e a INCLINACAO -- a cabeca
       tombando de lado enquanto olha. E o unico gesto que le universalmente como
       "estou te avaliando" e nao como "vou te atacar", e e por isso que ele tem
       de ser o movimento maior do clipe.
    3. O OLHAR PARA. As pausas sao a parte que se esquece e a parte que faz o
       clipe funcionar: um varrimento continuo le como vigia procurando qualquer
       coisa. Um olhar que se move e FICA le como alguem que achou o que estava
       procurando -- e esse alguem e voce.

    AS MAOS FICAM QUIETAS de proposito e isso esta conferido: mao que se mexe e
    de onde vem a agressao, e o jogador le mao antes de ler qualquer outra coisa.

    PONTO CEGO DECLARADO: este clipe nao sabe onde o jogador esta. Nao ha camada
    de head-tracking no renderer hoje (`KirikoRenderer` nao sobrescreve
    `setCustomAnimations`), entao a cabeca daqui e a unica cabeca que existe -- e
    o que ela faz e a LEITURA de acompanhamento, nao o acompanhamento. No dia em
    que alguem acrescentar head-tracking, os dois yaws se SOMAM e a cabeca passa
    do jogador: e a mesma familia do dano multiplicado em dois handlers. O
    OBSERVE_YAW_MAXIMO limita o estrago; nao o impede.
    """
    dur = DUR_OBSERVE
    b = {}
    base_humana(b, dur)
    # Respiracao minima: presente e quase invisivel. O `> 0` e o `<= 2.5` sao os
    # dois lados do mesmo portao, porque os dois erros sao silenciosos.
    curva(b, "body", "position",
          [(t, vetor(y=v)) for t, v in ciclo(dur, 2.5, 0.18)])
    curva(b, "body", "rotation",
          [(t, vetor(x=v)) for t, v in ciclo(dur, 2.5, -0.6)])
    # O OLHAR: tres poses e tres pausas. Os tempos longos sao as pausas, e sao
    # elas que carregam a leitura -- a terceira pose e a INCLINACAO (z=7), que e
    # o momento em que o clipe diz "estou te medindo".
    curva(b, "head", "rotation", [
        (0.0, vetor(x=3.0, y=-6.0)),
        (1.1, vetor(x=3.0, y=-6.0)),
        (1.55, vetor(x=6.0, y=1.0, z=2.0)),
        (2.9, vetor(x=6.0, y=1.0, z=2.0)),
        (3.35, vetor(x=10.0, y=5.0, z=7.0)),
        (4.3, vetor(x=10.0, y=5.0, z=7.0)),
        (4.75, vetor(x=3.0, y=-6.0)),
        (dur, vetor(x=3.0, y=-6.0))])
    # O CHAPEU NAO SE MEXE. Zero escrito, e nao omitido: e a afirmacao "este corpo
    # esta parado", e ela aparece no diff.
    curva(b, "hat", "rotation", [(0.0, vetor()), (dur, vetor())])
    # As maos acompanham a respiracao e nada mais.
    for lado, sinal in (("arm_left", 1.0), ("arm_right", -1.0)):
        curva(b, lado, "rotation",
              [(t, vetor(x=v, z=v * 0.3 * sinal))
               for t, v in ciclo(dur, 2.5, 0.6)])
    # PES PLANTADOS, em zero exato. Um passo de ajuste aqui seria a coisa mais
    # natural do mundo de acrescentar e destruiria a leitura: quem esta decidindo
    # sobre voce nao remexe os pes.
    for lado in ("leg_left", "leg_right"):
        curva(b, lado, "rotation", [(0.0, vetor()), (dur, vetor())])
    return {"loop": LOOPS_DISFARCE["observe"], "animation_length": dur, "bones": b}


def humano_transform(crescimento, braco_extra, queda_do_chapeu):
    """24 ticks. A REVELACAO -- e ela NAO e um susto.

    A diferenca com o `reveal` do man-faced ape e a entrega inteira deste clipe.
    Aquele e uma emboscada: RECOLHE, ROMPE, PASSA DO PONTO, ASSENTA, em dez
    ticks, com o corpo desabando por baixo de uma cabeca que nao se mexe. Este e
    uma criatura inteligente LARGANDO um disfarce que nao precisa mais -- vinte e
    quatro ticks, sem quique, sem arranco, e com o corpo crescendo para dentro do
    proprio tamanho em vez de se retorcer.

    A forma e: DECIDE (0.25) -> LARGA (0.50) -> CRESCE (0.80) -> ASSENTA (1.20).
    Os primeiros seis ticks quase nao tem movimento de corpo de proposito. E o
    beat de decisao, e e ele que faz a revelacao ler como escolha em vez de
    reflexo. `conferir_que_a_revelacao_nao_e_um_susto` guarda isso medindo onde
    esta o quadro mais rapido do clipe -- se ele cair no comeco, alguem
    transformou a decisao num estalo.

    O CHAPEU E A UNICA COISA QUE CAI DE VERDADE, e ele e o relogio do clipe. Ele
    e tambem o unico osso autorizado a passar do ponto: um chapeu que
    desacelerasse para parar suavemente nao estaria caindo, estaria sendo pousado.

    O ULTIMO QUADRO E A POSTURA DO BICHO -- a mesma que todo clipe do outro
    modelo comeca. Ver `conferir_a_costura`.
    """
    dur = DUR_TRANSFORM
    rotacoes, posicao_do_body = postura_comum()
    b = {}

    # --- o corpo: cresce e assenta, sem quicar -------------------------------
    curva(b, "body", "rotation", [
        (0.0, vetor()), (0.25, vetor(x=1.5)), (0.5, vetor(x=-1.0)),
        (0.8, vetor(x=-2.4)), (1.05, vetor(x=-2.9)), (dur, rotacoes["body"])])
    curva(b, "body", "position", [
        (0.0, vetor()), (0.25, vetor(y=0.3)), (0.5, vetor(y=0.5)),
        (0.8, vetor(y=0.3)), (dur, posicao_do_body)])
    # A ESCALA E LIDA DOS DOIS GEOS, nao escolhida: ela existe para o corpo humano
    # chegar ao tamanho do bicho no quadro em que o modelo troca. Uniforme, ela
    # inflaria a pessoa como um balao -- por isso X e Z levam a razao das
    # LARGURAS e Y a das ALTURAS, que e o que "virar outro corpo" quer dizer.
    sx, sy = crescimento
    for t, f in ((0.0, 0.0), (0.25, 0.04), (0.5, 0.34), (0.8, 0.74),
                 (1.05, 0.93), (dur, 1.0)):
        curva(b, "body", "scale",
              [(t, escala(1.0 + (sx - 1.0) * f, 1.0 + (sy - 1.0) * f,
                          1.0 + (sx - 1.0) * f))])

    # --- a cabeca: ela DECIDE antes de o corpo se mexer -----------------------
    # Nos primeiros seis ticks so a cabeca se aprumar ja conta a cena: o viajante
    # para de fingir. E a unica coisa que acontece no beat de decisao.
    curva(b, "head", "rotation", [
        (0.0, vetor()), (0.25, vetor(x=4.0, z=-1.5)), (0.5, vetor(x=5.0)),
        (0.8, vetor(x=5.5)), (dur, rotacoes["head"])])

    # --- o chapeu: cai, tomba e sai de cena ----------------------------------
    curva(b, "hat", "rotation", [
        (0.0, vetor()), (0.25, vetor(x=5.0, z=-4.0)),
        (0.5, vetor(x=TOMBO_DO_CHAPEU * 0.41, z=-9.0)),
        (0.8, vetor(x=TOMBO_DO_CHAPEU * 0.79, z=-13.0)),
        (1.05, vetor(x=TOMBO_DO_CHAPEU * 0.93, z=-15.0)),
        (dur, vetor(x=TOMBO_DO_CHAPEU, z=-16.0))])
    curva(b, "hat", "position", [
        (0.0, vetor()), (0.25, vetor(y=-0.3)),
        (0.5, vetor(y=-queda_do_chapeu * 0.35, z=RECUO_DO_CHAPEU * 0.4)),
        (0.8, vetor(y=-queda_do_chapeu * 0.8, z=RECUO_DO_CHAPEU * 0.8)),
        (dur, vetor(y=-queda_do_chapeu, z=RECUO_DO_CHAPEU))])

    # --- os bracos: alongam MAIS que o resto ---------------------------------
    # A escala extra mora no BRACO, e nao no body, e a diferenca e visivel: no
    # body ela cresceria a pessoa inteira por igual; no braco ela cresce a partir
    # do ombro, que e exatamente o membro longo do Magical Beast aparecendo.
    for lado, sinal in (("arm_left", 1.0), ("arm_right", -1.0)):
        curva(b, lado, "rotation", [
            (0.0, vetor()), (0.25, vetor(x=1.0, z=2.0 * sinal)),
            (0.5, vetor(x=3.0, z=5.0 * sinal)),
            (0.8, vetor(x=4.5, z=7.5 * sinal)),
            (dur, rotacoes[lado])])
        for t, f in ((0.0, 0.0), (0.25, 0.02), (0.5, 0.3), (0.8, 0.72),
                     (1.05, 0.92), (dur, 1.0)):
            curva(b, lado, "scale",
                  [(t, escala(1.0, 1.0 + (braco_extra - 1.0) * f, 1.0))])

    # --- as pernas: so assentam ----------------------------------------------
    for lado, sinal in (("leg_left", 1.0), ("leg_right", -1.0)):
        curva(b, lado, "rotation", [
            (0.0, vetor()), (0.25, vetor(x=0.5)),
            (0.5, vetor(x=1.5, z=1.2 * sinal)),
            (0.8, vetor(x=2.4, z=2.4 * sinal)),
            (dur, rotacoes[lado])])

    return {"loop": LOOPS_DISFARCE["transform"], "animation_length": dur,
            "bones": b}


# ------------------------------------------- clipes da FORMA VERDADEIRA

def kiriko_idle():
    """Postura ereta, respiracao, crista mexendo de leve, cabeca varrendo.

    Quatro coisas, e todas devagar. A respiracao e do peito de um bicho grande
    (duas por 4s), a cabeca faz UMA varredura completa por volta do clipe, a
    crista da dois estalos pequenos e assimetricos, e a cauda contrabalanca.

    OS ESTALOS DA CRISTA SAO ASSIMETRICOS DE PROPOSITO -- tempos e tamanhos
    diferentes. Simetricos, eles lem como um unico gesto ensaiado; irregulares,
    lem como um bicho vivo que nao esta atuando para ninguem. Nao "corrigir" para
    o par.
    """
    dur = DUR_IDLE_KIRIKO
    b = {}
    postura_base(b, dur)
    curva(b, "body", "position",
          [(t, vetor(y=-AGACHAMENTO + v)) for t, v in ciclo(dur, 2.0, 0.35)])
    curva(b, "body", "rotation",
          [(t, vetor(x=BODY_X + v)) for t, v in ciclo(dur, 2.0, -1.4)])
    # A varredura: lenta e completa, uma so por volta. Rapida, viraria um bicho
    # nervoso -- e o kiriko nao esta nervoso, ele esta em paz.
    curva(b, "head", "rotation", [
        (0.0, vetor(x=CABECA_X)),
        (1.2, vetor(x=CABECA_X + 1.5, y=-12.0, z=-2.0)),
        (2.1, vetor(x=CABECA_X, y=-2.0)),
        (3.1, vetor(x=CABECA_X + 1.0, y=11.0, z=2.0)),
        (dur, vetor(x=CABECA_X))])
    curva(b, "beak", "rotation",
          [(t, vetor(x=BICO_X + v)) for t, v in ciclo(dur, 2.0, 1.2)])
    curva(b, "crest", "rotation", [
        (0.0, vetor(x=CRISTA_X)), (1.0, vetor(x=CRISTA_X)),
        (1.12, vetor(x=CRISTA_X - 9.0, z=4.0)), (1.3, vetor(x=CRISTA_X + 2.0)),
        (1.5, vetor(x=CRISTA_X)), (2.85, vetor(x=CRISTA_X)),
        (2.96, vetor(x=CRISTA_X - 6.0, z=-3.0)), (3.14, vetor(x=CRISTA_X + 1.0)),
        (3.32, vetor(x=CRISTA_X)), (dur, vetor(x=CRISTA_X))])
    curva(b, "tail", "rotation",
          [(t, vetor(x=CAUDA_X + v * 0.4, y=v)) for t, v in ciclo(dur, dur, 5.0)])
    # Peso passando de um pe para o outro, sem tirar nenhum do chao.
    for lado, sinal, fase in (("arm_left", 1.0, 0.75), ("arm_right", -1.0, 0.25)):
        membro(b, lado, [(t, vetor(x=v, z=BRACO_Z * sinal))
                         for t, v in ciclo(dur, dur, 1.6, BRACO_X, fase)])
    for lado, sinal, fase in (("leg_left", 1.0, 0.75), ("leg_right", -1.0, 0.25)):
        membro(b, lado, [(t, vetor(x=v, z=PERNA_Z * sinal))
                         for t, v in ciclo(dur, dur, 0.8, PERNA_X, fase)])
    return {"loop": LOOPS_VERDADEIRO["idle"], "animation_length": dur, "bones": b}


def kiriko_walk():
    """Andar de bicho grande e CONSCIENTE. 0.95s por ciclo completo.

    O que ele NAO e: nao e arrastado e nao e bestial. Em animacao isso vira tres
    afirmacoes conferiveis, e a primeira e o inverso do que vale para o
    man-faced ape:

    (a) A PERNA MANDA E O BRACO ACOMPANHA, como num bipede. No macaco e ao
        contrario -- o braco longo puxa o corpo -- e copiar aquele portao para ca
        enforcaria a marcha errada. Um kiriko com braco de amplitude de perna
        vira quadrupede-ish, e quadrupede-ish e monstro.
    (b) O TRONCO NAO MERGULHA: body.rotation.x nao sai da postura. Peito abaixo
        da linha dos ombros e posicao de bote, qualquer que seja a perna.
    (c) A CABECA FICA NIVELADA -- ela desfaz o rolamento do quadril em vez de
        balancar com a passada. Cabeca que sobe e desce a cada passo e o que da a
        leitura de bicho pesado se arrastando.

    A CAUDA E O CONTRAPESO e trabalha contra o quadril: e ela que diz que este
    corpo sabe andar. Uma cauda em FASE com o rolamento leria como rabo balancando
    junto, que e um cachorro contente.
    """
    dur = DUR_WALK_KIRIKO
    b = {}
    postura_base(b, dur)
    for nome, sinal, fase, amplitude, base_x, base_z in (
            ("leg_left", 1.0, 0.75, KIRIKO_PERNA, PERNA_X, PERNA_Z),
            ("leg_right", -1.0, 0.25, KIRIKO_PERNA, PERNA_X, PERNA_Z),
            ("arm_left", 1.0, 0.25, KIRIKO_BRACO, BRACO_X, BRACO_Z),
            ("arm_right", -1.0, 0.75, KIRIKO_BRACO, BRACO_X, BRACO_Z)):
        membro(b, nome, [(t, vetor(x=v, z=base_z * sinal))
                         for t, v in ciclo(dur, dur, amplitude, base_x, fase)])
    # Sobe e desce duas vezes por ciclo: mais alto em cada apoio.
    curva(b, "body", "position",
          [(t, vetor(y=-AGACHAMENTO + v)) for t, v in ciclo(dur, dur / 2.0, 0.5)])
    # SO ROLAMENTO. O X fica cravado na postura -- ver (b) no docstring.
    curva(b, "body", "rotation",
          [(t, vetor(x=BODY_X, z=v)) for t, v in ciclo(dur, dur, 2.2)])
    curva(b, "head", "rotation",
          [(t, vetor(x=CABECA_X, z=-v)) for t, v in ciclo(dur, dur, 1.5)])
    curva(b, "beak", "rotation", [(0.0, vetor(x=BICO_X)), (dur, vetor(x=BICO_X))])
    curva(b, "crest", "rotation",
          [(t, vetor(x=CRISTA_X + v)) for t, v in ciclo(dur, dur / 2.0, 2.5)])
    # CONTRAPESO: o sinal negativo e a afirmacao. Ver o docstring.
    curva(b, "tail", "rotation",
          [(t, vetor(x=CAUDA_X, y=-v)) for t, v in ciclo(dur, dur, 9.0)])
    return {"loop": LOOPS_VERDADEIRO["walk"], "animation_length": dur, "bones": b}


def kiriko_strike():
    """O GOLPE. 10 ticks de aviso, 5 de janela, 14 de recuperacao -- 1.45s.

    ELE SO EXISTE PORQUE O JOGADOR REPROVOU. E o unico clipe agressivo do mob, e
    o que ele tem de comunicar e que a decisao ja foi tomada -- por isso o aviso e
    longo (dez ticks e muito) e comeca pela CRISTA, nao pelo braco. O bicho que
    estava com a crista erguida em alerta a ABAIXA no instante em que decide
    bater. Quem viu o `observe` ja aprendeu a olhar para a cabeca dele; e la que o
    aviso tem de aparecer.

    OS TRES TEMPOS SAO DO SERVIDOR, nao deste arquivo:

      0.00 -> 0.50  o braco SOBE. Sao os 10 ticks em que o jogador ainda sai.
      0.50 -> 0.75  o braco DESCE. Sao os 5 ticks em que o dano acontece.
      0.75 -> 1.45  recuperacao. 14 ticks em que bater nele e de graca.

    Um braco que chegasse ao topo em 0.6 prometeria o golpe depois da hora em que
    ele ja aconteceu. `conferir_a_janela_do_golpe` reprova isso.

    UM BRACO SO, o direito: dez ticks de aviso precisam caber num membro que o
    jogador consiga seguir com o olho.
    """
    dur = DUR_STRIKE
    b = {}
    postura_base(b, dur)
    completa, _ = postura_completa()
    # X crescente = a garra sobe pela FRENTE. Em 112 graus ela esta acima e a
    # frente da cabeca; em 16, ja varreu para baixo. E essa VARREDURA, e nao a
    # pose do topo, que e o golpe.
    curva(b, "arm_right", "rotation", [
        (0.0, vetor(x=BRACO_X, z=-BRACO_Z)),
        (0.18, vetor(x=34.0, z=-16.0)),
        (0.34, vetor(x=78.0, z=-22.0)),
        (FIM_DO_WINDUP, vetor(x=112.0, z=-26.0)),   # APICE: fim do aviso
        (0.62, vetor(x=60.0, z=-18.0)),
        (FIM_DA_JANELA, vetor(x=16.0, z=-10.0)),    # fim da janela de dano
        (0.95, vetor(x=-4.0, z=-8.0)),              # a inercia passa do ponto
        (1.25, vetor(x=BRACO_X + 3.0, z=-BRACO_Z)),
        (dur, vetor(x=BRACO_X, z=-BRACO_Z))])
    # A GARRA NAO CONTRA-GIRA COMO NO ANDAR: aqui ela LIDERA. Derivar a garra da
    # formula do apoio a deixaria apontando para tras no momento do impacto. Mas
    # ela SAI e VOLTA para a postura -- fora do golpe e uma mao como outra
    # qualquer.
    garra = completa["claw_right"]
    curva(b, "claw_right", "rotation", [
        (0.0, garra), (FIM_DO_WINDUP, vetor(x=-30.0)),
        (0.62, vetor(x=6.0)), (FIM_DA_JANELA, vetor(x=18.0)),
        (1.05, vetor(x=-4.0)), (dur, garra)])
    # O braco esquerdo contrabalanca: sem ele o bicho gira no proprio eixo.
    membro(b, "arm_left", [
        (0.0, vetor(x=BRACO_X, z=BRACO_Z)),
        (FIM_DO_WINDUP, vetor(x=BRACO_X - 16.0, z=BRACO_Z + 8.0)),
        (FIM_DA_JANELA, vetor(x=BRACO_X - 24.0, z=BRACO_Z + 4.0)),
        (1.05, vetor(x=BRACO_X - 6.0, z=BRACO_Z)),
        (dur, vetor(x=BRACO_X, z=BRACO_Z))])
    # O tronco desenrola no aviso e despenca na janela. E o corpo, e nao o braco,
    # que da PESO ao golpe.
    curva(b, "body", "rotation", [
        (0.0, vetor(x=BODY_X)), (0.18, vetor(x=BODY_X + 5.0)),
        (FIM_DO_WINDUP, vetor(x=4.0)),
        (0.62, vetor(x=-12.0)), (FIM_DA_JANELA, vetor(x=-20.0)),
        (1.05, vetor(x=-7.0)), (dur, vetor(x=BODY_X))])
    curva(b, "body", "position", [
        (0.0, vetor(y=-AGACHAMENTO)), (FIM_DO_WINDUP, vetor(y=-AGACHAMENTO + 1.2)),
        (FIM_DA_JANELA, vetor(y=-AGACHAMENTO - 1.4)),
        (1.05, vetor(y=-AGACHAMENTO + 0.3)), (dur, vetor(y=-AGACHAMENTO))])
    # A cabeca e lida SOMADA ao tronco. No aviso o tronco vai a +4 e o pescoco a
    # +16: liquido +20, o bicho ergue a cara. Na janela o tronco despenca para -20
    # e o pescoco vai a -8: liquido -28, a cabeca desce junto com a garra.
    curva(b, "head", "rotation", [
        (0.0, vetor(x=CABECA_X)), (FIM_DO_WINDUP, vetor(x=16.0)),
        (0.64, vetor(x=2.0)), (FIM_DA_JANELA, vetor(x=-8.0)),
        (1.05, vetor(x=5.0)), (dur, vetor(x=CABECA_X))])
    # O BICO ACOMPANHA a cabeca e adianta no impacto.
    curva(b, "beak", "rotation", [
        (0.0, vetor(x=BICO_X)), (FIM_DO_WINDUP, vetor(x=8.0)),
        (FIM_DA_JANELA, vetor(x=-14.0)), (1.05, vetor(x=-3.0)),
        (dur, vetor(x=BICO_X))])
    # A CRISTA ABAIXA, E ELA E O AVISO. X positivo joga a massa (em +Y) para tras
    # = crista colada ao cranio. Ela comeca a cair em 0.14, antes de o braco ter
    # se mexido: quem aprendeu a olhar para a cabeca dele no `observe` ganha esses
    # sete ticks de graca.
    curva(b, "crest", "rotation", [
        (0.0, vetor(x=CRISTA_X)), (0.14, vetor(x=CRISTA_X + 14.0)),
        (FIM_DO_WINDUP, vetor(x=26.0)), (FIM_DA_JANELA, vetor(x=30.0)),
        (1.15, vetor(x=CRISTA_X + 6.0)), (dur, vetor(x=CRISTA_X))])
    # A cauda e o contrapeso do golpe: vai para tras no aviso, chicoteia na janela.
    curva(b, "tail", "rotation", [
        (0.0, vetor(x=CAUDA_X)), (FIM_DO_WINDUP, vetor(x=CAUDA_X - 16.0, y=8.0)),
        (FIM_DA_JANELA, vetor(x=CAUDA_X + 18.0, y=-10.0)),
        (1.1, vetor(x=CAUDA_X + 4.0)), (dur, vetor(x=CAUDA_X))])
    # A perna do lado que bate empurra; a outra segura o chao.
    membro(b, "leg_right", [
        (0.0, vetor(x=PERNA_X, z=-PERNA_Z)),
        (FIM_DO_WINDUP, vetor(x=PERNA_X - 14.0, z=-PERNA_Z)),
        (FIM_DA_JANELA, vetor(x=PERNA_X - 22.0, z=-PERNA_Z - 3.0)),
        (1.1, vetor(x=PERNA_X - 5.0, z=-PERNA_Z)),
        (dur, vetor(x=PERNA_X, z=-PERNA_Z))])
    membro(b, "leg_left", [
        (0.0, vetor(x=PERNA_X, z=PERNA_Z)),
        (FIM_DO_WINDUP, vetor(x=PERNA_X + 11.0, z=PERNA_Z)),
        (FIM_DA_JANELA, vetor(x=PERNA_X + 17.0, z=PERNA_Z + 4.0)),
        (1.1, vetor(x=PERNA_X + 5.0, z=PERNA_Z)),
        (dur, vetor(x=PERNA_X, z=PERNA_Z))])
    return {"loop": LOOPS_VERDADEIRO["strike"], "animation_length": dur, "bones": b}


def kiriko_approve():
    """O CLIPE MAIS IMPORTANTE, E O QUE MAIS FACIL SAI ERRADO. Ele te aprovou.

    Este clipe e o premio do encontro inteiro. O jogador esperou 200 ticks sem
    sacar nada, viu um viajante virar um Magical Beast a dois metros dele, e o que
    acontece agora e a unica confirmacao que ele vai receber de que fez certo.

    E ELE TEM DOIS JEITOS DE SAIR ERRADO, E OS DOIS SAO PLAUSIVEIS:

      ATAQUE -- bracos abrindo com o peito indo junto le como investida. O jogador
                bate primeiro, e uma aprovacao vira a luta que o mob existe para
                nao ter.
      FUGA   -- virar depressa le como bicho assustado indo embora. Nao e falso do
                ponto de vista da cena, e e desastroso do ponto de vista do que a
                cena significa: o jogador aprende que assustou o bicho, e nao que
                foi reconhecido por ele.

    AS QUATRO ESCOLHAS QUE SEPARAM RECONHECIMENTO DAS DUAS:

    1. O TRONCO NUNCA VAI PARA A FRENTE. body.rotation.x nunca fica mais negativo
       que a postura, o clipe inteiro. Peito adiantado e investida, e nao existe
       gesto de mao que desfaca isso.
    2. A INCLINACAO DE CABECA VEM PRIMEIRO, e sozinha. Antes de qualquer braco se
       mexer, a cabeca baixa e tomba de lado -- a mesma inclinacao do `observe`,
       levada ate o fim. E a rima que fecha o encontro: o gesto com que ele
       comecou a te medir e o gesto com que ele diz que terminou.
    3. OS BRACOS ABREM PARA FORA (Z), E UMA VEZ SO. Em X eles fariam o arco do
       golpe; duas vezes viraria display de ameaca. Uma abertura lenta e larga,
       que volta.
    4. A VIRADA E TARDE, CURTA E LENTA. Ela so comeca no ultimo terco, e o MODELO
       so a COMECA: quem termina de virar e a entidade, andando. Um modelo que
       girasse 180 graus ficaria de costas dentro da propria hitbox -- o corpo
       desenhado passaria a discordar da caixa em que ele e acertado, e num mob
       que o jogador acabou de decidir NAO atacar isso e o pior momento possivel
       para a silhueta mentir sobre onde ele esta.

    E por cima das quatro, uma regua unica:
    `conferir_que_approve_nao_e_ataque_nem_fuga` mede o quadro mais rapido deste
    clipe contra o quadro mais rapido do GOLPE. Nada aqui pode se mexer na
    velocidade do combate. Fuga e exatamente isso: o mesmo corpo, na mesma
    pressa, para o outro lado.

    hold_on_last_frame: ele fica virado. A saida de cena e da entidade.
    """
    dur = DUR_APPROVE
    b = {}
    postura_base(b, dur)

    # --- 1 e 2: a cabeca se inclina, e o tronco NAO acompanha ----------------
    curva(b, "head", "rotation", [
        (0.0, vetor(x=CABECA_X)),
        (0.25, vetor(x=-4.0, z=5.0)),
        (0.45, vetor(x=-14.0, z=9.0)),      # a inclinacao: o gesto do `observe`
        (0.7, vetor(x=-6.0, z=6.0)),
        (0.95, vetor(x=4.0, z=1.0)),
        (1.25, vetor(x=CABECA_X, y=40.0)),  # a cabeca entra na virada antes
        (dur, vetor(x=CABECA_X, y=62.0))])
    # O tronco so RECUA (x mais positivo que a postura) e rola um pouco. Nunca
    # para a frente -- e o portao mede cada quadro, nao so os extremos.
    curva(b, "body", "rotation", [
        (0.0, vetor(x=BODY_X)),
        (0.45, vetor(x=BODY_X + 2.0, z=2.0)),
        (0.9, vetor(x=BODY_X + 4.0)),
        (1.25, vetor(x=BODY_X + 1.0, y=22.0, z=-2.0)),
        (dur, vetor(x=BODY_X, y=48.0, z=-3.0))])
    curva(b, "body", "position", [
        (0.0, vetor(y=-AGACHAMENTO)), (0.45, vetor(y=-AGACHAMENTO - 0.4)),
        (0.9, vetor(y=-AGACHAMENTO + 0.5)), (dur, vetor(y=-AGACHAMENTO))])
    curva(b, "beak", "rotation", [
        (0.0, vetor(x=BICO_X)), (0.45, vetor(x=BICO_X - 6.0)),
        (0.95, vetor(x=BICO_X + 2.0)), (dur, vetor(x=BICO_X))])
    # A CRISTA SOBE. No `strike` ela abaixa; aqui ela abre. As duas revelacoes
    # mostram o mesmo corpo, e e este osso que diz qual das duas voce conseguiu.
    curva(b, "crest", "rotation", [
        (0.0, vetor(x=CRISTA_X)), (0.45, vetor(x=CRISTA_X - 10.0)),
        (0.9, vetor(x=CRISTA_X - 16.0, z=3.0)),
        (1.25, vetor(x=CRISTA_X - 8.0)), (dur, vetor(x=CRISTA_X - 4.0))])

    # --- 3: os bracos abrem para FORA, uma vez -------------------------------
    for lado, sinal in (("arm_left", 1.0), ("arm_right", -1.0)):
        membro(b, lado, [
            (0.0, vetor(x=BRACO_X, z=BRACO_Z * sinal)),
            (0.45, vetor(x=BRACO_X, z=BRACO_Z * sinal)),
            (0.7, vetor(x=BRACO_X + 2.0, z=30.0 * sinal)),
            (0.9, vetor(x=BRACO_X + 3.0, z=38.0 * sinal)),   # o unico apice
            (1.15, vetor(x=BRACO_X + 1.0, z=12.0 * sinal)),
            (dur, vetor(x=BRACO_X, z=BRACO_Z * sinal))])

    # --- 4: a virada, tarde e curta ------------------------------------------
    # A perna de dentro da curva planta e a de fora da o passo. Um bicho que
    # virasse com os dois pes parados estaria girando sobre um eixo, e giro sobre
    # eixo le como mecanismo, nao como criatura.
    membro(b, "leg_left", [
        (0.0, vetor(x=PERNA_X, z=PERNA_Z)),
        (1.25, vetor(x=PERNA_X + 7.0, z=PERNA_Z + 2.0)),
        (dur, vetor(x=PERNA_X + 11.0, z=PERNA_Z + 3.0))])
    membro(b, "leg_right", [
        (0.0, vetor(x=PERNA_X, z=-PERNA_Z)),
        (1.25, vetor(x=PERNA_X - 4.0, z=-PERNA_Z)),
        (dur, vetor(x=PERNA_X - 7.0, z=-PERNA_Z - 1.0))])
    # A cauda acompanha a virada: ela sai para FORA da curva, que e o que
    # equilibra o corpo. Para dentro, o bicho pareceria tropecar em si mesmo.
    curva(b, "tail", "rotation", [
        (0.0, vetor(x=CAUDA_X)), (0.9, vetor(x=CAUDA_X - 5.0)),
        (1.25, vetor(x=CAUDA_X - 2.0, y=-18.0)),
        (dur, vetor(x=CAUDA_X, y=-34.0))])
    return {"loop": LOOPS_VERDADEIRO["approve"], "animation_length": dur,
            "bones": b}


def kiriko_hurt():
    """Tranco de 5 ticks. Nao interrompe leitura de fase nenhuma.

    NAO KEYA position do body alem da postura -- so rotacao. `position` e o canal
    que carregaria qualquer deslocamento, e um hurt que o escrevesse arrancaria o
    bicho do lugar por cinco ticks, possivelmente no meio de um golpe. (Isso
    REDUZ o estrago; nao o elimina -- o hurt ainda pode entrar por cima do strike
    e roubar a leitura da janela. Esta no relato.)
    """
    dur = DUR_HURT
    b = {}
    postura_base(b, dur)
    curva(b, "body", "rotation", [
        (0.0, vetor(x=BODY_X)), (0.06, vetor(x=BODY_X + 8.0, z=9.0)),
        (0.15, vetor(x=BODY_X - 3.0, z=-3.0)), (dur, vetor(x=BODY_X))])
    curva(b, "head", "rotation", [
        (0.0, vetor(x=CABECA_X)), (0.06, vetor(x=CABECA_X + 16.0, y=10.0)),
        (0.15, vetor(x=CABECA_X - 6.0, y=-5.0)), (dur, vetor(x=CABECA_X))])
    curva(b, "beak", "rotation", [
        (0.0, vetor(x=BICO_X)), (0.05, vetor(x=BICO_X + 12.0)),
        (0.15, vetor(x=BICO_X - 4.0)), (dur, vetor(x=BICO_X))])
    # A crista ESTALA para tras no impacto e volta. E o mesmo osso do aviso do
    # golpe, e e de proposito: ela e onde este bicho poe tudo que sente.
    curva(b, "crest", "rotation", [
        (0.0, vetor(x=CRISTA_X)), (0.05, vetor(x=CRISTA_X + 24.0)),
        (0.16, vetor(x=CRISTA_X + 5.0)), (dur, vetor(x=CRISTA_X))])
    curva(b, "tail", "rotation", [
        (0.0, vetor(x=CAUDA_X)), (0.06, vetor(x=CAUDA_X - 14.0, y=7.0)),
        (0.16, vetor(x=CAUDA_X + 4.0)), (dur, vetor(x=CAUDA_X))])
    for lado, sinal in (("arm_left", 1.0), ("arm_right", -1.0)):
        membro(b, lado, [
            (0.0, vetor(x=BRACO_X, z=BRACO_Z * sinal)),
            (0.06, vetor(x=BRACO_X - 11.0, z=(BRACO_Z + 6.0) * sinal)),
            (dur, vetor(x=BRACO_X, z=BRACO_Z * sinal))])
    for lado, sinal in (("leg_left", 1.0), ("leg_right", -1.0)):
        membro(b, lado, [
            (0.0, vetor(x=PERNA_X, z=PERNA_Z * sinal)),
            (0.06, vetor(x=PERNA_X + 9.0, z=PERNA_Z * sinal)),
            (dur, vetor(x=PERNA_X, z=PERNA_Z * sinal))])
    return {"loop": LOOPS_VERDADEIRO["hurt"], "animation_length": dur, "bones": b}


def kiriko_death(queda):
    """Ele CEDE, TOMBA, e a cabeca e a ultima coisa a parar. 1.4s.

    A ORDEM IMPORTA MAIS QUE AS POSES, e ela e a ficha: "cede, tomba, cabeca por
    ultimo". Em tempo:

        0.00 -> 0.30  as pernas CEDEM. O corpo ainda esta em pe, mas ja caiu.
        0.30 -> 0.70  o tronco TOMBA. E o movimento maior do clipe.
        0.70 -> 1.05  o corpo chega ao chao.
        1.05 -> 1.40  so a CABECA ainda se mexe -- ela rola e assenta depois de
                      todo o resto ja ter parado.

    `conferir_que_a_cabeca_e_a_ultima` mede isso: o quadro mais rapido da cabeca
    tem de vir DEPOIS do quadro mais rapido do corpo. Sem esse portao, a primeira
    correcao distraida sincroniza os dois e a morte deixa de ler como um corpo
    perdendo as partes uma a uma; passa a ler como desligar o bicho.

    A CRISTA CAI DEVAGAR E POR ULTIMO, junto da cabeca. Ela foi o aviso do golpe
    e o sinal da aprovacao: e o osso que o jogador aprendeu a ler, e e o que ainda
    se mexe depois de o corpo parar.

    A queda NAO e um numero de gosto: sai do geo, em `queda_da_morte`. Escrita a
    mao, ela sobreviveria a proxima correcao do modelo e o kiriko morreria
    flutuando (ou enterrado) sem um unico erro no log.
    """
    dur = DUR_DEATH
    b = {}
    postura_base(b, dur)
    curva(b, "body", "rotation", [
        (0.0, vetor(x=BODY_X)), (0.15, vetor(x=BODY_X + 6.0, z=3.0)),
        (0.3, vetor(x=-14.0, z=9.0)),
        (0.7, vetor(x=-44.0, z=38.0)),      # O TOMBO: o maior quadro do corpo
        (1.05, vetor(x=-54.0, z=64.0)),
        (dur, vetor(x=-56.0, z=72.0))])
    curva(b, "body", "position", [
        (0.0, vetor(y=-AGACHAMENTO)), (0.15, vetor(y=-AGACHAMENTO - 0.6)),
        (0.3, vetor(y=-AGACHAMENTO - queda * 0.22)),
        (0.7, vetor(x=1.0, y=-AGACHAMENTO - queda * 0.68)),
        (1.05, vetor(x=2.0, y=-AGACHAMENTO - queda * 0.95)),
        (dur, vetor(x=2.4, y=-AGACHAMENTO - queda))])
    # A CABECA POR ULTIMO: ela quase nao se mexe ate 1.05 e so entao rola.
    curva(b, "head", "rotation", [
        (0.0, vetor(x=CABECA_X)), (0.3, vetor(x=CABECA_X + 4.0)),
        (0.7, vetor(x=2.0, z=-6.0)), (1.05, vetor(x=6.0, z=-10.0)),
        (dur, vetor(x=30.0, z=-30.0))])
    curva(b, "beak", "rotation", [
        (0.0, vetor(x=BICO_X)), (0.7, vetor(x=BICO_X - 4.0)),
        (1.05, vetor(x=BICO_X - 8.0)), (dur, vetor(x=BICO_X - 18.0))])
    curva(b, "crest", "rotation", [
        (0.0, vetor(x=CRISTA_X)), (0.3, vetor(x=CRISTA_X - 6.0)),
        (0.8, vetor(x=CRISTA_X + 8.0)), (1.05, vetor(x=CRISTA_X + 14.0)),
        (dur, vetor(x=CRISTA_X + 26.0, z=-8.0))])
    curva(b, "tail", "rotation", [
        (0.0, vetor(x=CAUDA_X)), (0.3, vetor(x=CAUDA_X + 10.0, y=6.0)),
        (0.7, vetor(x=CAUDA_X + 24.0, y=14.0)),
        (dur, vetor(x=CAUDA_X + 30.0, y=18.0))])
    # O braco esquerdo larga cedo; o DIREITO ainda tenta escorar ate 0.85. E esse
    # atraso num membro so que faz a morte ler como morte em vez de desligamento.
    membro(b, "arm_left", [
        (0.0, vetor(x=BRACO_X, z=BRACO_Z)), (0.3, vetor(x=-8.0, z=14.0)),
        (0.7, vetor(x=-24.0, z=6.0)), (dur, vetor(x=-28.0, z=4.0))])
    membro(b, "arm_right", [
        (0.0, vetor(x=BRACO_X, z=-BRACO_Z)),
        (0.5, vetor(x=BRACO_X + 6.0, z=-(BRACO_Z + 3.0))),
        (0.85, vetor(x=BRACO_X + 3.0, z=-BRACO_Z)),
        (1.1, vetor(x=-10.0, z=-8.0)), (dur, vetor(x=-26.0, z=-3.0))])
    # AS PERNAS CEDEM PRIMEIRO: elas fazem o movimento delas antes de 0.3.
    for lado, sinal in (("leg_left", 1.0), ("leg_right", -1.0)):
        membro(b, lado, [
            (0.0, vetor(x=PERNA_X, z=PERNA_Z * sinal)),
            (0.3, vetor(x=38.0, z=(PERNA_Z + 5.0) * sinal)),
            (0.7, vetor(x=50.0, z=(PERNA_Z + 9.0) * sinal)),
            (dur, vetor(x=54.0, z=(PERNA_Z + 11.0) * sinal))])
    return {"loop": LOOPS_VERDADEIRO["death"], "animation_length": dur, "bones": b}


# ------------------------------------------------------- leitura do geo

def carregar_geo(mob):
    caminho = os.path.join(DIR_GEO, mob + ".geo.json")
    if not os.path.exists(caminho):
        raise SystemExit(
            "%s nao existe. Este gerador LE os DOIS geos -- o crescimento do"
            " corpo, o alongamento do braco, a queda do chapeu e a queda da morte"
            " saem de la, e os eixos sao conferidos contra eles. Sem os dois nao"
            " ha o que animar." % caminho)
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


def filhos(geometria, nome):
    return [b["name"] for b in geometria["bones"] if b.get("parent") == nome]


def subarvore(geometria, nome):
    saida = [nome]
    for f in filhos(geometria, nome):
        saida += subarvore(geometria, f)
    return saida


def subarvore_tem_volume(geometria, nome):
    return any(caixas(geometria, n) for n in subarvore(geometria, nome))


def faixa(geometria, nomes, eixo):
    valores = []
    for nome in nomes:
        for c in caixas(geometria, nome):
            valores += [c["origin"][eixo], c["origin"][eixo] + c["size"][eixo]]
    if not valores:
        raise SystemExit("nenhum cubo em %s -- nao da para medir" % list(nomes))
    return min(valores), max(valores)


def centro(geometria, nome, eixo):
    cubos = caixas(geometria, nome)
    if not cubos:
        raise SystemExit("o osso '%s' nao tem cubo; nao da para ler o eixo" % nome)
    return sum(c["origin"][eixo] + c["size"][eixo] / 2.0 for c in cubos) / len(cubos)


def todos_os_cubos(geometria):
    return tuple(b["name"] for b in geometria["bones"] if b.get("cubes"))


def altura(geometria):
    return faixa(geometria, todos_os_cubos(geometria), 1)[1]


def largura(geometria):
    menor, maior = faixa(geometria, todos_os_cubos(geometria), 0)
    return max(abs(menor), abs(maior)) * 2.0


# -------------------------------------------------- numeros LIDOS do geo

def crescimento_do_corpo(geo_d, geo_r):
    """Quanto o corpo humano cresce no `transform` -- LIDO dos DOIS geos.

    A escala do `body` gira em torno do PIVOT dele, entao o topo do modelo vai
    parar em pivot + (topo - pivot)*s. Igualar esse topo ao topo do bicho da o s
    que faz a troca de modelo nao pular de tamanho -- que e o ponto inteiro de as
    duas formas dividirem a mesma hitbox.

    SAO DUAS ESCALAS, e nao uma. Y sai das alturas, X e Z saem das LARGURAS. Uma
    escala uniforme engordaria o humano na proporcao em que o bicho e mais alto (e
    vice-versa), e o resultado le como balao inflando em vez de criatura.

    O CRESCIMENTO NAO MORA NA ALTURA, E ISSO E ARQUITETURA E NAO ACIDENTE. As duas
    formas cabem na MESMA `sized(1.0F, 2.1F)` por contrato, e os dois geos usam a
    caixa quase inteira -- entao a altura ja esta gasta e a razao entre elas nasce
    perto de 1.0. Exigir crescimento em Y seria exigir que a forma verdadeira
    estourasse a propria hitbox.

    Por isso o portao pede crescimento em PELO MENOS UM eixo, e nao nos dois: o
    que a revelacao mostra e MASSA, nao estatura. Um bicho que cabe numa caixa de
    2,1 blocos ja tinha de estar disfarcado de gente grande.

    PONTO CEGO DECLARADO: a conta iguala TOPO com TOPO e LARGURA com LARGURA. Ela
    nao prova que as silhuetas coincidem em nenhum outro ponto -- ombro, quadril e
    joelho podem saltar no quadro da troca, e nada aqui ve isso.
    """
    pivo = pivot(geo_d, "body")[1]
    topo_d, topo_r = altura(geo_d), altura(geo_r)
    if topo_d <= pivo:
        raise SystemExit(
            "o topo do modelo humano (%.1f px) nao esta acima do pivot do body"
            " (%.1f px): nao ha o que escalar." % (topo_d, pivo))
    sy = (topo_r - pivo) / (topo_d - pivo)
    sx = largura(geo_r) / largura(geo_d)
    if max(sx, sy) < CRESCIMENTO_MINIMO:
        raise SystemExit(
            "a forma verdadeira e %.3fx a humana em largura e %.3fx em altura, e"
            " nenhuma das duas chega a %.2f. A revelacao promete 'o corpo cresce' e"
            " nao teria o que mostrar -- e como os dois corpos dividem a mesma"
            " hitbox, o jogador nao tem nem o tamanho da caixa para perceber a"
            " troca. ISTO E UMA DIVERGENCIA ENTRE LANES: e no geo que o bicho"
            " precisa ter mais massa que o disfarce."
            % (sx, sy, CRESCIMENTO_MINIMO))
    for nome, s in (("altura", sy), ("largura", sx)):
        if s > CRESCIMENTO_MAXIMO:
            raise SystemExit(
                "a %s da forma verdadeira e %.3fx a da humana (maximo %.2f). O"
                " corpo humano teria de se deformar tanto antes da troca que a"
                " leitura vira balao inflando, e nao criatura largando disfarce."
                % (nome, s, CRESCIMENTO_MAXIMO))
    return sx, sy


def alongamento_do_braco(geo_d, geo_r, sy):
    """Quanto o braco alonga ALEM do corpo -- LIDO dos DOIS geos.

    O braco e filho do body, entao as duas escalas se MULTIPLICAM: para o braco
    humano chegar ao comprimento do braco do bicho, a escala local dele tem de ser
    a razao dos comprimentos DIVIDIDA pela escala que o body ja aplicou. Escrever
    a razao inteira aqui alongaria o braco duas vezes -- e o resultado seria
    plausivel demais para alguem notar sem medir, que e a mesma familia do dano
    multiplicado em dois handlers.
    """
    comp_d = pivot(geo_d, "arm_left")[1] - faixa(
        geo_d, subarvore(geo_d, "arm_left"), 1)[0]
    comp_r = pivot(geo_r, "arm_left")[1] - faixa(
        geo_r, subarvore(geo_r, "arm_left"), 1)[0]
    if comp_d <= 0 or comp_r <= 0:
        raise SystemExit(
            "comprimento de braco nao positivo (humano %.1f, verdadeiro %.1f): o"
            " braco nao pendura abaixo do proprio pivot em algum dos dois geos."
            % (comp_d, comp_r))
    razao = comp_r / comp_d
    if razao < sy * VANTAGEM_MINIMA_DO_BRACO:
        raise SystemExit(
            "o braco do kiriko e %.3fx o do humano e o corpo ja cresce %.3fx em"
            " altura. Os bracos nao alongam MAIS que o resto -- entao 'os bracos"
            " alongam' deixa de ser uma batida da revelacao e vira o corpo inteiro"
            " ficando maior. ISTO E UMA DIVERGENCIA ENTRE LANES: ou a forma"
            " verdadeira precisa de braco mais longo no geo, ou a ficha muda."
            % (razao, sy))
    return razao / sy


def queda_do_chapeu(geo_d):
    """Quanto o chapeu desce para SAIR da cabeca -- LIDO do geo.

    O chapeu e filho da cabeca: a posicao dele e local, e descer o fundo dele para
    baixo do fundo do cranio e o que "o chapeu caiu" quer dizer em pixels.

    Decorar 10 px funcionaria hoje e mentiria no dia em que a aba ficasse mais
    larga ou a copa mais alta: o chapeu pararia raspando o queixo, ainda cobrindo
    parte do rosto no quadro em que o modelo troca -- a revelacao mostraria menos
    do que promete, e nada acusaria.
    """
    fundo_do_chapeu = faixa(geo_d, subarvore(geo_d, "hat"), 1)[0]
    fundo_da_cabeca = faixa(geo_d, ("head",), 1)[0]
    queda = fundo_do_chapeu - fundo_da_cabeca + MARGEM_DO_CHAPEU
    if queda < QUEDA_MINIMA_DO_CHAPEU:
        raise SystemExit(
            "o chapeu comeca em y=%.1f e a cabeca em y=%.1f: bastariam %.1f px"
            " para tira-lo. Abaixo de %.1f px ninguem ve o chapeu cair -- ele so"
            " muda de angulo, e a revelacao perde a unica coisa que o jogador"
            " consegue apontar como 'ele largou o disfarce'."
            % (fundo_do_chapeu, fundo_da_cabeca, queda, QUEDA_MINIMA_DO_CHAPEU))
    return queda


def queda_da_morte(geo_r):
    """Quanto o body desce ao tombar -- LIDO do geo.

    Deitado de lado, o que fica na vertical e a LARGURA do torso; entao o pivot do
    corpo termina a meia largura do chao, e a queda e a diferenca entre a altura
    de pe e essa.

    PONTO CEGO DECLARADO: a conta ignora o rolamento em Z que acontece junto, e
    por isso e uma aproximacao. Ela impede o erro grosso (corpo boiando um bloco
    acima do chao, ou afundado nele); nao garante que cada quadro encoste certo.
    """
    altura_de_pe = pivot(geo_r, "body")[1]
    menor_x, maior_x = faixa(geo_r, ("body",), 0)
    deitado = (maior_x - menor_x) / 2.0
    queda = altura_de_pe - deitado
    if queda < QUEDA_MINIMA_DA_MORTE:
        raise SystemExit(
            "o pivot do corpo esta a %.1f px e o torso deitado ocupa %.1f px de"
            " meia largura: a queda daria %.1f px. Abaixo de %.1f px ninguem ve o"
            " kiriko cair -- ele so muda de angulo."
            % (altura_de_pe, deitado, queda, QUEDA_MINIMA_DA_MORTE))
    if queda > altura_de_pe:
        raise SystemExit(
            "a queda calculada (%.1f px) passa da altura do pivot (%.1f px): o"
            " corpo terminaria abaixo do chao." % (queda, altura_de_pe))
    return queda


# ------------------------------------------------------------------ portoes

def conferir_eixos(geometria, mob):
    """Os eixos se LEEM do geo. Decorados, produzem um bicho ao contrario."""
    if pivot(geometria, "head")[1] <= pivot(geometria, "body")[1]:
        raise SystemExit(
            "%s: a cabeca nao esta acima do corpo (head y=%.1f, body y=%.1f). Todo"
            " este gerador supoe +Y = CIMA."
            % (mob, pivot(geometria, "head")[1], pivot(geometria, "body")[1]))
    if centro(geometria, "arm_left", 0) <= 0:
        raise SystemExit(
            "%s: arm_left esta em x=%.1f, e o contrato supoe +X = ESQUERDA. Com o"
            " sinal trocado, ABRIR OS BRACOS no approve os CRUZA -- o gesto de"
            " reconhecimento vira um abraco de si mesmo -- e o tombo da morte cai"
            " para o lado errado." % (mob, centro(geometria, "arm_left", 0)))

    if mob == ID_VERDADEIRO:
        # O bico se mede contra o PROPRIO pivot: o que importa e a massa dele estar
        # A FRENTE da juncao, porque e isso que faz "apontar para cima" ser X
        # positivo. Compara-lo com a cabeca reprovaria um modelo correto em que os
        # dois ocupam a mesma faixa de Z.
        if centro(geometria, "beak", 2) >= pivot(geometria, "beak")[2]:
            raise SystemExit(
                "%s: o bico nao se estende a frente da propria juncao (massa em"
                " z=%.1f, pivot em z=%.1f). Assim o bico aponta para o lado errado"
                " no golpe e na morte, e so aparece para quem olhar de perto."
                % (mob, centro(geometria, "beak", 2), pivot(geometria, "beak")[2]))
        if centro(geometria, "crest", 1) <= pivot(geometria, "crest")[1]:
            raise SystemExit(
                "%s: a crista nao esta ACIMA do proprio pivot (massa em y=%.1f,"
                " pivot em y=%.1f). A crista e onde este mob poe tudo que sente --"
                " ela abaixa no aviso do golpe e levanta na aprovacao. Com a massa"
                " abaixo do pivot os dois sinais se invertem, e o jogador aprende"
                " que crista baixa e aprovacao."
                % (mob, centro(geometria, "crest", 1), pivot(geometria, "crest")[1]))
        if centro(geometria, "tail", 2) <= pivot(geometria, "tail")[2]:
            raise SystemExit(
                "%s: a cauda nao se estende ATRAS do proprio pivot (massa em"
                " z=%.1f, pivot em z=%.1f). Com a massa a frente, CAUDA_X negativo"
                " ABAIXA a cauda em vez de levanta-la, e o bicho anda arrastando o"
                " rabo -- que e a leitura de monstro pesado que a ficha recusa."
                % (mob, centro(geometria, "tail", 2), pivot(geometria, "tail")[2]))
    else:
        # PONTO CEGO DECLARADO: na forma humana nao ha bico nem cauda, e nada aqui
        # prova sozinho que -Z e a frente. O que da para conferir e que o chapeu
        # esta ACIMA da cabeca -- se nao estivesse, "cair" nao seria descer. O
        # resto herda a convencao do outro geo, que o bico prova, e a convencao
        # vanilla do repositorio.
        if centro(geometria, "hat", 1) <= centro(geometria, "head", 1):
            raise SystemExit(
                "%s: a massa do chapeu (y=%.1f) nao esta acima da cabeca (y=%.1f)."
                " Um chapeu que nao esta em cima da cabeca nao 'cai': o transform"
                " arrastaria alguma coisa para baixo do queixo."
                % (mob, centro(geometria, "hat", 1), centro(geometria, "head", 1)))


def conferir_que_a_forma_cabe_na_caixa(geometria, mob):
    """AS DUAS FORMAS DIVIDEM A MESMA `sized(1.0F, 2.1F)`, e isso e o disfarce.

    Morde dos dois lados porque os dois erros sao silenciosos e opostos:

      maior que a caixa  -> parte do corpo desenhado fica fora da hitbox. O
                           jogador acerta o ar e erra o bicho -- e neste mob um
                           golpe no ar ainda conta como agressao e REPROVA.
      muito menor        -> o bicho e acertado num espaco vazio acima da cabeca,
                           pelo mesmo motivo e com o mesmo custo.

    Se as duas formas divergirem de tamanho, a caixa deixa de ser a mesma NA TELA
    mesmo continuando a mesma no servidor -- e o jogador aprende a reconhecer o
    kiriko disfarcado pelo tamanho, em vez de pelo comportamento, que e
    exatamente o que o encontro existe para testar.
    """
    alta, larga = altura(geometria), largura(geometria)
    if alta > HITBOX_ALTURA_PX + 1e-9:
        raise SystemExit(
            "%s tem %.1f px de altura e a hitbox tem %.1f (sized(1.0F, 2.1F)). O"
            " que passa da caixa e desenhado onde o servidor nao aceita golpe."
            % (mob, alta, HITBOX_ALTURA_PX))
    if larga > HITBOX_LARGURA_PX + 1e-9:
        raise SystemExit(
            "%s tem %.1f px de largura e a hitbox tem %.1f."
            % (mob, larga, HITBOX_LARGURA_PX))
    if alta < HITBOX_ALTURA_PX * OCUPACAO_MINIMA_DA_CAIXA:
        raise SystemExit(
            "%s tem %.1f px de altura e a hitbox %.1f: ele ocupa %.0f%% da caixa"
            " (minimo %.0f%%). O bicho e acertado no vazio acima da cabeca."
            % (mob, alta, HITBOX_ALTURA_PX, 100.0 * alta / HITBOX_ALTURA_PX,
               100.0 * OCUPACAO_MINIMA_DA_CAIXA))


def conferir_postura_nao_duplicada(geometria, mob, ossos):
    """Postura assada no geo + postura animada = postura aplicada DUAS vezes.

    Mesma familia do dano multiplicado em dois handlers: o resultado fica
    plausivel demais para alguem notar sem medir. Se a outra lane precisar de uma
    rotacao assada em algum destes ossos, ela reprova aqui -- e ai as duas lanes
    conversam, que e o ponto.
    """
    for nome in ossos:
        r = osso(geometria, nome).get("rotation")
        if r and any(abs(v) > 1e-9 for v in r):
            raise SystemExit(
                "%s: o osso '%s' ja vem com rotation=%s no geo, e a postura gira"
                " esse mesmo osso na animacao. Os dois se SOMAM. Avise a lane de"
                " animacao antes de assar postura no modelo." % (mob, nome, r))


def conferir_ossos(animacoes, geometria, contrato, mob):
    """Osso errado nao da erro no GeckoLib: o membro so fica parado.

    Morde dos tres lados -- contrato, geo de verdade e animacao. Uma lane
    renomeando um osso sem avisar a outra reprova AQUI, e nao na tela de quem
    joga.
    """
    do_contrato = set(contrato)
    do_geo = {b["name"] for b in geometria["bones"]}
    if do_geo != do_contrato:
        raise SystemExit("%s: geo e contrato discordam de osso: %s"
                         % (mob, sorted(do_geo ^ do_contrato)))
    for nome, clipe in animacoes.items():
        desconhecidos = sorted(set(clipe["bones"]) - do_contrato)
        if desconhecidos:
            raise SystemExit(
                "%s move osso que nao existe nesta forma: %s. Se esses nomes sao"
                " da OUTRA forma, o par geo/animacao foi trocado."
                % (nome, desconhecidos))


def conferir_hierarquia(geometria, pais, mob):
    """O PAI e parte do contrato, e trocar o pai nao da erro -- da outro gesto.

    `claw_left` pendurada no `body` em vez do `arm_left` receberia so o proprio
    angulo: no meio da varredura do golpe ela ficaria parada no ar enquanto o
    braco passa por baixo dela.
    """
    for nome, pai in pais.items():
        tem = osso(geometria, nome).get("parent")
        if tem != pai:
            raise SystemExit(
                "%s: o osso '%s' tem parent=%r e o contrato manda %r. Osso com o"
                " pai errado herda a rotacao errada, e nada acusa."
                % (mob, nome, tem, pai))


def conferir_ossos_exclusivos(animacoes, exclusivos, mob):
    """Cada forma tem de CITAR osso que so ela tem.

    Sem isto, um arquivo que so mexesse em {body, head, arm_*, leg_*} serviria as
    duas formas por acidente -- e trocar os pares passaria pelo portao acima sem
    um unico aviso.
    """
    citados = set()
    for clipe in animacoes.values():
        citados |= set(clipe["bones"])
    faltando = sorted(set(exclusivos) - citados)
    if faltando:
        raise SystemExit(
            "%s nao anima nenhum de %s, que sao os ossos exclusivos desta forma."
            " Um arquivo assim serviria as DUAS formas, e trocar os pares deixaria"
            " de ser detectavel." % (mob, faltando))


def conferir_que_trocar_os_pares_reprova(disfarce, verdadeiro, geo_d, geo_r):
    """ALIMENTA o portao com o erro que ele existe para pegar.

    Regua que nunca reprovou e carimbo. Aqui os pares sao trocados de proposito --
    animacao humana contra geo do bicho e vice-versa -- e o portao TEM de recusar
    os dois. Se um dia ele deixar passar, este teste quebra antes de alguem
    publicar um kiriko com metade dos membros congelados.
    """
    for animacoes, geometria, contrato, rotulo in (
            (disfarce, geo_r, OSSOS_VERDADEIRO, "humano contra geo do bicho"),
            (verdadeiro, geo_d, OSSOS_DISFARCE, "bicho contra geo humano")):
        try:
            conferir_ossos(animacoes, geometria, contrato, "par trocado")
        except SystemExit:
            continue
        raise SystemExit(
            "o portao de ossos ACEITOU os pares trocados (%s). Ele nao esta"
            " protegendo nada: revise conferir_ossos e os ossos exclusivos."
            % rotulo)


def conferir_ossos_com_volume(animacoes, geometria, mob):
    """Osso sem cubo na subarvore e osso que a animacao move e ninguem ve."""
    movidos = set()
    for clipe in animacoes.values():
        movidos |= set(clipe["bones"])
    vazios = sorted(n for n in movidos if not subarvore_tem_volume(geometria, n))
    if vazios:
        raise SystemExit(
            "%s: estes ossos sao animados e nao tem cubo nenhum na subarvore: %s."
            " O clipe roda, o osso gira e a tela nao muda." % (mob, vazios))


def conferir_que_todo_clipe_cita_todo_osso_animavel(animacoes, animaveis, mob):
    """Osso que um clipe nao cita volta ao DEFAULT DO MODELO -- nao a pose anterior.

    E o buraco mais barato de abrir e o mais caro de achar. Um `hurt` sem crista
    apruma a crista por cinco ticks; um `observe` sem pernas nao pode afirmar que
    os pes estao plantados. Exigir a lista inteira em todos os dez clipes tambem
    torna a pergunta "o que ficou de fora?" respondivel numa linha: so o `root`.
    """
    for nome, clipe in animacoes.items():
        faltando = sorted(set(animaveis) - set(clipe["bones"]))
        if faltando:
            raise SystemExit(
                "%s (%s) nao cita %s. Esses ossos voltam para o default do MODELO"
                " enquanto o clipe toca, e nao para a pose em que estavam."
                % (nome, mob, faltando))
        sobrando = sorted(set(clipe["bones"]) & set(NAO_ANIMADOS))
        if sobrando:
            raise SystemExit(
                "%s (%s) anima %s. A raiz e a ancora que o renderer alinha com a"
                " hitbox: mexe-la move a silhueta para fora da caixa em que o mob"
                " e acertado." % (nome, mob, sobrando))


def conferir_clipes(animacoes, mob, clipes, loops):
    esperado = {"animation.%s.%s" % (mob, c) for c in clipes}
    if set(animacoes) != esperado:
        raise SystemExit("%s: chaves fora do contrato: %s"
                         % (mob, sorted(set(animacoes) ^ esperado)))
    for curto in clipes:
        nome = "animation.%s.%s" % (mob, curto)
        if animacoes[nome]["loop"] != loops[curto]:
            raise SystemExit(
                "%s: loop=%r e a tabela LOOPS manda %r. O campo 'loop' e a UNICA"
                " fonte (o codigo usa LoopType.DEFAULT, que delega para ele)."
                % (nome, animacoes[nome]["loop"], loops[curto]))
    for nome, clipe in animacoes.items():
        if clipe["loop"] not in (True, False, "hold_on_last_frame"):
            raise SystemExit("%s: loop invalido %r" % (nome, clipe["loop"]))
        fim = clipe["animation_length"]
        for nome_osso, canais in clipe["bones"].items():
            for canal, quadros in canais.items():
                if canal not in ("rotation", "position", "scale"):
                    raise SystemExit("%s/%s: canal invalido %s"
                                     % (nome, nome_osso, canal))
                for t, v in quadros.items():
                    if float(t) < 0 or float(t) > fim + 1e-9:
                        raise SystemExit("%s/%s: quadro %s fora de [0, %s]"
                                         % (nome, nome_osso, t, fim))
                    if canal == "scale" and min(v) <= 0:
                        raise SystemExit("%s/%s: escala %s some com o osso"
                                         % (nome, nome_osso, v))


def conferir_que_o_loop_fecha(animacoes, mob):
    """Clipe que repete e nao FECHA salta uma vez por volta -- para sempre.

    O `observe` repete ~40 vezes durante os 200 ticks de julgamento. Um salto de
    dois graus na emenda, repetido quarenta vezes, deixa de ser um detalhe: vira
    um tique, e um tique le como nervosismo -- que e o oposto exato do que o clipe
    existe para dizer. Nada no GeckoLib confere isto.
    """
    for nome, clipe in animacoes.items():
        if clipe["loop"] is not True:
            continue
        fim = tempo(clipe["animation_length"])
        for nome_osso, canais in clipe["bones"].items():
            for canal, quadros in canais.items():
                if "0.0" not in quadros or fim not in quadros:
                    raise SystemExit(
                        "%s/%s/%s repete e nao tem quadro em 0.0 e em %s. Sem os"
                        " dois extremos, a emenda interpola de um valor que"
                        " ninguem escreveu." % (nome, nome_osso, canal, fim))
                if quadros["0.0"] != quadros[fim]:
                    raise SystemExit(
                        "%s/%s/%s comeca em %s e termina em %s. O clipe repete: a"
                        " diferenca vira um salto, uma vez por volta, para sempre."
                        % (nome, nome_osso, canal, quadros["0.0"], quadros[fim]))


def conferir_duracoes(disfarce, verdadeiro):
    """As duracoes que o SERVIDOR manda nao podem divergir em silencio."""
    strike = verdadeiro["animation.%s.strike" % ID_VERDADEIRO]["animation_length"]
    if abs(strike - DUR_STRIKE) > 1e-9:
        raise SystemExit(
            "strike dura %.3fs e o golpe do servidor da %.3fs (%d+%d+%d ticks)"
            % (strike, DUR_STRIKE, TICKS_WINDUP, TICKS_ACTIVE, TICKS_RECOVERY))
    transform = disfarce["animation.%s.transform" % ID_DISFARCE]["animation_length"]
    if abs(transform - DUR_TRANSFORM) > 1e-9:
        raise SystemExit(
            "transform dura %.3fs e TICKS_TRANSFORM da %.3fs (%d ticks). Lembrando"
            " que esta constante e a UNICA fonte: a entidade copia daqui."
            % (transform, DUR_TRANSFORM, TICKS_TRANSFORM))
    # Ciclica cujo periodo nao divide a duracao da um salto a cada volta.
    for animacoes, mob, curto, periodo in (
            (disfarce, ID_DISFARCE, "walk", DUR_WALK_HUMANO),
            (verdadeiro, ID_VERDADEIRO, "walk", DUR_WALK_KIRIKO)):
        voltas = animacoes["animation.%s.%s" % (mob, curto)]["animation_length"] / periodo
        if abs(voltas - round(voltas)) > 1e-6:
            raise SystemExit("%s.%s: %.3f voltas do ciclo de %.3fs -- o loop salta"
                             % (mob, curto, voltas, periodo))


def conferir_a_janela_de_observacao(disfarce):
    """O clipe da avaliacao tem de PREENCHER os 200 ticks, e ser lido repetindo.

    Morde dos dois lados. Curto demais, `observe` vira um tique que se repete
    dezenas de vezes e o jogador le nervosismo. Longo demais (perto dos 200 ticks
    inteiros), ele nao chega a repetir uma vez sequer, e um gesto que acontece uma
    vez so nao ensina que aquilo e um ESTADO -- o jogador nao entende que esta
    sendo medido durante todo o tempo, so que o bicho fez uma coisa.
    """
    dur = disfarce["animation.%s.observe" % ID_DISFARCE]["animation_length"]
    if dur < 3.0:
        raise SystemExit(
            "observe dura %.2fs e repetiria %.0f vezes dentro dos %d ticks de"
            " julgamento. Abaixo de 3s o ciclo vira tique, e tique le como"
            " nervosismo." % (dur, JANELA_DE_OBSERVACAO / dur, TICKS_DE_OBSERVACAO))
    if dur > JANELA_DE_OBSERVACAO / 2.0:
        raise SystemExit(
            "observe dura %.2fs e a janela de julgamento tem %.2fs: ele nao chega"
            " a repetir. Um gesto que acontece uma vez nao ensina que a avaliacao"
            " e um ESTADO." % (dur, JANELA_DE_OBSERVACAO))


def faixa_do_canal(clipe, osso_nome, canal):
    """Amplitude (max - min) por eixo de um canal, somada nos tres eixos."""
    quadros = clipe["bones"].get(osso_nome, {}).get(canal)
    if not quadros:
        return 0.0
    valores = list(quadros.values())
    return sum(max(v[i] for v in valores) - min(v[i] for v in valores)
               for i in range(3))


def conferir_que_observe_e_atencao(disfarce):
    """O CLIPE QUE DEFINE O MOB, EM SEIS NUMEROS.

    "Ele esta te avaliando" nao aparece num diff, e nao ha portao neste
    repositorio que olhe para a tela. O que da para medir e a forma da afirmacao:
    a cabeca lidera, o corpo respira pouco, as maos nao fazem nada, os pes nao
    saem do lugar, o olhar nao varre o horizonte e o olhar PARA.

    Cada um destes seis existe porque o erro correspondente e a coisa mais natural
    do mundo de fazer "melhorando" o clipe -- e cada um deles muda o que o jogador
    decide fazer nos 200 ticks seguintes.
    """
    clipe = disfarce["animation.%s.observe" % ID_DISFARCE]
    cabeca = faixa_do_canal(clipe, "head", "rotation")
    bracos = max(faixa_do_canal(clipe, "arm_left", "rotation"),
                 faixa_do_canal(clipe, "arm_right", "rotation"))
    corpo = faixa_do_canal(clipe, "body", "rotation")
    corpo_pos = faixa_do_canal(clipe, "body", "position")

    if corpo + corpo_pos <= 0.0:
        raise SystemExit(
            "no observe o corpo nao se mexe NADA. Isso e o truque do man-faced"
            " ape, e la ele serve para assustar: um corpo humano que nao respira e"
            " a pista de que aquilo nao e humano. Aqui e a leitura errada -- o"
            " kiriko nao esta emboscando ninguem, ele esta decidindo.")
    if corpo > OBSERVE_CORPO_MAXIMO:
        raise SystemExit(
            "no observe o corpo gira %.1f graus (maximo %.1f). Corpo que se mexe"
            " assim nao esta parado, e 'corpo imovel' e metade do que este clipe"
            " existe para dizer -- sem ele, observe vira um segundo idle."
            % (corpo, OBSERVE_CORPO_MAXIMO))
    if bracos > OBSERVE_BRACO_MAXIMO:
        raise SystemExit(
            "no observe os bracos giram %.1f graus (maximo %.1f). O jogador le MAO"
            " antes de ler qualquer outra coisa: mao que se mexe e de onde ele"
            " espera que venha a agressao, e quem saca primeiro REPROVA."
            % (bracos, OBSERVE_BRACO_MAXIMO))
    if cabeca < bracos * OBSERVE_CABECA_SOBRE_BRACO:
        raise SystemExit(
            "no observe a cabeca gira %.1f graus e os bracos %.1f: a cabeca nao"
            " esta liderando com folga (exige %.1fx). A inclinacao de cabeca e o"
            " unico gesto que le universalmente como 'estou te avaliando' em vez de"
            " 'vou te atacar'." % (cabeca, bracos, OBSERVE_CABECA_SOBRE_BRACO))

    pernas = max(faixa_do_canal(clipe, "leg_left", "rotation"),
                 faixa_do_canal(clipe, "leg_right", "rotation"))
    if pernas > 0.0:
        raise SystemExit(
            "no observe as pernas giram %.1f graus. Os pes ficam PLANTADOS: um"
            " passinho de ajuste seria a coisa mais natural de acrescentar e e"
            " exatamente o que faz o corpo parecer inquieto -- quem esta decidindo"
            " sobre voce nao remexe os pes." % pernas)

    quadros = sorted((float(t), v) for t, v
                     in clipe["bones"]["head"]["rotation"].items())
    yaw = max(v[1] for _, v in quadros) - min(v[1] for _, v in quadros)
    if yaw > OBSERVE_YAW_MAXIMO:
        raise SystemExit(
            "no observe a cabeca varre %.1f graus de yaw (maximo %.1f). Varrimento"
            " largo le como vigia procurando qualquer coisa, que e o contrario de"
            " alguem medindo VOCE -- e e a quantidade que passaria do jogador se"
            " um dia houver head-tracking no renderer." % (yaw, OBSERVE_YAW_MAXIMO))

    parado = 0.0
    for (t0, v0), (t1, v1) in zip(quadros, quadros[1:]):
        if max(abs(a - b) for a, b in zip(v1, v0)) < OBSERVE_LIMIAR_DE_PARADO:
            parado += t1 - t0
    fracao = parado / clipe["animation_length"]
    if fracao < OBSERVE_FRACAO_PARADA:
        raise SystemExit(
            "no observe o olhar so fica parado em %.0f%% do clipe (minimo %.0f%%)."
            " As pausas SAO o clipe: um varrimento continuo le como radar, e um"
            " olhar que se move e FICA le como alguem que achou o que procurava."
            % (100.0 * fracao, 100.0 * OBSERVE_FRACAO_PARADA))


def conferir_idle_e_observe_sao_distinguiveis(disfarce):
    """Se os dois ficarem parecidos, o mob perde a unica pista que da.

    O jogador tem de conseguir ver a diferenca entre "o viajante esta ali" e "o
    viajante esta olhando para mim" -- e a diferenca nao e o que se mexe, e ONDE.
    No idle o corpo e o maior movimento; no observe, a cabeca.
    """
    idle = disfarce["animation.%s.idle" % ID_DISFARCE]
    obs = disfarce["animation.%s.observe" % ID_DISFARCE]
    corpo_idle = faixa_do_canal(idle, "body", "rotation")
    corpo_obs = faixa_do_canal(obs, "body", "rotation")
    if corpo_idle <= corpo_obs:
        raise SystemExit(
            "o corpo do idle gira %.1f graus e o do observe %.1f. O idle tem de"
            " ser o clipe em que o corpo se ocupa de si; sem esse contraste,"
            " observe deixa de ser um estado especial e vira o normal do bicho."
            % (corpo_idle, corpo_obs))


def maior_taxa(clipe, canal, excluir=()):
    """(instante medio, taxa) do trecho mais rapido de um canal.

    Trecho e par de keyframes consecutivos; a taxa e o maior delta entre os tres
    eixos dividido pelo intervalo. Serve para perguntar coisas que so se respondem
    com velocidade -- "isto le como susto?", "isto le como fuga?" -- e que nenhuma
    pose isolada responde.
    """
    melhor = (None, 0.0)
    for nome, canais in clipe["bones"].items():
        if nome in excluir:
            continue
        quadros = canais.get(canal)
        if not quadros:
            continue
        ordenados = sorted((float(t), v) for t, v in quadros.items())
        for (t0, v0), (t1, v1) in zip(ordenados, ordenados[1:]):
            dt = t1 - t0
            if dt <= 1e-9:
                continue
            taxa = max(abs(a - b) for a, b in zip(v1, v0)) / dt
            if taxa > melhor[1]:
                melhor = ((t0 + t1) / 2.0, taxa)
    return melhor


def conferir_a_janela_do_golpe(verdadeiro):
    """O golpe tem 10 ticks de aviso e 5 de janela, e quem decide e o SERVIDOR.

    Se o braco nao chegar ao topo EXATAMENTE em 0.50 e nao estiver descendo ao
    longo de [0.50, 0.75], o desenho promete um tempo que o jogo nao cumpre -- e o
    jogador aprende a se esquivar da animacao, nao do golpe. Nada no GeckoLib
    confere isso, e nenhum gametest ve a tela.
    """
    clipe = verdadeiro["animation.%s.strike" % ID_VERDADEIRO]
    quadros = clipe["bones"]["arm_right"]["rotation"]
    for marco, rotulo in ((FIM_DO_WINDUP, "fim do aviso"),
                          (FIM_DA_JANELA, "fim da janela de dano")):
        if tempo(marco) not in quadros:
            raise SystemExit(
                "strike nao tem keyframe de arm_right em %s (%s). Sem um quadro"
                " cravado ali, mexer em qualquer pose vizinha desloca o golpe sem"
                " que nada acuse." % (tempo(marco), rotulo))
    apice_t, apice_v = max(quadros.items(), key=lambda kv: kv[1][0])
    if apice_t != tempo(FIM_DO_WINDUP):
        raise SystemExit(
            "o braco chega ao topo em %s, e o aviso do servidor acaba em %s."
            % (apice_t, tempo(FIM_DO_WINDUP)))
    fim = quadros[tempo(FIM_DA_JANELA)][0]
    if fim >= apice_v[0]:
        raise SystemExit(
            "de %s a %s o braco vai de %.1f a %.1f graus: ele nao esta DESCENDO na"
            " janela em que o dano acontece."
            % (tempo(FIM_DO_WINDUP), tempo(FIM_DA_JANELA), apice_v[0], fim))
    for t, v in quadros.items():
        if float(t) < FIM_DO_WINDUP and v[0] > apice_v[0] + 1e-9:
            raise SystemExit("strike: o braco passa do apice em %s, antes do aviso"
                             " terminar" % t)


def conferir_que_a_crista_avisa_antes_do_braco(verdadeiro):
    """O aviso do golpe comeca na CABECA, e isso e uma promessa feita no observe.

    Durante 200 ticks o jogador aprendeu a olhar para a cabeca deste bicho -- e a
    inclinacao da cabeca no observe, e a crista erguida em alerta. Se o golpe
    comecasse pelo braco, aquele aprendizado nao serviria de nada no unico momento
    em que ele importa.
    """
    clipe = verdadeiro["animation.%s.strike" % ID_VERDADEIRO]
    crista = sorted((float(t), v) for t, v
                    in clipe["bones"]["crest"]["rotation"].items())
    braco = sorted((float(t), v) for t, v
                   in clipe["bones"]["arm_right"]["rotation"].items())

    def primeiro_movimento(quadros, limiar):
        base = quadros[0][1]
        for t, v in quadros[1:]:
            if max(abs(a - b) for a, b in zip(v, base)) >= limiar:
                return t
        return None

    t_crista = primeiro_movimento(crista, 5.0)
    t_braco = primeiro_movimento(braco, 5.0)
    if t_crista is None or t_braco is None or t_crista >= t_braco:
        raise SystemExit(
            "no strike a crista se mexe em %s e o braco em %s: o aviso nao comeca"
            " pela cabeca. O jogador passou 200 ticks aprendendo a ler a cabeca"
            " deste bicho -- e o unico momento em que essa leitura vale alguma"
            " coisa e este." % (t_crista, t_braco))


def conferir_que_approve_nao_e_ataque_nem_fuga(verdadeiro):
    """O PORTAO MAIS IMPORTANTE DESTE ARQUIVO.

    `approve` e o premio do encontro e tem dois jeitos plausiveis de sair errado:
    lendo como ataque (e o jogador bate primeiro) ou lendo como fuga (e o jogador
    aprende que assustou o bicho em vez de ter sido reconhecido por ele). Cinco
    medidas, uma para cada jeito de errar.
    """
    clipe = verdadeiro["animation.%s.approve" % ID_VERDADEIRO]
    dur = clipe["animation_length"]

    # 1. O TRONCO NUNCA VAI PARA A FRENTE.
    for t, v in clipe["bones"]["body"]["rotation"].items():
        if v[0] < BODY_X - APPROVE_TOLERANCIA_DO_TRONCO:
            raise SystemExit(
                "no approve o tronco chega a %.1f graus em %s e a postura e %.1f:"
                " ele esta inclinando o peito PARA A FRENTE. Peito adiantado le"
                " como investida, e nenhum gesto de mao desfaz isso."
                % (v[0], t, BODY_X))

    # 2. OS BRACOS ABREM PARA FORA (Z), NAO PARA A FRENTE (X).
    for lado in ("arm_left", "arm_right"):
        quadros = list(clipe["bones"][lado]["rotation"].values())
        em_x = max(v[0] for v in quadros) - min(v[0] for v in quadros)
        em_z = max(v[2] for v in quadros) - min(v[2] for v in quadros)
        if em_z < em_x * APPROVE_RAZAO_FORA_SOBRE_FRENTE:
            raise SystemExit(
                "no approve o %s abre %.1f graus em Z e %.1f em X (exige %.1fx)."
                " Em X o braco faz o arco do GOLPE: o gesto de reconhecimento"
                " passa a ter a mesma forma da agressao."
                % (lado, em_z, em_x, APPROVE_RAZAO_FORA_SOBRE_FRENTE))

    # 3. UMA vez so.
    serie = [v[2] for _, v in sorted((float(t), v) for t, v
                                     in clipe["bones"]["arm_left"]["rotation"].items())]
    apices = sum(1 for i in range(1, len(serie) - 1)
                 if serie[i] > serie[i - 1] and serie[i] >= serie[i + 1])
    if apices != APPROVE_ABERTURAS:
        raise SystemExit(
            "no approve o braco esquerdo abre %d vezes e tem de abrir %d. Duas"
            " aberturas viram bater de asas, que em bicho e display de AMEACA."
            % (apices, APPROVE_ABERTURAS))

    # 4. A VIRADA E TARDE E CURTA -- e o modelo so a COMECA.
    virada = sorted((float(t), v[1]) for t, v
                    in clipe["bones"]["body"]["rotation"].items())
    for t, y in virada:
        if t < dur * APPROVE_INICIO_DA_VIRADA and abs(y) > 1e-9:
            raise SystemExit(
                "no approve o corpo ja esta virando %.1f graus em %s, antes de"
                " %.2fs. Virar durante o gesto transforma reconhecimento em 'ele"
                " se assustou e foi embora'."
                % (y, t, dur * APPROVE_INICIO_DA_VIRADA))
    maior = max(abs(y) for _, y in virada)
    if maior > APPROVE_YAW_MAXIMO:
        raise SystemExit(
            "no approve o corpo vira %.1f graus (maximo %.1f). O MODELO so comeca"
            " a virada; quem termina e a entidade, andando. Um modelo virado de"
            " mais fica de costas dentro da propria hitbox, e o corpo desenhado"
            " passa a discordar da caixa em que o mob e acertado."
            % (maior, APPROVE_YAW_MAXIMO))

    # 5. NADA AQUI SE MEXE NA VELOCIDADE DO COMBATE.
    _, taxa_approve = maior_taxa(clipe, "rotation")
    _, taxa_strike = maior_taxa(
        verdadeiro["animation.%s.strike" % ID_VERDADEIRO], "rotation")
    teto = taxa_strike * APPROVE_FRACAO_DA_VELOCIDADE_DO_GOLPE
    if taxa_approve > teto:
        raise SystemExit(
            "o quadro mais rapido do approve corre a %.0f graus/s e o do strike a"
            " %.0f (teto %.0f). Fuga e exatamente isto: o mesmo corpo, na mesma"
            " pressa, para o outro lado." % (taxa_approve, taxa_strike, teto))


def conferir_que_a_revelacao_nao_e_um_susto(disfarce):
    """"Nao e um susto" vira dois numeros.

    (a) NADA PASSA DA POSE FINAL. Quique ao assentar e o que da o arranco, e
        arranco e a gramatica de monstro saindo de tras da porta. O chapeu fica de
        fora: ele esta CAINDO, e um chapeu que desacelerasse suavemente estaria
        sendo pousado, nao caindo.
    (b) O QUADRO MAIS RAPIDO NAO ESTA NO COMECO. Transformacao que estala nos
        primeiros seis ticks e jump scare. Os primeiros ticks deste clipe sao o
        beat de decisao, e e ele que faz a revelacao ler como escolha.

    Os tres canais sao medidos separados porque as unidades nao se comparam --
    graus/s, px/s e escala/s -- e porque um chapeu que TELEPORTASSE para baixo
    passaria despercebido numa conta que so olhasse rotacao.
    """
    clipe = disfarce["animation.%s.transform" % ID_DISFARCE]
    dur = clipe["animation_length"]
    fim = tempo(dur)

    for nome, canais in clipe["bones"].items():
        if nome == "hat":
            continue
        for canal, quadros in canais.items():
            if canal == "scale":
                continue
            final = quadros[fim]
            for t, v in quadros.items():
                for eixo in range(3):
                    alvo, tem = final[eixo], v[eixo]
                    if alvo == 0:
                        continue
                    passou = (tem > alvo + TRANSFORM_TOLERANCIA_DE_OVERSHOOT
                              if alvo > 0 else
                              tem < alvo - TRANSFORM_TOLERANCIA_DE_OVERSHOOT)
                    if passou:
                        raise SystemExit(
                            "no transform, %s/%s eixo %d chega a %.2f em %s e"
                            " termina em %.2f: o corpo PASSA da pose final e volta."
                            " Esse quique e o arranco, e arranco e susto."
                            % (nome, canal, eixo, tem, t, alvo))

    protegido = dur * TRANSFORM_INICIO_PROTEGIDO
    for canal in ("rotation", "position", "scale"):
        quando, taxa = maior_taxa(clipe, canal)
        if quando is not None and taxa > 0 and quando < protegido:
            raise SystemExit(
                "no transform o trecho mais rapido de %s esta em %.2fs, dentro dos"
                " primeiros %.2fs. Transformacao que estala no comeco e jump scare"
                " -- e este mob nao assusta, ele decide." % (canal, quando, protegido))


def conferir_que_o_transform_comeca_no_repouso(disfarce):
    """O primeiro quadro da revelacao E o corpo humano em repouso.

    Ele nao comeca na pose do `observe` de proposito: o observe esta em movimento
    (a cabeca), e nao existe um quadro dele que sirva de ancora. Comecar no
    repouso e a unica escolha que nao depende de em que ponto do loop o servidor
    mandou transformar -- e a transicao do controller cobre a diferenca.
    """
    clipe = disfarce["animation.%s.transform" % ID_DISFARCE]
    for nome, canais in clipe["bones"].items():
        for canal, quadros in canais.items():
            neutro = escala() if canal == "scale" else vetor()
            if quadros.get("0.0") != neutro:
                raise SystemExit(
                    "o transform comeca com %s/%s em %s e o repouso humano e %s."
                    " Comecar fora do repouso amarra a revelacao ao ponto do loop"
                    " em que o servidor mandou transformar."
                    % (nome, canal, quadros.get("0.0"), neutro))


def conferir_a_costura(disfarce, verdadeiro):
    """A TROCA DE MODELO E A FRONTEIRA MAIS PERIGOSA DESTA ENTREGA.

    O ultimo quadro do transform e o primeiro quadro de TODO clipe do bicho tem de
    ser a MESMA pose, osso a osso. Se divergirem, o jogador ve um salto no exato
    quadro em que o mob se revela -- e o GeckoLib nao tem nada a dizer sobre isso,
    porque para ele sao dois modelos que nunca se encontraram.

    A ESCALA FICA FORA DA COMPARACAO de proposito: o corpo humano cresce ate o
    tamanho do bicho, que ja nasce grande no proprio geo. Exigir escala igual aqui
    compararia duas coisas que nao sao a mesma.

    Do lado do bicho a conferencia inclui bico, crista, cauda, garras e pes, que a
    forma humana nem tem: a costura com o humano e so dos ossos comuns, mas a
    costura de um clipe do bicho para o SEGUINTE e de todos.
    """
    comuns, posicao = postura_comum()
    transform = disfarce["animation.%s.transform" % ID_DISFARCE]
    fim = tempo(transform["animation_length"])
    for nome in COMUNS:
        tinha = transform["bones"].get(nome, {}).get("rotation", {}).get(fim)
        if tinha != comuns[nome]:
            raise SystemExit(
                "o transform termina com '%s' em %s e a postura do kiriko e %s. O"
                " modelo troca nesse quadro: a diferenca vira um salto."
                % (nome, tinha, comuns[nome]))
    if transform["bones"]["body"]["position"].get(fim) != posicao:
        raise SystemExit(
            "o transform termina com body.position=%s e a postura pede %s."
            % (transform["bones"]["body"]["position"].get(fim), posicao))

    completa, _ = postura_completa()
    for nome_clipe, clipe in verdadeiro.items():
        for nome, esperado in completa.items():
            tem = clipe["bones"].get(nome, {}).get("rotation", {}).get("0.0")
            if tem != esperado:
                raise SystemExit(
                    "%s comeca com '%s' em %s e a postura e %s. Osso que um clipe"
                    " nao poe na postura volta para o DEFAULT DO MODELO, nao para a"
                    " postura -- e aquele membro endireita sozinho."
                    % (nome_clipe, nome, tem, esperado))
        if clipe["bones"]["body"]["position"].get("0.0") != posicao:
            raise SystemExit(
                "%s comeca com body.position=%s e a postura pede %s."
                % (nome_clipe, clipe["bones"]["body"]["position"].get("0.0"),
                   posicao))


def conferir_a_marcha(disfarce, verdadeiro):
    """"Nao pode parecer bicho" e "nao pode ser bestial" viram quatro numeros.

    ATENCAO A INVERSAO EM RELACAO AO MAN-FACED APE: la o portao exige braco com
    amplitude MAIOR que a perna, porque o primata se puxa pelos bracos. Aqui as
    DUAS formas exigem o contrario. Copiar aquele portao para ca (que e o reflexo
    natural, ja que os dois mobs sao shapeshifters) enforcaria exatamente a
    marcha que a ficha deste recusa.
    """
    humano = disfarce["animation.%s.walk" % ID_DISFARCE]["bones"]
    inclinacao = max(abs(v[0]) for v in humano["body"]["rotation"].values())
    if inclinacao > 1e-9:
        raise SystemExit(
            "a caminhada humana inclina o tronco em %.1f graus (X). Basta inclinar"
            " o peito para a frente e a silhueta vira bicho, e este corpo precisa"
            " passar por gente ate a hora em que ele escolher." % inclinacao)
    if HUMANO_BRACO >= HUMANO_PERNA:
        raise SystemExit(
            "no humano o braco oscila %.0f e a perna %.0f: braco com a amplitude da"
            " perna e marcha de quem carrega o proprio peso nos bracos."
            % (HUMANO_BRACO, HUMANO_PERNA))
    if KIRIKO_BRACO >= KIRIKO_PERNA:
        raise SystemExit(
            "no kiriko o braco oscila %.0f e a perna %.0f. A ficha pede 'andar de"
            " bicho grande e consciente -- nao arrastado, nao bestial', e braco com"
            " amplitude de perna e quadrupede-ish. E o INVERSO do que vale para o"
            " man-faced ape, de proposito." % (KIRIKO_BRACO, KIRIKO_PERNA))
    bicho = verdadeiro["animation.%s.walk" % ID_VERDADEIRO]["bones"]
    for t, v in bicho["body"]["rotation"].items():
        if abs(v[0] - BODY_X) > 1e-9:
            raise SystemExit(
                "no walk do kiriko o tronco vai a %.1f graus em %s e a postura e"
                " %.1f. O peso deste bicho vai em Z (rolamento); mergulhar em X e"
                " posicao de bote, qualquer que seja a perna." % (v[0], t, BODY_X))


def conferir_que_a_cabeca_e_a_ultima(verdadeiro):
    """A ficha da morte e uma ORDEM, e ordem se mede em tempo.

    "Cede, tomba, cabeca por ultimo." O quadro mais rapido da cabeca tem de vir
    DEPOIS do quadro mais rapido do corpo. Sem este portao, a primeira correcao
    distraida sincroniza os dois e a morte deixa de ler como um corpo perdendo as
    partes uma a uma; passa a ler como desligar o bicho.
    """
    clipe = verdadeiro["animation.%s.death" % ID_VERDADEIRO]
    quando_cabeca, _ = maior_taxa({"bones": {"head": clipe["bones"]["head"]}},
                                  "rotation")
    quando_corpo, _ = maior_taxa({"bones": {"body": clipe["bones"]["body"]}},
                                 "rotation")
    if quando_cabeca is None or quando_corpo is None:
        raise SystemExit("a morte nao move cabeca ou corpo -- nao ha ordem a medir.")
    if quando_cabeca <= quando_corpo:
        raise SystemExit(
            "na morte o quadro mais rapido da cabeca esta em %.2fs e o do corpo em"
            " %.2fs: eles caem JUNTOS. A ficha pede 'cede, tomba, cabeca por"
            " ultimo' -- sincronizados, os dois lem como desligar o bicho."
            % (quando_cabeca, quando_corpo))


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
    clipe["bones"] = dict(sorted(clipe["bones"].items()))
    return clipe


def escrever(mob, animacoes):
    destino = os.path.join(DIR_ANIM, mob + ".animation.json")
    os.makedirs(DIR_ANIM, exist_ok=True)
    texto = serializar({"format_version": "1.8.0", "animations": animacoes})
    json.loads(texto)  # o que sai daqui tem de voltar como JSON, sempre.
    with open(destino, "w", encoding="utf-8", newline="\n") as saida:
        saida.write(texto + "\n")
    return destino


def tabela(titulo, animacoes):
    print("\n%s" % titulo)
    print("  %-38s %6s  %-19s %s" % ("clipe", "dur", "loop", "ossos"))
    for nome, clipe in animacoes.items():
        print("  %-38s %5.2fs  %-19s %s"
              % (nome, clipe["animation_length"], str(clipe["loop"]),
                 ", ".join(sorted(clipe["bones"]))))


# ------------------------------------------------------------------- main

def main():
    geo_d = carregar_geo(ID_DISFARCE)
    geo_r = carregar_geo(ID_VERDADEIRO)

    conferir_hierarquia(geo_d, PAI_DISFARCE, ID_DISFARCE)
    conferir_hierarquia(geo_r, PAI_VERDADEIRO, ID_VERDADEIRO)
    conferir_eixos(geo_d, ID_DISFARCE)
    conferir_eixos(geo_r, ID_VERDADEIRO)
    conferir_que_a_forma_cabe_na_caixa(geo_d, ID_DISFARCE)
    conferir_que_a_forma_cabe_na_caixa(geo_r, ID_VERDADEIRO)
    conferir_postura_nao_duplicada(geo_r, ID_VERDADEIRO, ANIMAVEIS_VERDADEIRO)
    conferir_postura_nao_duplicada(geo_d, ID_DISFARCE, ANIMAVEIS_DISFARCE)

    crescimento = crescimento_do_corpo(geo_d, geo_r)
    braco_extra = alongamento_do_braco(geo_d, geo_r, crescimento[1])
    chapeu = queda_do_chapeu(geo_d)
    queda = queda_da_morte(geo_r)

    disfarce = {
        "animation.%s.idle" % ID_DISFARCE: ordenar(humano_idle()),
        "animation.%s.walk" % ID_DISFARCE: ordenar(humano_walk()),
        "animation.%s.observe" % ID_DISFARCE: ordenar(humano_observe()),
        "animation.%s.transform" % ID_DISFARCE:
            ordenar(humano_transform(crescimento, braco_extra, chapeu)),
    }
    verdadeiro = {
        "animation.%s.idle" % ID_VERDADEIRO: ordenar(kiriko_idle()),
        "animation.%s.walk" % ID_VERDADEIRO: ordenar(kiriko_walk()),
        "animation.%s.strike" % ID_VERDADEIRO: ordenar(kiriko_strike()),
        "animation.%s.approve" % ID_VERDADEIRO: ordenar(kiriko_approve()),
        "animation.%s.hurt" % ID_VERDADEIRO: ordenar(kiriko_hurt()),
        "animation.%s.death" % ID_VERDADEIRO: ordenar(kiriko_death(queda)),
    }

    conferir_clipes(disfarce, ID_DISFARCE, CLIPES_DISFARCE, LOOPS_DISFARCE)
    conferir_clipes(verdadeiro, ID_VERDADEIRO, CLIPES_VERDADEIRO, LOOPS_VERDADEIRO)
    conferir_ossos(disfarce, geo_d, OSSOS_DISFARCE, ID_DISFARCE)
    conferir_ossos(verdadeiro, geo_r, OSSOS_VERDADEIRO, ID_VERDADEIRO)
    conferir_ossos_exclusivos(disfarce, EXCLUSIVOS_DISFARCE, ID_DISFARCE)
    conferir_ossos_exclusivos(verdadeiro, EXCLUSIVOS_VERDADEIRO, ID_VERDADEIRO)
    conferir_que_trocar_os_pares_reprova(disfarce, verdadeiro, geo_d, geo_r)
    conferir_ossos_com_volume(disfarce, geo_d, ID_DISFARCE)
    conferir_ossos_com_volume(verdadeiro, geo_r, ID_VERDADEIRO)
    conferir_que_todo_clipe_cita_todo_osso_animavel(
        disfarce, ANIMAVEIS_DISFARCE, ID_DISFARCE)
    conferir_que_todo_clipe_cita_todo_osso_animavel(
        verdadeiro, ANIMAVEIS_VERDADEIRO, ID_VERDADEIRO)
    conferir_que_o_loop_fecha(disfarce, ID_DISFARCE)
    conferir_que_o_loop_fecha(verdadeiro, ID_VERDADEIRO)
    conferir_duracoes(disfarce, verdadeiro)
    conferir_a_janela_de_observacao(disfarce)
    conferir_que_observe_e_atencao(disfarce)
    conferir_idle_e_observe_sao_distinguiveis(disfarce)
    conferir_que_o_transform_comeca_no_repouso(disfarce)
    conferir_que_a_revelacao_nao_e_um_susto(disfarce)
    conferir_a_costura(disfarce, verdadeiro)
    conferir_a_janela_do_golpe(verdadeiro)
    conferir_que_a_crista_avisa_antes_do_braco(verdadeiro)
    conferir_que_approve_nao_e_ataque_nem_fuga(verdadeiro)
    conferir_a_marcha(disfarce, verdadeiro)
    conferir_que_a_cabeca_e_a_ultima(verdadeiro)

    print("escrito", escrever(ID_DISFARCE, disfarce))
    print("escrito", escrever(ID_VERDADEIRO, verdadeiro))
    print("\nnumeros LIDOS dos geos:")
    print("  altura humana / bicho    : %.2f / %.2f px  (hitbox %.1f)"
          % (altura(geo_d), altura(geo_r), HITBOX_ALTURA_PX))
    print("  crescimento do corpo     : %.3fx em X/Z, %.3fx em Y"
          % (crescimento[0], crescimento[1]))
    print("  alongamento extra do braco: %.3fx (local, sobre a escala do body)"
          % braco_extra)
    print("  queda do chapeu          : %.2f px" % chapeu)
    print("  queda da morte           : %.2f px" % queda)
    tabela("FORMA HUMANA (%s)" % ID_DISFARCE, disfarce)
    tabela("FORMA VERDADEIRA (%s)" % ID_VERDADEIRO, verdadeiro)


if __name__ == "__main__":
    main()
