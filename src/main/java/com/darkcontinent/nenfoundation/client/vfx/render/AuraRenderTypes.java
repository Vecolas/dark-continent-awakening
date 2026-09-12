package com.darkcontinent.nenfoundation.client.vfx.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.client.vfx.shader.AuraShaders;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * A tabela unica de tipos de render da aura.
 *
 * <p>ARQUIVO HOSTIL A MERGE: todos acrescentam linhas aqui, como em
 * {@code NenProtocol}. Uma pessoa por vez, e diga no titulo do PR.
 *
 * <p>ELE EXISTE PARA CENTRALIZAR blend, profundidade, cull e mascara de
 * escrita. Espalhados pelos renderers, cada passe acabaria com a propria ideia
 * de transparencia, e o resultado seria uma aura que muda de material conforme
 * quem a desenhou.
 *
 * <p>CACHEADO, e nunca criado por frame. {@code RenderType} entra em mapas e em
 * comparacoes de estado do buffer; instancias novas a cada quadro trocam de
 * estado a toa e quebram o agrupamento de chamadas de desenho.
 *
 * <p><b>O estado, e por que cada peca esta assim:</b>
 *
 * <ul>
 *   <li><b>transparencia translucida</b> -- alpha, e nao aditivo puro. Tudo
 *       aditivo estoura; tudo alpha fica plastico. A borda quase branca vem do
 *       shader, e nao de somar luz sobre luz.</li>
 *   <li><b>sem cull</b> -- a shell e uma casca fina, e as faces de tras
 *       participam da leitura. O shader usa {@code abs} no Fresnel justamente
 *       porque elas chegam com a normal invertida.</li>
 *   <li><b>escrita de profundidade DESLIGADA</b> ({@code COLOR_WRITE}) -- e o
 *       que impede uma camada de esconder a outra. Com escrita ligada, a camada
 *       externa apagaria as duas de dentro e as tres viram uma.</li>
 *   <li><b>teste de profundidade LIGADO</b> -- a aura nao atravessa parede.
 *       Enquanto In e Zetsu existirem, isso e vazamento de informacao, e nao
 *       feiura.</li>
 *   <li><b>textura com filtro linear</b> -- o ruido e um desenho continuo de
 *       filamentos; amostra-lo sem suavizar produziria degraus visiveis.</li>
 * </ul>
 *
 * <p>NAO FOI PRECISO ACCESS TRANSFORMER. O NeoForge ja abre
 * {@code RenderType.create} e {@code RenderStateShard} inteiro no proprio
 * transformador -- conferido no {@code ats/accesstransformer.cfg} da versao
 * 21.1.250, e nao suposto. O {@code build.gradle}, que e o arquivo mais hostil
 * do repositorio, fica intocado.
 */
public final class AuraRenderTypes {

    /** O ruido autoral: filamentos verticais ondulados, e nao manchas. */
    public static final ResourceLocation TEXTURA_DE_RUIDO =
            NenFoundation.id("textures/vfx/nen/aura_noise_veios.png");

    /** A textura chapada do AV0, usada so quando o shader nao esta disponivel. */
    public static final ResourceLocation TEXTURA_PLANA =
            NenFoundation.id("textures/vfx/nen/aura_shell_plano.png");

    private static final RenderType SHELL = RenderType.create(
            "nenfoundation_aura_shell",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            // Tamanho de buffer modesto: a shell sao seis caixas, nao um chunk.
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(AuraShaders::shell))
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            TEXTURA_DE_RUIDO, true, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));

    /**
     * O caminho de degradacao, herdado do AV0.
     *
     * <p>Ele existe para o dia em que o shader nao compilar. Perde o Fresnel, o
     * ruido e o fluxo -- vira uma pelicula chapada --, mas o jogo continua de
     * pe e a aura continua existindo.
     */
    private static final RenderType SHELL_SIMPLES =
            RenderType.entityTranslucentEmissive(TEXTURA_PLANA);

    /** A textura do filamento: nucleo claro, borda em fade. Autoral (ADR-007). */
    public static final ResourceLocation TEXTURA_DE_RIBBON =
            NenFoundation.id("textures/vfx/nen/aura_ribbon_core.png");

    /**
     * O material dos filamentos.
     *
     * <p>SEM SHADER PROPRIO NO AV2, de proposito. O gate deste marco e
     * geometrico -- "os filamentos nascem na superficie e acompanham os
     * membros" --, e um shader novo so acrescentaria uma variavel a mais entre
     * o codigo e a resposta. O material emissivo da o estado certo: translucido,
     * sem cull, e sem escrita de profundidade.
     *
     * <p>NO_CULL IMPORTA AQUI mais do que na shell: a tira e uma superficie de
     * espessura zero, e a orientacao de face esta invertida pelo
     * {@code scale(-1,-1,1)} do renderer de entidade. Com cull ligado, metade
     * dos filamentos desapareceria -- e os que sumissem dependeriam do angulo
     * da camera, que e o pior tipo de bug para reproduzir.
     */
    private static final RenderType RIBBON =
            RenderType.entityTranslucentEmissive(TEXTURA_DE_RIBBON);

    private AuraRenderTypes() {
    }

    /**
     * O tipo de render da shell, ja escolhido conforme o shader carregou ou nao.
     *
     * <p>A ESCOLHA E FEITA AQUI, e nao em quem desenha: senao cada renderer novo
     * repetiria o teste, e um deles esqueceria.
     */
    public static RenderType shell() {
        return AuraShaders.pronto() ? SHELL : SHELL_SIMPLES;
    }

    /** O tipo de render dos filamentos. Sempre a mesma instancia. */
    public static RenderType ribbon() {
        return RIBBON;
    }

    /** Se o que sera desenhado usa o shader proprio. */
    public static boolean comShaderProprio() {
        return AuraShaders.pronto();
    }
}
