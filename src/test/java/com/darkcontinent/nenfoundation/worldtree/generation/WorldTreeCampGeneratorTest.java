package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldTreeCampGeneratorTest {
    @Test
    void footprintDoCampEChunkLocal() {
        assertTrue(WorldTreeCampGenerator.chunkIntersects(64, 80, -16, 0, 72, 0));
        assertFalse(WorldTreeCampGenerator.chunkIntersects(256, 272, 256, 272, 72, 0));
    }
}
