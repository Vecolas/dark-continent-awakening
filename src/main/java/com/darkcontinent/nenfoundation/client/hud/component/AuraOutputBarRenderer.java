package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.AuraHudProjection;
import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Apresenta o Output confirmado pelo servidor, sem prever a proxima entrada. */
public final class AuraOutputBarRenderer {
    private static final int COR_TEXTO = 0xFFFFC35A;
    private static final int COR_FUNDO = 0xFF3A3025;
    private static final ResourceLocation TEXTURA_OUTPUT =
            HudTextureRenderer.textura("aura_output_fill.png");

    public void desenhar(GuiGraphics graficos, NenHudLayout.Retangulo area,
            AuraHudProjection aura) {
        Minecraft mc = Minecraft.getInstance();
        graficos.drawString(mc.font, Component.translatable("nenfoundation.hud.output"),
                area.x(), area.y() - 11, COR_TEXTO, true);
        String valor = Math.round(aura.outputPercent() * 100.0F) + "%";
        graficos.drawString(mc.font, valor, area.x() + area.largura() - mc.font.width(valor),
                area.y() - 11, COR_TEXTO, true);
        BarraSegmentadaRenderer.desenhar(graficos, area, aura.outputVisual(),
                COR_FUNDO, TEXTURA_OUTPUT);
    }
}
