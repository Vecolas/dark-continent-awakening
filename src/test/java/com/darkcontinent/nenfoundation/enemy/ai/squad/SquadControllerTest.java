package com.darkcontinent.nenfoundation.enemy.ai.squad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SquadControllerTest {
    @Test
    void liderCoordenaAlvoERebaixaPapelQuandoMorre() {
        UUID target = UUID.randomUUID();
        SquadController controller = new SquadController(SquadRole.LEADER);
        SquadOrder order = controller.update(new SquadInput(target, true, true, false, false));
        assertEquals(target, order.target());
        assertEquals(SquadRole.LEADER, order.role());

        SquadOrder afterDeath = controller.update(new SquadInput(target, false, true, false, false));
        assertEquals(SquadRole.FRONTLINER, afterDeath.role());
    }

    @Test
    void alvoRecuandoGeraRetreatSemTarget() {
        SquadController controller = new SquadController(SquadRole.FLANKER);
        SquadOrder order = controller.update(new SquadInput(UUID.randomUUID(), true, true, true, false));
        assertTrue(order.retreat());
        assertEquals(null, order.target());
    }
}
