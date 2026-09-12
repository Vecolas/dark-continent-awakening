package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.registry.EnemyEntityTypes;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Registro client-only dos renderizadores de entidades. */
public final class EnemyRenderers {
    private EnemyRenderers() { }
    public static void registrar(EntityRenderersEvent.RegisterRenderers evento) {
        evento.registerEntityRenderer(EnemyEntityTypes.FOXBEAR.get(), FoxbearRenderer::new);
        // Great stamp: unico ja migrado para GeoEntityRenderer (ADR-017). O registro nao
        // muda -- o construtor continua recebendo so o Context, e e por isso que a troca
        // de corpo cabe nesta linha sem tocar em mais nada.
        evento.registerEntityRenderer(com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes.GREAT_STAMP.get(), GreatStampRenderer::new);
        evento.registerEntityRenderer(com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes.FROG_IN_WAITING.get(), FrogInWaitingRenderer::new);
        evento.registerEntityRenderer(com.darkcontinent.nenfoundation.enemy.entity.ManFacedApeEntity.registeredType(), ManFacedApeRenderer::new);
        evento.registerEntityRenderer(com.darkcontinent.nenfoundation.enemy.entity.SpiderEagleEntity.registeredType(), SpiderEagleRenderer::new);
    }
}
