package com.darkcontinent.nenfoundation.enemy.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Resultado de uma varredura; os consumidores nao repetem scans no mesmo tick. */
public record EnemyPerceptionSnapshot(LivingEntity target, boolean targetVisible,
        boolean targetAudible, boolean targetInTerritory, boolean targetLost,
        int memoryTicksRemaining, Vec3 audibleNoise, boolean scannedThisTick) {
    public static EnemyPerceptionSnapshot empty() {
        return new EnemyPerceptionSnapshot(null, false, false, false, true, 0, null, false);
    }
}
