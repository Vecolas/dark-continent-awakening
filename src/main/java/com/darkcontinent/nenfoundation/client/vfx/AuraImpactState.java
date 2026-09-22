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
public record AuraImpactState(
        com.darkcontinent.nenfoundation.nen.combat.FaixaDoCorpo faixa,
        int remainingTicks, float strength) {

    /** Quanto tempo o ripple vive. Ver o javadoc da classe. */
    public static final int DURACAO_EM_TICKS = 12;

    public AuraImpactState {
        if (faixa == null || remainingTicks < 0 || !Float.isFinite(strength)
                || strength < 0.0F || strength > 1.0F) {
            throw new IllegalArgumentException("impacto visual invalido");
        }
    }

    public static AuraImpactState iniciar(
            com.darkcontinent.nenfoundation.nen.combat.FaixaDoCorpo faixa, float strength) {
        return new AuraImpactState(faixa, DURACAO_EM_TICKS, strength);
    }

    public AuraImpactState avancar() {
        return new AuraImpactState(faixa, Math.max(0, remainingTicks - 1), strength);
    }

    /**
     * Fracao de PLATO: quanto do tempo o ripple fica no pico antes de cair.
     *
     * <p>Um terco. Sem plato o efeito decaia desde o PRIMEIRO quadro, entao o
     * pico durava um quadro -- e um pico de um quadro nao e um flash, e um
     * cintilar que o olho descarta como ruido. Com o plato, o impacto SEGURA e
     * so entao some, que e como uma pancada le.
     */
    private static final float PLATO = 1.0F / 3.0F;

    /**
     * A curva do ripple: PLATO no pico, depois queda linear.
     *
     * <p>Devolve 1.0 enquanto resta mais de {@link #PLATO} do tempo, e cai
     * proporcionalmente depois disso.
     */
    public float progresso() {
        float restante = remainingTicks / (float) DURACAO_EM_TICKS;
        return restante >= 1.0F - PLATO ? 1.0F : restante / (1.0F - PLATO);
    }

    public boolean ativo() { return remainingTicks > 0 && strength > 0.0F; }
}
