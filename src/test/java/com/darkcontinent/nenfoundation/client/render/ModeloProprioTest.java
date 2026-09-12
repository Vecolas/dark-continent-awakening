package com.darkcontinent.nenfoundation.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.darkcontinent.nenfoundation.Repo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do MODELO PROPRIO do great stamp (ADR-017: mob vanilla e andaime).
 *
 * <p>DECISAO QUE ESTE ARQUIVO CARREGA: no GeckoLib quase toda falha de asset e
 * SILENCIOSA. Nome de animacao com erro de digitacao nao levanta excecao -- o
 * clipe simplesmente nao toca. Osso citado numa animacao que nao existe no geo
 * nao levanta excecao -- o membro fica parado. Textura com dimensao diferente
 * da declarada no geo nao levanta excecao -- o bicho sai borrado. Geo ou
 * animation ausente nao levanta excecao -- o mob some ou nao anima. Todas essas
 * falhas so aparecem na tela, e tela nao tem portao: este arquivo e o portao.
 *
 * <p>Ele e PURO: le os arquivos do disco com Gson e ImageIO, sem carregar
 * Minecraft nem GeckoLib. E ele varre a FONTE dos dois lados -- os nomes de
 * animacao saem do codigo Java, os ossos saem do arquivo de animacao, e os dois
 * sao conferidos contra o geo. Uma lista escrita a mao aqui envelheceria em
 * silencio, que e exatamente a falha que o portao existe para impedir.
 *
 * <p>Toda varredura tem piso: laco que percorre zero itens imprime verde sem
 * verificar nada, e verde com zero verificacoes e o falso verde mais barato de
 * produzir.
 */
class ModeloProprioTest {

    private static final String ID = "great_stamp";

    private static final String GEO =
            "src/main/resources/assets/nenfoundation/geo/entity/" + ID + ".geo.json";
    private static final String ANIMACAO =
            "src/main/resources/assets/nenfoundation/animations/entity/" + ID + ".animation.json";
    private static final String TEXTURA =
            "src/main/resources/assets/nenfoundation/textures/entity/" + ID + "/adulto.png";

    /** O que {@code DefaultedEntityGeoModel} procura a partir de "nenfoundation:great_stamp". */
    private static final String IDENTIFICADOR = "geometry." + ID;

    /** Dimensao da folha de textura fixada no contrato desta entrega. */
    private static final int LARGURA_CONTRATADA = 128;
    private static final int ALTURA_CONTRATADA = 64;

    /** Fonte varrida atras dos literais de animacao; o resto da arvore entra junto. */
    private static final String ENTIDADE =
            "src/main/java/com/darkcontinent/nenfoundation/enemy/entity/GreatStampEntity.java";

    /** Nomes de clipe fixados no contrato. Cada lane escreve contra esta lista. */
    private static final List<String> CLIPES_DO_CONTRATO = List.of(
            "animation." + ID + ".idle",
            "animation." + ID + ".walk",
            "animation." + ID + ".run",
            "animation." + ID + ".windup",
            "animation." + ID + ".charge",
            "animation." + ID + ".stagger",
            "animation." + ID + ".hurt",
            "animation." + ID + ".death");

    /**
     * Esqueleto contratado: osso -> parent. {@code null} e raiz.
     *
     * <p>Um esqueleto que perde um osso nao da erro; da uma animacao que nao
     * mexe nada. Um esqueleto que troca o parent de um osso tambem nao da erro;
     * da um membro que gira em volta do pivo errado.
     */
    private static final Map<String, String> ESQUELETO_DO_CONTRATO = esqueleto();

    private static Map<String, String> esqueleto() {
        Map<String, String> ossos = new LinkedHashMap<>();
        ossos.put("root", null);
        ossos.put("body", "root");
        ossos.put("chest", "body");
        ossos.put("neck", "chest");
        ossos.put("head", "neck");
        ossos.put("jaw", "head");
        ossos.put("tusk_left", "head");
        ossos.put("tusk_right", "head");
        ossos.put("leg_front_left", "body");
        ossos.put("hoof_front_left", "leg_front_left");
        ossos.put("leg_front_right", "body");
        ossos.put("hoof_front_right", "leg_front_right");
        ossos.put("leg_back_left", "body");
        ossos.put("hoof_back_left", "leg_back_left");
        ossos.put("leg_back_right", "body");
        ossos.put("hoof_back_right", "leg_back_right");
        ossos.put("tail", "body");
        // Collections.unmodifiableMap, e nao Map.copyOf: a raiz tem parent null e
        // Map.copyOf rejeita valor nulo.
        return java.util.Collections.unmodifiableMap(ossos);
    }

