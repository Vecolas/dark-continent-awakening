package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeAnchorPlatform;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeTrunkSurface;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeCrownGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.ChunkPos;

/** Materializa anchors da rota principal somente no chunk que os contém. */
public final class WorldTreeCheckpointGenerator {
    private WorldTreeCheckpointGenerator() {
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout) {
        for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
            int y = checkpoint.y();
            BlockPos anchor = anchorPosition(layout, checkpoint);
            if (!platformIntersectsChunk(chunk, anchor)) {
                continue;
            }
            if (chunk.getPos().equals(new ChunkPos(anchor))) {
                chunk.setBlockState(anchor, WorldTreeBlocks.HUNTER_CLIMBING_ANCHOR.get()
                        .defaultBlockState(), false);
            }
            // A FORMA DA VARANDA MORA EM WorldTreeAnchorPlatform, e nao aqui.
            // Ela e a mesma do pe da arvore no Overworld, e enquanto morou
            // duplicada nos dois lacos ja tinha divergido -- la era UM bloco de
            // lenho embaixo da ancora.
            for (int dx = -WorldTreeAnchorPlatform.ALCANCE_PARA_DENTRO;
                    dx <= WorldTreeAnchorPlatform.ALCANCE_PARA_FORA; dx++) {
                for (int dz = -WorldTreeAnchorPlatform.ALCANCE_LATERAL;
                        dz <= WorldTreeAnchorPlatform.ALCANCE_LATERAL; dz++) {
                    if (!WorldTreeAnchorPlatform.contem(dx, dz)) {
                        continue;
                    }
                    BlockPos support = anchor.below().offset(dx, 0, dz);
                    if (chunk.getBlockState(support).isAir()) {
                        chunk.setBlockState(support, checkpoint == WorldTreeCheckpoint.SUMMIT
                                ? WorldTreeBlocks.WORLD_TREE_HEARTWOOD.get().defaultBlockState()
                                : WorldTreeBlocks.WORLD_TREE_DEADWOOD.get().defaultBlockState(), false);
                    }
                }
            }
        }
    }

    /**
     * Onde a ancora encosta na madeira.
     *
     * <p><b>DUAS COISAS ESTAVAM ERRADAS AQUI, e as duas produziam o mesmo
     * sintoma: ancora flutuando.</b>
     *
     * <p>A primeira era um {@code Math.max(18, ...)} sem relacao com a arvore.
     * Onde o tronco tem raio 10, ele punha a ancora em x=20 -- oito blocos de ar
     * entre a madeira e a plataforma.
     *
     * <p>A segunda sobreviveria a correcao da primeira: {@code radiusAt} devolve
     * o raio NOMINAL do perfil, e a casca que o gerador escreve tem lobos de ate
     * 4,5 blocos e ruido de ate 2. Usar o nominal erra por ate 6,5 blocos para
     * cada lado -- ora flutuando, ora com a ancora ENTERRADA na madeira.
     *
     * <p>Agora a conta e a mesma que o escritor usa, em
     * {@link WorldTreeTrunkSurface}. O {@code +1} e o unico folgado que sobra: a
     * ancora fica um bloco fora da casca, e a plataforma de baixo e que atravessa
     * para dentro da madeira.
     */
    static BlockPos anchorPosition(WorldTreeLayout layout, WorldTreeCheckpoint checkpoint) {
        if (checkpoint.y() > layout.trunk().topY()) {
            WorldTreePoint leader = WorldTreeCrownGenerator.leaderCenter(checkpoint.y(), layout.seed());
            double radius = WorldTreeCrownGenerator.leaderRadius(checkpoint.y());
            return new BlockPos((int) Math.ceil(leader.x() + radius) + 1,
                    checkpoint.y(), (int) Math.round(leader.z()));
        }
        int y = Math.min(checkpoint.y(), layout.trunk().topY());
        double casca = WorldTreeTrunkSurface.naDirecaoX(
                layout.trunk().radiusAt(y), y, layout.seed());
        return new BlockPos((int) Math.ceil(casca) + 1, checkpoint.y(), 0);
    }

    private static boolean platformIntersectsChunk(ChunkAccess chunk, BlockPos anchor) {
        int minX = WorldTreeAnchorPlatform.pontaInterna(anchor.getX());
        int maxX = anchor.getX() + WorldTreeAnchorPlatform.ALCANCE_PARA_FORA;
        int minZ = anchor.getZ() - WorldTreeAnchorPlatform.ALCANCE_LATERAL;
        int maxZ = anchor.getZ() + WorldTreeAnchorPlatform.ALCANCE_LATERAL;
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunkMinZ + 15;
        return chunkMinX <= maxX && chunkMaxX >= minX
                && chunkMinZ <= maxZ && chunkMaxZ >= minZ;
    }
}
