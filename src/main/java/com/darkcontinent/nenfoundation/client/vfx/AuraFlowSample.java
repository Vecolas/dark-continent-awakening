package com.darkcontinent.nenfoundation.client.vfx;

/** Amostra deterministica de um filamento, sem criar entidade ou particula. */
public record AuraFlowSample(float x, float y, float z, float amplitude, float phase) {
    public AuraFlowSample {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
                || !Float.isFinite(amplitude) || !Float.isFinite(phase) || amplitude < 0.0F) {
            throw new IllegalArgumentException("amostra de fluxo invalida");
        }
    }
}
