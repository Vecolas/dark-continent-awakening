package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.SpiderEagleEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib AUTORAL da spider eagle (ADR-017: mob vanilla e andaime).
 *
 * <p>O {@link DefaultedEntityGeoModel} resolve os tres caminhos a partir de um
 * unico id, e nenhum deles falha alto se estiver errado -- arquivo ausente vira
 * um bicho invisivel ou um osso parado, nunca uma excecao. Por isso os caminhos
 * estao escritos aqui:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/spider_eagle.geo.json}</li>
 *   <li>{@code animations/entity/spider_eagle.animation.json}</li>
 *   <li>{@code textures/entity/spider_eagle/adulto.png} (via {@code withAltTexture})</li>
 * </ul>
 *
 * <p>A textura e a UNICA que sai do padrao, e de proposito: sozinho o Defaulted
 * procuraria {@code textures/entity/spider_eagle.png}, e o pipeline de assets
 * deste repositorio guarda textura de mob por VARIANTE, numa pasta com o id do
 * mob. A segunda variante entra ao lado de {@code adulto.png}, sem mexer no
 * resto.</p>
 *
 * <p>UM ID SO, e nao dois como o man-faced ape. A ave tem uma forma unica: o que
 * muda entre pousada, voando, avisando e mergulhando e POSE, e pose mora na
 * animacao. Em particular a ASA ABERTA do aviso e um clipe, nunca uma segunda
 * geometria -- a envergadura em repouso ja nasce dobrada no geo justamente para
 * a silhueta parada nao prometer um alcance que o mergulho nao tem.</p>
 */
public final class SpiderEagleGeoModel extends DefaultedEntityGeoModel<SpiderEagleEntity> {
    public SpiderEagleGeoModel() {
        super(NenFoundation.id("spider_eagle"));
        withAltTexture(NenFoundation.id("spider_eagle/adulto"));
    }
}
