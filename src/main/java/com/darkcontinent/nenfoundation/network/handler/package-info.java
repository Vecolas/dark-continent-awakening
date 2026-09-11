/**
 * Handlers de payload.
 *
 * <p>DECISAO: todo handler C2S valida antes de agir -- existencia, unlock, aura,
 * cooldown, estado, distancia, dimensao e linha de visao quando cabe -- e todo
 * handler C2S tem rate limit por jogador. Handler sem rate limit e um cliente
 * modificado derrubando o TPS do servidor sem nenhum erro no log. A entrada
 * concreta fica em {@link NenC2SHandlers}; os executores de habilidade chegam
 * quando o marco M5 criar seus consumidores.
 *
 * <p>Nasce no M1. Owner: Dev A.
 */
package com.darkcontinent.nenfoundation.network.handler;
