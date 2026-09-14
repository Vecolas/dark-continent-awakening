package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * O que um golpe fez com o stagger -- resultado EXPLICITO, nunca boolean.
 *
 * <p>Um {@code boolean} responderia "interrompeu?" e esconderia a diferenca
 * entre "nao chegou perto" e "quase". Quem escreve a transicao de estado precisa
 * das tres respostas: a absorvida nao gera feedback nenhum, a acumulada pede um
 * som ou um tremor, e a disparada troca o estado do mob. Colapsar isso em
 * verdadeiro/falso e como a recusa silenciosa que o CLAUDE.md proibe -- o
 * jogador bate, alguma coisa acontece, e nada explica o que.</p>
 */
public enum StaggerResult {
    /** A resistencia comeu o golpe inteiro. Nao entrou nada na conta. */
    ABSORVIDO,
    /** Entrou na conta e ainda nao chegou ao limiar. */
    ACUMULOU,
    /** Chegou ao limiar: o mob cambaleia AGORA. */
    DISPAROU,
    /** O mesmo ataque ja tinha contado. Repetir nao soma -- ver StaggerState. */
    REPETIDO
}
