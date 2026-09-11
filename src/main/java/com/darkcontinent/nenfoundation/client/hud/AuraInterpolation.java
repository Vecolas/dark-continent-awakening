package com.darkcontinent.nenfoundation.client.hud;

import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import java.util.function.IntSupplier;

/**
 * Interpola apenas o preenchimento. Primeiro delta e esgotamento sao imediatos;
 * chegada durante animacao parte do ponto visual atual, sem saltar para atras.
 */
public final class AuraInterpolation {
    private final IntSupplier duracao;
    private boolean inicializada;
    private float anterior;
    private float alvo;
    private float maxima;
    private double inicio;

    public AuraInterpolation(long duracao) {
        this(() -> Math.toIntExact(duracao));
        if (duracao < 0) throw new IllegalArgumentException("duracao negativa");
    }

    public AuraInterpolation(IntSupplier duracao) { this.duracao = duracao; }

    public void receber(DeltaDeRuntimeS2C delta, double tick) {
        validar(delta);
        float valor = delta.aura();
        this.anterior = !this.inicializada || valor == 0 || tick < this.inicio
                ? valor : Math.min(delta.auraMaxima(), valorAtual(tick));
        this.alvo = valor;
        this.maxima = delta.auraMaxima();
        this.inicializada = true;
        this.inicio = tick;
    }

    public static void validar(DeltaDeRuntimeS2C delta) {
        if (!Float.isFinite(delta.aura()) || !Float.isFinite(delta.auraMaxima())
                || delta.aura() < 0 || delta.auraMaxima() < 0 || delta.aura() > delta.auraMaxima()) {
            throw new IllegalArgumentException("delta de aura invalido");
        }
    }

    public float valorAtual(double tick) {
        if (!this.inicializada) return 0;
        int tempo = this.duracao.getAsInt();
        if (tempo < 0) throw new IllegalArgumentException("duracao negativa");
        double fracao = tempo == 0 ? 1 : Math.clamp((tick - this.inicio) / tempo, 0, 1);
        return Math.clamp((float) (this.anterior + (this.alvo - this.anterior) * fracao), 0, this.maxima);
    }

    public void limpar() {
        this.inicializada = false;
        this.anterior = 0;
        this.alvo = 0;
        this.maxima = 0;
        this.inicio = 0;
    }
}
