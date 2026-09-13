"""Gera a geometria autoral do Foxbear (bedrock 1.12.0, carregada pelo GeckoLib 4).

POR QUE ESTE ARQUIVO EXISTE. O ADR-017 diz que mob vanilla e andaime: o Foxbear
ja e territorial em jogo -- ele avisa antes de atacar, tem raio de territorio de
12 blocos e desiste depois de 80 ticks de aviso ignorado -- vestindo a GEOMETRIA
DO URSO POLAR. Por isso ele parece pronto: o comportamento passa, o log nao diz
nada, e o unico sinal do contrario e alguem abrir o jogo e reconhecer um urso
polar marrom. Ele e o ULTIMO nome da divida visual; os outros quatro ja sairam.

Nao ha Blockbench aqui, entao a FONTE VERSIONADA e este Python: JSON gerado sem
gerador no git vira um arquivo que ninguem consegue corrigir depois.

ESTE MODULO E A UNICA TABELA DE CAIXAS. O gerador da textura importa CAIXAS daqui
de proposito: duas tabelas separadas divergem na primeira correcao de modelo, e a
divergencia nao da erro -- da face pintada no lugar errado, que so aparece na
tela.

ESTA ENTREGA NAO MUDA COMPORTAMENTO NENHUM. Raio de territorio, duracao do aviso
e alvo sao do servidor e continuam la; aqui so existe o corpo que os desenha.

O QUE TORNA ESTE BICHO DIFICIL: ELE E DOIS ANIMAIS.
--------------------------------------------------
A diretriz de mobs customizados e explicita sobre o que NAO fazer: "nao fazer
apenas urso polar + textura marrom + orelhas diferentes". Um hibrido que herda a
silhueta inteira de um dos pais nao e hibrido, e o pai pintado. Entao a anatomia
tem de dizer as DUAS coisas ao mesmo tempo, e e o CONTRASTE entre elas que faz o
bicho parecer errado -- que e exatamente o que se quer de um Foxbear:

  * do URSO vem o VOLUME. Cernelha alta (o cupim de ombro, `chest`, e a peca mais
    alta E mais larga do modelo), pescoco mais grosso que a propria cabeca, patas
    mais largas que as pernas, garras a frente de cada pata, ventre alto do chao;
  * da RAPOSA vem a CABECA. Focinho mais comprido que largo e no maximo METADE da
    largura do cranio, orelhas eretas mais altas que largas, cauda densa que
    sobra atras da garupa e cai abaixo da linha da barriga.

`valida_urso` e `valida_raposa` cobram cada um desses itens, porque "ficou bonito"
nao e criterio e porque a falha aqui e muda: um modelo que perdeu metade da
heranca continua carregando, continua animando e continua passando em todo portao
do repositorio.

O CLIPE `rear_warn` MANDA NO QUADRIL, E ISSO E GEOMETRIA, NAO ANIMACAO.
-----------------------------------------------------------------------
Este mob se ergue nas patas traseiras para avisar. A pose nao mora aqui -- mora no
.animation.json -- mas ela so e POSSIVEL se o modelo tiver sido feito para ela, e
nada no repositorio reprova um modelo que nao aguenta a propria animacao: o clipe
toca, os ossos giram, e o que aparece na tela e o peito saindo por dentro da
garupa. Entao o gerador simula a pose e reprova os tres jeitos de erra-la:

  * `valida_quadril` exige que o pivot de `body` seja um QUADRIL DE VERDADE --
    metade de tras e metade de baixo do proprio barril -- e que os pivots de
    `leg_back_*` coincidam com ele em y e z. Se nao coincidirem, contra-girar as
    traseiras para mante-las plantadas NAO devolve a perna ao lugar: o bicho se
    ergue flutuando ou afunda as patas no chao, e nada acusa;
  * `valida_pose_rear_warn` gira o tronco ANGULO_DE_REAR_WARN graus em torno desse
    quadril e reprova se alguma quina descer abaixo do chao (a garupa atravessa o
    piso) ou se o ganho de altura for pequeno demais (o bicho "se ergue" e a
    silhueta mal muda -- o aviso existe no arquivo e ninguem ve);
  * `valida_peito_nao_atravessa_o_quadril` arqueia `chest` em torno da cintura e
    exige que toda quina do peito que estava ENTERRADA no barril continue
    enterrada. Quina enterrada que sai do barril e o peito furando o proprio
    lombo, e e o sintoma que a diretriz nomeia.

E HA UMA FALHA MUDA QUE SO ESTE GERADOR PEGA: duas caixas com a MESMA face no
MESMO plano. Em jogo isso nao e erro, e cintilacao -- o famoso "z-fighting", que o
jogador le como bug de driver e que nenhum portao do repositorio enxerga, porque
o JSON continua perfeitamente valido. `valida_faces_coplanares` reprova. Caixa que
so ENCOSTA na vizinha (faces OPOSTAS no mesmo plano) e permitida de proposito:
isso e como todo modelo do jogo e feito.

A HITBOX MANDA NA LARGURA E NA ALTURA. NAO NO COMPRIMENTO.
----------------------------------------------------------
A entidade e sized(1.4F, 1.35F) -- 22,4 x 21,6 px -- e `valida_hitbox` reprova o
modelo PARADO que estourar isso em largura ou altura. Sao esses dois que o jogador
LE como distancia: a largura diz de quanto ele precisa desviar, a altura diz se da
para pular por cima. Silhueta que mente sobre qualquer um dos dois mata quem fez a
conta certa pelo desenho errado.

Comprimento e outra conversa, e a evidencia esta no proprio bicho que sai daqui: a
geometria emprestada do URSO POLAR -- na MESMA hitbox de 1,4 -- tem os cubos de
tronco 14x14x11 e 12x12x10, ou seja ~21 px de torso, mais 7 de cranio e 3 de
focinho: perto de 31 px de comprimento dentro de uma caixa de 22,4. Nao e descuido
da Mojang. Quadrupede nenhum do jogo cabe no proprio comprimento de colisao, e a
razao e geometrica: um torso limitado a 22 px sobre um ombro de 21 px vira um CUBO
com pernas. Este gerador ja produziu esse cubo uma vez, e o que denunciou nao foi
portao nenhum -- foi olhar o perfil desenhado.

Entao o comprimento tem teto PROPRIO e declarado, COMPRIMENTO_MAXIMO_PX, ancorado
no urso polar que esta sendo substituido. Que a pose de `rear_warn` saia da hitbox
tambem e esperado e correto: quem responde por isso e `visible_bounds_*`, e nao a
caixa de colisao.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

Regerar:  python art-source/enemies/foxbear/foxbear_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/foxbear.geo.json
"""
from collections import namedtuple
import json
import math
import os
import re

