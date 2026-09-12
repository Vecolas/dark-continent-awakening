package com.darkcontinent.nenfoundation.nen.combat;

import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import java.util.List;

/**
 * Onde o golpe acertou, pela altura de onde ele veio.
 *
 * <p>O MINECRAFT NAO TEM REGIAO DE ACERTO. Um golpe atinge a entidade, e nao o
 * braco dela. Sem resolver isso, metade das tecnicas perde o sentido: Ko
 * concentra num ponto <b>e deixa o resto exposto</b> -- sem regiao, nao ha
 * "resto"; Ryu redistribui para onde o golpe vem -- sem regiao, nao ha para
 * onde.
 *
 * <p>A ALTURA DA FONTE DO DANO E A RESPOSTA, e ela e informacao de verdade:
 * quem ataca de cima acerta a cabeca, e quem vem rasteiro acerta as pernas.
 *
 * <p>DETERMINISTICO, E NAO SORTEADO. Dano aleatorio num jogo de combate e o
 * tipo de coisa que ninguem consegue aprender a jogar -- o jogador nao teria
 * como saber se morreu por erro dele ou por azar. Com a altura, a regra e
 * observavel e da para usar.
 *
 * <p>OS BRACOS NAO ENTRAM, e isso e ponto cego declarado: nao ha como saber
 * que alguem bloqueou com o braco. A aura neles continua tendo uso ofensivo
 * (Shu, Ko) e nenhum defensivo, ate existir um sistema de bloqueio -- que o
 * Minecraft vanilla nao da.
 */
public enum FaixaDoCorpo {

    /** Acima da linha alta: a cabeca. */
    CABECA(List.of(RegiaoDoCorpo.CABECA)),

    /** O meio do corpo. E onde quase tudo acerta. */
    TRONCO(List.of(RegiaoDoCorpo.TRONCO)),

    /** Abaixo da linha baixa: as duas pernas juntas. */
    PERNAS(List.of(RegiaoDoCorpo.PERNA_ESQUERDA, RegiaoDoCorpo.PERNA_DIREITA));

    /**
     * Onde a cabeca comeca, como fracao da altura.
     *
     * <p>CONSTANTE NO CODIGO, e nao config: nao e botao de balanceamento, e sim
     * a forma do corpo. Quem quiser mudar o quanto a cabeca e vulneravel mexe
     * na protecao, nao em onde a cabeca fica.
     */
    private static final double LINHA_DA_CABECA = 0.80D;

    /** Onde as pernas terminam, como fracao da altura. */
    private static final double LINHA_DAS_PERNAS = 0.35D;

    private final List<RegiaoDoCorpo> regioes;

    FaixaDoCorpo(List<RegiaoDoCorpo> regioes) {
        this.regioes = regioes;
    }

    /**
     * A faixa atingida, dada a altura relativa do golpe.
     *
     * @param fracaoDaAltura 0 nos pes, 1 no topo da cabeca. Valores fora desse
     *     intervalo sao presos nele -- um golpe vindo de baixo do chao ou de
     *     muito alto ainda acerta alguma parte do corpo.
     */
    public static FaixaDoCorpo porAltura(double fracaoDaAltura) {
        if (!Double.isFinite(fracaoDaAltura)) {
            // NaN NAO VIRA UMA FAIXA QUALQUER. Ele chega de divisao por altura
            // zero, que acontece com entidade em estado estranho, e o padrao
            // seguro e o tronco: a faixa que quase tudo acerta.
            return TRONCO;
        }
        if (fracaoDaAltura >= LINHA_DA_CABECA) {
            return CABECA;
        }
        if (fracaoDaAltura < LINHA_DAS_PERNAS) {
            return PERNAS;
        }
        return TRONCO;
    }

    /**
     * Quanta aura esta defendendo esta faixa, ja normalizada.
     *
     * <p>NORMALIZADA PELO NUMERO DE REGIOES, e este detalhe e a diferenca entre
     * a defesa funcionar e a defesa valer um sexto do que deveria. A alocacao
     * soma 1.0 entre seis regioes, entao em repouso cada uma vale 0,167 -- usar
     * isso cru faria o jogador em repouso ter 17% da protecao que a tecnica
     * promete.
     *
     * <p>Multiplicando pelo total de regioes, a alocacao uniforme devolve
     * exatamente 1.0, e concentrar em outro lugar devolve menos.
     */
    public float auraDefendendo(AlocacaoDeAura alocacao) {
        if (alocacao == null) {
            return 0.0F;
        }
        float soma = 0.0F;
        for (RegiaoDoCorpo regiao : this.regioes) {
            soma += alocacao.em(regiao);
        }
        // As pernas contam como UMA faixa: a media delas, e nao a soma, senao
        // duas regioes defenderiam o dobro de uma.
        float media = soma / this.regioes.size();
        return media * RegiaoDoCorpo.values().length;
    }

    /** As regioes que compoem esta faixa. */
    public List<RegiaoDoCorpo> regioes() {
        return this.regioes;
    }
}
