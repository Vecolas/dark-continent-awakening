"""Biblioteca comum dos geradores de arte dos inimigos (Bedrock, GeckoLib 4.8.3).

POR QUE ESTA BIBLIOTECA EXISTE. Os sete primeiros mobs foram escritos com o
pincel, o ruido, o layout de caixa e os portoes COPIADOS de arquivo em arquivo --
18.814 linhas, das quais boa parte e a mesma coisa dita sete vezes. Isso foi
deliberado enquanto eram poucos: quatro usos com paletas diferentes nao pagavam a
indirecao. Faltam dezessete mobs, e a conta virou. O que decide a troca nao e o
tamanho: e que a duplicacao ja comecou a DIVERGIR. Hoje

  * `valida_sem_buraco` existe em tres mobs e falta em quatro -- os quatro sem
    ela podem ter uma face sem tinta agora, e ninguem saberia;
  * `valida_pivots` e `valida_faces_coplanares` existem em UM mob;
  * o mesmo metodo se chama `faixa_no_topo_da_face` em dois arquivos e
    `faixa_no_topo` nos outros cinco;
  * o gerador de animacao do great_stamp le o geo SE ele existir, e os outros
    exigem o arquivo.

Nenhuma dessas divergencias da erro. Cada uma delas e uma regua que um mob tem e
o vizinho nao -- e o mob sem a regua sai verde exatamente porque ninguem o mede.

ENTAO O QUE SOBE PARA CA E O QUE SOBRA LA. Sobe o que e do FORMATO: o layout de
caixa do Bedrock, a serializacao, a conta do atlas, a hierarquia de ossos, o
ruido deterministico, o esqueleto dos portoes. Fica no arquivo do mob tudo que e
do BICHO: as tabelas OSSOS e CAIXAS, a paleta, o script de pintura, os limiares
nomeados (0.62 da testa, 0.55 da boca, 1.6 da envergadura) e as validacoes
semanticas que os cobram. Essas validacoes sao o valor do projeto; a biblioteca
existe para elas CABEREM no arquivo do mob em vez de disputarem espaco com
seiscentas linhas de encanamento.

A BIBLIOTECA NAO PODE TER NUMERO DE DESIGN. `PX_POR_BLOCO` e `TICKS_POR_SEGUNDO`
sao do Minecraft; largura de hitbox, tick de windup e fracao minima de boca sao do
bicho e ficam onde o comentario que os explica esta. Um numero de balanceamento
escondido aqui viraria um botao que sete arquivos giram sem saber.

A TABELA DE CAIXAS CONTINUA SENDO UNICA, E AGORA POR CONSTRUCAO. O modulo de
textura NAO declara caixa: ele recebe a mesma tupla que o modulo de geometria
validou e emitiu. Duas tabelas divergem na primeira correcao de modelo e a
divergencia nao da erro -- da face pintada no lugar errado, que so aparece na
tela.

DETERMINISMO E REQUISITO, NAO ESTILO. Nao ha `random` sem semente, nao ha
`time`, nao ha iteracao sobre `set` na hora de escrever. Rodar o gerador duas
vezes tem de produzir bytes identicos, porque a saida e VERSIONADA: um PNG que
muda sozinho enche todo diff de ruido e, no dia em que ele mudar de verdade,
ninguem vai reparar.

Convencao de eixos (igual a vanilla): y=0 e o chao, -Z e a FRENTE, +X e o lado
ESQUERDO do bicho. 16 px = 1 bloco.

Layout de caixa do Bedrock -- para um cubo w x h x d a partir de (u,v), a caixa
ocupa (2d + 2w) x (d + h) px:

    topo     (u+d,       v)     w x d
    base     (u+d+w,     v)     w x d
    direita  (u,         v+d)   d x h   (lado -X; a FRENTE fica na borda direita)
    frente   (u+d,       v+d)   w x h   (face -Z)
    esquerda (u+d+w,     v+d)   d x h   (lado +X; a FRENTE fica na borda esquerda)
    tras     (u+2d+w,    v+d)   w x h   (face +Z)

Esta tabela e escrita UMA vez, em `textura.Pincel.faces`, e medida UMA vez, em
`geometria.area_no_atlas`. O portao Java (`CoerenciaDeGeckoLibTest.uvNaoSeAtropela`)
faz a mesma conta pela terceira vez, do lado dele, de proposito: sao as duas
pontas de um portao que morde dos dois lados.

Como usar: ver LEIA-ME.md nesta pasta.
Como provar que a biblioteca funciona: python art-source/comum/autoteste.py
"""
import os

