package com.darkcontinent.nenfoundation.client.vfx.render;

import com.darkcontinent.nenfoundation.client.vfx.AuraBodyRegion;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualState;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualSystem;
import com.darkcontinent.nenfoundation.client.vfx.CorDaAura;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfis;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPlayerModel;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraGeometryLadder;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellPass;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraAnchor;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraCurve;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraRibbonBatch;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraRibbonProfile;
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

    /**
     * Quanto da borda sobra em primeira pessoa.
     *
     * <p>LIMITE DE DESIGN, E NAO AJUSTE. Uma shell inteira na frente da camera
     * cobre a mira e cansa em minutos; {@code AuraPrimeiraPessoaTest} existe
     * para que ninguem suba isto a 1.0 sem decidir que a aura pode cobrir a
     * mira.
     */
    private static final float FATOR = 0.55F;

    private static final float ALPHA_MINIMO = 0.002F;

    /**
     * As ancoras de filamento de UM braco, na ordem em que nascem.
     *
     * <p><b>TRES, E ISSO E LIMITE DE DESIGN E NAO BOTAO DE TUNING.</b> O gate
     * do AV4 pede "2-4 filamentos" em primeira pessoa
     * ({@code capturas/AV4/LEIA-ME.md}); o braco tem exatamente tres ancoras, e
     * usar as tres cai dentro da faixa sem precisar de um numero novo. Mais que
     * isso competiria com a mira, que e o que a regra desta classe proibe.
     */
    private static final int FILAMENTOS_POR_BRACO = 3;

    /**
     * A espessura de borda que a primeira pessoa usa para calcular a folga.
     *
     * <p>E o degrau BASE da escada, e nao o do estado: a shell de primeira
     * pessoa e uma camada so, construida uma vez por skin e sem a escada de
     * espessuras da terceira. Pedir a folga de um degrau que esta geometria nao
     * tem faria os filamentos nascerem dentro dela -- sumindo, sem erro.
     */
    private static final int DEGRAU_DA_PRIMEIRA_PESSOA = 0;

    private final AuraRibbonBatch filamentos = new AuraRibbonBatch();

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

        // A POSE E A MESMA QUE A VANILLA MONTA, e nao uma pose zerada.
        //
        // <p><b>ZERAR A POSICAO DESLOCAVA A SHELL DO BRACO, e foi o que a
        // primeira sessao de cliente viu.</b> Este bloco fazia
        // `setPos(0,0,0)` mais os tres eixos de rotacao em zero, com o
        // comentario de que "o jogo ja posicionou o braco". Ele ja posicionou a
        // PILHA -- a camera e a mao --, mas nao este `ModelPart`, que e da
        // aura e nunca passou por `setupAnim`. Levado para a origem do modelo,
        // ele desenhava a caixa no centro do corpo em vez de em volta do braco.
        //
        // <p>`PlayerRenderer.renderHand` faz exatamente o que esta abaixo:
        // neutraliza ataque, agachamento e nado, chama `setupAnim` e zera
        // APENAS o `xRot`. A posicao padrao da parte (x = +-5, y = 2) e o que
        // poe o braco onde a mao esta, e ela tem de ficar.
        modelo.attackTime = 0.0F;
        modelo.crouching = false;
        modelo.swimAmount = 0.0F;
        modelo.setupAnim(jogador, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        braco.xRot = 0.0F;
        braco.visible = true;
        braco.render(pilha, vertices, evento.getPackedLight(), OverlayTexture.NO_OVERLAY,
                CorDaAura.comAlpha(estado.primaryColor(), alpha));

        // UM MATERIAL POR VEZ. A shell sai inteira antes de a ribbon pedir o
        // proprio buffer -- ver LoteDaAuraTest e #299: dois consumidores vivos
        // no mesmo BufferSource derrubam o cliente com "Not building!".
        if (buffers instanceof MultiBufferSource.BufferSource lote) {
            lote.endBatch(tipo);
        }

        desenharFilamentos(pilha, buffers, evento.getPackedLight(), jogador, estado, perfil,
                braco, regiao, alpha);
    }

    /**
     * Os filamentos do braco, que o gate do AV4 pede e que nao existiam.
     *
     * <p><b>SO OS DO BRACO EM CENA, e nunca as colunas.</b> O
     * {@code capturas/AV4/LEIA-ME.md} e explicito: em {@code
     * primeira_pessoa_ren} "as colunas NAO podem aparecer -- so borda mais
     * forte, 2-4 filamentos e o pulso de ativacao". Uma coluna de dois blocos
     * nascendo no ombro, a essa distancia da camera, atravessaria a tela
     * inteira.
     *
     * <p>O CICLO E O MESMO DA TERCEIRA PESSOA -- mesma semente, mesmo envelope
     * --, pelo mesmo motivo do tempo: trocar de camera nao pode fazer o
     * filamento saltar de forma.
     */
    private void desenharFilamentos(PoseStack pilha, MultiBufferSource buffers, int luz,
            AbstractClientPlayer jogador, AuraVisualState estado, AuraPerfilVisual perfil,
            ModelPart braco, AuraBodyRegion regiao, float alphaDaBorda) {

        AuraRibbonProfile filamento = perfil.filamentos();
        if (filamento.quantidade() == 0) {
            return;
        }
        float folga = AuraCurve.folgaBase(
                AuraGeometryLadder.espessuraDaBordaDe(DEGRAU_DA_PRIMEIRA_PESSOA));
        long semeadura = jogador.getUUID().getLeastSignificantBits();
        float tempo = jogador.tickCount / 20.0F;

        RenderType tipo = AuraRenderTypes.ribbon();
        VertexConsumer buffer = buffers.getBuffer(tipo);

        pilha.pushPose();
        braco.translateAndRotate(pilha);
        PoseStack.Pose pose = pilha.last();

        int emitidos = 0;
        for (AuraAnchor ancora : AuraAnchor.values()) {
            if (ancora.regiao() != regiao || emitidos >= FILAMENTOS_POR_BRACO) {
                continue;
            }
            float fase = (emitidos * 0.618F) % 1.0F;
            float t = tempo / filamento.cicloSegundos() + fase;
            int ciclo = (int) Math.floor(t);
            float envelope = (float) Math.sin(Math.PI * (t - ciclo));
            float alpha = alphaDaBorda * envelope;
            emitidos++;
            if (alpha < ALPHA_MINIMO) {
                continue;
            }
            long semente = AuraCurve.semente(semeadura, ancora, emitidos, ciclo);
            this.filamentos.desenhar(buffer, pose, ancora, jogador.getSkin().model()
                            == PlayerSkin.Model.SLIM, semente, folga,
                    filamento.comprimentoDe(semente), filamento.largura(),
                    CorDaAura.comAlpha(estado.primaryColor(), alpha), luz);
        }
        pilha.popPose();

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
