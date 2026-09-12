package com.darkcontinent.nenfoundation.api;

/**
 * O que um observador consegue perceber da aura de outra pessoa.
 *
 * <p>ELE E DELIBERADAMENTE POBRE. Nao carrega quanta aura o alvo tem, nem que
 * tecnicas ele desbloqueou, nem qual e a categoria dele. Carrega so o que
 * alguem de pe ao lado perceberia -- e nada do que so o dono deveria saber.
 *
 * <p>{@link #NENHUM} E O PONTO INTEIRO DO DESENHO. Quem esta em Zetsu transmite
 * exatamente o mesmo valor de quem nunca despertou. Nao ha bandeira de
 * "escondido" para um cliente modificado ler: os dois casos sao o MESMO byte na
 * rede, e nao ha o que revelar.
 *
 * <p>E por isso que a decisao mora no servidor. Filtrar no cliente exigiria
 * mandar a ele o segredo junto do pedido de nao mostrar -- o que funciona
 * perfeitamente com cliente honesto, e so com ele.
 *
 * <p>SEM INTENSIDADE, por enquanto. O canone diz que se sente a magnitude de um
 * Ren, e um byte quantizado caberia aqui. Ficou de fora porque {@link #TEN} e
 * {@link #REN} ja carregam a diferenca que a tela precisa, e todo campo a mais
 * e mais uma coisa para versionar e mais uma coisa que pode vazar. Entra se um
 * playtest pedir, e nao antes.
 */
public enum SinalDeAura {

    /**
     * Nada perceptivel.
     *
     * <p>Quem nunca despertou, quem esta sem tecnica ligada e quem esta em
     * Zetsu -- os tres sao indistinguiveis daqui, de proposito.
     */
    NENHUM,

    /** Uma pelicula fina e estavel: aura retida junto ao corpo. */
    TEN,

    /** Volume grande de aura liberada. No canone, uma pessoa em Ren e sentida. */
    REN;

    /** O valor seguro para qualquer situacao desconhecida. */
    public static SinalDeAura seguro() {
        return NENHUM;
    }
}
