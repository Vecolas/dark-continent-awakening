package com.darkcontinent.nenfoundation.client.vfx;

/** Parametros visuais imutaveis; tuning futuro pode vir de config client-side. */
public record AuraVisualPreset(float shellScale, float shellOpacity, float edgeIntensity,
        float flowIntensity, float particleIntensity, float pulseAmplitude, float pulseFrequency) {
    public AuraVisualPreset {
        validar(shellScale, "shellScale", 0.0F, 2.0F);
        validar(shellOpacity, "shellOpacity", 0.0F, 1.0F);
        validar(edgeIntensity, "edgeIntensity", 0.0F, 1.0F);
        validar(flowIntensity, "flowIntensity", 0.0F, 1.0F);
        validar(particleIntensity, "particleIntensity", 0.0F, 1.0F);
        validar(pulseAmplitude, "pulseAmplitude", 0.0F, 1.0F);
        validar(pulseFrequency, "pulseFrequency", 0.0F, Float.POSITIVE_INFINITY);
    }

    public static AuraVisualPreset tenBasic() {
        return new AuraVisualPreset(1.035F, 0.18F, 0.35F, 0.18F, 0.03F, 0.04F, 0.35F);
    }

    public static AuraVisualPreset renBasic() {
        return new AuraVisualPreset(1.065F, 0.30F, 0.70F, 0.75F, 0.16F, 0.18F, 0.70F);
    }

    public static AuraVisualPreset zetsu() {
        return new AuraVisualPreset(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    public static AuraVisualPreset off() { return zetsu(); }

    private static void validar(float valor, String nome, float minimo, float maximo) {
        if (!Float.isFinite(valor) || valor < minimo || valor > maximo) {
            throw new IllegalArgumentException(nome + " fora do intervalo visual permitido");
        }
    }
}
