package com.darkcontinent.nenfoundation.bestiary;

import net.minecraft.resources.ResourceLocation;

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
    public BestiaryEntryDefinition {
        if (threat < 1 || threat > 5) throw new IllegalArgumentException("threat deve estar entre I e V");
    }
}
