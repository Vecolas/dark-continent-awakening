package com.darkcontinent.nenfoundation.client.hud.component;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import com.darkcontinent.nenfoundation.client.hud.PaletaDaHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * O retrato como NUCLEO DE LEITURA -- o centro da assinatura da HUD.
 *
 * <p>A direcao pede que ele deixe de ser "um avatar" e vire um identificador
 * biometrico: aro em camadas, marcas de leitura, um no lateral, e segmentos que
 * <b>acendem com a aura</b>. E o unico elemento da HUD que reage
 * continuamente, e por isso ele carrega a identidade.
 *
 * <p><b>O QUE ELE NAO FAZ: um circulo.</b> Recortar a face quadrada da skin num
 * disco exige mascara -- textura ou stencil --, e o resultado em 26 pixels e uma
 * borda serrilhada que le como defeito, nao como estilo. A forma vem do ARO, e
 * nao do recorte: um octogono chanfrado em volta de um retrato quadrado da a
 * leitura "circular" que a referencia pede, e sobrevive a qualquer GUI Scale.
 *
 * <p><b>O ARO TEM TRES CAMADAS</b>, e cada uma responde a uma coisa diferente:
 *
 * <ol>
 *   <li><b>chanfro</b> -- estrutura fixa, define a silhueta;
 *   <li><b>orbita quebrada</b> -- quatro arcos com vaos, na cor da tecnica
 *       dominante. Os vaos sao o que a impede de virar so "mais uma borda";
 *   <li><b>segmentos</b> -- oito marcas radiais que acendem na proporcao da
 *       aura. E o medidor que faz o nucleo parecer instrumento.
 * </ol>
 */
public final class NucleoDeRetrato {

    /** Tamanho do chanfro em cada canto do aro. */
    private static final int CHANFRO = 4;

    /** Quantos segmentos o anel de leitura tem. */
    private static final int SEGMENTOS = 8;

    /**
     * Desenha o nucleo.
     *
     * @param fracaoDeAura de 0 a 1; quantos segmentos do aro acendem
     * @param corDoEstado  a cor da tecnica dominante, ou o acento quando nao ha
     * @param pulso        de 0 a 1; so Ren e Ken o movem
     */
    public void desenhar(GuiGraphics g, NenHudLayout.Retangulo area,
            AbstractClientPlayer jogador, float fracaoDeAura, int corDoEstado, float pulso) {
        int lado = Math.min(area.largura(), area.altura()) - 8;
        int x = area.x() + (area.largura() - lado) / 2;
        int y = area.y() + (area.altura() - lado) / 2;

        // O fundo entra ANTES da face: uma skin com face transparente existe, e
        // sem ele o mundo apareceria dentro do retrato.
        g.fill(x, y, x + lado, y + lado, PaletaDaHud.TRILHO);
        ResourceLocation skin = jogador.getSkin().texture();
        g.blit(skin, x, y, lado, lado, 8, 8, 8, 8, 64, 64);
        g.blit(skin, x, y, lado, lado, 40, 8, 8, 8, 64, 64);

        chanfro(g, area);
        orbitaQuebrada(g, area, corDoEstado, pulso);
        segmentos(g, area, fracaoDeAura, corDoEstado);
        noLateral(g, area, corDoEstado);
    }

    /**
     * A silhueta: um retangulo com os quatro cantos cortados.
     *
     * <p>AQUI OS QUATRO CANTOS SAO CORTADOS, ao contrario da plataforma. A
     * razao e oposta: la o objetivo era NAO fechar a forma; aqui o objetivo e
     * fechar, porque o nucleo precisa ler como uma peca unica e solida contra o
     * mundo atras.
     */
    private static void chanfro(GuiGraphics g, NenHudLayout.Retangulo a) {
        int cor = PaletaDaHud.BORDA;
        int x0 = a.x();
        int y0 = a.y();
        int x1 = a.fimX();
        int y1 = a.fimY();

        g.fill(x0 + CHANFRO, y0, x1 - CHANFRO, y0 + 1, cor);
        g.fill(x0 + CHANFRO, y1 - 1, x1 - CHANFRO, y1, cor);
        g.fill(x0, y0 + CHANFRO, x0 + 1, y1 - CHANFRO, cor);
        g.fill(x1 - 1, y0 + CHANFRO, x1, y1 - CHANFRO, cor);

        for (int i = 0; i < CHANFRO; i++) {
            int d = CHANFRO - 1 - i;
            g.fill(x0 + d, y0 + i, x0 + d + 1, y0 + i + 1, cor);
            g.fill(x1 - d - 1, y0 + i, x1 - d, y0 + i + 1, cor);
            g.fill(x0 + d, y1 - i - 1, x0 + d + 1, y1 - i, cor);
            g.fill(x1 - d - 1, y1 - i - 1, x1 - d, y1 - i, cor);
        }
    }

