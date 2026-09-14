/**
 * O nucleo de Greed Island: derrota, captura e conversao em card.
 *
 * <p>Carrega a decisao da issue #142 e a regra que a acompanha: <b>informacao
 * escondida nao viaja ate o cliente para ele filtrar.</b> Um efeito que so um
 * observador autorizado deveria ver e decidido no servidor, e o pacote nem sai
 * -- mandar e esconder e o mesmo que nao esconder, so que com um passo a mais
 * para quem estiver lendo a rede.</p>
 *
 * <p>A conversao em card passa OBRIGATORIAMENTE pelo ledger de recompensa da
 * fundacao de encontro. Um segundo caminho de pagamento aqui seria a duplicata
 * que este pacote existe para impedir.</p>
 */
package com.darkcontinent.nenfoundation.enemy.greedisland;
