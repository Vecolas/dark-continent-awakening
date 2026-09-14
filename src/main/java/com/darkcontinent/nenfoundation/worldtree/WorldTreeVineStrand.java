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

    /**
     * Se esta cortina pode escrever alguma coisa num chunk.
     *
     * <p><b>ELA NAO EXISTIA, E A FALTA DELA CUSTAVA EM TODO CHUNK DA DIMENSAO.</b>
     * O laco de prateleiras sempre teve um corte por chunk na frente; o de vinhas
     * nao tinha nada -- o corte de bounds morava dentro do desenho, por PASSO.
     * Com ~6.000 cortinas de ~23 passos, isso era ~140.000 iteracoes por chunk,
     * pagas igualmente por um chunk a 5.000 blocos do tronco, onde nao ha uma
     * folha sequer. Medido: ~1,2 ms por chunk, escrevendo zero blocos.
     *
     * <p>Nao dava erro. Dava mundo demorando para carregar -- o mesmo sintoma que
     * motivou o cache do plano, e que ninguem atribuiria a folhagem.
     *
     * <p>A caixa e generosa em um bloco para cada lado, porque a deriva e
     * interpolada e o arredondamento pode cair na coluna vizinha.
     */
    public boolean touchesChunk(int minX, int minZ) {
        double x0 = Math.min(originX, originX + driftX) - 1.0;
        double x1 = Math.max(originX, originX + driftX) + 1.0;
        double z0 = Math.min(originZ, originZ + driftZ) - 1.0;
        double z1 = Math.max(originZ, originZ + driftZ) + 1.0;
        return minX <= x1 && minX + 16 >= x0 && minZ <= z1 && minZ + 16 >= z0;
    }

    /** Quantos blocos o desenho visitaria desta cortina, dentro de um chunk. */
    public long estimatedVisitsInChunk(int minX, int minZ) {
        if (!touchesChunk(minX, minZ)) {
            return 0L;
        }
        long visits = 0;
        for (int step = 0; step < length; step++) {
            int x = (int) Math.floor(xAt(step));
            int z = (int) Math.floor(zAt(step));
            if (x >= minX && x < minX + 16 && z >= minZ && z < minZ + 16) {
                visits++;
            }
        }
        return visits;
    }
}
