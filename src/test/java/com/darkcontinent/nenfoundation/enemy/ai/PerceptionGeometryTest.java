package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class PerceptionGeometryTest {
    @Test
    void coneAceitaFrenteERecusaCostasOuForaDoAlcance() {
        assertTrue(PerceptionGeometry.insideCone(Vec3.ZERO, new Vec3(1, 0, 0),
                new Vec3(8, 0, 0), 16, 45));
        assertFalse(PerceptionGeometry.insideCone(Vec3.ZERO, new Vec3(1, 0, 0),
                new Vec3(-8, 0, 0), 16, 45));
        assertFalse(PerceptionGeometry.insideCone(Vec3.ZERO, new Vec3(1, 0, 0),
                new Vec3(20, 0, 0), 16, 45));
    }

    @Test
    void coneNaoTransformaVetorDegeneradoEmAlvoVisivel() {
        assertFalse(PerceptionGeometry.insideCone(Vec3.ZERO, Vec3.ZERO,
                Vec3.ZERO, 16, 90));
    }
}
