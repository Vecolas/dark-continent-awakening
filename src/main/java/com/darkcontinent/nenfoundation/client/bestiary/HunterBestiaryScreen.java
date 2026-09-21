package com.darkcontinent.nenfoundation.client.bestiary;

import com.darkcontinent.nenfoundation.bestiary.BestiaryEntryDefinition;
import com.darkcontinent.nenfoundation.bestiary.BestiaryKnowledgeLevel;
import com.darkcontinent.nenfoundation.bestiary.BestiaryRegistry;
import com.darkcontinent.nenfoundation.bestiary.BestiaryProgress;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.components.EditBox;
import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import com.darkcontinent.nenfoundation.bestiary.BestiaryCategory;

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
    private boolean draggingPreview;
    private int previewCursorX;
    private int previewCursorY;
    private float previewScale = 0.06F;
    private BestiaryCategory categoryFilter;
    private EditBox searchBox;
    private final Map<net.minecraft.resources.ResourceLocation, LivingEntity> previewCache = new HashMap<>();
    private int selectedIndex;

    public HunterBestiaryScreen() {
        super(Component.translatable("item.nenfoundation.hunter_bestiary"));
    }

    @Override
    protected void init() {
        // Keep the field guide readable without turning it into a full-screen
        // panel. The content coordinates below are designed for this compact
        // Every page reserves separate vertical bands so optional sections
        // cannot draw over the footer or over one another.
        bookWidth = Math.min(620, width - 40);
        bookHeight = Math.min(370, height - 40);
        left = (width - bookWidth) / 2;
        top = (height - bookHeight) / 2;
        previewCursorX = left + 120;
        previewCursorY = top + 130;
        searchBox = new EditBox(font, left + bookWidth / 2 + 20, top + 28, Math.max(80, bookWidth / 2 - 50), 18,
                Component.literal("Search"));
        searchBox.setHint(Component.literal("Search field notes"));
        addRenderableWidget(searchBox);
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
        searchBox.setVisible(index);
        if (!index) searchBox.setFocused(false);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (index) {
            int tabsY = top + 48;
            drawMarker(graphics, left + 25, tabsY, "ALL", categoryFilter == null);
            drawMarker(graphics, left + 95, tabsY, "WILD", categoryFilter == BestiaryCategory.WILDLIFE);
            drawMarker(graphics, left + 165, tabsY, "MAGIC", categoryFilter == BestiaryCategory.MAGICAL);
            drawMarker(graphics, left + 235, tabsY, "SPECIAL", categoryFilter == BestiaryCategory.SPECIAL);
            drawIndex(graphics);
        } else drawEntry(graphics);
    }

    private void drawIndex(GuiGraphics graphics) {
        drawTitle(graphics, "FIELD INDEX", left + 30, top + 17);
        graphics.drawString(font, "ASSOCIATION HUNTER / FIELD RESEARCH", left + 30, top + 35, OLIVE, false);
        int y = top + 100;
        int number = 1;
        int visible = 0;
        int rowIndex = 0;
        int maxRows = Math.max(1, (bookHeight - 120) / 34);
        for (BestiaryEntryDefinition entry : filteredEntries()) {
            if (visible == maxRows) break;
            BestiaryProgress progress = BestiaryClientState.progress(entry.id());
            graphics.fill(left + 25, y - 4, left + bookWidth - 25, y + 29,
                    rowIndex++ == selectedIndex ? 0x55606A5E : 0x22606A5E);
            graphics.drawString(font, String.format("%02d", number++), left + 35, y + 5, OLIVE, false);
            graphics.drawString(font, nome(entry, progress), left + 72, y + 5, INK, false);
            graphics.drawString(font, status(progress), left + bookWidth / 2 + 24, y + 5,
                    progress.knowledgeLevel() == BestiaryKnowledgeLevel.UNKNOWN ? ALERTA : OLIVE, false);
            graphics.drawString(font, progress.knowledgeLevel() == BestiaryKnowledgeLevel.UNKNOWN ? "????"
                            : categoryName(entry) + "  /  " + entry.threat(),
                    left + 72, y + 17, 0xFF66716C, false);
            y += 34;
            visible++;
        }
        if (visible == 0) graphics.drawString(font, "NO FIELD NOTES MATCH", left + 35, top + 105, ALERTA, false);
        int footerY = top + bookHeight - 16;
        graphics.drawString(font, "A field guide is built one observation at a time.", left + 30, footerY, OLIVE, false);
        graphics.drawString(font, "CLICK AN ENTRY TO OPEN THE RECORD", left + bookWidth / 2 + 20, footerY, PETROLEO, false);
    }

    private void drawEntry(GuiGraphics graphics) {
        BestiaryEntryDefinition entry = BestiaryRegistry.get(selectedId);
        if (entry == null) {
            index = true;
            return;
        }
        BestiaryProgress progress = BestiaryClientState.progress(entry.id());
        drawTitle(graphics, nome(entry, progress), left + 30, top + 20);
        graphics.drawString(font, categoryName(entry), left + 30, top + 38, OLIVE, false);
        graphics.drawString(font, "THREAT  " + (progress.knowledgeLevel() == BestiaryKnowledgeLevel.UNKNOWN ? "?" : entry.threat()),
                left + bookWidth / 2 + 24, top + 38, ALERTA, false);
        drawPreview(graphics, entry, progress, left + 30, top + 60);
        graphics.drawString(font, "HABITAT", left + 30, top + 216, PETROLEO, false);
        graphics.drawString(font, progress.knowledgeLevel().atLeast(BestiaryKnowledgeLevel.OBSERVED)
                ? Component.translatable(entry.habitatKey()).getString() : "LOCKED", left + 30, top + 233, INK, false);
        graphics.drawString(font, "KNOWLEDGE", left + 30, top + 257, PETROLEO, false);
        graphics.drawString(font, status(progress), left + 30, top + 274, OLIVE, false);

        int right = left + bookWidth / 2 + 24;
        int textWidth = bookWidth / 2 - 52;
        drawSection(graphics, "FIELD NOTES", right, top + 60);
        if (progress.knowledgeLevel() == BestiaryKnowledgeLevel.UNKNOWN) {
            graphics.drawWordWrap(font, Component.literal("Registro não catalogado. Observe a criatura para iniciar a ficha."), right,
                    top + 80, textWidth, INK);
        } else {
            drawWrapped(graphics, Component.translatable(entry.summaryKey()), right, top + 80,
                    textWidth, INK, 3);
            drawSection(graphics, "BEHAVIOR", right, top + 115);
            drawWrapped(graphics, Component.translatable(progress.knowledgeLevel().atLeast(BestiaryKnowledgeLevel.FOUGHT)
                    ? entry.combatKey() : entry.behaviorKey()), right, top + 135, textWidth, INK, 4);
            drawSection(graphics, "RESEARCH", right, top + 190);
            drawResearchMeter(graphics, entry, progress, right, top + 208);
            if (progress.knowledgeLevel().atLeast(BestiaryKnowledgeLevel.STUDIED)) {
                drawSection(graphics, "WEAK POINTS", right, top + 245);
                drawWrapped(graphics, Component.literal(progress.knowledgeLevel().atLeast(BestiaryKnowledgeLevel.MASTERED)
                        ? "Weak points and advanced behavior recorded in the field report."
                        : "A strategic opening has been identified. Further confirmation is required."),
                        right, top + 263, textWidth, INK, 3);
                if (!progress.weakPointsDiscovered().isEmpty()) {
                    drawSingleLine(graphics, Component.translatable("bestiary.weak_point.discovered"),
                            right, top + 296, textWidth, ALERTA);
                } else if (progress.specialDiscoveries().contains("foxbear.territorial_behavior")) {
                    drawSingleLine(graphics, Component.translatable("bestiary.discovery.foxbear.territorial"),
                            right, top + 296, textWidth, OLIVE);
                }
                if (progress.nenStatus() != com.darkcontinent.nenfoundation.bestiary.BestiaryNenStatus.NONE) {
                    drawSection(graphics, "NEN", right, top + 315);
                    graphics.drawString(font, progress.nenStatus().name(), right, top + 332, PETROLEO, false);
                }
            }
        }
        int footerY = top + bookHeight - 16;
        graphics.drawString(font, "< INDEX", left + 30, footerY, PETROLEO, false);
        graphics.drawString(font, "FIELD NOTE  /  01", right, footerY, OLIVE, false);
    }

    private void drawPreview(GuiGraphics graphics, BestiaryEntryDefinition entry, BestiaryProgress progress, int x, int y) {
        int previewWidth = previewWidth();
        graphics.fill(x, y, x + previewWidth, y + 145, 0x334B5A57);
        if (progress.knowledgeLevel() == BestiaryKnowledgeLevel.UNKNOWN) {
            graphics.drawCenteredString(font, "?", x + previewWidth / 2, y + 52, 0xFF59615D);
            graphics.drawCenteredString(font, "SILHOUETTE LOCKED", x + previewWidth / 2, y + 82, OLIVE);
            return;
        }
        if (minecraft == null || minecraft.level == null) return;
        Entity preview = previewCache.computeIfAbsent(entry.id(), ignored ->
                BuiltInRegistries.ENTITY_TYPE.getOptional(entry.entityType())
                        .map(type -> type.create(minecraft.level))
                        .filter(LivingEntity.class::isInstance)
                        .map(LivingEntity.class::cast).orElse(null));
        if (preview instanceof LivingEntity living) {
            living.setYRot(25.0F);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 5, y + 10, x + previewWidth - 10, y + 140,
                    (int) (55 * (previewScale / 0.06F)), previewScale,
                    mouseXForPreview(), mouseYForPreview(), living);
        }
    }

    private int mouseXForPreview() { return previewCursorX; }
    private int mouseYForPreview() { return previewCursorY; }
    private int previewWidth() { return Math.min(180, Math.max(80, bookWidth / 2 - 40)); }
    private void drawTitle(GuiGraphics g, String text, int x, int y) { g.drawString(font, text, x, y, INK, false); }
    private void drawSection(GuiGraphics g, String text, int x, int y) { g.drawString(font, text, x, y, PETROLEO, false); }
    private void drawWrapped(GuiGraphics g, Component text, int x, int y, int width, int color, int maxLines) {
        var lines = font.split(text, width);
        for (int line = 0; line < Math.min(maxLines, lines.size()); line++) {
            g.drawString(font, lines.get(line), x, y + line * 9, color, false);
        }
    }
    private void drawSingleLine(GuiGraphics g, Component text, int x, int y, int width, int color) {
        g.drawString(font, font.plainSubstrByWidth(text.getString(), width), x, y, color, false);
    }
    private void drawMarker(GuiGraphics g, int x, int y, String text, boolean selected) {
        g.fill(x, y, x + 65, y + 25, selected ? OLIVE : 0xFF9EAA96);
        g.drawString(font, text, x + 7, y + 8, PAPER, false);
    }
    private String nome(BestiaryEntryDefinition entry, BestiaryProgress progress) {
        return progress.knowledgeLevel() == BestiaryKnowledgeLevel.UNKNOWN
                ? "????" : Component.translatable("bestiary.entry." + entry.id().getPath()).getString();
    }
    private String status(BestiaryProgress progress) {
        return Component.translatable("bestiary.knowledge." + progress.knowledgeLevel().name().toLowerCase(Locale.ROOT)).getString();
    }

    private String categoryName(BestiaryEntryDefinition entry) {
        return Component.translatable("bestiary.category." + entry.category().name().toLowerCase(Locale.ROOT)).getString();
    }

    private void drawResearchMeter(GuiGraphics graphics, BestiaryEntryDefinition entry,
            BestiaryProgress progress, int x, int y) {
        int width = bookWidth / 2 - 52;
        int target = Math.max(1, entry.masteredAt());
        int filled = Math.min(width, width * progress.researchPoints() / target);
        graphics.fill(x, y, x + width, y + 7, 0x335A665E);
        graphics.fill(x, y, x + filled, y + 7, progress.knowledgeLevel().atLeast(BestiaryKnowledgeLevel.MASTERED)
                ? ALERTA : OLIVE);
        graphics.drawString(font, "RESEARCH " + progress.researchPoints() + " / " + target,
                x, y + 12, INK, false);
    }

    private List<BestiaryEntryDefinition> filteredEntries() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        return BestiaryRegistry.entries().stream()
                .filter(entry -> categoryFilter == null || entry.category() == categoryFilter)
                .filter(entry -> query.isEmpty()
                        || nome(entry, BestiaryClientState.progress(entry.id())).toLowerCase(Locale.ROOT).contains(query)
                        || entry.category().name().toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (index && x >= left + 25 && x <= left + 90 && y >= top + 48 && y <= top + 73) {
            categoryFilter = null;
            return true;
        }
        if (index && x >= left + 95 && x <= left + 160 && y >= top + 48 && y <= top + 73) {
            categoryFilter = BestiaryCategory.WILDLIFE;
            return true;
        }
        if (index && x >= left + 165 && x <= left + 230 && y >= top + 48 && y <= top + 73) {
            categoryFilter = BestiaryCategory.MAGICAL;
            return true;
        }
        if (index && x >= left + 235 && x <= left + 300 && y >= top + 48 && y <= top + 73) {
            categoryFilter = BestiaryCategory.SPECIAL;
            return true;
        }
        if (!index && button == 0 && x >= left + 30 && x <= left + 30 + previewWidth()
                && y >= top + 60 && y <= top + 205) {
            draggingPreview = true;
            previewCursorX = (int) x;
            previewCursorY = (int) y;
            return true;
        }
        if (index && x >= left + 20 && x <= left + bookWidth - 20 && y >= top + 96 && y < top + bookHeight - 30) {
            int row = (int) ((y - (top + 100)) / 34);
            var entries = filteredEntries();
            if (row >= 0 && row < entries.size()) {
                selectedIndex = row;
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

    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && draggingPreview) {
            draggingPreview = false;
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        if (draggingPreview && button == 0) {
            previewCursorX += (int) dragX * 2;
            previewCursorY += (int) dragY;
            return true;
        }
        return super.mouseDragged(x, y, button, dragX, dragY);
    }

    @Override public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!index && x >= left + 30 && x <= left + 30 + previewWidth() && y >= top + 60 && y <= top + 205) {
            previewScale = Math.max(0.035F, Math.min(0.095F, previewScale + (float) scrollY * 0.006F));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { onClose(); return true; }
        if (searchBox != null && searchBox.isFocused()) return super.keyPressed(keyCode, scanCode, modifiers);
        var entries = filteredEntries();
        if (index && !entries.isEmpty()) {
            if (keyCode == 87) { selectedIndex = Math.max(0, selectedIndex - 1); return true; }
            if (keyCode == 83) { selectedIndex = Math.min(entries.size() - 1, selectedIndex + 1); return true; }
            if (keyCode == 257) { selectedId = entries.get(selectedIndex).id(); index = false; return true; }
        } else if (!index && keyCode == 65) {
            index = true;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void onClose() {
        previewCache.clear();
        super.onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
}
