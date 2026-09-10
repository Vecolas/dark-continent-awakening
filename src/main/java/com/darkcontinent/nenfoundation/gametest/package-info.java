/**
 * Gametests: o comportamento com o jogo de pe.
 *
 * <p>POR QUE ESTE PACOTE EXISTE. A suite JUnit roda sem o Minecraft carregado.
 * Ela cobre logica pura -- codec, migracao, invariantes, arvore de comandos --
 * e nao alcanca nada que dependa de attachment, ciclo de vida de jogador,
 * registro ou nivel. Toda entrega ate agora terminou declarando "nunca foi
 * executado contra um jogador de verdade"; e aqui que essa frase deixa de ser
 * necessaria.
 *
 * <p>Como rodar: {@code ./gradlew runGameTestServer}. **Nao confie no codigo de
 * saida do Gradle** -- ele imprime BUILD SUCCESSFUL mesmo quando o servidor
 * morre. Procure o resumo de gametests no log.
 *
 * <p>DECISAO: o template {@code nenfoundation:empty} e uma estrutura 3x3x3 sem
 * bloco nenhum, em {@code data/nenfoundation/structure/empty.nbt}. Ele e
 * BINARIO e gerado, e nao escrito a mao: a alternativa em SNBT vive em
 * {@code gameteststructures/} no diretorio de execucao, que e ignorado pelo git
 * e nao viaja no JAR. O procedimento de regerar esta no PR que criou este
 * pacote.
 *
 * <p>PONTO CEGO DECLARADO: {@code makeMockServerPlayerInLevel()} esta
 * {@code @Deprecated(forRemoval = true)} no 1.21.1. Quando sumir, estes testes
 * param de COMPILAR -- que e o modo de falha bom: barulhento, na hora de
 * atualizar.
 *
 * <p>Owner: quem estiver entregando o comportamento coberto.
 */
package com.darkcontinent.nenfoundation.gametest;
