package com.darkcontinent.nenfoundation.client.hud;

/**
 * Vida e vida maxima, saneadas, prontas para desenhar.
 *
 * <p>VIDA NAO VEM DO DELTA DE NEN, e nao deveria: ela e do Minecraft, ja chega
 * sincronizada pelo proprio jogo, e po-la no payload de Nen criaria a segunda
 * fonte da mesma verdade -- com a agravante de que a nossa chegaria sempre um
 * tick atras da do vanilla.
 *
 * <p>ENTAO POR QUE UMA CLASSE, e nao dois floats no renderer? Porque o
 * saneamento tem casos, e caso sem teste e onde mora a divisao por zero. Vida
 * maxima pode ser zero (jogador morto em alguns estados), pode ser alterada por
 * atributo de outro mod, e absorcao faz a vida passar do maximo. Nenhum desses
 * tres levanta excecao; todos os tres desenham errado.
 */
public record ProjecaoDeVida(float atual, float maxima) {

    /** O estado em que ainda nao ha jogador para perguntar. */
    public static ProjecaoDeVida ausente() {
        return new ProjecaoDeVida(0.0F, 0.0F);
    }

    public static ProjecaoDeVida de(float atual, float maxima) {
        return new ProjecaoDeVida(atual, maxima);
    }

    /**
     * A fracao preenchida, entre 0 e 1.
     *
     * <p>ABSORCAO E PRESA NO TETO, e nao deixada estourar: a barra tem largura
     * fixa, e uma fracao de 1,4 desenharia por cima do valor numerico a
     * direita. Que a absorcao "nao apareca" e o custo aceito -- ela e efeito
     * temporario, e o numero ao lado continua dizendo a verdade.
     */
    public float fracao() {
        if (!Float.isFinite(this.atual) || !Float.isFinite(this.maxima)
                || this.maxima <= 0.0F) {
            return 0.0F;
        }
        return Math.clamp(this.atual / this.maxima, 0.0F, 1.0F);
    }

    /** Se ha o que desenhar. Maximo zero nao e "vida vazia", e "sem leitura". */
    public boolean disponivel() {
        return Float.isFinite(this.maxima) && this.maxima > 0.0F;
    }

    /**
     * O texto do valor, ja arredondado.
     *
     * <p>MEIO CORACAO NAO VIRA MEIO NUMERO. A vida do Minecraft anda de 0,5 em
     * 0,5, e "19.5 / 20" e mais digito do que a HUD tem largura e mais precisao
     * do que a leitura de relance usa. Arredondar para CIMA evita o pior caso:
     * mostrar "0" para quem ainda tem meio coracao e nao morreu.
     */
    public String texto() {
        if (!disponivel()) {
            return "--";
        }
        return (int) Math.ceil(Math.max(0.0F, this.atual)) + "/" + Math.round(this.maxima);
    }
}
