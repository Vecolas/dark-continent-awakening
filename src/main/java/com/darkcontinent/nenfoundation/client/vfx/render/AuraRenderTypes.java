package com.darkcontinent.nenfoundation.client.vfx.render;

import com.darkcontinent.nenfoundation.NenFoundation;
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
 * estado a toa e quebram o agrupamento de draw calls.
 *
 * <p><b>Por que emissivo, e o que isso custa no AV0.</b>
 * {@code entityTranslucentEmissive} da exatamente o estado que a shell precisa
 * -- translucido, sem cull, e <b>sem escrita de profundidade</b>, que e o que
 * impede uma camada de esconder a outra. O preco e que ele ignora a luz do
 * mundo: no AV0 a aura fica fullbright, e dentro de caverna isso vira um borrao
 * branco. A mistura de emissivo com luz do mundo (algo como 60-80% / 20-40%) e
 * trabalho do shader proprio, no AV1. <b>Isto e limitacao declarada do spike,
 * nao descuido.</b>
 *
 * <p>NAO HA {@code RenderType} PROPRIO AINDA. {@code RenderType.create} nao e
 * acessivel de fora do pacote, e widen-la exigiria um access transformer e uma
 * linha no {@code build.gradle} -- o arquivo mais hostil do repositorio. O AV0
 * nao precisa disso; o AV1 precisa, e paga quando for necessario.
 */
public final class AuraRenderTypes {

    /**
     * A textura da shell no AV0: branca, chapada, autoral.
     *
     * <p>Ela e branca de proposito: quem colore e a cor por vertice, que vem da
     * mesma {@code AparenciaDeTecnica} que o HUD e a roda usam. Duas fontes para
     * "de que cor e Ren" garantem que um dia as duas discordem.
     */
    public static final ResourceLocation TEXTURA_DA_SHELL =
            NenFoundation.id("textures/vfx/nen/aura_shell_plano.png");

    private static final RenderType SHELL =
            RenderType.entityTranslucentEmissive(TEXTURA_DA_SHELL);

    private AuraRenderTypes() {
    }

    /** O tipo de render da shell. Sempre a mesma instancia. */
    public static RenderType shell() {
        return SHELL;
    }
}
