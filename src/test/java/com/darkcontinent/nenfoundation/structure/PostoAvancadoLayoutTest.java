package com.darkcontinent.nenfoundation.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PostoAvancadoLayoutTest {
    @Test
    void layoutDeReferenciaMantemSilhuetaEModulosDentroDoFootprint() {
        PostoAvancadoLayout layout = PostoAvancadoLayout.referencia();
        assertEquals(37, layout.footprint());
        assertEquals(14, layout.towerHeight());
        assertEquals(8, layout.modulos().size());
        assertTrue(layout.modulos().stream().allMatch(modulo -> modulo.cabeNo(layout.footprint())));
    }

    @Test
    void moduloForaDoFootprintERejeitadoAntesDoWorldgen() {
        assertThrows(IllegalArgumentException.class,
                () -> new PostoAvancadoLayout(37, 4, 9, 16,
                        java.util.List.of(new PostoAvancadoLayout.Modulo("torre", 18, -18, 2, 2))));
    }

    @Test
    void colecaoDeModulosNaoFicaMutavelDepoisDaPublicacao() {
        PostoAvancadoLayout layout = PostoAvancadoLayout.referencia();
        assertThrows(UnsupportedOperationException.class, () -> layout.modulos().clear());
    }
}
