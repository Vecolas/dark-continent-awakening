package com.darkcontinent.nenfoundation.enemy.ai;

/** Intervalos explícitos para impedir scan caro por mob a cada tick. */
public record PerceptionBudget(int normalIntervalTicks, int expensiveIntervalTicks) {
    public PerceptionBudget {
        if (normalIntervalTicks < 5 || normalIntervalTicks > 10
                || expensiveIntervalTicks < 20 || expensiveIntervalTicks > 40
                || expensiveIntervalTicks < normalIntervalTicks) {
            throw new IllegalArgumentException("orcamento de percepcao invalido");
        }
    }
}
