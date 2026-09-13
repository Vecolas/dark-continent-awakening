package com.darkcontinent.nenfoundation.worldtree;

/** Marco nomeado que pode ser consumido por geradores e progressao depois. */
public record WorldTreeLandmark(String id, int y, double x, double z) {
    public WorldTreeLandmark {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("landmark sem id");
        }
        if (y < 0 || y > 1535 || !Double.isFinite(x) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("posicao de landmark invalida");
        }
    }
}
