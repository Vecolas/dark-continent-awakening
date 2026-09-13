package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Parcela idempotente da base Overworld pertencente ao chunk atual. */
public final class WorldTreeBaseGenerator {
    private static final int BASE_Y = 64;

    private WorldTreeBaseGenerator() {
    }

    public static void generateChunk(ChunkAccess chunk, WorldTreeLayout layout) {
        WorldTreeTrunkGenerator.generateBase(chunk, layout, BASE_Y);
        WorldTreeRootGenerator.generate(chunk, layout, BASE_Y);
        BlockPos anchor = new BlockPos(layout.overworldOriginX() + 49, BASE_Y + 16,
                layout.overworldOriginZ());
        if (chunk.getPos().equals(new net.minecraft.world.level.ChunkPos(anchor))
                && chunk.getBlockState(anchor).isAir()) {
            chunk.setBlockState(anchor, WorldTreeBlocks.HUNTER_CLIMBING_ANCHOR.get()
                    .defaultBlockState(), false);
            BlockPos support = anchor.below();
            if (chunk.getBlockState(support).isAir()) {
                chunk.setBlockState(support, WorldTreeBlocks.WORLD_TREE_DEADWOOD.get()
                        .defaultBlockState(), false);
            }
        }
    }
}
