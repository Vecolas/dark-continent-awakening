package com.darkcontinent.nenfoundation.enemy.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EnemyDebugStateTest {
    @Test
    void estadoDefaultNaoVazaGameplay() {
        assertEquals(EnemyDebugState.DEFAULT, new EnemyDebugState(false, false, false, ""));
        assertEquals("strike", EnemyDebugState.DEFAULT.withAnimation("strike").animationOverride());
    }

    @Test
    void animacaoNulaNaoEEstadoValido() {
        assertThrows(NullPointerException.class, () -> new EnemyDebugState(false, false, false, null));
    }
}
