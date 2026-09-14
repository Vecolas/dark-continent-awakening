"""Textura: pincel preso a caixa, paleta nomeada, ruido deterministico, PNG.

A TABELA DE CAIXAS NAO NASCE AQUI, E NAO PODE NASCER. A `Folha` recebe a MESMA
tupla `CAIXAS` que o modulo de geometria validou e emitiu. Se cada gerador
tivesse a sua, os dois divergiriam na primeira correcao de modelo, e o sintoma
nao seria erro nenhum -- seria uma face pintada no lugar errado, visivel so na
tela e so de um angulo. O jeito de garantir isso e estrutural: o arquivo do mob
importa CAIXAS do modulo de geometria dele e passa a mesma referencia para ca.

DUAS COORDENADAS, E CONFUNDI-LAS E O ERRO QUE NAO DA ERRO
----------------------------------------------------------
`Pincel.retangulo` fala em coordenada da FOLHA e so garante o retangulo da caixa.
`Pincel.na_face` fala em (coluna, linha) DENTRO de uma face, e e ele que impede o
defeito caro: um olho pintado 2 px fora cai na face vizinha, e o portao de
sobreposicao nem pisca, porque continua dentro da mesma caixa. Tudo que desenha
detalhe usa `na_face` e derivados; `retangulo` existe para os derivados e para
quem tiver motivo declarado.

O RUIDO E DETERMINISTICO, E ISSO E REQUISITO
---------------------------------------------
Cor chapada le como plastico a distancia, entao a folha precisa de variacao. Mas
a saida e um binario VERSIONADO: `random` sem semente faria a PNG mudar a cada
execucao, todo diff viraria ruido, e no dia em que a textura mudasse de verdade
ninguem repararia. Por isso a variacao vem de um hash da propria coordenada --
mesma posicao, mesma cor, sempre, em qualquer maquina.

O hash e embaralhado de proposito. Combinacao linear simples (x*7 + y*13) produz
FAIXAS DIAGONAIS regulares, que a distancia leem como listra e nao como pelo,
pena ou rocha -- e isso so aparece na tela.

O QUE ESTE MODULO NAO PROVA: se a folha e bonita, se ha um unico contraste alto,
se os pretos foram contados. Isso e leitura, e leitura mora no arquivo do mob --
onde a paleta tem, por item, o comentario dizendo que PAPEL aquela cor cumpre, e
nao que cor ela e.
"""
import os

from PIL import Image

from . import ErroDeArte, FACES, LADOS, PAREDES, caminho_textura  # noqa: F401

# LADOS e PAREDES sao reexportados de proposito: quase todo script de pintura os
# usa, e obrigar cada mob a importar de dois modulos convida a importar de um so
# e redeclarar o outro -- que e como a duplicacao recomeca.


def ruido(x, y, forca):
    """Variacao deterministica por pixel: delta em {-2..2} * forca // 2.

    Somado igualmente aos tres canais: mexe na luminancia e mantem o matiz. Uma
    variacao por canal mudaria a COR, e a paleta deixaria de dizer o que diz.
    `forca=0` desliga -- e o que se usa em pupila, narina e fenda de casco, onde
    o pixel precisa ser exato.
    """
    h = (x * 374761393 + y * 668265263) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((h >> 7) % 5 - 2) * forca // 2


def sorteio(x, y, semente):
    """0..255 deterministico por pixel, para decidir onde cai um borrao.

    Semente separada do ruido, e por pixel e nao por chamada: assim salpicar a
    mesma face duas vezes com a mesma semente pinta os MESMOS pixels, e camadas
    diferentes usam sementes diferentes (semente, semente+7, semente+13).
    """
    h = (x * 2654435761 + y * 40503 + semente * 2246822519) & 0xFFFFFFFF
    h = (h ^ (h >> 15)) * 2246822519 & 0xFFFFFFFF
    return (h >> 13) & 0xFF


