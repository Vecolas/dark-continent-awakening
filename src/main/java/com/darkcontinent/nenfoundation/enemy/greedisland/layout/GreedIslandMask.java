package com.darkcontinent.nenfoundation.enemy.greedisland.layout;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Lobo;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Ponto;
import java.util.List;

/**
 * A forma da ilha, como campo de distancia com sinal. Secao 11 do documento.
 *
 * <pre>
 *   &gt; 0   terra       (o valor e quantos blocos para dentro)
 *   = 0   a costa
 *   &lt; 0   oceano      (quantos blocos para fora)
 * </pre>
 *
 * <p><b>ELA EXISTE PARA NAO SER UM CIRCULO.</b> O nao-negociavel numero 1 do
 * documento e "No circular island", e o andaime que ela substitui era
 * literalmente {@code distanciaDoCentro < raio}. A secao 10 lista o que a forma
 * tem de combinar, e as quatro camadas abaixo sao essa lista:
 *
 * <ol>
 *   <li><b>poligono das treze ancoras</b> -- a silhueta macro, assimetrica por
 *       construcao. Norte estreito, nordeste recortado, sudeste com peninsula
 *       grande, sudoeste com arquipelago;
 *   <li><b>lobos</b> -- peninsulas empurram a costa para fora, baias puxam para
 *       dentro. Sem eles o poligono seria uma elipse amassada;
 *   <li><b>ruido de baixa frequencia</b> -- desmancha a linha reta entre duas
 *       ancoras, que e o que denuncia um poligono a olho nu;
 *   <li><b>ruido de media frequencia</b> -- so perto da costa, para dar
 *       enseada pequena e ponta de rocha sem mexer no interior.
 * </ol>
 *
 * <p><b>SEM UMA LINHA DE MINECRAFT, de proposito.</b> O portao precisa varrer
 * dezenas de milhares de pontos para dizer se a ilha parece continental, e a
 * suite JUnit deste projeto roda sem o jogo carregado. Uma dependencia de
 * {@code DensityFunction} aqui dentro ja custou um {@code NoClassDefFoundError}
 * neste mesmo dominio.
 *
 * <p><b>E DETERMINISTICA E SEM ESTADO.</b> O mesmo ponto devolve o mesmo valor
 * em qualquer servidor, sem cache e sem seed de mundo -- e e isso que faz
 * Masadora continuar sendo Masadora naquele lugar (secao 5).
 */
public final class GreedIslandMask {

    /**
     * A semente do layout. FIXA NO MOD, e nao no mundo.
     *
     * <p>Se ela viesse da seed do mundo, cada servidor teria uma ilha
     * diferente -- e o documento inteiro existe para impedir isso.
     */
    private static final long SEMENTE_DO_LAYOUT = 0x6D_EE_D1_5L;

    /**
     * Amplitude do ruido macro, em blocos de deslocamento de costa.
     *
     * <p>O PORTAO CALIBROU ESTE NUMERO. A primeira tentativa usou 2.600, e com
     * tres oitavas (pesos 1 + 0,5 + 0,25) isso da ate 4.550 blocos de empurrao.
     * As ancoras do documento ja chegam a 39.000, e a caixa declarada tem
     * 80.000 de largura -- ou seja, folga de 1.000 por lado. A ilha vazava da
     * propria caixa, e o teste de escala pegou.
     */
    private static final double AMPLITUDE_MACRO = 1_400.0D;

    /** Comprimento de onda do ruido macro. */
    private static final double ONDA_MACRO = 14_000.0D;

    /** Amplitude do ruido de costa. Menor, e so onde importa. */
    private static final double AMPLITUDE_COSTEIRA = 900.0D;

    /** Comprimento de onda do ruido de costa. */
    private static final double ONDA_COSTEIRA = 3_200.0D;

    /**
     * Ate que distancia da costa o ruido fino ainda age.
     *
     * <p>SO PERTO DA COSTA: aplicado no interior, ele criaria lagos e buracos
     * no meio do continente, e a mascara deixaria de significar "terra".
     */
    private static final double ALCANCE_DO_RECORTE = 4_000.0D;

    private GreedIslandMask() {
    }

    /**
     * A distancia com sinal, em blocos.
     *
     * <p>O VALOR IMPORTA, e nao so o sinal: o campo de elevacao usa a distancia
     * para fazer a praia subir devagar, e o roteador de estradas usa para
     * manter caminho longe da agua. Uma mascara booleana obrigaria os dois a
     * recalcular a mesma coisa.
     */
    public static double distanciaComSinal(double x, double z) {
        double base = distanciaAoPoligono(x, z, GreedIslandConstants.ANCORAS_DE_COSTA);
        double comLobos = base + contribuicaoDosLobos(x, z);
        double comMacro = comLobos + ruido(x, z, ONDA_MACRO, AMPLITUDE_MACRO, 0L);

        // O RECORTE FINO SO PERTO DA COSTA. O peso cai a zero para dentro e
        // para fora, entao ele nao abre buraco no interior nem ilha solta no
        // meio do oceano.
        double peso = Math.max(0.0D, 1.0D - Math.abs(comMacro) / ALCANCE_DO_RECORTE);
        double fino = ruido(x, z, ONDA_COSTEIRA, AMPLITUDE_COSTEIRA, 977L) * peso;
        return comMacro + fino;
    }

    /** Se aquela coluna e terra. */
    public static boolean terra(double x, double z) {
        return distanciaComSinal(x, z) > 0.0D;
    }

