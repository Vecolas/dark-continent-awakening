/**
 * Leitura somente-leitura do estado de Nen, para quests e integracoes.
 *
 * <p>DECISAO: quem consulta nunca recebe o objeto mutavel. Uma quest que recebesse
 * o {@code PersistentNenData} vivo poderia altera-lo sem passar pelo servico -- e
 * sem passar pela sincronizacao, deixando cliente e servidor discordando sem erro
 * nenhum.
 *
 * <p>Nasce no M1. Owner: Dev A.
 */
package com.darkcontinent.nenfoundation.api.query;
