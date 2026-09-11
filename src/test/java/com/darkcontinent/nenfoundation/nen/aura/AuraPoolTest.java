package com.darkcontinent.nenfoundation.nen.aura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuraPoolTest {

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
}
