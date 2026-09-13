package com.darkcontinent.nenfoundation.structure;

import java.util.Objects;
import java.util.Optional;
import java.util.function.ToIntBiFunction;
import net.minecraft.core.BlockPos;

/** Decide se e em qual cota o posto pode ser inserido no terreno. */
public final class PostoAvancadoPlacementPlanner {
    private PostoAvancadoPlacementPlanner() { }

    public static Optional<Plano> planejar(int centroX, int centroZ,
            PostoAvancadoPlacementTransform.Rotacao rotacao,
            ToIntBiFunction<Integer, Integer> alturaDoTerreno) {
        Objects.requireNonNull(rotacao, "rotacao ausente");
        var layout = PostoAvancadoLayout.referencia();
        var terreno = PostoAvancadoTerrainCheck.avaliar(centroX, centroZ,
                layout.footprint(), alturaDoTerreno);
        if (!terreno.aceitavel()) {
            return Optional.empty();
        }
        BlockPos origem = new BlockPos(centroX, terreno.minima(), centroZ);
        var placements = PostoAvancadoPlacementTransform.aplicar(
                PostoAvancadoBlockout.gerar(), origem, rotacao);
        return Optional.of(new Plano(origem, rotacao, terreno, placements));
    }

    public record Plano(BlockPos origem,
            PostoAvancadoPlacementTransform.Rotacao rotacao,
            PostoAvancadoTerrainCheck.Resultado terreno,
            java.util.List<PostoAvancadoBlockout.Placement> placements) {
        public Plano {
            origem = origem.immutable();
            Objects.requireNonNull(rotacao, "rotacao ausente");
            Objects.requireNonNull(terreno, "resultado de terreno ausente");
            placements = java.util.List.copyOf(placements);
        }
    }
}
