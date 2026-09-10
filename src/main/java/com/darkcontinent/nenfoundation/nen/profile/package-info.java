/**
 * O perfil do jogador: o que persiste e o que nao persiste.
 *
 * <p>{@code PersistentNenData} sobrevive a logout, restart e morte.
 * {@code RuntimeNenState} (M1) nao sobrevive a nada e nunca e serializado.
 * Confundir os dois e a origem tanto de disco escrito a cada tick quanto de
 * progresso perdido em crash. Ver ADR-002.
 */
package com.darkcontinent.nenfoundation.nen.profile;
