/**
 * A bancada de trabalho do visual da aura: comandos de dev, overlay de tuning,
 * sliders e o modo de captura.
 *
 * <p><b>O que ele faz.</b> Permite ajustar a aura sem recompilar e produzir
 * capturas comparaveis entre si. Os gates AV0 a AV8 sao aprovados por imagem
 * arquivada, comparada lado a lado com a referencia -- e duas imagens so se
 * comparam se hora, clima, HUD, camera e versao de assets forem as mesmas nas
 * duas. E isso que mora aqui.
 *
 * <p><b>Qual decisao ele carrega.</b> Tudo e client-only e nada e autoritativo.
 * Os comandos nascem em {@code RegisterClientCommandsEvent}, e nao no evento de
 * servidor: nenhuma linha deste pacote muda tecnica, aura, custo, cooldown ou
 * visibilidade (ADR-001). O que se mexe aqui e no que ESTE cliente desenha.
 *
 * <p>A segunda decisao e sobre onde o ESTADO mora: ele nao mora aqui. Quem
 * guarda as sobreposicoes e {@code client.vfx.SobreposicaoDeVfx}, e quem guarda
 * os contadores e {@code client.vfx.MedidorDeVfx}. Se o estado morasse neste
 * pacote, o renderer teria de importar a interface de depuracao para desenhar
 * -- e a seta de dependencia ficaria apontando para o lado errado.
 *
 * <p>A terceira: <b>nenhum ajuste daqui persiste</b>. Ele morre no logout, e o
 * overlay grita enquanto houver um. Sem isso, alguem aprova uma captura com um
 * numero que nao esta em perfil nenhum, e o jogo que os jogadores veem nunca
 * foi aquele.
 *
 * <p><b>Em que marco nasce.</b> AV0, issue #168, junto do gate #169.
 *
 * <p><b>De quem e.</b> Dev B (superficie), com o estado lido do contrato de
 * Dev A.
 */
package com.darkcontinent.nenfoundation.client.vfx.debug;
