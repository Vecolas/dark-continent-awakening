package com.darkcontinent.nenfoundation.client.worldtree;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Applies the vanilla biome foliage tint to the World Tree leaf family. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WorldTreeColorHandlers {
    private WorldTreeColorHandlers() {
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return FoliageColor.getDefaultColor();
            }
            return BiomeColors.getAverageFoliageColor(level, pos);
        }, WorldTreeBlocks.WORLD_TREE_LEAVES.get(),
                WorldTreeBlocks.WORLD_TREE_LEAVES_DENSE.get(),
                WorldTreeBlocks.WORLD_TREE_LEAVES_PALE.get());
    }
}
