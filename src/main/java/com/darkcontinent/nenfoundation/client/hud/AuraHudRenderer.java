package com.darkcontinent.nenfoundation.client.hud;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Desenha as barras de Aura (e Vigor, quando habilitado) no HUD do cliente.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. O RENDER NAO DECIDE SE O VIGOR EXISTE. Ele le {@code cache.temVigor()}.
 * A logica de "o servidor habilitou a segunda barra?" mora no cache, que
 * reflete o ultimo delta recebido. O renderer e burro de proposito.
 *
 * <p>2. INTERPOLACAO POR FRAME. Aura e Vigor tem campos "exibidos" que se
 * aproximam dos valores reais a uma taxa de lerp por frame. Isso faz as barras
 * esvaziar/encher suavemente entre deltas (que chegam a 1-2/s), sem nenhum
 * pacote extra.
 *
 * <p>3. NAO DESENHA ANTES DO PRIMEIRO DELTA. {@code cache.recebeuAlgumDelta()}
 * == false significa "ainda nao sei o estado do jogador". Desenhar barra vazia
 * nesse momento mentiria para o jogador logo no login.
 *
 * <p>4. POSICAO: canto superior esquerdo, 8px de margem. Cada barra tem
 * 100px de largura e 8px de altura, com 2px de separacao.
 *
 * <p>CLIENT-ONLY. Nao importar classes de server/, nen/ nem network/.
 */
public final class AuraHudRenderer {

    // -------------------------------------------------------- layout
    private static final int X = 8;
    private static final int Y_AURA = 8;
    private static final int Y_VIGOR = 20;
    private static final int LARGURA = 100;
    private static final int ALTURA = 8;

    // -------------------------------------------------------- cores (ARGB)
    private static final int COR_FUNDO = 0xAA000000;
    private static final int COR_AURA  = 0xFF44DD88;   // verde
    private static final int COR_OUTPUT = 0xFF4488DD;  // azul para output
    private static final int COR_EXAUSTAO = 0xFFDD4444; // vermelho quando critico

    // ------------------------------------------------------ interpolacao
    private static float auraExibida = 0.0F;
    private static float outputExibido = 1.0F;

    /** Velocidade de lerp por frame (1 = instantaneo, menor = mais suave). */
    private static final float LERP_VELOCIDADE = 0.12F;

    private AuraHudRenderer() {
    }

    /**
     * Registrar: {@code NenFoundationClient} chama este metodo uma vez.
     *
     * <p>Retorna o Runnable de cancelamento para remover o handler se necessario.
     */
    public static void registrar(net.neoforged.bus.api.IEventBus modEventBus) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(AuraHudRenderer::aoRenderizarHud);
    }

    private static void aoRenderizarHud(RenderGuiEvent.Post evento) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        NenClientCache cache = com.darkcontinent.nenfoundation.client.NenFoundationClient.cache();
        if (!cache.recebeuAlgumDelta()) return;

        float auraReal = cache.auraOuZero();
        float auraMax  = cache.auraMaximaOuZero();
        if (auraMax <= 0) return;

        // interpolacao suave por frame
        DeltaTracker dt = evento.getPartialTick();
        float delta = (float) dt.getGameTimeDeltaPartialTick(true);
        auraExibida = lerp(auraExibida, auraReal, LERP_VELOCIDADE * delta * 20);

        GuiGraphics gui = evento.getGuiGraphics();

        // --- barra de Aura ---
        float fracaoAura = Math.min(1.0F, auraMax > 0 ? auraExibida / auraMax : 0.0F);
        boolean emExaustao = fracaoAura < 0.10F;
        int corAura = emExaustao ? COR_EXAUSTAO : COR_AURA;

        desenharBarra(gui, X, Y_AURA, LARGURA, ALTURA, fracaoAura, corAura);

        // --- barra de Output (AOP) ---
        float outputReal = cache.outputPercent();
        outputExibido = lerp(outputExibido, outputReal, LERP_VELOCIDADE * delta * 20);
        float fracaoOutput = Math.max(0.0F, Math.min(1.0F, outputExibido));
        desenharBarra(gui, X, Y_VIGOR, LARGURA, ALTURA, fracaoOutput, COR_OUTPUT);
    }

    /**
     * Desenha fundo escuro + barra preenchida.
     *
     * @param fracao de 0.0 a 1.0
     */
    private static void desenharBarra(GuiGraphics gui, int x, int y,
            int largura, int altura, float fracao, int cor) {
        // fundo
        gui.fill(x, y, x + largura, y + altura, COR_FUNDO);
        // preenchimento
        int preenchido = Math.round(largura * fracao);
        if (preenchido > 0) {
            gui.fill(x, y, x + preenchido, y + altura, cor);
        }
    }

    private static float lerp(float atual, float alvo, float velocidade) {
        return atual + (alvo - atual) * Math.min(1.0F, velocidade);
    }
}
