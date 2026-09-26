package com.darkcontinent.nenfoundation.enemy.greedisland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.greedisland.ecology.HabitatDaIlha;
import com.darkcontinent.nenfoundation.enemy.greedisland.landmark.Landmark;
import com.darkcontinent.nenfoundation.enemy.greedisland.landmark.RegistroDeLandmarks;
import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandMask;
import com.darkcontinent.nenfoundation.enemy.greedisland.navigation.ServicoDeNavegacao;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Os portoes das fases G9 (landmarks), G10 (habitat) e G11 (navegacao). */
class LandmarksHabitatENavegacaoTest {

    // ------------------------------------------------------------------
    // G9 — LANDMARKS
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ha landmarks grandes em numero util, e nenhum id repete")
    void oElencoDeReferenciasExiste() {
        var todos = RegistroDeLandmarks.todos();
        assertTrue(todos.size() >= 30,
                "so " + todos.size() + " landmarks grandes: o documento pede 35-60, e"
                        + " numa ilha de 80.000 blocos poucos pontos de referencia"
                        + " transformam wilderness em vazio");
        Set<String> ids = new HashSet<>();
        for (Landmark l : todos) {
            assertTrue(ids.add(l.id()), "landmark repetido: " + l.id());
        }
    }

    @Test
    @DisplayName("TODO landmark fica em terra")
    void nenhumNoMar() {
        for (Landmark l : RegistroDeLandmarks.todos()) {
            assertTrue(GreedIslandMask.terra(l.ancora().x(), l.ancora().z()),
                    l.id() + " caiu no oceano");
        }
    }

    @Test
    @DisplayName("o espacamento NAO e grade -- ele varia")
    void naoHaGradeUniforme() {
        // O documento proibe grade com todas as letras. Grade regular se
        // reconhece em cinco minutos, e dali cada landmark deixa de ser
        // descoberta e vira item de lista.
        var todos = RegistroDeLandmarks.todos();
        double soma = 0.0D;
        double somaQuadrados = 0.0D;
        int n = 0;
        for (Landmark a : todos) {
            double maisPerto = Double.MAX_VALUE;
            for (Landmark b : todos) {
                if (a == b) {
                    continue;
                }
                maisPerto = Math.min(maisPerto, Math.hypot(
                        a.ancora().x() - b.ancora().x(), a.ancora().z() - b.ancora().z()));
            }
            soma += maisPerto;
            somaQuadrados += maisPerto * maisPerto;
            n++;
        }
        double media = soma / n;
        double desvio = Math.sqrt(somaQuadrados / n - media * media) / media;
        assertTrue(desvio > 0.25D,
                "a distancia ao vizinho mais proximo varia so "
                        + String.format("%.0f%%", desvio * 100) + ": isso e uma grade");
    }

    @Test
    @DisplayName("os dois landmarks DA OBRA estao marcados como tal")
    void oCanonEstaRotulado() {
        long daObra = RegistroDeLandmarks.todos().stream()
                .filter(l -> l.canon() == Landmark.Canon.DA_OBRA).count();
        assertTrue(daObra >= 2,
                "nenhum landmark rotulado como vindo da obra: sem o rotulo, alguem"
                        + " defende como cânone, numa discussao futura, algo que este"
                        + " projeto inventou");
        assertTrue(RegistroDeLandmarks.porId("arvore_shiso").isPresent());
        assertTrue(RegistroDeLandmarks.porId("farol_de_soufrabi").isPresent());
    }

    @Test
    @DisplayName("as cachoeiras sao DERIVADAS -- elas nascem onde rio cruza serra")
    void asCachoeirasAcompanhamAGeografia() {
        long cachoeiras = RegistroDeLandmarks.todos().stream()
                .filter(l -> l.tipo() == Landmark.Tipo.CACHOEIRA).count();
        assertTrue(cachoeiras >= 2,
                "nenhuma cachoeira derivada: ou nenhum rio cruza cordilheira -- e ai a"
                        + " hidrografia nao desce das montanhas --, ou a derivacao"
                        + " parou de funcionar");
    }

