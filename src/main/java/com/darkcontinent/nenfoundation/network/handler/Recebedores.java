package com.darkcontinent.nenfoundation.network.handler;

import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import com.darkcontinent.nenfoundation.network.payload.FeedbackDeErroS2C;
import com.darkcontinent.nenfoundation.network.payload.PresencaDeAuraS2C;
import com.darkcontinent.nenfoundation.network.payload.FxDeHabilidadeS2C;
import com.darkcontinent.nenfoundation.network.payload.SnapshotDePerfilS2C;
import com.darkcontinent.nenfoundation.network.payload.BestiarySnapshotS2C;
import java.util.Objects;

/**
 * O registro do {@link RecebedorDeNen} ativo.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. O padrao e um NO-OP, e nao {@code null}. O servidor dedicado nunca
 * registra recebedor nenhum, e o handler de payload roda antes de qualquer
 * verificacao de lado. Um {@code null} aqui viraria NPE dentro de um handler
 * de rede — e excecao engolida num handler de rede desconecta o jogador com
 * uma mensagem generica e nenhum rastro.
 *
 * <p>2. Registrar duas vezes LANCA. Dois recebedores significa que alguem
 * criou um segundo cache, e a partir dai duas telas mostram valores
 * diferentes do mesmo jogador sem que nada acuse.
 *
 * <p>3. Quem liga, desliga: {@link #limpar()} existe para o ciclo de vida do
 * cliente, e nao para "resetar quando der problema".
 */
public final class Recebedores {

    /** Nao faz nada, e nao guarda nada. E o que responde no servidor dedicado. */
    private static final RecebedorDeNen NENHUM = new RecebedorDeNen() {
        @Override
        public void aoReceberSnapshot(SnapshotDePerfilS2C payload) {
        }

        @Override
        public void aoReceberDelta(DeltaDeRuntimeS2C payload) {
        }

        @Override
        public void aoReceberFx(FxDeHabilidadeS2C payload) {
        }

        @Override
        public void aoReceberErro(FeedbackDeErroS2C payload) {
        }

        @Override
        public void aoReceberPresenca(PresencaDeAuraS2C payload) {
        }

        @Override
        public void aoReceberBestiary(BestiarySnapshotS2C payload) {
        }
    };

    private static volatile RecebedorDeNen atual = NENHUM;

    private Recebedores() {
    }

    /** O recebedor ativo. Nunca {@code null}. */
    public static RecebedorDeNen atual() {
        return atual;
    }

    /** Registra o recebedor deste lado. Um so, uma vez. */
    public static void registrar(RecebedorDeNen recebedor) {
        Objects.requireNonNull(recebedor, "recebedor");
        if (atual != NENHUM) {
            throw new IllegalStateException(
                    "Ja existe um RecebedorDeNen registrado (" + atual.getClass().getName()
                            + "). Dois recebedores fazem duas telas mostrarem valores"
                            + " diferentes do mesmo jogador, sem nada acusar.");
        }
        atual = recebedor;
    }

    /** Volta ao no-op. Usado pelo ciclo de vida de quem registrou. */
    public static void limpar() {
        atual = NENHUM;
    }

    /** Se ha um recebedor de verdade. Util em diagnostico. */
    public static boolean temRecebedor() {
        return atual != NENHUM;
    }
}
