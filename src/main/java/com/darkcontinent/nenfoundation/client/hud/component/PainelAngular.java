package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import com.darkcontinent.nenfoundation.client.hud.PaletaDaHud;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A moldura da HUD: um painel com as pontas cortadas em diagonal.
 *
 * <p><b>DESENHADO, E NAO BLITADO.</b> Ate 2026-09-25 a moldura era um PNG de
 * 512x128 esticado para 250x63. Trocar a proporcao do painel deformaria a
 * textura, e a altura VARIAVEL -- que e o que permite o fluxo aparecer sem
 * empurrar nada -- seria impossivel: um blit estica, ele nao cresce so de um
 * lado. Desenhar tambem apaga a pergunta "em que resolucao esta arte foi
 * feita", que e a que faz HUD de mod ficar borrada em GUI Scale 1.
 *
 * <p>A DIAGONAL E UMA ESCADA DE UM PIXEL, e e assim que se faz com
 * {@code fill}, que so conhece retangulo. Em GUI Scale 2 ou 3 a escada vira a
 * borda serrilhada que a estetica de painel tecnico usa de proposito -- nao e
 * um antialiasing que faltou.
 *
 * <p>SO DUAS PONTAS SAO CORTADAS: a superior esquerda e a inferior direita. As
 * quatro cortadas dariam um losango, que le como "cartao" e nao como
 * "instrumento"; e a diagonal so na diagonal principal e o que faz o painel
 * apontar para o canto da tela em que ele mora.
 */
public final class PainelAngular {

    private PainelAngular() {
    }

    /**
     * Quanto a linha {@code linha} recua da esquerda.
     *
     * <p>PURA E PUBLICA para poder ser conferida sem abrir o jogo: o portao
     * mede a escada inteira sem instanciar {@code GuiGraphics}, que exigiria o
     * cliente de pe.
     *
     * @param linha    distancia do topo, em pixels
     * @param corte    tamanho da diagonal
     */
    public static int recuoEsquerdo(int linha, int corte) {
        if (linha < 0 || corte <= 0 || linha >= corte) {
            return 0;
        }
        return corte - 1 - linha;
    }

    /**
     * Quanto a linha {@code linha} recua da direita, num painel de {@code altura}.
     *
     * <p>Espelho exato do recuo esquerdo, medido a partir da ultima linha.
     */
    public static int recuoDireito(int linha, int altura, int corte) {
        if (corte <= 0 || altura <= 0) {
            return 0;
        }
        int daBase = altura - 1 - linha;
        if (daBase < 0 || daBase >= corte) {
            return 0;
        }
        return corte - 1 - daBase;
    }

    /** O painel inteiro: fundo, borda e os dois acentos de canto. */
    public static void desenhar(GuiGraphics g, NenHudLayout.Retangulo area, int corte) {
        preencher(g, area, corte, PaletaDaHud.FUNDO);
        contornar(g, area, corte, PaletaDaHud.BORDA);
        acentuar(g, area, corte, PaletaDaHud.ACENTO);
    }

    private static void preencher(GuiGraphics g, NenHudLayout.Retangulo area, int corte,
            int cor) {
        for (int linha = 0; linha < area.altura(); linha++) {
            int x0 = area.x() + recuoEsquerdo(linha, corte);
            int x1 = area.fimX() - recuoDireito(linha, area.altura(), corte);
            if (x1 > x0) {
                g.fill(x0, area.y() + linha, x1, area.y() + linha + 1, cor);
            }
        }
    }

    /**
     * O contorno de um pixel, acompanhando a escada.
     *
     * <p>A BORDA E FINA DE PROPOSITO. O diagnostico da HUD anterior foi "peso
     * visual concentrado no contorno"; um contorno de dois pixels numa caixa de
     * 44 de altura ocupa quase um decimo dela so em moldura.
     */
    private static void contornar(GuiGraphics g, NenHudLayout.Retangulo area, int corte,
            int cor) {
        for (int linha = 0; linha < area.altura(); linha++) {
            int y = area.y() + linha;
            int x0 = area.x() + recuoEsquerdo(linha, corte);
            int x1 = area.fimX() - recuoDireito(linha, area.altura(), corte);
            if (x1 <= x0) {
                continue;
            }
            g.fill(x0, y, x0 + 1, y + 1, cor);
            g.fill(x1 - 1, y, x1, y + 1, cor);
            // O topo e a base recebem a linha inteira; nas linhas do meio so as
            // duas pontas foram pintadas acima.
            if (linha == 0 || linha == area.altura() - 1) {
                g.fill(x0, y, x1, y + 1, cor);
            }
        }
    }

    /**
     * Os dois acentos: um tracinho ciano em cada ponta cortada.
     *
     * <p>E TODO O BRILHO QUE O PAINEL TEM. O plano pede "uso contido de brilho",
     * e acento que corre a moldura inteira deixa de ser acento e vira a cor da
     * moldura.
     */
    private static void acentuar(GuiGraphics g, NenHudLayout.Retangulo area, int corte,
            int cor) {
        int comprimento = Math.max(2, corte * 2);
        // Superior esquerda, na horizontal, logo depois do fim da diagonal.
        g.fill(area.x() + corte, area.y(), area.x() + corte + comprimento,
                area.y() + 1, cor);
        // Inferior direita, espelhada.
        g.fill(area.fimX() - corte - comprimento, area.fimY() - 1,
                area.fimX() - corte, area.fimY(), cor);
    }
}
