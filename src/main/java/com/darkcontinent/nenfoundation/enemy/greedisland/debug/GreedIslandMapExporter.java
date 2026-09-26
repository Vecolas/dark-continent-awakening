package com.darkcontinent.nenfoundation.enemy.greedisland.debug;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandElevationField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandMask;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * O mapa macro num PNG, SEM GERAR UM CHUNK. Secao 92 do documento.
 *
 * <p><b>ELE E O QUE TORNA O GATE MACRO POSSIVEL.</b> A pergunta "isso parece
 * uma ilha enorme?" nao se responde andando: a 80.000 blocos de extensao,
 * atravessar a ilha a pe levaria horas, e gerar os chunks para ver de cima
 * levaria dias de CPU. O documento separa os dois gates justamente porque e
 * possivel ter Masadora linda dentro de uma Greed Island que, vista de cima,
 * e um parque tematico.
 *
 * <p>Ele le a mascara e o campo de elevacao -- duas funcoes puras -- e pinta
 * um pixel por amostra. Um PNG de 800 pixels de largura cobre a ilha inteira
 * com uma amostra a cada cem blocos, e sai em segundos.
 *
 * <p><b>ELE NAO E UM RENDERIZADOR DE JOGO.</b> {@code java.awt} so existe aqui
 * porque isto roda em ferramenta e em teste, nunca no cliente nem no servidor
 * de jogo. Nenhuma classe desta arvore pode ser chamada de dentro do tick.
 */
public final class GreedIslandMapExporter {

    /** Cor do mar profundo. */
    private static final int MAR_FUNDO = 0x0B_1E_33;

    /** Cor do mar raso, na plataforma. */
    private static final int MAR_RASO = 0x1D_49_6B;

    /** A linha de costa, destacada: e o que o gate olha primeiro. */
    private static final int COSTA = 0xE8_DA_A8;

    /**
     * Quanto do oceano entra no quadro, alem da caixa da ilha.
     *
     * <p><b>O PRIMEIRO MAPA EXPORTADO FOI ILEGIVEL POR CAUSA DISTO.</b> O
     * quadro era exatamente a caixa de 80.000 x 70.000 -- que E a extensao da
     * ilha --, entao a terra encostava nas quatro bordas e a imagem lia como
     * "o mundo inteiro e terra". A silhueta, que e o que o gate macro julga,
     * simplesmente nao aparecia.
     *
     * <p>Trinta por cento de folga mostra a ilha INTEIRA cercada de agua, que e
     * a unica forma de responder "isso parece uma ilha?".
     */
    private static final double FOLGA_DE_OCEANO = 1.30D;

    private GreedIslandMapExporter() {
    }

    /** A largura do quadro em blocos, ja com a folga de oceano. */
    public static double quadroX() {
        return GreedIslandConstants.EXTENSAO_LESTE_OESTE * FOLGA_DE_OCEANO;
    }

    /** Idem, na vertical. */
    public static double quadroZ() {
        return GreedIslandConstants.EXTENSAO_NORTE_SUL * FOLGA_DE_OCEANO;
    }

    /**
     * Desenha a ilha inteira.
     *
     * @param largura em pixels; a altura sai da proporcao da caixa da ilha
     */
    public static BufferedImage desenhar(int largura) {
        if (largura < 64) {
            throw new IllegalArgumentException("largura inutil para julgar forma: " + largura);
        }
        int altura = Math.max(64, (int) Math.round(largura * quadroZ() / quadroX()));
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        escalaDoPixel = quadroX() / largura;

        for (int px = 0; px < largura; px++) {
            for (int pz = 0; pz < altura; pz++) {
                double x = mundoX(px, largura);
                double z = mundoZ(pz, altura);
                imagem.setRGB(px, pz, corDe(x, z));
            }
        }
        marcarCidades(imagem, largura, altura);
        return imagem;
    }

    /** Idem, gravando em disco. */
    public static void exportar(Path destino, int largura) throws IOException {
        java.nio.file.Files.createDirectories(destino.toAbsolutePath().getParent());
        ImageIO.write(desenhar(largura), "png", destino.toFile());
    }

    /** A coordenada de mundo de uma coluna de pixel. Publica: o portao usa. */
    public static double mundoX(int px, int largura) {
        double t = (px + 0.5D) / largura;
        return -quadroX() / 2.0D + t * quadroX();
    }

