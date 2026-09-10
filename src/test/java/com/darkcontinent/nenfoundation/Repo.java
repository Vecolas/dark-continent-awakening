package com.darkcontinent.nenfoundation;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Acesso a arvore do repositorio a partir dos testes.
 *
 * <p>DECISAO QUE ESTE ARQUIVO CARREGA: a raiz vem de uma propriedade de sistema
 * injetada pelo Gradle, e o teste FALHA se ela nao estiver la.
 *
 * <p>O caminho preguicoso seria {@code Paths.get("")} e torcer para que o
 * diretorio de trabalho seja a raiz. Quando ele nao for -- e ele muda entre
 * Gradle, IDE e CI -- os portoes que varrem arquivos encontram zero arquivos e
 * passam. Verde com zero verificacoes e o falso verde mais barato de produzir.
 */
public final class Repo {

    private Repo() {
    }

    /** Raiz do repositorio. Falha alto se o build nao a injetou. */
    public static Path raiz() {
        String prop = System.getProperty("nenfoundation.repoRoot");
        if (prop == null || prop.isBlank()) {
            fail("A propriedade de sistema nenfoundation.repoRoot nao foi definida. "
                    + "Sem ela os portoes que varrem arquivos nao sabem onde procurar "
                    + "e passariam sem verificar nada. Rode pelo Gradle, ou defina "
                    + "-Dnenfoundation.repoRoot=<raiz> na configuracao da IDE.");
        }
        Path raiz = Paths.get(prop);
        if (!Files.isDirectory(raiz.resolve("src").resolve("main").resolve("java"))) {
            fail("nenfoundation.repoRoot aponta para " + raiz
                    + ", que nao parece ser a raiz do repositorio.");
        }
        return raiz;
    }

    public static Path arquivo(String caminhoRelativo) {
        Path p = raiz().resolve(caminhoRelativo);
        if (!Files.isRegularFile(p)) {
            fail("Arquivo esperado nao existe: " + caminhoRelativo);
        }
        return p;
    }

    public static String texto(String caminhoRelativo) {
        try {
            return Files.readString(arquivo(caminhoRelativo), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<String> linhas(String caminhoRelativo) {
        return List.of(texto(caminhoRelativo).split("\r?\n", -1));
    }

    /** Varre a FONTE (o diretorio), nunca uma lista escrita a mao. */
    public static List<Path> varrer(String diretorioRelativo, String sufixo) {
        Path base = raiz().resolve(diretorioRelativo);
        if (!Files.isDirectory(base)) {
            fail("Diretorio esperado nao existe: " + diretorioRelativo);
        }
        try (Stream<Path> s = Files.walk(base)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(sufixo))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
