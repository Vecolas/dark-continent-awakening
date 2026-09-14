package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * Se o arranque sai -- e, quando nao sai, POR QUE nao.
 *
 * <p><b>Sete recusas nomeadas, e nao um booleano.</b> Arranque que falha em
 * silencio produz o pior relato de bug que existe: "as vezes ele corre e as
 * vezes nao". Com o motivo nomeado, o comando de debug imprime a razao e a
 * proxima pessoa nao vai procurar aleatoriedade onde ha uma regra.</p>
 *
 * <p>Cada recusa fecha uma falha diferente, e nenhuma delas levanta excecao:</p>
 *
 * <ul>
 *   <li>{@link #EM_ANDAMENTO} -- reautorizar o arranque no meio dele reiniciaria
 *       o relogio a cada tick, e a fase nunca chegaria ao fim: o bicho correria
 *       acelerado para sempre, sem nunca pagar a fadiga;</li>
 *   <li>{@link #FATIGADA} -- arrancar durante a fadiga apaga a unica janela de
 *       resposta que o jogador tem contra um bicho mais rapido que ele;</li>
 *   <li>{@link #EM_RECARGA} -- sem ela, dois arranques ficam colados e a fadiga
 *       passa a ser o intervalo entre eles, e nao o preco deles;</li>
 *   <li>{@link #SEM_ALVO} -- arrancar para lugar nenhum gasta o ciclo inteiro e
 *       deixa o bicho lento quando alguem finalmente aparece;</li>
 *   <li>{@link #SEM_VISAO} -- arrancar contra quem ele nao ve gasta o arranque
 *       numa parede, e pune o jogador por ter feito exatamente a coisa certa,
 *       que e quebrar a linha de visao;</li>
 *   <li>{@link #PERTO_DEMAIS} -- arrancar em cima de quem ja esta ao alcance do
 *       golpe viaja zero blocos e entrega a fadiga colada no jogador: a vantagem
 *       inteira do bicho vira uma desvantagem, e nada acusa;</li>
 *   <li>{@link #LONGE_DEMAIS} -- alem do alcance do ciclo ele chega FATIGADO, e
 *       o jogador aprende que o arranque e inofensivo;</li>
 *   <li>{@link #ALIADO_JA_ABRIU} -- o esquadrao dele ja esta em cima do alvo. O
 *       arranque e o que faz este bicho ABRIR o combate; gasto para entrar num
 *       combate que ja comecou, ele nao abre nada e ainda chega fatigado no meio
 *       da briga.</li>
 * </ul>
 */
public enum DecisaoDeArranque {
    /** Sai. */
    ARRANCAR,
    /** Ja esta arrancando: o relogio nao se reinicia. */
    EM_ANDAMENTO,
    /** Esta pagando o arranque anterior. */
    FATIGADA,
    /** Ja pagou, mas ainda nao reconquistou o direito ao proximo. */
    EM_RECARGA,
    /** Nao ha para quem correr. */
    SEM_ALVO,
    /** Ha alvo, e ele nao esta a vista. */
    SEM_VISAO,
    /** O alvo ja esta ao alcance do golpe; correr nao serve para nada. */
    PERTO_DEMAIS,
    /** O alvo esta alem do que o ciclo cobre. */
    LONGE_DEMAIS,
    /** Um membro do esquadrao ja chegou: nao ha combate para abrir. */
    ALIADO_JA_ABRIU
}
