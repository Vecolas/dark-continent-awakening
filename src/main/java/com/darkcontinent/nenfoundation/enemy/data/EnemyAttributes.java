package com.darkcontinent.nenfoundation.enemy.data;

/** Atributos de balanceamento; EntityAttributes sera montado na integracao NeoForge. */
public record EnemyAttributes(float maxHealth, float movementSpeed, float attackDamage,
        float armor, float followRange, float knockbackResistance) {
    public EnemyAttributes {
        if (!Float.isFinite(maxHealth) || maxHealth <= 0 || !Float.isFinite(movementSpeed)
                || movementSpeed < 0 || !Float.isFinite(attackDamage) || attackDamage < 0
                || !Float.isFinite(armor) || armor < 0 || !Float.isFinite(followRange) || followRange < 0
                || !Float.isFinite(knockbackResistance) || knockbackResistance < 0 || knockbackResistance > 1) {
            throw new IllegalArgumentException("atributos de inimigo invalidos");
        }
    }
}
