package com.darkcontinent.nenfoundation.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;

/** Estação de pesquisa; a tela de amostras será acoplada na próxima fatia. */
public final class ResearchTableBlock extends Block {
    public ResearchTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                    new ResearchTableMenu(id, inventory, pos),
                    Component.translatable("block.nenfoundation.research_table")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