IDENTIFICADOR = "geometry.foxbear"
UV_LARGURA, UV_ALTURA = 128, 64

# Limites que a entidade impoe: FoxbearEntity / EntityType .sized(1.4F, 1.35F).
HITBOX_LARGURA_PX = 1.4 * 16.0
HITBOX_ALTURA_PX = 1.35 * 16.0

# Teto de COMPRIMENTO, que nao sai da hitbox. Ver o cabecalho: a geometria do urso
# polar que este modelo substitui tem ~31 px de comprimento na mesma hitbox de
# 1,4 bloco. Dois blocos e o teto adotado -- o bastante para o torso ler como
# torso, e pouco o bastante para o bicho nao virar um trem.
COMPRIMENTO_MAXIMO_PX = 32.0

# Angulo de referencia da pose de aviso. NAO e botao de tuning e nao e o valor que
# a animacao vai usar: e o angulo com que este gerador PROVA que a geometria
# aguenta se erguer. Se a lane de animacao quiser mais que isto, este numero sobe
# junto -- senao a prova passa a cobrir uma pose que o jogo nao faz.
ANGULO_DE_REAR_WARN = 60.0

# Quanto o tronco erguido tem de crescer sobre a altura parada para o aviso ser um
# GESTO. Abaixo disso o bicho se levanta e a silhueta quase nao muda: quem esta a
# 12 blocos nao ve diferenca nenhuma, e o aviso deixa de avisar.
GANHO_MINIMO_DO_REAR_WARN = 1.25

# Arqueamento do peito sobre o barril, em torno da cintura. Existe para a pose nao
# ficar de tabua: o tronco erguido dobra um pouco na lombar.
ARQUEAMENTO_DO_PEITO = 15.0

# Quanto de peito tem de continuar ENTERRADO no barril depois do arqueamento. Nao
# e "dentro ou fora": uma quina a 0,1 px da parede ja esta em fenda no proximo
# grau, e fenda entre peito e lombo nao da erro nenhum -- da um vinco que aparece
# so no quadro do aviso e que ninguem consegue localizar depois.
ENCAIXE_MINIMO_DO_PEITO = 1.0

Osso = namedtuple("Osso", "nome pai pivot rotacao")

# Hierarquia CONGELADA pelo contrato da entrega -- ela e a da secao 5 da diretriz
# de mobs customizados, nao escolha deste gerador. As animacoes e o GeoModel
# escrevem contra estes nomes. Nome errado aqui nao da erro: o GeckoLib so deixa o
# osso parado, e isso so aparece na tela.
#
# O pivot de um filho mora na JUNCAO com o pai, e `valida_pivots` cobra isso
# MEDINDO: todo pivot cai dentro de um cubo do pai. Os que mais importam:
#   * `body` gira no QUADRIL (y=11, z=8) -- e e ele que `rear_warn` usa;
#   * `leg_back_*` giram NO MESMO PONTO, para poderem ser contra-girados e ficarem
#     plantados enquanto o tronco sobe;
#   * `chest` gira na CINTURA (y=14, z=5), a lombar;
#   * `head` gira na nuca e `jaw` na articulacao, atras do focinho.
#
# NENHUM osso nasce com `rotation`. E decisao, nao esquecimento: toda a pose de
# repouso esta nas CAIXAS. Com bind rotation, cada valor escrito no .animation.json
# seria somado a um angulo invisivel aqui -- duas fontes para a mesma pose,
# decididas em silencio.
OSSOS = (
    Osso("root", None, (0, 0, 0), None),
    # O QUADRIL. Ver valida_quadril: metade de tras e metade de baixo do barril.
    Osso("body", "root", (0, 11, 10), None),
    Osso("chest", "body", (0, 14, 4), None),        # a cintura / lombar
    Osso("neck", "chest", (0, 15, -4), None),       # base do pescoco, no peito
    Osso("head", "neck", (0, 13, -6), None),        # a nuca
    Osso("jaw", "head", (0, 9, -8), None),          # a articulacao da mandibula
    Osso("ear_left", "head", (2, 15, -6), None),    # a raiz da orelha, no cranio
    Osso("ear_right", "head", (-2, 15, -6), None),
    Osso("leg_front_left", "body", (4, 11, 0), None),
    Osso("paw_front_left", "leg_front_left", (4, 2, 0), None),
    Osso("leg_front_right", "body", (-4, 11, 0), None),
    Osso("paw_front_right", "leg_front_right", (-4, 2, 0), None),
    # As traseiras giram NO QUADRIL, junto com o tronco. Ver valida_quadril.
    Osso("leg_back_left", "body", (4, 11, 10), None),
    Osso("paw_back_left", "leg_back_left", (4, 2, 11), None),
    Osso("leg_back_right", "body", (-4, 11, 10), None),
    Osso("paw_back_right", "leg_back_right", (-4, 2, 11), None),
    Osso("tail", "body", (0, 14, 14), None),        # a raiz da cauda, na garupa
)

