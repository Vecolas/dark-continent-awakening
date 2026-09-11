package com.darkcontinent.nenfoundation.nen.aura;

/**
 * Reserva de aura autoritativa de uma unica sessao de jogador.
 *
 * <p>As invariantes ficam nesta fronteira para que nenhuma tecnica precise
 * repetir clamp ou validacao: maxima e atual sao sempre finitas, nao negativas
 * e atual nunca passa da maxima. Exaustao e derivada do valor atual, portanto
 * nao existe um segundo booleano capaz de ficar dessincronizado.
 */
public final class AuraPool {

    private double maxima;
    private double atual;

    /** Cria uma reserva cheia explicitamente; a sessao normal nasce neutra. */
    public AuraPool(double maxima) {
        validarNumero(maxima, "maxima");
        if (maxima < 0.0D) {
            throw new IllegalArgumentException("maxima nao pode ser negativa");
        }
        this.maxima = maxima;
        this.atual = maxima;
    }

    /** Cria uma reserva neutra, usada antes de a formula definir a maxima. */
    public AuraPool() {
        this(0.0D);
    }

    public double maxima() {
        return this.maxima;
    }

    public double atual() {
        return this.atual;
    }

    /** Aura zerada: a tecnica nao deve iniciar nem continuar neste estado. */
    public boolean exausto() {
        return this.atual == 0.0D;
    }

    /**
     * Troca a maxima sem criar aura acima dela. O valor atual e reduzido apenas
     * quando necessario; aumentar a maxima nao concede aura automaticamente.
     */
    public void definirMaxima(double maxima) {
        validarNumero(maxima, "maxima");
        if (maxima < 0.0D) {
            throw new IllegalArgumentException("maxima nao pode ser negativa");
        }
        this.maxima = maxima;
        this.atual = Math.min(this.atual, maxima);
    }

    /** Define a aura atual somente dentro dos limites do pool. */
    public void definirAtual(double atual) {
        validarNumero(atual, "atual");
        if (atual < 0.0D || atual > this.maxima) {
            throw new IllegalArgumentException("atual deve estar entre zero e a maxima");
        }
        this.atual = atual;
    }

    /**
     * Debita tudo ou nada. Quantidade zero nao e um gasto e valores invalidos
     * sao recusados para nao transformar NaN em uma aura aparentemente valida.
     */
    public boolean gastar(double quantidade) {
        validarNumero(quantidade, "quantidade");
        if (quantidade <= 0.0D || quantidade > this.atual) {
            return false;
        }
        this.atual -= quantidade;
        return true;
    }

    /** Recupera aura sem ultrapassar a maxima. */
    public double recuperar(double quantidade) {
        validarNumero(quantidade, "quantidade");
        if (quantidade < 0.0D) {
            throw new IllegalArgumentException("quantidade nao pode ser negativa");
        }
        double antes = this.atual;
        // Soma so o espaco disponivel: duas entradas finitas podem somar infinito.
        this.atual += Math.min(this.maxima - this.atual, quantidade);
        return this.atual - antes;
    }

    private static void validarNumero(double valor, String nome) {
        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException(nome + " deve ser finito");
        }
    }
}
