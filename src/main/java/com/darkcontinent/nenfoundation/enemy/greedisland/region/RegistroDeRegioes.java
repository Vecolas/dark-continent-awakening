package com.darkcontinent.nenfoundation.enemy.greedisland.region;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Ponto;
import com.darkcontinent.nenfoundation.enemy.greedisland.region.RegiaoMacro.Terreno;
import com.darkcontinent.nenfoundation.enemy.greedisland.region.RegiaoMacro.Umidade;
import java.util.List;
import java.util.Optional;

/**
 * As 24 macro-regioes da secao 60, congeladas. Fase G4.
 *
 * <p>Os NOMES e a CONTAGEM sao do documento. Os centros e raios sao leitura:
 * cada regiao foi ancorada no acidente geografico que a nomeia -- "Northfall
 * Valley" no vale do rio Northfall, "North Crown Central" no meio da cadeia
 * norte, "Masadora Basin" na bacia de Masadora. Sao
 * {@code ORIGINAL_COMPATIBLE}, como tudo que o documento nao fixa.
 *
 * <p><b>A DIFICULDADE CRESCE COM A DISTANCIA DO SHISO, MAS NAO E RADIAL.</b> A
 * secao 61 e explicita sobre isso, e o documento reforca: o jogador pode
 * entrar cedo numa area perigosissima se quiser -- e Hunter x Hunter. Por isso
 * ha faixa 6 encostada em faixa 2 no nordeste remoto, e ha um corredor de
 * faixa 1-2 indo de Shiso ate Antokiba.
 */
public final class RegistroDeRegioes {

