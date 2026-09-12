package com.darkcontinent.nenfoundation.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Portao de sintaxe: todo JSON entregue pelo mod precisa ser legivel pelo Gson. */
class RecursosJsonTest {

    @Test
    @DisplayName("todo recurso JSON tem sintaxe valida")
    void todoRecursoJsonTemSintaxeValida() throws IOException {
        Path raiz = Path.of(System.getProperty("nenfoundation.repoRoot", "."));
        Path recursos = raiz.resolve("src/main/resources");
        List<Path> arquivos;
        try (Stream<Path> encontrados = Files.walk(recursos)) {
            arquivos = encontrados
                    .filter(Files::isRegularFile)
                    .filter(arquivo -> arquivo.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }

        assertFalse(arquivos.isEmpty(),
                "Nenhum JSON encontrado em src/main/resources; varredura vazia nao e aprovacao.");
        for (Path arquivo : arquivos) {
            try {
                JsonParser.parseString(Files.readString(arquivo));
            } catch (JsonParseException erro) {
                fail("JSON invalido em " + raiz.relativize(arquivo) + ": " + erro.getMessage(), erro);
            }
        }
    }
}
