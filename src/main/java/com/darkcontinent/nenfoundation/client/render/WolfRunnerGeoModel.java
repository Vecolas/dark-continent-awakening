package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.WolfRunnerEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Wolf Runner.
 *
 * <p>Os caminhos ficam escritos porque nenhum deles falha alto: arquivo ausente
 * vira um bicho invisivel ou um osso parado, nunca uma excecao.</p>
 *
 * <ul>
 *   <li>{@code geo/entity/wolf_runner.geo.json}</li>
 *   <li>{@code animations/entity/wolf_runner.animation.json}</li>
 *   <li>{@code textures/entity/wolf_runner/adulto.png}</li>
 * </ul>
 */
public final class WolfRunnerGeoModel extends DefaultedEntityGeoModel<WolfRunnerEntity> {
    public WolfRunnerGeoModel() {
        super(NenFoundation.id("wolf_runner"));
        withAltTexture(NenFoundation.id("wolf_runner/adulto"));
    }
}
