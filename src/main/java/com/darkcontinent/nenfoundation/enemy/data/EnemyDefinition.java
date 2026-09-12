package com.darkcontinent.nenfoundation.enemy.data;

import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.spawn.SpawnRule;

/** Definicao agnostica de runtime para balanceamento e datapacks futuros. */
public record EnemyDefinition(EnemyMetadata metadata, EnemyAttributes attributes, SpawnRule spawnRule) {
    public EnemyDefinition {
        if (metadata == null || attributes == null || spawnRule == null) {
            throw new NullPointerException("definicao de inimigo incompleta");
        }
    }
}
