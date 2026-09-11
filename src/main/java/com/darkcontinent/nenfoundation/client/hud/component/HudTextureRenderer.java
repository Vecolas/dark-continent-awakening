package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Aplica as molduras autorais sem fundir os assets numa textura gigante. */
public final class HudTextureRenderer {
    private static final ResourceLocation MOLDURA = textura("frame.png");
    private static final ResourceLocation MOLDURA_RETRATO = textura("portrait_frame.png");
    private static final ResourceLocation MOLDURA_BADGE = textura("badge_frame.png");

    public void desenharMolduraDasBarras(GuiGraphics graficos, NenHudLayout layout) {
        desenhar(graficos, MOLDURA, layout.molduraDasBarras(), 256, 80);
    }

    public void desenharMolduraDoRetrato(GuiGraphics graficos, NenHudLayout layout) {
        desenhar(graficos, MOLDURA_RETRATO, layout.retrato(), 64, 64);
    }

    public void desenharMolduraDoBadge(GuiGraphics graficos, NenHudLayout layout) {
        desenhar(graficos, MOLDURA_BADGE, layout.badge(), 32, 32);
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
