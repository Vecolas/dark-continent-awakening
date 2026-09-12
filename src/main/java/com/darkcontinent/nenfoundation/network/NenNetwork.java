package com.darkcontinent.nenfoundation.network;

import com.darkcontinent.nenfoundation.network.handler.Recebedores;
import com.darkcontinent.nenfoundation.network.handler.PedidosC2S;
import com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido.Motivo;
import com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido.Recusa;
import com.darkcontinent.nenfoundation.network.payload.AtivarTecnicaC2S;
import com.darkcontinent.nenfoundation.network.payload.DesativarTecnicaC2S;
import com.darkcontinent.nenfoundation.network.payload.AjustarOutputC2S;
import com.darkcontinent.nenfoundation.network.payload.AtivarHabilidadeC2S;
import java.util.function.BiFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.HandlerThread;
import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import com.darkcontinent.nenfoundation.network.payload.FeedbackDeErroS2C;
import com.darkcontinent.nenfoundation.network.payload.PresencaDeAuraS2C;
import com.darkcontinent.nenfoundation.network.payload.FxDeHabilidadeS2C;
import com.darkcontinent.nenfoundation.network.payload.SnapshotDePerfilS2C;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * O registro dos payloads no NeoForge.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. A VERSAO DO PROTOCOLO E O HANDSHAKE. {@code event.registrar(versao)}
 * faz o proprio NeoForge recusar a conexao quando cliente e servidor declaram
 * versoes diferentes. Nao ha handshake escrito a mao: escrever um seria uma
 * segunda fonte para a mesma verdade, e a que vale seria a do loader. A versao
 * sai de {@link NenProtocol#VERSION}, que e o mesmo numero conferido contra o
 * documento por portao.
 *
 * <p>2. O handler nao conhece o cliente. Ele fala com
 * {@link Recebedores#atual()}, um PAPEL. O registro roda tambem no servidor
 * dedicado, e importar aqui a classe do cache de cliente derrubaria o servidor
 * com {@code NoClassDefFoundError} — sem falhar na compilacao, em
 * singleplayer, nem em {@code runClient}.
 *
 * <p>3. S2C usa a thread principal padrao. C2S usa NETWORK explicitamente
 * para cortar spam antes de enqueueWork; a validacao de jogo e injetada pela
 * composicao do mod, preservando a direcao server -> network.
 *
 * <p>ARQUIVO HOSTIL A MERGE: uma pessoa por vez.
 */
public final class NenNetwork {

    private NenNetwork() {
    }

    public static void registrar(RegisterPayloadHandlersEvent evento,
            BiFunction<ServerPlayer, CustomPacketPayload, Recusa> validar) {
        PayloadRegistrar registrar = evento.registrar(String.valueOf(NenProtocol.VERSION));

        registrar.playToClient(
                SnapshotDePerfilS2C.TYPE,
                SnapshotDePerfilS2C.STREAM_CODEC,
                (payload, contexto) -> contexto.enqueueWork(
                        () -> Recebedores.atual().aoReceberSnapshot(payload)));

        registrar.playToClient(
                DeltaDeRuntimeS2C.TYPE,
                DeltaDeRuntimeS2C.STREAM_CODEC,
                (payload, contexto) -> contexto.enqueueWork(
                        () -> Recebedores.atual().aoReceberDelta(payload)));

        registrar.playToClient(
                FxDeHabilidadeS2C.TYPE,
                FxDeHabilidadeS2C.STREAM_CODEC,
                (payload, contexto) -> contexto.enqueueWork(
                        () -> Recebedores.atual().aoReceberFx(payload)));

        registrar.playToClient(
                FeedbackDeErroS2C.TYPE,
                FeedbackDeErroS2C.STREAM_CODEC,
                (payload, contexto) -> contexto.enqueueWork(
                        () -> Recebedores.atual().aoReceberErro(payload)));

        registrar.playToClient(
                PresencaDeAuraS2C.TYPE,
                PresencaDeAuraS2C.STREAM_CODEC,
                (payload, contexto) -> contexto.enqueueWork(
                        () -> Recebedores.atual().aoReceberPresenca(payload)));

        PayloadRegistrar entrada = registrar.executesOn(HandlerThread.NETWORK);
        entrada.playToServer(AtivarTecnicaC2S.TYPE, AtivarTecnicaC2S.STREAM_CODEC,
                (pedido, contexto) -> PedidosC2S.receber(pedido, contexto, validar));
        entrada.playToServer(AjustarOutputC2S.TYPE, AjustarOutputC2S.STREAM_CODEC,
                (pedido, contexto) -> PedidosC2S.receber(pedido, contexto, validar));
        entrada.playToServer(DesativarTecnicaC2S.TYPE, DesativarTecnicaC2S.STREAM_CODEC,
                (pedido, contexto) -> PedidosC2S.receber(pedido, contexto, validar));
        entrada.playToServer(AtivarHabilidadeC2S.TYPE, AtivarHabilidadeC2S.STREAM_CODEC,
                (pedido, contexto) -> PedidosC2S.receber(pedido, contexto, validar));
    }
}
