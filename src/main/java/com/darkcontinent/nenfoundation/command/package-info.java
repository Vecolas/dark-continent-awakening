/**
 * Comandos de debug e administracao, todos permissionados.
 *
 * <p>UMA arvore, um arquivo: {@code NenCommands}. Brigadier funde nos de mesmo
 * nome quando dois lugares registram {@code /nen}, e a fusao FUNCIONA -- e por
 * isso ela e perigosa: a permissao do outro registro pode ser diferente, e nada
 * acusa. Com um registro so, a permissao da raiz cobre tudo por construcao, e o
 * portao consegue afirmar isso varrendo a arvore de verdade.
 *
 * <p>DECISAO: comando de debug disponivel ao jogador comum e um exploit com
 * sintaxe amigavel. A raiz exige nivel de operador, e um portao reprova se
 * alguem tirar o {@code requires} ou pendurar comando fora dela.
 *
 * <p>DECISAO: o que APAGA exige a palavra {@code confirmar} e imprime o dump do
 * que vai destruir ANTES de destruir. Nao ha desfazer; o texto na tela e a
 * unica recuperacao possivel.
 *
 * <p>DECISAO: leitura nao avisa ninguem, mutacao avisa os outros operadores. Um
 * operador alterando o perfil de outro jogador em silencio e a origem de "meu
 * progresso sumiu" sem rastro.
 *
 * <p>Nada aqui escreve attachment direto: tudo passa pelo servico, que migra na
 * leitura e valida na escrita.
 *
 * <p>Owner: Dev B.
 */
package com.darkcontinent.nenfoundation.command;
