package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.GreatStampEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib AUTORAL do great stamp (ADR-017: mob vanilla e andaime).
 *
 * <p>O {@link DefaultedEntityGeoModel} resolve os tres caminhos a partir de um
 * unico id, e nenhum deles falha alto se estiver errado -- arquivo ausente vira
 * um bicho invisivel ou um osso parado, nunca uma excecao. Por isso os caminhos
 * estao escritos aqui:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/great_stamp.geo.json}</li>
 *   <li>{@code animations/entity/great_stamp.animation.json}</li>
 *   <li>{@code textures/entity/great_stamp/adulto.png} (via {@code withAltTexture})</li>
 * </ul>
 *
 * <p>A textura e a UNICA que sai do padrao, e de proposito: sozinho o Defaulted
 * procuraria {@code textures/entity/great_stamp.png}, e o pipeline de assets deste
 * repositorio guarda textura de mob por VARIANTE, numa pasta com o id do mob. A
 * segunda variante entra ao lado de {@code adulto.png}, sem mexer no resto.</p>
 */
public final class GreatStampGeoModel extends DefaultedEntityGeoModel<GreatStampEntity> {
    public GreatStampGeoModel() {
        super(NenFoundation.id("great_stamp"));
        withAltTexture(NenFoundation.id("great_stamp/adulto"));
    }
}
