package com.darkcontinent.nenfoundation.client.worldtree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorldTreeTransitionClientTest {
    @Test
    void fadeTemDuracaoCurtaEDeterminada() {
        assertEquals(20, WorldTreeTransitionClient.FADE_TICKS);
    }
}
