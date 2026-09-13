package com.darkcontinent.nenfoundation.worldtree;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private WorldTreeItems() {
    }
}
