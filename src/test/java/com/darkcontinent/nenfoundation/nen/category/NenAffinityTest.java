package com.darkcontinent.nenfoundation.nen.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do servico de afinidade.
 *
 * <p>O servico guarda estado global (a matriz carregada), entao cada teste o
 * deixa limpo ao sair. Sem isso, a ordem dos testes mudaria o resultado -- e o
 * sintoma seria um teste que passa sozinho e falha na suite.
 */
class NenAffinityTest {

    private static Afinidade a(double valor) {
        return new Afinidade(valor, valor, valor);
    }

    /** Matriz de teste com valores DISTINTOS por eixo, de proposito. */
    private static MatrizDeAfinidade matrizDeTeste() {
        Map<NenCategory, Map<NenCategory, Afinidade>> comuns = new LinkedHashMap<>();
        for (NenCategory origem : MatrizDeAfinidade.COMUNS) {
            Map<NenCategory, Afinidade> linha = new LinkedHashMap<>();
            for (NenCategory alvo : MatrizDeAfinidade.COMUNS) {
                // Tres numeros diferentes em cada celula: assim um metodo que
                // devolve o campo ERRADO nao passa por coincidencia.
                linha.put(alvo, origem == alvo
                        ? new Afinidade(1.0D, 1.1D, 1.2D)
                        : new Afinidade(0.5D, 0.6D, 0.7D));
            }
            comuns.put(origem, linha);
        }
        return new MatrizDeAfinidade(comuns,
                new MatrizDeAfinidade.RegraDeEspecializacao(
                        new Afinidade(1.0D, 1.1D, 1.2D), a(0.0D), a(0.3D)),
                Optional.empty());
    }

    private static PersistentNenData perfilCom(NenCategory categoria, boolean revelada) {
        return new PersistentNenData(
                PersistentNenData.SCHEMA_ATUAL, true, categoria, revelada,
                0.0D, 0.0D, 0.0D, Map.of(), Set.of(), Set.of(), Set.of());
    }

    @BeforeEach
    void limparAntes() {
        NenAffinity.limpar();
    }

    @AfterEach
    void limparDepois() {
        NenAffinity.limpar();
    }

    // ---------------------------------------------------- sem matriz

    @Test
    @DisplayName("sem matriz carregada, toda consulta devolve NENHUMA e nao estoura")
    void semMatrizRespondeZero() {
        assertFalse(NenAffinity.carregada());
        assertTrue(NenAffinity.matriz().isEmpty());

        assertSame(Afinidade.NENHUMA,
                NenAffinity.entre(NenCategory.EMISSION, NenCategory.EMISSION),
                "Sem matriz a resposta tem de ser DEFINIDA. null ou excecao aqui"
                        + " viraria NPE ou tick derrubado longe da causa.");
        assertEquals(0.0D,
                NenAffinity.effectiveness(perfilCom(NenCategory.EMISSION, true),
                        NenCategory.EMISSION));
    }

    // ---------------------------------------------------- as tres perguntas

    @Test
    @DisplayName("cada pergunta devolve o SEU campo, e nao o do vizinho")
    void cadaPerguntaDevolveOSeuCampo() {
        NenAffinity.instalar(matrizDeTeste());
        PersistentNenData perfil = perfilCom(NenCategory.EMISSION, true);

        assertEquals(1.0D, NenAffinity.learningRate(perfil, NenCategory.EMISSION));
        assertEquals(1.1D, NenAffinity.maxProficiency(perfil, NenCategory.EMISSION));
        assertEquals(1.2D, NenAffinity.effectiveness(perfil, NenCategory.EMISSION));
    }

    @Test
    @DisplayName("as tres sao eixos separados, e podem divergir")
    void tresEixosPodemDivergir() {
        // A matriz que o mod distribui nasce com os tres iguais, porque o
        // canone da um numero so. A ESTRUTURA precisa permitir que divirjam --
        // sem isso, "aprende devagar mas chega longe" nao e representavel, e
        // ninguem descobre ate tentar desenhar a habilidade que precisa disso.
        Map<NenCategory, Map<NenCategory, Afinidade>> comuns = new LinkedHashMap<>();
        for (NenCategory origem : MatrizDeAfinidade.COMUNS) {
            Map<NenCategory, Afinidade> linha = new LinkedHashMap<>();
            for (NenCategory alvo : MatrizDeAfinidade.COMUNS) {
                linha.put(alvo, new Afinidade(0.2D, 0.9D, 0.5D));
            }
            comuns.put(origem, linha);
        }
        NenAffinity.instalar(new MatrizDeAfinidade(comuns,
                new MatrizDeAfinidade.RegraDeEspecializacao(a(1.0D), a(0.0D), a(0.5D)),
                Optional.empty()));

        PersistentNenData perfil = perfilCom(NenCategory.CONJURATION, true);
        assertEquals(0.2D, NenAffinity.learningRate(perfil, NenCategory.EMISSION),
                "aprende devagar");
        assertEquals(0.9D, NenAffinity.maxProficiency(perfil, NenCategory.EMISSION),
                "e mesmo assim chega longe");
    }

