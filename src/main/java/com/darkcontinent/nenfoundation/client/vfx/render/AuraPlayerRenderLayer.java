package com.darkcontinent.nenfoundation.client.vfx.render;

import com.darkcontinent.nenfoundation.client.vfx.AuraBodyRegion;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualState;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualSystem;
import com.darkcontinent.nenfoundation.client.vfx.CorDaAura;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPlayerModel;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellMaterial;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellOpacity;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellPass;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraAnchor;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraCurve;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraRibbonBatch;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraRibbonProfile;
import com.darkcontinent.nenfoundation.client.vfx.shader.AuraShaders;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * A aura desenhada por cima do jogador, seguindo a pose dele.
 *
 * <p>POR QUE UMA LAYER, E NAO UM RENDERER PARALELO. A layer roda dentro do
 * {@code PlayerRenderer}, entao yaw, pitch, agachar, nadar, atacar, correr e a
 * rotacao da cabeca chegam PRONTOS. Um renderer paralelo teria de reimplementar
 * a pose, e divergiria dela na primeira animacao nova -- inclusive nas que
 * outros mods adicionam.
 *
 * <p>A POSE E COPIADA, E SO. {@code HumanoidModel.copyPropertiesTo} ja copia as
 * ROTACOES de cada parte do modelo do jogador -- {@code head.copyFrom(...)},
 * {@code body.copyFrom(...)} e os quatro membros --, e nao apenas os flags.
 *
 * <p>CHAMAR {@code setupAnim} DEPOIS SERIA UM ERRO, e um erro silencioso:
 * ele DESCARTARIA a pose final que acabou de ser copiada para recalcular uma
 * aproximacao a partir dos parametros crus. A pose do pai ja inclui o que o
 * renderer, as outras layers e outros mods fizeram com ela; recalcular joga
 * tudo isso fora e a aura passa a divergir do corpo em casos especificos --
 * montado, dormindo, com pose de arma de outro mod. E a divergencia nao aparece
 * como erro: aparece como aura levemente fora do lugar.
 *
 * <p>E o mesmo caminho que {@code HumanoidArmorLayer} usa: copia, e desenha.
 *
 * <p>DESENHA PARTE POR PARTE, e nao o modelo inteiro de uma vez. E o que
 * permite multiplicar a intensidade por REGIAO -- e e o que torna Gyo, Ko e Ryu
 * uma mudanca de numero em vez de um renderer novo (ADR-014, ADR-015).
 *
 * <p>ATENCAO A VERSAO: 1.21.1 e ANTERIOR ao {@code EntityRenderState}, que
 * chegou em 1.21.2. Exemplo de renderer publicado depois disso nao serve aqui.
 */
