package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/** Renderiza a cabeça diretamente da skin atual do jogador. */
public final class PlayerHeadRenderer {
    public void desenhar(GuiGraphics graficos, NenHudLayout.Retangulo area,
            AbstractClientPlayer jogador) {
        ResourceLocation skin = jogador.getSkin().texture();
        int tamanho = Math.min(area.largura(), area.altura()) - 4;
        int x = area.x() + (area.largura() - tamanho) / 2;
        int y = area.y() + (area.altura() - tamanho) / 2;
        // UVs oficiais da face e da camada de chapéu na textura 64x64.
        graficos.blit(skin, x, y, tamanho, tamanho, 8, 8, 8, 8, 64, 64);
        graficos.blit(skin, x, y, tamanho, tamanho, 40, 8, 8, 8, 64, 64);
    }
}
