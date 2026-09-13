package com.darkcontinent.nenfoundation.worldtree.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeDebugCommands;
import org.junit.jupiter.api.Test;

class WorldTreeEcologyServiceTest {
    @Test
    void derivaHabitatsPorAltitude() {
        assertEquals(WorldTreeHabitat.TRUNK,
                WorldTreeEcologyService.habitatAt(WorldTreeDebugCommands.WORLD_TREE_LEVEL, 200));
        assertEquals(WorldTreeHabitat.CLOUD_LAYER,
                WorldTreeEcologyService.habitatAt(WorldTreeDebugCommands.WORLD_TREE_LEVEL, 400));
        assertEquals(WorldTreeHabitat.CROWN,
                WorldTreeEcologyService.habitatAt(WorldTreeDebugCommands.WORLD_TREE_LEVEL, 1400));
        assertNull(WorldTreeEcologyService.habitatAt(WorldTreeDebugCommands.WORLD_TREE_LEVEL, 1500));
    }

    @Test
    void spiderEagleFicaNasFaixasAereas() {
        assertTrue(WorldTreeEcologyService.speciesAllowed(
                WorldTreeDebugCommands.WORLD_TREE_LEVEL, 400, "spider_eagle"));
        assertTrue(WorldTreeEcologyService.speciesAllowed(
                WorldTreeDebugCommands.WORLD_TREE_LEVEL, 900, "spider_eagle"));
        assertFalse(WorldTreeEcologyService.speciesAllowed(
                WorldTreeDebugCommands.WORLD_TREE_LEVEL, 200, "spider_eagle"));
    }

    @Test
    void tierAumentaComAltitudeSemEstadoCongelado() {
        assertEquals(1, WorldTreeEcologyService.resourceTier(
                WorldTreeDebugCommands.WORLD_TREE_LEVEL, 200));
        assertEquals(3, WorldTreeEcologyService.resourceTier(
                WorldTreeDebugCommands.WORLD_TREE_LEVEL, 900));
        assertEquals(0, WorldTreeEcologyService.resourceTier(
                WorldTreeDebugCommands.WORLD_TREE_LEVEL, 1500));
    }
}
