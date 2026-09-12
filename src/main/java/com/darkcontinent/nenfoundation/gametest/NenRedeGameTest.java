package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido.Motivo;
import com.darkcontinent.nenfoundation.network.payload.*;
import com.darkcontinent.nenfoundation.server.NenPedidoService;
import com.darkcontinent.nenfoundation.server.NenProfileService;
import com.darkcontinent.nenfoundation.server.NenSyncService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Perfil/jogador reais, transporte capturado no listener. Prova a chamada real
 * de envio e seu destino, mas nao handshake, socket nem renderizacao do cliente.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class NenRedeGameTest {
    private static final ResourceLocation ID = NenFoundation.id("teste_rede");

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void mutacaoReenviaSoAoDono(GameTestHelper helper) {
        ServerPlayer dono = helper.makeMockServerPlayerInLevel();
        ServerPlayer outro = helper.makeMockServerPlayerInLevel();
        var anterior = dono.connection;
        var anteriorOutro = outro.connection;
        try {
            var captura = new Captura(dono);
            var capturaOutro = new Captura(outro);
            NenSyncService.enviarSnapshot(dono);
            helper.assertTrue(captura.snapshots.size() == 1, "snapshot inicial nao saiu");
            NenProfileService.atualizar(dono, NenRedeGameTest::comTecnica);
            helper.assertTrue(captura.snapshots.size() == 2, "mutacao nao enviou segundo snapshot");
            helper.assertTrue(captura.snapshots.get(1).tecnicasDesbloqueadas().contains(ID), "snapshot velho");
            NenProfileService.atualizar(dono, NenRedeGameTest::comTecnica);
            helper.assertTrue(captura.snapshots.size() == 2, "no-op enviou pacote");
            helper.assertTrue(capturaOutro.snapshots.isEmpty(), "snapshot vazou para outro jogador");
            captura.canal = false;
            NenProfileService.atualizar(dono, perfil -> PersistentNenData.NAO_DESPERTADO);
            helper.assertTrue(captura.snapshots.size() == 2, "hasChannel foi ignorado");
            helper.assertTrue(NenProfileService.ler(dono).equals(PersistentNenData.NAO_DESPERTADO),
                    "ausencia de canal impediu persistencia");
            helper.succeed();
        } finally {
            dono.connection = anterior;
            outro.connection = anteriorOutro;
        }
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void pedidosNaoCriamTecnicasNemAceitamAlvoForjado(GameTestHelper helper) {
        ServerPlayer jogador = helper.makeMockServerPlayerInLevel();
        for (var pedido : List.of(new AtivarTecnicaC2S(ID), new DesativarTecnicaC2S(ID),
                new AtivarHabilidadeC2S(ID, 0, OptionalInt.empty(), Optional.empty()))) {
            helper.assertTrue(Motivo.NAO_DESPERTO.chave().equals(chaveDe(NenPedidoService.validar(jogador, pedido))),
                    "pedido passou para jogador nao desperto");
        }
        NenProfileService.atualizar(jogador, perfil -> {
            var novo = comTecnica(perfil);
            return new PersistentNenData(novo.schemaVersion(), true, novo.category(), false,
                    0, 0, 0, novo.techniqueProficiency(), novo.unlockedTechniques(), Set.of(ID), Set.of());
        });
        helper.assertTrue(Motivo.ID_INEXISTENTE.chave().equals(chaveDe(NenPedidoService.validar(jogador, new AtivarTecnicaC2S(ID)))),
                "unlock de debug foi tratado como registro executavel");
        helper.assertTrue(chaveDe(NenPedidoService.validar(jogador,
                new AtivarHabilidadeC2S(ID, 0, OptionalInt.of(Integer.MAX_VALUE), Optional.empty())))
                .equals(Motivo.ALVO_INVALIDO.chave()), "alvo inexistente aceito");
        helper.assertTrue(chaveDe(NenPedidoService.validar(jogador,
                new AtivarHabilidadeC2S(ID, 0, OptionalInt.empty(), Optional.of(new Vec3(Double.NaN, 0, 0)))))
                .equals(Motivo.ALVO_INVALIDO.chave()), "posicao NaN aceita");
        helper.succeed();
    }

    private static PersistentNenData comTecnica(PersistentNenData antes) {
        return new PersistentNenData(antes.schemaVersion(), antes.awakened(), antes.category(),
                antes.categoryRevealed(), antes.auraPotential(), antes.control(), antes.output(),
                antes.techniqueProficiency(), Set.of(ID), antes.unlockedAbilities(), antes.progressionFlags());
    }

    private static final class Captura extends ServerGamePacketListenerImpl {
        private final List<SnapshotDePerfilS2C> snapshots = new ArrayList<>();
        private boolean canal = true;

        Captura(ServerPlayer jogador) {
            super(jogador.serverLevel().getServer(), jogador.connection.getConnection(), jogador,
                    CommonListenerCookie.createInitial(jogador.getGameProfile(), false));
        }

        @Override public boolean hasChannel(ResourceLocation id) { return this.canal; }

        @Override public void send(Packet<?> pacote) {
            if (pacote instanceof ClientboundCustomPayloadPacket envelope
                    && envelope.payload() instanceof SnapshotDePerfilS2C snapshot) {
                this.snapshots.add(snapshot);
            }
        }
    }

    /** A chave da recusa, ou nulo quando o pedido foi aceito. */
    private static String chaveDe(
            com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido.Recusa r) {
        return r == null ? null : r.chave();
    }
}
