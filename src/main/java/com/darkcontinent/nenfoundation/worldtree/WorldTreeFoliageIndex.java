package com.darkcontinent.nenfoundation.worldtree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QUAIS prateleiras e vinhas encostam num chunk, sem percorrer o plano inteiro.
 *
 * <p><b>O DEFEITO QUE ELE EXISTE PARA MATAR.</b> O gerador tinha um corte por
 * chunk, e o corte estava certo -- mas ele era um {@code if} DENTRO de um laco
 * sobre a lista inteira. Com 549 prateleiras e 3.070 vinhas, todo chunk da
 * dimensao pagava 3.619 testes de caixa antes de descobrir que nao tinha nada a
 * desenhar. Um chunk a 5.000 blocos do tronco pagava exatamente o mesmo que o
 * chunk em cima do tronco.
 *
 * <p>Isso nao da erro, e nao aparece em lugar nenhum a nao ser como mundo
 * demorando para carregar -- o mesmo sintoma do cache do plano e do corte de
 * vinha, e pela mesma razao: custo proporcional ao PLANO onde deveria ser
 * proporcional ao CHUNK.
 *
 * <p><b>A PEGADA E CALCULADA UMA VEZ.</b> Cada prateleira e cada vinha declaram
 * os chunks que tocam, e o mapa e montado junto com o plano. Depois disso, um
 * chunk vazio custa uma consulta de mapa que devolve vazio.
 *
 * <p><b>ELE NAO DECIDE NADA.</b> A resposta e a MESMA lista que o laco linear
 * produzia -- o portao {@code WorldTreeFoliageIndexTest} compara as duas em
 * vinte seeds. Um indice que "otimiza" mudando o resultado e o pior tipo de
 * otimizacao: a copa mudaria de forma e ninguem atribuiria isso a um indice.
 */
public final class WorldTreeFoliageIndex {

    private static final int[] VAZIO = new int[0];

    private final Map<Long, int[]> prateleirasPorChunk;
    private final Map<Long, int[]> vinhasPorChunk;

    private WorldTreeFoliageIndex(Map<Long, int[]> prateleirasPorChunk,
            Map<Long, int[]> vinhasPorChunk) {
        this.prateleirasPorChunk = prateleirasPorChunk;
        this.vinhasPorChunk = vinhasPorChunk;
    }

    static WorldTreeFoliageIndex of(List<WorldTreeFoliageShelf> shelves,
            List<WorldTreeVineStrand> vines) {
        Map<Long, List<Integer>> prateleiras = new HashMap<>();
        for (int index = 0; index < shelves.size(); index++) {
            WorldTreeFoliageShelf shelf = shelves.get(index);
            // A MESMA MARGEM DE UM BLOCO do corte linear. Ela nao e frouxidao: o
            // laco de desenho varre ate `ceil(centro + raio) + 1`, e um indice
            // mais apertado que o escritor cortaria a copa na fronteira de chunk.
            double r = shelf.radius() + 1.0;
            registrar(prateleiras, index,
                    shelf.centerX() - r, shelf.centerX() + r,
                    shelf.centerZ() - r, shelf.centerZ() + r);
        }
        Map<Long, List<Integer>> vinhas = new HashMap<>();
        for (int index = 0; index < vines.size(); index++) {
            WorldTreeVineStrand vine = vines.get(index);
            // A CAIXA E A DE `touchesChunk`, e tem de continuar sendo: o indice
            // responde a mesma pergunta, so que sem varrer a lista.
            registrar(vinhas, index,
                    Math.min(vine.originX(), vine.originX() + vine.driftX()) - 1.0,
                    Math.max(vine.originX(), vine.originX() + vine.driftX()) + 1.0,
                    Math.min(vine.originZ(), vine.originZ() + vine.driftZ()) - 1.0,
                    Math.max(vine.originZ(), vine.originZ() + vine.driftZ()) + 1.0);
        }
        return new WorldTreeFoliageIndex(congelar(prateleiras), congelar(vinhas));
    }

    private static void registrar(Map<Long, List<Integer>> destino, int index,
            double minX, double maxX, double minZ, double maxZ) {
        for (int chunkX = primeiroChunk(minX); chunkX <= ultimoChunk(maxX); chunkX++) {
            for (int chunkZ = primeiroChunk(minZ); chunkZ <= ultimoChunk(maxZ); chunkZ++) {
                destino.computeIfAbsent(chave(chunkX, chunkZ), key -> new ArrayList<>())
                        .add(index);
            }
        }
    }

    /*
     * A FAIXA DE CHUNKS E A INVERSA EXATA DO TESTE DE CAIXA, e nao uma
     * aproximacao generosa.
     *
     * A primeira versao usava `floorDiv(floor(min))` e `floorDiv(ceil(max))`, que
     * PARECE seguro -- erra para mais, nunca para menos, entao nenhuma folha
     * some. O portao reprovou assim mesmo, e com razao: uma faixa mais larga faz
     * o gerador visitar prateleiras que nao escrevem nada, que e exatamente o
     * custo que o indice existe para matar, e faz o indice guardar entradas a
     * toa. "Erra so para o lado seguro" e como um indice engorda em silencio.
     *
     * O teste de caixa e `chunk*16 <= max && chunk*16 + 16 >= min`. Resolvido
     * para `chunk`, da o par abaixo -- e o portao compara os dois em vinte seeds.
     */
    private static int primeiroChunk(double min) {
        return (int) Math.ceil(min / 16.0) - 1;
    }

    private static int ultimoChunk(double max) {
        return (int) Math.floor(max / 16.0);
    }

    private static Map<Long, int[]> congelar(Map<Long, List<Integer>> origem) {
        Map<Long, int[]> resultado = new HashMap<>(origem.size() * 2);
        for (Map.Entry<Long, List<Integer>> entrada : origem.entrySet()) {
            List<Integer> lista = entrada.getValue();
            int[] indices = new int[lista.size()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = lista.get(i);
            }
            resultado.put(entrada.getKey(), indices);
        }
        return Map.copyOf(resultado);
    }

    private static long chave(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFF_FFFFL);
    }

    /** Os indices das prateleiras que podem escrever neste chunk, em ordem do plano. */
    public int[] prateleirasEm(int chunkX, int chunkZ) {
        return prateleirasPorChunk.getOrDefault(chave(chunkX, chunkZ), VAZIO);
    }

    /** Os indices das vinhas que podem escrever neste chunk, em ordem do plano. */
    public int[] vinhasEm(int chunkX, int chunkZ) {
        return vinhasPorChunk.getOrDefault(chave(chunkX, chunkZ), VAZIO);
    }

    /**
     * Quantas entradas o indice guarda ao todo.
     *
     * <p>E o custo de MEMORIA dele, e existe para ter regua: um indice que
     * explode troca um custo por outro em silencio.
     */
    public long entradas() {
        long total = 0;
        for (int[] indices : prateleirasPorChunk.values()) {
            total += indices.length;
        }
        for (int[] indices : vinhasPorChunk.values()) {
            total += indices.length;
        }
        return total;
    }
}
