package com.darkcontinent.nenfoundation.worldtree;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Comandos permissionados para validar a casca da dimensao World Tree. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class WorldTreeDebugCommands {
    public static final ResourceKey<Level> WORLD_TREE_LEVEL = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            NenFoundation.id("world_tree"));

    private WorldTreeDebugCommands() {
    }

    @SubscribeEvent
    public static void registrar(RegisterCommandsEvent evento) {
        evento.getDispatcher().register(Commands.literal("hxh")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("worldtree")
                        .then(Commands.literal("tp")
                                .then(Commands.argument("ponto", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("lower");
                                            builder.suggest("cloud");
                                            builder.suggest("canopy");
                                            builder.suggest("crown");
                                            builder.suggest("summit");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> teleportarPonto(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "ponto")))))
                        .then(Commands.literal("tp_y")
                                .then(Commands.argument("y", IntegerArgumentType.integer(0, 1535))
                                        .executes(ctx -> teleportarY(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "y")))))));
    }

    private static int teleportarPonto(net.minecraft.commands.CommandSourceStack source,
            String ponto) {
        int y = switch (ponto) {
            case "lower" -> 48;
            case "cloud" -> 400;
            case "canopy" -> 900;
            case "crown" -> 1250;
            case "summit" -> 1450;
            default -> -1;
        };
        if (y < 0) {
            source.sendFailure(Component.literal(
                    "Ponto invalido. Use lower, cloud, canopy, crown ou summit."));
            return 0;
        }
        return teleportar(source, y);
    }

    private static int teleportarY(net.minecraft.commands.CommandSourceStack source, int y) {
        return teleportar(source, y);
    }

    private static int teleportar(net.minecraft.commands.CommandSourceStack source, int y) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            source.sendFailure(Component.literal("Este comando exige um jogador."));
            return 0;
        }

        ServerLevel destination = source.getServer().getLevel(WORLD_TREE_LEVEL);
        if (destination == null) {
            source.sendFailure(Component.literal(
                    "A dimensao nenfoundation:world_tree nao esta carregada."));
            return 0;
        }

        player.teleportTo(destination, 0.5D, y, 0.5D, player.getYRot(), player.getXRot());
        source.sendSuccess(() -> Component.literal(
                "Teletransportado para World Tree em Y=" + y + "."), true);
        return 1;
    }
}
