package com.darkcontinent.nenfoundation.worldtree;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Exposes the real World Tree blocks in the vanilla building-blocks tab. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class WorldTreeCreativeContents {
    private WorldTreeCreativeContents() {
    }

    @SubscribeEvent
    public static void addWorldTreeBlocks(BuildCreativeModeTabContentsEvent event) {
        if (!CreativeModeTabs.BUILDING_BLOCKS.equals(event.getTabKey())) {
            return;
        }
        event.accept(WorldTreeItems.WORLD_TREE_BARK_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_BARK_DARK_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_BARK_MOSSY_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_BARK_SCARRED_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_SAPWOOD_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_HEARTWOOD_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_CORE_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_ROOT_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_ROOT_MOSSY_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_LEAVES_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_LEAVES_DENSE_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_LEAVES_PALE_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_LEAVES_LUMINOUS_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_VINE_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_THICK_VINE_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_MOSS_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_MOSS_CARPET_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_RESIN_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_RESIN_VEIN_BLOCK);
        event.accept(WorldTreeItems.WORLD_TREE_DEADWOOD_BLOCK);
        event.accept(WorldTreeItems.HUNTER_CLIMBING_ANCHOR_BLOCK);
    }
}
