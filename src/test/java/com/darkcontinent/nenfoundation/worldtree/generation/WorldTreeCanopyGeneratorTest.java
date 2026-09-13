package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import org.junit.jupiter.api.Test;

class WorldTreeCanopyGeneratorTest {
    @Test
    void raioDoClusterEDeterministicoELimitado() {
        double first = WorldTreeCanopyGenerator.clusterRadius(42L, 2, 1);

        assertEquals(first, WorldTreeCanopyGenerator.clusterRadius(42L, 2, 1));
        assertTrue(first >= 6.0 && first <= 18.0);
    }

    @Test
    void envelopeRespeitaChunkEPermiteFronteira() {
        WorldTreePoint center = new WorldTreePoint(15.5, 900, 15.5);

        assertTrue(WorldTreeCanopyGenerator.chunkIntersectsCluster(0, 16, 0, 16, center, 6));
        assertFalse(WorldTreeCanopyGenerator.chunkIntersectsCluster(512, 528, 512, 528,
                center, 6));
    }
}
