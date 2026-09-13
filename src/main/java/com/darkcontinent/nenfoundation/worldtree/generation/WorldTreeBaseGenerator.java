package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Parcela idempotente da base Overworld pertencente ao chunk atual. */
public final class WorldTreeBaseGenerator {
    private static final int BASE_Y = 64;

    private WorldTreeBaseGenerator() {
    }

    public static void generateChunk(ChunkAccess chunk, WorldTreeLayout layout) {
        WorldTreeTrunkGenerator.generateBase(chunk, layout, BASE_Y);
        WorldTreeRootGenerator.generate(chunk, layout, BASE_Y);
    }
}
