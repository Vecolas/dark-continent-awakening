package com.darkcontinent.nenfoundation.worldtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A textura da folha luminosa, medida.
 *
 * <p><b>O QUE ESTE PORTAO PODE E O QUE ELE NAO PODE.</b> Ele nao julga se a
 * textura e bonita, e nao regera o PNG -- o gerador esta em
 * {@code art-source/worldtree/folha_luminosa.py} e este portao nao fala Python.
 * O que ele mede sao as PROPRIEDADES que o pedido descreve e que um arquivo
 * trocado a mao quebraria sem levantar erro nenhum: majoritariamente verde,
 * pontos dourados esparsos e agrupados, recorte de folha, e -- a que custou uma
 * rodada -- sem emenda ao ladrilhar.
 *
 * <p><b>A EMENDA E O CASO QUE ALIMENTOU ESTE PORTAO.</b> A primeira versao do
 * gerador concentrava buraco na BORDA do quadro, o que parece razoavel para uma
 * folha que se desfaz nas pontas. O bloco ladrilha: duas bordas vizinhas
 * encostam, e em tela apareceu uma GRADE preta de um pixel cortando a copa a
 * cada 16 blocos -- indistinguivel, para quem visse, de um defeito de geracao
 * por chunk. Olhar o quadro sozinho nao mostra; so ladrilhar mostra. Por isso
 * {@link #semEmendaAoLadrilhar} existe.
 */
class TexturaDaFolhaLuminosaTest {

    private static final Path ARQUIVO = Repo.raiz().resolve(
            "src/main/resources/assets/nenfoundation/textures/block/"
                    + "world_tree_leaves_luminous.png");

    private static BufferedImage imagem() throws IOException {
        assertTrue(Files.isRegularFile(ARQUIVO),
                "a textura nao existe em " + ARQUIVO + ". Sem ela o bloco renderiza"
                        + " como o quadriculado preto-e-rosa, e nenhum outro portao"
                        + " daqui reprova.");
        BufferedImage imagem = ImageIO.read(ARQUIVO.toFile());
        assertTrue(imagem != null, "o arquivo existe e nao e um PNG legivel");
        return imagem;
    }

    private static boolean opaco(BufferedImage img, int x, int y) {
        return (img.getRGB(x, y) >>> 24) > 0;
    }

    /** Dourado: vermelho na frente do verde, e o azul bem atras dos dois. */
    private static boolean dourado(BufferedImage img, int x, int y) {
        int argb = img.getRGB(x, y);
        if ((argb >>> 24) == 0) {
            return false;
        }
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return r > g && g > b && (r - b) > 60;
    }

    private static boolean verde(BufferedImage img, int x, int y) {
        int argb = img.getRGB(x, y);
        if ((argb >>> 24) == 0) {
            return false;
        }
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return g > r && g > b;
    }

    @Test
    @DisplayName("16x16, que e o que o atlas de bloco espera")
    void tamanhoDeBloco() throws IOException {
        BufferedImage img = imagem();
        assertEquals(16, img.getWidth());
        assertEquals(16, img.getHeight());
    }

    @Test
    @DisplayName("majoritariamente VERDE -- ela e folha antes de ser luminaria")
    void verdeDomina() throws IOException {
        BufferedImage img = imagem();
        int opacos = 0;
        int verdes = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (!opaco(img, x, y)) {
                    continue;
                }
                opacos++;
                if (verde(img, x, y)) {
                    verdes++;
                }
            }
        }
        double fracao = (double) verdes / opacos;
        assertTrue(fracao >= 0.80,
                "so " + String.format("%.0f%%", fracao * 100) + " dos pixels opacos"
                        + " sao verdes. Se o bloco luminoso deixar de ler como folha,"
                        + " a copa vira mosaico.");
    }

    @Test
    @DisplayName("os pontos dourados sao ESPARSOS -- eles pontuam, nao cobrem")
    void douradoEsparso() throws IOException {
        BufferedImage img = imagem();
        int opacos = 0;
        int dourados = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (!opaco(img, x, y)) {
                    continue;
                }
                opacos++;
                if (dourado(img, x, y)) {
                    dourados++;
                }
            }
        }
        double fracao = (double) dourados / opacos;
        assertTrue(fracao >= 0.04,
                "so " + String.format("%.1f%%", fracao * 100) + " de dourado: a"
                        + " referencia pedida e folha COBERTA de liquen, e abaixo disso"
                        + " o bloco nao se distingue das outras tres folhas.");
        assertTrue(fracao <= 0.20,
                String.format("%.1f%%", fracao * 100) + " de dourado. Acima disso ela"
                        + " deixa de ser folha com liquen e vira bloco dourado.");
    }

    @Test
    @DisplayName("o dourado vem em MANCHA, e nao em chuvisco")
    void douradoAgrupado() throws IOException {
        // MESMO ARGUMENTO DO CAMPO QUE ESPALHA AS FOLHAS LUMINOSAS PELA COPA, uma
        // escala abaixo: pixel dourado isolado some na primeira mipmap, e a 30
        // blocos a folha volta a ser verde lisa -- o jogador perde a pista de
        // onde vem a luz.
        BufferedImage img = imagem();
        boolean[][] visto = new boolean[img.getWidth()][img.getHeight()];
        int manchas = 0;
        int maiorMancha = 0;
        int total = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (!dourado(img, x, y) || visto[x][y]) {
                    continue;
                }
                manchas++;
                int tamanho = 0;
                Deque<int[]> fila = new ArrayDeque<>();
                fila.add(new int[] {x, y});
                visto[x][y] = true;
                while (!fila.isEmpty()) {
                    int[] p = fila.poll();
                    tamanho++;
                    for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int nx = p[0] + d[0];
                        int ny = p[1] + d[1];
                        if (nx < 0 || ny < 0 || nx >= img.getWidth() || ny >= img.getHeight()
                                || visto[nx][ny] || !dourado(img, nx, ny)) {
                            continue;
                        }
                        visto[nx][ny] = true;
                        fila.add(new int[] {nx, ny});
                    }
                }
                total += tamanho;
                maiorMancha = Math.max(maiorMancha, tamanho);
            }
        }
        assertTrue(manchas > 0, "nenhum pixel dourado na textura");
        double media = (double) total / manchas;
        assertTrue(media >= 2.0,
                "mancha dourada media de " + String.format("%.1f", media) + " pixels."
                        + " Abaixo de 2 isso e chuvisco, e some na mipmap.");
        assertTrue(maiorMancha <= 12,
                "a maior mancha tem " + maiorMancha + " pixels. Mancha grande demais"
                        + " deixa de pontuar a folha e vira remendo.");
        assertTrue(manchas >= 3 && manchas <= 14,
                manchas + " manchas. Poucas somem; muitas voltam a ser chuvisco.");
    }

    @Test
    @DisplayName("tem RECORTE de folha -- quadrado cheio le como musgo")
    void temRecorte() throws IOException {
        BufferedImage img = imagem();
        int vazios = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (!opaco(img, x, y)) {
                    vazios++;
                }
            }
        }
        double fracao = (double) vazios / (img.getWidth() * img.getHeight());
        assertTrue(fracao >= 0.05,
                String.format("%.0f%%", fracao * 100) + " de recorte. Sem buraco"
                        + " nenhum o bloco le como um cubo solido, e nao como folhagem.");
        assertTrue(fracao <= 0.35,
                String.format("%.0f%%", fracao * 100) + " de recorte. Acima disso a"
                        + " copa fica rala vista de perto e da para enxergar atraves"
                        + " dela.");
    }

    @Test
    @DisplayName("sem EMENDA ao ladrilhar -- o defeito que criou este portao")
    void semEmendaAoLadrilhar() throws IOException {
        // ALIMENTAR O PORTAO COM O DEFEITO: concentrar buraco na borda do quadro.
        // O bloco ladrilha, as bordas encostam, e o resultado e uma grade preta
        // cortando a copa a cada 16 blocos. Foi o que a primeira versao produziu.
        BufferedImage img = imagem();
        int largura = img.getWidth();
        int altura = img.getHeight();
        int vaziosEsq = 0;
        int vaziosDir = 0;
        int vaziosTopo = 0;
        int vaziosBase = 0;
        for (int y = 0; y < altura; y++) {
            if (!opaco(img, 0, y)) {
                vaziosEsq++;
            }
            if (!opaco(img, largura - 1, y)) {
                vaziosDir++;
            }
        }
        for (int x = 0; x < largura; x++) {
            if (!opaco(img, x, 0)) {
                vaziosTopo++;
            }
            if (!opaco(img, x, altura - 1)) {
                vaziosBase++;
            }
        }
        // A media do quadro fica em torno de 12%; o limite aqui e o DOBRO disso.
        // Uma borda muito mais vazia que o miolo vira linha continua no ladrilho.
        int limite = (int) Math.ceil(altura * 0.35);
        assertTrue(vaziosEsq <= limite && vaziosDir <= limite
                        && vaziosTopo <= limite && vaziosBase <= limite,
                "bordas vazias (esq/dir/topo/base): " + vaziosEsq + "/" + vaziosDir
                        + "/" + vaziosTopo + "/" + vaziosBase + " de " + altura
                        + ". Borda muito mais vazia que o miolo vira uma GRADE preta"
                        + " na copa ladrilhada -- e quem visse procuraria o defeito na"
                        + " geracao por chunk, que esta certa.");
    }
}
