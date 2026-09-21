package com.darkcontinent.nenfoundation.enemy.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.enemy.content.EnemyCatalog;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da FILA UNICA: um so registro de entidade, e ninguem entra nele pela
 * metade.
 *
 * <p>POR QUE ELE EXISTE, com o caso que o criou: este repositorio passou meses
 * com DOIS {@code DeferredRegister} de entidade vivos ao mesmo tempo -- um com o
 * foxbear, outro com os seis irmaos -- registrados lado a lado no
 * {@code NenFoundation}. Nada disso levantava excecao. O compilador aceita, o
 * jogo sobe, os dois registros funcionam. O preco apareceu em outro lugar: o
 * foxbear ficou de fora do par atributos/placement que a outra fila recebia,
 * ficou sem tag de bioma, sem biome modifier, sem entrada em
 * {@code publicados()} -- e, portanto, sem portao. Ele simplesmente nunca
 * nasceu no mundo, e o relato de bug possivel era "a floresta esta vazia".
 *
 * <p>ELE MORDE DOS DOIS LADOS. Um segundo registro de entidade reprova; e um id
 * que esta no registro unico e falta em qualquer um dos pontos obrigatorios
 * tambem reprova. Sem a segunda metade, o "um so registro" viraria carimbo: a
 * fila seria unica e mesmo assim alguem entraria nela sem atributos.
 *
 * <p>ELE LE A FONTE, e nao uma lista escrita a mao. Um portao com os sete nomes
 * cravados deixaria o oitavo mob descoberto ate alguem LEMBRAR de estende-lo, e
 * lembrar nao e mecanismo -- e a falha que ele previne e justamente a de quem
 * esqueceu de repetir uma linha.
 *
 * <p>O QUE ELE NAO PROVA: que o placement e o CERTO para a especie (chao para um
 * peixe passaria aqui), que o peso do biome modifier produz densidade jogavel, e
 * que o mob de fato aparece num mundo gerado. Isso e servidor de pe e olho
 * humano -- ver {@code docs/testing/o-que-nao-provamos.md}.
 */
class FilaUnicaDeInimigosTest {

    private static final String FONTE = "src/main/java";
    private static final String REGISTRO =
            "src/main/java/com/darkcontinent/nenfoundation/enemy/registry/EnemyEntityTypes.java";
    private static final String EVENTOS =
            "src/main/java/com/darkcontinent/nenfoundation/enemy/registry/EnemyEntityEvents.java";
    private static final String LOOT =
            "src/main/resources/data/nenfoundation/loot_table/entities";
    private static final List<String> IDIOMAS = List.of(
            "src/main/resources/assets/nenfoundation/lang/en_us.json",
            "src/main/resources/assets/nenfoundation/lang/pt_br.json");

    /** {@code NOME_DA_CONSTANTE = TYPES.register("id"}, com quebra de linha no meio. */
    private static final Pattern REGISTRADO =
            Pattern.compile("([A-Z][A-Z0-9_]*)\\s*=\\s*TYPES\\.register\\(\\s*\"([a-z0-9_]+)\"");

    /** Qualquer {@code DeferredRegister} do registro de ENTITY_TYPE. */
    private static final Pattern REGISTRO_DE_ENTIDADE = Pattern.compile(
            "DeferredRegister\\s*(?:<[^;]*?>)?\\s*\\.?\\s*create\\(\\s*[A-Za-z.]*\\bENTITY_TYPE\\b");

    // ------------------------------------------------------------- descoberta

    /** Constante do registro -> id da entidade, lidos do arquivo de registro. */
    private static Map<String, String> idPorConstante() {
        Map<String, String> ids = new LinkedHashMap<>();
        Matcher m = REGISTRADO.matcher(Repo.texto(REGISTRO));
        while (m.find()) {
            ids.put(m.group(1), m.group(2));
        }
        return ids;
    }

    /** Corpo de um metodo, delimitado por contagem de chaves a partir da assinatura. */
    private static String corpoDe(String fonte, String assinatura) {
        int inicio = fonte.indexOf(assinatura);
        assertTrue(inicio >= 0,
                "Metodo '" + assinatura + "' nao encontrado em " + EVENTOS + ". Ou ele foi"
                        + " renomeado, ou este portao passou a varrer o vazio -- e varredura"
                        + " vazia imprime aprovacao sem verificar nada.");
        int abre = fonte.indexOf('{', inicio);
        int profundidade = 0;
        for (int i = abre; i < fonte.length(); i++) {
            char c = fonte.charAt(i);
            if (c == '{') {
                profundidade++;
            } else if (c == '}') {
                profundidade--;
                if (profundidade == 0) {
                    return fonte.substring(abre, i + 1);
                }
            }
        }
        throw new IllegalStateException("chave nao fechada em " + assinatura);
    }

    // ------------------------------------------------------------------ casos

