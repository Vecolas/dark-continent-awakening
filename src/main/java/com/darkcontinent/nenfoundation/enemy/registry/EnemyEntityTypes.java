package com.darkcontinent.nenfoundation.enemy.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.FrogInWaitingEntity;
import com.darkcontinent.nenfoundation.enemy.entity.GreatStampEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;

/** Registro isolado; a entrada do mod deve apenas chamar {@link #register}. */
public final class EnemyEntityTypes {
    public static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, NenFoundation.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GreatStampEntity>> GREAT_STAMP = TYPES.register(
            "great_stamp", () -> EntityType.Builder.of(GreatStampEntity::new, MobCategory.CREATURE)
                    .sized(1.9F, 1.55F).build(NenFoundation.id("great_stamp").toString()));

    // Caixa baixa e larga de proposito: enterrado, o sapo precisa caber no chao
    // sem que a hitbox entregue o bicho antes do emerge.
    public static final DeferredHolder<EntityType<?>, EntityType<FrogInWaitingEntity>> FROG_IN_WAITING =
            TYPES.register("frog_in_waiting",
                    () -> EntityType.Builder.of(FrogInWaitingEntity::new, MobCategory.CREATURE)
                            .sized(1.4F, 1.0F).build(NenFoundation.id("frog_in_waiting").toString()));

    private EnemyEntityTypes() { }
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
