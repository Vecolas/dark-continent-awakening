package com.darkcontinent.nenfoundation.worldtree;

/**
 * Uma cortina de vinha caindo da borda inferior de uma prateleira.
 *
 * <p><b>ELAS NAO SAO ENFEITE.</b> Na referencia, metade da leitura de ESCALA vem
 * das cortinas penduradas: uma copa sem elas parece um cogumelo, e com elas
 * parece dossel. Foi a coisa que faltava por inteiro na versao anterior.
 *
 * <p><b>TODA VINHA NASCE DENTRO DE UMA PRATELEIRA.</b> Uma vinha orfa e uma ilha
 * flutuante FINA -- e fina e pior que grossa: ela le como bug, e nao como
 * paisagem. O portao verifica isso em vinte seeds.
 *
 * @param originX  onde ela nasce, ja dentro do disco da prateleira
 * @param originY  a face de baixo da prateleira
 * @param originZ  onde ela nasce
 * @param length   quantos blocos ela desce
 * @param driftX   desvio horizontal acumulado ao longo da queda
 * @param driftZ   idem
 * @param thick    se e a vinha grossa (rara) ou a fina
 * @param shelfIndex qual prateleira a sustenta, no plano
 */
public record WorldTreeVineStrand(
        double originX, double originY, double originZ,
        int length, double driftX, double driftZ, boolean thick, int shelfIndex) {

    /** Abaixo disto a cortina nao se le como cortina; e um bloco solto. */
    public static final int COMPRIMENTO_MINIMO = 6;

    /** Teto duro. Acima disto a vinha vira corda de rapel ate o chao. */
    public static final int COMPRIMENTO_MAXIMO = 40;

    public WorldTreeVineStrand {
        if (length < COMPRIMENTO_MINIMO || length > COMPRIMENTO_MAXIMO) {
            throw new IllegalArgumentException("comprimento de vinha fora da faixa: " + length);
        }
        if (shelfIndex < 0) {
            throw new IllegalArgumentException("vinha sem prateleira de origem");
        }
    }

    /** A posicao da vinha {@code step} blocos abaixo da origem. */
    public double xAt(int step) {
        return originX + driftX * ((double) step / length);
    }

    public double zAt(int step) {
        return originZ + driftZ * ((double) step / length);
    }

    public double endY() {
        return originY - length;
    }
}
