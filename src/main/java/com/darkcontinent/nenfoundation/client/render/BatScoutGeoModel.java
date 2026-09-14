package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.BatScoutEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Bat Scout.
 *
 * <p>Os caminhos ficam escritos porque nenhum deles falha alto: arquivo ausente
 * vira um bicho invisivel ou um osso parado, nunca uma excecao.</p>
 *
 * <ul>
 *   <li>{@code geo/entity/bat_scout.geo.json}</li>
 *   <li>{@code animations/entity/bat_scout.animation.json}</li>
 *   <li>{@code textures/entity/bat_scout/adulto.png}</li>
 * </ul>
 */
public final class BatScoutGeoModel extends DefaultedEntityGeoModel<BatScoutEntity> {
    public BatScoutGeoModel() {
        super(NenFoundation.id("bat_scout"));
        withAltTexture(NenFoundation.id("bat_scout/adulto"));
    }
}
