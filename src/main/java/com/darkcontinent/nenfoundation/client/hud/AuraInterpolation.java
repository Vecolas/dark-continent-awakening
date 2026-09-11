package com.darkcontinent.nenfoundation.client.hud;

import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import com.darkcontinent.nenfoundation.client.hud.animation.HudValueAnimator;
import java.util.function.IntSupplier;

/**
 * Interpola apenas o preenchimento. Primeiro delta e esgotamento sao imediatos;
 * chegada durante animacao parte do ponto visual atual, sem saltar para atras.
 */
public final class AuraInterpolation {
    private final HudValueAnimator animador;

    public AuraInterpolation(long duracao) {
        this(() -> Math.toIntExact(duracao));
        if (duracao < 0) throw new IllegalArgumentException("duracao negativa");
    }

    public AuraInterpolation(IntSupplier duracao) {
        this.animador = new HudValueAnimator(duracao);
    }

    public void receber(DeltaDeRuntimeS2C delta, double tick) {
        validar(delta);
        this.animador.receber(delta.aura(), delta.auraMaxima(), tick, delta.aura() == 0.0F);
    }

    public static void validar(DeltaDeRuntimeS2C delta) {
        if (!Float.isFinite(delta.aura()) || !Float.isFinite(delta.auraMaxima())
                || !Float.isFinite(delta.outputPercent())
                || delta.aura() < 0 || delta.auraMaxima() < 0 || delta.aura() > delta.auraMaxima()
                || delta.outputPercent() < 0.0F || delta.outputPercent() > 1.0F) {
            throw new IllegalArgumentException("delta de aura invalido");
        }
    }

    public float valorAtual(double tick) {
        return this.animador.valorAtual(tick);
    }

    public void limpar() {
        this.animador.limpar();
    }
}
