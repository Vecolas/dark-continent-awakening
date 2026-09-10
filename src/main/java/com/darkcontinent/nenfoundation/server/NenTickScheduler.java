package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Um unico laco server-side que entrega tick a todos os subsistemas de Nen. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class NenTickScheduler {

    private static final Despachante<ServerPlayer> CENTRAL = new Despachante<>();

    private NenTickScheduler() {
    }

    /**
     * Registra um subsistema sem criar outro event handler ou outra varredura de
     * jogadores. Fechar o registro o remove do scheduler.
     */
    public static Registro registrar(NenTickSubsystem subsistema) {
        Objects.requireNonNull(subsistema, "subsistema");
        return CENTRAL.registrar(subsistema::serverTick);
    }

    /** Executa uma vez por tick do servidor, depois do trabalho vanilla. */
    @SubscribeEvent
    public static void aoFimDoTick(ServerTickEvent.Post evento) {
        for (ServerPlayer jogador : evento.getServer().getPlayerList().getPlayers()) {
            CENTRAL.executar(jogador, NenRuntimeService.estadoDe(jogador));
        }
    }

    /** Registro removivel, util tambem para descarregar integracoes opcionais. */
    @FunctionalInterface
    public interface Registro extends AutoCloseable {
        @Override
        void close();
    }

    /** Nucleo generico deixa o despacho ser provado sem fabricar ServerPlayer. */
    static final class Despachante<J> {
        private final CopyOnWriteArrayList<BiConsumer<J, RuntimeNenState>> subsistemas =
                new CopyOnWriteArrayList<>();

        Registro registrar(BiConsumer<J, RuntimeNenState> subsistema) {
            Objects.requireNonNull(subsistema, "subsistema");
            this.subsistemas.add(subsistema);
            return () -> this.subsistemas.remove(subsistema);
        }

        void executar(J jogador, RuntimeNenState estado) {
            Objects.requireNonNull(jogador, "jogador");
            Objects.requireNonNull(estado, "estado");
            for (BiConsumer<J, RuntimeNenState> subsistema : this.subsistemas) {
                subsistema.accept(jogador, estado);
            }
        }
    }
}
