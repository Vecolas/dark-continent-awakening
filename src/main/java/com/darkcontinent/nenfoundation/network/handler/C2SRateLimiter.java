package com.darkcontinent.nenfoundation.network.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Limite simples por jogador, medido em ticks do servidor. */
public final class C2SRateLimiter {

    /** Janela curta: spam e cortado, nunca enfileirado. */
    static final long JANELA_EM_TICKS = 20L;
    static final int MAXIMO_POR_JANELA = 20;

    private final Map<UUID, Janela> janelas = new HashMap<>();

    /** Retorna falso sem consumir memoria nova quando a janela ainda esta cheia. */
    public boolean permitido(UUID jogadorId, long tick) {
        Objects.requireNonNull(jogadorId, "jogadorId");
        Janela anterior = this.janelas.get(jogadorId);
        if (anterior == null || tick - anterior.inicio() >= JANELA_EM_TICKS
                || tick < anterior.inicio()) {
            this.janelas.put(jogadorId, new Janela(tick, 1));
            return true;
        }
        if (anterior.quantidade() >= MAXIMO_POR_JANELA) {
            return false;
        }
        this.janelas.put(jogadorId, new Janela(anterior.inicio(), anterior.quantidade() + 1));
        return true;
    }

    public void limpar(UUID jogadorId) {
        this.janelas.remove(Objects.requireNonNull(jogadorId, "jogadorId"));
    }

    public void limparTudo() {
        this.janelas.clear();
    }

    private record Janela(long inicio, int quantidade) {
    }
}
