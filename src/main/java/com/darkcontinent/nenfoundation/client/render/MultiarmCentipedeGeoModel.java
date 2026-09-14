package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.MultiarmCentipedeEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Multiarm Centipede.
 *
 * <p>Os caminhos ficam escritos porque nenhum deles falha alto: arquivo ausente
 * vira um bicho invisivel ou um osso parado, nunca uma excecao.</p>
 *
 * <ul>
 *   <li>{@code geo/entity/multiarm_centipede.geo.json}</li>
 *   <li>{@code animations/entity/multiarm_centipede.animation.json}</li>
 *   <li>{@code textures/entity/multiarm_centipede/adulto.png}</li>
 * </ul>
 */
public final class MultiarmCentipedeGeoModel extends DefaultedEntityGeoModel<MultiarmCentipedeEntity> {
    public MultiarmCentipedeGeoModel() {
        super(NenFoundation.id("multiarm_centipede"));
        withAltTexture(NenFoundation.id("multiarm_centipede/adulto"));
    }
}
