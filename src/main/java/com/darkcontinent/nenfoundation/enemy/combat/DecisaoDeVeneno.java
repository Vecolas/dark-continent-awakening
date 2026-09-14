package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * O que uma ferroada fez com o veneno do alvo -- resultado EXPLICITO, nunca boolean.
 *
 * <p>Um {@code boolean envenenou} responderia "entrou?" e colapsaria tres
 * situacoes que pedem coisas diferentes de quem chama: uma renova a duracao e
 * sobe o nivel, outra acerta um alvo que ja esta no teto e nao sobe nada, e a
 * terceira acerta alguem que nao pode ser envenenado. As duas ultimas sao
 * RECUSAS, e "recusa sempre tem motivo" -- sem o motivo, o jogador leva uma
 * ferroada telegrafada por 28 ticks, ve o dano direto reduzido e nada mais
 * acontece. Esse e o pior relato de bug que existe: "as vezes o ferrao nao faz
 * nada".</p>
 *
 * <p>O jogador nao le este enum. O que chega a ele e a consequencia -- o icone do
 * efeito subindo de nivel, ou a mensagem na barra de acao dizendo por que nao
 * subiu.</p>
 */
public enum DecisaoDeVeneno {
    /** Entrou: a duracao cresceu, e com ela o nivel do efeito. */
    APLICOU,
    /**
     * O alvo ja estava no teto de duracao: a ferroada renova e nao acumula.
     *
     * <p>O teto e o que impede uma sequencia de ferroadas -- de uma formiga so ou
     * de um esquadrao inteiro -- de virar uma execucao por dano continuo que o
     * jogador nao consegue rastrear ate a origem. Sem ele, o veneno seria o unico
     * ataque do jogo capaz de matar sem nunca aparecer na tela.</p>
     */
    NO_TETO,
    /**
     * O alvo nao pode ser envenenado -- morto-vivo, leite, resistencia.
     *
     * <p>Distinguir isto de {@link #NO_TETO} importa porque as duas recusas pedem
     * mensagens opostas: uma diz "espere o veneno baixar", a outra diz "este alvo
     * nao sofre veneno". Trocadas, elas ensinam o jogador a fazer a coisa errada.</p>
     */
    ALVO_IMUNE
}
