package com.darkcontinent.nenfoundation.client.hud.animation;

import java.util.function.IntSupplier;

/**
 * Interpolador escalar somente visual.
 *
 * <p>O valor autoritativo nunca passa por esta classe. Ela existe apenas no
 * cliente e sempre converge para o ultimo alvo recebido do servidor.
 */
public final class HudValueAnimator {
    private final IntSupplier duracao;
    private boolean inicializado;
    private float anterior;
    private float alvo;
    private float maximo;
    private double inicio;

    public HudValueAnimator(IntSupplier duracao) {
        this.duracao = duracao;
    }

    public void receber(float valor, float maximo, double tick, boolean imediato) {
        validar(valor, maximo);
        float visualAtual = valorAtual(tick);
        this.anterior = !this.inicializado || imediato || tick < this.inicio
                ? valor : Math.min(maximo, visualAtual);
        this.alvo = valor;
        this.maximo = maximo;
        this.inicializado = true;
        this.inicio = tick;
    }

    public float valorAtual(double tick) {
        if (!this.inicializado) return 0.0F;
        int tempo = this.duracao.getAsInt();
        if (tempo < 0) throw new IllegalArgumentException("duracao negativa");
        double fracao = tempo == 0 ? 1.0D
                : Math.clamp((tick - this.inicio) / tempo, 0.0D, 1.0D);
        return Math.clamp((float) (this.anterior + (this.alvo - this.anterior) * fracao),
                0.0F, this.maximo);
    }

    public void limpar() {
        this.inicializado = false;
        this.anterior = 0.0F;
        this.alvo = 0.0F;
        this.maximo = 0.0F;
        this.inicio = 0.0D;
    }

    private static void validar(float valor, float maximo) {
        if (!Float.isFinite(valor) || !Float.isFinite(maximo)
                || valor < 0.0F || maximo < 0.0F || valor > maximo) {
            throw new IllegalArgumentException("valor visual invalido");
        }
    }
}
