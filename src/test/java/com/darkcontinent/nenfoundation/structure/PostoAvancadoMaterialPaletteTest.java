package com.darkcontinent.nenfoundation.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PostoAvancadoMaterialPaletteTest {
    @Test
    void paletaVanillaCobreTodosOsMateriaisDoBlockout() {
        var palette = PostoAvancadoMaterialPalette.vanilla();

        assertEquals(PostoAvancadoBlockout.Material.values().length, palette.size());
        for (PostoAvancadoBlockout.Material material : PostoAvancadoBlockout.Material.values()) {
            assertTrue(palette.containsKey(material));
            assertFalse(palette.get(material).getPath().isBlank());
        }
    }

    @Test
    void paletaInicialMantemLeituraIndustrialNaoMedieval() {
        var palette = PostoAvancadoMaterialPalette.vanilla();

        assertEquals("smooth_stone", palette.get(PostoAvancadoBlockout.Material.FOUNDATION).getPath());
        assertEquals("iron_block", palette.get(PostoAvancadoBlockout.Material.METAL).getPath());
        assertEquals("light_gray_wool", palette.get(PostoAvancadoBlockout.Material.CANVAS).getPath());
        assertFalse(palette.values().stream().anyMatch(id -> id.getPath().contains("dark_oak")));
        assertFalse(palette.values().stream().anyMatch(id -> id.getPath().contains("stone_bricks")));
    }
}
