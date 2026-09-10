package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Portoes da politica de persistencia definida na issue #2 e no ADR-002. */
class NenProfileBoundaryTest {

    private static final String ATTACHMENT =
            "src/main/java/com/darkcontinent/nenfoundation/data/attachment/NenAttachments.java";
    private static final String SERVICO =
            "src/main/java/com/darkcontinent/nenfoundation/server/NenProfileService.java";
    private static final String CICLO_DE_VIDA =
            "src/main/java/com/darkcontinent/nenfoundation/server/NenPlayerLifecycle.java";

    @Test
    @DisplayName("perfil persistente declara exatamente uma politica copyOnDeath")
    void attachmentCopiaNaMorteUmaVez() {
        String fonte = Repo.texto(ATTACHMENT);

        assertEquals(1, ocorrencias(fonte, ".copyOnDeath()"),
                "Remover perde progresso na morte; duplicar a copia cria duas politicas.");
    }

    @Test
    @DisplayName("handler de clone migra depois da copia e nao recopia manualmente")
    void cloneNaoDuplicaPoliticaDoNeoForge() {
        String fonte = Repo.texto(CICLO_DE_VIDA);

        assertTrue(fonte.contains("EventPriority.LOWEST"),
                "A migracao deve acontecer depois do handler de copia do NeoForge.");
        assertFalse(fonte.contains("getOriginal()"));
        assertFalse(fonte.contains("copyAttachmentsFrom("));
    }

    @Test
    @DisplayName("somente o registro e o servico acessam o attachment do perfil")
    void nenhumConsumidorContornaOServico() {
        List<String> acessos = Repo.varrer("src/main/java", ".java").stream()
                .filter(path -> contem(path, "NEN_PERSISTENTE"))
                .map(path -> Repo.raiz().relativize(path).toString().replace('\\', '/'))
                .toList();

        assertEquals(List.of(ATTACHMENT, SERVICO), acessos,
                "Consumidor acessou o attachment direto e contornou migracao/sync.");
    }

    private static boolean contem(Path path, String trecho) {
        return Repo.texto(Repo.raiz().relativize(path).toString()).contains(trecho);
    }

    private static int ocorrencias(String texto, String trecho) {
        return (texto.length() - texto.replace(trecho, "").length()) / trecho.length();
    }
}
