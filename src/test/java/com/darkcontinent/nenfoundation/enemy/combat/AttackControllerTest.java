package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class AttackControllerTest {
    private static final AttackDefinition ATAQUE =
            new AttackDefinition("strike", 2, 2, 1, 8.0F, 0.5F, true, false, true);
    private static final AttackHitbox HITBOX = new AttackHitbox(-0.5D, 0.0D, 0.0D,
            0.5D, 2.0D, 2.0D);
    private static final Vec3 ORIGEM = new Vec3(0.0D, 0.0D, 0.0D);

    @Test
    void soAJanelaActivePodeAtingirEUmAlvoNaoRecebeDoisHits() {
        AttackController controller = new AttackController(2);
        long instancia = controller.start(ATAQUE);
        assertEquals(1L, instancia);
        assertTrue(controller.tryHit(7, new AABB(-0.25, 0.5, 1.0, 0.25, 1.5, 1.5),
                HITBOX, ORIGEM, 0.0F, "body").isEmpty());

        controller.tick();
        controller.tick();
        assertEquals(AttackPhase.ACTIVE, controller.phase());
        var primeiro = controller.tryHit(7, new AABB(-0.25, 0.5, 1.0, 0.25, 1.5, 1.5),
                HITBOX, ORIGEM, 0.0F, "body");
        assertTrue(primeiro.isPresent());
        assertEquals(8.0F, primeiro.orElseThrow().damage());
        assertTrue(controller.tryHit(7, new AABB(-0.25, 0.5, 1.0, 0.25, 1.5, 1.5),
                HITBOX, ORIGEM, 0.0F, "body").isEmpty());
        assertTrue(controller.tryHit(8, new AABB(-0.25, 0.5, 1.5, 0.25, 1.5, 1.9),
                HITBOX, ORIGEM, 0.0F, "forehead").isPresent());
    }

    @Test
    void hitboxLocalGiraComYawDoAtacante() {
        AttackController controller = new AttackController(0);
        controller.start(ATAQUE);
        controller.tick();
        controller.tick();

        assertTrue(controller.tryHit(4, new AABB(-1.5, 0.5, -0.25, -1.0, 1.5, 0.25),
                HITBOX, ORIGEM, 90.0F, "body").isPresent());
        assertFalse(controller.tryHit(5, new AABB(1.0, 0.5, -0.25, 1.5, 1.5, 0.25),
                HITBOX, ORIGEM, 90.0F, "body").isPresent());
    }

    @Test
    void instanciaNovaLimpaAtingidosMasRespeitaCooldown() {
        AttackController controller = new AttackController(2);
        controller.start(ATAQUE);
        for (int i = 0; i < 5; i++) controller.tick();
        assertEquals(AttackPhase.COMPLETE, controller.phase());
        assertEquals(2, controller.cooldownRemaining());
        assertThrows(IllegalStateException.class, () -> controller.start(ATAQUE));
        controller.tick();
        controller.tick();
        long segunda = controller.start(ATAQUE);
        assertEquals(2L, segunda);
    }

    @Test
    void cooldownNegativoEAlvoInvalidoReprovam() {
        assertThrows(IllegalArgumentException.class, () -> new AttackController(-1));
        AttackController controller = new AttackController(0);
        controller.start(ATAQUE);
        controller.tick();
        controller.tick();
        assertThrows(IllegalArgumentException.class, () -> controller.tryHit(-1,
                new AABB(0, 0, 0, 1, 1, 1), HITBOX, ORIGEM, 0.0F, "body"));
    }
}
