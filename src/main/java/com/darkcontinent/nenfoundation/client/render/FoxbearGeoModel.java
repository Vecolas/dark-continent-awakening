package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.FoxbearEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib AUTORAL do foxbear (ADR-017: mob vanilla e andaime).
 *
 * <p>O {@link DefaultedEntityGeoModel} resolve os tres caminhos a partir de um
 * unico id, e nenhum deles falha alto se estiver errado -- arquivo ausente vira
 * um bicho invisivel ou um osso parado, nunca uma excecao. Por isso os caminhos
 * estao escritos aqui:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/foxbear.geo.json}</li>
 *   <li>{@code animations/entity/foxbear.animation.json}</li>
 *   <li>{@code textures/entity/foxbear/adulto.png} (via {@code withAltTexture})</li>
 * </ul>
 *
 * <p>A textura e a UNICA que sai do padrao, e de proposito: sozinho o Defaulted
 * procuraria {@code textures/entity/foxbear.png}, e o pipeline de assets deste
 * repositorio guarda textura de mob por VARIANTE, numa pasta com o id do mob. A
 * segunda variante entra ao lado de {@code adulto.png}, sem mexer no resto.</p>
 *
 * <p>UM ID SO. O foxbear tem uma forma unica: o que muda entre rondar, avisar,
 * erguer-se e investir e POSE, e pose mora na animacao -- nunca numa segunda
 * geometria e nunca num osso escondido pelo renderer. Duas maneiras de mudar a
 * silhueta divergem na primeira correcao, em silencio.</p>
 */
public final class FoxbearGeoModel extends DefaultedEntityGeoModel<FoxbearEntity> {
    public FoxbearGeoModel() {
        super(NenFoundation.id("foxbear"));
        withAltTexture(NenFoundation.id("foxbear/adulto"));
    }
}
