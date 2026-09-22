"""Animacao Bedrock 1.8.0: keyframes, portoes e o emissor do .animation.json.

O FORMATO E 1.8.0, E NAO 1.12.0 DO GEO. Igualar os dois "por coerencia" faz o
GeckoLib recusar o arquivo, e o sintoma e o mob parado na tela sem log nenhum.

O CONJUNTO DE OSSOS SAI DO GEO, E SO DE LA. Este modulo LE o .geo.json ja escrito
em vez de aceitar uma lista de nomes. Nao e conveniencia: uma segunda lista seria
uma segunda fonte para a mesma verdade, e a divergencia se manifestaria como um
membro que nao se mexe -- sem erro, sem log, sem nada. Pela mesma razao o arquivo
e OBRIGATORIO: um dos sete geradores antigos lia o geo `if os.path.exists(...)`,
e um geo ausente virava verde.

O `loop` MORA NO ARQUIVO, NUNCA NO CODIGO. No GeckoLib 4.8.3 o `RawAnimation`
carrega o LoopType e VENCE o que esta no JSON, entao `.thenLoop(...)` no Java
transforma este dicionario em documentacao -- e documentacao que discorda do
comportamento. O portao Java `oLoopMoraNoArquivoDeAnimacao` reprova os atalhos do
lado de la; aqui, `LOOPS` e a unica declaracao e tambem a lista de clipes.

O QUE OS PORTOES DAQUI COBRAM
------------------------------
  `valida_clipes`          chave fora do contrato, loop invalido, canal invalido,
                           quadro fora de [0, duracao], escala <= 0 (que SOME com
                           o osso em vez de dar erro).
  `valida_ossos`           osso citado que a geometria nao tem, e raiz animada. O
                           GeckoLib ignora nome desconhecido em silencio; a raiz
                           animada arrasta a silhueta para fora da hitbox, que e
                           o pior defeito possivel num mob que ensina distancia.
  `valida_ossos_com_volume` osso animado cuja SUBARVORE inteira nao tem cubo: o
                           clipe roda, o valor muda e a tela nao.
  `valida_loop_fecha`      clipe que repete e nao termina onde comecou. Da um
                           tranco a cada volta, para sempre -- e o defeito que
                           ninguem reporta porque ninguem consegue descrever.
  `valida_duracao_de_ataque` clipe de ataque mais curto que o orcamento de ticks
                           que o SERVIDOR gasta no golpe. Ver abaixo.

POR QUE A DURACAO DE ATAQUE E COBRADA CONTRA O SERVIDOR
--------------------------------------------------------
O servidor gasta windup + active + recovery ticks num ataque, e durante todos
eles o jogador esta decidindo se recua, se bloqueia ou se entra. Se o clipe
acabar antes, o GeckoLib devolve os ossos para a pose neutra (ou segura o ultimo
quadro, dependendo do loop) e o bicho RELAXA no meio do golpe que ainda vai
acertar. Nada acusa: o dano sai certo, o cooldown sai certo, o log fica limpo. O
que quebra e a unica coisa que o jogador tem para ler.

Um ataque pode estar repartido em varios clipes (windup / active / recovery como
tres arquivos de animacao encadeados). Por isso a chave de `ataques` aceita uma
tupla de nomes, e o orcamento e conferido contra a SOMA das duracoes: o que tem
de cobrir os ticks do servidor e o episodio inteiro, nao cada pedaco.

Convencao de eixos: y=0 e o chao, -Z e a FRENTE, +X e o lado ESQUERDO. Disso, e
so disso, sai a tabela de consequencia que cada mob escreve no proprio cabecalho
(rotacao X negativa desce o focinho e sobe a cauda, etc.). Isso e geometria do
arquivo, nao convencao decorada -- e `ossos_do_geo` existe para o mob CONFERIR no
proprio geo em vez de chutar.
"""
from collections import namedtuple
import json
import math
import os

from . import (ErroDeArte, FORMATO_ANIMACAO, TICKS_POR_SEGUNDO,
               caminho_animacao, caminho_geo, escrever_texto)

LOOPS_VALIDOS = (True, False, "hold_on_last_frame")

