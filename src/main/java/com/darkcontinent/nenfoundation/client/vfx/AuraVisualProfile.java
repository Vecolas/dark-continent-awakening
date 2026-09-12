package com.darkcontinent.nenfoundation.client.vfx;

/** Perfil de personalidade visual, separado de regras e afinidade de Nen. */
public record AuraVisualProfile(float flowSharpness, float flowSpeed, float edgeStrength,
        float pulseFrequency, float particleRate, float distortion, int primaryColor,
        int secondaryColor) {
    public AuraVisualProfile {
        validar(flowSharpness, "flowSharpness");
        validar(flowSpeed, "flowSpeed");
        validar(edgeStrength, "edgeStrength");
        validar(pulseFrequency, "pulseFrequency");
        validar(particleRate, "particleRate");
        validar(distortion, "distortion");
    }

    public static AuraVisualProfile controlada() {
        return new AuraVisualProfile(0.25F, 0.35F, 0.35F, 0.30F, 0.05F, 0.05F,
                0xFFFFFFFF, 0xFFDDEBFF);
    }

    public static AuraVisualProfile agressiva() {
        return new AuraVisualProfile(0.70F, 0.80F, 0.75F, 0.70F, 0.20F, 0.25F,
                0xFFFFFFFF, 0xFFE8F4FF);
    }

    private static void validar(float valor, String campo) {
        if (!Float.isFinite(valor) || valor < 0.0F || valor > 1.0F) {
            throw new IllegalArgumentException(campo + " deve estar entre 0 e 1");
        }
    }
}
