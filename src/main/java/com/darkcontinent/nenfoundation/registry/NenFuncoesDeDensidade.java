package com.darkcontinent.nenfoundation.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.worldtree.generation.FuncaoDeIlha;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * As funcoes de densidade autorais, registradas para o datapack poder usa-las.
 *
 * <p>ELAS PRECISAM EXISTIR NOS DOIS LADOS. Geracao de terreno roda no servidor,
 * mas o cliente tambem carrega o registro para validar o datapack ao entrar --
 * um tipo registrado so no servidor faz o cliente recusar a conexao com um erro
 * de codec, e o relato e "nao consigo entrar no mundo".
 *
 * <p>REGISTRO PROPRIO, e nao uma linha no {@code NenFoundation.java}: o arquivo
 * principal so instala subsistemas, e ele nao cresce.
 */
public final class NenFuncoesDeDensidade {

    public static final DeferredRegister<MapCodec<? extends DensityFunction>> TIPOS =
            DeferredRegister.create(BuiltInRegistries.DENSITY_FUNCTION_TYPE,
                    NenFoundation.MOD_ID);

    /**
     * A forma da ilha de Greed Island.
     *
     * <p>O id entra em {@code noise_settings} como {@code "type":
     * "nenfoundation:ilha"}. Renomear aqui quebra o datapack em silencio -- o
     * jogo recusa a dimensao e cai no gerador padrao --, e por isso
     * {@code GeracaoDaIlhaTest} confere que o JSON e o registro concordam.
     */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>,
            MapCodec<FuncaoDeIlha>> ILHA = TIPOS.register("ilha", () -> FuncaoDeIlha.CODEC);

    private NenFuncoesDeDensidade() {
    }
}
