package com.darkcontinent.nenfoundation.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PostoAvancadoPlacementPlannerTest {
    @Test
    void terrenoAceitavelDefineFundacaoNaMenorCota() {
        var plano = PostoAvancadoPlacementPlanner.planejar(100, 200,
                PostoAvancadoPlacementTransform.Rotacao.CLOCKWISE_90,
                (x, z) -> x == 80 ? 70 : 74);

        assertTrue(plano.isPresent());
        assertEquals(70, plano.orElseThrow().origem().getY());
        assertEquals(PostoAvancadoPlacementTransform.Rotacao.CLOCKWISE_90,
                plano.orElseThrow().rotacao());
        assertTrue(plano.orElseThrow().placements().stream()
                .allMatch(p -> p.posicao().getY() >= 70 && p.posicao().getY() <= 83));
    }

    @Test
    void terrenoRejeitadoNaoGeraPlacements() {
        var plano = PostoAvancadoPlacementPlanner.planejar(0, 0,
                PostoAvancadoPlacementTransform.Rotacao.NONE,
                (x, z) -> x == -20 && z == 0 ? 60 : 66);

        assertFalse(plano.isPresent());
    }
}
