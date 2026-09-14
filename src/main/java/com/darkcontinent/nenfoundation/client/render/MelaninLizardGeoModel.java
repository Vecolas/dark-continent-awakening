package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.MelaninLizardEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Melanin Lizard.
 *
 * <p>Os tres caminhos ficam escritos porque nenhum deles falha alto se estiver
 * errado -- arquivo ausente vira um bicho invisivel ou um osso parado, nunca uma
 * excecao:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/melanin_lizard.geo.json}</li>
 *   <li>{@code animations/entity/melanin_lizard.animation.json}</li>
 *   <li>{@code textures/entity/melanin_lizard/adulto.png}</li>
 * </ul>
 */
public final class MelaninLizardGeoModel extends DefaultedEntityGeoModel<MelaninLizardEntity> {
    public MelaninLizardGeoModel() {
        super(NenFoundation.id("melanin_lizard"));
        withAltTexture(NenFoundation.id("melanin_lizard/adulto"));
    }
}
