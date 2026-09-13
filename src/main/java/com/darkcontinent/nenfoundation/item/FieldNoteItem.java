package com.darkcontinent.nenfoundation.item;

import com.darkcontinent.nenfoundation.server.BestiaryPlayerService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Nota de campo consumível; a autoridade do desbloqueio permanece no servidor. */
public final class FieldNoteItem extends Item {
    private final ResourceLocation entryId;
    private final int researchPoints;

    public FieldNoteItem(Properties properties, ResourceLocation entryId, int researchPoints) {
        super(properties.stacksTo(1));
        if (researchPoints <= 0) throw new IllegalArgumentException("nota precisa conceder pesquisa");
        this.entryId = entryId;
        this.researchPoints = researchPoints;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            BestiaryPlayerService.pesquisar(serverPlayer, entryId, researchPoints);
            stack.shrink(1);
            player.displayClientMessage(Component.translatable("item.nenfoundation.field_note.used"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockState(context.getClickedPos()).getBlock()
                instanceof com.darkcontinent.nenfoundation.structure.ResearchTableBlock)) {
            return super.useOn(context);
        }
        if (!context.getLevel().isClientSide() && context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
            BestiaryPlayerService.pesquisar(player, entryId, researchPoints);
            context.getItemInHand().shrink(1);
            player.displayClientMessage(Component.translatable("item.nenfoundation.field_note.researched"), true);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }
}
