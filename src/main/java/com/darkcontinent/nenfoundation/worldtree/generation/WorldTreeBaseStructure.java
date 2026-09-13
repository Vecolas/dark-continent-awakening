package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Fachada chunk-local da base Overworld; localizacao e SavedData ficam em #278. */
public final class WorldTreeBaseStructure {
    public static final int GROVE_RADIUS = 180;

    private WorldTreeBaseStructure() {
    }

    public static void generateRoots(ChunkAccess chunk, WorldTreeLayout layout, int baseY) {
        WorldTreeRootGenerator.generate(chunk, layout, baseY);
    }

    public static boolean insideGrove(int blockX, int blockZ, WorldTreeLayout layout) {
        return Math.hypot(blockX - layout.overworldOriginX(), blockZ - layout.overworldOriginZ())
                <= GROVE_RADIUS;
    }
}
