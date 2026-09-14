package com.darkcontinent.nenfoundation.enemy.spawn;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.enemy.content.EnemyCatalog;
import com.darkcontinent.nenfoundation.enemy.content.GreedIslandProfiles;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandRegion;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao de INTEGRACAO DE MUNDO (EN12 / issue #147).
 *
 * <p>A issue pede uma auditoria: spawn natural, estruturas, densidade, isolamento
 * de Greed Island, colocacao de colonia, distancia segura e tags compativeis. O
 * que da para transformar em portao esta aqui; o resto -- "nenhum spawn
 * impossivel num mundo gerado" -- e mundo de pe e olho humano, e esta declarado
 * em {@code o-que-nao-provamos.md}.</p>
 *
 * <p>Todas as verificacoes abaixo cobrem falhas que <b>nao levantam excecao</b>:
 * um bicho que nasce em cima do jogador, um chefe de outra dimensao no
 * Overworld, uma tag vazia que nunca casa com bioma nenhum, e um biome modifier
 * apontando para um id que ninguem mais publica.</p>
 */
class IntegracaoDeMundoTest {

    private static final String MODIFICADORES =
            "src/main/resources/data/nenfoundation/neoforge/biome_modifier";
    private static final String TAGS = "src/main/resources/data/nenfoundation/tags/worldgen/biome";

    // ------------------------------------------------------ duas verdades

    @Test
    @DisplayName("a dimensao de Greed Island e escrita UMA vez -- a regra e o perfil concordam")
    void dimensaoDaIlhaNaoTemDuasGrafias() {
        String daRegiao = GreedIslandRegion.DIMENSAO.location().toString();
        Set<String> dosPerfis = new TreeSet<>();
        GreedIslandProfiles.publicados().values()
                .forEach(definicao -> dosPerfis.addAll(definicao.spawnRule().dimensions()));

        assertTrue(dosPerfis.contains(daRegiao),
                "A regra de spawn das criaturas de Greed Island declara " + dosPerfis
                        + " e o isolamento confere '" + daRegiao + "'. Duas grafias para a mesma"
                        + " dimensao NAO dao erro: deixam a regra de spawn conferindo um lugar e"
                        + " o isolamento conferindo outro, e a criatura passa por um dos dois.");
        assertTrue(dosPerfis.size() == 1,
                "As sete criaturas da ilha declaram mais de uma dimensao: " + dosPerfis
                        + ". Se uma delas puder nascer fora da ilha, o isolamento acabou.");
    }

    @Test
    @DisplayName("nenhum inimigo do Overworld declara a dimensao da ilha, e vice-versa")
    void ninguemMisturaDimensao() {
        String ilha = GreedIslandRegion.DIMENSAO.location().toString();
        Set<String> daIlha = GreedIslandProfiles.publicados().keySet();

        Set<String> vazando = new TreeSet<>();
        for (Map.Entry<String, EnemyDefinition> entrada : EnemyCatalog.publicados().entrySet()) {
            boolean declaraIlha = entrada.getValue().spawnRule().dimensions().contains(ilha);
            if (declaraIlha != daIlha.contains(entrada.getKey())) vazando.add(entrada.getKey());
        }
        assertTrue(vazando.isEmpty(),
                "Inimigos com dimensao incoerente com a familia: " + vazando + ". Um bicho de"
                        + " Greed Island que declara o Overworld, ou o contrario, nao reclama de"
                        + " nada -- ele so aparece onde ninguem o projetou.");
    }

    // ----------------------------------------------------- distancia segura

    @Test
    @DisplayName("nenhum inimigo natural pode nascer EM CIMA do jogador")
    void naturalNaoNasceEmCimaDoJogador() {
        Set<String> perigosos = new TreeSet<>();
        for (Map.Entry<String, EnemyDefinition> entrada : EnemyCatalog.publicados().entrySet()) {
            SpawnRule regra = entrada.getValue().spawnRule();
            if (!regra.entraNaListaDeBioma()) continue;
            if (regra.caps().distanciaMinimaDeJogador() <= 0) perigosos.add(entrada.getKey());
        }
        assertTrue(perigosos.isEmpty(),
                "Naturais com distancia minima de jogador zero: " + perigosos + ". O vanilla"
                        + " tem regra propria de distancia, entao isto nao aparece como bug"
                        + " todo dia -- aparece como um bicho materializando ao lado de quem"
                        + " acabou de virar a esquina, e o jogador chama de trapaca.");
    }

    @Test
    @DisplayName("todo grupo natural tem distancia declarada ENTRE grupos")
    void gruposNaturaisNaoSeAmontoam() {
        Set<String> semDistancia = new TreeSet<>();
        for (Map.Entry<String, EnemyDefinition> entrada : EnemyCatalog.publicados().entrySet()) {
            SpawnRule regra = entrada.getValue().spawnRule();
            if (!regra.entraNaListaDeBioma()) continue;
            if (regra.caps().distanciaMinimaEntreGrupos() <= 0) semDistancia.add(entrada.getKey());
        }
        assertTrue(semDistancia.isEmpty(),
                "Naturais sem distancia entre grupos: " + semDistancia + ". Sem ela a lista de"
                        + " bioma continua valendo a cada tentativa, e o vale vira parede de"
                        + " carne -- tudo dentro das regras.");
    }

    // --------------------------------------------------------- tags e dados

    @Test
    @DisplayName("toda tag de bioma declarada existe, e nenhuma esta VAZIA")
    void tagsExistemENaoEstaoVazias() {
        Set<String> vazias = new TreeSet<>();
        Set<String> ausentes = new TreeSet<>();
        for (Map.Entry<String, EnemyDefinition> entrada : EnemyCatalog.publicados().entrySet()) {
            for (String tag : entrada.getValue().spawnRule().biomeTags()) {
                String caminho = tag.substring(tag.indexOf(':') + 1);
                Path arquivo = Repo.raiz().resolve(TAGS).resolve(caminho + ".json");
                if (!Files.isRegularFile(arquivo)) {
                    ausentes.add(entrada.getKey() + " -> " + tag);
                    continue;
                }
                JsonObject json = JsonParser.parseString(ler(arquivo)).getAsJsonObject();
                JsonArray valores = json.getAsJsonArray("values");
                if (valores == null || valores.isEmpty()) vazias.add(tag);
            }
        }
        assertTrue(ausentes.isEmpty(), "Tags declaradas e sem arquivo: " + ausentes);
        assertTrue(vazias.isEmpty(),
                "Tags de bioma VAZIAS: " + vazias + ". O arquivo existe, o portao de existencia"
                        + " passa, o biome modifier a referencia -- e ela nao casa com bioma"
                        + " nenhum. O mob nunca nasce, e o relato de bug vira 'o bioma esta"
                        + " vazio'.");
    }

    @Test
    @DisplayName("nenhum biome modifier fala de um inimigo que o catalogo nao publica")
    void modifierNaoReferenciaDesconhecido() {
        Set<String> publicados = EnemyCatalog.publicados().keySet();
        Set<String> orfaos = new TreeSet<>();
        for (Path modificador : Repo.varrer(MODIFICADORES, ".json")) {
            String texto = ler(modificador);
            int inicio = 0;
            while (true) {
                int posicao = texto.indexOf("nenfoundation:", inicio);
                if (posicao < 0) break;
                inicio = posicao + 1;
                int fim = posicao + "nenfoundation:".length();
                while (fim < texto.length()
                        && (Character.isLetterOrDigit(texto.charAt(fim)) || texto.charAt(fim) == '_')) {
                    fim++;
                }
                String id = texto.substring(posicao + "nenfoundation:".length(), fim);
                // Tags de bioma tambem usam o namespace; so interessa o que parece id de mob.
                if (!id.isEmpty() && !id.endsWith("_biomes") && !publicados.contains(id)) {
                    orfaos.add(Repo.raiz().relativize(modificador) + " -> " + id);
                }
            }
        }
        assertTrue(orfaos.isEmpty(),
                "Biome modifiers citando ids fora do catalogo: " + orfaos + ". O jogo carrega o"
                        + " modifier, nao acha a entidade e NAO reclama: o spawn simplesmente"
                        + " nao acontece, e o arquivo fica parecendo que funciona.");
    }

    // ----------------------------------------------------------- densidade

    @Test
    @DisplayName("nenhum natural pede mais por chunk do que o limite de grupo dele")
    void tetoPorChunkNaoContradizOLimiteDeGrupo() {
        Set<String> contraditorios = new TreeSet<>();
        for (Map.Entry<String, EnemyDefinition> entrada : EnemyCatalog.publicados().entrySet()) {
            SpawnRule regra = entrada.getValue().spawnRule();
            if (!regra.entraNaListaDeBioma()) continue;
            if (regra.caps().maximoPorChunk() > regra.maxNearbySameFaction()) {
                contraditorios.add(entrada.getKey() + " (chunk=" + regra.caps().maximoPorChunk()
                        + " > grupo=" + regra.maxNearbySameFaction() + ")");
            }
        }
        assertTrue(contraditorios.isEmpty(),
                "Teto por chunk maior que o limite de grupo: " + contraditorios + ". Os dois"
                        + " numeros ficam em lugares diferentes e falam da mesma coisa; quando"
                        + " discordam, um deles nunca morde -- e a sessao de balanceamento gira"
                        + " o botao morto.");
    }

    // ------------------------------------------------------------- canarios

    @Test
    @DisplayName("CANARIO: as duas reguas de densidade reprovam o caso que deveriam reprovar")
    void asReguasDeDensidadeMordem() {
        // Regua que nunca reprova e carimbo. Aqui ela e alimentada com os dois
        // casos exatos que existe para pegar, montados a mao -- nenhum mob real
        // precisa estar errado para provar que a regra funciona.
        SpawnRule emCimaDoJogador = new SpawnRule(Set.of("#nenfoundation:x"),
                Set.of("minecraft:overworld"), 0, 15, true, false, false, 4,
                SpawnProfile.ON_GROUND, new SpawnCaps(4, 48, 0));
        assertTrue(emCimaDoJogador.entraNaListaDeBioma()
                        && emCimaDoJogador.caps().distanciaMinimaDeJogador() <= 0,
                "O caso que a regua procura deixou de ser construivel: se SpawnCaps passar a"
                        + " recusar distancia zero, esta verificacao vira varredura do vazio e"
                        + " tem de ser removida -- nao mantida verde sem olhar nada.");

        SpawnRule amontoado = new SpawnRule(Set.of("#nenfoundation:x"),
                Set.of("minecraft:overworld"), 0, 15, true, false, false, 2,
                SpawnProfile.ON_GROUND, new SpawnCaps(8, 48, 24));
        assertTrue(amontoado.caps().maximoPorChunk() > amontoado.maxNearbySameFaction(),
                "O caso de teto por chunk maior que o limite de grupo deixou de ser"
                        + " construivel; a regua correspondente passou a medir o vazio.");
    }

    @Test
    @DisplayName("a varredura nao esta vazia -- varredura vazia nao e aprovacao")
    void varreduraNaoEVazia() {
        assertFalse(EnemyCatalog.publicados().isEmpty());
        assertFalse(Repo.varrer(MODIFICADORES, ".json").isEmpty());
        assertFalse(Repo.varrer(TAGS, ".json").isEmpty());
        List<String> familias = List.of("great_stamp", "cyclops", "crab_heavy");
        for (String esperado : familias) {
            assertTrue(EnemyCatalog.publicados().containsKey(esperado),
                    "O catalogo perdeu '" + esperado + "': a varredura passaria a olhar menos"
                            + " do que existe, e o verde diria menos do que parece.");
        }
    }

    private static String ler(Path arquivo) {
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }
}
