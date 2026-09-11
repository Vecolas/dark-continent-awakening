package com.darkcontinent.nenfoundation.network.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class C2SRateLimiterTest {

    @Test
    void cortaSpamPorJogadorSemCompartilharJanela() {
        C2SRateLimiter limite = new C2SRateLimiter();
        UUID primeiro = UUID.randomUUID();
        UUID segundo = UUID.randomUUID();

        for (int i = 0; i < C2SRateLimiter.MAXIMO_POR_JANELA; i++) {
            assertTrue(limite.permitido(primeiro, 100), "pedido dentro da janela deveria passar");
        }
        assertFalse(limite.permitido(primeiro, 100), "o 21o pedido deveria ser cortado");
        assertTrue(limite.permitido(segundo, 100), "jogadores nao compartilham o limite");
        assertTrue(limite.permitido(primeiro, 120), "janela seguinte deve liberar o jogador");
    }

    @Test
    void reloginDescartaJanelaDoJogador() {
        C2SRateLimiter limite = new C2SRateLimiter();
        UUID jogador = UUID.randomUUID();
        for (int i = 0; i < C2SRateLimiter.MAXIMO_POR_JANELA; i++) {
            limite.permitido(jogador, 1);
        }
        limite.limpar(jogador);
        assertTrue(limite.permitido(jogador, 1));
    }
}
