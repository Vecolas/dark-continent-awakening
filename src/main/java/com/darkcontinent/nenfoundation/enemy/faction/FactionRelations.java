package com.darkcontinent.nenfoundation.enemy.faction;

import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.FactionRelation;
import java.util.EnumMap;
import java.util.Map;

/** Relacoes explicitas entre faccoes; nao confunde fauna territorial com hostilidade global. */
public final class FactionRelations {
    private final Map<EnemyFaction, Map<EnemyFaction, FactionRelation>> relations;

    public FactionRelations(Map<EnemyFaction, Map<EnemyFaction, FactionRelation>> relations) {
        EnumMap<EnemyFaction, Map<EnemyFaction, FactionRelation>> copy = new EnumMap<>(EnemyFaction.class);
        if (relations != null) {
            relations.forEach((from, values) -> copy.put(from, Map.copyOf(values)));
        }
        this.relations = Map.copyOf(copy);
    }

    public FactionRelation relation(EnemyFaction from, EnemyFaction to) {
        if (from == null || to == null) throw new NullPointerException("faccao ausente");
        if (from == to) return FactionRelation.ALLY;
        return relations.getOrDefault(from, Map.of()).getOrDefault(to, FactionRelation.NEUTRAL);
    }

    public static FactionRelations padrao() {
        EnumMap<EnemyFaction, Map<EnemyFaction, FactionRelation>> values = new EnumMap<>(EnemyFaction.class);
        values.put(EnemyFaction.CHIMERA_ANT, Map.of(
                EnemyFaction.CIVILIAN, FactionRelation.PREY,
                EnemyFaction.HUNTER_ASSOCIATION, FactionRelation.HOSTILE,
                EnemyFaction.CHIMERA_ANT_REBEL, FactionRelation.HOSTILE));
        values.put(EnemyFaction.CHIMERA_ANT_REBEL, Map.of(
                EnemyFaction.CHIMERA_ANT, FactionRelation.HOSTILE,
                EnemyFaction.CIVILIAN, FactionRelation.NEUTRAL));
        values.put(EnemyFaction.WILDLIFE, Map.of(
                EnemyFaction.CIVILIAN, FactionRelation.NEUTRAL,
                EnemyFaction.HUNTER_ASSOCIATION, FactionRelation.HOSTILE));
        values.put(EnemyFaction.MAGICAL_BEAST, Map.of(
                EnemyFaction.HUNTER_ASSOCIATION, FactionRelation.HOSTILE));
        values.put(EnemyFaction.GREED_ISLAND_MONSTER, Map.of(
                EnemyFaction.HUNTER_ASSOCIATION, FactionRelation.HOSTILE));
        values.put(EnemyFaction.CRIMINAL, Map.of(
                EnemyFaction.HUNTER_ASSOCIATION, FactionRelation.HOSTILE));
        values.put(EnemyFaction.MAFIA, Map.of(
                EnemyFaction.HUNTER_ASSOCIATION, FactionRelation.HOSTILE));
        return new FactionRelations(values);
    }
}
