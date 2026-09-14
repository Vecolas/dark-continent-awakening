package com.darkcontinent.nenfoundation.enemy.perception;

/**
 * Orcamento de percepcao: de quantos em quantos ticks cada varredura roda.
 *
 * <p><b>O numero existe porque o erro que ele evita nao da erro.</b> Um mob que
 * consulta o mundo todo tick funciona perfeitamente com um mob na tela. Com
 * quarenta, o TPS cai devagar ao longo de uma sessao e ninguem consegue apontar
 * a causa -- nao ha excecao, nao ha log, so um servidor que ficou lento. Por
 * isso o intervalo e um CONTRATO, e nao uma boa intencao: {@link
 * PerceptionController} so chama o sensor caro quando este record autoriza.</p>
 *
 * <p>As faixas vem da issue #136: percepcao normal entre 5 e 10 ticks, scan caro
 * entre 20 e 40. Elas sao validadas aqui em vez de serem comentario, porque
 * comentario nao reprova nada -- e um 1 escrito por engano passaria calado.</p>
 */
public record PerceptionBudget(int ticksDeVisao, int ticksDeScanCaro) {

    /** Limites da issue #136; escritos uma vez e cobrados no construtor. */
    public static final int VISAO_MINIMA = 5;
    public static final int VISAO_MAXIMA = 10;
    public static final int SCAN_MINIMO = 20;
    public static final int SCAN_MAXIMO = 40;

    public PerceptionBudget {
        if (ticksDeVisao < VISAO_MINIMA || ticksDeVisao > VISAO_MAXIMA) {
            throw new IllegalArgumentException("visao fora do orcamento (" + VISAO_MINIMA + ".."
                    + VISAO_MAXIMA + " ticks): " + ticksDeVisao);
        }
        if (ticksDeScanCaro < SCAN_MINIMO || ticksDeScanCaro > SCAN_MAXIMO) {
            throw new IllegalArgumentException("scan caro fora do orcamento (" + SCAN_MINIMO + ".."
                    + SCAN_MAXIMO + " ticks): " + ticksDeScanCaro);
        }
        if (ticksDeScanCaro <= ticksDeVisao) {
            throw new IllegalArgumentException("scan caro tem de ser MAIS raro que a visao;"
                    + " invertido ele deixa de ser caro e o orcamento vira enfeite");
        }
    }

    /** Orcamento padrao: visao a cada 6 ticks, scan caro a cada 30. */
    public static PerceptionBudget padrao() {
        return new PerceptionBudget(6, 30);
    }

    /**
     * Desfasa o mob pelo proprio id para a manada inteira nao varrer no mesmo tick.
     *
     * <p>Sem isto, quarenta mobs com o mesmo intervalo varrem TODOS no tick
     * multiplo de 6 e o custo se concentra num pico em vez de se espalhar. O
     * sintoma seria travada periodica, nao lentidao constante -- pior de
     * diagnosticar, porque parece rede.</p>
     */
    public boolean visaoNesteTick(int tickDoMundo, int desfasagem) {
        return Math.floorMod(tickDoMundo + desfasagem, ticksDeVisao) == 0;
    }

    public boolean scanCaroNesteTick(int tickDoMundo, int desfasagem) {
        return Math.floorMod(tickDoMundo + desfasagem, ticksDeScanCaro) == 0;
    }
}
