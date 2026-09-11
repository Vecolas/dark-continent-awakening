package com.darkcontinent.nenfoundation.nen.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da matriz de afinidade.
 *
 * <p>Ele mede duas coisas diferentes, e as duas importam:
 *
 * <p>1. que a VALIDACAO morde dos dois lados -- par faltando E par sobrando;
 *
 * <p>2. que o arquivo que o mod REALMENTE DISTRIBUI passa nessa validacao. Sem
 * o segundo, a matriz do jar poderia estar quebrada e o unico sintoma seria uma
 * linha de log num servidor de alguem -- com toda afinidade respondendo zero e
 * nada acusando no build.
 */
class MatrizDeAfinidadeTest {

    /** O caminho do arquivo que vai dentro do jar. */
    private static final String CAMINHO =
            "src/main/resources/data/nenfoundation/nen_afinidade/matriz.json";

    private static Afinidade a(double valor) {
        return new Afinidade(valor, valor, valor);
    }

    /** Uma matriz 5x5 completa, com um valor qualquer. Base para as mutacoes. */
    private static Map<NenCategory, Map<NenCategory, Afinidade>> comunsCompletas() {
        Map<NenCategory, Map<NenCategory, Afinidade>> fora = new LinkedHashMap<>();
        for (NenCategory origem : MatrizDeAfinidade.COMUNS) {
            Map<NenCategory, Afinidade> linha = new LinkedHashMap<>();
            for (NenCategory alvo : MatrizDeAfinidade.COMUNS) {
                linha.put(alvo, a(origem == alvo ? 1.0D : 0.5D));
            }
            fora.put(origem, linha);
        }
        return fora;
    }

    private static MatrizDeAfinidade matriz(
            Map<NenCategory, Map<NenCategory, Afinidade>> comuns) {
        return new MatrizDeAfinidade(comuns,
                new MatrizDeAfinidade.RegraDeEspecializacao(a(1.0D), a(0.0D), a(0.6D)),
                Optional.empty());
    }

    // ------------------------------------------------ o arquivo distribuido

    @Test
    @DisplayName("a matriz que o mod distribui e lida pelo codec e passa na validacao")
    void arquivoDistribuidoEValido() {
        JsonElement json = new Gson().fromJson(Repo.texto(CAMINHO), JsonElement.class);

        MatrizDeAfinidade lida = MatrizDeAfinidade.CODEC
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow(erro -> new AssertionError(
                        "O codec recusou " + CAMINHO + ": " + erro
                                + ". Este arquivo vai DENTRO do jar; quebrado, o"
                                + " unico sintoma em producao e uma linha de log e"
                                + " toda afinidade respondendo zero."));

        assertEquals(List.of(), lida.problemas(),
                "A matriz distribuida esta incompleta ou tem pares a mais.");
    }

    @Test
    @DisplayName("a matriz distribuida declara a incerteza de canone no proprio dado")
    void arquivoDistribuidoCarregaANota() {
        JsonElement json = new Gson().fromJson(Repo.texto(CAMINHO), JsonElement.class);
        MatrizDeAfinidade lida = MatrizDeAfinidade.CODEC
                .parse(JsonOps.INSTANCE, json).getOrThrow(AssertionError::new);

        assertTrue(lida.nota().isPresent(),
                "A matriz perdeu a nota. A incerteza sobre o Specialist precisa"
                        + " viajar COM o dado: quem abrir a matriz num pack de"
                        + " terceiros nao vai ler o javadoc.");
        assertTrue(lida.nota().orElseThrow().toLowerCase().contains("canone"),
                "A nota existe mas nao fala do canone; ela e o lugar onde a"
                        + " incerteza fica escrita.");
    }

    @Test
    @DisplayName("a matriz distribuida e simetrica entre as cinco comuns")
    void arquivoDistribuidoESimetrico() {
        JsonElement json = new Gson().fromJson(Repo.texto(CAMINHO), JsonElement.class);
        MatrizDeAfinidade lida = MatrizDeAfinidade.CODEC
                .parse(JsonOps.INSTANCE, json).getOrThrow(AssertionError::new);

        // A tabela classica e por DISTANCIA no diagrama, e distancia e
        // simetrica. Uma celula digitada errado quase sempre quebra a simetria
        // -- e sem este portao ela so apareceria como "essa combinacao parece
        // fraca", meses depois, numa sessao de balanceamento.
        for (NenCategory origem : MatrizDeAfinidade.COMUNS) {
            for (NenCategory alvo : MatrizDeAfinidade.COMUNS) {
                assertEquals(lida.entre(origem, alvo), lida.entre(alvo, origem),
                        "assimetria entre " + origem.getSerializedName()
                                + " e " + alvo.getSerializedName());
            }
        }
    }

