package com.darkcontinent.nenfoundation.enemy.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EnemyDefinitionReloadListenerTest {
    private static final String JSON = """
            {"metadata":{"id":"example:stable","canon_level":"ORIGINAL_COMPATIBLE",
            "faction":"WILDLIFE","threat_tier":"LOW","territorial":false,"social":false,
            "visual_id":"example:stable"},"attributes":{"max_health":10.0,"movement_speed":0.2,
            "attack_damage":1.0,"armor":0.0,"follow_range":8.0,"knockback_resistance":0.0},
            "spawn":{"biome_tags":["#example:field_biomes"],"dimensions":["minecraft:overworld"],
            "min_light":0,"max_light":15,"require_ground":true,"allow_water":false,
            "require_sky":false,"max_nearby_same_faction":4},"audio_id":"example:entity/stable",
            "timings":{"idle":{"windup_ticks":1,"active_ticks":1,"recovery_ticks":1,
            "interruptible_windup":false,"interruptible_active":false,"interruptible_recovery":false}},
            "schema_version":1}
            """;

    @Test
    void arquivoInvalidoPreservaSnapshotAnterior() {
        EnemyDefinitionRegistry registry = new EnemyDefinitionRegistry();
        registry.replaceAll(java.util.List.of(definition("example:stable")));
        EnemyDefinitionReloadListener listener = new EnemyDefinitionReloadListener(registry);

        listener.apply(Map.of(ResourceLocation.parse("example:broken"),
                JsonParser.parseString(JSON.replace("ORIGINAL_COMPATIBLE", "NO_SUCH_LEVEL"))), null, null);

        assertEquals(1, registry.snapshot().size());
        assertTrue(registry.find(ResourceLocation.parse("example:stable")).isPresent());
    }

    @Test
    void idsDuplicadosPreservamSnapshotAnterior() {
        EnemyDefinitionRegistry registry = new EnemyDefinitionRegistry();
        registry.replaceAll(java.util.List.of(definition("example:stable")));
        EnemyDefinitionReloadListener listener = new EnemyDefinitionReloadListener(registry);
        var primeiro = JsonParser.parseString(JSON);
        var segundo = JsonParser.parseString(JSON);

        listener.apply(Map.of(ResourceLocation.parse("example:first"), primeiro,
                ResourceLocation.parse("example:second"), segundo), null, null);

        assertEquals(1, registry.snapshot().size());
        assertTrue(registry.find(ResourceLocation.parse("example:stable")).isPresent());
    }

    private static EnemyDefinition definition(String id) {
        return new EnemyDefinition(
                new com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata(
                        ResourceLocation.parse(id),
                        com.darkcontinent.nenfoundation.enemy.api.CanonLevel.ORIGINAL_COMPATIBLE,
                        com.darkcontinent.nenfoundation.enemy.api.EnemyFaction.WILDLIFE,
                        com.darkcontinent.nenfoundation.enemy.api.ThreatTier.LOW, false, false, id),
                new EnemyAttributes(10, 0.2F, 1, 0, 8, 0),
                new com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule(
                        java.util.Set.of("#example:field_biomes"), java.util.Set.of("minecraft:overworld"),
                        0, 15, true, false, false, 4,
                        com.darkcontinent.nenfoundation.enemy.spawn.SpawnProfile.ON_GROUND,
                        com.darkcontinent.nenfoundation.enemy.spawn.SpawnCaps.fauna()), ResourceLocation.parse(id),
                java.util.Map.of("idle", new EnemyTiming(1, 1, 1, false, false, false)), 1);
    }
}
