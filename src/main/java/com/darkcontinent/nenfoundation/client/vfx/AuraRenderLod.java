package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Quanto da aura se desenha, conforme a distancia.
 *
 * <p>ESTA TABELA ERA UMA DIVERGENCIA DECLARADA. Havia duas no repositorio: este
 * enum cortava em 8/20/40 com quatro niveis, e
 * {@code docs/vfx/arquitetura-do-render-de-aura.md} definia cinco niveis em
 * 12/24/48/72. Duas fontes para a mesma verdade, registradas em
 * {@code o-que-nao-provamos.md} como pendencia do AV3. <b>Esta classe e a
 * reconciliacao</b>, e o documento passa a apontar para ela.
 *
 * <p>A ESCOLHA FOI PELA TABELA DO DOCUMENTO, e nao pela do codigo, por um motivo
 * concreto: cortar a aura completa a oito blocos e perto demais. Oito blocos e a
 * distancia de uma briga corpo a corpo -- exatamente onde o jogador PRECISA ler
 * o estado de Nen do adversario.
 *
 * <p>O NIVEL NAO CORTA FEATURES DE FORMA ARBITRARIA: ele reduz na ordem da
 * hierarquia de leitura. Primeiro somem os filamentos, depois o halo externo,
 * depois o filme interno. <b>A borda e a ULTIMA a sair</b>, porque e ela que
 * carrega a leitura -- "aquela pessoa esta em Ren" precisa continuar legivel a
 * quarenta blocos.
 */
public enum AuraRenderLod {

    /** Perto: tudo. */
    FULL(1.0F, 1.0F, true, true, true),

    /** Ainda proximo: a shell inteira, com menos filamentos. */
    NEAR(0.85F, 0.60F, true, true, true),

    /** Medio: o halo externo sai, e sobram poucos filamentos. */
    MEDIUM(0.60F, 0.25F, true, true, false),

    /** Longe: so a borda. E ela que diz qual e o estado. */
    FAR(0.35F, 0.0F, false, true, false),

    /** Fora de alcance: nada, e custo zero. */
    HIDDEN(0.0F, 0.0F, false, false, false);

    /**
     * Onde cada faixa termina, em blocos.
     *
     * <p>Tem exatamente um corte a menos que o numero de niveis, e ha teste
     * fixando isso: um nivel novo sem corte novo ficaria inalcancavel, e nada
     * acusaria.
     */
    private static final double[] CORTES = {12.0D, 24.0D, 48.0D, 72.0D};

    private final float intensidade;
    private final float fracaoDeFilamentos;
    private final boolean filmeInterno;
    private final boolean borda;
    private final boolean haloExterno;

    AuraRenderLod(float intensidade, float fracaoDeFilamentos, boolean filmeInterno,
            boolean borda, boolean haloExterno) {
        this.intensidade = intensidade;
        this.fracaoDeFilamentos = fracaoDeFilamentos;
        this.filmeInterno = filmeInterno;
        this.borda = borda;
        this.haloExterno = haloExterno;
    }

    /**
     * O nivel para uma distancia, em blocos.
     *
     * @throws IllegalArgumentException se a distancia nao for finita ou for negativa
     */
    public static AuraRenderLod porDistancia(double distancia) {
        if (!Double.isFinite(distancia) || distancia < 0.0D) {
            // NaN PRECISA SER BARRADO DE PROPOSITO: toda comparacao com NaN e
            // falsa, entao ele atravessaria o laco inteiro e cairia em HIDDEN.
            // A aura sumiria, e nada acusaria o motivo.
            throw new IllegalArgumentException("distancia invalida: " + distancia);
        }
        for (int i = 0; i < CORTES.length; i++) {
            if (distancia < CORTES[i]) {
                return values()[i];
            }
        }
        return HIDDEN;
    }

    /** Quanto da aura se mostra neste nivel, de 0 a 1. */
    public float intensidade() {
        return this.intensidade;
    }

    /** Que fracao dos filamentos sobrevive neste nivel. */
    public float fracaoDeFilamentos() {
        return this.fracaoDeFilamentos;
    }

    /** Se este nivel desenha alguma coisa. */
    public boolean visivel() {
        return this != HIDDEN;
    }

    /** Se a camada indicada e desenhada neste nivel. */
    public boolean desenha(Camada camada) {
        return switch (camada) {
            case INTERNA -> this.filmeInterno;
            case BORDA -> this.borda;
            case EXTERNA -> this.haloExterno;
        };
    }

    /**
     * As camadas da shell, nomeadas aqui de proposito.
     *
     * <p>O enum de verdade ({@code AuraShellPass}) vive no pacote de modelo, que
     * importa tipos do Minecraft. Se o LOD dependesse dele, nenhum teste puro
     * conseguiria ler esta tabela -- e ela e justamente a parte que da para
     * provar sem tela.
     */
    public enum Camada { INTERNA, BORDA, EXTERNA }
}