    @Test
    @DisplayName("na matriz distribuida, a propria categoria e o maior valor da linha")
    void aPropriaCategoriaEOMaiorDaLinha() {
        JsonElement json = new Gson().fromJson(Repo.texto(CAMINHO), JsonElement.class);
        MatrizDeAfinidade lida = MatrizDeAfinidade.CODEC
                .parse(JsonOps.INSTANCE, json).getOrThrow(AssertionError::new);

        for (NenCategory origem : MatrizDeAfinidade.COMUNS) {
            double propria = lida.entre(origem, origem).effectiveness();
            for (NenCategory alvo : MatrizDeAfinidade.COMUNS) {
                if (alvo == origem) {
                    continue;
                }
                assertTrue(lida.entre(origem, alvo).effectiveness() <= propria,
                        origem.getSerializedName() + " rende mais em "
                                + alvo.getSerializedName() + " do que na propria"
                                + " categoria. Duas celulas trocadas de lugar"
                                + " produzem exatamente isto, e nada acusa.");
            }
        }
    }

    // ------------------------------------------------ a validacao morde

    @Test
    @DisplayName("matriz completa nao tem problema nenhum")
    void completaPassa() {
        assertEquals(List.of(), matriz(comunsCompletas()).problemas());
    }

    @Test
    @DisplayName("par FALTANDO reprova")
    void parFaltandoReprova() {
        Map<NenCategory, Map<NenCategory, Afinidade>> comuns = comunsCompletas();
        Map<NenCategory, Afinidade> linha = new LinkedHashMap<>(
                comuns.get(NenCategory.EMISSION));
        linha.remove(NenCategory.CONJURATION);
        comuns.put(NenCategory.EMISSION, linha);

        List<String> problemas = matriz(comuns).problemas();

        assertFalse(problemas.isEmpty(),
                "Uma matriz com um par a menos e JSON perfeitamente valido, e"
                        + " produziria zero numa combinacao so.");
        assertTrue(problemas.stream().anyMatch(
                        p -> p.contains("emission") && p.contains("conjuration")),
                "o problema nao nomeia o par que falta: " + problemas);
    }

    @Test
    @DisplayName("linha FALTANDO reprova")
    void linhaFaltandoReprova() {
        Map<NenCategory, Map<NenCategory, Afinidade>> comuns = comunsCompletas();
        comuns.remove(NenCategory.MANIPULATION);

        List<String> problemas = matriz(comuns).problemas();

        assertTrue(problemas.stream().anyMatch(p -> p.contains("manipulation")),
                "linha inteira faltando passou despercebida: " + problemas);
    }

    @Test
    @DisplayName("Specialization DENTRO da matriz reprova, nos dois sentidos")
    void especializacaoNaMatrizReprova() {
        // O portao morde dos dois lados de proposito. Specialization tem regra
        // PROPRIA; uma linha dela na matriz parece completar a tabela e
        // contradiz a regra separada -- e a matriz venceria em silencio.
        Map<NenCategory, Map<NenCategory, Afinidade>> comoAlvo = comunsCompletas();
        Map<NenCategory, Afinidade> linha = new LinkedHashMap<>(
                comoAlvo.get(NenCategory.ENHANCEMENT));
        linha.put(NenCategory.SPECIALIZATION, a(0.4D));
        comoAlvo.put(NenCategory.ENHANCEMENT, linha);

        assertTrue(matriz(comoAlvo).problemas().stream()
                        .anyMatch(p -> p.contains("sobra") && p.contains("specialization")),
                "Specialization como ALVO na matriz passou: "
                        + matriz(comoAlvo).problemas());

        Map<NenCategory, Map<NenCategory, Afinidade>> comoOrigem = comunsCompletas();
        comoOrigem.put(NenCategory.SPECIALIZATION, comunsCompletas().get(NenCategory.EMISSION));

        assertTrue(matriz(comoOrigem).problemas().stream()
                        .anyMatch(p -> p.contains("sobra") && p.contains("specialization")),
                "Specialization como ORIGEM na matriz passou: "
                        + matriz(comoOrigem).problemas());
    }

