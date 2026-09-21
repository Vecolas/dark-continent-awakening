package com.darkcontinent.nenfoundation.enemy.chimera;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Âncora visível do ninho; o estado real continua no SavedData da colônia. */
public final class ChimeraNestBlock extends Block {
    public ChimeraNestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement,
            boolean moving) {
        super.onRemove(state, level, pos, replacement, moving);
        if (level.isClientSide() || replacement.getBlock() == this
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        MinecraftServer servidor = level.getServer();
        if (servidor == null) return;
        // UMA leitura do SavedData, e nao duas. A anterior resolvia o servidor de
        // novo dentro do lambda: o mesmo dado lido por dois caminhos e como uma
        // divergencia entra sem dar erro.
        ChimeraColonySavedData dados = ChimeraColonySavedData.de(servidor);
        dados.coloniaNoNinho(pos).ifPresent(colonia -> dados.remover(colonia.id()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        MinecraftServer servidor = level.getServer();
        if (servidor == null) return InteractionResult.PASS;
        Optional<ChimeraColony> colonia = ChimeraColonySavedData.de(servidor).coloniaNoNinho(pos);
        if (colonia.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "block.nenfoundation.chimera_nest.orphan"));
        } else {
            ChimeraColony valor = colonia.get();
            player.sendSystemMessage(Component.translatable(
                    "block.nenfoundation.chimera_nest.status", valor.tamanho(),
                    valor.alerta(), valor.nascimentosPendentes()));
        }
        return InteractionResult.SUCCESS;
    }
}