# O neutro de cada canal. `scale` neutro e UM e os outros dois sao ZERO -- por
# isso `escala` existe separada de `vetor`. Um vetor de escala com zero nao da
# erro: ele SOME com o osso, e o relato que chega e "o braco desapareceu".
CANAIS = ("rotation", "position", "scale")


class Ataque(namedtuple("Ataque", "windup active recovery")):
    """Os ticks que o SERVIDOR gasta num golpe, copiados do Java.

    O mob escreve, ao lado, o metodo de onde cada numero veio -- e o que permite
    a proxima pessoa achar a divergencia quando o balanceamento mexer no servidor
    e esquecer a animacao.
    """
    __slots__ = ()

    @property
    def ticks(self):
        return self.windup + self.active + self.recovery

    @property
    def segundos(self):
        return self.ticks / TICKS_POR_SEGUNDO


# -------------------------------------------------------------- ferramentas

def tempo(t):
    """Chave de keyframe: string, sempre com decimal, sem zero sobrando.

    O formato exige string. Emitir 0 em vez de "0.0" nao da erro de parse -- o
    GeckoLib simplesmente nao acha a chave, e o canal inteiro vira neutro.
    """
    texto = ("%.4f" % round(t, 4)).rstrip("0")
    return texto + "0" if texto.endswith(".") else texto


def num(v):
    """Duas casas, e inteiro quando for inteiro.

    [-1, 0, 0] em vez de [-1.0, 0.0, 0.0]: o arquivo e lido em diff, e float
    cheio de zeros esconde a linha que de fato mudou.
    """
    v = round(v, 2)
    return int(v) if v == int(v) else v


def vetor(x=0.0, y=0.0, z=0.0):
    """Rotacao ou posicao: o neutro e zero."""
    return [num(x), num(y), num(z)]


def escala(x=1.0, y=1.0, z=1.0):
    """Escala: o neutro e UM. Vetor de escala com zero some com o osso."""
    return [num(x), num(y), num(z)]


NEUTRO = {"rotation": vetor, "position": vetor, "scale": escala}


def curva(bones, osso, canal, pares):
    """Acumula keyframes de um canal de um osso.

    Escreve por CHAVE DE TEMPO, entao duas escritas no mesmo instante se
    sobrepoem em silencio -- e e por isso que `ordenar` existe e que a pose-base
    e deitada ANTES das curvas, e nao depois.
    """
    if canal not in CANAIS:
        raise ErroDeArte("canal '%s' nao existe no formato 1.8.0; os canais sao %s"
                         % (canal, list(CANAIS)))
    alvo = bones.setdefault(osso, {}).setdefault(canal, {})
    for t, v in pares:
        alvo[tempo(t)] = list(v)
    return bones


def derivar(bones, origem, destino, canal, fator, eixos=(0,)):
    """Escreve num osso o que ja foi escrito no pai, multiplicado.

    O membro terminal -- casco, pe, garra -- NUNCA e escrito a mao. Escrito a
    mao ele fica com a fase certa hoje e errada na primeira correcao da perna, e
    a correcao nao da erro: da um pe que arrasta meio quadro atras da canela.

    `eixos` existe porque so o eixo do balanco contra-gira; contra-girar o eixo
    que carrega o afastamento do par viraria a garra para dentro da barriga.
    """
    quadros = bones.get(origem, {}).get(canal)
    if not quadros:
        raise ErroDeArte("nao da para derivar '%s' de '%s': o canal %s do pai esta vazio"
                         % (destino, origem, canal))
    pares = []
    for chave, valor in quadros.items():
        novo = [v * fator if i in eixos else 0.0 for i, v in enumerate(valor)]
        pares.append((float(chave), vetor(*novo)))
    return curva(bones, destino, canal, pares)


def cossenoide(duracao, periodo, amplitude, base=0.0, fase=0.0, amostras=8):
    """Cossenoide amostrada, em pares (t, valor).

    Amostrar em vez de escrever os extremos a mao e o que mantem a fase certa
    quando alguem mexe no periodo, e o que faz a interpolacao LINEAR do formato
    1.8.0 parecer curva em vez de zigue-zague.
    """
    passo = periodo / float(amostras)
    n = int(round(duracao / passo))
    return [(i * passo,
             base + amplitude * math.cos(2 * math.pi * (i * passo / periodo + fase)))
            for i in range(n + 1)]


