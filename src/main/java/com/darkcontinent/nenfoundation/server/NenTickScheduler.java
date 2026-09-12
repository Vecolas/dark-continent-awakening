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

    private static final org.slf4j.Logger LOG =
            com.mojang.logging.LogUtils.getLogger();

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

    /**
     * Executa uma vez por tick do servidor, depois do trabalho vanilla.
     *
     * <p>UM JOGADOR QUEBRADO NAO DERRUBA O SERVIDOR. {@code estadoDe} lanca de
     * proposito quando nao ha sessao -- ausencia de runtime e erro de
     * integracao, e engolir isso em silencio esconderia o defeito. So que este
     * laco roda dentro do tick do servidor: deixar a excecao subir mata o
     * <b>loop inteiro</b>, e o que era um jogador sem sessao vira um crash de
     * servidor para todo mundo que estava online.
     *
     * <p>Entao o erro continua alto -- vai para o log com o nome de quem
     * falhou -- e os outros jogadores seguem tickando. Perder Nen para um e
     * ruim; perder o mundo para todos e outra ordem de problema.
     *
     * <p>Descoberto escrevendo os gametests de ponto de saida: um jogador que
     * continuava na lista depois de a sessao ser encerrada derrubou o servidor
     * de teste com "Exception in server tick loop".
     */
    @SubscribeEvent
    public static void aoFimDoTick(ServerTickEvent.Post evento) {
        for (ServerPlayer jogador : evento.getServer().getPlayerList().getPlayers()) {
            try {
                CENTRAL.executar(jogador, NenRuntimeService.estadoDe(jogador));
            } catch (RuntimeException falha) {
                LOG.error("Tick de Nen falhou para {}; os demais jogadores seguem.",
                        jogador.getGameProfile().getName(), falha);
            }
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
