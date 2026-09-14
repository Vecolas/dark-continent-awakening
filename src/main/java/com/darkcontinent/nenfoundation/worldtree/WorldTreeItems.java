package com.darkcontinent.nenfoundation.worldtree;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** Recursos de expedição, separados dos blocos estruturais da árvore. */
public final class WorldTreeItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NenFoundation.MOD_ID);

    public static final DeferredItem<Item> WORLD_TREE_BARK_FRAGMENT = ITEMS.registerSimpleItem(
            "world_tree_bark_fragment");
    public static final DeferredItem<Item> WORLD_TREE_RESIN_CHUNK = ITEMS.registerSimpleItem(
            "world_tree_resin_chunk");
    public static final DeferredItem<Item> WORLD_TREE_FIBER = ITEMS.registerSimpleItem(
            "world_tree_fiber");
    public static final DeferredItem<Item> PALE_LEAF = ITEMS.registerSimpleItem("pale_leaf");
    public static final DeferredItem<Item> CANOPY_SAMPLE = ITEMS.registerSimpleItem("canopy_sample");

    // BlockItems keep the custom block ids while making the actual blocks available
    // in inventory, creative search and the creative middle-click pick block action.
    public static final DeferredItem<BlockItem> WORLD_TREE_BARK_BLOCK = registerBlockItem("world_tree_bark", WorldTreeBlocks.WORLD_TREE_BARK);
    public static final DeferredItem<BlockItem> WORLD_TREE_BARK_DARK_BLOCK = registerBlockItem("world_tree_bark_dark", WorldTreeBlocks.WORLD_TREE_BARK_DARK);
    public static final DeferredItem<BlockItem> WORLD_TREE_BARK_MOSSY_BLOCK = registerBlockItem("world_tree_bark_mossy", WorldTreeBlocks.WORLD_TREE_BARK_MOSSY);
    public static final DeferredItem<BlockItem> WORLD_TREE_BARK_SCARRED_BLOCK = registerBlockItem("world_tree_bark_scarred", WorldTreeBlocks.WORLD_TREE_BARK_SCARRED);
    public static final DeferredItem<BlockItem> WORLD_TREE_SAPWOOD_BLOCK = registerBlockItem("world_tree_sapwood", WorldTreeBlocks.WORLD_TREE_SAPWOOD);
    public static final DeferredItem<BlockItem> WORLD_TREE_HEARTWOOD_BLOCK = registerBlockItem("world_tree_heartwood", WorldTreeBlocks.WORLD_TREE_HEARTWOOD);
    public static final DeferredItem<BlockItem> WORLD_TREE_CORE_BLOCK = registerBlockItem("world_tree_core", WorldTreeBlocks.WORLD_TREE_CORE);
    public static final DeferredItem<BlockItem> WORLD_TREE_ROOT_BLOCK = registerBlockItem("world_tree_root", WorldTreeBlocks.WORLD_TREE_ROOT);
    public static final DeferredItem<BlockItem> WORLD_TREE_ROOT_MOSSY_BLOCK = registerBlockItem("world_tree_root_mossy", WorldTreeBlocks.WORLD_TREE_ROOT_MOSSY);
    public static final DeferredItem<BlockItem> WORLD_TREE_LEAVES_BLOCK = registerBlockItem("world_tree_leaves", WorldTreeBlocks.WORLD_TREE_LEAVES);
    public static final DeferredItem<BlockItem> WORLD_TREE_LEAVES_DENSE_BLOCK = registerBlockItem("world_tree_leaves_dense", WorldTreeBlocks.WORLD_TREE_LEAVES_DENSE);
    public static final DeferredItem<BlockItem> WORLD_TREE_LEAVES_PALE_BLOCK = registerBlockItem("world_tree_leaves_pale", WorldTreeBlocks.WORLD_TREE_LEAVES_PALE);
    public static final DeferredItem<BlockItem> WORLD_TREE_VINE_BLOCK = registerBlockItem("world_tree_vine", WorldTreeBlocks.WORLD_TREE_VINE);
    public static final DeferredItem<BlockItem> WORLD_TREE_THICK_VINE_BLOCK = registerBlockItem("world_tree_thick_vine", WorldTreeBlocks.WORLD_TREE_THICK_VINE);
    public static final DeferredItem<BlockItem> WORLD_TREE_MOSS_BLOCK = registerBlockItem("world_tree_moss", WorldTreeBlocks.WORLD_TREE_MOSS);
    public static final DeferredItem<BlockItem> WORLD_TREE_MOSS_CARPET_BLOCK = registerBlockItem("world_tree_moss_carpet", WorldTreeBlocks.WORLD_TREE_MOSS_CARPET);
    public static final DeferredItem<BlockItem> WORLD_TREE_RESIN_BLOCK = registerBlockItem("world_tree_resin", WorldTreeBlocks.WORLD_TREE_RESIN);
    public static final DeferredItem<BlockItem> WORLD_TREE_RESIN_VEIN_BLOCK = registerBlockItem("world_tree_resin_vein", WorldTreeBlocks.WORLD_TREE_RESIN_VEIN);
    public static final DeferredItem<BlockItem> WORLD_TREE_DEADWOOD_BLOCK = registerBlockItem("world_tree_deadwood", WorldTreeBlocks.WORLD_TREE_DEADWOOD);
    public static final DeferredItem<BlockItem> HUNTER_CLIMBING_ANCHOR_BLOCK = registerBlockItem("hunter_climbing_anchor", WorldTreeBlocks.HUNTER_CLIMBING_ANCHOR);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private static DeferredItem<BlockItem> registerBlockItem(String id, DeferredBlock<? extends Block> block) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private WorldTreeItems() {
    }
}
