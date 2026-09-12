package com.darkcontinent.nenfoundation.enemy.ai.squad;

/** Decide ordens simples de pack sem impor movimento ou dano as entidades. */
public final class SquadController {
    private SquadRole role;

    public SquadController(SquadRole role) {
        if (role == null) throw new NullPointerException("papel ausente");
        this.role = role;
    }

    public SquadRole role() { return role; }

    public SquadOrder update(SquadInput input) {
        if (input == null) throw new NullPointerException("leitura da squad ausente");
        if (!input.leaderAlive() && role == SquadRole.LEADER) role = SquadRole.FRONTLINER;
        if (input.targetRetreating() || (input.memberInjured() && !input.leaderAlive())) {
            return new SquadOrder(null, true, true, role);
        }
        if (!input.targetVisible() || input.target() == null) {
            return new SquadOrder(null, false, true, role);
        }
        return new SquadOrder(input.target(), false, false, role);
    }
}
