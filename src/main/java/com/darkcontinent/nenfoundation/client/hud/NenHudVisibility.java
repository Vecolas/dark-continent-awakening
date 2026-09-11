package com.darkcontinent.nenfoundation.client.hud;

/** Regra pura de visibilidade; zero de Aura ou Output nunca ocultam a HUD. */
public final class NenHudVisibility {
    private NenHudVisibility() {
    }

    public static boolean deveRenderizar(boolean guiOculta, boolean jogadorPresente,
            boolean espectador) {
        return !guiOculta && jogadorPresente && !espectador;
    }
}