def ordenar(clipe, ordem_dos_ossos=None):
    """Keyframes em ordem de tempo. JSON nao garante ordem; o diff humano exige.

    Sem isto, o mesmo clipe sai com as chaves em ordem diferente conforme a ordem
    em que as curvas foram escritas -- e o diff acusa mudanca onde nao houve.
    """
    for canais in clipe["bones"].values():
        for canal, quadros in list(canais.items()):
            canais[canal] = dict(sorted(quadros.items(), key=lambda kv: float(kv[0])))
    if ordem_dos_ossos is not None:
        indice = {nome: i for i, nome in enumerate(ordem_dos_ossos)}
        clipe["bones"] = dict(sorted(clipe["bones"].items(),
                                     key=lambda kv: indice.get(kv[0], len(indice))))
    return clipe


def valor_em(quadros, t, neutro):
    """O valor que o JOGADOR ve em t, e nao a chave que existe em t.

    Costura e loop se conferem com isto, e nao com a presenca de um keyframe: um
    canal cuja ultima chave esta antes do fim SEGURA aquele valor ate o fim, e
    comparar chaves reprovaria um clipe correto (ou, pior, aprovaria um errado
    por comparar duas ausencias).
    """
    if not quadros:
        return list(neutro)
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


def quadro_em(clipe, t, animaveis):
    """A pose COMPLETA do clipe em t: todo osso animavel, todo canal.

    Completa de proposito. Osso que um clipe nao cita nao fica onde estava: ele
    volta para o DEFAULT DO MODELO. Comparar so os ossos que os dois clipes
    citam aprovaria uma costura em que um membro salta sozinho.
    """
    pose = {}
    for osso in animaveis:
        canais = clipe["bones"].get(osso, {})
        pose[osso] = {c: valor_em(canais.get(c, {}), t, NEUTRO[c]()) for c in CANAIS}
    return pose


# ------------------------------------------------------------- leitura do geo

def carregar_geo(mob, raiz=".", caminho=None):
    """LE o geo ja escrito. Arquivo ausente e recusa, nunca verde.

    Este gerador depende do geo para saber quais ossos existem e onde eles ficam.
    Deixar o geo opcional transforma "o modelo nao foi gerado" em "a animacao
    passou", que e a forma mais barata de falso verde que esta lane consegue
    produzir.
    """
    alvo = caminho or caminho_geo(mob, raiz)
    if not os.path.exists(alvo):
        raise ErroDeArte("%s nao existe. Este gerador LE o geo: os nomes dos ossos, os pivots e as "
                         "medidas de que os clipes dependem saem de la. Rode o gerador de "
                         "geometria antes -- sem geo nao ha o que animar." % alvo)
    with open(alvo, encoding="utf-8") as arquivo:
        return json.load(arquivo)["minecraft:geometry"][0]


def ossos_do_geo(geometria):
    """Nomes na ordem em que o geo os declara -- e essa ordem manda no arquivo."""
    return tuple(b["name"] for b in geometria["bones"])


def osso_do_geo(geometria, nome):
    for b in geometria["bones"]:
        if b["name"] == nome:
            return b
    raise ErroDeArte("o geo nao tem o osso '%s'. Ossos do geo: %s"
                     % (nome, list(ossos_do_geo(geometria))))


def cubos_de(geometria, nome):
    return osso_do_geo(geometria, nome).get("cubes", [])


def pivot_de(geometria, nome):
    b = osso_do_geo(geometria, nome)
    if "pivot" not in b:
        raise ErroDeArte("o osso '%s' nao declara pivot, e toda rotacao daqui gira em torno dele"
                         % nome)
    return b["pivot"]


def centro_de(geometria, nome, eixo):
    """Centro dos cubos do osso num eixo (0=x, 1=y, 2=z), em px do modelo."""
    cubos = cubos_de(geometria, nome)
    if not cubos:
        raise ErroDeArte("o osso '%s' nao tem cubo: nao da para ler o eixo dele" % nome)
    return sum(c["origin"][eixo] + c["size"][eixo] / 2.0 for c in cubos) / len(cubos)


