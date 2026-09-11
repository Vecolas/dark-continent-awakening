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
     * O jogador SOUBE qual e a sua categoria.
     *
     * <p>Gravado pela revelacao, junto com {@code category_revealed = true}.
     * E o marco do onboarding: e nele que uma quest "descubra o seu Nen"
     * completa.
     *
     * <p>POR QUE NAO HA UM MARCO PARA A ATRIBUICAO: os marcos viajam INTEIROS
     * no snapshot ate o cliente. Um marco posto no momento em que a categoria
     * e sorteada contaria ao cliente um fato que o jogador ainda nao tem --
     * exatamente o que {@code categoriaVisivel()} existe para impedir. A
     * atribuicao ja e legivel no servidor por
     * {@code category() != UNDETERMINED}, e uma segunda fonte para a mesma
     * verdade divergiria.
     */
    public static final ResourceLocation CATEGORIA_REVELADA =
            NenFoundation.id("categoria_revelada");

    private Marcos() {
    }
}
