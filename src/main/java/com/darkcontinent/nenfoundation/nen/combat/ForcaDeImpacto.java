package com.darkcontinent.nenfoundation.nen.combat;

/**
 * Quanto um golpe vale como LEITURA VISUAL: dano vira forca de ripple, em 0..1.
 *
 * <p>POR QUE ELE MUDOU DE LADO. Esta curva morava em {@code DetectorDeImpacto},
 * no cliente, alimentada pela diferenca entre duas leituras de vida. O servidor
 * tem o numero de dano REAL; o cliente tinha o mesmo numero depois de passar por
 * arredondamento, absorcao e um tick de atraso -- e, pior, sem saber ONDE o
 * golpe caiu. Com {@code ImpactoDeAuraS2C} a origem passa a ser o servidor, e
 * manter as duas seria duas fontes para a mesma verdade.
 *
 * <p><b>A CURVA E O QUE FOI PRESERVADO, e ela custou caro.</b> O ripple passou
 * por quatro correcoes ate aparecer na tela, e tres delas foram por raciocinio e
 * falharam. Os numeros aqui sao o que sobrou depois de instrumentar.
 *
 * <p>NAO E BALANCEAMENTO, e por isso nao vai para config: sao limites de
 * DESENHO. Quem quiser mudar o quanto um golpe machuca mexe no dano; isto aqui
 * decide apenas o quanto ele ACENDE.
 */
public final class ForcaDeImpacto {

    /**
     * Piso de dano que vira ripple, como fracao da vida maxima.
     *
     * <p>Sem ele, dano de fome, de veneno e o arranhao de meio coracao
     * acenderiam a aura a cada poucos segundos -- e um efeito que acende sempre
     * deixa de comunicar qualquer coisa.
     */
    public static final float PISO_DE_DANO = 0.02F;

    /**
     * A fracao da vida maxima que produz um ripple de forca TOTAL.
     *
     * <p>Um quarto da vida. Acima disso o efeito satura, e e proposital: a
     * diferenca entre "levei uma pancada seria" e "quase morri" nao precisa
     * caber no halo -- ela ja esta na barra de vida.
     */
    public static final float DANO_DE_FORCA_TOTAL = 0.25F;

    private ForcaDeImpacto() {
    }

    /**
     * A forca do ripple para este dano.
     *
     * <p><b>A CURVA EXISTE PORQUE A FRACAO CRUA E INVISIVEL.</b> Um soco de mao
     * vazia tira 1 de 20 -- 5% --, e um realce de 0,05 no multiplicador de alpha
     * nao aparece na tela. Era um dos motivos de <i>"nao acende nunca"</i>. Com
     * a raiz sobre {@link #DANO_DE_FORCA_TOTAL} o mesmo soco vale 0,45: um flash
     * que se ve, sem igualar o golpe que quase mata.
     *
     * <p>E a fracao e da vida MAXIMA, nao da atual: pela atual, o ultimo golpe
     * de alguem quase morto seria sempre o mais forte da luta.
     *
     * @param dano dano que efetivamente saiu, depois da defesa de Nen
     * @param vidaMaxima vida maxima do alvo; zero ou negativa desliga a medicao
     * @return forca em {@code 0..1}, ou {@code 0} quando o golpe nao merece flash
     */
    public static float de(float dano, float vidaMaxima) {
        if (!(vidaMaxima > 0.0F) || !Float.isFinite(dano) || !(dano > 0.0F)) {
            // Sem dano nao houve impacto NA AURA: escudo, invulnerabilidade ou
            // cura. O corpo pode ate piscar; a aura nao reage.
            return 0.0F;
        }
        float fracao = dano / vidaMaxima;
        if (fracao < PISO_DE_DANO) {
            return 0.0F;
        }
        return (float) Math.min(1.0D, Math.sqrt(fracao / DANO_DE_FORCA_TOTAL));
    }
}
