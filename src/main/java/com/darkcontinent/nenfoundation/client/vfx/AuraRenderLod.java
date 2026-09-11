package com.darkcontinent.nenfoundation.client.vfx;

/** Niveis de detalhe definidos por distancia; a renderer decide o que desenhar. */
public enum AuraRenderLod {
    FULL, SHELL, SIMPLIFIED, HIDDEN;

    public static AuraRenderLod porDistancia(double distancia) {
        if (!Double.isFinite(distancia) || distancia < 0.0D) {
            throw new IllegalArgumentException("distancia invalida");
        }
        if (distancia < 8.0D) return FULL;
        if (distancia < 20.0D) return SHELL;
        if (distancia <= 40.0D) return SIMPLIFIED;
        return HIDDEN;
    }
}
