"""Sintese de audio AUTORAL para os inimigos -- sem amostra de lugar nenhum.

POR QUE SINTESE, E NAO BIBLIOTECA DE SONS. O ADR-007 e curto: nenhum asset
extraido da obra. Um banco de sons "livre" baixado da internet nao resolve isso
-- resolve a licenca e nao a AUTORIA, e a diferenca aparece no dia em que alguem
pergunta de onde veio o rugido. Sintetizado aqui, o som e nosso por construcao, e
o arquivo que o gerou esta no git.

POR QUE ISTO EXISTIU BLOQUEADO ATE AGORA. O Minecraft so toca OGG Vorbis, e esta
maquina nao tem ffmpeg nem oggenc. O que destravou foi o `soundfile`: o
libsndfile que vem no wheel escreve Vorbis de verdade -- conferido no cabecalho
(OggS...vorbis). Um `.wav` renomeado para `.ogg` CARREGA MUDO: o jogo nao
reclama, nao loga nada, e o mob simplesmente nao emite som. E o falso verde mais
barato deste dominio, e por isso `valida_vorbis` existe e roda sempre.

DETERMINISMO, E ELE TEM DUAS METADES. A primeira e obvia: nenhuma chamada a
`random` sem semente. A segunda so apareceu quando `art-source/verificar.py`
regerou tudo e comparou -- o libsndfile SORTEIA o numero de serie do fluxo Ogg a
cada codificacao, e ele vai no cabecalho de toda pagina, com o CRC junto. O audio
decodificado saia identico; o BINARIO nao.

Isso nao dava erro nenhum, e o preco era de historico: cada regeneracao marcava
os 120 arquivos como alterados, todo diff virava ruido, e no dia em que um som
mudasse DE VERDADE ninguem repararia. `determinizar` fixa o serial (derivado do
nome do arquivo) e recalcula o CRC de cada pagina; regenerar sem mexer no som
passou a nao produzir diff nenhum.

Uso:
    from comum_sons import sintese as s
    voz = s.Voz(duracao=0.8, taxa=22050)
    onda = voz.rugido(base=90, aspereza=0.5, semente=7)
    s.escrever('rugido.ogg', onda, taxa=22050)
"""
import hashlib
import math
import os
import struct

import numpy as np
import soundfile as sf

# 22050 Hz e mono: o Minecraft reamostra de qualquer jeito, e som posicional de
# mob nao ganha nada com estereo -- ganha o dobro de bytes no repositorio.
TAXA_PADRAO = 22050
CANAIS = 1

# Pico maximo depois da normalizacao. Nao e 1.0 de proposito: o Vorbis com
# margem zero produz clipping audivel na decodificacao, e o sintoma e um chiado
# que nao existe na onda original -- ninguem procura isso no gerador.
PICO = 0.89


class ErroDeSom(ValueError):
    """Recusa de sintese, com o motivo e o que aconteceria em jogo."""


def _rng(semente):
    """Gerador com semente OBRIGATORIA.

    Sem semente, duas execucoes produzem arquivos diferentes. Isso nao da erro:
    enche o historico de diffs binarios que ninguem consegue ler.
    """
    if semente is None:
        raise ErroDeSom("sintese sem semente: o .ogg e binario versionado, e sem "
                        "determinismo todo diff vira ruido")
    return np.random.default_rng(int(semente))


def envelope(n, ataque, sustentacao, queda):
    """Envelope ADSR simplificado, em FRACOES da duracao total.

    O ataque manda na leitura: ataque longo le como 'sopro', curto le como
    'batida'. Um envelope sem ataque nenhum produz um estalo no primeiro
    amostra -- e o estalo e o unico som que o jogador vai lembrar.
    """
    total = ataque + sustentacao + queda
    if total <= 0 or min(ataque, sustentacao, queda) < 0:
        raise ErroDeSom("envelope invalido: %s/%s/%s" % (ataque, sustentacao, queda))
    a = max(1, int(n * ataque / total))
    s = max(1, int(n * sustentacao / total))
    d = max(1, n - a - s)
    return np.concatenate([
        np.linspace(0.0, 1.0, a, endpoint=False),
        np.ones(s),
        np.linspace(1.0, 0.0, d),
    ])[:n]


