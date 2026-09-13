package com.darkcontinent.nenfoundation.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PostoAvancadoCommandsTest {
    @Test
    void comandoAceitaAsQuatroRotacoesDoPlacement() {
        assertEquals(PostoAvancadoPlacementTransform.Rotacao.NONE,
                PostoAvancadoCommands.parseRotation("none"));
        assertEquals(PostoAvancadoPlacementTransform.Rotacao.CLOCKWISE_90,
                PostoAvancadoCommands.parseRotation("clockwise_90"));
        assertEquals(PostoAvancadoPlacementTransform.Rotacao.CLOCKWISE_180,
                PostoAvancadoCommands.parseRotation("clockwise_180"));
        assertEquals(PostoAvancadoPlacementTransform.Rotacao.COUNTERCLOCKWISE_90,
                PostoAvancadoCommands.parseRotation("counterclockwise_90"));
    }

    @Test
    void comandoRecusaRotacaoDesconhecida() {
        assertNull(PostoAvancadoCommands.parseRotation("medieval"));
    }
}
