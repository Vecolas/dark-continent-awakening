package com.darkcontinent.nenfoundation.client.vfx.render;

import com.darkcontinent.nenfoundation.client.vfx.model.AuraGeometryProfile;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPlayerModel;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellPass;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Onde a aura se pendura no jogador.
 *
 * <p>DOIS EVENTOS, NESTA ORDEM, e a ordem e do NeoForge e nao nossa:
 * {@code RegisterLayerDefinitions} constroi as malhas; {@code AddLayers}
 * anexa a layer aos renderers que ja existem.
 *
 * <p>AS MALHAS SAO CONSTRUIDAS UMA VEZ. Sao seis -- tres passes vezes dois
 * modelos de jogador --, e depois disso so os uniformes mudam. Construir
 * geometria por frame e o primeiro item da lista de otimizacao de qualquer
 * renderer, e e mais barato nunca ter feito.
 *
 * <p>VARRE {@code getSkins()}, e nao uma lista de dois nomes escrita a mao. O
 * Minecraft entrega {@code default} e {@code slim}; se algum dia entregar um
 * terceiro, ele ganha aura sozinho. Lista escrita a mao e a que alguem esquece
 * de atualizar.
 */
public final class AuraRenderRegistro {

    private static final Logger LOG = LoggerFactory.getLogger(AuraRenderRegistro.class);

    /**
     * A geometria com que as malhas nascem.
     *
     * <p>E A DE TEN, e so ela. Ren usa a mesma malha com alpha maior no AV0 --
     * limitacao declarada do spike, resolvida no AV4.
     */
    private static final AuraGeometryProfile GEOMETRIA = AuraGeometryProfile.ten();

    /** A geometria com que as malhas foram construidas. Fonte unica. */
    public static AuraGeometryProfile geometria() {
        return GEOMETRIA;
    }

    private AuraRenderRegistro() {
    }

    /** Constroi as seis malhas infladas. */
    public static void registrarDefinicoes(EntityRenderersEvent.RegisterLayerDefinitions evento) {
        for (AuraShellPass passe : AuraShellPass.values()) {
            for (boolean slim : new boolean[] {false, true}) {
                evento.registerLayerDefinition(AuraModelLayers.de(passe, slim),
                        () -> AuraPlayerModel.definicao(GEOMETRIA.deformacaoDe(passe), slim));
            }
        }
    }

    /** Anexa a layer aos renderers de jogador -- aos DOIS modelos. */
    public static void adicionarLayers(EntityRenderersEvent.AddLayers evento) {
        int anexadas = 0;
        for (PlayerSkin.Model skin : evento.getSkins()) {
            EntityRenderer<?> renderer = evento.getSkin(skin);
            // INSTANCEOF, E NAO CAST DIRETO. Outro mod pode ter trocado o
            // renderer de jogador por um proprio; um cast cego derrubaria o
            // carregamento do cliente inteiro por causa de um efeito visual.
            if (!(renderer instanceof PlayerRenderer jogador)) {
                LOG.warn("Renderer de jogador '{}' nao e um PlayerRenderer ({}); a aura nao"
                        + " sera desenhada nele.", skin, renderer == null
                                ? "ausente" : renderer.getClass().getName());
                continue;
            }
            jogador.addLayer(new AuraPlayerRenderLayer(jogador, evento.getEntityModels(),
                    skin == PlayerSkin.Model.SLIM));
            anexadas++;
        }
        LOG.debug("Aura anexada a {} renderer(s) de jogador.", anexadas);
    }
}