class Paleta:
    """Cores nomeadas por PAPEL de leitura.

    Existe por duas razoes. A primeira: um nome errado vira recusa com a lista de
    nomes conhecidos, em vez de um `NameError` no meio de trezentas linhas de
    pintura. A segunda: com a paleta como objeto, `valida_paletas_disjuntas`
    consegue comparar duas folhas do mesmo mob -- um tom que vaza de uma para a
    outra nao da erro, e faz as duas variantes do bicho pararem de se distinguir
    a distancia, que e o unico lugar onde a distincao serve.
    """

    def __init__(self, **cores):
        for nome, cor in cores.items():
            if nome != nome.upper():
                raise ErroDeArte("a cor '%s' devia estar em caixa alta: a paleta e constante, e "
                                 "minuscula no meio da pintura le como variavel local" % nome)
            if (not isinstance(cor, tuple) or len(cor) != 3
                    or any(not isinstance(v, int) or v < 0 or v > 255 for v in cor)):
                raise ErroDeArte("a cor '%s' e %r: cor aqui e uma tupla (r, g, b) de inteiros "
                                 "0..255. O alfa nao se declara -- ele e sempre 255, porque pixel "
                                 "semitransparente dentro de uma caixa vira buraco em jogo"
                                 % (nome, cor))
        self._cores = dict(cores)

    def __getattr__(self, nome):
        # `self.__dict__.get` e nao `self._cores`: dentro de __getattr__, tocar o
        # atributo pelo caminho normal reentra aqui quando ele ainda nao existe, e
        # a recursao infinita mascararia o nome errado que se quer relatar.
        cores = self.__dict__.get("_cores", {})
        if nome in cores:
            return cores[nome]
        raise ErroDeArte("a paleta nao tem a cor '%s'. Cores declaradas: %s"
                         % (nome, sorted(cores)))

    def __iter__(self):
        return iter(sorted(self._cores.items()))

    def cores(self):
        return set(self._cores.values())


def valida_paletas_disjuntas(paletas):
    """Nenhum tom em comum entre duas folhas do mesmo mob.

    `paletas` mapeia nome da folha para `Paleta`. Um tom que vaza de uma variante
    para a outra nao da erro: faz as duas pararem de se distinguir a distancia, e
    a distancia e onde a distincao precisa funcionar.
    """
    nomes = sorted(paletas)
    for i, a in enumerate(nomes):
        for b in nomes[i + 1:]:
            comuns = paletas[a].cores() & paletas[b].cores()
            if comuns:
                raise ErroDeArte("as folhas '%s' e '%s' compartilham %d tom(ns) %s: a distancia as "
                                 "duas variantes deixam de se distinguir, que e a unica coisa que "
                                 "a separacao de paleta entrega" % (a, b, len(comuns), sorted(comuns)))


