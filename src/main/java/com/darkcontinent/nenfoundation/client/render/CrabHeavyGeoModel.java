package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.CrabHeavyEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Crab Heavy.
 *
 * <p>Os caminhos ficam escritos porque nenhum deles falha alto: arquivo ausente
 * vira um bicho invisivel ou um osso parado, nunca uma excecao.</p>
 *
 * <ul>
 *   <li>{@code geo/entity/crab_heavy.geo.json}</li>
 *   <li>{@code animations/entity/crab_heavy.animation.json}</li>
 *   <li>{@code textures/entity/crab_heavy/adulto.png}</li>
 * </ul>
 */
public final class CrabHeavyGeoModel extends DefaultedEntityGeoModel<CrabHeavyEntity> {
    public CrabHeavyGeoModel() {
        super(NenFoundation.id("crab_heavy"));
        withAltTexture(NenFoundation.id("crab_heavy/adulto"));
    }
}
