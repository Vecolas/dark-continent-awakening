package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Quanto tempo cada troca de estado leva, e com que curva.
 *
 * <p>CADA TROCA TEM O PROPRIO TEMPO, e essa e a razao desta classe existir.
 * Antes havia um numero so -- {@code vfx.ticksDeTransicao} -- valendo para
 * tudo, e com ele ligar Ten e explodir em Ren levavam exatamente o mesmo tempo.
 * O resultado nao e um bug: e uma aura em que nenhuma troca tem PESO.
 *
 * <p>AS DURACOES SAIRAM DA DIRECAO DE ARTE, e estao em
 * {@code docs/vfx/perfis-visuais.md}: ligar em ~350 ms, subir para Ren em
 * ~900 ms, voltar em ~700 ms, suprimir entre 200 e 500 ms. Vinte ticks e um
 * segundo.
 *
 * <p>SUPRIMIR E A MAIS RAPIDA, de proposito. Zetsu e uma decisao de sumir, e uma
 * supressao lenta e uma supressao que nao salva ninguem.
 *
 * <p>SUBIR PARA REN E A MAIS LENTA. A direcao de arte pede que a BORDA
 * ultrapasse o alvo antes de assentar -- o detalhe que faz Ren parecer
 * LIBERACAO em vez de interpolacao --, mas essa ultrapassagem pertence ao
 * material da borda, e nao ao progresso da transicao. Ver o javadoc de
 * {@link #ELEVAR}.
 */
public enum AuraTransicao {

    /** Do nada para Ten. Sem explosao: a pelicula so aparece. */
    LIGAR(AuraVisualMode.OFF, AuraVisualMode.TEN, 7, Curva.SUAVE),

    /**
     * Ten para Ren. A mais lenta da tabela.
     *
     * <p>ELA NAO USA {@code OVERSHOOT}, e a razao e um defeito que este PR
     * cometeu e desfez. A direcao de arte pede que a BORDA ultrapasse o alvo em
     * 10-15% antes de assentar -- e eu apliquei a ultrapassagem a interpolacao
     * INTEIRA. O efeito colateral: a intensidade geral batia no maximo na
     * METADE da transicao, porque a curva passa de 1 ali. Nada lancava; Ren so
     * chegava cedo demais.
     *
     * <p>A ultrapassagem pertence ao MATERIAL da borda, e nao ao progresso da
     * transicao. Enquanto esse lugar nao existir, {@code SAIDA_SUAVE} entrega
     * uma subida honesta. {@code OVERSHOOT} continua na tabela de curvas,
     * testada e sem consumidor -- declarado, e nao esquecido.
     */
    ELEVAR(AuraVisualMode.TEN, AuraVisualMode.REN, 18, Curva.SAIDA_SUAVE),

    /** Ren para Ten. Mais suave que a subida: a pressao cai antes do resto. */
    BAIXAR(AuraVisualMode.REN, AuraVisualMode.TEN, 14, Curva.SAIDA_SUAVE),

    /** Qualquer coisa para Zetsu. A mais rapida que existe. */
    SUPRIMIR(null, AuraVisualMode.ZETSU, 6, Curva.ENTRADA_RAPIDA),

    /** Qualquer coisa para nada. */
    DESLIGAR(null, AuraVisualMode.OFF, 6, Curva.SAIDA_SUAVE),

    /**
     * O que sobrar.
     *
     * <p>EXISTE PARA QUE UMA TECNICA NOVA NAO PRECISE MEXER AQUI para funcionar.
     * Ela ganha uma troca razoavel de graca, e so entra na tabela quando alguem
     * decidir que ela merece tempo proprio.
     */
    PADRAO(null, null, 10, Curva.SUAVE);

    private final AuraVisualMode origem;
    private final AuraVisualMode destino;
    private final int ticks;
    private final Curva curva;

    AuraTransicao(AuraVisualMode origem, AuraVisualMode destino, int ticks, Curva curva) {
        this.origem = origem;
        this.destino = destino;
        this.ticks = ticks;
        this.curva = curva;
    }

    /**
     * A transicao de um par.
     *
     * <p>A ORDEM DA TABELA E A PRECEDENCIA: o primeiro casamento vence. Por isso
     * as entradas com origem definida vem ANTES das curingas -- senao
     * {@code TEN -> ZETSU} cairia em {@code SUPRIMIR} ou em {@code ELEVAR}
     * conforme a ordem de declaracao, e a diferenca so apareceria em jogo.
     */
    public static AuraTransicao de(AuraVisualMode origem, AuraVisualMode destino) {
        if (destino == null) {
            return PADRAO;
        }
        for (AuraTransicao t : values()) {
            if (t == PADRAO) {
                continue;
            }
            boolean origemCasa = t.origem == null || t.origem == origem;
            boolean destinoCasa = t.destino == destino;
            if (origemCasa && destinoCasa) {
                return t;
            }
        }
        return PADRAO;
    }

    /** Quanto a transicao avanca por tick, de 0 a 1, com a escala do jogador. */
    public float passoPorTick(float escala) {
        if (!Float.isFinite(escala) || escala <= 0.0F) {
            // Escala invalida NAO pode congelar a aura no primeiro quadro da
            // animacao. Cai para a escala neutra.
            escala = 1.0F;
        }
        return 1.0F / Math.max(1.0F, this.ticks * escala);
    }

    public int ticks() {
        return this.ticks;
    }

    public Curva curva() {
        return this.curva;
    }

    /** Como o tempo vira progresso. */
    public enum Curva {

        /** Progresso cru. Util para teste, raramente para arte. */
        LINEAR,

        /** Comeca e termina devagar. O padrao. */
        SUAVE,

        /** Sai rapido e assenta. Bom para o que precisa SUMIR. */
        SAIDA_SUAVE,

        /** Entra rapido. Bom para o que precisa APARECER e ja estar la. */
        ENTRADA_RAPIDA,

        /**
         * Ultrapassa o alvo e volta.
         *
         * <p>E o que faz Ren parecer liberacao. O valor PASSA de 1 no meio do
         * caminho -- de proposito --, e quem consome precisa aguentar isso: a
         * borda fica 10-15% mais forte antes de assentar.
         */
        OVERSHOOT;

        /** O progresso, dado o tempo normalizado. */
        public float aplicar(float t) {
            float x = Math.clamp(t, 0.0F, 1.0F);
            return switch (this) {
                case LINEAR -> x;
                case SUAVE -> x * x * (3.0F - 2.0F * x);
                case SAIDA_SUAVE -> 1.0F - (1.0F - x) * (1.0F - x);
                case ENTRADA_RAPIDA -> (float) Math.sqrt(x);
                case OVERSHOOT -> ultrapassar(x);
            };
        }

        /**
         * Ultrapassagem "back-out", com tensao calibrada em ~12%.
         *
         * <p>A CONTA TERMINA EXATAMENTE EM 1 quando {@code x == 1}, e ha teste
         * fixando isso. Uma curva de ultrapassagem que nao fecha deixa a aura
         * um pouco acima do alvo PARA SEMPRE -- e ninguem liga um brilho
         * levemente alto a uma formula de easing meses depois.
         */
        private static float ultrapassar(float x) {
            final float tensao = 1.70158F * 0.62F;
            float u = x - 1.0F;
            return 1.0F + u * u * ((tensao + 1.0F) * u + tensao);
        }
    }
}
