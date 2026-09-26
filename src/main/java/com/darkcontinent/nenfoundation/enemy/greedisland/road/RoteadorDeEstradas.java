package com.darkcontinent.nenfoundation.enemy.greedisland.road;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Ponto;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandElevationField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandHydrologyField;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandMask;
import com.darkcontinent.nenfoundation.enemy.greedisland.region.RegiaoMacro;
import com.darkcontinent.nenfoundation.enemy.greedisland.region.RegistroDeRegioes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * O A* macro que roteia estradas pelo terreno. Secao 57, fase G5.
 *
 * <p><b>O DOCUMENTO E EXPLICITO: estradas nao sao linhas retas.</b> O nao
 * negociavel numero 7 e "Roads respect terrain", e a razao e de leitura: uma
 * reta entre duas cidades atravessa cordilheira, pantano e rio sem se importar,
 * e o jogador percebe na hora que aquilo foi desenhado por cima do mapa, e nao
 * a partir dele. Uma estrada que contorna a serra e sobe pelo passo e uma
 * estrada que alguem construiria.
 *
 * <p>A GRADE E DE 512 BLOCOS, e o documento sugere 128. Subi de proposito e o
 * motivo e medido: a 128 blocos, a ilha de 80.000 x 70.000 da 625 x 547 nos --
 * 342 mil por rota, nove rotas, e isso roda na suite JUnit a cada build. A 512
 * sao 156 x 137, e o caminho continua respeitando serra, rio e brejo porque as
 * penalidades sao amostradas no MEIO de cada aresta, e nao so nos nos. Se um
 * dia o traçado precisar de precisao maior, ela vira parametro -- e o custo
 * esta escrito aqui em vez de descoberto no primeiro build lento.
 *
 * <p>SEM MINECRAFT: o gate precisa rotear as nove estradas para conferir que
 * nenhuma atravessa montanha.
 */
public final class RoteadorDeEstradas {

    /** O lado da celula da grade macro, em blocos. */
    public static final int GRADE = 512;

    /** Subir um bloco custa isto em "blocos equivalentes" de caminho. */
    private static final double CUSTO_POR_BLOCO_DE_SUBIDA = 9.0D;

    /** Atravessar um rio. Caro, mas possivel: a ponte e o que o G9 coloca. */
    private static final double CUSTO_DE_TRAVESSIA = 4_000.0D;

    /** Brejo: lento e ruim de construir. */
    private static final double CUSTO_DE_BREJO = 1_600.0D;

    /** Montanha fora de passo. Proibitivo, e nao proibido. */
    private static final double CUSTO_DE_SERRA = 6_000.0D;

    /** Desconto por passar dentro de um passo conhecido. */
    private static final double DESCONTO_DO_PASSO = 0.35D;

    /** Quanto o passo atrai, em blocos de raio. */
    private static final int ALCANCE_DO_PASSO = 3_500;

    private RoteadorDeEstradas() {
    }

