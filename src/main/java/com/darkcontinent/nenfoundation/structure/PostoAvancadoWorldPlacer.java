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
        if (!level.getBiome(new BlockPos(centroX, level.getMinBuildHeight(), centroZ))
                .is(PostoAvancadoBiomes.HUNTER_OUTPOST_BIOMES)) {
            return Resultado.rejeitado(Resultado.Motivo.BIOMA);
        }
        var plano = PostoAvancadoPlacementPlanner.planejar(centroX, centroZ, rotacao,
                (x, z) -> level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1);
        if (plano.isEmpty()) {
            return Resultado.rejeitado(Resultado.Motivo.TERRENO);
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
                plano.orElseThrow().origem(), plano.orElseThrow().terreno().diferenca(),
                Resultado.Motivo.ACEITO);
    }

    public record Resultado(int placements, int blocosUnicos, BlockPos origem, int variacaoTerreno,
            Motivo motivo) {
        private static Resultado rejeitado(Motivo motivo) {
            return new Resultado(0, 0, BlockPos.ZERO, -1, motivo);
        }

        public Resultado {
            origem = origem.immutable();
            Objects.requireNonNull(motivo, "motivo ausente");
        }

        public boolean rejeitado() {
            return motivo != Motivo.ACEITO;
        }

        public enum Motivo { ACEITO, BIOMA, TERRENO }
    }
}