Caixa = namedtuple("Caixa", "nome osso u v x y z w h d")

# (x,y,z) e o canto MINIMO do cubo; (u,v) e o canto do layout de caixa no atlas e
# NAO e escrito a mao: `_empacotar` calcula. Uma caixa w x h x d ocupa
# (2d+2w) x (d+h) px a partir de (u,v).
#
# POR QUE O ATLAS E CALCULADO, e os quatro mobs irmaos escreveram (u,v) a mao:
# este bicho tem 21 caixas em 72% da folha, e nesse aperto o layout deixa de ser
# arrumacao e vira quebra-cabeca. Cada mudanca de tamanho de um osso obrigaria a
# reposicionar os vizinhos na mao, e o erro que isso produz -- duas caixas
# dividindo o mesmo pedaco -- nao da erro: uma pinta por cima da outra e o
# resultado le como erro de arte. `_empacotar` e deterministico (ordem fixa por
# altura, largura e nome) e `valida_uv` continua conferindo o que ele produziu:
# a regua nao foi trocada por confianca no arrumador.
#
# Todo TAMANHO e inteiro de proposito, mesmo com origem em meio pixel. O portao
# Java arredonda `size` para calcular o retangulo do atlas; um size fracionario
# faria o portao medir um retangulo e este gerador medir outro, e as duas contas
# so discordariam no dia em que uma delas achasse sobreposicao.
CAIXAS_CRUAS = (
    # --- o tronco: e aqui que mora o urso ------------------------------------
    # O BARRIL vai do ombro a garupa e e o osso `body`: e dele que TODAS as quatro
    # pernas e a cauda pendem, entao ele precisa existir sob as dianteiras tambem
    # -- senao o pivot do ombro cairia fora de todo cubo do pai. 19 px de
    # comprimento contra 10 de altura: torso de quadrupede e COMPRIDO, e foi
    # encurta-lo para caber na hitbox que produziu, na primeira tentativa deste
    # arquivo, um cubo com pernas.
    Caixa("body", "body", 0, 0, -6, 8, -5, 12, 10, 19),
    # A CERNELHA. O cupim de ombro do urso: 3 px MAIS ALTA que o dorso e 2 px mais
    # larga que o barril. E a peca mais alta do bicho inteiro, e e ela que faz o
    # perfil descer do ombro para a garupa em vez de ser uma tabua.
    Caixa("chest", "chest", 0, 0, -7, 10, -6, 14, 11, 10),

    # --- o pescoco: grosso, de urso, e nao fino de raposa --------------------
    Caixa("neck", "neck", 0, 0, -4, 11, -8, 8, 6, 6),

    # --- a cabeca: e aqui que mora a raposa ----------------------------------
    # O cranio e MAIS ESTREITO que o pescoco (7 contra 8), e e carregado BAIXO:
    # topo em 15 contra 21 da cernelha. Cabeca na altura do ombro e de cavalo;
    # abaixo dele e de urso.
    Caixa("head", "head", 0, 0, -3.5, 8, -11, 7, 7, 6),
    # O FOCINHO. 3 px de largura contra 7 do cranio, e mais comprido que largo:
    # focinho de urso e um bloco na frente da cara, este e uma cunha.
    Caixa("muzzle", "head", 0, 0, -1.5, 8, -15, 3, 3, 4),
    # A MANDIBULA, fina, recuada 1 px atras do nariz.
    Caixa("jaw", "jaw", 0, 0, -2, 6, -14, 4, 2, 7),
    # AS ORELHAS. Mais altas que largas e que fundas: eretas. Elas sobem ate 20 px,
    # 1 px abaixo da cernelha -- com a cabeca carregada baixa, sao as orelhas que
    # alcancam a linha do ombro, e e isso que faz a cara ser vista de longe.
    # Elas tem 2 px de profundidade, e nao 1: orelha de 1 px some DE PERFIL, que e
    # de onde o jogador ve este bicho passar. O desenho de perfil e o unico lugar
    # onde isso aparece -- na folha e nas medidas, uma orelha de 1 px passa.
    Caixa("ear_left", "ear_left", 0, 0, 0.5, 15, -7, 3, 5, 2),
    Caixa("ear_right", "ear_right", 0, 0, -3.5, 15, -7, 3, 5, 2),

    # --- a cauda: densa, e em DUAS pecas que afinam --------------------------
    # A raiz e grossa (5 px) e sai da garupa; a escova cai abaixo dela, mais
    # estreita e mais funda. Em uma peca so a cauda fica um retangulo pendurado --
    # nenhuma medida acusa, e de perfil le como sacola, nao como cauda de raposa.
    Caixa("tail", "tail", 0, 0, -2.5, 9, 12, 5, 5, 5),
    Caixa("tail_brush", "tail", 0, 0, -2, 4, 13, 4, 6, 3),

    # --- pernas: colunas grossas, sem joelho aparente -------------------------
    Caixa("leg_front_left", "leg_front_left", 0, 0, 1.5, 2, -2, 5, 9, 4),
    Caixa("leg_front_right", "leg_front_right", 0, 0, -6.5, 2, -2, 5, 9, 4),
    Caixa("leg_back_left", "leg_back_left", 0, 0, 1.5, 2, 9, 5, 9, 4),
    Caixa("leg_back_right", "leg_back_right", 0, 0, -6.5, 2, 9, 5, 9, 4),

    # --- patas: MAIS LARGAS que a perna, plantigradas -------------------------
    # E o que separa pata de urso de pata de canideo. A traseira e mais comprida
    # que a dianteira -- urso pisa com o calcanhar no chao, e e desse pe comprido
    # que sai a estabilidade para ele se erguer.
    Caixa("paw_front_left", "paw_front_left", 0, 0, 1, 0, -3, 6, 2, 5),
    Caixa("paw_front_right", "paw_front_right", 0, 0, -7, 0, -3, 6, 2, 5),
    Caixa("paw_back_left", "paw_back_left", 0, 0, 1, 0, 7, 6, 2, 6),
    Caixa("paw_back_right", "paw_back_right", 0, 0, -7, 0, 7, 6, 2, 6),

    # --- garras: projetam A FRENTE de cada pata -------------------------------
    # Uma caixa so por pata, e a TEXTURA a divide em tres unhas. E quando o bicho
    # se ergue -- com as dianteiras penduradas na altura do peito -- que elas
    # viram a leitura: a 60 graus elas param a 19 px do chao, na frente do babador.
    Caixa("claw_front_left", "paw_front_left", 0, 0, 1.5, 0, -5, 5, 1, 2),
    Caixa("claw_front_right", "paw_front_right", 0, 0, -6.5, 0, -5, 5, 1, 2),
    Caixa("claw_back_left", "paw_back_left", 0, 0, 1.5, 0, 5, 5, 1, 2),
    Caixa("claw_back_right", "paw_back_right", 0, 0, -6.5, 0, 5, 5, 1, 2),
)


