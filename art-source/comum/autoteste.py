"""Autoteste da biblioteca comum: monta um bicho minimo e mede o que ela promete.

POR QUE ESTE ARQUIVO EXISTE. Uma biblioteca de PORTOES que nunca reprovou nada e
um carimbo. O CLAUDE.md e explicito: ao criar um portao, alimente-o com um caso
que DEVE reprovar e confirme que ele reprova. Aqui sao dez casos, e cada um deles
e um defeito real que ja apareceu -- ou que apareceria -- num gerador de mob:
pai inexistente, caixa fora da folha, caixas dividindo pixel, modelo maior que a
hitbox, pivot no vazio, z-fighting, osso fantasma na animacao, clipe de ataque
mais curto que o golpe do servidor, pincel vazando da face e face sem tinta.

Nenhum desses defeitos da erro por conta propria. Todos produzem JSON valido,
PNG valida e log limpo. E por isso que a regua tem de ser medida.

O BICHO DE EXEMPLO NAO E UM MOB. Ele existe so para exercitar o formato, e as
medidas dele foram escolhidas para caber com folga: hitbox de 1 x 1 bloco, atlas
64 x 64, seis ossos, seis caixas, tres clipes. Ele nao vai para o resource pack --
tudo e escrito num diretorio temporario, que some no fim.

Rode:  python art-source/comum/autoteste.py
Sai 0 se tudo passou; sai 1 e diz o que falhou se nao.
"""
import io
import json
import os
import sys
import tempfile

# Este arquivo e executado direto (`python art-source/comum/autoteste.py`), entao
# `comum` ainda nao e importavel: o Python poe no sys.path a pasta DO ARQUIVO, e
# nao a pasta acima dela. Sem este ajuste o autoteste so rodaria como modulo, e um
# portao que so roda de um jeito e um portao que ninguem roda.
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from comum import ErroDeArte, TICKS_POR_SEGUNDO  # noqa: E402
from comum import animacao as anim               # noqa: E402
from comum import geometria as geo               # noqa: E402
from comum import textura as tex                 # noqa: E402

MOB = "bicho_de_prova"
UV = (64, 64)

# A hitbox do bicho de exemplo, no formato em que o Java a declara. Num mob de
# verdade esta linha carrega o literal exato de EnemyEntityTypes.
HITBOX_BLOCOS = (1.0, 1.0)

OSSOS = (
    geo.Osso("root", None, (0, 0, 0)),
    geo.Osso("body", "root", (0, 9, 0)),
    geo.Osso("head", "body", (0, 10, -4)),
    geo.Osso("leg_left", "body", (2, 6, -1)),
    geo.Osso("leg_right", "body", (-2, 6, -1)),
    geo.Osso("tail", "body", (0, 8, 4)),
)

# Caixa(nome, osso, u, v, x, y, z, w, h, d)
CAIXAS = (
    geo.Caixa("corpo", "body", 0, 0, -3, 6, -4, 6, 6, 8),
    geo.Caixa("corcova", "body", 28, 0, -2, 12, -1, 4, 2, 4),
    geo.Caixa("cabeca", "head", 44, 0, -2, 9, -7, 4, 4, 3),
    geo.Caixa("cauda", "tail", 0, 14, -2, 7, 4, 4, 2, 4),
    geo.Caixa("perna_esq", "leg_left", 16, 14, 1, 0, -2, 2, 6, 2),
    geo.Caixa("perna_dir", "leg_right", 24, 14, -3, 0, -2, 2, 6, 2),
)

# O loop mora no arquivo, e LOOPS e tambem a lista de clipes -- ver o cabecalho
# de comum/animacao.py.
LOOPS = {
    "idle": True,
    "walk": True,
    "attack": "hold_on_last_frame",
}

# Copiados de um servidor imaginario, no formato em que o mob de verdade copia do
# Java: 10 + 4 + 6 = 20 ticks = 1,00s. O clipe `attack` dura exatamente isso.
ATAQUE = anim.Ataque(windup=10, active=4, recovery=6)
ATAQUES = {"attack": ATAQUE}

