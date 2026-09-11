package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.network.handler.PedidosC2S;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** Limpa estado estatico quando uma instancia de servidor termina. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class NenServerLifecycle {
    private static NenTickScheduler.Registro registroDeAura;

    private NenServerLifecycle() {
    }

    @SubscribeEvent
    public static void aoIniciarServidor(ServerAboutToStartEvent evento) {
        if (registroDeAura == null) registroDeAura = NenTickScheduler.registrar(NenAuraService::tick);
    }

    @SubscribeEvent
    public static void aoEncerrarServidor(ServerStoppedEvent evento) {
        if (registroDeAura != null) {
            registroDeAura.close();
            registroDeAura = null;
        }
        NenRuntimeService.encerrarTodasAsSessoes();
        PedidosC2S.limpar();
        NenSyncService.limparMetricas();
    }
}
