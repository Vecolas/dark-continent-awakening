/**
 * Entidades autorais e as regras de ameaca delas.
 *
 * <p>ALVO, ESTADO E DANO SAO DECIDIDOS NO SERVIDOR. A renderizacao fica em
 * {@code client.render} e nunca e alcancada daqui -- o portao
 * {@code PacotesDeclaradosTest} reprova o contrario.
 *
 * <p>As regras deterministicas moram separadas da entidade ({@code
 * FoxbearTerritory}) para poderem ser testadas sem o jogo de pe.
 *
 * <p>Nasce no conjunto de inimigos (#107). Owner: Dev A.
 */
package com.darkcontinent.nenfoundation.enemy;