DUR_IDLE = 2.0
DUR_WALK = 1.0
DUR_ATTACK = ATAQUE.segundos

PALETA = tex.Paleta(
    DORSO=(58, 48, 40),      # o que se ve de cima
    COURO=(96, 80, 64),      # o flanco, entre os dois
    VENTRE=(148, 128, 106),  # o que se ve de baixo
    UNHA=(26, 24, 20),       # o unico quase-preto da folha
)


# ------------------------------------------------------ o bicho, de verdade

def modelo(ossos=OSSOS, caixas=CAIXAS, uv=UV, hitbox=HITBOX_BLOCOS):
    return geo.Modelo(MOB, ossos, caixas, uv, hitbox)


def clipes(geometria, dur_attack=DUR_ATTACK):
    """Tres clipes: dois que repetem e um de ataque.

    `idle` e `walk` fecham o loop porque sao cossenoides de periodo inteiro --
    e nao porque alguem escreveu o mesmo valor nas duas pontas a mao.

    `dur_attack` e parametro para que o caso (8) possa montar um ataque CURTO e
    por tudo mais no lugar: encurtar so o `animation_length` de um clipe pronto
    deixaria os keyframes para fora da janela, e a recusa viria de
    `valida_clipes` em vez de vir da regua do orcamento do servidor. O caso
    passaria, a regua continuaria sem nunca ter sido medida, e esse e exatamente
    o falso verde que este arquivo existe para impedir.
    """
    animacoes = anim.Animacoes(MOB, geometria, LOOPS)

    respirar = {}
    anim.curva(respirar, "body", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 2.0)])
    anim.curva(respirar, "head", "rotation",
               [(t, anim.vetor(x=v)) for t, v in
                anim.cossenoide(DUR_IDLE, DUR_IDLE, 3.0, fase=0.25)])
    animacoes.clipe("idle", DUR_IDLE, respirar)

    marchar = {}
    for perna, fase in (("leg_left", 0.0), ("leg_right", 0.5)):
        anim.curva(marchar, perna, "rotation",
                   [(t, anim.vetor(x=v)) for t, v in
                    anim.cossenoide(DUR_WALK, DUR_WALK, 24.0, fase=fase)])
    anim.curva(marchar, "tail", "rotation",
               [(t, anim.vetor(y=v)) for t, v in
                anim.cossenoide(DUR_WALK, DUR_WALK, 8.0)])
    animacoes.clipe("walk", DUR_WALK, marchar)

    # O ataque cobre os 20 ticks do servidor: sobe a cabeca, bate, e volta. As
    # fracoes sao do GESTO (meio do curso, o golpe, a volta) e por isso escalam
    # com a duracao -- um instante em segundos fixos sairia da janela assim que
    # alguem mexesse no tempo do ataque.
    bater = {}
    anim.curva(bater, "head", "rotation",
               [(0.0 * dur_attack, anim.vetor()), (0.5 * dur_attack, anim.vetor(x=28)),
                (0.7 * dur_attack, anim.vetor(x=-34)), (1.0 * dur_attack, anim.vetor())])
    anim.curva(bater, "body", "position",
               [(0.0 * dur_attack, anim.vetor()), (0.5 * dur_attack, anim.vetor(z=1)),
                (0.7 * dur_attack, anim.vetor(z=-2)), (1.0 * dur_attack, anim.vetor())])
    animacoes.clipe("attack", dur_attack, bater)
    return animacoes


def folha(caixas=CAIXAS, uv=UV):
    """A pintura: contraluz em tudo, e unha escura nas pernas."""
    f = tex.Folha(uv, caixas)
    for c in caixas:
        p = f[c.nome]
        p.tudo(PALETA.COURO)
        p.face("topo", PALETA.DORSO)
        p.face("base", PALETA.VENTRE)
        for parede in tex.PAREDES:
            p.faixa_no_topo(parede, 1, PALETA.DORSO)
            p.faixa_no_pe(parede, 1, PALETA.VENTRE)
        p.salpicar("topo", PALETA.DORSO, 60, 3)
    for nome in ("perna_esq", "perna_dir"):
        f[nome].faixa_no_pe("frente", 1, PALETA.UNHA)
        f[nome].face("base", PALETA.UNHA)
    return f


