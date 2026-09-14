package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.AvianCommanderEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Avian Commander.
 *
 * <p>Os caminhos ficam escritos porque nenhum deles falha alto: arquivo ausente
 * vira um bicho invisivel ou um osso parado, nunca uma excecao.</p>
 *
 * <ul>
 *   <li>{@code geo/entity/avian_commander.geo.json}</li>
 *   <li>{@code animations/entity/avian_commander.animation.json}</li>
 *   <li>{@code textures/entity/avian_commander/adulto.png}</li>
 * </ul>
 */
public final class AvianCommanderGeoModel extends DefaultedEntityGeoModel<AvianCommanderEntity> {
    public AvianCommanderGeoModel() {
        super(NenFoundation.id("avian_commander"));
        withAltTexture(NenFoundation.id("avian_commander/adulto"));
    }
}
