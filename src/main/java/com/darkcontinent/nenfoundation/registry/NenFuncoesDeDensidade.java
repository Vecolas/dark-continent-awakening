package com.darkcontinent.nenfoundation.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
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
     * O layout macro inteiro -- mascara, cordilheiras, bacias e rios -- como
     * densidade.
     *
     * <p>Ela SUBSTITUIU o andaime radial {@code nenfoundation:ilha}, que foi
     * APAGADO junto com a conta dele. Nao ha compatibilidade a preservar: a
     * secao 128 do documento manda apagar e regenerar a dimensao durante o
     * desenvolvimento, em vez de migrar chunk de prototipo -- e
     * {@code GreedIslandLayoutVersion.ATUAL} continua em zero justamente para
     * dizer que nenhum mundo de Greed Island e permanente ainda.
     */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>,
            MapCodec<com.darkcontinent.nenfoundation.enemy.greedisland.worldgen
                    .FuncaoDeLayout>> LAYOUT = TIPOS.register("layout_da_ilha",
            () -> com.darkcontinent.nenfoundation.enemy.greedisland.worldgen
                    .FuncaoDeLayout.CODEC);

    private NenFuncoesDeDensidade() {
    }
}
