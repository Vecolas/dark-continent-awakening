package com.darkcontinent.nenfoundation.enemy.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Barramento runtime de ruído, limitado e isolado por Level. */
public final class EnemyHearingBus {
    private static final int MAX_SIGNALS_PER_LEVEL = 256;
    private static final Map<Level, List<EnemyHearingSignal>> SIGNALS = new WeakHashMap<>();

    private EnemyHearingBus() { }

    public static synchronized void emit(Entity source, double radius) {
        if (source == null || source.level().isClientSide) return;
        emit(source.level(), source.position(), source.getUUID(), radius);
    }

    public static synchronized void emit(Level level, Vec3 position, UUID sourceId, double radius) {
        if (level == null || level.isClientSide || position == null || radius <= 0.0) return;
        List<EnemyHearingSignal> signals = SIGNALS.computeIfAbsent(level, ignored -> new ArrayList<>());
        signals.add(new EnemyHearingSignal(level, position, sourceId, radius, level.getGameTime()));
        if (signals.size() > MAX_SIGNALS_PER_LEVEL) {
            signals.subList(0, signals.size() - MAX_SIGNALS_PER_LEVEL).clear();
        }
    }

    public static synchronized List<EnemyHearingSignal> recent(Level level, long afterTick) {
        List<EnemyHearingSignal> signals = SIGNALS.get(level);
        if (signals == null) return List.of();
        long cutoff = afterTick == Long.MIN_VALUE ? Long.MIN_VALUE : afterTick - 200;
        signals.removeIf(signal -> signal.gameTime() < cutoff);
        return List.copyOf(signals.stream().filter(signal -> signal.gameTime() >= afterTick).toList());
    }

    public static synchronized void clear(Level level) {
        SIGNALS.remove(level);
    }
}
