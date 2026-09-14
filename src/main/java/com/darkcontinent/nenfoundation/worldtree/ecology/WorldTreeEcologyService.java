package com.darkcontinent.nenfoundation.worldtree.ecology;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeAltitudeService;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeZone;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Deriva habitat e fauna sem manter estado derivado no jogador. */
public final class WorldTreeEcologyService {
    private static final Set<String> CLOUD_SPECIES = Set.of("spider_eagle", "cloud_moth");
    private static final Set<String> CANOPY_SPECIES = Set.of("spider_eagle", "canopy_beast");

    private WorldTreeEcologyService() {
    }

    public static WorldTreeHabitat habitatAt(ResourceKey<Level> dimension, int y) {
        if (!WorldTreeAltitudeService.isWorldTree(dimension)) {
            return null;
        }
        WorldTreeZone zone = WorldTreeAltitudeService.zoneAt(y);
        if (zone == null) {
            return null;
        }
        return switch (zone) {
            case LOWER_ASCENT -> WorldTreeHabitat.TRUNK;
            case CLOUD_SEA -> WorldTreeHabitat.CLOUD_LAYER;
            case MID_BOUGHS, HIGH_CANOPY -> WorldTreeHabitat.CANOPY;
            case CROWN, SUMMIT -> WorldTreeHabitat.CROWN;
        };
    }

    public static boolean speciesAllowed(ResourceKey<Level> dimension, int y, String speciesId) {
        WorldTreeHabitat habitat = habitatAt(dimension, y);
        if (habitat == WorldTreeHabitat.CLOUD_LAYER) {
            return CLOUD_SPECIES.contains(speciesId);
        }
        if (habitat == WorldTreeHabitat.CANOPY) {
            return CANOPY_SPECIES.contains(speciesId);
        }
        return false;
    }

    public static int resourceTier(ResourceKey<Level> dimension, int y) {
        WorldTreeHabitat habitat = habitatAt(dimension, y);
        if (habitat == null) {
            return 0;
        }
        return switch (habitat) {
            case GROVE, TRUNK -> 1;
            case CLOUD_LAYER -> 2;
            case CANOPY -> 3;
            case CROWN -> 4;
        };
    }
}
