package com.darkcontinent.nenfoundation.enemy.ai;

/** Numeros de comportamento injetados, em vez de congelados no motor da IA. */
public record AwarenessTuning(int warnTicks, int memoryTicks) {
    public AwarenessTuning {
        if (warnTicks < 1 || memoryTicks < 0) throw new IllegalArgumentException("tuning invalido");
    }
}
