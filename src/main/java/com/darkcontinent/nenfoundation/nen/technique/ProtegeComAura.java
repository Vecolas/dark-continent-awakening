package com.darkcontinent.nenfoundation.nen.technique;

/**
 * Uma tecnica que segura dano enquanto estiver ativa.
 *
 * <p>ELA FECHA UMA DIVIDA DECLARADA EM TRES LUGARES. Ten prometia "defesa
 * basica de Nen", Zetsu prometia ficar "muito vulneravel", e Ken e a
 * "principal defesa geral" -- e nenhuma das tres fazia nada, porque nao havia
 * dano de Nen. A issue #127 existia so para registrar isso.
 *
 * <p>ZERO NAO E AUSENCIA. Uma tecnica que implementa esta interface e devolve
 * zero esta dizendo "eu apago a protecao", que e exatamente o que Zetsu faz --
 * e e diferente de nao implementar, que e "eu nao mexo nisso".
 *
 * <p>Interface a parte pelo mesmo motivo de {@link ModificaTetoDeOutput}:
 * {@code NenTechnique} e contrato congelado (ADR-004), e a maioria das
 * tecnicas nao protege de nada.
 */
public interface ProtegeComAura {

    /**
     * Quanto esta tecnica protege, de 0 para cima.
     *
     * <p>O valor e multiplicado pela aura da regiao atingida, entao ele e a
     * protecao com a aura ESPALHADA por igual. Concentrar noutro lugar reduz o
     * que sobra para a faixa do golpe -- e e assim que Ko fica perigoso.
     */
    double protecaoBase();
}
