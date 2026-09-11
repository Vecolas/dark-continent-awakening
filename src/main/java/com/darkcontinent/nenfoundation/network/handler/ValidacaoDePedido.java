package com.darkcontinent.nenfoundation.network.handler;

import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import net.minecraft.resources.ResourceLocation;

/** Validacao comum, sem mutacao. O catalogo vem do servidor, nunca dos unlocks. */
public final class ValidacaoDePedido {
    private ValidacaoDePedido() { }

    public enum Motivo {
        LIMITE, ESTADO_INVALIDO, NAO_DESPERTO, ID_INEXISTENTE,
        NAO_DESBLOQUEADO, ALVO_INVALIDO, PEDIDO_INVALIDO, EM_RECARGA,
        INDISPONIVEL, ERRO_INTERNO;

        public String chave() {
            return "nenfoundation.error." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static Motivo validar(ResourceLocation id, boolean habilidade,
            boolean existe, boolean estadoValido, PersistentNenData perfil, RuntimeNenState runtime) {
        if (!estadoValido) return Motivo.ESTADO_INVALIDO;
        if (!perfil.awakened()) return Motivo.NAO_DESPERTO;
        if (!existe) return Motivo.ID_INEXISTENTE;
        if (!(habilidade ? perfil.unlockedAbilities() : perfil.unlockedTechniques()).contains(id)) {
            return Motivo.NAO_DESBLOQUEADO;
        }
        if (habilidade && runtime.cooldowns().getOrDefault(id, 0) > 0) return Motivo.EM_RECARGA;
        // M1 nao possui motores/definicoes M4/M5. Nunca fabricar ativacao aqui.
        return Motivo.INDISPONIVEL;
    }
}
