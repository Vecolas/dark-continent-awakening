package com.darkcontinent.nenfoundation.structure;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Spawn natural determinístico e persistente do posto, sem duplicação em reload. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class PostoAvancadoNaturalSpawner {
    private static final String DATA_ID = "hunter_outpost_spawns";
    private static final int PERIOD = 32;

    private PostoAvancadoNaturalSpawner() { }

    @SubscribeEvent
    public static void aoCarregarChunk(ChunkEvent.Load evento) {
        if (!(evento.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ChunkPos chunk = evento.getChunk().getPos();
        if (!isCandidate(chunk)) {
            return;
        }
        var data = level.getDataStorage().computeIfAbsent(
                PostoAvancadoSpawnData.factory(), DATA_ID);
        long chave = chunk.toLong();
        if (data.contem(chave)) {
            return;
        }
        int centroX = chunk.getMinBlockX() + 8;
        int centroZ = chunk.getMinBlockZ() + 8;
        var resultado = PostoAvancadoWorldPlacer.colocar(level, centroX, centroZ,
                PostoAvancadoPlacementTransform.Rotacao.values()[Math.floorMod(chunk.x + chunk.z, 4)]);
        if (!resultado.rejeitado()) {
            data.registrar(chave);
        }
    }

    static boolean isCandidate(ChunkPos chunk) {
        return Math.floorMod(chunk.x * 31 + chunk.z, PERIOD) == 0;
    }
}
