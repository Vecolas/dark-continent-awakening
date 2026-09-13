package com.darkcontinent.nenfoundation.worldtree.ecology;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/** Filtra spawns de fauna da World Tree no ponto final de autoridade do spawn. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class WorldTreeSpawnController {
    private WorldTreeSpawnController() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity().getType() != EnemyEntityTypes.SPIDER_EAGLE.get()) {
            return;
        }
        ServerLevelAccessor level = event.getLevel();
        if (!WorldTreeEcologyService.speciesAllowed(level.getLevel().dimension(),
                (int) Math.floor(event.getY()), "spider_eagle")) {
            event.setSpawnCancelled(true);
        }
    }
}
