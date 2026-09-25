package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NenHudVisibilityTest {

    @Test
    void modosJogaveisRenderizamMesmoComValoresZero() {
        assertTrue(NenHudVisibility.deveRenderizar(false, true, false));
    }

    @Test
    void f1EspectadorEAusenciaDeJogadorOcultam() {
        assertFalse(NenHudVisibility.deveRenderizar(true, true, false));
        assertFalse(NenHudVisibility.deveRenderizar(false, true, true));
        assertFalse(NenHudVisibility.deveRenderizar(false, false, false));
    }

    @Test
    @DisplayName("o fluxo so aparece com tecnica ligada")
    void oFluxoEContextual() {
        assertFalse(NenHudVisibility.deveMostrarFluxo(0),
                "sem tecnica o output esta em repouso, e a barra so repetiria a "
                        + "ausencia do chip");
        assertTrue(NenHudVisibility.deveMostrarFluxo(1));
        assertTrue(NenHudVisibility.deveMostrarFluxo(4));
    }

    @Test
    @DisplayName("a regra do fluxo e por PRESENCA, e nao por magnitude")
    void oFluxoNaoSomeComZetsu() {
        // Zetsu zera o output. Uma regra por magnitude esconderia a barra
        // exatamente quando ela explica o zero -- o caso que motivou a escolha.
        assertTrue(NenHudVisibility.deveMostrarFluxo(1),
                "uma tecnica que zera o output continua sendo uma tecnica ligada");
    }

    @Test
    @DisplayName("a fila de indicadores so entra a partir da SEGUNDA tecnica")
    void aFilaNaoRepeteOChip() {
        assertFalse(NenHudVisibility.deveMostrarFilaDeTecnicas(0));
        assertFalse(NenHudVisibility.deveMostrarFilaDeTecnicas(1),
                "com uma so, o chip ja disse -- desenhar a fila seria a mesma "
                        + "informacao duas vezes a dois centimetros de distancia");
        assertTrue(NenHudVisibility.deveMostrarFilaDeTecnicas(2));
    }

    @Test
    @DisplayName("contagem negativa nao liga nada")
    void contagemImpossivelNaoAcende() {
        assertFalse(NenHudVisibility.deveMostrarFluxo(-1));
        assertFalse(NenHudVisibility.deveMostrarFilaDeTecnicas(-1));
    }
}
