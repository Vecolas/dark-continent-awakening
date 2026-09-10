package com.darkcontinent.nenfoundation.client.screen;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.network.NenProtocol;
import com.darkcontinent.nenfoundation.network.handler.Recebedores;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * O overlay tecnico: o que o cliente recebeu, e quando.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. ELE E SEPARADO DO HUD DE JOGADOR, e vai continuar sendo. Misturar os
 * dois faz numero de diagnostico vazar para a tela de quem esta jogando — e,
 * pior, faz o HUD depender de dado que so existe em modo dev.
 *
 * <p>2. Ele mostra a diferenca entre "zero" e "nunca recebi". Um HUD que
 * desenha barra vazia porque nada chegou mente para o jogador no login, e essa
 * e a primeira coisa que se quer enxergar ao depurar sincronizacao.
 *
 * <p>3. Ele exige {@code dev.enabled} NA CONFIG e a tecla ligada. Duas
 * condicoes de proposito: a config impede que um servidor de jogo mostre isso
 * por acidente, e a tecla evita que o desenvolvedor tenha diagnostico na tela
 * o tempo todo.
 *
 * <p>4. A configuracao e lida A CADA QUADRO, e nao guardada num campo no boot.
 * A config e recarregavel; uma copia guardada ignora a recarga sem dar erro.
 *
 * <p>CLIENT-ONLY.
 */
public final class OverlayDeDebug {

    private static final int MARGEM = 4;
    private static final int ALTURA_DA_LINHA = 10;
    private static final int COR = 0xFFE0E0E0;

    private final NenClientCache cache;
    private boolean visivel;

    public OverlayDeDebug(NenClientCache cache) {
        this.cache = cache;
    }

    /** Chamado pela tecla. Idempotente por natureza: e um alternador. */
    public void alternar() {
        this.visivel = !this.visivel;
    }

    public boolean visivel() {
        return this.visivel;
    }

    public void aoRenderizar(RenderGuiEvent.Post evento) {
        if (!this.visivel || !NenConfig.devModeAtivo()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }

        GuiGraphics g = evento.getGuiGraphics();
        int y = MARGEM;
        for (String linha : linhas()) {
            g.drawString(mc.font, linha, MARGEM, y, COR, true);
            y += ALTURA_DA_LINHA;
        }
    }

    /**
     * O conteudo do overlay, como texto puro.
     *
     * <p>Separado da renderizacao para poder ser testado sem o jogo carregado.
     */
    public List<String> linhas() {
        List<String> l = new ArrayList<>();
        l.add("[Nen] protocolo v" + NenProtocol.VERSION
                + " | recebedor: " + (Recebedores.temRecebedor() ? "sim" : "NAO"));

        l.add("perfil: " + (this.cache.recebeuAlgumSnapshot()
                ? this.cache.categoriaVisivel().getSerializedName()
                : "nunca recebido"));

        // A distincao que este overlay existe para mostrar.
        l.add("aura: " + (this.cache.recebeuAlgumDelta()
                ? this.cache.auraOuZero() + " / " + this.cache.auraMaximaOuZero()
                : "nunca recebida (nao e zero)"));

        long desde = this.cache.ticksDesdeOUltimoDelta();
        l.add("ultimo delta: " + (desde < 0 ? "nenhum" : desde + " ticks atras"));

        l.add("recebidos  snapshot=" + this.cache.snapshotsRecebidos()
                + "  delta=" + this.cache.deltasRecebidos()
                + "  fx=" + this.cache.fxRecebidos()
                + "  erro=" + this.cache.errosRecebidos());

        this.cache.ultimoErro().ifPresent(e -> l.add("ultimo erro: " + e));
        return l;
    }
}
