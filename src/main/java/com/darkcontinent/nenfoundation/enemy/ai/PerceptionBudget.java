package com.darkcontinent.nenfoundation.enemy.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.Entity;

/** Agenda e limite de candidatos da percepcao; impede scan caro a cada tick. */
public final class PerceptionBudget {
    private long lastScanTick = Long.MIN_VALUE;

    public boolean due(long currentTick, int intervalTicks) {
        if (intervalTicks < 1) throw new IllegalArgumentException("intervalo invalido");
        if (lastScanTick != Long.MIN_VALUE && currentTick - lastScanTick < intervalTicks) return false;
        lastScanTick = currentTick;
        return true;
    }

    public static <T extends Entity> List<T> nearest(List<T> candidates, Entity observer,
            int maximum) {
        if (maximum < 1) throw new IllegalArgumentException("limite invalido");
        List<T> copy = new ArrayList<>(candidates);
        copy.sort(Comparator.comparingDouble(observer::distanceToSqr));
        return List.copyOf(copy.subList(0, Math.min(maximum, copy.size())));
    }
}
