/**
 * Pontes opcionais para mods externos.
 *
 * <p>REGRA DE DEPENDENCIA, e ela vale nos dois sentidos: esta arvore pode depender
 * das APIs externas; o NUCLEO nao pode depender desta arvore. O JAR tem de iniciar
 * em cliente e em servidor dedicado sem FTB Quests, sem KubeJS, sem Jade e sem
 * Epic Fight instalados. Ver ADR-003.
 *
 * <p>Toda classe aqui e carregada sob verificacao de mod presente. Import direto de
 * classe de mod ausente estoura no carregamento, nao no uso.
 */
package com.darkcontinent.nenfoundation.integration;
