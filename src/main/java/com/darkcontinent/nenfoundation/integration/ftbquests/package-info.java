/**
 * Ponte para o FTB Quests: a quest chama a API do nucleo.
 *
 * <p>DECISAO: a quest nunca escreve NBT do Nen diretamente. Ela pede ao nucleo, e o
 * nucleo decide. Quest escrevendo NBT e a segunda fonte da mesma verdade -- e a que
 * vale costuma ser a errada. Ver ADR-003.
 *
 * <p>Nasce no M6. Owner: Dev B.
 */
package com.darkcontinent.nenfoundation.integration.ftbquests;
