package com.darkcontinent.nenfoundation.client.vfx.render;

import com.darkcontinent.nenfoundation.client.vfx.AuraBodyRegion;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualState;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualSystem;
import com.darkcontinent.nenfoundation.client.vfx.CorDaAura;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfis;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPlayerModel;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellPass;
import com.darkcontinent.nenfoundation.client.vfx.shader.AuraShaders;
import com.darkcontinent.nenfoundation.config.NenClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * A aura em PRIMEIRA PESSOA, e ela e deliberadamente menos do que em terceira.
 *
 * <p>A REGRA E NAO ATRAPALHAR. Uma shell inteira na frente da camera cobre a
 * mira, compete com o item na mao e cansa em minutos. O que aparece aqui e a
 * BORDA do braco -- uma camada so, com alpha reduzido --, o suficiente para o
 * jogador saber que esta em Ten sem precisar olhar o HUD.
 *
 * <p>SO A BORDA, e nao os tres passes. O filme interno some atras do proprio
 * braco nesta distancia, e o halo externo, tao perto da camera, vira um veu
 * sobre a tela inteira. Desenhar os tres aqui seria pagar tres vezes por um
 * efeito pior.
 *
 * <p>DESENHA ANTES DO BRACO VANILLA. O evento e disparado antes da renderizacao
 * do braco, e nao se cancela nada: o braco continua sendo desenhado pelo jogo. A
 * shell fica POR FORA da geometria dele, entao o teste de profundidade resolve a
 * ordem sozinho.
 *
 * <p>OS MODELOS SAO CONSTRUIDOS SOB DEMANDA. O conjunto de modelos nao existe
 * quando este objeto nasce; ele so fica pronto depois do carregamento de
 * recursos. Construir no construtor daria o erro classico de "modelo nulo" no
 * primeiro quadro.
 */
public final class AuraPrimeiraPessoa {

    /** Quanto da borda sobra em primeira pessoa. */
    private static final float FATOR = 0.55F;

    private static final float ALPHA_MINIMO = 0.002F;

    private final Map<PlayerSkin.Model, AuraPlayerModel> modelos =
            new EnumMap<>(PlayerSkin.Model.class);

    /** Desenha a borda da aura no braco, se houver aura e se o jogador quiser. */
    public void aoRenderizarBraco(RenderArmEvent evento) {
        if (!NenClientConfig.auraEmPrimeiraPessoa()) {
            return;
        }
        AbstractClientPlayer jogador = evento.getPlayer();
        AuraVisualState estado = AuraVisualSystem.estadoDe(jogador);
        if (!estado.enabled() || jogador.isInvisible() || jogador.isSpectator()) {
            return;
        }

        AuraBodyRegion regiao = evento.getArm() == HumanoidArm.RIGHT
                ? AuraBodyRegion.RIGHT_ARM
                : AuraBodyRegion.LEFT_ARM;
        AuraPerfilVisual perfil = AuraPerfis.de(estado.mode());
        float alpha = perfil.alphaDe(AuraShellPass.BORDA)
                * estado.intensity()
                * estado.distribution().intensidade(regiao)
                * FATOR;
        if (alpha < ALPHA_MINIMO) {
            return;
        }

        AuraPlayerModel modelo = modeloDe(jogador.getSkin().model());
        if (modelo == null) {
            return;
        }

        // O TEMPO E O MESMO DA TERCEIRA PESSOA -- a idade da entidade --, para
        // que o fluxo nao "salte" quando o jogador troca de camera.
        AuraShaders.configurar(jogador.tickCount / 20.0F,
                perfil.fresnelDe(AuraShellPass.BORDA), perfil.velocidadeDeFluxo(),
                perfil.escalaDeRuido(), perfil.reforcoDaBorda());

        PoseStack pilha = evento.getPoseStack();
        MultiBufferSource buffers = evento.getMultiBufferSource();
        RenderType tipo = AuraRenderTypes.shell();
        VertexConsumer vertices = buffers.getBuffer(tipo);

        ModelPart braco = evento.getArm() == HumanoidArm.RIGHT
                ? modelo.rightArm
                : modelo.leftArm;
        // A POSE DO BRACO EM PRIMEIRA PESSOA E A DA PILHA, e nao a do modelo: o
        // jogo ja posicionou a camera e o braco antes de disparar o evento.
        // Zerar a rotacao da parte evita aplicar a pose de terceira pessoa por
        // cima da que ja esta correta.
        braco.setPos(0.0F, 0.0F, 0.0F);
        braco.xRot = 0.0F;
        braco.yRot = 0.0F;
        braco.zRot = 0.0F;
        braco.visible = true;
        braco.render(pilha, vertices, evento.getPackedLight(), OverlayTexture.NO_OVERLAY,
                CorDaAura.comAlpha(estado.primaryColor(), alpha));

        if (buffers instanceof MultiBufferSource.BufferSource lote) {
            lote.endBatch(tipo);
        }
    }

    private AuraPlayerModel modeloDe(PlayerSkin.Model skin) {
        AuraPlayerModel existente = this.modelos.get(skin);
        if (existente != null) {
            return existente;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getEntityModels() == null) {
            return null;
        }
        boolean slim = skin == PlayerSkin.Model.SLIM;
        AuraPlayerModel novo = new AuraPlayerModel(
                mc.getEntityModels().bakeLayer(AuraModelLayers.de(AuraShellPass.BORDA, slim)),
                slim);
        this.modelos.put(skin, novo);
        return novo;
    }
}
