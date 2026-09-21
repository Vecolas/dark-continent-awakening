package com.darkcontinent.nenfoundation.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraNestBlock;
import com.darkcontinent.nenfoundation.structure.ResearchTableBlock;
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

    public static final DeferredHolder<Block, Block> CHIMERA_NEST = BLOCKS.register(
            "chimera_nest", () -> new ChimeraNestBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Item, Item> CHIMERA_NEST_ITEM = ITEMS.register(
            "chimera_nest", () -> new BlockItem(CHIMERA_NEST.get(), new Item.Properties()));

    private NenBlocks() { }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
