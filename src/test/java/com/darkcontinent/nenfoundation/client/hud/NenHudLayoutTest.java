package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NenHudLayoutTest {
    @Test
    void ancoraNoCantoSuperiorEsquerdoEmCoordenadasGui() {
        NenHudLayout pequeno = NenHudLayout.para(120);
        NenHudLayout grande = NenHudLayout.para(640);

        assertEquals(NenHudLayout.MARGEM, pequeno.retrato().x());
        assertEquals(NenHudLayout.MARGEM, grande.retrato().x());
        assertEquals(pequeno.barraDeAura().x(), grande.barraDeAura().x());
        assertTrue(pequeno.barraDeAura().x() + pequeno.barraDeAura().largura() <= 120);
        assertTrue(grande.barraDeAura().x() + grande.barraDeAura().largura() <= 640);
    }

    @Test
    void segmentacaoNaoArredondaAFracaoAntesDoPixel() {
        assertEquals(74, NenHudLayout.preenchimento(100, 0.737F));
        assertEquals(0, NenHudLayout.preenchimento(100, Float.NaN));
        assertEquals(100, NenHudLayout.preenchimento(100, 2.0F));
    }
}
