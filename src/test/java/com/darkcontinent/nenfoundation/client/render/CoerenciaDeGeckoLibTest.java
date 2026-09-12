package com.darkcontinent.nenfoundation.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
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
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao de coerencia do GeckoLib, para TODO mob com modelo proprio.
 *
 * <p>POR QUE ELE EXISTE: no GeckoLib, quase toda falha e MUDA. Nenhuma das
 * abaixo levanta excecao, nenhuma aparece no log, e todas so se manifestam na
 * tela -- que e justamente onde este repositorio nao tem portao nenhum:
 *
 * <ul>
 *   <li>nome de clipe com erro de digitacao: a animacao simplesmente nao toca;
 *   <li>osso citado numa animacao e ausente no geo: o membro fica parado;
 *   <li>osso cujo {@code parent} nao existe: o galho inteiro nunca e desenhado;
 *   <li>textura com dimensao diferente da declarada no geo: o bicho sai borrado;
 *   <li>duas caixas dividindo o mesmo pedaco do atlas: uma pinta por cima da
 *       outra, e o resultado parece "erro de arte";
 *   <li>caixa com UV fora da folha: textura repetida ou transparente.
 * </ul>
 *
 * <p>ELE DESCOBRE OS MOBS NO DISCO, e nao numa lista. Essa e a diferenca que
 * importa: um portao escrito para {@code great_stamp} deixaria o proximo mob
 * migrado sem cobertura ate alguem LEMBRAR de estende-lo -- e lembrar nao e
 * mecanismo. Aqui, quem ganhar um {@code .geo.json} entra na varredura sozinho.
 *
 * <p>O QUE ELE CONTINUA NAO PROVANDO: que o modelo esta bonito, que a pose de
 * windup le como "vai investir", que a silhueta cabe na hitbox. Isso e humano e
 * segue humano -- ver {@code docs/inimigos/mobs-customizados.md} e
 * {@code docs/testing/o-que-nao-provamos.md}.
 */
class CoerenciaDeGeckoLibTest {

    private static final String GEO_DIR = "src/main/resources/assets/nenfoundation/geo/entity";
    private static final String ANIM_DIR = "src/main/resources/assets/nenfoundation/animations/entity";
    private static final String TEXTURA_DIR = "src/main/resources/assets/nenfoundation/textures/entity";
    private static final String FONTE_JAVA = "src/main/java";

    /** Um literal de clipe no codigo: animation.&lt;mob&gt;.&lt;clipe&gt;. */
    private static final Pattern CLIPE_NO_CODIGO =
            Pattern.compile("animation\\.([a-z0-9_]+)\\.([a-z0-9_]+)");

    // ------------------------------------------------------------- descoberta

    /** Ids que tem modelo proprio, lidos do DISCO. */
    private static Set<String> mobsComModelo() {
        Set<String> ids = new LinkedHashSet<>();
        for (Path p : Repo.varrer(GEO_DIR, ".json")) {
            String nome = p.getFileName().toString();
            if (nome.endsWith(".geo.json")) {
                ids.add(nome.substring(0, nome.length() - ".geo.json".length()));
            }
        }
        return ids;
    }

    private static JsonObject json(String caminho) {
        return JsonParser.parseString(Repo.texto(caminho)).getAsJsonObject();
    }

