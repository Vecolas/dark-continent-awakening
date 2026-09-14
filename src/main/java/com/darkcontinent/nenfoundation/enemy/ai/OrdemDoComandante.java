package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * O que sai do alto neste tick de coordenacao -- ou POR QUE nao saiu nada.
 *
 * <p><b>As recusas moram no mesmo enum que as ordens de proposito.</b> Um
 * {@code Optional} vazio diria "nao houve ordem" e engoliria a unica informacao
 * util: qual das quatro razoes foi. E elas sao diferentes demais para virar uma
 * so -- {@link #BAIXA_DEMAIS} e uma conquista do jogador, {@link #FORA_DO_ORCAMENTO}
 * e o relogio funcionando, {@link #NAO_LIDERA} e um erro de alistamento e
 * {@link #SEM_BANDO} e uma comandante sozinha. Tratadas como "nada", as quatro
 * viram a mesma linha em branco num diagnostico, e a investigacao comeca do zero.
 * O CLAUDE.md escreve isso em uma frase: recusa sempre tem motivo.</p>
 */
public enum OrdemDoComandante {
    /** Publicar no bando o alvo que so ela viu -- e a ordem que justifica o alcance 36. */
    TROCAR_ALVO,
    /** Mandar o bando fechar de volta em torno do alvo. */
    REAGRUPAR,
    /** Nada a mandar: o bando ja esta como ela quer. Ordem repetida e ordem ignorada. */
    NADA_A_ORDENAR,

    // ------------------------------------------------------------- as recusas

    /** Ela nao e a lider deste bando. Duas vozes no mesmo bando dao duas ordens por tick. */
    NAO_LIDERA,
    /** Nao e o tick de coordenacao. O orcamento de SquadRules manda, nunca o tick. */
    FORA_DO_ORCAMENTO,
    /** Ela esta fora da altitude de comando. E o que o jogador aprende a forcar. */
    BAIXA_DEMAIS,
    /** Nao ha bando para ordenar. */
    SEM_BANDO;

    /** Saiu ordem de verdade? {@link #NADA_A_ORDENAR} nao e ordem: e silencio com o bando ok. */
    public boolean saiuOrdem() { return this == TROCAR_ALVO || this == REAGRUPAR; }

    /** Foi recusa? Serve para diagnostico e teste; o consumidor age pelo valor, nao por isto. */
    public boolean recusada() {
        return this == NAO_LIDERA || this == FORA_DO_ORCAMENTO
                || this == BAIXA_DEMAIS || this == SEM_BANDO;
    }
}