def _empacotar(cruas):
    """Distribui as caixas pelo atlas em prateleiras, de forma DETERMINISTICA.

    Ordem fixa: mais alta primeiro, mais larga em seguida, nome para desempatar.
    Sem a ordem fixa, o mesmo modelo geraria atlas diferentes entre execucoes e o
    .geo.json versionado mudaria sozinho a cada regeracao -- ruido de diff que
    esconde a mudanca de verdade.
    """
    def retangulo(c):
        return (2 * c.d + 2 * c.w, c.d + c.h)

    pendentes = sorted(cruas, key=lambda c: (-retangulo(c)[1], -retangulo(c)[0], c.nome))
    postas = []
    y = 0
    while pendentes:
        altura_da_prateleira = retangulo(pendentes[0])[1]
        x = 0
        i = 0
        while i < len(pendentes):
            largura, alta = retangulo(pendentes[i])
            if largura <= UV_LARGURA - x and alta <= altura_da_prateleira:
                postas.append(pendentes.pop(i)._replace(u=x, v=y))
                x += largura
            else:
                i += 1
        y += altura_da_prateleira
    # A ordem de CAIXAS volta a ser a da tabela escrita a mao: ela e a ordem em que
    # os cubos saem no JSON e em que o gerador da textura le, e a do empacotador e
    # so um detalhe de arrumacao.
    por_nome = {c.nome: c for c in postas}
    return tuple(por_nome[c.nome] for c in cruas)


CAIXAS = _empacotar(CAIXAS_CRUAS)

PERNAS = ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right")
PERNAS_TRASEIRAS = ("leg_back_left", "leg_back_right")
PATAS = ("paw_front_left", "paw_front_right", "paw_back_left", "paw_back_right")
GARRAS = ("claw_front_left", "claw_front_right", "claw_back_left", "claw_back_right")


# ------------------------------------------------------------------ utilidades

def caixa(nome):
    return [c for c in CAIXAS if c.nome == nome][0]


def osso(nome):
    return [o for o in OSSOS if o.nome == nome][0]


def area_no_atlas(c):
    """Retangulo (x0, y0, x1, y1) que a caixa ocupa no atlas."""
    return (c.u, c.v, c.u + 2 * c.d + 2 * c.w, c.v + c.d + c.h)


def volume(c):
    """Caixa envolvente do cubo: ((x0,x1),(y0,y1),(z0,z1))."""
    return ((c.x, c.x + c.w), (c.y, c.y + c.h), (c.z, c.z + c.d))


def limites(caixas=None):
    # `caixas=CAIXAS` como default seria avaliado UMA vez, na definicao: a funcao
    # passaria a medir para sempre a tabela do momento do import. Isso nao da erro
    # -- da uma regua que aprova qualquer modelo, porque ela nunca ve a mudanca.
    caixas = CAIXAS if caixas is None else caixas
    xs = [c.x for c in caixas] + [c.x + c.w for c in caixas]
    ys = [c.y for c in caixas] + [c.y + c.h for c in caixas]
    zs = [c.z for c in caixas] + [c.z + c.d for c in caixas]
    return (min(xs), max(xs)), (min(ys), max(ys)), (min(zs), max(zs))


def descendentes(raiz):
    """Nomes dos ossos do galho que comeca em `raiz`, ela inclusa."""
    galho = {raiz}
    mudou = True
    while mudou:
        mudou = False
        for o in OSSOS:
            if o.pai in galho and o.nome not in galho:
                galho.add(o.nome)
                mudou = True
    return galho


def _girar_yz(y, z, pivot, graus):
    """Gira (y,z) em torno de (pivot_y, pivot_z). Angulo POSITIVO levanta a FRENTE.

    A frente e -Z: um ponto com z abaixo do pivot sobe quando o angulo cresce.
    """
    a = math.radians(graus)
    py, pz = pivot
    dy, dz = y - py, z - pz
    return (py + dy * math.cos(a) - dz * math.sin(a),
            pz + dy * math.sin(a) + dz * math.cos(a))


def quinas_yz(c):
    """As quatro quinas do cubo no plano y-z -- o plano em que a pose de aviso gira."""
    (_, _), (y0, y1), (z0, z1) = volume(c)
    return ((y0, z0), (y0, z1), (y1, z0), (y1, z1))


def encaixe_no_yz(ponto, c):
    """Quanto o ponto (y,z) esta ENTERRADO no retangulo y-z do cubo, em px.

    Positivo = dentro, e o valor e a distancia ate a parede mais proxima.
    Negativo = fora. Medir a profundidade, e nao so 'dentro ou fora', e o que faz
    a validacao morder ANTES de a quina sair: uma quina a 0,1 px da parede ja e
    uma fenda que abre no proximo grau de animacao.
    """
    y, z = ponto
    (_, _), (y0, y1), (z0, z1) = volume(c)
    return min(y - y0, y1 - y, z - z0, z1 - z)


