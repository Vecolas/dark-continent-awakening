package com.darkcontinent.nenfoundation.client.vfx;

/** Qualidade visual local; nao muda custo, output ou visibilidade autoritativa. */
public enum AuraVisualQuality {
    OFF, LOW, MEDIUM, HIGH;

    public AuraRenderLod limitar(AuraRenderLod distancia) {
        if (distancia == null) throw new NullPointerException("distancia");
        return switch (this) {
            case OFF -> AuraRenderLod.HIDDEN;
            case LOW -> distancia == AuraRenderLod.FULL ? AuraRenderLod.SHELL : distancia;
            case MEDIUM -> distancia == AuraRenderLod.FULL ? AuraRenderLod.FULL : distancia;
            case HIGH -> distancia;
        };
    }
}
