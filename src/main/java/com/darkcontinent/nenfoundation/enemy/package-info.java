/**
 * Entidades autorais e as regras de ameaca delas.
 *
 * <p>ALVO, ESTADO E DANO SAO DECIDIDOS NO SERVIDOR. A renderizacao fica em
 * {@code client.render} e nunca e alcancada daqui -- o portao
 * {@code PacotesDeclaradosTest} reprova o contrario.
 *
 * <p>UM LUGAR POR PAPEL, e o mob novo nao precisa escolher:</p>
 *
 * <ul>
 *   <li>{@code entity}: as entidades registradas, todas elas;
 *   <li>{@code registry}: o UNICO {@code DeferredRegister} de entidade, mais os
 *       atributos e os {@code SpawnPlacement} -- guardado pelo portao
 *       {@code FilaUnicaDeInimigosTest};
 *   <li>{@code ai}, {@code combat}, {@code encounter}: as regras deterministicas,
 *       que moram separadas da entidade para serem testadas sem o jogo de pe
 *       ({@code FoxbearTerritory}, {@code AmbushRules}, {@code RegrasDeJulgamento});
 *   <li>{@code content}: os perfis de balanceamento, que nao registram nada.
 * </ul>
 *
 * <p>Ate a issue #266 o foxbear morava fora dessa divisao -- entidade na raiz e
 * registro num pacote {@code registry} proprio, paralelo a este. Nao dava erro:
 * dava um mob que ficava de fora de tudo que a fila ganhava depois.
 *
 * <p>Nasce no conjunto de inimigos (#107). Owner: Dev A.
 */
package com.darkcontinent.nenfoundation.enemy;
