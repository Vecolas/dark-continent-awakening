package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.data.attachment.NenAttachments;
import com.darkcontinent.nenfoundation.nen.profile.NenProfileMigrator;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.server.level.ServerPlayer;

/**
 * Porta unica de leitura e mutacao do perfil persistente no servidor.
 *
 * <p>Todo acesso passa pelo migrador antes de devolver o dado. Escrever pelo
 * {@link ServerPlayer#setData} substitui o record imutavel no attachment; nao ha
 * referencia mutavel que possa vazar entre jogadores.
 */
public final class NenProfileService {

    private NenProfileService() {
    }

    /** Le e migra o perfil antes de expo-lo a qualquer consumidor. */
    public static PersistentNenData ler(ServerPlayer jogador) {
        Objects.requireNonNull(jogador, "jogador");
        return ler(new AttachmentDoJogador(jogador));
    }

    /**
     * Aplica uma mutacao imutavel e grava o resultado no attachment do mesmo
     * jogador. Mudanca efetiva publica o snapshot depois de persistir; no-op
     * nao publica. Assim todo consumidor, incluindo comandos, herda o sync.
     */
    public static PersistentNenData atualizar(
            ServerPlayer jogador, UnaryOperator<PersistentNenData> mutacao) {
        Objects.requireNonNull(jogador, "jogador");
        return atualizar(new AttachmentDoJogador(jogador), mutacao);
    }

    static PersistentNenData ler(Armazenamento armazenamento) {
        Objects.requireNonNull(armazenamento, "armazenamento");
        PersistentNenData lido = Objects.requireNonNull(
                armazenamento.ler(), "o attachment devolveu um perfil nulo");
        PersistentNenData migrado = NenProfileMigrator.migrar(lido);
        if (migrado != lido) {
            armazenamento.gravar(migrado);
        }
        return migrado;
    }

    static PersistentNenData atualizar(
            Armazenamento armazenamento, UnaryOperator<PersistentNenData> mutacao) {
        Objects.requireNonNull(mutacao, "mutacao");
        PersistentNenData atual = ler(armazenamento);
        PersistentNenData alterado = Objects.requireNonNull(
                mutacao.apply(atual), "a mutacao devolveu um perfil nulo");
        PersistentNenData valido = NenProfileMigrator.migrar(alterado);
        if (!valido.equals(atual)) {
            armazenamento.gravar(valido);
            armazenamento.aoAlterar(valido);
        }
        return valido;
    }

    interface Armazenamento {
        PersistentNenData ler();

        void gravar(PersistentNenData perfil);

        /** Publica somente depois da escrita; leitura/migracao nao e mutacao. */
        default void aoAlterar(PersistentNenData perfil) {
        }
    }

    private record AttachmentDoJogador(ServerPlayer jogador) implements Armazenamento {
        @Override
        public PersistentNenData ler() {
            return this.jogador.getData(NenAttachments.NEN_PERSISTENTE);
        }

        @Override
        public void gravar(PersistentNenData perfil) {
            this.jogador.setData(NenAttachments.NEN_PERSISTENTE, perfil);
        }

        @Override
        public void aoAlterar(PersistentNenData perfil) {
            NenSyncService.enviarSnapshot(this.jogador, perfil);
        }
    }
}
