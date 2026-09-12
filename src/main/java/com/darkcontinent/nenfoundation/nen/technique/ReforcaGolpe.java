package com.darkcontinent.nenfoundation.nen.technique;

/**
 * Uma tecnica que faz o golpe do usuario doer mais enquanto estiver ativa.
 *
 * <p>ELA FECHA O ULTIMO BURACO FUNCIONAL DO M4. A issue guarda-chuva pedia
 * "modificadores defensivos <b>e ofensivos</b>, com uma ordem documentada", e
 * so a metade defensiva existia: a aura segurava golpe desde #127 e nunca
 * desferiu um. O sintoma era silencioso -- Ren, que no cânone <i>aumenta o
 * poder de ataque</i>, so levantava o teto de Output e cobrava aura.
 *
 * <p>O BRACO PASSA A DEFENDER ALGUMA COISA, e isso resolve um ponto cego
 * declarado em {@link com.darkcontinent.nenfoundation.nen.combat.FaixaDoCorpo}:
 * os bracos nao entram em nenhuma faixa de defesa, porque nao ha como saber que
 * alguem bloqueou com o braco. Ate agora, aura no braco nao fazia nada em lugar
 * nenhum -- concentrar la era puro prejuizo. Com esta interface, o braco vira a
 * regiao OFENSIVA, que e o que Shu e Ko sempre prometeram.
 *
 * <p>ZERO NAO E AUSENCIA, pelo mesmo motivo de {@link ProtegeComAura}: uma
 * tecnica que devolve zero esta dizendo "eu apago o reforco", e Zetsu diz
 * exatamente isso.
 *
 * <p>Interface a parte porque {@code NenTechnique} e contrato congelado
 * (ADR-004) e a maioria das tecnicas nao reforca golpe nenhum.
 */
public interface ReforcaGolpe {

    /**
     * Quanto esta tecnica reforca o golpe, de 0 para cima.
     *
     * <p>O valor e multiplicado pela aura do BRACO que ataca, entao ele e o
     * reforco com a aura espalhada por igual. Concentrar no braco multiplica;
     * concentrar em outro lugar -- na cabeca, que e o padrao de Gyo --
     * <b>reduz o golpe a quase nada</b>. E essa troca que faz Ko valer o risco.
     */
    double reforcoBase();
}
