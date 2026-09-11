package com.darkcontinent.nenfoundation.nen.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do playerdata REAL.
 *
 * <p>POR QUE ELE EXISTE, e por que ele nao e redundante com o
 * {@code FixturesDeSaveTest}:
 *
 * <p>Aquele portao le uma fixture que NOS escrevemos, em SNBT, pelo nosso
 * codec. Ele prova que o codec le o que o codec escreve, e pega renomeacao de
 * chave. O que ele nao pode provar e que o NeoForge, o Minecraft e o nosso
 * attachment produzem JUNTOS, no caminho real de save, um dado que ainda
 * conseguimos ler.
 *
 * <p>O arquivo lido aqui foi gravado por um servidor dedicado salvando um
 * jogador de verdade, conectado por um cliente de verdade. Ninguem o
 * construiu em codigo. Ver {@code src/test/resources/saves/playerdata/LEIA-ME.md}
 * para o procedimento de regeracao.
 *
 * <p>O que este portao pega e a coisa mais cara que pode acontecer neste
 * projeto: uma atualizacao de NeoForge, de Minecraft ou do nosso attachment
 * que mude onde ou como o dado e gravado — e apague o progresso de quem ja
 * jogou, sem erro nenhum.
 */
class PlayerdataRealTest {

    private static final String DIR = "src/test/resources/saves/playerdata";

    /** Onde o NeoForge guarda os attachments dentro do NBT da entidade. */
    private static final String CHAVE_DE_ATTACHMENTS = "neoforge:attachments";

    /** O id do nosso attachment, registrado em {@code NenAttachments}. */
    private static final String NOSSO_ATTACHMENT = "nenfoundation:nen_persistente";

    private static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath("nenfoundation", caminho);
    }

    // -------------------------------------------------------------- portoes

    @Test
    @DisplayName("existe pelo menos um playerdata real guardado")
    void existeFixture() {
        List<Path> arquivos = fixtures();
        assertFalse(arquivos.isEmpty(),
                "Nenhum playerdata em " + DIR + ". Um laco que nao acha nada"
                        + " imprime aprovacao com zero verificacoes.");
    }

    @Test
    @DisplayName("o attachment de Nen esta gravado onde o NeoForge o poe")
    void attachmentEstaNoLugar() {
        for (Path arquivo : fixtures()) {
            CompoundTag raiz = lerNbt(arquivo);

            assertTrue(raiz.contains(CHAVE_DE_ATTACHMENTS),
                    arquivo.getFileName() + ": o NBT do jogador nao tem '"
                            + CHAVE_DE_ATTACHMENTS + "'. Ou o NeoForge mudou onde"
                            + " guarda attachments, ou o nosso deixou de ser gravado.");

            CompoundTag attachments = raiz.getCompound(CHAVE_DE_ATTACHMENTS);
            assertTrue(attachments.contains(NOSSO_ATTACHMENT),
                    arquivo.getFileName() + ": '" + NOSSO_ATTACHMENT + "' nao esta"
                            + " no save. O attachment parou de ser gravado, e o"
                            + " progresso de quem ja jogou some no proximo login"
                            + " — sem erro nenhum.");
        }
    }

    @Test
    @DisplayName("o perfil gravado pelo servidor de verdade continua legivel, campo por campo")
    void perfilRealContinuaLegivel() {
        PersistentNenData perfil = lerPerfil(Repo.arquivo(DIR + "/dev-v1.dat"));

        assertEquals(1, perfil.schemaVersion(), "schema_version");

        // O estado exato em que o jogador Dev foi salvo. Ver o LEIA-ME.
        Set<ResourceLocation> esperadas = new LinkedHashSet<>(Set.of(
                id("ten"), id("ren"), id("zetsu"), id("gyo")));
        assertEquals(esperadas, perfil.unlockedTechniques(),
                "As tecnicas desbloqueadas mudaram. Se o codec renomeou uma chave,"
                        + " este conjunto vem VAZIO e nao da erro — optionalFieldOf"
                        + " devolve o padrao em silencio.");

        assertFalse(perfil.awakened(), "awakened");
        assertSame(NenCategory.UNDETERMINED, perfil.category(), "category");
        assertFalse(perfil.categoryRevealed(), "category_revealed");
        assertTrue(perfil.unlockedAbilities().isEmpty(), "unlocked_abilities");
        assertTrue(perfil.progressionFlags().isEmpty(), "progression_flags");
    }

    @Test
    @DisplayName("o perfil real nao le como o neutro")
    void naoDegradaParaNeutro() {
        PersistentNenData perfil = lerPerfil(Repo.arquivo(DIR + "/dev-v1.dat"));
        assertFalse(PersistentNenData.NAO_DESPERTADO.equals(perfil),
                "O playerdata real leu como perfil neutro. E o sintoma exato de"
                        + " chave renomeada. Se este for o unico teste vermelho,"
                        + " procure um rename no codec antes de qualquer outra coisa.");
    }

    @Test
    @DisplayName("o perfil real atravessa o migrador sem mudar de versao")
    void atravessaOMigrador() {
        PersistentNenData lido = lerPerfil(Repo.arquivo(DIR + "/dev-v1.dat"));
        PersistentNenData migrado = NenProfileMigrator.migrar(lido);
        assertEquals(PersistentNenData.SCHEMA_ATUAL, migrado.schemaVersion());
    }

    // --------------------------------------------------------------- apoio

    private static List<Path> fixtures() {
        Path base = Repo.raiz().resolve(DIR);
        if (!Files.isDirectory(base)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(base)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".dat"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static CompoundTag lerNbt(Path arquivo) {
        try {
            CompoundTag tag = NbtIo.readCompressed(arquivo, NbtAccounter.unlimitedHeap());
            assertNotNull(tag, "NbtIo devolveu nulo para " + arquivo);
            return tag;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Le o nosso attachment de dentro do NBT do jogador, pelo nosso codec. */
    private static PersistentNenData lerPerfil(Path arquivo) {
        CompoundTag attachment = lerNbt(arquivo)
                .getCompound(CHAVE_DE_ATTACHMENTS)
                .getCompound(NOSSO_ATTACHMENT);
        return PersistentNenData.CODEC
                .parse(NbtOps.INSTANCE, attachment)
                .getOrThrow(msg -> new AssertionError(
                        "O attachment gravado por um servidor real nao pode ser lido"
                                + " por este codigo: " + msg));
    }
}
