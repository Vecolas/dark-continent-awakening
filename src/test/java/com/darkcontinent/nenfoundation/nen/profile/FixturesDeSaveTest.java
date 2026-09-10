package com.darkcontinent.nenfoundation.nen.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Portao das fixtures de save.
 *
 * <p>POR QUE ELE EXISTE, e por que o {@code PersistentNenDataTest} nao basta:
 *
 * <p>Aquele teste grava e le com o MESMO codec. Ele prova que o codec e
 * consistente consigo mesmo — e e exatamente por isso que ele NAO pega o
 * defeito mais caro desta classe: renomear uma chave.
 *
 * <p>Todo campo usa {@code optionalFieldOf} com um padrao. Renomear
 * {@code aura_potential} para {@code auraPotential} faz o save antigo ler como
 * {@code 0.0}: sem erro, sem aviso, sem nada no log. O jogador perde progresso e
 * a suite continua verde, porque o teste de ida e volta escreve e le com o nome
 * NOVO nos dois lados.
 *
 * <p>O que fecha isso e um dado CONGELADO, escrito uma vez e commitado, que o
 * codec precisa continuar conseguindo ler. E o que vive em
 * {@code src/test/resources/saves/}.
 *
 * <p>O portao varre a FONTE — o diretorio de fixtures — e exige uma pasta por
 * versao de schema publicada. Quando alguem subir {@code SCHEMA_ATUAL} sem
 * deixar uma fixture da versao anterior, ele reprova: e o unico momento em que
 * ainda e barato produzir aquele dado.
 */
class FixturesDeSaveTest {

    private static final String DIR = "src/test/resources/saves";

    // -------------------------------------------------------------- portoes

    @Test
    @DisplayName("existe uma fixture para cada versao de schema ja publicada")
    void todaVersaoTemFixture() {
        Path base = Repo.raiz().resolve(DIR);
        assertTrue(Files.isDirectory(base), "Diretorio de fixtures ausente: " + DIR);

        Set<Integer> versoesComFixture = new LinkedHashSet<>();
        for (Path snbt : fixtures()) {
            versoesComFixture.add(versaoDaPasta(snbt));
        }

        assertFalse(versoesComFixture.isEmpty(),
                "Nenhuma fixture encontrada em " + DIR + ". Um laco que nao acha"
                        + " nada imprime aprovacao com zero verificacoes.");

        List<Integer> faltando = new ArrayList<>();
        for (int v = 1; v <= PersistentNenData.SCHEMA_ATUAL; v++) {
            if (!versoesComFixture.contains(v)) {
                faltando.add(v);
            }
        }
        assertTrue(faltando.isEmpty(),
                "Sem fixture para o schema v" + faltando + ". Se SCHEMA_ATUAL subiu,"
                        + " a fixture da versao ANTERIOR precisa entrar no mesmo PR —"
                        + " depois que existirem saves de jogadores, produzir aquele"
                        + " dado deixa de ser barato.");

        List<Integer> doFuturo = versoesComFixture.stream()
                .filter(v -> v > PersistentNenData.SCHEMA_ATUAL)
                .sorted()
                .toList();
        assertTrue(doFuturo.isEmpty(),
                "Fixture de schema que este codigo nao escreve: v" + doFuturo
                        + ". O portao morde dos dois lados: pasta orfa e o rastro de"
                        + " uma mudanca de schema que voltou atras pela metade.");
    }

    @Test
    @DisplayName("toda fixture do disco e lida e migrada para o schema atual")
    void todaFixtureAtravessaOMigrador() {
        int conferidas = 0;
        for (Path snbt : fixtures()) {
            String nome = Repo.raiz().relativize(snbt).toString().replace('\\', '/');
            PersistentNenData lido = ler(snbt);

            assertEquals(versaoDaPasta(snbt), lido.schemaVersion(),
                    nome + ": a pasta diz uma versao e o conteudo diz outra."
                            + " Uma das duas esta errada, e a que vale e a do conteudo.");

            PersistentNenData migrado = NenProfileMigrator.migrar(lido);
            assertEquals(PersistentNenData.SCHEMA_ATUAL, migrado.schemaVersion(),
                    nome + ": o migrador nao trouxe o dado para o schema atual.");
            conferidas++;
        }
        assertTrue(conferidas > 0, "Zero fixtures conferidas.");
    }

    // ------------------------------------------------- o dado congelado, v1

