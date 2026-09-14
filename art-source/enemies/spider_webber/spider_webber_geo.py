"""Geometria da Spider Webber -- a aracnidea cuja FIANDEIRA e o aviso.

O QUE O CORPO PRECISA CONTAR, e o que acontece se ele nao contar.

Esta formiga quimera nao vence pelo golpe: ela vence pelo TEMPO que tira do
jogador. O unico ataque dela e uma teia que imobiliza por tres segundos, e o
unico jeito de desligar a teia e chegar perto -- dentro de 3.5 blocos o servidor
RECUSA o lancamento. Essas duas frases sao a ficha inteira, e o modelo tem de
dizer as duas antes de a primeira teia sair.

A PRIMEIRA COISA QUE O CORPO CONTA E A FIANDEIRA. Ela e o orgao que da nome ao
bicho e e a unica informacao que o jogador tem ANTES do primeiro lancamento --
depois disso ele ja aprendeu apanhando, que e tarde. Uma fiandeira escondida
dentro do abdome nao levanta excecao nenhuma: da uma aranha generica, e o
telegrafo passa a existir so no movimento (o clipe `windup`), ou seja, so para
quem ja esta olhando na hora exata. `valida_fiandeira_e_o_aviso` cobra que ela
seja a peca mais de TRAS, que ela SAIA do abdome e que tenha silhueta suficiente
para ser vista de longe.

A SEGUNDA E A ZONA MORTA, e essa liga o desenho a regra do servidor.
SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA recusa o tiro abaixo de 3.5 blocos --
mas essa recusa so e uma RESPOSTA se um jogador couber, em pe, entre o corpo
desenhado da aranha e o comeco da area da teia. Se o modelo crescesse para a
frente, ou se alguem encolhesse o alcance minimo numa sessao de balanceamento, a
zona de recusa ficaria mais estreita que o proprio jogador: ele continuaria
apanhando de teia enquanto estivesse literalmente dentro do bicho, e a unica
saida do encontro sumiria sem um unico erro no log.
`valida_zona_morta_da_teia` cobra exatamente essa folga.

A HITBOX MANDA NO MODELO. A entidade e sized(1.2F, 1.3F) -- 19.2 x 20.8 px -- e a
bateria da biblioteca reprova se o modelo estourar isso. O piso das patas fica em
y=0, e o corpo e ALTO na caixa de proposito: uma aranha rasteira numa hitbox de
1.3 bloco deixaria um palmo de ar acima dela, e o jogador que mirasse na
silhueta erraria a caixa de colisao sem entender por que.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

CUIDADO COM AS DUAS CONVENCOES DE FRENTE. Aqui, na GEOMETRIA, a frente e -Z. Na
transformacao de mundo de AttackHitbox a frente e +Z, porque com yaw 0 o olhar
vanilla aponta para +Z. Misturar as duas ja custou um bug neste repositorio -- a
caixa do golpe ficou ATRAS do mob, que atacava, animava e nao encostava em
ninguem na frente. As validacoes daqui convertem uma na outra de proposito, e a
conversao esta escrita em cada uma delas.

Regerar:  python art-source/enemies/spider_webber/spider_webber_geo.py
Exporta:  src/main/resources/assets/nenfoundation/geo/entity/spider_webber.geo.json
"""
import os
import sys

# art-source/enemies/<mob>/<arquivo>.py  ->  art-source
sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))

from comum import geometria as geo      # noqa: E402

MOB = "spider_webber"
UV_LARGURA, UV_ALTURA = 128, 64

# O literal da chamada Java, copiado como esta: EnemyEntityTypes .sized(1.2F, 1.3F).
HITBOX = (1.2, 1.3)

# Distancia (centro a centro, em BLOCOS) abaixo da qual o servidor RECUSA a teia.
# SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA = 3.5D.
#
# Ele esta aqui para ser COBRADO contra o desenho: a zona de recusa tem de caber
# um jogador em pe, senao "chegar perto" deixa de ser uma resposta possivel.
ALCANCE_MINIMO_DA_TEIA_EM_BLOCOS = 3.5

