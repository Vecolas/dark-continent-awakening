package com.darkcontinent.nenfoundation.worldtree;

/** Perfil parametrico do tronco, sem colocacao de blocos. */
public record WorldTreeTrunkProfile(int baseY, int topY, double baseRadius, double topRadius) {
    public WorldTreeTrunkProfile {
        if (baseY < 0 || topY <= baseY || topY > 1536) {
            throw new IllegalArgumentException("faixa vertical do tronco invalida");
        }
        if (!Double.isFinite(baseRadius) || !Double.isFinite(topRadius)
                || baseRadius <= topRadius || topRadius <= 0.0) {
            throw new IllegalArgumentException("raios do tronco invalidos");
        }
    }

    public double radiusAt(int y) {
        double t = Math.max(0.0, Math.min(1.0, (double) (y - baseY) / (topY - baseY)));
        return baseRadius + (topRadius - baseRadius) * t;
    }
}
