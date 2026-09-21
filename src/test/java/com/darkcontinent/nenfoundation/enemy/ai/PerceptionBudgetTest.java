package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PerceptionBudgetTest {
    @Test
    void naoVarreAEntidadeEmTodoTick() {
        PerceptionBudget budget = new PerceptionBudget();
        int scans = 0;
        for (int tick = 0; tick < 100; tick++) {
            if (budget.due(tick, 5)) scans++;
        }
        assertEquals(20, scans);
        assertFalse(budget.due(99, 5));
        assertTrue(budget.due(100, 5));
    }
}
