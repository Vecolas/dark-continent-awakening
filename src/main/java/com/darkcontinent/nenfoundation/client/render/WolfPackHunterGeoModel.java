package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.WolfPackHunterEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Wolf Pack Hunter.
 *
 * <p>Os tres caminhos ficam escritos porque nenhum deles falha alto se estiver
 * errado -- arquivo ausente vira um bicho invisivel ou um osso parado, nunca uma
 * excecao:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/wolf_pack_hunter.geo.json}</li>
 *   <li>{@code animations/entity/wolf_pack_hunter.animation.json}</li>
 *   <li>{@code textures/entity/wolf_pack_hunter/adulto.png}</li>
 * </ul>
 */
public final class WolfPackHunterGeoModel extends DefaultedEntityGeoModel<WolfPackHunterEntity> {
    public WolfPackHunterGeoModel() {
        super(NenFoundation.id("wolf_pack_hunter"));
        withAltTexture(NenFoundation.id("wolf_pack_hunter/adulto"));
    }
}
