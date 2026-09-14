package com.darkcontinent.nenfoundation.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do ponto cego declarado: todo pacote diz o que faz e qual decisao
 * carrega.
 *
 * <p>POR QUE ELE EXISTE: com duas pessoas trabalhando em areas diferentes, um
 * pacote sem {@code package-info.java} e um pedaco da arquitetura que so existe
 * na cabeca de quem o criou. Ele some junto com o contexto.
 *
 * <p>Ele varre a FONTE -- o diretorio de codigo -- e nao uma lista. E a lista de
 * divida {@link #SEM_PACKAGE_INFO_AINDA} morde dos dois lados: pacote fora dela
 * tem de estar coberto, e pacote DENTRO dela tem de continuar descoberto. Sem a
 * segunda metade, a linha ficaria para sempre cobrindo em silencio o dia em que
 * aquele pacote ganhasse documentacao.
 */
class PacotesDeclaradosTest {

    private static final String FONTE = "src/main/java";

    /**
     * Divida declarada. Vazia hoje, de proposito.
     *
     * <p>Um pacote so entra aqui com uma issue aberta. Divida que so existe na
     * cabeca de quem escreveu nao existe.
     */
    private static final Set<String> SEM_PACKAGE_INFO_AINDA = Set.of();

    @Test
    @DisplayName("todo pacote tem package-info, ou esta na lista nomeada de divida")
    void todoPacoteDeclarado() {
        List<Path> pacotes = pacotesComConteudo();
        assertFalse(pacotes.isEmpty(),
                "Nenhum pacote encontrado em " + FONTE + ". Um laco que nao encontra"
                        + " nada imprime aprovacao com zero verificacoes.");

        Set<String> descobertos = new LinkedHashSet<>();
        Set<String> cobertos = new LinkedHashSet<>();
        for (Path dir : pacotes) {
            String nome = nomeDoPacote(dir);
            if (Files.isRegularFile(dir.resolve("package-info.java"))) {
                cobertos.add(nome);
            } else {
                descobertos.add(nome);
            }
        }

        Set<String> inesperados = new LinkedHashSet<>(descobertos);
        inesperados.removeAll(SEM_PACKAGE_INFO_AINDA);
        assertTrue(inesperados.isEmpty(),
                "Pacote sem package-info.java e fora da lista de divida: " + inesperados
                        + ". Escreva o que o pacote faz e qual decisao ele carrega, ou"
                        + " declare a divida com uma issue.");

        Set<String> dividaQuitada = new LinkedHashSet<>(SEM_PACKAGE_INFO_AINDA);
        dividaQuitada.retainAll(cobertos);
        assertTrue(dividaQuitada.isEmpty(),
                "Estes pacotes ganharam package-info mas continuam na lista de divida: "
                        + dividaQuitada + ". Tire-os da lista -- senao ela passa a cobrir"
                        + " em silencio o dia em que a documentacao se perder.");
    }

    @Test
    @DisplayName("o dominio nao importa nada de client")
    void dominioNaoDependeDeCliente() {
        List<String> violacoes = new java.util.ArrayList<>();
        for (Path java : Repo.varrer(FONTE, ".java")) {
            String caminho = Repo.raiz().relativize(java).toString().replace('\\', '/');
            boolean ehNucleo = caminho.contains("/nenfoundation/nen/")
                    || caminho.contains("/nenfoundation/api/")
                    || caminho.contains("/nenfoundation/network/")
                    || caminho.contains("/nenfoundation/server/")
                    || caminho.contains("/nenfoundation/enemy/");
            if (!ehNucleo) {
                continue;
            }
            for (String linha : lerLinhas(java)) {
                String t = linha.strip();
                if (t.startsWith("import ") && t.contains(".nenfoundation.client.")) {
                    violacoes.add(caminho + " -> " + t);
                }
                if (t.startsWith("import net.minecraft.client.")) {
                    violacoes.add(caminho + " -> " + t);
                }
            }
        }
        assertTrue(violacoes.isEmpty(),
                "O nucleo importou codigo de cliente:\n  " + String.join("\n  ", violacoes)
                        + "\nIsso compila, roda em singleplayer e roda em runClient."
                        + " Falha so no servidor dedicado, com NoClassDefFoundError,"
                        + " na frente dos jogadores.");
    }

    private static List<Path> pacotesComConteudo() {
        Path base = Repo.raiz().resolve(FONTE);
        try (Stream<Path> s = Files.walk(base)) {
            return s.filter(Files::isDirectory)
                    .filter(PacotesDeclaradosTest::temJava)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean temJava(Path dir) {
        try (Stream<Path> s = Files.list(dir)) {
            return s.anyMatch(p -> Files.isRegularFile(p)
                    && p.getFileName().toString().endsWith(".java"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String nomeDoPacote(Path dir) {
        return Repo.raiz().resolve(FONTE).relativize(dir).toString().replace('\\', '.');
    }

    private static List<String> lerLinhas(Path p) {
        try {
            return Files.readAllLines(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
