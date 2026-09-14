package com.darkcontinent.nenfoundation.enemy.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** Identidade estavel de uma criatura; atributos de runtime ficam fora deste record. */
public record EnemyMetadata(ResourceLocation id, CanonLevel canonLevel, EnemyFaction faction,
        ThreatTier threatTier, boolean territorial, boolean social, String visualId) {
    public static final Codec<EnemyMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(EnemyMetadata::id),
            EnemyCodecs.enumCodec(CanonLevel.class).fieldOf("canon_level")
                    .forGetter(EnemyMetadata::canonLevel),
            EnemyCodecs.enumCodec(EnemyFaction.class).fieldOf("faction")
                    .forGetter(EnemyMetadata::faction),
            EnemyCodecs.enumCodec(ThreatTier.class).fieldOf("threat_tier")
                    .forGetter(EnemyMetadata::threatTier),
            Codec.BOOL.fieldOf("territorial").forGetter(EnemyMetadata::territorial),
            Codec.BOOL.fieldOf("social").forGetter(EnemyMetadata::social),
            Codec.STRING.fieldOf("visual_id").forGetter(EnemyMetadata::visualId))
            .apply(instance, EnemyMetadata::new));

    public EnemyMetadata {
        if (id == null || canonLevel == null || faction == null || threatTier == null) {
            throw new NullPointerException("identidade de inimigo incompleta");
        }
        if (visualId == null || visualId.isBlank()) {
            throw new IllegalArgumentException("visualId deve ser informado");
        }
    }
}
