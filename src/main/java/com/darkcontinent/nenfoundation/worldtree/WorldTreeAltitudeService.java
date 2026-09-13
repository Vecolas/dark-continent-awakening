package com.darkcontinent.nenfoundation.worldtree;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Serviço puro para derivar a zona atual sem congelar estado no jogador. */
public final class WorldTreeAltitudeService {
    private WorldTreeAltitudeService() {
    }

    public static WorldTreeZone zoneAt(int y) {
        return WorldTreeZone.forY(y);
    }

    public static boolean isWorldTree(ResourceKey<Level> dimension) {
        return dimension == WorldTreeDebugCommands.WORLD_TREE_LEVEL;
    }

    public static boolean isCloudSea(int y) {
        return WorldTreeZone.CLOUD_SEA.contains(y);
    }

    public static boolean isSummit(int y) {
        return WorldTreeZone.SUMMIT.contains(y);
    }
}
