package com.darkcontinent.nenfoundation.client.bestiary;

import com.darkcontinent.nenfoundation.registry.NenItems;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Ponte client-only: o item comum não alcança classes de tela no servidor. */
public final class BestiaryClientEvents {
    private BestiaryClientEvents() { }

    public static void abrirAoUsar(PlayerInteractEvent.RightClickItem evento) {
        if (evento.getItemStack().is(NenItems.HUNTER_BESTIARY.get())
                && Minecraft.getInstance().screen == null) {
            Minecraft.getInstance().setScreen(new HunterBestiaryScreen());
            evento.setCanceled(true);
        }
    }
}
