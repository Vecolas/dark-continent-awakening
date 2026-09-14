package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * O que a sequencia decidiu neste tick -- resposta EXPLICITA, nunca boolean.
 *
 * <p>Um {@code boolean} responderia "comeca um golpe agora?" e apagaria a unica
 * coisa que um combo precisa distinguir: <em>por que</em> ele nao comecou. As
 * causas ensinam coisas diferentes -- {@link #RECUSADA_POR_RECARGA} e "espere",
 * {@link #RECUSADA_POR_RECUO} e "ele desistiu", {@link #INTERROMPIDA} e "voce
 * cortou" e {@link #ENCERRADA_POR_FIM} e "acabou" -- e quem escreve som,
 * particula, tela de debug ou teste precisa saber qual delas aconteceu.</p>
 *
 * <p>Isto e a mesma regra do CLAUDE.md que manda toda recusa ter motivo. "O
 * oficial parou de atacar no meio" e um relato de bug quando a resposta e
 * silencio, e e um aprendizado quando o codigo sabe dizer que foi o stagger que
 * cortou a sequencia.</p>
 */
public enum DecisaoDeSequencia {
    /** Nao ha sequencia em curso e tambem nao ha nada a comecar. */
    SEGUE_PARADA,
    /** O primeiro golpe comeca AGORA; quem chama inicia o relogio de ataque. */
    COMECAR,
    /**
     * O golpe em curso ainda esta acontecendo; nao se faz nada.
     *
     * <p>E a resposta mais comum de todas, e ela e ativa e nao omissa: quem chama
     * precisa saber que a sequencia continua viva para nao deixar outra Goal
     * assumir a navegacao no meio do combo.</p>
     */
    ESPERAR,
    /** O proximo golpe da sequencia comeca agora, emendado na recuperacao do anterior. */
    ENCADEAR,
    /**
     * O stagger cortou a sequencia, e o resto dela NAO acontece.
     *
     * <p>Esta e a recompensa de quem interrompeu, e ela precisa de nome proprio:
     * sem ela, cortar tres golpes e cortar zero teriam a mesma leitura no
     * codigo, e a diferenca -- que e a coisa mais valiosa que o jogador pode
     * fazer contra este mob -- viraria invisivel.</p>
     */
    INTERROMPIDA,
    /** O alvo morreu ou sumiu; nao ha em quem encadear. */
    ENCERRADA_SEM_ALVO,
    /** O ultimo golpe terminou. A sequencia acabou por onde devia acabar. */
    ENCERRADA_POR_FIM,
    /**
     * A recarga ainda corre. Recusa com motivo, e nao silencio.
     *
     * <p>Ela cobre dois casos que parecem o mesmo e nao sao: a recarga normal
     * entre sequencias e a recarga PUNITIVA imposta a quem foi interrompido. As
     * duas chegam aqui, e e o valor da recarga -- nao esta constante -- que as
     * separa.</p>
     */
    RECUSADA_POR_RECARGA,
    /**
     * O bicho decidiu recuar, e quem recua nao comeca combo.
     *
     * <p>Uma sequencia JA em curso nao e cancelada por isto, de proposito: o
     * jogador ja viu o primeiro golpe sair, e desarmar o resto em silencio faria
     * o telegrafo mentir. Quem recua ganha o combo TERMINANDO, e nao o combo
     * desaparecendo.</p>
     */
    RECUSADA_POR_RECUO
}
