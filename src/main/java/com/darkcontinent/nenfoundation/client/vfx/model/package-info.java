/**
 * A geometria da aura: modelos inflados do jogador e as espessuras deles.
 *
 * <p><b>O que faz:</b> constroi uma segunda silhueta do jogador, ligeiramente
 * maior, para servir de superficie a aura. Nao desenha nada -- quem desenha e
 * {@code client.vfx.render}.
 *
 * <p><b>Que decisao carrega:</b> o
 * <a href="../../../../../../../../docs/adr/ADR-015-aura-e-geometria-e-shader.md">ADR-015</a>
 * -- a aura e geometria e shader, e particula e acabamento. E, dentro dele, a
 * regra que este pacote existe para cumprir: a shell e inflada por
 * {@code CubeDeformation} CUBO A CUBO, e nunca por {@code poseStack.scale}, que
 * gira em torno do origin de cada parte e descola a pelicula nas articulacoes.
 *
 * <p><b>Em que marco nasce:</b> AV0, o tech spike da trilha do visual da aura.
 *
 * <p><b>De quem e:</b> lane de superficie (Dev B).
 *
 * <p>Nada de gameplay entra aqui. Nem custo, nem cooldown, nem dano, nem
 * validacao: este pacote so sabe de forma.
 */
package com.darkcontinent.nenfoundation.client.vfx.model;
