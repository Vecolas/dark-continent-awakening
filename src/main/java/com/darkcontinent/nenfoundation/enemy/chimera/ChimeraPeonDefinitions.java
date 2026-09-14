package com.darkcontinent.nenfoundation.enemy.chimera;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Os TRES peoes iniciais da issue #121: Crab Heavy, Bat Scout e Wolf Runner.
 *
 * <p><b>Tres moldes, tres papeis DISTINTOS -- e nao tres recolores.</b> A issue e
 * explicita sobre isso, e a razao e de leitura: um bando de tres bichos parecidos
 * com funcoes parecidas nao ensina nada ao jogador. Frontliner que segura,
 * batedor que avisa e corredor que flanqueia sao tres respostas diferentes, e e
 * por isso que os traits garantidos de cada um nao se repetem.</p>
 *
 * <p><b>Nenhum nasce com Nen.</b> {@link ChimeraIdentity#nova} entrega
 * DORMANT, e isso e a decisao da #121 escrita onde o nascimento acontece: dar
 * Nen ativo a todo peon apagaria a leitura de que Nen e raro e caro, e nao
 * apareceria como erro nenhum.</p>
 */
public final class ChimeraPeonDefinitions {

    private ChimeraPeonDefinitions() { }

    /**
     * CRAB HEAVY -- o que segura a linha.
     *
     * <p>Carapaca orientada: a armadura vale de FRENTE, e o encontro e sobre
     * chegar por tras. {@code ARMOR_PLATE} e {@code STRENGTH} sao garantidos
     * porque sao o mob; sem os dois ele vira um peon comum mais gordo, e a
     * resposta que ele ensina desaparece.</p>
     *
     * <p>Ele tambem AGARRA, e por isso reusara o {@code GrabController}
     * compartilhado -- nao um agarrao proprio.</p>
     */
    public static ChimeraDefinition crabHeavy() {
        return new ChimeraDefinition(NenFoundation.id("crab_heavy"), ChimeraRank.PEON,
                ChimeraMorphology.HEAVY,
                Set.of(ChimeraTrait.ARMOR_PLATE, ChimeraTrait.STRENGTH, ChimeraTrait.CLAWS,
                        ChimeraTrait.AQUATIC),
                Set.of(ChimeraTrait.ARMOR_PLATE, ChimeraTrait.STRENGTH),
                SquadRole.FRONTLINER);
    }

    /**
     * BAT SCOUT -- o que avisa.
     *
     * <p>{@code WINGS} e {@code NIGHT_VISION} garantidos: ele existe para VER
     * primeiro e voltar. Um batedor que nao enxerga a noite seria um batedor que
     * so serve de dia, e a colonia ficaria cega metade do tempo -- sem que nada
     * indicasse o motivo.</p>
     *
     * <p>Ele e SCOUT no bando e nao frontliner: alista-lo na frente o mataria
     * antes do relatorio, e o relatorio e o valor dele.</p>
     */
    public static ChimeraDefinition batScout() {
        return new ChimeraDefinition(NenFoundation.id("bat_scout"), ChimeraRank.PEON,
                ChimeraMorphology.WINGED,
                Set.of(ChimeraTrait.WINGS, ChimeraTrait.NIGHT_VISION, ChimeraTrait.SPEED,
                        ChimeraTrait.VENOM),
                Set.of(ChimeraTrait.WINGS, ChimeraTrait.NIGHT_VISION),
                SquadRole.SCOUT);
    }

    /**
     * WOLF RUNNER -- o que flanqueia.
     *
     * <p>{@code SPEED} e {@code LEAP} garantidos: ele nao vence pelo golpe, vence
     * por CHEGAR. A memoria de rastro com expiracao (issue #121) e o que o faz
     * perseguir quem fugiu sem virar perseguicao eterna -- e o prazo dessa
     * memoria e o que separa "tenso" de "injusto".</p>
     */
    public static ChimeraDefinition wolfRunner() {
        return new ChimeraDefinition(NenFoundation.id("wolf_runner"), ChimeraRank.PEON,
                ChimeraMorphology.QUADRUPED,
                Set.of(ChimeraTrait.SPEED, ChimeraTrait.LEAP, ChimeraTrait.CLAWS,
                        ChimeraTrait.TAIL),
                Set.of(ChimeraTrait.SPEED, ChimeraTrait.LEAP),
                SquadRole.FLANKER);
    }

    /**
     * Os tres, por id.
     *
     * <p>ACRESCENTE O MOLDE AQUI NO MESMO PR QUE O REGISTRA. Fora desta lista ele
     * fica sem portao, e nada acusa -- e a mesma armadilha que custou meses ao
     * foxbear na issue #266.</p>
     */
    public static Map<String, ChimeraDefinition> todos() {
        Map<String, ChimeraDefinition> mapa = new LinkedHashMap<>();
        mapa.put("crab_heavy", crabHeavy());
        mapa.put("bat_scout", batScout());
        mapa.put("wolf_runner", wolfRunner());
        return Map.copyOf(mapa);
    }
}
