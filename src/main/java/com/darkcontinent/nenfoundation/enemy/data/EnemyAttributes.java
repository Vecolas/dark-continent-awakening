package com.darkcontinent.nenfoundation.enemy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Atributos de balanceamento; EntityAttributes sera montado na integracao NeoForge. */
public record EnemyAttributes(float maxHealth, float movementSpeed, float attackDamage,
        float armor, float followRange, float knockbackResistance) {
    public static final Codec<EnemyAttributes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("max_health").forGetter(EnemyAttributes::maxHealth),
            Codec.FLOAT.fieldOf("movement_speed").forGetter(EnemyAttributes::movementSpeed),
            Codec.FLOAT.fieldOf("attack_damage").forGetter(EnemyAttributes::attackDamage),
            Codec.FLOAT.fieldOf("armor").forGetter(EnemyAttributes::armor),
            Codec.FLOAT.fieldOf("follow_range").forGetter(EnemyAttributes::followRange),
            Codec.FLOAT.fieldOf("knockback_resistance").forGetter(EnemyAttributes::knockbackResistance))
            .apply(instance, EnemyAttributes::new));

    public EnemyAttributes {
        if (!Float.isFinite(maxHealth) || maxHealth <= 0 || !Float.isFinite(movementSpeed)
                || movementSpeed < 0 || !Float.isFinite(attackDamage) || attackDamage < 0
                || !Float.isFinite(armor) || armor < 0 || !Float.isFinite(followRange) || followRange < 0
                || !Float.isFinite(knockbackResistance) || knockbackResistance < 0 || knockbackResistance > 1) {
            throw new IllegalArgumentException("atributos de inimigo invalidos");
        }
    }
}
