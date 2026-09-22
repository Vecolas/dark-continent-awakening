package com.darkcontinent.nenfoundation.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.item.HunterBestiaryItem;
import com.darkcontinent.nenfoundation.item.FieldNoteItem;
import net.minecraft.world.item.Item;
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
    public static final DeferredHolder<Item, Item> GREAT_STAMP_FIELD_NOTE = note("great_stamp_field_note", "great_stamp");
    public static final DeferredHolder<Item, Item> FROG_FIELD_NOTE = note("frog_in_waiting_field_note", "frog_in_waiting");
    public static final DeferredHolder<Item, Item> KIRIKO_FIELD_NOTE = note("kiriko_field_note", "kiriko");
    public static final DeferredHolder<Item, Item> APE_FIELD_NOTE = note("man_faced_ape_field_note", "man_faced_ape");
    public static final DeferredHolder<Item, Item> SWAMP_FIELD_NOTE = note("master_of_the_swamp_field_note", "master_of_the_swamp");
    public static final DeferredHolder<Item, Item> SPIDER_EAGLE_FIELD_NOTE = note("spider_eagle_field_note", "spider_eagle");

    /**
     * UM item para os vinte e tres cards, e a identidade mora no dado.
     *
     * <p>Um registro por criatura daria vinte e tres entradas aqui, e cada card
     * novo exigiria tocar neste arquivo -- que e hostil a merge. Com um item so,
     * acrescentar um card e acrescentar um dado.</p>
     */
    public static final DeferredHolder<Item, Item> GREED_ISLAND_CARD = ITEMS.register(
            "greed_island_card",
            () -> new com.darkcontinent.nenfoundation.item.GreedIslandCardItem(new Item.Properties()));

    /**
     * O anel que abre Greed Island.
     *
     * <p>{@code stacksTo(1)} porque ele e uma CHAVE, e nao um consumivel: uma
     * pilha de sessenta e quatro aneis nao significa nada, e permiti-la faria o
     * item parecer material de receita.</p>
     */
    public static final DeferredHolder<Item, Item> GREED_ISLAND_RING = ITEMS.register(
            "greed_island_ring",
            () -> new com.darkcontinent.nenfoundation.item.GreedIslandRingItem(
                    new Item.Properties().stacksTo(1)));

    private static DeferredHolder<Item, Item> note(String itemId, String entryId) {
        return ITEMS.register(itemId, () -> new FieldNoteItem(new Item.Properties(),
                NenFoundation.id(entryId), 3));
    }

    private NenItems() { }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    /** Coloca uma cópia no inventário criativo, sem criar uma aba nova para um único item. */
    public static void adicionarAoCriativo(BuildCreativeModeTabContentsEvent evento) {
        if (evento.getTabKey() == net.minecraft.world.item.CreativeModeTabs.INGREDIENTS) {
            evento.accept(HUNTER_BESTIARY.get());
            evento.accept(FOXBEAR_FIELD_NOTE.get());
            evento.accept(GREAT_STAMP_FIELD_NOTE.get());
            evento.accept(FROG_FIELD_NOTE.get());
            evento.accept(KIRIKO_FIELD_NOTE.get());
            evento.accept(APE_FIELD_NOTE.get());
            evento.accept(SWAMP_FIELD_NOTE.get());
            evento.accept(SPIDER_EAGLE_FIELD_NOTE.get());
            evento.accept(GREED_ISLAND_CARD.get());
            evento.accept(GREED_ISLAND_RING.get());
        }
        if (evento.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            evento.accept(NenBlocks.RESEARCH_TABLE_ITEM.get());
        }
    }

}