    @Test
    @DisplayName("existe UM registro de entidade na arvore inteira, e nao dois")
    void umSoRegistroDeEntidade() {
        List<String> arquivos = new ArrayList<>();
        for (Path java : Repo.varrer(FONTE, ".java")) {
            String caminho = Repo.raiz().relativize(java).toString().replace('\\', '/');
            if (REGISTRO_DE_ENTIDADE.matcher(Repo.texto(caminho)).find()) {
                arquivos.add(caminho);
            }
        }

        assertFalse(arquivos.isEmpty(),
                "Nenhum DeferredRegister de ENTITY_TYPE encontrado em " + FONTE + "."
                        + " Ou o mod parou de registrar entidade -- o que e regressao --"
                        + " ou o padrao desta varredura deixou de casar, e ai este portao"
                        + " estaria verde sem olhar para nada.");

        assertEquals(List.of(REGISTRO), arquivos,
                "Ha mais de um registro de entidade vivo: " + arquivos + ". Duas filas NAO"
                        + " dao erro -- as duas funcionam, e o jogo sobe. O que elas dao e um"
                        + " mob que entra numa e fica de fora do que a outra recebe depois"
                        + " (atributos, placement, tag de bioma, portao), e o sintoma disso e"
                        + " um bioma vazio que ninguem consegue explicar. Foi exatamente o que"
                        + " aconteceu com o foxbear ate a issue #266.");
    }

    @Test
    @DisplayName("todo id registrado tem atributos e SpawnPlacement")
    void ninguemEntraNaFilaPelaMetade() {
        Map<String, String> ids = idPorConstante();
        assertFalse(ids.isEmpty(),
                "Nenhuma entidade registrada encontrada em " + REGISTRO + ".");

        String eventos = Repo.texto(EVENTOS);
        String atributos = corpoDe(eventos, "public static void attributes(");
        String placements = corpoDe(eventos, "public static void spawnPlacements(");

        Set<String> semAtributo = new LinkedHashSet<>();
        Set<String> semPlacement = new LinkedHashSet<>();
        for (Map.Entry<String, String> entrada : ids.entrySet()) {
            String referencia = "EnemyEntityTypes." + entrada.getKey();
            if (!atributos.contains(referencia)) {
                semAtributo.add(entrada.getValue());
            }
            if (!placements.contains(referencia)) {
                semPlacement.add(entrada.getValue());
            }
        }

        assertTrue(semAtributo.isEmpty(),
                "Registrados e sem atributos: " + semAtributo + ". Sem AttributeSupplier o"
                        + " jogo estoura ao criar a entidade -- e estoura na hora do spawn, no"
                        + " servidor, nao no build.");

        assertTrue(semPlacement.isEmpty(),
                "Registrados e sem SpawnPlacement: " + semPlacement + ". ESTE E O SILENCIOSO:"
                        + " o mob existe, o ovo funciona, o comando funciona, e ele nunca nasce"
                        + " sozinho. Nada no log, nada no build -- so um bioma vazio.");
    }

    @Test
    @DisplayName("todo id registrado tem perfil publicado, loot e traducao")
    void oQueEstaNoMundoTemFichaCompleta() {
        Map<String, String> ids = idPorConstante();
        Set<String> publicados = EnemyCatalog.publicados().keySet();

        Set<String> semPerfil = new LinkedHashSet<>();
        Set<String> semLoot = new LinkedHashSet<>();
        Set<String> semTraducao = new LinkedHashSet<>();

        List<String> linguas = IDIOMAS.stream().map(Repo::texto).toList();
        for (String id : ids.values()) {
            if (!publicados.contains(id)) {
                semPerfil.add(id);
            }
            if (!Files.isRegularFile(Repo.raiz().resolve(LOOT + "/" + id + ".json"))) {
                semLoot.add(id);
            }
            String chave = "\"entity.nenfoundation." + id + "\"";
            if (linguas.stream().anyMatch(texto -> !texto.contains(chave))) {
                semTraducao.add(id);
            }
        }

        assertTrue(semPerfil.isEmpty(),
                "Registrados e ausentes de EnemyCatalog.publicados(): " + semPerfil
                        + ". Fora dessa lista eles saem do portao que confere tag de bioma e"
                        + " biome modifier -- o portao segue verde varrendo menos, que e o"
                        + " falso verde mais barato que existe.");

        assertTrue(semLoot.isEmpty(),
                "Registrados e sem loot table em " + LOOT + ": " + semLoot + ". O mob morre e"
                        + " nao dropa nada; o jogo nao reclama de tabela ausente.");

        assertTrue(semTraducao.isEmpty(),
                "Registrados e sem traducao nos dois idiomas: " + semTraducao + ". A entidade"
                        + " aparece com a chave crua na tela -- 'entity.nenfoundation.<id>' --"
                        + " e isso passa despercebido em toda tela que nao a mostre.");
    }
}
