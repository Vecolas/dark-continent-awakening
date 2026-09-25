package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import com.darkcontinent.nenfoundation.client.hud.PaletaDaHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * O retrato: a cabeca da skin atual dentro de um aro tecnico.
 *
 * <p>O ARO E O UNICO ENFEITE DA HUD, e ele existe por leitura e nao por gosto:
 * a face da skin e um quadrado de pixels arbitrarios, e sem moldura ela se
 * funde com o mundo atras. O aro tambem e o que dava "cara de dispositivo" na
 * referencia do plano.
 *
 * <p>NAO E CIRCULAR. Um circulo exigiria mascara -- textura ou stencil -- para
 * recortar a face quadrada, e o resultado em 26 pixels seria uma borda
 * serrilhada que parece defeito. Os quatro CANTOS marcados dao a mesma
 * sensacao de instrumento e sobrevivem a qualquer GUI Scale.
 */
public final class PlayerHeadRenderer {

    /** O comprimento de cada marca de canto. */
    private static final int CANTO = 5;

    public void desenhar(GuiGraphics graficos, NenHudLayout.Retangulo area,
            AbstractClientPlayer jogador) {
        ResourceLocation skin = jogador.getSkin().texture();
        int tamanho = Math.min(area.largura(), area.altura()) - 6;
        int x = area.x() + (area.largura() - tamanho) / 2;
        int y = area.y() + (area.altura() - tamanho) / 2;

        // O fundo entra ANTES da face: uma skin com face transparente existe, e
        // sem ele o mundo apareceria dentro do retrato.
        graficos.fill(x, y, x + tamanho, y + tamanho, PaletaDaHud.TRILHO);
        // UVs oficiais da face e da camada de chapéu na textura 64x64.
        graficos.blit(skin, x, y, tamanho, tamanho, 8, 8, 8, 8, 64, 64);
        graficos.blit(skin, x, y, tamanho, tamanho, 40, 8, 8, 8, 64, 64);

        aro(graficos, area);
    }

    /**
     * Quatro cantos, e nao um retangulo fechado.
     *
     * <p>O retangulo fechado somaria uma segunda moldura dentro da moldura do
     * painel, e o diagnostico desta HUD foi peso visual concentrado justamente
     * no contorno.
     */
    private static void aro(GuiGraphics g, NenHudLayout.Retangulo area) {
        int x0 = area.x();
        int y0 = area.y();
        int x1 = area.fimX();
        int y1 = area.fimY();
        int cor = PaletaDaHud.ACENTO;

        g.fill(x0, y0, x0 + CANTO, y0 + 1, cor);
        g.fill(x0, y0, x0 + 1, y0 + CANTO, cor);

        g.fill(x1 - CANTO, y0, x1, y0 + 1, cor);
        g.fill(x1 - 1, y0, x1, y0 + CANTO, cor);

        g.fill(x0, y1 - 1, x0 + CANTO, y1, cor);
        g.fill(x0, y1 - CANTO, x0 + 1, y1, cor);

        g.fill(x1 - CANTO, y1 - 1, x1, y1, cor);
        g.fill(x1 - 1, y1 - CANTO, x1, y1, cor);
    }
}
