package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuraVfxSupportTest {
    @Test
    void lodReduzDetalhePorDistancia() {
        assertEquals(AuraRenderLod.FULL, AuraRenderLod.porDistancia(2));
        assertEquals(AuraRenderLod.SHELL, AuraRenderLod.porDistancia(8));
        assertEquals(AuraRenderLod.SIMPLIFIED, AuraRenderLod.porDistancia(20));
        assertEquals(AuraRenderLod.HIDDEN, AuraRenderLod.porDistancia(40.01));
    }

    @Test
    void visibilidadeRespeitaZetsuInEGyo() {
        assertFalse(AuraVisibilityPolicy.podeRenderizar(false, false, false, false));
        assertTrue(AuraVisibilityPolicy.podeRenderizar(true, false, false, false));
        assertFalse(AuraVisibilityPolicy.podeRenderizar(true, true, false, true));
        assertFalse(AuraVisibilityPolicy.podeRenderizar(true, false, true, false));
        assertTrue(AuraVisibilityPolicy.podeRenderizar(true, false, true, true));
    }

    @Test
    void fluxoEDeterministicoEPermaneceProximoDaSilhueta() {
        AuraFlowSample primeiro = AuraFlowPattern.sample(42L, 2, 10.0F, 1.0F);
        AuraFlowSample repetido = AuraFlowPattern.sample(42L, 2, 10.0F, 1.0F);
        AuraFlowSample outro = AuraFlowPattern.sample(42L, 3, 10.0F, 1.0F);
        assertEquals(primeiro, repetido);
        assertNotEquals(primeiro, outro);
        assertTrue(Math.hypot(primeiro.x(), primeiro.z()) < 0.3F);
        assertTrue(primeiro.amplitude() >= 0.0F && primeiro.amplitude() <= 1.0F);
    }

    @Test
    void rippleExpiraEmOitoTicks() {
        AuraImpactState impacto = AuraImpactState.iniciar(AuraBodyRegion.RIGHT_ARM, 0.8F);
        for (int i = 0; i < 8; i++) impacto = impacto.avancar();
        assertEquals(0, impacto.remainingTicks());
        assertFalse(impacto.ativo());
    }

    @Test
    void entradasNaoNumericasSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class, () -> AuraRenderLod.porDistancia(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> AuraFlowPattern.sample(1, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> AuraImpactState.iniciar(AuraBodyRegion.HEAD, -1));
    }
}
