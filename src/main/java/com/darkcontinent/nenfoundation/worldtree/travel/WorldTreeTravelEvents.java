package com.darkcontinent.nenfoundation.worldtree.travel;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Dispara a transicao somente no servidor, uma vez por tick do jogador. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class WorldTreeTravelEvents {
    private WorldTreeTravelEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WorldTreeTravelService.tryTravel(player);
        }
    }
}
