/**
 * O shader proprio da aura, e so ele.
 *
 * <p><b>O que faz:</b> registra e configura o programa GLSL que da a borda de
 * Fresnel, o ruido de filamentos e o fluxo vertical -- os tres elementos que
 * separam uma aura de uma camada translucida qualquer.
 *
 * <p><b>Que decisao carrega:</b> o
 * <a href="../../../../../../../../docs/adr/ADR-015-aura-e-geometria-e-shader.md">ADR-015</a>.
 * E, dentro dele, a regra de conter o risco: a linha de {@code RenderType} e de
 * shader mudou no 1.21 e vai mudar de novo, entao TUDO que conhece
 * {@code ShaderInstance} mora neste pacote. Quanto menor ele for, menor a
 * reescrita na proxima versao do Minecraft.
 *
 * <p><b>Falha aqui degrada, nao derruba.</b> Shader que nao compila devolve
 * {@code pronto() == false} e a shell cai para o material simples do AV0 --
 * a mesma regra que o ADR-016 impoe ao bloom.
 *
 * <p><b>Em que marco nasce:</b> AV1.
 *
 * <p><b>De quem e:</b> lane de superficie (Dev B).
 */
package com.darkcontinent.nenfoundation.client.vfx.shader;
