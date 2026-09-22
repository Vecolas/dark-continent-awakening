package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O ripple de impacto (#103): quando ele nasce, quanto vale e quando NAO nasce.
 *
 * <p>A issue estava bloqueada por #127 -- "nao ha dano de Nen para disparar" --
 * e passou a "disponivel; falta alimentar {@code AuraImpactState} a partir da
 * camada de dano". O estado ja validava e decaia; <b>ninguem o alimentava</b>,
 * e um estado sem produtor e uma peca que parece pronta.
 *
 * <p>Os casos de NAO nascer sao a maior parte destes testes, e de proposito: um
 * efeito que acende sempre deixa de comunicar qualquer coisa, e o defeito
 * aparece como "a aura pisca o tempo todo" -- que ninguem abre bug sobre.
 */
class ImpactoDeAuraTest {

    private static final int ALGUEM = 7;
    private static final float VIDA_MAXIMA = 20.0F;

    @Test
    @DisplayName("a primeira observacao e base: chegar perto de quem ja pisca nao acende nada")
    void primeiraObservacaoNaoAcende() {
        DetectorDeImpacto detector = new DetectorDeImpacto();
        assertNull(detector.registrar(ALGUEM, 10, 6.0F, VIDA_MAXIMA),
                "A primeira leitura de alguem que ja estava piscando virou impacto. O jogador"
                        + " veria um ripple de uma pancada que nunca aconteceu na frente dele.");
    }

    @Test
    @DisplayName("a borda de subida do hurtTime acende, e a forca vem da vida perdida")
    void bordaDeDanoAcende() {
        DetectorDeImpacto detector = new DetectorDeImpacto();
        detector.registrar(ALGUEM, 0, 20.0F, VIDA_MAXIMA);

        AuraImpactState impacto = detector.registrar(ALGUEM, 10, 15.0F, VIDA_MAXIMA);
        assertNotNull(impacto, "Cinco de vinte de dano nao acendeu o ripple.");
        assertEquals(AuraBodyRegion.TORSO, impacto.region(),
                "O ripple nasceu fora do TRONCO. O cliente NAO sabe onde o golpe acertou --"
                        + " em 1.21.1 o animateHurt descarta o yaw --, e escolher outra regiao"
                        + " seria inventar informacao.");
        assertEquals(0.25F, impacto.strength(), 1.0E-4F,
                "A forca deveria ser a fracao da vida MAXIMA perdida (5/20).");
        assertTrue(impacto.ativo(), "O impacto nasceu morto.");
    }

    @Test
    @DisplayName("a forca e fracao da vida MAXIMA, nao da atual")
    void forcaNaoEscalaComVidaBaixa() {
        DetectorDeImpacto detector = new DetectorDeImpacto();
        detector.registrar(ALGUEM, 0, 4.0F, VIDA_MAXIMA);
        AuraImpactState quaseMorto = detector.registrar(ALGUEM, 10, 2.0F, VIDA_MAXIMA);

        assertNotNull(quaseMorto, "Dois de dano nao acendeu.");
        assertEquals(0.10F, quaseMorto.strength(), 1.0E-4F,
                "Pela vida ATUAL isto seria 50% e o ultimo golpe de quem esta quase morto"
                        + " seria sempre o mais forte da luta, o que inverte a leitura.");
    }

    @Test
    @DisplayName("pancada ABSORVIDA nao acende: o corpo pisca, a aura nao reage")
    void danoAbsorvidoNaoAcende() {
        DetectorDeImpacto detector = new DetectorDeImpacto();
        detector.registrar(ALGUEM, 0, 20.0F, VIDA_MAXIMA);
        assertNull(detector.registrar(ALGUEM, 10, 20.0F, VIDA_MAXIMA),
                "Escudo, armadura ou invulnerabilidade fazem o corpo piscar sem tirar vida."
                        + " Nao houve impacto NA AURA.");
    }

    @Test
    @DisplayName("arranhao abaixo do piso nao acende -- senao a aura pisca o tempo todo")
    void arranhaoNaoAcende() {
        DetectorDeImpacto detector = new DetectorDeImpacto();
        detector.registrar(ALGUEM, 0, 20.0F, VIDA_MAXIMA);
        assertNull(detector.registrar(ALGUEM, 10, 19.8F, VIDA_MAXIMA),
                "1% da vida acendeu o ripple. Fome, veneno e queda de meio coracao acenderiam"
                        + " a aura a cada poucos segundos, e um efeito que acende sempre deixa"
                        + " de comunicar qualquer coisa.");
    }

    @Test
    @DisplayName("hurtTime descendo nao e pancada nova -- e a mesma, desaparecendo")
    void relogioDescendoNaoAcende() {
        DetectorDeImpacto detector = new DetectorDeImpacto();
        detector.registrar(ALGUEM, 0, 20.0F, VIDA_MAXIMA);
        assertNotNull(detector.registrar(ALGUEM, 10, 15.0F, VIDA_MAXIMA));
        assertNull(detector.registrar(ALGUEM, 9, 15.0F, VIDA_MAXIMA),
                "O relogio de dano descendo acendeu de novo. Uma pancada viraria dez.");
    }

    @Test
    @DisplayName("quem sai da vista e esquecido, e ao voltar nao traz ripple atrasado")
    void podaPorPresencaEsquece() {
        DetectorDeImpacto detector = new DetectorDeImpacto();
        detector.registrar(ALGUEM, 0, 20.0F, VIDA_MAXIMA);
        detector.reterSomente(Set.of());

        assertNull(detector.registrar(ALGUEM, 10, 15.0F, VIDA_MAXIMA),
                "Depois da poda, a proxima leitura tem de ser BASE outra vez. Sem isso, quem"
                        + " se afasta, apanha longe e volta traria o ripple de uma pancada que"
                        + " ninguem viu acontecer.");
    }

    // ------------------------------------------------- o realce na distribuicao

    @Test
    @DisplayName("o ripple SOMA na regiao atingida e nao apaga a distribuicao do servidor")
    void realceSomaSemApagar() {
        AuraDistribution ko = new AuraDistribution(0.1F, 0.2F, 0.1F, 1.0F, 0.1F, 0.1F);
        AuraDistribution comRipple = ko.comImpacto(
                AuraImpactState.iniciar(AuraBodyRegion.TORSO, 0.5F));

        assertTrue(comRipple.torso() > ko.torso(), "O tronco nao acendeu.");
        assertEquals(1.0F, comRipple.rightArm(), 1.0E-4F,
                "A concentracao no braco direito mudou por causa de uma pancada no tronco."
                        + " Quem estivesse em Ko perderia a concentracao NA TELA sem ter"
                        + " perdido nada no jogo.");
        assertEquals(ko.head(), comRipple.head(), 1.0E-4F, "Regiao nao atingida mudou.");
    }

    @Test
    @DisplayName("o realce tem TETO: acima de 1 o renderer satura e o efeito some")
    void realceNaoPassaDeUm() {
        AuraDistribution cheia = AuraDistribution.uniforme();
        AuraDistribution comRipple = cheia.comImpacto(
                AuraImpactState.iniciar(AuraBodyRegion.TORSO, 1.0F));
        assertEquals(1.0F, comRipple.torso(), 1.0E-4F,
                "Passar de 1.0 nao acende mais nada e tira a diferenca entre aceso e"
                        + " muito aceso.");
    }

    @Test
    @DisplayName("impacto morto devolve a MESMA distribuicao, sem copia")
    void impactoMortoNaoAloca() {
        AuraDistribution base = AuraDistribution.uniforme();
        AuraImpactState morto = new AuraImpactState(AuraBodyRegion.TORSO, 0, 0.5F);
        assertSame(base, base.comImpacto(morto),
                "Um impacto expirado alocou uma distribuicao nova. Isso roda por jogador por"
                        + " quadro, e lixo por quadro nao aparece como erro -- aparece como"
                        + " coletor trabalhando durante a luta.");
        assertSame(base, base.comImpacto(null), "Impacto ausente alocou.");
    }

    @Test
    @DisplayName("o ripple DECAI: o mesmo impacto acende menos a cada tick")
    void rippleDecai() {
        AuraDistribution base = AuraDistribution.uniforme();
        AuraImpactState novo = AuraImpactState.iniciar(AuraBodyRegion.HEAD, 0.8F);
        AuraDistribution zerado = new AuraDistribution(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        float primeiro = zerado.comImpacto(novo).head();
        float depois = zerado.comImpacto(novo.avancar().avancar()).head();
        assertTrue(depois < primeiro,
                "O ripple nao decaiu: " + primeiro + " -> " + depois + ". Um realce que nao"
                        + " some deixa a regiao acesa para sempre.");
        assertTrue(depois > 0.0F, "O ripple morreu cedo demais.");
    }
}
