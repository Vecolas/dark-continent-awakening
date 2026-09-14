package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Se o salto sai -- e, quando nao sai, POR QUE nao.
 *
 * <p><b>Cinco recusas nomeadas, e nao um booleano.</b> Salto que falha em
 * silencio produz o pior relato de bug que existe: "as vezes ele pula e as vezes
 * nao". Com o motivo nomeado, o comando de debug imprime a razao e a proxima
 * pessoa nao vai procurar aleatoriedade onde ha uma regra.</p>
 *
 * <p>Cada recusa fecha uma falha diferente, e nenhuma delas levanta excecao:</p>
 *
 * <ul>
 *   <li>{@link #EM_RECARGA} -- sem ela, um mob interrompido recomeca o salto no
 *       tick seguinte, e interromper deixa de ser tatica;</li>
 *   <li>{@link #SEM_APOIO} -- saltar no ar SOMA impulsos. O bicho ganha altura a
 *       cada salto e atravessa o cenario voando, com o mesmo dano e o mesmo log
 *       limpo;</li>
 *   <li>{@link #PERTO_DEMAIS} -- saltar em cima de quem ja esta ao alcance gasta
 *       o telegrafo inteiro para viajar zero blocos. Na tela e um bicho que
 *       agacha por um segundo e morde exatamente onde ja estava;</li>
 *   <li>{@link #LONGE_DEMAIS} -- o salto tem alcance fisico. Aceitar alvos alem
 *       dele faz o bicho pousar no vazio e recomecar, e o jogador aprende que o
 *       salto e inofensivo;</li>
 *   <li>{@link #SUBIDA_DEMAIS} -- saltar contra um alvo acima do que o impulso
 *       alcanca e bater na parede e cair de volta, com a recarga gasta. Nada
 *       acusa: o mob parece estar tentando.</li>
 * </ul>
 */
public enum DecisaoDeSalto {
    /** Sai. */
    SALTAR,
    /** A recarga ainda nao acabou. */
    EM_RECARGA,
    /** Os pes nao estao no chao: nao ha de onde empurrar. */
    SEM_APOIO,
    /** O alvo esta dentro do alcance do golpe comum; o salto nao serve para nada. */
    PERTO_DEMAIS,
    /** O alvo esta alem do que o impulso cobre. */
    LONGE_DEMAIS,
    /** O alvo esta mais alto do que o salto sobe. */
    SUBIDA_DEMAIS
}
