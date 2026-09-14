package com.darkcontinent.nenfoundation.enemy.encounter;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Quais trocas de estado de encontro sao legais -- e a lista e fechada.
 *
 * <p><b>Por que uma tabela e nao {@code if}s espalhados.</b> A transicao errada
 * de encontro nao levanta excecao: ela produz um encontro que volta de COMPLETED
 * para ACTIVE e paga a recompensa duas vezes, ou um que sai de COOLDOWN para
 * COMPLETED sem ninguem ter jogado. Nenhuma das duas aparece em log -- a primeira
 * aparece como item duplicado no bau do jogador, e a segunda como conquista que
 * ninguem lembra de ter feito.</p>
 *
 * <p><b>COMPLETED nao volta para ACTIVE. Nunca.</b> Um encontro repetivel vai de
 * COMPLETED para COOLDOWN e de COOLDOWN para ARMED -- passando, obrigatoriamente,
 * pelo estado em que nao ha entidade nenhuma. E esse desvio que garante que o
 * segundo episodio comece do zero em vez de herdar participantes e travas do
 * primeiro.</p>
 */
public final class EncounterTransitions {

    private static final Map<EncounterState, Set<EncounterState>> PERMITIDAS =
            new EnumMap<>(EncounterState.class);

    static {
        PERMITIDAS.put(EncounterState.DORMANT, EnumSet.of(EncounterState.ARMED));
        // ARMED pode ir direto a FAILED: o ancoradouro pode deixar de valer (a
        // estrutura foi destruida, a dimensao sumiu) antes de alguem chegar.
        PERMITIDAS.put(EncounterState.ARMED,
                EnumSet.of(EncounterState.ACTIVE, EncounterState.FAILED, EncounterState.DORMANT));
        PERMITIDAS.put(EncounterState.ACTIVE,
                EnumSet.of(EncounterState.COMPLETED, EncounterState.FAILED));
        // COMPLETED -> COOLDOWN e o unico caminho de volta, e ele passa por um
        // estado sem entidade nenhuma DE PROPOSITO.
        PERMITIDAS.put(EncounterState.COMPLETED, EnumSet.of(EncounterState.COOLDOWN));
        PERMITIDAS.put(EncounterState.FAILED, EnumSet.of(EncounterState.COOLDOWN));
        PERMITIDAS.put(EncounterState.COOLDOWN, EnumSet.of(EncounterState.ARMED, EncounterState.DORMANT));
    }

    private EncounterTransitions() { }

    public static boolean permitida(EncounterState de, EncounterState para) {
        if (de == null || para == null) throw new NullPointerException("estado ausente");
        return PERMITIDAS.getOrDefault(de, Set.of()).contains(para);
    }

    /**
     * Exige a transicao, com motivo na recusa.
     *
     * @throws IllegalStateException quando a troca nao e legal -- e a mensagem diz
     *         quais eram as saidas, porque recusa sem motivo produz o pior relato
     *         de bug que existe
     */
    public static void exigir(EncounterState de, EncounterState para) {
        if (!permitida(de, para)) {
            throw new IllegalStateException("transicao ilegal de encontro: " + de + " -> " + para
                    + ". Saidas legais de " + de + ": " + PERMITIDAS.getOrDefault(de, Set.of())
                    + ". Voltar a ACTIVE depois de COMPLETED pagaria a recompensa duas vezes,"
                    + " e isso aparece como item duplicado no bau, nunca como erro.");
        }
    }

    /** A tabela inteira, para portao e para documentacao. */
    public static Map<EncounterState, Set<EncounterState>> tabela() {
        return Map.copyOf(PERMITIDAS);
    }
}
