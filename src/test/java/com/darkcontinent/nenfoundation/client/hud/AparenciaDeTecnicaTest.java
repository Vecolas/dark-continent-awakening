package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.nen.technique.NenTechnique;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A aparencia das tecnicas na tela: uma fonte so, e distinguivel sem ler. */
class AparenciaDeTecnicaTest {

    /**
     * Toda tecnica que existe, tirada do PACOTE e nao de uma lista aqui.
     *
     * <p>ESTE METODO SUBSTITUIU UMA LISTA ESCRITA A MAO, e a lista tinha tres
     * nomes: Ten, Ren e Zetsu. Ela foi escrita quando essas eram todas as
     * tecnicas do mod. Depois chegaram Gyo, Shu, Ken e Ko -- e o arquivo de
     * teste nao sabia. <b>Ken foi acrescentado ao mapa de aparencias e nunca
     * passou por prova nenhuma; Gyo, Shu e Ko nao foram acrescentados e
     * ninguem reclamou.</b>
     *
     * <p>Esse e o defeito exato que uma lista paralela produz: ela nao fica
     * errada, ela fica VELHA -- e velha e verde ao mesmo tempo. O portao que
     * existia para garantir que nenhuma tecnica caisse na forma neutra passou a
     * garantir isso sobre tres tecnicas de sete, sem avisar.
     *
     * <p>A oitava tecnica entra nesta prova sozinha, no dia em que o arquivo
     * dela existir.
     */
    private static List<ResourceLocation> todasAsTecnicas() {
        Path pacote = Repo.raiz().resolve("src/main/java/com/darkcontinent/nenfoundation/nen/technique");
        List<ResourceLocation> ids = new ArrayList<>();
        try (Stream<Path> arquivos = Files.list(pacote)) {
            for (Path arquivo : arquivos.sorted().toList()) {
                String nome = arquivo.getFileName().toString();
                if (!nome.endsWith(".java") || nome.equals("package-info.java")) {
                    continue;
                }
                Class<?> classe;
                try {
                    classe = Class.forName("com.darkcontinent.nenfoundation.nen.technique."
                            + nome.substring(0, nome.length() - ".java".length()));
                } catch (ClassNotFoundException e) {
                    throw new AssertionError("classe nao encontrada para " + nome, e);
                }
                // SO IMPLEMENTACAO CONCRETA: o pacote tambem guarda a interface
                // NenTechnique, as capacidades (ConsomeAura, ProtegeComAura) e o
                // registro. Nenhuma delas aparece na tela.
                if (!NenTechnique.class.isAssignableFrom(classe)
                        || classe.isInterface()
                        || Modifier.isAbstract(classe.getModifiers())) {
                    continue;
                }
                ids.add(idDe(classe));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertTrue(ids.size() >= 4,
                "achei so " + ids.size() + " tecnica(s) no pacote. Se a varredura"
                        + " parar de achar arquivos, este teste passa sem verificar"
                        + " nada -- e e por isso que ele confere o proprio achado.");
        return ids;
    }

    private static ResourceLocation idDe(Class<?> classe) {
        try {
            Field campo = classe.getField("ID");
            assertTrue(Modifier.isStatic(campo.getModifiers()),
                    classe.getSimpleName() + ".ID nao e estatico.");
            return (ResourceLocation) campo.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(classe.getSimpleName()
                    + " implementa NenTechnique e nao expoe um ID publico estatico."
                    + " Sem ele esta prova nao consegue ver a tecnica, e ela"
                    + " entraria na tela sem aparencia nenhuma.", e);
        }
    }

    @Test
    @DisplayName("nenhuma tecnica compartilha cor com outra")
    void coresDistintas() {
        Set<Integer> cores = new LinkedHashSet<>();
        for (ResourceLocation id : todasAsTecnicas()) {
            assertTrue(cores.add(AparenciaDeTecnica.de(id).cor()),
                    "Duas tecnicas usam a mesma cor; a segunda foi " + id
                            + ". O criterio da #89 e distinguir OLHANDO, e duas"
                            + " tecnicas da mesma cor so se separam lendo o nome.");
        }
    }

    @Test
    @DisplayName("nenhuma tecnica compartilha FORMA com outra")
    void formasDistintas() {
        // COR SOZINHA NAO BASTA, e esta e a metade do criterio que some com
        // facilidade: cerca de um em doze homens nao separa vermelho de verde,
        // e o indicador da HUD tem nove pixels. Se as tres virassem a mesma
        // forma, o teste de cor acima continuaria passando -- e a tela ficaria
        // ilegivel para quem nao distingue as cores.
        Set<AparenciaDeTecnica.Forma> formas = new LinkedHashSet<>();
        for (ResourceLocation id : todasAsTecnicas()) {
            assertTrue(formas.add(AparenciaDeTecnica.de(id).forma()),
                    "Duas tecnicas usam a mesma forma; a segunda foi " + id);
        }
    }

    @Test
    @DisplayName("nenhuma das conhecidas cai na forma NEUTRA")
    void conhecidasNaoSaoNeutras() {
        // A forma NEUTRA e a saida de emergencia para id desconhecido. Se uma
        // tecnica de verdade cair nela, e porque o mapa esqueceu dela -- e o
        // sintoma em jogo seria um disco sem significado, que nao parece erro.
        for (ResourceLocation id : todasAsTecnicas()) {
            assertTrue(AparenciaDeTecnica.de(id).forma() != AparenciaDeTecnica.Forma.NEUTRA,
                    id + " caiu na forma neutra; ela nao esta no mapa de conhecidas.");
        }
    }

    @Test
    @DisplayName("tecnica desconhecida nao quebra, e ganha cor estavel")
    void desconhecidaNaoQuebra() {
        ResourceLocation inventada = ResourceLocation.fromNamespaceAndPath("outromod", "hatsu_x");

        AparenciaDeTecnica.Aparencia primeira = AparenciaDeTecnica.de(inventada);
        assertNotNull(primeira, "id desconhecido devolveu nulo; a HUD cairia inteira.");
        assertEquals(AparenciaDeTecnica.Forma.NEUTRA, primeira.forma(),
                "id desconhecido devia cair na forma neutra.");

        // ESTAVEL ENTRE CHAMADAS, e o teste existe porque a alternativa obvia
        // -- sortear uma cor -- passaria em todo o resto deste arquivo. A cor
        // mudaria a cada entrada no mundo, e o jogador nunca aprenderia nada.
        assertEquals(primeira.cor(), AparenciaDeTecnica.de(inventada).cor(),
                "a cor derivada mudou entre duas chamadas com o mesmo id.");
    }

    @Test
    @DisplayName("a cor derivada nunca sai escura demais nem transparente")
    void corDerivadaELegivel() {
        // O indicador aparece SOBRE O MUNDO. Uma cor quase preta some contra
        // qualquer caverna, e alfa zero some contra tudo -- e nenhum dos dois
        // da erro: o indicador simplesmente nao aparece, e o relato vira "a
        // tecnica nao liga".
        for (String nome : List.of("a", "zzz", "hatsu_1", "tecnica_com_nome_longo", "")) {
            int cor = AparenciaDeTecnica.corDerivadaDe(
                    ResourceLocation.fromNamespaceAndPath("outromod", nome.isEmpty() ? "x" : nome));
            assertEquals(0xFF, (cor >>> 24) & 0xFF, "alfa nao ficou opaco para " + nome);
            for (int deslocamento : List.of(16, 8, 0)) {
                int canal = (cor >>> deslocamento) & 0xFF;
                assertTrue(canal >= 0x60,
                        "canal escuro demais (" + canal + ") para " + nome
                                + ": o indicador sumiria contra fundo escuro.");
            }
        }
    }
}
