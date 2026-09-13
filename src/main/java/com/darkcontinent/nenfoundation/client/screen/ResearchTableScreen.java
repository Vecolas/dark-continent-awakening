package com.darkcontinent.nenfoundation.client.screen;

import com.darkcontinent.nenfoundation.structure.ResearchTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Tela própria da estação; a operação é executada pelo menu server-side. */
public final class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFF1E8D2);
        graphics.fill(leftPos + 7, topPos + 7, leftPos + 169, topPos + 28, 0xFF58664B);
        graphics.fill(leftPos + 70, topPos + 25, leftPos + 98, topPos + 55, 0x224B5A57);
        graphics.fill(leftPos + 7, topPos + 75, leftPos + 169, topPos + 77, 0xFFB5A889);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 12, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.translatable("bestiary.research.input"), 59, 62, 0xFF31545A, false);
        graphics.drawString(font, Component.translatable("container.inventory"), 8, 72, 0xFF252A2B, false);
    }
}
