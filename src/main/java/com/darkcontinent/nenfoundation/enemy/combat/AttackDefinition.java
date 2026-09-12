package com.darkcontinent.nenfoundation.enemy.combat;

/** Definicao imutavel de ataque; o dano so e aplicado durante ACTIVE. */
public record AttackDefinition(String id, int windupTicks, int activeTicks, int recoveryTicks,
        float damage, float knockback, boolean interruptibleWindup,
        boolean interruptibleActive, boolean interruptibleRecovery) {
    public AttackDefinition {
        if (id == null || id.isBlank() || windupTicks < 1 || activeTicks < 1 || recoveryTicks < 1
                || !Float.isFinite(damage) || damage < 0.0F || !Float.isFinite(knockback) || knockback < 0.0F) {
            throw new IllegalArgumentException("definicao de ataque invalida");
        }
    }
}
