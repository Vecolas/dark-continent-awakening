package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.FrogInWaitingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer do frog-in-waiting com geometria e UVs EMPRESTADOS do sapo vanilla.
 *
 * <p>A textura precisa acompanhar a camada: os UVs bakeados sao os do sapo, entao
 * apontar para qualquer outra imagem produz um mob manchado. Entre as tres
 * variantes vanilla, a temperada e a da selva do Exame Hunter. Quando a arte
 * autoral chegar, camada e textura trocam JUNTAS, e a {@link #ESCALA_EMPRESTADA}
 * deixa de existir porque o modelo proprio ja nascera no tamanho da hitbox.</p>
 */
public final class FrogInWaitingRenderer extends MobRenderer<FrogInWaitingEntity, FrogInWaitingModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/frog/temperate_frog.png");

    /**
     * A hitbox e sized(1.4F, 1.0F) e o sapo vanilla e sized(0.5F, 0.5F): sem
     * ampliar, o modelo boia perdido dentro da propria caixa e o jogador nao tem
     * como julgar o alcance do bote. O fator segue a ALTURA (1.0 / 0.5), porque
     * um modelo mais baixo que a hitbox engana menos do que um mais alto.
     */
    private static final float ESCALA_EMPRESTADA = 2.0F;

    public FrogInWaitingRenderer(EntityRendererProvider.Context context) {
        super(context, new FrogInWaitingModel(context.bakeLayer(ModelLayers.FROG)), 0.7F);
    }

    @Override
    protected void scale(FrogInWaitingEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(ESCALA_EMPRESTADA, ESCALA_EMPRESTADA, ESCALA_EMPRESTADA);
    }

    @Override public ResourceLocation getTextureLocation(FrogInWaitingEntity entity) { return TEXTURE; }
}
