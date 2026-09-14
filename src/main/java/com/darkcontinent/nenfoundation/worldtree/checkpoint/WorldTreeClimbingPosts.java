package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeClimbingPost;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * As sete cabanas de uma seed, calculadas UMA VEZ.
 *
 * <p><b>POR QUE ESTE ARQUIVO EXISTE, e o custo que ele mata.</b>
 * {@code WorldTreeClimbingPost.forCheckpoint} varre os 109 galhos do layout em 41
 * amostras cada para achar o apoio -- cerca de 4.500 avaliacoes de spline. Ele era
 * chamado uma vez por checkpoint, por chunk: <b>31 mil avaliacoes em todo chunk
 * da dimensao</b>, sempre com o mesmo resultado, porque nada disso depende do
 * chunk.
 *
 * <p>Era o mesmo defeito que o cache do plano de copa ja tinha matado uma vez,
 * cometido de novo uma camada ao lado -- e pelo mesmo motivo: a conta e pura, e
 * conta pura parece de graca. Nao aparece como erro; aparece como mundo
 * demorando para carregar.
 *
 * <p><b>UMA ENTRADA SO, num campo so</b>, pela mesma razao que o cache da copa:
 * existe uma World Tree por mundo, e tres campos separados nao sao atomicos em
 * conjunto -- {@code forget()} podia zerar a lista entre duas leituras na thread
 * de worldgen e devolver {@code null} dentro da geracao de chunk.
 */
public final class WorldTreeClimbingPosts {

    private record Cache(long seed, List<WorldTreeClimbingPost> postos) {
    }

    private static volatile Cache cache;

    private WorldTreeClimbingPosts() {
    }

    /** As sete cabanas, na ordem do enum. */
    public static List<WorldTreeClimbingPost> of(WorldTreeLayout layout) {
        Cache atual = cache;
        if (atual != null && atual.seed() == layout.seed()) {
            return atual.postos();
        }
        List<WorldTreeClimbingPost> postos = new ArrayList<>();
        for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
            postos.add(WorldTreeClimbingPost.forCheckpoint(layout, checkpoint.y()));
        }
        List<WorldTreeClimbingPost> congelada = List.copyOf(postos);
        cache = new Cache(layout.seed(), congelada);
        return congelada;
    }

    /**
     * Esquece as cabanas.
     *
     * <p>QUEM LIGA, DESLIGA -- o par do cache, chamado quando o nivel descarrega.
     * Sem ele a lista do mundo anterior fica retida pelo resto da sessao.
     */
    public static void forget() {
        cache = null;
    }

    /**
     * O menor y limpo de folha nesta coluna, considerando TODAS as cabanas.
     *
     * <p>E o ponto unico que a copa consulta. Se cada gerador fizesse a propria
     * varredura, um deles esqueceria uma cabana e ela voltaria a ser enterrada --
     * sem erro, e so numa seed.
     */
    public static int yDoPoco(List<WorldTreeClimbingPost> postos, int x, int z) {
        int menor = Integer.MAX_VALUE;
        for (int indice = 0; indice < postos.size(); indice++) {
            int candidato = postos.get(indice).yDoPoco(x, z);
            if (candidato < menor) {
                menor = candidato;
            }
        }
        return menor;
    }
}
