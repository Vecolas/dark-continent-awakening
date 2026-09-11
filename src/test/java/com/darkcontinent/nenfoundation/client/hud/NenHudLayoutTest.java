package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NenHudLayoutTest {
    @Test
    void ancoraNoCantoSuperiorEsquerdoEmCoordenadasGui() {
        NenHudLayout pequeno = NenHudLayout.para(120);
        NenHudLayout grande = NenHudLayout.para(640);

        assertTrue(pequeno.retrato().x() >= NenHudLayout.MARGEM);
        assertEquals(NenHudLayout.MARGEM, pequeno.moldura().x());
        assertEquals(NenHudLayout.MARGEM, grande.moldura().x());
        assertTrue(pequeno.barraDeAura().x() + pequeno.barraDeAura().largura() <= 120);
        assertTrue(grande.barraDeAura().x() + grande.barraDeAura().largura() <= 640);
        assertTrue(grande.barraDeAura().x() + grande.barraDeAura().largura()
                < grande.valorDeAura().x());
        assertTrue(grande.barraDeOutput().x() + grande.barraDeOutput().largura()
                < grande.valorDeOutput().x());
    }

    @Test
    void segmentacaoNaoArredondaAFracaoAntesDoPixel() {
        assertEquals(74, NenHudLayout.preenchimento(100, 0.737F));
        assertEquals(0, NenHudLayout.preenchimento(100, Float.NaN));
        assertEquals(100, NenHudLayout.preenchimento(100, 2.0F));
    }
}
