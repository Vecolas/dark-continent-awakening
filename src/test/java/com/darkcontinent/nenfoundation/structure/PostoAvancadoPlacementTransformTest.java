package com.darkcontinent.nenfoundation.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class PostoAvancadoPlacementTransformTest {
    @Test
    void rotacaoMantemAlturaEReposicionaOrigem() {
        var local = new PostoAvancadoBlockout.Placement(
                new BlockPos(2, 5, -3), PostoAvancadoBlockout.Material.METAL);

        var resultado = PostoAvancadoPlacementTransform.aplicar(List.of(local),
                new BlockPos(10, 20, 30), PostoAvancadoPlacementTransform.Rotacao.CLOCKWISE_90);

        assertEquals(new BlockPos(13, 25, 32), resultado.get(0).posicao());
        assertSame(PostoAvancadoBlockout.Material.METAL, resultado.get(0).material());
    }

    @Test
    void rotacaoDaEstruturaContinuaDentroDeUmQuadradoDe37() {
        var original = PostoAvancadoBlockout.gerar();
        for (var rotacao : PostoAvancadoPlacementTransform.Rotacao.values()) {
            var transformado = PostoAvancadoPlacementTransform.aplicar(original, BlockPos.ZERO, rotacao);
            assertEquals(original.size(), transformado.size());
            var maxX = transformado.stream().mapToInt(p -> Math.abs(p.posicao().getX())).max().orElseThrow();
            var maxZ = transformado.stream().mapToInt(p -> Math.abs(p.posicao().getZ())).max().orElseThrow();
            assertEquals(18, maxX);
            assertEquals(18, maxZ);
        }
    }
}
