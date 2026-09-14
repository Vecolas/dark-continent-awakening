package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.KingWhiteStagBeetleEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do King White Stag Beetle.
 *
 * <p>Os tres caminhos ficam escritos porque nenhum deles falha alto se estiver
 * errado -- arquivo ausente vira um bicho invisivel ou um osso parado, nunca uma
 * excecao:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/king_white_stag_beetle.geo.json}</li>
 *   <li>{@code animations/entity/king_white_stag_beetle.animation.json}</li>
 *   <li>{@code textures/entity/king_white_stag_beetle/adulto.png}</li>
 * </ul>
 */
public final class KingWhiteStagBeetleGeoModel extends DefaultedEntityGeoModel<KingWhiteStagBeetleEntity> {
    public KingWhiteStagBeetleGeoModel() {
        super(NenFoundation.id("king_white_stag_beetle"));
        withAltTexture(NenFoundation.id("king_white_stag_beetle/adulto"));
    }
}
