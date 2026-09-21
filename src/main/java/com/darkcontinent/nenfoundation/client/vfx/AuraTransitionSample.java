package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Quanto de cada componente da aura esta em cena NESTE instante da transicao.
 *
 * <p>PESO, E NAO NUMERO DE ARTE. Espessura, alpha, contagem de filamento e raio
 * de anel moram no perfil ({@code assets/nenfoundation/nen_vfx/*.json}); o que
 * mora aqui e o quanto de cada um ja chegou. A separacao importa: sem ela, a
 * transicao precisaria conhecer os numeros de arte, e girar um numero na sessao
 * de arte passaria a exigir mexer na maquina de estados.
 *
 * <p>O COMPONENTE E A UNIDADE, e nao o modo. A direcao de arte pede que, ao
 * subir para Ren, a shell CONTRAIA enquanto a borda ainda nem comecou a crescer,
 * que as colunas entrem antes da pressao de chao, e que na volta a pressao caia
 * primeiro. Um progresso unico de 0 a 1 nao consegue dizer nada disso -- ele
 * produz uma rampa, e rampa e lida como interpolacao, nunca como liberacao.
 *
 * <p><b>{@link #borda()} PODE PASSAR DE 1, e isso e o efeito e nao o defeito.</b>
 * A ultrapassagem de 10 a 15% antes de assentar e o detalhe que faz Ren parecer
 * um golpe sendo dado. Quem consome precisa aguentar o valor acima de um -- e e
 * por isso que este record NAO valida contra 1, so contra
 * {@link #TETO_DE_ULTRAPASSAGEM}.
 *
 * <p>SEM TIPO DE MINECRAFT: a tabela inteira se prova sem subir o jogo.
 *
 * @param shell      o filme interno; abaixo de 1 a aura esta CONTRAINDO
 * @param borda      o contorno; passa de 1 na ultrapassagem
 * @param filamentos presenca dos filamentos de corpo
 * @param colunas    presenca das correntes verticais de Ren
 * @param pressao    presenca do anel de chao e dos detritos
 * @param flash      o estouro curto de borda, so no instante da liberacao
 */
public record AuraTransitionSample(float shell, float borda, float filamentos,
        float colunas, float pressao, float flash) {

    /**
     * O maior valor que qualquer peso pode assumir.
     *
     * <p>CONSTANTE DE DESENHO. A direcao de arte pede 10 a 15% de ultrapassagem;
     * este teto deixa folga para a curva e recusa qualquer tabela que tente
     * dobrar a borda -- o que nao seria "Ren forte", seria outro efeito.
     */
    public static final float TETO_DE_ULTRAPASSAGEM = 1.30F;

    /** Tudo em cena, sem ultrapassagem. E o estado assentado de Ren. */
    public static final AuraTransitionSample CHEIO =
            new AuraTransitionSample(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F);

    /** Shell, borda e filamentos em cena; nada de pressao. O estado assentado de Ten. */
    public static final AuraTransitionSample SEM_PRESSAO =
            new AuraTransitionSample(1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F);

    /** Nada. O estado assentado de Zetsu e de OFF -- a ausencia e a informacao. */
    public static final AuraTransitionSample VAZIO =
            new AuraTransitionSample(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    public AuraTransitionSample {
        peso(shell, "shell");
        peso(borda, "borda");
        peso(filamentos, "filamentos");
        peso(colunas, "colunas");
        peso(pressao, "pressao");
        peso(flash, "flash");
    }

    /**
     * O estado ASSENTADO de um modo -- o que se ve quando nada esta mudando.
     *
     * <p>ELE E A RESPOSTA PARA QUEM NAO TEM TRANSICAO: os outros jogadores
     * chegam como sinal derivado, sem interpolador proprio (custo declarado em
     * {@link EstadoVisualDeTerceiro}), e precisam de um peso valido mesmo assim.
     * Sem isto, a aura de terceiros desenharia com peso zero e sumiria -- e o
     * sintoma seria "so a minha aura aparece".
     */
    public static AuraTransitionSample assentado(AuraVisualMode modo) {
        if (modo == null) {
            return VAZIO;
        }
        return switch (modo) {
            case OFF, ZETSU -> VAZIO;
            // REN e o unico modo com pressao de chao e colunas. CUSTOM herda o
            // conjunto completo de proposito: uma tecnica nova que nao souber
            // dizer o que quer aparece INTEIRA, que e visivel e portanto
            // corrigivel -- o contrario apareceria como "a tecnica nova nao
            // desenha nada", que e o pior relato de bug que existe.
            case REN, CUSTOM -> CHEIO;
            case TEN -> SEM_PRESSAO;
        };
    }

    /** O mesmo conjunto multiplicado por um fator -- o LOD e a intensidade entram assim. */
    public AuraTransitionSample escalado(float fator) {
        if (!Float.isFinite(fator) || fator < 0.0F) {
            throw new IllegalArgumentException("fator invalido: " + fator);
        }
        return new AuraTransitionSample(
                limitar(this.shell * fator), limitar(this.borda * fator),
                limitar(this.filamentos * fator), limitar(this.colunas * fator),
                limitar(this.pressao * fator), limitar(this.flash * fator));
    }

    /** Se nada deste conjunto chega a desenhar. */
    public boolean vazio() {
        return this.shell <= 0.0F && this.borda <= 0.0F && this.filamentos <= 0.0F
                && this.colunas <= 0.0F && this.pressao <= 0.0F && this.flash <= 0.0F;
    }

    private static float limitar(float valor) {
        return Math.clamp(valor, 0.0F, TETO_DE_ULTRAPASSAGEM);
    }

    private static void peso(float valor, String nome) {
        if (!Float.isFinite(valor) || valor < 0.0F || valor > TETO_DE_ULTRAPASSAGEM) {
            throw new IllegalArgumentException(nome + " deve estar entre 0 e "
                    + TETO_DE_ULTRAPASSAGEM + ": " + valor);
        }
    }
}
