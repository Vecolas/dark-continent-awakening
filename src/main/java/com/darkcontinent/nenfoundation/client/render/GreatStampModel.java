package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.entity.GreatStampEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Modelo do Great Stamp com geometria EMPRESTADA da camada vanilla do hoglin.
 *
 * <p>Nao ha arte autoral do Great Stamp ainda e o ADR-007 proibe asset extraido
 * da obra; ate a arte propria existir, a silhueta do javali gigante e a que mais
 * se aproxima. Quando a arte autoral chegar, so o construtor e os nomes dos
 * filhos mudam — o telegrafo abaixo continua lendo a MESMA fase publicada pelo
 * servidor.</p>
 *
 * <p>Este modelo nao decide nada: ele le {@link GreatStampEntity#faseDeAtaque()},
 * que vem do SynchedEntityData. O cliente representa, o servidor decide.</p>
 *
 * <p>TODO: PLACEHOLDER -- geometria do HOGLIN vanilla (ModelLayers.HOGLIN). Sai quando o modelo GeckoLib proprio do great stamp existir.
 * Mob vanilla e andaime: ver ADR-017 e docs/inimigos/mobs-customizados.md.
 */
public final class GreatStampModel extends EntityModel<GreatStampEntity> {
    /** Pose de descanso do hoglin vanilla (focinho baixo, pastando). */
    private static final float CABECA_NEUTRA = 0.87266463F;
    /** Cabeca abaixada de investida: o jogador ve a carga ANTES de levar o dano. */
    private static final float CABECA_DE_CARGA = 1.2217305F;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;

    public GreatStampModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
    }

    @Override
    public void setupAnim(GreatStampEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        AttackPhase fase = entity.faseDeAtaque();
        boolean carregando = fase == AttackPhase.WINDUP || fase == AttackPhase.ACTIVE;

        this.head.xRot = carregando ? CABECA_DE_CARGA : CABECA_NEUTRA;
        // Durante a investida a cabeca aponta para onde o corpo vai, e nao para
        // onde o olhar do mob estaria; manter o yRot travado evita que o
        // telegrafo fique ambiguo.
        this.head.yRot = carregando ? 0.0F : netHeadYaw * (float) (Math.PI / 180.0);

        this.rightFrontLeg.xRot = Mth.cos(limbSwing) * 1.2F * limbSwingAmount;
        this.leftFrontLeg.xRot = Mth.cos(limbSwing + (float) Math.PI) * 1.2F * limbSwingAmount;
        this.rightHindLeg.xRot = this.leftFrontLeg.xRot;
        this.leftHindLeg.xRot = this.rightFrontLeg.xRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