def faixa_de(geometria, nomes, eixo):
    """(min, max) num eixo, sobre todos os cubos dos ossos citados."""
    valores = []
    for nome in nomes:
        for c in cubos_de(geometria, nome):
            valores += [c["origin"][eixo], c["origin"][eixo] + c["size"][eixo]]
    if not valores:
        raise ErroDeArte("nenhum cubo em %s: nao da para medir" % list(nomes))
    return min(valores), max(valores)


def filhos_de(geometria, nome):
    return [b["name"] for b in geometria["bones"] if b.get("parent") == nome]


def subarvore_tem_volume(geometria, nome):
    if cubos_de(geometria, nome):
        return True
    return any(subarvore_tem_volume(geometria, f) for f in filhos_de(geometria, nome))


# --------------------------------------------------------------- validacoes

def valida_clipes(animacoes, mob, loops):
    esperado = {"animation.%s.%s" % (mob, c) for c in loops}
    if set(animacoes) != esperado:
        raise ErroDeArte("clipes fora do contrato: %s. O Java pede o clipe pelo nome completo; um "
                         "nome que nao existe no arquivo nao levanta erro, so nao toca"
                         % sorted(set(animacoes) ^ esperado))
    for clipe, tipo in loops.items():
        if tipo not in LOOPS_VALIDOS:
            raise ErroDeArte("o loop de '%s' e %r e os unicos valores que o formato entende sao %s"
                             % (clipe, tipo, list(LOOPS_VALIDOS)))
    for nome, clipe in animacoes.items():
        fim = clipe["animation_length"]
        if fim <= 0:
            raise ErroDeArte("%s tem duracao %s: um clipe de duracao zero nunca termina de comecar"
                             % (nome, fim))
        if not clipe["bones"]:
            raise ErroDeArte("%s nao move osso nenhum: o clipe existe, o Java o toca e a tela nao "
                             "muda" % nome)
        for nome_osso, canais in clipe["bones"].items():
            for canal, quadros in canais.items():
                if canal not in CANAIS:
                    raise ErroDeArte("%s/%s: canal invalido '%s'; o formato 1.8.0 so tem %s"
                                     % (nome, nome_osso, canal, list(CANAIS)))
                if not quadros:
                    raise ErroDeArte("%s/%s/%s: canal declarado sem keyframe nenhum"
                                     % (nome, nome_osso, canal))
                for t, v in quadros.items():
                    if float(t) < 0 or float(t) > fim + 1e-9:
                        raise ErroDeArte("%s/%s: quadro %s fora de [0, %s] -- o que passa do fim "
                                         "nunca e tocado" % (nome, nome_osso, t, fim))
                    if len(v) != 3:
                        raise ErroDeArte("%s/%s/%s: keyframe %s nao e um vetor de tres"
                                         % (nome, nome_osso, canal, v))
                    if canal == "scale" and min(v) <= 0:
                        raise ErroDeArte("%s/%s: escala %s nao da erro -- ela SOME com o osso"
                                         % (nome, nome_osso, v))


def valida_ossos(animacoes, geometria, nao_animados=("root",)):
    """Todo osso citado existe na GEOMETRIA. Osso errado nao da erro no GeckoLib.

    Ele so deixa o membro parado -- e um membro parado num clipe de doze ossos
    passa por decisao de animacao ate alguem comparar com o modelo.
    """
    do_geo = set(ossos_do_geo(geometria))
    for nome, clipe in animacoes.items():
        desconhecidos = sorted(set(clipe["bones"]) - do_geo)
        if desconhecidos:
            raise ErroDeArte("%s move osso que a geometria nao tem: %s. O GeckoLib ignora o nome "
                             "em silencio, e o membro fica parado. Ossos do geo: %s"
                             % (nome, desconhecidos, sorted(do_geo)))
        proibidos = sorted(set(clipe["bones"]) & set(nao_animados))
        if proibidos:
            raise ErroDeArte("%s anima %s. A raiz e a ancora que o renderer alinha com a hitbox: "
                             "mexer nela desloca a silhueta inteira para fora da caixa de colisao, "
                             "e o jogador passa a mirar onde o bicho nao esta"
                             % (nome, proibidos))


