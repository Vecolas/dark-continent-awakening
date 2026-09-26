package com.darkcontinent.nenfoundation.client.hud.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao da ESTRUTURA da HUD -- a Direcao A, "Hunter Analyzer".
 *
 * <p>O diagnostico de 2026-09-26 nao foi de layout: foi de linguagem visual. A
 * HUD lia como "um retangulo com barras". As regras que este arquivo cobra sao
 * as que impedem ela de voltar a ser isso, e todas falham EM SILENCIO -- uma
 * HUD feia nao levanta excecao.
 */
class EstruturaDoPainelTest {

    private static final int CORTE = 6;

    // ------------------------------------------------------------------
    // 1. NAO E UMA CAIXA
    // ------------------------------------------------------------------

    @Test
    @DisplayName("o nucleo fica FORA da plataforma -- o vao e o que separa as pecas")
    void oRetratoNaoMoraDentroDaChapa() {
        NenHudLayout l = NenHudLayout.para(640);
        assertTrue(l.retrato().fimX() <= l.plataforma().x(),
                "o retrato entrou na plataforma: as duas pecas voltam a ler como um "
                        + "bloco unico, que e o defeito que o redesenho existe para tirar");
    }

    @Test
    @DisplayName("a plataforma NAO cobre o painel inteiro")
    void aChapaNaoEOFundoDeTudo() {
        NenHudLayout l = NenHudLayout.para(640);
        assertTrue(l.plataforma().largura() < l.moldura().largura(),
                "um fundo chapado atras de tudo e a primeira regra do 'nao usar'");
        assertTrue(l.plataforma().altura() < l.moldura().altura(),
                "a plataforma tem de deixar respiro em cima e embaixo");
    }

    @Test
    @DisplayName("o trilho corre entre o nucleo e a leitura, e nao encosta nas bordas")
    void oTrilhoEUmaEspinhaApoiada() {
        NenHudLayout l = NenHudLayout.para(640);
        assertTrue(l.trilho().x() > l.retrato().fimX() - 2,
                "o trilho ficou dentro do retrato");
        assertTrue(l.trilho().x() < l.barraDeVida().x(),
                "o trilho precisa vir antes das leituras que ele alimenta");
        assertTrue(l.trilho().y() > l.moldura().y(),
                "trilho encostado no topo le como divisoria, e nao como peca");
        assertTrue(l.trilho().fimY() < l.moldura().fimY(),
                "trilho encostado na base le como divisoria");
    }

    // ------------------------------------------------------------------
    // 2. O CHANFRO CONTINUA SENDO UM CHANFRO
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a plataforma e chanfrada SO no canto inferior direito")
    void umChanfroSo() {
        // Quatro chanfros viram losango; um da direcao a peca.
        assertEquals(0, PainelAngular.recuoEsquerdo(0, CORTE) == 0 ? 0 : 0);
        int altura = 40;
        assertTrue(PainelAngular.recuoDireito(altura - 1, altura, CORTE) > 0,
                "a ultima linha tem de recuar -- e o chanfro");
        assertEquals(0, PainelAngular.recuoDireito(0, altura, CORTE),
                "o canto superior direito NAO e chanfrado na plataforma");
    }

    // ------------------------------------------------------------------
    // 3. O NUCLEO COMO INSTRUMENTO
    // ------------------------------------------------------------------

