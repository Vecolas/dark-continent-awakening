/**
 * O Nen Foundation: a fundacao autoral de Nen do Dark Continent Awakening.
 *
 * <p>DECISAO CENTRAL DO PROJETO: este mod e a UNICA autoridade sobre Nen. Mods
 * externos complementam quests, loot, spawn, documentacao, animacao, interface e
 * performance -- nenhum deles controla aura, categoria, tecnica, cooldown ou dano
 * de Nen. Duas autoridades sobre a mesma mecanica divergem, e a divergencia
 * aparece como desbalanceamento inexplicavel, nao como erro.
 *
 * <p>REGRA DE DEPENDENCIA entre as arvores deste pacote:
 *
 * <pre>
 *   api        &lt;- ninguem de fora do mod precisa de mais nada
 *   nen        -&gt; api            (o dominio implementa a API)
 *   network    -&gt; api, nen
 *   server     -&gt; api, nen, network
 *   client     -&gt; api, network   (NUNCA o contrario)
 *   integration-&gt; api            (o nucleo NUNCA depende daqui)
 *   datagen    -&gt; tudo, mas nada depende dele
 * </pre>
 *
 * <p>Ver docs/adr/ para as decisoes e docs/processo/ownership.md para quem cuida
 * de que area.
 */
package com.darkcontinent.nenfoundation;
