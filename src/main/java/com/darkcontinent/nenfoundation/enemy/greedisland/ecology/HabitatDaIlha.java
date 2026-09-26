package com.darkcontinent.nenfoundation.enemy.greedisland.ecology;

import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandElevationField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandHydrologyField;
import com.darkcontinent.nenfoundation.enemy.greedisland.region.BiomaDaIlha;
import com.darkcontinent.nenfoundation.enemy.greedisland.region.RegiaoMacro;
import com.darkcontinent.nenfoundation.enemy.greedisland.region.RegistroDeRegioes;
import java.util.List;
import java.util.Set;

/**
 * Onde cada criatura de Greed Island VIVE. Secao 69, fase G10.
 *
 * <p>O documento e categorico sobre o que NAO fazer:
 *
 * <pre>
 *   Nunca: biome apenas.
 * </pre>
 *
 * <p>Uma criatura com {@code spawnWeight = 20, biome = forest} aparece em toda
 * floresta da ilha, e o jogador nunca aprende onde procurar. Com habitat, ele
 * passa a pensar <i>"preciso dessa carta; sei onde esse bicho vive"</i> -- e
 * essa frase e o objetivo declarado da secao.
 *
 * <p>O spawn depende de <b>regiao, habitat, altitude, distancia da estrada e
 * distancia da cidade</b>, e nao de bioma sozinho. Duas criaturas podem
 * dividir o mesmo bioma e nunca se encontrar, porque uma mora na regiao alagada
 * do sul e a outra no planalto central.
 *
 * <p>ELE NAO REGISTRA MOB. As sete criaturas de Greed Island ja existem em
 * {@code enemy/entity}; o que faltava era a GEOGRAFIA delas. Este arquivo
 * responde "aqui cabe tal bicho?", e quem povoa continua sendo o spawner.
 */
public final class HabitatDaIlha {

    /** A que distancia de uma estrada o bicho selvagem ja evita aparecer. */
    private static final int RECUO_DA_ESTRADA = 90;

    /** Idem, de uma cidade. Maior: cidade espanta mais longe que estrada. */
    private static final int RECUO_DA_CIDADE = 260;

    /** O habitat de uma especie: onde ela cabe. */
    public record Habitat(String criatura, Set<String> regioesPreferidas,
            Set<BiomaDaIlha> biomas, int alturaMinima, int alturaMaxima,
            boolean precisaDeAgua) {

        /** Se este ponto serve para esta especie. */
        public boolean cabeEm(double x, double z) {
            RegiaoMacro regiao = RegistroDeRegioes.em(x, z);
            if (!this.regioesPreferidas.isEmpty()
                    && !this.regioesPreferidas.contains(regiao.id())) {
                return false;
            }
            if (!this.biomas.isEmpty() && !this.biomas.contains(BiomaDaIlha.em(x, z))) {
                return false;
            }
            int y = GreedIslandElevationField.alturaEm(x, z);
            if (y < this.alturaMinima || y > this.alturaMaxima) {
                return false;
            }
            boolean perto = GreedIslandHydrologyField.distanciaAoRio(x, z) < 400
                    || GreedIslandHydrologyField.molhado(x, z);
            return this.precisaDeAgua == perto || !this.precisaDeAgua;
        }
    }

