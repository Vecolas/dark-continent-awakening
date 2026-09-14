package com.darkcontinent.nenfoundation.enemy.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.FrogInWaitingEntity;
import com.darkcontinent.nenfoundation.enemy.entity.GreatStampEntity;
import com.darkcontinent.nenfoundation.enemy.entity.ManFacedApeEntity;
import com.darkcontinent.nenfoundation.enemy.entity.SpiderEagleEntity;
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

    // Caixa de GENTE, de proposito: 0.9 x 1.95 e quase a do jogador. O disfarce
    // comeca pela silhueta -- uma hitbox de macaco entregaria o bicho de longe, e a
    // pista que o mob ensina (ele so anda quando ninguem olha) nunca seria testada.
    public static final DeferredHolder<EntityType<?>, EntityType<ManFacedApeEntity>> MAN_FACED_APE =
            TYPES.register("man_faced_ape",
                    () -> EntityType.Builder.of(ManFacedApeEntity::new, MobCategory.CREATURE)
                            .sized(0.9F, 1.95F).build(NenFoundation.id("man_faced_ape").toString()));

    // Caixa de ave grande: larga e baixa (1.2 x 0.9). A envergadura mora no modelo,
    // nao na hitbox -- uma caixa do tamanho das asas faria a ave raspar em cada
    // parede do canyon e transformaria o mergulho numa colisao constante.
    public static final DeferredHolder<EntityType<?>, EntityType<SpiderEagleEntity>> SPIDER_EAGLE =
            TYPES.register("spider_eagle",
                    () -> EntityType.Builder.of(SpiderEagleEntity::new, MobCategory.CREATURE)
                            .sized(1.2F, 0.9F).build(NenFoundation.id("spider_eagle").toString()));

    private EnemyEntityTypes() { }
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
