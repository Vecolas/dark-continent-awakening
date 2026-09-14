package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.DummyEnemyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderer GeckoLib do DummyEnemy; nenhuma decisão de gameplay mora no cliente. */
public final class DummyEnemyRenderer extends GeoEntityRenderer<DummyEnemyEntity> {
    public DummyEnemyRenderer(EntityRendererProvider.Context context) {
        super(context, new DummyEnemyGeoModel());
        shadowRadius = 0.4F;
    }
}
