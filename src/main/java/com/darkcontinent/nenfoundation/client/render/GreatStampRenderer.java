package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.GreatStampEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer do Great Stamp com geometria e UVs EMPRESTADOS do hoglin vanilla.
 *
 * <p>A textura precisa acompanhar a camada: os UVs bakeados sao os do hoglin,
 * entao apontar para qualquer outra imagem produz um mob manchado. Quando a arte
 * autoral chegar, camada e textura trocam JUNTAS, e o {@link #scale} deixa de
 * existir porque o modelo proprio ja nascera no tamanho da hitbox.</p>
 */
public final class GreatStampRenderer extends MobRenderer<GreatStampEntity, GreatStampModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/hoglin/hoglin.png");

    /** A hitbox e sized(1.9F, 1.55F); o hoglin vanilla e menor que isso. */
    private static final float ESCALA_EMPRESTADA = 1.25F;

    public GreatStampRenderer(EntityRendererProvider.Context context) {
        super(context, new GreatStampModel(context.bakeLayer(ModelLayers.HOGLIN)), 0.9F);
    }

    @Override
    protected void scale(GreatStampEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(ESCALA_EMPRESTADA, ESCALA_EMPRESTADA, ESCALA_EMPRESTADA);
    }

    @Override public ResourceLocation getTextureLocation(GreatStampEntity entity) { return TEXTURE; }
}
