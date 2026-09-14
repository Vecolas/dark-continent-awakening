package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.DummyEnemyEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Boneco de Treino.
 *
 * <p>O {@link DefaultedEntityGeoModel} resolve os tres caminhos a partir de um
 * id, e nenhum deles falha alto se estiver errado -- arquivo ausente vira um
 * bicho invisivel ou um osso parado, nunca uma excecao. Por isso os caminhos
 * estao escritos aqui:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/dummy_enemy.geo.json}</li>
 *   <li>{@code animations/entity/dummy_enemy.animation.json}</li>
 *   <li>{@code textures/entity/dummy_enemy/adulto.png} (via {@code withAltTexture})</li>
 * </ul>
 *
 * <p>A textura sai do padrao pelo mesmo motivo dos outros sete: o pipeline deste
 * repositorio guarda textura de mob por VARIANTE, numa pasta com o id do mob.</p>
 */
public final class DummyEnemyGeoModel extends DefaultedEntityGeoModel<DummyEnemyEntity> {
    public DummyEnemyGeoModel() {
        super(NenFoundation.id("dummy_enemy"));
        withAltTexture(NenFoundation.id("dummy_enemy/adulto"));
    }
}
