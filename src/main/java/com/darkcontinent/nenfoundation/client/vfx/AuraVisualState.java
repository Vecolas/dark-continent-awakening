package com.darkcontinent.nenfoundation.client.vfx;

/** Snapshot visual interpolavel. Nao contem aura autoritativa nem regras de combate. */
public record AuraVisualState(AuraVisualMode mode, AuraVisualPreset preset, float intensity,
        float transitionProgress, AuraDistribution distribution, int primaryColor,
        int secondaryColor) {
    public AuraVisualState {
        if (mode == null || preset == null || distribution == null) {
            throw new NullPointerException("modo, preset e distribuicao sao obrigatorios");
        }
        validar(intensity, "intensity");
        validar(transitionProgress, "transitionProgress");
    }

    public boolean enabled() { return mode != AuraVisualMode.OFF && intensity > 0.0F; }

    public static AuraVisualState desligado() {
        return new AuraVisualState(AuraVisualMode.OFF, AuraVisualPreset.off(), 0.0F, 1.0F,
                AuraDistribution.zetsu(), 0xFFFFFFFF, 0xFFFFFFFF);
    }

    private static void validar(float valor, String nome) {
        if (!Float.isFinite(valor) || valor < 0.0F || valor > 1.0F) {
            throw new IllegalArgumentException(nome + " deve estar entre 0 e 1");
        }
    }
}