# Largura de um jogador, em blocos -- ele tem 0.6 de lado.
# E a regua de "cabe alguem aqui?", e nao um numero de design desta aranha.
LARGURA_DE_UM_JOGADOR_EM_BLOCOS = 0.6

# Quanto a fiandeira tem de SOBRAR para tras do abdome, em px.
#
# Dois pixels sao pouco no atlas e muito na silhueta: e a diferenca entre uma
# bossa colada no corpo e uma peca que se ve recortada contra o fundo a quinze
# blocos. Limite de LEITURA -- ele nao muda nada no servidor, e e por isso que
# ele precisa de uma regua: nada mais no repositorio percebe se ele sumir.
SALIENCIA_MINIMA_DA_FIANDEIRA_PX = 2

# Area minima da silhueta da fiandeira vista de tras (largura x altura), em px.
#
# Doze pixels (4x3) sao o menor retangulo que ainda le como ORGAO e nao como
# ruido de textura na ponta do abdome. Abaixo disso a peca existe no modelo,
# passa em todos os portoes e desaparece na tela.
AREA_MINIMA_DA_FIANDEIRA_PX = 12

# ---------------------------------------------------------------- hierarquia
#
# Hierarquia CONGELADA: o .animation.json escreve contra estes nomes, e nome
# errado nao da erro -- o GeckoLib so deixa o osso parado.
#
# `abdomen` e um osso PROPRIO, e nao um pedaco do torax, por uma razao mecanica:
# o aviso da teia e o abdome se ERGUENDO sobre as costas para apontar a fiandeira
# para a frente. Peca pintada nao se mexe, e um abdome fundido ao torax obrigaria
# o clipe a girar o corpo inteiro -- as oito patas subiriam junto e a aranha
# empinaria como um cavalo em vez de mirar.
#
# `fiandeira` pendura no `abdomen`, e nao no `body`. Pendurada no corpo ela
# ficaria PARADA enquanto o abdome sobe: o pai existe, o portao Java passa, e o
# que aparece na tela e o unico telegrafo do mob deixando de acompanhar a peca
# que ele telegrafa.
#
# As oito patas sao QUATRO PARES, e cada uma e um par de ossos: `leg_*` e o femur
# que sai do torax na horizontal, `foot_*` e o tarso que desce ate o chao. Dois
# ossos porque a perna de aracnideo DOBRA no joelho, e um cubo so obrigaria a
# escolher entre uma perna reta (que le como inseto de brinquedo) e uma perna
# diagonal presa por bind rotation (que criaria uma segunda fonte para a pose de
# repouso, decidida em silencio entre o geo e o .animation.json).
PARES_DE_PATAS = ("front", "second", "third", "rear")
LADOS = ("left", "right")

# Z do par, em px. Elas se espalham ao longo do torax, e nao no mesmo ponto:
# quatro pares saindo do mesmo Z seriam um leque, e nao um corpo.
Z_DO_PAR = {"front": -3, "second": -1, "third": 1, "rear": 3}
# +X e o lado ESQUERDO; o femur sai da parede do torax para fora.
SINAL_DO_LADO = {"left": 1, "right": -1}


def _ossos():
    ossos = [
        geo.Osso("root", None, (0, 0, 0)),
        # o torax e o PIVO do corpo inteiro; ele fica no meio da altura util
        geo.Osso("body", "root", (0, 15, 0)),
        geo.Osso("head", "body", (0, 15, -3)),
        # o pivo do abdome fica na JUNCAO com o torax, e nao no centro da peca:
        # e em torno dele que o aviso ergue o abdome, e um pivo no meio faria a
        # peca girar atravessando o proprio torax
        geo.Osso("abdomen", "body", (0, 16, 3)),
        geo.Osso("fiandeira", "abdomen", (0, 14, 9)),
    ]
    for par in PARES_DE_PATAS:
        for lado in LADOS:
            sinal = SINAL_DO_LADO[lado]
            ossos.append(geo.Osso("leg_%s_%s" % (par, lado), "body",
                                  (5 * sinal, 15, Z_DO_PAR[par])))
            ossos.append(geo.Osso("foot_%s_%s" % (par, lado), "leg_%s_%s" % (par, lado),
                                  (8 * sinal, 15, Z_DO_PAR[par])))
    return tuple(ossos)