    // ---------------------------------------------------- determinismo

    @Test
    @DisplayName("a consulta e deterministica e cobre os 36 pares das seis")
    void deterministicaECompleta() {
        NenAffinity.instalar(matrizDeTeste());

        for (NenCategory origem : NenCategory.REAIS) {
            for (NenCategory alvo : NenCategory.REAIS) {
                Afinidade primeira = NenAffinity.entre(origem, alvo);
                Afinidade segunda = NenAffinity.entre(origem, alvo);
                assertNotNull(primeira,
                        origem + " -> " + alvo + " devolveu null.");
                assertEquals(primeira, segunda,
                        "Duas consultas iguais deram respostas diferentes: "
                                + origem + " -> " + alvo);
            }
        }
    }

    // ------------------------------------------- o que a afinidade NAO ve

    @Test
    @DisplayName("a afinidade sai da categoria ATRIBUIDA, e nao da revelada")
    void revelacaoNaoMudaPoder() {
        NenAffinity.instalar(matrizDeTeste());

        PersistentNenData escondida = perfilCom(NenCategory.EMISSION, false);
        PersistentNenData revelada = perfilCom(NenCategory.EMISSION, true);

        assertEquals(NenAffinity.consultar(revelada, NenCategory.EMISSION),
                NenAffinity.consultar(escondida, NenCategory.EMISSION),
                "A revelacao mudou a afinidade. Um jogador que ainda nao fez a"
                        + " Water Divination JA TEM a afinidade dele -- ele so nao"
                        + " sabe qual e. Ler categoriaVisivel() aqui transformaria"
                        + " um evento de narrativa num buff.");
    }

    @Test
    @DisplayName("jogador sem categoria nao tem afinidade com nada")
    void semCategoriaNaoTemAfinidade() {
        NenAffinity.instalar(matrizDeTeste());
        PersistentNenData naoDesperto = PersistentNenData.NAO_DESPERTADO;

        for (NenCategory alvo : NenCategory.REAIS) {
            assertSame(Afinidade.NENHUMA, NenAffinity.consultar(naoDesperto, alvo),
                    "UNDETERMINED e a ausencia de categoria; ele nao pode ter"
                            + " afinidade com " + alvo);
        }
    }

    // ---------------------------------------------------- troca de matriz

    @Test
    @DisplayName("instalar troca a matriz inteira, sem estado intermediario")
    void instalarTrocaTudo() {
        NenAffinity.instalar(matrizDeTeste());
        assertEquals(1.2D, NenAffinity.entre(NenCategory.EMISSION, NenCategory.EMISSION)
                .effectiveness());

        Map<NenCategory, Map<NenCategory, Afinidade>> outras = new LinkedHashMap<>();
        for (NenCategory origem : MatrizDeAfinidade.COMUNS) {
            Map<NenCategory, Afinidade> linha = new LinkedHashMap<>();
            for (NenCategory alvo : MatrizDeAfinidade.COMUNS) {
                linha.put(alvo, a(0.25D));
            }
            outras.put(origem, linha);
        }
        NenAffinity.instalar(new MatrizDeAfinidade(outras,
                new MatrizDeAfinidade.RegraDeEspecializacao(a(1.0D), a(0.0D), a(0.5D)),
                Optional.empty()));

        // Mudar o dado muda a resposta, sem recompilar nada. E o criterio de
        // aceite inteiro da issue, num assert.
        assertEquals(0.25D, NenAffinity.entre(NenCategory.EMISSION, NenCategory.EMISSION)
                .effectiveness(),
                "A matriz nova nao entrou. Se o valor antigo sobrevive a troca,"
                        + " editar o datapack nao muda nada e a sessao de"
                        + " balanceamento gira um botao morto.");
    }
}
