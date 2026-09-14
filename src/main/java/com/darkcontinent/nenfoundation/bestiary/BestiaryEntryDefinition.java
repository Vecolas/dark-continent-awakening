package com.darkcontinent.nenfoundation.bestiary;

import net.minecraft.resources.ResourceLocation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Definição editorial da ficha; números exatos ficam fora da experiência normal. */
public record BestiaryEntryDefinition(
        ResourceLocation id,
        ResourceLocation entityType,
        BestiaryCategory category,
        int threat,
        int studiedAt,
        int masteredAt,
        String habitatKey,
        String summaryKey,
        String behaviorKey,
        String combatKey) {
    public static final Codec<BestiaryEntryDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("entity_type").forGetter(BestiaryEntryDefinition::entityType),
            Codec.STRING.comapFlatMap(BestiaryEntryDefinition::parseCategory, Enum::name)
                    .fieldOf("category").forGetter(BestiaryEntryDefinition::category),
            Codec.intRange(1, 5).fieldOf("threat").forGetter(BestiaryEntryDefinition::threat),
            Codec.intRange(1, 1000).optionalFieldOf("studied_at", 3).forGetter(BestiaryEntryDefinition::studiedAt),
            Codec.intRange(1, 1000).optionalFieldOf("mastered_at", 8).forGetter(BestiaryEntryDefinition::masteredAt),
            Codec.STRING.fieldOf("habitat").forGetter(BestiaryEntryDefinition::habitatKey),
            Codec.STRING.fieldOf("summary").forGetter(BestiaryEntryDefinition::summaryKey),
            Codec.STRING.fieldOf("behavior").forGetter(BestiaryEntryDefinition::behaviorKey),
            Codec.STRING.fieldOf("combat").forGetter(BestiaryEntryDefinition::combatKey)
    ).apply(instance, (entityType, category, threat, studiedAt, masteredAt, habitat, summary, behavior, combat) ->
            fromData(entityType, category, threat, studiedAt, masteredAt,
                    habitat, summary, behavior, combat)));

    private static BestiaryEntryDefinition fromData(ResourceLocation entityType, BestiaryCategory category,
            int threat, int studiedAt, int masteredAt, String habitat, String summary,
            String behavior, String combat) {
        return new BestiaryEntryDefinition(entityType, entityType, category, threat, studiedAt, masteredAt,
                habitat, summary, behavior, combat);
    }

    private static DataResult<BestiaryCategory> parseCategory(String value) {
        try {
            return DataResult.success(BestiaryCategory.valueOf(value));
        } catch (IllegalArgumentException error) {
            return DataResult.error(() -> "categoria de bestiario desconhecida: " + value);
        }
    }

    public BestiaryEntryDefinition {
        if (threat < 1 || threat > 5 || studiedAt > masteredAt) {
            throw new IllegalArgumentException("limiares do bestiario invalidos");
        }
    }
}
