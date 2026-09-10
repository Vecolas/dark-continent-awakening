/**
 * Servicos server-side: scheduler de tick, ciclo de vida de jogador, runtime de
 * Nen.
 *
 * <p>DECISAO: existe UM scheduler central de tick de Nen, e nao um laco por
 * subsistema. Cada subsistema que quer tick se registra nele. Varios lacos
 * independentes sobre a lista de jogadores e o caminho conhecido para o profiler
 * apontar Nen como gargalo sem dizer qual parte.
 *
 * <p>DECISAO: nenhum jogador removido pode continuar em colecao global. Trabalho
 * iniciado morre com quem o iniciou.
 *
 * <p>Nasce no M1. Owner: Dev A.
 */
package com.darkcontinent.nenfoundation.server;
