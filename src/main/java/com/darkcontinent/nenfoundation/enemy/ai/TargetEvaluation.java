package com.darkcontinent.nenfoundation.enemy.ai;

import java.util.UUID;

/** Resultado server-side da elegibilidade de um candidato. */
public record TargetEvaluation(UUID targetId, boolean valid, boolean visible,
        boolean audible, boolean retreating) {
    public TargetEvaluation {
        if (targetId == null) throw new NullPointerException("id de alvo ausente");
        if (!valid && (visible || audible)) {
            throw new IllegalArgumentException("alvo invalido nao pode emitir sinal valido");
        }
    }
}
