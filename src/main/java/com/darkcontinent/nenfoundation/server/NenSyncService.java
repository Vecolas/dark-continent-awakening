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

    private static final NenSyncMetrics METRICAS = new NenSyncMetrics();

    private NenSyncService() {
    }

    /**
     * Se a conexao deste jogador consegue receber o payload.
     *
     * <p>POR QUE ESTA CONFERENCIA EXISTE. {@code PacketDistributor.sendToPlayer}
     * LANCA {@code UnsupportedOperationException} quando a conexao nao negociou
     * o canal do mod. Como o envio acontece no evento de login, a excecao sobe
     * de dentro do tratamento de entrada do jogador.
     *
     * <p>Isso nao e hipotese: os quatro primeiros gametests do projeto
     * reprovaram com
     * {@code Payload nenfoundation:nen_profile_snapshot may not be sent to the client!}
     * ao criar um jogador de teste. Qualquer jogador cuja conexao nao tenha
     * negociado o canal -- teste, FakePlayer de outro mod, conexao ja em
     * encerramento -- produz o mesmo.
     *
     * <p>Nao receber o snapshot NAO dessincroniza ninguem: um cliente que nao
     * tem o canal tambem nao tem o mod, e nao ha HUD para ficar desatualizado.
     * Entao pular e a resposta correta, e nao um contorno.
     */
    private static boolean consegueReceber(ServerPlayer dono, CustomPacketPayload payload) {
        return consegueReceber(dono, payload.type());
    }

    private static boolean consegueReceber(
            ServerPlayer dono, CustomPacketPayload.Type<?> tipo) {
        return dono.connection != null && dono.connection.hasChannel(tipo);
    }

    /** Envia ao dono seu perfil visivel; login e mutacao usam o mesmo transporte. */
    public static void enviarSnapshot(ServerPlayer dono) {
        PersistentNenData perfil = NenProfileService.ler(dono);
        enviarSnapshot(dono, perfil);
    }

    /** Entrega de verdade, pulando quem nao pode receber. */
    private static boolean entregarSePuder(ServerPlayer dono, CustomPacketPayload payload) {
        if (consegueReceber(dono, payload)) {
            entregarAoDono(dono, payload, PacketDistributor::sendToPlayer);
            return true;
        }
        return false;
    }

    /**
     * Envia o delta do runtime atual ao dono. {@code auraMaxima} vem do
     * consumidor que conhece a formula do M2; este servico nao cria uma segunda
     * fonte para esse numero.
     */
    public static void enviarDelta(ServerPlayer dono, double auraMaxima) {
        RuntimeNenState estado = NenRuntimeService.estadoDe(dono);
        if (entregarSePuder(dono, criarDelta(estado, auraMaxima))) {
            METRICAS.registrarDeltaEnviado();
        }
    }

    /**
     * Envia somente quando o runtime observou uma mudanca de aura.
     *
     * @return {@code true} quando um delta foi construido e a marca foi
     *     consumida; {@code false} em tick limpo ou conexao sem canal.
     */
    public static boolean enviarDeltaSeAuraSuja(ServerPlayer dono, double auraMaxima) {
        RuntimeNenState estado = NenRuntimeService.estadoDe(dono);
        if (!estado.auraSuja() || !consegueReceber(dono, DeltaDeRuntimeS2C.TYPE)) {
            return false;
        }
        if (!entregarSePuder(dono, criarDelta(estado, auraMaxima))) {
            return false;
        }
        METRICAS.registrarDeltaEnviado();
        estado.marcarAuraSincronizada();
        return true;
    }

    public static NenSyncMetrics metricas() {
        return METRICAS;
    }

    static void limparMetricas() {
        METRICAS.limpar();
    }

    static void enviarSnapshot(ServerPlayer dono, PersistentNenData perfil) {
        entregarSePuder(dono, criarSnapshot(perfil));
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
