package com.darkcontinent.nenfoundation.nen.aura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuraPoolTest {

    @Test
    void mutacoesInvalidasPreservamTodaAReserva() {
        AuraPool pool = new AuraPool(10);
        for (double valor : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new AuraPool(valor));
            assertThrows(IllegalArgumentException.class, () -> pool.definirMaxima(valor));
            assertThrows(IllegalArgumentException.class, () -> pool.definirAtual(valor));
            assertThrows(IllegalArgumentException.class, () -> pool.gastar(valor));
            assertThrows(IllegalArgumentException.class, () -> pool.recuperar(valor));
            assertEquals(10, pool.atual());
            assertEquals(10, pool.maxima());
        }
        assertFalse(pool.gastar(0));
        assertFalse(pool.gastar(-1));
        assertThrows(IllegalArgumentException.class, () -> pool.definirMaxima(-1));
        assertEquals(10, pool.atual());
    }

    @Test
    void capacidadeDinamicaERecuperacaoExtremaNaoCriamAuraInvalida() {
        AuraPool pool = new AuraPool(10);
        pool.definirMaxima(20);
        assertEquals(10, pool.atual(), "aumento de capacidade nao concede reserva");
        pool.definirMaxima(4);
        assertEquals(4, pool.atual());
        pool.definirMaxima(0);
        assertEquals(0, pool.atual());
        pool.definirMaxima(Double.MAX_VALUE);
        pool.definirAtual(Double.MAX_VALUE / 2);
        pool.recuperar(Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, pool.atual());
        assertTrue(Double.isFinite(pool.atual()));
        assertEquals(0, pool.recuperar(Double.MAX_VALUE));
    }

    @Test
    void nasceCheioEExaustaoECalculada() {
        AuraPool pool = new AuraPool(100.0D);

        assertEquals(100.0D, pool.atual());
        assertFalse(pool.exausto());
        assertTrue(pool.gastar(100.0D));
        assertEquals(0.0D, pool.atual());
        assertTrue(pool.exausto());
    }

    @Test
    void gastoEAtomicoERecuperacaoTemTeto() {
        AuraPool pool = new AuraPool(10.0D);

        assertFalse(pool.gastar(11.0D));
        assertEquals(10.0D, pool.atual());
        assertTrue(pool.gastar(4.0D));
        assertEquals(6.0D, pool.atual());
        assertEquals(4.0D, pool.recuperar(10.0D));
        assertEquals(10.0D, pool.atual());
    }

    @Test
    void valoresInvalidosNaoEntram() {
        assertThrows(IllegalArgumentException.class, () -> new AuraPool(-1.0D));
        assertThrows(IllegalArgumentException.class, () -> new AuraPool(Double.NaN));

        AuraPool pool = new AuraPool(10.0D);
        assertThrows(IllegalArgumentException.class, () -> pool.definirAtual(-1.0D));
        assertThrows(IllegalArgumentException.class, () -> pool.definirAtual(11.0D));
        assertThrows(IllegalArgumentException.class, () -> pool.gastar(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> pool.recuperar(-1.0D));
        assertEquals(10.0D, pool.atual());
    }
    @Test
    @DisplayName("ajustar output altera o valor e clampa entre 0.0 e 1.0")
    void ajustarOutputClampaOValor() {
        AuraPool pool = new AuraPool(10.0D);
        assertEquals(1.0F, pool.outputSelecionado());

        assertTrue(pool.definirOutputSelecionado(0.5F));
        assertEquals(0.5F, pool.outputSelecionado());

        assertFalse(pool.definirOutputSelecionado(0.5F), "mesmo valor nao retorna true");

        assertTrue(pool.definirOutputSelecionado(1.5F));
        assertEquals(1.0F, pool.outputSelecionado());

        assertTrue(pool.definirOutputSelecionado(-0.5F));
        assertEquals(0.0F, pool.outputSelecionado());
    }

    @Test
    @DisplayName("definirOutputSelecionado recusa NaN e infinito, porque o clamp NAO os segura")
    void outputRecusaNumeroInvalido() {
        AuraPool pool = new AuraPool(100.0D);
        pool.definirOutputSelecionado(0.5F);

        // ESTE E O PONTO: Math.min(NaN, 1.0F) devolve NaN, e Math.max(0.0F, NaN)
        // tambem. Um clamp que PARECE defensivo deixa NaN passar inteiro.
        //
        // E o valor vem de um payload C2S: o cliente manda a variacao e o
        // servidor soma. Um cliente modificado enviando NaN gravava NaN no
        // runtime, e dali ele saia no delta para o HUD -- sem excecao e sem log.
        assertThrows(IllegalArgumentException.class,
                () -> pool.definirOutputSelecionado(Float.NaN),
                "NaN foi aceito no output.");
        assertThrows(IllegalArgumentException.class,
                () -> pool.definirOutputSelecionado(Float.POSITIVE_INFINITY),
                "infinito positivo foi aceito no output.");
        assertThrows(IllegalArgumentException.class,
                () -> pool.definirOutputSelecionado(Float.NEGATIVE_INFINITY),
                "infinito negativo foi aceito no output.");

        assertFalse(Float.isNaN(pool.outputSelecionado()),
                "o output ficou NaN mesmo com a recusa.");
        assertEquals(0.5F, pool.outputSelecionado(),
                "uma tentativa recusada nao pode mexer no valor que ja estava la.");
    }

    @Test
    @DisplayName("definirOutputSelecionado continua limitando valores validos fora da faixa")
    void outputAindaFazClampDoQueEValido() {
        AuraPool pool = new AuraPool(100.0D);

        pool.definirOutputSelecionado(5.0F);
        assertEquals(1.0F, pool.outputSelecionado(), "acima de 1.0 devia virar 1.0");

        pool.definirOutputSelecionado(-3.0F);
        assertEquals(0.0F, pool.outputSelecionado(), "abaixo de zero devia virar 0.0");
    }

    // ------------------------------------- o output como TRES grandezas (#70)

    @Test
    @DisplayName("o efetivo e o MENOR entre selecionado e maximo")
    void efetivoEOMenorDosDois() {
        AuraPool pool = new AuraPool(100.0D);

        pool.definirOutputSelecionado(1.0F);
        pool.definirOutputMaximo(0.6F);
        assertEquals(0.6F, pool.outputEfetivo(),
                "O jogador pediu 100% e o teto e 60%: o jogo tem de consumir 60%.");

        pool.definirOutputSelecionado(0.25F);
        assertEquals(0.25F, pool.outputEfetivo(),
                "Abaixo do teto, quem manda e a escolha do jogador.");
    }

    @Test
    @DisplayName("abaixar o maximo NAO apaga a escolha do jogador")
    void maximoNaoRebaixaOSelecionado() {
        AuraPool pool = new AuraPool(100.0D);
        pool.definirOutputSelecionado(1.0F);

        pool.definirOutputMaximo(0.4F);
        assertEquals(1.0F, pool.outputSelecionado(),
                "Rebaixar o selecionado junto apagaria a intencao do jogador.");
        assertEquals(0.4F, pool.outputEfetivo());

        // E ao voltar o teto, ele volta ao que tinha pedido -- sem precisar
        // reapertar a tecla. Se a escolha tivesse sido apagada, ele ficaria
        // preso em 40% sem entender por que.
        pool.definirOutputMaximo(1.0F);
        assertEquals(1.0F, pool.outputEfetivo(),
                "A escolha do jogador nao sobreviveu ao teto voltar.");
    }

    @Test
    @DisplayName("o efetivo nunca e guardado: ele acompanha as duas fontes")
    void efetivoNaoEUmaCopia() {
        AuraPool pool = new AuraPool(100.0D);
        pool.definirOutputSelecionado(0.8F);
        pool.definirOutputMaximo(0.8F);
        assertEquals(0.8F, pool.outputEfetivo());

        // Guardado numa terceira variavel, este seria o momento em que ele
        // divergiria: alguem muda o maximo e esquece de recalcular.
        pool.definirOutputMaximo(0.3F);
        assertEquals(0.3F, pool.outputEfetivo(), "o efetivo nao acompanhou o maximo");

        pool.definirOutputSelecionado(0.1F);
        assertEquals(0.1F, pool.outputEfetivo(), "o efetivo nao acompanhou o selecionado");
    }

    @Test
    @DisplayName("um passo move exatamente 5 pontos percentuais")
    void passoMoveCincoPontos() {
        AuraPool pool = new AuraPool(100.0D);
        pool.definirOutputSelecionado(0.50F);

        assertTrue(pool.diminuirOutput());
        assertEquals(0.45F, pool.outputSelecionado(), 1.0E-6F);

        assertTrue(pool.aumentarOutput());
        assertEquals(0.50F, pool.outputSelecionado(), 1.0E-6F);
    }

    @Test
    @DisplayName("vinte passos fecham exatamente em 0% e 100%")
    void passosFechamNasPontas() {
        // POR QUE ISTO IMPORTA: um passo que nao divide 1.0 -- 0.07, por
        // exemplo -- leva a 98% e o jogador NUNCA alcanca o proprio teto.
        // Nao da erro; so nunca chega la.
        AuraPool pool = new AuraPool(100.0D);
        pool.definirOutputSelecionado(0.0F);

        // CONTA os passos em vez de dar vinte e olhar o fim: com um passo que
        // nao divide 1.0 o clamp disfarca o defeito. Com 0.07, o decimo quinto
        // passo pede 1.05 e o clamp entrega 1.0 -- e um teste que so olhasse o
        // valor final aprovaria. Foi o que aconteceu na primeira versao.
        int passos = 0;
        while (pool.aumentarOutput()) {
            passos++;
            assertTrue(passos <= 64, "o output nunca alcancou o teto");
        }
        assertEquals(20, passos,
                "Foram " + passos + " passos ate 100%, e nao 20. O passo precisa"
                        + " dividir 1.0 exatamente, senao o jogador chega ao"
                        + " proprio teto por clamp -- e o penultimo degrau fica"
                        + " a uma distancia diferente de todos os outros.");
        assertEquals(1.0F, pool.outputSelecionado(), 1.0E-6F);

        int paraBaixo = 0;
        while (pool.diminuirOutput()) {
            paraBaixo++;
            assertTrue(paraBaixo <= 64, "o output nunca alcancou o piso");
        }
        assertEquals(20, paraBaixo, "a descida precisa ter o mesmo numero de degraus");
        assertEquals(0.0F, pool.outputSelecionado(), 1.0E-6F);
    }

    @Test
    @DisplayName("os passos param nas pontas em vez de estourar a faixa")
    void passosParamNasPontas() {
        AuraPool pool = new AuraPool(100.0D);

        pool.definirOutputSelecionado(1.0F);
        assertFalse(pool.aumentarOutput(), "no teto, subir nao e mudanca");
        assertEquals(1.0F, pool.outputSelecionado());

        pool.definirOutputSelecionado(0.0F);
        assertFalse(pool.diminuirOutput(), "no piso, descer nao e mudanca");
        assertEquals(0.0F, pool.outputSelecionado());
    }

    @Test
    @DisplayName("o maximo tambem recusa NaN e e limitado a faixa")
    void maximoValidaIgual() {
        AuraPool pool = new AuraPool(100.0D);

        assertThrows(IllegalArgumentException.class,
                () -> pool.definirOutputMaximo(Float.NaN));
        assertEquals(1.0F, pool.outputMaximo(), "a recusa nao pode mexer no valor");

        pool.definirOutputMaximo(9.0F);
        assertEquals(1.0F, pool.outputMaximo(), "acima de 1.0 devia virar 1.0");
        pool.definirOutputMaximo(-2.0F);
        assertEquals(0.0F, pool.outputMaximo(), "abaixo de zero devia virar 0.0");
    }

    @Test
    @DisplayName("maximo zero zera o efetivo, e o selecionado sobrevive")
    void maximoZeroZeraOEfetivo() {
        AuraPool pool = new AuraPool(100.0D);
        pool.definirOutputSelecionado(1.0F);
        pool.definirOutputMaximo(0.0F);

        assertEquals(0.0F, pool.outputEfetivo(),
                "com teto zero o jogo nao pode consumir nada");
        assertEquals(1.0F, pool.outputSelecionado(),
                "a escolha do jogador nao some porque o teto zerou");
    }

    @Test
    @DisplayName("uma reserva nova nasce com output em 100%, selecionado e maximo")
    void nasceEmCem() {
        AuraPool pool = new AuraPool();
        assertEquals(1.0F, pool.outputSelecionado());
        assertEquals(1.0F, pool.outputMaximo());
        assertEquals(1.0F, pool.outputEfetivo());
    }
}