def gerar(raiz, silencioso=True):
    """Um ciclo inteiro: geo, animacao e textura, nesta ordem.

    A ordem nao e arbitraria -- a animacao LE o geo do disco, e a textura LE a
    mesma tabela de caixas que o geo validou.
    """
    caminho_geo = modelo().emitir(raiz=raiz, silencioso=silencioso)
    geometria = anim.carregar_geo(MOB, raiz)
    caminho_anim = clipes(geometria).emitir(ataques=ATAQUES, raiz=raiz,
                                            silencioso=silencioso)
    caminho_png = folha().emitir(MOB, raiz=raiz, silencioso=silencioso)
    return caminho_geo, caminho_anim, caminho_png


# --------------------------------------------------------------- os casos

class Relatorio:
    def __init__(self):
        self.linhas = []
        self.falhas = 0

    def ok(self, rotulo, detalhe=""):
        self.linhas.append("  ok      %-52s %s" % (rotulo, detalhe))

    def falhou(self, rotulo, detalhe):
        self.falhas += 1
        self.linhas.append("  FALHOU  %-52s %s" % (rotulo, detalhe))

    def confere(self, rotulo, condicao, detalhe=""):
        if condicao:
            self.ok(rotulo, detalhe)
        else:
            self.falhou(rotulo, detalhe or "condicao falsa")

    def deve_reprovar(self, rotulo, funcao, trecho):
        """O caso que DEVE levantar ValueError, E PELA REGUA CERTA.

        `trecho` nao e zelo: um caso montado para furar a regua do atlas pode
        acabar reprovando na regua da hitbox, e ai o autoteste fica verde com a
        regua do atlas nunca tendo sido medida. Conferir a mensagem e o que
        transforma "alguma coisa reclamou" em "a coisa certa reclamou".
        """
        try:
            funcao()
        except ValueError as erro:
            mensagem = " ".join(str(erro).split())
            if trecho not in mensagem:
                self.falhou(rotulo, "reprovou pela regua ERRADA: %s" % mensagem[:70])
            else:
                self.ok(rotulo, mensagem[:62] + ("..." if len(mensagem) > 62 else ""))
        else:
            self.falhou(rotulo, "NAO reprovou -- a regua deixou passar o defeito")


