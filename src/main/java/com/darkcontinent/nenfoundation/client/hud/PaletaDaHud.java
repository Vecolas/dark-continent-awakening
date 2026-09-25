package com.darkcontinent.nenfoundation.client.hud;

/**
 * As cores da HUD, e as operacoes que o estado de Nen faz sobre elas.
 *
 * <p>FONTE UNICA, pela mesma razao de {@link AparenciaDeTecnica}: o verde da
 * vida escrito em dois renderers vira dois verdes no dia em que alguem ajustar
 * um deles, e ninguem percebe ate ver as duas barras juntas.
 *
 * <p>O CIANO DA AURA E O MESMO DE TEN. Nao e coincidencia nem economia: Ten e
 * a aura em repouso, e a barra de reserva e a mesma coisa medida. Duas ciano
 * ligeiramente diferentes na mesma tela leem como defeito de calibragem.
 *
 * <p>AS TRANSFORMACOES SAO FUNCOES PURAS sobre ARGB empacotado, e nao um
 * segundo conjunto de constantes por estado. Uma tabela "cor de Zetsu", "cor de
 * Ren", "cor de Ten" seria sete vezes o mesmo verde com sete nomes, e o dia em
 * que o verde mudasse ela mudaria em seis lugares e ficaria no setimo.
 */
public final class PaletaDaHud {

    /** Base do painel: grafite azulado, translucido o bastante para o mundo passar. */
    public static final int FUNDO = 0xE6_0C_12_17;

    /** A linha externa: cinza-claro frio, sem brilho proprio. */
    public static final int BORDA = 0xFF_9F_B4_BF;

    /** O acento tecnico, usado com parcimonia: so nos cantos e no trilho do chip. */
    public static final int ACENTO = 0xFF_4A_C8_F0;

    /** O trilho vazio das barras. Escuro, mas nunca preto puro. */
    public static final int TRILHO = 0xFF_16_1F_26;

    /** Vida: verde vivo, e deliberadamente NAO neon. */
    public static final int VIDA = 0xFF_57_C7_5E;

    /** Aura: o mesmo ciano de Ten. */
    public static final int AURA = 0xFF_4A_C8_F0;

    /** Fluxo: dourado suave. Ele so aparece em contexto, e nao deve competir. */
    public static final int FLUXO = 0xFF_E8_C8_60;

    /** Texto principal. */
    public static final int TEXTO = 0xFF_EA_F8_FF;

    /** Texto secundario: rotulos, que nao devem disputar com o valor. */
    public static final int TEXTO_FRACO = 0xFF_8E_A6_B2;

    /** Aura zerada: o unico vermelho da HUD, e por isso ele significa algo. */
    public static final int ALERTA = 0xFF_FF_77_77;

    private PaletaDaHud() {
    }

    /**
     * Clareia na direcao do branco.
     *
     * <p>Usada no pulso de Ren e no fio de brilho do topo das barras. O alpha
     * NAO entra na conta -- clarear uma cor translucida deveria deixa-la mais
     * clara, e nao mais opaca.
     */
    public static int clarear(int argb, float fator) {
        float f = Math.clamp(fator, 0.0F, 1.0F);
        return recompor(argb,
                canal -> Math.round(canal + (255 - canal) * f));
    }

    /** Escurece na direcao do preto. Usada no trilho derivado de cada barra. */
    public static int escurecer(int argb, float fator) {
        float f = Math.clamp(fator, 0.0F, 1.0F);
        return recompor(argb, canal -> Math.round(canal * (1.0F - f)));
    }

    /**
     * Puxa a cor para o cinza de mesma luminancia.
     *
     * <p>E ISTO QUE ZETSU FAZ COM A HUD, e nao escurecer: Zetsu nao e "menos
     * aura", e a aura deixando de ser emitida. Escurecer leria como reserva
     * baixa -- a informacao errada, e bem no estado em que o jogador mais
     * precisa saber que a reserva continua cheia.
     */
    public static int dessaturar(int argb, float fator) {
        float f = Math.clamp(fator, 0.0F, 1.0F);
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        // Coeficientes de luminancia perceptual. Uma media simples deixaria o
        // ciano cinza-claro demais e o verde cinza-escuro demais, e as duas
        // barras dessaturariam em ritmos diferentes.
        int cinza = Math.round(0.2126F * r + 0.7152F * g + 0.0722F * b);
        return (argb & 0xFF_00_00_00)
                | (Math.round(r + (cinza - r) * f) << 16)
                | (Math.round(g + (cinza - g) * f) << 8)
                | Math.round(b + (cinza - b) * f);
    }

    /** Troca o alpha, preservando a cor. */
    public static int comAlpha(int argb, float alpha) {
        int a = Math.round(255 * Math.clamp(alpha, 0.0F, 1.0F));
        return (a << 24) | (argb & 0x00_FF_FF_FF);
    }

    private static int recompor(int argb, java.util.function.IntUnaryOperator porCanal) {
        int r = Math.clamp(porCanal.applyAsInt((argb >> 16) & 0xFF), 0, 255);
        int g = Math.clamp(porCanal.applyAsInt((argb >> 8) & 0xFF), 0, 255);
        int b = Math.clamp(porCanal.applyAsInt(argb & 0xFF), 0, 255);
        return (argb & 0xFF_00_00_00) | (r << 16) | (g << 8) | b;
    }
}
