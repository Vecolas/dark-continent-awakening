package com.darkcontinent.nenfoundation.network.handler;

import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido.Motivo;
import com.darkcontinent.nenfoundation.network.handler.ValidacaoDePedido.Recusa;
import com.darkcontinent.nenfoundation.network.payload.FeedbackDeErroS2C;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A barreira roda na thread de rede ANTES de enqueueWork. So o trabalho
 * admitido consulta jogador/mundo na thread principal. A sessao nasce no
 * login e morre no logout; pacotes atrasados nao a recriam.
 */
public final class PedidosC2S {
    private static final Logger LOG = LoggerFactory.getLogger(PedidosC2S.class);
    private static final Map<Connection, LimiteDePedidos> SESSOES = new ConcurrentHashMap<>();

    private PedidosC2S() { }

    public static void iniciar(Connection conexao) {
        LimiteDePedidos anterior = SESSOES.put(conexao, new LimiteDePedidos(System.nanoTime()));
        if (anterior != null) anterior.encerrar();
    }

    public static void invalidar(Connection conexao) {
        LimiteDePedidos limite = SESSOES.get(conexao);
        if (limite != null) limite.invalidar();
    }

    public static void encerrar(Connection conexao) {
        LimiteDePedidos limite = SESSOES.remove(conexao);
        if (limite != null) {
            limite.encerrar();
            if (NenConfig.devModeAtivo()) {
                LOG.info("C2S: {} pedidos admitidos, {} cortados antes da fila.",
                        limite.aceitos(), limite.recusados());
            }
        }
    }

    public static void limpar() {
        SESSOES.values().forEach(LimiteDePedidos::encerrar);
        SESSOES.clear();
    }

    public static void receber(CustomPacketPayload pedido, IPayloadContext contexto,
            BiFunction<ServerPlayer, CustomPacketPayload, Recusa> validar) {
        try {
            receber(pedido, contexto, validar, NenConfig.pedidosPorSegundo());
        } catch (RuntimeException erro) {
            falhar(contexto, erro);
        }
    }

    static void receber(CustomPacketPayload pedido, IPayloadContext contexto,
            BiFunction<ServerPlayer, CustomPacketPayload, Recusa> validar, int cota) {
        LimiteDePedidos limite = SESSOES.get(contexto.connection());
        if (limite == null) {
            recusar(contexto, Motivo.ESTADO_INVALIDO);
            return;
        }
        long geracao;
        try {
            geracao = limite.admitir(System.nanoTime(), cota);
        } catch (RuntimeException erro) {
            falhar(contexto, erro);
            return;
        }
        if (geracao < 0) {
            // Feedback direto na conexao: nem o aviso de spam entra na fila do jogo.
            recusar(contexto, Motivo.LIMITE);
            return;
        }
        try {
            contexto.enqueueWork(() -> {
                try {
                    if (!limite.atual(geracao) || !contexto.connection().isConnected()
                            || !(contexto.player() instanceof ServerPlayer jogador)
                            || jogador.connection != contexto.listener()) {
                        recusar(contexto, Motivo.ESTADO_INVALIDO);
                        return;
                    }
                    Recusa rejeicao = validar.apply(jogador, pedido);
                    if (rejeicao != null) recusar(contexto, rejeicao);
                } catch (RuntimeException erro) {
                    falhar(contexto, erro);
                }
            }).whenComplete((ignorado, erro) -> {
                limite.concluir();
                if (erro != null) falhar(contexto, erro);
            });
        } catch (RuntimeException erro) {
            limite.concluir();
            falhar(contexto, erro);
        }
    }

    private static void recusar(IPayloadContext contexto, Motivo motivo) {
        recusar(contexto, Recusa.de(motivo));
    }

    /**
     * Recusa com a chave que a origem escreveu.
     *
     * <p>A CHAVE VEM DE QUEM RECUSOU, e nao e reconstruida aqui. Traduzir toda
     * recusa num motivo generico joga fora o texto que a tecnica escolheu -- e
     * o jogador de mao vazia passa a ler "seu estado atual nao permite esse
     * pedido", que nao diz onde esta o problema.
     */
    private static void recusar(IPayloadContext contexto, Recusa recusa) {
        if (!contexto.connection().isConnected()) return;
        try {
            contexto.reply(new FeedbackDeErroS2C(recusa.chave()));
        } catch (RuntimeException erro) {
            LOG.error("Falha ao enviar recusa C2S.", erro);
            contexto.disconnect(Component.translatable(Motivo.ERRO_INTERNO.chave()));
        }
    }

    private static void falhar(IPayloadContext contexto, Throwable erro) {
        LOG.error("Falha no tratamento C2S; pedido nao executado.", erro);
        recusar(contexto, Motivo.ERRO_INTERNO);
    }
}
