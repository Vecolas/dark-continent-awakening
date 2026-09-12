package com.darkcontinent.nenfoundation.enemy.combat;

/** Relogio autoritativo da janela de ataque; animacao apenas representa este estado. */
public final class AttackTimeline {
    private AttackDefinition definition;
    private AttackPhase phase = AttackPhase.IDLE;
    private int remainingTicks;

    public AttackPhase phase() { return phase; }
    public int remainingTicks() { return remainingTicks; }
    public boolean active() { return phase == AttackPhase.ACTIVE; }
    public boolean finished() { return phase == AttackPhase.COMPLETE; }

    public void start(AttackDefinition definition) {
        if (definition == null || phase != AttackPhase.IDLE && phase != AttackPhase.COMPLETE) {
            throw new IllegalStateException("ataque nao pode iniciar agora");
        }
        this.definition = definition;
        this.phase = AttackPhase.WINDUP;
        this.remainingTicks = definition.windupTicks();
    }

    public void tick() {
        if (phase == AttackPhase.IDLE || phase == AttackPhase.COMPLETE) return;
        remainingTicks--;
        if (remainingTicks > 0) return;
        switch (phase) {
            case WINDUP -> { phase = AttackPhase.ACTIVE; remainingTicks = definition.activeTicks(); }
            case ACTIVE -> { phase = AttackPhase.RECOVERY; remainingTicks = definition.recoveryTicks(); }
            case RECOVERY -> { phase = AttackPhase.COMPLETE; remainingTicks = 0; }
            default -> { }
        }
    }

    public boolean canInterrupt() {
        return switch (phase) {
            case WINDUP -> definition != null && definition.interruptibleWindup();
            case ACTIVE -> definition != null && definition.interruptibleActive();
            case RECOVERY -> definition != null && definition.interruptibleRecovery();
            default -> false;
        };
    }

    public void cancel() {
        if (!canInterrupt()) throw new IllegalStateException("fase nao interrompivel");
        phase = AttackPhase.COMPLETE;
        remainingTicks = 0;
    }
}
