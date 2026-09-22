package com.darkcontinent.nenfoundation.nen.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A curva que transforma dano em forca de ripple.
 *
 * <p>ELA MUDOU DE LADO EM 2026-09-22, e estes testes vieram junto. A conta
 * morava em {@code DetectorDeImpacto}, no cliente, alimentada pela diferenca
 * entre duas leituras de vida. Com {@code ImpactoDeAuraS2C} a origem passou a
 * ser o servidor, que tem o numero de dano real -- e a inferencia foi
 * aposentada.
 *
 * <p><b>O QUE OS TESTES DO DETECTOR PROVAVAM E NAO CABE MAIS AQUI:</b> que a
 * primeira observacao era base, que a queda de vida era o gatilho, e que a poda
 * por presenca esquecia quem saiu de vista. Os tres existiam porque o cliente
 * tinha de ADIVINHAR que houve um golpe. Ele nao adivinha mais: o servidor
 * avisa.
 *
 * <p>Os casos de NAO acender continuam sendo a maior parte, e de proposito: um
 * efeito que acende sempre deixa de comunicar qualquer coisa, e o defeito
 * aparece como "a aura pisca o tempo todo" -- que ninguem abre bug sobre.
 */
class ForcaDeImpactoTest {

    private static final float VIDA_MAXIMA = 20.0F;

    @Test
    @DisplayName("um SOCO de mao vazia tem de acender de forma VISIVEL")
    void socoDeMaoVaziaAcende() {
        float soco = ForcaDeImpacto.de(1.0F, VIDA_MAXIMA);

        assertTrue(soco > 0.35F,
                "O soco acendeu em " + soco + ", fraco demais para ver. Pela fracao CRUA isto"
                        + " daria 0,05 -- um realce de 5% no multiplicador de alpha, que nao"
                        + " aparece na tela. Foi assim que o efeito nasceu invisivel: jogador"
                        + " batendo em jogador e o teste mais obvio que existe, e a curva"
                        + " existe exatamente para ele.");
    }

    @Test
    @DisplayName("a forca e fracao da vida MAXIMA, nao da atual")
    void fracaoDaVidaMaxima() {
        // O mesmo dano, em alguem com o dobro da vida maxima, vale MENOS.
        assertTrue(ForcaDeImpacto.de(2.0F, 40.0F) < ForcaDeImpacto.de(2.0F, VIDA_MAXIMA),
                "A forca deixou de depender da vida maxima. Pela vida ATUAL, o ultimo golpe de"
                        + " alguem quase morto seria sempre o mais forte da luta.");
    }

    @Test
    @DisplayName("pancada ABSORVIDA nao acende: o corpo pisca, a aura nao reage")
    void absorvidaNaoAcende() {
        assertEquals(0.0F, ForcaDeImpacto.de(0.0F, VIDA_MAXIMA), 1.0E-6F,
                "Dano zero acendeu. Escudo, invulnerabilidade e cura nao sao impacto NA AURA.");
        assertEquals(0.0F, ForcaDeImpacto.de(-3.0F, VIDA_MAXIMA), 1.0E-6F,
                "Dano negativo acendeu.");
    }

    @Test
    @DisplayName("arranhao abaixo do piso nao acende -- senao a aura pisca o tempo todo")
    void arranhaoNaoAcende() {
        // 2% de 20 e 0,4; o piso e exatamente 2%.
        assertEquals(0.0F, ForcaDeImpacto.de(0.3F, VIDA_MAXIMA), 1.0E-6F,
                "Um arranhao abaixo do piso acendeu. Dano de fome, de veneno e o arranhao de"
                        + " meio coracao acenderiam a aura a cada poucos segundos.");
        assertTrue(ForcaDeImpacto.de(0.5F, VIDA_MAXIMA) > 0.0F,
                "O piso subiu e engoliu dano legitimo.");
    }

    @Test
    @DisplayName("acima de um quarto da vida a forca SATURA, e nao passa de 1")
    void saturaNoTeto() {
        assertEquals(1.0F, ForcaDeImpacto.de(5.0F, VIDA_MAXIMA), 1.0E-4F,
                "Um quarto da vida deveria dar forca TOTAL; e esse o ponto de saturacao.");
        assertEquals(1.0F, ForcaDeImpacto.de(19.0F, VIDA_MAXIMA), 1.0E-4F,
                "Um golpe quase fatal passou de 1,0. A diferenca entre \"levei uma pancada"
                        + " seria\" e \"quase morri\" nao precisa caber no halo -- ela ja esta"
                        + " na barra de vida. E acima de 1 o multiplicador de alpha estoura.");
    }

    @Test
    @DisplayName("vida maxima invalida nao acende, em vez de estourar")
    void vidaMaximaInvalida() {
        assertEquals(0.0F, ForcaDeImpacto.de(5.0F, 0.0F), 1.0E-6F, "vida maxima zero");
        assertEquals(0.0F, ForcaDeImpacto.de(5.0F, -1.0F), 1.0E-6F, "vida maxima negativa");
        assertEquals(0.0F, ForcaDeImpacto.de(Float.NaN, VIDA_MAXIMA), 1.0E-6F, "dano NaN");
    }
}
