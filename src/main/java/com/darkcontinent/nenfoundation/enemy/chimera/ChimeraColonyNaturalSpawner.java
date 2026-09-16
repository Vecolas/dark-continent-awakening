package com.darkcontinent.nenfoundation.enemy.chimera;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Funda colônias Chimera de forma rara, determinística e persistente. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class ChimeraColonyNaturalSpawner {

    private static final int PERIOD = 96;
    private static final int MIN_DISTANCE_BETWEEN_COLONIES = 128;
    private static final int PLAYER_SAFETY_RADIUS = 48;
    private static final int INITIAL_BIRTHS = 3;

    private ChimeraColonyNaturalSpawner() { }

    @SubscribeEvent
    public static void aoCarregarChunk(ChunkEvent.Load evento) {
        if (!(evento.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) return;
        ChunkPos chunk = evento.getChunk().getPos();
        if (!isCandidate(chunk)) return;

        MinecraftServer servidor = level.getServer();
        servidor.tell(new TickTask(servidor.getTickCount() + 1,
                () -> fundarSeAindaValido(level, chunk)));
    }

    /** Segunda metade do evento, fora do carregamento reentrante de chunks. */
    private static void fundarSeAindaValido(ServerLevel level, ChunkPos chunk) {
        if (!level.hasChunk(chunk.x, chunk.z)) return;
        ChimeraColonySavedData dados = ChimeraColonySavedData.de(level.getServer());
        BlockPos centro = pontoDeFundacao(level, chunk);
        if (!pontoSeguro(level, centro)
                || dados.existePerto(centro, MIN_DISTANCE_BETWEEN_COLONIES)) return;

        ChimeraColony colonia = new ChimeraColony(java.util.UUID.randomUUID(), centro);
        colonia.autorizarNascimentos(INITIAL_BIRTHS);
        dados.registrar(colonia);
        ChimeraColonyMaterializer.materializar(level, dados);
    }

    /** O hash usa as coordenadas, não o RNG do mundo: reload não muda o mapa. */
    static boolean isCandidate(ChunkPos chunk) {
        long hash = chunk.x * 341873128712L + chunk.z * 132897987541L;
        return Math.floorMod(hash, PERIOD) == 0;
    }

    static BlockPos pontoDeFundacao(ServerLevel level, ChunkPos chunk) {
        int x = chunk.getMinBlockX() + 8;
        int z = chunk.getMinBlockZ() + 8;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    static boolean pontoSeguro(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos) || pos.getY() <= level.getMinBuildHeight()
                || pos.getY() >= level.getMaxBuildHeight() - 2) return false;
        BlockState suporte = level.getBlockState(pos.below());
        if (!suporte.isSolid()) return false;
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
            return false;
        }
        double raio = PLAYER_SAFETY_RADIUS * (double) PLAYER_SAFETY_RADIUS;
        return level.players().stream().noneMatch(player ->
                player.isAlive() && player.distanceToSqr(pos.getX() + 0.5D,
                        pos.getY(), pos.getZ() + 0.5D) < raio);
    }
}
