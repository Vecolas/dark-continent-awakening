package com.darkcontinent.nenfoundation.enemy.greedisland.layout;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants;

/**
 * A altura macro de qualquer ponto da ilha. Secoes 16 e 17 do documento.
 *
 * <p>Ela responde "que altura ESTA REGIAO tem", e nao "que bloco fica aqui". O
 * relevo fino -- morro, pedra, ravina -- e microgeografia, e entra depois, no
 * gerador de chunk. Misturar os dois faria o mapa de debug precisar gerar
 * terreno para ser desenhado, e o gate macro existe justamente para nao
 * depender disso.
 *
 * <p><b>A ALTURA SAI DA MASCARA</b>, e nao de um segundo ruido independente. A
 * distancia com sinal ja diz o quanto se esta para dentro da ilha, e o
 * documento descreve a escada em funcao disso: praia, planicie, colina,
 * planalto, montanha. Um campo de altura que ignorasse a mascara produziria
 * montanha dentro do mar e planicie na ponta da peninsula.
 *
 * <p>SEM CADEIAS AINDA. As tres cordilheiras da secao 18 -- North Crown,
 * Western Spine, Central-Southeast Highlands -- sao a fase G2. O que existe
 * aqui e a base continental sobre a qual elas vao ser somadas, e o javadoc diz
 * isso em vez de deixar a ausencia parecer esquecimento.
 */
public final class GreedIslandElevationField {

    /**
     * Ate onde a praia sobe, contado da linha de costa para dentro.
     *
     * <p>Uma faixa estreita produz falesia em toda a volta; larga demais, a
     * ilha vira uma rampa. Dois mil blocos e o bastante para a subida ser
     * visivel ao caminhar e curta o bastante para nao comer a planicie.
     */
    private static final double FAIXA_DE_PRAIA = 2_000.0D;

    /** A partir daqui o interior ja esta na faixa de colina. */
    private static final double FAIXA_DE_INTERIOR = 14_000.0D;

    /** Profundidade maxima do oceano, abaixo do nivel do mar. */
    private static final int FUNDO_DO_OCEANO = 30;

    /** Onde o mar deixa de ficar mais fundo. */
    private static final double PLATAFORMA = 6_000.0D;

    private GreedIslandElevationField() {
    }

    /**
     * A altura macro em Y.
     *
     * <p>No mar, ela devolve o LEITO -- e nao o nivel da agua. Quem desenha o
     * mapa precisa dos dois, e devolver o nivel do mar aqui apagaria a
     * plataforma continental do mapa de debug.
     */
    public static int alturaEm(double x, double z) {
        double d = GreedIslandMask.distanciaComSinal(x, z);
        if (d <= 0.0D) {
            double t = Math.clamp(-d / PLATAFORMA, 0.0D, 1.0D);
            return (int) Math.round(
                    GreedIslandConstants.NIVEL_DO_MAR - FUNDO_DO_OCEANO * suave(t));
        }
        if (d < FAIXA_DE_PRAIA) {
            double t = d / FAIXA_DE_PRAIA;
            return (int) Math.round(interpolar(GreedIslandConstants.NIVEL_DO_MAR + 1,
                    GreedIslandConstants.TOPO_DA_PLANICIE - 20, suave(t)));
        }
        double t = Math.clamp((d - FAIXA_DE_PRAIA)
                / (FAIXA_DE_INTERIOR - FAIXA_DE_PRAIA), 0.0D, 1.0D);
        // O INTERIOR SOBE ATE A FAIXA DE COLINA, e para ali. Levar a base ate
        // a faixa de montanha nao deixaria espaco para as cadeias do G2 se
        // destacarem -- elas viram inchacos num planalto alto, e o documento
        // pede cordilheiras conectadas e legiveis.
        return (int) Math.round(interpolar(GreedIslandConstants.TOPO_DA_PLANICIE - 20,
                GreedIslandConstants.TOPO_DAS_COLINAS, suave(t)));
    }

    /** Se este ponto fica abaixo da linha d'agua. */
    public static boolean submerso(double x, double z) {
        return alturaEm(x, z) < GreedIslandConstants.NIVEL_DO_MAR;
    }

    private static double interpolar(double de, double para, double t) {
        return de + (para - de) * t;
    }

    private static double suave(double t) {
        return t * t * (3.0D - 2.0D * t);
    }
}
