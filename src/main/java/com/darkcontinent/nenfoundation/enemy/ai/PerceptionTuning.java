package com.darkcontinent.nenfoundation.enemy.ai;

/** Ajustes de sensores; o servidor lê estes valores no instante de cada varredura. */
public record PerceptionTuning(double visionRange, double visionHalfAngleDegrees,
        double hearingRange, int scanIntervalTicks, int maxCandidates, int memoryTicks,
        double territoryRadius) {
    public PerceptionTuning {
        if (!Double.isFinite(visionRange) || visionRange <= 0.0
                || !Double.isFinite(visionHalfAngleDegrees)
                || visionHalfAngleDegrees <= 0.0 || visionHalfAngleDegrees > 180.0
                || !Double.isFinite(hearingRange) || hearingRange <= 0.0
                || scanIntervalTicks < 1 || maxCandidates < 1 || memoryTicks < 0
                || !Double.isFinite(territoryRadius) || territoryRadius <= 0.0) {
            throw new IllegalArgumentException("tuning de percepcao invalido");
        }
    }
}
