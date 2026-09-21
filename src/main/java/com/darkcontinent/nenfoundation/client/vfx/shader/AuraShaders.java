package com.darkcontinent.nenfoundation.client.vfx.shader;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O shader proprio da shell, e o unico lugar que o guarda.
 *
 * <p>MODULO PEQUENO E PRESO A VERSAO, de proposito. A linha de
 * {@code RenderType} e de shader mudou no 1.21 e vai mudar de novo; quanto menos
 * codigo depender dela, menor a reescrita na proxima versao do Minecraft. Tudo
 * que sabe o que e um {@code ShaderInstance} cabe neste arquivo.
 *
 * <p>FALHAR AQUI NAO PODE DERRUBAR NADA. Se o shader nao compilar -- driver
 * antigo, GLSL recusado, arquivo ausente --, {@link #pronto()} passa a devolver
 * falso e quem desenha cai para o material simples do AV0. <b>Efeito visual
 * nunca crasha o jogo</b>, e essa e a mesma regra que o ADR-016 impoe ao bloom.
 *
 * <p><b>UM HOLDER SO PARA TODOS OS SHADERS DA AURA, e nao dois.</b> A issue
 * #195 propunha um {@code AuraShaderManager} ao lado deste arquivo, para os
 * shaders do passe de brilho. Seriam dois objetos guardando
 * {@code ShaderInstance} registrados no MESMO evento e soltos na MESMA recarga
 * de recurso -- ou seja, dois ciclos de vida para uma coisa so. O motivo de este
 * modulo ser pequeno e justamente esse: tudo que sabe o que e um
 * {@code ShaderInstance} cabe num arquivo, e a reescrita da proxima versao do
 * Minecraft tambem.
 *
 * <p>OS UNIFORMES SAO ESCRITOS POR PASSE, e nao por quadro. Fresnel e fluxo
 * mudam entre a camada interna, a borda e o halo; quem desenha configura, emite
 * a geometria daquele passe e forca a descarga do lote. O custo -- uma chamada
 * de desenho por passe, por jogador -- esta declarado, e o AV8 e quem o ataca.
 */
public final class AuraShaders {

    private static final Logger LOG = LoggerFactory.getLogger(AuraShaders.class);

    @Nullable
    private static ShaderInstance shell;

    @Nullable
    private static ShaderInstance downsample;

    @Nullable
    private static ShaderInstance blur;

    @Nullable
    private static ShaderInstance composite;

    private AuraShaders() {
    }

    /** Registra os shaders da aura. Mod event bus, so no cliente. */
    public static void registrar(RegisterShadersEvent evento) {
        shell = carregar(evento, "aura_shell", DefaultVertexFormat.NEW_ENTITY,
                "shell com Fresnel, ruido e fluxo proprios",
                instancia -> shell = instancia);
        // OS TRES DO PASSE DE BRILHO USAM `POSITION_TEX`, e nao `NEW_ENTITY`:
        // eles desenham um quadro de tela cheia, e um formato maior faria o
        // buffer e o shader discordarem sobre o que cada byte significa.
        carregar(evento, "aura_downsample", DefaultVertexFormat.POSITION_TEX,
                "reducao para meia resolucao", instancia -> downsample = instancia);
        carregar(evento, "aura_blur", DefaultVertexFormat.POSITION_TEX,
                "desfoque separavel", instancia -> blur = instancia);
        carregar(evento, "aura_composite", DefaultVertexFormat.POSITION_TEX,
                "composite aditivo", instancia -> composite = instancia);
    }

    /**
     * Carrega um shader, e NUNCA relanca.
     *
     * <p>Uma excecao aqui aborta o carregamento do cliente inteiro por causa de
     * um efeito visual. A falha de cada shader tem um caminho de degradacao
     * proprio -- a shell cai para o material simples do AV0, e o passe de brilho
     * cai para {@code FAST} --, e nenhum deles derruba nada.
     */
    @Nullable
    private static ShaderInstance carregar(RegisterShadersEvent evento, String nome,
            com.mojang.blaze3d.vertex.VertexFormat formato, String descricao,
            java.util.function.Consumer<ShaderInstance> guardar) {
        try {
            evento.registerShader(
                    new ShaderInstance(evento.getResourceProvider(),
                            NenFoundation.id(nome), formato),
                    instancia -> {
                        guardar.accept(instancia);
                        // SUCESSO TAMBEM E RELATADO, e nao so a falha. Sem esta
                        // linha, "o log nao acusou erro" seria indistinguivel de
                        // "o evento nunca disparou" -- e as duas situacoes
                        // produzem exatamente a mesma tela. Foi o que aconteceu
                        // na primeira execucao do shader de shell.
                        LOG.info("Shader da aura carregado: {} ({}).", nome, descricao);
                    });
        } catch (IOException erro) {
            LOG.error("Shader da aura '{}' nao carregou; o caminho de degradacao assume.",
                    nome, erro);
        }
        return null;
    }

    /** O shader de reducao para meia resolucao. Pode ser nulo. */
    @Nullable
    public static ShaderInstance downsample() {
        return downsample;
    }

    /** O shader de desfoque em um eixo. Pode ser nulo. */
    @Nullable
    public static ShaderInstance blur() {
        return blur;
    }

    /** O shader de composite aditivo. Pode ser nulo. */
    @Nullable
    public static ShaderInstance composite() {
        return composite;
    }

    /** Se os tres shaders do passe de brilho estao carregados. */
    public static boolean brilhoPronto() {
        return downsample != null && blur != null && composite != null;
    }

    /** Se o shader esta carregado e utilizavel. */
    public static boolean pronto() {
        return shell != null;
    }

    /** A instancia, para o {@code ShaderStateShard}. Pode ser nula. */
    @Nullable
    public static ShaderInstance shell() {
        return shell;
    }

    /**
     * Escreve os uniformes deste passe.
     *
     * <p>Silenciosa quando o shader nao esta pronto: quem chama ja decidiu o
     * caminho de fallback, e lancar aqui seria transformar um efeito degradado
     * numa tela preta.
     *
     * @param tempo        segundos desde o inicio da sessao, ja interpolado
     * @param fresnel      expoente; MENOR deixa a borda mais espessa
     * @param fluxo        velocidade com que a energia sobe pelo corpo
     * @param escala       quantas repeticoes do ruido cabem na superficie
     * @param reforcoDaBorda quanto o Fresnel soma a intensidade
     */
    public static void configurar(float tempo, float fresnel, float fluxo, float escala,
            float reforcoDaBorda) {
        ShaderInstance atual = shell;
        if (atual == null) {
            return;
        }
        escrever(atual, "AuraTime", tempo);
        escrever(atual, "AuraFresnelPower", fresnel);
        escrever(atual, "AuraFlowSpeed", fluxo);
        escrever(atual, "AuraNoiseScale", escala);
        escrever(atual, "AuraEdgeBoost", reforcoDaBorda);
    }

    private static void escrever(ShaderInstance instancia, String nome, float valor) {
        var uniforme = instancia.getUniform(nome);
        if (uniforme != null) {
            uniforme.set(valor);
        }
    }
}
