package com.darkcontinent.nenfoundation.client.bestiary;

import com.darkcontinent.nenfoundation.bestiary.BestiaryEntryDefinition;
import com.darkcontinent.nenfoundation.bestiary.BestiaryKnowledgeLevel;
import com.darkcontinent.nenfoundation.bestiary.BestiaryRegistry;
import com.darkcontinent.nenfoundation.bestiary.BestiaryProgress;
import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;

/** Caderno de campo próprio: duas páginas, papel e marcadores, sem BookViewScreen. */
public final class HunterBestiaryScreen extends Screen {
    private static final int PAPER = 0xFFF1E8D2;
    private static final int PAPER_SHADOW = 0xFFB5A889;
    private static final int INK = 0xFF252A2B;
    private static final int OLIVE = 0xFF58664B;
    private static final int PETROLEO = 0xFF31545A;
    private static final int ALERTA = 0xFF863C35;
    private int left;
    private int top;
    private int bookWidth;
    private int bookHeight;
    private boolean index = true;
    private net.minecraft.resources.ResourceLocation selectedId = BestiaryRegistry.FOXBEAR_ID;

    public HunterBestiaryScreen() {
        super(Component.translatable("item.nenfoundation.hunter_bestiary"));
    }

    @Override
    protected void init() {
        bookWidth = Math.min(760, width - 20);
        bookHeight = Math.min(460, height - 20);
        left = (width - bookWidth) / 2;
        top = (height - bookHeight) / 2;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA172124);
        graphics.fill(left - 7, top - 5, left + bookWidth + 7, top + bookHeight + 5, PAPER_SHADOW);
        graphics.fill(left, top, left + bookWidth, top + bookHeight, PAPER);
        graphics.fill(left + bookWidth / 2 - 2, top + 8, left + bookWidth / 2 + 2,
                top + bookHeight - 8, 0xFFB7A984);
        graphics.fill(left + 12, top + 13, left + bookWidth / 2 - 13, top + 16, 0x33766E54);
        graphics.fill(left + bookWidth / 2 + 13, top + 13, left + bookWidth - 12, top + 16, 0x33766E54);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawMarker(graphics, left - 11, top + 47, "ALL", true);
        drawMarker(graphics, left - 11, top + 82, "WILD", false);
        drawMarker(graphics, left + bookWidth - 4, top + 47, "MAGIC", false);
        drawMarker(graphics, left + bookWidth - 4, top + 82, "SPECIAL", false);
        if (index) drawIndex(graphics);
        else drawEntry(graphics);
    }

    private void drawIndex(GuiGraphics graphics) {
        drawTitle(graphics, "FIELD INDEX", left + 30, top + 30);
        graphics.drawString(font, "ASSOCIATION HUNTER / FIELD RESEARCH", left + 30, top + 49, OLIVE, false);
        int y = top + 82;
        int number = 1;
        for (BestiaryEntryDefinition entry : BestiaryRegistry.entries()) {
            BestiaryProgress progress = BestiaryClientState.progress(entry.id());
            graphics.fill(left + 25, y - 4, left + bookWidth - 25, y + 29, 0x22606A5E);
            graphics.drawString(font, String.format("%02d", number++), left + 35, y + 5, OLIVE, false);
            graphics.drawString(font, nome(entry, progress), left + 72, y + 5, INK, false);
            graphics.drawString(font, status(progress), left + bookWidth - 135, y + 5, progress.knowledgeLevel() == BestiaryKnowledgeLevel.UNKNOWN ? ALERTA : OLIVE, false);
            graphics.drawString(font, progress.knowledgeLevel() == BestiaryKnowledgeLevel.UNKNOWN ? "????"
                            : entry.category().name() + "  /  " + entry.threat(),
                    left + 72, y + 17, 0xFF66716C, false);
            y += 42;
        }
        graphics.drawString(font, "A field guide is built one observation at a time.", left + 30, top + bookHeight - 38, OLIVE, false);
        graphics.drawString(font, "CLICK AN ENTRY TO OPEN THE RECORD", left + bookWidth / 2 + 20, top + bookHeight - 38, PETROLEO, false);
    }

    private void drawEntry(GuiGraphics graphics) {
        BestiaryEntryDefinition entry = BestiaryRegistry.get(selectedId);
        if (entry == null) {
            index = true;
            return;
        }
        BestiaryProgress progress = BestiaryClientState.progress(entry.id());
        drawTitle(graphics, nome(entry, progress), left + 30, top + 30);
        graphics.drawString(font, entry.category().name(), left + 30, top + 49, OLIVE, false);
        graphics.drawString(font, "THREAT  " + (progress.knowledgeLevel() == BestiaryKnowledgeLevel.UNKNOWN ? "?" : entry.threat()),
                left + bookWidth / 2 - 115, top + 49, ALERTA, false);
        drawPreview(graphics, entry, progress, left + 30, top + 78);
        graphics.drawString(font, "HABITAT", left + 30, top + 285, PETROLEO, false);
        graphics.drawString(font, progress.knowledgeLevel().atLeast(BestiaryKnowledgeLevel.OBSERVED)
                ? Component.translatable(entry.habitatKey()).getString() : "LOCKED", left + 30, top + 302, INK, false);
        graphics.drawString(font, "KNOWLEDGE", left + 30, top + 337, PETROLEO, false);
        graphics.drawString(font, status(progress), left + 30, top + 354, OLIVE, false);

        int right = left + bookWidth / 2 + 24;
        drawSection(graphics, "FIELD NOTES", right, top + 78);
        if (progress.knowledgeLevel() == BestiaryKnowledgeLevel.UNKNOWN) {
            graphics.drawWordWrap(font, Component.literal("Registro não catalogado. Observe a criatura para iniciar a ficha."), right,
                    top + 104, bookWidth / 2 - 52, INK);
        } else {
            graphics.drawWordWrap(font, Component.translatable(entry.summaryKey()), right, top + 104,
                    bookWidth / 2 - 52, INK);
            drawSection(graphics, "BEHAVIOR", right, top + 164);
            graphics.drawWordWrap(font, Component.translatable(progress.knowledgeLevel().atLeast(BestiaryKnowledgeLevel.FOUGHT)
                    ? entry.combatKey() : entry.behaviorKey()), right, top + 190, bookWidth / 2 - 52, INK);
            drawSection(graphics, "RESEARCH", right, top + 260);
            graphics.drawWordWrap(font, Component.literal(progress.knowledgeLevel().atLeast(BestiaryKnowledgeLevel.STUDIED)
                    ? "Weak points and advanced behavior recorded." : "Further observation required."), right, top + 286,
                    bookWidth / 2 - 52, INK);
        }
        graphics.drawString(font, "< INDEX", left + 30, top + bookHeight - 30, PETROLEO, false);
        graphics.drawString(font, "FIELD NOTE  /  01", right, top + bookHeight - 30, OLIVE, false);
    }

    private void drawPreview(GuiGraphics graphics, BestiaryEntryDefinition entry, BestiaryProgress progress, int x, int y) {
        graphics.fill(x, y, x + 220, y + 188, 0x334B5A57);
        if (progress.knowledgeLevel() == BestiaryKnowledgeLevel.UNKNOWN) {
            graphics.drawCenteredString(font, "?", x + 110, y + 70, 0xFF59615D);
            graphics.drawCenteredString(font, "SILHOUETTE LOCKED", x + 110, y + 103, OLIVE);
            return;
        }
        if (minecraft == null || minecraft.level == null) return;
        Entity preview = BuiltInRegistries.ENTITY_TYPE.getOptional(entry.entityType())
                .map(type -> type.create(minecraft.level)).orElse(null);
        if (preview instanceof LivingEntity living) {
            living.setYRot(25.0F);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 12, y + 12, x + 208, y + 178,
                    55, 0.06F, mouseXForPreview(), mouseYForPreview(), living);
        }
    }

    private int mouseXForPreview() { return left + 130; }
    private int mouseYForPreview() { return top + 170; }
    private void drawTitle(GuiGraphics g, String text, int x, int y) { g.drawString(font, text, x, y, INK, false); }
    private void drawSection(GuiGraphics g, String text, int x, int y) { g.drawString(font, text, x, y, PETROLEO, false); }
    private void drawMarker(GuiGraphics g, int x, int y, String text, boolean selected) {
        g.fill(x, y, x + 65, y + 25, selected ? OLIVE : 0xFF9EAA96);
        g.drawString(font, text, x + 7, y + 8, PAPER, false);
    }
    private String nome(BestiaryEntryDefinition entry, BestiaryProgress progress) {
        return progress.knowledgeLevel() == BestiaryKnowledgeLevel.UNKNOWN
                ? "????" : Component.translatable(entry.entityType().toLanguageKey()).getString();
    }
    private String status(BestiaryProgress progress) { return progress.knowledgeLevel().name(); }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (index && x >= left + 20 && x <= left + bookWidth - 20 && y >= top + 75 && y < top + bookHeight - 55) {
            int row = (int) ((y - (top + 78)) / 42);
            var entries = BestiaryRegistry.entries();
            if (row >= 0 && row < entries.size()) {
                selectedId = entries.get(row).id();
                index = false;
                return true;
            }
        }
        if (!index && x < left + 100 && y > top + bookHeight - 55) {
            index = true;
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
}
