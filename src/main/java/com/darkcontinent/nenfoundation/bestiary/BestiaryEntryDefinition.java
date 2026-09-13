package com.darkcontinent.nenfoundation.bestiary;

import net.minecraft.resources.ResourceLocation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Definição editorial da ficha; números exatos ficam fora da experiência normal. */
public record BestiaryEntryDefinition(
        ResourceLocation id,
        ResourceLocation entityType,
        BestiaryCategory category,
        int threat,
        String habitatKey,
        String summaryKey,
        String behaviorKey,
        String combatKey) {
    public static final Codec<BestiaryEntryDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("entity_type").forGetter(BestiaryEntryDefinition::entityType),
            Codec.STRING.xmap(BestiaryCategory::valueOf, Enum::name).fieldOf("category").forGetter(BestiaryEntryDefinition::category),
            Codec.intRange(1, 5).fieldOf("threat").forGetter(BestiaryEntryDefinition::threat),
            Codec.STRING.fieldOf("habitat").forGetter(BestiaryEntryDefinition::habitatKey),
            Codec.STRING.fieldOf("summary").forGetter(BestiaryEntryDefinition::summaryKey),
            Codec.STRING.fieldOf("behavior").forGetter(BestiaryEntryDefinition::behaviorKey),
            Codec.STRING.fieldOf("combat").forGetter(BestiaryEntryDefinition::combatKey)
    ).apply(instance, (entityType, category, threat, habitat, summary, behavior, combat) ->
            new BestiaryEntryDefinition(entityType, entityType, category, threat, habitat, summary, behavior, combat)));

    public BestiaryEntryDefinition {
        if (threat < 1 || threat > 5) throw new IllegalArgumentException("threat deve estar entre I e V");
    }
}
