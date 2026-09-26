package com.darkcontinent.nenfoundation.enemy.greedisland.city;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandConstants.Ponto;
import com.darkcontinent.nenfoundation.enemy.greedisland.city.DefinicaoDeCidade.Papel;
import java.util.List;
import java.util.Optional;

/**
 * As oito cidades, congeladas. Fases G6, G7 e G8.
 *
 * <p>Pegadas, papeis, distritos e landmarks vem das secoes 39 a 46 do
 * documento, sem invencao. As ancoras vem da secao 37, por
 * {@link GreedIslandConstants#CIDADES} -- <b>uma fonte so para a posicao</b>,
 * porque duas listas de coordenada divergem no dia em que alguem move uma
 * cidade e esquece a outra, e o sintoma seria a estrada chegando num campo
 * vazio ao lado da cidade.
 */
public final class RegistroDeCidades {

    private static final List<DefinicaoDeCidade> CIDADES = List.of(
            new DefinicaoDeCidade("shiso_tree", "Shiso Tree", ancora("shiso_tree"),
                    260, 260, Papel.ENTRADA,
                    List.of("clareira_do_shiso", "acampamento", "trilhas"),
                    "shiso_tree"),

            new DefinicaoDeCidade("antokiba", "Antokiba", ancora("antokiba"),
                    420, 520, Papel.HUB_INICIAL,
                    // "funcional, colorida, nao medieval" -- secao 40.
                    List.of("prize_square", "distrito_de_eventos", "comercial",
                            "estalagem", "residencial", "arrabalde", "campos_de_evento"),
                    "prize_square"),

            new DefinicaoDeCidade("rubicuta", "Rubicuta", ancora("rubicuta"),
                    380, 460, Papel.HUB_COMERCIAL,
                    List.of("mercado", "oficinas", "estalagem", "bairro_dos_viajantes",
                            "administracao"),
                    "mercado"),

            new DefinicaoDeCidade("masadora", "Masadora", ancora("masadora"),
                    700, 850, Papel.CARTAS,
                    List.of("mercado_central_de_feitico", "lojas_de_carta", "cambio",
                            "servicos", "estalagem", "logistica", "residencial",
                            "mercado_externo", "patio_de_teste"),
                    "spell_card_hall"),

            new DefinicaoDeCidade("aiai", "Aiai", ancora("aiai"),
                    550, 700, Papel.SOCIAL,
                    // "evitar caricatura rosa" -- secao 43. A identidade vem de
                    // cafe, praca, jardim, caminho cenico e ponte; nao de cor.
                    List.of("praca_central", "cafes", "jardins", "caminhos_cenicos",
                            "pontes", "residencial"),
                    "praca_central"),

            new DefinicaoDeCidade("dorias", "Dorias", ancora("dorias"),
                    650, 900, Papel.JOGO,
                    List.of("nucleo_de_cassino", "saloes_de_jogo", "entretenimento",
                            "anel_comercial", "hospedagem", "vielas_de_servico"),
                    "nucleo_de_cassino"),

            new DefinicaoDeCidade("soufrabi", "Soufrabi", ancora("soufrabi"),
                    900, 1_250, Papel.PORTO,
                    // "a topografia costeira deve participar da cidade" -- secao 45.
                    List.of("porto", "docas", "armazens", "bairro_pesqueiro",
                            "cidade_alta", "farol", "zona_pirata"),
                    "farol"),

            new DefinicaoDeCidade("limeiro", "Limeiro", ancora("limeiro"),
                    1_200, 1_500, Papel.CAPITAL,
                    // "monumental, mas nao medieval generico" -- secao 46.
                    List.of("castelo", "avenida_central", "mercado", "residencial",
                            "pracas_publicas", "jardins", "cidade_externa",
                            "distrito_de_servico"),
                    "castelo"));

    private RegistroDeCidades() {
    }

    /** As oito, na ordem de progressao. */
    public static List<DefinicaoDeCidade> todas() {
        return CIDADES;
    }

    /** Uma cidade pelo id. */
    public static Optional<DefinicaoDeCidade> porId(String id) {
        return CIDADES.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    /**
     * A cidade cuja pegada cobre este ponto, se houver.
     *
     * <p>PEGADA, e nao aproximacao: quem pergunta "estou na cidade?" quer saber
     * se ha rua embaixo do pe, e nao se ha campo cultivado no horizonte.
     */
    public static Optional<DefinicaoDeCidade> em(double x, double z) {
        return CIDADES.stream()
                .filter(c -> Math.abs(x - c.ancora().x()) <= c.larguraX() / 2.0D
                        && Math.abs(z - c.ancora().z()) <= c.larguraZ() / 2.0D)
                .findFirst();
    }

    private static Ponto ancora(String id) {
        var a = GreedIslandConstants.cidade(id).orElseThrow(
                () -> new IllegalStateException("cidade sem ancora na secao 37: " + id));
        return new Ponto(a.x(), a.z());
    }
}
