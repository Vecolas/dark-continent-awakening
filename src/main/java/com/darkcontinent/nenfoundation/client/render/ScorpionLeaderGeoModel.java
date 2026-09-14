package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.ScorpionLeaderEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Scorpion Leader.
 *
 * <p>Os caminhos ficam escritos porque nenhum deles falha alto: arquivo ausente
 * vira um bicho invisivel ou um osso parado, nunca uma excecao.</p>
 *
 * <ul>
 *   <li>{@code geo/entity/scorpion_leader.geo.json}</li>
 *   <li>{@code animations/entity/scorpion_leader.animation.json}</li>
 *   <li>{@code textures/entity/scorpion_leader/adulto.png}</li>
 * </ul>
 */
public final class ScorpionLeaderGeoModel extends DefaultedEntityGeoModel<ScorpionLeaderEntity> {
    public ScorpionLeaderGeoModel() {
        super(NenFoundation.id("scorpion_leader"));
        withAltTexture(NenFoundation.id("scorpion_leader/adulto"));
    }
}
