package com.darkcontinent.nenfoundation.enemy.greedisland.worldgen;

import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandElevationField;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * A ponte entre o layout macro e o gerador de terreno da vanilla.
 *
 * <p><b>ELA E O QUE FAZ G1-G5 EXISTIREM EM JOGO.</b> Mascara, cordilheiras,
 * bacias e rios sao funcoes puras que ate agora so apareciam num PNG. Esta
 * classe as transforma em densidade, e a densidade em bloco.
 *
 * <p><b>POR QUE DENSITY FUNCTION, E NAO UM ChunkGenerator PROPRIO.</b> Um
 * gerador autoral teria de reimplementar aquiferos, cavernas, veios de minerio,
 * regras de superficie e povoamento -- tudo isso ja existe, testado, no
 * caminho do {@code minecraft:noise}. O que falta a ele e saber ONDE a ilha
 * fica, e isso e uma funcao. Trocar o pipeline inteiro para acrescentar uma
 * informacao seria pagar caro por pouco.
 *
 * <p>A CONVERSAO E LINEAR EM TORNO DA ALTURA-ALVO:
 *
 * <pre>
 *   densidade = (alturaMacro - y) * INCLINACAO
 * </pre>
 *
 * <p>Acima do alvo a densidade fica negativa (ar); abaixo, positiva (pedra). A
 * {@code INCLINACAO} controla quao abrupta e a transicao -- e ela NAO pode ser
 * alta demais: com transicao muito seca, o interpolador do gerador produz
 * degraus de um bloco em toda encosta, e a montanha vira escada.
 *
 * <p>O RUIDO LOCAL ENTRA POR FORA, no {@code noise_settings}: aqui mora a forma
 * MACRO, e misturar detalhe local nesta classe faria o mapa de debug divergir
 * do mundo gerado -- os dois leem funcoes diferentes.
 */
public record FuncaoDeLayout(double inclinacao) implements DensityFunction.SimpleFunction {

    public static final MapCodec<FuncaoDeLayout> CODEC = RecordCodecBuilder.mapCodec(i -> i
            .group(com.mojang.serialization.Codec.DOUBLE
                    .optionalFieldOf("inclinacao", 0.06D)
                    .forGetter(FuncaoDeLayout::inclinacao))
            .apply(i, FuncaoDeLayout::new));

    private static final KeyDispatchDataCodec<FuncaoDeLayout> DESPACHO =
            KeyDispatchDataCodec.of(CODEC);

    public FuncaoDeLayout {
        if (!Double.isFinite(inclinacao) || inclinacao <= 0.0D) {
            throw new IllegalArgumentException("inclinacao invalida: " + inclinacao);
        }
    }

    @Override
    public double compute(FunctionContext contexto) {
        double alvo = GreedIslandElevationField.alturaEm(
                contexto.blockX(), contexto.blockZ());
        return Math.clamp((alvo - contexto.blockY()) * this.inclinacao, -1.0D, 1.0D);
    }

    @Override
    public double minValue() {
        return -1.0D;
    }

    @Override
    public double maxValue() {
        return 1.0D;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return DESPACHO;
    }
}
