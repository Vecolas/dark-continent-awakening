/**
 * Quem desenha a aura: layers, tipos de render e o registro deles.
 *
 * <p><b>O que faz:</b> pendura a shell no renderer do jogador e a desenha,
 * parte por parte, com a intensidade que cada regiao do corpo recebeu.
 *
 * <p><b>Que decisao carrega:</b> o
 * <a href="../../../../../../../../docs/adr/ADR-015-aura-e-geometria-e-shader.md">ADR-015</a>
 * -- a aura e geometria e shader. Em concreto: a layer e anexada por
 * {@code EntityRenderersEvent.AddLayers} aos DOIS modelos de jogador, porque
 * layer herda a pose; um renderer paralelo divergiria da animacao na primeira
 * que aparecesse.
 *
 * <p><b>Atencao a versao:</b> 1.21.1 e ANTERIOR ao {@code EntityRenderState},
 * que chegou em 1.21.2. Exemplo de renderer publicado depois disso nao compila
 * aqui, ou compila contra uma API que nao existe.
 *
 * <p><b>Em que marco nasce:</b> AV0, o tech spike da trilha do visual da aura.
 *
 * <p><b>De quem e:</b> lane de superficie (Dev B).
 *
 * <p>Este pacote e CLIENT-ONLY, e nada fora de {@code client} pode alcanca-lo.
 * O portao {@code PacotesDeclaradosTest} reprova quem inverter a seta. Ele
 * tambem nao decide regra nenhuma: le um estado que o servidor ja autorizou e
 * desenha.
 */
package com.darkcontinent.nenfoundation.client.vfx.render;
