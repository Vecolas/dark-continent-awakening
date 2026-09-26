package com.darkcontinent.nenfoundation.enemy.greedisland.layout;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Cordilheira;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Ponto;
import java.util.List;

/**
 * As cordilheiras como GRAFO DE CRISTAS, e nao como ruido alto. Fase G2.
 *
 * <p>A secao 18 do documento e categorica: <i>"cada um e um graph de ridges,
 * nao um biome com noise alto"</i>. A diferenca nao e tecnica, e de leitura:
 *
 * <ul>
 *   <li><b>ruido alto</b> da montanhas espalhadas sem direcao. O jogador nunca
 *       diz "a cadeia separa Dorias do centro" porque nao ha cadeia -- ha
 *       manchas altas;
 *   <li><b>polilinha</b> da uma cadeia com eixo, que se atravessa por um passo
 *       e se contorna pela base. E o que torna a ilha memorizavel, e
 *       memorizavel e o objetivo declarado do documento inteiro.
 * </ul>
 *
 * <p>A altura de um ponto sai da DISTANCIA ate a linha de crista mais proxima:
 * em cima dela, o pico; a meia largura, o sope; alem, nada. Somada a isso vem
 * uma variacao ao longo do eixo, que impede a cadeia de ser uma muralha de
 * altura constante -- e e ela que produz os passos por onde as estradas do G5
 * vao passar.
 *
 * <p>SEM MINECRAFT, como todo o pacote: o gate macro precisa varrer a ilha.
 */
public final class GreedIslandRidgeField {

    /** Quantos pontos por segmento ao amostrar a crista. Mais, mais liso. */
    private static final int AMOSTRAS_POR_SEGMENTO = 12;

    /** As cristas ja amostradas. Mesma razao da hidrografia: elas nao mudam. */
    private static final java.util.Map<String, double[][]> CRISTAS = amostrarCristas();

    private static java.util.Map<String, double[][]> amostrarCristas() {
        var mapa = new java.util.HashMap<String, double[][]>();
        for (Cordilheira c : GreedIslandConstants.CORDILHEIRAS) {
            mapa.put(c.id(), amostrar(c.nos()));
        }
        return java.util.Map.copyOf(mapa);
    }

    private static double[][] amostrar(List<Ponto> nos) {
        int total = (nos.size() - 1) * AMOSTRAS_POR_SEGMENTO + 1;
        double[][] pontos = new double[total][2];
        int k = 0;
        for (int i = 0; i < nos.size() - 1; i++) {
            Ponto p0 = nos.get(Math.max(0, i - 1));
            Ponto p1 = nos.get(i);
            Ponto p2 = nos.get(i + 1);
            Ponto p3 = nos.get(Math.min(nos.size() - 1, i + 2));
            for (int a = 0; a < AMOSTRAS_POR_SEGMENTO; a++) {
                double t = a / (double) AMOSTRAS_POR_SEGMENTO;
                pontos[k][0] = catmull(p0.x(), p1.x(), p2.x(), p3.x(), t);
                pontos[k][1] = catmull(p0.z(), p1.z(), p2.z(), p3.z(), t);
                k++;
            }
        }
        Ponto ultimo = nos.get(nos.size() - 1);
        pontos[k][0] = ultimo.x();
        pontos[k][1] = ultimo.z();
        return pontos;
    }

    private static double menorDistancia(double x, double z, double[][] pontos) {
        double menor = Double.MAX_VALUE;
        for (double[] p : pontos) {
            double dx = x - p[0];
            double dz = z - p[1];
            double d2 = dx * dx + dz * dz;
            if (d2 < menor) {
                menor = d2;
            }
        }
        return Math.sqrt(menor);
    }

    private GreedIslandRidgeField() {
    }

