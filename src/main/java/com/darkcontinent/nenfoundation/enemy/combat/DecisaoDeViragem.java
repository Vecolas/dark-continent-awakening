package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * O que um evento fez com a postura do besouro -- resposta EXPLICITA, nunca boolean.
 *
 * <p>Um {@code boolean virou} responderia "caiu de costas?" e apagaria a unica
 * coisa que o encontro precisa distinguir: <em>por que</em> ele caiu. As duas
 * causas ensinam licoes opostas -- {@link #VIRA_PELA_INVESTIDA} e o premio de
 * quem DESVIOU, {@link #VIRA_PELO_TRANCO} e o premio de quem BATEU no momento
 * certo -- e quem escreve som, particula ou bestiario precisa saber qual das
 * duas aconteceu. Colapsar as duas em verdadeiro faria o jogo dar o mesmo
 * retorno para as duas jogadas, e o jogador nunca descobriria que sao duas.</p>
 *
 * <p>{@link #SO_CAMBALEIA} e {@link #JA_ESTA_DE_COSTAS} existem pela mesma razao
 * que {@code StaggerResult.ABSORVIDO} existe: a recusa tem de ter motivo. "Bati
 * com tudo e ele nao virou" e um relato de bug quando a resposta e silencio, e e
 * um aprendizado quando o codigo sabe dizer que o tranco chegou fora da janela
 * em que ele derruba.</p>
 */
public enum DecisaoDeViragem {
    /** Nada aconteceu com a postura: ele segue de pe, e a carapaca segue pagando pouco. */
    SEGUE_DE_PE,
    /**
     * O tranco disparou, mas fora da pose que derruba.
     *
     * <p>Interromper um besouro que NAO esta empinado e um stagger comum: ele
     * perde o golpe e continua de pe. Tratar isto como viragem transformaria
     * qualquer interrupcao em ponto fraco de graca, e a janela deixaria de
     * depender de acertar o momento -- sem que nada acusasse, porque o mob
     * continuaria funcionando perfeitamente.</p>
     */
    SO_CAMBALEIA,
    /**
     * A investida fechou a janela ACTIVE sem encostar em ninguem.
     *
     * <p>Ele se derruba sozinho: os chifres entram no chao e o peso da carapaca
     * termina o giro. E o premio de quem desviou, e e a unica forma de virar o
     * bicho que nao exige bater nele.</p>
     */
    VIRA_PELA_INVESTIDA,
    /**
     * O tranco disparou enquanto ele estava EMPINADO para investir.
     *
     * <p>Nessa pose ele se apoia so no par traseiro de pernas. E o premio de
     * quem leu o telegrafo e bateu dentro dele.</p>
     */
    VIRA_PELO_TRANCO,
    /**
     * Ele ja esta de costas, e nada reinicia a janela.
     *
     * <p>Esta e a recusa que protege o jogador de si mesmo: sem ela, cada golpe
     * no ventre renovaria o contador e o besouro ficaria de costas para sempre
     * enquanto alguem estivesse batendo. Isso nao da erro nenhum -- da um chefe
     * que morre sem nunca mais se levantar, e o encontro inteiro desaparece.</p>
     */
    JA_ESTA_DE_COSTAS
}
