package com.darkcontinent.nenfoundation.client.hud;

import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;

/** Suaviza apenas a leitura visual; nunca altera o valor autoritativo recebido. */
public final class AuraInterpolation {

    private final long duracao;
    private boolean inicializada;
    private float anterior;
    private float alvo;
    private long inicio;

    public AuraInterpolation(long duracao) {
        if (duracao < 1L) {
            throw new IllegalArgumentException("duracao deve ser positiva");
        }
        this.duracao = duracao;
    }

    public void receber(DeltaDeRuntimeS2C delta, long tick) {
        float valor = delta.aura();
        if (!Float.isFinite(valor)) {
            throw new IllegalArgumentException("aura deve ser finita");
        }
        if (!this.inicializada) {
            this.anterior = valor;
            this.alvo = valor;
            this.inicializada = true;
        } else {
            this.anterior = valorAtual(tick);
            this.alvo = valor;
        }
        this.inicio = tick;
    }

    public float valorAtual(long tick) {
        if (!this.inicializada) {
            return 0.0F;
        }
        long decorrido = Math.max(0L, tick - this.inicio);
        float fracao = Math.min(1.0F, (float) decorrido / (float) this.duracao);
        return this.anterior + (this.alvo - this.anterior) * fracao;
    }

    public void limpar() {
        this.inicializada = false;
        this.anterior = 0.0F;
        this.alvo = 0.0F;
        this.inicio = 0L;
    }
}
