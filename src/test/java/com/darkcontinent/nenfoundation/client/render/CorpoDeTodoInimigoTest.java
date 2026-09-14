package com.darkcontinent.nenfoundation.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.enemy.content.EnemyCatalog;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O buraco que {@code CoerenciaDeGeckoLibTest} NAO fecha, e nao podia fechar.
 *
 * <p>Aquele portao descobre os mobs pelo DISCO: ele varre os {@code .geo.json}
 * existentes e cobra, para cada um, animacao, textura, ossos e clipes coerentes.
 * E a escolha certa para o que ele mede -- mas ela tem uma consequencia que so
 * aparece do outro lado: <b>uma criatura sem geo nenhum nao e varrida.</b> Ela
 * some da conta, o portao fica verde, e o mob e invisivel em jogo.</p>
 *
 * <p>Foi exatamente o estado em que as dezesseis criaturas novas passaram a
 * existir: registradas, com atributos, loot, traducao, voz, ficha de bestiario e
 * perfil publicado -- e sem corpo. Todos os portoes aprovavam, porque cada um
 * mede a coluna dele e nenhum media a AUSENCIA.</p>
 *
 * <p>Este arquivo varre a lista de PUBLICADOS, e nao o disco. A diferenca e a
 * regra inteira: a fonte da varredura tem de ser o que deveria existir, e nao o
 * que existe.</p>
 */
class CorpoDeTodoInimigoTest {

    private static final String ASSETS = "src/main/resources/assets/nenfoundation";

    @Test
    @DisplayName("PORTAO: todo inimigo publicado tem geo, animacao e textura")
    void todoInimigoTemCorpo() {
        Set<String> semGeo = new TreeSet<>();
        Set<String> semAnimacao = new TreeSet<>();
        Set<String> semTextura = new TreeSet<>();

        for (String id : EnemyCatalog.publicados().keySet()) {
            if (!Files.isRegularFile(caminho("geo/entity/" + id + ".geo.json"))) semGeo.add(id);
            if (!Files.isRegularFile(caminho("animations/entity/" + id + ".animation.json"))) {
                semAnimacao.add(id);
            }
            Path pasta = caminho("textures/entity/" + id);
            if (!Files.isDirectory(pasta) || vazia(pasta)) semTextura.add(id);
        }

        assertTrue(semGeo.isEmpty(),
                "Inimigos publicados e SEM MODELO: " + semGeo + ". Eles nascem, andam, atacam e"
                        + " sao INVISIVEIS. CoerenciaDeGeckoLibTest nao pega este caso porque ele"
                        + " descobre mobs pelo disco -- quem nao tem geo simplesmente sai da"
                        + " varredura, e o verde dele passa a significar menos do que parece.");
        assertTrue(semAnimacao.isEmpty(),
                "Inimigos publicados e sem arquivo de animacao: " + semAnimacao + ". O GeckoLib"
                        + " carrega o modelo e nao reclama: o bicho vira estatua.");
        assertTrue(semTextura.isEmpty(),
                "Inimigos publicados e sem pasta de textura: " + semTextura + ". O modelo"
                        + " aparece com a textura ausente do jogo, que a maioria le como bug de"
                        + " pacote de recursos e nao como asset faltando.");
    }

    @Test
    @DisplayName("PORTAO: todo inimigo publicado tem renderer e GeoModel proprios (ADR-017)")
    void todoInimigoTemRenderer() {
        String registro = Repo.texto(
                "src/main/java/com/darkcontinent/nenfoundation/client/render/EnemyRenderers.java");
        Set<String> semRenderer = new TreeSet<>();
        for (String id : EnemyCatalog.publicados().keySet()) {
            if (!registro.contains("EnemyEntityTypes." + id.toUpperCase(java.util.Locale.ROOT))
                    && !registro.contains(classeDe(id) + "Entity.registeredType()")) {
                semRenderer.add(id);
            }
        }
        assertTrue(semRenderer.isEmpty(),
                "Inimigos publicados e sem renderer registrado: " + semRenderer + ". Sem"
                        + " renderer o jogo desenha a entidade com o renderer padrao -- e o"
                        + " padrao, para um tipo sem um, e NADA. O mob existe, colide, machuca"
                        + " e nao aparece.");
    }

    @Test
    @DisplayName("a varredura sai da lista de PUBLICADOS, e nao do disco")
    void aVarreduraNaoOlhaODisco() {
        assertFalse(EnemyCatalog.publicados().isEmpty(),
                "Catalogo vazio: este portao passaria a aprovar tudo.");
        assertEquals(24, EnemyCatalog.publicados().size(),
                "O numero de inimigos publicados mudou. Se foi de proposito, ajuste aqui no"
                        + " mesmo PR -- este caso existe para que um mob REMOVIDO por engano"
                        + " apareca como reprovacao em vez de como uma varredura menor.");
    }

    private static Path caminho(String relativo) {
        return Repo.raiz().resolve(ASSETS).resolve(relativo);
    }

    private static boolean vazia(Path pasta) {
        try (Stream<Path> conteudo = Files.list(pasta)) {
            return conteudo.findAny().isEmpty();
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }

    /** "king_white_stag_beetle" -> "KingWhiteStagBeetle". */
    private static String classeDe(String id) {
        StringBuilder nome = new StringBuilder();
        for (String parte : id.split("_")) {
            nome.append(Character.toUpperCase(parte.charAt(0))).append(parte.substring(1));
        }
        return nome.toString();
    }
}