def a_superficie_que_o_leia_me_promete(r, geometria):
    """Tudo que o LEIA-ME.md ensina a usar, exercitado de verdade.

    Documento que descreve uma API que ninguem chama vira a segunda fonte de
    verdade mais barata que existe: ele nao da erro, so envelhece. O que esta
    escrito la e chamado aqui.
    """
    # `derivar`: o membro terminal sai do pai, nunca da mao.
    bones = {}
    anim.curva(bones, "leg_left", "rotation",
               [(0.0, anim.vetor(x=20)), (0.5, anim.vetor(x=-20))])
    anim.derivar(bones, "leg_left", "leg_right", "rotation", -0.45)
    derivado = bones["leg_right"]["rotation"]
    r.confere("derivar() tira o membro terminal do pai, com o fator negativo",
              derivado["0.0"] == [-9, 0, 0] and derivado["0.5"] == [9, 0, 0],
              "%s -> %s" % (bones["leg_left"]["rotation"]["0.0"], derivado["0.0"]))

    # `valida_encadeamento`: pai certo, e nao qualquer ancestral.
    geo.valida_encadeamento(OSSOS, (("head", "body"),), "a cabeca pendura no corpo")
    r.deve_reprovar("encadeamento: filho pendurado no ancestral errado",
                    lambda: geo.valida_encadeamento(OSSOS, (("head", "root"),),
                                                    "a cabeca tem de pendurar no corpo"),
                    "devia pendurar em")

    # Orcamento de ataque repartido em clipes encadeados: confere a SOMA.
    trio = clipes(geometria)
    encadeado = {("idle", "walk", "attack"): anim.Ataque(20, 20, 20)}
    anim.valida_duracao_de_ataque(trio.clipes, MOB, encadeado)
    r.ok("orcamento de ataque repartido soma as duracoes dos clipes",
         "idle+walk+attack = %.2fs >= %.2fs" % (DUR_IDLE + DUR_WALK + DUR_ATTACK,
                                                anim.Ataque(20, 20, 20).segundos))
    r.deve_reprovar("orcamento repartido: a soma nao cobre o golpe",
                    lambda: anim.valida_duracao_de_ataque(
                        trio.clipes, MOB, {("walk", "attack"): anim.Ataque(40, 20, 20)}),
                    "o servidor gasta")

    # `Paleta`: nome errado vira recusa que ensina, e nao NameError no meio da
    # pintura.
    r.deve_reprovar("paleta: cor que nao existe",
                    lambda: PALETA.VENTRA, "a paleta nao tem a cor")
    r.deve_reprovar("paleta: cor que nao e (r, g, b) de 0..255",
                    lambda: tex.Paleta(DORSO=(300, 0, 0)), "e uma tupla (r, g, b)")

    # Duas folhas do mesmo mob nao podem compartilhar tom.
    tex.valida_paletas_disjuntas({"adulto": PALETA,
                                  "filhote": tex.Paleta(PELO=(200, 190, 180))})
    r.ok("valida_paletas_disjuntas aceita duas folhas sem tom em comum")
    r.deve_reprovar("duas folhas do mesmo mob compartilhando um tom",
                    lambda: tex.valida_paletas_disjuntas(
                        {"adulto": PALETA, "filhote": tex.Paleta(PELO=PALETA.DORSO)}),
                    "deixam de se distinguir")


def a_regua_que_mede_as_reguas(r):
    """Alimenta `deve_reprovar` com os dois jeitos de ele mentir, e confere.

    Um verificador que sempre diz 'ok' e o defeito mais caro possivel aqui: ele
    carimba as dez reguas de uma vez e ninguem descobre, porque o autoteste
    passa. Entao ele tambem e medido -- com um caso que nao levanta nada e um que
    levanta pela razao errada.
    """
    espelho = Relatorio()
    espelho.deve_reprovar("caso que nao levanta nada", lambda: None, "qualquer coisa")
    espelho.deve_reprovar("caso que levanta pela razao errada",
                          lambda: modelo(caixas=caixas_com("cauda", u=60, v=60)).validar(),
                          "cai fora de todo cubo do pai")
    r.confere("deve_reprovar() reprova quem nao levanta e quem levanta errado",
              espelho.falhas == 2, "%d de 2 falhas detectadas" % espelho.falhas)


def caixas_com(nome, **campos):
    """Copia a tabela trocando campos de UMA caixa. As outras cinco ficam."""
    return tuple(c._replace(**campos) if c.nome == nome else c for c in CAIXAS)


def ossos_com(nome, **campos):
    return tuple(o._replace(**campos) if o.nome == nome else o for o in OSSOS)


