package com.darkcontinent.nenfoundation.enemy.greedisland;

/**
 * COMO um alvo de Greed Island saiu de cena -- e nem toda saida paga card.
 *
 * <p><b>"Derrotado" nao e sinonimo de "morto", e esse e o ponto do sistema.</b>
 * Em Greed Island o premio vem da CONVERSAO, e uma criatura pode ser convertida
 * sem morrer (Hyper Puffball estoura, Bubble Horse e domado) ou morrer sem
 * converter nada (matar o Cyclops com fogo, longe da condicao de captura). Um
 * {@code boolean morreu} colapsaria os dois e o card sairia de todo cadaver --
 * sem erro nenhum, com o jogador aprendendo que a condicao de captura e
 * decorativa.</p>
 */
public enum DefeatResult {
    /** Ainda em campo. Nao paga nada. */
    EM_CURSO,
    /** Morreu sem satisfazer a condicao de captura. Nao vira card. */
    MORTO,
    /** Satisfez a condicao de captura. E o unico resultado que converte. */
    CAPTURADO,
    /** Escapou por conta propria -- fugiu, mergulhou, se enterrou. */
    ESCAPOU,
    /** Todos os participantes foram embora. Nao ha a quem pagar. */
    ABANDONADO;

    /** A UNICA pergunta que o servico de conversao faz. */
    public boolean converte() { return this == CAPTURADO; }
}
