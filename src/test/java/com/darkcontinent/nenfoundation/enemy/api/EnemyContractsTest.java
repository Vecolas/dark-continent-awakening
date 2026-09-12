package com.darkcontinent.nenfoundation.enemy.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EnemyContractsTest {
    @Test
    void metadataSeparaCanonidadeFaccaoEAmeaca() {
        EnemyMetadata stamp = new EnemyMetadata(ResourceLocation.fromNamespaceAndPath("nenfoundation", "great_stamp"),
                CanonLevel.CANON_EXACT, EnemyFaction.WILDLIFE, ThreatTier.HUNTER, true, true, "great_stamp");
        assertEquals(CanonLevel.CANON_EXACT, stamp.canonLevel());
        assertEquals(EnemyFaction.WILDLIFE, stamp.faction());
        assertEquals(ThreatTier.HUNTER, stamp.threatTier());
        assertTrue(stamp.territorial());
    }

    @Test
    void metadataRecusaIdentidadeVisualAusente() {
        assertThrows(IllegalArgumentException.class, () -> new EnemyMetadata(
                ResourceLocation.fromNamespaceAndPath("nenfoundation", "x"), CanonLevel.ORIGINAL_COMPATIBLE,
                EnemyFaction.CUSTOM, ThreatTier.LOW, false, false, " "));
    }

    @Test
    void estadosMantemTelegraphExplicito() {
        assertEquals(EnemyCombatState.WINDUP, EnemyCombatState.valueOf("WINDUP"));
        assertEquals(EnemyCombatState.ACTIVE, EnemyCombatState.valueOf("ACTIVE"));
        assertEquals(EnemyCombatState.RECOVERY, EnemyCombatState.valueOf("RECOVERY"));
        assertEquals(EnemyAwarenessState.WARN, EnemyAwarenessState.valueOf("WARN"));
        assertEquals(EnemyAwarenessState.AMBUSH, EnemyAwarenessState.valueOf("AMBUSH"));
    }
}
