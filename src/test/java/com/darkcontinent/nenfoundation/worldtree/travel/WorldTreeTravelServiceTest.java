package com.darkcontinent.nenfoundation.worldtree.travel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldTreeTravelServiceTest {
    @Test
    void gateOverworldExigeFaixaVerticalEProximidade() {
        assertTrue(WorldTreeTravelService.isOverworldGate(100, 284, 200, 100, 200));
        assertFalse(WorldTreeTravelService.isOverworldGate(100, 240, 200, 100, 200));
        assertFalse(WorldTreeTravelService.isOverworldGate(130, 284, 200, 100, 200));
    }

    @Test
    void gateDaDimensaoFicaNaEntradaInferior() {
        assertTrue(WorldTreeTravelService.isWorldTreeGate(0, 48, 0));
        assertFalse(WorldTreeTravelService.isWorldTreeGate(0, 120, 0));
        assertFalse(WorldTreeTravelService.isWorldTreeGate(20, 48, 0));
    }

    @Test
    void entradaDoOverworldReencontraAArvoreEmAlturaIntermediaria() {
        assertTrue(WorldTreeTravelService.worldTreeEntryY() > 48);
        assertTrue(WorldTreeTravelService.worldTreeEntryY() < 400);
        assertEquals(200, WorldTreeTravelService.worldTreeEntryY());
    }
}
