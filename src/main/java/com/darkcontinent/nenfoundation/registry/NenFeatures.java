package com.darkcontinent.nenfoundation.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.greedisland.worldgen.ObrasDaIlha;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * As features autorais do mod.
 *
 * <p>Hoje ha uma: o que a mao humana deixou em Greed Island -- estradas,
 * cidades e landmarks. Ela entra nos biomas por {@code biome_modifier}, e
 * modificador de bioma NAO conhece dimensao: a guarda que impede as cidades de
 * nascerem no Overworld mora dentro da propria feature, e nao no JSON.
 */
public final class NenFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(BuiltInRegistries.FEATURE, NenFoundation.MOD_ID);

    /** Estradas, cidades e landmarks de Greed Island. */
    public static final DeferredHolder<Feature<?>, ObrasDaIlha> OBRAS_DA_ILHA =
            FEATURES.register("obras_da_ilha",
                    () -> new ObrasDaIlha(NoneFeatureConfiguration.CODEC));

    private NenFeatures() {
    }

    public static void register(IEventBus barramento) {
        FEATURES.register(barramento);
    }
}
