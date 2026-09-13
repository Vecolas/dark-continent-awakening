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
        // A REGRA DE LOW MUDOU NO AV3. Antes ela so rebaixava FULL um degrau;
        // agora impoe TETO em FAR -- porque quem escolhe LOW quer menos
        // trabalho perto, e perto era onde o custo estava.
        assertEquals(AuraRenderLod.FAR, AuraVisualQuality.LOW.limitar(AuraRenderLod.FULL));
        assertEquals(AuraRenderLod.FAR, AuraVisualQuality.LOW.limitar(AuraRenderLod.MEDIUM));
        assertEquals(AuraRenderLod.HIDDEN, AuraVisualQuality.LOW.limitar(AuraRenderLod.HIDDEN),
                "qualidade nunca MELHORA o que a distancia ja cortou");
        // A ULTIMA LINHA DESTE TESTE SAIU. Ela comparava
        // `AuraVisualProfile.agressiva().flowSpeed()` com o da `controlada()` --
        // um record que NINGUEM no repositorio construia fora deste teste. Ele
        // provava a si mesmo, e o record foi removido. A mesma propriedade,
        // sobre o dado que o jogo carrega, vive em AuraPerfilVisualTest:
        // `ren.velocidadeDeFluxo() > ten.velocidadeDeFluxo()`.
    }
}
