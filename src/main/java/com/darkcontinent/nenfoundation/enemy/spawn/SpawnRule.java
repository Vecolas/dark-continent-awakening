package com.darkcontinent.nenfoundation.enemy.spawn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;

/** Regra configuravel que complementa Biome Modifiers e SpawnPlacements. */
public record SpawnRule(Set<String> biomeTags, Set<String> dimensions, int minLight,
        int maxLight, boolean requireGround, boolean allowWater, boolean requireSky,
        int maxNearbySameFaction) {
    private static final Codec<Set<String>> STRING_SET = Codec.STRING.listOf()
            .xmap(Set::copyOf, List::copyOf);

    public static final Codec<SpawnRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STRING_SET.fieldOf("biome_tags").forGetter(SpawnRule::biomeTags),
            STRING_SET.fieldOf("dimensions").forGetter(SpawnRule::dimensions),
            Codec.INT.fieldOf("min_light").forGetter(SpawnRule::minLight),
            Codec.INT.fieldOf("max_light").forGetter(SpawnRule::maxLight),
            Codec.BOOL.fieldOf("require_ground").forGetter(SpawnRule::requireGround),
            Codec.BOOL.fieldOf("allow_water").forGetter(SpawnRule::allowWater),
            Codec.BOOL.fieldOf("require_sky").forGetter(SpawnRule::requireSky),
            Codec.INT.fieldOf("max_nearby_same_faction").forGetter(SpawnRule::maxNearbySameFaction))
            .apply(instance, SpawnRule::new));

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
