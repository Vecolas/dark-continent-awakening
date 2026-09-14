package com.darkcontinent.nenfoundation.sound;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da VOZ dos inimigos (EN13 / issue #148).
 *
 * <p><b>A falha central aqui nao levanta excecao e nao aparece em log:</b> um
 * {@code .wav} renomeado para {@code .ogg} carrega MUDO. O Minecraft aceita o
 * arquivo, nao reclama, e o mob simplesmente nao emite som. Nenhum portao de
 * build ouve nada, entao a unica defesa e conferir a ASSINATURA do arquivo --
 * e e isso que este teste faz, byte a byte.</p>
 *
 * <p><b>Ele morde dos dois lados.</b> Mob registrado sem voz reprova, e arquivo
 * de som sem mob reprova. Sem a segunda metade, um mob renomeado deixaria 5
 * arquivos orfaos no repositorio para sempre, e o "verde" so diria que o que
 * sobrou esta certo.</p>
 */
class VozDeInimigoTest {

    private static final String SONS = "src/main/resources/assets/nenfoundation/sounds/entity";
    private static final String SOUNDS_JSON = "src/main/resources/assets/nenfoundation/sounds.json";
    private static final List<String> IDIOMAS = List.of(
            "src/main/resources/assets/nenfoundation/lang/en_us.json",
            "src/main/resources/assets/nenfoundation/lang/pt_br.json");

    /** Tamanho abaixo do qual um .ogg nao pode conter audio util. */
    private static final long BYTES_MINIMOS = 512L;

    // ----------------------------------------------------------- arquivos

    @Test
    @DisplayName("todo mob com voz tem os CINCO arquivos, e todos sao Ogg Vorbis de verdade")
    void todaVozTemOsCincoArquivosEmVorbis() {
        assertFalse(EnemyVoiceCatalog.COM_VOZ.isEmpty(),
                "Lista de vozes vazia: a varredura nao olharia nada, e varredura vazia nao e"
                        + " aprovacao.");

        Set<String> semArquivo = new TreeSet<>();
        Set<String> naoVorbis = new TreeSet<>();
        Set<String> pequenoDemais = new TreeSet<>();

        for (String mob : EnemyVoiceCatalog.COM_VOZ) {
            for (String momento : EnemyVoiceCatalog.MOMENTOS) {
                Path arquivo = Repo.raiz().resolve(SONS).resolve(mob).resolve(momento + ".ogg");
                if (!Files.isRegularFile(arquivo)) {
                    semArquivo.add(mob + "/" + momento);
                    continue;
                }
                byte[] cabecalho = ler(arquivo, 64);
                if (!comecaCom(cabecalho, "OggS") || !contem(cabecalho, "vorbis")) {
                    naoVorbis.add(mob + "/" + momento);
                }
                if (tamanho(arquivo) < BYTES_MINIMOS) pequenoDemais.add(mob + "/" + momento);
            }
        }

        assertTrue(semArquivo.isEmpty(),
                "Eventos registrados sem arquivo: " + semArquivo + ". O evento existe, o jogo"
                        + " tenta tocar, nao acha, e NAO reclama -- o mob fica mudo naquele"
                        + " momento especifico, que e o mais dificil de perceber de todos.");

        assertTrue(naoVorbis.isEmpty(),
                "Arquivos que nao sao Ogg Vorbis: " + naoVorbis + ". Um .wav renomeado para"
                        + " .ogg CARREGA MUDO: o Minecraft aceita, nao loga nada, e o mob nao"
                        + " emite som. Nao ha erro para procurar depois.");

        assertTrue(pequenoDemais.isEmpty(),
                "Arquivos com menos de " + BYTES_MINIMOS + " bytes: " + pequenoDemais
                        + ". Um arquivo vazio soa exatamente como nenhum arquivo.");
    }

    @Test
    @DisplayName("nenhum arquivo de som orfao -- o portao morde o outro lado tambem")
    void nenhumArquivoOrfao() {
        Path raiz = Repo.raiz().resolve(SONS);
        if (!Files.isDirectory(raiz)) return;

        Set<String> conhecidos = new LinkedHashSet<>(EnemyVoiceCatalog.COM_VOZ);
        Set<String> orfaos = new TreeSet<>();
        try (var pastas = Files.list(raiz)) {
            pastas.filter(Files::isDirectory)
                    .map(pasta -> pasta.getFileName().toString())
                    .filter(mob -> !conhecidos.contains(mob))
                    .forEach(orfaos::add);
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }

        assertTrue(orfaos.isEmpty(),
                "Pastas de som sem mob correspondente: " + orfaos + ". Sem esta metade, um mob"
                        + " renomeado deixaria os arquivos antigos no repositorio para sempre, e"
                        + " o verde so diria que o que sobrou esta certo.");
    }

    // -------------------------------------------------------- sounds.json

    @Test
    @DisplayName("todo evento de voz esta em sounds.json, apontando para o arquivo certo")
    void soundsJsonCobreTodosOsEventos() {
        JsonObject json = JsonParser.parseString(Repo.texto(SOUNDS_JSON)).getAsJsonObject();

        Set<String> ausentes = new TreeSet<>();
        Set<String> apontandoParaOVazio = new TreeSet<>();
        for (String mob : EnemyVoiceCatalog.COM_VOZ) {
            for (String momento : EnemyVoiceCatalog.MOMENTOS) {
                String chave = "entity." + mob + "." + momento;
                if (!json.has(chave)) {
                    ausentes.add(chave);
                    continue;
                }
                String nome = json.getAsJsonObject(chave).getAsJsonArray("sounds")
                        .get(0).getAsJsonObject().get("name").getAsString();
                // "nenfoundation:entity/great_stamp/ambient" -> o .ogg no disco
                String caminho = nome.substring(nome.indexOf(':') + 1);
                Path arquivo = Repo.raiz()
                        .resolve("src/main/resources/assets/nenfoundation/sounds")
                        .resolve(caminho + ".ogg");
                if (!Files.isRegularFile(arquivo)) apontandoParaOVazio.add(chave + " -> " + nome);
            }
        }

        assertTrue(ausentes.isEmpty(),
                "Eventos registrados em Java e ausentes de sounds.json: " + ausentes
                        + ". O registro carrega, o evento existe, e o jogo nao sabe qual"
                        + " arquivo tocar -- silencio, sem log.");

        assertTrue(apontandoParaOVazio.isEmpty(),
                "Entradas de sounds.json apontando para arquivo inexistente: "
                        + apontandoParaOVazio + ". O caminho errado nao reclama.");
    }

    @Test
    @DisplayName("toda entrada de voz em sounds.json tem legenda nos DOIS idiomas")
    void toLegendaExisteNosDoisIdiomas() {
        List<String> linguas = IDIOMAS.stream().map(Repo::texto).toList();
        Set<String> semLegenda = new TreeSet<>();
        for (String mob : EnemyVoiceCatalog.COM_VOZ) {
            for (String momento : EnemyVoiceCatalog.MOMENTOS) {
                String chave = "\"subtitles.nenfoundation.entity." + mob + "." + momento + "\"";
                if (linguas.stream().anyMatch(texto -> !texto.contains(chave))) {
                    semLegenda.add(mob + "." + momento);
                }
            }
        }
        assertTrue(semLegenda.isEmpty(),
                "Sons sem legenda nos dois idiomas: " + semLegenda + ". Quem joga com legenda"
                        + " ligada -- e quem depende dela para jogar -- ve a chave crua na"
                        + " tela, e chave crua parece defeito do mod.");
    }

    // ------------------------------------------------------------- catalogo

    @Test
    @DisplayName("PORTAO: nenhum inimigo publicado chega ao jogo MUDO")
    void nenhumInimigoPublicadoFicaMudo() {
        Set<String> mudos = new TreeSet<>(EnemyCatalog.publicados().keySet());
        mudos.removeAll(new LinkedHashSet<>(EnemyVoiceCatalog.COM_VOZ));

        assertTrue(mudos.isEmpty(),
                "Inimigos publicados e sem voz: " + mudos + ". Silencio nao reprova nada"
                        + " sozinho: o mob nasce, anda, ataca e morre sem um som, e o relato de"
                        + " bug possivel e 'parece que falta alguma coisa'. Ate a issue #148"
                        + " isso valia para os sete primeiros, e o custo foi meses.");
    }

    @Test
    @DisplayName("a lista de vozes nao repete mob")
    void listaDeVozesNaoRepete() {
        assertEquals(EnemyVoiceCatalog.COM_VOZ.size(),
                new LinkedHashSet<>(EnemyVoiceCatalog.COM_VOZ).size(),
                "Id repetido na lista registraria o mesmo evento duas vezes, e o segundo"
                        + " registro derrubaria o carregamento do mod inteiro.");
        assertEquals(5, EnemyVoiceCatalog.MOMENTOS.size());
    }

    // ------------------------------------------------------------- auxiliares

    private static byte[] ler(Path arquivo, int quantos) {
        try (var entrada = Files.newInputStream(arquivo)) {
            return entrada.readNBytes(quantos);
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }

    private static long tamanho(Path arquivo) {
        try {
            return Files.size(arquivo);
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }

    private static boolean comecaCom(byte[] dados, String assinatura) {
        byte[] esperado = assinatura.getBytes(StandardCharsets.US_ASCII);
        if (dados.length < esperado.length) return false;
        for (int i = 0; i < esperado.length; i++) {
            if (dados[i] != esperado[i]) return false;
        }
        return true;
    }

    private static boolean contem(byte[] dados, String trecho) {
        byte[] alvo = trecho.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i + alvo.length <= dados.length; i++) {
            boolean bate = true;
            for (int j = 0; j < alvo.length; j++) {
                if (dados[i + j] != alvo[j]) { bate = false; break; }
            }
            if (bate) return true;
        }
        return false;
    }
}
