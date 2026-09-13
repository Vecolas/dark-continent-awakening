package com.darkcontinent.nenfoundation.enemy.combat;

/** Resultado autoritativo de um único alvo atingido por uma instância de ataque. */
public record AttackHit(int targetId, long attackInstanceId, String region, float damage) {
    public AttackHit {
        if (targetId < 0 || attackInstanceId < 1 || region == null || region.isBlank()
                || !Float.isFinite(damage) || damage < 0.0F) {
            throw new IllegalArgumentException("resultado de ataque invalido");
        }
    }
}