    /** Chaves de um clipe que NAO sao osso; citar uma delas como osso seria falso positivo. */
    private static final Set<String> CHAVES_QUE_NAO_SAO_OSSO = Set.of(
            "loop", "animation_length", "override_previous_animation", "anim_time_update",
            "blend_weight", "start_delay", "loop_delay", "particle_effects", "sound_effects",
            "timeline", "effects");

    private static final Pattern LITERAL_DE_ANIMACAO =
            Pattern.compile("animation\\." + ID + "\\.[A-Za-z0-9_]+");

    // ------------------------------------------------------------------ 1 e 2

    @Test
    @DisplayName("os tres arquivos do modelo proprio existem e os dois JSON sao legiveis")
    void osTresArquivosExistemEOsJsonSaoValidos() {
        for (String caminho : List.of(GEO, ANIMACAO, TEXTURA)) {
            Path arquivo = Repo.arquivo(caminho);
            assertTrue(arquivo.toFile().length() > 0L,
                    caminho + " existe mas esta VAZIO. Arquivo vazio nao levanta excecao no"
                            + " GeckoLib: o mob some da tela, ou aparece sem animar nenhuma.");
        }
        // parse() ja reprova com a mensagem certa; chamar aqui prova a sintaxe dos dois.
        assertNotNull(json(GEO), GEO + " precisa ser um JSON legivel pelo Gson");
        assertNotNull(json(ANIMACAO), ANIMACAO + " precisa ser um JSON legivel pelo Gson");
    }

    @Test
    @DisplayName("o geo declara o identifier que o DefaultedEntityGeoModel resolve")
    void oGeoDeclaraOIdentificadorContratado() {
        assertEquals(IDENTIFICADOR, descricaoDoGeo().get("identifier").getAsString(),
                "identifier diferente de " + IDENTIFICADOR + " nao da erro de compilacao nem de"
                        + " carregamento: o modelo simplesmente nao e encontrado e o great stamp"
                        + " nao e desenhado em jogo.");
    }

    // ---------------------------------------------------------------------- 3

    @Test
    @DisplayName("a textura tem exatamente as dimensoes que o geo declara")
    void aTexturaBateComAFolhaDeclaradaNoGeo() {
        JsonObject descricao = descricaoDoGeo();
        int larguraDeclarada = inteiro(descricao, "texture_width");
        int alturaDeclarada = inteiro(descricao, "texture_height");

        assertEquals(LARGURA_CONTRATADA, larguraDeclarada,
                "o contrato desta entrega fixa a folha em " + LARGURA_CONTRATADA + "x"
                        + ALTURA_CONTRATADA + "; mudar texture_width no geo sem refazer a UV"
                        + " reposiciona TODA face do modelo em silencio.");
        assertEquals(ALTURA_CONTRATADA, alturaDeclarada,
                "o contrato desta entrega fixa a folha em " + LARGURA_CONTRATADA + "x"
                        + ALTURA_CONTRATADA + "; mudar texture_height no geo sem refazer a UV"
                        + " reposiciona TODA face do modelo em silencio.");

        BufferedImage imagem = png(TEXTURA);
        assertEquals(larguraDeclarada, imagem.getWidth(),
                "a textura tem largura " + imagem.getWidth() + " e o geo declara texture_width "
                        + larguraDeclarada + ". O jogo NAO reclama: ele estica a imagem sobre os"
                        + " UVs e entrega um bicho borrado, com as faces trocadas de lugar.");
        assertEquals(alturaDeclarada, imagem.getHeight(),
                "a textura tem altura " + imagem.getHeight() + " e o geo declara texture_height "
                        + alturaDeclarada + ". O jogo NAO reclama: ele estica a imagem sobre os"
                        + " UVs e entrega um bicho borrado, com as faces trocadas de lugar.");
    }

    // ---------------------------------------------------------------------- 4

    @Test
    @DisplayName("todo nome de animacao citado no codigo Java existe no arquivo de animacao")
    void todoClipeCitadoNoCodigoExisteNoArquivoDeAnimacao() {
        // A fonte da lista e o CODIGO, nunca uma lista escrita a mao aqui: e o unico
        // jeito de um erro de digitacao reprovar antes de alguem abrir o jogo.
        Repo.arquivo(ENTIDADE);
        Map<String, List<String>> citados = clipesCitadosNaFonte();
        assertFalse(citados.isEmpty(),
                "nenhum literal \"animation." + ID + ".*\" foi encontrado em src/main/java."
                        + " Ou o codigo do GeoEntity ainda nao chegou, ou os nomes de clipe foram"
                        + " montados por concatenacao e este portao perdeu a capacidade de"
                        + " enxerga-los -- varredura vazia nao e aprovacao.");

        Set<String> declarados = clipesDeclarados();
        for (Map.Entry<String, List<String>> citado : citados.entrySet()) {
            assertTrue(declarados.contains(citado.getKey()),
                    "o codigo pede o clipe \"" + citado.getKey() + "\" em " + citado.getValue()
                            + ", e ele NAO existe em " + ANIMACAO + ". Clipes declarados: "
                            + new TreeSet<>(declarados) + ". Isso nao levanta excecao nenhuma:"
                            + " o GeckoLib simplesmente nao toca a animacao, e o great stamp"
                            + " carrega, atordoa ou morre parado.");
        }
    }

