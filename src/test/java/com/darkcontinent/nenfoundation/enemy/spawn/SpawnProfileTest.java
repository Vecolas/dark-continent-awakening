package com.darkcontinent.nenfoundation.enemy.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao dos cinco perfis de spawn (issue #112).
 *
 * <p>A entrega da issue nao e "existem cinco nomes": e a invariante de que
 * natural, structure-only e encounter-only NAO caem no mesmo caminho. O caso que
 * este portao existe para reprovar nao levanta excecao nenhuma -- um chefe de
 * encontro que vaze para a lista de bioma nasce pelo mundo inteiro, cada
 * instancia e uma entidade legitima, e a unica denuncia possivel e alguem
 * encontrar no mato o que deveria estar num encontro unico.</p>
 */
class SpawnProfileTest {

    private static final Path MODIFICADORES =
            Path.of("src/main/resources/data/nenfoundation/neoforge/biome_modifier");

    private static SpawnRule regra(SpawnProfile perfil, Set<String> tags) {
        return new SpawnRule(tags, Set.of("minecraft:overworld"), 0, 15,
                true, false, false, 4, perfil, SpawnCaps.fauna());
    }

    // --------------------------------------------------------------- perfis

    @Test
    @DisplayName("so os tres perfis naturais entram na lista de bioma")
    void apenasNaturaisEntramNaListaDeBioma() {
        assertTrue(SpawnProfile.ON_GROUND.entraNaListaDeBioma());
        assertTrue(SpawnProfile.IN_WATER.entraNaListaDeBioma());
        assertTrue(SpawnProfile.FLYING_SURFACE_ANCHOR.entraNaListaDeBioma());
        assertFalse(SpawnProfile.STRUCTURE_ONLY.entraNaListaDeBioma());
        assertFalse(SpawnProfile.ENCOUNTER_ONLY.entraNaListaDeBioma(),
                "ENCOUNTER_ONLY na lista de bioma transforma encontro unico em farm, e cada"
                        + " bicho gerado e legitimo -- nao ha duplicata para um portao achar.");
    }

    @Test
    @DisplayName("pedir placement natural a ENCOUNTER_ONLY estoura, e estoura no carregamento")
    void encounterOnlyRecusaPlacement() {
        assertFalse(SpawnProfile.ENCOUNTER_ONLY.registraPlacement());
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                SpawnProfile.ENCOUNTER_ONLY::placement);
        assertTrue(erro.getMessage().contains("controller"),
                "A recusa tem de dizer POR QUE; recusa sem motivo e o pior relato de bug.");
        assertThrows(IllegalStateException.class, SpawnProfile.ENCOUNTER_ONLY::heightmap);
    }

    @Test
    @DisplayName("a ave voadora nasce POUSADA -- placement de chao, e nao de ar")
    void voadoraNasceAncorada() {
        assertEquals(SpawnProfile.ON_GROUND.placement(), SpawnProfile.FLYING_SURFACE_ANCHOR.placement(),
                "Nascer no ar ancoraria o ninho no vazio, e toda distancia medida a partir dele"
                        + " sairia de um ponto que ninguem alcanca.");
    }

    // ---------------------------------------------------------------- regra

    @Test
    @DisplayName("tag de bioma num perfil que nao usa lista de bioma reprova")
    void tagOrfaReprova() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> regra(SpawnProfile.ENCOUNTER_ONLY, Set.of("#nenfoundation:x")));
        assertTrue(erro.getMessage().contains("alarme orfao"));
    }

    @Test
    @DisplayName("perfil natural sem tag nenhuma reprova -- seria um bioma vazio")
    void naturalSemTagReprova() {
        assertThrows(IllegalArgumentException.class, () -> regra(SpawnProfile.ON_GROUND, Set.of()));
    }

    @Test
    @DisplayName("ENCOUNTER_ONLY sem tag e uma regra VALIDA")
    void encounterOnlySemTagEValido() {
        SpawnRule regra = regra(SpawnProfile.ENCOUNTER_ONLY, Set.of());
        assertFalse(regra.entraNaListaDeBioma());
        assertTrue(regra.biomeTags().isEmpty());
    }

    // ----------------------------------------------------------------- caps

    @Test
    @DisplayName("o teto por chunk e a distancia entre grupos sao cobrados")
    void tetosSaoCobrados() {
        SpawnCaps caps = new SpawnCaps(3, 40, 20);
        assertTrue(caps.permite(2, 50.0D, 30.0D));
        assertFalse(caps.permite(3, 50.0D, 30.0D), "Sem teto por chunk, o vale vira parede de carne.");
        assertFalse(caps.permite(0, 20.0D, 30.0D));
        assertFalse(caps.permite(0, 50.0D, 10.0D));
    }

    @Test
    @DisplayName("ausencia de medida (NaN) nao pode virar recusa silenciosa")
    void ausenciaDeMedidaNaoRecusa() {
        SpawnCaps caps = new SpawnCaps(3, 40, 20);
        assertTrue(caps.permite(0, Double.NaN, Double.NaN),
                "Deixar NaN decidir a comparacao devolveria false sempre, e o mob nunca"
                        + " nasceria -- o bioma vazio de sempre, agora por aritmetica.");
    }

    @Test
    @DisplayName("teto zero reprova: desligar um mob se faz pelo PERFIL, nao por um zero")
    void tetoZeroReprova() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new SpawnCaps(0, 10, 10));
        assertTrue(erro.getMessage().contains("ENCOUNTER_ONLY"));
    }

    // ---------------------------------------------------------------- portao

    @Test
    @DisplayName("PORTAO: quem nao entra na lista de bioma nao tem biome modifier")
    void quemNaoNasceSozinhoNaoTemModifier() {
        Map<String, EnemyDefinition> publicados = HunterExamProfiles.publicados();
        assertFalse(publicados.isEmpty(), "Varredura vazia nao e aprovacao.");

        List<Path> modificadores = Repo.varrer(MODIFICADORES.toString(), ".json");
        assertFalse(modificadores.isEmpty(), "Nenhum biome modifier: a varredura nao olharia nada.");

        for (Map.Entry<String, EnemyDefinition> entrada : publicados.entrySet()) {
            String id = entrada.getKey();
            if (entrada.getValue().spawnRule().entraNaListaDeBioma()) continue;
            String referencia = "nenfoundation:" + id;
            for (Path modificador : modificadores) {
                assertFalse(ler(modificador).contains(referencia),
                        id + " usa o perfil " + entrada.getValue().spawnRule().profile()
                                + ", que NAO nasce sozinho, e mesmo assim aparece em "
                                + Repo.raiz().relativize(modificador) + ". O jogo vai coloca-lo"
                                + " na lista de spawn do bioma e nada vai reclamar.");
            }
        }
    }

    @Test
    @DisplayName("PORTAO: todo perfil publicado declara explicitamente o seu SpawnProfile")
    void todoPublicadoDeclaraPerfil() {
        for (Map.Entry<String, EnemyDefinition> entrada : HunterExamProfiles.publicados().entrySet()) {
            SpawnProfile perfil = entrada.getValue().spawnRule().profile();
            assertTrue(perfil != null, entrada.getKey() + " sem perfil de spawn");
            // Coerencia minima que o compilador nao pega: quem exige agua no ambiente
            // nao pode ter sido registrado com o perfil de chao, e vice-versa.
            boolean aquatico = entrada.getValue().spawnRule().allowWater()
                    && !entrada.getValue().spawnRule().requireGround();
            if (perfil == SpawnProfile.IN_WATER) {
                assertTrue(aquatico, entrada.getKey() + " tem perfil IN_WATER mas o ambiente da"
                        + " regra exige chao: o placement aquatico procuraria agua onde a regra"
                        + " so aprova terra, e o mob nunca nasceria.");
            }
        }
    }

    private static String ler(Path arquivo) {
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
