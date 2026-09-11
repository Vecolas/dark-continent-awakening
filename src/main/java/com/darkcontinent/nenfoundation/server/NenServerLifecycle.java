package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.network.handler.PedidosC2S;
import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.nen.technique.RegistroDeTecnicas;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** Limpa estado estatico quando uma instancia de servidor termina. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class NenServerLifecycle {
    private static NenTickScheduler.Registro registroDeAura;
    private static NenTickScheduler.Registro registroDeTecnicas;

    private NenServerLifecycle() {
    }

    @SubscribeEvent
    public static void aoIniciarServidor(ServerAboutToStartEvent evento) {
        // AS TECNICAS SAO SELADAS AQUI, uma vez por servidor. O selamento
        // confere a simetria das exclusoes e RECUSA um registro torto -- e
        // recusar na subida e o ponto: uma exclusao pela metade descoberta em
        // jogo ja e uma combinacao ilegal que alguem usou.
        NenTechniqueService.instalar(RegistroDeTecnicas.selar(List.of(
                new Ten(NenConfig::tenCustoPorSegundo,
                        NenConfig::tenMultiplicadorDeRegeneracao))));

        if (registroDeAura == null) registroDeAura = NenTickScheduler.registrar(NenAuraService::tick);
        // AURA PRIMEIRO, TECNICA DEPOIS, e a ordem importa: a tecnica gasta a
        // aura que o motor acabou de regenerar neste mesmo tick. Invertida, a
        // tecnica decidiria com o saldo do tick anterior -- e o erro apareceria
        // so como "as vezes Ren desliga uma fracao de segundo antes".
        if (registroDeTecnicas == null) {
            registroDeTecnicas = NenTickScheduler.registrar(NenTechniqueService::tick);
        }
    }

    @SubscribeEvent
    public static void aoEncerrarServidor(ServerStoppedEvent evento) {
        if (registroDeTecnicas != null) {
            registroDeTecnicas.close();
            registroDeTecnicas = null;
        }
        if (registroDeAura != null) {
            registroDeAura.close();
            registroDeAura = null;
        }
        NenRuntimeService.encerrarTodasAsSessoes();
        PedidosC2S.limpar();
        NenSyncService.limparMetricas();
    }
}
