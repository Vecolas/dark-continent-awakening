package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * O que um perseguidor faz com o rastro de quem fugiu.
 *
 * <p><b>Por que tres valores e nao um booleano "persegue".</b> Perseguir e nao
 * perseguir sao dois estados; o que falta entre eles e o unico que interessa
 * neste mob -- <b>seguir para onde o alvo estava</b>. Cada valor existe por uma
 * falha que nao levanta erro:</p>
 *
 * <ul>
 *   <li>{@link #PERSEGUIR_A_VISTA} -- e o unico caso em que a posicao do alvo
 *       vale AGORA. Tratar tudo como rastro faria o bicho correr sempre para
 *       onde o alvo estava um segundo atras, e ele nunca alcancaria ninguem
 *       andando em linha reta;</li>
 *   <li>{@link #SEGUIR_O_RASTRO} -- sem ele, sair do campo de visao encerra a
 *       perseguicao no mesmo tick, e fugir passa a ser gratis: bastaria dobrar
 *       uma esquina. O encontro inteiro deixa de existir, e nada acusa;</li>
 *   <li>{@link #DESISTIR} -- sem ele, "perdi de vista" vira "persigo para
 *       sempre". O bicho atravessa o mapa atras de uma memoria, nunca volta
 *       para casa e nunca para de consumir caminho. Nao da erro: da um mob que
 *       o jogador descreve como injusto, e um servidor que fica mais lento a
 *       cada hora porque ninguem nunca desengaja.</li>
 * </ul>
 */
public enum DecisaoDeRastro {
    /** O alvo esta a vista: a posicao atual dele e que manda. */
    PERSEGUIR_A_VISTA,
    /** Sem contato, mas dentro do prazo: vai ate onde o cheiro termina. */
    SEGUIR_O_RASTRO,
    /** Prazo vencido, ou fim do rastro sem ninguem la: larga. */
    DESISTIR
}
