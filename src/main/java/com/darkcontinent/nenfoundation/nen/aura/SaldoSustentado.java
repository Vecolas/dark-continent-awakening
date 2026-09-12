package com.darkcontinent.nenfoundation.nen.aura;

/**
 * O saldo de aura de um estado que fica LIGADO.
 *
 * <p>O ITEM 6 DO ADR-010 EM CODIGO: usar Nen custa aura, e nenhum estado
 * sustentado se paga. O saldo por segundo de qualquer tecnica que se mantenha
 * ativa precisa ser <b>negativo em absoluto</b> -- a reserva tem de CAIR
 * enquanto ela estiver ligada.
 *
 * <p>POR QUE ISTO VIROU UMA CLASSE. Este projeto ja errou a conta uma vez: o
 * portao comparava o GANHO SOBRE A BASE com o custo, em vez da regeneracao
 * TOTAL. Com regeneracao 1.0, multiplicador 2.0 e custo 1.5, o ganho sobre a
 * base e 1.0 -- menor que 1.5, e o teste passava. O saldo real e
 * {@code 1.0 x 2.0 - 1.5 = +0.5}: a aura SOBE com a tecnica ligada, e ela vira
 * um estado permanente de graca.
 *
 * <p>Foi encontrado jogando, e nao por teste. A conta mora aqui para nao ser
 * refeita de cabeca a cada consumidor novo.
 */
public final class SaldoSustentado {

    private SaldoSustentado() {
    }

    /**
     * Quanto de aura por segundo a reserva ganha ({@code > 0}) ou perde
     * ({@code < 0}) com a tecnica ligada.
     *
     * <p>A REGENERACAO ENTRA MULTIPLICADA, e nao o ganho que o multiplicador
     * acrescenta. E essa a diferenca entre a conta certa e a que ja passou
     * batida.
     */
    public static double porSegundo(double regeneracaoBase, double multiplicador,
            double custoPorSegundo) {
        return regeneracaoBase * multiplicador - custoPorSegundo;
    }

    /** Se o estado se paga -- ou seja, se ele viola o item 6 do ADR-010. */
    public static boolean sePaga(double regeneracaoBase, double multiplicador,
            double custoPorSegundo) {
        double saldo = porSegundo(regeneracaoBase, multiplicador, custoPorSegundo);
        // ZERO TAMBEM SE PAGA. Um estado de saldo exatamente neutro nao custa
        // nada para manter: nao ha motivo para desliga-lo, e ele vira o padrao
        // silencioso. "Negativo em absoluto" nao admite o empate.
        return !(saldo < 0.0D);
    }

    /**
     * O custo minimo para o estado nao se pagar.
     *
     * <p>Existe para a mensagem de erro poder dizer o que fazer, e nao so que
     * esta errado. Recusa sem instrucao gera a mesma pergunta toda vez.
     */
    public static double custoMinimo(double regeneracaoBase, double multiplicador) {
        return regeneracaoBase * multiplicador;
    }
}
