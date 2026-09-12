package com.darkcontinent.nenfoundation.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.FoxbearEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registro das entidades autorais do pacote.
 *
 * <p>A API AQUI E A DO NEOFORGE, e nao a do Forge. A primeira versao usava
 * {@code RegistryObject} e {@code NeoForgeRegistries.ENTITY_TYPES}, que sao do
 * Forge antigo e nao existem nesta stack -- o compilador reprovou com "cannot
 * find symbol", que e a melhor hora possivel para descobrir isso.
 */
public final class EnemyEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, NenFoundation.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<FoxbearEntity>> FOXBEAR =
            ENTITY_TYPES.register("foxbear",
                    () -> EntityType.Builder.of(FoxbearEntity::new, MobCategory.CREATURE)
                            .sized(1.4F, 1.35F)
                            .build(NenFoundation.id("foxbear").toString()));

    private EnemyEntityTypes() {
    }
}
