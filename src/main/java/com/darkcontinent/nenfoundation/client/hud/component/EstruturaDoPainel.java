package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import com.darkcontinent.nenfoundation.client.hud.PaletaDaHud;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A estrutura da HUD: plataforma, suportes e trilho. <b>Nunca uma caixa.</b>
 *
 * <p><b>O DIAGNOSTICO QUE ESTE ARQUIVO RESPONDE</b> foi de linguagem visual, e
 * nao de layout: a HUD parecia "um retangulo com barras", um overlay de RPG
 * adaptado. O redesenho anterior encolheu e alinhou -- e continuou sendo uma
 * caixa, so que menor e melhor diagramada.
 *
 * <p>A regra que substitui a moldura: <b>contorno quebrado, e peca que sugere
 * funcao.</b> Nada aqui desenha um retangulo fechado em volta do conteudo. O
 * que existe e:
 *
 * <ul>
 *   <li><b>plataforma</b> -- uma base translucida SO atras das linhas de
 *       leitura, e nao atras do modulo inteiro. O retrato fica fora dela, e e
 *       esse vazio que faz os dois lerem como pecas separadas;
 *   <li><b>suportes</b> -- dois colchetes em cantos opostos, e nao quatro:
 *       quatro fecham a forma de volta num retangulo implicito;
 *   <li><b>trilho</b> -- a linha vertical que liga o nucleo as leituras, com um
 *       no em cada derivacao. E o que faz a HUD parecer montada em cima de uma
 *       espinha, em vez de empilhada dentro de um quadro.
 * </ul>
 *
 * <p>TUDO E DESENHADO, e nenhum pixel vem de arquivo. Alem de a altura ser
 * variavel -- um blit estica, ele nao cresce so de um lado --, uma estrutura em
 * codigo permite que o trilho ganhe um no quando a barra contextual aparece.
 */
public final class EstruturaDoPainel {

    /** O comprimento de cada perna dos colchetes de canto. */
    private static final int PERNA = 9;

    /** Raio do no de derivacao no trilho. */
    private static final int NO = 2;

    private EstruturaDoPainel() {
    }

    /**
     * A base translucida, so atras da area de leitura.
     *
     * <p>ELA COMECA DEPOIS DO NUCLEO. O retrato fica sobre o mundo, sem chapa
     * atras, e e isso que o separa visualmente do resto -- o vazio entre as
     * duas pecas e o que as faz parecer acopladas em vez de impressas no mesmo
     * fundo.
     *
     * <p>O CANTO INFERIOR DIREITO E CHANFRADO, e so ele. Um chanfro em cada
     * canto vira um losango; um so da direcao a peca, e aponta para fora da
     * tela, longe do conteudo.
     */
    public static void plataforma(GuiGraphics g, NenHudLayout.Retangulo area, int corte) {
        for (int linha = 0; linha < area.altura(); linha++) {
            int recuo = PainelAngular.recuoDireito(linha, area.altura(), corte);
            int x1 = area.fimX() - recuo;
            if (x1 > area.x()) {
                g.fill(area.x(), area.y() + linha, x1, area.y() + linha + 1,
                        PaletaDaHud.FUNDO);
            }
        }
        // A LINHA DE BASE e o unico traco continuo da estrutura, e ela fica em
        // BAIXO: o olho ocidental le de cima para baixo e para na ultima linha,
        // entao e ali que a peca ganha peso sem cercar nada.
        g.fill(area.x(), area.fimY() - 1, area.fimX() - corte, area.fimY(),
                PaletaDaHud.BORDA);
    }

    /**
     * Os dois colchetes, em cantos opostos.
     *
     * <p>SUPERIOR ESQUERDO E INFERIOR DIREITO: a mesma diagonal do chanfro. Os
     * quatro cantos marcados fechariam a forma de volta num retangulo, que e
     * exatamente o que o diagnostico pediu para tirar.
     */
    public static void suportes(GuiGraphics g, NenHudLayout.Retangulo area) {
        int cor = PaletaDaHud.BORDA;
        g.fill(area.x(), area.y(), area.x() + PERNA, area.y() + 1, cor);
        g.fill(area.x(), area.y(), area.x() + 1, area.y() + PERNA / 2, cor);

        g.fill(area.fimX() - PERNA, area.fimY() - 1, area.fimX(), area.fimY(), cor);
        g.fill(area.fimX() - 1, area.fimY() - PERNA / 2, area.fimX(), area.fimY(), cor);
    }

    /**
     * O trilho vertical e os nos de derivacao.
     *
     * <p><b>E A PECA QUE MAIS MUDA A LEITURA.</b> Sem ela, duas barras
     * empilhadas sao duas barras empilhadas. Com ela, elas sao duas derivacoes
     * de um mesmo sistema -- e a diferenca nao custa quase pixel nenhum.
     *
     * @param ys o topo de cada linha que deriva do trilho
     */
    public static void trilho(GuiGraphics g, int x, int y0, int y1, int[] ys, int alturaDaLinha) {
        // O trilho nao vai de ponta a ponta: ele para antes das duas bordas, e
        // essa folga e o que o faz parecer uma peca apoiada em vez de uma
        // divisoria desenhada.
        g.fill(x, y0 + 2, x + 1, y1 - 2, PaletaDaHud.BORDA);

        for (int y : ys) {
            int centro = y + alturaDaLinha / 2;
            // O conector: um toco horizontal do trilho ate a leitura.
            g.fill(x + 1, centro, x + 4, centro + 1, PaletaDaHud.BORDA);
            // O no: o ponto de derivacao, em acento.
            g.fill(x - NO + 1, centro - NO + 1, x + NO, centro + NO,
                    PaletaDaHud.ACENTO);
        }
    }

    /**
     * As marcas de calibracao ao longo de uma barra.
     *
     * <p>MICRODETALHE COM FUNCAO, e nao enfeite: elas dividem a leitura em
     * quartos, que e a precisao que se usa de relance ("estou abaixo da
     * metade"). Divisorias de decimo -- as que a versao anterior desenhava --
     * competiam com o preenchimento numa barra de sete pixels.
     *
     * <p>ELAS FICAM ACIMA DA BARRA, e nao dentro. Dentro, elas viram ruido
     * sobre o valor; acima, viram regua.
     */
    public static void calibracao(GuiGraphics g, NenHudLayout.Retangulo barra, int divisoes) {
        if (divisoes < 2 || barra.largura() < divisoes * 4) {
            return;
        }
        int y = barra.y() - 2;
        for (int i = 1; i < divisoes; i++) {
            int x = barra.x() + Math.round(barra.largura() * i / (float) divisoes);
            // A marca do MEIO e mais alta: e a referencia que o olho procura.
            int altura = (i * 2 == divisoes) ? 2 : 1;
            g.fill(x, y - altura + 1, x + 1, y + 1,
                    PaletaDaHud.comAlpha(PaletaDaHud.BORDA, 0.45F));
        }
    }
}
