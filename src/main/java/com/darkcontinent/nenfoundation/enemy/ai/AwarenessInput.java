package com.darkcontinent.nenfoundation.enemy.ai;

/** Leitura ja calculada pelos sensores; nao executa scans de mundo. */
public record AwarenessInput(boolean targetVisible, boolean targetAudible,
        boolean targetInTerritory, boolean targetRetreating, boolean targetLost,
        int memoryTicksRemaining, boolean ambushOpportunity, boolean healthCritical) {
    public AwarenessInput {
        if (memoryTicksRemaining < 0) throw new IllegalArgumentException("memoria negativa");
    }
}
