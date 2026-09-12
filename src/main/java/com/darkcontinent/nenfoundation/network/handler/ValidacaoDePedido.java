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

    /**
     * Uma recusa, com a chave que o jogador vai LER.
     *
     * <p>ELA EXISTE PORQUE O MOTIVO ESTAVA SENDO JOGADO FORA. O servico de
     * pedidos traduzia toda recusa de tecnica em {@code ESTADO_INVALIDO} e
     * descartava o texto que a tecnica tinha escrito. Shu recusa mao vazia com
     * {@code "nenfoundation.error.shu_sem_item"} -- a chave existe, o gametest
     * passa, e o jogador de mao vazia le <i>"Seu estado atual nao permite esse
     * pedido"</i>.
     *
     * <p>Isso e o princípio 8 quebrado por dentro: a recusa TEM motivo, e o
     * motivo nao chega. Eu tinha classificado este buraco como latente quando
     * o HUD foi entregue -- e ele deixou de ser latente no dia em que Shu
     * nasceu, sem que ninguem voltasse aqui.
     *
     * @param chave a chave de traducao mostrada ao jogador
     */
    public record Recusa(String chave) {
        public Recusa {
            if (chave == null || chave.isBlank()) {
                throw new IllegalArgumentException("recusa sem chave");
            }
        }

        /** A recusa de um motivo padrao. */
        public static Recusa de(Motivo motivo) {
            return new Recusa(motivo.chave());
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
