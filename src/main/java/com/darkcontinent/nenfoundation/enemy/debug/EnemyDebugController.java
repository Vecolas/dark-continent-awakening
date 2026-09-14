package com.darkcontinent.nenfoundation.enemy.debug;

import com.darkcontinent.nenfoundation.enemy.api.HxHEnemy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Mob;

/** Guarda de debug por entidade; dados transitórios e server-side. */
public final class EnemyDebugController {
    private static final Map<UUID, EnemyDebugState> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ORIGINAL_NO_AI = new ConcurrentHashMap<>();

    private EnemyDebugController() { }

    public static EnemyDebugState state(UUID id) {
        return STATES.getOrDefault(id, EnemyDebugState.DEFAULT);
    }

    public static EnemyDebugState state(HxHEnemy enemy) {
        if (!(enemy instanceof Mob mob)) throw new IllegalArgumentException("inimigo sem entidade");
        return state(mob.getUUID());
    }

    public static EnemyDebugState freeze(Mob mob, boolean frozen) {
        requireEnemy(mob);
        if (frozen) ORIGINAL_NO_AI.putIfAbsent(mob.getUUID(), mob.isNoAi());
        else {
            Boolean previous = ORIGINAL_NO_AI.remove(mob.getUUID());
            if (previous != null) mob.setNoAi(previous);
        }
        if (frozen) mob.setNoAi(true);
        EnemyDebugState next = state(mob.getUUID()).withFrozen(frozen);
        STATES.put(mob.getUUID(), next);
        if (!frozen && next.equals(EnemyDebugState.DEFAULT)) STATES.remove(mob.getUUID());
        return next;
    }

    public static EnemyDebugState toggleHitboxes(Mob mob) {
        requireEnemy(mob);
        EnemyDebugState current = state(mob.getUUID());
        EnemyDebugState next = current.withHitboxes(!current.showHitboxes());
        STATES.put(mob.getUUID(), next);
        return next;
    }

    public static EnemyDebugState toggleWeakPoints(Mob mob) {
        requireEnemy(mob);
        EnemyDebugState current = state(mob.getUUID());
        EnemyDebugState next = current.withWeakPoints(!current.showWeakPoints());
        STATES.put(mob.getUUID(), next);
        return next;
    }

    public static EnemyDebugState animation(Mob mob, String animation) {
        requireEnemy(mob);
        if (animation == null || animation.isBlank()) throw new IllegalArgumentException("animacao vazia");
        EnemyDebugState next = state(mob.getUUID()).withAnimation(animation);
        STATES.put(mob.getUUID(), next);
        return next;
    }

    public static void clear(UUID id, Mob mob) {
        Boolean previous = ORIGINAL_NO_AI.remove(id);
        if (previous != null && mob != null) mob.setNoAi(previous);
        STATES.remove(id);
    }

    private static void requireEnemy(Mob mob) {
        if (!(mob instanceof HxHEnemy)) throw new IllegalArgumentException("entidade nao e inimigo HxH");
    }
}
