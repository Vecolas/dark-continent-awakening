package com.darkcontinent.nenfoundation.enemy.content;

import com.darkcontinent.nenfoundation.enemy.api.CanonLevel;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.api.ThreatTier;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPoint;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointRegistry;
import com.darkcontinent.nenfoundation.enemy.data.EnemyAttributes;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Perfis de balanceamento do primeiro vertical slice; nao registra EntityType. */
public final class HunterExamProfiles {
    private static final String MOD = "nenfoundation";
    private HunterExamProfiles() { }

    public static EnemyDefinition greatStamp() {
        return new EnemyDefinition(metadata("great_stamp", ThreatTier.HUNTER, true, true),
                new EnemyAttributes(70, 0.23F, 11, 7, 28, 0.55F),
                spawn("#nenfoundation:great_stamp_biomes", 0, 10, true, false, 4));
    }

    public static AttackDefinition greatStampCharge() {
        return new AttackDefinition("charge", 18, 6, 24, 16, 1.8F, true, false, true);
    }

    public static WeakPointRegistry greatStampWeakPoints() {
        return new WeakPointRegistry(Map.of("forehead", new WeakPoint("forehead", "head", 4.0F, true)));
    }

    public static EnemyDefinition frogInWaiting() {
        return new EnemyDefinition(metadata("frog_in_waiting", ThreatTier.DANGEROUS, true, false),
                new EnemyAttributes(50, 0.12F, 10, 3, 20, 0.35F),
                spawn("#nenfoundation:swamp_predator_biomes", 0, 7, false, true, 2));
    }

    public static EnemyDefinition foxbear() {
        return new EnemyDefinition(metadata("foxbear", ThreatTier.HUNTER, true, false),
                new EnemyAttributes(44, 0.25F, 6, 2, 20, 0.25F),
                spawn("#nenfoundation:foxbear_biomes", 0, 12, true, false, 3));
    }

    private static EnemyMetadata metadata(String id, ThreatTier tier, boolean territorial, boolean social) {
        return new EnemyMetadata(ResourceLocation.fromNamespaceAndPath(MOD, id), CanonLevel.CANON_EXACT,
                EnemyFaction.WILDLIFE, tier, territorial, social, id);
    }

    private static SpawnRule spawn(String biome, int minLight, int maxLight, boolean ground,
            boolean water, int groupLimit) {
        return new SpawnRule(Set.of(biome), Set.of("minecraft:overworld"), minLight, maxLight,
                ground, water, false, groupLimit);
    }
}
