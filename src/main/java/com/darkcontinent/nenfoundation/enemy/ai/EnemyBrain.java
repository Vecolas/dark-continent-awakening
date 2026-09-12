package com.darkcontinent.nenfoundation.enemy.ai;

import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;

/**
 * Maquina de intencao de alto nivel. Goals de movimento/ataque devem executar
 * o estado atual; esta classe nao acessa mundo nem entidades.
 */
public final class EnemyBrain {
    private final AwarenessTuning tuning;
    private EnemyAwarenessState state = EnemyAwarenessState.IDLE;
    private int stateTicks;

    public EnemyBrain(AwarenessTuning tuning) {
        this.tuning = tuning;
    }

    public EnemyAwarenessState state() { return state; }
    public int stateTicks() { return stateTicks; }

    public EnemyAwarenessState tick(AwarenessInput input) {
        if (input == null) throw new NullPointerException("leitura de awareness ausente");
        EnemyAwarenessState proximo = decidir(input);
        if (proximo != state) stateTicks = 0;
        else stateTicks++;
        state = proximo;
        return state;
    }

    public void reset() {
        state = EnemyAwarenessState.IDLE;
        stateTicks = 0;
    }

    private EnemyAwarenessState decidir(AwarenessInput i) {
        if (i.healthCritical() && (state == EnemyAwarenessState.ENGAGE
                || state == EnemyAwarenessState.STALK)) return EnemyAwarenessState.FLEE;
        return switch (state) {
            case IDLE, ROAM -> semAlvo(i);
            case SUSPICIOUS, INVESTIGATE -> i.targetLost() && i.memoryTicksRemaining() == 0
                    ? EnemyAwarenessState.RETURN_HOME
                    : i.targetVisible()
                    ? (i.targetInTerritory() ? EnemyAwarenessState.WARN : EnemyAwarenessState.STALK)
                    : (i.memoryTicksRemaining() > 0 ? EnemyAwarenessState.INVESTIGATE : EnemyAwarenessState.ROAM);
            case WARN -> i.targetRetreating() ? EnemyAwarenessState.RETURN_HOME
                    : (!i.targetVisible() ? EnemyAwarenessState.INVESTIGATE
                    : (stateTicks + 1 >= tuning.warnTicks() ? EnemyAwarenessState.ENGAGE : EnemyAwarenessState.WARN));
            case STALK -> i.ambushOpportunity() ? EnemyAwarenessState.AMBUSH
                    : (i.targetVisible() ? EnemyAwarenessState.ENGAGE : EnemyAwarenessState.INVESTIGATE);
            case AMBUSH -> i.targetVisible() ? EnemyAwarenessState.ENGAGE : EnemyAwarenessState.AMBUSH;
            case ENGAGE -> i.targetRetreating() || (i.targetLost() && i.memoryTicksRemaining() == 0)
                    ? EnemyAwarenessState.RETURN_HOME : EnemyAwarenessState.ENGAGE;
            case FLEE -> i.targetLost() ? EnemyAwarenessState.RETURN_HOME : EnemyAwarenessState.FLEE;
            case RETURN_HOME -> i.targetVisible() && i.targetInTerritory()
                    ? EnemyAwarenessState.WARN : EnemyAwarenessState.ROAM;
            case REPOSITION -> i.targetVisible() ? EnemyAwarenessState.ENGAGE : EnemyAwarenessState.INVESTIGATE;
        };
    }

    private EnemyAwarenessState semAlvo(AwarenessInput i) {
        if (i.ambushOpportunity()) return EnemyAwarenessState.AMBUSH;
        if (i.targetVisible() && i.targetInTerritory()) return EnemyAwarenessState.WARN;
        if (i.targetVisible()) return EnemyAwarenessState.SUSPICIOUS;
        if (i.targetAudible()) return EnemyAwarenessState.INVESTIGATE;
        return EnemyAwarenessState.ROAM;
    }
}
