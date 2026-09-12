package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.FoxbearEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Modelo de urso reaproveitando a geometria vanilla ja registrada.
 *
 * <p>A camada e a do urso-polar, portanto os UVs pertencem a textura do urso
 * e nao a um atlas de vaca ou a uma imagem autoral sem contrato de UV.</p>
 *
 * <p>TODO: PLACEHOLDER -- geometria do URSO-POLAR vanilla (ModelLayers.POLAR_BEAR). Sai quando o modelo GeckoLib proprio do foxbear existir -- geo, animation e skeleton.
 * Mob vanilla e andaime: ver ADR-017 e docs/inimigos/mobs-customizados.md.
 */
public final class FoxbearModel extends EntityModel<FoxbearEntity> {
    private final ModelPart root;

    public FoxbearModel(ModelPart root) {
        this.root = root;
    }

    @Override
    public void setupAnim(FoxbearEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // A geometria vanilla ja fornece a silhueta correta; manter a pose
        // neutra evita aplicar a animacao de outra especie por engano.
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
