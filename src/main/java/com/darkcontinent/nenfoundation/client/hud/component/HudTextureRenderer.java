package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Aplica a moldura unificada que define a silhueta inteira da HUD. */
public final class HudTextureRenderer {
    private static final ResourceLocation MOLDURA = textura("frame.png");

    public void desenharMoldura(GuiGraphics graficos, NenHudLayout layout) {
        desenhar(graficos, MOLDURA, layout.moldura(), 512, 128);
    }

    private static void desenhar(GuiGraphics graficos, ResourceLocation textura,
            NenHudLayout.Retangulo area, int larguraDaTextura, int alturaDaTextura) {
        graficos.blit(textura, area.x(), area.y(), area.largura(), area.altura(),
                0.0F, 0.0F, larguraDaTextura, alturaDaTextura,
                larguraDaTextura, alturaDaTextura);
    }

    static ResourceLocation textura(String arquivo) {
        return NenFoundation.id("textures/gui/nen_hud/" + arquivo);
    }
}
