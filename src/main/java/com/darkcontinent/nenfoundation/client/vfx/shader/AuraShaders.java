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
 * <p>OS UNIFORMES SAO ESCRITOS POR PASSE, e nao por quadro. Fresnel e fluxo
 * mudam entre a camada interna, a borda e o halo; quem desenha configura, emite
 * a geometria daquele passe e forca a descarga do lote. O custo -- uma chamada
 * de desenho por passe, por jogador -- esta declarado, e o AV8 e quem o ataca.
 */
public final class AuraShaders {

    private static final Logger LOG = LoggerFactory.getLogger(AuraShaders.class);

    @Nullable
    private static ShaderInstance shell;

    private AuraShaders() {
    }

    /** Registra o shader da shell. Mod event bus, so no cliente. */
    public static void registrar(RegisterShadersEvent evento) {
        try {
            evento.registerShader(
                    new ShaderInstance(evento.getResourceProvider(),
                            NenFoundation.id("aura_shell"),
                            // O MESMO FORMATO DA GEOMETRIA DE ENTIDADE. `ModelPart`
                            // emite posicao, cor, UV, overlay, luz e normal; um
                            // formato menor faria o buffer e o shader discordarem
                            // sobre o que cada byte significa.
                            DefaultVertexFormat.NEW_ENTITY),
                    instancia -> {
                        shell = instancia;
                        // SUCESSO TAMBEM E RELATADO, e nao so a falha. Sem esta
                        // linha, "o log nao acusou erro" seria indistinguivel de
                        // "o evento nunca disparou" -- e as duas situacoes
                        // produzem exatamente a mesma tela. Foi o que aconteceu
                        // na primeira execucao deste shader.
                        LOG.info("Shader da aura carregado: shell com Fresnel,"
                                + " ruido e fluxo proprios.");
                    });
        } catch (IOException erro) {
            // NAO RELANCA. Uma excecao aqui aborta o carregamento do cliente
            // inteiro por causa de um efeito.
            LOG.error("Shader da aura nao carregou; a shell cai para o material"
                    + " simples do AV0.", erro);
            shell = null;
        }
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
