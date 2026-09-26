package com.darkcontinent.nenfoundation.enemy.greedisland.region;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandElevationField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandHydrologyField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandMask;

/**
 * Os nove biomas da secao 62, e a regra que escolhe um. Fase G4.
 *
 * <p><b>NOVE BIOMAS PARA 24 REGIOES, e essa razao e o ponto.</b> A armadilha
 * que a secao 62 nomeia e criar um bioma por regiao: com 24, cada ajuste de
 * arvore vira arquivo novo, e duas regioes vizinhas com a mesma cara precisam
 * de dois biomas identicos com nomes diferentes.
 *
 * <p>A escolha usa as cinco entradas que o documento lista -- regiao,
 * elevacao, umidade, distancia do rio e distancia da costa -- e a ORDEM delas
 * importa:
 *
 * <ol>
 *   <li><b>agua primeiro.</b> Um ponto dentro do rio e {@code RIVERLAND},
 *       esteja ele na serra ou no brejo. Perguntar a regiao antes faria o rio
 *       que corta a montanha virar bioma de montanha -- com agua correndo
 *       dentro;
 *   <li><b>costa depois.</b> Praia e praia em qualquer regiao;
 *   <li><b>altitude</b> resolve montanha e planalto, porque altitude e o que
 *       o jogador ve;
 *   <li><b>regiao e umidade</b> resolvem o resto, que e a maior parte da ilha.
 * </ol>
 */
public enum BiomaDaIlha {

    /** Campo aberto. O bioma mais comum da ilha. */
    GI_MEADOW,
    /** Mata rala, com clareira. */
    GI_LIGHT_FOREST,
    /** Mata fechada e velha. */
    GI_OLD_FOREST,
    /** Encosta alta, vegetacao curta. */
    GI_HIGHLAND,
    /** Rocha exposta acima da linha de arvore. */
    GI_MOUNTAIN,
    /** Brejo, canal e lama. */
    GI_WETLAND,
    /** A faixa de praia e falesia. */
    GI_COAST,
    /** Mesa alta e seca. */
    GI_TABLELAND,
    /** A margem de rio e de lago, mais verde que o entorno. */
    GI_RIVERLAND;

    /** Ate onde a faixa costeira vai, para dentro. */
    private static final double FAIXA_COSTEIRA = 900.0D;

    /** A que distancia do rio a margem ainda conta como ribeirinha. */
    private static final double FAIXA_RIBEIRINHA = 260.0D;

    /**
     * O bioma de um ponto.
     *
     * <p>Pura e sem Minecraft, como todo o layout: o gate precisa varrer a ilha
     * inteira para conferir que nenhum bioma sumiu e que nenhum domina.
     */
    public static BiomaDaIlha em(double x, double z) {
        double doMar = GreedIslandMask.distanciaComSinal(x, z);
        if (doMar <= 0.0D) {
            return GI_COAST;
        }
        if (GreedIslandHydrologyField.molhado(x, z)
                || GreedIslandHydrologyField.distanciaAoRio(x, z) < FAIXA_RIBEIRINHA) {
            return GI_RIVERLAND;
        }
        if (doMar < FAIXA_COSTEIRA) {
            return GI_COAST;
        }

        RegiaoMacro regiao = RegistroDeRegioes.em(x, z);
        int altura = GreedIslandElevationField.alturaEm(x, z);

        if (altura >= GreedIslandConstants.TOPO_DO_PLANALTO) {
            return GI_MOUNTAIN;
        }
        if (regiao.terreno() == RegiaoMacro.Terreno.ALAGADO) {
            return GI_WETLAND;
        }
        if (regiao.terreno() == RegiaoMacro.Terreno.PLANALTO
                && altura >= GreedIslandConstants.TOPO_DAS_COLINAS - 20) {
            return GI_TABLELAND;
        }
        if (altura >= GreedIslandConstants.TOPO_DAS_COLINAS) {
            return GI_HIGHLAND;
        }
        return switch (regiao.umidade()) {
            case ENCHARCADA -> GI_WETLAND;
            // MATA VELHA PEDE UMIDADE E ALTURA MEDIA: no seco ela viraria
            // floresta que nao se sustenta, e na planicie baixa ela tapa a
            // leitura de longe que a ilha precisa ter.
            case UMIDA -> altura >= GreedIslandConstants.TOPO_DA_PLANICIE
                    ? GI_OLD_FOREST : GI_LIGHT_FOREST;
            case MEDIA -> altura >= GreedIslandConstants.TOPO_DA_PLANICIE
                    ? GI_LIGHT_FOREST : GI_MEADOW;
            case SECA -> GI_MEADOW;
        };
    }
}
