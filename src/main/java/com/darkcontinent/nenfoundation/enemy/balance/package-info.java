/**
 * Balanceamento de inimigos por PAPEL, e a telemetria que o mede.
 *
 * <p>Carrega a decisao da issue #150 e a regra que a acompanha: <b>telemetria e
 * LOCAL e opt-in.</b> Nada aqui abre conexao, escreve em servidor remoto ou liga
 * sozinho. O que ele faz e responder, sem servidor de pe, quanto tempo um
 * loadout de referencia leva para derrubar cada um dos vinte e quatro bichos --
 * e reprovar quem sai da faixa do proprio papel.</p>
 *
 * <p>A conta existe porque o erro que ela pega e caro e mudo: um ELITE que morre
 * em dois segundos e um LOW que leva um minuto nao produzem erro nenhum. Eles
 * produzem um bestiario em que o rotulo de ameaca mente, e o jogador para de
 * confiar nele -- que e pior do que nao ter rotulo.</p>
 */
package com.darkcontinent.nenfoundation.enemy.balance;
