package com.darkcontinent.nenfoundation.client.screen;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import com.darkcontinent.nenfoundation.client.hud.AuraHudProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** HUD minimo da Aura: desenha apenas quando o servidor ja enviou um delta. */
public final class OverlayDeAura {

    private static final int X = 8;
    private static final int Y = 8;
    private static final int LARGURA = 120;
    private static final int ALTURA = 5;

    private final NenClientCache cache;

    public OverlayDeAura(NenClientCache cache) {
        this.cache = cache;
    }

    public void aoRenderizar(RenderGuiEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }
        AuraHudProjection aura = AuraHudProjection.de(this.cache);
        if (!aura.disponivel()) {
            return;
        }
        GuiGraphics g = evento.getGuiGraphics();
        g.drawString(mc.font, Component.translatable("nenfoundation.hud.aura"), X, Y, 0xFFE3EEE8, true);
        int barraY = Y + 10;
        g.fill(X, barraY, X + LARGURA, barraY + ALTURA, 0xFF26352F);
        g.fill(X, barraY, X + Math.round(LARGURA * aura.fracao()), barraY + ALTURA,
                aura.exausto() ? 0xFFB33A3A : 0xFF4CCB89);
        if (aura.exausto()) {
            g.drawString(mc.font, Component.translatable("nenfoundation.hud.aura_exausta"),
                    X, barraY + 8, 0xFFFF7777, true);
        }
    }
}
