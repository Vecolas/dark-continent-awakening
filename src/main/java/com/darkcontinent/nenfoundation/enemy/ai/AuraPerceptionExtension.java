package com.darkcontinent.nenfoundation.enemy.ai;

import net.minecraft.world.entity.LivingEntity;

/**
 * Ponto de extensao para aura/Nen.
 *
 * <p>O default e inerte: a framework de inimigos nao importa o dominio de Nen
 * nem inventa uma segunda autoridade sobre aura. Um adapter futuro pode negar
 * uma deteccao quando os contratos reais de Gyo/In existirem.</p>
 */
@FunctionalInterface
public interface AuraPerceptionExtension {
    boolean canDetect(LivingEntity observer, LivingEntity target);

    static AuraPerceptionExtension inerte() {
        return (observer, target) -> true;
    }
}