# Do Minecraft, nao do bicho. Mudar qualquer um destes dois nao e balanceamento:
# e trocar de jogo.
PX_POR_BLOCO = 16.0
TICKS_POR_SEGUNDO = 20.0

# Versoes de formato. Elas DIVERGEM de proposito -- geometria fala 1.12.0 e
# animacao fala 1.8.0. Igualar as duas "por coerencia" faz o GeckoLib recusar o
# arquivo em runtime, e o sintoma e o mob aparecer parado, sem log nenhum.
FORMATO_GEOMETRIA = "1.12.0"
FORMATO_ANIMACAO = "1.8.0"

# A ordem das faces e fixa porque ela atravessa o arquivo gerado: iterar um
# conjunto nao-ordenado aqui faria a mesma folha sair com pixels em ordem
# diferente a cada execucao, e o diff da PNG deixaria de significar alguma coisa.
FACES = ("topo", "base", "direita", "frente", "esquerda", "tras")

# As quatro faces verticais. Separadas porque quase todo esquema de pintura trata
# topo e base (dorso e ventre) de um jeito e as paredes de outro.
PAREDES = ("frente", "tras", "direita", "esquerda")

# Os dois flancos. Na face 'direita' a FRENTE do bicho fica na borda DIREITA do
# retangulo, e na 'esquerda' ela fica na borda ESQUERDA. Copiar a mesma coluna
# nos dois lados nao da erro: da um bicho vesgo de um lado so, e so quem girar a
# camera em volta dele descobre.
LADOS = ("direita", "esquerda")

# Onde a saida mora dentro do resource pack do mod. Caminho nao e numero de
# design: se ele mudar, muda para todo mundo, e muda aqui.
ASSETS = ("src", "main", "resources", "assets", "nenfoundation")


class ErroDeArte(ValueError):
    """Toda recusa desta biblioteca.

    E uma `ValueError` de proposito: os geradores antigos de geo e textura ja
    levantavam `ValueError` e os de animacao levantavam `SystemExit`. As duas
    convencoes conviviam, e a diferenca importa -- `SystemExit` nao e pego por
    `except Exception`, entao um portao que envolvesse os geradores num laco
    engoliria as recusas de geometria e deixaria passar as de animacao (ou o
    contrario, dependendo de como o laco fosse escrito). Um tipo so, e ele herda
    de `ValueError` para que quem ja escreveu `except ValueError` continue
    funcionando.
    """


def caminho_geo(mob, raiz="."):
    return os.path.join(raiz, *(ASSETS + ("geo", "entity", mob + ".geo.json")))


def caminho_animacao(mob, raiz="."):
    return os.path.join(raiz, *(ASSETS + ("animations", "entity",
                                          mob + ".animation.json")))


def caminho_textura(mob, arquivo="adulto.png", raiz="."):
    return os.path.join(raiz, *(ASSETS + ("textures", "entity", mob, arquivo)))


def escrever_texto(destino, texto):
    """Escreve UTF-8 com \\n, sem BOM, com quebra de linha final.

    `newline="\\n"` e obrigatorio: no Windows o padrao vira CRLF, e um arquivo
    que nasce LF na maquina de uma pessoa e CRLF na da outra aparece no diff como
    o arquivo inteiro reescrito -- sem que uma linha de conteudo tenha mudado.
    """
    pasta = os.path.dirname(destino)
    if pasta:
        os.makedirs(pasta, exist_ok=True)
    with open(destino, "w", encoding="utf-8", newline="\n") as saida:
        saida.write(texto + "\n")
    return destino


def em_px(blocos):
    """Converte a medida que a entidade Java declara (em blocos) para pixels.

    O mob copia o literal exato da chamada -- `sized(1.9F, 1.55F)` -- em vez de
    ja escrever 30.4: assim o numero no gerador e reconhecivel ao lado do numero
    no Java, e quem mudar a hitbox la acha o gerador aqui procurando por '1.9'.
    """
    return blocos * PX_POR_BLOCO
