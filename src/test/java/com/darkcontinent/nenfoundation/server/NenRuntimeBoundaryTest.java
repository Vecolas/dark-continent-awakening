package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Portoes da separacao persistente/runtime e do ciclo central da issue #3. */
class NenRuntimeBoundaryTest {

    private static final String RUNTIME =
            "src/main/java/com/darkcontinent/nenfoundation/nen/profile/RuntimeNenState.java";
    private static final String CICLO_DE_VIDA =
            "src/main/java/com/darkcontinent/nenfoundation/server/NenPlayerLifecycle.java";
    private static final String SCHEDULER =
            "src/main/java/com/darkcontinent/nenfoundation/server/NenTickScheduler.java";

    @Test
    @DisplayName("runtime nao possui codec, attachment ou serializacao")
    void runtimeNuncaEPersistido() {
        String fonte = Repo.texto(RUNTIME);

        assertFalse(fonte.contains("com.mojang.serialization"));
        assertFalse(fonte.contains("AttachmentType"));
        assertFalse(fonte.contains("Serializable"));
        assertFalse(fonte.contains("CompoundTag"));

        List<String> importsNoPacoteDeDados = Repo.varrer(
                        "src/main/java/com/darkcontinent/nenfoundation/data", ".java").stream()
                .filter(path -> Repo.texto(relativo(path)).contains(
                        "import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState"))
                .map(NenRuntimeBoundaryTest::relativo)
                .toList();
        assertTrue(importsNoPacoteDeDados.isEmpty(),
                "RuntimeNenState nao pode entrar na camada de attachments/codecs: "
                        + importsNoPacoteDeDados);
    }

    @Test
    @DisplayName("existe um unico consumidor do evento global de tick")
    void somenteSchedulerAssinaTickGlobal() {
        List<String> consumidores = Repo.varrer("src/main/java", ".java").stream()
                .filter(path -> Repo.texto(relativo(path)).contains(
                        "import net.neoforged.neoforge.event.tick.ServerTickEvent"))
                .map(NenRuntimeBoundaryTest::relativo)
                .toList();

        assertEquals(List.of(SCHEDULER), consumidores,
                "Subsistemas registram no scheduler; nao criam loops globais paralelos.");
    }

    @Test
    @DisplayName("login, logout, clone e dimensao ligam a politica de runtime")
    void cicloDeVidaLigaTodasAsFronteiras() {
        String fonte = Repo.texto(CICLO_DE_VIDA);

        assertTrue(fonte.contains("PlayerLoggedInEvent")
                && fonte.contains("NenRuntimeService.iniciarSessao(jogador)"));
        assertTrue(fonte.contains("PlayerLoggedOutEvent")
                && fonte.contains("NenRuntimeService.encerrarSessao(jogador)"));
        assertTrue(fonte.contains("PlayerEvent.Clone")
                && fonte.contains("NenRuntimeService.reiniciar(jogador)"));
        assertTrue(fonte.contains("PlayerChangedDimensionEvent"));
        assertEquals(2, ocorrencias(fonte, "NenRuntimeService.reiniciar(jogador)"),
                "clone e troca de dimensao devem resetar o runtime");
    }

    private static String relativo(Path path) {
        return Repo.raiz().relativize(path).toString().replace('\\', '/');
    }

    private static int ocorrencias(String texto, String trecho) {
        return (texto.length() - texto.replace(trecho, "").length()) / trecho.length();
    }
}
