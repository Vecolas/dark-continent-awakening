package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * O que UM flanqueador faz com a ordem que o esquadrao lhe deu.
 *
 * <p><b>{@link #CONTORNAR} e o valor que define o bicho, e ele e o que costuma
 * ser esquecido.</b> Um mob que so sabe avancar e recuar vai sempre pelo caminho
 * mais curto -- e o caminho mais curto ate um jogador e a cara dele. Isso nao da
 * erro: da um "flanqueador" que ataca de frente, com o mesmo dano, o mesmo
 * cooldown e a mesma ficha, e a unica coisa que se perde e a razao de ele
 * existir separado do frontliner.</p>
 *
 * <ul>
 *   <li>{@link #RECUAR} -- sozinho, um bicho de 28 de vida e dano 7 que avanca
 *       morre de graca. O mob continua passando em todo portao; o que se perde e
 *       a unica licao que ele ensina: eles precisam ser dois.</li>
 *   <li>{@link #AGUARDAR} -- sem ele, perder o alvo de vista manda o
 *       flanqueador correr para a ultima posicao conhecida e desmancha o cerco
 *       que os outros estavam fechando.</li>
 *   <li>{@link #CONTORNAR} -- o posto e relativo ao OLHAR do alvo, e nao ao
 *       mundo. Fixo no mundo, o flanqueador ficaria sempre ao norte do jogador e
 *       deixaria de reagir a quem gira.</li>
 *   <li>{@link #INVESTIR} -- so depois de estar fora do arco frontal. Investir
 *       de frente e a mesma coisa que nao flanquear.</li>
 * </ul>
 */
public enum DecisaoDeFlanco {
    /** Sai de perto: sozinho, ou com o esquadrao quebrado. */
    RECUAR,
    /** Segura a posicao: nao ha alvo utilizavel para contornar. */
    AGUARDAR,
    /** Vai para o proprio posto no flanco -- nunca pela reta ate o alvo. */
    CONTORNAR,
    /** Compromete-se com o golpe. */
    INVESTIR
}
