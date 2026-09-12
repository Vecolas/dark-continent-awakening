package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/** Porta unica para o estado de combate em memoria dos jogadores conectados. */
public final class NenRuntimeService {

    /**
     * A chave e o UUID, nunca o ServerPlayer. Assim o registro nao conserva uma
     * entidade removida por uma referencia forte esquecida.
     */
    private static final Map<UUID, RuntimeNenState> ESTADOS = new HashMap<>();

    private NenRuntimeService() {
    }

    /** Inicia uma sessao limpa no login, descartando qualquer resto defensivamente. */
    public static RuntimeNenState iniciarSessao(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador");
        return iniciarSessao(jogador.getUUID());
    }

    /** Reinicia o combate em morte, volta do End ou mudanca de dimensao. */
    public static RuntimeNenState reiniciar(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador");
        return iniciarSessao(jogador.getUUID());
    }

    /** Retorna o estado criado pelo ciclo de vida; ausencia e erro de integracao. */
    public static RuntimeNenState estadoDe(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador");
        return estadoDe(jogador.getUUID());
    }

    /** Remove todo estado do jogador assim que ele deixa o servidor. */
    public static void encerrarSessao(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador");
        encerrarSessao(jogador.getUUID());
    }

    static RuntimeNenState iniciarSessao(UUID jogadorId) {
        Objects.requireNonNull(jogadorId, "jogadorId");
        NenSyncService.encerrarSessao(jogadorId);
        NenAuraService.encerrarSessao(jogadorId);
        RuntimeNenState novo = new RuntimeNenState();
        ESTADOS.put(jogadorId, novo);

        // O TETO DE REPOUSO PRECISA VALER DESDE O PRIMEIRO TICK. Um runtime
        // recem-criado nasce com o teto ABSOLUTO; sem esta chamada o jogador
        // comecaria a sessao com o teto de quem esta em Ren, e so voltaria ao
        // normal depois de ligar e desligar alguma tecnica -- sem erro nenhum.
        NenTechniqueService.recalcularDerivados(novo, NenGyoService.regiaoDe(jogadorId));
        return novo;
    }

    static RuntimeNenState estadoDe(UUID jogadorId) {
        Objects.requireNonNull(jogadorId, "jogadorId");
        RuntimeNenState estado = ESTADOS.get(jogadorId);
        if (estado == null) {
            throw new IllegalStateException("jogador sem RuntimeNenState ativo");
        }
        return estado;
    }

    static void encerrarSessao(UUID jogadorId) {
        ESTADOS.remove(Objects.requireNonNull(jogadorId, "jogadorId"));
        NenSyncService.encerrarSessao(jogadorId);
        NenAuraService.encerrarSessao(jogadorId);
    }

    static int quantidadeDeSessoes() {
        return ESTADOS.size();
    }

    static void encerrarTodasAsSessoes() {
        for (UUID id : Set.copyOf(ESTADOS.keySet())) encerrarSessao(id);
        ESTADOS.clear();
    }
}
