package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Renderiza apenas head e hat da skin atual, em angulo fixo. */
public final class PlayerHeadRenderer {
    public void desenhar(GuiGraphics graficos, NenHudLayout.Retangulo area,
            AbstractClientPlayer jogador) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getEntityRenderDispatcher().getRenderer(jogador) instanceof PlayerRenderer renderer)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> modelo = renderer.getModel();
        float cabecaX = modelo.head.xRot;
        float cabecaY = modelo.head.yRot;
        float cabecaZ = modelo.head.zRot;
        float chapeuX = modelo.hat.xRot;
        float chapeuY = modelo.hat.yRot;
        float chapeuZ = modelo.hat.zRot;

        PoseStack pose = graficos.pose();
        pose.pushPose();
        graficos.enableScissor(area.x() + 2, area.y() + 2,
                area.x() + area.largura() - 2, area.y() + area.altura() - 2);
        try {
            pose.translate(area.x() + area.largura() / 2.0F, area.y() + 3.0F, 100.0F);
            pose.scale(48.0F, -48.0F, 48.0F);
            pose.mulPose(Axis.YP.rotationDegrees(195.0F));
            modelo.head.xRot = -0.08F;
            modelo.head.yRot = 0.12F;
            modelo.head.zRot = 0.0F;
            modelo.hat.copyFrom(modelo.head);

            var buffers = mc.renderBuffers().bufferSource();
            RenderType tipo = RenderType.entityTranslucent(renderer.getTextureLocation(jogador));
            var vertices = buffers.getBuffer(tipo);
            modelo.head.render(pose, vertices, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY);
            modelo.hat.render(pose, vertices, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY);
            buffers.endBatch(tipo);
        } finally {
            modelo.head.xRot = cabecaX;
            modelo.head.yRot = cabecaY;
            modelo.head.zRot = cabecaZ;
            modelo.hat.xRot = chapeuX;
            modelo.hat.yRot = chapeuY;
            modelo.hat.zRot = chapeuZ;
            graficos.disableScissor();
            pose.popPose();
        }
    }
}