# ------------------------------------------------------------------ validacoes

def valida_ossos():
    nomes = [o.nome for o in OSSOS]
    if len(set(nomes)) != len(nomes):
        raise ValueError("osso repetido")
    raizes = [o for o in OSSOS if o.pai is None]
    if len(raizes) != 1 or raizes[0].nome != "root":
        raise ValueError("a unica raiz tem de ser 'root'")
    for o in OSSOS:
        if o.pai is not None and o.pai not in nomes:
            raise ValueError("osso '%s' aponta para pai inexistente '%s'" % (o.nome, o.pai))
    for c in CAIXAS:
        if c.osso not in nomes:
            raise ValueError("caixa '%s' pendurada em osso inexistente '%s'" % (c.nome, c.osso))
    # Osso sem cubo e osso que a animacao move e ninguem ve. Aqui TODOS tem volume
    # menos a raiz, que existe so para o bicho inteiro poder subir e descer.
    sem_cubo = [o.nome for o in OSSOS
                if o.nome != "root" and not any(c.osso == o.nome for c in CAIXAS)]
    if sem_cubo:
        raise ValueError("ossos sem volume proprio: %s -- a animacao os move e nada aparece"
                         % sem_cubo)
    # A hierarquia da secao 5 da diretriz, cobrada nome a nome: ela nao e escolha
    # deste gerador, e trocar um pai aqui nao da erro -- da um membro que gira em
    # volta do bicho errado.
    esperado = {
        "body": "root", "chest": "body", "neck": "chest", "head": "neck",
        "jaw": "head", "ear_left": "head", "ear_right": "head", "tail": "body",
    }
    for perna in PERNAS:
        esperado[perna] = "body"
        esperado[perna.replace("leg", "paw")] = perna
    for nome, pai in esperado.items():
        if osso(nome).pai != pai:
            raise ValueError("'%s' devia pendurar em '%s' e pendura em '%s': a hierarquia e a da "
                             "secao 5 da diretriz, nao escolha do gerador"
                             % (nome, pai, osso(nome).pai))


def valida_pivots():
    """Pivot de filho mora na JUNCAO com o pai -- e junta se mede.

    Pivot fora do volume do pai nao da erro: da um membro que gira em torno de um
    ponto que nao existe no bicho, e o sintoma e um clipe 'meio quebrado' que
    ninguem sabe explicar.
    """
    for o in OSSOS:
        if o.pai is None or o.pai == "root":
            continue
        cubos_do_pai = [c for c in CAIXAS if c.osso == o.pai]
        if not cubos_do_pai:
            continue
        px, py, pz = o.pivot
        dentro = any(vx0 <= px <= vx1 and vy0 <= py <= vy1 and vz0 <= pz <= vz1
                     for (vx0, vx1), (vy0, vy1), (vz0, vz1)
                     in (volume(c) for c in cubos_do_pai))
        if not dentro:
            raise ValueError("o pivot de '%s' %s cai fora de todo cubo do pai '%s': o osso giraria "
                             "em torno de um ponto que nao existe no bicho"
                             % (o.nome, o.pivot, o.pai))


def valida_uv():
    """Sobreposicao de UV nao da erro: da textura errada na face. Entao reprova aqui."""
    nomes = [c.nome for c in CAIXAS]
    if len(set(nomes)) != len(nomes):
        raise ValueError("caixa com nome repetido -- o gerador da textura indexa por nome")
    for c in CAIXAS:
        if int(c.w) != c.w or int(c.h) != c.h or int(c.d) != c.d:
            raise ValueError("a caixa '%s' tem tamanho fracionario %s: o portao Java arredonda o "
                             "size para medir o atlas e passaria a medir um retangulo diferente "
                             "deste gerador" % (c.nome, (c.w, c.h, c.d)))
        x0, y0, x1, y1 = area_no_atlas(c)
        if x1 > UV_LARGURA or y1 > UV_ALTURA:
            raise ValueError("caixa '%s' estoura o atlas: vai ate (%d,%d)" % (c.nome, x1, y1))
    for i, a in enumerate(CAIXAS):
        ax0, ay0, ax1, ay1 = area_no_atlas(a)
        for b in CAIXAS[i + 1:]:
            bx0, by0, bx1, by1 = area_no_atlas(b)
            if ax0 < bx1 and bx0 < ax1 and ay0 < by1 and by0 < ay1:
                raise ValueError("caixas '%s' e '%s' se sobrepoem no atlas" % (a.nome, b.nome))


def valida_faces_coplanares():
    """Duas caixas com a MESMA face no MESMO plano cintilam em jogo.

    Nao e erro de JSON, nao aparece no log, e o jogador le como bug de driver. E o
    unico defeito de modelo que nenhum portao do repositorio enxerga.

    Face OPOSTA no mesmo plano (uma caixa encostando na outra) e permitida: e
    assim que todo modelo do jogo e montado, e as normais contrarias resolvem a
    disputa sozinhas. O que se reprova aqui e MESMA direcao com area em comum.
    """
    eixos = (("x", 0), ("y", 1), ("z", 2))
    for eixo, indice in eixos:
        outros = [i for _, i in eixos if i != indice]
        for lado in (0, 1):  # 0 = face minima, 1 = face maxima
            faces = []
            for c in CAIXAS:
                v = volume(c)
                faces.append((c.nome, v[indice][lado], v[outros[0]], v[outros[1]]))
            for i, (nome_a, plano_a, ra0, ra1) in enumerate(faces):
                for nome_b, plano_b, rb0, rb1 in faces[i + 1:]:
                    if plano_a != plano_b:
                        continue
                    if min(ra0[1], rb0[1]) - max(ra0[0], rb0[0]) <= 0:
                        continue
                    if min(ra1[1], rb1[1]) - max(ra1[0], rb1[0]) <= 0:
                        continue
                    raise ValueError(
                        "'%s' e '%s' tem a mesma face %s=%s virada para o mesmo lado, com area em "
                        "comum: em jogo isso cintila, e o jogador le como bug de video"
                        % (nome_a, nome_b, eixo, plano_a))


