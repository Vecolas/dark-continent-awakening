package com.darkcontinent.nenfoundation.enemy.ai;

import java.util.Optional;
import java.util.UUID;

/** Saída do controlador para o EnemyBrain e para o chamador que reconstrói o alvo. */
public record PerceptionResult(Optional<UUID> targetId, AwarenessInput awareness,
        boolean normalScan, boolean expensiveScan) {
    public PerceptionResult {
        if (targetId == null || awareness == null) throw new NullPointerException("resultado incompleto");
        if (expensiveScan && !normalScan) throw new IllegalArgumentException("scan caro sem scan normal");
    }
}