    /** Idem, para a linha. */
    public static double mundoZ(int pz, int altura) {
        double t = (pz + 0.5D) / altura;
        return -quadroZ() / 2.0D + t * quadroZ();
    }

    /**
     * A cor de um ponto: mar por profundidade, terra por altura.
     *
     * <p>A FAIXA DE COSTA GANHA COR PROPRIA. Sem ela, praia e mar raso ficam
     * quase do mesmo tom e a silhueta -- que e o que o gate julga -- some.
     */
    /**
     * Quantos blocos cabem num pixel do mapa atual.
     *
     * <p>CAMPO, e nao parametro em cada chamada: ele e constante durante um
     * desenho inteiro e passa-lo por sete niveis de chamada so para chegar na
     * cor de um rio seria ruido. Ele e escrito uma vez, no inicio de
     * {@code desenhar}.
     */
    private static double escalaDoPixel = 100.0D;

    private static int corDe(double x, double z) {
        double d = GreedIslandMask.distanciaComSinal(x, z);
        if (Math.abs(d) < 250.0D) {
            return COSTA;
        }
        if (d > 0.0D) {
            // RIO E LAGO ANTES DA ALTURA: eles sao o que o gate procura depois
            // da silhueta, e pintados por altura sumiriam dentro do verde.
            var agua = com.darkcontinent.nenfoundation.enemy.greedisland.layout
                    .GreedIslandHydrologyField.aguaEm(x, z);
            if (agua == com.darkcontinent.nenfoundation.enemy.greedisland.layout
                    .GreedIslandHydrologyField.Agua.LAGO) {
                return 0x2E_6C_A8;
            }
            if (agua == com.darkcontinent.nenfoundation.enemy.greedisland.layout
                    .GreedIslandHydrologyField.Agua.RIO) {
                return 0x3C_84_C4;
            }
            // ESPESSURA MINIMA DE DESENHO. Ver o javadoc de `distanciaAoRio`:
            // em escala, os sete rios sao invisiveis, e um mapa de debug que
            // nao mostra a hidrografia nao serve ao gate que a secao 92 pede.
            if (com.darkcontinent.nenfoundation.enemy.greedisland.layout
                    .GreedIslandHydrologyField.distanciaAoRio(x, z) < escalaDoPixel) {
                return 0x3C_84_C4;
            }
        }
        int altura = GreedIslandElevationField.alturaEm(x, z);
        if (d < 0.0D) {
            double t = Math.clamp(-d / 6_000.0D, 0.0D, 1.0D);
            return misturar(MAR_RASO, MAR_FUNDO, t);
        }
        // Verde baixo -> ocre -> cinza, seguindo a escada da secao 17.
        double t = Math.clamp((altura - GreedIslandConstants.NIVEL_DO_MAR)
                / (double) (GreedIslandConstants.PICO_MAXIMO
                        - GreedIslandConstants.NIVEL_DO_MAR), 0.0D, 1.0D);
        if (t < 0.5D) {
            return misturar(0x2E_5E_34, 0x7A_8A_3E, t * 2.0D);
        }
        return misturar(0x7A_8A_3E, 0xB8_B8_B2, (t - 0.5D) * 2.0D);
    }

    /**
     * Um quadrado por cidade.
     *
     * <p>ELAS SAO O SEGUNDO CRITERIO DO GATE: o documento reprova o mapa se as
     * cidades estiverem agrupadas demais, ou se mais de uma couber no mesmo
     * render distance. Sem marca-las, isso se julgaria de cabeca.
     */
    private static void marcarCidades(BufferedImage imagem, int largura, int altura) {
        for (var cidade : GreedIslandConstants.CIDADES) {
            int px = (int) Math.round((cidade.x() + quadroX() / 2.0D) / quadroX() * largura);
            int pz = (int) Math.round((cidade.z() + quadroZ() / 2.0D) / quadroZ() * altura);
            int cor = GreedIslandConstants.CIDADE_INICIAL.equals(cidade.id())
                    ? 0xFF_D9_5A : 0xE8_44_44;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int ax = px + dx;
                    int az = pz + dz;
                    if (ax >= 0 && ax < largura && az >= 0 && az < altura) {
                        imagem.setRGB(ax, az, cor);
                    }
                }
            }
        }
    }

    private static int misturar(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
