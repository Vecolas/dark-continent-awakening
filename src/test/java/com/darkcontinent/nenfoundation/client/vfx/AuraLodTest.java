package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O nivel de detalhe, que ate o AV3 eram DUAS tabelas no repositorio.
 *
 * <p>Este arquivo fixa a tabela que sobrou. Se alguem reintroduzir a outra, ou
 * acrescentar um nivel sem acrescentar o corte, aqui reprova.
 */
class AuraLodTest {

    @Test
    @DisplayName("os cortes sao 12 / 24 / 48 / 72, e a aura completa vale a briga de perto")
    void tabelaDeCortes() {
        assertSame(AuraRenderLod.FULL, AuraRenderLod.porDistancia(0.0D));
        assertSame(AuraRenderLod.FULL, AuraRenderLod.porDistancia(11.99D));
        assertSame(AuraRenderLod.NEAR, AuraRenderLod.porDistancia(12.0D));
        assertSame(AuraRenderLod.NEAR, AuraRenderLod.porDistancia(23.99D));
        assertSame(AuraRenderLod.MEDIUM, AuraRenderLod.porDistancia(24.0D));
        assertSame(AuraRenderLod.MEDIUM, AuraRenderLod.porDistancia(47.99D));
        assertSame(AuraRenderLod.FAR, AuraRenderLod.porDistancia(48.0D));
        assertSame(AuraRenderLod.FAR, AuraRenderLod.porDistancia(71.99D));
        assertSame(AuraRenderLod.HIDDEN, AuraRenderLod.porDistancia(72.0D));
        assertSame(AuraRenderLod.HIDDEN, AuraRenderLod.porDistancia(10_000.0D));

        // A tabela ANTIGA cortava a aura completa a OITO blocos -- distancia de
        // briga corpo a corpo, exatamente onde o estado de Nen precisa ser lido.
        assertSame(AuraRenderLod.FULL, AuraRenderLod.porDistancia(9.0D),
                "a oito blocos a aura tem de estar completa; era a divergencia do AV3");
    }

    @Test
    @DisplayName("cada nivel tem um corte: um nivel novo sem corte ficaria inalcancavel")
    void todoNivelEAlcancavel() {
        for (AuraRenderLod nivel : AuraRenderLod.values()) {
            boolean alcancado = false;
            for (double d = 0.0D; d <= 200.0D; d += 0.25D) {
                alcancado |= AuraRenderLod.porDistancia(d) == nivel;
            }
            assertTrue(alcancado, "nivel inalcancavel por distancia nenhuma: " + nivel);
        }
    }

    @Test
    @DisplayName("a borda e a ultima a sair -- e ela que carrega a leitura")
    void bordaSobreviveAteOFim() {
        assertTrue(AuraRenderLod.FAR.desenha(AuraRenderLod.Camada.BORDA),
                "a quarenta blocos ainda tem de dar para dizer que a pessoa esta em Ren");
        assertFalse(AuraRenderLod.FAR.desenha(AuraRenderLod.Camada.INTERNA));
        assertFalse(AuraRenderLod.FAR.desenha(AuraRenderLod.Camada.EXTERNA));

        for (AuraRenderLod.Camada camada : AuraRenderLod.Camada.values()) {
            assertFalse(AuraRenderLod.HIDDEN.desenha(camada), "HIDDEN nao desenha nada");
        }
    }

    @Test
    @DisplayName("a degradacao e monotona: nada volta a crescer com a distancia")
    void degradacaoMonotona() {
        AuraRenderLod[] niveis = AuraRenderLod.values();
        for (int i = 1; i < niveis.length; i++) {
            assertTrue(niveis[i].intensidade() < niveis[i - 1].intensidade(),
                    "intensidade subiu de " + niveis[i - 1] + " para " + niveis[i]);
            assertTrue(niveis[i].fracaoDeFilamentos() <= niveis[i - 1].fracaoDeFilamentos(),
                    "filamentos aumentaram de " + niveis[i - 1] + " para " + niveis[i]);
        }
    }

    @Test
    @DisplayName("distancia invalida reprova em vez de virar HIDDEN em silencio")
    void distanciaInvalida() {
        // Toda comparacao com NaN e falsa: sem a guarda, NaN atravessaria o
        // laco e cairia em HIDDEN. A aura sumiria sem motivo visivel.
        assertThrows(IllegalArgumentException.class,
                () -> AuraRenderLod.porDistancia(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> AuraRenderLod.porDistancia(-1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> AuraRenderLod.porDistancia(Double.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("a qualidade impoe TETO, e nunca melhora o que a distancia piorou")
    void qualidadeSoPiora() {
        for (AuraVisualQuality qualidade : AuraVisualQuality.values()) {
            for (AuraRenderLod porDistancia : AuraRenderLod.values()) {
                AuraRenderLod efetivo = qualidade.limitar(porDistancia);
                assertTrue(efetivo.ordinal() >= porDistancia.ordinal(),
                        "qualidade melhorou o nivel: " + qualidade + " + " + porDistancia);
                assertTrue(efetivo.intensidade() <= porDistancia.intensidade());
            }
        }
    }

    @Test
    @DisplayName("OFF desliga tudo, e ULTRA nunca atrapalha")
    void extremosDaQualidade() {
        for (AuraRenderLod porDistancia : AuraRenderLod.values()) {
            assertSame(AuraRenderLod.HIDDEN, AuraVisualQuality.OFF.limitar(porDistancia));
            assertSame(porDistancia, AuraVisualQuality.ULTRA.limitar(porDistancia));
        }
        assertFalse(AuraVisualQuality.OFF.teto().visivel());
    }

    @Test
    @DisplayName("LOW mantem a borda de perto: menos custo, mesma leitura")
    void lowPreservaALeitura() {
        AuraRenderLod perto = AuraVisualQuality.LOW.limitar(AuraRenderLod.FULL);
        assertSame(AuraRenderLod.FAR, perto);
        assertTrue(perto.desenha(AuraRenderLod.Camada.BORDA),
                "quem escolheu LOW quer menos trabalho, e nao perder a informacao");
    }

    @Test
    @DisplayName("a terceira pessoa le a intensidade do proprio nivel, sem segunda tabela")
    void intensidadeVemDoNivel() {
        assertEquals(AuraRenderLod.FULL.intensidade(), 1.0F);
        assertEquals(AuraRenderLod.HIDDEN.intensidade(), 0.0F);
    }
}
