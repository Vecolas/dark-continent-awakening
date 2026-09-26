package com.darkcontinent.nenfoundation.enemy.greedisland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.greedisland.layout.GreedIslandMask;
import com.darkcontinent.nenfoundation.enemy.greedisland.region.BiomaDaIlha;
import com.darkcontinent.nenfoundation.enemy.greedisland.region.RegiaoMacro;
import com.darkcontinent.nenfoundation.enemy.greedisland.region.RegistroDeRegioes;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O portao da fase G4: as 24 macro-regioes e os nove biomas. */
class RegioesEBiomasTest {

    @Test
    @DisplayName("sao 24 regioes, e nenhuma repete id")
    void aContagemBateComASecao60() {
        assertEquals(24, RegistroDeRegioes.todas().size(),
                "a secao 60 lista 24 macro-regioes");
        Set<String> ids = new HashSet<>();
        for (RegiaoMacro r : RegistroDeRegioes.todas()) {
            assertTrue(ids.add(r.id()), "id repetido: " + r.id());
        }
    }

    @Test
    @DisplayName("TODA regiao tem territorio de verdade na ilha")
    void nenhumaRegiaoEFantasma() {
        // Uma regiao que nunca vence a disputa do mais-proximo existe so na
        // lista: ela nao tem chao, nao tem criatura e nao tem quest. E isso nao
        // da erro nenhum -- so um nome que ninguem encontra.
        Map<String, Integer> contagem = new java.util.HashMap<>();
        for (int x = -42_000; x <= 42_000; x += 700) {
            for (int z = -38_000; z <= 38_000; z += 700) {
                if (GreedIslandMask.terra(x, z)) {
                    contagem.merge(RegistroDeRegioes.em(x, z).id(), 1, Integer::sum);
                }
            }
        }
        Set<String> semChao = new TreeSet<>();
        for (RegiaoMacro r : RegistroDeRegioes.todas()) {
            if (contagem.getOrDefault(r.id(), 0) < 20) {
                semChao.add(r.id() + "(" + contagem.getOrDefault(r.id(), 0) + ")");
            }
        }
        assertTrue(semChao.isEmpty(),
                "regioes sem territorio util: " + semChao);
    }

    @Test
    @DisplayName("nenhuma regiao engole a ilha")
    void aDistribuicaoNaoEDominada() {
        Map<String, Integer> contagem = new java.util.HashMap<>();
        int total = 0;
        for (int x = -42_000; x <= 42_000; x += 700) {
            for (int z = -38_000; z <= 38_000; z += 700) {
                if (GreedIslandMask.terra(x, z)) {
                    total++;
                    contagem.merge(RegistroDeRegioes.em(x, z).id(), 1, Integer::sum);
                }
            }
        }
        for (var e : contagem.entrySet()) {
            double fatia = e.getValue() / (double) total;
            assertTrue(fatia < 0.18D, e.getKey() + " ocupa "
                    + String.format("%.0f%%", fatia * 100) + " da ilha: com 24 regioes,"
                    + " a media e 4%, e uma que passa de 18% apagou as vizinhas");
        }
    }

    @Test
    @DisplayName("a consulta de regiao NUNCA devolve vazio -- nao ha buraco na ilha")
    void todoPontoTemRegiao() {
        for (int x = -40_000; x <= 40_000; x += 3_300) {
            for (int z = -36_000; z <= 36_000; z += 3_300) {
                assertTrue(RegistroDeRegioes.em(x, z) != null,
                        "ponto sem regiao em (" + x + "," + z + ")");
            }
        }
    }

    @Test
    @DisplayName("a dificuldade cobre a escada do documento, do inicio ao endgame")
    void haProgressaoEspacial() {
        Set<Integer> faixas = new TreeSet<>();
        RegistroDeRegioes.todas().forEach(r -> faixas.add(r.dificuldade()));

        assertTrue(faixas.contains(1), "nenhuma regiao de faixa 1: nao ha onde comecar");
        assertTrue(faixas.stream().anyMatch(f -> f >= 7),
                "a faixa mais alta e " + faixas.stream().max(Integer::compare).orElseThrow()
                        + ": nao ha endgame geografico");
        assertTrue(faixas.size() >= 6,
                "so " + faixas.size() + " faixas distintas: a progressao fica em degraus");
    }

