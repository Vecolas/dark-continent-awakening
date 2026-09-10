/**
 * Os records de payload e seus StreamCodecs.
 *
 * <p>Os ids e as direcoes ja estao CONGELADOS em
 * {@code com.darkcontinent.nenfoundation.network.NenProtocol}, e um portao cruza
 * aquela tabela com {@code docs/multiplayer/protocol.md}.
 *
 * <p>DECISAO: nada de serializar objeto Java arbitrario. Cada payload e um record
 * de campos minimos com StreamCodec escrito a mao. [NF-4]
 *
 * <p>Nasce no M1. Owner: Dev A (envio S2C), Dev B (leitura no cliente).
 */
package com.darkcontinent.nenfoundation.network.payload;
