package com.darkcontinent.nenfoundation.network.handler;

import com.darkcontinent.nenfoundation.network.payload.*;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** O contexto simula apenas transporte/fila, nao a integracao do NeoForge. */
class PedidosC2STest {
    private final List<Runnable> fila = new ArrayList<>();
    private final List<FeedbackDeErroS2C> respostas = new ArrayList<>();
    private final Connection conexao = new Connection(PacketFlow.SERVERBOUND) {
        @Override public boolean isConnected() { return true; }
    };
    private boolean falharAoEnfileirar;

    private final IPayloadContext contexto = (IPayloadContext) Proxy.newProxyInstance(
            IPayloadContext.class.getClassLoader(), new Class<?>[]{IPayloadContext.class},
            (proxy, metodo, args) -> switch (metodo.getName()) {
                case "connection" -> conexao;
                case "reply" -> { respostas.add((FeedbackDeErroS2C) args[0]); yield null; }
                case "player" -> null;
                case "enqueueWork" -> {
                    if (falharAoEnfileirar) throw new IllegalStateException("falha de teste");
                    var futuro = new CompletableFuture<Void>();
                    fila.add(() -> {
                        try { ((Runnable) args[0]).run(); futuro.complete(null); }
                        catch (Throwable erro) { futuro.completeExceptionally(erro); }
                    });
                    yield futuro;
                }
                default -> throw new AssertionError("chamada inesperada: " + metodo.getName());
            });

    private List<CustomPacketPayload> pedidos() {
        var id = ResourceLocation.fromNamespaceAndPath("nenfoundation", "teste");
        return List.of(new AtivarTecnicaC2S(id), new DesativarTecnicaC2S(id),
                new AtivarHabilidadeC2S(id, 0, OptionalInt.empty(), Optional.empty()));
    }

    private void receber(CustomPacketPayload pedido) {
        PedidosC2S.receber(pedido, contexto, (jogador, valor) -> {
            fail("estado invalido nao pode alcancar dominio");
            return ValidacaoDePedido.Motivo.ERRO_INTERNO;
        }, 20);
    }

    @AfterEach void limpar() { PedidosC2S.limpar(); }

    @Test void tresPedidosCompartilhamBarreiraAntesDaFila() {
        PedidosC2S.iniciar(conexao);
        var pedidos = pedidos();
        for (int i = 0; i < 500; i++) receber(pedidos.get(i % pedidos.size()));
        assertEquals(8, fila.size());
        assertEquals(492, respostas.size());
        assertTrue(respostas.stream().allMatch(r -> r.chaveDeTraducao().equals(
                ValidacaoDePedido.Motivo.LIMITE.chave())));
        fila.forEach(Runnable::run);
        assertEquals(500, respostas.size());
    }

    @Test void sessaoEncerradaNaoERecriadaPorPacoteAtrasado() {
        PedidosC2S.iniciar(conexao);
        receber(pedidos().getFirst());
        PedidosC2S.limpar();
        fila.getFirst().run();
        receber(pedidos().getFirst());
        assertEquals(1, fila.size());
        assertEquals(2, respostas.size());
        assertTrue(respostas.stream().allMatch(r -> r.chaveDeTraducao().equals(
                ValidacaoDePedido.Motivo.ESTADO_INVALIDO.chave())));
    }

    @Test void excecaoDeAgendamentoTemFeedbackESoltaVaga() {
        PedidosC2S.iniciar(conexao);
        falharAoEnfileirar = true;
        for (int i = 0; i < 10; i++) receber(pedidos().getFirst());
        assertEquals(10, respostas.size());
        assertTrue(respostas.stream().allMatch(r -> r.chaveDeTraducao().equals(
                ValidacaoDePedido.Motivo.ERRO_INTERNO.chave())));
        falharAoEnfileirar = false;
        receber(pedidos().getFirst());
        assertEquals(1, fila.size());
    }
}
