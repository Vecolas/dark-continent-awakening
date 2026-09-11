package com.darkcontinent.nenfoundation.network.handler;

import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import net.minecraft.resources.ResourceLocation;

/**
 * Validacao comum, sem mutacao. O catalogo vem do servidor, nunca dos unlocks.
 *
 * <p>{@code null} significa PEDIDO VALIDO. Quem chama decide o que fazer com
 * ele; este arquivo nao executa nada.
 */
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

        // DAQUI PARA BAIXO O PEDIDO ESTA VALIDO. Quem executa e o chamador.
        //
        // Ate o M4 este metodo devolvia INDISPONIVEL sempre, porque nao havia
        // motor para executar nada -- e fabricar ativacao sem motor seria
        // dizer ao jogador que algo aconteceu quando nada aconteceu.
        //
        // Agora existe motor de TECNICA. HABILIDADE continua sem motor ate o
        // M5, e por isso continua respondendo INDISPONIVEL: o que mudou foi o
        // mundo, e nao a regra.
        return habilidade ? Motivo.INDISPONIVEL : null;
    }
}
