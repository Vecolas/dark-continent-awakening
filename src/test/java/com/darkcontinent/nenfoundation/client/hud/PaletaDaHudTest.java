package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao das operacoes de cor.
 *
 * <p>Aritmetica de canal e o lugar classico de o defeito nao dar erro: um
 * estouro de 255 vira preto por wrap-around, e um alpha comido por engano
 * transforma a HUD inteira em opaca sem ninguem saber por que.
 */
class PaletaDaHudTest {

    @Test
    @DisplayName("clarear e escurecer nunca estouram o canal")
    void nenhumCanalDaAVolta() {
        for (float f = 0.0F; f <= 1.0F; f += 0.1F) {
            for (int cor : new int[] {0xFF_00_00_00, 0xFF_FF_FF_FF, PaletaDaHud.AURA,
                    PaletaDaHud.VIDA, PaletaDaHud.FLUXO}) {
                conferirCanais(PaletaDaHud.clarear(cor, f));
                conferirCanais(PaletaDaHud.escurecer(cor, f));
                conferirCanais(PaletaDaHud.dessaturar(cor, f));
            }
        }
    }

    @Test
    @DisplayName("as tres transformacoes preservam o alpha")
    void oAlphaNaoEComido() {
        int meioTransparente = 0x80_4A_C8_F0;
        assertEquals(0x80, PaletaDaHud.clarear(meioTransparente, 0.9F) >>> 24,
                "clarear opacificou a cor -- a HUD inteira ficaria solida");
        assertEquals(0x80, PaletaDaHud.escurecer(meioTransparente, 0.9F) >>> 24);
        assertEquals(0x80, PaletaDaHud.dessaturar(meioTransparente, 0.9F) >>> 24);
    }

    @Test
    @DisplayName("fator zero e a identidade nas tres")
    void fatorZeroNaoMexeEmNada() {
        int cor = PaletaDaHud.VIDA;
        assertEquals(cor, PaletaDaHud.clarear(cor, 0.0F));
        assertEquals(cor, PaletaDaHud.escurecer(cor, 0.0F));
        assertEquals(cor, PaletaDaHud.dessaturar(cor, 0.0F));
    }

    @Test
    @DisplayName("fator fora de 0..1 e preso, e nao propagado")
    void fatorForaDeFaixaEPreso() {
        assertEquals(PaletaDaHud.clarear(PaletaDaHud.AURA, 1.0F),
                PaletaDaHud.clarear(PaletaDaHud.AURA, 7.0F));
        assertEquals(PaletaDaHud.AURA, PaletaDaHud.clarear(PaletaDaHud.AURA, -3.0F));
    }

    @Test
    @DisplayName("dessaturar por inteiro produz cinza de verdade")
    void dessaturacaoTotalIgualaOsTresCanais() {
        int cinza = PaletaDaHud.dessaturar(PaletaDaHud.AURA, 1.0F);
        int r = (cinza >> 16) & 0xFF;
        int g = (cinza >> 8) & 0xFF;
        int b = cinza & 0xFF;
        assertTrue(Math.abs(r - g) <= 1 && Math.abs(g - b) <= 1,
                "dessaturacao total tem de zerar a diferenca entre canais: "
                        + Integer.toHexString(cinza));
    }

    @Test
    @DisplayName("dessaturar preserva a luminancia; escurecer nao")
    void asDuasOperacoesNaoSaoAMesmaCoisa() {
        int dessaturada = PaletaDaHud.dessaturar(PaletaDaHud.AURA, 1.0F);
        int escurecida = PaletaDaHud.escurecer(PaletaDaHud.AURA, 1.0F);
        assertTrue(luminancia(dessaturada) > 40,
                "a barra suprimida nao pode sumir -- a reserva continua cheia");
        assertEquals(0, luminancia(escurecida));
    }

    @Test
    void comAlphaTrocaSoOAlpha() {
        int cor = PaletaDaHud.comAlpha(PaletaDaHud.AURA, 0.5F);
        assertEquals(PaletaDaHud.AURA & 0x00_FF_FF_FF, cor & 0x00_FF_FF_FF);
        assertEquals(128, cor >>> 24);
        assertEquals(255, PaletaDaHud.comAlpha(PaletaDaHud.AURA, 2.0F) >>> 24);
        assertEquals(0, PaletaDaHud.comAlpha(PaletaDaHud.AURA, -1.0F) >>> 24);
    }

    @Test
    @DisplayName("a Aura da HUD e o MESMO ciano de Ten -- uma verdade, uma cor")
    void aBarraDeAuraNaoInventaUmSegundoCiano() {
        assertEquals(AparenciaDeTecnica.de(
                        com.darkcontinent.nenfoundation.nen.technique.Ten.ID).cor(),
                PaletaDaHud.AURA,
                "dois cianos ligeiramente diferentes na mesma tela leem como "
                        + "defeito de calibragem");
    }

    private static void conferirCanais(int argb) {
        for (int deslocamento : new int[] {16, 8, 0}) {
            int canal = (argb >> deslocamento) & 0xFF;
            assertTrue(canal >= 0 && canal <= 255, "canal fora de faixa: " + canal);
        }
    }

    private static int luminancia(int argb) {
        return (int) Math.round(0.2126 * ((argb >> 16) & 0xFF)
                + 0.7152 * ((argb >> 8) & 0xFF) + 0.0722 * (argb & 0xFF));
    }
}