class Pincel:
    """Pincel preso a UMA caixa. Pintar fora do retangulo dela e erro, nao aviso.

    O pincel recebe a folha em vez de escrever num global do modulo. Nos sete
    geradores antigos `retangulo` escrevia direto num `px` de nivel de modulo, o
    que amarrava um pincel a exatamente uma imagem por processo -- e tornava
    impossivel um mob com duas folhas, que e justamente o caso do kiriko.
    """

    def __init__(self, caixa, folha):
        self.c = caixa
        self.folha = folha
        self.x0, self.y0 = caixa.u, caixa.v
        self.x1 = caixa.u + 2 * caixa.d + 2 * caixa.w
        self.y1 = caixa.v + caixa.d + caixa.h

    def faces(self):
        """O layout de caixa do Bedrock, escrito UMA vez no repositorio inteiro."""
        c = self.c
        return {
            "topo": (c.u + c.d, c.v, c.w, c.d),
            "base": (c.u + c.d + c.w, c.v, c.w, c.d),
            "direita": (c.u, c.v + c.d, c.d, c.h),
            "frente": (c.u + c.d, c.v + c.d, c.w, c.h),
            "esquerda": (c.u + c.d + c.w, c.v + c.d, c.d, c.h),
            "tras": (c.u + 2 * c.d + c.w, c.v + c.d, c.w, c.h),
        }

    # -- pintura crua, em coordenada da FOLHA -------------------------------

    def retangulo(self, x, y, w, h, cor, forca=6):
        if w <= 0 or h <= 0:
            return
        if x < self.x0 or y < self.y0 or x + w > self.x1 or y + h > self.y1:
            raise ErroDeArte("pincel da caixa '%s' vazou para fora do retangulo dela: (%d,%d)+%dx%d "
                             "fora de (%d,%d)..(%d,%d). O que vaza cai na area de outra caixa e "
                             "aparece como a face errada no bicho errado"
                             % (self.c.nome, x, y, w, h, self.x0, self.y0, self.x1, self.y1))
        for i in range(x, x + w):
            for j in range(y, y + h):
                d = ruido(i, j, forca)
                self.folha.pintar(i, j, (cor[0] + d, cor[1] + d, cor[2] + d))

    # -- pintura em coordenada da FACE --------------------------------------
    # Tudo abaixo fala em (coluna, linha) DENTRO da face. E o que impede o erro
    # que nao da erro: um detalhe pintado 2 px fora cai na face vizinha e o
    # portao de sobreposicao nem pisca, porque continua dentro da mesma caixa.

    def na_face(self, nome, dx, dy, w, h, cor, forca=4):
        x, y, fw, fh = self._face(nome)
        if dx < 0 or dy < 0 or dx + w > fw or dy + h > fh:
            raise ErroDeArte("caixa '%s', face '%s': (%d,%d)+%dx%d nao cabe em %dx%d -- o excedente "
                             "cairia na face vizinha, e de um angulo so"
                             % (self.c.nome, nome, dx, dy, w, h, fw, fh))
        self.retangulo(x + dx, y + dy, w, h, cor, forca)

    def face(self, nome, cor, forca=6):
        x, y, w, h = self._face(nome)
        self.retangulo(x, y, w, h, cor, forca)

    def tudo(self, cor, forca=6):
        """As SEIS faces. E o primeiro gesto de toda peca, e nao e enfeite:
        pintar cinco de seis e a falha mais barata que existe aqui, e o sintoma e
        um buraco por onde se ve o interior do bicho. `Folha.valida_sem_buraco`
        reprova, mas so depois; comecar por `tudo` e o que faz nao acontecer."""
        for nome in FACES:
            self.face(nome, cor, forca)

    def linha(self, nome, dy, cor, forca=3):
        """Uma fiada horizontal contada a partir do TOPO da face."""
        _, _, w, _ = self._face(nome)
        self.na_face(nome, 0, dy, w, 1, cor, forca)

    def linha_central(self, nome, espessura, cor, forca=3):
        _, _, w, h = self._face(nome)
        self.na_face(nome, 0, (h - espessura) // 2, w, min(espessura, h), cor, forca)

    def coluna(self, nome, dx, cor, forca=3):
        _, _, _, h = self._face(nome)
        self.na_face(nome, dx, 0, 1, h, cor, forca)

    def faixa_no_topo(self, nome, linhas, cor, forca=4):
        _, _, w, h = self._face(nome)
        self.na_face(nome, 0, 0, w, min(linhas, h), cor, forca)

    def faixa_no_pe(self, nome, linhas, cor, forca=4):
        _, _, w, h = self._face(nome)
        n = min(linhas, h)
        self.na_face(nome, 0, h - n, w, n, cor, forca)

    def ponto(self, nome, dx, dy, cor):
        """Um pixel exato: forca 0, porque aqui o ruido seria o defeito."""
        self.na_face(nome, dx, dy, 1, 1, cor, 0)

    def salpicar(self, nome, cor, limiar, semente, forca=3):
        """Borroes de 1 px espalhados pela face, deterministicos.

        Mosqueado nao e enfeite: e o que faz o dorso ler como couro ou rocha e
        nao como um bloco pintado. `limiar` (0..255) e a densidade; `semente`
        separa camadas.
        """
        x, y, w, h = self._face(nome)
        for i in range(x, x + w):
            for j in range(y, y + h):
                if sorteio(i, j, semente) < limiar:
                    self.retangulo(i, j, 1, 1, cor, forca)

    def _face(self, nome):
        faces = self.faces()
        if nome not in faces:
            raise ErroDeArte("'%s' nao e uma face. As faces sao %s -- em portugues, porque sao "
                             "vocabulario do gerador e nao do formato" % (nome, list(FACES)))
        return faces[nome]


class Folha:
    """Uma PNG de atlas inteira, com um pincel por caixa.

    `caixas` e `uv` vem do modulo de geometria do mob, sempre. A folha nao
    declara nem deduz nada sobre a tabela: se ela pudesse, ja haveria duas.
    """

    def __init__(self, uv, caixas):
        self.largura, self.altura = uv
        self.caixas = tuple(caixas)
        nomes = [c.nome for c in self.caixas]
        repetidos = sorted({n for n in nomes if nomes.count(n) > 1})
        if repetidos:
            raise ErroDeArte("caixa com nome repetido: %s -- os pinceis sao indexados por nome, e o "
                             "segundo pincel pintaria por cima do primeiro sem nenhum aviso"
                             % repetidos)
        # Fundo transparente: pixel nao pintado FICA com alfa 0, e e assim que
        # `valida_sem_buraco` consegue enxergar a face esquecida. Um fundo opaco
        # esconderia exatamente o defeito que ela existe para achar.
        self.img = Image.new("RGBA", (self.largura, self.altura), (0, 0, 0, 0))
        self.px = self.img.load()
        self.pinceis = {c.nome: Pincel(c, self) for c in self.caixas}

    def __getitem__(self, nome):
        if nome not in self.pinceis:
            raise ErroDeArte("nao ha caixa '%s' nesta folha. Caixas: %s"
                             % (nome, sorted(self.pinceis)))
        return self.pinceis[nome]

    def pintar(self, x, y, cor):
        """Grava um pixel opaco, com os canais presos em 0..255.

        O alfa e 255 sempre. Semitransparencia dentro de uma caixa nao da erro:
        em jogo ela vira um pedaco do bicho por onde se ve o outro lado.
        """
        if not (0 <= x < self.largura and 0 <= y < self.altura):
            raise ErroDeArte("pixel (%d,%d) fora da folha %dx%d"
                             % (x, y, self.largura, self.altura))
        self.px[x, y] = (max(0, min(255, cor[0])),
                         max(0, min(255, cor[1])),
                         max(0, min(255, cor[2])), 255)

    def cada(self, nomes, funcao):
        """Aplica a mesma pintura a varias caixas, na ordem dada.

        Ordem dada, e nao ordem de conjunto: iterar um `set` aqui faria os pixels
        sairem em ordem diferente entre execucoes onde duas caixas se tocam.
        """
        for nome in nomes:
            funcao(self[nome])

    def valida_sem_buraco(self):
        """Nenhum pixel transparente DENTRO do retangulo de uma caixa.

        Face esquecida nao da erro e nao aparece no atlas aberto num editor --
        aparece como um buraco por onde se ve o interior do bicho, e so de um
        angulo. E a falha mais barata de cometer aqui: basta pintar cinco faces
        de seis.
        """
        for c in self.caixas:
            p = self.pinceis[c.nome]
            for nome in FACES:
                x, y, w, h = p.faces()[nome]
                for i in range(x, x + w):
                    for j in range(y, y + h):
                        if self.px[i, j][3] != 255:
                            raise ErroDeArte("a face '%s' da caixa '%s' ficou sem tinta em (%d,%d): "
                                             "em jogo isso e um buraco no bicho, visto de um "
                                             "angulo so" % (nome, c.nome, i, j))
        return self

    def validar(self, extras=()):
        self.valida_sem_buraco()
        for extra in extras:
            extra(self)
        return self

    def escrever(self, destino):
        """Salva a PNG.

        Nao ha metadado de data: o Pillow nao escreve o chunk tIME por conta
        propria, e nada aqui pede um. Se algum dia alguem passar `pnginfo` com
        hora, a PNG passa a mudar sozinha a cada geracao e o diff do binario
        deixa de significar alguma coisa.
        """
        pasta = os.path.dirname(destino)
        if pasta:
            os.makedirs(pasta, exist_ok=True)
        self.img.save(destino)
        return destino

    def resumo(self, destino):
        return "escrito %s %s\ncaixas pintadas: %d" % (destino, self.img.size,
                                                       len(self.pinceis))

    def emitir(self, mob, extras=(), arquivo="adulto.png", destino=None,
               raiz=".", silencioso=False):
        """Valida, escreve e relata. A recusa vem antes do arquivo: uma PNG meio
        pintada no disco passa no portao Java de dimensao e so falha na tela."""
        self.validar(extras)
        caminho = self.escrever(destino or caminho_textura(mob, arquivo, raiz))
        if not silencioso:
            print(self.resumo(caminho))
        return caminho
