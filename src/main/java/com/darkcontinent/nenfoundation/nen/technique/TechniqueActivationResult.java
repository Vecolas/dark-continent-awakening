package com.darkcontinent.nenfoundation.nen.technique;

import java.util.Optional;
import net.minecraft.network.chat.Component;

/**
 * A resposta do servidor a um pedido de ativacao de tecnica.
 *
 * <p>DECISAO: recusa SEMPRE carrega um motivo legivel. Ativacao que falha em
 * silencio produz o pior relato de bug que existe — "aperto a tecla e nao
 * acontece nada" — e nao ha log que diga qual das oito validacoes reprovou.
 *
 * @param permitido se a ativacao pode prosseguir
 * @param motivo    texto para o jogador; presente sempre que {@code permitido}
 *                  for {@code false}
 */
public record TechniqueActivationResult(boolean permitido, Optional<Component> motivo) {

    private static final TechniqueActivationResult PERMITIDO =
            new TechniqueActivationResult(true, Optional.empty());

    public TechniqueActivationResult {
        if (!permitido && motivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Recusa sem motivo. Toda negativa precisa de texto para o jogador.");
        }
        if (permitido && motivo.isPresent()) {
            throw new IllegalArgumentException(
                    "Permissao com motivo de recusa: contrato ambiguo.");
        }
    }

    /**
     * Nome deliberadamente diferente do componente {@code permitido()}: um
     * metodo estatico com o mesmo nome de um accessor de record nao compila.
     */
    public static TechniqueActivationResult aceito() {
        return PERMITIDO;
    }

    public static TechniqueActivationResult negado(Component motivo) {
        return new TechniqueActivationResult(false, Optional.of(motivo));
    }

    /** Atalho para negar com uma chave de traducao. O texto nao mora no codigo. */
    public static TechniqueActivationResult negado(String chaveDeTraducao) {
        return negado(Component.translatable(chaveDeTraducao));
    }
}
