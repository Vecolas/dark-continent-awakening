package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.CyclopsEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Cyclops.
 *
 * <p>Os tres caminhos ficam escritos porque nenhum deles falha alto se estiver
 * errado -- arquivo ausente vira um bicho invisivel ou um osso parado, nunca uma
 * excecao:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/cyclops.geo.json}</li>
 *   <li>{@code animations/entity/cyclops.animation.json}</li>
 *   <li>{@code textures/entity/cyclops/adulto.png}</li>
 * </ul>
 */
public final class CyclopsGeoModel extends DefaultedEntityGeoModel<CyclopsEntity> {
    public CyclopsGeoModel() {
        super(NenFoundation.id("cyclops"));
        withAltTexture(NenFoundation.id("cyclops/adulto"));
    }
}
