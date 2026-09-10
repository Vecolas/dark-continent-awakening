package com.darkcontinent.nenfoundation.nen.technique;

/**
 * Por que uma tecnica ou habilidade parou.
 *
 * <p>DECISAO: o motivo e um valor tipado e obrigatorio, nao um booleano nem uma
 * string. Duas coisas dependem dele — a mensagem que o jogador ve e a politica
 * de reativacao — e as duas ficam erradas em silencio quando o motivo se perde.
 *
 * <p>Nao ha valor "OUTRO". Motivo inventado e pior que motivo ausente: se um
 * caso novo aparecer, ele ganha uma constante aqui e todo {@code switch}
 * exaustivo passa a reprovar na compilacao, que e exatamente o que se quer.
 */
public enum StopReason {

    /** O jogador desligou. */
    PLAYER_REQUEST,

    /** A aura acabou. */
    OUT_OF_AURA,

    /** Outra tecnica incompativel foi ativada. Ver a matriz de exclusao. */
    REPLACED_BY_INCOMPATIBLE,

    /** Efeito externo interrompeu (stun, silence, mecanica de boss). */
    INTERRUPTED,

    /** Duracao natural terminou. */
    EXPIRED,

    /** O jogador morreu. */
    DEATH,

    /** O jogador saiu do servidor. */
    LOGOUT,

    /** O jogador trocou de dimensao e o estado nao sobrevive a troca. */
    DIMENSION_CHANGE,

    /** O alvo ou a ancora do efeito deixou de ser valido. */
    TARGET_LOST,

    /** Recarga de datapack/config invalidou a definicao em uso. */
    DEFINITION_RELOADED
}