# (u,v) e o canto do layout de caixa no atlas; (x,y,z) e o canto MINIMO do cubo.
# Uma caixa w x h x d ocupa (2d+2w) x (d+h) pixels a partir de (u,v).
def _caixas():
    caixas = [
        # cefalotorax: o bloco central de onde saem as oito patas. Largo e curto,
        # porque num aracnideo o volume do bicho mora no ABDOME e nao no torax --
        # inverter os dois daria a silhueta de uma formiga comum, e a ficha
        # promete outra coisa.
        geo.Caixa("cefalotorax", "body", 0, 0, -5, 12, -3, 10, 6, 6),
        # abdome: a peca mais alta e a mais funda. Ela e a aranha vista de longe,
        # e e ela que carrega a fiandeira.
        geo.Caixa("abdomen", "abdomen", 32, 0, -5, 11, 3, 10, 8, 6),
        # cabeca: pequena, para que a leitura de frente seja "quelicera e olhos" e
        # nao "cara". Ela nao compete com o abdome, que e onde a acao acontece.
        geo.Caixa("cabeca", "head", 64, 0, -3, 13, -7, 6, 4, 4),
        # A FIANDEIRA. Ela sai 2 px para tras do abdome (z 9..11 contra 3..9) e
        # tem 4x3 px de silhueta vista de tras. As duas medidas sao cobradas por
        # valida_fiandeira_e_o_aviso, porque nenhum outro portao do repositorio
        # percebe se esta peca encolher ate sumir.
        geo.Caixa("fiandeira", "fiandeira", 84, 0, -2, 12, 9, 4, 3, 2),
    ]
    u_femur, v_femur = 96, 0
    u_tarso, v_tarso = 0, 20
    i = 0
    for par in PARES_DE_PATAS:
        for lado in LADOS:
            sinal = SINAL_DO_LADO[lado]
            z = Z_DO_PAR[par] - 1
            # femur: sai na horizontal, 4 px para fora da parede do torax. Ele e
            # quem afasta o tarso do corpo -- sem ele, as patas desceriam coladas
            # e a aranha leria como um besouro de patas curtas.
            x_femur = 5 if sinal > 0 else -9
            caixas.append(geo.Caixa("femur_%s_%s" % (par, lado), "leg_%s_%s" % (par, lado),
                                    u_femur + 12 * (i % 2), v_femur + 4 * (i // 2),
                                    x_femur, 14, z, 4, 2, 2))
            # tarso: desce do joelho ate o chao. Ele e o que define o piso y=0 do
            # modelo inteiro e o que mais se mexe na marcha.
            x_tarso = 7 if sinal > 0 else -9
            caixas.append(geo.Caixa("tarso_%s_%s" % (par, lado), "foot_%s_%s" % (par, lado),
                                    u_tarso + 8 * i, v_tarso,
                                    x_tarso, 0, z, 2, 14, 2))
            i += 1
    return tuple(caixas)


OSSOS = _ossos()
CAIXAS = _caixas()


# --------------------------------------------------- validacoes DO BICHO
# A primeira liga o DESENHO a REGRA DO SERVIDOR; a segunda liga o desenho ao
# unico aviso que o mob da. Nenhuma das divergencias que elas pegam levanta
# excecao em lugar nenhum.

def valida_zona_morta_da_teia(m):
    """A zona em que o servidor RECUSA a teia tem de caber um jogador em pe.

    Esta e a regua que liga a arte a mecanica inteira do bicho.

    `SpiderWebberTuning.ALCANCE_MINIMO_DA_TEIA` recusa o lancamento abaixo de 3.5
    blocos do centro da aranha. Essa recusa e a UNICA resposta que o encontro
    oferece contra a imobilizacao -- e ela so e uma resposta se um jogador couber,
    de pe, entre o corpo DESENHADO e o comeco da area da teia.

    A conta e feita na frente do bicho, e a conversao de convencao esta aqui de
    propostio: na geometria a frente e -Z, entao o alcance do desenho para a
    frente e `-z0`. O alcance da regra e medido em blocos do CENTRO da entidade,
    que e o mesmo ponto de onde `z=0` parte.

    Duas coisas quebram isto, e as duas sao silenciosas:

      * o modelo cresce para a frente (uma quelicera longa, um par de patas
        dianteiras esticadas no repouso) e come a folga;
      * alguem encolhe ALCANCE_MINIMO_DA_TEIA numa sessao de balanceamento.

    Nos dois casos a zona de recusa fica mais estreita que o proprio jogador: ele
    continuaria apanhando de teia enquanto estivesse literalmente dentro do bicho,
    e a saida do encontro sumiria sem um unico erro no log.
    """
    _, _, (z0, _) = m.limites()
    frente_px = -z0
    zona_px = ALCANCE_MINIMO_DA_TEIA_EM_BLOCOS * 16.0
    folga_px = zona_px - frente_px
    exigido_px = LARGURA_DE_UM_JOGADOR_EM_BLOCOS * 16.0
    if folga_px < exigido_px:
        raise geo.ErroDeArte(
            "o corpo desenhado chega a %.1f px a frente do centro e o servidor so recusa a teia "
            "abaixo de %.1f px (%.2f blocos): sobram %.1f px de zona morta e um jogador ocupa "
            "%.1f px. Nao da para 'chegar perto' sem ficar dentro do bicho, e a unica resposta "
            "que este encontro oferece deixa de existir -- sem erro nenhum"
            % (frente_px, zona_px, ALCANCE_MINIMO_DA_TEIA_EM_BLOCOS, folga_px, exigido_px))


def valida_fiandeira_e_o_aviso(m):
    """A fiandeira tem de ser vista, e vista de TRAS.

    Ela e o orgao que nomeia o bicho e a unica informacao que o jogador tem antes
    do primeiro lancamento. O molde genetico garante o trait WEB
    (ChimeraOfficerDefinitions.spiderWebber) e a entidade tem UM ataque so, que e
    a teia: se a peca que a produz nao aparece, o mob vira uma aranha generica e o
    telegrafo passa a existir apenas no movimento -- ou seja, so para quem ja
    estava olhando no segundo e meio do aviso.

    Tres coisas sao cobradas, e nenhuma delas levanta excecao em jogo:

      * a fiandeira e a peca mais de TRAS. Enfiada no meio do abdome ela some por
        tras da propria silhueta, de todo angulo util;
      * ela SOBRA do abdome por pelo menos SALIENCIA_MINIMA_DA_FIANDEIRA_PX. Uma
        peca rente ao corpo nao tem recorte contra o fundo, e a distancia le como
        mancha de textura;
      * ela tem silhueta (largura x altura) de pelo menos
        AREA_MINIMA_DA_FIANDEIRA_PX. Abaixo disso a peca existe no modelo, passa
        em todos os portoes e desaparece na tela.
    """
    fiandeira = m.caixa("fiandeira")
    (_, _), (_, _), (fz0, fz1) = geo.volume(fiandeira)

    mais_de_tras = max(geo.volume(c)[2][1] for c in m.caixas)
    if fz1 < mais_de_tras:
        atras = [c.nome for c in m.caixas if geo.volume(c)[2][1] == mais_de_tras]
        raise geo.ErroDeArte(
            "a fiandeira termina em z=%s e %s vai ate z=%s: o orgao que da nome ao bicho fica "
            "escondido atras do proprio corpo, e o unico aviso que o mob da antes da primeira "
            "teia deixa de ser visivel" % (fz1, atras, mais_de_tras))

    _, _, (az0, az1) = geo.volume(m.caixa("abdomen"))
    saliencia = fz1 - az1
    if saliencia < SALIENCIA_MINIMA_DA_FIANDEIRA_PX:
        raise geo.ErroDeArte(
            "a fiandeira sobra %s px do abdome (z ate %s contra %s) e o minimo e %s: rente ao "
            "corpo ela nao tem recorte contra o fundo, e a distancia le como mancha de textura "
            "em vez de orgao" % (saliencia, fz1, az1, SALIENCIA_MINIMA_DA_FIANDEIRA_PX))
    if fz0 < az0:
        raise geo.ErroDeArte(
            "a fiandeira comeca em z=%s, a frente do abdome (z=%s): ela atravessaria o corpo em "
            "vez de sair dele, e o cintilamento resultante o jogador le como bug de video"
            % (fz0, az0))

    silhueta = fiandeira.w * fiandeira.h
    if silhueta < AREA_MINIMA_DA_FIANDEIRA_PX:
        raise geo.ErroDeArte(
            "a fiandeira tem %d x %d = %d px de silhueta vista de tras e o minimo e %d: a peca "
            "existe no modelo, passa em todos os portoes e some na tela"
            % (fiandeira.w, fiandeira.h, silhueta, AREA_MINIMA_DA_FIANDEIRA_PX))


def valida_oito_patas_pareadas(m):
    """Oito patas, em quatro pares espelhados -- nem sete, nem oito do mesmo lado.

    A ficha diz INSECTOID de oito patas, e oito e o que separa "aranha" de
    "inseto" na leitura de um segundo. Mas a razao de a regua existir e mecanica e
    nao estetica: a marcha do .animation.json anda os pares em CONTRAFASE, e ela
    monta essa contrafase a partir destes nomes. Um par que perdesse o espelho
    ficaria com um lado animado e o outro parado -- o clipe roda, o osso gira, e
    metade do bicho arrasta.

    Ela tambem cobra que as patas caibam na caixa de colisao pelo eixo X, que e
    onde elas chegam mais perto da parede: uma pata 2 px mais larga nao da erro,
    entra no bloco vizinho e so aparece para quem encurralou a aranha numa parede.
    """
    limite = m.hitbox_largura_px / 2.0
    for par in PARES_DE_PATAS:
        for lado in LADOS:
            for prefixo in ("femur", "tarso"):
                nome = "%s_%s_%s" % (prefixo, par, lado)
                caixa = m.caixa(nome)  # levanta ErroDeArte se o par perdeu o espelho
                (x0, x1), _, _ = geo.volume(caixa)
                if abs(x0) > limite or abs(x1) > limite:
                    raise geo.ErroDeArte(
                        "a peca '%s' vai de x=%s a x=%s e a meia-largura da hitbox e %.1f px: o "
                        "membro atravessa a caixa de colisao, e o sintoma e uma pata dentro do "
                        "bloco vizinho" % (nome, x0, x1, limite))

    patas = [c for c in m.caixas if c.nome.startswith("tarso_")]
    if len(patas) != 8:
        raise geo.ErroDeArte(
            "o modelo tem %d tarso(s) e uma aranha tem 8: com outro numero a silhueta le como "
            "inseto, e a marcha em tripe do .animation.json passa a mover pares que nao existem"
            % len(patas))


MODELO = geo.Modelo(MOB, OSSOS, CAIXAS, uv=(UV_LARGURA, UV_ALTURA), hitbox_blocos=HITBOX)

if __name__ == "__main__":
    MODELO.emitir(extras=(valida_zona_morta_da_teia,
                          valida_fiandeira_e_o_aviso,
                          valida_oito_patas_pareadas))
