/**
 * Registros comuns pequenos que precisam existir nos dois lados.
 *
 * <p>O tipo e os parametros da faisca vivem aqui porque o registro do mod e
 * comum. A implementacao visual e o provider continuam em {@code client/*},
 * registrados somente por {@code NenFoundationClient}; assim o servidor
 * dedicado nunca carrega classes de render.
 *
 * <p>Nasce no AV3. Owner: Dev B.
 */
package com.darkcontinent.nenfoundation.registry;
