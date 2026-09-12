package com.darkcontinent.nenfoundation.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;

/** Transformações server-side para inserir o posto em diferentes orientações. */
public final class PostoAvancadoPlacementTransform {
    private PostoAvancadoPlacementTransform() { }

    public static List<PostoAvancadoBlockout.Placement> aplicar(
            List<PostoAvancadoBlockout.Placement> placements, BlockPos origem, Rotacao rotacao) {
        Objects.requireNonNull(placements, "placements ausentes");
        Objects.requireNonNull(origem, "origem ausente");
        Objects.requireNonNull(rotacao, "rotacao ausente");
        List<PostoAvancadoBlockout.Placement> transformados = new ArrayList<>(placements.size());
        for (PostoAvancadoBlockout.Placement placement : placements) {
            Objects.requireNonNull(placement, "placement ausente");
            BlockPos local = placement.posicao();
            int x = local.getX();
            int z = local.getZ();
            int rotacionadoX = rotacao.x(x, z);
            int rotacionadoZ = rotacao.z(x, z);
            transformados.add(new PostoAvancadoBlockout.Placement(
                    origem.offset(rotacionadoX, local.getY(), rotacionadoZ), placement.material()));
        }
        return List.copyOf(transformados);
    }

    public enum Rotacao {
        NONE {
            @Override int x(int x, int z) { return x; }
            @Override int z(int x, int z) { return z; }
        },
        CLOCKWISE_90 {
            @Override int x(int x, int z) { return -z; }
            @Override int z(int x, int z) { return x; }
        },
        CLOCKWISE_180 {
            @Override int x(int x, int z) { return -x; }
            @Override int z(int x, int z) { return -z; }
        },
        COUNTERCLOCKWISE_90 {
            @Override int x(int x, int z) { return z; }
            @Override int z(int x, int z) { return -x; }
        };

        abstract int x(int x, int z);

        abstract int z(int x, int z);
    }
}
