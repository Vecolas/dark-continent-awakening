package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Comando de jogador para seleção explícita de um anchor já desbloqueado. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class WorldTreeCheckpointCommands {
    private WorldTreeCheckpointCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hxh")
                .then(Commands.literal("worldtree")
                        .then(Commands.literal("travel")
                                .then(Commands.argument("checkpoint", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
                                                builder.suggest(checkpoint.id());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> travel(context.getSource(),
                                                StringArgumentType.getString(context, "checkpoint")))))));
    }

    private static int travel(net.minecraft.commands.CommandSourceStack source, String id) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Este comando exige um jogador."));
            return 0;
        }
        WorldTreeCheckpoint checkpoint = WorldTreeCheckpoint.byId(id);
        if (checkpoint == null) {
            source.sendFailure(Component.literal("Checkpoint desconhecido: " + id));
            return 0;
        }
        if (!WorldTreeCheckpointService.travelTo(player, checkpoint)) {
            source.sendFailure(Component.literal("Checkpoint ainda não desbloqueado ou dimensão indisponível."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Viajando para o anchor: " + checkpoint.id()), false);
        return 1;
    }
}
