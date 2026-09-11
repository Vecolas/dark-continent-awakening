package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import net.minecraft.client.gui.GuiGraphics;

/** Desenha preenchimento continuo com divisorias puramente esteticas. */
final class BarraSegmentadaRenderer {
    private static final int SEGMENTOS = 10;

    private BarraSegmentadaRenderer() {
    }

    static void desenhar(GuiGraphics graficos, NenHudLayout.Retangulo area,
            float fracao, int fundo, int preenchimento) {
        int fimX = area.x() + area.largura();
        int fimY = area.y() + area.altura();
        graficos.fill(area.x(), area.y(), fimX, fimY, fundo);
        graficos.fill(area.x(), area.y(),
                area.x() + NenHudLayout.preenchimento(area.largura(), fracao),
                fimY, preenchimento);
        for (int segmento = 1; segmento < SEGMENTOS; segmento++) {
            int x = area.x() + Math.round(area.largura() * segmento / (float) SEGMENTOS);
            graficos.fill(x, area.y(), x + 1, fimY, 0x6631413D);
        }
    }
}
