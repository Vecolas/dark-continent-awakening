package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.nen.aura.MotorDeAura;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import com.darkcontinent.nenfoundation.server.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Exerce registro real do scheduler, config carregada, perfil e transporte por
 * 120 ticks. Listener captura envelopes; rede fisica e HUD exigem QA com cliente.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenAuraGameTest {
    @GameTest(template = "empty", timeoutTicks = 160)
    @PrefixGameTestTemplate(false)
    public static void schedulerRegeneraESincronizaSemPacoteLimpo(GameTestHelper helper) {
        // Apenas jogadores isolados do arnes: nenhum perfil de usuario e preparado aqui.
        ServerPlayer dono = helper.makeMockServerPlayerInLevel();
        ServerPlayer outro = helper.makeMockServerPlayerInLevel();
        var original = dono.connection;
        var originalOutro = outro.connection;
        var captura = new Captura(dono);
        var capturaOutro = new Captura(outro);
        NenRuntimeService.iniciarSessao(dono);
        NenRuntimeService.iniciarSessao(outro);
        NenProfileService.atualizar(dono, p -> new PersistentNenData(p.schemaVersion(), true,
                p.category(), p.categoryRevealed(), p.auraPotential(), p.control(), p.output(),
                p.techniqueProficiency(), p.unlockedTechniques(), p.unlockedAbilities(), p.progressionFlags()));
        var estado = NenAuraService.consultar(dono);
        estado.definirAuraAtual(estado.auraMaxima());
        for (int tick = 1; tick <= 120; tick++) {
            int agora = tick;
            helper.runAtTickTime(tick, () -> {
                try {
                    // O helper colocou os jogadores na PlayerList: somente o evento
                    // global pode entrega-los ao motor. Chamar aqui esconderia registro ausente.
                    if (agora == 25) {
                        helper.assertTrue(captura.deltas.size() == 1, "scheduler nao enviou inicial ou enviou ticks limpos");
                        helper.assertTrue(capturaOutro.deltas.size() == 1, "sessao neutra nao recebeu seu zero");
                        helper.assertTrue(capturaOutro.deltas.getFirst().auraMaxima() == 0, "aura vazou para outro jogador");
                        helper.assertTrue(NenAuraService.gastar(dono, Double.NaN) == MotorDeAura.Gasto.INVALIDO,
                                "servidor aceitou gasto NaN");
                        double custo = Math.min(NenAuraService.output(dono), NenConfig.auraRegeneracaoPorSegundo());
                        helper.assertTrue(custo > 0, "fixture exige regeneracao e output positivos");
                        helper.assertTrue(NenAuraService.gastar(dono, custo) == MotorDeAura.Gasto.PERMITIDO,
                                "gasto legitimo recusado");
                    }
                    if (agora == 60) {
                        helper.assertTrue(captura.deltas.size() > 1, "nenhum delta apos gasto: motor desconectado");
                        helper.assertTrue(estado.auraAtual() == estado.auraMaxima(), "regeneracao nao alcancou teto");
                        captura.estaveis = captura.deltas.size();
                    }
                    if (agora == 90) {
                        helper.assertTrue(captura.deltas.size() == captura.estaveis, "pool cheio gerou pacotes");
                        captura.canal = false;
                        estado.definirAuraAtual(0);
                    }
                    if (agora == 100) {
                        helper.assertTrue(estado.auraSuja(), "canal ausente consumiu marca");
                        captura.canal = true;
                    }
                    if (agora == 110) {
                        helper.assertTrue(captura.deltas.size() > captura.estaveis, "canal voltou sem novo delta");
                        NenRuntimeService.reiniciar(dono);
                    }
                    if (agora == 120) {
                        helper.assertTrue(NenRuntimeService.estadoDe(dono) != estado, "reset conservou runtime antigo");
                        helper.assertTrue(captura.deltas.getLast().aura() < estado.auraAtual(), "reset nao chegou ao cliente");
                        helper.succeed();
                    }
                } finally {
                    if (agora == 120) {
                        dono.serverLevel().getServer().getPlayerList().remove(dono);
                        outro.serverLevel().getServer().getPlayerList().remove(outro);
                        NenRuntimeService.encerrarSessao(dono);
                        NenRuntimeService.encerrarSessao(outro);
                        dono.connection = original;
                        outro.connection = originalOutro;
                    }
                }
            });
        }
    }

    private static final class Captura extends ServerGamePacketListenerImpl {
        private final List<DeltaDeRuntimeS2C> deltas = new ArrayList<>();
        private boolean canal = true;
        private int estaveis;
        Captura(ServerPlayer jogador) {
            super(jogador.serverLevel().getServer(), jogador.connection.getConnection(), jogador,
                    CommonListenerCookie.createInitial(jogador.getGameProfile(), false));
        }
        @Override public boolean hasChannel(ResourceLocation id) { return this.canal; }
        @Override public void send(Packet<?> pacote) {
            if (pacote instanceof ClientboundCustomPayloadPacket envelope
                    && envelope.payload() instanceof DeltaDeRuntimeS2C delta) this.deltas.add(delta);
        }
    }
}
