package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorldTreeCanopyGeneratorTest {
    @Test
    void raioDoClusterEDeterministicoELimitado() {
        double first = WorldTreeCanopyGenerator.clusterRadius(42L, 2, 1);

        assertEquals(first, WorldTreeCanopyGenerator.clusterRadius(42L, 2, 1));
        assertTrue(first >= 2.0 && first <= 10.0);
    }

    @Test
    void clusterNasceAcimaDoGalhoESemCongelarASeed() {
        WorldTreeSpline branch = new WorldTreeSpline(List.of(
                new WorldTreePoint(0, 600, 0),
                new WorldTreePoint(16, 604, 0),
                new WorldTreePoint(42, 612, 6),
                new WorldTreePoint(80, 620, 12)), 8, 3);

        WorldTreePoint center = WorldTreeCanopyGenerator.foliageCenter(
                branch, new WorldTreePoint(42, 612, 6), 6.0, 42L, 2, 1);

        assertTrue(center.y() > 612.0);
        assertEquals(center, WorldTreeCanopyGenerator.foliageCenter(
                branch, new WorldTreePoint(42, 612, 6), 6.0, 42L, 2, 1));
    }

    @Test
    void envelopeRespeitaChunkEPermiteFronteira() {
        WorldTreePoint center = new WorldTreePoint(15.5, 900, 15.5);

        assertTrue(WorldTreeCanopyGenerator.chunkIntersectsCluster(0, 16, 0, 16, center, 6));
        assertFalse(WorldTreeCanopyGenerator.chunkIntersectsCluster(512, 528, 512, 528,
                center, 6));
    }
}
