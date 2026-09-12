package com.darkcontinent.nenfoundation.nen.divination;

/**
 * Por que a agua nao reagiu.
 *
 * <p>SAO DUAS MENSAGENS POR RECUSA, de proposito: a primeira diz a REGRA, a
 * segunda diz o que a AGUA fez. Quem so le a segunda ja entende que nao foi um
 * bug; quem le a primeira sabe o que fazer a respeito.
 *
 * <p>MORA AQUI, e nao junto do ritual, por dois motivos. O primeiro e que este
 * pacote ja e a casa do texto da adivinhacao -- as seis reacoes estao em
 * {@link ResultadoDaAdivinhacao}. O segundo e que o portao que confere as
 * traducoes vive na camada pura e nao carrega nada de servidor: com as recusas
 * aqui, ele DERIVA as chaves em vez de manter uma lista propria.
 *
 * <p>Essa lista existiu. Ela tinha {@code "nenfoundation.divinacao.sem_nen"}
 * escrito a mao, e na primeira recusa nova o portao reprovou chamando a chave
 * de orfa. Ele estava certo sobre o sintoma e errado sobre a causa -- o que
 * estava duplicado era a verdade, e nao a chave.
 */
public enum RecusaDaAdivinhacao {

    /** Nunca despertou o Nen. */
    NAO_DESPERTOU("nenfoundation.error.nao_desperto",
            "nenfoundation.divinacao.sem_nen"),

    /** Despertou, mas nao esta com Ren sobre a agua. */
    SEM_REN("nenfoundation.error.ren_necessario",
            "nenfoundation.divinacao.agua_parada");

    private final String regra;
    private final String agua;

    RecusaDaAdivinhacao(String regra, String agua) {
        this.regra = regra;
        this.agua = agua;
    }

    /** Chave da mensagem que explica a REGRA que barrou o ritual. */
    public String chaveDaRegra() {
        return this.regra;
    }

    /** Chave da mensagem que descreve o que a AGUA fez. */
    public String chaveDaAgua() {
        return this.agua;
    }
}
