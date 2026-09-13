package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Materializa anchors da rota principal somente no chunk que os contém. */
public final class WorldTreeCheckpointGenerator {
    private WorldTreeCheckpointGenerator() {
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout) {
        for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
            int y = checkpoint.y();
            int radius = Math.max(18, (int) Math.ceil(layout.trunk().radiusAt(Math.min(y,
                    layout.trunk().topY()))));
            BlockPos anchor = new BlockPos(radius + 1, y, 0);
            if (!chunk.getPos().equals(new net.minecraft.world.level.ChunkPos(anchor))) {
                continue;
            }
            BlockState current = chunk.getBlockState(anchor);
            if (!current.isAir()) {
                continue;
            }
            chunk.setBlockState(anchor, WorldTreeBlocks.HUNTER_CLIMBING_ANCHOR.get()
                    .defaultBlockState(), false);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dz * dz > 5) {
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
}
