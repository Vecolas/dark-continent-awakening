package com.darkcontinent.nenfoundation.enemy.ai.squad;

import java.util.UUID;

/** Fatos ja observados pelo servidor; o controller nao faz consulta de mundo. */
public record SquadInput(UUID target, boolean leaderAlive, boolean targetVisible,
        boolean targetRetreating, boolean memberInjured) {
}
