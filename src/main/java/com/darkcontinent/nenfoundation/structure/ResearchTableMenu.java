package com.darkcontinent.nenfoundation.structure;

import com.darkcontinent.nenfoundation.item.FieldNoteItem;
import com.darkcontinent.nenfoundation.registry.NenMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ResearchTableMenu extends AbstractContainerMenu {
    private final BlockPos tablePos;
    private final Level level;
    private final Player owner;
    private final ResearchSlot researchSlot;

    public ResearchTableMenu(int id, Inventory inventory, BlockPos tablePos) {
        super(NenMenus.RESEARCH_TABLE.get(), id);
        this.tablePos = tablePos;
        this.level = inventory.player.level();
        this.owner = inventory.player;
        this.researchSlot = new ResearchSlot();
        addSlot(researchSlot);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }

    public static ResearchTableMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        return new ResearchTableMenu(id, inventory, buffer.readBlockPos());
    }

    @Override public boolean stillValid(Player player) {
        return level.getBlockState(tablePos).getBlock() instanceof ResearchTableBlock
                && player.distanceToSqr(tablePos.getCenter()) <= 64.0D;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack copy = slot.getItem().copy();
        if (index == 0) {
            if (!moveItemStackTo(slot.getItem(), 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (slot.getItem().getItem() instanceof FieldNoteItem) {
            if (!moveItemStackTo(slot.getItem(), 0, 1, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (slot.getItem().isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public void slotsChanged(net.minecraft.world.Container container) {
        super.slotsChanged(container);
        if (!level.isClientSide() && researchSlot.hasItem()
                && researchSlot.getItem().getItem() instanceof FieldNoteItem note
                && owner instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && stillValid(owner)) {
            note.applyAtResearchTable(serverPlayer,
                    researchSlot.getItem());
        }
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && researchSlot.hasItem())
            player.getInventory().placeItemBackInInventory(researchSlot.remove(researchSlot.getItem().getCount()));
    }

    private final class ResearchSlot extends Slot {
        ResearchSlot() { super(new net.minecraft.world.SimpleContainer(1), 0, 80, 35); }
        @Override public void setChanged() { super.setChanged(); }
    }
}