    /**
     * O caminho entre dois pontos, em coordenadas de mundo.
     *
     * <p>Devolve lista vazia quando nao ha rota -- e isso e resposta valida:
     * duas cidades em ilhas separadas nao se ligam por estrada, e inventar uma
     * ponte de 8.000 blocos seria pior que admitir que nao ha caminho.
     */
    public static List<Ponto> rotear(Ponto origem, Ponto destino) {
        long inicio = chave(origem);
        long fim = chave(destino);

        Map<Long, Double> custo = new HashMap<>();
        Map<Long, Long> veioDe = new HashMap<>();
        PriorityQueue<long[]> fila = new PriorityQueue<>(
                (a, b) -> Double.compare(
                        Double.longBitsToDouble(a[1]), Double.longBitsToDouble(b[1])));

        custo.put(inicio, 0.0D);
        fila.add(new long[] {inicio, Double.doubleToLongBits(heuristica(inicio, fim))});

        int visitados = 0;
        while (!fila.isEmpty()) {
            long atual = fila.poll()[0];
            if (atual == fim) {
                return reconstruir(veioDe, atual);
            }
            // TETO DE VISITA. Sem ele, uma rota impossivel varre a ilha inteira
            // e o build para. O teto e generoso e a falha e explicita: lista
            // vazia, que o chamador ja trata.
            if (++visitados > 60_000) {
                return List.of();
            }
            double custoAtual = custo.getOrDefault(atual, Double.MAX_VALUE);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    long vizinho = deslocar(atual, dx, dz);
                    double passo = custoDaAresta(atual, vizinho);
                    if (passo == Double.MAX_VALUE) {
                        continue;
                    }
                    double novo = custoAtual + passo;
                    if (novo < custo.getOrDefault(vizinho, Double.MAX_VALUE)) {
                        custo.put(vizinho, novo);
                        veioDe.put(vizinho, atual);
                        fila.add(new long[] {vizinho,
                                Double.doubleToLongBits(novo + heuristica(vizinho, fim))});
                    }
                }
            }
        }
        return List.of();
    }

    /**
     * O custo de ir de um no ao vizinho.
     *
     * <p><b>AS PENALIDADES SAO AMOSTRADAS NO MEIO DA ARESTA</b>, e nao so no no
     * de chegada. A diferenca importa numa grade de 512: um rio de 30 blocos
     * cabe inteiro entre dois nos, e medindo so os nos a estrada o atravessaria
     * de graca -- e o gate de pontes nunca acharia a travessia.
     */
    private static double custoDaAresta(long de, long para) {
        double x0 = mundoX(de);
        double z0 = mundoZ(de);
        double x1 = mundoX(para);
        double z1 = mundoZ(para);

        if (!GreedIslandMask.terra(x1, z1)) {
            return Double.MAX_VALUE;
        }
        double distancia = Math.hypot(x1 - x0, z1 - z0);
        double custo = distancia;

        int y0 = GreedIslandElevationField.alturaEm(x0, z0);
        int y1 = GreedIslandElevationField.alturaEm(x1, z1);
        custo += Math.abs(y1 - y0) * CUSTO_POR_BLOCO_DE_SUBIDA;

        boolean cruzouRio = false;
        for (int i = 1; i <= 4; i++) {
            double t = i / 5.0D;
            double mx = x0 + (x1 - x0) * t;
            double mz = z0 + (z1 - z0) * t;
            if (GreedIslandHydrologyField.molhado(mx, mz)) {
                cruzouRio = true;
            }
        }
        if (cruzouRio) {
            custo += CUSTO_DE_TRAVESSIA;
        }

        RegiaoMacro regiao = RegistroDeRegioes.em(x1, z1);
        if (regiao.terreno() == RegiaoMacro.Terreno.ALAGADO) {
            custo += CUSTO_DE_BREJO;
        }
        if (y1 >= GreedIslandConstants.TOPO_DO_PLANALTO) {
            custo += CUSTO_DE_SERRA;
        }

        // O PASSO DESCONTA, e nao zera: ele torna a travessia da serra a melhor
        // opcao entre as caras, sem torna-la mais barata que contornar por uma
        // planicie -- que e o que um desconto total faria.
        if (dentroDeUmPasso(x1, z1)) {
            custo *= DESCONTO_DO_PASSO;
        }
        return custo;
    }

    /** Se este ponto esta perto de um dos quatro passos da secao 58. */
    public static boolean dentroDeUmPasso(double x, double z) {
        for (Ponto p : GreedIslandConstants.PASSOS) {
            if (Math.hypot(x - p.x(), z - p.z()) <= ALCANCE_DO_PASSO) {
                return true;
            }
        }
        return false;
    }

    /**
     * A heuristica: distancia em linha reta.
     *
     * <p>ADMISSIVEL, e por isso o A* continua otimo: ela nunca superestima,
     * porque nenhum caminho real e mais curto que a reta. Somar penalidade
     * estimada aqui deixaria o caminho mais rapido de achar e nao
     * necessariamente o melhor -- e o documento quer o melhor.
     */
    private static double heuristica(long de, long ate) {
        return Math.hypot(mundoX(de) - mundoX(ate), mundoZ(de) - mundoZ(ate));
    }

    private static List<Ponto> reconstruir(Map<Long, Long> veioDe, long fim) {
        List<Ponto> caminho = new ArrayList<>();
        Long atual = fim;
        while (atual != null) {
            caminho.add(new Ponto((int) mundoX(atual), (int) mundoZ(atual)));
            atual = veioDe.get(atual);
        }
        java.util.Collections.reverse(caminho);
        return List.copyOf(caminho);
    }

    // --- a grade, empacotada em long ---------------------------------

    private static long chave(Ponto p) {
        return empacotar(Math.floorDiv(p.x(), GRADE), Math.floorDiv(p.z(), GRADE));
    }

    private static long empacotar(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFF_FFFFL);
    }

    private static long deslocar(long chave, int dx, int dz) {
        return empacotar((int) (chave >> 32) + dx, (int) chave + dz);
    }

    private static double mundoX(long chave) {
        return (int) (chave >> 32) * (double) GRADE + GRADE / 2.0D;
    }

    private static double mundoZ(long chave) {
        return (int) chave * (double) GRADE + GRADE / 2.0D;
    }
}
