package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorldTreeTrunkGeneratorTest {
    @Test
    void baseComecaImediatamenteAcimaDaBedrock() {
        assertEquals(-63, WorldTreeTrunkGenerator.overworldBottomY(-64));
        assertEquals(1, WorldTreeTrunkGenerator.overworldBottomY(0));
    }
}