    private static final List<RegiaoMacro> REGIOES = List.of(
            // --- o corredor inicial, faixa 1-2 -------------------------
            new RegiaoMacro("shiso_approach", "Shiso Approach",
                    new Ponto(-18_000, 1_000), 4_500, 1,
                    Terreno.PLANICIE, Umidade.MEDIA),
            new RegiaoMacro("antokiba_basin", "Antokiba Basin",
                    new Ponto(-14_000, 5_000), 6_000, 2,
                    Terreno.PLANICIE, Umidade.MEDIA),
            new RegiaoMacro("western_lowlands", "Western Lowlands",
                    new Ponto(-24_000, 6_000), 6_500, 2,
                    Terreno.PLANICIE, Umidade.UMIDA),

            // --- a espinha ocidental -----------------------------------
            new RegiaoMacro("western_spine_foothills", "Western Spine Foothills",
                    new Ponto(-23_000, -6_000), 6_000, 3,
                    Terreno.COLINA, Umidade.MEDIA),
            new RegiaoMacro("western_spine", "Western Spine",
                    new Ponto(-27_000, -5_000), 5_500, 5,
                    Terreno.MONTANHA, Umidade.SECA),

            // --- o norte ------------------------------------------------
            new RegiaoMacro("northfall_valley", "Northfall Valley",
                    new Ponto(-10_000, -18_000), 6_500, 3,
                    Terreno.COLINA, Umidade.UMIDA),
            new RegiaoMacro("north_crown_west", "North Crown West",
                    new Ponto(-20_000, -26_000), 6_000, 5,
                    Terreno.MONTANHA, Umidade.SECA),
            new RegiaoMacro("north_crown_central", "North Crown Central",
                    new Ponto(-3_000, -26_000), 6_500, 6,
                    Terreno.MONTANHA, Umidade.SECA),
            new RegiaoMacro("north_crown_east", "North Crown East",
                    new Ponto(16_000, -24_000), 6_500, 6,
                    Terreno.MONTANHA, Umidade.SECA),

            // --- o centro -----------------------------------------------
            new RegiaoMacro("central_plateau_west", "Central Plateau West",
                    new Ponto(-11_000, -6_000), 5_500, 3,
                    Terreno.COLINA, Umidade.MEDIA),
            new RegiaoMacro("central_plateau", "Central Plateau",
                    new Ponto(-3_000, -2_000), 6_500, 4,
                    Terreno.PLANALTO, Umidade.SECA),
            new RegiaoMacro("masadora_basin", "Masadora Basin",
                    new Ponto(4_500, -2_000), 5_500, 3,
                    Terreno.PLANICIE, Umidade.UMIDA),

            // --- o leste -------------------------------------------------
            new RegiaoMacro("eastern_tableland", "Eastern Tableland",
                    new Ponto(19_000, -5_000), 6_000, 4,
                    Terreno.PLANALTO, Umidade.SECA),
            new RegiaoMacro("eastflow_woods", "Eastflow Woods",
                    new Ponto(28_000, -8_000), 6_500, 4,
                    Terreno.COLINA, Umidade.UMIDA),
            new RegiaoMacro("aiai_country", "Aiai Country",
                    new Ponto(15_500, 5_500), 6_000, 3,
                    Terreno.PLANICIE, Umidade.MEDIA),

            // --- o sul e o sudeste ---------------------------------------
            new RegiaoMacro("central_highlands", "Central Highlands",
                    new Ponto(7_000, 9_000), 6_000, 5,
                    Terreno.MONTANHA, Umidade.MEDIA),
            new RegiaoMacro("dorias_reach", "Dorias Reach",
                    new Ponto(8_500, 17_000), 6_000, 4,
                    Terreno.COLINA, Umidade.MEDIA),
            new RegiaoMacro("southern_marsh_north", "Southern Marsh North",
                    new Ponto(-6_000, 17_000), 6_000, 4,
                    Terreno.ALAGADO, Umidade.UMIDA),
            new RegiaoMacro("southern_marsh_deep", "Southern Marsh Deep",
                    new Ponto(-8_000, 26_000), 6_500, 6,
                    Terreno.ALAGADO, Umidade.ENCHARCADA),
            new RegiaoMacro("limeiro_plain", "Limeiro Plain",
                    new Ponto(-1_000, 27_000), 6_000, 7,
                    Terreno.PLANICIE, Umidade.MEDIA),
            new RegiaoMacro("southeast_highlands", "Southeast Highlands",
                    new Ponto(18_000, 18_000), 6_000, 6,
                    Terreno.MONTANHA, Umidade.MEDIA),

            // --- Soufrabi -------------------------------------------------
            new RegiaoMacro("soufrabi_hinterland", "Soufrabi Hinterland",
                    new Ponto(23_000, 19_000), 5_000, 5,
                    Terreno.COLINA, Umidade.MEDIA),
            new RegiaoMacro("soufrabi_coast", "Soufrabi Coast",
                    new Ponto(28_000, 24_000), 5_500, 5,
                    Terreno.COSTEIRO, Umidade.UMIDA),

            // --- o fim do mundo -------------------------------------------
            new RegiaoMacro("remote_northeast", "Remote Northeast",
                    new Ponto(33_000, -17_000), 6_500, 8,
                    Terreno.COSTEIRO, Umidade.SECA));

    private RegistroDeRegioes() {
    }

    /** As 24, na ordem do documento. */
    public static List<RegiaoMacro> todas() {
        return REGIOES;
    }

    /**
     * A regiao de um ponto: a de centro mais proximo.
     *
     * <p>MAIS PROXIMO, e nao "dentro do raio": com raios, um ponto entre duas
     * regioes ficaria FORA das duas, e a ilha teria buracos sem regiao -- e
     * quem consulta receberia vazio num lugar perfeitamente normal. O raio
     * serve para medir quao central o ponto e, e nao para excluir.
     */
    public static RegiaoMacro em(double x, double z) {
        RegiaoMacro melhor = REGIOES.get(0);
        double menor = Double.MAX_VALUE;
        for (RegiaoMacro r : REGIOES) {
            // NORMALIZADA PELO RAIO: sem isso, uma regiao grande engole as
            // vizinhas pequenas so por ter o centro mais longe de tudo.
            double d = r.distanciaDe(x, z) / r.raio();
            if (d < menor) {
                menor = d;
                melhor = r;
            }
        }
        return melhor;
    }

    /** Uma regiao pelo id. */
    public static Optional<RegiaoMacro> porId(String id) {
        return REGIOES.stream().filter(r -> r.id().equals(id)).findFirst();
    }
}