def valida_hitbox():
    """O bicho PARADO cabe na hitbox. A pose de aviso e o unico que pode sobrar."""
    (x0, x1), (y0, y1), (z0, z1) = limites()
    if y0 != 0:
        raise ValueError("o piso das garras tem de ficar em y=0, esta em %s" % y0)
    if y1 > HITBOX_ALTURA_PX:
        raise ValueError("modelo parado mais alto (%s px) que a hitbox (%s px)"
                         % (y1, HITBOX_ALTURA_PX))
    if (x1 - x0) > HITBOX_LARGURA_PX:
        raise ValueError("modelo parado com %s px de largura numa hitbox de %s px: a silhueta "
                         "passa a prometer um alcance que o servidor nao tem"
                         % (x1 - x0, HITBOX_LARGURA_PX))
    # O COMPRIMENTO tem teto proprio, e nao o da hitbox. Ver o cabecalho: a
    # geometria do urso polar que sai daqui tem ~31 px nesta mesma hitbox, e um
    # torso espremido em 22 px sobre um ombro de 21 vira um cubo com pernas.
    if (z1 - z0) > COMPRIMENTO_MAXIMO_PX:
        raise ValueError("modelo parado com %s px de comprimento contra o teto de %s px: passou "
                         "de bicho comprido para trem" % (z1 - z0, COMPRIMENTO_MAXIMO_PX))
    barril = caixa("body")
    if barril.d <= barril.h:
        raise ValueError("o barril tem %s px de comprimento contra %s de altura: torso de "
                         "quadrupede e COMPRIDO, e um torso quadrado le como caixote com pernas -- "
                         "nenhum portao do repositorio ve isso, so o perfil desenhado"
                         % (barril.d, barril.h))


def valida_urso():
    """O VOLUME tem de ser de urso. Sem isto sobra uma raposa grande."""
    barril, cernelha, pescoco, cranio = (caixa("body"), caixa("chest"),
                                         caixa("neck"), caixa("head"))
    if cernelha.y + cernelha.h <= barril.y + barril.h:
        raise ValueError("a cernelha termina em y=%s e o dorso em y=%s: sem o cupim de ombro o "
                         "perfil vira uma tabua e o bicho perde o urso"
                         % (cernelha.y + cernelha.h, barril.y + barril.h))
    if cernelha.w <= barril.w or cernelha.w <= cranio.w:
        raise ValueError("a cernelha tem %s px de largura contra %s do barril e %s do cranio: ela "
                         "tem de ser a peca mais larga, e e esse ombro que carrega o peso"
                         % (cernelha.w, barril.w, cranio.w))
    if pescoco.w <= cranio.w:
        raise ValueError("o pescoco (%s px) nao e mais grosso que a cabeca (%s px): pescoco fino e "
                         "de canideo, e o corpo deste bicho e de urso"
                         % (pescoco.w, cranio.w))
    if barril.y < 8:
        raise ValueError("a barriga fica a %s px do chao: baixo assim o bicho le como rasteiro, e "
                         "nao como predador pesado de pernas altas" % barril.y)
    for nome in PATAS:
        pata = caixa(nome)
        perna = caixa(nome.replace("paw", "leg"))
        if pata.w <= perna.w or pata.d <= perna.d:
            raise ValueError("a pata '%s' (%s x %s) nao e mais larga que a perna (%s x %s): sem "
                             "isso ela vira o fim de um palito e a pata de urso some"
                             % (nome, pata.w, pata.d, perna.w, perna.d))
    for nome in GARRAS:
        garra = caixa(nome)
        pata = caixa([p for p in PATAS if nome.replace("claw", "paw") == p][0])
        if garra.z >= pata.z:
            raise ValueError("a garra '%s' comeca em z=%s e a pata em z=%s: garra que nao projeta "
                             "a frente da pata e uma palavra no contrato, nao um volume na tela"
                             % (nome, garra.z, pata.z))
        if garra.y != 0:
            raise ValueError("a garra '%s' nao encosta no chao (y=%s): ela tem de ser o que toca o "
                             "solo, ou o contorno da pata some de perfil" % (nome, garra.y))


