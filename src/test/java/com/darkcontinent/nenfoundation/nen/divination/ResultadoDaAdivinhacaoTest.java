package com.darkcontinent.nenfoundation.nen.divination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da tabela de reacoes da Water Divination.
 *
 * <p>O criterio de aceite da issue e "cada uma das seis categorias produz um
 * resultado DISTINGUIVEL". Este portao transforma isso em algo que da para
 * perguntar ao codigo, em vez de algo que so da para conferir lendo.
 *
 * <p>Ele cobre a metade que roda sem o jogo: cobertura das seis, busca, e as
 * frases nos dois idiomas. A distincao de PARTICULA e SOM esta no gametest,
 * porque ler {@code ParticleTypes} exige o registro do Minecraft de pe.
 */
class ResultadoDaAdivinhacaoTest {

    private static final List<String> IDIOMAS = List.of("pt_br", "en_us");

    private static String caminhoDoLang(String idioma) {
        return "src/main/resources/assets/nenfoundation/lang/" + idioma + ".json";
    }

    @Test
    @DisplayName("as seis categorias reais tem reacao, e o neutro nao tem")
    void asSeisEstaoCobertas() {
        Set<NenCategory> cobertas = EnumSet.noneOf(NenCategory.class);
        for (ResultadoDaAdivinhacao resultado : ResultadoDaAdivinhacao.values()) {
            cobertas.add(resultado.categoria());
        }

        assertEquals(EnumSet.copyOf(NenCategory.REAIS), cobertas,
                "A tabela nao cobre exatamente as seis. Uma categoria sem reacao"
                        + " nao da erro: o jogador clica, a agua nao faz nada, e"
                        + " o relato de bug e 'nao aconteceu nada'.");

        assertEquals(NenCategory.REAIS.size(), ResultadoDaAdivinhacao.values().length,
                "Ha mais resultados que categorias: alguma categoria tem duas"
                        + " reacoes, e qual delas sai vira detalhe de ordem do enum.");

        assertTrue(ResultadoDaAdivinhacao.para(NenCategory.UNDETERMINED).isEmpty(),
                "O neutro ganhou reacao. Ele e a AUSENCIA de categoria: a agua"
                        + " reagiria para quem nao tem nada a descobrir.");
    }

    @Test
    @DisplayName("cada categoria e alcancavel pela busca, e devolve a sua reacao")
    void buscaPorCategoriaFunciona() {
        for (ResultadoDaAdivinhacao resultado : ResultadoDaAdivinhacao.values()) {
            assertEquals(resultado,
                    ResultadoDaAdivinhacao.para(resultado.categoria()).orElseThrow(),
                    "a busca por " + resultado.categoria() + " nao devolveu " + resultado);
        }
    }

    @Test
    @DisplayName("as frases dos seis sao distintas")
    void asFrasesSaoDistintas() {
        // PARTICULA E SOM NAO SAO CONFERIDOS AQUI, e isso e uma limitacao da
        // camada, nao um esquecimento: ler ParticleTypes exige o registro do
        // Minecraft de pe, e o JUnit puro deste projeto nao o tem (Bootstrap
        // precisa do FML carregado). A distincao daqueles dois sinais esta no
        // gametest, com o jogo rodando.
        conferirDistincao("chave de traducao",
                ResultadoDaAdivinhacao::chaveDeTraducao);
        conferirDistincao("chave de repeticao",
                ResultadoDaAdivinhacao::chaveDeRepeticao);
    }

    private static void conferirDistincao(
            String sinal, java.util.function.Function<ResultadoDaAdivinhacao, String> lente) {

        Set<String> vistos = new LinkedHashSet<>();
        List<String> repetidos = new ArrayList<>();
        for (ResultadoDaAdivinhacao resultado : ResultadoDaAdivinhacao.values()) {
            String valor = lente.apply(resultado);
            if (!vistos.add(valor)) {
                repetidos.add(resultado.categoria().getSerializedName() + " usa " + valor);
            }
        }
        assertTrue(repetidos.isEmpty(),
                "Dois resultados compartilham o mesmo " + sinal + ": " + repetidos
                        + ". Textos diferentes nao salvam: quem joga com o chat"
                        + " fechado ve os outros dois sinais, e so eles.");
    }

