package com.darkcontinent.nenfoundation.worldtree;

/** Faixas verticais do layout da World Tree na dimensao dedicada. */
public enum WorldTreeZone {
    LOWER_ASCENT(48, 320),
    CLOUD_SEA(320, 520),
    MID_BOUGHS(520, 820),
    HIGH_CANOPY(820, 1120),
    CROWN(1120, 1380),
    SUMMIT(1380, 1480);

    private final int minY;
    private final int maxYExclusive;

    WorldTreeZone(int minY, int maxYExclusive) {
        this.minY = minY;
        this.maxYExclusive = maxYExclusive;
    }

    public int minY() {
        return minY;
    }

    public int maxYExclusive() {
        return maxYExclusive;
    }

    public boolean contains(int y) {
        return y >= minY && y < maxYExclusive;
    }

    public static WorldTreeZone forY(int y) {
        for (WorldTreeZone zone : values()) {
            if (zone.contains(y)) {
                return zone;
            }
        }
        return null;
    }
}
