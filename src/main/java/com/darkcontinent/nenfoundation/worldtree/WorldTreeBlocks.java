package com.darkcontinent.nenfoundation.worldtree;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import com.darkcontinent.nenfoundation.worldtree.checkpoint.HunterClimbingAnchorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Registro e contrato material dos blocos estruturais da World Tree. */
public final class WorldTreeBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(NenFoundation.MOD_ID);

    /** Obsidian-equivalent mining/blast profile for every harvestable tree block. */
    private static BlockBehaviour.Properties structuralProperties() {
        return BlockBehaviour.Properties.of().strength(50.0F, 1200.0F)
                .requiresCorrectToolForDrops();
    }

    private static final BlockBehaviour.Properties BARK = structuralProperties()
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD);
    private static final BlockBehaviour.Properties WOOD = structuralProperties()
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD);
    private static final BlockBehaviour.Properties LEAVES = structuralProperties()
            .mapColor(MapColor.COLOR_GREEN)
            .sound(SoundType.GRASS)
            .noOcclusion();
    private static final BlockBehaviour.Properties MOSS = structuralProperties()
            .mapColor(MapColor.COLOR_GREEN)
            .sound(SoundType.MOSS);

    public static final DeferredBlock<Block> WORLD_TREE_BARK = BLOCKS.registerSimpleBlock("world_tree_bark", BARK);
    public static final DeferredBlock<Block> WORLD_TREE_BARK_DARK = BLOCKS.registerSimpleBlock("world_tree_bark_dark", BARK);
    public static final DeferredBlock<Block> WORLD_TREE_BARK_MOSSY = BLOCKS.registerSimpleBlock("world_tree_bark_mossy", BARK);
    public static final DeferredBlock<Block> WORLD_TREE_BARK_SCARRED = BLOCKS.registerSimpleBlock("world_tree_bark_scarred", BARK);

    public static final DeferredBlock<Block> WORLD_TREE_SAPWOOD = BLOCKS.registerSimpleBlock("world_tree_sapwood", WOOD);
    public static final DeferredBlock<Block> WORLD_TREE_HEARTWOOD = BLOCKS.registerSimpleBlock("world_tree_heartwood", WOOD);
    public static final DeferredBlock<Block> WORLD_TREE_CORE = BLOCKS.registerSimpleBlock("world_tree_core",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3_600_000.0F).sound(SoundType.STONE)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));

    public static final DeferredBlock<Block> WORLD_TREE_ROOT = BLOCKS.registerSimpleBlock("world_tree_root", WOOD);
    public static final DeferredBlock<Block> WORLD_TREE_ROOT_MOSSY = BLOCKS.registerSimpleBlock("world_tree_root_mossy", BARK);

    public static final DeferredBlock<Block> WORLD_TREE_LEAVES = BLOCKS.registerSimpleBlock("world_tree_leaves", LEAVES);
    public static final DeferredBlock<Block> WORLD_TREE_LEAVES_DENSE = BLOCKS.registerSimpleBlock("world_tree_leaves_dense", LEAVES);
    public static final DeferredBlock<Block> WORLD_TREE_LEAVES_PALE = BLOCKS.registerSimpleBlock("world_tree_leaves_pale", LEAVES);

    public static final DeferredBlock<Block> WORLD_TREE_VINE = BLOCKS.registerSimpleBlock("world_tree_vine",
            BlockBehaviour.Properties.ofFullCopy(Blocks.VINE).strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredBlock<Block> WORLD_TREE_THICK_VINE = BLOCKS.registerSimpleBlock("world_tree_thick_vine",
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GRASS).noOcclusion());

    public static final DeferredBlock<Block> WORLD_TREE_MOSS = BLOCKS.registerSimpleBlock("world_tree_moss", MOSS);
    public static final DeferredBlock<Block> WORLD_TREE_MOSS_CARPET = BLOCKS.register("world_tree_moss_carpet",
            () -> new CarpetBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_CARPET)
                    .strength(50.0F, 1200.0F).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> WORLD_TREE_RESIN = BLOCKS.registerSimpleBlock("world_tree_resin",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS).noOcclusion());
    public static final DeferredBlock<Block> WORLD_TREE_RESIN_VEIN = BLOCKS.registerSimpleBlock("world_tree_resin_vein",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS).noOcclusion());
    public static final DeferredBlock<Block> WORLD_TREE_DEADWOOD = BLOCKS.registerSimpleBlock("world_tree_deadwood", WOOD);
    public static final DeferredBlock<Block> HUNTER_CLIMBING_ANCHOR = BLOCKS.register(
            "hunter_climbing_anchor", () -> new HunterClimbingAnchorBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                            .strength(50.0F, 1200.0F).requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private WorldTreeBlocks() {
    }
}
