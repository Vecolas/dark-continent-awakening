package com.darkcontinent.nenfoundation.enemy.data;

import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Definicao agnostica de runtime para balanceamento e datapacks futuros. */
public record EnemyDefinition(EnemyMetadata metadata, EnemyAttributes attributes, SpawnRule spawnRule,
        ResourceLocation audioId, Map<String, EnemyTiming> timings, int schemaVersion) {
    public static final int SCHEMA_VERSION = 1;
    public static final Codec<EnemyDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EnemyMetadata.CODEC.fieldOf("metadata").forGetter(EnemyDefinition::metadata),
            EnemyAttributes.CODEC.fieldOf("attributes").forGetter(EnemyDefinition::attributes),
            SpawnRule.CODEC.fieldOf("spawn").forGetter(EnemyDefinition::spawnRule),
            ResourceLocation.CODEC.fieldOf("audio_id").forGetter(EnemyDefinition::audioId),
            Codec.unboundedMap(Codec.STRING, EnemyTiming.CODEC).fieldOf("timings")
                    .forGetter(EnemyDefinition::timings),
            Codec.intRange(SCHEMA_VERSION, SCHEMA_VERSION).optionalFieldOf("schema_version", SCHEMA_VERSION)
                    .forGetter(EnemyDefinition::schemaVersion))
            .apply(instance, EnemyDefinition::new));

    /** Compatibilidade explícita dos perfis Java; definições JSON exigem audio_id. */
    public EnemyDefinition(EnemyMetadata metadata, EnemyAttributes attributes, SpawnRule spawnRule) {
        this(metadata, attributes, spawnRule, metadata.id(), Map.of(), SCHEMA_VERSION);
    }

    /** Compatibilidade explícita para perfis Java com timings declarados em código legado. */
    public EnemyDefinition(EnemyMetadata metadata, EnemyAttributes attributes, SpawnRule spawnRule,
            ResourceLocation audioId) {
        this(metadata, attributes, spawnRule, audioId, Map.of(), SCHEMA_VERSION);
    }

    public EnemyDefinition {
        if (metadata == null || attributes == null || spawnRule == null) {
            throw new NullPointerException("definicao de inimigo incompleta");
        }
        if (audioId == null) throw new NullPointerException("audioId ausente");
        if (timings == null) throw new NullPointerException("timings ausentes");
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("schema de inimigo nao suportado: " + schemaVersion);
        }
        timings = Map.copyOf(timings);
    }
}
