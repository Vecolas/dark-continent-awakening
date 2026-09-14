package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * POR QUE o agarrao foi recusado -- nunca um {@code false} calado.
 *
 * <p>"Ativacao que falha em silencio produz o pior relato de bug que existe."
 * Aqui o relato seria: <em>"as vezes o bicho me morde e nao me engole"</em>.
 * Sem motivo, ninguem descobre que a diferenca era o tamanho do alvo ou o
 * predador ja estar com alguem na boca.</p>
 */
public enum GrabRefusal {
    /** Nao ha recusa: este agarrao pode comecar. */
    NENHUMA,
    /** O predador ja segura alguem. Uma boca, uma vitima. */
    JA_AGARRANDO,
    /** O alvo nao cabe: alto ou largo demais para a boca deste bicho. */
    ALVO_GRANDE_DEMAIS,
    /** O alvo ja esta montado em outra coisa; roubar quebraria o outro ciclo. */
    ALVO_OCUPADO,
    /** Espectador, criativo ou alvo invalido de qualquer outra forma. */
    ALVO_INVALIDO
}
