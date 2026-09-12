package com.darkcontinent.nenfoundation.enemy.api;

import net.minecraft.resources.ResourceLocation;

/** Identidade estavel de uma criatura; atributos de runtime ficam fora deste record. */
public record EnemyMetadata(ResourceLocation id, CanonLevel canonLevel, EnemyFaction faction,
        ThreatTier threatTier, boolean territorial, boolean social, String visualId) {
    public EnemyMetadata {
        if (id == null || canonLevel == null || faction == null || threatTier == null) {
            throw new NullPointerException("identidade de inimigo incompleta");
        }
        if (visualId == null || visualId.isBlank()) {
            throw new IllegalArgumentException("visualId deve ser informado");
        }
    }
}
