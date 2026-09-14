package com.darkcontinent.nenfoundation.worldtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeBranchNetwork;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O indice espacial da copa.
 *
 * <p><b>O QUE ELE PRECISA PROVAR E UMA COISA SO: QUE ELE NAO MUDA NADA.</b> Ele
 * substituiu um laco sobre o plano inteiro com um {@code if} de caixa dentro. Se
 * a resposta dele diferir da daquele laco -- em um chunk, numa seed --, a copa
 * muda de forma, e ninguem vai atribuir uma copa diferente a um indice. Por isso
 * o teste principal aqui e uma COMPARACAO com o laco linear, e nao uma medida de
 * velocidade.
 *
 * <p>O ganho de custo tem regua propria, e ela e negativa: chunk longe da arvore
 * nao pode devolver candidato nenhum.
 */
class WorldTreeFoliageIndexTest {

    private static final int SEEDS = 20;

    private static WorldTreeFoliagePlan plan(long seed) {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
        return WorldTreeFoliagePlan.of(layout, WorldTreeBranchNetwork.secondaryAndTertiary(layout));
    }

    private static long seedAt(int index) {
        return 1_000L + index * 7_919L;
    }

    /** O corte que o gerador fazia antes do indice, palavra por palavra. */
    private static List<Integer> prateleirasPeloLacoLinear(WorldTreeFoliagePlan plano,
            int chunkX, int chunkZ) {
        int minX = chunkX * 16;
        int minZ = chunkZ * 16;
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        List<Integer> encontradas = new ArrayList<>();
        List<WorldTreeFoliageShelf> shelves = plano.shelves();
        for (int i = 0; i < shelves.size(); i++) {
            WorldTreeFoliageShelf shelf = shelves.get(i);
            double r = shelf.radius() + 1.0;
            if (minX <= shelf.centerX() + r && maxX >= shelf.centerX() - r
                    && minZ <= shelf.centerZ() + r && maxZ >= shelf.centerZ() - r) {
                encontradas.add(i);
            }
        }
        return encontradas;
    }

    private static List<Integer> vinhasPeloLacoLinear(WorldTreeFoliagePlan plano,
            int chunkX, int chunkZ) {
        List<Integer> encontradas = new ArrayList<>();
        List<WorldTreeVineStrand> vines = plano.vines();
        for (int i = 0; i < vines.size(); i++) {
            if (vines.get(i).touchesChunk(chunkX * 16, chunkZ * 16)) {
                encontradas.add(i);
            }
        }
        return encontradas;
    }

    private static List<Integer> lista(int[] indices) {
        List<Integer> saida = new ArrayList<>(indices.length);
        for (int valor : indices) {
            saida.add(valor);
        }
        return saida;
    }

    @Test
    @DisplayName("o indice devolve EXATAMENTE o que o laco linear devolvia")
    void indiceNaoMudaResultado() {
        // ESTE E O TESTE INTEIRO. Uma otimizacao que muda o resultado nao e uma
        // otimizacao: e uma mudanca de forma disfarcada de melhoria de custo, e
        // ela apareceria como copa cortada na fronteira de chunk -- o defeito
        // mais caro de diagnosticar desta trilha, porque a geracao por chunk
        // estaria certa.
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            WorldTreeFoliagePlan plano = plan(seed);
            WorldTreeFoliageIndex indice = plano.index();
            for (int chunkX = -20; chunkX <= 20; chunkX += 2) {
                for (int chunkZ = -20; chunkZ <= 20; chunkZ += 2) {
                    assertEquals(prateleirasPeloLacoLinear(plano, chunkX, chunkZ),
                            lista(indice.prateleirasEm(chunkX, chunkZ)),
                            "seed " + seed + ", chunk (" + chunkX + "," + chunkZ
                                    + "): o indice e o laco linear discordam sobre"
                                    + " quais prateleiras encostam aqui.");
                    assertEquals(vinhasPeloLacoLinear(plano, chunkX, chunkZ),
                            lista(indice.vinhasEm(chunkX, chunkZ)),
                            "seed " + seed + ", chunk (" + chunkX + "," + chunkZ
                                    + "): o indice e o laco linear discordam sobre"
                                    + " quais vinhas encostam aqui.");
                }
            }
        }
    }

    @Test
    @DisplayName("o indice pega os chunks NEGATIVOS -- metade da arvore mora neles")
    void quadranteNegativo() {
        // ALIMENTAR O PORTAO COM O DEFEITO: trocar a conversao de coordenada para
        // chunk por uma divisao inteira (`(int) min / 16`) acerta o quadrante
        // positivo e erra o negativo por um chunk, porque a divisao inteira
        // arredonda em direcao a zero. O efeito em tela seria copa faltando em
        // faixas de 16 blocos, so de um lado da arvore -- e o teste geral acima
        // varre de 2 em 2 chunks, entao podia nao cair no chunk errado. Este caso
        // varre o quadrante negativo inteiro, chunk a chunk.
        WorldTreeFoliagePlan plano = plan(seedAt(0));
        int comCopa = 0;
        for (int chunkX = -16; chunkX < 0; chunkX++) {
            for (int chunkZ = -16; chunkZ < 0; chunkZ++) {
                assertEquals(prateleirasPeloLacoLinear(plano, chunkX, chunkZ),
                        lista(plano.index().prateleirasEm(chunkX, chunkZ)),
                        "chunk negativo (" + chunkX + "," + chunkZ + ") diverge");
                if (plano.index().prateleirasEm(chunkX, chunkZ).length > 0) {
                    comCopa++;
                }
            }
        }
        assertTrue(comCopa > 40, "so " + comCopa + " chunks do quadrante negativo tem copa;"
                + " o caso nao esta exercitando o que devia");
    }

    @Test
    @DisplayName("chunk longe da arvore nao devolve candidato NENHUM")
    void chunkDistanteNaoTemCandidato() {
        // O CUSTO QUE O INDICE EXISTE PARA MATAR. Antes dele, este chunk pagava
        // 549 testes de prateleira e 3.070 de vinha antes de descobrir que nao
        // tinha nada a desenhar -- em todo chunk da dimensao, que e infinita.
        for (int index = 0; index < SEEDS; index++) {
            WorldTreeFoliagePlan plano = plan(seedAt(index));
            for (int[] longe : new int[][] {{300, 300}, {-400, 120}, {0, 260}, {900, -900}}) {
                assertEquals(0, plano.index().prateleirasEm(longe[0], longe[1]).length);
                assertEquals(0, plano.index().vinhasEm(longe[0], longe[1]).length);
            }
        }
    }

    @Test
    @DisplayName("o indice nao troca um custo por outro: a memoria dele tem teto")
    void memoriaDoIndice() {
        // UM INDICE QUE EXPLODE resolve o tempo e cria um problema de memoria, em
        // silencio. Cada entrada e um int; medido, sao ~20 mil por mundo (80 KB).
        // O teto e quatro vezes isso, e protege contra regressao de ordem de
        // grandeza -- alguem dobrar o raio das prateleiras, por exemplo, ou
        // alargar a faixa de chunks "para o lado seguro".
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            long entradas = plan(seed).index().entradas();
            assertTrue(entradas > 0, "seed " + seed + ": indice vazio");
            assertTrue(entradas <= 80_000L,
                    "seed " + seed + ": o indice guarda " + entradas + " entradas."
                            + " Isso e memoria paga em todo mundo carregado.");
        }
    }
}
