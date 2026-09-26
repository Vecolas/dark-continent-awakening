package com.darkcontinent.nenfoundation.enemy.greedisland.landmark;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Ponto;
import com.darkcontinent.nenfoundation.enemy.greedisland.landmark.Landmark.Canon;
import com.darkcontinent.nenfoundation.enemy.greedisland.landmark.Landmark.Tipo;
import com.darkcontinent.nenfoundation.enemy.greedisland.region.RegistroDeRegioes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Os landmarks grandes da ilha. Secao 67, fase G9.
 *
 * <p>O documento pede 35-60 de grande porte. Os que carregam nome da obra --
 * a arvore Shiso e o farol de Soufrabi -- sao {@code DA_OBRA}; o resto e
 * {@code ORIGINAL_COMPATIVEL}, e o campo diz isso em vez de deixar a duvida.
 *
 * <p><b>PARTE DELES E DERIVADA DA GEOGRAFIA, e nao digitada.</b> Os passos de
 * montanha ja existem em {@link GreedIslandConstants#PASSOS}; as cachoeiras
 * nascem onde um rio cruza uma cordilheira. Digitar coordenada para eles
 * criaria a segunda fonte da mesma verdade -- alguem moveria o passo e o
 * landmark ficaria num descampado.
 *
 * <p><b>O ESPACAMENTO NAO E GRADE.</b> O documento e explicito, e a razao e
 * que grade regular se reconhece em cinco minutos: a partir dali, cada
 * landmark deixa de ser descoberta e vira item de lista.
 */
public final class RegistroDeLandmarks {

    private static final List<Landmark> LANDMARKS = montar();

    private RegistroDeLandmarks() {
    }

    private static List<Landmark> montar() {
        List<Landmark> lista = new ArrayList<>();

        // --- os dois que a obra nomeia ------------------------------------
        // O ID E `arvore_shiso`, E NAO `shiso_tree`, e a diferenca nao e
        // estetica: `shiso_tree` ja e o id da CIDADE. Com os dois iguais,
        // `ServicoDeNavegacao.destino("shiso_tree")` devolvia um dos dois por
        // ordem de lista -- e o portao de navegacao pegou, contando dois
        // destinos onde devia haver um.
        adicionar(lista, "arvore_shiso", Tipo.ARVORE_SHISO,
                ponto("shiso_tree"), Canon.DA_OBRA, 900);
        adicionar(lista, "farol_de_soufrabi", Tipo.FAROL,
                new Ponto(28_600, 23_400), Canon.DA_OBRA, 1_200);

        // --- DERIVADOS: os quatro passos ----------------------------------
        int i = 1;
        for (Ponto p : GreedIslandConstants.PASSOS) {
            adicionar(lista, "passo_" + i++, Tipo.PASSO, p, Canon.ORIGINAL_COMPATIVEL, 800);
        }

        // --- DERIVADOS: onde rio encontra serra, ha queda ------------------
        lista.addAll(cachoeiras());

        // --- sitios de carta e de Game Master ------------------------------
        adicionar(lista, "sitio_de_carta_norte", Tipo.SITIO_DE_CARTA,
                new Ponto(-5_000, -20_000), Canon.ORIGINAL_COMPATIVEL, 700);
        adicionar(lista, "sitio_de_carta_leste", Tipo.SITIO_DE_CARTA,
                new Ponto(22_000, -3_000), Canon.ORIGINAL_COMPATIVEL, 700);
        adicionar(lista, "sitio_de_carta_sul", Tipo.SITIO_DE_CARTA,
                new Ponto(3_000, 21_000), Canon.ORIGINAL_COMPATIVEL, 700);
        adicionar(lista, "torre_dos_game_masters", Tipo.SITIO_DE_GAME_MASTER,
                new Ponto(-2_000, -12_000), Canon.ORIGINAL_COMPATIVEL, 1_100);
        adicionar(lista, "ruina_dos_game_masters", Tipo.SITIO_DE_GAME_MASTER,
                new Ponto(20_000, 12_000), Canon.ORIGINAL_COMPATIVEL, 900);

        // --- arenas --------------------------------------------------------
        adicionar(lista, "arena_de_antokiba", Tipo.ARENA,
                new Ponto(-13_200, 7_200), Canon.ORIGINAL_COMPATIVEL, 600);
        adicionar(lista, "arena_do_planalto", Tipo.ARENA,
                new Ponto(-1_000, -4_000), Canon.ORIGINAL_COMPATIVEL, 600);

        // --- rochedos e arvores antigas, espalhados sem grade ---------------
        adicionar(lista, "rochedo_norte", Tipo.ROCHEDO,
                new Ponto(-12_000, -30_000), Canon.ORIGINAL_COMPATIVEL, 700);
        adicionar(lista, "rochedo_oeste", Tipo.ROCHEDO,
                new Ponto(-32_000, -12_000), Canon.ORIGINAL_COMPATIVEL, 700);
        adicionar(lista, "rochedo_leste", Tipo.ROCHEDO,
                new Ponto(33_000, -6_000), Canon.ORIGINAL_COMPATIVEL, 700);
        adicionar(lista, "rochedo_sudeste", Tipo.ROCHEDO,
                new Ponto(24_000, 27_000), Canon.ORIGINAL_COMPATIVEL, 700);
        adicionar(lista, "arvore_antiga_oeste", Tipo.ARVORE_ANTIGA,
                new Ponto(-25_000, 14_000), Canon.ORIGINAL_COMPATIVEL, 800);
        adicionar(lista, "arvore_antiga_leste", Tipo.ARVORE_ANTIGA,
                new Ponto(29_000, -13_000), Canon.ORIGINAL_COMPATIVEL, 800);
        adicionar(lista, "arvore_antiga_sul", Tipo.ARVORE_ANTIGA,
                new Ponto(-4_000, 29_000), Canon.ORIGINAL_COMPATIVEL, 800);

        // --- habitats unicos ------------------------------------------------
        adicionar(lista, "ninho_do_cyclops", Tipo.HABITAT_UNICO,
                new Ponto(11_000, -22_000), Canon.ORIGINAL_COMPATIVEL, 900);
        adicionar(lista, "brejo_do_puffball", Tipo.HABITAT_UNICO,
                new Ponto(-9_000, 20_000), Canon.ORIGINAL_COMPATIVEL, 900);
        adicionar(lista, "campo_do_bubble_horse", Tipo.HABITAT_UNICO,
                new Ponto(-19_000, 9_000), Canon.ORIGINAL_COMPATIVEL, 900);
        adicionar(lista, "toca_do_radio_rat", Tipo.HABITAT_UNICO,
                new Ponto(6_000, 7_000), Canon.ORIGINAL_COMPATIVEL, 800);

        // --- assentamentos menores (secao 51) -------------------------------
        adicionar(lista, "posto_da_trilha", Tipo.ASSENTAMENTO,
                new Ponto(-16_200, 3_200), Canon.ORIGINAL_COMPATIVEL, 500);
        adicionar(lista, "vilarejo_do_vale", Tipo.ASSENTAMENTO,
                new Ponto(-11_000, -14_000), Canon.ORIGINAL_COMPATIVEL, 500);
        adicionar(lista, "vilarejo_do_planalto", Tipo.ASSENTAMENTO,
                new Ponto(-6_000, -2_000), Canon.ORIGINAL_COMPATIVEL, 500);
        adicionar(lista, "vilarejo_do_leste", Tipo.ASSENTAMENTO,
                new Ponto(24_000, 2_000), Canon.ORIGINAL_COMPATIVEL, 500);
        adicionar(lista, "vilarejo_pesqueiro", Tipo.ASSENTAMENTO,
                new Ponto(14_000, 26_000), Canon.ORIGINAL_COMPATIVEL, 500);
        adicionar(lista, "posto_da_serra", Tipo.ASSENTAMENTO,
                new Ponto(-22_000, -22_000), Canon.ORIGINAL_COMPATIVEL, 500);
        adicionar(lista, "vilarejo_do_brejo", Tipo.ASSENTAMENTO,
                new Ponto(-3_000, 17_000), Canon.ORIGINAL_COMPATIVEL, 500);
        adicionar(lista, "posto_do_norte", Tipo.ASSENTAMENTO,
                new Ponto(7_000, -29_000), Canon.ORIGINAL_COMPATIVEL, 500);

        // --- pontes grandes: onde estrada principal cruza rio maior ---------
        adicionar(lista, "ponte_do_antokiba", Tipo.PONTE_GRANDE,
                new Ponto(-15_500, 9_000), Canon.ORIGINAL_COMPATIVEL, 600);
        adicionar(lista, "ponte_do_masadora", Tipo.PONTE_GRANDE,
                new Ponto(8_000, 11_000), Canon.ORIGINAL_COMPATIVEL, 600);
        adicionar(lista, "ponte_do_sul", Tipo.PONTE_GRANDE,
                new Ponto(-5_000, 23_000), Canon.ORIGINAL_COMPATIVEL, 600);

        return List.copyOf(lista);
    }

    /**
     * Onde um rio cruza uma cordilheira, ha queda d'agua.
     *
     * <p>DERIVADO, e nao digitado: mover um rio ou uma cadeia move a cachoeira
     * junto. Coordenada digitada viraria a segunda fonte da mesma verdade, e o
     * sintoma seria uma cachoeira num descampado.
     */
    private static List<Landmark> cachoeiras() {
        List<Landmark> achadas = new ArrayList<>();
        int n = 1;
        for (var rio : GreedIslandConstants.RIOS) {
            for (var no : rio.curso()) {
                double aCrista = com.darkcontinent.nenfoundation.enemy.greedisland.layout
                        .GreedIslandRidgeField.distanciaAteACrista(no.x(), no.z());
                if (aCrista < 2_600) {
                    achadas.add(new Landmark("cachoeira_" + n++, Tipo.CACHOEIRA,
                            no, regiaoDe(no), Canon.ORIGINAL_COMPATIVEL, 700));
                    break;
                }
            }
        }
        return achadas;
    }

    private static void adicionar(List<Landmark> lista, String id, Tipo tipo,
            Ponto p, Canon canon, int raio) {
        lista.add(new Landmark(id, tipo, p, regiaoDe(p), canon, raio));
    }

    private static String regiaoDe(Ponto p) {
        return RegistroDeRegioes.em(p.x(), p.z()).id();
    }

    private static Ponto ponto(String cidade) {
        var c = GreedIslandConstants.cidade(cidade).orElseThrow();
        return new Ponto(c.x(), c.z());
    }

    /** Todos os landmarks grandes. */
    public static List<Landmark> todos() {
        return LANDMARKS;
    }

    /** Um landmark pelo id. */
    public static Optional<Landmark> porId(String id) {
        return LANDMARKS.stream().filter(l -> l.id().equals(id)).findFirst();
    }

    /** O landmark mais proximo de um ponto. */
    public static Optional<Landmark> maisProximo(double x, double z) {
        return LANDMARKS.stream().min((a, b) -> Double.compare(
                Math.hypot(x - a.ancora().x(), z - a.ancora().z()),
                Math.hypot(x - b.ancora().x(), z - b.ancora().z())));
    }
}
