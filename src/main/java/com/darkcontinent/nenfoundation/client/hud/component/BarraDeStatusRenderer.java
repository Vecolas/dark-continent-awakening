package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import com.darkcontinent.nenfoundation.client.hud.PaletaDaHud;
import net.minecraft.client.gui.GuiGraphics;

/**
 * As barras da HUD -- e elas sao de DUAS naturezas, nao de uma.
 *
 * <p><b>O DIAGNOSTICO:</b> "hoje vida e aura parecem variacoes do mesmo
 * objeto". Elas precisam parecer do mesmo SISTEMA e de naturezas diferentes --
 * e a diferenca tem de vir da forma, e nao so da cor, porque cor sozinha nao
 * separa para cerca de um em doze homens.
 *
 * <p>A separacao escolhida:
 *
 * <table border="1">
 *   <caption>As duas naturezas</caption>
 *   <tr><th></th><th>Vital</th><th>Fluxo</th></tr>
 *   <tr><td>preenchimento</td><td>solido e continuo</td><td>segmentado</td></tr>
 *   <tr><td>trilho vazio</td><td>liso</td><td>com trilha fantasma</td></tr>
 *   <tr><td>brilho</td><td>fio fixo no topo</td><td>crista que ANDA com o pulso</td></tr>
 *   <tr><td>sensacao</td><td>estabilidade, autoridade</td><td>reservatorio, corrente</td></tr>
 * </table>
 *
 * <p>VIDA E MAIS BAIXA QUE AURA na chamada do layout, e isso reforca o mesmo:
 * a leitura vital e compacta e firme; a de aura tem corpo e respira.
 */
public final class BarraDeStatusRenderer {

    /** Largura de cada celula da barra de fluxo, em pixels. */
    private static final int CELULA = 4;

    /** Vao entre celulas. Um pixel: mais que isso vira tracejado. */
    private static final int VAO = 1;

    private BarraDeStatusRenderer() {
    }

    /**
     * A leitura VITAL: solida, estavel, sem animacao propria.
     *
     * <p>SEM SEGMENTO E SEM CRISTA de proposito. Vida nao flui -- ela esta ou
     * nao esta --, e uma barra de vida que se mexe sozinha e lida como perigo
     * mesmo quando esta cheia.
     */
    public static void vital(GuiGraphics g, NenHudLayout.Retangulo area, float fracao,
            int cor) {
        if (area.largura() <= 0 || area.altura() <= 0) {
            return;
        }
        trilho(g, area, cor);
        int preenchida = NenHudLayout.preenchimento(area.largura(), fracao);
        if (preenchida <= 0) {
            return;
        }
        g.fill(area.x(), area.y(), area.x() + preenchida, area.fimY(), cor);
        if (area.altura() >= 3) {
            g.fill(area.x(), area.y(), area.x() + preenchida, area.y() + 1,
                    PaletaDaHud.clarear(cor, 0.4F));
        }
        // O TERMINAL: um pixel cheio na ponta do preenchimento. E o que da
        // "autoridade" -- a barra termina num batente, e nao esmaece.
        g.fill(area.x() + preenchida - 1, area.y(), area.x() + preenchida, area.fimY(),
                PaletaDaHud.clarear(cor, 0.7F));
    }

    /**
     * A leitura de FLUXO: segmentada, com trilha fantasma e crista movel.
     *
     * <p>A CRISTA E O UNICO ELEMENTO DA HUD QUE ANDA. Ela e uma celula mais
     * clara que percorre a parte preenchida, e existe para a aura parecer
     * corrente e nao nivel. Ela e contida: uma celula, e so dentro do que ja
     * esta cheio.
     *
     * @param fase de 0 a 1; de onde vem o movimento da crista
     */
    public static void fluxo(GuiGraphics g, NenHudLayout.Retangulo area, float fracao,
            int cor, float fase) {
        if (area.largura() <= 0 || area.altura() <= 0) {
            return;
        }
        trilho(g, area, cor);

        int preenchida = NenHudLayout.preenchimento(area.largura(), fracao);
        int celulas = Math.max(1, (area.largura() + VAO) / (CELULA + VAO));
        int cheias = Math.round(celulas * Math.clamp(fracao, 0.0F, 1.0F));

        // A TRILHA FANTASMA cobre a barra inteira, inclusive o vazio: e o que
        // mostra a ESCALA. Sem ela, uma aura em 10% e indistinguivel de uma
        // barra curta.
        for (int i = 0; i < celulas; i++) {
            int x = area.x() + i * (CELULA + VAO);
            int larg = Math.min(CELULA, area.fimX() - x);
            if (larg <= 0) {
                break;
            }
            boolean cheia = i < cheias;
            int tinta = cheia ? cor
                    : PaletaDaHud.comAlpha(PaletaDaHud.escurecer(cor, 0.6F), 0.35F);
            g.fill(x, area.y(), x + larg, area.fimY(), tinta);
            if (cheia && area.altura() >= 3) {
                g.fill(x, area.y(), x + larg, area.y() + 1, PaletaDaHud.clarear(cor, 0.45F));
            }
        }

        if (cheias > 0 && preenchida > 0) {
            int indice = (int) (Math.clamp(fase, 0.0F, 1.0F) * cheias) % Math.max(1, cheias);
            int x = area.x() + indice * (CELULA + VAO);
            int larg = Math.min(CELULA, area.fimX() - x);
            if (larg > 0) {
                g.fill(x, area.y(), x + larg, area.fimY(),
                        PaletaDaHud.comAlpha(PaletaDaHud.clarear(cor, 0.65F), 0.55F));
            }
        }
    }

    /**
     * A microbarra contextual: fina, sem celula, sem crista.
     *
     * <p>ELA NAO COMPETE. So aparece em contexto, e um terceiro tratamento
     * visual na mesma coluna faria as tres brigarem.
     */
    public static void micro(GuiGraphics g, NenHudLayout.Retangulo area, float fracao,
            int cor) {
        if (area.largura() <= 0 || area.altura() <= 0) {
            return;
        }
        g.fill(area.x(), area.y(), area.fimX(), area.fimY(),
                PaletaDaHud.comAlpha(PaletaDaHud.TRILHO, 0.8F));
        int preenchida = NenHudLayout.preenchimento(area.largura(), fracao);
        if (preenchida > 0) {
            g.fill(area.x(), area.y(), area.x() + preenchida, area.fimY(), cor);
        }
    }

    /**
     * O leito da barra, comum as duas naturezas.
     *
     * <p>O FIO DA PROPRIA COR NO FUNDO existe para o caso de reserva zerada:
     * sem ele, uma barra vazia e indistinguivel de uma barra que nao existe --
     * e "aura exausta" e exatamente quando o jogador mais precisa acha-la.
     */
    private static void trilho(GuiGraphics g, NenHudLayout.Retangulo area, int cor) {
        g.fill(area.x(), area.y(), area.fimX(), area.fimY(), PaletaDaHud.TRILHO);
        g.fill(area.x(), area.fimY() - 1, area.fimX(), area.fimY(),
                PaletaDaHud.comAlpha(PaletaDaHud.escurecer(cor, 0.55F), 0.55F));
    }

    /** Largura de celula mais vao. Para o portao conferir a contagem. */
    public static int passoDaCelula() {
        return CELULA + VAO;
    }
}
