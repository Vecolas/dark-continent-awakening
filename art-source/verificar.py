"""Reproduz TODA a arte e denuncia o que nao bate com o que esta no git.

POR QUE ISTO EXISTE. Os .geo.json, .animation.json, .png e .ogg sao binarios e
textos GERADOS, e eles estao versionados. Nada obriga o arquivo no git a ser o
que o gerador produz hoje: alguem edita o JSON a mao para resolver uma urgencia,
o gerador continua dizendo outra coisa, e a proxima execucao desfaz a correcao
sem avisar. Ou pior -- nao desfaz, porque ninguem roda o gerador de novo, e o
arquivo fica sendo a verdade enquanto o codigo que o explica vira ficcao.

Nenhum portao Java pega isso: eles conferem que o asset EXISTE e que ele e
coerente com o modelo, nunca que ele foi REPRODUZIDO.

Rodar:  python art-source/verificar.py
Saida:  0 se tudo reproduz; 1 e a lista do que mudou.

Ele nao escreve nada fora dos caminhos que os proprios geradores escrevem -- mas
ELE ESCREVE: a verificacao e "gerar por cima e comparar com o git". Rode com a
arvore limpa, ou o relato vai misturar o seu trabalho com a divergencia.
"""
import hashlib
import os
import subprocess
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENEMIES = os.path.join(RAIZ, 'art-source', 'enemies')

ASSETS = os.path.join(RAIZ, 'src', 'main', 'resources', 'assets', 'nenfoundation')
ALVOS = [
    os.path.join(ASSETS, 'geo', 'entity'),
    os.path.join(ASSETS, 'animations', 'entity'),
    os.path.join(ASSETS, 'textures', 'entity'),
    os.path.join(ASSETS, 'textures', 'item'),
    os.path.join(ASSETS, 'sounds', 'entity'),
]

# A ordem dentro de um mob e obrigatoria: o gerador de animacao LE o geo do disco.
ETAPAS = ('geo', 'animacoes', 'textura')


def instantaneo():
    """md5 de todo asset gerado, por caminho relativo."""
    mapa = {}
    for alvo in ALVOS:
        for pasta, _subs, arquivos in os.walk(alvo):
            for arquivo in arquivos:
                caminho = os.path.join(pasta, arquivo)
                with open(caminho, 'rb') as fonte:
                    mapa[os.path.relpath(caminho, RAIZ)] = hashlib.md5(fonte.read()).hexdigest()
    return mapa


def rodar(caminho):
    saida = subprocess.run([sys.executable, caminho], cwd=RAIZ,
                           capture_output=True, text=True)
    if saida.returncode != 0:
        print('  FALHOU %s' % os.path.relpath(caminho, RAIZ))
        print('    ' + (saida.stderr.strip().splitlines() or ['(sem stderr)'])[-1])
        return False
    return True


def main():
    if not os.path.isdir(ENEMIES):
        print('nao encontrei art-source/enemies')
        return 1

    antes = instantaneo()
    print('%d assets gerados no disco antes de reproduzir' % len(antes))

    falhas = []
    mobs = sorted(d for d in os.listdir(ENEMIES)
                  if os.path.isdir(os.path.join(ENEMIES, d)))
    for mob in mobs:
        for etapa in ETAPAS:
            caminho = os.path.join(ENEMIES, mob, '%s_%s.py' % (mob, etapa))
            if not os.path.isfile(caminho):
                # Etapa ausente NAO e erro: nem todo mob tem as tres (um mob de
                # duas folhas pode ter dois geradores de textura, por exemplo).
                # O que seria erro e a etapa existir e nao rodar.
                continue
            if not rodar(caminho):
                falhas.append('%s/%s' % (mob, etapa))

    sons = os.path.join(RAIZ, 'art-source', 'sons', 'inimigos.py')
    if os.path.isfile(sons) and not rodar(sons):
        falhas.append('sons/inimigos')

    item = os.path.join(RAIZ, 'art-source', 'items', 'greed_island_card_textura.py')
    if os.path.isfile(item) and not rodar(item):
        falhas.append('items/greed_island_card')

    depois = instantaneo()

    mudaram = sorted(c for c in depois if antes.get(c) != depois[c])
    sumiram = sorted(c for c in antes if c not in depois)

    print('')
    if falhas:
        print('GERADORES QUE FALHARAM (%d): %s' % (len(falhas), ', '.join(falhas)))
    if sumiram:
        print('ASSETS QUE SUMIRAM (%d):' % len(sumiram))
        for caminho in sumiram:
            print('  ' + caminho)
    if mudaram:
        print('ASSETS QUE NAO REPRODUZEM (%d):' % len(mudaram))
        for caminho in mudaram:
            print('  ' + caminho)
        print('')
        print('Ou o arquivo foi editado a mao, ou o gerador mudou e ninguem o rodou.')
        print('Nos dois casos o codigo que EXPLICA a arte divergiu da arte.')

    if falhas or sumiram or mudaram:
        return 1

    print('%d assets reproduzidos byte a byte. Nenhum divergiu.' % len(depois))
    print('')
    print('Sem prova aqui: que a arte esta BOA. Isto mede reproducao, e nao qualidade --')
    print('silhueta, leitura a distancia e timbre continuam sendo olho e ouvido humano.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
