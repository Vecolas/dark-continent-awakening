package com.darkcontinent.nenfoundation.network.handler;

import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Regras comuns, puras e testaveis dos pedidos C2S. */
public final class C2SValidacao {

    private static final int PRIMEIRO_SLOT = 0;
    private static final int ULTIMO_SLOT = 8;

    private C2SValidacao() {
    }

    public static String tecnica(PersistentNenData perfil, RuntimeNenState estado,
            ResourceLocation id) {
        Objects.requireNonNull(perfil, "perfil");
        Objects.requireNonNull(estado, "estado");
        if (id == null) {
            return "nen.error.invalid_request";
        }
        if (!perfil.awakened()) {
            return "nen.error.not_awakened";
        }
        if (!perfil.conheceTecnica(id)) {
            return "nen.error.technique_locked";
        }
        return null;
    }

    public static String habilidade(PersistentNenData perfil, RuntimeNenState estado,
            ResourceLocation id, int slot) {
        Objects.requireNonNull(perfil, "perfil");
        Objects.requireNonNull(estado, "estado");
        if (id == null || slot < PRIMEIRO_SLOT || slot > ULTIMO_SLOT) {
            return "nen.error.invalid_request";
        }
        if (!perfil.awakened()) {
            return "nen.error.not_awakened";
        }
        if (!perfil.conheceHabilidade(id)) {
            return "nen.error.ability_locked";
        }
        return null;
    }
}
