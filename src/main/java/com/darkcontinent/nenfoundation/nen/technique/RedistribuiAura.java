package com.darkcontinent.nenfoundation.nen.technique;

import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;

/**
 * Uma tecnica que muda ONDE a aura esta enquanto estiver ativa.
 *
 * <p>E a interface de Gyo, Ko, Ken e Ryu -- quatro das sete tecnicas que
 * faltam. Elas nao "usam" alocacao como quem usa uma biblioteca: elas SAO a
 * alocacao, com politicas diferentes. Ver o ADR-014.
 *
 * <p>INTERFACE A PARTE pelo mesmo motivo de {@link ModificaTetoDeOutput} e
 * {@link ConsomeAura}: {@code NenTechnique} e contrato congelado (ADR-004), e a
 * maioria das tecnicas nao mexe em distribuicao.
 *
 * <p>ENTRE VARIAS ATIVAS, VALE A MAIS CONCENTRADA -- e nao a soma nem a media.
 * Somar estouraria a invariante na primeira combinacao; a media transformaria
 * Ko com Ken numa concentracao morna, que e o oposto do que as duas fazem. A
 * regra esta no servico, e tem portao.
 */
public interface RedistribuiAura {

    /**
     * Como esta tecnica quer a aura distribuida. A soma precisa fechar em 1.0.
     *
     * <p>O FOCO VEM COMO ARGUMENTO, e nao de um campo da tecnica. A
     * implementacao e uma so, compartilhada por todos os jogadores do servidor:
     * guardar a regiao escolhida num campo seria o erro numero 2 da lista do
     * CLAUDE.md -- dois jogadores concentrando em membros diferentes
     * escreveriam no mesmo lugar, sem erro nenhum.
     *
     * <p>A primeira versao disto passava o foco por {@code ThreadLocal}, para
     * nao mexer na assinatura. Era o argumento certo escondido: qualquer
     * recalculo fora da janela leria o padrao em silencio. Trocar agora custa
     * pouco justamente porque so ha um implementador -- e esse custo so cresce.
     *
     * <p>Uma tecnica que nao dependa de regiao -- Ken, que distribui alto em
     * todas -- simplesmente ignora o argumento.
     *
     * @param foco a regiao em que ESTE jogador esta concentrando
     */
    AlocacaoDeAura alocacaoDesejada(RegiaoDoCorpo foco);
}
