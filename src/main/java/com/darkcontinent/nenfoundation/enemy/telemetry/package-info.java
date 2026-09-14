/**
 * Telemetria LOCAL e opt-in de combate contra inimigos (issue #150).
 *
 * <p>Tres regras, e as tres sao da issue:</p>
 *
 * <ol>
 *   <li><b>LOCAL.</b> Nada aqui abre conexao, resolve nome de host ou escreve
 *       fora do diretorio do mundo. Nao ha cliente HTTP, nao ha socket, nao ha
 *       fila de envio -- e nao ha um "so um ping para saber quantos usam".</li>
 *   <li><b>OPT-IN.</b> O padrao e DESLIGADO, e a chave de config diz isso com o
 *       motivo ao lado. Um padrao ligado transformaria "medir o proprio jogo" em
 *       "coletar sem perguntar", e a diferenca nao aparece em lugar nenhum do
 *       jogo -- so na conta de quem confia.</li>
 *   <li><b>SEM IDENTIDADE.</b> A amostra guarda o que aconteceu, e nao quem
 *       estava jogando: nome, uuid e posicao ficam de fora. Um numero de
 *       jogadores basta para separar solo de grupo, e e o unico dado de pessoa
 *       que entra.</li>
 * </ol>
 *
 * <p>O que ele mede complementa {@code enemy/balance}: aquele PREVE pela conta,
 * este REGISTRA o que aconteceu. Os dois juntos respondem a pergunta que nenhum
 * responde sozinho -- o combate real bate com o previsto?</p>
 */
package com.darkcontinent.nenfoundation.enemy.telemetry;
