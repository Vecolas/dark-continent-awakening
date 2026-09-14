package com.darkcontinent.nenfoundation.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.structure.ResearchTableBlock;
import com.darkcontinent.nenfoundation.enemy.entity.SpiderEagleNestBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registro das estações físicas que conectam estruturas ao gameplay. */
public final class NenBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, NenFoundation.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, NenFoundation.MOD_ID);

    public static final DeferredHolder<Block, Block> RESEARCH_TABLE = BLOCKS.register(
            "research_table", () -> new ResearchTableBlock(BlockBehaviour.Properties.of()
                    .strength(2.5F).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Item, Item> RESEARCH_TABLE_ITEM = ITEMS.register(
            "research_table", () -> new BlockItem(RESEARCH_TABLE.get(), new Item.Properties()));
    public static final DeferredHolder<Block, Block> SPIDER_EAGLE_NEST = BLOCKS.register(
            "spider_eagle_nest", () -> new SpiderEagleNestBlock(BlockBehaviour.Properties.of()
                    .strength(0.8F).noOcclusion().noCollission()));
    public static final DeferredHolder<Item, Item> SPIDER_EAGLE_NEST_ITEM = ITEMS.register(
            "spider_eagle_nest", () -> new BlockItem(SPIDER_EAGLE_NEST.get(), new Item.Properties()));

    private NenBlocks() { }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
