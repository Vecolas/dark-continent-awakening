package com.darkcontinent.nenfoundation.enemy.encounter;

import java.util.Set;

/** Condicao de sucesso de encontro; matar nao e pressuposto universal. */
public record EncounterDefinition(String id, EncounterKind kind, Set<String> entityIds,
        int maxParticipants, boolean allowsLethalResolution, boolean requiresObservation) {
    public EncounterDefinition {
        if (id == null || id.isBlank() || kind == null || entityIds == null || entityIds.isEmpty()
                || maxParticipants < 1 || requiresObservation && kind == EncounterKind.NATURAL) {
            throw new IllegalArgumentException("encounter invalido");
        }
        entityIds = Set.copyOf(entityIds);
    }
}
