package com.darkcontinent.nenfoundation.enemy.ai;

/** Regras deterministicas do territorio do Foxbear. */
public final class FoxbearTerritory {
    public static final double TERRITORY_RADIUS = 12.0D;
    public static final double ADVANCE_TOLERANCE = 0.5D;
    private FoxbearTerritory() { }
    public static FoxbearState stateFor(boolean playerInTerritory, boolean playerAdvanced,
                                        boolean playerRetreated, boolean targetStillInside) {
        if (!playerInTerritory || !targetStillInside) return FoxbearState.RETURN_HOME;
        if (playerAdvanced) return FoxbearState.ENGAGE;
        if (playerRetreated) return FoxbearState.RETURN_HOME;
        return FoxbearState.WARN;
    }
}
