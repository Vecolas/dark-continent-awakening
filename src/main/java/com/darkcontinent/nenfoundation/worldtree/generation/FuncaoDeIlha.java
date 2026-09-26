package com.darkcontinent.nenfoundation.worldtree.generation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Uma ilha: terra dentro de um raio, oceano fora, medido da origem.
 *
 * <p><b>POR QUE ISTO PRECISA DE JAVA.</b> As funcoes de densidade da vanilla
 * nao tem primitiva radial. Da para somar ruido, deslocar, interpolar e cortar
 * por altura -- nao da para perguntar "a que distancia da origem eu estou". Sem
 * essa pergunta, toda geracao e infinita por construcao, e uma ilha cercada de
 * mar dos quatro lados e impossivel de escrever em JSON.
 *
 * <p>Greed Island usava {@code minecraft:flat}: 123 camadas de pedra e uma de
 * grama, ate o horizonte. Funcionava e nao era uma ilha.
 *
 * <p><b>ELA E SIMPLES DE PROPOSITO.</b> A saida e um numero entre {@code -1} e
 * {@code +1} que so depende de X e Z:
 *
 * <pre>
 *   dentro do raio              -> +1        terra
 *   na faixa de transicao       -> +1 .. -1  a costa
 *   alem                        -> -1        mar aberto
 * </pre>
 *
 * <p>Quem transforma isso em relevo e o {@code noise_settings}, somando este
 * valor ao terreno. Colocar a conta do relevo aqui dentro amarraria a forma da
 * ilha ao formato do terreno, e os dois precisam girar em separado numa sessao
 * de arte.
 *
 * <p><b>ELA ALIMENTA DUAS COISAS, e essa e a parte que faz o mar ser MAR.</b> O
 * mesmo valor entra no {@code final_density} -- e por isso o chao some -- e em
 * {@code continents} do roteador, que e o que o {@code multi_noise} le para
 * escolher bioma. Sem a segunda ligacao haveria agua sobre um bioma de planicie:
 * cor de grama na agua, mob errado, e nada acusando.
 *
 * <p>SEM CACHE, e por isso {@link SimpleFunction}: a conta e uma raiz quadrada
 * por posicao, e envolver isso num {@code cache_2d} custaria mais memoria do que
 * a conta economiza.
 *
 * <p><b>A CONTA NAO MORA AQUI.</b> Ela esta em {@link FormaDaIlha}, sem uma
 * linha de Minecraft, porque o campo estatico de codec desta classe exige meia
 * pilha do jogo para inicializar -- e a suite JUnit roda sem ela. O portao
 * morria com {@code NoClassDefFoundError} antes da primeira asserção, e o que
 * ficava sem prova era justamente a forma da costa. Esta classe e uma casca de
 * adaptacao; ela nao decide nada.
 */
public record FuncaoDeIlha(double raio, double transicao, double centroX, double centroZ)
        implements DensityFunction.SimpleFunction {

    public static final MapCodec<FuncaoDeIlha> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            com.mojang.serialization.Codec.DOUBLE.fieldOf("raio")
                    .forGetter(FuncaoDeIlha::raio),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("transicao")
                    .forGetter(FuncaoDeIlha::transicao),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("centro_x", 0.0D)
                    .forGetter(FuncaoDeIlha::centroX),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("centro_z", 0.0D)
                    .forGetter(FuncaoDeIlha::centroZ)
    ).apply(i, FuncaoDeIlha::new));

    private static final KeyDispatchDataCodec<FuncaoDeIlha> DESPACHO =
            KeyDispatchDataCodec.of(CODEC);

    public FuncaoDeIlha {
        FormaDaIlha.exigirMedidasValidas(raio, transicao);
    }

    /**
     * O valor em uma posicao. So X e Z entram.
     *
     * <p>A ALTURA NAO PARTICIPA, e isso e o desenho: esta funcao responde "aqui
     * e ilha ou mar?", e a resposta nao muda conforme se sobe. Quem faz o
     * relevo variar com Y e o gradiente do {@code noise_settings}.
     */
    @Override
    public double compute(FunctionContext contexto) {
        return FormaDaIlha.valorEm(contexto.blockX() - this.centroX,
                contexto.blockZ() - this.centroZ, this.raio, this.transicao);
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