    /**
     * Quanto as cordilheiras somam a altura neste ponto, em blocos.
     *
     * <p>DEVOLVE SOMA, e nao altura absoluta: o campo base ja deu a altura
     * continental, e substituir aquilo aqui faria a montanha ignorar se esta
     * na costa ou no interior -- e apareceria cadeia nascendo do mar.
     *
     * <p>ENTRE DUAS CADEIAS, VALE A MAIOR, e nao a soma. Somar faria o
     * cruzamento de duas cristas virar um pico do dobro da altura, e o
     * documento poe teto em 315.
     */
    public static double contribuicao(double x, double z) {
        double maior = 0.0D;
        for (Cordilheira c : GreedIslandConstants.CORDILHEIRAS) {
            maior = Math.max(maior, deUmaCordilheira(x, z, c));
        }
        return maior;
    }

    /** A cordilheira mais proxima e a distancia ate a crista dela. */
    public static double distanciaAteACrista(double x, double z) {
        double menor = Double.MAX_VALUE;
        for (Cordilheira c : GreedIslandConstants.CORDILHEIRAS) {
            menor = Math.min(menor, menorDistancia(x, z, CRISTAS.get(c.id())));
        }
        return menor;
    }

    private static double deUmaCordilheira(double x, double z, Cordilheira c) {
        double d = menorDistancia(x, z, CRISTAS.get(c.id()));
        double meiaLargura = c.larguraBase() / 2.0D;
        if (d >= meiaLargura) {
            return 0.0D;
        }
        // O PERFIL E UM COSENO ELEVADO, e nao uma rampa: rampa deixa o sope
        // com um vinco reto que corre por dezenas de milhares de blocos, e
        // esse vinco le como parede de mapa, nao como montanha.
        double t = d / meiaLargura;
        double perfil = 0.5D * (1.0D + Math.cos(Math.PI * t));
        perfil = perfil * perfil;

        // A ALTURA VARIA AO LONGO DO EIXO. Sem isto a cadeia e uma muralha de
        // altura constante -- e nao existiria passo nenhum para atravessar.
        double variacao = (GreedIslandMask.ruido(x, z, 5_000.0D, 1.0D, 4_231L) + 1.0D) / 2.0D;
        double alturaLocal = c.alturaMinima()
                + (c.alturaMaxima() - c.alturaMinima()) * variacao;

        return (alturaLocal - GreedIslandConstants.NIVEL_DO_MAR) * perfil;
    }

    /**
     * Distancia ate a polilinha, amostrando cada segmento.
     *
     * <p>AMOSTRAGEM, e nao distancia analitica ate a curva: os nos do documento
     * sao poucos e distantes, e ligar dois com uma reta produziria cadeias de
     * cotovelo. As amostras seguem uma Catmull-Rom, que passa pelos nos e
     * curva entre eles.
     */
    static double distanciaAPolilinha(double x, double z, List<Ponto> nos) {
        double menor = Double.MAX_VALUE;
        for (int i = 0; i < nos.size() - 1; i++) {
            Ponto p0 = nos.get(Math.max(0, i - 1));
            Ponto p1 = nos.get(i);
            Ponto p2 = nos.get(i + 1);
            Ponto p3 = nos.get(Math.min(nos.size() - 1, i + 2));
            for (int a = 0; a < AMOSTRAS_POR_SEGMENTO; a++) {
                double t = a / (double) AMOSTRAS_POR_SEGMENTO;
                double cx = catmull(p0.x(), p1.x(), p2.x(), p3.x(), t);
                double cz = catmull(p0.z(), p1.z(), p2.z(), p3.z(), t);
                menor = Math.min(menor, Math.hypot(x - cx, z - cz));
            }
        }
        Ponto ultimo = nos.get(nos.size() - 1);
        return Math.min(menor, Math.hypot(x - ultimo.x(), z - ultimo.z()));
    }

    /** Catmull-Rom: passa pelos pontos de controle e curva entre eles. */
    static double catmull(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5D * ((2.0D * p1)
                + (-p0 + p2) * t
                + (2.0D * p0 - 5.0D * p1 + 4.0D * p2 - p3) * t2
                + (-p0 + 3.0D * p1 - 3.0D * p2 + p3) * t3);
    }
}
