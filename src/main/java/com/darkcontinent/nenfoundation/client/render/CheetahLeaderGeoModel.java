package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.CheetahLeaderEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Cheetah Leader.
 *
 * <p>Os caminhos ficam escritos porque nenhum deles falha alto: arquivo ausente
 * vira um bicho invisivel ou um osso parado, nunca uma excecao.</p>
 *
 * <ul>
 *   <li>{@code geo/entity/cheetah_leader.geo.json}</li>
 *   <li>{@code animations/entity/cheetah_leader.animation.json}</li>
 *   <li>{@code textures/entity/cheetah_leader/adulto.png}</li>
 * </ul>
 */
public final class CheetahLeaderGeoModel extends DefaultedEntityGeoModel<CheetahLeaderEntity> {
    public CheetahLeaderGeoModel() {
        super(NenFoundation.id("cheetah_leader"));
        withAltTexture(NenFoundation.id("cheetah_leader/adulto"));
    }
}
