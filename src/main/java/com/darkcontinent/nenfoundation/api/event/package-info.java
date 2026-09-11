/**
 * Eventos que o nucleo emite e que quests, addons e conteudo escutam.
 *
 * <p>DECISAO: esta e a unica forma de o mundo externo saber o que aconteceu no
 * Nen. Nada de fora navega pela arvore de objetos do nucleo para descobrir estado
 * -- quem faz algo anuncia, quem se importa escuta.
 *
 * <p>Nome de evento no passado: {@code NenDespertadoEvent},
 * {@code CategoriaReveladaEvent}, {@code TecnicaEncerradaEvent}.
 *
 * <p>DOIS EVENTOS POR FATO, quando alguem pode impedir: um cancelavel antes
 * ({@code ...andoEvent}) e um informativo depois ({@code ...adoEvent}). Quem
 * precisa IMPEDIR e um caso diferente de quem precisa SABER, e um evento so
 * com booleano faz o primeiro listener registrado decidir por todos.
 *
 * <p>Os eventos deste pacote rodam no barramento de jogo
 * ({@code NeoForge.EVENT_BUS}) e sempre no servidor. Os tipos usam
 * {@code ServerPlayer} de proposito: nao deve existir a duvida de se aquele
 * jogador veio do cliente.
 *
 * <p>Nasce no M3, junto do despertar. Owner: Dev A.
 */
package com.darkcontinent.nenfoundation.api.event;
