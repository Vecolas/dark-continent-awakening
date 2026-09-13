package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldTreeTrunkGeneratorTest {
    @Test
    void chunksDistantesSaoDescartadosAntesDoLoopVertical() {
        assertFalse(WorldTreeTrunkGenerator.chunkIntersectsTrunk(160, 176, 0, 16));
        assertFalse(WorldTreeTrunkGenerator.chunkIntersectsTrunk(0, 16, -176, -160));
        assertTrue(WorldTreeTrunkGenerator.chunkIntersectsTrunk(-16, 0, -16, 0));
    }

    @Test
    void raioIrregularEReproduzivelPorCoordenada() {
        double primeiro = WorldTreeTrunkGenerator.irregularRadius(30.0, 12, 400, -7, 99L);
        double segundo = WorldTreeTrunkGenerator.irregularRadius(30.0, 12, 400, -7, 99L);
        double diferente = WorldTreeTrunkGenerator.irregularRadius(30.0, 13, 400, -7, 99L);

        assertEquals(primeiro, segundo);
        assertNotEquals(primeiro, diferente);
        assertTrue(primeiro > 0.0);
    }
}
