package com.darkcontinent.nenfoundation.worldtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldTreeAltitudeServiceTest {
    @Test
    void derivaZonaSemSobreporLimites() {
        assertEquals(WorldTreeZone.LOWER_ASCENT, WorldTreeAltitudeService.zoneAt(48));
        assertEquals(WorldTreeZone.CLOUD_SEA, WorldTreeAltitudeService.zoneAt(320));
        assertEquals(WorldTreeZone.SUMMIT, WorldTreeAltitudeService.zoneAt(1450));
        assertNull(WorldTreeAltitudeService.zoneAt(1480));
    }

    @Test
    void atalhosDeZonaCorrespondemAoContrato() {
        assertTrue(WorldTreeAltitudeService.isCloudSea(400));
        assertFalse(WorldTreeAltitudeService.isCloudSea(520));
        assertTrue(WorldTreeAltitudeService.isSummit(1400));
        assertFalse(WorldTreeAltitudeService.isSummit(1379));
    }
}
