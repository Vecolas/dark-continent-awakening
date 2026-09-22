package com.darkcontinent.nenfoundation.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.darkcontinent.nenfoundation.Repo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do REGISTRO DE RESPOSTAS ({@code docs/testing/RESPOSTAS.md}).
 *
 * <p>POR QUE ELE EXISTE. Os gates da trilha AV repetem perguntas: {@code
 * particulas 0} foi feita tres vezes (AV1, AV2, AV3); movimento, duas; as tres
 * regressoes, duas. Em 2026-09-22 veio o pedido, e ele e justo: <i>"e perda de
 * tempo responder a mesma coisa sem que nada tenha mudado"</i>.
 *
 * <p><b>O PERIGO DE ATENDER ESSE PEDIDO MAL.</b> Carregar um veredito adiante e
 * exatamente como se fabrica falso verde: a resposta continua escrita, o codigo
 * muda por baixo, e ninguem percebe que o julgamento passou a descrever outro
 * jogo. A frase "nada mudou" nao pode morar na memoria de quem carrega -- ela
 * tem de ser uma conta.
 *
 * <p>Entao cada resposta declara SEUS invalidadores, e este portao reprova
 * quando qualquer um deles muda. <b>A resposta nao expira por tempo; expira por
 * causa.</b>
 *
 * <p><b>PONTO CEGO DECLARADO, e ele e grande.</b> Este portao le ARQUIVO, e
 * julgamento visual nao mora em arquivo. Resource pack, driver de video, GPU
 * diferente, mudanca no proprio Minecraft -- nada disso move um digest, e
 * qualquer um deles pode invalidar um veredito. Os invalidadores sao os que
 * estao ao nosso alcance, e e so isso que este portao promete.
 */
class RespostasAindaValidasTest {

    private static final String REGISTRO = "docs/testing/RESPOSTAS.md";

    /** Uma linha da tabela: id, veredito, onde, invalidadores, digest. */
    private static final Pattern LINHA = Pattern.compile(
            "^\\|\\s*`([a-z0-9-]+)`\\s*\\|\\s*([^|]+?)\\s*\\|\\s*([^|]+?)\\s*\\|\\s*`([^`]+)`"
                    + "\\s*\\|\\s*`([^`]+)`\\s*\\|\\s*$",
            Pattern.MULTILINE);

    /**
     * Quantas respostas o registro precisa ter para o portao significar algo.
     *
     * <p>Sem este piso, esvaziar a tabela deixaria o portao VERDE varrendo zero
     * linhas -- o falso verde mais barato que existe, e o mesmo que
     * {@code Repo.raiz()} ja evita para os portoes que varrem diretorio.
     */
    private static final int PISO_DE_RESPOSTAS = 8;

    private record Resposta(String id, String veredito, String onde, List<String> invalidadores,
            String digest) {
    }

    // ------------------------------------------------------------------ gate

    @Test
    @DisplayName("nenhuma resposta carregada teve seus invalidadores mexidos")
    void nenhumaRespostaEstaDesatualizada() {
        List<Resposta> respostas = ler(Repo.texto(REGISTRO));

        assertTrue(respostas.size() >= PISO_DE_RESPOSTAS,
                "O registro caiu para " + respostas.size() + " respostas. Ou a tabela foi"
                        + " esvaziada, ou o formato das linhas mudou e o regex parou de casar"
                        + " -- e nos dois casos este portao passaria verificando NADA.");

        List<String> problemas = desatualizadas(respostas);
        if (!problemas.isEmpty()) {
            fail("Resposta carregada adiante cujo invalidador MUDOU:\n\n"
                    + String.join("\n\n", problemas)
                    + "\n\nNAO atualize o digest para calar este portao. O digest e a"
                    + " consequencia, nao a causa: reprovar aqui significa que a pergunta"
                    + " precisa ser OLHADA de novo, numa sessao, antes de o veredito valer"
                    + " outra vez. Recarimbar sem olhar converte a regua em carimbo.");
        }
    }

    @Test
    @DisplayName("todo invalidador declarado EXISTE -- some um arquivo, some a protecao")
    void todoInvalidadorExiste() {
        for (Resposta r : ler(Repo.texto(REGISTRO))) {
            for (String caminho : r.invalidadores()) {
                Path p = Repo.raiz().resolve(caminho.trim());
                assertTrue(Files.exists(p),
                        "A resposta `" + r.id() + "` declara como invalidador `" + caminho
                                + "`, que nao existe mais. Um arquivo renomeado sai do digest"
                                + " EM SILENCIO: a resposta continuaria valendo enquanto o"
                                + " codigo de que ela depende seria livre para mudar.");
            }
        }
    }

    @Test
    @DisplayName("nenhuma resposta fica sem invalidador -- isso seria um veredito eterno")
    void nenhumaRespostaEEterna() {
        for (Resposta r : ler(Repo.texto(REGISTRO))) {
            assertTrue(!r.invalidadores().isEmpty(),
                    "A resposta `" + r.id() + "` nao declara invalidador nenhum. Ela nunca"
                            + " reprovaria, o que a torna um veredito ETERNO -- e nenhum"
                            + " julgamento visual e eterno.");
        }
    }

