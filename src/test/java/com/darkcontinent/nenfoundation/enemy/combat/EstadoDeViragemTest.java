package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A janela de costas conta certo, avisa antes de acabar e nao se renova.
 *
 * <p>Os tres defeitos que este arquivo impede tem a mesma assinatura -- nenhum
 * levanta excecao: uma janela que nao termina da um chefe eternamente virado; uma
 * que termina sem aviso transforma o quebra-cabeca em sorte; e uma que se renova
 * a cada golpe da as duas coisas ao mesmo tempo.</p>
 */
class EstadoDeViragemTest {

    private static final RegrasDeViragem REGRAS = new RegrasDeViragem(
            new WeakPointResolver("ventre", "carapaca", 0.20D, 0.5D),
            new WeakPointResolver("ventre", "carapaca", 0.60D, -1.0D),
            40, 10);

    private static EstadoDeViragem novo() { return new EstadoDeViragem(REGRAS); }

    @Test
    @DisplayName("o caso normal: vira, conta os ticks e volta de pe no ultimo")
    void aJanelaComecaEAcaba() {
        EstadoDeViragem estado = novo();
        assertFalse(estado.deCostas(), "ele nasce de pe");
        assertTrue(estado.virar());
        assertEquals(40, estado.ticksRestantes());

        for (int i = 0; i < 39; i++) {
            assertFalse(estado.tick(), "tick " + i + " ainda nao e o fim da janela");
            assertTrue(estado.deCostas());
        }
        assertTrue(estado.tick(), "o quadragesimo tick e o que devolve o bicho para de pe");
        assertFalse(estado.deCostas());
        assertEquals(0, estado.ticksRestantes());
    }

    @Test
    @DisplayName("o aviso de fim comeca nos ultimos ticks, e nem um antes")
    void oAvisoChegaNaHoraCerta() {
        EstadoDeViragem estado = novo();
        estado.virar();
        for (int i = 0; i < 30; i++) {
            assertFalse(estado.levantando(),
                    "com " + estado.ticksRestantes() + " ticks restando ele ainda esta caido");
            estado.tick();
        }
        assertEquals(10, estado.ticksRestantes());
        assertTrue(estado.levantando(), "faltando 10, a pose de endireitar tem de entrar");
        // Se esta asercao cair, a janela acaba sem nada na tela dizendo que ela vai
        // acabar, e o jogador leva a investida seguinte no meio de uma espadada.
    }

    @Test
    @DisplayName("de pe ele nao esta levantando -- sao coisas diferentes")
    void dePeNaoELevantando() {
        EstadoDeViragem estado = novo();
        assertFalse(estado.levantando(),
                "zero ticks restantes e 'de pe', e nao 'no fim da janela': confundir os dois"
                        + " deixaria o clipe de endireitar rodando com o bicho em pe");
    }

    @Test
    @DisplayName("tick com o bicho de pe nao faz nada, e nao conta para baixo de zero")
    void tickDePeNaoFazNada() {
        EstadoDeViragem estado = novo();
        for (int i = 0; i < 5; i++) {
            assertFalse(estado.tick());
        }
        assertEquals(0, estado.ticksRestantes(),
                "contador negativo deixaria deCostas() falso e levantando() imprevisivel");
    }

    @Test
    @DisplayName("a SEGUNDA tranca: virar de novo nao renova a janela")
    void oSegundoVirarERecusado() {
        EstadoDeViragem estado = novo();
        assertTrue(estado.virar());
        estado.tick();
        estado.tick();
        assertFalse(estado.virar(), "a chamada e recusada, e a recusa tem valor de retorno");
        assertEquals(38, estado.ticksRestantes(),
                "renovando, cada golpe no ventre esticaria a janela e o bicho morreria"
                        + " de costas sem nunca mais se levantar");
    }

    @Test
    @DisplayName("limpar apaga a janela nos pontos de saida")
    void limparApagaAJanela() {
        EstadoDeViragem estado = novo();
        estado.virar();
        estado.limpar();
        assertFalse(estado.deCostas());
        assertEquals(0, estado.ticksRestantes());
        assertTrue(estado.virar(),
                "depois de limpo ele volta a poder virar: limpar e saida, e nao trava");
    }

    @Test
    @DisplayName("estado sem regras e recusado na construcao")
    void regrasAusentesSaoRejeitadas() {
        assertThrows(NullPointerException.class, () -> new EstadoDeViragem(null));
    }
}
