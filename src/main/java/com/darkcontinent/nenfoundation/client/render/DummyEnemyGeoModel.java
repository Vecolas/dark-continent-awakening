package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.DummyEnemyEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/** Modelo client-only do alvo de integração do EN1. */
public final class DummyEnemyGeoModel extends DefaultedEntityGeoModel<DummyEnemyEntity> {
    public DummyEnemyGeoModel() {
        super(NenFoundation.id("dummy_enemy"));
        withAltTexture(NenFoundation.id("dummy_enemy/standard"));
    }
}
