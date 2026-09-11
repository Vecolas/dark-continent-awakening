package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Badge provisório; a categoria visível do M3 substituirá apenas o conteúdo. */
public final class NenTypeBadgeRenderer {
    public void desenhar(GuiGraphics graficos, NenHudLayout.Retangulo area) {
        var fonte = Minecraft.getInstance().font;
        Component simbolo = Component.translatable("nenfoundation.hud.badge_provisorio");
        graficos.drawString(fonte, simbolo,
                area.x() + (area.largura() - fonte.width(simbolo)) / 2,
                area.y() + (area.altura() - fonte.lineHeight) / 2,
                0xFFFFD98A, false);
    }
}
