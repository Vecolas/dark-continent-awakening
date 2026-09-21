package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.api.CanonLevel;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class TargetEvaluatorTest {
    private static final EnemyMetadata TERRITORIAL = new EnemyMetadata(
            ResourceLocation.fromNamespaceAndPath("test", "guard"), CanonLevel.CANON_DERIVED,
            EnemyFaction.WILDLIFE, com.darkcontinent.nenfoundation.enemy.api.ThreatTier.LOW,
            true, false, "guard");

    @Test
    void respeitaFaccaoDistanciaEterritorio() {
        TargetEvaluation prey = TargetEvaluator.evaluate(TERRITORIAL,
                EnemyFaction.CIVILIAN, 8, 32, true, true, FactionRelations.padrao());
        assertFalse(prey.accepted());
        assertEquals("faction_neutral", prey.rejectionReason());

        TargetEvaluation hostile = TargetEvaluator.evaluate(new EnemyMetadata(
                ResourceLocation.fromNamespaceAndPath("test", "ant"), CanonLevel.CANON_DERIVED,
                EnemyFaction.CHIMERA_ANT, com.darkcontinent.nenfoundation.enemy.api.ThreatTier.LOW,
                true, false, "ant"), EnemyFaction.HUNTER_ASSOCIATION, 8, 32,
                true, true, FactionRelations.padrao());
        assertTrue(hostile.accepted());
        assertEquals("accepted", hostile.rejectionReason());
    }

    @Test
    void recusaDimensaoDistanciaETerritorio() {
        assertEquals("wrong_dimension", TargetEvaluator.evaluate(TERRITORIAL,
                EnemyFaction.HUNTER_ASSOCIATION, 8, 32, false, true,
                FactionRelations.padrao()).rejectionReason());
        assertEquals("out_of_range", TargetEvaluator.evaluate(TERRITORIAL,
                EnemyFaction.HUNTER_ASSOCIATION, 40, 32, true, true,
                FactionRelations.padrao()).rejectionReason());
        assertEquals("outside_territory", TargetEvaluator.evaluate(new EnemyMetadata(
                ResourceLocation.fromNamespaceAndPath("test", "ant2"), CanonLevel.CANON_DERIVED,
                EnemyFaction.CHIMERA_ANT, com.darkcontinent.nenfoundation.enemy.api.ThreatTier.LOW,
                true, false, "ant2"), EnemyFaction.HUNTER_ASSOCIATION, 8, 32, true, false,
                FactionRelations.padrao()).rejectionReason());
    }
}