def casos_que_devem_reprovar(r, geometria):
    # (1) osso orfao: o GeckoLib nao reclama, so deixa o membro parado.
    r.deve_reprovar("osso orfao (pai inexistente)",
                    lambda: modelo(ossos=ossos_com("head", pai="cranio")).validar(),
                    "aponta para pai inexistente")

    # (2) caixa fora do atlas: o que sai da folha vira amostragem da borda.
    r.deve_reprovar("caixa fora do atlas",
                    lambda: modelo(caixas=caixas_com("cauda", u=60, v=60)).validar(),
                    "estoura o atlas")

    # (3) caixas sobrepostas: as duas leem o mesmo pixel.
    r.deve_reprovar("caixas sobrepostas no atlas",
                    lambda: modelo(caixas=caixas_com("cauda", u=0, v=0)).validar(),
                    "se sobrepoem no atlas")

    # (4) modelo maior que a hitbox: a silhueta promete o que o servidor nao da.
    r.deve_reprovar("modelo maior que a hitbox",
                    lambda: modelo(caixas=caixas_com("cabeca", y=20)).validar(),
                    "px de altura e a hitbox tem")

    # (5) pivot fora do volume do pai: gira em torno de um ponto que nao existe.
    r.deve_reprovar("pivot de filho fora do pai",
                    lambda: modelo(ossos=ossos_com("head", pivot=(0, 40, -4))).validar(),
                    "cai fora de todo cubo do pai")

    # (6) z-fighting: mesma face, mesmo plano, mesma direcao, area em comum.
    r.deve_reprovar("faces coplanares (z-fighting)",
                    lambda: modelo(caixas=caixas_com("cauda", y=10, z=2)).validar(),
                    "virada para o mesmo lado")

    def osso_fantasma():
        a = clipes(geometria)
        extra = {}
        anim.curva(extra, "asa_esquerda", "rotation", [(0.0, anim.vetor(x=10))])
        a.clipes[a.nome_completo("idle")]["bones"].update(extra)
        a.validar(ataques=ATAQUES)

    # (7) a animacao cita osso que a geometria nao tem.
    r.deve_reprovar("animacao move osso que o geo nao tem", osso_fantasma,
                    "move osso que a geometria nao tem")

    # (8) o clipe acaba antes do golpe: o bicho relaxa no meio do ataque. O clipe
    # curto e MONTADO curto, e nao truncado -- ver a nota em `clipes`.
    r.deve_reprovar("clipe de ataque mais curto que o servidor",
                    lambda: clipes(geometria, dur_attack=0.4).validar(ataques=ATAQUES),
                    "o servidor gasta")

    # (9) pincel vazando da face: cai na face vizinha e o atlas nao acusa.
    def pincel_vazando():
        f = tex.Folha(UV, CAIXAS)
        f["cabeca"].na_face("frente", 0, 0, 99, 1, PALETA.UNHA)

    r.deve_reprovar("pincel vazando para fora da face", pincel_vazando,
                    "nao cabe em")

    # (10) face sem tinta: um buraco no bicho, visto de um angulo so.
    def face_esquecida():
        f = tex.Folha(UV, CAIXAS)
        for c in CAIXAS:
            for face in ("topo", "base", "frente", "tras", "direita"):
                f[c.nome].face(face, PALETA.COURO)   # falta a 'esquerda'
        f.valida_sem_buraco()

    r.deve_reprovar("face sem tinta (buraco no bicho)", face_esquecida,
                    "ficou sem tinta")


# ----------------------------------------------------------------- o main

