package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StaggerControllerTest {
    private static final StaggerDefinition DEF = new StaggerDefinition(5.0F, 1.0F, 0.5F, 3);

    @Test
    void resistenciaLimiarDecayEDuracao() {
        StaggerController controller = new StaggerController(DEF);
        assertEquals(StaggerResult.Outcome.ACCUMULATED, controller.apply(3.0F).outcome());
        assertEquals(2.0F, controller.meter());
        controller.tick();
        assertEquals(1.5F, controller.meter());
        assertEquals(StaggerResult.Outcome.APPLIED, controller.apply(4.5F).outcome());
        assertEquals(3, controller.remainingTicks());
        controller.tick();
        assertEquals(2, controller.remainingTicks());
    }

    @Test
    void repeticaoDuranteStaggerNaoAcumulaNemReaplica() {
        StaggerController controller = new StaggerController(DEF);
        controller.apply(6.0F);
        StaggerResult result = controller.apply(100.0F);
        assertEquals(StaggerResult.Outcome.IGNORED, result.outcome());
        assertEquals(3, result.remainingTicks());
    }

    @Test
    void entradasInvalidasNaoViraramStaggerSilencioso() {
        assertThrows(IllegalArgumentException.class, () -> controller().apply(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new StaggerDefinition(0.0F, 0, 0, 1));
    }

    private static StaggerController controller() { return new StaggerController(DEF); }
}
