package com.darkcontinent.nenfoundation.enemy.greedisland.city;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Ponto;
import java.util.List;

/**
 * Uma cidade de Greed Island: ancora, pegada e distritos. Fases G6-G8.
 *
 * <p><b>NENHUMA CIDADE E UMA NBT GIGANTE</b>, e a secao 47 e explicita sobre o
 * pipeline: {@code CityAnchor -> DistrictGraph -> RoadSkeleton -> Parcels ->
 * jigsaw}. Uma estrutura monolitica de 1.200x1.500 -- o tamanho de Limeiro --
 * seria dezenas de megabytes, impossivel de revisar em diff e impossivel de
 * adaptar ao terreno: ela seria colada por cima do relevo, e o documento pede
 * adaptacao suave (secao 48).
 *
 * <p>O que este record congela e a IDENTIDADE: onde a cidade fica, que tamanho
 * ela ocupa, que papel ela exerce e de que distritos ela e feita. O arranjo dos
 * distritos dentro da pegada e o preenchimento dos lotes sao geracao.
 *
 * <p><b>OS DISTRITOS SAO ORDENADOS, e o primeiro e o NUCLEO.</b> E ele que
 * ganha o centro da cidade e o landmark; os outros orbitam. Uma lista sem ordem
 * faria o mercado externo de Masadora ter a mesma chance de cair no meio que o
 * Spell Card Hall.
 *
 * @param larguraX pegada leste-oeste, em blocos
 * @param larguraZ pegada norte-sul
 */
public record DefinicaoDeCidade(String id, String nome, Ponto ancora,
        int larguraX, int larguraZ, Papel papel, List<String> distritos,
        String landmark) {

    /** O que a cidade FAZ no jogo -- e nao como ela parece. */
    public enum Papel {
        /** Shiso Tree: entrada, tutorial, perigo baixo. */
        ENTRADA,
        /** Antokiba: primeiro grande hub, City of Prizes. */
        HUB_INICIAL,
        /** Rubicuta: comercio intermediario. */
        HUB_COMERCIAL,
        /** Masadora: Spell Cards, e o eixo da progressao. */
        CARTAS,
        /** Aiai: social, City of Love. */
        SOCIAL,
        /** Dorias: cassino, risco e recompensa. */
        JOGO,
        /** Soufrabi: porto, farol, piratas de Razor. */
        PORTO,
        /** Limeiro: capital e endgame. */
        CAPITAL
    }

    public DefinicaoDeCidade {
        if (larguraX <= 0 || larguraZ <= 0) {
            throw new IllegalArgumentException("cidade sem pegada: " + id);
        }
        if (distritos.isEmpty()) {
            throw new IllegalArgumentException(
                    "cidade sem distrito: " + id + ". Uma pegada vazia gera um terreno"
                            + " aplainado sem nada em cima -- pior que nao gerar nada.");
        }
        distritos = List.copyOf(distritos);
    }

    /** O distrito-nucleo: o que fica no centro e carrega o landmark. */
    public String nucleo() {
        return this.distritos.get(0);
    }

    /** O raio que a cidade ocupa, para consultas rapidas de "estou na cidade?". */
    public int raio() {
        return Math.max(this.larguraX, this.larguraZ) / 2;
    }

    /**
     * A zona de aproximacao da secao 49.
     *
     * <p>Ela existe para a cidade nao NASCER do nada: entre a wilderness e a
     * primeira casa tem de haver campo cultivado, cerca, estrada larga. Sem
     * isso a cidade parece colada no mapa, que e o que a secao 48 manda evitar.
     */
    public int raioDeAproximacao() {
        return raio() + Math.max(180, raio() / 2);
    }
}
