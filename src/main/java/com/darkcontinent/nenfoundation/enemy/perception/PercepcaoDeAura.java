package com.darkcontinent.nenfoundation.enemy.perception;

import java.util.List;

/**
 * Ponto de extensao para o dia em que inimigos enxergarem AURA.
 *
 * <p><b>Ele nasce inerte, e isso e a entrega.</b> A issue #136 e explicita:
 * aura fica como porta, e ninguem inventa Gyo ou In antes de os contratos de Nen
 * existirem (#126, #213). Uma implementacao especulativa aqui viraria uma
 * segunda autoridade sobre Nen -- exatamente o que o CLAUDE.md proibe -- e o
 * pior e que ela pareceria pronta.</p>
 *
 * <p>Enquanto {@link #NENHUMA} for a unica implementacao, o comportamento
 * observavel do mod e identico ao de nao ter a interface. Quando Gyo chegar, o
 * unico arquivo que muda e este, e o {@link PerceptionController} nao sabe a
 * diferenca.</p>
 */
@FunctionalInterface
public interface PercepcaoDeAura {

    /**
     * Filtra ou promove candidatos com base em aura.
     *
     * @param candidatos o que a visao comum ja apurou
     * @return a lista que o avaliador deve considerar
     */
    List<TargetCandidate> aplicar(List<TargetCandidate> candidatos);

    /** Nao ve aura nenhuma: devolve o que recebeu, sem copia e sem custo. */
    PercepcaoDeAura NENHUMA = candidatos -> candidatos;
}
