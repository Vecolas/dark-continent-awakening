/**
 * Os filamentos da aura: ancoras, curva e a tira de quads.
 *
 * <p><b>O que faz:</b> desenha os fios de energia que contornam bracos, tronco e
 * pernas. Eles sao MALHA, e nao particula -- e essa e a diferenca que o
 * documento de direcao chama de "o componente que mais aproxima o resultado do
 * visual do anime".
 *
 * <p><b>Que decisao carrega:</b> o
 * <a href="../../../../../../../../docs/adr/ADR-015-aura-e-geometria-e-shader.md">ADR-015</a>,
 * item 4. Cada filamento nasce num {@code AuraAnchor} preso a um
 * {@code ModelPart}, e por isso herda a transformacao do membro: ele acompanha
 * corrida, ataque e agachamento em vez de ficar para tras como fumaca.
 *
 * <p><b>Nada e sorteado por quadro.</b> A curva e deterministica a partir de
 * {@code hash(UUID + ancora + indice + ciclo)}. Sorteio a sessenta hertz nao e
 * organico: e ruido branco, e ruido branco e a assinatura do raio eletrico que
 * a direcao de arte reprova.
 *
 * <p><b>Em que marco nasce:</b> AV2.
 *
 * <p><b>De quem e:</b> lane de superficie (Dev B).
 *
 * <p>Client-only. Nao decide regra nenhuma: le um estado que o servidor ja
 * autorizou e desenha.
 */
package com.darkcontinent.nenfoundation.client.vfx.ribbon;
