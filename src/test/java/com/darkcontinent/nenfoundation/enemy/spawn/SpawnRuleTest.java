package com.darkcontinent.nenfoundation.enemy.spawn;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SpawnRuleTest {
    private static final SpawnRule RULE = new SpawnRule(
            Set.of("nenfoundation:great_stamp_biomes"), Set.of("minecraft:overworld"),
            0, 10, true, false, false, 4);

    @Test
    void validaBiomeDimensaoSoloLuzAguaELimiteDeGrupo() {
        assertTrue(RULE.permite(new SpawnContext("nenfoundation:great_stamp_biomes",
                "minecraft:overworld", 8, true, false, true, 2)));
        assertFalse(RULE.permite(new SpawnContext("minecraft:plains", "minecraft:overworld",
                8, true, false, true, 2)));
        assertFalse(RULE.permite(new SpawnContext("nenfoundation:great_stamp_biomes",
                "minecraft:overworld", 8, false, false, true, 2)));
        assertFalse(RULE.permite(new SpawnContext("nenfoundation:great_stamp_biomes",
                "minecraft:overworld", 8, true, true, true, 2)));
        assertFalse(RULE.permite(new SpawnContext("nenfoundation:great_stamp_biomes",
                "minecraft:overworld", 8, true, false, true, 4)));
    }

    @Test
    void regrasImpossiveisSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class, () -> new SpawnRule(
                Set.of("x"), Set.of("y"), 12, 4, true, false, false, 1));
        assertThrows(IllegalArgumentException.class, () -> new SpawnContext(
                "x", "y", 16, true, false, true, 0));
    }
}
