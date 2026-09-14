"""Identidade sonora dos inimigos (EN13 / issue #148).

CINCO SONS POR BICHO: ambiente, alerta, ataque, dor e morte. A lista nao e
arbitraria -- ela e o minimo para o jogador SABER, sem olhar, o que esta
acontecendo:

  ambiente  onde ele esta, antes de voce ver
  alerta    ele percebeu voce  -- e a unica chance de recuar
  ataque    o golpe COMECOU    -- casa com o WINDUP, nao com o dano
  dor       voce acertou       -- a confirmacao que a barra de vida nao da
  morte     acabou

O ALERTA E O ATAQUE SAO OS DOIS QUE IMPORTAM. Um mob sem alerta ataca do nada; um
mob cujo som de ataque toca junto com o dano avisa quando ja nao da tempo. Nenhum
dos dois produz erro: produzem um jogo em que o jogador leva dano sem entender de
onde veio, e conclui que o combate e injusto.

TUDO E SINTETIZADO. Nenhuma amostra, de lugar nenhum (ADR-007). O gerador esta no
git, e por isso o som pode ser corrigido -- uma amostra binaria sem fonte e um
arquivo que ninguem consegue mexer depois.

Regerar:  python art-source/sons/inimigos.py
Exporta:  src/main/resources/assets/nenfoundation/sounds/entity/<id>/<evento>.ogg
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import sintese as s  # noqa: E402

DESTINO = os.path.join('src', 'main', 'resources', 'assets', 'nenfoundation',
                       'sounds', 'entity')

EVENTOS = ('ambient', 'alert', 'attack', 'hurt', 'death')

# (id, arquetipo, altura de voz em Hz, semente)
#
# AS ALTURAS FORAM REESPACADAS PELA PROPRIA REGUA. A primeira versao desta tabela
# punha o besouro em 88 Hz e a centopeia em 92 -- 4.5% de diferenca, que o ouvido
# nao separa. Ninguem teria notado tocando os dois em momentos diferentes; o
# sintoma seria um bestiario que soa todo igual, e ninguem abre bug sobre isso.
#
# A ALTURA E A FICHA DO BICHO, e nao gosto: o ouvido estima TAMANHO por
# frequencia antes de qualquer outra coisa. Um ciclope de 4 blocos com voz de
# 200 Hz le como bicho pequeno gritando, e a silhueta passa a discordar do som --
# o jogador confia no som, e erra a distancia de recuo.
VOZES = [
    # ------------------------------------------------ exame hunter (EN2/EN3)
    ("great_stamp", "grande", 74, 11),
    ("foxbear", "medio", 140, 12),
    ("frog_in_waiting", "umido", 120, 13),
    ("man_faced_ape", "medio", 166, 14),
    ("spider_eagle", "aereo", 420, 15),
    ("master_of_the_swamp", "umido", 95, 16),
    ("kiriko", "medio", 152, 17),
    # ----------------------------------------------------- ferramenta (EN1)
    ("dummy_enemy", "madeira", 210, 18),
    # ---------------------------------------------------- greed island (EN6)
    ("cyclops", "grande", 62, 21),
    ("hyper_puffball", "fungo", 260, 22),
    ("melanin_lizard", "medio", 197, 23),
    ("radio_rat", "agudo", 900, 24),
    ("bubble_horse", "umido", 230, 25),
    ("king_white_stag_beetle", "grande", 84, 26),
    ("wolf_pack_hunter", "medio", 234, 27),
    # ------------------------------------------------- chimera peons (EN8)
    ("crab_heavy", "grande", 110, 31),
    ("bat_scout", "agudo", 1150, 32),
    ("wolf_runner", "medio", 215, 33),
    # ---------------------------------------------- chimera officers (EN11)
    ("spider_webber", "aereo", 330, 41),
    ("mosquito_officer", "agudo", 780, 42),
    ("multiarm_centipede", "grande", 96, 43),
    ("cheetah_leader", "medio", 181, 44),
    ("scorpion_leader", "grande", 126, 45),
    ("avian_commander", "aereo", 300, 46),
]

# Duracao por evento, em segundos.
#
# O ATAQUE E O MAIS CURTO de proposito: ele casa com o WINDUP, e um som de ataque
# mais longo que o telegrafo continuaria tocando enquanto o golpe ja acertou --
# o jogador ouviria "vai atacar" depois de ter apanhado.
DURACAO = {"ambient": 1.4, "alert": 0.7, "attack": 0.45, "hurt": 0.35, "death": 1.1}


def voz(evento):
    return s.Voz(DURACAO[evento])


def _grande(evento, altura, semente):
    v = voz(evento)
    if evento == "ambient":
        return v.misturar((1.0, v.rugido(altura * 0.8, 0.18, semente)),
                          (0.25, v.chiado(semente + 1, brilho=0.8)))
    if evento == "alert":
        return v.misturar((1.0, v.rugido(altura * 1.15, 0.35, semente + 2)),
                          (0.3, v.estalo(altura * 4, 22.0)))
    if evento == "attack":
        return v.misturar((1.0, v.rugido(altura * 1.3, 0.5, semente + 3)),
                          (0.45, v.estalo(altura * 6, 30.0)))
    if evento == "hurt":
        return v.misturar((1.0, v.rugido(altura * 1.5, 0.6, semente + 4)))
    return v.misturar((1.0, v.varredura(altura * 1.2, altura * 0.55, curva=1.8)),
                      (0.4, v.rugido(altura * 0.7, 0.3, semente + 5)))


def _medio(evento, altura, semente):
    v = voz(evento)
    if evento == "ambient":
        return v.misturar((1.0, v.rugido(altura * 0.9, 0.22, semente)))
    if evento == "alert":
        return v.misturar((1.0, v.rugido(altura * 1.2, 0.42, semente + 2)),
                          (0.35, v.guincho(altura * 3, altura * 4.2, semente + 6)))
    if evento == "attack":
        return v.misturar((1.0, v.rugido(altura * 1.35, 0.55, semente + 3)),
                          (0.4, v.estalo(altura * 5, 34.0)))
    if evento == "hurt":
        return v.misturar((1.0, v.guincho(altura * 3.4, altura * 1.8, semente + 4)),
                          (0.5, v.rugido(altura * 1.4, 0.5, semente + 7)))
    return v.misturar((1.0, v.varredura(altura * 2.4, altura * 0.7, curva=2.0)),
                      (0.35, v.chiado(semente + 5, brilho=0.5)))


def _agudo(evento, altura, semente):
    v = voz(evento)
    if evento == "ambient":
        return v.misturar((1.0, v.guincho(altura * 0.9, altura * 1.05, semente)),
                          (0.2, v.chiado(semente + 1, brilho=-0.2)))
    if evento == "alert":
        return v.misturar((1.0, v.guincho(altura * 0.8, altura * 1.6, semente + 2)))
    if evento == "attack":
        return v.misturar((1.0, v.guincho(altura * 1.4, altura * 0.9, semente + 3)),
                          (0.3, v.estalo(altura * 1.2, 60.0)))
    if evento == "hurt":
        return v.misturar((1.0, v.guincho(altura * 1.7, altura * 0.8, semente + 4)))
    return v.misturar((1.0, v.varredura(altura * 1.3, altura * 0.35, curva=2.2)))


def _aereo(evento, altura, semente):
    v = voz(evento)
    if evento == "ambient":
        return v.misturar((1.0, v.chiado(semente, brilho=0.1, pulsos=7)),
                          (0.35, v.guincho(altura * 0.8, altura, semente + 1)))
    if evento == "alert":
        return v.misturar((1.0, v.guincho(altura, altura * 1.8, semente + 2)),
                          (0.4, v.chiado(semente + 3, brilho=0.0, pulsos=11)))
    if evento == "attack":
        return v.misturar((1.0, v.chiado(semente + 4, brilho=-0.3, pulsos=16)),
                          (0.5, v.guincho(altura * 1.6, altura * 0.9, semente + 5)))
    if evento == "hurt":
        return v.misturar((1.0, v.guincho(altura * 1.9, altura * 0.9, semente + 6)))
    return v.misturar((1.0, v.varredura(altura * 1.5, altura * 0.4, curva=2.0)),
                      (0.4, v.chiado(semente + 7, brilho=0.4, pulsos=4)))


def _umido(evento, altura, semente):
    v = voz(evento)
    if evento == "ambient":
        return v.misturar((1.0, v.borbulha(semente, densidade=10)),
                          (0.4, v.rugido(altura * 0.7, 0.25, semente + 1)))
    if evento == "alert":
        return v.misturar((1.0, v.borbulha(semente + 2, densidade=22)),
                          (0.5, v.rugido(altura, 0.4, semente + 3)))
    if evento == "attack":
        return v.misturar((1.0, v.rugido(altura * 1.2, 0.5, semente + 4)),
                          (0.6, v.borbulha(semente + 5, densidade=18)))
    if evento == "hurt":
        return v.misturar((1.0, v.rugido(altura * 1.4, 0.55, semente + 6)),
                          (0.4, v.borbulha(semente + 7, densidade=8)))
    return v.misturar((1.0, v.borbulha(semente + 8, densidade=26)),
                      (0.5, v.varredura(altura * 1.2, altura * 0.5, curva=1.9)))


def _fungo(evento, altura, semente):
    v = voz(evento)
    if evento == "ambient":
        return v.misturar((1.0, v.chiado(semente, brilho=0.9)))
    if evento == "alert":
        return v.misturar((1.0, v.chiado(semente + 2, brilho=0.5, pulsos=5)),
                          (0.3, v.guincho(altura, altura * 1.3, semente + 3)))
    if evento == "attack":
        return v.misturar((1.0, v.chiado(semente + 4, brilho=-0.5)),
                          (0.5, v.estalo(altura * 2, 18.0)))
    if evento == "hurt":
        return v.misturar((1.0, v.chiado(semente + 5, brilho=0.2, pulsos=9)))
    # O estouro: o unico som do jogo que E o gameplay, e nao o acompanha.
    return v.misturar((1.0, v.chiado(semente + 6, brilho=-0.8)),
                      (0.8, v.estalo(altura * 0.6, 9.0)),
                      (0.4, v.varredura(altura * 3, altura * 0.5, curva=2.4)))


def _madeira(evento, altura, semente):
    v = voz(evento)
    if evento == "ambient":
        return v.misturar((1.0, v.estalo(altura * 0.5, 6.0)),
                          (0.3, v.chiado(semente, brilho=1.1)))
    if evento == "alert":
        return v.misturar((1.0, v.estalo(altura, 14.0)), (0.5, v.estalo(altura * 1.6, 20.0)))
    if evento == "attack":
        return v.misturar((1.0, v.estalo(altura * 1.4, 26.0)),
                          (0.4, v.chiado(semente + 1, brilho=-0.2)))
    if evento == "hurt":
        return v.misturar((1.0, v.estalo(altura * 2.1, 34.0)))
    return v.misturar((1.0, v.estalo(altura * 0.7, 8.0)),
                      (0.6, v.chiado(semente + 2, brilho=0.7)))


ARQUETIPOS = {
    "grande": _grande, "medio": _medio, "agudo": _agudo,
    "aereo": _aereo, "umido": _umido, "fungo": _fungo, "madeira": _madeira,
}


def valida_alturas():
    """Duas vozes iguais demais deixam de identificar o bicho.

    Se dois mobs do MESMO arquetipo tem alturas quase iguais, o jogador nao
    consegue distinguir um do outro sem olhar -- e o som deixa de informar
    qualquer coisa. Isso nao da erro: da um bestiario que soa todo igual.
    """
    por_arquetipo = {}
    for mob, arq, altura, _s in VOZES:
        por_arquetipo.setdefault(arq, []).append((mob, altura))
    for arq, vozes in por_arquetipo.items():
        vozes.sort(key=lambda par: par[1])
        for (a, fa), (b, fb) in zip(vozes, vozes[1:]):
            razao = fb / float(fa)
            if razao < 1.06:
                raise s.ErroDeSom(
                    "'%s' (%d Hz) e '%s' (%d Hz) sao do arquetipo '%s' e estao a %.1f%% um do "
                    "outro: o jogador nao distingue os dois sem olhar, e o som deixa de "
                    "informar" % (a, fa, b, fb, arq, (razao - 1) * 100))


def main():
    valida_alturas()
    escritos = 0
    for mob, arquetipo, altura, semente in VOZES:
        gerador = ARQUETIPOS[arquetipo]
        for evento in EVENTOS:
            onda = gerador(evento, float(altura), semente)
            caminho = os.path.join(DESTINO, mob, evento + '.ogg')
            tamanho = s.valida_vorbis(s.escrever(caminho, onda))
            escritos += 1
        print("  %-24s %-8s %5d Hz   %d sons" % (mob, arquetipo, altura, len(EVENTOS)))
    print("\n%d arquivos .ogg escritos em %s" % (escritos, DESTINO))
    print("Sem prova aqui: que o som PARECE o bicho. Nenhuma regua ouve.")


if __name__ == '__main__':
    main()
