package com.darkcontinent.nenfoundation.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Nome de tecnica e nome de tecla tem traducao nos dois idiomas -- e a lista
 * das duas coisas e DERIVADA, nunca escrita aqui.
 *
 * <p>POR QUE ELE EXISTE. Havia portao para as categorias, portao para a ficha e
 * portao para os motivos de recusa. Para os NOMES DAS TECNICAS e para os NOMES
 * DAS TECLAS nao havia nada. O resultado apareceu sozinho:
 * {@code key.nenfoundation.ajustar_output} estava declarada em
 * {@code NenKeybinds} e faltava nos DOIS arquivos de idioma -- o jogador abria
 * a tela de Controles e lia a chave crua. O build ficou verde o tempo todo.
 *
 * <p>ELE NAO TEM LISTA, e essa e a parte que importa. As chaves de tecnica saem
 * do pacote {@code nen/technique}; as de tecla saem do proprio
 * {@code NenKeybinds.java}. A oitava tecnica e a quinta tecla entram nesta prova
 * no dia em que forem escritas, sem ninguem lembrar de vir aqui -- que e
 * exatamente o que nao aconteceu com a lista de aparencias nem com o roteiro do
 * gate, e foi por isso que os dois envelheceram em silencio.
 *
 * <p>ORFA TAMBEM REPROVA. Uma chave que sobrou de tecnica removida nao quebra
 * nada e nunca some sozinha; ela vira lixo que o proximo tradutor traduz.
 *
 * <p>LE O CODIGO-FONTE, e nao carrega as classes. {@code NenKeybinds} e
 * client-only: carrega-la num teste do nucleo construiria {@code KeyMapping}
 * sem o jogo em pe, e seria o proprio nucleo alcancando o cliente -- o que
 * outro portao deste repositorio existe para proibir.
 */
class TraducaoDerivadaTest {

    private static final String[] IDIOMAS = {"pt_br", "en_us"};

    private static JsonObject idioma(String nome) {
        try {
            return JsonParser.parseString(Files.readString(Repo.raiz().resolve(
                    "src/main/resources/assets/nenfoundation/lang/" + nome + ".json")))
                    .getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** As chaves de nome de tecnica, tiradas dos arquivos do pacote. */
    private static Set<String> chavesDeTecnica() {
        Path pacote = Repo.raiz().resolve(
                "src/main/java/com/darkcontinent/nenfoundation/nen/technique");
        Set<String> chaves = new LinkedHashSet<>();
        try (Stream<Path> arquivos = Files.list(pacote)) {
            for (Path arquivo : arquivos.sorted().toList()) {
                String fonte = Files.readString(arquivo);
                // O id vem do proprio `NenFoundation.id("...")` da tecnica, que e
                // a mesma fonte que a roda usa para montar a chave em runtime.
                Matcher m = Pattern.compile(
                        "ResourceLocation ID = NenFoundation\\.id\\(\"([a-z_]+)\"\\)")
                        .matcher(fonte);
                while (m.find()) {
                    chaves.add("nenfoundation.tecnica." + m.group(1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return chaves;
    }

    /** As chaves de tecla, tiradas do proprio arquivo que as declara. */
    private static Set<String> chavesDeTecla() {
        String fonte = Repo.texto(
                "src/main/java/com/darkcontinent/nenfoundation/client/keybind/NenKeybinds.java");
        Set<String> chaves = new LinkedHashSet<>();
        Matcher m = Pattern.compile("\"(key\\.[a-z_.]+)\"").matcher(fonte);
        while (m.find()) {
            chaves.add(m.group(1));
        }
        return chaves;
    }

    @Test
    @DisplayName("toda tecnica registrada tem nome nos dois idiomas, e sem orfa")
    void nomeDeTecnica() {
        Set<String> usadas = chavesDeTecnica();
        // A VARREDURA CONFERE O PROPRIO ACHADO. Se o pacote mudar de lugar ou a
        // forma do id mudar, ela devolveria o conjunto vazio -- e conjunto vazio
        // igual a conjunto vazio passa, sem verificar nada.
        assertTrue(usadas.size() >= 4,
                "achei so " + usadas.size() + " chave(s) de tecnica no pacote;"
                        + " a varredura parou de enxergar e passaria vazia.");

        for (String nome : IDIOMAS) {
            JsonObject json = idioma(nome);
            Set<String> declaradas = new LinkedHashSet<>();
            json.keySet().stream()
                    .filter(chave -> chave.startsWith("nenfoundation.tecnica."))
                    .forEach(declaradas::add);
            assertEquals(usadas, declaradas,
                    "as chaves de tecnica de " + nome + " nao batem com as tecnicas"
                            + " que existem. Faltando: o jogador le o id cru na"
                            + " roda. Sobrando: ficou chave de tecnica que nao"
                            + " existe mais, e ninguem apaga o que nao quebra.");
            for (String chave : usadas) {
                assertFalse(json.get(chave).getAsString().isBlank(),
                        chave + " esta em branco em " + nome);
            }
        }
    }

    @Test
    @DisplayName("toda tecla declarada tem nome nos dois idiomas, e sem orfa")
    void nomeDeTecla() {
        Set<String> usadas = chavesDeTecla();
        assertTrue(usadas.size() >= 4,
                "achei so " + usadas.size() + " chave(s) de tecla; a varredura"
                        + " parou de enxergar NenKeybinds.");

        for (String nome : IDIOMAS) {
            JsonObject json = idioma(nome);
            Set<String> declaradas = new LinkedHashSet<>();
            json.keySet().stream()
                    .filter(chave -> chave.startsWith("key."))
                    .forEach(declaradas::add);
            assertEquals(usadas, declaradas,
                    "as chaves de tecla de " + nome + " nao batem com o que"
                            + " NenKeybinds declara. Uma tecla sem traducao"
                            + " aparece na tela de Controles com a chave crua, e"
                            + " nada no build acusa isso.");
            for (String chave : usadas) {
                assertFalse(json.get(chave).getAsString().isBlank(),
                        chave + " esta em branco em " + nome);
            }
        }
    }
}
