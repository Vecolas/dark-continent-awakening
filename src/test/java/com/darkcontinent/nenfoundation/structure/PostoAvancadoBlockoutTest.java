package com.darkcontinent.nenfoundation.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class PostoAvancadoBlockoutTest {
    @Test
    void blockoutUsaPaletaIndustrialEContemTorreAberta() {
        var blocos = PostoAvancadoBlockout.gerar();
        assertTrue(blocos.size() > 1200, "blockout pequeno demais para os modulos V2");
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.METAL));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.CONCRETE));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.CANVAS));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.WINDOW));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.WOOD));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.GRATING));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.CRATE));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.CABLE));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.ANTENNA));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.GATE));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.SECRET_CACHE));
        assertTrue(blocos.stream().anyMatch(p -> p.material() == PostoAvancadoBlockout.Material.EMBLEM_RED));
        assertTrue(blocos.stream().anyMatch(p -> p.posicao().equals(new BlockPos(0, 4, 20))
                && p.material() == PostoAvancadoBlockout.Material.METAL),
                "o portao deve ocupar a linha frontal da cerca");
        assertFalse(blocos.stream().anyMatch(p -> p.material().name().contains("DARK_OAK")));
    }

    @Test
    void blockoutRespeitaFootprintEAlturaDaTorre() {
        var blocos = PostoAvancadoBlockout.gerar();
        assertTrue(blocos.stream().allMatch(p -> Math.abs(p.posicao().getX()) <= 20
                && Math.abs(p.posicao().getZ()) <= 20));
        assertTrue(blocos.stream().anyMatch(p -> p.posicao().getY() == 4
                && Math.abs(p.posicao().getX()) == 20
                && p.material() == PostoAvancadoBlockout.Material.FOUNDATION),
                "a cerca precisa de uma coroa superior de smooth stone");
        assertTrue(blocos.stream().allMatch(p -> p.posicao().getY() >= 0 && p.posicao().getY() <= 13));
    }

    @Test
    void blockoutNaoEmiteDuasVezesAMesmaPosicao() {
        var blocos = PostoAvancadoBlockout.gerar();
        var posicoes = blocos.stream().map(PostoAvancadoBlockout.Placement::posicao).toList();

        assertEquals(posicoes.size(), new HashSet<>(posicoes).size(),
                "cada coordenada deve ter uma decisao de material");
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
