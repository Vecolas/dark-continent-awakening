package com.darkcontinent.nenfoundation.worldtree;

import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeBranchNetwork;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Desenha a silhueta do plano de copa, para OLHAR antes de subir o jogo.
 *
 * <p><b>NAO E UM TESTE, E UM INSTRUMENTO.</b> Nao ha {@code @Test} aqui de
 * proposito: ele nao aprova nada. A aprovacao continua sendo captura em jogo
 * comparada com {@code docs/insp/arvoremundo.png}, por gente olhando.
 *
 * <p><b>O QUE ELE RESOLVEU.</b> Todos os portoes numericos da copa passavam --
 * cobertura, sobreposicao, ancoragem, camadas -- e a arvore era um PINHEIRO. Cada
 * prateleira estava correta sozinha; o que estava errado era a distribuicao
 * delas na altura, e isso nenhuma assercao pegava. Bastou olhar a projecao
 * lateral por dez segundos. Uma regua que mede peca por peca nao ve a silhueta.
 *
 * <p>Como rodar, do diretorio do projeto, depois de {@code gradlew build}:
 *
 * <pre>
 * java -cp build/classes/java/main;build/classes/java/test  *      com.darkcontinent.nenfoundation.worldtree.SilhuetaDaCopa 1000 docs/worldtree/silhueta
 * </pre>
 */
public final class SilhuetaDaCopa {

    private static final int LARGURA = 900;
    private static final int ALTURA = 900;

    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1000L;
        String destino = args.length > 1 ? args[1] : ".";

        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
        List<WorldTreeSpline> derivados = WorldTreeBranchNetwork.secondaryAndTertiary(layout);
        WorldTreeFoliagePlan plano = WorldTreeFoliagePlan.of(layout, derivados);

        System.out.println("seed " + seed
                + " | prateleiras " + plano.shelves().size()
                + " | vinhas " + plano.vines().size()
                + " | galhos " + layout.branches().size() + "+" + derivados.size());

        double alcance = 0.0;
        for (WorldTreeFoliageShelf s : plano.shelves()) {
            alcance = Math.max(alcance, Math.hypot(s.centerX(), s.centerZ()) + s.radius());
        }
        System.out.println("alcance horizontal da copa: " + (int) alcance + " blocos");

        lateral(plano, layout, derivados, new File(destino, "copa-lateral-" + seed + ".png"));
        superior(plano, new File(destino, "copa-topo-" + seed + ".png"));
    }

    /** Vista lateral: X para a direita, Y para cima. */
    private static void lateral(WorldTreeFoliagePlan plano, WorldTreeLayout layout,
            List<WorldTreeSpline> derivados, File saida) throws Exception {
        BufferedImage img = novaImagem();
        // escala: 1500 blocos de altura e +-380 de largura
        double escalaY = (ALTURA - 20.0) / 1560.0;
        double escalaX = (LARGURA - 20.0) / 780.0;

        // tronco
        for (int y = layout.trunk().baseY(); y <= layout.trunk().topY(); y += 1) {
            double r = layout.trunk().radiusAt(y);
            for (double x = -r; x <= r; x += 0.5) {
                ponto(img, x * escalaX + LARGURA / 2.0, ALTURA - 10 - y * escalaY, 0x6B4A2F);
            }
        }
        // galhos
        List<WorldTreeSpline> todos = new ArrayList<>(layout.branches());
        todos.addAll(derivados);
        for (WorldTreeSpline b : todos) {
            for (int i = 0; i <= 160; i++) {
                double t = i / 160.0;
                WorldTreePoint p = b.pointAt(t);
                double r = b.radiusAt(t);
                for (double d = -r; d <= r; d += 1.0) {
                    ponto(img, p.x() * escalaX + LARGURA / 2.0,
                            ALTURA - 10 - (p.y() + d) * escalaY, 0x8A6239);
                }
            }
        }
        // vinhas
        for (WorldTreeVineStrand v : plano.vines()) {
            for (int step = 0; step < v.length(); step++) {
                ponto(img, v.xAt(step) * escalaX + LARGURA / 2.0,
                        ALTURA - 10 - (v.originY() - step) * escalaY, 0x2F6B3A);
            }
        }
        // folhagem: seccao em Z=0, para nao virar mancha solida
        for (WorldTreeFoliageShelf s : plano.shelves()) {
            int passos = (int) Math.max(8, s.radius());
            for (int i = -passos; i <= passos; i++) {
                double x = s.centerX() + s.radius() * i / (double) passos;
                double dx = x - s.centerX();
                double dz = 0 - s.centerZ();
                double h2 = (dx * dx + dz * dz) / (s.radius() * s.radius());
                double f = s.verticalSpanFactor(h2);
                if (f <= 0.0) {
                    continue;
                }
                for (double y = s.centerY() - f * s.bottomThickness();
                        y <= s.centerY() + f * s.topThickness(); y += 0.7) {
                    ponto(img, x * escalaX + LARGURA / 2.0, ALTURA - 10 - y * escalaY, 0x3E8E41);
                }
            }
        }
        ImageIO.write(img, "png", saida);
        System.out.println("escrito: " + saida);
    }

    /** Vista de cima: X para a direita, Z para baixo. */
    private static void superior(WorldTreeFoliagePlan plano, File saida) throws Exception {
        BufferedImage img = novaImagem();
        double escala = (LARGURA - 20.0) / 760.0;
        // Desenha de baixo para cima, para a copa alta cobrir a baixa.
        List<WorldTreeFoliageShelf> ordenadas = new ArrayList<>(plano.shelves());
        ordenadas.sort((a, b) -> Double.compare(a.centerY(), b.centerY()));
        for (WorldTreeFoliageShelf s : ordenadas) {
            int tom = (int) Math.min(255, 60 + s.centerY() / 7.0);
            int cor = (tom / 3) << 16 | tom << 8 | (tom / 3);
            for (double a = 0; a < Math.PI * 2; a += 0.02) {
                for (double r = 0; r <= s.radius(); r += 1.0) {
                    ponto(img, (s.centerX() + Math.cos(a) * r) * escala + LARGURA / 2.0,
                            (s.centerZ() + Math.sin(a) * r) * escala + ALTURA / 2.0, cor);
                }
            }
        }
        ImageIO.write(img, "png", saida);
        System.out.println("escrito: " + saida);
    }

    private static BufferedImage novaImagem() {
        BufferedImage img = new BufferedImage(LARGURA, ALTURA, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < LARGURA; x++) {
            for (int y = 0; y < ALTURA; y++) {
                img.setRGB(x, y, 0x101418);
            }
        }
        return img;
    }

    private static void ponto(BufferedImage img, double px, double py, int cor) {
        int x = (int) Math.round(px);
        int y = (int) Math.round(py);
        if (x >= 0 && x < LARGURA && y >= 0 && y < ALTURA) {
            img.setRGB(x, y, cor);
        }
    }

    private SilhuetaDaCopa() {
    }
}
