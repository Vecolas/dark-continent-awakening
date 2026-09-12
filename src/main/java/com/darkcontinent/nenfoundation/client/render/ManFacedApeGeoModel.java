package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.enemy.entity.ManFacedApeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Modelo GeckoLib AUTORAL do man-faced ape (ADR-017: mob vanilla e andaime).
 *
 * <p>ELE ESTENDE O {@code GeoModel} ABSTRATO, e nao o
 * {@code DefaultedEntityGeoModel} que os outros mobs usam. O motivo e o unico
 * que justifica a excecao: este mob tem DUAS SILHUETAS. Disfarcado ele e gente;
 * revelado ele e um primata. O Defaulted resolve os tres caminhos a partir de um
 * id fixo, e um id fixo so descreve um corpo.</p>
 *
 * <p>SAO DOIS IDS, e nao um modelo com metade dos ossos escondida. Esconder osso
 * e o truque que acabou de sair do frog-in-waiting: ele cria uma segunda maneira
 * de mudar a silhueta -- uma no renderer, outra na animacao -- e as duas
 * divergem na primeira correcao, em silencio.</p>
 *
 * <p>OS TRES RECURSOS TROCAM JUNTOS, pela MESMA pergunta
 * ({@link ManFacedApeEntity#vestindoCorpoHumano()}). Esta e a regra que o
 * arquivo existe para garantir: trocar o modelo sem trocar a animacao daria um
 * clipe procurando osso que nao existe -- nenhuma excecao, nenhum log, so um
 * membro parado; e trocar o modelo sem trocar a textura daria a silhueta humana
 * pintada com o atlas do macaco, que le como erro de arte.</p>
 *
 * <p>QUEM MANDA E O SERVIDOR. {@code vestindoCorpoHumano()} le apenas
 * {@code SynchedEntityData} ({@code DISFARCADO} e a fase do ataque); o cliente
 * nao decide disfarce, nao mede olhar e nao antecipa revelacao. Ver
 * {@code ManFacedApeEntity#vestindoCorpoHumano()} para o porque de a fase de
 * telegrafo ainda contar como corpo humano.</p>
 */
public final class ManFacedApeGeoModel extends GeoModel<ManFacedApeEntity> {
    private static final ResourceLocation GEO_HUMANO =
            NenFoundation.id("geo/entity/man_faced_ape_disfarce.geo.json");
    private static final ResourceLocation GEO_REVELADO =
            NenFoundation.id("geo/entity/man_faced_ape.geo.json");

    private static final ResourceLocation TEXTURA_HUMANA =
            NenFoundation.id("textures/entity/man_faced_ape_disfarce/humano.png");
    private static final ResourceLocation TEXTURA_REVELADA =
            NenFoundation.id("textures/entity/man_faced_ape/revelado.png");

    private static final ResourceLocation ANIMACAO_HUMANA =
            NenFoundation.id("animations/entity/man_faced_ape_disfarce.animation.json");
    private static final ResourceLocation ANIMACAO_REVELADA =
            NenFoundation.id("animations/entity/man_faced_ape.animation.json");

    @Override
    public ResourceLocation getModelResource(ManFacedApeEntity macaco) {
        return macaco.vestindoCorpoHumano() ? GEO_HUMANO : GEO_REVELADO;
    }

    @Override
    public ResourceLocation getTextureResource(ManFacedApeEntity macaco) {
        return macaco.vestindoCorpoHumano() ? TEXTURA_HUMANA : TEXTURA_REVELADA;
    }

    @Override
    public ResourceLocation getAnimationResource(ManFacedApeEntity macaco) {
        return macaco.vestindoCorpoHumano() ? ANIMACAO_HUMANA : ANIMACAO_REVELADA;
    }
}
