package com.darkcontinent.nenfoundation.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;
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
            "hunter_bestiary", () -> new WrittenBookItem(new Item.Properties()
                    .stacksTo(1)
                    .component(DataComponents.WRITTEN_BOOK_CONTENT, conteudoDoBestiario())));

    private NenItems() { }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    /** Coloca uma cópia no inventário criativo, sem criar uma aba nova para um único item. */
    public static void adicionarAoCriativo(BuildCreativeModeTabContentsEvent evento) {
        if (evento.getTabKey() == net.minecraft.world.item.CreativeModeTabs.INGREDIENTS) {
            evento.accept(HUNTER_BESTIARY.get());
        }
    }

    static WrittenBookContent conteudoDoBestiario() {
        return BestiarioContent.conteudo();
    }
}
