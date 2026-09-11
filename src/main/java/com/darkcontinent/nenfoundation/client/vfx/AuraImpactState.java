package com.darkcontinent.nenfoundation.client.vfx;

/** Ripple localizado de curta duracao; nao altera dano nem defesa. */
public record AuraImpactState(AuraBodyRegion region, int remainingTicks, float strength) {
    public AuraImpactState {
        if (region == null || remainingTicks < 0 || !Float.isFinite(strength)
                || strength < 0.0F || strength > 1.0F) {
            throw new IllegalArgumentException("impacto visual invalido");
        }
    }

    public static AuraImpactState iniciar(AuraBodyRegion region, float strength) {
        return new AuraImpactState(region, 8, strength);
    }

    public AuraImpactState avancar() {
        return new AuraImpactState(region, Math.max(0, remainingTicks - 1), strength);
    }

    public float progresso() { return remainingTicks / 8.0F; }
    public boolean ativo() { return remainingTicks > 0 && strength > 0.0F; }
}
