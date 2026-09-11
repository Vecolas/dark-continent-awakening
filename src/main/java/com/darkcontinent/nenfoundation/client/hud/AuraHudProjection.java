package com.darkcontinent.nenfoundation.client.hud;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import java.util.Optional;

/** Projecao somente-leitura do delta autoritativo para o HUD de aura. */
public record AuraHudProjection(boolean disponivel, float atual, float maxima, boolean exausto,
        float visual) {

    public static AuraHudProjection de(NenClientCache cache) {
        return de(cache, 0);
    }

    public static AuraHudProjection de(NenClientCache cache, float parcial) {
        Optional<com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C> delta = cache.delta();
        if (delta.isEmpty()) {
            return new AuraHudProjection(false, 0.0F, 0.0F, false, 0.0F);
        }
        var valor = delta.orElseThrow();
        float auraVisual = cache.auraInterpolada(parcial);
        return new AuraHudProjection(true, valor.aura(), valor.auraMaxima(),
                valor.auraMaxima() > 0 && valor.aura() == 0, auraVisual);
    }

    /** Percentual protegido contra maximo zero ou pacote invalido. */
    public float fracao() {
        if (!this.disponivel || !Float.isFinite(this.atual) || !Float.isFinite(this.maxima)
                || this.maxima <= 0.0F) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, this.visual / this.maxima));
    }
}
