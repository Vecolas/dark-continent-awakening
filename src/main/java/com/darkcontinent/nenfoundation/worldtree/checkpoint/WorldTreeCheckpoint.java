package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeZone;

/** Checkpoints fixos da rota principal, derivados da altitude. */
public enum WorldTreeCheckpoint {
    BASE(48, "base"),
    LOWER(260, "lower"),
    CLOUD(400, "cloud"),
    MID(650, "mid"),
    CANOPY(950, "canopy"),
    CROWN(1250, "crown"),
    SUMMIT(1450, "summit");

    private final int y;
    private final String id;

    WorldTreeCheckpoint(int y, String id) {
        this.y = y;
        this.id = id;
    }

    public int y() { return y; }
    public String id() { return id; }

    public WorldTreeZone zone() {
        return WorldTreeZone.forY(y);
    }

    public static WorldTreeCheckpoint nearest(int y) {
        WorldTreeCheckpoint result = null;
        int distance = Integer.MAX_VALUE;
        for (WorldTreeCheckpoint checkpoint : values()) {
            int candidate = Math.abs(checkpoint.y - y);
            if (candidate < distance) {
                result = checkpoint;
                distance = candidate;
            }
        }
        return distance <= 24 ? result : null;
    }
}
