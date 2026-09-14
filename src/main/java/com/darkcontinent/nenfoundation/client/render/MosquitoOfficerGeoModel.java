package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.MosquitoOfficerEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib do Mosquito Officer.
 *
 * <p>Os caminhos ficam escritos porque nenhum deles falha alto: arquivo ausente
 * vira um bicho invisivel ou um osso parado, nunca uma excecao.</p>
 *
 * <ul>
 *   <li>{@code geo/entity/mosquito_officer.geo.json}</li>
 *   <li>{@code animations/entity/mosquito_officer.animation.json}</li>
 *   <li>{@code textures/entity/mosquito_officer/adulto.png}</li>
 * </ul>
 */
public final class MosquitoOfficerGeoModel extends DefaultedEntityGeoModel<MosquitoOfficerEntity> {
    public MosquitoOfficerGeoModel() {
        super(NenFoundation.id("mosquito_officer"));
        withAltTexture(NenFoundation.id("mosquito_officer/adulto"));
    }
}
