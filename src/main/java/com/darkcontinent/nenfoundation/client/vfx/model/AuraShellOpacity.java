package com.darkcontinent.nenfoundation.client.vfx.model;

/**
 * Quanto cada camada da shell aparece.
 *
 * <p>SEPARADO DE {@link AuraGeometryProfile} POR UM MOTIVO CONCRETO, e nao por
 * gosto de dividir: a espessura e consumida UMA VEZ, no registro, quando as
 * malhas sao construidas; o alpha e consumido A CADA QUADRO. Junta-los faria um
 * slider de alpha em modo dev exigir a reconstrucao dos seis modelos -- e a
 * pessoa ajustando arte esperaria um recarregamento inteiro para ver dois
 * centesimos de diferenca.
 *
 * <p>A BORDA E A CAMADA MAIS FORTE, e e ela que carrega a leitura. O filme
 * interno da presenca; o halo externo quase nao aparece, e existe para separar
 * a aura do fundo e para alimentar o bloom no AV5. Se o halo externo for
 * visivel como uma segunda pele solida, ele esta forte demais.
 *
 * <p>NO AV0 ISSO E UMA APROXIMACAO GROSSEIRA, e vale dizer em voz alta: sem
 * Fresnel, a "borda" e so uma shell maior, entao as tres camadas leem como um
 * brilho uniforme em vez do contorno das referencias. O contorno nasce no AV1.
 *
 * <p>NUMEROS DE DIRECAO DE ARTE, nunca medidos em jogo. Eles saem daqui quando
 * o perfil em datapack existir (issue #98).
 */
public record AuraShellOpacity(float alphaInterno, float alphaBorda, float alphaExterno) {

    public AuraShellOpacity {
        validar(alphaInterno, "alphaInterno");
        validar(alphaBorda, "alphaBorda");
        validar(alphaExterno, "alphaExterno");
    }

    /** Ten: discreto, com a borda carregando a leitura. */
    public static AuraShellOpacity ten() {
        return new AuraShellOpacity(0.055F, 0.20F, 0.035F);
    }

    /** Ren: a MESMA shell, mais densa. */
    public static AuraShellOpacity ren() {
        return new AuraShellOpacity(0.09F, 0.32F, 0.09F);
    }

    /** Zetsu, e qualquer estado desligado: a ausencia e a informacao. */
    public static AuraShellOpacity zero() {
        return new AuraShellOpacity(0.0F, 0.0F, 0.0F);
    }

    /** O alpha deste passe, de 0 a 1. */
    public float alphaDe(AuraShellPass passe) {
        return switch (passe) {
            case INTERNA -> this.alphaInterno;
            case BORDA -> this.alphaBorda;
            case EXTERNA -> this.alphaExterno;
        };
    }

    private static void validar(float valor, String nome) {
        if (!Float.isFinite(valor) || valor < 0.0F || valor > 1.0F) {
            throw new IllegalArgumentException(nome + " deve estar entre 0 e 1: " + valor);
        }
    }
}
