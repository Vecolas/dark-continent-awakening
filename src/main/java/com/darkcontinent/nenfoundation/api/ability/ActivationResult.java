package com.darkcontinent.nenfoundation.api.ability;

import java.util.Optional;
import net.minecraft.network.chat.Component;

/**
 * A resposta do servidor a um pedido de ativacao de habilidade.
 *
 * <p>Mesmo contrato de {@code TechniqueActivationResult}: recusa carrega
 * motivo, sempre. Sao dois tipos porque sao dois pipelines de validacao com
 * causas de recusa diferentes; unificar agora esconderia essa diferenca.
 *
 * @param permitido se a ativacao pode prosseguir
 * @param motivo    texto para o jogador; presente sempre que {@code permitido}
 *                  for {@code false}
 */
public record ActivationResult(boolean permitido, Optional<Component> motivo) {

    private static final ActivationResult PERMITIDO =
            new ActivationResult(true, Optional.empty());

    public ActivationResult {
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
    public static ActivationResult aceito() {
        return PERMITIDO;
    }

    public static ActivationResult negado(Component motivo) {
        return new ActivationResult(false, Optional.of(motivo));
    }

    public static ActivationResult negado(String chaveDeTraducao) {
        return negado(Component.translatable(chaveDeTraducao));
    }
}
