package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O portao da barra de vida: os casos que nao levantam excecao e desenham errado. */
class ProjecaoDeVidaTest {

    @Test
    @DisplayName("vida maxima zero nao divide por zero nem desenha barra cheia")
    void maximoZeroNaoEBarraCheia() {
        ProjecaoDeVida morto = ProjecaoDeVida.de(0.0F, 0.0F);
        assertFalse(morto.disponivel());
        assertEquals(0.0F, morto.fracao());
        assertEquals("--", morto.texto());
        assertFalse(ProjecaoDeVida.ausente().disponivel());
    }

    @Test
    @DisplayName("absorcao e presa no teto em vez de vazar por cima do valor")
    void absorcaoNaoEstouraABarra() {
        ProjecaoDeVida comAbsorcao = ProjecaoDeVida.de(28.0F, 20.0F);
        assertEquals(1.0F, comAbsorcao.fracao(),
                "fracao acima de 1 desenharia por cima do numero a direita");
    }

    @Test
    @DisplayName("meio coracao nunca vira zero")
    void meioCoracaoArredondaParaCima() {
        assertEquals("1/20", ProjecaoDeVida.de(0.5F, 20.0F).texto(),
                "arredondar para baixo diria que um jogador vivo tem zero de vida");
        assertEquals("20/20", ProjecaoDeVida.de(20.0F, 20.0F).texto());
        assertEquals("10/20", ProjecaoDeVida.de(9.5F, 20.0F).texto());
    }

    @Test
    @DisplayName("vida negativa e NaN nao propagam para a tela")
    void valoresImpossiveisSaoSaneados() {
        assertEquals(0.0F, ProjecaoDeVida.de(-5.0F, 20.0F).fracao());
        assertEquals("0/20", ProjecaoDeVida.de(-5.0F, 20.0F).texto());
        assertEquals(0.0F, ProjecaoDeVida.de(Float.NaN, 20.0F).fracao());
        assertEquals(0.0F, ProjecaoDeVida.de(10.0F, Float.NaN).fracao());
        assertFalse(ProjecaoDeVida.de(10.0F, Float.NaN).disponivel());
    }

    @Test
    @DisplayName("vida maxima alterada por atributo continua correta")
    void maximoNaoVanillaFunciona() {
        ProjecaoDeVida reforcado = ProjecaoDeVida.de(30.0F, 60.0F);
        assertEquals(0.5F, reforcado.fracao());
        assertEquals("30/60", reforcado.texto());
        assertTrue(reforcado.disponivel());
    }
}
