package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.hud.component.TecnicasAtivasRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A fila de indicadores de tecnica ativa.
 *
 * <p>O DESENHO EM SI NAO E TESTAVEL AQUI -- ele precisa do cliente de pe. O que
 * da para conferir sem jogo e a aritmetica da fila, que e onde moram os dois
 * erros que nao dariam excecao nenhuma: indicadores empilhados uns sobre os
 * outros, e tecnicas somem em silencio quando passam do teto.
 */
class TecnicasAtivasRendererTest {

    @Test
    @DisplayName("os indicadores nao se sobrepoem")
    void naoSeSobrepoem() {
        int anterior = Integer.MIN_VALUE;
        for (int i = 0; i < TecnicasAtivasRenderer.TETO_DE_INDICADORES; i++) {
            int x = TecnicasAtivasRenderer.xDoIndicador(10, i);
            assertTrue(x > anterior,
                    "o indicador " + i + " nasceu em x=" + x + ", antes ou em cima do"
                            + " anterior (" + anterior + "). Dois desenhados no mesmo"
                            + " lugar parecem UM: o jogador contaria tecnicas a menos.");
            anterior = x;
        }
    }

    @Test
    @DisplayName("a fila comeca no canto que recebeu, e nao num offset proprio")
    void comecaNoCanto() {
        assertEquals(42, TecnicasAtivasRenderer.xDoIndicador(42, 0),
                "o primeiro indicador saiu do lugar que o layout mandou. Geometria"
                        + " do HUD mora no layout; um offset escondido aqui seria a"
                        + " mesma posicao decidida em dois lugares.");
    }

    @Test
    @DisplayName("nada e desenhado quando nada esta ativo")
    void nadaAtivoNadaDesenha() {
        assertEquals(0, TecnicasAtivasRenderer.quantosDesenhar(0));
        assertEquals(0, TecnicasAtivasRenderer.quantasSobram(0));
    }

    @Test
    @DisplayName("passando do teto, o que sobra vira contagem em vez de sumir")
    void oQueSobraEContado() {
        int teto = TecnicasAtivasRenderer.TETO_DE_INDICADORES;

        assertEquals(teto, TecnicasAtivasRenderer.quantosDesenhar(teto + 3),
                "desenhou mais do que cabe; a fila sairia da moldura.");
        assertEquals(3, TecnicasAtivasRenderer.quantasSobram(teto + 3),
                "Tres tecnicas ligadas sumiram da tela sem virar contagem. Truncar"
                        + " em silencio faz o jogador acreditar que so as visiveis"
                        + " estao ligadas -- e sao elas que estao drenando aura.");
        assertEquals(0, TecnicasAtivasRenderer.quantasSobram(teto),
                "no limite exato nao sobra nada, e o '+0' nao deve aparecer.");
    }

    @Test
    @DisplayName("contagem negativa nao inventa indicador")
    void negativoNaoQuebra() {
        // Nao deveria acontecer -- mas um `size()` vindo de calculo em vez de
        // colecao ja chegou negativo neste projeto antes, e um loop com limite
        // negativo aqui nao daria erro: so nao desenharia, sem dizer por que.
        assertEquals(0, TecnicasAtivasRenderer.quantosDesenhar(-1));
        assertEquals(0, TecnicasAtivasRenderer.quantasSobram(-1));
    }
}
