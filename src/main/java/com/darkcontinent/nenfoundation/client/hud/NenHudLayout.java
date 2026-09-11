package com.darkcontinent.nenfoundation.client.hud;

/**
 * Geometria da HUD de Nen em coordenadas GUI.
 *
 * <p>DECISAO: o layout nasce do tamanho logico da GUI, nunca da resolucao
 * fisica. Assim o proprio Minecraft aplica GUI Scale e a HUD permanece presa
 * ao canto superior esquerdo sem espalhar numeros pelos renderers.
 */
public record NenHudLayout(
        Retangulo molduraDasBarras,
        Retangulo retrato,
        Retangulo badge,
        Retangulo barraDeAura,
        Retangulo barraDeOutput) {

    public static final int MARGEM = 8;
    public static final int RETRATO = 30;
    public static final int BADGE = 14;
    public static final int ESPACO = 6;
    public static final int LARGURA_DAS_BARRAS = 150;
    public static final int ALTURA_DA_BARRA = 7;

    public static NenHudLayout para(int larguraGui) {
        int barrasX = MARGEM + RETRATO + ESPACO;
        int larguraDisponivel = Math.max(1, larguraGui - barrasX - MARGEM);
        int largura = Math.min(LARGURA_DAS_BARRAS, larguraDisponivel);
        return new NenHudLayout(
                new Retangulo(barrasX - 5, MARGEM + 7, largura + 10, 42),
                new Retangulo(MARGEM, MARGEM, RETRATO, RETRATO),
                new Retangulo(MARGEM + (RETRATO - BADGE) / 2,
                        MARGEM + RETRATO + 2, BADGE, BADGE),
                new Retangulo(barrasX, MARGEM + 12, largura, ALTURA_DA_BARRA),
                new Retangulo(barrasX, MARGEM + 37, largura, ALTURA_DA_BARRA));
    }

    /** Preenchimento continuo; as marcas de segmento nunca alteram esta conta. */
    public static int preenchimento(int largura, float fracao) {
        if (largura <= 0 || !Float.isFinite(fracao)) return 0;
        return Math.round(largura * Math.clamp(fracao, 0.0F, 1.0F));
    }

    public record Retangulo(int x, int y, int largura, int altura) {
        public Retangulo {
            if (largura < 0 || altura < 0) {
                throw new IllegalArgumentException("dimensao negativa");
            }
        }
    }
}
