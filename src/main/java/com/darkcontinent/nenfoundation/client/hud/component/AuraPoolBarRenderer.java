package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.AuraHudProjection;
import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Apresenta a reserva atual/maxima sem conhecer consumo ou regeneracao. */
public final class AuraPoolBarRenderer {
    private static final int COR_TEXTO = 0xFFEAF8FF;
    private static final int COR_FUNDO = 0xFF26343B;
    private static final int COR_AURA = 0xFFAEDFFF;
    private static final int COR_EXAUSTA = 0xFFB84B4B;

    public void desenhar(GuiGraphics graficos, NenHudLayout.Retangulo area,
            AuraHudProjection aura) {
        Minecraft mc = Minecraft.getInstance();
        graficos.drawString(mc.font, Component.translatable("nenfoundation.hud.aura"),
                area.x(), area.y() - 11, COR_TEXTO, true);
        String valor = String.format(Locale.ROOT, "%.0f / %.0f", aura.atual(), aura.maxima());
        graficos.drawString(mc.font, valor, area.x() + area.largura() - mc.font.width(valor),
                area.y() - 11, COR_TEXTO, true);
        BarraSegmentadaRenderer.desenhar(graficos, area, aura.fracao(), COR_FUNDO,
                aura.exausto() ? COR_EXAUSTA : COR_AURA);
    }
}
