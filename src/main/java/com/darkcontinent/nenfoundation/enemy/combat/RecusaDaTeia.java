package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Por que a teia NAO saiu -- e ela quase sempre nao sai.
 *
 * <p><b>Ela existe porque recusa em silencio e o pior relato de bug que existe.</b>
 * Um {@code boolean podeLancar} devolveria {@code false} para seis situacoes
 * diferentes, e a unica frase que sobraria para quem joga -- e para quem depura --
 * seria "a aranha nao atira". Com nome, a mesma observacao vira uma pergunta
 * respondivel: ela nao atira porque voce esta COLADO nela, e nao porque ela
 * quebrou.</p>
 *
 * <p><b>O nome tambem e o que fica visivel na tela.</b> {@link #PERTO_DEMAIS} nao
 * e so um valor: e o unico motivo desta lista que o jogador consegue provocar de
 * proposito, e ele existe para ser aprendido. A Spider Webber que recusa por
 * perto recua no mesmo tick, e esse recuo E a mensagem.</p>
 */
public enum RecusaDaTeia {
    /** Nao ha recusa: a teia pode sair agora. */
    NENHUMA,

    /**
     * A distancia medida nao e um numero utilizavel.
     *
     * <p>NaN ou negativo nao podem LIBERAR o tiro. Um alvo em estado estranho --
     * meio teleportado, num tick de troca de dimensao -- produziria uma distancia
     * invalida, e tratar invalido como "dentro da faixa" faria a teia sair contra
     * alguem do outro lado do mundo, sem uma linha de log.</p>
     */
    MEDIDA_INVALIDA,

    /**
     * Ela ja tem alguem preso.
     *
     * <p>Uma fiandeira, uma presa. Permitir a segunda teia deixaria o relogio da
     * primeira orfao -- o contador seria sobrescrito e a primeira vitima ficaria
     * imobilizada sem prazo nenhum, que e exatamente a morte sem resposta que o
     * plano proibe.</p>
     */
    JA_PRENDENDO,

    /** O ataque anterior ainda esta em recarga ou em curso. */
    EM_RECARGA,

    /**
     * O alvo esta DENTRO da zona morta -- e esta e a licao do encontro.
     *
     * <p>Ela e uma atiradora: de perto nao ha fio para esticar. Quem encosta nela
     * desliga a teia, e e por isso que este motivo nao e um defeito a ser
     * corrigido com um golpe corpo-a-corpo de consolo. Dar a ela um ataque de
     * perto apagaria a unica resposta que o jogador tem contra a imobilizacao.</p>
     */
    PERTO_DEMAIS,

    /** O alvo esta fora do alcance do fio; ela aproxima em vez de atirar. */
    LONGE_DEMAIS,

    /**
     * Ha parede entre as duas.
     *
     * <p>Teia que atravessa bloco e a versao aranha de "aceitar a entidade que o
     * cliente apontou": funciona perfeitamente em campo aberto e transforma
     * qualquer cobertura em decoracao, sem que nada acuse.</p>
     */
    SEM_LINHA_DE_VISAO
}
