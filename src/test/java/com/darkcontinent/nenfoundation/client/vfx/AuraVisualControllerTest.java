package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuraVisualControllerTest {
    @Test
    void presetsMantemTenContidoRenMaisIntensoEZetsuInvisivel() {
        assertTrue(AuraVisualPreset.tenBasic().shellOpacity() < AuraVisualPreset.renBasic().shellOpacity());
        assertTrue(AuraVisualPreset.tenBasic().flowIntensity() < AuraVisualPreset.renBasic().flowIntensity());
        assertEquals(0.0F, AuraVisualPreset.zetsu().shellOpacity());
        assertEquals(0.0F, AuraVisualPreset.zetsu().flowIntensity());
    }

    @Test
    void transicaoTenParaRenNaoTrocaInstantaneamente() {
        AuraVisualController controller = new AuraVisualController();
        controller.receber(AuraVisualMode.TEN, 0.5F);
        controller.avancar(1.0F);
        controller.receber(AuraVisualMode.REN, 1.0F);

        AuraVisualState meio = controller.avancar(0.5F);
        assertEquals(AuraVisualMode.TEN, meio.mode());
        assertTrue(meio.intensity() > 0.5F && meio.intensity() < 1.0F);
        assertTrue(meio.transitionProgress() < 1.0F);

        AuraVisualState finalizado = controller.avancar(0.5F);
        assertEquals(AuraVisualMode.REN, finalizado.mode());
        assertEquals(1.0F, finalizado.intensity());
    }

    @Test
    void distribuicaoRejeitaValoresInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuraDistribution(Float.NaN, 1, 1, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AuraDistribution(-1, 1, 1, 1, 1, 1));
    }

    @Test
    void zetsuZeraVisualEContinuaSeguroParaRenderer() {
        AuraVisualController controller = new AuraVisualController();
        controller.receber(AuraVisualMode.TEN, 1.0F);
        controller.avancar(1.0F);
        controller.receber(AuraVisualMode.ZETSU, 1.0F);
        AuraVisualState estado = controller.avancar(1.0F);
        assertEquals(AuraVisualMode.ZETSU, estado.mode());
        assertEquals(0.0F, estado.intensity());
        assertTrue(!estado.enabled());
    }
}