    @Test
    void landmarkSemRaioReprova() {
        assertThrows(IllegalArgumentException.class,
                () -> new Landmark("x", Landmark.Tipo.ROCHEDO,
                        new GreedIslandConstants.Ponto(0, 0), "r",
                        Landmark.Canon.ORIGINAL_COMPATIVEL, 0));
    }

    // ------------------------------------------------------------------
    // G10 — HABITAT
    // ------------------------------------------------------------------

    @Test
    @DisplayName("as sete criaturas tem habitat, e nenhuma divide a MESMA geografia")
    void cadaBichoNoSeuLugar() {
        assertEquals(7, HabitatDaIlha.HABITATS.size(),
                "Greed Island tem sete criaturas; cada uma precisa de habitat proprio");

        var assinaturas = new TreeSet<String>();
        for (var h : HabitatDaIlha.HABITATS) {
            String assinatura = new TreeSet<>(h.regioesPreferidas()) + "|"
                    + new TreeSet<>(h.biomas().stream().map(Enum::name).toList());
            assertTrue(assinaturas.add(assinatura),
                    h.criatura() + " divide regiao E bioma com outra especie: saber onde"
                            + " uma vive nao ensina nada sobre a outra, e o conhecimento"
                            + " geografico da secao 69 deixa de se construir");
        }
    }

    @Test
    @DisplayName("TODO habitat tem chao de verdade na ilha")
    void nenhumHabitatEVazio() {
        for (var h : HabitatDaIlha.HABITATS) {
            boolean achou = false;
            for (int x = -40_000; x <= 40_000 && !achou; x += 900) {
                for (int z = -36_000; z <= 36_000 && !achou; z += 900) {
                    if (GreedIslandMask.terra(x, z) && h.cabeEm(x, z)) {
                        achou = true;
                    }
                }
            }
            assertTrue(achou, h.criatura() + " nao cabe em NENHUM ponto da ilha: a"
                    + " especie existe no registro e nao tem onde nascer, e nada"
                    + " acusa isso -- e o mesmo silencio da config orfa");
        }
    }

    @Test
    @DisplayName("a estrada e a cidade espantam a fauna")
    void aRotaSeguraEMaisSegura() {
        // Sem isto, viajar pela estrada e tao perigoso quanto cortar pela mata,
        // e a escolha da secao 71 -- rota longa e segura contra curta e
        // perigosa -- deixa de existir.
        var antokiba = GreedIslandConstants.cidade("antokiba").orElseThrow();
        assertTrue(HabitatDaIlha.perturbadoPorGente(antokiba.x(), antokiba.z()),
                "o centro de Antokiba nao espanta fauna: os bichos nasceriam na praca");
        assertTrue(HabitatDaIlha.em(antokiba.x(), antokiba.z()).isEmpty(),
                "ha criatura selvagem prevista dentro da cidade");
    }

    @Test
    @DisplayName("a maior parte da ilha NAO tem bicho previsto")
    void aNaturezaEAbertaNaMaiorParte() {
        int comBicho = 0;
        int total = 0;
        for (int x = -38_000; x <= 38_000; x += 1_700) {
            for (int z = -34_000; z <= 34_000; z += 1_700) {
                if (!GreedIslandMask.terra(x, z)) {
                    continue;
                }
                total++;
                if (!HabitatDaIlha.em(x, z).isEmpty()) {
                    comBicho++;
                }
            }
        }
        double fatia = comBicho / (double) total;
        assertTrue(fatia < 0.45D,
                String.format("%.0f%%", fatia * 100) + " da ilha tem criatura prevista."
                        + " A secao 111 pede 60-70% de natureza relativamente aberta:"
                        + " uma ilha com bicho em todo lugar e um corredor de combate,"
                        + " e nao um lugar.");
    }

    // ------------------------------------------------------------------
    // G11 — NAVEGACAO
    // ------------------------------------------------------------------

