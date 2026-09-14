"""Folha da Spider Webber.

A FIANDEIRA TEM DE SER A COISA MAIS CLARA DO BICHO, e essa e a unica exigencia de
leitura desta folha. Ela existe por causa da regra do servidor:

  * o unico ataque desta formiga e a teia, e ela tem um aviso de 30 ticks. O
    aviso e o abdome se erguendo para apontar a FIANDEIRA para a frente. Se a
    fiandeira tiver a mesma cor da quitina, o aviso existe so como movimento -- e
    movimento a nove blocos, contra um fundo de floresta, e exatamente o que um
    jogador em combate nao esta olhando;

  * a saida ATIVA da imobilizacao e acertar a fiandeira: oito de dano rasgam o
    fio. A mensagem diz ao jogador onde bater, e a folha tem de responder a
    pergunta "onde fica isso?" numa olhada, com o jogador parado e o esquadrao
    chegando.

Por isso a paleta tem UM claro e ele mora na fiandeira. Tudo o mais -- torax,
abdome, cabeca, patas -- e quitina escura em dois degraus, e o unico outro ponto
de cor sao os oito olhos, que sao minusculos de proposito: dois claros grandes
dividiriam a silhueta em duas leituras, e a leitura precisa ser uma so.

A tabela CAIXAS vem do modulo de geometria, e nao de uma copia: duas tabelas
divergem na primeira correcao de modelo, e a divergencia nao da erro -- da face
pintada no lugar errado, visivel so na tela e so de um angulo.

Regerar:  python art-source/enemies/spider_webber/spider_webber_textura.py
Exporta:  src/main/resources/assets/nenfoundation/textures/entity/spider_webber/adulto.png
"""
import os
import sys

