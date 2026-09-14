package com.darkcontinent.nenfoundation.worldtree;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Harness de servidor para medir custo de carregar uma grade de chunks da árvore. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class WorldTreeStressCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldTreeStressCommand.class);

    private WorldTreeStressCommand() {
    }

    @SubscribeEvent
    public static void registrar(RegisterCommandsEvent evento) {
        evento.getDispatcher().register(Commands.literal("hxh")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("worldtree")
                        .then(Commands.literal("stress")
                                .then(Commands.argument("size", IntegerArgumentType.integer(1, 20))
                                        .executes(ctx -> executar(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "size")))))));
    }

    private static int executar(net.minecraft.commands.CommandSourceStack source, int size) {
        ServerLevel level = source.getServer().getLevel(WorldTreeDebugCommands.WORLD_TREE_LEVEL);
        if (level == null) {
            source.sendFailure(Component.literal("A dimensao World Tree nao esta disponivel."));
            return 0;
        }

        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        long started = System.nanoTime();
        int start = -(size / 2);
        int loaded = 0;
        for (int chunkX = start; chunkX < start + size; chunkX++) {
            for (int chunkZ = start; chunkZ < start + size; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
                loaded++;
            }
        }
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long deltaMiB = (afterMemory - beforeMemory) / (1024L * 1024L);
        double mspt = level.getServer().getAverageTickTimeNanos() / 1_000_000.0D;
        String result = "World Tree stress " + size + "x" + size + ": " + loaded
                + " chunks, " + elapsedMillis + " ms, MSPT " + String.format(java.util.Locale.ROOT,
                        "%.2f", mspt) + ", memory delta " + deltaMiB + " MiB";
        LOGGER.info(result);
        source.sendSuccess(() -> Component.literal(result), true);
        return loaded;
    }
}