def _ajustar(onda, n):
    if len(onda) < n:
        return np.pad(onda, (0, n - len(onda)))
    return onda[:n]


class Voz:
    """Uma voz de mob: duracao e taxa fixas, timbres por metodo."""

    def __init__(self, duracao, taxa=TAXA_PADRAO):
        if duracao <= 0 or duracao > 8.0:
            raise ErroDeSom("duracao de %.2fs fora da faixa util (0, 8]: som de mob "
                            "mais longo que isso atravessa o proprio encontro" % duracao)
        self.duracao = float(duracao)
        self.taxa = int(taxa)
        self.n = int(self.duracao * self.taxa)
        self.t = np.arange(self.n) / float(self.taxa)

    # ------------------------------------------------------------ primitivas

    def ruido(self, semente, cor=0.0):
        """Ruido com inclinacao espectral.

        `cor` 0 e branco; positivo escurece (mais grave), negativo clareia. Ruido
        branco puro le como estatica de radio em qualquer contexto -- e o que faz
        ele soar organico e justamente a inclinacao.
        """
        bruto = _rng(semente).standard_normal(self.n)
        if abs(cor) < 1e-9:
            return bruto
        espectro = np.fft.rfft(bruto)
        freq = np.fft.rfftfreq(self.n, 1.0 / self.taxa)
        freq[0] = freq[1] if len(freq) > 1 else 1.0
        return np.fft.irfft(espectro * (freq ** (-cor)), n=self.n)

    def seno(self, freq, fase=0.0):
        return np.sin(2.0 * math.pi * freq * self.t + fase)

    def varredura(self, inicio, fim, curva=1.0):
        """Glissando de `inicio` a `fim`.

        A curva decide a leitura: 1.0 e linear (mecanico), >1 desce depressa e
        segura no fim (organico, como ar saindo), <1 faz o contrario.
        """
        if inicio <= 0 or fim <= 0:
            raise ErroDeSom("varredura com frequencia nao positiva")
        p = np.linspace(0.0, 1.0, self.n) ** curva
        instantanea = inicio * (fim / inicio) ** p
        return np.sin(2.0 * math.pi * np.cumsum(instantanea) / self.taxa)

    # --------------------------------------------------------------- timbres

    def rugido(self, base, aspereza, semente):
        """Grave com harmonicos irregulares -- a voz de bicho grande.

        A aspereza e modulacao de frequencia por ruido, e nao distorcao: distorcer
        um seno da um zumbido eletrico, que e exatamente o que um rugido nao pode
        soar. Base baixa (60-120 Hz) le como massa; acima de 200 ja le como
        rosnado de bicho pequeno.
        """
        if not 20.0 <= base <= 400.0:
            raise ErroDeSom("rugido em %.0f Hz: fora de 20..400 ele deixa de ler como "
                            "corpo e passa a ler como apito" % base)
        modulador = self.ruido(semente, cor=1.6)
        modulador = modulador / (np.max(np.abs(modulador)) or 1.0)
        desvio = base * float(aspereza)
        fase = 2.0 * math.pi * np.cumsum(base + desvio * modulador) / self.taxa
        corpo = np.sin(fase) + 0.45 * np.sin(2.0 * fase) + 0.2 * np.sin(3.0 * fase)
        return corpo * envelope(self.n, 0.12, 0.5, 0.38)

    def chiado(self, semente, brilho=0.35, pulsos=0):
        """Ruido filtrado -- sopro, esporo, teia, asa.

        `pulsos` acima de zero recorta o ruido em batidas regulares: e o que faz
        asa ler como asa em vez de vento.
        """
        onda = self.ruido(semente, cor=-brilho)
        onda = onda / (np.max(np.abs(onda)) or 1.0)
        if pulsos > 0:
            batida = 0.5 + 0.5 * np.sign(np.sin(2.0 * math.pi * pulsos * self.t))
            onda = onda * (0.25 + 0.75 * batida)
        return onda * envelope(self.n, 0.2, 0.45, 0.35)

    def estalo(self, freq, decaimento=28.0):
        """Percussao curta -- casco, carapaca, mandibula."""
        return np.sin(2.0 * math.pi * freq * self.t) * np.exp(-decaimento * self.t)

    def guincho(self, inicio, fim, semente):
        """Agudo que sobe ou desce -- rato, morcego, alarme.

        Guincho puro le como sintetizador; a pitada de ruido e o que o torna
        biologico. O ruido entra em 12% de proposito: acima disso vira chiado com
        tom, e o tom e justamente o que carrega a informacao de alarme.
        """
        return (0.88 * self.varredura(inicio, fim, curva=1.4)
                + 0.12 * self.ruido(semente, cor=-0.4)) * envelope(self.n, 0.06, 0.3, 0.64)

    def borbulha(self, semente, densidade=14):
        """Bolhas -- gotas de frequencia aleatoria com decaimento curto.

        Deterministico apesar do nome: as posicoes e alturas saem do gerador com
        semente, e nao de sorteio livre.
        """
        rng = _rng(semente)
        onda = np.zeros(self.n)
        for _ in range(int(densidade)):
            inicio = int(rng.uniform(0.0, 0.85) * self.n)
            freq = float(rng.uniform(320.0, 1400.0))
            comprimento = int(self.taxa * rng.uniform(0.03, 0.09))
            fim = min(self.n, inicio + comprimento)
            t = np.arange(fim - inicio) / float(self.taxa)
            onda[inicio:fim] += np.sin(2.0 * math.pi * freq * t) * np.exp(-40.0 * t)
        return onda * envelope(self.n, 0.05, 0.6, 0.35)

    # --------------------------------------------------------------- mistura

    def misturar(self, *partes):
        """Soma camadas e normaliza UMA vez, no fim.

        Normalizar camada a camada faz a ultima dominar: cada uma chega no pico e
        a soma satura. O resultado nao da erro -- da um som em que so se ouve o
        ultimo ingrediente.
        """
        if not partes:
            raise ErroDeSom("mistura vazia")
        soma = np.zeros(self.n)
        for parte in partes:
            peso, onda = parte if isinstance(parte, tuple) else (1.0, parte)
            soma += float(peso) * _ajustar(np.asarray(onda, dtype=float), self.n)
        return normalizar(soma)


