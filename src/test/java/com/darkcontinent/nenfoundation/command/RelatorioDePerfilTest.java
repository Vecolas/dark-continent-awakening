package com.darkcontinent.nenfoundation.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Portao do relatorio de perfil. */
class RelatorioDePerfilTest {

    private static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath("nenfoundation", caminho);
    }

    private static PersistentNenData perfil() {
        return new PersistentNenData(
                1, true, NenCategory.ENHANCEMENT, true,
                12.5D, 0.25D, 0.75D,
                Map.of(id("ten"), 0.4D),
                Set.of(id("ren"), id("ten")),
                Set.of(id("punho_reforcado")),
                Set.of(id("despertou")));
    }

    @Test
    @DisplayName("o resumo carrega todo o estado persistente, numa linha")
    void resumoCompleto() {
        String texto = RelatorioDePerfil.resumo(perfil());

        assertTrue(texto.contains("schema=1"));
        assertTrue(texto.contains("awakened=true"));
        assertTrue(texto.contains("category=enhancement"));
        assertTrue(texto.contains("revealed=true"));
        assertTrue(texto.contains("auraPotential=12.500"));
        assertTrue(texto.contains("proficiencies={nenfoundation:ten=0.400}"));
        assertTrue(texto.contains("techniques=[nenfoundation:ren, nenfoundation:ten]"),
                "Ids saem ordenados para o relato ser comparavel entre execucoes.");
        assertTrue(texto.contains("abilities=[nenfoundation:punho_reforcado]"));
        assertTrue(texto.contains("flags=[nenfoundation:despertou]"));
    }

    @Test
    @DisplayName("o resumo e identico entre execucoes")
    void resumoEDeterministico() {
        assertEquals(RelatorioDePerfil.resumo(perfil()), RelatorioDePerfil.resumo(perfil()),
                "Conjunto e mapa nao tem ordem garantida. Um relato que muda de"
                        + " ordem nao pode ser comparado com outro -- e comparar dois"
                        + " relatos e a coisa mais util que se faz com eles.");
    }

    @Test
    @DisplayName("o dump traz as versoes, sem as quais o relato de bug fica pela metade")
    void dumpTrazVersoes() {
        List<String> linhas = RelatorioDePerfil.dump("Alguem", perfil());
        String tudo = String.join("\n", linhas);

        assertTrue(tudo.contains("jogador: Alguem"));
        assertTrue(tudo.contains("schema: 1"),
                "Sem a versao de schema, ninguem sabe contra qual formato o bug"
                        + " aconteceu.");
        assertTrue(tudo.contains("protocolo: v"),
                "Sem a versao de protocolo, divergencia cliente/servidor nao e"
                        + " diagnosticavel a partir do relato.");
    }

    @Test
    @DisplayName("o dump mostra categoria real E visivel, que sao coisas diferentes")
    void dumpDistingueCategoriaRealDaVisivel() {
        PersistentNenData escondida = new PersistentNenData(
                1, true, NenCategory.SPECIALIZATION, false,
                1.0D, 1.0D, 1.0D, Map.of(), Set.of(), Set.of(), Set.of());

        String tudo = String.join("\n", RelatorioDePerfil.dump("Alguem", escondida));

        assertTrue(tudo.contains("category: specialization"),
                "O operador precisa ver a categoria REAL: e disso que ele precisa"
                        + " para diagnosticar.");
        assertTrue(tudo.contains("visivel: undetermined"),
                "E precisa ver o que o JOGADOR ve, que e outra coisa. Sem as duas"
                        + " lado a lado, um bug de revelacao e invisivel no relato.");
    }

    @Test
    @DisplayName("o dump nao vaza UUID")
    void dumpNaoVazaUuid() {
        String tudo = String.join("\n", RelatorioDePerfil.dump("Alguem", perfil()));
        assertFalse(tudo.toLowerCase(java.util.Locale.ROOT).contains("uuid"),
                "Um dump colado numa issue publica nao precisa levar identificador"
                        + " de conta junto. O nome basta para correlacionar dentro"
                        + " de um servidor.");
    }

    @Test
    @DisplayName("o dump de um perfil neutro nao mente dizendo que ha algo")
    void dumpDoNeutro() {
        String tudo = String.join("\n",
                RelatorioDePerfil.dump("Novato", PersistentNenData.NAO_DESPERTADO));

        assertTrue(tudo.contains("awakened: false"));
        assertTrue(tudo.contains("category: undetermined"));
        assertTrue(tudo.contains("techniques: []"));
        assertTrue(tudo.contains("flags: []"));
    }
}
