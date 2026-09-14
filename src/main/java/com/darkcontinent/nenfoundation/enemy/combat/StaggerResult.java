package com.darkcontinent.nenfoundation.enemy.combat;

/** Resultado explícito de aplicar impacto no medidor server-side. */
public record StaggerResult(Outcome outcome, float meter, int remainingTicks) {
    public enum Outcome { IGNORED, ACCUMULATED, APPLIED }
}
