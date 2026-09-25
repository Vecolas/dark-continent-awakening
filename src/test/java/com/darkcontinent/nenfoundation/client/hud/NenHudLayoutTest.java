package com.darkcontinent.nenfoundation.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.hud.NenHudLayout.Retangulo;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao da geometria da HUD.
 *
 * <p>Ele cobra tres coisas que falham EM SILENCIO: conteudo vazando para fora
 * da moldura, colunas desalinhadas entre as duas linhas, e a HUD crescendo
 * alem do orcamento de tela combinado no redesenho de 2026-09-25.
 */
class NenHudLayoutTest {

    @Test
    void ancoraNoCantoSuperiorEsquerdoEmCoordenadasGui() {
        NenHudLayout pequeno = NenHudLayout.para(120);
        NenHudLayout grande = NenHudLayout.para(640);

        assertTrue(pequeno.retrato().x() >= NenHudLayout.MARGEM);
        assertEquals(NenHudLayout.MARGEM, pequeno.moldura().x());
        assertEquals(NenHudLayout.MARGEM, grande.moldura().x());
        assertTrue(pequeno.barraDeAura().fimX() <= 120);
        assertTrue(grande.barraDeAura().fimX() <= 640);
        assertTrue(grande.barraDeAura().fimX() < grande.valorDeAura().x());
        assertTrue(grande.barraDeVida().fimX() < grande.valorDeVida().x());
    }

    @Test
    void segmentacaoNaoArredondaAFracaoAntesDoPixel() {
        assertEquals(74, NenHudLayout.preenchimento(100, 0.737F));
        assertEquals(0, NenHudLayout.preenchimento(100, Float.NaN));
        assertEquals(100, NenHudLayout.preenchimento(100, 2.0F));
    }

    @Test
    @DisplayName("as colunas de Vida e Aura sao as MESMAS -- o layout e diagramado")
    void asDuasLinhasCompartilhamAsTresColunas() {
        NenHudLayout l = NenHudLayout.para(640);

        assertEquals(l.rotuloDeVida().x(), l.rotuloDeAura().x(), "rotulo fora do eixo");
        assertEquals(l.barraDeVida().x(), l.barraDeAura().x(), "barra fora do eixo");
        assertEquals(l.barraDeVida().largura(), l.barraDeAura().largura(),
                "barras de comprimentos diferentes sem motivo -- e o defeito que o "
                        + "redesenho existe para tirar");
        assertEquals(l.valorDeVida().fimX(), l.valorDeAura().fimX(),
                "valor alinhado pela direita em uma linha e nao na outra");
        assertEquals(l.barraDeAura().x(), l.barraDeFluxo().x(),
                "a microbarra tem de nascer na mesma coluna das principais");
    }

    @Test
    @DisplayName("nada do conteudo vaza para fora da moldura, com ou sem fluxo")
    void oConteudoCabeDentroDaMoldura() {
        for (boolean comFluxo : List.of(false, true)) {
            NenHudLayout l = NenHudLayout.para(640, comFluxo);
            Retangulo m = l.moldura();
            for (Retangulo r : List.of(l.retrato(), l.nome(), l.chip(),
                    l.rotuloDeVida(), l.barraDeVida(), l.valorDeVida(),
                    l.rotuloDeAura(), l.barraDeAura(), l.valorDeAura())) {
                assertTrue(r.x() >= m.x() && r.fimX() <= m.fimX(),
                        "vazou na horizontal com fluxo=" + comFluxo + ": " + r);
                assertTrue(r.y() >= m.y() && r.fimY() <= m.fimY(),
                        "vazou na vertical com fluxo=" + comFluxo + ": " + r);
            }
        }
    }

    @Test
    @DisplayName("as barras nunca colapsam -- largura minima util, e nao 1px")
    void aBarraNaoEncolheEmSilencio() {
        // ESTE PORTAO NASCEU DE UMA FALHA DO ANTERIOR. Ao alimentar o teste de
        // vazamento com uma coluna de valor larga demais, ele PASSOU: a coluna
        // do valor e ancorada a direita, entao ela nao vaza -- ela come a
        // barra. E `escalar` tem um `Math.max(1, ...)`, que transforma largura
        // negativa em um pixel sem levantar nada. O sintoma seria uma barra
        // invisivel, e nenhum teste falando disso.
        NenHudLayout l = NenHudLayout.para(640);
        int minimo = 40;
        assertTrue(l.barraDeVida().largura() >= minimo,
                "barra de Vida colapsou para " + l.barraDeVida().largura() + "px");
        assertTrue(l.barraDeAura().largura() >= minimo,
                "barra de Aura colapsou para " + l.barraDeAura().largura() + "px");
        assertTrue(NenHudLayout.para(640, true).barraDeFluxo().largura() >= minimo,
                "microbarra de fluxo colapsou");
        assertTrue(l.chip().largura() >= 20, "o chip nao caberia nem 'ZETSU'");
        assertTrue(l.nome().largura() >= 30, "a faixa do nome cortaria tudo");
    }

