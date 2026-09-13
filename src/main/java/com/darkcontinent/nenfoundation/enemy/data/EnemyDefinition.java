package com.darkcontinent.nenfoundation.enemy.data;

import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** Definicao agnostica de runtime para balanceamento e datapacks futuros. */
public record EnemyDefinition(EnemyMetadata metadata, EnemyAttributes attributes, SpawnRule spawnRule,
        ResourceLocation audioId) {
    public static final Codec<EnemyDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EnemyMetadata.CODEC.fieldOf("metadata").forGetter(EnemyDefinition::metadata),
            EnemyAttributes.CODEC.fieldOf("attributes").forGetter(EnemyDefinition::attributes),
            SpawnRule.CODEC.fieldOf("spawn").forGetter(EnemyDefinition::spawnRule),
            ResourceLocation.CODEC.fieldOf("audio_id").forGetter(EnemyDefinition::audioId))
            .apply(instance, EnemyDefinition::new));

    /** Compatibilidade explícita dos perfis Java; definições JSON exigem audio_id. */
    public EnemyDefinition(EnemyMetadata metadata, EnemyAttributes attributes, SpawnRule spawnRule) {
        this(metadata, attributes, spawnRule, metadata.id());
    }

    public EnemyDefinition {
        if (metadata == null || attributes == null || spawnRule == null) {
            throw new NullPointerException("definicao de inimigo incompleta");
        }
        if (audioId == null) throw new NullPointerException("audioId ausente");
    }
}
