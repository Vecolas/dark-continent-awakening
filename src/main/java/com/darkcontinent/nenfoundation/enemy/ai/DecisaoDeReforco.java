package com.darkcontinent.nenfoundation.enemy.ai;

/**
 * A resposta a "este lobo pode entrar naquele bando?" -- com o MOTIVO junto.
 *
 * <p><b>Por que nao um booleano.</b> Recusa sem motivo produz o pior relato de
 * bug que existe: "as vezes eles nao se juntam". As tres recusas abaixo tem
 * causas completamente diferentes, e duas delas sao coisas que alguem vai querer
 * ajustar -- distinguir uma da outra e a diferenca entre uma tarde de
 * diagnostico e uma linha de log.</p>
 *
 * <p><b>E, principalmente, {@link #BANDO_CHEIO} e a trava que impede o reforco de
 * chamar reforco ate o chunk inteiro.</b> Sem teto, cada lobo que chega e mais um
 * lobo que pode chamar outro, e a matilha cresce enquanto houver lobo no raio. O
 * servidor nao reclama de nada disso: ele so fica lento, e o vale vira parede de
 * carne dentro das regras.</p>
 */
public enum DecisaoDeReforco {
    /** Entra. */
    CHAMA,
    /** O teto de {@code SquadRules.maximoDeMembros} ja foi atingido. */
    BANDO_CHEIO,
    /**
     * O bando esta abaixo da moral minima.
     *
     * <p>Bando quebrado nao recruta. Sem esta recusa, uma matilha em fuga
     * continuaria puxando lobos novos para dentro de um combate que ela ja
     * desistiu de travar -- e cada recem-chegado nasceria ja desmoralizado, o que
     * transforma "recuar junto" num fluxo continuo de bichos entrando e saindo.</p>
     */
    BANDO_DESMORALIZADO,
    /** O candidato esta fora do raio de reforco do bando. */
    LONGE_DEMAIS
}