def valida_raposa():
    """A CABECA tem de ser de raposa. Sem isto sobra um urso de orelha pontuda."""
    cranio, focinho = caixa("head"), caixa("muzzle")
    cernelha = caixa("chest")
    if focinho.z >= cranio.z:
        raise ValueError("o focinho (frente z=%s) nao projeta alem do cranio (z=%s)"
                         % (focinho.z, cranio.z))
    if focinho.d <= focinho.w or focinho.d <= focinho.h:
        raise ValueError("o focinho tem %s de comprimento contra %s de largura e %s de altura: "
                         "focinho que nao e mais comprido que grosso e focinho de urso"
                         % (focinho.d, focinho.w, focinho.h))
    if focinho.w > cranio.w / 2.0:
        raise ValueError("o focinho tem %s px de largura contra %s do cranio (teto: metade): mais "
                         "grosso que isso ele vira cara de urso com o nariz esticado"
                         % (focinho.w, cranio.w))
    if cranio.w > 0.6 * cernelha.w:
        raise ValueError("o cranio tem %s px contra %s da cernelha: sem o degrau entre cabeca fina "
                         "e corpo macico o hibrido nao incomoda, e o desconforto E o bicho"
                         % (cranio.w, cernelha.w))
    for nome in ("ear_left", "ear_right"):
        orelha = caixa(nome)
        if orelha.h <= orelha.w or orelha.h <= orelha.d:
            raise ValueError("a orelha '%s' (%s x %s x %s) nao e mais alta que larga: orelha de "
                             "raposa e ERETA, e deitada ela vira orelha de urso"
                             % (nome, orelha.w, orelha.h, orelha.d))
        if orelha.h < cranio.h / 2.0:
            raise ValueError("a orelha '%s' tem %s px contra %s de cranio: pequena assim ela some "
                             "a 12 blocos, que e a distancia em que o jogador ainda pode escolher "
                             "dar meia-volta" % (nome, orelha.h, cranio.h))
        if orelha.y < cranio.y + cranio.h:
            raise ValueError("a orelha '%s' comeca em y=%s, abaixo do topo do cranio (y=%s): ela "
                             "nasceria enterrada na cabeca"
                             % (nome, orelha.y, cranio.y + cranio.h))
    raiz, escova, barril = caixa("tail"), caixa("tail_brush"), caixa("body")
    if raiz.z + raiz.d <= barril.z + barril.d:
        raise ValueError("a cauda acaba em z=%s, dentro da garupa (z=%s): ela e um osso que a "
                         "animacao move e ninguem ve"
                         % (raiz.z + raiz.d, barril.z + barril.d))
    if escova.y >= barril.y:
        raise ValueError("a escova da cauda comeca em y=%s e a barriga em y=%s: cauda que nao "
                         "desce abaixo da linha do ventre le como coto, e a desta especie e densa"
                         % (escova.y, barril.y))
    if raiz.w < 4 or raiz.h + escova.h < 8:
        raise ValueError("a cauda tem %s px de largura e %s de altura somada: fina assim ela e um "
                         "rabo, nao uma cauda densa" % (raiz.w, raiz.h + escova.h))
    if escova.w >= raiz.w or escova.d >= raiz.d:
        raise ValueError("a escova (%s x %s) nao afina em relacao a raiz (%s x %s): sem o degrau a "
                         "cauda vira um retangulo pendurado na garupa"
                         % (escova.w, escova.d, raiz.w, raiz.d))


def valida_quadril():
    """O pivot de `body` tem de ser um QUADRIL, e as traseiras tem de girar nele.

    Se `leg_back_*` girar num ponto diferente do tronco, contra-gira-las para
    mante-las plantadas NAO devolve a perna ao lugar: a pose de aviso sai com o
    bicho flutuando ou com as patas dentro do chao. Nada disso levanta excecao.
    """
    barril = caixa("body")
    _, (y0, y1), (z0, z1) = volume(barril)
    _, py, pz = osso("body").pivot
    if pz < (z0 + z1) / 2.0:
        raise ValueError("o pivot de 'body' esta em z=%s, na METADE DA FRENTE do barril (%s..%s): "
                         "isso nao e um quadril, e um eixo no meio do bicho -- erguido, ele gira "
                         "em torno da barriga e a garupa vai para trapicar no chao"
                         % (pz, z0, z1))
    if py > (y0 + y1) / 2.0:
        raise ValueError("o pivot de 'body' esta em y=%s, na metade de CIMA do barril (%s..%s): o "
                         "quadril fica na altura da junta da perna, nao na do lombo" % (py, y0, y1))
    for nome in PERNAS_TRASEIRAS:
        _, ly, lz = osso(nome).pivot
        if (ly, lz) != (py, pz):
            raise ValueError("'%s' gira em (y=%s, z=%s) e o tronco em (y=%s, z=%s): contra-girar a "
                             "traseira para ela ficar plantada nao devolve a perna ao lugar, e a "
                             "pose de aviso sai flutuando" % (nome, ly, lz, py, pz))


def valida_pose_rear_warn():
    """Ergue o tronco e confere que a pose CABE no bicho.

    O modelo nao guarda esta pose -- ela mora no .animation.json. O que se prova
    aqui e que a geometria AGUENTA: nada afunda no chao, e a silhueta cresce o
    bastante para o aviso ser visto de longe.
    """
    pivot = osso("body").pivot[1], osso("body").pivot[2]
    # As traseiras sao contra-giradas pela animacao para ficarem plantadas: elas
    # terminam onde ja estavam, entao nao entram na conta.
    plantadas = descendentes("leg_back_left") | descendentes("leg_back_right")
    girados = [c for c in CAIXAS if c.osso not in plantadas]

    piso = None
    topo = None
    for c in girados:
        for y, z in quinas_yz(c):
            ny, _ = _girar_yz(y, z, pivot, ANGULO_DE_REAR_WARN)
            piso = ny if piso is None else min(piso, ny)
            topo = ny if topo is None else max(topo, ny)

    if piso < 0:
        raise ValueError("erguido %s graus, o tronco desce ate y=%.2f: a garupa atravessa o chao, "
                         "e em jogo isso e o bicho enterrado ate a cintura sem um erro no log"
                         % (ANGULO_DE_REAR_WARN, piso))

    _, (_, altura_parado), _ = limites()
    if topo < GANHO_MINIMO_DO_REAR_WARN * altura_parado:
        raise ValueError("erguido, o bicho chega a %.1f px contra %.1f px parado (%.2fx, minimo "
                         "%.2fx): a silhueta mal muda, e quem esta a 12 blocos nao ve aviso nenhum"
                         % (topo, altura_parado, topo / altura_parado,
                            GANHO_MINIMO_DO_REAR_WARN))


