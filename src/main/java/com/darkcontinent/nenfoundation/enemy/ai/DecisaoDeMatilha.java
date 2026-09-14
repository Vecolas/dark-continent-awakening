package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * O que UM lobo faz com a ordem que o bando lhe deu.
 *
 * <p><b>Por que quatro valores e nao um booleano "ataca".</b> Um bando que so
 * sabe atacar ou nao atacar nao e um bando: e uma fila de bichos correndo para o
 * mesmo ponto. As tres alternativas ao ataque sao o que separa cerco de
 * multidao, e cada uma existe por uma falha que nao levanta erro:</p>
 *
 * <ul>
 *   <li>{@link #RECUAR} -- sem ele, um lobo sozinho com HP 26 e dano 6 avanca
 *       contra um jogador de ferro e morre de graca. O bicho continua funcionando
 *       em todo portao, e o que se perde e a unica coisa que ele ensina: conte
 *       quantos sao antes de decidir.</li>
 *   <li>{@link #AGUARDAR} -- sem ele, um lobo que perdeu o alvo de vista sai
 *       correndo para a ultima posicao conhecida e desmancha o cerco que os
 *       outros tres estavam fechando.</li>
 *   <li>{@link #CERCAR} -- sem ele, todos avancam pelo caminho mais curto,
 *       chegam no mesmo ponto e viram uma coluna empurrando a si mesma. O jogador
 *       le isso como travamento, nunca como IA.</li>
 *   <li>{@link #INVESTIR} -- e ele tem de ser de POUCOS, nunca de todos. Quatro
 *       lobos mordendo ao mesmo tempo transformam o cerco numa trituradora, e a
 *       janela de recuperacao -- que e o que espaca os golpes no tempo -- deixa
 *       de valer.</li>
 * </ul>
 */
public enum DecisaoDeMatilha {
    /** Sai de perto: sozinho, ou com o bando quebrado. */
    RECUAR,
    /** Segura a posicao: nao ha alvo utilizavel para cercar. */
    AGUARDAR,
    /** Vai para o proprio posto no anel e espera a vez. */
    CERCAR,
    /** Compromete-se com a mordida. */
    INVESTIR
}