sys.path.insert(0, os.path.abspath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comum import textura as tex        # noqa: E402
from spider_webber_geo import (CAIXAS, LADOS, MOB, PARES_DE_PATAS,   # noqa: E402
                              UV_ALTURA, UV_LARGURA)

PALETA = tex.Paleta(
    # a quitina: o bicho inteiro visto de longe. Roxo-acinzentado escuro, para
    # que o unico claro da folha nao tenha concorrencia nenhuma.
    QUITINA=(78, 62, 82),
    # um degrau acima, para o dorso do abdome: volume, e nao cor nova
    QUITINA_CLARA=(104, 86, 108),
    # o contorno das pecas e a ponta das patas -- quase preto
    BORDA=(34, 28, 36),
    # A FIANDEIRA. O unico claro da folha, e o que o jogador tem de achar em uma
    # olhada quando a mensagem manda ele bater nela.
    FIANDEIRA=(236, 240, 244),
    # a seda ja fiada, presa ao abdome e a ponta das patas: mais suja que a
    # fiandeira de proposito, para que a FONTE do fio continue sendo o ponto mais
    # claro do bicho
    SEDA=(170, 180, 190),
    # os oito olhos: ambar, pequenos, so para a cabeca ter onde ser lida
    OLHO=(214, 150, 60),
)

# Distancia minima (soma das diferencas de canal) entre a fiandeira pintada e o
# abdome pintado.
#
# Limite de LEITURA, e nao de balanceamento. Duzentos e quarenta e mais ou menos
# um degrau de valor inteiro em cada canal: abaixo disso as duas pecas viram a
# mesma mancha a nove blocos, que e o limite da faixa em que este mob opera. Nada
# no repositorio percebe isso sozinho -- o portao Java de coerencia so confere a
# DIMENSAO da folha, nunca o que esta pintado nela.
CONTRASTE_MINIMO_DA_FIANDEIRA = 240

folha = tex.Folha((UV_LARGURA, UV_ALTURA), CAIXAS)


def patas():
    for par in PARES_DE_PATAS:
        for lado in LADOS:
            yield par, lado


def pintar():
    # ---------------------------------------------------------- cefalotorax
    torax = folha["cefalotorax"]
    torax.tudo(PALETA.QUITINA)
    # O topo e mais escuro: e o que se ve de cima, e a distancia ele precisa
    # separar o torax do abdome, senao os dois leem como um corpo so e a silhueta
    # de aranha (dois volumes e um estrangulamento) desaparece.
    torax.face("topo", PALETA.BORDA)
    torax.salpicar("topo", PALETA.QUITINA, 40, semente=5)
    for face in ("frente", "tras", "direita", "esquerda"):
        torax.faixa_no_pe(face, 1, PALETA.BORDA)

    # ---------------------------------------------------------------- abdome
    abdome = folha["abdomen"]
    abdome.tudo(PALETA.QUITINA)
    abdome.face("topo", PALETA.QUITINA_CLARA)
    # O desenho dorsal em faixas. Ele e o que faz o abdome ler como abdome de
    # aranha e nao como um caixote -- e e ele que torna o GIRO do aviso visivel:
    # uma peca chapada girando 55 graus nao muda quase nada na tela.
    for dy in (1, 3, 5):
        abdome.linha("topo", dy, PALETA.BORDA, forca=2)
    # A seda fiada escorrendo do fundo do abdome em direcao a fiandeira: ela e a
    # seta que aponta para o ponto que a mensagem manda acertar.
    for dx in (3, 4, 5, 6):
        abdome.coluna("tras", dx, PALETA.SEDA, forca=2)
    for face in ("direita", "esquerda"):
        abdome.faixa_no_pe(face, 1, PALETA.BORDA)
    abdome.face("base", PALETA.BORDA)

    # ------------------------------------------------------------- fiandeira
    # O UNICO CLARO DA FOLHA. As seis faces recebem a mesma cor, sem sombra: ela
    # tem de ler igual de qualquer angulo, porque quem esta preso nao escolhe de
    # onde olha.
    fiandeira = folha["fiandeira"]
    fiandeira.tudo(PALETA.FIANDEIRA, forca=2)
    # As tres aberturas, em seda suja, na face de TRAS -- que e a que fica virada
    # para o jogador exatamente no quadro do aviso, com o abdome erguido.
    for dx in (0, 2):
        fiandeira.ponto("tras", dx, 1, PALETA.SEDA)
    fiandeira.ponto("tras", 3, 1, PALETA.SEDA)

    # ---------------------------------------------------------------- cabeca
    cabeca = folha["cabeca"]
    cabeca.tudo(PALETA.QUITINA)
    cabeca.face("frente", PALETA.BORDA)
    cabeca.face("topo", PALETA.BORDA)
    # OITO OLHOS, quatro de cada lado da linha central, na face da frente. Eles
    # sao pontos de 1 px porque a leitura desta folha e a fiandeira: olhos grandes
    # roubariam o foco justamente no bicho em que o foco tem endereco.
    for dx in (1, 4):
        cabeca.ponto("frente", dx, 1, PALETA.OLHO)
        cabeca.ponto("frente", dx + 1, 1, PALETA.OLHO)
        cabeca.ponto("frente", dx, 2, PALETA.OLHO)
        cabeca.ponto("frente", dx + 1, 2, PALETA.OLHO)

    # ----------------------------------------------------------------- patas
    for par, lado in patas():
        femur = folha["femur_%s_%s" % (par, lado)]
        femur.tudo(PALETA.QUITINA)
        femur.face("topo", PALETA.QUITINA_CLARA)

        tarso = folha["tarso_%s_%s" % (par, lado)]
        tarso.tudo(PALETA.QUITINA)
        # A ponta escura e o que separa a pata do chao numa silhueta de patas
        # finas: sem ela as oito somem contra qualquer bloco escuro, e a aranha
        # parece flutuar.
        for face in ("frente", "tras", "direita", "esquerda"):
            tarso.faixa_no_pe(face, 3, PALETA.BORDA)
        tarso.face("base", PALETA.BORDA)
        # Um fio de seda preso ao joelho: repete a informacao do abdome em outro
        # lugar do corpo, para que a leitura "esse bicho fia" sobreviva a um
        # angulo em que a fiandeira esteja escondida.
        tarso.linha("frente", 1, PALETA.SEDA, forca=2)


def valida_fiandeira_contrasta(f):
    """A fiandeira PINTADA tem de destacar do abdome PINTADO.

    Esta regua mede o resultado, e nao a intencao: ela le os pixels que a folha
    tem depois de pintada, e nao as constantes da paleta. Uma paleta correta com
    um `tudo()` esquecido -- ou com uma faixa escura cobrindo a peca inteira --
    passaria por qualquer verificacao feita sobre as cores declaradas.

    O que ela protege e a ponta de um contrato que atravessa tres arquivos:

      * `SpiderWebberTuning.WINDUP_DA_TEIA` gasta 30 ticks avisando, e o aviso e o
        abdome erguendo a FIANDEIRA. Sem contraste, o aviso existe so como
        movimento, e movimento a nove blocos contra folhagem e o que um jogador
        em combate nao esta olhando;

      * `SpiderWebberTuning.DANO_QUE_LIBERTA` manda o preso acertar a fiandeira
        para rasgar o fio, e a mensagem traduzida diz isso com todas as letras.
        Uma fiandeira da cor da quitina transforma a instrucao numa charada.

    Nada disso levanta excecao. O portao Java de coerencia confere a DIMENSAO da
    folha e a sobreposicao de UV; o que esta pintado dentro de cada retangulo ele
    nao olha, e nenhum outro portao do repositorio olha tampouco.
    """
    def media(nome):
        """Cor media das SEIS FACES -- e nao do retangulo da caixa no atlas.

        O layout de caixa do Bedrock deixa dois quadrados de canto sem face
        nenhuma, e eles ficam transparentes. Medir o retangulo inteiro somaria
        esses vazios, e o resultado passaria a depender da PROPORCAO da peca em
        vez da cor dela: uma caixa funda teria a media puxada para baixo e uma
        rasa nao. A regua mediria geometria achando que mede tinta.
        """
        pincel = f[nome]
        soma = [0, 0, 0]
        total = 0
        for x0, y0, largura, altura in pincel.faces().values():
            for x in range(x0, x0 + largura):
                for y in range(y0, y0 + altura):
                    pixel = f.px[x, y]
                    for i in range(3):
                        soma[i] += pixel[i]
                    total += 1
        return [canal / float(total) for canal in soma]

    clara = media("fiandeira")
    escura = media("abdomen")
    distancia = sum(abs(a - b) for a, b in zip(clara, escura))
    if distancia < CONTRASTE_MINIMO_DA_FIANDEIRA:
        raise tex.ErroDeArte(
            "a fiandeira media (%d,%d,%d) e o abdome medio (%d,%d,%d) distam %d, e o minimo e %d: "
            "a nove blocos as duas pecas viram a mesma mancha. O aviso de 30 ticks passa a existir "
            "so como movimento, e a mensagem que manda o preso acertar a fiandeira vira uma charada"
            % (clara[0], clara[1], clara[2], escura[0], escura[1], escura[2],
               distancia, CONTRASTE_MINIMO_DA_FIANDEIRA))

    # E a fiandeira tem de ser o PONTO MAIS CLARO do bicho. Um segundo claro em
    # outro lugar dividiria a silhueta em duas leituras, e o endereco que a
    # mensagem da deixaria de ser unico.
    brilho = lambda cor: sum(cor)  # noqa: E731
    for caixa in f.caixas:
        if caixa.nome == "fiandeira":
            continue
        if brilho(media(caixa.nome)) > brilho(clara):
            raise tex.ErroDeArte(
                "a peca '%s' esta em media MAIS clara que a fiandeira: com dois claros a silhueta "
                "se divide em duas leituras, e 'acerte a fiandeira' deixa de ter um endereco so"
                % caixa.nome)


if __name__ == "__main__":
    pintar()
    folha.emitir(MOB, extras=(valida_fiandeira_contrasta,))
