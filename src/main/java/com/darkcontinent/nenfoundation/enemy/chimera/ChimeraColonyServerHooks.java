package com.darkcontinent.nenfoundation.enemy.chimera;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.server.NenTickScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Liga a colonia persistente ao ciclo de vida do servidor. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class ChimeraColonyServerHooks {

    private static final Logger LOG = LoggerFactory.getLogger(ChimeraColonyServerHooks.class);
    private static final SimulacaoOffline SIMULACAO = SimulacaoOffline.padrao();
    private static final ChimeraTrackingBudget ORCAMENTO = ChimeraTrackingBudget.padrao();
    private static final int INTERVALO_DE_SAVE = 20;

    private static NenTickScheduler.Registro registroNoScheduler;
    private static ChimeraColonySavedData dados;
    private static long ultimoSave = Long.MIN_VALUE;

    private ChimeraColonyServerHooks() { }

    @SubscribeEvent
    public static void aoIniciar(ServerStartedEvent evento) {
        MinecraftServer servidor = evento.getServer();
        dados = ChimeraColonySavedData.de(servidor);
        ServerLevel overworld = servidor.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        long tick = overworld.getGameTime();
        int autorizados = dados.recuperarAusencias(tick, SIMULACAO, ORCAMENTO);
        int materializados = ChimeraColonyMaterializer.materializar(overworld, dados);
        dados.sujar();
        ultimoSave = tick;
        registroNoScheduler = NenTickScheduler.registrarDoMundo(
                ChimeraColonyServerHooks::tickDasColonias);
        if (autorizados > 0 || materializados > 0) {
            LOG.info("Colonias recuperaram {} nascimentos autorizados; {} materializados.",
                    autorizados, materializados);
        }
    }

    @SubscribeEvent
    public static void aoParar(ServerStoppedEvent evento) {
        if (registroNoScheduler != null) {
            registroNoScheduler.close();
            registroNoScheduler = null;
        }
        dados = null;
        ultimoSave = Long.MIN_VALUE;
    }

    private static void tickDasColonias(MinecraftServer servidor) {
        ChimeraColonySavedData atual = dados;
        ServerLevel overworld = servidor.getLevel(Level.OVERWORLD);
        if (atual == null || overworld == null) return;

        long tick = overworld.getGameTime();
        boolean mudou = false;
        int materializados = ChimeraColonyMaterializer.materializar(overworld, atual);
        if (materializados > 0) mudou = true;
        for (ChimeraColony colonia : atual.colonias().values()) {
            if (colonia.expirarRelatorios(tick) > 0) mudou = true;
            colonia.marcarTick(tick);
        }
        // O relogio e salvo no mesmo intervalo do ciclo central. Salvar em todo tick
        // faria uma colonia ativa transformar o disco em contador de FPS.
        if (mudou || tick - ultimoSave >= INTERVALO_DE_SAVE) {
            atual.sujar();
            ultimoSave = tick;
        }
    }
}
