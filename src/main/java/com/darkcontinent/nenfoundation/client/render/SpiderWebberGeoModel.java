package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.SpiderWebberEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Spider Webber.
 *
 * <p>Os caminhos ficam escritos porque nenhum deles falha alto: arquivo ausente
 * vira um bicho invisivel ou um osso parado, nunca uma excecao.</p>
 *
 * <ul>
 *   <li>{@code geo/entity/spider_webber.geo.json}</li>
 *   <li>{@code animations/entity/spider_webber.animation.json}</li>
 *   <li>{@code textures/entity/spider_webber/adulto.png}</li>
 * </ul>
 */
public final class SpiderWebberGeoModel extends DefaultedEntityGeoModel<SpiderWebberEntity> {
    public SpiderWebberGeoModel() {
        super(NenFoundation.id("spider_webber"));
        withAltTexture(NenFoundation.id("spider_webber/adulto"));
    }
}
