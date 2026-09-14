package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorldTreeFloraGeneratorTest {
    @Test
    void distribuicaoDeFloraEesparsaEDeterministica() {
        boolean first = WorldTreeGroveGenerator.shouldPlaceFlower(1234L, 10, 20);
        boolean second = WorldTreeGroveGenerator.shouldPlaceFlower(1234L, 10, 20);

        assertEquals(first, second);
    }
}
