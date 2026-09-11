package com.darkcontinent.nenfoundation.api.event;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Disparado DEPOIS de um jogador despertar. Nao cancelavel.
 *
 * <p>Quando este evento roda, o perfil ja foi gravado e o marco
 * {@code nenfoundation:despertou} ja existe. Cancelar aqui seria mentir: o
 * fato ja aconteceu, e desfaze-lo exigiria outra operacao com nome proprio.
 *
 * <p>Nome no PASSADO, como todo evento deste mod. Ele relata; nao pede
 * permissao. Quem pede permissao e {@link NenDespertandoEvent}.
 *
 * <p>E o unico jeito de o mundo externo -- quest, addon, conteudo do pack --
 * saber que alguem despertou. Nada de fora navega pela arvore de objetos do
 * nucleo nem le o attachment direto para descobrir isso.
 */
public class NenDespertadoEvent extends PlayerEvent {

    private final OrigemDoDespertar origem;

    public NenDespertadoEvent(ServerPlayer jogador, OrigemDoDespertar origem) {
        super(Objects.requireNonNull(jogador, "jogador"));
        this.origem = Objects.requireNonNull(origem, "origem");
    }

    /** O jogador que despertou. Sempre server-side. */
    public ServerPlayer jogador() {
        return (ServerPlayer) getEntity();
    }

    /** Como o despertar aconteceu. Ver {@link OrigemDoDespertar}. */
    public OrigemDoDespertar origem() {
        return this.origem;
    }
}
