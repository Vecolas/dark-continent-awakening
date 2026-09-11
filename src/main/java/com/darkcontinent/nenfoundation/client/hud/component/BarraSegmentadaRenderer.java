package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Desenha preenchimento continuo com divisorias puramente esteticas. */
final class BarraSegmentadaRenderer {
    private static final int SEGMENTOS = 10;

    private BarraSegmentadaRenderer() {
    }

    static void desenhar(GuiGraphics graficos, NenHudLayout.Retangulo area,
            float fracao, int fundo, ResourceLocation preenchimento) {
        int fimX = area.x() + area.largura();
        int fimY = area.y() + area.altura();
        graficos.fill(area.x(), area.y(), fimX, fimY, fundo);
        int larguraPreenchida = NenHudLayout.preenchimento(area.largura(), fracao);
        if (larguraPreenchida > 0) {
            int larguraFonte = Math.max(1, Math.round(256.0F * larguraPreenchida / area.largura()));
            graficos.blit(preenchimento, area.x(), area.y(), larguraPreenchida, area.altura(),
                    0.0F, 0.0F, larguraFonte, 16, 256, 16);
        }
        for (int segmento = 1; segmento < SEGMENTOS; segmento++) {
            int x = area.x() + Math.round(area.largura() * segmento / (float) SEGMENTOS);
            graficos.fill(x, area.y(), x + 1, fimY, 0x6631413D);
        }
    }
}
