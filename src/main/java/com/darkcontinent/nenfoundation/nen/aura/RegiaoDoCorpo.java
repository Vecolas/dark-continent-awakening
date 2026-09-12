package com.darkcontinent.nenfoundation.nen.aura;

/**
 * As regioes em que a aura se distribui.
 *
 * <p>SEIS, E NEM MAIS NEM MENOS. O numero sai do documento-fonte
 * ({@code docs/pesquisa/tecnicas-canonicas.md}) e cada corte tem motivo:
 * juntar os bracos num so impediria Ko num punho, que e o exemplo canonico da
 * tecnica; e nenhuma tecnica do material distingue nada mais fino que isto.
 *
 * <p>A ORDEM DOS VALORES E CONTRATO. Ela vira a ordem dos campos na rede, e
 * reordenar aqui trocaria braco por perna em todo cliente ja conectado -- sem
 * erro nenhum, so com a aura aparecendo no lugar errado.
 */
public enum RegiaoDoCorpo {

    CABECA,
    TRONCO,
    BRACO_ESQUERDO,
    BRACO_DIREITO,
    PERNA_ESQUERDA,
    PERNA_DIREITA;

    /** Quanto cada regiao recebe quando a aura esta distribuida por igual. */
    public static float fracaoUniforme() {
        return 1.0F / values().length;
    }
}
