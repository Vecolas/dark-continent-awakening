package com.darkcontinent.nenfoundation.client.vfx;

/**
 * Os tres niveis de brilho do [ADR-016](docs/adr/ADR-016-pos-processamento-proprio-da-aura.md).
 *
 * <p><b>TRES GRAUS DA MESMA LEITURA, e nao tres estilos.</b> Se {@code FAST} e
 * {@code HIGH} parecerem efeitos diferentes, o jogador que baixa a qualidade nao
 * esta vendo "menos do mesmo jogo" -- esta vendo outro jogo. A trava contra essa
 * divergencia nao e um portao automatico: e a captura de comparacao do mesmo
 * quadro nos tres niveis, refeita a cada mudanca de qualquer um dos dois. Isso
 * esta declarado como ponto cego em
 * {@code docs/testing/o-que-nao-provamos.md}.
 *
 * <p><b>O FALLBACK E {@code FAST}, E NUNCA {@code OFF}.</b> A distincao importa:
 * {@code OFF} e uma escolha legitima de quem quer o jogo mais leve, e rebaixar
 * ate ele esconderia que alguma coisa FALHOU. Caindo em {@code FAST}, o efeito
 * continua visivel e a diferenca em relacao ao que a pessoa escolheu e
 * perceptivel -- que e o unico jeito de "o shader nao compilou" chegar a alguem
 * sem virar uma linha de log que ninguem le.
 *
 * <p>CONFIG DE CLIENTE, e nunca COMMON. Dois jogadores no mesmo servidor podem
 * ver brilhos diferentes, de proposito: um brilho decidido pelo servidor
 * obrigaria os dois a ver a mesma coisa, que e o oposto de conforto visual.
 */
public enum AuraBloomLevel {

    /**
     * Nenhum passe extra.
     *
     * <p>A aura continua legivel pela borda com Fresnel e pelos filamentos --
     * o nucleo do efeito tem de funcionar sem nenhum pos-processamento, e isso e
     * decisao do ADR-016: bloom e melhoria, nao identidade.
     */
    OFF,

    /**
     * Sem framebuffer nenhum: o halo externo da shell e engrossado e clareado.
     *
     * <p>E UMA APROXIMACAO GEOMETRICA, e ela e assumidamente mais pobre. O que
     * ela nao faz e sangrar luz para FORA da silhueta -- ela apenas alarga o que
     * ja e geometria. A vantagem e que nao existe alvo de render para criar,
     * redimensionar, liberar ou vazar.
     */
    FAST,

    /**
     * A cadeia real: alvo com mascara de profundidade, downsample, blur
     * separavel e composite aditivo.
     */
    HIGH;

    /** Se este nivel precisa de alvo de render. Exatamente um precisa. */
    public boolean usaFramebuffer() {
        return this == HIGH;
    }

    /** Se este nivel desenha alguma coisa a mais que a shell crua. */
    public boolean desenhaAlgo() {
        return this != OFF;
    }

    /**
     * Para onde este nivel cai quando alguma coisa falha.
     *
     * <p>{@code HIGH} cai para {@code FAST}; {@code FAST} e {@code OFF} nao tem
     * para onde cair -- e nao precisam, porque nenhum dos dois cria alvo nem
     * depende de shader de pos-processamento.
     */
    public AuraBloomLevel rebaixado() {
        return this == HIGH ? FAST : this;
    }
}
