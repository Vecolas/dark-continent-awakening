package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * O que um inimigo de alcance faz com os pes: chegar, ficar ou sair.
 *
 * <p><b>Ela existe porque "papel RANGED" nao e comportamento.</b> Um oficial
 * marcado como atirador que anda ate encostar no alvo continua nascendo,
 * atacando, dropando loot e passando em todo portao -- ele so deixa de ser um
 * atirador. O rotulo no perfil nao produz recuo nenhum; esta decisao produz.</p>
 *
 * <p>Os tres valores saem do MESMO par de alcances que autoriza o tiro (ver
 * {@link RegrasDeTeia}). Escritos separados, o limiar de recuo e o limiar de
 * recusa divergiriam, e o sintoma seria uma aranha que recua ate uma distancia
 * em que ela ainda se recusa a atirar: ela andaria para tras e para frente para
 * sempre, sem erro nenhum e sem nunca disparar.</p>
 */
public enum DecisaoDeEspacamento {
    /** Longe demais para o fio: ela fecha a distancia. */
    APROXIMAR,

    /** Na faixa de tiro: ela para e atira. Parar e o que faz o telegrafo ser lido. */
    MANTER,

    /**
     * Perto demais: ela sai de perto.
     *
     * <p>Este e o valor que o jogador PROVOCA. Ele e a resposta ao agarramento --
     * encostar nela custa a teia -- e por isso ele precisa ser visivel: uma
     * aranha que recusa o tiro e continua parada ensinaria que ela travou.</p>
     */
    RECUAR
}
