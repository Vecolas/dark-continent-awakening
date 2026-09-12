/**
 * O pipeline de dano e defesa de Nen.
 *
 * <p>DECISAO: existe UMA ordem documentada de aplicacao de modificadores, e ela
 * mora num lugar so. Multiplicador aplicado em dois event handlers diferentes
 * multiplica duas vezes, e o numero final e plausivel demais para alguem notar.
 *
 * <p>Vanilla primeiro. Epic Fight entra como adapter opcional no M7, se passar no
 * gate. Ver ADR-006.
 *
 * <p>NASCEU NA #213, e o handler unico e {@code server.NenDanoService}. A conta
 * mora em {@code DefesaDeNen} e a faixa atingida em {@code FaixaDoCorpo} --
 * as duas puras, porque a regra que decide quanto dano alguem toma e a que
 * mais precisa ser provavel sem subir o jogo.
 *
 * <p>Nasce no M4. Owner: Dev A.
 */
package com.darkcontinent.nenfoundation.nen.combat;
