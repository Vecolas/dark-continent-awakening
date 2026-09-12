package com.darkcontinent.nenfoundation.enemy.spawn;

import java.util.Set;

/** Regra configuravel que complementa Biome Modifiers e SpawnPlacements. */
public record SpawnRule(Set<String> biomeTags, Set<String> dimensions, int minLight,
        int maxLight, boolean requireGround, boolean allowWater, boolean requireSky,
        int maxNearbySameFaction) {
    public SpawnRule {
        if (biomeTags == null || dimensions == null || biomeTags.isEmpty() || dimensions.isEmpty()
                || minLight < 0 || maxLight > 15 || minLight > maxLight || maxNearbySameFaction < 0) {
            throw new IllegalArgumentException("regra de spawn invalida");
        }
        biomeTags = Set.copyOf(biomeTags);
        dimensions = Set.copyOf(dimensions);
    }

    public boolean permite(SpawnContext context) {
        if (context == null) throw new NullPointerException("contexto ausente");
        return biomeTags.contains(context.biomeTag()) && dimensions.contains(context.dimension())
                && context.light() >= minLight && context.light() <= maxLight
                && (!requireGround || context.onGround())
                && (allowWater || !context.inWater())
                && (!requireSky || context.skyVisible())
                && context.nearbySameFaction() < maxNearbySameFaction;
    }
}
