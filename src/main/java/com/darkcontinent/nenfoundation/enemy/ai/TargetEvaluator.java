package com.darkcontinent.nenfoundation.enemy.ai;

import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.api.FactionRelation;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;

/**
 * Guarda única das recusas de alvo: vida, dimensão, alcance, território e facção.
 * A classe recebe somente a leitura do servidor; nunca aceita decisão do cliente.
 */
public final class TargetEvaluator {
    private final EnemyFaction ownFaction;
    private final String dimension;
    private final double maxDistance;
    private final double visionCosine;
    private final FactionRelations relations;

    public TargetEvaluator(EnemyFaction ownFaction, String dimension, double maxDistance,
            double visionHalfAngleDegrees, FactionRelations relations) {
        if (ownFaction == null || dimension == null || dimension.isBlank()
                || !Double.isFinite(maxDistance) || maxDistance <= 0.0D
                || !Double.isFinite(visionHalfAngleDegrees)
                || visionHalfAngleDegrees <= 0.0D || visionHalfAngleDegrees > 180.0D) {
            throw new IllegalArgumentException("configuracao de alvo invalida");
        }
        this.ownFaction = ownFaction;
        this.dimension = dimension;
        this.maxDistance = maxDistance;
        this.visionCosine = Math.cos(Math.toRadians(visionHalfAngleDegrees));
        this.relations = java.util.Objects.requireNonNull(relations, "relacoes ausentes");
    }

    public TargetEvaluation evaluate(PerceptionSnapshot snapshot) {
        if (snapshot == null) throw new NullPointerException("snapshot ausente");
        boolean valid = snapshot.targetAlive()
                && dimension.equals(snapshot.dimension())
                && snapshot.distance() <= maxDistance
                && snapshot.targetInTerritory()
                && hostilOuPresa(snapshot.targetFaction());
        if (!valid) return new TargetEvaluation(snapshot.targetId(), false, false, false, false);
        boolean visible = snapshot.lineOfSight() && snapshot.forwardDot() >= visionCosine;
        return new TargetEvaluation(snapshot.targetId(), true, visible,
                snapshot.audible(), snapshot.targetRetreating());
    }

    private boolean hostilOuPresa(EnemyFaction target) {
        FactionRelation relation = relations.relation(ownFaction, target);
        return relation == FactionRelation.HOSTILE || relation == FactionRelation.PREY;
    }
}
