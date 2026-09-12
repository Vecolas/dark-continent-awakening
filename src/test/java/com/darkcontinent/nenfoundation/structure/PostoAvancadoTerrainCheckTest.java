package com.darkcontinent.nenfoundation.structure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PostoAvancadoTerrainCheckTest {
    @Test
    void terrenoPlanoEIdeal() {
        var resultado = PostoAvancadoTerrainCheck.avaliar(100, 200, 37, (x, z) -> 70);

        assertTrue(resultado.ideal());
        assertTrue(resultado.aceitavel());
        assertTrue(resultado.diferenca() == 0);
    }

    @Test
    void diferencaCincoAindaEAplicavelMasNaoIdeal() {
        var resultado = PostoAvancadoTerrainCheck.avaliar(0, 0, 37,
                (x, z) -> x == -18 ? 0 : 5);

        assertFalse(resultado.ideal());
        assertTrue(resultado.aceitavel());
        assertTrue(resultado.diferenca() == 5);
    }

    @Test
    void footprintMuitoInclinadoERejeitado() {
        var resultado = PostoAvancadoTerrainCheck.avaliar(0, 0, 37,
                (x, z) -> x == -18 && z == 0 ? 0 : 6);

        assertFalse(resultado.aceitavel());
    }

    @Test
    void footprintPrecisaSerImpar() {
        assertThrows(IllegalArgumentException.class,
                () -> PostoAvancadoTerrainCheck.avaliar(0, 0, 36, (x, z) -> 0));
    }
}