def valida_ossos_com_volume(animacoes, geometria):
    """Osso sem cubo na subarvore e osso que a animacao move e ninguem ve."""
    movidos = set()
    for clipe in animacoes.values():
        movidos |= set(clipe["bones"])
    vazios = sorted(n for n in movidos if not subarvore_tem_volume(geometria, n))
    if vazios:
        raise ErroDeArte("estes ossos sao animados e nao tem cubo nenhum na subarvore: %s. O clipe "
                         "roda, o osso gira e a tela nao muda." % vazios)


def valida_loop_fecha(animacoes, mob, loops, animaveis):
    """Clipe que repete tem de TERMINAR onde comecou, canal a canal.

    Um loop cujas pontas nao batem da um tranco a cada volta -- uma vez por
    segundo, para sempre. E o tipo de defeito que ninguem reporta porque ninguem
    consegue descrever: "tem alguma coisa estranha nesse bicho".
    """
    for clipe, tipo in loops.items():
        if tipo is not True:
            continue
        animacao = animacoes["animation.%s.%s" % (mob, clipe)]
        fim = animacao["animation_length"]
        inicio, final = (quadro_em(animacao, 0.0, animaveis),
                         quadro_em(animacao, fim, animaveis))
        for nome in animaveis:
            for canal in CANAIS:
                if inicio[nome][canal] != final[nome][canal]:
                    raise ErroDeArte("%s repete, mas '%s'.%s vale %s no comeco e %s no fim. O "
                                     "tranco acontece a cada %.2fs, para sempre."
                                     % (clipe, nome, canal, inicio[nome][canal],
                                        final[nome][canal], fim))


def valida_duracao_de_ataque(animacoes, mob, ataques):
    """O clipe de ataque cobre o orcamento de ticks que o servidor gasta no golpe.

    `ataques` mapeia nome de clipe -- ou tupla de nomes encadeados -- para um
    `Ataque(windup, active, recovery)` copiado do Java. Clipe mais curto que o
    orcamento nao da erro: o dano sai, o cooldown sai, o log fica limpo, e o
    bicho volta para a pose de repouso no meio do golpe que ainda vai acertar.
    Quem le a animacao para decidir se recua aprende um relogio que o jogo nao
    cumpre.

    Clipe MAIS LONGO que o orcamento e permitido de proposito: o servidor manda
    no fim, e o Java corta o clipe quando o ataque acaba. O que nao pode e faltar.
    """
    for chave, ataque in ataques.items():
        nomes = (chave,) if isinstance(chave, str) else tuple(chave)
        total = 0.0
        for nome in nomes:
            completo = "animation.%s.%s" % (mob, nome)
            if completo not in animacoes:
                raise ErroDeArte("o orcamento de ataque cita o clipe '%s', que nao existe. Clipes: "
                                 "%s" % (completo, sorted(animacoes)))
            total += animacoes[completo]["animation_length"]
        if total + 1e-9 < ataque.segundos:
            raise ErroDeArte("%s dura %.2fs e o servidor gasta %.2fs no golpe (windup %d + active "
                             "%d + recovery %d = %d ticks): o clipe acaba antes, o bicho relaxa no "
                             "meio do ataque que ainda vai acertar, e quem le a animacao para "
                             "decidir se recua aprende um relogio que o jogo nao cumpre"
                             % (" + ".join(nomes), total, ataque.segundos, ataque.windup,
                                ataque.active, ataque.recovery, ataque.ticks))


# ------------------------------------------------------------- serializacao

