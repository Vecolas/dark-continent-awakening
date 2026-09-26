package com.darkcontinent.nenfoundation.enemy.greedisland.road;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Ponto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * As nove estradas ja roteadas, indexadas para consulta por chunk.
 *
 * <p><b>O A* NAO PODE RODAR POR CHUNK.</b> Cada rota custa dezenas de milhares
 * de nos; um jogador explorando gera centenas de chunks por minuto, e recalcular
 * a rota em cada um travaria o servidor. O sintoma nao seria erro -- seria o
 * mundo parando de carregar enquanto alguem anda.
 *
 * <p>Entao as nove rotas sao calculadas UMA VEZ, na primeira consulta, e
 * guardadas. A partir dali, perguntar "passa estrada por este bloco?" e olhar
 * um balde da grade.
 *
 * <p><b>O CALCULO E PREGUICOSO, e nao no carregamento do mod.</b> Um servidor
 * que nunca abre Greed Island nao deve pagar segundos de A* no boot -- e a
 * maioria nao abre.
 *
 * <p><b>O TRACADO E INTERPOLADO entre os nos do A*.</b> A grade macro tem 512
 * blocos; sem interpolar, a estrada seria uma fila de pontos a meio quilometro
 * um do outro, e no chao ficariam ilhas de cascalho separadas por mato.
 */
public final class TracadoDeEstradas {

    /** O lado do balde do indice. Deve ser multiplo de 16 para casar com chunk. */
    private static final int BALDE = 256;

    /** Distancia entre pontos interpolados, em blocos. */
    private static final int PASSO_DO_TRACADO = 8;

    /** Meia largura da pista principal. */
    public static final int MEIA_LARGURA = 3;

    private static volatile Map<Long, List<Ponto>> indice;

    private TracadoDeEstradas() {
    }

    /**
     * O indice, calculado na primeira chamada.
     *
     * <p>Sincronizado porque dois threads de geracao de chunk podem chegar
     * juntos: sem a guarda, os dois pagariam o A* inteiro, e um jogaria o
     * resultado fora.
     */
    private static Map<Long, List<Ponto>> indice() {
        Map<Long, List<Ponto>> pronto = indice;
        if (pronto != null) {
            return pronto;
        }
        synchronized (TracadoDeEstradas.class) {
            if (indice == null) {
                indice = construir();
            }
            return indice;
        }
    }

    private static Map<Long, List<Ponto>> construir() {
        Map<Long, List<Ponto>> mapa = new HashMap<>();
        for (var ligacao : GreedIslandConstants.ESTRADAS) {
            var a = GreedIslandConstants.cidade(ligacao.de()).orElseThrow();
            var b = GreedIslandConstants.cidade(ligacao.para()).orElseThrow();
            List<Ponto> rota = RoteadorDeEstradas.rotear(
                    new Ponto(a.x(), a.z()), new Ponto(b.x(), b.z()));
            for (Ponto p : interpolar(rota)) {
                mapa.computeIfAbsent(chave(p.x(), p.z()), k -> new ArrayList<>()).add(p);
            }
        }
        return Map.copyOf(mapa);
    }

    /** Preenche os vaos entre nos do A* com pontos a cada poucos blocos. */
    private static List<Ponto> interpolar(List<Ponto> rota) {
        List<Ponto> saida = new ArrayList<>();
        for (int i = 0; i < rota.size() - 1; i++) {
            Ponto a = rota.get(i);
            Ponto b = rota.get(i + 1);
            double d = Math.hypot(b.x() - a.x(), b.z() - a.z());
            int passos = Math.max(1, (int) (d / PASSO_DO_TRACADO));
            for (int s = 0; s < passos; s++) {
                double t = s / (double) passos;
                saida.add(new Ponto((int) Math.round(a.x() + (b.x() - a.x()) * t),
                        (int) Math.round(a.z() + (b.z() - a.z()) * t)));
            }
        }
        if (!rota.isEmpty()) {
            saida.add(rota.get(rota.size() - 1));
        }
        return saida;
    }

    /**
     * A que distancia passa a estrada mais proxima, em blocos.
     *
     * <p>Devolve {@link Double#MAX_VALUE} quando nao ha estrada por perto -- e
     * esse e o caso na maior parte da ilha, por decisao da secao 111.
     *
     * <p>Olha os NOVE baldes em volta, e nao so o de baixo: uma estrada que
     * passa dois blocos alem da borda do balde ficaria invisivel para quem
     * esta do lado de ca.
     */
    public static double distanciaAte(int x, int z) {
        // QUADRADO NO LACO, raiz uma vez no fim: a raiz e a conta mais cara e
        // nao muda qual ponto e o mais proximo. Este metodo roda por BLOCO
        // durante a geracao -- 256 vezes por chunk --, e foi por descuidos
        // assim que o watchdog derrubou o servidor uma vez.
        long menor = Long.MAX_VALUE;
        Map<Long, List<Ponto>> mapa = indice();
        int bx = Math.floorDiv(x, BALDE);
        int bz = Math.floorDiv(z, BALDE);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                List<Ponto> pontos = mapa.get(empacotar(bx + dx, bz + dz));
                if (pontos == null) {
                    continue;
                }
                for (Ponto p : pontos) {
                    long ddx = x - p.x();
                    long ddz = z - p.z();
                    long d2 = ddx * ddx + ddz * ddz;
                    if (d2 < menor) {
                        menor = d2;
                    }
                }
            }
        }
        return menor == Long.MAX_VALUE ? Double.MAX_VALUE : Math.sqrt(menor);
    }

    /**
     * Se ha alguma estrada perto o bastante deste chunk para valer olhar.
     *
     * <p>REJEICAO RAPIDA, e ela vale muito: a maior parte da ilha nao tem
     * estrada nenhuma -- 60-70% e natureza aberta, por decisao da secao 111 --
     * e sem esta pergunta cada um desses chunks pagaria 256 varreduras de
     * balde para descobrir que nao ha nada.
     */
    public static boolean algumaEstradaPertoDoChunk(int chunkX, int chunkZ) {
        Map<Long, List<Ponto>> mapa = indice();
        int bx = Math.floorDiv(chunkX * 16, BALDE);
        int bz = Math.floorDiv(chunkZ * 16, BALDE);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (mapa.containsKey(empacotar(bx + dx, bz + dz))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Se este bloco esta na pista. */
    public static boolean naEstrada(int x, int z) {
        return distanciaAte(x, z) <= MEIA_LARGURA;
    }

    /** Se este bloco esta no acostamento -- a faixa batida em volta da pista. */
    public static boolean noAcostamento(int x, int z) {
        double d = distanciaAte(x, z);
        return d > MEIA_LARGURA && d <= MEIA_LARGURA + 2;
    }

    /** Quantos pontos o tracado tem. Para o portao e para diagnostico. */
    public static int totalDePontos() {
        return indice().values().stream().mapToInt(List::size).sum();
    }

    private static long chave(int x, int z) {
        return empacotar(Math.floorDiv(x, BALDE), Math.floorDiv(z, BALDE));
    }

    private static long empacotar(int bx, int bz) {
        return ((long) bx << 32) | (bz & 0xFFFF_FFFFL);
    }
}
