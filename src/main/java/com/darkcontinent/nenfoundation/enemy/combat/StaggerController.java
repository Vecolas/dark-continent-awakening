package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Medidor server-side independente de knockback.
 *
 * <p>O chamador fornece o impacto real do hit ou colisão. Este objeto só decide
 * limiar, resistência, decay e duração; a entidade é quem transforma o resultado
 * em {@code EnemyCombatState.STAGGERED}. Assim o empurrão nunca vira substituto
 * silencioso do atordoamento.</p>
 */
public final class StaggerController {
    private final StaggerDefinition definition;
    private float meter;
    private int remainingTicks;

    public StaggerController(StaggerDefinition definition) {
        this.definition = java.util.Objects.requireNonNull(definition, "definicao ausente");
    }

    public float meter() { return meter; }
    public int remainingTicks() { return remainingTicks; }
    public boolean active() { return remainingTicks > 0; }

    public StaggerResult apply(float rawImpact) {
        if (!Float.isFinite(rawImpact) || rawImpact <= 0.0F) {
            throw new IllegalArgumentException("impacto de stagger invalido");
        }
        if (active()) return new StaggerResult(StaggerResult.Outcome.IGNORED, meter, remainingTicks);
        float effective = Math.max(0.0F, rawImpact - definition.resistance());
        if (effective == 0.0F) return new StaggerResult(StaggerResult.Outcome.IGNORED, meter, 0);
        meter += effective;
        if (meter < definition.threshold()) {
            return new StaggerResult(StaggerResult.Outcome.ACCUMULATED, meter, 0);
        }
        meter = 0.0F;
        remainingTicks = definition.durationTicks();
        return new StaggerResult(StaggerResult.Outcome.APPLIED, meter, remainingTicks);
    }

    /** Avança a duração e aplica decay somente quando o mob não está atordoado. */
    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
            return;
        }
        meter = Math.max(0.0F, meter - definition.decayPerTick());
    }

    /** Limpeza simétrica de morte, unload e troca de dimensão. */
    public void reset() {
        meter = 0.0F;
        remainingTicks = 0;
    }
}
