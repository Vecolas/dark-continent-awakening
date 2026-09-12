package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.registry.EnemyEntityTypes;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Registro client-only dos renderizadores de entidades. */
public final class EnemyRenderers {
    private EnemyRenderers() { }
    public static void registrar(EntityRenderersEvent.RegisterRenderers evento) {
        evento.registerEntityRenderer(EnemyEntityTypes.FOXBEAR.get(), FoxbearRenderer::new);
        evento.registerEntityRenderer(com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes.GREAT_STAMP.get(), GreatStampRenderer::new);
        evento.registerEntityRenderer(com.darkcontinent.nenfoundation.enemy.registry.EnemyEntityTypes.FROG_IN_WAITING.get(), FrogInWaitingRenderer::new);
    }
}
