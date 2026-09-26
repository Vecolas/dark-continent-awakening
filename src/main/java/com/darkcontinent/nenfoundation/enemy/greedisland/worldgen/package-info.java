/**
 * A ponte entre o layout macro de Greed Island e a geracao de blocos.
 *
 * <p>DECISAO: o mod NAO escreve um ChunkGenerator proprio. Ele ensina o
 * {@code minecraft:noise} onde a ilha fica, por funcao de densidade, e herda
 * aquiferos, cavernas, veios, regras de superficie e povoamento ja testados.
 * Trocar o pipeline inteiro para acrescentar uma informacao seria caro por
 * pouco.
 *
 * <p>Owner: conjunto. Nasce na fase G6 da trilha GI-MACRO.
 */
package com.darkcontinent.nenfoundation.enemy.greedisland.worldgen;
