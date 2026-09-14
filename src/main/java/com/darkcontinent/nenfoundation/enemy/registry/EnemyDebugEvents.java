package com.darkcontinent.nenfoundation.enemy.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.api.HxHEnemy;
import com.darkcontinent.nenfoundation.enemy.debug.EnemyDebugController;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/** Aplica e limpa apenas instrumentação de debug, sem alterar regras de combate. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class EnemyDebugEvents {
    private EnemyDebugEvents() { }

    @SubscribeEvent
    public static void aoSairDoNivel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.Mob mob && mob instanceof HxHEnemy) {
            EnemyDebugController.clear(mob.getUUID(), mob);
        }
    }
}