    @Test
    @DisplayName("a fixture v1 abre com exatamente os valores que foram gravados")
    void fixtureV1AbreComOsValoresCertos() {
        PersistentNenData p = ler(Repo.arquivo(DIR + "/v1/perfil-completo.snbt"));

        // Campo por campo, e nao com equals contra um objeto construido aqui.
        // Comparar com um objeto construido no teste faz um campo renomeado
        // falhar sem dizer QUAL — e a mensagem e o que transforma uma falha em
        // conserto de cinco minutos.
        assertEquals(1, p.schemaVersion(), "schema_version");
        assertTrue(p.awakened(), "awakened");
        assertSame(NenCategory.TRANSMUTATION, p.category(), "category");
        assertTrue(p.categoryRevealed(), "category_revealed");
        assertEquals(42.5D, p.auraPotential(), "aura_potential");
        assertEquals(0.75D, p.control(), "control");
        assertEquals(1.25D, p.output(), "output");

        assertEquals(0.4D, p.proficiencia(id("ten")), "technique_proficiency[ten]");
        assertEquals(0.125D, p.proficiencia(id("ren")), "technique_proficiency[ren]");
        assertEquals(2, p.techniqueProficiency().size(), "technique_proficiency");

        assertEquals(Set.of(id("ten"), id("ren")),
                p.unlockedTechniques(), "unlocked_techniques");
        assertEquals(Set.of(id("aura_cortante")),
                p.unlockedAbilities(), "unlocked_abilities");
        assertEquals(Set.of(id("despertou"), id("categoria_revelada")),
                p.progressionFlags(), "progression_flags");
    }

    @Test
    @DisplayName("a fixture v1 nao le como o perfil neutro")
    void fixtureNaoDegradaParaNeutro() {
        PersistentNenData p = ler(Repo.arquivo(DIR + "/v1/perfil-completo.snbt"));
        assertFalse(PersistentNenData.NAO_DESPERTADO.equals(p),
                "A fixture leu como o perfil neutro. E o sintoma exato de chave"
                        + " renomeada: optionalFieldOf devolve o padrao e nao reclama."
                        + " Se este teste for o unico vermelho, procure um rename no"
                        + " codec antes de qualquer outra coisa.");
    }

    // ------------------------------------------------ o caminho binario/disco

    @Test
    @DisplayName("ida e volta em NBT binario comprimido, passando pelo disco")
    void idaEVoltaEmNbtBinario(@TempDir Path pasta) throws IOException {
        PersistentNenData original = ler(Repo.arquivo(DIR + "/v1/perfil-completo.snbt"));

        CompoundTag escrito = (CompoundTag) PersistentNenData.CODEC
                .encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow(m -> new AssertionError("Falhou ao escrever: " + m));

        Path arquivo = pasta.resolve("perfil.dat");
        NbtIo.writeCompressed(escrito, arquivo);

        CompoundTag relido = NbtIo.readCompressed(arquivo, NbtAccounter.unlimitedHeap());
        PersistentNenData volta = PersistentNenData.CODEC
                .parse(NbtOps.INSTANCE, relido)
                .getOrThrow(m -> new AssertionError("Falhou ao ler: " + m));

        assertEquals(original, volta,
                "O perfil nao sobreviveu a gravacao comprimida em disco. Este e o"
                        + " caminho que o save de verdade usa; o teste em JSON nao"
                        + " passa por ele.");
    }

    // -------------------------------------------------------------- apoio

    private static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath("nenfoundation", caminho);
    }

    /** Varre o diretorio de fixtures. A FONTE, nunca uma lista escrita a mao. */
    private static List<Path> fixtures() {
        Path base = Repo.raiz().resolve(DIR);
        if (!Files.isDirectory(base)) {
            return List.of();
        }
        try (Stream<Path> s = Files.walk(base)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".snbt"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** {@code .../saves/v3/x.snbt} vira {@code 3}. */
    private static int versaoDaPasta(Path snbt) {
        String pasta = snbt.getParent().getFileName().toString();
        if (!pasta.matches("v\\d+")) {
            throw new AssertionError(
                    "Fixture fora de uma pasta v<N>: " + snbt
                            + ". A versao de schema tem de estar no caminho, senao o"
                            + " portao nao consegue saber o que falta.");
        }
        return Integer.parseInt(pasta.substring(1));
    }

    /**
     * Le uma fixture SNBT.
     *
     * <p>O parser de SNBT do Minecraft NAO aceita comentario. As fixtures tem um
     * cabecalho explicando por que existem — quem abrir o arquivo daqui a um ano
     * precisa entender o que quebra se ele mudar —, entao as linhas iniciadas
     * por {@code //} sao removidas aqui.
     *
     * <p>Removemos so a linha cujo INICIO (depois de espacos) e {@code //}. Um
     * {@code //} dentro de uma string de valor sobrevive.
     */
    private static PersistentNenData ler(Path snbt) {
        String bruto;
        try {
            bruto = Files.readString(snbt, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        StringBuilder limpo = new StringBuilder();
        for (String linha : bruto.split("\r?\n", -1)) {
            limpo.append(linha.stripLeading().startsWith("//") ? "" : linha).append('\n');
        }

        CompoundTag tag;
        try {
            tag = TagParser.parseTag(limpo.toString());
        } catch (Exception e) {
            throw new AssertionError("SNBT invalido em " + snbt + ": " + e.getMessage(), e);
        }

        return PersistentNenData.CODEC
                .parse(NbtOps.INSTANCE, tag)
                .getOrThrow(m -> new AssertionError("Fixture " + snbt + " nao pode ser lida: " + m));
    }
}
