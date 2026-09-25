package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import com.darkcontinent.nenfoundation.client.hud.PaletaDaHud;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Uma barra de status: trilho, preenchimento e um fio de luz no topo.
 *
 * <p><b>UMA CLASSE PARA AS TRES.</b> Vida, Aura e Fluxo sao a mesma figura com
 * cores e alturas diferentes. Tres renderers quase iguais foi o que existia
 * antes, e o custo apareceu na hora de alinhar: cada um tinha a propria ideia
 * de onde a barra comeca.
 *
 * <p>SEM TEXTURA. A versao anterior blitava um gradiente de 256x16 por barra, o
 * que travava a cor no arquivo -- e este redesenho precisa tingir a Aura por
 * estado de Nen. Um PNG por estado seria arte gerada para representar uma
 * multiplicacao.
 *
 * <p>AS DIVISORIAS DE SEGMENTO SAIRAM. Elas existiam para dar escala, e numa
 * barra de sete pixels de altura por oitenta de largura elas competem com o
 * preenchimento em vez de medi-lo. O valor numerico ao lado ja da a escala, e
 * com precisao que dez tracinhos nao dao.
 */
public final class BarraDeStatusRenderer {

    private BarraDeStatusRenderer() {
    }

    /**
     * Desenha a barra.
     *
     * @param fracao ja saneada pelo chamador; valores fora de 0..1 sao presos
     * @param cor    ARGB do preenchimento; o trilho e derivado dela
     */
    public static void desenhar(GuiGraphics g, NenHudLayout.Retangulo area, float fracao,
            int cor) {
        if (area.largura() <= 0 || area.altura() <= 0) {
            return;
        }
        g.fill(area.x(), area.y(), area.fimX(), area.fimY(), PaletaDaHud.TRILHO);

        // UM FIO DA PROPRIA COR NO TRILHO VAZIO. Sem ele, uma barra em zero e
        // indistinguivel de uma barra que nao existe -- e "aura exausta" e
        // exatamente o estado em que o jogador mais precisa achar a barra.
        int fantasma = PaletaDaHud.comAlpha(PaletaDaHud.escurecer(cor, 0.55F), 0.55F);
        g.fill(area.x(), area.fimY() - 1, area.fimX(), area.fimY(), fantasma);

        int preenchida = NenHudLayout.preenchimento(area.largura(), fracao);
        if (preenchida <= 0) {
            return;
        }
        g.fill(area.x(), area.y(), area.x() + preenchida, area.fimY(), cor);

        // O fio de luz vive so sobre o preenchimento, e por isso ele TERMINA
        // onde o valor termina: e a leitura mais rapida de "ate aqui".
        if (area.altura() >= 3) {
            g.fill(area.x(), area.y(), area.x() + preenchida, area.y() + 1,
                    PaletaDaHud.clarear(cor, 0.45F));
        }
    }
}
