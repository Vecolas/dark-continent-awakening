package com.darkcontinent.nenfoundation.enemy.combat;

/** Ponto fraco validado no servidor; region e resolvida pelo mob, nao pelo cliente. */
public record WeakPoint(String id, String region, float damageMultiplier, boolean enabled) {
    public WeakPoint {
        if (id == null || id.isBlank() || region == null || region.isBlank()
                || !Float.isFinite(damageMultiplier) || damageMultiplier < 1.0F) {
            throw new IllegalArgumentException("weak point invalido");
        }
    }
}