    @Test
    @DisplayName("o arquivo de animacao entrega os oito clipes do contrato")
    void osOitoClipesDoContratoEstaoDeclarados() {
        Set<String> declarados = clipesDeclarados();
        for (String clipe : CLIPES_DO_CONTRATO) {
            assertTrue(declarados.contains(clipe),
                    "o contrato desta entrega fixa o clipe \"" + clipe + "\", e ele nao esta em "
                            + ANIMACAO + ". Enquanto o codigo nao pedir esse clipe ninguem ve"
                            + " erro; no dia em que pedir, o mob fica parado naquela fase.");
        }
    }

    // ---------------------------------------------------------------------- 5

    @Test
    @DisplayName("todo osso citado em qualquer animacao existe na arvore de bones do geo")
    void todoOssoCitadoEmAnimacaoExisteNoGeo() {
        Set<String> ossosDoGeo = ossosDoGeo().keySet();
        JsonObject animacoes = animacoes();
        assertFalse(animacoes.keySet().isEmpty(),
                ANIMACAO + " nao declara nenhuma animacao. Varrer zero clipes imprime verde sem"
                        + " verificar nada.");

        int referencias = 0;
        for (String clipe : animacoes.keySet()) {
            JsonElement corpo = animacoes.get(clipe);
            if (!corpo.isJsonObject() || !corpo.getAsJsonObject().has("bones")) continue;
            JsonObject ossos = corpo.getAsJsonObject().getAsJsonObject("bones");
            for (String osso : ossos.keySet()) {
                if (CHAVES_QUE_NAO_SAO_OSSO.contains(osso)) continue;
                referencias++;
                assertTrue(ossosDoGeo.contains(osso),
                        "a animacao \"" + clipe + "\" move o osso \"" + osso + "\", que NAO existe"
                                + " em " + GEO + ". Ossos do geo: " + new TreeSet<>(ossosDoGeo)
                                + ". O GeckoLib ignora o osso desconhecido em silencio: aquele"
                                + " membro fica parado enquanto o resto anima.");
            }
        }
        assertTrue(referencias > 0,
                "nenhuma animacao de " + ANIMACAO + " move osso nenhum. Oito clipes que nao citam"
                        + " um unico osso sao oito clipes que nao mexem nada em jogo -- e um laco"
                        + " com zero iteracoes imprime verde sem verificar nada.");
    }

    // ---------------------------------------------------------------------- 6

    @Test
    @DisplayName("a arvore de ossos do geo bate com a hierarquia do contrato")
    void oEsqueletoBateComOContrato() {
        Map<String, String> ossos = ossosDoGeo();
        assertFalse(ossos.isEmpty(),
                GEO + " nao declara nenhum osso. Um geo sem bones desenha um bicho invisivel e"
                        + " nao levanta excecao nenhuma.");

        ESQUELETO_DO_CONTRATO.forEach((osso, parent) -> {
            assertTrue(ossos.containsKey(osso),
                    "o esqueleto contratado exige o osso \"" + osso + "\" e ele nao esta em " + GEO
                            + ". Ossos encontrados: " + new TreeSet<>(ossos.keySet())
                            + ". Osso que falta nao da erro: da uma animacao que nao mexe nada"
                            + " naquela parte do corpo.");
            assertEquals(parent, ossos.get(osso),
                    "o osso \"" + osso + "\" deveria ter parent " + (parent == null ? "NENHUM"
                            + " (e raiz)" : "\"" + parent + "\"") + " e tem "
                            + (ossos.get(osso) == null ? "NENHUM" : "\"" + ossos.get(osso) + "\"")
                            + ". Parent errado nao da erro: o membro passa a girar em volta do"
                            + " pivo de outro osso, e a carga do great stamp sai torta.");
        });
    }

    // ------------------------------------------------------------------ leitura

    /** Nome do clipe -> arquivos que o citam. Varre a FONTE inteira, nao uma lista. */
    private static Map<String, List<String>> clipesCitadosNaFonte() {
        Map<String, List<String>> citados = new LinkedHashMap<>();
        List<Path> fontes = Repo.varrer("src/main/java", ".java");
        assertFalse(fontes.isEmpty(),
                "a varredura de src/main/java nao encontrou nenhum .java; sem fonte este portao"
                        + " nao verifica nada.");
        Path raiz = Repo.raiz();
        for (Path fonte : fontes) {
            Matcher m = LITERAL_DE_ANIMACAO.matcher(texto(fonte));
            while (m.find()) {
                citados.computeIfAbsent(m.group(), chave -> new ArrayList<>())
                        .add(raiz.relativize(fonte).toString().replace('\\', '/'));
            }
        }
        return citados;
    }

