package com.darkcontinent.nenfoundation.nen.technique;

/**
 * Uma tecnica que levanta o teto de Output enquanto estiver ativa.
 *
 * <p>ELA FECHA UMA DIVIDA DECLARADA. O PR #80 entregou o Output como
 * selecionado / maximo / efetivo e disse, em voz alta, que <b>nada abaixava o
 * maximo</b>: o {@code min} existia, era testado, e nunca mordia em producao.
 * Ren e o primeiro codigo a mexer naquele teto, e por isso o ponto cego morre
 * aqui.
 *
 * <p>O TETO E O MAIOR ENTRE OS DECLARADOS, e nao o produto nem a soma. Duas
 * tecnicas que levantam o teto nao se empilham: quem manda e a que levanta
 * mais. Somar produziria um teto acima de 100% com duas tecnicas modestas, e
 * multiplicar teria o mesmo problema com numeros menores ainda.
 *
 * <p>E interface a parte pelo mesmo motivo de {@link ModificaRegeneracao} e
 * {@link ConsomeAura}: {@code NenTechnique} e contrato congelado (ADR-004), e
 * a maioria das tecnicas nao mexe no teto.
 */
public interface ModificaTetoDeOutput {

    /**
     * O teto que esta tecnica permite, de {@code 0.0} a {@code 1.0}.
     *
     * <p>Abaixo do teto de repouso, ela nao abaixa nada: o efetivo continua
     * sendo o MAIOR entre repouso e o que as ativas permitem. Uma tecnica que
     * precise REDUZIR o teto -- Zetsu, por exemplo -- vai precisar de outra
     * regra, com nome proprio e decisao registrada.
     */
    float tetoDeOutput();
}
