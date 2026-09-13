package com.darkcontinent.nenfoundation.structure;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.registries.BuiltInRegistries;

/** Coloca todos os placements do posto no lado servidor. */
public final class PostoAvancadoWorldPlacer {
    private PostoAvancadoWorldPlacer() { }

    public static Resultado colocar(ServerLevel level, int centroX, int centroZ,
            PostoAvancadoPlacementTransform.Rotacao rotacao) {
        Objects.requireNonNull(level, "nivel ausente");
        var plano = PostoAvancadoPlacementPlanner.planejar(centroX, centroZ, rotacao,
                (x, z) -> level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1);
        if (plano.isEmpty()) {
            return Resultado.REJEITADO;
        }
        var palette = PostoAvancadoMaterialPalette.vanilla();
        Set<BlockPos> unicos = new HashSet<>();
        for (var placement : plano.orElseThrow().placements()) {
            BlockState estado = BuiltInRegistries.BLOCK
                    .get(palette.get(placement.material()))
                    .defaultBlockState();
            BlockPos posicao = placement.posicao();
            if (unicos.add(posicao)) {
                level.setBlock(posicao, estado, 3);
            }
        }
        return new Resultado(plano.orElseThrow().placements().size(), unicos.size(),
                plano.orElseThrow().origem(), plano.orElseThrow().terreno().diferenca());
    }

    public record Resultado(int placements, int blocosUnicos, BlockPos origem, int variacaoTerreno) {
        private static final Resultado REJEITADO = new Resultado(0, 0, BlockPos.ZERO, -1);

        public Resultado {
            origem = origem.immutable();
        }

        public boolean rejeitado() {
            return variacaoTerreno < 0;
        }
    }
}