    /**
     * Os habitats das sete criaturas de Greed Island.
     *
     * <p>CADA UMA NUMA GEOGRAFIA DIFERENTE, e isso e o desenho: se duas
     * dividissem regiao, bioma e altura, saber onde uma vive nao ensinaria
     * nada sobre a outra -- e o conhecimento geografico que a secao 69 quer
     * construir nao existiria.
     */
    public static final List<Habitat> HABITATS = List.of(
            // Campo aberto, longe da serra: o Puffball precisa de espaco raso.
            new Habitat("hyper_puffball",
                    Set.of("southern_marsh_north", "southern_marsh_deep"),
                    Set.of(BiomaDaIlha.GI_WETLAND, BiomaDaIlha.GI_MEADOW),
                    60, 100, true),

            // Rota e mata rala: o Radio Rat vive perto de onde se passa.
            new Habitat("radio_rat",
                    Set.of("masadora_basin", "aiai_country", "central_plateau"),
                    Set.of(BiomaDaIlha.GI_LIGHT_FOREST, BiomaDaIlha.GI_MEADOW),
                    60, 140, false),

            // Planicie ocidental, com agua por perto.
            new Habitat("bubble_horse",
                    Set.of("western_lowlands", "antokiba_basin"),
                    Set.of(BiomaDaIlha.GI_MEADOW, BiomaDaIlha.GI_RIVERLAND),
                    60, 120, true),

            // Serra: o Cyclops mora alto, e e por isso que encontra-lo custa.
            new Habitat("cyclops",
                    Set.of("north_crown_central", "north_crown_east", "north_crown_west"),
                    Set.of(BiomaDaIlha.GI_MOUNTAIN, BiomaDaIlha.GI_HIGHLAND),
                    150, 320, false),

            // Mata velha e umida do leste.
            new Habitat("man_faced_ape",
                    Set.of("eastflow_woods", "southeast_highlands"),
                    Set.of(BiomaDaIlha.GI_OLD_FOREST),
                    70, 160, false),

            // Costa e brejo: o sapo espera enterrado perto da agua.
            new Habitat("frog_in_waiting",
                    Set.of("southern_marsh_deep", "soufrabi_coast"),
                    Set.of(BiomaDaIlha.GI_WETLAND, BiomaDaIlha.GI_RIVERLAND),
                    58, 95, true),

            // O pantano do mestre: uma regiao so, e por isso ele e raro.
            new Habitat("master_of_the_swamp",
                    Set.of("southern_marsh_deep"),
                    Set.of(BiomaDaIlha.GI_WETLAND),
                    58, 90, true));

    private HabitatDaIlha() {
    }

    /**
     * As criaturas que cabem neste ponto.
     *
     * <p>PODE DEVOLVER VAZIO, e isso e o desejado na maior parte da ilha: a
     * secao 111 pede 60-70% de natureza relativamente aberta, e uma ilha onde
     * todo ponto tem bicho e um corredor de combate, nao um lugar.
     */
    public static List<Habitat> em(double x, double z) {
        if (perturbadoPorGente(x, z)) {
            return List.of();
        }
        return HABITATS.stream().filter(h -> h.cabeEm(x, z)).toList();
    }

    /**
     * Se ha estrada ou cidade perto o bastante para espantar a fauna.
     *
     * <p>ELE E O QUE FAZ A ESTRADA SIGNIFICAR ALGO. Sem isso, viajar pela
     * estrada e tao perigoso quanto cortar pela mata, e a secao 71 -- rota
     * longa e segura contra rota curta e perigosa -- deixa de existir.
     */
    public static boolean perturbadoPorGente(double x, double z) {
        for (var c : com.darkcontinent.nenfoundation.enemy.greedisland.city
                .RegistroDeCidades.todas()) {
            if (Math.hypot(x - c.ancora().x(), z - c.ancora().z())
                    < c.raio() + RECUO_DA_CIDADE) {
                return true;
            }
        }
        return distanciaAEstradaMenorQue(x, z, RECUO_DA_ESTRADA);
    }

    /**
     * Aproximacao barata de "ha estrada perto".
     *
     * <p>ELA NAO ROTEIA. Rotear as nove estradas para cada tentativa de spawn
     * derrubaria o servidor -- o A* custa milhares de nos. Aqui basta a reta
     * entre as cidades ligadas: ela erra onde a estrada contorna uma serra, e
     * o erro e para o lado seguro (fauna aparecendo perto de uma estrada que
     * na verdade passa longe), e nao para o lado que esvazia o mapa.
     *
     * <p>Quando o G13 guardar o traçado em {@code SavedData}, isto passa a ler
     * o traçado real -- e este javadoc e o lembrete.
     */
    private static boolean distanciaAEstradaMenorQue(double x, double z, double limite) {
        for (var l : com.darkcontinent.nenfoundation.enemy.greedisland
                .GreedIslandConstants.ESTRADAS) {
            var a = com.darkcontinent.nenfoundation.enemy.greedisland
                    .GreedIslandConstants.cidade(l.de()).orElseThrow();
            var b = com.darkcontinent.nenfoundation.enemy.greedisland
                    .GreedIslandConstants.cidade(l.para()).orElseThrow();
            if (distanciaAoSegmento(x, z, a.x(), a.z(), b.x(), b.z()) < limite) {
                return true;
            }
        }
        return false;
    }

    private static double distanciaAoSegmento(double px, double pz,
            double ax, double az, double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        double comp = dx * dx + dz * dz;
        if (comp <= 0.0D) {
            return Math.hypot(px - ax, pz - az);
        }
        double t = Math.clamp(((px - ax) * dx + (pz - az) * dz) / comp, 0.0D, 1.0D);
        return Math.hypot(px - (ax + t * dx), pz - (az + t * dz));
    }
}
