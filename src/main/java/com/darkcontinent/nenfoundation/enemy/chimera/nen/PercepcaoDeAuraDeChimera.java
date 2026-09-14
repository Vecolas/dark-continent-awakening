package com.darkcontinent.nenfoundation.enemy.chimera.nen;

import com.darkcontinent.nenfoundation.enemy.perception.PercepcaoDeAura;
import com.darkcontinent.nenfoundation.enemy.perception.TargetCandidate;
import java.util.List;

/**
 * A porta de aura da formiga -- e ela continua INERTE, de proposito.
 *
 * <p><b>Por que ela existe vazia.</b> A issue #145 pede que a formiga desperta
 * enxergue aura; a issue #126 (a camada de percepcao de Gyo) ainda esta ABERTA no
 * nucleo de Nen. Implementar a leitura aqui significaria inventar, no pacote de
 * inimigos, a regra de quem-ve-o-que -- e isso e autoridade do Nen Foundation,
 * nao deste lado. Uma implementacao especulativa pareceria pronta e seria a
 * segunda autoridade sobre Nen que o CLAUDE.md proibe.</p>
 *
 * <p><b>O que ela entrega hoje:</b> o LUGAR, com o nome certo e o contrato certo,
 * de modo que, quando Gyo existir, a mudanca seja um arquivo -- e nao uma
 * arqueologia atras de todos os pontos onde alguem espalhou "se enxerga aura".</p>
 *
 * <p>Enquanto {@link #DORMENTE} for a unica implementacao, o comportamento
 * observavel e identico ao de nao ter a porta: a lista sai como entrou, sem
 * copia e sem custo.</p>
 */
public final class PercepcaoDeAuraDeChimera {

    private PercepcaoDeAuraDeChimera() { }

    /**
     * Nao ve aura nenhuma.
     *
     * <p>Nao e {@code PercepcaoDeAura.NENHUMA} por acaso -- e literalmente ela.
     * Um segundo objeto inerte com o mesmo comportamento seria duas fontes para a
     * mesma verdade, e no dia em que uma ganhasse regra a outra continuaria
     * calada.</p>
     */
    public static final PercepcaoDeAura DORMENTE = PercepcaoDeAura.NENHUMA;

    /**
     * A porta que a formiga usa hoje.
     *
     * @param despertou se a formiga ja tem algum grau de Nen
     * @return sempre a porta inerte -- o parametro existe para o dia em que a
     *         resposta passar a depender dele, e para que o chamador ja escreva a
     *         pergunta certa desde agora
     */
    public static PercepcaoDeAura para(boolean despertou) {
        // O parametro e lido e descartado DE PROPOSITO, e o comentario existe para
        // que ninguem "limpe" a assinatura: sem ele, o dia em que Gyo chegar
        // obrigaria a mexer em todos os pontos de construcao de formiga.
        return DORMENTE;
    }

    /** Uma porta que nunca filtra, escrita por extenso para o teste poder compara-la. */
    static List<TargetCandidate> inerte(List<TargetCandidate> candidatos) {
        return candidatos;
    }
}