def valida_peito_nao_atravessa_o_quadril():
    """Quina do peito ENTERRADA no barril continua enterrada depois do arqueamento.

    Este e o defeito que a diretriz nomeia. Enterrar e normal -- todo modelo do
    jogo tem peca dentro de peca. O que quebra e a quina enterrada SAIR: dai o
    peito fura o proprio lombo ou a propria barriga, e o clipe parece um bug de
    modelo que ninguem consegue localizar.
    """
    barril, peito = caixa("body"), caixa("chest")
    pivot = osso("chest").pivot[1], osso("chest").pivot[2]
    enterradas = [q for q in quinas_yz(peito) if encaixe_no_yz(q, barril) > 0]
    if not enterradas:
        raise ValueError("nenhuma quina do peito esta dentro do barril: as duas pecas so se "
                         "encostam, e no primeiro grau de arqueamento vai abrir fenda entre elas")
    for graus in (ARQUEAMENTO_DO_PEITO, -ARQUEAMENTO_DO_PEITO):
        for y, z in enterradas:
            destino = _girar_yz(y, z, pivot, graus)
            encaixe = encaixe_no_yz(destino, barril)
            if encaixe < ENCAIXE_MINIMO_DO_PEITO:
                raise ValueError("arqueando o peito %s graus, a quina (y=%s, z=%s) para em "
                                 "(y=%.2f, z=%.2f), a %.2f px da parede do barril (minimo %.1f): o "
                                 "peito esta saindo pelo lombo -- e o que a pose de aviso nao pode "
                                 "fazer. Quase sempre isso quer dizer que a CINTURA esta no lugar "
                                 "errado, e nao que o peito esta grande"
                                 % (graus, y, z, destino[0], destino[1], encaixe,
                                    ENCAIXE_MINIMO_DO_PEITO))


# ------------------------------------------------------------------ geracao

def _osso_json(o):
    d = {"name": o.nome}
    if o.pai is not None:
        d["parent"] = o.pai
    d["pivot"] = list(o.pivot)
    if o.rotacao is not None:
        d["rotation"] = list(o.rotacao)
    cubos = [{"origin": [c.x, c.y, c.z], "size": [c.w, c.h, c.d], "uv": [c.u, c.v]}
             for c in CAIXAS if c.osso == o.nome]
    if cubos:
        d["cubes"] = cubos
    return d


def geometria():
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                # O portao generico confere ESTA string contra o nome do arquivo:
                # errada, o modelo nao e achado e o mob some da tela sem log.
                "identifier": IDENTIFICADOR,
                "texture_width": UV_LARGURA,
                "texture_height": UV_ALTURA,
                # A caixa de visibilidade acompanha o bicho ERGUIDO, e nao o
                # parado: apertada demais, ele sumiria da tela no quadro exato do
                # aviso -- o unico quadro em que ele precisa ser visto.
                "visible_bounds_width": 3,
                "visible_bounds_height": 2.5,
                "visible_bounds_offset": [0, 1.25, 0],
            },
            "bones": [_osso_json(o) for o in OSSOS],
        }],
    }


def _achatar_numeros(texto):
    """Poe os vetores numericos numa linha so -- o JSON e gerado, mas e lido em review."""
    return re.sub(r"\[\s+([-\d.,\se]+?)\s*\]",
                  lambda m: "[" + ", ".join(m.group(1).replace(",", " ").split()) + "]",
                  texto)


def main():
    valida_ossos()
    valida_pivots()
    valida_uv()
    valida_faces_coplanares()
    valida_hitbox()
    valida_urso()
    valida_raposa()
    valida_quadril()
    valida_pose_rear_warn()
    valida_peito_nao_atravessa_o_quadril()

    texto = _achatar_numeros(json.dumps(geometria(), indent=2))
    json.loads(texto)  # o JSON gerado tem de continuar valido depois do achatamento

    destino = os.path.join("src", "main", "resources", "assets", "nenfoundation",
                           "geo", "entity", "foxbear.geo.json")
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    with open(destino, "w", encoding="utf-8", newline="\n") as f:
        f.write(texto + "\n")

    (x0, x1), (y0, y1), (z0, z1) = limites()
    ocupado = sum((2 * c.d + 2 * c.w) * (c.d + c.h) for c in CAIXAS)

    pivot = osso("body").pivot[1], osso("body").pivot[2]
    plantadas = descendentes("leg_back_left") | descendentes("leg_back_right")
    erguido = [_girar_yz(y, z, pivot, ANGULO_DE_REAR_WARN)
               for c in CAIXAS if c.osso not in plantadas for y, z in quinas_yz(c)]
    topo = max(p[0] for p in erguido)
    piso = min(p[0] for p in erguido)

    print("escrito", destino)
    print("ossos: %d   caixas: %d" % (len(OSSOS), len(CAIXAS)))
    print("caixa envolvente: x %s..%s  y %s..%s  z %s..%s (px)" % (x0, x1, y0, y1, z0, z1))
    print("parado: %s x %s x %s px (largura x altura x comprimento)  hitbox %.1f x %.1f"
          % (x1 - x0, y1, z1 - z0, HITBOX_LARGURA_PX, HITBOX_ALTURA_PX))
    print("cernelha em y=%s, dorso em y=%s, cabeca ate y=%s, orelhas ate y=%s"
          % (caixa("chest").y + caixa("chest").h, caixa("body").y + caixa("body").h,
             caixa("head").y + caixa("head").h, caixa("ear_left").y + caixa("ear_left").h))
    print("cranio %s px contra cernelha %s px (%.2fx) -- o degrau que faz o hibrido"
          % (caixa("head").w, caixa("chest").w,
             float(caixa("head").w) / caixa("chest").w))
    print("rear_warn a %.0f graus no quadril (y=%s, z=%s): topo %.1f px (%.2f bloco, %.2fx o "
          "parado), piso %.1f px" % (ANGULO_DE_REAR_WARN, pivot[0], pivot[1], topo, topo / 16.0,
                                     topo / y1, piso))
    print("atlas %dx%d: %d de %d px ocupados (%.0f%%)"
          % (UV_LARGURA, UV_ALTURA, ocupado, UV_LARGURA * UV_ALTURA,
             100.0 * ocupado / (UV_LARGURA * UV_ALTURA)))


if __name__ == "__main__":
    main()
