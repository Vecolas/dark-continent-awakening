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
    }
}
