package com.darkcontinent.nenfoundation.enemy.debug;

/** Estado transitório de depuração; nunca participa de save, combate ou protocolo. */
public record EnemyDebugState(boolean aiFrozen, boolean showHitboxes,
        boolean showWeakPoints, String animationOverride) {
    public static final EnemyDebugState DEFAULT = new EnemyDebugState(false, false, false, "");

    public EnemyDebugState {
        if (animationOverride == null) throw new NullPointerException("animacao ausente");
    }

    public EnemyDebugState withFrozen(boolean value) {
        return new EnemyDebugState(value, showHitboxes, showWeakPoints, animationOverride);
    }
    public EnemyDebugState withHitboxes(boolean value) {
        return new EnemyDebugState(aiFrozen, value, showWeakPoints, animationOverride);
    }
    public EnemyDebugState withWeakPoints(boolean value) {
        return new EnemyDebugState(aiFrozen, showHitboxes, value, animationOverride);
    }
    public EnemyDebugState withAnimation(String value) {
        return new EnemyDebugState(aiFrozen, showHitboxes, showWeakPoints, value);
    }
}
