package com.darkcontinent.nenfoundation.network.handler;

import com.darkcontinent.nenfoundation.network.payload.AtivarHabilidadeC2S;
import com.darkcontinent.nenfoundation.network.payload.AtivarTecnicaC2S;
import com.darkcontinent.nenfoundation.network.payload.DesativarTecnicaC2S;
import com.darkcontinent.nenfoundation.network.payload.FeedbackDeErroS2C;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import com.darkcontinent.nenfoundation.server.NenProfileService;
import com.darkcontinent.nenfoundation.server.NenRuntimeService;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entrada unica dos pedidos cliente-servidor. O cliente manda intencao; aqui o servidor decide. */
public final class NenC2SHandlers {

    private static final Logger LOG = LoggerFactory.getLogger(NenC2SHandlers.class);
    private static final C2SRateLimiter LIMITE = new C2SRateLimiter();

    private NenC2SHandlers() {
    }

    public static void aoAtivarTecnica(AtivarTecnicaC2S pedido, IPayloadContext contexto) {
        executar(contexto, jogador -> {
            if (!permitido(jogador)) {
                return "nen.error.rate_limited";
            }
            PersistentNenData perfil = NenProfileService.ler(jogador);
            RuntimeNenState estado = NenRuntimeService.estadoDe(jogador);
            String erro = C2SValidacao.tecnica(perfil, estado, pedido.tecnicaId());
            if (erro != null) {
                return erro;
            }
            estado.ativarTecnica(pedido.tecnicaId());
            return null;
        });
    }

    public static void aoDesativarTecnica(DesativarTecnicaC2S pedido, IPayloadContext contexto) {
        executar(contexto, jogador -> {
            if (!permitido(jogador)) {
                return "nen.error.rate_limited";
            }
            PersistentNenData perfil = NenProfileService.ler(jogador);
            RuntimeNenState estado = NenRuntimeService.estadoDe(jogador);
            String erro = C2SValidacao.tecnica(perfil, estado, pedido.tecnicaId());
            if (erro != null) {
                return erro;
            }
            estado.desativarTecnica(pedido.tecnicaId());
            return null;
        });
    }

    public static void aoAtivarHabilidade(AtivarHabilidadeC2S pedido, IPayloadContext contexto) {
        executar(contexto, jogador -> {
            if (!permitido(jogador)) {
                return "nen.error.rate_limited";
            }
            PersistentNenData perfil = NenProfileService.ler(jogador);
            RuntimeNenState estado = NenRuntimeService.estadoDe(jogador);
            String erro = C2SValidacao.habilidade(
                    perfil, estado, pedido.habilidadeId(), pedido.slot());
            if (erro != null) {
                return erro;
            }
            // O executor de habilidades nasce no M5; aceitar aqui seria uma promessa falsa.
            return "nen.error.ability_unavailable";
        });
    }

    public static void limparJogador(ServerPlayer jogador) {
        LIMITE.limpar(Objects.requireNonNull(jogador, "jogador").getUUID());
    }

    static void limparTudo() {
        LIMITE.limparTudo();
    }

    private static boolean permitido(ServerPlayer jogador) {
        return LIMITE.permitido(jogador.getUUID(), jogador.serverLevel().getGameTime());
    }

    private static void executar(IPayloadContext contexto, Operacao operacao) {
        try {
            if (!(contexto.player() instanceof ServerPlayer jogador)) {
                contexto.reply(new FeedbackDeErroS2C("nen.error.server_only"));
                return;
            }
            String erro = operacao.aplicar(jogador);
            if (erro != null) {
                contexto.reply(new FeedbackDeErroS2C(erro));
            }
        } catch (RuntimeException excecao) {
            LOG.error("Falha ao validar pedido C2S de Nen", excecao);
            contexto.reply(new FeedbackDeErroS2C("nen.error.internal"));
        }
    }

    @FunctionalInterface
    private interface Operacao {
        String aplicar(ServerPlayer jogador);
    }
}
