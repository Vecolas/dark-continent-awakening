package com.darkcontinent.nenfoundation.enemy.ai;

import java.util.UUID;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Ruido emitido por evento; nao e uma varredura de blocos ou entidades. */
public record EnemyHearingSignal(Level level, Vec3 position, UUID sourceId,
        double radius, long gameTime) {
    public EnemyHearingSignal {
        if (level == null || position == null || !Double.isFinite(radius) || radius <= 0.0) {
            throw new IllegalArgumentException("sinal sonoro invalido");
        }
    }
}