public final class AuraPlayerRenderLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** Alpha abaixo do qual nem vale montar a geometria. */
    private static final float ALPHA_MINIMO = 0.002F;

    private final Map<AuraShellPass, AuraPlayerModel> modelos =
            new EnumMap<>(AuraShellPass.class);

    /** Uma instancia por layer: todos os vetores de trabalho vivem dentro dela. */
    private final AuraRibbonBatch filamentos = new AuraRibbonBatch();

    private final boolean slim;

    public AuraPlayerRenderLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pai,
            EntityModelSet modelos, boolean slim) {
        super(pai);
        this.slim = slim;
        for (AuraShellPass passe : AuraShellPass.values()) {
            this.modelos.put(passe,
                    new AuraPlayerModel(modelos.bakeLayer(AuraModelLayers.de(passe, slim)), slim));
        }
    }

    @Override
    public void render(PoseStack pilha, MultiBufferSource buffers, int luzEmpacotada,
            AbstractClientPlayer jogador, float balancoDosMembros, float amplitudeDoBalanco,
            float parcial, float idadeEmTicks, float guinadaDaCabeca, float inclinacaoDaCabeca) {

        AuraVisualState estado = AuraVisualSystem.estadoDe(jogador);
        if (!estado.enabled()) {
            // CUSTO ZERO, e nao custo pequeno: sem aura, nada e montado,
            // nada e alocado e nenhum buffer e pedido.
            return;
        }
        // JOGADOR INVISIVEL NAO GANHA CONTORNO. Sem esta linha, a pocao de
        // invisibilidade passaria a REVELAR quem esta em Ten -- o oposto do que
        // ela faz, e uma informacao que o observador nao deveria ter.
        if (jogador.isInvisible() || jogador.isSpectator()) {
            return;
        }

        AuraShellOpacity opacidade = opacidadeDe(estado);
        AuraShellMaterial material = materialDe(estado);
        RenderType tipo = AuraRenderTypes.shell();

        // O TEMPO E POR ENTIDADE, e nao global. `idadeEmTicks` conta desde que
        // AQUELA entidade nasceu, entao dois jogadores nunca estao na mesma fase
        // do fluxo -- e sincronia acidental e a coisa mais artificial que um
        // efeito organico pode fazer. Custo declarado: respawn e troca de
        // dimensao recriam a entidade, e o fluxo da um salto.
        float tempo = idadeEmTicks / 20.0F;

        for (AuraShellPass passe : AuraShellPass.values()) {
            float alphaDoPasse = opacidade.alphaDe(passe) * estado.intensity();
            if (alphaDoPasse < ALPHA_MINIMO) {
                continue;
            }
            AuraPlayerModel modelo = this.modelos.get(passe);
            // COPIA A POSE FINAL. Ver o javadoc: `setupAnim` aqui DESCARTARIA
            // o que acabou de ser copiado.
            this.getParentModel().copyPropertiesTo(modelo);

            AuraShaders.configurar(tempo, material.fresnelDe(passe),
                    material.velocidadeDeFluxo(), material.escalaDeRuido(),
                    material.reforcoDaBorda());

            VertexConsumer vertices = buffers.getBuffer(tipo);
            desenharPorRegiao(modelo, pilha, vertices, luzEmpacotada, estado, alphaDoPasse);

            // DESCARREGA O LOTE AGORA, e nao no fim do quadro. Os uniformes sao
            // do PROGRAMA, e nao do vertice: sem esta descarga, os tres passes
            // seriam desenhados juntos no fim com os uniformes do ULTIMO, e as
            // tres camadas ficariam identicas -- exatamente o que os tres
            // expoentes de Fresnel existem para evitar.
            //
            // O preco e uma chamada de desenho por passe, por jogador. Esta
            // declarado, e e o AV8 que o ataca.
            descarregar(buffers, tipo);
        }

        desenharFilamentos(pilha, buffers, luzEmpacotada, jogador, estado, tempo);
    }

    /**
     * Os filamentos, agrupados POR PARTE do corpo.
     *
     * <p>AGRUPAR IMPORTA: empilhar a transformacao de uma parte custa uma
     * multiplicacao de matriz, e fazer isso por filamento repetiria a conta ate
     * vinte e oito vezes por jogador. Agrupado, sao seis.
     *
     * <p>A POSE E A DA PARTE, e nao a do modelo: e o que faz o filamento nascer
     * na superficie que se ve, e acompanhar o membro quando ele gira.
     */
    private void desenharFilamentos(PoseStack pilha, MultiBufferSource buffers, int luz,
            AbstractClientPlayer jogador, AuraVisualState estado, float tempo) {

        AuraRibbonProfile perfil = perfilDeFilamento(estado);
        if (perfil.quantidade() == 0) {
            return;
        }
        AuraPlayerModel modelo = this.modelos.get(AuraShellPass.BORDA);
        // A FOLGA SAI DA ESPESSURA DA BORDA, e nao de uma constante: engordar a
        // shell no perfil passaria a esconder os filamentos dentro dela.
        float folga = AuraCurve.folgaBase(
                AuraRenderRegistro.geometria().espessuraBorda());
        long semeadura = jogador.getUUID().getLeastSignificantBits();
        VertexConsumer buffer = buffers.getBuffer(AuraRenderTypes.ribbon());

        AuraAnchor[] ancoras = AuraAnchor.values();
        for (AuraBodyRegion regiao : AuraBodyRegion.values()) {
            float pesoDaRegiao = estado.distribution().intensidade(regiao);
            if (pesoDaRegiao < 0.02F) {
                continue;
            }
            pilha.pushPose();
            parteDe(modelo, regiao).translateAndRotate(pilha);
            PoseStack.Pose pose = pilha.last();

            for (int i = 0; i < perfil.quantidade(); i++) {
                AuraAnchor ancora = ancoras[i % ancoras.length];
                if (ancora.regiao() != regiao) {
                    continue;
                }
                // O CICLO E POR FILAMENTO, e defasado pelo indice: sem a
                // defasagem os oito trocariam de curva no MESMO quadro, e a
                // troca simultanea e visivel como um pisco.
                float fase = (i * 0.618F) % 1.0F;
                float t = tempo / perfil.cicloSegundos() + fase;
                int ciclo = (int) Math.floor(t);
                float dentroDoCiclo = t - ciclo;

                // O ENVELOPE ZERA NAS DUAS PONTAS DO CICLO. E o que torna a
                // troca de curva invisivel: o filamento some antes de virar
                // outro, em vez de saltar de uma forma para a seguinte.
                float envelope = (float) Math.sin(Math.PI * dentroDoCiclo);
                float alpha = estado.intensity() * pesoDaRegiao * envelope * 0.85F;
                if (alpha < ALPHA_MINIMO) {
                    continue;
                }

                long semente = AuraCurve.semente(semeadura, ancora, i, ciclo);
                this.filamentos.desenhar(buffer, pose, ancora, this.slim, semente, folga,
                        perfil.comprimentoDe(semente), perfil.largura(),
                        CorDaAura.comAlpha(estado.primaryColor(), alpha), luz);
            }
            pilha.popPose();
        }
        descarregar(buffers, AuraRenderTypes.ribbon());
    }

    /** O perfil de filamento do modo ativo. */
    static AuraRibbonProfile perfilDeFilamento(AuraVisualState estado) {
        return switch (estado.mode()) {
            case REN -> AuraRibbonProfile.ren();
            case TEN, CUSTOM -> AuraRibbonProfile.ten();
            case ZETSU, OFF -> AuraRibbonProfile.zero();
        };
    }

    /**
     * Forca a emissao do que ja foi acumulado neste tipo de render.
     *
     * <p>SILENCIOSA QUANDO A FONTE NAO SABE DESCARREGAR. Nem todo
     * {@code MultiBufferSource} e um lote -- capturas de tela e alguns mods
     * passam implementacoes proprias. Nesses casos as tres camadas sairao com os
     * mesmos uniformes, que e feio e nao e quebrado.
     */
    private static void descarregar(MultiBufferSource buffers, RenderType tipo) {
        if (buffers instanceof MultiBufferSource.BufferSource lote) {
            lote.endBatch(tipo);
        }
    }

    /** O material do modo ativo. Ver {@link #opacidadeDe} para o par dele. */
    static AuraShellMaterial materialDe(AuraVisualState estado) {
        return estado.mode() == com.darkcontinent.nenfoundation.client.vfx.AuraVisualMode.REN
                ? AuraShellMaterial.ren()
                : AuraShellMaterial.ten();
    }

    /**
     * A opacidade do modo ativo.
     *
     * <p>REN E TEN MAIS DENSO, e nao outro efeito -- e no AV0 essa diferenca
     * existe SO no alpha. A espessura maior de Ren mora no perfil, mas as
     * malhas sao construidas uma vez, no registro, com a geometria de Ten.
     * <b>Limitacao declarada do spike:</b> no AV0, Ren e mais forte, e nao mais
     * espesso. A shell propria de Ren e o AV4.
     */
    static AuraShellOpacity opacidadeDe(AuraVisualState estado) {
        return switch (estado.mode()) {
            case REN -> AuraShellOpacity.ren();
            case TEN, CUSTOM -> AuraShellOpacity.ten();
            // ZETSU e OFF nao chegam aqui -- `enabled()` ja barrou --, mas a
            // ausencia e a informacao, e ela precisa estar escrita no switch e
            // nao depender de uma guarda la em cima.
            case ZETSU, OFF -> AuraShellOpacity.zero();
        };
    }

    /**
     * Desenha as seis partes, cada uma com a intensidade da SUA regiao.
     *
     * <p>A distribuicao vem do servidor (ADR-014) e ja chega como projecao. Em
     * repouso todas valem o mesmo e o resultado e uma aura uniforme; com Gyo,
     * uma regiao acende e as outras recuam, sem nenhum codigo novo.
     */
    private static void desenharPorRegiao(AuraPlayerModel modelo, PoseStack pilha,
            VertexConsumer vertices, int luz, AuraVisualState estado, float alphaDoPasse) {
        for (AuraBodyRegion regiao : AuraBodyRegion.values()) {
            float alpha = alphaDoPasse * estado.distribution().intensidade(regiao);
            if (alpha < ALPHA_MINIMO) {
                continue;
            }
            parteDe(modelo, regiao).render(pilha, vertices, luz, OverlayTexture.NO_OVERLAY,
                    CorDaAura.comAlpha(estado.primaryColor(), alpha));
        }
    }

    private static ModelPart parteDe(AuraPlayerModel modelo, AuraBodyRegion regiao) {
        return switch (regiao) {
            case HEAD -> modelo.head;
            case TORSO -> modelo.body;
            case LEFT_ARM -> modelo.leftArm;
            case RIGHT_ARM -> modelo.rightArm;
            case LEFT_LEG -> modelo.leftLeg;
            case RIGHT_LEG -> modelo.rightLeg;
        };
    }

}
