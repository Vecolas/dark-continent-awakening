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
 * <p>Nasce no M3, junto do despertar. Owner: Dev A.
 */
package com.darkcontinent.nenfoundation.api.event;
