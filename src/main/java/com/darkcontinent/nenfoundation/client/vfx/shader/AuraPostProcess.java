package com.darkcontinent.nenfoundation.client.vfx.shader;

import com.darkcontinent.nenfoundation.client.vfx.AuraBloomLevel;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * O alvo de brilho da aura e a cadeia que o transforma em luz.
 *
 * <p>Executa o [ADR-016](docs/adr/ADR-016-pos-processamento-proprio-da-aura.md):
 * {@code AuraGlowTarget -> downsample 1/2 -> blur H -> blur V -> composite
 * aditivo}.
 *
 * <p><b>SO A CONTRIBUICAO LUMINOSA DA AURA ENTRA NO ALVO.</b> Nao a skin do
 * jogador, nao o mundo, nao a interface. Isso nao e garantido por ordem de
 * desenho feliz: e por CONSTRUCAO -- apenas os tipos de render de brilho da
 * aura tem um {@code OutputStateShard} apontando para este alvo, e nada mais no
 * jogo o conhece. Borrar o personagem inverteria a hierarquia de leitura, em que
 * o personagem e o item 1.
 *
 * <p><b>O ALVO E DE RESOLUCAO CHEIA, e o downsample e o passo seguinte -- ao
 * contrario do que a issue #194 escreveu.</b> A razao e do OpenGL, e nao de
 * gosto: a mascara de oclusao vem de COPIAR a profundidade da cena
 * ({@code copyDepthFrom}), e um blit de profundidade exige dimensoes IDENTICAS
 * entre origem e destino. Com o alvo ja em meia resolucao, a copia falharia --
 * silenciosamente, em muitos drivers -- e o halo passaria a atravessar parede,
 * que e justamente o vazamento de informacao que o gate do AV5 existe para
 * reprovar. O diagrama do proprio ADR-016 ja punha o downsample DEPOIS do alvo.
 *
 * <p><b>A PROFUNDIDADE E COPIADA ANTES DAS ENTIDADES</b>, quando os blocos
 * solidos ja escreveram a deles. Consequencia declarada: vidro, folhas e agua --
 * que sao translucidos e desenhados depois -- nao ocluem o halo. Parede solida,
 * que e o caso do gate, oclui.
 *
 * <p><b>SEM AURA NA TELA, O PASSE INTEIRO E PULADO</b> antes de qualquer
 * {@code bind} de framebuffer. Custo ZERO, e nao custo pequeno: nenhum alvo e
 * amarrado, nenhum shader e trocado, nenhum quadro de tela cheia e desenhado.
 *
 * <p><b>FALHAR AQUI NAO PODE DERRUBAR NADA.</b> Shader que nao compila ou alvo
 * que nao nasce rebaixam o nivel para {@code FAST} -- nunca para {@code OFF} --
 * e registram UMA linha no log, com motivo legivel. O rebaixamento e LEMBRADO na
 * sessao: tentar recompilar a cada quadro transformaria uma falha de driver num
 * congelamento.
 *
 * <p>QUEM LIGA, DESLIGA, e os dois contadores tem de bater: {@link #criados()} e
 * {@link #liberados()} estao lado a lado no overlay de dev exatamente porque
 * framebuffer nao recriado NAO DA ERRO -- da tela que some, ou memoria que sobe
 * devagar ao longo de uma sessao.
 */
public final class AuraPostProcess {

    private static final Logger LOG = LoggerFactory.getLogger(AuraPostProcess.class);

    /** Nunca criar alvo menor que isto, em pixels. Janela minimizada chega a zero. */
    private static final int LADO_MINIMO = 2;

    @Nullable
    private static RenderTarget brilho;
    @Nullable
    private static RenderTarget meiaA;
    @Nullable
    private static RenderTarget meiaB;

    private static int larguraDaTela;
    private static int alturaDaTela;

    private static int criados;
    private static int liberados;

    /** Se este quadro esta capturando aura para o alvo. */
    private static boolean capturando;

    /** Se o ultimo quadro pulou o passe inteiro. Regua do overlay. */
    private static boolean passePulado = true;

    @Nullable
    private static String motivoDoRebaixamento;

    private AuraPostProcess() {
    }

    // ------------------------------------------------- nivel efetivo

    /**
     * O nivel que esta REALMENTE rodando.
     *
     * <p>DIFERENTE DA CONFIG, e a distincao e o ponto: quem le a chave de config
     * como se fosse o estado real faz o overlay afirmar {@code HIGH} enquanto a
     * tela mostra {@code FAST}.
     */
    public static AuraBloomLevel nivelEfetivo() {
        // A SOBREPOSICAO DA SESSAO DE ARTE VEM ANTES DA CONFIG, e depois dela
        // o rebaixamento continua valendo: forcar HIGH sobre um pipeline que
        // nao o suporta produziria uma captura afirmando um nivel que a tela
        // nao desenha.
        AuraBloomLevel forcado =
                com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx.bloomForcado();
        AuraBloomLevel escolhido = forcado != null ? forcado
                : com.darkcontinent.nenfoundation.config.NenClientConfig.bloom();
        if (motivoDoRebaixamento != null) {
            return escolhido.rebaixado();
        }
        if (escolhido == AuraBloomLevel.HIGH && DeteccaoDeShaderPack.pipelineSubstituido()) {
            rebaixar("um pack de shader substituiu o pipeline de render");
            return AuraBloomLevel.FAST;
        }
        return escolhido;
    }

    /** O motivo do rebaixamento, ou {@code null} se nada falhou. */
    @Nullable
    public static String motivoDoRebaixamento() {
        return motivoDoRebaixamento;
    }

    /**
     * Rebaixa para {@code FAST} e registra UMA linha.
     *
     * <p>UMA, E NAO UMA POR QUADRO. Um log por quadro a sessenta hertz enche o
     * arquivo em minutos e esconde justamente a linha que importa -- e quem
     * abre o log procurando o motivo encontra dez mil copias dele.
     */
    static void rebaixar(String motivo) {
        if (motivoDoRebaixamento != null) {
            return;
        }
        motivoDoRebaixamento = motivo;
        LOG.warn("Brilho da aura rebaixado de HIGH para FAST: {}. O efeito continua"
                + " visivel pelo halo geometrico; nenhum alvo de render sera criado nesta"
                + " sessao.", motivo);
        liberar();
    }

    // ------------------------------------------------- ciclo de vida

    /**
     * Prepara o quadro: decide se captura, garante os alvos e copia a profundidade.
     *
     * <p>CHAMADO ANTES DAS ENTIDADES, porque e la que a aura e desenhada e a
     * copia de profundidade precisa ja estar feita.
     *
     * @param haAuraNaTela se alguma aura visivel existe neste quadro
     */
    public static void prepararQuadro(Minecraft mc, boolean haAuraNaTela) {
        capturando = false;
        if (nivelEfetivo() != AuraBloomLevel.HIGH || !haAuraNaTela) {
            // CUSTO ZERO. Nem o `bind` acontece -- e e por isso que a regua do
            // AV8 consegue afirmar zero em vez de "quase nada".
            passePulado = true;
            return;
        }
        RenderTarget principal = mc.getMainRenderTarget();
        if (!garantirAlvos(principal.width, principal.height)) {
            passePulado = true;
            return;
        }
        RenderTarget alvo = brilho;
        if (alvo == null) {
            passePulado = true;
            return;
        }
        alvo.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        alvo.clear(Minecraft.ON_OSX);
        // A MASCARA E A PROFUNDIDADE DA CENA. Sem esta copia, o alvo nasce com
        // profundidade vazia e TUDO passa no teste -- o halo apareceria atraves
        // de parede, que nao e feiura: e vazamento de informacao enquanto In e
        // Zetsu existirem.
        alvo.copyDepthFrom(principal);
        principal.bindWrite(false);
        capturando = true;
        passePulado = false;
    }

    /** Se os tipos de render de brilho devem escrever neste quadro. */
    public static boolean capturando() {
        return capturando;
    }

    /** O alvo de brilho, para o {@code OutputStateShard}. Pode ser nulo. */
    @Nullable
    public static RenderTarget alvo() {
        return brilho;
    }

    /**
     * Garante alvos do tamanho certo, recriando-os quando a janela muda.
     *
     * <p>REDIMENSIONAR E RECARREGAR RECURSO SAO OS DOIS CAMINHOS QUE MATAM
     * FRAMEBUFFER EM SILENCIO. Nenhum dos dois da erro: o primeiro da tela que
     * some, e o segundo da memoria que sobe devagar. A comparacao de tamanho
     * aqui e por quadro de proposito -- ela e duas comparacoes de inteiro, e
     * depender de um evento de resize amarraria a correcao a um gancho que muda
     * entre versoes.
     *
     * @return {@code false} se os alvos nao puderam ser criados
     */
    private static boolean garantirAlvos(int largura, int altura) {
        int l = Math.max(LADO_MINIMO, largura);
        int a = Math.max(LADO_MINIMO, altura);
        if (brilho != null && meiaA != null && meiaB != null
                && larguraDaTela == l && alturaDaTela == a) {
            return true;
        }
        // LIBERA O ANTIGO ANTES DE CRIAR O NOVO, e anula a referencia. Criar
        // primeiro e liberar depois deixa dois conjuntos vivos ao mesmo tempo, e
        // esquecer de liberar deixa o antigo vivo para sempre -- nos dois casos
        // sem uma linha de erro.
        liberar();
        try {
            // COM PROFUNDIDADE: e ela que da a mascara de oclusao.
            brilho = new TextureTarget(l, a, true, Minecraft.ON_OSX);
            // SEM PROFUNDIDADE nos intermediarios: eles sao passes de tela
            // cheia, e um buffer de profundidade que ninguem le e memoria
            // reservada para nada.
            meiaA = new TextureTarget(Math.max(LADO_MINIMO, l / 2),
                    Math.max(LADO_MINIMO, a / 2), false, Minecraft.ON_OSX);
            meiaB = new TextureTarget(Math.max(LADO_MINIMO, l / 2),
                    Math.max(LADO_MINIMO, a / 2), false, Minecraft.ON_OSX);
            brilho.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            meiaA.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            meiaB.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            criados += 3;
            larguraDaTela = l;
            alturaDaTela = a;
            return true;
        } catch (RuntimeException erro) {
            // NAO RELANCA. Uma excecao aqui acontece dentro do laco de render, e
            // derrubaria o desenho do mundo inteiro por causa de um efeito.
            LOG.error("Alvo de brilho da aura nao pode ser criado em {}x{}.", l, a, erro);
            liberar();
            rebaixar("o alvo de render nao pode ser criado (" + erro.getClass()
                    .getSimpleName() + ")");
            return false;
        }
    }

    /**
     * Roda a cadeia e soma o resultado na cena.
     *
     * @param peso  forca do perfil vezes o multiplicador do jogador
     * @param raioEmPixels raio do desfoque, em pixels de TELA
     */
    public static void aplicar(Minecraft mc, float peso, float raioEmPixels) {
        if (!capturando || brilho == null || meiaA == null || meiaB == null) {
            return;
        }
        capturando = false;
        if (peso <= 0.0F || raioEmPixels <= 0.0F) {
            return;
        }
        ShaderInstance reduzir = AuraShaders.downsample();
        ShaderInstance borrar = AuraShaders.blur();
        ShaderInstance somar = AuraShaders.composite();
        if (reduzir == null || borrar == null || somar == null) {
            rebaixar("um dos shaders do passe de brilho nao carregou");
            return;
        }

        try {
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            // 1. MEIA RESOLUCAO. Quatro amostras, e nao uma: reduzir pegando um
            //    pixel de cada quatro faz filamento fino CINTILAR em movimento,
            //    e cintilacao nao aparece em imagem parada.
            escrever(reduzir, meiaA, brilho.getColorTextureId(), () -> {
                var u = reduzir.getUniform("AuraMeioTexel");
                if (u != null) {
                    u.set(0.5F / brilho.width, 0.5F / brilho.height);
                }
            });

            // 2 e 3. DESFOQUE SEPARAVEL. O passo ja chega multiplicado pelo raio
            //        e convertido para texels do alvo de MEIA resolucao -- um
            //        raio em pixels de tela vale metade disso aqui.
            float raioEmTexels = Math.clamp(raioEmPixels * 0.5F, 0.0F,
                    com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilDeBrilho
                            .RAIO_MAXIMO);
            float[] pesos = KernelDeBorrao.pesos(raioEmTexels);

            escrever(borrar, meiaB, meiaA.getColorTextureId(), () -> {
                aplicarPesos(borrar, pesos);
                var u = borrar.getUniform("AuraPassoDoBorrao");
                if (u != null) {
                    u.set(1.0F / meiaA.width, 0.0F);
                }
            });
            escrever(borrar, meiaA, meiaB.getColorTextureId(), () -> {
                aplicarPesos(borrar, pesos);
                var u = borrar.getUniform("AuraPassoDoBorrao");
                if (u != null) {
                    u.set(0.0F, 1.0F / meiaB.height);
                }
            });

            // 4. COMPOSITE ADITIVO de volta na cena. A cena por baixo continua
            //    intacta: este passe SOMA luz, e nunca substitui.
            RenderTarget principal = mc.getMainRenderTarget();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            escrever(somar, principal, meiaA.getColorTextureId(), () -> {
                var u = somar.getUniform("AuraPesoDoBrilho");
                if (u != null) {
                    u.set(peso);
                }
            });
        } catch (RuntimeException erro) {
            LOG.error("O passe de brilho da aura explodiu; caindo para FAST.", erro);
            rebaixar("o passe de brilho lancou em runtime ("
                    + erro.getClass().getSimpleName() + ")");
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    private static void aplicarPesos(ShaderInstance shader, float[] pesos) {
        var a = shader.getUniform("AuraPesosA");
        var b = shader.getUniform("AuraPesosB");
        if (a != null) {
            a.set(pesos[0], pesos[1], pesos[2], pesos[3]);
        }
        if (b != null) {
            b.set(pesos[4], pesos[5], pesos[6], pesos[7]);
        }
    }

    /**
     * Desenha um quadro de tela cheia com o shader dado.
     *
     * <p>SEM MATRIZ: a posicao ja vai em coordenada normalizada de dispositivo,
     * entao nao ha o que projetar. E o caminho com menos superficie de contato
     * com a linha de {@code RenderSystem}, que e a que mais muda entre versoes.
     */
    private static void escrever(ShaderInstance shader, RenderTarget destino, int textura,
            Runnable uniformes) {
        destino.bindWrite(true);
        shader.setSampler("Sampler0", textura);
        uniformes.run();
        shader.apply();

        BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(-1.0F, -1.0F, 0.0F).setUv(0.0F, 0.0F);
        buffer.addVertex(1.0F, -1.0F, 0.0F).setUv(1.0F, 0.0F);
        buffer.addVertex(1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F);
        buffer.addVertex(-1.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F);
        BufferUploader.draw(buffer.buildOrThrow());
        shader.clear();
    }

    /**
     * Libera os alvos.
     *
     * <p>QUEM LIGA, DESLIGA -- e a referencia e ANULADA junto. Liberar sem
     * anular deixa um objeto apontando para um buffer que ja nao existe, e o
     * proximo uso desenha em lugar nenhum sem lancar.
     */
    public static void liberar() {
        if (brilho != null) {
            brilho.destroyBuffers();
            brilho = null;
            liberados++;
        }
        if (meiaA != null) {
            meiaA.destroyBuffers();
            meiaA = null;
            liberados++;
        }
        if (meiaB != null) {
            meiaB.destroyBuffers();
            meiaB = null;
            liberados++;
        }
        larguraDaTela = 0;
        alturaDaTela = 0;
        capturando = false;
    }

    /**
     * Recarga de recurso (F3+T): solta tudo e ESQUECE o rebaixamento.
     *
     * <p>ESQUECER O REBAIXAMENTO E O PONTO. Se o motivo da queda foi um resource
     * pack com shader torto e a pessoa o removeu, recarregar precisa devolver o
     * nivel escolhido -- caso contrario a unica saida seria reiniciar o jogo, e
     * ninguem liga "o bloom nao volta" a uma variavel de sessao.
     */
    public static void aoRecarregarRecursos() {
        liberar();
        motivoDoRebaixamento = null;
        DeteccaoDeShaderPack.esquecer();
    }

    // ------------------------------------------------- reguas

    /** Quantos alvos foram criados nesta sessao. */
    public static int criados() {
        return criados;
    }

    /** Quantos alvos foram liberados nesta sessao. Tem de bater com os criados. */
    public static int liberados() {
        return liberados;
    }

    /** Quantos alvos estao vivos agora. Zero quando o passe nao esta em uso. */
    public static int vivos() {
        return criados - liberados;
    }

    /** O tamanho do alvo de brilho, ou {@code null} quando nao ha alvo. */
    @Nullable
    public static String tamanhoDoAlvo() {
        RenderTarget alvo = brilho;
        return alvo == null ? null : alvo.width + "x" + alvo.height;
    }

    /** Se o ultimo quadro pulou o passe inteiro. */
    public static boolean passePulado() {
        return passePulado;
    }

    /** Quem liga, desliga: o logout zera os contadores junto com o resto. */
    public static void limparContadores() {
        criados = 0;
        liberados = 0;
    }
}