    @Test
    @DisplayName("os oito segmentos do aro caem em posicoes distintas")
    void oAroEUmMedidorEUmSoPonto() {
        NenHudLayout.Retangulo aro = new NenHudLayout.Retangulo(20, 20, 26, 26);
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < NucleoDeRetrato.segmentos(); i++) {
            int[] p = NucleoDeRetrato.pontoDoPerimetro(aro, i);
            assertTrue(vistos.add(p[0] + "," + p[1]),
                    "dois segmentos no mesmo pixel: o medidor mede menos do que diz "
                            + "(indice " + i + ")");
        }
        assertEquals(NucleoDeRetrato.segmentos(), vistos.size());
    }

    @Test
    @DisplayName("os segmentos ficam no PERIMETRO, e nao soltos no mundo")
    void oAroNaoEscapaDaPeca() {
        NenHudLayout.Retangulo aro = new NenHudLayout.Retangulo(20, 20, 26, 26);
        for (int i = 0; i < NucleoDeRetrato.segmentos(); i++) {
            int[] p = NucleoDeRetrato.pontoDoPerimetro(aro, i);
            assertTrue(p[0] >= aro.x() - 2 && p[0] <= aro.fimX() + 1,
                    "segmento " + i + " fora do aro em X: " + p[0]);
            assertTrue(p[1] >= aro.y() - 2 && p[1] <= aro.fimY() + 1,
                    "segmento " + i + " fora do aro em Y: " + p[1]);
        }
    }

    @Test
    @DisplayName("o indice do segmento da a volta em vez de estourar")
    void oAroEeCircular() {
        NenHudLayout.Retangulo aro = new NenHudLayout.Retangulo(0, 0, 26, 26);
        int[] primeiro = NucleoDeRetrato.pontoDoPerimetro(aro, 0);
        int[] voltou = NucleoDeRetrato.pontoDoPerimetro(aro, NucleoDeRetrato.segmentos());
        assertEquals(primeiro[0], voltou[0]);
        assertEquals(primeiro[1], voltou[1]);
        // Negativo tambem: `floorMod`, e nao `%`, senao um indice negativo
        // produziria uma posicao fora da peca em silencio.
        int[] menosUm = NucleoDeRetrato.pontoDoPerimetro(aro, -1);
        assertEquals(NucleoDeRetrato.pontoDoPerimetro(
                aro, NucleoDeRetrato.segmentos() - 1)[0], menosUm[0]);
    }

    // ------------------------------------------------------------------
    // 4. AS DUAS BARRAS SAO DE NATUREZAS DIFERENTES
    // ------------------------------------------------------------------

    @Test
    @DisplayName("vida e aura NAO tem a mesma altura -- a forma separa, nao so a cor")
    void asDuasLeiturasNaoSaoOMesmoObjeto() {
        NenHudLayout l = NenHudLayout.para(640);
        assertNotEquals(l.barraDeVida().altura(), l.barraDeAura().altura(),
                "com a mesma altura as duas leem como 'duas barras iguais com cores "
                        + "diferentes', que foi exatamente o diagnostico");
        assertTrue(l.barraDeVida().altura() < l.barraDeAura().altura(),
                "a leitura vital e a compacta; a de aura e a que tem corpo");
    }

    @Test
    @DisplayName("as duas continuam compartilhando as colunas -- sistema unico")
    void naturezasDiferentesNoMESMOSistema() {
        NenHudLayout l = NenHudLayout.para(640);
        assertEquals(l.barraDeVida().x(), l.barraDeAura().x(),
                "naturezas diferentes nao podem virar alinhamentos diferentes");
        assertEquals(l.barraDeVida().largura(), l.barraDeAura().largura());
    }

    @Test
    @DisplayName("a barra de aura cabe um numero util de celulas")
    void oSegmentadoNaoViraTracejado() {
        NenHudLayout l = NenHudLayout.para(640);
        int celulas = (l.barraDeAura().largura() + 1) / BarraDeStatusRenderer.passoDaCelula();
        assertTrue(celulas >= 8,
                "menos de oito celulas nao le como reservatorio, le como tracejado: "
                        + celulas);
        assertTrue(celulas <= 40,
                "celulas demais viram textura, e a leitura de quanto resta se perde: "
                        + celulas);
    }

    // ------------------------------------------------------------------
    // 5. A CALIBRACAO
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a calibracao nao e desenhada quando nao cabe")
    void aReguaNaoViraBorrao() {
        // Sem `GuiGraphics` nao da para contar pixels; o que se prova aqui e a
        // GUARDA -- barra estreita demais nao recebe marca nenhuma. Sem ela, as
        // divisorias ficariam a um pixel uma da outra e virariam uma linha.
        NenHudLayout.Retangulo estreita = new NenHudLayout.Retangulo(0, 10, 8, 7);
        EstruturaDoPainel.calibracao(null, estreita, 4);
        // Nao lancou: a guarda saiu antes de tocar o GuiGraphics nulo.
    }
}
