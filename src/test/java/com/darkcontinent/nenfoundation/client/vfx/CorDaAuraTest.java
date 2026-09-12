package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O defeito aqui nao aparece como erro: aparece como COR TROCADA.
 *
 * <p>Este teste existe porque a intensidade por regiao PASSA DE 1 -- e vai
 * passar, com Gyo e com Ko, que e todo o proposito dela.
 */
class CorDaAuraTest {

    private static final int AZUL = 0xFF6CA8FF;

    @Test
    @DisplayName("alpha maior que 1 satura, e NAO transborda para o vermelho")
    void alphaAcimaDeUmNaoVazaParaACor() {
        // COM GYO, a regiao concentrada chega a 1.8; com Ko, a 4.0. Sem o
        // clamp, (int) (1.8 * 255) = 459 = 0x1CB, e o bit 8 sobe para o canal
        // vermelho: a aura mudaria de cor em vez de ficar opaca.
        int comGyo = CorDaAura.comAlpha(AZUL, 1.8F);
        int saturado = CorDaAura.comAlpha(AZUL, 1.0F);

        assertEquals(saturado, comGyo, "alpha acima de 1 tem de saturar, nao transbordar");
        assertEquals(0xFF, (comGyo >>> 24) & 0xFF, "alpha saturado e 255");
        assertEquals(AZUL & 0x00FFFFFF, comGyo & 0x00FFFFFF, "a cor tem de sobreviver intacta");
    }

    @Test
    @DisplayName("Ko no braco direito nao pinta o resto do corpo de outra cor")
    void koExtremoPreservaACor() {
        int extremo = CorDaAura.comAlpha(AZUL, 4.0F);
        assertEquals(AZUL & 0x00FFFFFF, extremo & 0x00FFFFFF);
        assertEquals(0xFF, (extremo >>> 24) & 0xFF);
    }

    @Test
    @DisplayName("NaN vira zero, e nao uma cor qualquer")
    void naoFinitoViraZero() {
        // NaN ja atravessou o protocolo deste projeto uma vez. Math.clamp o
        // PROPAGA, entao ele precisa ser barrado de proposito.
        assertEquals(0, (CorDaAura.comAlpha(AZUL, Float.NaN) >>> 24) & 0xFF);
        assertEquals(AZUL & 0x00FFFFFF, CorDaAura.comAlpha(AZUL, Float.NaN) & 0x00FFFFFF);
    }

    @Test
    @DisplayName("alpha negativo vira zero")
    void negativoViraZero() {
        assertEquals(0, (CorDaAura.comAlpha(AZUL, -3.0F) >>> 24) & 0xFF);
    }

    @Test
    @DisplayName("Zetsu e alpha zero, e a cor continua sendo a mesma por baixo")
    void zeroEZero() {
        assertEquals(0x006CA8FF, CorDaAura.comAlpha(AZUL, 0.0F));
    }

    @Test
    @DisplayName("meio caminho e meio caminho")
    void proporcional() {
        assertEquals(128, (CorDaAura.comAlpha(AZUL, 0.5F) >>> 24) & 0xFF);
    }
}
