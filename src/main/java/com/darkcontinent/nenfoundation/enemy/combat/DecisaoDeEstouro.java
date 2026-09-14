package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * POR QUE o estouro de esporos aconteceu -- ou nao -- nunca um {@code false} calado.
 *
 * <p>"Recusa sempre tem motivo." Um boolean aqui responderia "estourou?" e
 * esconderia quatro historias completamente diferentes: o bicho ainda estava
 * saudavel, quem bateu estava longe, ele ja tinha estourado, ou nao havia
 * ninguem para estourar contra. O relato de bug que vem de um boolean e o pior
 * que existe -- <em>"as vezes ele explode e as vezes nao"</em> -- e ninguem
 * descobre sozinho que a diferenca era a distancia do arqueiro.</p>
 *
 * <p>Este enum nao e exibido ao jogador: a recusa dele e silenciosa em jogo de
 * proposito, porque um fungo que anuncia "nao vou estourar" entregaria a
 * resposta. Quem le o motivo e quem depura e quem testa.</p>
 */
public enum DecisaoDeEstouro {
    /** Vida abaixo do limiar, atacante ao alcance do toque e ainda inteiro: estoura AGORA. */
    ESTOURA,
    /** Ja estourou uma vez. Nao existe segunda -- ver {@link RegrasDeEstouro}. */
    JA_ESTOUROU,
    /** Levou dano e continua acima do limiar: a casca aguentou. */
    VIDA_ACIMA_DO_LIMIAR,
    /**
     * O dano veio de longe.
     *
     * <p>E ESTA e a licao do bicho: flecha, magia e qualquer coisa disparada de
     * fora do alcance do toque matam o fungo sem estouro nenhum.</p>
     */
    GATILHO_LONGE_DEMAIS,
    /**
     * Nao havia corpo nenhum para medir distancia: fogo, queda, afogamento, veneno.
     *
     * <p>Sem atacante nao ha quem pagar o preco, e um estouro contra o vazio
     * gastaria a unica carta do bicho sem que ninguem visse.</p>
     */
    SEM_ATACANTE
}
