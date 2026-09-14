package com.darkcontinent.nenfoundation.enemy.greedisland;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterServerHooks;
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
                                .executes(ctx -> teleportar(ctx.getSource(), 0.5D, 65.0D, 0.5D)))
                        .then(Commands.literal("status")
                                .executes(ctx -> status(ctx.getSource())))));
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

    /** Mostra o estado autoritativo dos episodios e da economia da ilha. */
    private static int status(net.minecraft.commands.CommandSourceStack source) {
        var controlador = EncounterServerHooks.controlador().orElse(null);
        if (controlador == null) {
            source.sendFailure(Component.literal("O controlador de encontros ainda nao iniciou."));
            return 0;
        }

        int encontrados = 0;
        for (var encontro : controlador.dados().instancias().values()) {
            if (!GreedIslandRegion.dentro(encontro.dimensao())) continue;
            encontrados++;
            source.sendSuccess(() -> Component.literal(String.format(
                    "GI %s: %s, entidades=%d, desfechos=%d, participantes=%d",
                    encontro.definitionId(), encontro.estado(), encontro.entidades().size(),
                    encontro.desfechos().size(), encontro.participantes().size())), false);
        }
        if (encontrados == 0) {
            source.sendSuccess(() -> Component.literal("Greed Island: nenhum encontro registrado."), false);
        }
        controlador.dados().cardsEmitidos().forEach((id, quantidade) ->
                source.sendSuccess(() -> Component.literal(
                        "Card " + id + ": " + quantidade + " emitido(s)"), false));
        return encontrados;
    }
}
