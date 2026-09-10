package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import com.darkcontinent.nenfoundation.network.payload.SnapshotDePerfilS2C;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ponto server-side de envio do estado S2C. O dono e sempre o unico destino.
 *
 * <p>Este servico monta o snapshot a partir da leitura migrada do perfil e o
 * delta a partir do runtime atual. Nenhum valor e aceito do cliente, e nenhum
 * envio e feito para espectadores: a decisao de privacidade esta neste ponto.
 * Ver ADR-001 e ADR-002.
 */
public final class NenSyncService {

    private NenSyncService() {
    }

    /** Envia ao jogador seu perfil visivel, imediatamente apos o login. */
    public static void enviarSnapshot(ServerPlayer dono) {
        PersistentNenData perfil = NenProfileService.ler(dono);
        enviarSnapshot(dono, perfil);
    }

    /**
     * Envia o delta do runtime atual ao dono. {@code auraMaxima} vem do
     * consumidor que conhece a formula do M2; este servico nao cria uma segunda
     * fonte para esse numero.
     */
    public static void enviarDelta(ServerPlayer dono, double auraMaxima) {
        RuntimeNenState estado = NenRuntimeService.estadoDe(dono);
        entregarAoDono(dono, criarDelta(estado, auraMaxima), PacketDistributor::sendToPlayer);
    }

    static void enviarSnapshot(ServerPlayer dono, PersistentNenData perfil) {
        entregarAoDono(dono, criarSnapshot(perfil), PacketDistributor::sendToPlayer);
    }

    static SnapshotDePerfilS2C criarSnapshot(PersistentNenData perfil) {
        Objects.requireNonNull(perfil, "perfil");
        return new SnapshotDePerfilS2C(
                perfil.categoriaVisivel(),
                Set.copyOf(perfil.unlockedTechniques()),
                Set.copyOf(perfil.unlockedAbilities()),
                Set.copyOf(perfil.progressionFlags()));
    }

    static DeltaDeRuntimeS2C criarDelta(RuntimeNenState estado, double auraMaxima) {
        Objects.requireNonNull(estado, "estado");
        return new DeltaDeRuntimeS2C(
                (float) estado.auraAtual(),
                (float) auraMaxima,
                Set.copyOf(estado.tecnicasAtivas()),
                Map.copyOf(estado.cooldowns()));
    }

    /**
     * Contrato pequeno de entrega: uma chamada, um destino. Mantê-lo separado
     * permite provar a regra de roteamento sem fabricar uma conexão Minecraft.
     */
    static <D> void entregarAoDono(
            D dono,
            CustomPacketPayload payload,
            BiConsumer<D, CustomPacketPayload> transporte) {
        Objects.requireNonNull(dono, "dono");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(transporte, "transporte");
        transporte.accept(dono, payload);
    }
}
