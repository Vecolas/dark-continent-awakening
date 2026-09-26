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
 * <p><b>A ORDEM DAS CAMADAS E A REGRA.</b> Base continental, depois planaltos,
 * depois cordilheiras, e as bacias rebaixando por ultimo:
 *
 * <ol>
 *   <li>a base sai da mascara -- praia sobe devagar, interior chega a colina;
 *   <li>o planalto LEVANTA uma mesa inteira, com borda;
 *   <li>a cordilheira soma por distancia ate a crista (G2);
 *   <li>a bacia REBAIXA, e vem depois: ela precisa vencer o que estiver por
 *       baixo, senao Antokiba nasce num morro;
 *   <li>o rio ESCAVA por ultimo -- um rio que nasce na serra tem de cortar a
 *       serra, e nao ser soterrado por ela (G3).
 * </ol>
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
        return (int) Math.round(comRelevo(x, z, d));
    }

    /** A altura de terra firme, com todas as camadas somadas. */
    private static double comRelevo(double x, double z, double d) {
        double t = Math.clamp((d - FAIXA_DE_PRAIA)
                / (FAIXA_DE_INTERIOR - FAIXA_DE_PRAIA), 0.0D, 1.0D);
        // O INTERIOR SOBE ATE A FAIXA DE COLINA, e para ali. Levar a base ate
        // a faixa de montanha nao deixaria espaco para as cadeias do G2 se
        // destacarem -- elas viram inchacos num planalto alto, e o documento
        // pede cordilheiras conectadas e legiveis.
        double base = interpolar(GreedIslandConstants.TOPO_DA_PLANICIE - 20,
                GreedIslandConstants.TOPO_DAS_COLINAS, suave(t));

        double comPlanalto = Math.max(base, contribuicaoDosPlanaltos(x, z, base));
        double comCadeias = comPlanalto + GreedIslandRidgeField.contribuicao(x, z);
        double comBacias = comCadeias - rebaixamentoDasBacias(x, z);
        // A ESCAVACAO VEM DEPOIS DE TUDO, inclusive da cordilheira: um rio que
        // nasce na serra tem de cortar a serra, e nao ser soterrado por ela.
        double comRios = comBacias - GreedIslandHydrologyField.escavacao(x, z);

        // O TETO E O DO DOCUMENTO (secao 17): picos ate 310. Sem o corte, a
        // soma de planalto com cordilheira encostaria no teto do mundo, e o
        // topo da montanha sairia cortado reto.
        return Math.clamp(comRios, GreedIslandConstants.NIVEL_DO_MAR - 6,
                GreedIslandConstants.PICO_MAXIMO);
    }

    /**
     * Os planaltos: mesa alta com borda, e nao um morro suave.
     *
     * <p>O PERFIL E CHATO NO MEIO e cai na borda -- por isso a potencia alta na
     * queda. Um planalto com perfil de sino nao e planalto, e um monte.
     */
    private static double contribuicaoDosPlanaltos(double x, double z, double base) {
        double maior = base;
        for (var p : GreedIslandConstants.PLANALTOS) {
            double d = Math.hypot(x - p.x(), z - p.z());
            if (d >= p.raio()) {
                continue;
            }
            double t = d / p.raio();
            // Mesa: quase 1 ate 70% do raio, e ai desce.
            double mesa = 1.0D - Math.pow(t, 6.0D);
            maior = Math.max(maior, base + (p.altura() - base) * mesa);
        }
        return maior;
    }

    /**
     * As bacias rebaixam, e vem POR ULTIMO.
     *
     * <p>Se viessem antes da cordilheira, uma crista passando perto de Antokiba
     * levantaria a cidade de volta -- e o documento coloca a cidade na bacia
     * justamente para ela ter onde caber.
     */
    private static double rebaixamentoDasBacias(double x, double z) {
        double total = 0.0D;
        for (var b : GreedIslandConstants.BACIAS) {
            double d = Math.hypot(x - b.x(), z - b.z());
            if (d >= b.raio()) {
                continue;
            }
            double t = d / b.raio();
            double queda = 0.5D * (1.0D + Math.cos(Math.PI * t));
            total = Math.max(total, b.rebaixamento() * queda * queda);
        }
        return total;
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
