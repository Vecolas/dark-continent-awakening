package com.darkcontinent.nenfoundation.client.screen;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import com.darkcontinent.nenfoundation.client.hud.AuraHudProjection;
import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import com.darkcontinent.nenfoundation.client.hud.NenHudVisibility;
import com.darkcontinent.nenfoundation.client.hud.component.AuraOutputBarRenderer;
import com.darkcontinent.nenfoundation.client.hud.component.AuraPoolBarRenderer;
import com.darkcontinent.nenfoundation.client.hud.component.HudTextureRenderer;
import com.darkcontinent.nenfoundation.client.hud.component.NenTypeBadgeRenderer;
import com.darkcontinent.nenfoundation.client.hud.component.PlayerHeadRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Orquestra a HUD; visibilidade e composicao moram aqui, desenho nos componentes. */
public final class OverlayDeAura {
    private final NenClientCache cache;
    private final AuraPoolBarRenderer aura = new AuraPoolBarRenderer();
    private final AuraOutputBarRenderer output = new AuraOutputBarRenderer();
    private final HudTextureRenderer texturas = new HudTextureRenderer();
    private final PlayerHeadRenderer retrato = new PlayerHeadRenderer();
    private final NenTypeBadgeRenderer badge = new NenTypeBadgeRenderer();

    public OverlayDeAura(NenClientCache cache) {
        this.cache = cache;
    }

    public void aoRenderizar(RenderGuiEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        if (!NenHudVisibility.deveRenderizar(mc.options.hideGui, mc.player != null,
                mc.player != null && mc.player.isSpectator())) {
            return;
        }
        AuraHudProjection aura = AuraHudProjection.de(this.cache,
                evento.getPartialTick().getGameTimeDeltaPartialTick(false));
        GuiGraphics g = evento.getGuiGraphics();
        NenHudLayout layout = NenHudLayout.para(g.guiWidth());
        if (!aura.disponivel()) {
            g.drawString(mc.font, Component.translatable("nenfoundation.hud.aguardando"),
                    NenHudLayout.MARGEM, NenHudLayout.MARGEM, 0xFFAAAAAA, true);
            return;
        }
        this.retrato.desenhar(g, layout.retrato(), mc.player);
        this.aura.desenharPreenchimento(g, layout.barraDeAura(), aura);
        this.output.desenharPreenchimento(g, layout.barraDeOutput(), aura);
        this.texturas.desenharMoldura(g, layout);
        this.aura.desenharTexto(g, layout.barraDeAura(), layout.valorDeAura(), aura);
        this.output.desenharTexto(g, layout.barraDeOutput(), layout.valorDeOutput(), aura);
        this.badge.desenhar(g, layout.badge());
        if (aura.exausto() || aura.maxima() == 0) {
            g.drawString(mc.font, Component.translatable(aura.exausto()
                    ? "nenfoundation.hud.aura_exausta" : "nenfoundation.hud.sem_reserva"),
                    layout.barraDeAura().x(), layout.barraDeAura().y() + 9,
                    aura.exausto() ? 0xFFFF7777 : 0xFFAAAAAA, true);
        }
    }
}
