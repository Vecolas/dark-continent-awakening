package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.darkcontinent.nenfoundation.client.hud.animation.HudValueAnimator;
import org.junit.jupiter.api.Test;

class HudValueAnimatorTest {
    @Test
    void primeiroValorENeutroEMudancaConverge() {
        HudValueAnimator animador = new HudValueAnimator(() -> 3);
        assertEquals(0.0F, animador.valorAtual(0));
        animador.receber(0.5F, 1.0F, 10, false);
        assertEquals(0.5F, animador.valorAtual(10));
        animador.receber(0.8F, 1.0F, 11, false);
        assertEquals(0.5F, animador.valorAtual(11));
        assertEquals(0.6F, animador.valorAtual(12), 0.0001F);
        assertEquals(0.8F, animador.valorAtual(14));
    }

    @Test
    void imediatoNaoAnimaEZeroContinuaValorValido() {
        HudValueAnimator animador = new HudValueAnimator(() -> 5);
        animador.receber(1.0F, 1.0F, 0, false);
        animador.receber(0.0F, 1.0F, 1, true);
        assertEquals(0.0F, animador.valorAtual(1));
    }
}
