package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NenHudVisibilityTest {
    @Test
    void modosJogaveisRenderizamMesmoComValoresZero() {
        assertTrue(NenHudVisibility.deveRenderizar(false, true, false));
    }

    @Test
    void f1EspectadorEAusenciaDeJogadorOcultam() {
        assertFalse(NenHudVisibility.deveRenderizar(true, true, false));
        assertFalse(NenHudVisibility.deveRenderizar(false, true, true));
        assertFalse(NenHudVisibility.deveRenderizar(false, false, false));
    }
}
