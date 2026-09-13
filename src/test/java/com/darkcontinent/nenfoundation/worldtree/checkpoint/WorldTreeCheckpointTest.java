package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class WorldTreeCheckpointTest {
    @Test
    void nearestAceitaSomenteJanelaDoAnchor() {
        assertEquals(WorldTreeCheckpoint.CLOUD, WorldTreeCheckpoint.nearest(400));
        assertEquals(WorldTreeCheckpoint.SUMMIT, WorldTreeCheckpoint.nearest(1470));
        assertNull(WorldTreeCheckpoint.nearest(500));
    }
}