    @Test
    @DisplayName("as colunas nao se invadem -- rotulo, barra e valor em sequencia")
    void asTresColunasNaoSeSobrepoem() {
        NenHudLayout l = NenHudLayout.para(640);
        assertTrue(l.rotuloDeVida().fimX() <= l.barraDeVida().x(),
                "o rotulo invadiu a barra");
        assertTrue(l.barraDeVida().fimX() < l.valorDeVida().x(),
                "a barra invadiu o valor");
        assertTrue(l.retrato().fimX() <= l.rotuloDeVida().x(),
                "o retrato invadiu a coluna de rotulo");
        assertTrue(l.nome().fimX() <= l.chip().x(),
                "o nome invadiu o chip");
    }

    @Test
    @DisplayName("a microbarra de fluxo so cabe na moldura ALTA")
    void oFluxoExigeAAlturaMaior() {
        Retangulo semFluxo = NenHudLayout.para(640, false).moldura();
        Retangulo comFluxo = NenHudLayout.para(640, true).moldura();
        Retangulo barra = NenHudLayout.para(640, true).barraDeFluxo();

        assertTrue(comFluxo.altura() > semFluxo.altura(),
                "a moldura precisa crescer para caber a microbarra");
        assertEquals(semFluxo.largura(), comFluxo.largura(),
                "crescer para os lados faria a HUD respirar na horizontal a cada "
                        + "ativacao, e o olho segue movimento lateral");
        assertTrue(barra.fimY() <= comFluxo.fimY(), "o fluxo vazou por baixo");
        assertTrue(barra.fimY() > semFluxo.fimY(),
                "se ele coubesse na moldura baixa, a moldura alta seria espaco morto");
    }

    @Test
    @DisplayName("a fila de indicadores desce junto quando o fluxo aparece")
    void aFilaAcompanhaAAlturaDaMoldura() {
        NenHudLayout semFluxo = NenHudLayout.para(640, false);
        NenHudLayout comFluxo = NenHudLayout.para(640, true);

        assertTrue(semFluxo.tecnicasAtivas().y() >= semFluxo.moldura().fimY());
        assertTrue(comFluxo.tecnicasAtivas().y() >= comFluxo.moldura().fimY(),
                "com o fluxo ligado a fila invadiria o painel se o Y nao acompanhasse");
    }

    @Test
    @DisplayName("a HUD respeita o orcamento de largura acordado no redesenho")
    void naoPassaDaFaixaDeLarguraCombinada() {
        // A faixa do plano e 180-240. O portao cobra os dois lados: estreita
        // demais nao caberia o conteudo, larga demais e o defeito original.
        assertTrue(NenHudLayout.LARGURA_DA_MOLDURA >= 180
                        && NenHudLayout.LARGURA_DA_MOLDURA <= 240,
                "largura fora da faixa 180-240: " + NenHudLayout.LARGURA_DA_MOLDURA);
        assertTrue(NenHudLayout.ALTURA_COMPACTA >= 34
                        && NenHudLayout.ALTURA_COMPACTA <= 48,
                "altura compacta fora da faixa 34-48: " + NenHudLayout.ALTURA_COMPACTA);
        assertTrue(NenHudLayout.ALTURA_COM_FLUXO >= 46
                        && NenHudLayout.ALTURA_COM_FLUXO <= 58,
                "altura com fluxo fora da faixa 46-58: " + NenHudLayout.ALTURA_COM_FLUXO);
    }

    @Test
    @DisplayName("em tela estreita o painel encolhe em vez de sair da tela")
    void encolheEmTelaEstreita() {
        NenHudLayout estreito = NenHudLayout.para(100);
        assertTrue(estreito.moldura().fimX() <= 100,
                "a moldura saiu da tela em GUI estreita");
        assertTrue(estreito.moldura().largura() > 0);
    }

    @Test
    void dimensaoNegativaReprova() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new Retangulo(0, 0, -1, 4));
    }
}