    // ------------------------------------------------- o caso que DEVE reprovar

    @Test
    @DisplayName("REGUA DA REGUA: um digest errado e detectado")
    void umDigestErradoReprova() {
        String falso = "| id | veredito | perguntada em | invalidadores | digest |\n"
                + "| --- | --- | --- | --- | --- |\n"
                + "| `inventada` | PASSA | AV9 | `docs/testing/RESPOSTAS.md` | `000000000000` |\n";

        List<Resposta> uma = ler(falso);
        assertEquals(1, uma.size(), "o proprio regex do portao parou de casar uma linha valida");

        List<String> problemas = desatualizadas(uma);
        assertEquals(1, problemas.size(),
                "Um digest deliberadamente errado NAO foi detectado. Este portao estaria"
                        + " aprovando qualquer resposta carregada adiante, que e precisamente"
                        + " o falso verde que ele existe para impedir.");
        assertTrue(problemas.get(0).contains("inventada"),
                "o relato de falha nao nomeia a resposta culpada, e um relato assim manda"
                        + " alguem procurar em dez linhas qual delas quebrou");
    }

    @Test
    @DisplayName("REGUA DA REGUA: conteudo diferente da digest diferente")
    void conteudoDiferenteDaDigestDiferente() {
        assertTrue(!digestDe(List.of("docs/testing/RESPOSTAS.md"))
                        .equals(digestDe(List.of("docs/adr/index.md"))),
                "Dois arquivos de conteudo diferente deram o MESMO digest. A funcao nao esta"
                        + " lendo o conteudo, e o portao inteiro vira decoracao.");
    }

    @Test
    @DisplayName("REGUA DA REGUA: o digest nao depende do fim de linha da maquina")
    void oDigestNaoDependeDeCrLf() {
        // O repositorio e trabalhado no Windows e o CI roda em Linux. Sem esta
        // normalizacao o portao reprovaria TODAS as respostas ao trocar de
        // maquina -- e um portao que reprova sempre e desligado em uma semana.
        assertEquals(sha(normalizar("a\r\nb\r\n")), sha(normalizar("a\nb\n")),
                "CRLF e LF deram digests diferentes. No CI toda resposta reprovaria de uma"
                        + " vez, por causa de fim de linha, e ninguem confiaria neste portao"
                        + " depois disso.");
    }

    // ---------------------------------------------------------------- leitura

    private static List<Resposta> ler(String registro) {
        List<Resposta> lidas = new ArrayList<>();
        Matcher m = LINHA.matcher(registro);
        while (m.find()) {
            lidas.add(new Resposta(m.group(1), m.group(2), m.group(3),
                    List.of(m.group(4).split(";")), m.group(5)));
        }
        return lidas;
    }

    private static List<String> desatualizadas(List<Resposta> respostas) {
        List<String> problemas = new ArrayList<>();
        for (Resposta r : respostas) {
            String atual = digestDe(r.invalidadores());
            if (!atual.equals(r.digest())) {
                problemas.add("  `" + r.id() + "` (" + r.veredito() + ", respondida em "
                        + r.onde() + ")\n    registrado: " + r.digest() + "\n    agora:      "
                        + atual + "\n    depende de: " + String.join(", ", r.invalidadores()));
            }
        }
        return problemas;
    }

    /**
     * Digest do CONTEUDO dos caminhos declarados.
     *
     * <p>Diretorio entra inteiro, em ordem; o caminho relativo entra junto do
     * conteudo, para que RENOMEAR um arquivo mova o digest tanto quanto editar
     * um -- renomear tambem pode mudar o que a resposta julgou.
     */
    private static String digestDe(List<String> caminhos) {
        StringBuilder acumulado = new StringBuilder();
        for (Path p : expandir(caminhos)) {
            acumulado.append(Repo.raiz().relativize(p).toString().replace('\\', '/'))
                    .append(' ')
                    .append(normalizar(texto(p)))
                    .append(' ');
        }
        return sha(acumulado.toString());
    }

    private static List<Path> expandir(List<String> caminhos) {
        List<Path> arquivos = new ArrayList<>();
        for (String caminho : caminhos) {
            Path p = Repo.raiz().resolve(caminho.trim());
            if (Files.isDirectory(p)) {
                try (Stream<Path> s = Files.walk(p)) {
                    s.filter(Files::isRegularFile).forEach(arquivos::add);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else if (Files.isRegularFile(p)) {
                arquivos.add(p);
            } else {
                fail("Invalidador inexistente: " + caminho);
            }
        }
        // ORDEM ESTAVEL: `Files.walk` nao promete ordem, e um digest que depende
        // da ordem do sistema de arquivos muda sozinho entre maquinas.
        arquivos.sort(Path::compareTo);
        return arquivos;
    }

    private static String texto(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String normalizar(String conteudo) {
        return conteudo.replace("\r\n", "\n");
    }

    private static String sha(String conteudo) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(conteudo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 ausente na JVM", e);
        }
    }
}
