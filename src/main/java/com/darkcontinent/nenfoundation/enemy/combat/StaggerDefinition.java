package com.darkcontinent.nenfoundation.enemy.combat;

/** Parâmetros imutáveis do medidor de stagger, fornecidos pelo perfil do mob. */
public record StaggerDefinition(float threshold, float resistance, float decayPerTick,
        int durationTicks) {
    public StaggerDefinition {
        if (!Float.isFinite(threshold) || threshold <= 0.0F
                || !Float.isFinite(resistance) || resistance < 0.0F
                || !Float.isFinite(decayPerTick) || decayPerTick < 0.0F
                || durationTicks < 1) {
            throw new IllegalArgumentException("definicao de stagger invalida");
        }
    }
}