def main():
    r = Relatorio()
    raiz = tempfile.mkdtemp(prefix="autoteste_arte_")
    try:
        # --- primeira execucao -------------------------------------------
        um = os.path.join(raiz, "um")
        caminho_geo, caminho_anim, caminho_png = gerar(um)

        # --- geo valido e com todos os ossos ------------------------------
        with open(caminho_geo, encoding="utf-8") as arquivo:
            documento = json.load(arquivo)
        bloco = documento["minecraft:geometry"][0]
        nomes = [b["name"] for b in bloco["bones"]]
        r.confere("geo.json e JSON valido",
                  documento["format_version"] == "1.12.0", "format_version 1.12.0")
        r.confere("geo declara os %d ossos do contrato" % len(OSSOS),
                  nomes == [o.nome for o in OSSOS], ", ".join(nomes))
        r.confere("identifier derivado do nome do arquivo",
                  bloco["description"]["identifier"] == "geometry." + MOB,
                  bloco["description"]["identifier"])
        cubos = sum(len(b.get("cubes", [])) for b in bloco["bones"])
        r.confere("geo declara as %d caixas do contrato" % len(CAIXAS),
                  cubos == len(CAIXAS), "%d cubos" % cubos)
        r.confere("texture_width/height batem com o atlas declarado",
                  (bloco["description"]["texture_width"],
                   bloco["description"]["texture_height"]) == UV, "%dx%d" % UV)

        # --- animacao valida ----------------------------------------------
        with open(caminho_anim, encoding="utf-8") as arquivo:
            animacoes = json.load(arquivo)
        r.confere("animation.json e JSON valido",
                  animacoes["format_version"] == "1.8.0", "format_version 1.8.0")
        esperado = ["animation.%s.%s" % (MOB, c) for c in LOOPS]
        r.confere("animation declara os %d clipes do contrato" % len(LOOPS),
                  list(animacoes["animations"]) == esperado, ", ".join(esperado))
        do_geo = set(nomes)
        citados = set()
        for clipe in animacoes["animations"].values():
            citados |= set(clipe["bones"])
        r.confere("todo osso citado pela animacao existe no geo",
                  citados <= do_geo, "%d ossos citados" % len(citados))
        attack = animacoes["animations"]["animation.%s.attack" % MOB]
        r.confere("o clipe de ataque cobre os %d ticks do servidor" % ATAQUE.ticks,
                  attack["animation_length"] * TICKS_POR_SEGUNDO + 1e-9 >= ATAQUE.ticks,
                  "%.2fs >= %.2fs" % (attack["animation_length"], ATAQUE.segundos))
        r.confere("o loop mora no arquivo, e nao no codigo Java",
                  attack["loop"] == "hold_on_last_frame", repr(attack["loop"]))

        # --- PNG com o tamanho de atlas declarado -------------------------
        from PIL import Image
        with Image.open(caminho_png) as imagem:
            tamanho, modo = imagem.size, imagem.mode
        r.confere("PNG tem o tamanho de atlas declarado no geo",
                  tamanho == UV, "%dx%d, modo %s" % (tamanho[0], tamanho[1], modo))

        # --- duas execucoes, bytes identicos ------------------------------
        dois = os.path.join(raiz, "dois")
        segundos = gerar(dois)
        for rotulo, a, b in (("geo.json", caminho_geo, segundos[0]),
                             ("animation.json", caminho_anim, segundos[1]),
                             ("adulto.png", caminho_png, segundos[2])):
            with open(a, "rb") as fa, open(b, "rb") as fb:
                bytes_a, bytes_b = fa.read(), fb.read()
            r.confere("duas execucoes produzem o mesmo %s" % rotulo,
                      bytes_a == bytes_b, "%d bytes" % len(bytes_a))

        # --- e os casos que DEVEM reprovar --------------------------------
        geometria = anim.carregar_geo(MOB, um)
        a_regua_que_mede_as_reguas(r)
        casos_que_devem_reprovar(r, geometria)
        a_superficie_que_o_leia_me_promete(r, geometria)

        # --- o relato sai no terminal, e ele e para ser olhado -------------
        silencio = io.StringIO()
        anterior, sys.stdout = sys.stdout, silencio
        try:
            gerar(os.path.join(raiz, "tres"), silencioso=False)
        finally:
            sys.stdout = anterior
        r.confere("emitir() relata no terminal o que escreveu",
                  silencio.getvalue().count("escrito") == 3,
                  "%d linhas de relato" % len(silencio.getvalue().splitlines()))
    finally:
        for pasta, _, arquivos in os.walk(raiz, topdown=False):
            for arquivo in arquivos:
                os.remove(os.path.join(pasta, arquivo))
            os.rmdir(pasta)

    print("AUTOTESTE DA BIBLIOTECA COMUM DE ARTE")
    print("bicho de prova: %d ossos, %d caixas, %d clipes, atlas %dx%d, hitbox %s"
          % (len(OSSOS), len(CAIXAS), len(LOOPS), UV[0], UV[1], HITBOX_BLOCOS))
    print("")
    for linha in r.linhas:
        print(linha)
    print("")
    print("%d verificacoes, %d falha(s)" % (len(r.linhas), r.falhas))
    if r.falhas:
        print("A BIBLIOTECA NAO ESTA PRONTA: uma regua acima nao mediu o que promete.")
        return 1
    print("Sem prova aqui: que o bicho PARECE o bicho. Ver o relato e")
    print("docs/testing/o-que-nao-provamos.md.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
