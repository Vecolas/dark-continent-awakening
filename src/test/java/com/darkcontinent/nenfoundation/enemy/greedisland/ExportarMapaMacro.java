package com.darkcontinent.nenfoundation.enemy.greedisland;

import com.darkcontinent.nenfoundation.enemy.greedisland.debug.GreedIslandMapExporter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Nao e portao: e a ferramenta do gate MACRO humano.
 *
 * <p>Ela grava o PNG que a secao 92 do documento pede. Fica desligada por
 * padrao -- um teste que escreve arquivo a cada build e lixo no disco de todo
 * mundo. Rode com:
 *
 * <pre>./gradlew test --tests "*ExportarMapaMacro*" -Dgi.mapa=build/mapa-gi.png</pre>
 */
class ExportarMapaMacro {

    @Test
    @EnabledIfSystemProperty(named = "gi.mapa", matches = ".+")
    void gravar() throws Exception {
        Path destino = Path.of(System.getProperty("gi.mapa"));
        GreedIslandMapExporter.exportar(destino, 900);
        System.out.println("mapa macro: " + destino.toAbsolutePath());
    }
}
