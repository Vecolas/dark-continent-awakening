/**
 * Definicoes carregadas de datapack: afinidades, specs de habilidade, curvas de
 * progressao.
 *
 * <p>DECISAO: recarga de datapack precisa invalidar quem esta usando a definicao
 * antiga. Uma habilidade ativa cujo spec foi recarregado encerra com
 * {@code StopReason.DEFINITION_RELOADED} em vez de continuar rodando com numeros
 * que nao existem mais.
 *
 * <p>Nasce no M3 (matriz de afinidade). Owner: Dev A define o schema, Dev B
 * escreve os dados.
 */
package com.darkcontinent.nenfoundation.data.definition;
