package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Ripple localizado de curta duracao; nao altera dano nem defesa.
 *
 * <p><b>A DURACAO E DOZE TICKS, e nao oito.</b> Oito davam 0,4 s, e o relato da
 * primeira sessao a ver o efeito foi <i>"talvez 8 ticks seja rapido demais para
 * ver diferenca"</i>. Doze dao 0,6 s -- ainda um flash, que e o que um impacto
 * deve ser, mas tempo suficiente para o olho registrar que algo aconteceu.
 * LIMITE DE DESENHO, e nao balanceamento: ele nao muda dano nem defesa.
 */
public record AuraImpactState(AuraBodyRegion region, int remainingTicks, float strength) {

    /** Quanto tempo o ripple vive. Ver o javadoc da classe. */
    public static final int DURACAO_EM_TICKS = 12;

    public AuraImpactState {
        if (region == null || remainingTicks < 0 || !Float.isFinite(strength)
                || strength < 0.0F || strength > 1.0F) {
            throw new IllegalArgumentException("impacto visual invalido");
        }
    }

    public static AuraImpactState iniciar(AuraBodyRegion region, float strength) {
        return new AuraImpactState(region, DURACAO_EM_TICKS, strength);
    }

    public AuraImpactState avancar() {
        return new AuraImpactState(region, Math.max(0, remainingTicks - 1), strength);
    }

    public float progresso() { return remainingTicks / (float) DURACAO_EM_TICKS; }

    public boolean ativo() { return remainingTicks > 0 && strength > 0.0F; }
}
