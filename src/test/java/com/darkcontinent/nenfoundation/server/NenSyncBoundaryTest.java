package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Portoes de privacidade e alcançabilidade do envio S2C. */
class NenSyncBoundaryTest {

    private static final String SERVICO =
            "src/main/java/com/darkcontinent/nenfoundation/server/NenSyncService.java";
    private static final String CICLO_DE_VIDA =
            "src/main/java/com/darkcontinent/nenfoundation/server/NenPlayerLifecycle.java";

    @Test
    @DisplayName("envio usa sempre o dono, nunca broadcast ou tracking")
    void envioNaoVazaParaOutrosJogadores() {
        String fonte = Repo.texto(SERVICO);

        assertTrue(fonte.contains("PacketDistributor::sendToPlayer"));
        assertFalse(fonte.contains("sendToAllPlayers"));
        assertFalse(fonte.contains("sendToPlayersInDimension"));
        assertFalse(fonte.contains("sendToPlayersTracking"));
    }

    @Test
    @DisplayName("login alcanca o caminho real de snapshot")
    void loginEnviaSnapshotAoDono() {
        String fonte = Repo.texto(CICLO_DE_VIDA);

        assertTrue(fonte.contains("NenSyncService.enviarSnapshot(jogador, perfil)"),
                "O snapshot deve sair pelo ciclo real de login, nao somente por teste.");
    }
}
