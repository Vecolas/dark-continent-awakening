package com.darkcontinent.nenfoundation.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.item.HunterBestiaryItem;
import com.darkcontinent.nenfoundation.item.FieldNoteItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;

/** Itens de leitura e apoio do mod. Nenhum item deste registro decide regras de Nen. */
public final class NenItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, NenFoundation.MOD_ID);

    public static final DeferredHolder<Item, Item> HUNTER_BESTIARY = ITEMS.register(
            "hunter_bestiary", () -> new HunterBestiaryItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FOXBEAR_FIELD_NOTE = ITEMS.register(
            "foxbear_field_note", () -> new FieldNoteItem(new Item.Properties(),
                    com.darkcontinent.nenfoundation.bestiary.BestiaryRegistry.FOXBEAR_ID, 3));

    private NenItems() { }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    /** Coloca uma cópia no inventário criativo, sem criar uma aba nova para um único item. */
    public static void adicionarAoCriativo(BuildCreativeModeTabContentsEvent evento) {
        if (evento.getTabKey() == net.minecraft.world.item.CreativeModeTabs.INGREDIENTS) {
            evento.accept(HUNTER_BESTIARY.get());
            evento.accept(FOXBEAR_FIELD_NOTE.get());
        }
    }

}