    // ------------------------------------------------------------------
    // 1. O POLIGONO
    // ------------------------------------------------------------------

    /**
     * Distancia com sinal ate o anel de ancoras.
     *
     * <p>O SINAL VEM DO TESTE DE CRUZAMENTO (ray casting), e nao da orientacao
     * das arestas. Orientacao exige que o anel seja convexo ou que alguem
     * garanta o sentido; o cruzamento funciona com qualquer poligono simples, e
     * a costa de uma ilha e tudo menos convexa.
     */
    static double distanciaAoPoligono(double x, double z, List<Ponto> anel) {
        double menor = Double.MAX_VALUE;
        boolean dentro = false;

        for (int i = 0, j = anel.size() - 1; i < anel.size(); j = i++) {
            Ponto a = anel.get(i);
            Ponto b = anel.get(j);
            menor = Math.min(menor, distanciaAoSegmento(x, z, a.x(), a.z(), b.x(), b.z()));

            boolean cruza = (a.z() > z) != (b.z() > z)
                    && x < (double) (b.x() - a.x()) * (z - a.z())
                            / (double) (b.z() - a.z()) + a.x();
            if (cruza) {
                dentro = !dentro;
            }
        }
        return dentro ? menor : -menor;
    }

    private static double distanciaAoSegmento(double px, double pz,
            double ax, double az, double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        double comprimento = dx * dx + dz * dz;
        if (comprimento <= 0.0D) {
            return Math.hypot(px - ax, pz - az);
        }
        double t = Math.clamp(((px - ax) * dx + (pz - az) * dz) / comprimento, 0.0D, 1.0D);
        return Math.hypot(px - (ax + t * dx), pz - (az + t * dz));
    }

    // ------------------------------------------------------------------
    // 2. OS LOBOS
    // ------------------------------------------------------------------

    /**
     * Quanto peninsulas e baias movem a costa neste ponto.
     *
     * <p>A QUEDA E SUAVE (coseno elevado), e nao linear: uma queda linear deixa
     * um vinco visivel na borda do alcance, e o vinco corre por quilometros de
     * costa como uma cicatriz reta.
     */
    private static double contribuicaoDosLobos(double x, double z) {
        double total = 0.0D;
        total += somaDe(GreedIslandConstants.PENINSULAS, x, z);
        total += somaDe(GreedIslandConstants.BAIAS, x, z);
        total += somaDe(GreedIslandConstants.ENSEADAS, x, z);
        total += somaDe(GreedIslandConstants.ILHOTAS, x, z);
        return total;
    }

    private static double somaDe(List<Lobo> lobos, double x, double z) {
        double total = 0.0D;
        for (Lobo lobo : lobos) {
            // A DISTANCIA E MEDIDA NO ESPACO DO LOBO: gira para o eixo dele e
            // comprime o eixo longo. Uma peninsula circular e um inchaco; com
            // eixo, ela vira um dedo de terra -- e foi o mapa exportado que
            // mostrou a diferenca.
            double a = Math.toRadians(lobo.anguloGraus());
            double dx = x - lobo.x();
            double dz = z - lobo.z();
            double ao = dx * Math.cos(a) + dz * Math.sin(a);
            double at = -dx * Math.sin(a) + dz * Math.cos(a);
            double d = Math.hypot(ao / lobo.alongamento(), at);
            if (d >= lobo.alcance()) {
                continue;
            }
            double t = d / lobo.alcance();
            double queda = 0.5D * (1.0D + Math.cos(Math.PI * t));
            total += lobo.forca() * queda * queda;
        }
        return total;
    }

    // ------------------------------------------------------------------
    // 3. O RUIDO
    // ------------------------------------------------------------------

    /**
     * Ruido de valor com interpolacao suave, deterministico.
     *
     * <p>ESCRITO AQUI, e nao tirado do Minecraft, pela mesma razao que o resto
     * do arquivo: a mascara precisa rodar na suite JUnit. E ele nao precisa ser
     * bom -- precisa ser ESTAVEL. Duas oitavas dao o suficiente para desmanchar
     * a reta entre duas ancoras.
     */
    static double ruido(double x, double z, double onda, double amplitude, long sal) {
        double total = 0.0D;
        double peso = 1.0D;
        double escala = 1.0D;
        for (int oitava = 0; oitava < 3; oitava++) {
            total += peso * valorInterpolado(x / (onda / escala), z / (onda / escala),
                    sal + oitava * 7919L);
            escala *= 2.0D;
            peso *= 0.5D;
        }
        return total * amplitude;
    }

    private static double valorInterpolado(double x, double z, long sal) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = suavizar(x - x0);
        double fz = suavizar(z - z0);

        double v00 = aleatorio(x0, z0, sal);
        double v10 = aleatorio(x0 + 1, z0, sal);
        double v01 = aleatorio(x0, z0 + 1, sal);
        double v11 = aleatorio(x0 + 1, z0 + 1, sal);

        double a = v00 + (v10 - v00) * fx;
        double b = v01 + (v11 - v01) * fx;
        return a + (b - a) * fz;
    }

    /** Curva de Hermite: derivada zero nas pontas, entao a grade nao aparece. */
    private static double suavizar(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    /** Hash determinístico para -1..1. Sem {@code Random}: ele carrega estado. */
    private static double aleatorio(int x, int z, long sal) {
        long h = SEMENTE_DO_LAYOUT + sal
                + x * 0x9E3779B97F4A7C15L
                + z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) / (double) (1L << 52) * 2.0D - 1.0D;
    }
}
