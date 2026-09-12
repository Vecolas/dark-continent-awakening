package com.darkcontinent.nenfoundation.structure;

import java.util.List;
import java.util.Objects;

/** Contrato geometrico do primeiro posto, independente do template NBT. */
public record PostoAvancadoLayout(int footprint, int fenceHeight, int buildingHeight,
        int towerHeight, List<Modulo> modulos) {
    public PostoAvancadoLayout {
        if (footprint < 1 || fenceHeight < 1 || buildingHeight < 1 || towerHeight < buildingHeight
                || modulos == null || modulos.isEmpty()) {
            throw new IllegalArgumentException("layout do posto invalido");
        }
        modulos = List.copyOf(modulos);
        if (modulos.stream().anyMatch(Objects::isNull)
                || modulos.stream().anyMatch(modulo -> !modulo.cabeNo(footprint))) {
            throw new IllegalArgumentException("modulo fora do footprint do posto");
        }
    }

    public static PostoAvancadoLayout referencia() {
        return new PostoAvancadoLayout(37, 4, 9, 16, List.of(
                new Modulo("portao", 15, 33, 7, 4),
                new Modulo("patio", 8, 8, 21, 18),
                new Modulo("predio_principal", 9, 25, 19, 10),
                new Modulo("torre", 25, 3, 8, 8),
                new Modulo("pesquisa", 25, 18, 9, 10),
                new Modulo("deposito", 3, 18, 8, 9)));
    }

    public record Modulo(String id, int x, int z, int largura, int profundidade) {
        public Modulo {
            if (id == null || id.isBlank() || largura < 1 || profundidade < 1) {
                throw new IllegalArgumentException("modulo invalido");
            }
        }

        public boolean cabeNo(int lado) {
            return x >= 0 && z >= 0 && x + largura <= lado && z + profundidade <= lado;
        }
    }
}
