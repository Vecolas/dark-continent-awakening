package com.darkcontinent.nenfoundation.enemy.greedisland;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Acesso administrativo à dimensão jogável de Greed Island. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class GreedIslandDebugCommands {
    private GreedIslandDebugCommands() { }

    @SubscribeEvent
    public static void registrar(RegisterCommandsEvent evento) {
        evento.getDispatcher().register(Commands.literal("hxh")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("greedisland")
                        .then(Commands.literal("tp")
                                .executes(ctx -> teleportar(ctx.getSource(), 0.5D, 65.0D, 0.5D)))));
    }

    private static int teleportar(net.minecraft.commands.CommandSourceStack source,
            double x, double y, double z) {
        final ServerPlayer jogador;
        try {
            jogador = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException erro) {
            source.sendFailure(Component.literal("Este comando exige um jogador."));
            return 0;
        }

        ServerLevel ilha = source.getServer().getLevel(GreedIslandRegion.DIMENSAO);
        if (ilha == null) {
            source.sendFailure(Component.literal(
                    "A dimensao nenfoundation:greed_island nao esta carregada."));
            return 0;
        }

        jogador.teleportTo(ilha, x, y, z, jogador.getYRot(), jogador.getXRot());
        source.sendSuccess(() -> Component.literal(
                "Teletransportado para Greed Island em " + x + " " + y + " " + z + "."), true);
        return 1;
    }
}