def normalizar(onda):
    pico = float(np.max(np.abs(onda))) if len(onda) else 0.0
    if pico < 1e-9:
        raise ErroDeSom("onda silenciosa: o arquivo carregaria e o mob continuaria mudo, "
                        "que e exatamente a falha que este pipeline existe para fechar")
    return (onda / pico) * PICO


# --------------------------------------------------------------------- saida

def escrever(destino, onda, taxa=TAXA_PADRAO, serial=None):
    """Grava OGG Vorbis, torna o binario REPRODUTIVEL e confere o que gravou."""
    pasta = os.path.dirname(destino)
    if pasta and not os.path.isdir(pasta):
        os.makedirs(pasta)
    sf.write(destino, np.asarray(onda, dtype='float32'), int(taxa),
             format='OGG', subtype='VORBIS')
    determinizar(destino, serial)
    valida_vorbis(destino)
    return destino


# --------------------------------------------------------------- reproducao

# CRC do Ogg: polinomio 0x04c11db7, SEM reflexao de entrada ou saida, inicio 0 e
# sem xor final. Nao e o CRC-32 comum (o do zip/png): usar aquele produz um
# arquivo que o decodificador RECUSA, e o sintoma seria um som que some depois de
# passar por aqui -- com o gerador dizendo que escreveu.
def _tabela_crc():
    tabela = []
    for i in range(256):
        r = i << 24
        for _ in range(8):
            r = ((r << 1) ^ 0x04C11DB7) & 0xFFFFFFFF if r & 0x80000000 else (r << 1) & 0xFFFFFFFF
        tabela.append(r)
    return tuple(tabela)


