package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Registro client-only dos renderizadores de entidades. */
public final class EnemyRenderers {
    private EnemyRenderers() { }
    public static void registrar(EntityRenderersEvent.RegisterRenderers evento) {
        // Os sete vem do MESMO EnemyEntityTypes, e o import acima da conta de todos.
        // Ate aqui as duas primeiras linhas usavam nome curto e as duas seguintes
        // escreviam o pacote inteiro, porque existiam duas classes com este nome. O
        // nome qualificado era o sintoma visivel da fila paralela.
        evento.registerEntityRenderer(EnemyEntityTypes.FOXBEAR.get(), FoxbearRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.GREAT_STAMP.get(), GreatStampRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.FROG_IN_WAITING.get(), FrogInWaitingRenderer::new);
        evento.registerEntityRenderer(com.darkcontinent.nenfoundation.enemy.entity.ManFacedApeEntity.registeredType(), ManFacedApeRenderer::new);
        evento.registerEntityRenderer(com.darkcontinent.nenfoundation.enemy.entity.SpiderEagleEntity.registeredType(), SpiderEagleRenderer::new);
        evento.registerEntityRenderer(com.darkcontinent.nenfoundation.enemy.entity.MasterOfTheSwampEntity.registeredType(), MasterOfTheSwampRenderer::new);
        evento.registerEntityRenderer(com.darkcontinent.nenfoundation.enemy.entity.KirikoEntity.registeredType(), KirikoRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.DUMMY_ENEMY.get(), DummyEnemyRenderer::new);
        // As sete de Greed Island. Todas com renderer proprio desde o primeiro
        // dia: o ADR-017 e claro que mob vestindo corpo vanilla e andaime, e
        // andaime que nasce depois da regra ja nasce divida.
        evento.registerEntityRenderer(EnemyEntityTypes.CYCLOPS.get(), CyclopsRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.HYPER_PUFFBALL.get(), HyperPuffballRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.MELANIN_LIZARD.get(), MelaninLizardRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.RADIO_RAT.get(), RadioRatRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.BUBBLE_HORSE.get(), BubbleHorseRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.KING_WHITE_STAG_BEETLE.get(), KingWhiteStagBeetleRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.WOLF_PACK_HUNTER.get(), WolfPackHunterRenderer::new);

        // As nove formigas quimera, todas com renderer proprio desde o
        // primeiro dia (ADR-017).
        evento.registerEntityRenderer(EnemyEntityTypes.CRAB_HEAVY.get(), CrabHeavyRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.BAT_SCOUT.get(), BatScoutRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.WOLF_RUNNER.get(), WolfRunnerRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.SPIDER_WEBBER.get(), SpiderWebberRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.MOSQUITO_OFFICER.get(), MosquitoOfficerRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.MULTIARM_CENTIPEDE.get(), MultiarmCentipedeRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.CHEETAH_LEADER.get(), CheetahLeaderRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.SCORPION_LEADER.get(), ScorpionLeaderRenderer::new);
        evento.registerEntityRenderer(EnemyEntityTypes.AVIAN_COMMANDER.get(), AvianCommanderRenderer::new);
    }
}
