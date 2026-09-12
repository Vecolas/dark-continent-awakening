package com.darkcontinent.nenfoundation.client.vfx;

/**
 * A aritmetica de cor da aura, separada de quem desenha.
 *
 * <p>ELA MORA FORA DO RENDERER de proposito: aqui nao ha nenhum tipo do
 * Minecraft, entao a conta da para provar sem subir o jogo. Dentro da layer,
 * ela so seria exercitada por alguem olhando a tela -- e o defeito que ela
 * evita nao e visivel como erro, e sim como COR TROCADA.
 */
public final class CorDaAura {

    private CorDaAura() {
    }

    /**
     * Troca o alpha de um ARGB, preservando a cor.
     *
     * <p>O ALPHA E SATURADO, e nao deixado passar. A intensidade por regiao pode
     * passar de 1 -- e vai passar, com Gyo e com Ko, que e todo o proposito
     * dela. Sem o limite, {@code (int) (alpha * 255)} ultrapassa 255 e
     * TRANSBORDA para os bits de vermelho: a aura nao ficaria opaca demais, ela
     * mudaria de cor. E um deslocamento de bits nao lanca nada.
     *
     * <p>NaN TAMBEM E BARRADO. {@code Math.clamp} propaga NaN, e
     * {@code Math.round(NaN)} da zero -- o que por acaso e seguro, mas por
     * acaso nao e garantia. Aqui ele vira zero explicitamente.
     */
    public static int comAlpha(int argb, float alpha) {
        float seguro = Float.isNaN(alpha) ? 0.0F : Math.clamp(alpha, 0.0F, 1.0F);
        int a = Math.round(seguro * 255.0F);
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
