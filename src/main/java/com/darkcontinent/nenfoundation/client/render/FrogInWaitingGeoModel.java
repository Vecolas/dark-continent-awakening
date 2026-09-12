package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.FrogInWaitingEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib AUTORAL do frog-in-waiting (ADR-017: mob vanilla e andaime).
 *
 * <p>O {@link DefaultedEntityGeoModel} resolve os tres caminhos a partir de um
 * unico id, e nenhum deles falha alto se estiver errado -- arquivo ausente vira
 * um bicho invisivel ou um osso parado, nunca uma excecao. Por isso os caminhos
 * estao escritos aqui:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/frog_in_waiting.geo.json}</li>
 *   <li>{@code animations/entity/frog_in_waiting.animation.json}</li>
 *   <li>{@code textures/entity/frog_in_waiting/adulto.png} (via {@code withAltTexture})</li>
 * </ul>
 *
 * <p>A textura e a UNICA que sai do padrao, e de proposito: sozinho o Defaulted
 * procuraria {@code textures/entity/frog_in_waiting.png}, e o pipeline de assets
 * deste repositorio guarda textura de mob por VARIANTE, numa pasta com o id do
 * mob. A segunda variante entra ao lado de {@code adulto.png}, sem mexer no
 * resto.</p>
 */
public final class FrogInWaitingGeoModel extends DefaultedEntityGeoModel<FrogInWaitingEntity> {
    public FrogInWaitingGeoModel() {
        super(NenFoundation.id("frog_in_waiting"));
        withAltTexture(NenFoundation.id("frog_in_waiting/adulto"));
    }
}