    /**
     * Quatro arcos curtos com vaos, na cor da tecnica.
     *
     * <p>OS VAOS SAO O DESENHO. Um anel continuo seria uma segunda borda em
     * volta da primeira -- exatamente o "peso visual concentrado no contorno"
     * que o diagnostico mandou tirar. Quebrado, ele le como leitura de
     * instrumento.
     */
    private static void orbitaQuebrada(GuiGraphics g, NenHudLayout.Retangulo a,
            int cor, float pulso) {
        int tinta = PaletaDaHud.comAlpha(
                PaletaDaHud.clarear(cor, 0.2F * Math.clamp(pulso, 0.0F, 1.0F)), 0.85F);
        int arco = 5;
        int meioX = a.x() + a.largura() / 2;
        int meioY = a.y() + a.altura() / 2;

        g.fill(meioX - arco / 2, a.y() - 2, meioX + arco / 2 + 1, a.y() - 1, tinta);
        g.fill(meioX - arco / 2, a.fimY() + 1, meioX + arco / 2 + 1, a.fimY() + 2, tinta);
        g.fill(a.x() - 2, meioY - arco / 2, a.x() - 1, meioY + arco / 2 + 1, tinta);
        g.fill(a.fimX() + 1, meioY - arco / 2, a.fimX() + 2, meioY + arco / 2 + 1, tinta);
    }

    /**
     * Oito marcas radiais que acendem na proporcao da aura.
     *
     * <p>E O MEDIDOR DO NUCLEO, e a razao de ele ser um instrumento e nao um
     * avatar. A aura ja tem barra -- mas a barra e numero, e isto e presenca:
     * de relance, o aro cheio ou vazio conta o estado sem ler nada.
     *
     * <p>AS APAGADAS CONTINUAM DESENHADAS, fracas. Um medidor cujas marcas
     * somem nao mede: ele so mostra uma quantidade sem escala.
     */
    private static void segmentos(GuiGraphics g, NenHudLayout.Retangulo a,
            float fracao, int cor) {
        int acesos = Math.round(SEGMENTOS * Math.clamp(fracao, 0.0F, 1.0F));
        int apagada = PaletaDaHud.comAlpha(PaletaDaHud.BORDA, 0.25F);

        for (int i = 0; i < SEGMENTOS; i++) {
            boolean aceso = i < acesos;
            int tinta = aceso ? PaletaDaHud.comAlpha(cor, 0.9F) : apagada;
            // Os oito pontos correm o perimetro, dois por lado, comecando no
            // topo. Perimetro, e nao circunferencia: a forma-base e chanfrada,
            // e um raio trigonometrico cairia fora dela nos cantos.
            int[] p = pontoDoPerimetro(a, i);
            g.fill(p[0], p[1], p[0] + 1, p[1] + 1, tinta);
        }
    }

    /**
     * O i-esimo dos oito pontos, andando pelo perimetro do aro.
     *
     * <p>SEPARADO E DETERMINISTICO para poder ser conferido sem abrir o jogo: o
     * portao confere que os oito caem em posicoes distintas e dentro da moldura.
     */
    static int[] pontoDoPerimetro(NenHudLayout.Retangulo a, int indice) {
        int i = Math.floorMod(indice, SEGMENTOS);
        int x0 = a.x() - 1;
        int y0 = a.y() - 1;
        int x1 = a.fimX();
        int y1 = a.fimY();
        int tercoX = a.largura() / 3;
        int tercoY = a.altura() / 3;

        return switch (i) {
            case 0 -> new int[] {x0 + tercoX, y0};
            case 1 -> new int[] {x1 - tercoX, y0};
            case 2 -> new int[] {x1, y0 + tercoY};
            case 3 -> new int[] {x1, y1 - tercoY};
            case 4 -> new int[] {x1 - tercoX, y1};
            case 5 -> new int[] {x0 + tercoX, y1};
            case 6 -> new int[] {x0, y1 - tercoY};
            default -> new int[] {x0, y0 + tercoY};
        };
    }

    /**
     * O no lateral: um marcador solto, fora do aro.
     *
     * <p>ELE NAO INFORMA NADA, e e o unico elemento da HUD do qual isso e
     * verdade. Esta aqui porque a direcao pede "pequenos nodes circulares" como
     * sotaque de instrumento -- e porque uma peca assimetrica quebra a leitura
     * de "quadrado com coisas dentro". Se um dia ele precisar significar algo
     * (nivel Hunter, classe), o lugar ja existe.
     */
    private static void noLateral(GuiGraphics g, NenHudLayout.Retangulo a, int cor) {
        int y = a.fimY() - 3;
        int x = a.fimX() + 3;
        g.fill(x, y, x + 2, y + 2, PaletaDaHud.comAlpha(cor, 0.7F));
    }

    /** Quantos segmentos o aro tem. Para o portao. */
    public static int segmentos() {
        return SEGMENTOS;
    }
}
