package com.darkcontinent.nenfoundation.client.hud.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao da moldura desenhada.
 *
 * <p>Ele mede a escada da diagonal sem instanciar {@code GuiGraphics}. Uma
 * diagonal errada nao levanta excecao -- ela so fica torta, e "torta" e
 * exatamente o que este redesenho existe para tirar.
 */
class PainelAngularTest {

    private static final int CORTE = 6;
    private static final int ALTURA = 44;

    @Test
    @DisplayName("a diagonal superior esquerda desce um pixel por linha")
    void aEscadaEsquerdaEDeQuarentaECincoGraus() {
        for (int linha = 0; linha < CORTE; linha++) {
            assertEquals(CORTE - 1 - linha, PainelAngular.recuoEsquerdo(linha, CORTE),
                    "linha " + linha);
        }
        assertEquals(0, PainelAngular.recuoEsquerdo(CORTE, CORTE),
                "abaixo do corte a linha comeca na borda");
        assertEquals(0, PainelAngular.recuoEsquerdo(ALTURA - 1, CORTE));
    }

    @Test
    @DisplayName("a diagonal inferior direita e o espelho exato da superior esquerda")
    void aEscadaDireitaEspelhaAEsquerda() {
        for (int linha = 0; linha < ALTURA; linha++) {
            int espelhada = ALTURA - 1 - linha;
            assertEquals(PainelAngular.recuoEsquerdo(linha, CORTE),
                    PainelAngular.recuoDireito(espelhada, ALTURA, CORTE),
                    "as duas pontas precisam ter o mesmo angulo; linha " + linha);
        }
    }

    @Test
    @DisplayName("so as duas pontas sao cortadas -- o miolo e retangular")
    void oMioloNaoERecortado() {
        for (int linha = CORTE; linha < ALTURA - CORTE; linha++) {
            assertEquals(0, PainelAngular.recuoEsquerdo(linha, CORTE), "linha " + linha);
            assertEquals(0, PainelAngular.recuoDireito(linha, ALTURA, CORTE),
                    "linha " + linha);
        }
    }

    @Test
    @DisplayName("corte zero ou negativo devolve um retangulo, e nao lanca")
    void semCorteOPainelEUmRetangulo() {
        assertEquals(0, PainelAngular.recuoEsquerdo(0, 0));
        assertEquals(0, PainelAngular.recuoEsquerdo(0, -4));
        assertEquals(0, PainelAngular.recuoDireito(0, ALTURA, 0));
        assertEquals(0, PainelAngular.recuoDireito(0, 0, CORTE));
    }

    @Test
    @DisplayName("nenhum recuo come mais que o corte pedido")
    void oRecuoNuncaPassaDoCorte() {
        for (int linha = -3; linha < ALTURA + 3; linha++) {
            int esquerdo = PainelAngular.recuoEsquerdo(linha, CORTE);
            int direito = PainelAngular.recuoDireito(linha, ALTURA, CORTE);
            assertTrue(esquerdo >= 0 && esquerdo < CORTE, "esquerdo " + esquerdo);
            assertTrue(direito >= 0 && direito < CORTE, "direito " + direito);
        }
    }

    @Test
    @DisplayName("as duas diagonais nunca se cruzam na mesma linha")
    void asPontasNaoSeEncontram() {
        // Se as duas mordessem a mesma linha, um painel baixo viraria uma seta.
        // Com altura 44 e corte 6 isso nao acontece -- e o teste garante que
        // continue nao acontecendo se alguem encolher a moldura.
        int larguraMinima = 200 - 2 * CORTE;
        for (int linha = 0; linha < ALTURA; linha++) {
            int somaDosRecuos = PainelAngular.recuoEsquerdo(linha, CORTE)
                    + PainelAngular.recuoDireito(linha, ALTURA, CORTE);
            assertTrue(somaDosRecuos < larguraMinima,
                    "os recuos comeram a linha inteira; linha " + linha);
        }
    }
}