    @Test
    @DisplayName("nenhum id de destino se repete entre cidade e landmark")
    void osIdsDeDestinoSaoUnicos() {
        // O PORTAO ACHOU ESTE: a arvore Shiso e a cidade Shiso Tree usavam o
        // mesmo id, e `destino(id)` devolvia um dos dois por ordem de lista.
        // Uma carta levaria o jogador ora a arvore, ora a cidade, sem erro.
        Set<String> vistos = new HashSet<>();
        for (var d : ServicoDeNavegacao.todos()) {
            assertTrue(vistos.add(d.id()),
                    "id de destino repetido: " + d.id() + " -- a carta levaria a um dos"
                            + " dois por ordem de lista, e nada acusaria");
        }
    }

    @Test
    @DisplayName("os destinos sao cidades e landmarks -- e nao coordenada qualquer")
    void oCatalogoDeDestinos() {
        var todos = ServicoDeNavegacao.todos();
        assertTrue(todos.size() >= 38, "poucos destinos: " + todos.size());
        assertTrue(todos.stream().anyMatch(d -> d.id().equals("masadora")));
        assertTrue(todos.stream().anyMatch(d -> d.id().equals("shiso_tree")));
    }

    @Test
    @DisplayName("a carta so leva AONDE O JOGADOR JA ESTEVE")
    void oConhecimentoEAMoeda() {
        // Sem esta regra, a primeira carta de transporte torna a ilha inteira
        // acessivel, e explorar vira opcional no primeiro dia.
        var semNada = ServicoDeNavegacao.disponiveisPara(Set.of());
        assertEquals(1, semNada.size(),
                "um jogador que nao descobriu nada tem " + semNada.size() + " destinos");
        assertEquals(GreedIslandConstants.CIDADE_INICIAL, semNada.get(0).id(),
                "o unico destino de quem nao conhece nada tem de ser o hub inicial --"
                        + " senao um jogador sem mapa fica preso, e ficar preso nao e"
                        + " dificuldade");

        var comDuas = ServicoDeNavegacao.disponiveisPara(Set.of("masadora", "dorias"));
        assertEquals(3, comDuas.size(), "descobertos + o hub inicial");
    }

    @Test
    @DisplayName("chegar num lugar o DESCOBRE")
    void aPresencaVale() {
        var masadora = GreedIslandConstants.cidade("masadora").orElseThrow();
        assertEquals("masadora",
                ServicoDeNavegacao.descobertaEm(masadora.x(), masadora.z()).orElseThrow());

        // No meio do nada, nada e descoberto -- e a maior parte da ilha e isso.
        assertTrue(ServicoDeNavegacao.descobertaEm(-30_000, -30_000).isEmpty()
                        || ServicoDeNavegacao.descobertaEm(-30_000, -30_000).isPresent(),
                "consulta no vazio nao pode lancar");
    }

    @Test
    @DisplayName("a carta de retorno leva ao conhecido MAIS PROXIMO")
    void oRetornoEscolheOPertoEConhecido() {
        var dorias = GreedIslandConstants.cidade("dorias").orElseThrow();
        var destino = ServicoDeNavegacao.retornoPara(dorias.x(), dorias.z(),
                Set.of("masadora", "dorias", "limeiro")).orElseThrow();
        assertEquals("dorias", destino.id(),
                "estando em Dorias e conhecendo Dorias, o retorno escolheu " + destino.id());

        // Longe de tudo, com so Masadora conhecida, ele volta para Masadora.
        var so = ServicoDeNavegacao.retornoPara(30_000, -25_000, Set.of("masadora"))
                .orElseThrow();
        assertTrue(so.id().equals("masadora") || so.id().equals("shiso_tree"),
                "o retorno escolheu " + so.id() + ", que nao esta entre os conhecidos");
    }

    @Test
    @DisplayName("as distancias entre destinos justificam a carta")
    void aEscalaDaCarta() {
        var masadora = ServicoDeNavegacao.destino("masadora").orElseThrow();
        var soufrabi = ServicoDeNavegacao.destino("soufrabi").orElseThrow();
        double d = ServicoDeNavegacao.distanciaAte(masadora.x(), masadora.z(), soufrabi);
        assertTrue(d > 20_000,
                "Masadora a Soufrabi sao " + (int) d + " blocos. Se fosse pouco, ninguem"
                        + " gastaria carta -- e a secao 72 poe a escala a servico delas.");
    }
}
