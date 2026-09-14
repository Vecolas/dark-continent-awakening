package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.RadioRatEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Radio Rat.
 *
 * <p>Os tres caminhos ficam escritos porque nenhum deles falha alto se estiver
 * errado -- arquivo ausente vira um bicho invisivel ou um osso parado, nunca uma
 * excecao:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/radio_rat.geo.json}</li>
 *   <li>{@code animations/entity/radio_rat.animation.json}</li>
 *   <li>{@code textures/entity/radio_rat/adulto.png}</li>
 * </ul>
 */
public final class RadioRatGeoModel extends DefaultedEntityGeoModel<RadioRatEntity> {
    public RadioRatGeoModel() {
        super(NenFoundation.id("radio_rat"));
        withAltTexture(NenFoundation.id("radio_rat/adulto"));
    }
}
