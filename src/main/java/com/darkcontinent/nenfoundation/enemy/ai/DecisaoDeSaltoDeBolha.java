package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * POR QUE o Bubble Horse saltou -- ou nao -- nunca um {@code false} calado.
 *
 * <p>"Recusa sempre tem motivo." Um boolean aqui responderia "saltou?" e
 * esconderia sete historias diferentes: o bicho esta exausto, cambaleando, no
 * meio de uma patada, ainda no ar, ainda na pausa, sem ameaca nenhuma, ou com a
 * ameaca longe demais para valer o gasto. O relato de bug que vem de um boolean e
 * o pior que existe -- <em>"as vezes ele foge e as vezes nao"</em> -- e ninguem
 * descobre sozinho que a diferenca era a pausa de doze ticks.</p>
 *
 * <p>Este enum nao e exibido ao jogador. Quem le o motivo e quem depura, quem
 * testa, e o comando de diagnostico.</p>
 */
public enum DecisaoDeSaltoDeBolha {
    /** Ha ameaca perto, ele esta no chao e a pausa acabou: impulso AGORA. */
    SALTA,
    /**
     * Ele esta exausto -- a janela de captura esta aberta.
     *
     * <p>Este e o unico estado em que o bicho aceita ficar parado, e ele e a
     * mecanica inteira: fugir aqui apagaria a janela em que o jogador tem de
     * parar de bater.</p>
     */
    EXAUSTO,
    /** Cambaleando. Interrupcao que nao interrompe a fuga nao interrompe nada. */
    CAMBALEANDO,
    /**
     * No meio de uma patada.
     *
     * <p>Saltar durante o golpe arrastaria a caixa de dano junto e faria o bicho
     * acertar de longe -- sem erro, e com o jogador aprendendo uma distancia que
     * o desenho nao cumpre.</p>
     */
    EM_GOLPE,
    /**
     * Ainda no ar do salto anterior.
     *
     * <p>Um segundo impulso no ar SOMA ao primeiro: dois saltos seguidos
     * mandariam o cavalo a dez blocos de altura. Nao da erro nenhum -- da um mob
     * que "voa as vezes".</p>
     */
    NO_AR,
    /** A pausa entre impulsos ainda nao acabou. E ela que faz o salto ser legivel. */
    NA_PAUSA,
    /** Nao ha ameaca medida: nao ha de quem fugir. */
    SEM_AMEACA,
    /** A ameaca esta fora da distancia de conforto. Fugir do horizonte e ruido. */
    AMEACA_LONGE
}
