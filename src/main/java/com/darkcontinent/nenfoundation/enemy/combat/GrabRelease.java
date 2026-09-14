package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * POR QUE o agarrao terminou -- e a lista e fechada de proposito.
 *
 * <p><b>O motivo nao e enfeite de log.</b> Cada saida pede uma consequencia
 * diferente: soltar por tempo cospe a vitima viva, soltar por dano premia quem
 * reagiu, e soltar por unload nao pode tocar em mundo nenhum porque o mundo
 * acabou de sair da memoria. Um {@code boolean solta} colapsaria os dez casos e
 * a diferenca so apareceria no pior deles.</p>
 *
 * <p>A lista existe inteira num lugar so pelo motivo que o {@link GrabRules} ja
 * documenta: espalhar a decisao pelos pontos de saida -- um {@code if} na morte,
 * outro no tick, outro na remocao -- e exatamente como o agarrao fica ligado
 * para sempre em UM deles. O caminho esquecido nao da erro nenhum; deixa um
 * jogador preso dentro de um bicho ate o servidor reiniciar.</p>
 */
public enum GrabRelease {
    /** A janela acabou. A vitima sai VIVA -- cuspida, nao digerida. */
    TEMPO,
    /** O predador apanhou o bastante. E a recompensa de quem reagiu. */
    DANO,
    /** A vitima morreu enquanto presa. */
    VITIMA_MORTA,
    /** O predador morreu segurando alguem. */
    PREDADOR_MORTO,
    /** A vitima saiu do mundo por fora do nosso ciclo: logout, /kill, outro mod. */
    VITIMA_SUMIU,
    /** Chunk descarregado ou entidade removida; nao se toca em mundo nesta saida. */
    UNLOAD,
    /** Troca de dimensao: a vitima nao pode ir junto nem ficar presa a um fantasma. */
    DIMENSAO,
    /** Alguem teleportou a vitima para longe do predador. */
    TELEPORTE,
    /** Stagger, atordoamento ou qualquer interrupcao externa. */
    INTERROMPIDO
}
