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
        // Keep the vanilla container language without copying a complete
        // crafting/grindstone layout. The lower strip is the exact vanilla
        // player-inventory area; the table owns only the upper operation area.
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 70, 0xFFC6C6C6);
        graphics.blit(INVENTORY_LOCATION, leftPos, topPos + 70, 0, 70, imageWidth, imageHeight - 70);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        graphics.drawCenteredString(font, Component.translatable("bestiary.research.input"), 58, 61, 0xFF404040);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
    }
}
