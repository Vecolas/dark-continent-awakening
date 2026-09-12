package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.ManFacedApeEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer do man-faced ape: duas camadas vanilla bakeadas, duas texturas, e a
 * MESMA leitura do servidor escolhendo o par.
 *
 * <p>Textura e geometria trocam JUNTAS e pela mesma pergunta
 * ({@link ManFacedApeEntity#estaDisfarcado()}). Se o modelo escolhesse a
 * silhueta por um criterio e este metodo escolhesse a imagem por outro, nao
 * haveria erro nenhum: haveria um aldeao pintado com o atlas do piglin, que e
 * exatamente o tipo de bug que so aparece olhando a tela.</p>
 *
 * <p>Nao ha {@code scale()} aqui, de proposito. As duas camadas emprestadas
 * nascem com 1,95 bloco de altura -- a MESMA altura da hitbox
 * {@code sized(0.9F, 1.95F)}. A hitbox e mais LARGA que o modelo porque o bicho
 * e mais encorpado que um humano; ampliar o modelo para preencher essa largura
 * o deixaria tambem mais ALTO, e ai a silhueta mentiria sobre o alcance do
 * golpe. Quando a arte autoral chegar, camada, textura e tamanho nascem juntos
 * e nada disto precisa de fator.</p>
 *
 * <p>TODO: PLACEHOLDER -- camadas e texturas do ALDEAO e do PIGLIN vanilla. Sai quando houver geo, animation e texturas autorais das duas formas.
 * Mob vanilla e andaime: ver ADR-017 e docs/inimigos/mobs-customizados.md.
 */
public final class ManFacedApeRenderer extends MobRenderer<ManFacedApeEntity, ManFacedApeModel> {
    /**
     * A pele-base do aldeao, sem profissao e sem bioma: e a aparencia humana
     * que o jogo ja traz, e a unica que casa com os UVs de ModelLayers.VILLAGER.
     */
    private static final ResourceLocation TEXTURA_DO_DISFARCE =
            ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    /** Casa com os UVs de ModelLayers.PIGLIN; qualquer outra imagem mancha o mob. */
    private static final ResourceLocation TEXTURA_REVELADA =
            ResourceLocation.withDefaultNamespace("textures/entity/piglin/piglin.png");

    /** Mesma sombra dos dois humanoides vanilla de onde a silhueta foi emprestada. */
    private static final float RAIO_DA_SOMBRA = 0.5F;

    public ManFacedApeRenderer(EntityRendererProvider.Context context) {
        super(context,
                new ManFacedApeModel(context.bakeLayer(ModelLayers.VILLAGER),
                        context.bakeLayer(ModelLayers.PIGLIN)),
                RAIO_DA_SOMBRA);
    }

    @Override
    public ResourceLocation getTextureLocation(ManFacedApeEntity entity) {
        return entity.estaDisfarcado() ? TEXTURA_DO_DISFARCE : TEXTURA_REVELADA;
    }
}
