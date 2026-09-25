package com.darkcontinent.nenfoundation.client.hud;

/**
 * O que a HUD mostra, e quando.
 *
 * <p>TODAS AS REGRAS SAO FUNCOES PURAS, e nao {@code if} espalhado pelo
 * orquestrador. A diferenca aparece no dia em que alguem pergunta "por que a
 * barra de fluxo sumiu?": com a regra num lugar so, a resposta e um teste; com
 * ela espalhada, e uma sessao de jogo.
 *
 * <p>O PRINCIPIO DO REDESENHO DE 2026-09-25: o essencial fica sempre visivel, e
 * o resto aparece quando faz sentido. Vida e Aura sao permanentes porque
 * respondem "estou vivo?" e "posso agir?". Fluxo e a fila de indicadores sao
 * contextuais porque, sem contexto, os dois mostram zero -- e zero permanente e
 * a definicao de ruido.
 */
public final class NenHudVisibility {
    private NenHudVisibility() {
    }

    /** Zero de Aura ou de Output nunca oculta a HUD. */
    public static boolean deveRenderizar(boolean guiOculta, boolean jogadorPresente,
            boolean espectador) {
        return !guiOculta && jogadorPresente && !espectador;
    }

    /**
     * A microbarra de fluxo entra quando ha tecnica ligada.
     *
     * <p>A REGRA E "HA TECNICA", e nao "o output esta alto". As duas parecem a
     * mesma, e a diferenca importa: com Zetsu ligado o output vai a zero, e uma
     * regra por magnitude esconderia a barra exatamente quando ela explica o
     * zero. Com a regra por presenca, o jogador ve a barra vazia e entende que
     * o estado a zerou -- que e a informacao.
     *
     * <p>Sem tecnica nenhuma, o output esta em repouso e a barra so repetiria o
     * que a ausencia do chip ja diz.
     */
    public static boolean deveMostrarFluxo(int tecnicasAtivas) {
        return tecnicasAtivas > 0;
    }

    /**
     * A fila de indicadores entra a partir da SEGUNDA tecnica.
     *
     * <p>COM UMA SO, O CHIP JA DISSE. Desenhar a fila tambem seria escrever a
     * mesma informacao duas vezes a dois centimetros de distancia -- e a
     * poluicao que este redesenho existe para tirar. A partir de duas, o chip
     * nomeia a dominante e a fila mostra o conjunto: passam a dizer coisas
     * diferentes.
     */
    public static boolean deveMostrarFilaDeTecnicas(int tecnicasAtivas) {
        return tecnicasAtivas >= 2;
    }
}