    @Test
    @DisplayName("a regiao do Shiso e a MAIS FACIL, e a do inicio")
    void oComecoEProtegido() {
        var shiso = GreedIslandConstants.cidade("shiso_tree").orElseThrow();
        RegiaoMacro r = RegistroDeRegioes.em(shiso.x(), shiso.z());
        assertEquals(1, r.dificuldade(),
                "o Shiso Tree nasceu na regiao " + r.id() + ", de faixa " + r.dificuldade()
                        + ": a primeira hora do jogador seria numa area perigosa");
    }

    @Test
    @DisplayName("a dificuldade NAO e radial -- ha area dura perto e facil longe")
    void aProgressaoTemExcecoes() {
        // O documento e explicito: "o jogador pode entrar numa area
        // perigosissima cedo se quiser. E Hunter x Hunter." Uma dificuldade
        // estritamente radial seria um anel, e anel nao surpreende ninguem.
        var shiso = GreedIslandConstants.cidade("shiso_tree").orElseThrow();
        boolean duraPerto = false;
        boolean facilLonge = false;
        for (RegiaoMacro r : RegistroDeRegioes.todas()) {
            double d = Math.hypot(r.centro().x() - shiso.x(), r.centro().z() - shiso.z());
            if (d < 22_000 && r.dificuldade() >= 5) {
                duraPerto = true;
            }
            if (d > 30_000 && r.dificuldade() <= 4) {
                facilLonge = true;
            }
        }
        assertTrue(duraPerto, "nenhuma regiao dificil perto do inicio: a dificuldade virou"
                + " um anel radial");
        assertTrue(facilLonge, "nenhuma regiao facil longe: idem");
    }

    // ------------------------------------------------------------------
    // BIOMAS
    // ------------------------------------------------------------------

    @Test
    @DisplayName("os NOVE biomas aparecem, e nenhum domina")
    void aEcologiaEVariada() {
        Map<BiomaDaIlha, Integer> contagem = new EnumMap<>(BiomaDaIlha.class);
        int total = 0;
        for (int x = -42_000; x <= 42_000; x += 600) {
            for (int z = -38_000; z <= 38_000; z += 600) {
                if (!GreedIslandMask.terra(x, z)) {
                    continue;
                }
                total++;
                contagem.merge(BiomaDaIlha.em(x, z), 1, Integer::sum);
            }
        }
        Set<BiomaDaIlha> ausentes = new HashSet<>(java.util.List.of(BiomaDaIlha.values()));
        ausentes.removeAll(contagem.keySet());
        assertTrue(ausentes.isEmpty(),
                "bioma declarado e nunca usado: " + ausentes + ". Bioma sem chao e a"
                        + " mesma familia da config orfa -- ele existe, ninguem ve, e"
                        + " a proxima pessoa passa uma tarde ajustando o que nao aparece.");

        for (var e : contagem.entrySet()) {
            double fatia = e.getValue() / (double) total;
            assertTrue(fatia < 0.55D, e.getKey() + " cobre "
                    + String.format("%.0f%%", fatia * 100) + " da ilha: os outros oito"
                    + " viram detalhe, e a ilha le como um bioma so");
        }
    }

    @Test
    @DisplayName("o rio manda no bioma, inclusive dentro da montanha")
    void aAguaVenceOReleva() {
        var rio = GreedIslandConstants.RIOS.get(0);
        var ponto = rio.curso().get(1);
        assertEquals(BiomaDaIlha.GI_RIVERLAND, BiomaDaIlha.em(ponto.x(), ponto.z()),
                "um ponto DENTRO do rio virou bioma de terra: o rio que corta a serra"
                        + " sairia com agua correndo dentro de bioma de montanha");
    }

    @Test
    @DisplayName("a beira do mar e costa, em qualquer regiao")
    void aPraiaEPraia() {
        // Anda para o leste ate achar a costa e confere o bioma logo antes.
        for (int x = 20_000; x < 46_000; x += 200) {
            if (!GreedIslandMask.terra(x + 400, 0) && GreedIslandMask.terra(x, 0)) {
                assertEquals(BiomaDaIlha.GI_COAST, BiomaDaIlha.em(x, 0),
                        "a ultima terra antes do mar em x=" + x + " nao e costa");
                return;
            }
        }
        org.junit.jupiter.api.Assertions.fail("nao achei a costa varrendo para o leste");
    }
}
