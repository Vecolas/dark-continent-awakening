/**
 * Nen de Chimera Ant: QUANDO usar, e nunca O QUE Nen e.
 *
 * <p>Carrega a fronteira mais importante da issue #145, e ela vem do CLAUDE.md:
 * <b>o Nen Foundation e a unica autoridade sobre Nen.</b> Este pacote decide
 * taticamente qual tecnica uma formiga deveria estar usando; ele nao calcula
 * aura, nao inventa custo e nao cria uma segunda barra. Duas autoridades sobre a
 * mesma mecanica divergem, e a divergencia nao aparece como erro -- aparece como
 * desbalanceamento que ninguem consegue explicar.</p>
 *
 * <p>Por isso nada aqui referencia {@code AuraPool} ou {@code NenTechnique}
 * diretamente: o controlador recebe FATOS ja medidos (tem aura? a tecnica esta
 * disponivel?) e devolve uma INTENCAO. Quem aplica e o lado que ja e autoridade.
 * A ligacao real com as tecnicas e trabalho de integracao, e ela nao pode nascer
 * aqui por conveniencia.</p>
 *
 * <p>O ADR-009 vale por cima de tudo: qualquer mudanca no modelo de aura exige
 * decisao conjunta registrada. Nenhum arquivo deste pacote muda esse modelo.</p>
 */
package com.darkcontinent.nenfoundation.enemy.chimera.nen;
