package com.darkcontinent.nenfoundation.enemy.chimera;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.registry.NenBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Disponibiliza o bloco de ninho para mapas e testes de conteúdo. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ChimeraNestCreativeContents {
    private ChimeraNestCreativeContents() { }

    @SubscribeEvent
    public static void adicionar(BuildCreativeModeTabContentsEvent evento) {
        if (evento.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            evento.accept(NenBlocks.CHIMERA_NEST_ITEM.get());
        }
    }
}
