package com.darkcontinent.nenfoundation.network;

import com.darkcontinent.nenfoundation.network.handler.LimiteDePedidos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Abuso real do orcamento: rajada, servidor parado e troca de sessao. */
class LimiteDePedidosTest {
    @Test
    void cortaCentenasSemEnfileirarERecuperaDepois() {
        var limite = new LimiteDePedidos(0);
        int admitidos = 0;
        for (int i = 0; i < 500; i++) {
            if (limite.admitir(0, 20) >= 0) {
                admitidos++;
                limite.concluir();
            }
        }
        assertEquals(20, admitidos);
        assertEquals(480, limite.recusados());
        assertEquals(-1, limite.admitir(999_999_999, 20));
        assertTrue(limite.admitir(1_000_000_000, 20) >= 0);
    }

    @Test
    void servidorParadoNaoAcumulaFilaMesmoPassandoTempo() {
        var limite = new LimiteDePedidos(0);
        int admitidos = 0;
        for (int i = 0; i < 500; i++) {
            if (limite.admitir(i * 1_000_000_000L, 200) >= 0) admitidos++;
        }
        assertTrue(admitidos > 0 && admitidos <= 8);
        limite.concluir();
        assertTrue(limite.admitir(501_000_000_000L, 200) >= 0);
    }

    @Test
    void dimensaoInvalidaTrabalhoMasNaoRenovaCota() {
        var limite = new LimiteDePedidos(0);
        long geracao = limite.admitir(0, 1);
        assertTrue(limite.atual(geracao));
        limite.invalidar();
        assertFalse(limite.atual(geracao));
        limite.concluir();
        assertEquals(-1, limite.admitir(0, 1));
        limite.encerrar();
        assertEquals(-1, limite.admitir(2_000_000_000L, 1));
    }

    @Test
    void cotasNaoVazamEConfiguracaoEConsultadaPorPedido() {
        var um = new LimiteDePedidos(0);
        var outro = new LimiteDePedidos(0);
        assertEquals(0, um.admitir(0, 1));
        um.concluir();
        assertEquals(-1, um.admitir(0, 1));
        assertEquals(0, outro.admitir(0, 1));
        assertEquals(0, um.admitir(0, 2));
    }
}
