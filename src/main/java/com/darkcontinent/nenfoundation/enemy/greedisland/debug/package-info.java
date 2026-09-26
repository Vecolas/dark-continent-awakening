/**
 * Ferramentas de diagnostico da macrogeografia de Greed Island.
 *
 * <p>DECISAO: nada aqui roda no tick do jogo. O exportador de mapa usa
 * {@code java.awt} porque ele existe para ferramenta e para PORTAO -- o gate
 * macro do documento ("isso parece uma ilha enorme?") precisa julgar a
 * silhueta sem gerar chunk, e a 80.000 blocos de extensao gerar seria
 * impossivel.
 *
 * <p>Nasce na fase G1 da trilha GI-MACRO. Owner: conjunto.
 */
package com.darkcontinent.nenfoundation.enemy.greedisland.debug;
