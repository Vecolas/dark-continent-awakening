package com.darkcontinent.nenfoundation.network;

/**
 * De quem para quem um payload pode viajar.
 *
 * <p>A direcao e parte do CONTRATO, nao um detalhe de registro. Um payload
 * declarado C2S que o servidor tambem envia e o buraco por onde um cliente
 * passa a mandar o proprio estado — e essa classe de falha nao aparece em
 * nenhum teste de gameplay, so em quem esta trapaceando.
 */
public enum Direcao {

    /** Cliente para servidor. Carrega INTENCAO, nunca resultado. */
    C2S,

    /** Servidor para cliente. Carrega ESTADO ja decidido pelo servidor. */
    S2C
}
