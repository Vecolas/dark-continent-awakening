package com.darkcontinent.nenfoundation.client.hud;

import com.darkcontinent.nenfoundation.client.NenClientCache;
import java.util.Optional;

/** Projecao somente-leitura do delta autoritativo para o HUD de aura. */
public record AuraHudProjection(boolean disponivel, float atual, float maxima, boolean exausto) {

    public static AuraHudProjection de(NenClientCache cache) {
        Optional<com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C> delta = cache.delta();
        if (delta.isEmpty()) {
            return new AuraHudProjection(false, 0.0F, 0.0F, false);
        }
        var valor = delta.orElseThrow();
        return new AuraHudProjection(true, valor.aura(), valor.auraMaxima(), valor.aura() <= 0.0F);
    }

    /** Percentual protegido contra maximo zero ou pacote invalido. */
    public float fracao() {
        if (!this.disponivel || !Float.isFinite(this.atual) || !Float.isFinite(this.maxima)
                || this.maxima <= 0.0F) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, this.atual / this.maxima));
    }
}
