package com.darkcontinent.nenfoundation.client.screen;

import com.darkcontinent.nenfoundation.structure.ResearchTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Tela própria da estação; a operação é executada pelo menu server-side. */
public final class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("bestiary.research.button"),
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .bounds(leftPos + 104, topPos + 34, 64, 20).build());
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Vanilla container language without copying another screen's
        // texture: neutral container surface, vanilla slots and vanilla
        // Button. Only the research operation is specific to this screen.
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.fill(leftPos + 7, topPos + 7, leftPos + 169, topPos + 68, 0xFFBDBDBD);
        graphics.fill(leftPos + 7, topPos + 68, leftPos + 169, topPos + 69, 0xFF8B8B8B);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFF404040, false);
        graphics.drawString(font, Component.translatable("bestiary.research.input"), 42, 62, 0xFF404040, false);
        graphics.drawString(font, Component.translatable("container.inventory"), 8, 72, 0xFF404040, false);
    }
}
