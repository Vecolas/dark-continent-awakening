package com.darkcontinent.nenfoundation.client.screen;

import com.darkcontinent.nenfoundation.structure.ResearchTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Tela própria da estação; a operação é executada pelo menu server-side. */
public final class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");

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
        // Use the vanilla container surface and slot sprite directly. This
        // keeps one continuous panel without importing an unrelated crafting
        // or grindstone layout.
        int right = leftPos + imageWidth;
        int bottom = topPos + imageHeight;
        graphics.fill(leftPos, topPos, right, bottom, 0xFFC6C6C6);
        graphics.fill(leftPos, topPos, right, topPos + 1, 0xFFFFFFFF);
        graphics.fill(leftPos, topPos, leftPos + 1, bottom, 0xFFFFFFFF);
        graphics.fill(leftPos, bottom - 1, right, bottom, 0xFF555555);
        graphics.fill(right - 1, topPos, right, bottom, 0xFF555555);

        for (Slot slot : menu.slots) {
            graphics.blitSprite(SLOT_SPRITE, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        graphics.drawCenteredString(font, Component.translatable("bestiary.research.input"), 58, 61, 0xFF404040);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
    }
}