    private static Set<String> clipesDeclarados() {
        return new LinkedHashSet<>(animacoes().keySet());
    }

    private static JsonObject animacoes() {
        JsonObject raiz = json(ANIMACAO);
        if (!raiz.has("animations") || !raiz.get("animations").isJsonObject()) {
            fail(ANIMACAO + " nao tem o objeto \"animations\" na raiz. Sem ele o GeckoLib carrega"
                    + " zero clipes e o great stamp fica estatico em jogo.");
        }
        return raiz.getAsJsonObject("animations");
    }

    /** Osso -> parent ({@code null} na raiz), lido do geo. */
    private static Map<String, String> ossosDoGeo() {
        JsonObject geometria = geometria();
        if (!geometria.has("bones") || !geometria.get("bones").isJsonArray()) {
            fail(GEO + " nao tem o array \"bones\". Um geo sem ossos nao anima e nao reclama.");
        }
        JsonArray bones = geometria.getAsJsonArray("bones");
        Map<String, String> ossos = new LinkedHashMap<>();
        for (JsonElement elemento : bones) {
            JsonObject osso = elemento.getAsJsonObject();
            if (!osso.has("name")) {
                fail(GEO + " tem um bone sem \"name\". O GeckoLib nao consegue endereca-lo, e"
                        + " nenhuma animacao o alcanca.");
            }
            String nome = osso.get("name").getAsString();
            String parent = osso.has("parent") && !osso.get("parent").isJsonNull()
                    ? osso.get("parent").getAsString() : null;
            assertFalse(ossos.containsKey(nome),
                    GEO + " declara o osso \"" + nome + "\" DUAS vezes. O segundo vence em"
                            + " silencio, e metade das animacoes passa a mover o osso errado.");
            ossos.put(nome, parent);
        }
        return ossos;
    }

    private static JsonObject descricaoDoGeo() {
        JsonObject geometria = geometria();
        if (!geometria.has("description") || !geometria.get("description").isJsonObject()) {
            fail(GEO + " nao tem o objeto \"description\". Sem ele nao ha identifier nem folha de"
                    + " textura, e o modelo nao e encontrado.");
        }
        return geometria.getAsJsonObject("description");
    }

    private static JsonObject geometria() {
        JsonObject raiz = json(GEO);
        if (!raiz.has("minecraft:geometry") || !raiz.get("minecraft:geometry").isJsonArray()) {
            fail(GEO + " nao tem o array \"minecraft:geometry\" exigido pelo format_version"
                    + " 1.12.0. Exportado no formato errado, o modelo nao carrega e o mob some.");
        }
        JsonArray geometrias = raiz.getAsJsonArray("minecraft:geometry");
        assertTrue(geometrias.size() > 0,
                GEO + " declara \"minecraft:geometry\" VAZIO: zero geometrias, zero verificacoes,"
                        + " e um mob invisivel em jogo.");
        return geometrias.get(0).getAsJsonObject();
    }

    private static int inteiro(JsonObject objeto, String chave) {
        if (!objeto.has(chave) || !objeto.get(chave).isJsonPrimitive()) {
            fail(GEO + " nao declara \"" + chave + "\" na description. Sem ela o jogo assume um"
                    + " tamanho de folha proprio e a UV inteira sai deslocada, sem erro nenhum.");
        }
        return objeto.get(chave).getAsInt();
    }

    private static JsonObject json(String caminhoRelativo) {
        try {
            JsonElement raiz = JsonParser.parseString(Repo.texto(caminhoRelativo));
            if (!raiz.isJsonObject()) {
                fail(caminhoRelativo + " nao e um objeto JSON na raiz.");
            }
            return raiz.getAsJsonObject();
        } catch (JsonParseException erro) {
            fail(caminhoRelativo + " tem JSON invalido: " + erro.getMessage()
                    + ". O GeckoLib nao carrega o arquivo e o mob aparece sem modelo ou sem"
                    + " animacao -- na tela, nao no log.", erro);
            throw new AssertionError("inalcancavel");
        }
    }

    private static BufferedImage png(String caminhoRelativo) {
        try {
            BufferedImage imagem = ImageIO.read(Repo.arquivo(caminhoRelativo).toFile());
            assertNotNull(imagem, caminhoRelativo + " precisa ser um PNG legivel; um arquivo que o"
                    + " ImageIO nao abre vira uma textura rosa-e-preta em jogo.");
            return imagem;
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }

    private static String texto(Path arquivo) {
        try {
            return java.nio.file.Files.readString(arquivo, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }
}
