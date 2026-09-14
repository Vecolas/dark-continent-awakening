package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeClimbingPost;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeFoliagePlan;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeBranchNetwork;
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
        // A COPA ENTRA NA CONTA porque a altura da torre depende dela: ela sobe
        // ate emergir da folhagem LOCAL, e esse mesmo numero e o teto do poco.
        // Dois numeros separados divergiriam, e ai ou a torre nasce enterrada ou
        // o buraco sobra acima dela.
        WorldTreeFoliagePlan copa = WorldTreeFoliagePlan.of(layout,
                WorldTreeBranchNetwork.secondaryAndTertiary(layout));
        List<WorldTreeClimbingPost> postos = new ArrayList<>();
        for (WorldTreeCheckpoint checkpoint : WorldTreeCheckpoint.values()) {
            postos.add(WorldTreeClimbingPost.forCheckpoint(layout, checkpoint.y(), copa));
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
     * As FAIXAS de poco que cortam esta coluna.
     *
     * <p><b>FAIXAS, E NAO UM MINIMO.</b> A versao anterior devolvia "o menor y
     * limpo da coluna", e isso juntava pocos de cabanas diferentes: BASE em y=49
     * e CROWN em y=1249 na mesma coluna limpariam de 46 a 1252 -- exatamente o
     * tubo atravessando a copa inteira que este conserto veio matar. Cada cabana
     * responde pela sua faixa, e so por ela.
     *
     * <p>O resultado vai num buffer reaproveitado porque isto e consultado uma
     * vez por coluna de prateleira -- alocar ali seria lixo por coluna.
     *
     * @param destino par {@code (base, topo)} por faixa; precisa caber 2x o
     *                numero de cabanas
     * @return quantas faixas foram escritas
     */
    public static int faixasNaColuna(List<WorldTreeClimbingPost> postos,
            int x, int z, int[] destino) {
        int quantas = 0;
        for (int indice = 0; indice < postos.size(); indice++) {
            WorldTreeClimbingPost posto = postos.get(indice);
            if (!posto.colunaNoPoco(x, z)) {
                continue;
            }
            destino[quantas * 2] = posto.baseDoPoco();
            destino[quantas * 2 + 1] = posto.topoDoPoco();
            quantas++;
        }
        return quantas;
    }

    /** Se este bloco cai em alguma das faixas ja apuradas para a coluna. */
    public static boolean dentroDeFaixa(int[] faixas, int quantas, int y) {
        for (int indice = 0; indice < quantas; indice++) {
            if (y >= faixas[indice * 2] && y <= faixas[indice * 2 + 1]) {
                return true;
            }
        }
        return false;
    }
}
