package com.darkcontinent.nenfoundation.structure;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Comandos permissionados para validar e colocar o posto em ambiente dev. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class PostoAvancadoCommands {
    private PostoAvancadoCommands() { }

    @SubscribeEvent
    public static void registrar(RegisterCommandsEvent evento) {
        evento.getDispatcher().register(Commands.literal("hxh")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("structure")
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .then(Commands.argument("rotation", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> {
                                                            builder.suggest("none");
                                                            builder.suggest("clockwise_90");
                                                            builder.suggest("clockwise_180");
                                                            builder.suggest("counterclockwise_90");
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(ctx -> spawn(ctx))))))));
    }

    private static int spawn(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        var source = ctx.getSource();
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int z = IntegerArgumentType.getInteger(ctx, "z");
        var rotacao = parseRotation(StringArgumentType.getString(ctx, "rotation"));
        if (rotacao == null) {
            source.sendFailure(Component.literal("Rotacao invalida. Use none, clockwise_90, clockwise_180 ou counterclockwise_90."));
            return 0;
        }
        var resultado = PostoAvancadoWorldPlacer.colocar(source.getLevel(), x, z, rotacao);
        if (resultado.rejeitado()) {
            source.sendFailure(Component.literal("Terreno rejeitado: variacao acima de 5 blocos."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Posto Hunter colocado: "
                + resultado.blocosUnicos() + " blocos em " + resultado.origem()
                + " (variacao do terreno " + resultado.variacaoTerreno() + ")."), true);
        return resultado.blocosUnicos();
    }

    static PostoAvancadoPlacementTransform.Rotacao parseRotation(String value) {
        return switch (value) {
            case "none" -> PostoAvancadoPlacementTransform.Rotacao.NONE;
            case "clockwise_90" -> PostoAvancadoPlacementTransform.Rotacao.CLOCKWISE_90;
            case "clockwise_180" -> PostoAvancadoPlacementTransform.Rotacao.CLOCKWISE_180;
            case "counterclockwise_90" -> PostoAvancadoPlacementTransform.Rotacao.COUNTERCLOCKWISE_90;
            default -> null;
        };
    }
}
