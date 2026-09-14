package com.darkcontinent.nenfoundation.data;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.darkcontinent.nenfoundation.Repo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao dos CAMINHOS de datapack, e do par que faz um bloco dropar.
 *
 * <p><b>POR QUE ELE EXISTE, com o caso que o criou.</b> Este repositorio passou
 * meses com VINTE E UMA loot tables de bloco em
 * {@code data/nenfoundation/loot_tables/blocks/} e duas receitas em
 * {@code data/nenfoundation/recipes/}. Sao os nomes de 1.20. O Minecraft 1.21
 * renomeou os diretorios de datapack para o SINGULAR -- {@code loot_table},
 * {@code recipe}, {@code advancement}, {@code tags/block} -- e simplesmente
 * <b>ignora</b> os antigos.
 *
 * <p>Nao ha erro, nao ha aviso no build, e o arquivo continua la, bem formado e
 * versionado. O sintoma em jogo e "esse bloco nao dropa nada", que ninguem liga
 * a um nome de pasta. Parte do repositorio ja tinha migrado -- {@code tags/block},
 * {@code structure}, {@code loot_table/chests} e {@code loot_table/entities} --,
 * o que torna a divergencia ainda mais dificil de ver: metade certa e metade
 * errada leem igual num {@code ls}.
 *
 * <p><b>E HAVIA UM SEGUNDO DEFEITO EMPILHADO, que tornava o primeiro invisivel.</b>
 * Todo bloco da World Tree usa {@code requiresCorrectToolForDrops()}, e nao havia
 * uma unica tag {@code minecraft:mineable/*} no repositorio inteiro. Sem ela
 * NENHUMA ferramenta e a correta, {@code hasCorrectToolForDrops} devolve falso, e
 * {@code dropResources} nunca roda. Consertar so o caminho da pasta teria movido
 * vinte e um arquivos e mudado exatamente nada em jogo -- um conserto que parece
 * conserto.
 *
 * <p>Por isso este portao verifica o PAR, e nao cada metade: um bloco que pede
 * ferramenta certa precisa de loot table no caminho vivo <b>e</b> de uma tag de
 * ferramenta. Qualquer uma das duas sozinha da zero drop.
 */
class CaminhosDeDatapackTest {

    /**
     * Os nomes que 1.21 aposentou, e que o jogo ignora em silencio.
     *
     * <p>A lista e dos que este repositorio usa ou poderia usar. Um diretorio com
     * qualquer um destes nomes e codigo morto que parece vivo.
     */
    private static final List<String> NOMES_DE_1_20 = List.of(
            "loot_tables", "recipes", "advancements", "predicates",
            "item_modifiers", "functions", "structures",
            "tags/blocks", "tags/items", "tags/entity_types", "tags/fluids");

    private static final Path DADOS = Repo.raiz().resolve("src/main/resources/data");

    /**
     * Blocos que tem loot table e NAO precisam de tag de ferramenta, com o motivo.
     *
     * <p>A excecao mora aqui, escrita, em vez de o portao ser frouxo. Um portao
     * que ignora casos sem dizer quais vira carimbo.
     */
    private static final List<String> SEM_FERRAMENTA_POR_DECISAO = List.of(
            // strength(-1.0F, 3_600_000.0F): inquebravel de proposito, como a
            // bedrock. A loot table dele nunca dispara, e esta la por simetria.
            "world_tree_core");

    @Test
    @DisplayName("nenhum diretorio de datapack com nome de 1.20 -- o jogo os IGNORA")
    void semCaminhoDe120() throws IOException {
        List<String> achados = new ArrayList<>();
        try (Stream<Path> caminhos = Files.walk(DADOS)) {
            for (Path caminho : caminhos.filter(Files::isDirectory).toList()) {
                String relativo = DADOS.relativize(caminho).toString().replace('\\', '/');
                for (String morto : NOMES_DE_1_20) {
                    if (relativo.equals(morto) || relativo.endsWith("/" + morto)) {
                        achados.add(relativo);
                    }
                }
            }
        }
        assertTrue(achados.isEmpty(),
                "diretorio(s) de datapack com nome de 1.20: " + achados
                        + ". O Minecraft 1.21 renomeou todos para o singular e IGNORA"
                        + " os antigos -- sem erro, sem aviso. O conteudo continua"
                        + " versionado e nunca e carregado.");
    }

    @Test
    @DisplayName("todo bloco com loot table esta numa tag de ferramenta -- senao nao dropa")
    void lootTableSemFerramentaNaoDropa() throws IOException {
        Path blocos = DADOS.resolve("nenfoundation/loot_table/blocks");
        assertTrue(Files.isDirectory(blocos),
                "nao ha loot table de bloco nenhuma em " + blocos + "."
                        + " Um portao que nao encontra arquivo passa sem verificar nada.");

        Set<String> comFerramenta = idsEmTagsDeFerramenta();
        assertTrue(comFerramenta.size() >= 10,
                "so " + comFerramenta.size() + " blocos em tags mineable."
                        + " Se as tags sumirem, este portao precisa REPROVAR, e nao"
                        + " passar por falta de dados.");

        List<String> orfaos = new ArrayList<>();
        try (Stream<Path> arquivos = Files.list(blocos)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".json")).toList()) {
                String nome = arquivo.getFileName().toString().replace(".json", "");
                if (SEM_FERRAMENTA_POR_DECISAO.contains(nome)) {
                    continue;
                }
                if (!comFerramenta.contains("nenfoundation:" + nome)) {
                    orfaos.add(nome);
                }
            }
        }
        assertTrue(orfaos.isEmpty(),
                "estes blocos tem loot table e nao estao em nenhuma tag"
                        + " minecraft:mineable/*: " + orfaos + ". Eles usam"
                        + " requiresCorrectToolForDrops(), entao NENHUMA ferramenta e a"
                        + " correta e dropResources nunca roda -- a loot table existe e"
                        + " nunca dispara. Nao ha erro em lugar nenhum.");
    }

    @Test
    @DisplayName("nenhuma receita usa o atalho de ingrediente de 1.20")
    void semAtalhoDeIngredienteDe120() throws IOException {
        // ESTE CASO NASCEU DE UM LOG DE SERVIDOR, e ele so foi possivel PORQUE as
        // receitas sairam da pasta morta.
        //
        // `hunter_bestiary` usava o atalho de 1.20 -- {@code "P": "minecraft:paper"},
        // uma string solta onde 1.21 exige objeto ou lista. Enquanto o arquivo
        // estava em `recipes/`, o jogo nem o lia: dois defeitos empilhados, e o
        // de fora escondia o de dentro. Movido para `recipe/`, ele passou a
        // carregar -- e a falhar alto:
        //
        //   Parsing error loading recipe nenfoundation:hunter_bestiary
        //   Map entry 'P' : Failed to parse either. Not a json array / not a JSON object
        //
        // Consertar o caminho SEM consertar o conteudo troca um arquivo morto por
        // um erro no boot. Este portao existe para que a proxima receita nao
        // repita o atalho.
        Path receitas = DADOS.resolve("nenfoundation/recipe");
        if (!Files.isDirectory(receitas)) {
            return;
        }
        // `"X": "algum:id"` dentro do bloco `key` -- o atalho, em uma linha.
        //
        // `\s` COM DUAS BARRAS, e isto quase passou: desde o Java 15 `\s` e um
        // escape VALIDO de string -- ele vale um espaco, e nao a classe de regex.
        // Compila, roda, e casa um espaco literal em vez de qualquer branco: com
        // um tab ou uma quebra de linha no JSON, o portao passaria calado.
        Pattern atalho = Pattern.compile(
                "\"[A-Za-z#]\"\\s*:\\s*\"[a-z0-9_.-]+:[a-z0-9_/.-]+\"");
        List<String> culpadas = new ArrayList<>();
        try (Stream<Path> arquivos = Files.list(receitas)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".json")).toList()) {
                String texto = Files.readString(arquivo, StandardCharsets.UTF_8);
                int inicio = texto.indexOf("\"key\"");
                if (inicio < 0) {
                    continue;
                }
                int fim = texto.indexOf("\"result\"", inicio);
                String bloco = fim > inicio ? texto.substring(inicio, fim) : texto.substring(inicio);
                if (atalho.matcher(bloco).find()) {
                    culpadas.add(arquivo.getFileName().toString());
                }
            }
        }
        assertTrue(culpadas.isEmpty(),
                "receita(s) com o atalho de ingrediente de 1.20: " + culpadas
                        + ". Em 1.21 o ingrediente e objeto ({\"item\": \"...\"}) ou"
                        + " lista; a string solta reprova no carregamento, com a"
                        + " receita simplesmente ausente do jogo.");
    }

    @Test
    @DisplayName("as tags de ferramenta moram no namespace minecraft, e nao no nosso")
    void tagDeFerramentaNoNamespaceCerto() {
        // ALIMENTAR O PORTAO COM O DEFEITO: escrever as mesmas tags em
        // `data/nenfoundation/tags/block/mineable/axe.json` e um erro que o
        // datapack aceita sem reclamar -- ele cria uma tag NOVA,
        // `nenfoundation:mineable/axe`, que ferramenta nenhuma consulta. O
        // arquivo fica identico, o caminho quase identico, e o drop continua
        // zerado.
        Path certo = DADOS.resolve("minecraft/tags/block/mineable");
        assertTrue(Files.isDirectory(certo),
                "as tags de ferramenta precisam estar em " + certo
                        + ": quem as le e o item do vanilla, pelo id minecraft:.");
        Path errado = DADOS.resolve("nenfoundation/tags/block/mineable");
        if (Files.isDirectory(errado)) {
            fail("ha tags mineable em " + errado + ". Isso cria uma tag nova no"
                    + " NOSSO namespace, que ferramenta nenhuma consulta.");
        }
    }

    /** Todo id de bloco citado em qualquer {@code minecraft:mineable/*}. */
    private static Set<String> idsEmTagsDeFerramenta() throws IOException {
        Path pasta = DADOS.resolve("minecraft/tags/block/mineable");
        Set<String> ids = new TreeSet<>();
        if (!Files.isDirectory(pasta)) {
            return ids;
        }
        // O JSON destas tags e uma lista plana de strings; um regex de id e mais
        // honesto aqui do que arrastar um parser para dentro do portao.
        Pattern id = Pattern.compile("\"([a-z0-9_.-]+:[a-z0-9_./-]+)\"");
        try (Stream<Path> arquivos = Files.list(pasta)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".json")).toList()) {
                Matcher matcher = id.matcher(
                        Files.readString(arquivo, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    ids.add(matcher.group(1));
                }
            }
        }
        return ids;
    }
}
