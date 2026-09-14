package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.BubbleHorseEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Bubble Horse.
 *
 * <p>Os tres caminhos ficam escritos porque nenhum deles falha alto se estiver
 * errado -- arquivo ausente vira um bicho invisivel ou um osso parado, nunca uma
 * excecao:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/bubble_horse.geo.json}</li>
 *   <li>{@code animations/entity/bubble_horse.animation.json}</li>
 *   <li>{@code textures/entity/bubble_horse/adulto.png}</li>
 * </ul>
 */
public final class BubbleHorseGeoModel extends DefaultedEntityGeoModel<BubbleHorseEntity> {
    public BubbleHorseGeoModel() {
        super(NenFoundation.id("bubble_horse"));
        withAltTexture(NenFoundation.id("bubble_horse/adulto"));
    }
}
