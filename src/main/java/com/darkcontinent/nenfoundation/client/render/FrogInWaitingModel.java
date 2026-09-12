package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.entity.FrogInWaitingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Modelo do frog-in-waiting com geometria EMPRESTADA da camada vanilla do sapo.
 *
 * <p>Nao ha arte autoral do sapo e o ADR-007 proibe asset extraido da obra; ate
 * a arte propria existir, a silhueta do sapo vanilla e a que mais se aproxima.
 * Quando a arte autoral chegar, so o construtor e os nomes dos filhos mudam — o
 * telegrafo abaixo continua lendo o MESMO estado publicado pelo servidor.</p>
 *
 * <p>Este modelo nao decide nada: ele le {@link FrogInWaitingEntity#estaEnterrado()}
 * e {@link FrogInWaitingEntity#faseDeAtaque()}, ambos vindos do SynchedEntityData.
 * O cliente representa, o servidor decide.</p>
 *
 * <p>O enterrado e resolvido ESCONDENDO PARTE, nunca transladando o modelo para
 * baixo: dentro de {@code scale()}/{@code render} o eixo Y ja vem invertido pelo
 * LivingEntityRenderer e o sinal e facil de errar sem ver na tela. Esconder parte
 * e deterministico.</p>
 *
 * <p>A instancia do modelo e UMA SO para todos os sapos do mundo. Por isso todo
 * campo tocado aqui e reescrito a cada quadro, sem ramo que deixe de escrever:
 * quem esconde, mostra. Um sapo que emerge invisivel e o pior relato de bug que
 * existe — o jogador morre sem ver o que o matou.</p>
 *
 * <p>TODO: PLACEHOLDER -- geometria do SAPO vanilla (ModelLayers.FROG). Sai quando o modelo GeckoLib proprio do frog-in-waiting existir.
 * Mob vanilla e andaime: ver ADR-017 e docs/inimigos/mobs-customizados.md.
 */
public final class FrogInWaitingModel extends EntityModel<FrogInWaitingEntity> {
    /** Pose de descanso: o sapo vanilla nasce com a cabeca nivelada. */
    private static final float CABECA_NEUTRA = 0.0F;
    /**
     * Boca aberta do bote. xRot NEGATIVO levanta o focinho: a camada do sapo nao
     * tem mandibula separada, entao a goela abrindo e a cabeca inteira girando
     * para tras. E o aviso que o jogador tem antes de ser engolido.
     */
    private static final float CABECA_DE_BOTE = -0.9599311F;

    /** Raiz bakeada da camada; e ela que vai para o buffer. */
    private final ModelPart raiz;
    private final ModelPart corpo;
    private final ModelPart cabeca;
    private final ModelPart lingua;
    private final ModelPart bracoEsquerdo;
    private final ModelPart bracoDireito;
    private final ModelPart pernaEsquerda;
    private final ModelPart pernaDireita;
    /** Bolsa de coaxar do sapo vanilla; sem a animacao de coaxo ela so causa z-fighting. */
    private final ModelPart bolsaDeCoaxar;

    public FrogInWaitingModel(ModelPart raiz) {
        this.raiz = raiz;
        // A camada do sapo pendura tudo num filho chamado "root"; a raiz bakeada
        // e a malha, e nao esse no.
        ModelPart no = raiz.getChild("root");
        this.corpo = no.getChild("body");
        this.cabeca = corpo.getChild("head");
        this.lingua = corpo.getChild("tongue");
        this.bracoEsquerdo = corpo.getChild("left_arm");
        this.bracoDireito = corpo.getChild("right_arm");
        this.pernaEsquerda = no.getChild("left_leg");
        this.pernaDireita = no.getChild("right_leg");
        this.bolsaDeCoaxar = corpo.getChild("croaking_body");
    }

    @Override
    public void setupAnim(FrogInWaitingEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        boolean enterrado = entity.estaEnterrado();
        AttackPhase fase = entity.faseDeAtaque();
        boolean abocanhando = fase == AttackPhase.WINDUP || fase == AttackPhase.ACTIVE;

        // Enterrado: so a cabeca fica de fora. "eyes" e filho de "head" e vem junto,
        // que e exatamente a perturbacao minima do solo que o plano pede.
        //
        // O corpo usa skipDraw e NAO visible: em ModelPart, visible=false corta o
        // ramo inteiro, e a cabeca esta pendurada no corpo. skipDraw apaga so os
        // cubos do proprio corpo e continua desenhando os filhos.
        corpo.skipDraw = enterrado;
        lingua.visible = !enterrado;
        bracoEsquerdo.visible = !enterrado;
        bracoDireito.visible = !enterrado;
        pernaEsquerda.visible = !enterrado;
        pernaDireita.visible = !enterrado;
        cabeca.visible = true;

        // A bolsa de coaxar so existe durante o coaxo no vanilla; aqui nunca ha
        // coaxo, entao ela fica desligada sempre — e nao ha ramo que a religue.
        bolsaDeCoaxar.visible = false;

        cabeca.xRot = abocanhando ? CABECA_DE_BOTE : CABECA_NEUTRA;
        // Enterrado o sapo nao acompanha o alvo com o olhar: o telegrafo e a
        // imobilidade. Fora da terra a cabeca segue a cabeca da entidade.
        cabeca.yRot = enterrado || abocanhando ? 0.0F : netHeadYaw * (float) (Math.PI / 180.0);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        this.raiz.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
