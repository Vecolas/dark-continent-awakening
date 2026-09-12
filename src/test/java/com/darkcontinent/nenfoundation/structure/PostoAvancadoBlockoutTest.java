package com.darkcontinent.nenfoundation.structure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PostoAvancadoBlockoutTest {
    @Test
    void blockoutUsaPaletaIndustrialEContemTorreAberta() {
        var blocos = PostoAvancadoBlockout.gerar();
        assertTrue(blocos.size() > 500, "blockout pequeno demais para os modulos V2");
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.METAL));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.CONCRETE));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.CANVAS));
        assertFalse(blocos.stream().anyMatch(p -> p.material().name().contains("DARK_OAK")));
    }

    @Test
    void blockoutRespeitaFootprintEAlturaDaTorre() {
        var blocos = PostoAvancadoBlockout.gerar();
        assertTrue(blocos.stream().allMatch(p -> Math.abs(p.posicao().getX()) <= 18
                && Math.abs(p.posicao().getZ()) <= 18));
        assertTrue(blocos.stream().allMatch(p -> p.posicao().getY() >= 0 && p.posicao().getY() <= 13));
    }
}
