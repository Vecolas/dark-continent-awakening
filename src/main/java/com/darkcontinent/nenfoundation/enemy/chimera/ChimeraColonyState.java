package com.darkcontinent.nenfoundation.enemy.chimera;

import java.util.Map;
import java.util.UUID;

/** Estado compacto que podera ser projetado em SavedData na integracao. */
public record ChimeraColonyState(UUID colonyId, UUID queenId, ChimeraColonyStage stage,
        int foodScore, Map<String, Integer> genePool, int alertLevel, int nenAwakenedCount) {
    public ChimeraColonyState {
        if (colonyId == null || queenId == null || stage == null || foodScore < 0
                || genePool == null || genePool.values().stream().anyMatch(v -> v == null || v < 0)
                || alertLevel < 0 || alertLevel > 4 || nenAwakenedCount < 0) {
            throw new IllegalArgumentException("estado de colonia invalido");
        }
        genePool = Map.copyOf(genePool);
    }

    public ChimeraColonyState alimentar(String species, int amount) {
        if (species == null || species.isBlank() || amount <= 0) throw new IllegalArgumentException("presa invalida");
        java.util.HashMap<String, Integer> genes = new java.util.HashMap<>(genePool);
        genes.merge(species, amount, Integer::sum);
        return new ChimeraColonyState(colonyId, queenId, stage, foodScore + amount, genes, alertLevel, nenAwakenedCount);
    }

    public ChimeraColonyState elevarAlerta() {
        return new ChimeraColonyState(colonyId, queenId, stage, foodScore, genePool,
                Math.min(4, alertLevel + 1), nenAwakenedCount);
    }
}
