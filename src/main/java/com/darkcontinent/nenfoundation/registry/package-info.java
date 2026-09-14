/**
 * Registros comuns do mod, isolados do conteudo de combate e da camada cliente.
 *
 * <p>Registros pequenos que precisam existir nos DOIS lados moram aqui. O tipo e
 * os parametros da faisca, por exemplo: o registro do mod e comum, mas a
 * implementacao visual e o provider continuam em {@code client/*}, registrados
 * somente por {@code NenFoundationClient} -- assim o servidor dedicado nunca
 * carrega classe de render.
 *
 * <p>Itens, blocos e menus do bestiario seguem a mesma regra: o registro e
 * comum, a tela e cliente.
 *
 * <p>Nasce no AV3. Owner: Dev B.
 */
package com.darkcontinent.nenfoundation.registry;