_CRC = _tabela_crc()


def _crc_ogg(dados):
    r = 0
    for byte in dados:
        r = ((r << 8) & 0xFFFFFFFF) ^ _CRC[((r >> 24) & 0xFF) ^ byte]
    return r


def determinizar(caminho, serial=None):
    """Fixa o SERIAL do fluxo Ogg e recalcula o CRC de cada pagina.

    POR QUE ISTO PRECISA EXISTIR. O libsndfile sorteia o numero de serie do
    fluxo a cada codificacao, e ele aparece no cabecalho de TODA pagina -- com o
    CRC junto. O resultado: o mesmo audio, codificado duas vezes, produz arquivos
    com bytes diferentes. O audio decodificado e identico; o BINARIO nao e.

    Isso nao da erro nenhum, e o custo e de historico: cada regeneracao marca os
    120 arquivos como alterados, todo diff vira ruido, e no dia em que um som
    mudar DE VERDADE ninguem repara. Com o serial fixo, regenerar sem mexer no
    som nao produz diff nenhum -- e o diff que aparecer significa alguma coisa.

    O serial padrao sai do NOME do arquivo: dois sons diferentes continuam com
    fluxos distintos (o formato espera isso de fluxos multiplexados), e o mesmo
    som sai igual sempre.
    """
    if serial is None:
        semente = os.path.basename(os.path.dirname(caminho)) + '/' + os.path.basename(caminho)
        serial = int(hashlib.sha1(semente.encode('utf-8')).hexdigest()[:8], 16)
    serial &= 0xFFFFFFFF

    with open(caminho, 'rb') as arquivo:
        dados = bytearray(arquivo.read())

    i = 0
    paginas = 0
    while i + 27 <= len(dados):
        if dados[i:i + 4] != b'OggS':
            raise ErroDeSom("pagina Ogg malformada em %s (offset %d): o arquivo nao pode ser "
                            "reescrito com seguranca, e reescrever assim mesmo produziria um "
                            "som que o jogo recusa sem dizer por que" % (caminho, i))
        segmentos = dados[i + 26]
        cabecalho = 27 + segmentos
        corpo = sum(dados[i + 27:i + 27 + segmentos])
        total = cabecalho + corpo

        dados[i + 14:i + 18] = struct.pack('<I', serial)
        dados[i + 22:i + 26] = bytes(4)
        dados[i + 22:i + 26] = struct.pack('<I', _crc_ogg(dados[i:i + total]))

        i += total
        paginas += 1

    if paginas == 0:
        raise ErroDeSom("%s nao tem nenhuma pagina Ogg" % caminho)

    with open(caminho, 'wb') as arquivo:
        arquivo.write(dados)
    return paginas


def valida_vorbis(caminho):
    """O arquivo e MESMO Ogg Vorbis?

    Um `.wav` renomeado para `.ogg` carrega mudo: o jogo nao reclama, nao loga
    nada, e o mob nao emite som. Conferir a assinatura e a unica defesa, porque
    nao ha erro para procurar depois.
    """
    with open(caminho, 'rb') as arquivo:
        cabecalho = arquivo.read(64)
    if cabecalho[:4] != b'OggS':
        raise ErroDeSom("%s nao comeca com 'OggS': o Minecraft carrega o arquivo e o mob "
                        "fica MUDO, sem uma linha de log" % caminho)
    if b'vorbis' not in cabecalho:
        raise ErroDeSom("%s e um contêiner Ogg sem Vorbis dentro (Opus?): o jogo nao toca, "
                        "e tambem nao reclama" % caminho)
    tamanho = os.path.getsize(caminho)
    if tamanho < 512:
        raise ErroDeSom("%s tem %d bytes: pequeno demais para conter audio util, e um "
                        "arquivo vazio soa exatamente como nenhum arquivo" % (caminho, tamanho))
    return tamanho


def duracao_de(caminho):
    """Duracao real do arquivo, lida de volta do disco."""
    info = sf.info(caminho)
    return info.frames / float(info.samplerate)
