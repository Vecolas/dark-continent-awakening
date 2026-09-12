package com.darkcontinent.nenfoundation.nen.technique;

/**
 * Uma tecnica que ABAIXA o teto de Output enquanto estiver ativa.
 *
 * <p>ELA E O PAR DE {@link ModificaTetoDeOutput}, e a separacao e deliberada:
 * quem LEVANTA e quem ABAIXA sao regras diferentes, e juntar as duas numa
 * interface so obrigaria cada tecnica a declarar um valor que ela nao usa.
 *
 * <p>Foi prevista quando {@code ModificaTetoDeOutput} nasceu, que dizia com
 * todas as letras: <i>"uma tecnica que precise REDUZIR o teto -- Zetsu, por
 * exemplo -- vai precisar de outra regra, com nome proprio"</i>. E esta.
 *
 * <p>A ORDEM IMPORTA, e ela esta no servico: primeiro os que levantam, depois
 * os que abaixam. Quem abaixa VENCE. Um Zetsu que nao vencesse Ren seria uma
 * supressao que nao suprime -- e como as duas se excluem hoje, o defeito
 * ficaria invisivel ate alguem criar a terceira tecnica que combina com ambas.
 */
public interface LimitaTetoDeOutput {

    /**
     * O teto MAXIMO que esta tecnica permite, de {@code 0.0} a {@code 1.0}.
     *
     * <p>Entre varios limitadores ativos, vale o MENOR: limitar e restringir, e
     * duas restricoes nao se cancelam.
     */
    float tetoMaximoPermitido();
}
