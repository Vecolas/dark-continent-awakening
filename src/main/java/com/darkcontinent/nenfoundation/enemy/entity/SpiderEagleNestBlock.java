package com.darkcontinent.nenfoundation.enemy.entity;

import com.darkcontinent.nenfoundation.registry.NenItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Ninho saqueável da Spider Eagle; retirar o ovo não exige matar a guardiã. */
public final class SpiderEagleNestBlock extends Block {
    public SpiderEagleNestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            ItemStack ovo = new ItemStack(NenItems.SPIDER_EAGLE_EGG.get());
            if (!player.getInventory().add(ovo)) {
                player.drop(ovo, false);
            }
            level.removeBlock(pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
