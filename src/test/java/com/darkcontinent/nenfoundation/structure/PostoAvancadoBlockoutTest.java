package com.darkcontinent.nenfoundation.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

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

    @Test
    void blockoutPodeSerAplicadoPorUmResolvedorSemAcoplarRegistry() {
        Map<PostoAvancadoBlockout.Material, String> materiais = new EnumMap<>(
                PostoAvancadoBlockout.Material.class);
        for (PostoAvancadoBlockout.Material material : PostoAvancadoBlockout.Material.values()) {
            materiais.put(material, material.name().toLowerCase());
        }
        Map<String, Integer> contagem = new HashMap<>();

        PostoAvancadoBlockout.aplicar(materiais::get, (posicao, material) ->
                contagem.merge(material, 1, Integer::sum));

        assertEquals(PostoAvancadoBlockout.gerar().size(),
                contagem.values().stream().mapToInt(Integer::intValue).sum());
        assertTrue(contagem.getOrDefault("metal", 0) > 0);
        assertTrue(contagem.getOrDefault("canvas", 0) > 0);
    }
}
