/**
 * O grafo de estradas de Greed Island e o roteador que o desenha. Fase G5.
 *
 * <p>DECISAO: o grafo declara QUAIS cidades se ligam; o TRACADO sai de um A*
 * sobre o terreno. Congelar o tracado seria desenhar a estrada por cima do
 * mapa, e o nao-negociavel 7 do documento e "Roads respect terrain".
 *
 * <p>Sem Minecraft, como o resto do layout: o portao roteia as nove estradas a
 * cada build para conferir que nenhuma atravessa serra fora de um passo.
 *
 * <p>Owner: conjunto.
 */
package com.darkcontinent.nenfoundation.enemy.greedisland.road;
