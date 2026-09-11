package com.darkcontinent.nenfoundation.client.screen;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import com.darkcontinent.nenfoundation.client.hud.AuraHudProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import java.util.Locale;

/** HUD minimo da Aura: desenha apenas quando o servidor ja enviou um delta. */
public final class OverlayDeAura {

    private static final int Y = 8;
    private static final int LARGURA = 160;
    private static final int ALTURA = 5;

    private final NenClientCache cache;

    public OverlayDeAura(NenClientCache cache) {
        this.cache = cache;
    }

    public void aoRenderizar(RenderGuiEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.player.isSpectator()) {
            return;
        }
        AuraHudProjection aura = AuraHudProjection.de(this.cache,
                evento.getPartialTick().getGameTimeDeltaPartialTick(false));
        GuiGraphics g = evento.getGuiGraphics();
        int largura = Math.min(LARGURA, g.guiWidth() - 16);
        int x = g.guiWidth() - largura - 8;
        if (!aura.disponivel()) {
            g.drawString(mc.font, Component.translatable("nenfoundation.hud.aguardando"),
                    x, Y, 0xFFAAAAAA, true);
            return;
        }
        g.drawString(mc.font, Component.translatable("nenfoundation.hud.aura"), x, Y, 0xFFE3EEE8, true);
        g.drawString(mc.font, String.format(Locale.ROOT, "%.1f / %.1f", aura.atual(), aura.maxima()),
                x, Y + 10, 0xFFE3EEE8, true);
        int barraY = Y + 21;
        g.fill(x, barraY, x + largura, barraY + ALTURA, 0xFF26352F);
        g.fill(x, barraY, x + Math.round(largura * aura.fracao()), barraY + ALTURA,
                aura.exausto() ? 0xFFB33A3A : 0xFF4CCB89);
        if (aura.exausto() || aura.maxima() == 0) {
            g.drawString(mc.font, Component.translatable(aura.exausto()
                    ? "nenfoundation.hud.aura_exausta" : "nenfoundation.hud.sem_reserva"),
                    x, barraY + 8, aura.exausto() ? 0xFFFF7777 : 0xFFAAAAAA, true);
        }
    }
}
