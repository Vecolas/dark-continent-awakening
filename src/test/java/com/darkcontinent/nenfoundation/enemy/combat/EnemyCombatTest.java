package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EnemyCombatTest {
    private static AttackDefinition charge() {
        return new AttackDefinition("charge", 2, 2, 3, 10.0F, 1.5F, true, false, true);
    }

    @Test
    void timelineMantemWindupActiveERecoverySeparados() {
        AttackTimeline timeline = new AttackTimeline();
        timeline.start(charge());
        assertEquals(AttackPhase.WINDUP, timeline.phase());
        timeline.tick();
        assertEquals(AttackPhase.WINDUP, timeline.phase());
        timeline.tick();
        assertEquals(AttackPhase.ACTIVE, timeline.phase());
        timeline.tick();
        assertEquals(AttackPhase.ACTIVE, timeline.phase());
        timeline.tick();
        assertEquals(AttackPhase.RECOVERY, timeline.phase());
        timeline.tick(); timeline.tick(); timeline.tick();
        assertEquals(AttackPhase.COMPLETE, timeline.phase());
    }

    @Test
    void danoDoWeakPointEAplicadoSomenteQuandoHabilitado() {
        WeakPointRegistry registry = new WeakPointRegistry(Map.of(
                "forehead", new WeakPoint("forehead", "head", 4.0F, true),
                "scar", new WeakPoint("scar", "torso", 3.0F, false)));
        assertEquals(40.0F, registry.damage(10.0F, "forehead"));
        assertEquals(10.0F, registry.damage(10.0F, "scar"));
        assertEquals(10.0F, registry.damage(10.0F, "unknown"));
    }

    @Test
    void ataquesInvalidosEValoresNaoNumericosSaoRejeitados() {
        assertThrows(IllegalArgumentException.class,
                () -> new AttackDefinition("", 1, 1, 1, 1, 0, true, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new AttackDefinition("bad", 1, 1, 1, Float.NaN, 0, true, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WeakPoint("head", "head", 0.5F, true));
    }
}
