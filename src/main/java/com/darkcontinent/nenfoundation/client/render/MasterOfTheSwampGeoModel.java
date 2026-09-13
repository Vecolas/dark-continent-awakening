package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.MasterOfTheSwampEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Modelo GeckoLib AUTORAL do master of the swamp (ADR-017: mob vanilla e andaime).
 *
 * <p>O {@link DefaultedEntityGeoModel} resolve os tres caminhos a partir de um
 * unico id, e nenhum deles falha alto se estiver errado -- arquivo ausente vira
 * um bicho invisivel ou um osso parado, nunca uma excecao. Por isso os caminhos
 * estao escritos aqui:</p>
 *
 * <ul>
 *   <li>{@code geo/entity/master_of_the_swamp.geo.json}</li>
 *   <li>{@code animations/entity/master_of_the_swamp.animation.json}</li>
 *   <li>{@code textures/entity/master_of_the_swamp/adulto.png} (via {@code withAltTexture})</li>
 * </ul>
 *
 * <p>A textura e a UNICA que sai do padrao, e de proposito: sozinho o Defaulted
 * procuraria {@code textures/entity/master_of_the_swamp.png}, e o pipeline de
 * assets deste repositorio guarda textura de mob por VARIANTE, numa pasta com o
 * id do mob. Uma segunda variante entra ao lado de {@code adulto.png}, sem mexer
 * no resto.</p>
 *
 * <p>UM ID SO. Este peixe nao tem segunda forma: o que muda entre nadar, morder,
 * debater-se, ser recolhido e escapar e POSE, e pose mora na animacao. Em
 * particular o corpo TORCIDO do {@code thrash} -- o momento em que o jogador ve
 * que puxou demais -- e um clipe, nunca uma segunda geometria. Uma geometria
 * alternativa por estado significaria manter dois layouts de UV para o mesmo
 * bicho, e os dois divergem sem que nada acuse.</p>
 */
public final class MasterOfTheSwampGeoModel extends DefaultedEntityGeoModel<MasterOfTheSwampEntity> {
    public MasterOfTheSwampGeoModel() {
        super(NenFoundation.id("master_of_the_swamp"));
        withAltTexture(NenFoundation.id("master_of_the_swamp/adulto"));
    }
}
