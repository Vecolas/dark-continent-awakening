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
        assertEquals(1.0F, pool.outputPercent());

        assertTrue(pool.ajustarOutput(0.5F));
        assertEquals(0.5F, pool.outputPercent());

        assertFalse(pool.ajustarOutput(0.5F), "mesmo valor nao retorna true");

        assertTrue(pool.ajustarOutput(1.5F));
        assertEquals(1.0F, pool.outputPercent());

        assertTrue(pool.ajustarOutput(-0.5F));
        assertEquals(0.0F, pool.outputPercent());
    }
}
