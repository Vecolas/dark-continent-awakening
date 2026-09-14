package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
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
            for (int dx = -6; dx <= 3; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    double normalizedX = (dx + 1.5) / 5.5;
                    double normalizedZ = dz / 4.25;
                    if (normalizedX * normalizedX + normalizedZ * normalizedZ > 1.0) {
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

    static BlockPos anchorPosition(WorldTreeLayout layout, WorldTreeCheckpoint checkpoint) {
        if (checkpoint.y() > layout.trunk().topY()) {
            WorldTreePoint leader = WorldTreeCrownGenerator.leaderCenter(checkpoint.y(), layout.seed());
            double radius = WorldTreeCrownGenerator.leaderRadius(checkpoint.y());
            return new BlockPos((int) Math.ceil(leader.x() + radius) + 2,
                    checkpoint.y(), (int) Math.round(leader.z()));
        }
        int y = Math.min(checkpoint.y(), layout.trunk().topY());
        return new BlockPos(Math.max(18, (int) Math.ceil(layout.trunk().radiusAt(y))) + 2,
                checkpoint.y(), 0);
    }

    private static boolean platformIntersectsChunk(ChunkAccess chunk, BlockPos anchor) {
        int minX = anchor.getX() - 6;
        int maxX = anchor.getX() + 3;
        int minZ = anchor.getZ() - 4;
        int maxZ = anchor.getZ() + 4;
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunkMinZ + 15;
        return chunkMinX <= maxX && chunkMaxX >= minX
                && chunkMinZ <= maxZ && chunkMaxZ >= minZ;
    }
}
