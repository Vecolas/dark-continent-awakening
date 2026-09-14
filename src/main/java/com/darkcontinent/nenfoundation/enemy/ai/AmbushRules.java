package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * Regras do emboscador enterrado, sem mundo e sem entidade.
 *
 * <p>Carrega a decisao de que "quando a emboscada dispara" e "quando o bicho
 * volta a se enterrar" sao UMA fonte so, testavel sozinha. Espalhadas pela
 * Goal e pelo tick da entidade, cada condicao viraria um {@code if} diferente
 * e a divergencia entre elas nao daria erro nenhum: apareceria como um sapo
 * que dispara de longe demais, ou que nunca mais se enterra.</p>
 *
 * <p>Os numeros sao injetados pelo perfil de balanceamento; este record nao
 * conhece nenhum deles.</p>
 */
public record AmbushRules(double raioDeGatilho, double alturaDeGatilho, int ticksDeRecarga,
        int ticksParaReenterrar) {
    public AmbushRules {
        if (!Double.isFinite(raioDeGatilho) || !Double.isFinite(alturaDeGatilho)
                || raioDeGatilho <= 0.0D || alturaDeGatilho <= 0.0D
                || ticksDeRecarga < 0 || ticksParaReenterrar < 0) {
            throw new IllegalArgumentException("regras de emboscada invalidas");
        }
    }

    /**
     * So dispara com o bicho ENTERRADO, alvo valido, recarga zerada e o alvo
     * dentro do cilindro de gatilho.
     *
     * <p>A altura entra separada do raio de proposito: quem passa VOANDO ou
     * num andaime dois blocos acima nao esta pisando na zona, e engolir essa
     * pessoa nao seria emboscada, seria teleporte.</p>
     *
     * <p>Distancia ou diferenca de altura nao finita reprova: um NaN vindo de
     * um alvo em estado estranho nao pode virar uma emboscada "gratis", e
     * comparacao com NaN ja e falsa nas duas pontas.</p>
     */
    public boolean dispara(boolean enterrado, boolean alvoValido, double distanciaHorizontal,
            double diferencaDeAltura, int recargaRestante) {
        if (!Double.isFinite(distanciaHorizontal) || !Double.isFinite(diferencaDeAltura)) return false;
        return enterrado && alvoValido && recargaRestante == 0
                && distanciaHorizontal <= raioDeGatilho
                && Math.abs(diferencaDeAltura) <= alturaDeGatilho;
    }

    /**
     * So volta a se enterrar DESENTERRADO, sem alvo e depois da espera.
     *
     * <p>Reenterrar com alvo por perto devolveria a invisibilidade no meio da
     * briga -- e a janela de resposta que o plano exige some justamente quando
     * ela importa.</p>
     */
    public boolean reenterra(boolean enterrado, boolean temAlvo, int ticksSemAlvo) {
        return !enterrado && !temAlvo && ticksSemAlvo >= ticksParaReenterrar;
    }
}
