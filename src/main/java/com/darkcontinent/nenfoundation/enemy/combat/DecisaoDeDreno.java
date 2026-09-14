package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * POR QUE o dreno aconteceu -- ou nao -- nunca um {@code float} zero e calado.
 *
 * <p>"Recusa sempre tem motivo." Devolver so a cura responderia "quanto?" e
 * esconderia cinco historias completamente diferentes: o golpe era de outro, a
 * vitima nao tinha o que dar, o golpe nao encostou de verdade, o teto do encontro
 * ja estourou, ou ela ja estava cheia. O relato de bug que vem de um zero e o
 * pior que existe -- <em>"as vezes ela cura e as vezes nao"</em> -- e ninguem
 * descobre sozinho que a diferenca era o escudo do jogador ter comido o dano.</p>
 *
 * <p>Este enum nao e exibido ao jogador: a recusa dele e silenciosa em jogo de
 * proposito, porque um oficial que anuncia "nao drenei" entregaria de graca a
 * informacao de que bloquear o desarma. Quem le o motivo e quem depura e quem
 * testa.</p>
 */
public enum DecisaoDeDreno {

    /** Golpe proprio, vitima valida, dano real e teto com folga: ela SUGA agora. */
    DRENA,

    /**
     * O golpe nao foi dela.
     *
     * <p>Sem esta porta, qualquer dano que chegasse a vitima por perto -- fogo,
     * a espada de outro jogador, o golpe de outra formiga -- alimentaria este
     * oficial. O sintoma nao seria erro: seria um mob que cura em combates de
     * que nem participou, e que fica mais forte quanto mais gente luta.</p>
     */
    GOLPE_DE_OUTRO,

    /**
     * A vitima nao tem o que ser drenado.
     *
     * <p>E o que impede a colonia de virar bateria: drenar outra formiga faria um
     * esquadrao encurralado se curar em circulo, e o encontro deixaria de ter
     * fim sem uma linha de log.</p>
     */
    VITIMA_NAO_DRENAVEL,

    /**
     * A picada encostou e nao tirou nada.
     *
     * <p>Invulnerabilidade, absorcao, resistencia, modo criativo. O acerto existe
     * do lado do ataque e NAO existe do lado da vida, e curar aqui seria pagar
     * por um dano que nunca aconteceu -- exatamente o acerto que nao deveria
     * curar.</p>
     */
    SEM_DANO,

    /**
     * O teto do encontro ja foi gasto.
     *
     * <p>E a unica coisa que separa "tensa" de "impossivel". Sem teto, um combate
     * longo leva este oficial a vida praticamente infinita: cada golpe que ele
     * acerta devolve vida, e um encontro que dure o bastante nunca termina. Isso
     * nao levanta excecao nenhuma -- e um mob com 55 de vida que nao morre.</p>
     */
    TETO_ATINGIDO,

    /** Ela ja esta com a vida cheia; curar acima do maximo nao existe. */
    JA_ESTA_CHEIA,

    /**
     * A cura calculada seria pequena demais para valer.
     *
     * <p>Sem este piso, um respingo de 0.4 de dano viraria 0.2 de cura, gastaria
     * teto e nao mudaria um pixel da barra. O teto acabaria sem que nada visivel
     * tivesse acontecido, e a mecanica do bicho morreria de graca.</p>
     */
    CURA_PEQUENA_DEMAIS
}
