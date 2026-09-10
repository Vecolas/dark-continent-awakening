/**
 * Barra de aura, tecnica ativa e cooldowns.
 *
 * <p>DECISAO: o HUD INTERPOLA entre snapshots; ele nao pede um pacote por quadro. E
 * o HUD nunca calcula regra -- ele desenha o que o servidor mandou. Um HUD que
 * recalcula custo localmente acerta em quase todos os casos e mente exatamente no
 * caso em que o jogador precisava saber.
 *
 * <p>Nasce no M2. Owner: Dev B.
 */
package com.darkcontinent.nenfoundation.client.hud;
