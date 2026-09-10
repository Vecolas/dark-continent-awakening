/**
 * O Aura Engine: reserva, regeneracao, saida, custo, modificadores e exaustao.
 *
 * <p>DECISAO: o engine NAO conhece HUD, quest, personagem nem habilidade
 * especifica. O contrato dele e: perfil + estado + contexto para disponibilidade e
 * consumo. No dia em que aparecer um {@code if} com o nome de uma habilidade
 * dentro desta arvore, a arquitetura falhou -- e o conserto e extrair um
 * componente, nao acrescentar o {@code if}.
 *
 * <p>Nasce no M2. Owner: Dev A. Ver ADR-002.
 */
package com.darkcontinent.nenfoundation.nen.aura;
