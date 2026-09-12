package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuraVfxSupportTest {
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

    @Test
    void qualidadeLocalNuncaAumentaDetalheAlemDoLod() {
        assertEquals(AuraRenderLod.HIDDEN, AuraVisualQuality.OFF.limitar(AuraRenderLod.FULL));
        assertEquals(AuraRenderLod.SHELL, AuraVisualQuality.LOW.limitar(AuraRenderLod.FULL));
        assertEquals(AuraRenderLod.SIMPLIFIED, AuraVisualQuality.LOW.limitar(AuraRenderLod.SIMPLIFIED));
        assertTrue(AuraVisualProfile.agressiva().flowSpeed()
                > AuraVisualProfile.controlada().flowSpeed());
    }
}
