package com.darkcontinent.nenfoundation.nen.progression;

import com.darkcontinent.nenfoundation.NenFoundation;
import net.minecraft.resources.ResourceLocation;

/**
 * Os marcos de progressao do mod.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. Os marcos sao ids PROPRIOS do mod, nunca ids de quest do FTB. Trocar o
 * questbook nao pode apagar progresso de ninguem. O nucleo precisa saber onde
 * o jogador esta sem depender da camada do pack (ADR-003).
 *
 * <p>2. Eles vivem em CONSTANTES, e nao espalhados como
 * {@code id("despertou")} em cada lugar que precisa. Cinco copias da mesma
 * string divergem no primeiro erro de digitacao -- e o sintoma e um marco que
 * nunca completa, sem erro nenhum no log.
 *
 * <p>Marco novo entra aqui, com um comentario dizendo O QUE ele significa. Um
 * id sem significado escrito vira folclore em tres meses.
 */
public final class Marcos {

    /**
     * O jogador despertou para o Nen.
     *
     * <p>Gravado pelo servico de despertar, junto com {@code awakened = true}.
     * Os dois existem de proposito: o booleano e o estado, e o marco e o
     * registro de que o evento aconteceu -- e o que quests e conteudo
     * consultam sem precisar conhecer a forma do perfil.
     */
    public static final ResourceLocation DESPERTOU = NenFoundation.id("despertou");

    /**
     * O jogador descobriu qual e a propria categoria.
     *
     * <p>Gravado pela revelacao, junto com {@code category_revealed = true}.
     * Mesma dupla do despertar, pela mesma razao: o booleano e o estado, e o
     * marco e o registro consultavel de que o fato aconteceu.
     *
     * <p>POR QUE ELE PRECISA EXISTIR, e nao so o evento: o
     * {@code CategoriaReveladaEvent} passa uma vez e some. Uma quest que
     * pergunta "esta pessoa ja fez a Water Divination?" depois do fato -- ao
     * relogar, ao reabrir o questbook, ao recarregar o pack -- nao tem evento
     * nenhum para escutar. Sem o marco, a unica saida seria ler o perfil por
     * dentro, que e exatamente o que os marcos existem para evitar.
     *
     * <p>NAO ha marco para a ATRIBUICAO, e isso e de proposito: marcos viajam
     * no snapshot ate o cliente, e a atribuicao e o fato que precisa ficar
     * escondido.
     */
    public static final ResourceLocation CATEGORIA_REVELADA =
            NenFoundation.id("categoria_revelada");

    private Marcos() {
    }
}
