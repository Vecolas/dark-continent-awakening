package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import org.junit.jupiter.api.Test;

class EnemyBrainTest {
    private static final AwarenessInput VISIBLE_TERRITORY = new AwarenessInput(true, false, true, false, false, 0, false, false);

    @Test
    void avisaAntesDeEngajarERespeitaDuracaoConfigurada() {
        EnemyBrain brain = new EnemyBrain(new AwarenessTuning(2, 20));
        assertEquals(EnemyAwarenessState.WARN, brain.tick(VISIBLE_TERRITORY));
        assertEquals(EnemyAwarenessState.WARN, brain.tick(VISIBLE_TERRITORY));
        assertEquals(EnemyAwarenessState.ENGAGE, brain.tick(VISIBLE_TERRITORY));
    }

    @Test
    void audicaoInvestigaEPerdaDeAlvoRetornaParaCasa() {
        EnemyBrain brain = new EnemyBrain(new AwarenessTuning(1, 2));
        assertEquals(EnemyAwarenessState.INVESTIGATE,
                brain.tick(new AwarenessInput(false, true, false, false, false, 0, false, false)));
        assertEquals(EnemyAwarenessState.RETURN_HOME,
                brain.tick(new AwarenessInput(false, false, false, true, true, 0, false, false)));
    }

    @Test
    void oportunidadeDeEmboscadaERecuoDeVidaBaixa() {
        EnemyBrain brain = new EnemyBrain(new AwarenessTuning(1, 2));
        assertEquals(EnemyAwarenessState.AMBUSH,
                brain.tick(new AwarenessInput(false, false, false, false, false, 0, true, false)));
        assertEquals(EnemyAwarenessState.ENGAGE,
                brain.tick(new AwarenessInput(true, false, true, false, false, 0, false, false)));
        assertEquals(EnemyAwarenessState.FLEE,
                brain.tick(new AwarenessInput(true, false, true, false, false, 0, false, true)));
    }
}
