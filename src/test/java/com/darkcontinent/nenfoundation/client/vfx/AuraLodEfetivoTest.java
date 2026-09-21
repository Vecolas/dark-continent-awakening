package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O funil de nivel de detalhe: distancia maxima, sobreposicao e teto, em ordem.
 *
 * <p><b>O QUE ESTE ARQUIVO PROTEGE E UMA ORDEM, e ordem trocada nao lanca.</b> As
 * tres coisas que decidem o nivel efetivo se aplicam em sequencia, e inverter
 * duas delas produz um jogo que funciona e desobedece: {@code /nenvfx lod full}
 * passando por cima do limite que o jogador configurou, ou o teto de qualidade
 * sendo aplicado antes do corte e perdendo o efeito.
 *
 * <p>O outro modo de falha que ele fecha e a corrente COPIADA. Antes deste funil,
 * a sequencia estava escrita a mao em quatro lugares, com tres cortes proprios em
 * numero cru -- 24 no anel, 96 no brilho, nenhum nos filamentos. Quatro copias da
 * mesma decisao divergem, e a divergencia aparece como um anel que some antes da
 * aura, ou um halo que continua depois dela.
 */
class AuraLodEfetivoTest {

    /** A distancia maxima padrao, e o proprio corte da tabela. */
    private static final double SETENTA_E_DOIS = 72.0D;

    @Test
    @DisplayName("dentro do alcance, o nivel e o da tabela de distancia")
    void dentroDoAlcanceValeATabela() {
        for (double d : new double[] {0.0D, 5.0D, 11.9D, 12.0D, 23.0D, 47.0D, 71.0D}) {
            assertSame(AuraRenderLod.porDistancia(d),
                    AuraLodEfetivo.de(d, SETENTA_E_DOIS, AuraVisualQuality.ULTRA, null),
                    "a " + d + " blocos o funil divergiu da tabela");
        }
    }

    @Test
    @DisplayName("alem da distancia maxima do jogador, NADA -- nem com qualidade ULTRA")
    void alemDoMaximoNadaEDesenhado() {
        assertSame(AuraRenderLod.HIDDEN,
                AuraLodEfetivo.de(31.0D, 30.0D, AuraVisualQuality.ULTRA, null),
                "quem configurou trinta blocos nao quer ver aura a trinta e um");
        assertTrue(AuraLodEfetivo.de(29.0D, 30.0D, AuraVisualQuality.ULTRA, null).visivel());
    }

    @Test
    @DisplayName("a distancia maxima vence a sobreposicao do dev, e nao o contrario")
    void oLimiteDoJogadorVemPrimeiro() {
        // INVERTENDO ESTA ORDEM, `/nenvfx lod full` passaria a desenhar aura a
        // duzentos blocos para quem configurou trinta -- e "o meu limite parou
        // de valer" e um relato que ninguem liga a uma ordem de operacoes.
        assertSame(AuraRenderLod.HIDDEN,
                AuraLodEfetivo.de(50.0D, 30.0D, AuraVisualQuality.ULTRA, AuraRenderLod.FULL),
                "o lod forcado atravessou a distancia maxima do jogador");
        // DENTRO do alcance, o dev manda.
        assertSame(AuraRenderLod.FULL,
                AuraLodEfetivo.de(50.0D, 72.0D, AuraVisualQuality.ULTRA, AuraRenderLod.FULL),
                "dentro do alcance o comando de dev precisa valer -- senao ele nao serve"
                        + " para tirar captura a distancia");
    }

    @Test
    @DisplayName("o teto de qualidade e um TETO, e se aplica ao resultado")
    void qualidadeLimitaPorUltimo() {
        // Perto, com qualidade LOW: a tabela diria FULL, e o teto corta.
        assertSame(AuraRenderLod.FAR,
                AuraLodEfetivo.de(2.0D, SETENTA_E_DOIS, AuraVisualQuality.LOW, null),
                "LOW nunca passa da borda, mesmo a dois blocos");
        // E ele nao ENRIQUECE: longe, com ULTRA, continua sendo o que a
        // distancia permite.
        assertSame(AuraRenderLod.porDistancia(60.0D),
                AuraLodEfetivo.de(60.0D, SETENTA_E_DOIS, AuraVisualQuality.ULTRA, null));
    }

    @Test
    @DisplayName("qualidade OFF desliga o desenho inclusive perto")
    void qualidadeOffDesliga() {
        assertSame(AuraRenderLod.HIDDEN,
                AuraLodEfetivo.de(1.0D, SETENTA_E_DOIS, AuraVisualQuality.OFF, null));
        assertSame(AuraRenderLod.HIDDEN,
                AuraLodEfetivo.de(1.0D, SETENTA_E_DOIS, AuraVisualQuality.OFF,
                        AuraRenderLod.FULL),
                "OFF e escolha do jogador, e o comando de dev nao a revoga");
    }

    @Test
    @DisplayName("distancia invalida NAO desenha -- a falha vai na direcao segura")
    void distanciaInvalidaNaoDesenha() {
        // NaN PRECISA SER BARRADO DE PROPOSITO: toda comparacao com ele e falsa,
        // e sem a guarda ele atravessaria os cortes ate o fim. Ver de menos, e
        // nunca ver o que nao deveria estar la.
        assertSame(AuraRenderLod.HIDDEN,
                AuraLodEfetivo.de(Double.NaN, SETENTA_E_DOIS, AuraVisualQuality.ULTRA, null));
        assertSame(AuraRenderLod.HIDDEN,
                AuraLodEfetivo.de(Double.POSITIVE_INFINITY, SETENTA_E_DOIS,
                        AuraVisualQuality.ULTRA, null));
        assertSame(AuraRenderLod.HIDDEN,
                AuraLodEfetivo.de(-1.0D, SETENTA_E_DOIS, AuraVisualQuality.ULTRA, null));
    }

    @Test
    @DisplayName("qualidade nula nao derruba o desenho")
    void qualidadeNulaNaoDerruba() {
        // Quem chama e o caminho de render. Lancar aqui derrubaria o desenho do
        // mundo inteiro por causa de uma config nao carregada.
        assertSame(AuraRenderLod.porDistancia(10.0D),
                AuraLodEfetivo.de(10.0D, SETENTA_E_DOIS, null, null));
    }

    @Test
    @DisplayName("o padrao de 72 blocos coincide com o corte da propria tabela")
    void oPadraoNaoInventaCorte() {
        // SE OS DOIS DIVERGIREM, a chave passa a cortar antes ou depois da
        // tabela -- e o jogador que nunca tocou na config veria um
        // comportamento que ninguem escolheu.
        assertSame(AuraRenderLod.HIDDEN, AuraRenderLod.porDistancia(SETENTA_E_DOIS),
                "a tabela precisa esconder exatamente a partir de 72");
        assertTrue(AuraRenderLod.porDistancia(SETENTA_E_DOIS - 0.1D).visivel());
        assertEquals(AuraRenderLod.HIDDEN,
                AuraLodEfetivo.de(SETENTA_E_DOIS + 0.1D, SETENTA_E_DOIS,
                        AuraVisualQuality.ULTRA, null));
    }
}
