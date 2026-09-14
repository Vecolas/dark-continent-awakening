package com.darkcontinent.nenfoundation.enemy.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.api.CanonLevel;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.api.ThreatTier;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EnemyDefinitionRegistryTest {
    private static EnemyDefinition definition(String id) {
        return new EnemyDefinition(
                new EnemyMetadata(ResourceLocation.fromNamespaceAndPath("nenfoundation", id),
                        CanonLevel.CANON_DERIVED, EnemyFaction.WILDLIFE, ThreatTier.LOW,
                        false, false, id),
                new EnemyAttributes(10, 0.2F, 1, 0, 8, 0),
                new com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule(
                        Set.of("#nenfoundation:test"), Set.of("minecraft:overworld"), 0, 15,
                        true, false, false, 4,
                        com.darkcontinent.nenfoundation.enemy.spawn.SpawnProfile.ON_GROUND,
                        com.darkcontinent.nenfoundation.enemy.spawn.SpawnCaps.fauna()));
    }

    @Test
    void recargaTrocaSnapshotInteiroELeituraEImutavel() {
        EnemyDefinitionRegistry registry = new EnemyDefinitionRegistry();
        registry.replaceAll(List.of(definition("first")));
        assertTrue(registry.find(ResourceLocation.fromNamespaceAndPath("nenfoundation", "first")).isPresent());
        registry.replaceAll(List.of(definition("second")));
        assertTrue(registry.find(ResourceLocation.fromNamespaceAndPath("nenfoundation", "first")).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> registry.snapshot().clear());
    }

    @Test
    void cargaInvalidaNaoAplicaMetadeDoNovoSnapshot() {
        EnemyDefinitionRegistry registry = new EnemyDefinitionRegistry();
        registry.replaceAll(List.of(definition("stable")));
        assertThrows(IllegalArgumentException.class,
                () -> registry.replaceAll(List.of(definition("new"), definition("new"))));
        assertEquals(1, registry.snapshot().size());
        assertTrue(registry.find(ResourceLocation.fromNamespaceAndPath("nenfoundation", "stable")).isPresent());
    }
}
