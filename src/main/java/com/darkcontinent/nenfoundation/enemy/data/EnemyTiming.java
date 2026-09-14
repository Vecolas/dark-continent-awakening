package com.darkcontinent.nenfoundation.enemy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Janela temporal data-driven de uma ação; dano e regras de segurança ficam fora dela. */
public record EnemyTiming(int windupTicks, int activeTicks, int recoveryTicks,
        boolean interruptibleWindup, boolean interruptibleActive, boolean interruptibleRecovery) {
    public static final Codec<EnemyTiming> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("windup_ticks")
                    .forGetter(EnemyTiming::windupTicks),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("active_ticks")
                    .forGetter(EnemyTiming::activeTicks),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("recovery_ticks")
                    .forGetter(EnemyTiming::recoveryTicks),
            Codec.BOOL.fieldOf("interruptible_windup").forGetter(EnemyTiming::interruptibleWindup),
            Codec.BOOL.fieldOf("interruptible_active").forGetter(EnemyTiming::interruptibleActive),
            Codec.BOOL.fieldOf("interruptible_recovery").forGetter(EnemyTiming::interruptibleRecovery))
            .apply(instance, EnemyTiming::new));
}
