package com.darkcontinent.nenfoundation.client;

import static org.junit.jupiter.api.Assertions.*;

import com.darkcontinent.nenfoundation.client.screen.DadosDaFicha;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Varre os consumidores da ficha: chave faltante OU orfa reprova, nos dois idiomas. */
class TraducaoDaFichaTest {
    @Test
    void todaChaveDaTelaTemTraducaoSemOrfas() throws Exception {
        Path raiz = Path.of(System.getProperty("nenfoundation.repoRoot", "."));
        String fonte = Files.readString(raiz.resolve("src/main/java/com/darkcontinent/nenfoundation/client/screen/TelaDoJogador.java"));
        Set<String> usadas = new HashSet<>();
        var matcher = Pattern.compile("texto\\(\"([a-z_]+)\"").matcher(fonte);
        while (matcher.find()) usadas.add("nenfoundation.ficha." + matcher.group(1));
        for (var tipo : DadosDaFicha.Tipo.values()) usadas.add(tipo.chave());
        assertTrue(usadas.size() >= 20, "varredura vazia/incompleta nao e aprovacao");
        for (String idioma : new String[] {"pt_br", "en_us"}) {
            var json = JsonParser.parseString(Files.readString(raiz.resolve("src/main/resources/assets/nenfoundation/lang/" + idioma + ".json"))).getAsJsonObject();
            Set<String> declaradas = new HashSet<>();
            json.keySet().stream().filter(chave -> chave.startsWith("nenfoundation.ficha.")).forEach(declaradas::add);
            assertEquals(usadas, declaradas, idioma);
            for (String chave : usadas) assertFalse(json.get(chave).getAsString().isBlank(), chave);
            assertFalse(json.get("key.nenfoundation.ficha_do_jogador").getAsString().isBlank());
        }
    }
}
