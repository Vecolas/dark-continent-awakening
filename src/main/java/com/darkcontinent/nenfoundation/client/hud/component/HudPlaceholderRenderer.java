package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Reserva retrato e badge sem antecipar os assets finais da issue #74. */
public final class HudPlaceholderRenderer {
    public void desenhar(GuiGraphics graficos, NenHudLayout layout) {
        desenharCaixa(graficos, layout.retrato(), "?");
        desenharCaixa(graficos, layout.badge(), "N");
    }

    private static void desenharCaixa(GuiGraphics graficos,
            NenHudLayout.Retangulo area, String simbolo) {
        graficos.fill(area.x(), area.y(), area.x() + area.largura(),
                area.y() + area.altura(), 0xCC172126);
        graficos.renderOutline(area.x(), area.y(), area.largura(), area.altura(), 0xFF8FA6AD);
        var fonte = Minecraft.getInstance().font;
        graficos.drawString(fonte, simbolo,
                area.x() + (area.largura() - fonte.width(simbolo)) / 2,
                area.y() + (area.altura() - fonte.lineHeight) / 2,
                0xFFEAF8FF, false);
    }
}
