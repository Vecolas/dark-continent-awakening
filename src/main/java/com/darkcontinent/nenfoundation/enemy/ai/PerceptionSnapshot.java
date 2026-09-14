package com.darkcontinent.nenfoundation.enemy.ai;

import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import java.util.UUID;

/** Leitura já medida no servidor para um candidato; não contém entidade nem mundo. */
public record PerceptionSnapshot(UUID targetId, String dimension, double distance,
        double forwardDot, boolean lineOfSight, boolean audible, boolean targetRetreating,
        boolean targetAlive, boolean targetInTerritory, EnemyFaction targetFaction) {
    public PerceptionSnapshot {
        if (targetId == null || dimension == null || dimension.isBlank()
                || !Double.isFinite(distance) || distance < 0.0D
                || !Double.isFinite(forwardDot) || forwardDot < -1.0D || forwardDot > 1.0D
                || targetFaction == null) {
            throw new IllegalArgumentException("snapshot de percepcao invalido");
        }
    }
}
