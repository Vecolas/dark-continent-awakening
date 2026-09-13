package com.darkcontinent.nenfoundation.enemy.registry;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.FoxbearEntity;
import com.darkcontinent.nenfoundation.enemy.entity.FrogInWaitingEntity;
import com.darkcontinent.nenfoundation.enemy.entity.GreatStampEntity;
import com.darkcontinent.nenfoundation.enemy.entity.KirikoEntity;
import com.darkcontinent.nenfoundation.enemy.entity.ManFacedApeEntity;
import com.darkcontinent.nenfoundation.enemy.entity.MasterOfTheSwampEntity;
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

    // Caixa de PEIXE ENORME: 2.4 x 1.6 (38.4 x 25.6 px). Ela e larga de proposito --
    // o encontro inteiro e "isto nao cabe na sua vara", e uma caixa modesta faria a
    // silhueta mentir sobre o que esta puxando a linha. O preco esta declarado: com
    // esta largura ele so se move em agua FUNDA, e o predicado de spawn exige dois
    // blocos de agua justamente por isso.
    //
    // WATER_CREATURE, e nao CREATURE: e a categoria que faz o biome modifier cair na
    // lista de spawn de agua. Registrado como CREATURE ele simplesmente nunca
    // nasceria, e nada acusaria -- o pantano ficaria vazio.
    public static final DeferredHolder<EntityType<?>, EntityType<MasterOfTheSwampEntity>> MASTER_OF_THE_SWAMP =
            TYPES.register("master_of_the_swamp",
                    () -> EntityType.Builder.of(MasterOfTheSwampEntity::new, MobCategory.WATER_CREATURE)
                            .sized(2.4F, 1.6F).build(NenFoundation.id("master_of_the_swamp").toString()));

    // Caixa de GENTE ALTA: 1.0 x 2.1 (16 x 33.6 px). UMA SO para as DUAS formas, e
    // isso e o mob inteiro: se a hitbox mudasse na revelacao, o disfarce se
    // entregaria pela colisao antes de o corpo mudar -- o jogador tropecaria numa
    // caixa de bicho enquanto ve uma pessoa, e aprenderia a ler a hitbox em vez de
    // ler o comportamento. A forma verdadeira e desenhada PARA CABER aqui; e o
    // modelo que se ajusta a caixa, nunca o contrario.
    //
    // CREATURE, e nao MONSTER: ele nao nasce pela escuridao e nao e monstro. A
    // categoria escolhe a lista de spawn que o biome modifier alimenta, e registrado
    // como MONSTER ele nasceria no escuro, de noite, e ninguem seria enganado por um
    // homem parado no meio do mato as tres da manha.
    public static final DeferredHolder<EntityType<?>, EntityType<KirikoEntity>> KIRIKO =
            TYPES.register("kiriko",
                    () -> EntityType.Builder.of(KirikoEntity::new, MobCategory.CREATURE)
                            .sized(1.0F, 2.1F).build(NenFoundation.id("kiriko").toString()));

    // O foxbear foi o PRIMEIRO mob deste repositorio, e por isso era o unico que
    // nascia num DeferredRegister proprio, num pacote proprio, com atributos e
    // placement proprios. As duas filas nao davam erro: elas davam um mob que
    // ficava de fora de tudo que o resto da fila ganhava -- e foi exatamente o que
    // aconteceu com a faixa de luz do perfil dele, morta por meses.
    //
    // Caixa inalterada (1.4 x 1.35): esta entrega muda ONDE ele e registrado, nunca
    // o corpo. Mexer na caixa aqui trocaria em silencio o raio em que ele de fato
    // leva flecha, e o gametest de territorio mede distancia, nao colisao.
    public static final DeferredHolder<EntityType<?>, EntityType<FoxbearEntity>> FOXBEAR =
            TYPES.register("foxbear",
                    () -> EntityType.Builder.of(FoxbearEntity::new, MobCategory.CREATURE)
                            .sized(1.4F, 1.35F).build(NenFoundation.id("foxbear").toString()));

    private EnemyEntityTypes() { }
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
