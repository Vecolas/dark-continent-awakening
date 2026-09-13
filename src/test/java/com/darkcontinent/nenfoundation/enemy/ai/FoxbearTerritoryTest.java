package com.darkcontinent.nenfoundation.enemy.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoxbearTerritoryTest {
    @Test
    void warnsBeforeAttacking() {
        assertEquals(FoxbearState.WARN, FoxbearTerritory.stateFor(true, false, false, true));
        assertEquals(FoxbearState.ENGAGE, FoxbearTerritory.stateFor(true, true, false, true));
    }

    @Test
    void retreatOrLeavingTerritoryStopsEngagement() {
        assertEquals(FoxbearState.RETURN_HOME, FoxbearTerritory.stateFor(true, false, true, true));
        assertEquals(FoxbearState.RETURN_HOME, FoxbearTerritory.stateFor(false, true, false, false));
    }
}