    @Test
    @DisplayName("toda frase existe nos dois idiomas, e nenhuma sobra")
    void traducoesCasam() {
        Set<String> esperadas = new LinkedHashSet<>();
        esperadas.add("nenfoundation.divinacao.sem_nen");
        for (ResultadoDaAdivinhacao resultado : ResultadoDaAdivinhacao.values()) {
            esperadas.add(resultado.chaveDeTraducao());
            esperadas.add(resultado.chaveDeRepeticao());
        }

        for (String idioma : IDIOMAS) {
            Set<String> presentes = chavesDeDivinacao(Repo.texto(caminhoDoLang(idioma)));

            Set<String> faltando = new LinkedHashSet<>(esperadas);
            faltando.removeAll(presentes);
            assertTrue(faltando.isEmpty(),
                    idioma + ": frase sem traducao " + faltando
                            + ". A tela mostraria a chave crua, sem nenhum erro"
                            + " no log -- e num momento de descoberta.");

            Set<String> sobrando = new LinkedHashSet<>(presentes);
            sobrando.removeAll(esperadas);
            assertTrue(sobrando.isEmpty(),
                    idioma + ": frase de divinacao que nao existe mais " + sobrando
                            + ". O portao morde dos dois lados: chave orfa e o"
                            + " rastro de um rename que ficou pela metade.");
        }
    }

    @Test
    @DisplayName("toda frase de resultado nomeia a categoria descoberta")
    void asFrasesRecebemACategoria() {
        // As frases recebem a categoria como argumento. Uma frase sem o %s
        // compila, traduz e sai na tela sem dizer QUAL e a categoria -- que e a
        // unica informacao que o jogador foi ali buscar.
        for (String idioma : IDIOMAS) {
            String json = Repo.texto(caminhoDoLang(idioma));
            for (ResultadoDaAdivinhacao resultado : ResultadoDaAdivinhacao.values()) {
                for (String chave : List.of(resultado.chaveDeTraducao(),
                                            resultado.chaveDeRepeticao())) {
                    String frase = valorDe(json, chave);
                    assertTrue(frase.contains("%s"),
                            idioma + ": a frase " + chave + " nao tem %s, entao ela"
                                    + " nao diz qual categoria foi descoberta: \""
                                    + frase + "\"");
                }
            }
        }
    }

    private static Set<String> chavesDeDivinacao(String json) {
        Pattern p = Pattern.compile("\"(nenfoundation\\.divinacao\\.[a-z_.]+)\"\\s*:");
        Matcher m = p.matcher(json);
        Set<String> achadas = new LinkedHashSet<>();
        while (m.find()) {
            achadas.add(m.group(1));
        }
        return achadas;
    }

    private static String valorDe(String json, String chave) {
        Matcher m = Pattern.compile(
                        "\"" + Pattern.quote(chave) + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        assertTrue(m.find(), "chave ausente no lang: " + chave);
        return m.group(1);
    }

    @Test
    @DisplayName("a lista de idiomas do portao bate com os arquivos que existem")
    void oPortaoOlhaTodosOsIdiomas() {
        // Sem isto, acrescentar um idioma novo passaria a nao ser conferido, e
        // o portao continuaria verde olhando so para os dois antigos.
        Set<String> noDisco = Repo.varrer(
                        "src/main/resources/assets/nenfoundation/lang", ".json").stream()
                .map(p -> p.getFileName().toString().replace(".json", ""))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertEquals(new LinkedHashSet<>(IDIOMAS), noDisco,
                "Os idiomas no disco mudaram. Atualize a lista deste portao,"
                        + " senao o idioma novo nasce sem nenhuma conferencia.");
    }
}
