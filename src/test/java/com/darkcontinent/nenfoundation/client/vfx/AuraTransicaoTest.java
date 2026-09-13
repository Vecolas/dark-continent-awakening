package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A tabela de transicoes, que substituiu o numero unico de duracao. */
class AuraTransicaoTest {

    @Test
    @DisplayName("cada par cai na transicao certa, e a precedencia e a da tabela")
    void precedencia() {
        assertSame(AuraTransicao.LIGAR,
                AuraTransicao.de(AuraVisualMode.OFF, AuraVisualMode.TEN));
        assertSame(AuraTransicao.ELEVAR,
                AuraTransicao.de(AuraVisualMode.TEN, AuraVisualMode.REN));
        assertSame(AuraTransicao.BAIXAR,
                AuraTransicao.de(AuraVisualMode.REN, AuraVisualMode.TEN));
        // TEN -> ZETSU tem de cair em SUPRIMIR, e nao em ELEVAR: e a ordem da
        // tabela que decide, e por isso ela e contrato.
        assertSame(AuraTransicao.SUPRIMIR,
                AuraTransicao.de(AuraVisualMode.TEN, AuraVisualMode.ZETSU));
        assertSame(AuraTransicao.SUPRIMIR,
                AuraTransicao.de(AuraVisualMode.REN, AuraVisualMode.ZETSU));
        assertSame(AuraTransicao.DESLIGAR,
                AuraTransicao.de(AuraVisualMode.REN, AuraVisualMode.OFF));
    }

    @Test
    @DisplayName("uma tecnica nova ganha transicao razoavel sem mexer na tabela")
    void curingaCobreOResto() {
        assertSame(AuraTransicao.PADRAO,
                AuraTransicao.de(AuraVisualMode.TEN, AuraVisualMode.CUSTOM));
        assertSame(AuraTransicao.PADRAO, AuraTransicao.de(AuraVisualMode.TEN, null));
    }

    @Test
    @DisplayName("suprimir e a mais rapida: Zetsu lento nao salva ninguem")
    void zetsuEOMaisRapido() {
        int suprimir = AuraTransicao.SUPRIMIR.ticks();
        for (AuraTransicao t : AuraTransicao.values()) {
            assertTrue(suprimir <= t.ticks(), "SUPRIMIR precisa ser a mais rapida; " + t);
        }
        // A direcao de arte pede entre 200 e 500 ms. Vinte ticks e um segundo.
        assertTrue(suprimir >= 4 && suprimir <= 10,
                "supressao fora da faixa de 200-500 ms: " + suprimir + " ticks");
    }

    @Test
    @DisplayName("subir para Ren e mais lento que ligar Ten")
    void renSobeDevagar() {
        assertTrue(AuraTransicao.ELEVAR.ticks() > AuraTransicao.LIGAR.ticks());
        assertTrue(AuraTransicao.ELEVAR.ticks() > AuraTransicao.BAIXAR.ticks(),
                "subir e o gesto que precisa de peso; descer pode ser mais solto");
    }

    @Test
    @DisplayName("toda curva sai de 0 e CHEGA exatamente em 1")
    void curvasFecham() {
        for (AuraTransicao.Curva curva : AuraTransicao.Curva.values()) {
            assertEquals(0.0F, curva.aplicar(0.0F), 1.0e-6F, "curva nao parte de zero: " + curva);
            // Curva que nao fecha deixa a aura acima do alvo PARA SEMPRE, e
            // ninguem liga um brilho levemente alto a uma formula de easing.
            assertEquals(1.0F, curva.aplicar(1.0F), 1.0e-5F, "curva nao fecha em 1: " + curva);
        }
    }

    @Test
    @DisplayName("OVERSHOOT passa de 1 no meio -- e por isso nao pode reger o modo")
    void ultrapassagemPassaDeUm() {
        float meio = AuraTransicao.Curva.OVERSHOOT.aplicar(0.5F);
        assertTrue(meio > 1.0F,
                "sem ultrapassar, OVERSHOOT nao seria OVERSHOOT: " + meio);
        assertTrue(meio < 1.2F, "ultrapassagem alem de 20% le como elastico, e nao como energia");
    }

    @Test
    @DisplayName("escala invalida nao congela a aura no primeiro quadro")
    void escalaInvalidaCaiParaNeutra() {
        float neutro = AuraTransicao.LIGAR.passoPorTick(1.0F);
        assertEquals(neutro, AuraTransicao.LIGAR.passoPorTick(0.0F), 1.0e-6F);
        assertEquals(neutro, AuraTransicao.LIGAR.passoPorTick(-2.0F), 1.0e-6F);
        assertEquals(neutro, AuraTransicao.LIGAR.passoPorTick(Float.NaN), 1.0e-6F);
    }

    @Test
    @DisplayName("a escala estica e encolhe a duracao, e o passo nunca passa de 1")
    void escalaMudaADuracao() {
        assertTrue(AuraTransicao.ELEVAR.passoPorTick(2.0F)
                < AuraTransicao.ELEVAR.passoPorTick(1.0F));
        for (AuraTransicao t : AuraTransicao.values()) {
            assertTrue(t.passoPorTick(0.1F) <= 1.0F, "passo acima de 1 pularia a animacao; " + t);
        }
    }
}
