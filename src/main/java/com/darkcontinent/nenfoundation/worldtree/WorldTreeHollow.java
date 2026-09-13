package com.darkcontinent.nenfoundation.worldtree;

/** Pocket natural interno, sempre mantido dentro do envelope do tronco. */
public record WorldTreeHollow(int centerY, double centerX, double centerZ,
        double radiusX, double radiusY, double radiusZ) {
    public WorldTreeHollow {
        if (centerY < 48 || centerY >= 1200 || !Double.isFinite(centerX) || !Double.isFinite(centerZ)
                || radiusX <= 0.0 || radiusY <= 0.0 || radiusZ <= 0.0) {
            throw new IllegalArgumentException("cavidade invalida");
        }
    }

    public boolean contains(double x, double y, double z) {
        double dx = (x - centerX) / radiusX;
        double dy = (y - centerY) / radiusY;
        double dz = (z - centerZ) / radiusZ;
        return dx * dx + dy * dy + dz * dz <= 1.0;
    }
}
