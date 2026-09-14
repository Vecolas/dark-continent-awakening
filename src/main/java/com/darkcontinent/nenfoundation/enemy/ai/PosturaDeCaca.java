package com.darkcontinent.nenfoundation.enemy.ai;

import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenIntent;

/**
 * O que a INTENCAO de Nen muda no CORPO da formiga -- e so no corpo.
 *
 * <p><b>A fronteira que este arquivo existe para nao cruzar.</b> O Nen
 * Foundation e a unica autoridade sobre Nen (CLAUDE.md). Uma
 * {@link TacticalNenIntent} nao ativa tecnica, nao gasta aura e nao aplica
 * custo: ela e uma resposta a uma pergunta. O que este enum faz e traduzir essa
 * resposta em POSTURA -- para onde o bicho anda e se ele gasta o arranque --, e
 * postura e propriedade do inimigo, nao do Nen. Nenhum valor daqui toca em aura,
 * e e por isso que ele mora em {@code enemy.ai} e nao em {@code chimera.nen}.</p>
 *
 * <p><b>Por que a traducao e um mapa explicito e nao um {@code if}.</b> Um
 * {@code if (intencao == ENTRAR_EM_ZETSU)} espalhado pela entidade deixa as
 * outras cinco intencoes caindo no caso padrao por acidente -- e o acidente nao
 * da erro: a formiga treinada que decidiu se defender continuaria correndo para
 * cima do jogador como se nao tivesse decidido nada. Com o {@code switch}
 * exaustivo abaixo, uma intencao NOVA no nucleo quebra a COMPILACAO deste
 * arquivo, e alguem e obrigado a dizer o que o corpo faz com ela.</p>
 *
 * <p><b>Hoje quase nada disso e alcancavel, e a ausencia esta declarada.</b>
 * Enquanto o nucleo nao publicar a fracao de aura da formiga (a porta de
 * percepcao segue em {@code PercepcaoDeAura.NENHUMA}), o
 * {@code TacticalNenController} devolve {@link TacticalNenIntent#NENHUMA} para
 * todo mundo, e o unico ramo que roda em jogo e {@link #CACAR}. Isso e o valor
 * PADRAO e nao um buraco: no dia em que a porta abrir, os outros dois ramos
 * passam a valer sem que esta classe ou a entidade mudem uma linha.</p>
 */
public enum PosturaDeCaca {

    /**
     * Fechar distancia. E a postura normal, e a unica alcancavel hoje.
     *
     * <p>Com o arranque disponivel ela e o que faz o lider CHEGAR primeiro.</p>
     */
    CACAR,

    /**
     * Segurar a posicao: nao gastar o arranque.
     *
     * <p>Vale para quem esta se defendendo (Ken) e para quem esta procurando o
     * que a visao comum nao explica (Gyo). Nos dois casos o bicho decidiu que o
     * problema nao e distancia -- e arrancar cegamente gastaria o ciclo inteiro
     * para chegar fatigado no lugar errado.</p>
     */
    GUARDAR,

    /**
     * Romper contato: usar o arranque para SAIR, e nao para chegar.
     *
     * <p>E a postura de quem entrou em Zetsu. Zetsu e a decisao de sumir, e um
     * bicho que decide sumir e continua correndo na direcao do jogador contradiz
     * a propria decisao na tela -- sem que nada no servidor acuse.</p>
     */
    ROMPER;

    /**
     * A postura que corresponde a intencao.
     *
     * <p>O {@code switch} e exaustivo de proposito: ele e o portao que obriga
     * quem acrescentar uma intencao no nucleo a decidir, aqui, o que o corpo faz
     * com ela. Um {@code default} silencioso transformaria esse portao num
     * carimbo.</p>
     */
    public static PosturaDeCaca de(TacticalNenIntent intencao) {
        if (intencao == null) {
            throw new IllegalArgumentException("intencao tatica ausente: devolver CACAR por padrao"
                    + " esconderia a ausencia atras de um bicho que persegue normalmente, e a"
                    + " decisao de Nen que faltou nunca apareceria");
        }
        return switch (intencao) {
            case NENHUMA, MANTER_TEN, ELEVAR_REN -> CACAR;
            case USAR_GYO, MANTER_KEN -> GUARDAR;
            case ENTRAR_EM_ZETSU -> ROMPER;
        };
    }

    /** Esta postura autoriza gastar o arranque para FECHAR distancia? */
    public boolean fechaDistancia() { return this == CACAR; }

    /** Esta postura usa o arranque para AFASTAR-se? */
    public boolean rompeContato() { return this == ROMPER; }
}