    @Test
    @DisplayName("UNDETERMINED dentro da matriz reprova")
    void neutroNaMatrizReprova() {
        Map<NenCategory, Map<NenCategory, Afinidade>> comuns = comunsCompletas();
        Map<NenCategory, Afinidade> linha = new LinkedHashMap<>(
                comuns.get(NenCategory.ENHANCEMENT));
        linha.put(NenCategory.UNDETERMINED, a(0.1D));
        comuns.put(NenCategory.ENHANCEMENT, linha);

        assertTrue(matriz(comuns).problemas().stream().anyMatch(p -> p.contains("sobra")),
                "O neutro e a AUSENCIA de categoria. Uma celula para ele na"
                        + " matriz e um numero que nunca vai ser lido, e que"
                        + " sugere que alguem podia ter afinidade com nada.");
    }

    @Test
    @DisplayName("a lista COMUNS e derivada do enum, e nao escrita a mao")
    void comunsEDerivada() {
        assertEquals(NenCategory.REAIS.size() - 1, MatrizDeAfinidade.COMUNS.size());
        assertFalse(MatrizDeAfinidade.COMUNS.contains(NenCategory.SPECIALIZATION));
        assertFalse(MatrizDeAfinidade.COMUNS.contains(NenCategory.UNDETERMINED));
    }

    // ---------------------------------------------------- numeros invalidos

    @Test
    @DisplayName("NaN, infinito e negativo sao recusados na entrada")
    void numeroInvalidoNaoEntra() {
        // NaN e o caso que motiva isto: ele atravessa multiplicacao sem erro, e
        // o sintoma final e dano NaN numa habilidade tres marcos adiante.
        assertThrows(IllegalArgumentException.class,
                () -> new Afinidade(Double.NaN, 1.0D, 1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new Afinidade(1.0D, Double.POSITIVE_INFINITY, 1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new Afinidade(1.0D, 1.0D, -0.5D));
    }

    @Test
    @DisplayName("campo faltando no JSON e recusado, e nao recebe default")
    void campoFaltandoERecusado() {
        // Um default silencioso deixaria quem errasse o nome do campo no
        // datapack com a afinidade errada num canto so da matriz, sem erro.
        JsonElement json = new Gson().fromJson(
                "{\"learning_rate\": 1.0, \"max_proficiency\": 1.0}", JsonElement.class);

        assertTrue(Afinidade.CODEC.parse(JsonOps.INSTANCE, json).isError(),
                "O codec aceitou uma afinidade sem effectiveness.");
    }

    // ------------------------------------------------------ a regra propria

    @Test
    @DisplayName("Specialization sai da regra propria, nos tres casos")
    void regraPropriaCobreOsTresCasos() {
        MatrizDeAfinidade m = new MatrizDeAfinidade(comunsCompletas(),
                new MatrizDeAfinidade.RegraDeEspecializacao(a(1.0D), a(0.0D), a(0.6D)),
                Optional.empty());

        assertEquals(a(1.0D),
                m.entre(NenCategory.SPECIALIZATION, NenCategory.SPECIALIZATION),
                "Specialist na propria categoria.");
        assertEquals(a(0.0D),
                m.entre(NenCategory.ENHANCEMENT, NenCategory.SPECIALIZATION),
                "Outra categoria tentando Specialization: o canone diz que ela"
                        + " nao e normalmente aprendivel como secundaria.");
        assertEquals(a(0.6D),
                m.entre(NenCategory.SPECIALIZATION, NenCategory.EMISSION),
                "Specialist nas outras: o canone NAO fixa isto, e por isso o"
                        + " numero e um so, vindo do datapack.");
    }

    @Test
    @DisplayName("consultar UNDETERMINED devolve NENHUMA e nao estoura")
    void neutroNaoEstoura() {
        MatrizDeAfinidade m = matriz(comunsCompletas());

        assertSame(Afinidade.NENHUMA,
                m.entre(NenCategory.UNDETERMINED, NenCategory.EMISSION));
        assertSame(Afinidade.NENHUMA,
                m.entre(NenCategory.EMISSION, NenCategory.UNDETERMINED));
        assertSame(Afinidade.NENHUMA,
                m.entre(NenCategory.UNDETERMINED, NenCategory.UNDETERMINED));
        assertSame(Afinidade.NENHUMA, m.entre(null, null),
                "Todo jogador do servidor e UNDETERMINED ate despertar."
                        + " Perguntar por ele e legitimo e nao pode derrubar o tick.");
    }
}
