package com.darkcontinent.nenfoundation.enemy.ai.squad;

import java.util.UUID;

public record SquadOrder(UUID target, boolean retreat, boolean regroup, SquadRole role) {
    public SquadOrder {
        if (role == null) throw new NullPointerException("papel ausente");
        if (retreat && target != null) throw new IllegalArgumentException("retreat nao tem alvo");
    }
}
