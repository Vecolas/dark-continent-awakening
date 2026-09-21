package com.darkcontinent.nenfoundation.enemy.ai;

import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.EnemyMetadata;
import com.darkcontinent.nenfoundation.enemy.api.FactionRelation;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;

/** Autoridade pura para faccao, alcance, dimensao e territorio. */
public final class TargetEvaluator {
    private TargetEvaluator() { }

    public static TargetEvaluation evaluate(EnemyMetadata observer, EnemyFaction target,
            double distance, double maxRange, boolean sameDimension, boolean inTerritory,
            FactionRelations relations) {
        if (observer == null || target == null || relations == null) {
            throw new NullPointerException("alvo ou relacoes ausentes");
        }
        FactionRelation relation = relations.relation(observer.faction(), target);
        boolean inRange = Double.isFinite(distance) && distance >= 0.0 && distance <= maxRange;
        boolean allowedRelation = relation == FactionRelation.HOSTILE
                || relation == FactionRelation.PREY;
        boolean allowedTerritory = !observer.territorial() || inTerritory;
        boolean accepted = sameDimension && inRange && allowedTerritory && allowedRelation;
        return new TargetEvaluation(relation, sameDimension, inRange, inTerritory, accepted);
    }
}
