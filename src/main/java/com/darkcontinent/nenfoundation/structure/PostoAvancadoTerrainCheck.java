package com.darkcontinent.nenfoundation.structure;

import java.util.Objects;
import java.util.function.ToIntBiFunction;

/** Regras de elegibilidade do terreno para o footprint do posto. */
public final class PostoAvancadoTerrainCheck {
    public static final int ALTURA_IDEAL_MAXIMA = 4;
    public static final int ALTURA_ACEITAVEL_MAXIMA = 5;

    private PostoAvancadoTerrainCheck() { }

    /** Amostra centro, cantos e pontos médios do footprint. */
    public static Resultado avaliar(int centroX, int centroZ, int footprint,
            ToIntBiFunction<Integer, Integer> alturaDoTerreno) {
        if (footprint < 1 || footprint % 2 == 0) {
            throw new IllegalArgumentException("footprint deve ser impar e positivo");
        }
        Objects.requireNonNull(alturaDoTerreno, "altura do terreno ausente");
        int metade = footprint / 2;
        int[] offsets = { -metade, 0, metade };
        int minima = Integer.MAX_VALUE;
        int maxima = Integer.MIN_VALUE;
        for (int x : offsets) {
            for (int z : offsets) {
                int altura = alturaDoTerreno.applyAsInt(centroX + x, centroZ + z);
                minima = Math.min(minima, altura);
                maxima = Math.max(maxima, altura);
            }
        }
        int diferenca = maxima - minima;
        return new Resultado(minima, maxima, diferenca,
                diferenca <= ALTURA_ACEITAVEL_MAXIMA,
                diferenca <= ALTURA_IDEAL_MAXIMA);
    }

    public record Resultado(int minima, int maxima, int diferenca,
            boolean aceitavel, boolean ideal) { }
}
