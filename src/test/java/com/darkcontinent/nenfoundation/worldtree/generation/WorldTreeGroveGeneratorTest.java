package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class WorldTreeGroveGeneratorTest {
    @Test
    void decoracaoEReproduzivelPorCoordenada() {
        assertEquals(WorldTreeGroveGenerator.shouldDecorate(42L, 10, -20),
                WorldTreeGroveGenerator.shouldDecorate(42L, 10, -20));
        assertNotEquals(WorldTreeGroveGenerator.shouldDecorate(42L, 10, -20),
                WorldTreeGroveGenerator.shouldDecorate(43L, 10, -20));
    }

    @Test
    void mossSecundarioTambemEDeterministico() {
        assertEquals(WorldTreeGroveGenerator.shouldPlaceMoss(99L, 3, 7),
                WorldTreeGroveGenerator.shouldPlaceMoss(99L, 3, 7));
    }
}