def serializar(valor, recuo=0):
    """JSON com os vetores em UMA linha.

    `json.dump(indent=2)` quebra [0, -24, 0] em quatro linhas e o arquivo deixa
    de ser legivel num diff -- que e o unico lugar onde a outra pessoa vai
    conferir uma pose.
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


# ---------------------------------------------------------------- sentido de Z
#
# A CONVENCAO, conferida em jogo em 2026-09-22: no lado +X (esquerdo), quem abre
# o membro para FORA e o Z NEGATIVO. A vanilla concorda -- em `HumanoidModel` o
# balanco de ocio SOMA ao zRot do braco direito (x=-5) e SUBTRAI do esquerdo
# (x=+5), afastando os dois do corpo.
#
# MAS OS GERADORES NAO CONCORDAVAM ENTRE SI, e e por isso que isto e uma LISTA e
# nao um sinal global. Varios declaravam a premissa invertida ("Z positivo no
# lado +X abre para fora") e escreveram os numeros sob ela; outros --
# avian_commander e dummy_enemy -- ja estavam certos. Uma negacao aplicada a
# todos consertaria os primeiros e QUEBRARIA os segundos, trocando um defeito
# observado por um introduzido.
#
# Cada nome abaixo foi classificado medindo o extremo de Z do membro ESQUERDO no
# arquivo gerado antes da correcao: positivo = escrito sob a premissa errada.
MOBS_COM_PREMISSA_DE_Z_INVERTIDA = frozenset((
    "bat_scout",               # asas batiam para DENTRO -- o caso observado
    "crab_heavy",              # garras
    "kiriko",                  # bracos; a premissa estava escrita no proprio arquivo
    "kiriko_disfarce",
    "man_faced_ape",
    "man_faced_ape_disfarce",
    "mosquito_officer",
    "spider_eagle",
))

# Familias de osso em que Z significa ABRIR/FECHAR um par esquerda-direita.
# Perna e orelha ficam de FORA de proposito: nelas o eixo nao carrega essa
# semantica, ninguem as observou, e vira-las por simetria seria trocar um
# defeito medido por um nao medido.
_MEMBROS_PAREADOS = ("arm", "braco", "wing", "asa", "claw", "garra")


def _e_membro_pareado(osso):
    nome = osso.lower()
    if not ("left" in nome or "right" in nome):
        return False
    return any(familia in nome for familia in _MEMBROS_PAREADOS)


def _z_invertido(valor):
    """Nega o terceiro componente PRESERVANDO o tipo.

    `z * -1.0` parece equivalente e nao e: ele transforma `0` em `-0.0` e `85`
    em `-85.0`, e o arquivo passa a diferir em todo osso pareado de todo mob --
    inclusive nos que tem Z zerado e nao mudaram de comportamento nenhum. O diff
    vira ruido, e a mudanca de verdade se esconde dentro dele.
    """
    if not isinstance(valor, (list, tuple)) or len(valor) != 3:
        return valor
    x, y, z = valor
    if not isinstance(z, (int, float)) or z == 0:
        return valor
    return [x, y, -z]


def corrigir_sentido_de_z_em(mob, animacoes):
    """Aplica {@link SENTIDO_DE_Z} a um dicionario de clipes ja montado.

    A MESMA regra de `Animacoes.corrigir_sentido_de_z`, exposta para os SETE
    PRIMEIROS mobs -- foxbear, frog_in_waiting, great_stamp, kiriko,
    man_faced_ape, master_of_the_swamp e spider_eagle --, que foram escritos
    antes desta biblioteca existir e tem cada um o proprio caminho de escrita.

    Eles sao exatamente os arquivos que `estado-en.md` descreve como "o pincel
    copiado de arquivo em arquivo", e a premissa invertida veio junto na copia.
    Uma negacao escrita a mao em cada um deles seria a oitava copia da mesma
    regra -- e a que ficaria para tras no dia em que o sinal mudasse.
    """
    if mob not in MOBS_COM_PREMISSA_DE_Z_INVERTIDA:
        return animacoes
    for clipe in animacoes.values():
        for osso, canais in (clipe.get("bones") or {}).items():
            if not _e_membro_pareado(osso):
                continue
            rotacao = canais.get("rotation") if isinstance(canais, dict) else None
            if rotacao is None:
                continue
            if isinstance(rotacao, dict):
                for instante, valor in rotacao.items():
                    rotacao[instante] = _z_invertido(valor)
            else:
                canais["rotation"] = _z_invertido(rotacao)
    return animacoes


class Animacoes:
    """Os clipes de um mob: construcao, portoes e o arquivo.

    `loops` e a UNICA declaracao de quais clipes existem e como cada um repete.
    Uma tupla CLIPES ao lado de um dicionario LOOPS seria a mesma lista escrita
    duas vezes, e quem acrescentasse um clipe em so uma delas teria um arquivo
    que passa nos portoes e um clipe que o Java nunca acha.
    """

    def __init__(self, mob, geometria, loops, nao_animados=("root",)):
        self.mob = mob
        self.geometria = geometria
        self.loops = dict(loops)
        self.nao_animados = tuple(nao_animados)
        self.ossos = ossos_do_geo(geometria)
        self.animaveis = tuple(o for o in self.ossos if o not in self.nao_animados)
        self.clipes = {}

    def nome_completo(self, clipe):
        return "animation.%s.%s" % (self.mob, clipe)

    def clipe(self, nome, duracao, bones):
        """Registra um clipe ja montado.

        A ordem das chaves -- loop, animation_length, bones -- e fixa porque ela
        sobrevive ate o arquivo, e um arquivo que embaralha a ordem das chaves a
        cada geracao faz todo diff parecer uma reescrita.
        """
        if nome not in self.loops:
            raise ErroDeArte("o clipe '%s' nao esta declarado em LOOPS. LOOPS e a lista de clipes "
                             "deste mob; um clipe fora dela nunca e tocado pelo Java" % nome)
        self.clipes[self.nome_completo(nome)] = ordenar(
            {"loop": self.loops[nome], "animation_length": duracao, "bones": bones},
            self.animaveis)
        return self.clipes[self.nome_completo(nome)]

    def corrigir_sentido_de_z(self):
        """Delega para {@link corrigir_sentido_de_z_em}, com o nome deste mob.

        A regra e a lista moram num lugar so: repeti-las aqui daria duas copias
        da mesma verdade, e a segunda ficaria para tras.
        """
        return corrigir_sentido_de_z_em(self.mob, self.clipes_por_mob())

    def clipes_por_mob(self):
        """Os clipes deste mob, para a correcao de sinal."""
        return self.clipes

    def validar(self, ataques=None, extras=()):
        self.corrigir_sentido_de_z()
        valida_clipes(self.clipes, self.mob, self.loops)
        valida_ossos(self.clipes, self.geometria, self.nao_animados)
        valida_ossos_com_volume(self.clipes, self.geometria)
        valida_loop_fecha(self.clipes, self.mob, self.loops, self.animaveis)
        if ataques:
            valida_duracao_de_ataque(self.clipes, self.mob, ataques)
        for extra in extras:
            extra(self)
        return self

    def json(self):
        # Os clipes saem na ordem de LOOPS, e nao na ordem em que foram
        # construidos: a ordem de construcao muda quando alguem move uma funcao
        # de lugar, e o diff acusaria uma mudanca que nao houve.
        ordenados = {self.nome_completo(c): self.clipes[self.nome_completo(c)]
                     for c in self.loops}
        return {"format_version": FORMATO_ANIMACAO, "animations": ordenados}

    def texto(self):
        texto = serializar(self.json())
        json.loads(texto)  # o que sai daqui tem de voltar como JSON, sempre
        return texto

    def escrever(self, destino=None, raiz="."):
        return escrever_texto(destino or caminho_animacao(self.mob, raiz), self.texto())

    def resumo(self, destino):
        linhas = ["escrito %s" % destino,
                  "  %-34s %6s  %-19s %s" % ("clipe", "dur", "loop", "ossos")]
        for clipe in self.loops:
            a = self.clipes[self.nome_completo(clipe)]
            linhas.append("  %-34s %5.2fs  %-19s %s"
                          % (self.nome_completo(clipe), a["animation_length"],
                             str(a["loop"]), ", ".join(sorted(a["bones"]))))
        return "\n".join(linhas)

    def emitir(self, ataques=None, extras=(), destino=None, raiz=".", silencioso=False):
        """Valida, escreve e relata. Validar antes de escrever e o que impede um
        .animation.json truncado no disco depois de uma recusa no meio."""
        self.validar(ataques, extras)
        caminho = self.escrever(destino, raiz)
        if not silencioso:
            print(self.resumo(caminho))
        return caminho
