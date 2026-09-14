package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.HyperPuffballEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Hyper Puffball.
 *
 * <p>Os tres caminhos ficam escritos porque nenhum deles falha alto se estiver
 * errado -- arquivo ausente vira um bicho invisivel ou um osso parado, nunca uma
 * excecao:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/hyper_puffball.geo.json}</li>
 *   <li>{@code animations/entity/hyper_puffball.animation.json}</li>
 *   <li>{@code textures/entity/hyper_puffball/adulto.png}</li>
 * </ul>
 */
public final class HyperPuffballGeoModel extends DefaultedEntityGeoModel<HyperPuffballEntity> {
    public HyperPuffballGeoModel() {
        super(NenFoundation.id("hyper_puffball"));
        withAltTexture(NenFoundation.id("hyper_puffball/adulto"));
    }
}
