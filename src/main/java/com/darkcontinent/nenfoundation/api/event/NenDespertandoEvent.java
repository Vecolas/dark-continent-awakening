package com.darkcontinent.nenfoundation.api.event;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Disparado ANTES de um jogador despertar. Cancelavel.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. SAO DOIS EVENTOS, e nao um com um booleano. Quem precisa IMPEDIR o
 * despertar e um caso diferente de quem precisa SABER que ele aconteceu.
 * Misturar os dois faz o primeiro listener registrado decidir por todos, e o
 * segundo reagir a algo que talvez nao va acontecer.
 *
 * <p>2. Ele roda no servidor, sempre. O tipo do campo e {@link ServerPlayer},
 * e nao {@code Player}, para que nao exista a duvida de "sera que este veio do
 * cliente?". Despertar e estado autoritativo.
 *
 * <p>3. Cancelar aqui IMPEDE o despertar por inteiro: o perfil nao muda, o
 * marco nao entra e o evento informativo nao dispara. Nao ha estado
 * intermediario em que o jogador despertou "pela metade".
 *
 * <p>CUIDADO PARA QUEM CANCELA: cancelar em silencio produz um jogador que
 * aperta tudo certo e nao desperta, sem nada no log. Em modo de
 * desenvolvimento o servico registra o cancelamento; quem cancela deveria
 * tambem dizer ao jogador por que.
 */
public class NenDespertandoEvent extends PlayerEvent implements ICancellableEvent {

    private final OrigemDoDespertar origem;

    public NenDespertandoEvent(ServerPlayer jogador, OrigemDoDespertar origem) {
        super(Objects.requireNonNull(jogador, "jogador"));
        this.origem = Objects.requireNonNull(origem, "origem");
    }

    /** O jogador que vai despertar. Sempre server-side. */
    public ServerPlayer jogador() {
        return (ServerPlayer) getEntity();
    }

    /** Como o despertar foi provocado. Ver {@link OrigemDoDespertar}. */
    public OrigemDoDespertar origem() {
        return this.origem;
    }
}
