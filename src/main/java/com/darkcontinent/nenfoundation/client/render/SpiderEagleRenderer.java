package com.darkcontinent.nenfoundation.client.render;

import com.darkcontinent.nenfoundation.enemy.entity.SpiderEagleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer da spider eagle com geometria e UVs EMPRESTADOS do phantom vanilla.
 *
 * <p>A textura precisa acompanhar a camada: os UVs bakeados sao os do phantom,
 * entao apontar para qualquer outra imagem produz um mob manchado. Quando a arte
 * autoral chegar, camada e textura trocam JUNTAS, e a colagem vertical abaixo
 * deixa de existir porque o modelo proprio ja nascera na origem e no tamanho da
 * hitbox.</p>
 *
 * <p>NAO ha {@code poseStack.scale} aqui, e isso e decisao e nao esquecimento. A
 * dimensao dominante da camada do phantom e a ENVERGADURA (~2,7 blocos na escala
 * 1), que ja e mais que o dobro da largura da hitbox {@code sized(1.2F, 0.9F)} --
 * o proprio vanilla deixa a asa do phantom transbordar a caixa dele, porque asa
 * nao e alcance. O TRONCO (corpo + cabeca + cauda) mede ~1,3 bloco de
 * comprimento, ou seja, ja preenche a caixa. Ampliar seguindo a altura, como o
 * frog-in-waiting faz, levaria a envergadura para perto de 5 blocos e a silhueta
 * passaria a mentir sobre onde o mergulho alcanca.</p>
 *
 * <p>A {@link #COLAGEM_VERTICAL}, por outro lado, e obrigatoria. O
 * LivingEntityRenderer inverte o eixo Y e translada {@code -1.501} DEPOIS de
 * chamar este metodo; sem compensar, um modelo cuja raiz esta na altura do corpo
 * -- e nao no topo da cabeca, como os humanoides -- fica pendurado a um bloco e
 * meio acima da propria hitbox. O phantom vanilla compensa com {@code 1.3125};
 * aqui o numero e outro porque a nossa caixa e mais alta que a dele.</p>
 *
 * <p>A camada de olhos brilhantes do phantom ({@code PhantomEyesLayer}) fica de
 * FORA de proposito: ela e um overlay emissivo que le como horror noturno, e
 * este mob nao caca ninguem -- ele defende um lugar, de dia, e avisa antes.</p>
 */
public final class SpiderEagleRenderer extends MobRenderer<SpiderEagleEntity, SpiderEagleModel> {
    /**
     * Confirmado em client-extra.jar: o phantom e uma das poucas entidades cuja
     * textura mora na RAIZ de textures/entity, sem subpasta propria. Qualquer
     * outra imagem mancha o mob, porque os UVs bakeados sao os desta.
     */
    private static final ResourceLocation TEXTURA =
            ResourceLocation.withDefaultNamespace("textures/entity/phantom.png");

    /** Metade da largura da hitbox sized(1.2F, 0.9F), como nas irmas. */
    private static final float RAIO_DA_SOMBRA = 0.6F;

    /**
     * Deslocamento no espaco JA INVERTIDO do LivingEntityRenderer: valor maior
     * abaixa o modelo. Somado ao {@code -1.501} que vem logo depois, sobra
     * {@code 1.501 - 1.05 = 0.451} bloco acima dos pes -- metade dos 0,9 da
     * hitbox, que e onde o tronco de um bicho que voa deve ficar.
     */
    private static final float COLAGEM_VERTICAL = 1.05F;

    public SpiderEagleRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderEagleModel(context.bakeLayer(ModelLayers.PHANTOM)), RAIO_DA_SOMBRA);
    }

    @Override
    protected void scale(SpiderEagleEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.translate(0.0F, COLAGEM_VERTICAL, 0.0F);
    }

    @Override public ResourceLocation getTextureLocation(SpiderEagleEntity entity) { return TEXTURA; }
}
