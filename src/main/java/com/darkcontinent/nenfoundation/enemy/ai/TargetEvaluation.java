package com.darkcontinent.nenfoundation.enemy.ai;

import com.darkcontinent.nenfoundation.enemy.api.FactionRelation;

/** Explicacao completa da decisao de alvo; recusa nunca vira booleano sem motivo. */
public record TargetEvaluation(FactionRelation relation, boolean sameDimension,
        boolean inRange, boolean inTerritory, boolean accepted) {
    public String rejectionReason() {
        if (accepted) return "accepted";
        if (!sameDimension) return "wrong_dimension";
        if (!inRange) return "out_of_range";
        if (!inTerritory) return "outside_territory";
        return "faction_" + relation.name().toLowerCase(java.util.Locale.ROOT);
    }
}
