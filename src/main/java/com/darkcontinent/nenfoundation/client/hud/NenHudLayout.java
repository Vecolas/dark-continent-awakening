package com.darkcontinent.nenfoundation.client.hud;

/**
 * Geometria da HUD de Nen em coordenadas GUI.
 *
 * <p>DECISAO: o layout nasce do tamanho logico da GUI, nunca da resolucao
 * fisica. Assim o proprio Minecraft aplica GUI Scale e a HUD permanece presa
 * ao canto superior esquerdo sem espalhar numeros pelos renderers.
 */
public record NenHudLayout(
        Retangulo moldura,
        Retangulo retrato,
        Retangulo badge,
        Retangulo barraDeAura,
        Retangulo barraDeOutput,
        Retangulo valorDeAura,
        Retangulo valorDeOutput,
        Retangulo tecnicasAtivas) {

    public static final int MARGEM = 8;
    public static final int LARGURA_DA_MOLDURA = 250;
    public static final int ALTURA_DA_MOLDURA = 63;

    public static NenHudLayout para(int larguraGui) {
        int largura = Math.min(LARGURA_DA_MOLDURA,
                Math.max(1, larguraGui - 2 * MARGEM));
        float escala = largura / (float) LARGURA_DA_MOLDURA;
        return new NenHudLayout(
                new Retangulo(MARGEM, MARGEM, largura, escalar(ALTURA_DA_MOLDURA, escala)),
                area(17, 17, 28, 28, escala),
                area(10, 44, 14, 14, escala),
                area(51, 22, 144, 8, escala),
                area(51, 41, 144, 8, escala),
                area(197, 22, 38, 9, escala),
                area(209, 41, 26, 9, escala),
                // ABAIXO DA MOLDURA, e nao dentro dela: a moldura ja esta cheia,
                // e a fila cresce para a direita conforme o jogador liga mais
                // tecnicas. Dentro, ela colidiria com a barra de output no
                // primeiro indicador extra.
                area(10, 55, 72, 9, escala));
    }

    private static Retangulo area(int x, int y, int largura, int altura, float escala) {
        return new Retangulo(MARGEM + escalar(x, escala), MARGEM + escalar(y, escala),
                escalar(largura, escala), escalar(altura, escala));
    }

    private static int escalar(int valor, float escala) {
        return Math.max(1, Math.round(valor * escala));
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
