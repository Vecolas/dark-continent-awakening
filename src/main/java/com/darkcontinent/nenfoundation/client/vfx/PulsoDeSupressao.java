package com.darkcontinent.nenfoundation.client.vfx;

/**
 * O retorno de INPUT de Zetsu -- e o unico brilho que Zetsu produz.
 *
 * <p><b>SO PARA O JOGADOR LOCAL, E ISSO E O CRITERIO INTEIRO.</b> Zetsu e
 * ausencia total para observadores: nenhum contorno, nenhum halo residual, nem
 * os 2% de cintilacao que o documento-fonte chamava de opcionalissimo. Num
 * servidor com dois clientes, brilho residual entrega justamente quem esta se
 * escondendo.
 *
 * <p>Mas quem APERTA a tecla precisa de alguma resposta -- sem ela, suprimir e
 * indistinguivel de o comando nao ter chegado, e o jogador aperta de novo. A
 * saida e um pulso que existe apenas na tela de quem o disparou, e que nao e
 * aura: ele nao passa pelo perfil, nao entra no alvo de brilho, nao e desenhado
 * no mundo e nao atravessa a rede. Nao ha por onde vazar.
 *
 * <p><b>ELE TERMINA SOZINHO, em no maximo 500 ms.</b> "Ligar e desligar Zetsu
 * vinte vezes seguidas nao deixa estado preso" e criterio de aceite, e a forma
 * de garantir isso e nao ter estado que dure: o pulso e um contador que so
 * diminui.
 *
 * <p>A ARITMETICA E PURA, sem nenhum tipo de Minecraft -- o teste fixa o pico e
 * o termino exato em zero sem precisar de tela. Quem desenha le
 * {@link #intensidade(float)}.
 */
public final class PulsoDeSupressao {

    /**
     * Quanto o pulso dura, em ticks.
     *
     * <p>DEZ TICKS -- 500 ms, o teto que a issue #199 estabelece. Mais que isso
     * deixa de ser retorno de input e vira um estado visual de Zetsu, que e
     * exatamente o que Zetsu nao pode ter.
     */
    public static final int DURACAO_EM_TICKS = 10;

    /**
     * O pico, de 0 a 1.
     *
     * <p>DISCRETO DE PROPOSITO. Um flash forte na propria tela ao suprimir seria
     * lido como dano recebido -- e a ultima coisa que Zetsu deveria comunicar e
     * "algo me acertou".
     */
    public static final float PICO = 0.22F;

    private static float restante;

    private PulsoDeSupressao() {
    }

    /** Dispara o pulso. Chamado na borda para ZETSU da sessao LOCAL. */
    public static void disparar() {
        restante = DURACAO_EM_TICKS;
    }

    /** Avanca o decaimento um tick. */
    public static void aoTick() {
        if (restante > 0.0F) {
            restante = Math.max(0.0F, restante - 1.0F);
        }
    }

    /** Se ha pulso em curso. */
    public static boolean ativo() {
        return restante > 0.0F;
    }

    /** Quanto resta, em ticks. Regua do overlay de dev. */
    public static float restante() {
        return restante;
    }

    /** Quem liga, desliga: o pulso nao sobrevive ao logout nem a morte. */
    public static void limpar() {
        restante = 0.0F;
    }

    /**
     * A intensidade do pulso agora, de 0 a {@link #PICO}.
     *
     * <p>ENTRADA RAPIDA E SAIDA LONGA: o pulso aparece quase instantaneamente e
     * se dissipa. Um envelope simetrico le como "algo acendeu e apagou"; este le
     * como "algo fechou", que e o gesto de Zetsu.
     *
     * <p>PURA, e por isso provavel sem o jogo.
     */
    public static float intensidade(float restanteEmTicks) {
        if (!Float.isFinite(restanteEmTicks) || restanteEmTicks <= 0.0F) {
            return 0.0F;
        }
        float fracao = Math.clamp(restanteEmTicks / DURACAO_EM_TICKS, 0.0F, 1.0F);
        // `fracao` vale 1 no instante do input e cai a 0 no fim. O quadrado
        // encurta a cauda: o pulso passa a maior parte da vida ja fraco.
        return PICO * fracao * fracao;
    }

    /** A intensidade agora. */
    public static float intensidade() {
        return intensidade(restante);
    }
}
