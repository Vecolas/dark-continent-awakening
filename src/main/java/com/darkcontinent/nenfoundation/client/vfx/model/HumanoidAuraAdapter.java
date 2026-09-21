package com.darkcontinent.nenfoundation.client.vfx.model;

import com.darkcontinent.nenfoundation.client.vfx.AuraBodyRegion;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraAnchor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;

/**
 * A aura sobre um corpo de {@link HumanoidModel} -- zumbi, esqueleto, piglin.
 *
 * <p>ELE E O CASO FACIL, e existe para provar que o adaptador nao e cerimonia:
 * um humanoide vanilla tem exatamente as seis partes do
 * [ADR-014](docs/adr/ADR-014-alocacao-de-aura-por-regiao.md), com os mesmos
 * pivos do jogador. O que muda em relacao a layer de jogador e que aqui nao ha
 * variante {@code slim} -- nenhum mob vanilla tem braco fino.
 *
 * <p><b>A MALHA E A DO JOGADOR, INFLADA.</b> Isso e aproximacao declarada: um
 * zumbi tem os mesmos cubos de um Steve, e um piglin nao. O erro aparece como
 * aura levemente fora do lugar no focinho do piglin, e nao como falha -- e a
 * saida certa para um corpo que diverja e um adaptador proprio, e nao esticar
 * este.
 *
 * <p>A POSE E COPIADA, E SO -- como na layer de jogador. Chamar
 * {@code setupAnim} aqui DESCARTARIA a pose final que o renderer, as outras
 * layers e outros mods ja produziram.
 */
public final class HumanoidAuraAdapter implements AuraModelAdapter {

    private final HumanoidModel<?> modeloDoCorpo;
    private final AuraPlayerModel[] shell;

    /**
     * @param shell uma malha por passe, ja assada, na espessura escolhida
     */
    public HumanoidAuraAdapter(HumanoidModel<?> modeloDoCorpo, AuraPlayerModel[] shell) {
        if (modeloDoCorpo == null || shell == null
                || shell.length != AuraShellPass.values().length) {
            throw new IllegalArgumentException("o adaptador humanoide precisa do modelo do"
                    + " corpo e de uma malha por passe");
        }
        this.modeloDoCorpo = modeloDoCorpo;
        this.shell = shell;
    }

    @Override
    public boolean empilharRegiao(PoseStack pilha, AuraBodyRegion regiao) {
        ModelPart parte = parteDoCorpo(regiao);
        if (parte == null) {
            return false;
        }
        pilha.pushPose();
        parte.translateAndRotate(pilha);
        return true;
    }

    @Override
    public boolean empilharAncora(PoseStack pilha, AuraAnchor ancora) {
        // A ANCORA HERDA A PARTE DA PROPRIA REGIAO. Num humanoide as duas
        // coincidem -- e por isso `AuraAnchor` tem um campo so para as duas
        // coisas. Num corpo que nao coincida, e o adaptador daquele corpo que
        // resolve, e nao este.
        return ancora != null && empilharRegiao(pilha, ancora.regiao());
    }

    @Override
    public void desenharRegiao(PoseStack pilha, VertexConsumer vertices, AuraBodyRegion regiao,
            int luz, int argb) {
        for (AuraShellPass passe : AuraShellPass.values()) {
            AuraPlayerModel malha = this.shell[passe.ordinal()];
            ModelPart parte = parteDaShell(malha, regiao);
            if (parte == null) {
                continue;
            }
            parte.render(pilha, vertices, luz, OverlayTexture.NO_OVERLAY, argb);
        }
    }

    @Override
    public boolean temRegiao(AuraBodyRegion regiao) {
        return parteDoCorpo(regiao) != null;
    }

    /** Copia a pose do corpo para as malhas da aura. Chamado uma vez por quadro. */
    public void copiarPose() {
        for (AuraPlayerModel malha : this.shell) {
            malha.head.copyFrom(this.modeloDoCorpo.head);
            malha.body.copyFrom(this.modeloDoCorpo.body);
            malha.leftArm.copyFrom(this.modeloDoCorpo.leftArm);
            malha.rightArm.copyFrom(this.modeloDoCorpo.rightArm);
            malha.leftLeg.copyFrom(this.modeloDoCorpo.leftLeg);
            malha.rightLeg.copyFrom(this.modeloDoCorpo.rightLeg);
        }
    }

    private ModelPart parteDoCorpo(AuraBodyRegion regiao) {
        if (regiao == null) {
            return null;
        }
        return switch (regiao) {
            case HEAD -> this.modeloDoCorpo.head;
            case TORSO -> this.modeloDoCorpo.body;
            case LEFT_ARM -> this.modeloDoCorpo.leftArm;
            case RIGHT_ARM -> this.modeloDoCorpo.rightArm;
            case LEFT_LEG -> this.modeloDoCorpo.leftLeg;
            case RIGHT_LEG -> this.modeloDoCorpo.rightLeg;
        };
    }

    private static ModelPart parteDaShell(AuraPlayerModel malha, AuraBodyRegion regiao) {
        return switch (regiao) {
            case HEAD -> malha.head;
            case TORSO -> malha.body;
            case LEFT_ARM -> malha.leftArm;
            case RIGHT_ARM -> malha.rightArm;
            case LEFT_LEG -> malha.leftLeg;
            case RIGHT_LEG -> malha.rightLeg;
        };
    }

    /** Se esta entidade tem um modelo humanoide. */
    public static boolean serve(LivingEntity entidade,
            net.minecraft.client.model.EntityModel<?> modelo) {
        return entidade != null && modelo instanceof HumanoidModel<?>;
    }
}