    private static JsonObject geometria(String id) {
        return json(GEO_DIR + "/" + id + ".geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    private static JsonObject animacoes(String id) {
        return json(ANIM_DIR + "/" + id + ".animation.json").getAsJsonObject("animations");
    }

    // ------------------------------------------------------------------ casos

    @Test
    @DisplayName("quem decide se o clipe repete e o arquivo de animacao, nao o codigo")
    void oLoopMoraNoArquivoDeAnimacao() {
        // POR QUE ESTA REGRA EXISTE, com o caso que a criou: o arquivo do frog declarava
        // emerge como hold_on_last_frame e bite como false, e o codigo pedia os dois com
        // thenLoop(...). No GeckoLib o RawAnimation carrega o LoopType e ELE VENCE o
        // arquivo -- entao o telegrafo reiniciava em vez de segurar a pose, e o artista
        // que abrisse o .animation.json leria uma coisa que o jogo nao faz. Nada disso
        // levanta excecao; e duas fontes para a mesma verdade, decididas em silencio.
        //
        // Animation.LoopType.DEFAULT delega para Animation.loopType() do clipe carregado
        // -- conferido no bytecode do proprio GeckoLib 4.8.3, nao suposto.
        List<String> atalhos = List.of(".thenLoop(", ".thenPlay(", ".thenPlayAndHold(",
                ".thenPlayXTimes(");
        Set<String> violacoes = new LinkedHashSet<>();
        for (Path java : Repo.varrer("src/main/java", ".java")) {
            String caminho = Repo.raiz().relativize(java).toString().replace('\\', '/');
            String texto = Repo.texto(caminho);
            if (!CLIPE_NO_CODIGO.matcher(texto).find()) {
                continue;
            }
            for (String atalho : atalhos) {
                if (texto.contains(atalho)) {
                    violacoes.add(caminho + " -> " + atalho);
                }
            }
        }
        assertTrue(violacoes.isEmpty(),
                "Estes pontos cravam o tipo de repeticao no CODIGO: " + violacoes
                        + ". Use then(nome, Animation.LoopType.DEFAULT) e deixe o"
                        + " .animation.json mandar -- senao o arquivo diz uma coisa, o jogo faz"
                        + " outra, e quem for corrigir a animacao vai mexer no lugar errado.");
    }

    @Test
    @DisplayName("todo modelo proprio tem animacoes, e toda animacao tem modelo")
    void modeloEAnimacaoAndamEmPar() {
        Set<String> comModelo = mobsComModelo();
        assertFalse(comModelo.isEmpty(),
                "Nenhum .geo.json em " + GEO_DIR + ". Varredura vazia nao e aprovacao: se o mod ja"
                        + " teve modelo proprio e agora nao tem, isso e regressao, nao silencio.");

        Set<String> comAnimacao = new LinkedHashSet<>();
        for (Path p : Repo.varrer(ANIM_DIR, ".json")) {
            String nome = p.getFileName().toString();
            if (nome.endsWith(".animation.json")) {
                comAnimacao.add(nome.substring(0, nome.length() - ".animation.json".length()));
            }
        }

        Set<String> semAnimacao = new LinkedHashSet<>(comModelo);
        semAnimacao.removeAll(comAnimacao);
        assertTrue(semAnimacao.isEmpty(),
                "Estes mobs tem modelo e nao tem arquivo de animacao: " + semAnimacao
                        + ". O GeckoLib carrega o modelo e nao reclama: o bicho vira estatua.");

        Set<String> semModelo = new LinkedHashSet<>(comAnimacao);
        semModelo.removeAll(comModelo);
        assertTrue(semModelo.isEmpty(),
                "Estes arquivos de animacao nao tem modelo correspondente: " + semModelo
                        + ". Nenhum clipe tem o que mover, e nada avisa.");
    }

    @Test
    @DisplayName("o identifier do geo e o que o DefaultedEntityGeoModel vai procurar")
    void identifierBateComOId() {
        for (String id : mobsComModelo()) {
            String identifier = geometria(id).getAsJsonObject("description")
                    .get("identifier").getAsString();
            assertTrue(("geometry." + id).equals(identifier),
                    id + ".geo.json declara identifier '" + identifier + "', e o esperado e"
                            + " 'geometry." + id + "'. O modelo nao e encontrado e o mob some da"
                            + " tela -- sem erro no log.");
        }
    }

    @Test
    @DisplayName("nenhum osso aponta para um pai que nao existe")
    void nenhumOssoTemPaiFantasma() {
        for (String id : mobsComModelo()) {
            Map<String, String> paiDe = paiPorOsso(id);
            assertFalse(paiDe.isEmpty(), id + ".geo.json nao declara osso nenhum.");

            for (Map.Entry<String, String> osso : paiDe.entrySet()) {
                String pai = osso.getValue();
                if (pai == null) {
                    continue;
                }
                assertTrue(paiDe.containsKey(pai),
                        id + ": o osso '" + osso.getKey() + "' tem parent '" + pai + "', que nao"
                                + " existe no geo. O GeckoLib nao reclama: o galho inteiro"
                                + " simplesmente nunca e desenhado.");
            }
        }
    }

    @Test
    @DisplayName("todo osso citado numa animacao existe no modelo")
    void animacaoSoMoveOssoQueExiste() {
        for (String id : mobsComModelo()) {
            Set<String> ossos = paiPorOsso(id).keySet();
            JsonObject clipes = animacoes(id);
            assertFalse(clipes.keySet().isEmpty(), id + ".animation.json nao declara clipe nenhum.");

            for (String clipe : clipes.keySet()) {
                JsonElement corpo = clipes.get(clipe);
                if (!corpo.isJsonObject() || !corpo.getAsJsonObject().has("bones")) {
                    continue;
                }
                for (String osso : corpo.getAsJsonObject().getAsJsonObject("bones").keySet()) {
                    assertTrue(ossos.contains(osso),
                            id + ": o clipe '" + clipe + "' move o osso '" + osso + "', que nao"
                                    + " existe no geo. O membro fica parado e o clipe parece"
                                    + " 'meio quebrado', sem nada acusar.");
                }
            }
        }
    }

    @Test
    @DisplayName("todo clipe citado no codigo Java existe no arquivo de animacao")
    void clipeCitadoNoCodigoExiste() {
        Set<String> mobs = mobsComModelo();
        Map<String, Set<String>> declarados = new LinkedHashMap<>();
        for (String id : mobs) {
            declarados.put(id, new LinkedHashSet<>(animacoes(id).keySet()));
        }

        List<String> citados = new ArrayList<>();
        Set<String> ausentes = new LinkedHashSet<>();
        for (Path java : Repo.varrer(FONTE_JAVA, ".java")) {
            String caminho = Repo.raiz().relativize(java).toString().replace('\\', '/');
            Matcher m = CLIPE_NO_CODIGO.matcher(Repo.texto(caminho));
            while (m.find()) {
                String mob = m.group(1);
                if (!declarados.containsKey(mob)) {
                    continue;
                }
                String nomeCompleto = m.group(0);
                citados.add(nomeCompleto);
                if (!declarados.get(mob).contains(nomeCompleto)) {
                    ausentes.add(caminho + " -> " + nomeCompleto);
                }
            }
        }

        assertFalse(citados.isEmpty(),
                "Nenhum literal de clipe encontrado no codigo, mas ha " + mobs + " com modelo"
                        + " proprio. Ou o controller nao toca animacao nenhuma, ou o padrao desta"
                        + " varredura deixou de casar -- nos dois casos este portao estaria"
                        + " imprimindo verde sem verificar nada.");

        assertTrue(ausentes.isEmpty(),
                "Clipe citado no codigo e ausente no arquivo de animacao: " + ausentes
                        + ". ESTE E O ERRO DE DIGITACAO QUE NAO DA ERRO: o GeckoLib procura o"
                        + " clipe, nao acha, e a entidade fica na pose neutra. So aparece para"
                        + " quem estiver olhando o mob na hora exata.");
    }

    @Test
    @DisplayName("a textura do mob tem a folha que o geo declara")
    void texturaTemADimensaoDeclarada() {
        for (String id : mobsComModelo()) {
            JsonObject descricao = geometria(id).getAsJsonObject("description");
            int largura = descricao.get("texture_width").getAsInt();
            int altura = descricao.get("texture_height").getAsInt();

            // raiz().resolve, e nao Repo.arquivo: aquele exige ARQUIVO regular e
            // estoura em diretorio -- a mensagem sairia "arquivo nao existe" para uma
            // pasta que existe, culpando a arte por um erro do portao.
            Path dir = Repo.raiz().resolve(TEXTURA_DIR + "/" + id);
            assertTrue(Files.isDirectory(dir),
                    id + " tem modelo proprio e nenhuma pasta de textura em " + TEXTURA_DIR
                            + "/" + id + ".");

            List<Path> pngs = Repo.varrer(TEXTURA_DIR + "/" + id, ".png");
            assertFalse(pngs.isEmpty(),
                    id + " tem modelo proprio e nenhuma textura. O mob renderiza com a textura"
                            + " faltante do jogo -- o xadrez roxo -- e nada mais acusa.");

            for (Path png : pngs) {
                BufferedImage imagem = ler(png);
                assertTrue(largura == imagem.getWidth() && altura == imagem.getHeight(),
                        id + ": " + png.getFileName() + " tem " + imagem.getWidth() + "x"
                                + imagem.getHeight() + " e o geo declara " + largura + "x" + altura
                                + ". Os UVs sao normalizados pela folha declarada: com a folha"
                                + " errada o jogo estica a imagem sobre as caixas e entrega um"
                                + " bicho borrado, sem um unico erro no log.");
            }
        }
    }

    @Test
    @DisplayName("nenhuma caixa sai da folha nem divide atlas com outra")
    void uvNaoSeAtropela() {
        for (String id : mobsComModelo()) {
            JsonObject geo = geometria(id);
            JsonObject descricao = geo.getAsJsonObject("description");
            int folhaLargura = descricao.get("texture_width").getAsInt();
            int folhaAltura = descricao.get("texture_height").getAsInt();

            List<int[]> retangulos = new ArrayList<>();
            List<String> donos = new ArrayList<>();
            for (JsonElement osso : geo.getAsJsonArray("bones")) {
                JsonObject b = osso.getAsJsonObject();
                if (!b.has("cubes")) {
                    continue;
                }
                for (JsonElement cubo : b.getAsJsonArray("cubes")) {
                    JsonObject c = cubo.getAsJsonObject();
                    // UV por face e explicito e nao usa o layout de caixa: sai da conta.
                    if (!c.has("uv") || !c.get("uv").isJsonArray()) {
                        continue;
                    }
                    JsonArray uv = c.getAsJsonArray("uv");
                    JsonArray tamanho = c.getAsJsonArray("size");
                    int u = uv.get(0).getAsInt();
                    int v = uv.get(1).getAsInt();
                    int w = Math.round(tamanho.get(0).getAsFloat());
                    int h = Math.round(tamanho.get(1).getAsFloat());
                    int d = Math.round(tamanho.get(2).getAsFloat());
                    retangulos.add(new int[] {u, v, 2 * d + 2 * w, d + h});
                    donos.add(b.get("name").getAsString());
                }
            }

            assertFalse(retangulos.isEmpty(), id + ".geo.json nao tem caixa nenhuma com UV.");

            for (int i = 0; i < retangulos.size(); i++) {
                int[] r = retangulos.get(i);
                assertTrue(r[0] + r[2] <= folhaLargura && r[1] + r[3] <= folhaAltura,
                        id + ": a caixa de '" + donos.get(i) + "' ocupa ate (" + (r[0] + r[2])
                                + "," + (r[1] + r[3]) + ") numa folha de " + folhaLargura + "x"
                                + folhaAltura + ". Fora da folha o jogo repete ou transparenta a"
                                + " textura, e nada avisa.");
            }

            for (int i = 0; i < retangulos.size(); i++) {
                for (int j = i + 1; j < retangulos.size(); j++) {
                    int[] a = retangulos.get(i);
                    int[] b = retangulos.get(j);
                    boolean cruza = a[0] < b[0] + b[2] && b[0] < a[0] + a[2]
                            && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
                    assertFalse(cruza,
                            id + ": as caixas de '" + donos.get(i) + "' e '" + donos.get(j)
                                    + "' dividem o mesmo pedaco do atlas. Uma pinta por cima da"
                                    + " outra; em jogo isso le como erro de arte, e nao como o"
                                    + " erro de layout que e.");
                }
            }
        }
    }

    // ------------------------------------------------------------- utilitario

    /** Nome do osso -> nome do pai (nulo na raiz). */
    private static Map<String, String> paiPorOsso(String id) {
        Map<String, String> paiDe = new LinkedHashMap<>();
        for (JsonElement osso : geometria(id).getAsJsonArray("bones")) {
            JsonObject b = osso.getAsJsonObject();
            paiDe.put(b.get("name").getAsString(),
                    b.has("parent") ? b.get("parent").getAsString() : null);
        }
        return paiDe;
    }

    private static BufferedImage ler(Path png) {
        try {
            BufferedImage imagem = ImageIO.read(png.toFile());
            if (imagem == null) {
                throw new IllegalStateException(png + " nao e um PNG legivel.");
            }
            return imagem;
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }
}
