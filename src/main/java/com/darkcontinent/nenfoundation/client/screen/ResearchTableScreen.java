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
        // This screen deliberately follows the compact vanilla inventory
        // language. The field-guide styling belongs only to the bestiary.
        // One neutral vanilla container surface: no parchment, bands or
        // overlays continue into the player inventory area.
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 6, 0xFF404040);
        graphics.drawCenteredString(font, Component.translatable("bestiary.research.input"), imageWidth / 2, 62, 0xFF404040);
        graphics.drawString(font, Component.translatable("container.inventory"), 8, 72, 0xFF252A2B, false);
    }
}
