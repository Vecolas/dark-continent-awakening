package com.darkcontinent.nenfoundation.enemy.combat;

import java.util.Map;

/** Catalogo imutavel de pontos fracos de uma entidade. */
public final class WeakPointRegistry {
    private final Map<String, WeakPoint> points;

    public WeakPointRegistry(Map<String, WeakPoint> points) {
        if (points == null || points.values().stream().anyMatch(p -> p == null || !points.containsKey(p.id()))) {
            throw new IllegalArgumentException("catalogo de weak points invalido");
        }
        this.points = Map.copyOf(points);
    }

    public float multiplier(String id) {
        WeakPoint point = points.get(id);
        return point != null && point.enabled() ? point.damageMultiplier() : 1.0F;
    }

    public float damage(float baseDamage, String id) {
        if (!Float.isFinite(baseDamage) || baseDamage < 0.0F) throw new IllegalArgumentException("dano invalido");
        return baseDamage * multiplier(id);
    }

    public Map<String, WeakPoint> all() { return points; }
}
