package com.darkcontinent.nenfoundation.bestiary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.enemy.content.EnemyCatalog;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao que liga o BESTIARIO a fila de inimigos.
 *
 * <p><b>A falha que ele fecha nao da erro e quase nao da sintoma:</b> uma
 * criatura registrada, com corpo, voz, loot e traducao -- e sem ficha. Ela nasce,
 * o jogador a encontra, a mata, e o livro nao sabe dela. Nada no build reclama,
 * porque o bestiario e data-driven e um catalogo com menos entradas do que o
 * mundo tem mobs continua sendo um catalogo valido.</p>
 *
 * <p>Foi exatamente o estado em que as dezesseis criaturas novas ficaram: os
 * sete do exame tinham ficha desde o inicio, e as dezesseis entraram pela fila
 * sem passar pelo livro.</p>
 *
 * <p><b>Ele morde dos dois lados</b>, e a segunda metade importa: ficha sem
 * criatura tambem reprova. Um mob renomeado deixaria a ficha antiga no
 * repositorio para sempre, e o "verde" so diria que o que sobrou esta certo.</p>
 */
class FichaDeTodoInimigoTest {

    private static final String FICHAS = "src/main/resources/data/nenfoundation/bestiary";
    private static final List<String> IDIOMAS = List.of(
            "src/main/resources/assets/nenfoundation/lang/en_us.json",
            "src/main/resources/assets/nenfoundation/lang/pt_br.json");

    /**
     * O Boneco de Treino NAO tem ficha, e isso e uma decisao.
     *
     * <p>Ele e ferramenta, e nao conteudo -- uma ficha de bestiario o promoveria a
     * criatura do mundo, que e justamente o que a silhueta dele existe para
     * impedir. A excecao esta aqui, com o motivo, em vez de ser um id que
     * simplesmente nao aparece.</p>
     */
    private static final Set<String> SEM_FICHA_DE_PROPOSITO = Set.of("dummy_enemy");

    @Test
    @DisplayName("PORTAO: todo inimigo publicado tem ficha de bestiario")
    void todoInimigoTemFicha() {
        Set<String> semFicha = new TreeSet<>();
        for (String id : EnemyCatalog.publicados().keySet()) {
            if (SEM_FICHA_DE_PROPOSITO.contains(id)) continue;
            if (!Files.isRegularFile(Repo.raiz().resolve(FICHAS).resolve(id + ".json"))) {
                semFicha.add(id);
            }
        }
        assertTrue(semFicha.isEmpty(),
                "Inimigos publicados e sem ficha de bestiario: " + semFicha + ". Eles nascem, o"
                        + " jogador os encontra e os mata, e o livro nao sabe deles. Nada no"
                        + " build reclama: um catalogo com menos entradas do que o mundo tem"
                        + " mobs continua sendo um catalogo valido.");
    }

    @Test
    @DisplayName("PORTAO: nenhuma ficha sobra para uma criatura que nao existe mais")
    void nenhumaFichaOrfa() {
        Set<String> publicados = EnemyCatalog.publicados().keySet();
        Set<String> orfas = new TreeSet<>();
        for (Path ficha : Repo.varrer(FICHAS, ".json")) {
            String nome = ficha.getFileName().toString().replace(".json", "");
            if (!publicados.contains(nome)) orfas.add(nome);
        }
        assertTrue(orfas.isEmpty(),
                "Fichas sem criatura correspondente: " + orfas + ". Um mob renomeado deixaria a"
                        + " ficha antiga no repositorio para sempre, e o verde so diria que o"
                        + " que sobrou esta certo.");
    }

    @Test
    @DisplayName("PORTAO: a ficha aponta para a entidade certa e para textos que existem")
    void aFichaApontaParaCoisasQueExistem() {
        List<String> linguas = IDIOMAS.stream().map(Repo::texto).toList();
        Set<String> tipoErrado = new TreeSet<>();
        Set<String> textoAusente = new TreeSet<>();

        for (Path arquivo : Repo.varrer(FICHAS, ".json")) {
            String nome = arquivo.getFileName().toString().replace(".json", "");
            JsonObject ficha = JsonParser.parseString(ler(arquivo)).getAsJsonObject();

            String tipo = ficha.get("entity_type").getAsString();
            if (!tipo.equals("nenfoundation:" + nome)) {
                tipoErrado.add(nome + " -> " + tipo);
            }
            for (String campo : List.of("habitat", "summary", "behavior", "combat")) {
                String chave = "\"" + ficha.get(campo).getAsString() + "\"";
                if (linguas.stream().anyMatch(texto -> !texto.contains(chave))) {
                    textoAusente.add(nome + "." + campo);
                }
            }
        }

        assertTrue(tipoErrado.isEmpty(),
                "Fichas cujo entity_type nao bate com o nome do arquivo: " + tipoErrado
                        + ". A ficha carrega, aparece no livro, e descreve outro bicho.");
        assertTrue(textoAusente.isEmpty(),
                "Fichas com texto sem traducao nos dois idiomas: " + textoAusente + ". A pagina"
                        + " abre com a chave crua no lugar do texto, e chave crua parece"
                        + " defeito do mod.");
    }

    @Test
    @DisplayName("a excecao declarada continua fazendo sentido")
    void aExcecaoContinuaValendo() {
        for (String id : SEM_FICHA_DE_PROPOSITO) {
            assertTrue(EnemyCatalog.publicados().containsKey(id),
                    "A excecao '" + id + "' nao corresponde a inimigo publicado nenhum: ela"
                            + " cobriria em silencio outro que ganhasse esse id depois.");
            assertFalse(Files.isRegularFile(Repo.raiz().resolve(FICHAS).resolve(id + ".json")),
                    "O '" + id + "' ganhou ficha de bestiario. Se isso foi decidido, tire a"
                            + " linha da excecao; se nao foi, a ferramenta acabou de virar"
                            + " conteudo sem ninguem decidir.");
        }
        assertEquals(1, SEM_FICHA_DE_PROPOSITO.size(),
                "A lista de excecoes cresceu. Cada id fora do bestiario e uma criatura que o"
                        + " jogador encontra e nao consegue estudar -- e isso precisa de"
                        + " decisao, nao de habito.");
    }

    private static String ler(Path arquivo) {
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }
}
