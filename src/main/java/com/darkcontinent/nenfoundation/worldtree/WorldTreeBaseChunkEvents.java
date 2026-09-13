package com.darkcontinent.nenfoundation.worldtree;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeBaseGenerator;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeBaseStructure;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Agenda a parcela da base fora do callback reentrante de carregamento. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class WorldTreeBaseChunkEvents {
    private WorldTreeBaseChunkEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        ChunkPos chunk = event.getChunk().getPos();
        WorldTreeSavedData data = level.getDataStorage().computeIfAbsent(
                WorldTreeSavedData.factory(), WorldTreeSavedData.DATA_ID);
        data.initialize(level.getSeed());
        if (!WorldTreeBaseStructure.chunkIntersectsGrove(event.getChunk(),
                data.overworldOriginX(), data.overworldOriginZ())
                || data.isBaseChunkGenerated(chunk.toLong())) {
            return;
        }
        MinecraftServer server = level.getServer();
        server.tell(new TickTask(server.getTickCount() + 1,
                () -> generateIfStillLoaded(level, chunk)));
    }

    private static void generateIfStillLoaded(ServerLevel level, ChunkPos chunk) {
        if (!level.hasChunk(chunk.x, chunk.z)) {
            return;
        }
        WorldTreeSavedData data = level.getDataStorage().computeIfAbsent(
                WorldTreeSavedData.factory(), WorldTreeSavedData.DATA_ID);
        if (data.isBaseChunkGenerated(chunk.toLong())) {
            return;
        }
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(level.getSeed(),
                data.overworldOriginX(), data.overworldOriginZ());
        WorldTreeBaseGenerator.generateChunk(level.getChunk(chunk.x, chunk.z), layout);
        data.markBaseChunkGenerated(chunk.toLong());
    }
}
