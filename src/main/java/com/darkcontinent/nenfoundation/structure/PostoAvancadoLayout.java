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
        return new PostoAvancadoLayout(37, 3, 7, 14, List.of(
                new Modulo("portao_logistico", -4, 17, 9, 2),
                new Modulo("patio_operacional", -7, -7, 15, 10),
                new Modulo("comando", -8, 3, 17, 10),
                new Modulo("torre_scaffold", -3, -17, 7, 7),
                new Modulo("deposito", -17, -7, 9, 9),
                new Modulo("laboratorio", 9, -8, 9, 11),
                new Modulo("manutencao", -17, 3, 9, 10),
                new Modulo("triagem", 9, 4, 9, 9)));
    }

    public record Modulo(String id, int x, int z, int largura, int profundidade) {
        public Modulo {
            if (id == null || id.isBlank() || largura < 1 || profundidade < 1) {
                throw new IllegalArgumentException("modulo invalido");
            }
        }

        public boolean cabeNo(int lado) {
            int metade = lado / 2;
            return x >= -metade && z >= -metade
                    && x + largura - 1 <= metade && z + profundidade - 1 <= metade;
        }
    }
}
