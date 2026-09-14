package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * EM QUE PONTO da janela de captura o Bubble Horse esta -- nunca um boolean.
 *
 * <p>"Recusa sempre tem motivo." Um {@code boolean exausto} responderia "esta
 * parado?" e colapsaria seis situacoes que pedem coisas diferentes do servidor:
 * abrir a janela, manter a janela, fechar a janela, recuperar o folego, continuar
 * fugindo e morrer sem card. As tres primeiras se parecem demais entre si para
 * serem decididas em {@code if} espalhado por uma entidade.</p>
 *
 * <p>O jogador nao le este enum; ele le o corpo do bicho. O que chega a ele sao
 * as consequencias -- o cavalo que para, as bolhas que murcham, a mensagem que
 * diz que o card se perdeu.</p>
 */
public enum DecisaoDeExaustaoDeBolha {
    /**
     * Ele cruzou o limiar AGORA: para de fugir e a janela abre.
     *
     * <p>E o unico momento em que o servidor deve publicar o estado de exaustao e
     * avisar quem estava batendo.</p>
     */
    EXAURE,
    /** A janela esta aberta e ainda tem prazo. E aqui que a captura acontece. */
    JANELA_ABERTA,
    /**
     * O prazo acabou: ele recupera o folego e volta a fugir.
     *
     * <p>A janela TEM de fechar. Aberta para sempre, ela transforma a captura num
     * bicho parado que o jogador pega quando quiser, e a licao -- parar de bater
     * na hora certa -- deixa de existir. Nada disso daria erro.</p>
     */
    JANELA_FECHOU,
    /**
     * Ja exauriu ha pouco e ainda esta sem folego para exaurir de novo.
     *
     * <p>Sem esta carencia o bicho reexauriria no tick seguinte ao fechamento --
     * a vida continua abaixo do limiar -- e a janela nunca fecharia de verdade.
     * O sintoma nao e um erro: e um cavalo permanentemente parado.</p>
     */
    RECUPERANDO_O_FOLEGO,
    /** Vida acima do limiar: ele continua fugindo, que e o estado normal dele. */
    ACIMA_DO_LIMIAR,
    /**
     * Morreu -- e a captura morreu junto.
     *
     * <p>A condicao de card deste bicho e nao-letal. Este e o unico desfecho que
     * o jogador precisa VER acontecer, porque ele e a consequencia de ter
     * continuado batendo.</p>
     */
    MORREU_SEM_CARD
}
