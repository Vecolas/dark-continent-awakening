package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.combat.FaixaDoCorpo;
import com.darkcontinent.nenfoundation.nen.combat.ForcaDeImpacto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O ripple de impacto (#103): o que ele faz com a distribuicao, e onde.
 *
 * <p><b>ELE DEIXOU DE SER CORPO INTEIRO EM 2026-09-22.</b> Antes disso o
 * cliente inferia o golpe de uma queda de vida, e a queda de vida nao tem
 * endereco: acender uma regiao era inventar localizacao, e o palpite fixo no
 * TRONCO era ilegivel porque o tronco e a regiao mais larga e menos definida da
 * silhueta.
 *
 * <p>Com {@code ImpactoDeAuraS2C} o servidor passou a dizer a FAIXA -- ele ja
 * a calculava para a defesa e a jogava fora. A curva de forca mudou de lado
 * junto, e vive em {@code ForcaDeImpactoTest}.
 *
 * <p><b>POR QUE A FAIXA, E NAO UMA REGIAO.</b> {@code FaixaDoCorpo} tem tres
 * valores e {@code PERNAS} cobre as duas pernas. Mandar uma regiao so obrigaria
 * o servidor a escolher uma perna -- lateralidade que o golpe nao tem.
 */
class ImpactoDeAuraTest {

    private static final float VIDA_MAXIMA = 20.0F;

    // --------------------------------------------- o realce na distribuicao

    @Test
    @DisplayName("o ripple SOBE ACIMA de 1.0 -- em Ten a distribuicao ja esta saturada")
    void realceUltrapassaOTeto() {
        // Ten desenha com uniforme(): 1.0 em TODAS as regioes.
        AuraDistribution ten = AuraDistribution.uniforme();
        AuraDistribution comRipple = ten.comImpacto(
                AuraImpactState.iniciar(FaixaDoCorpo.TRONCO, 1.0F));

        assertTrue(comRipple.torso() > 1.0F,
                "O realce foi cortado em 1.0, e em Ten TODA regiao ja vale 1.0 -- o ripple"
                        + " virava no-op. Era o motivo principal de \"nao acende nunca\", e"
                        + " nenhum dos onze testes pegava: todos mediam uma distribuicao"
                        + " zerada. O renderer usa o valor como MULTIPLICADOR de alpha, e"
                        + " acima de 1.0 ele clareia -- que e o que um ripple precisa fazer.");
    }

    @Test
    @DisplayName("a FAIXA atingida acende MAIS que o resto -- e o resto ainda acende")
    void aFaixaAcendeMais() {
        AuraDistribution cheia = AuraDistribution.uniforme();
        AuraDistribution comRipple = cheia.comImpacto(
                AuraImpactState.iniciar(FaixaDoCorpo.CABECA, 1.0F));

        assertTrue(comRipple.head() > comRipple.torso(),
                "A faixa atingida nao acendeu mais que o resto. E esta a informacao nova que o"
                        + " servidor passou a mandar, e sem ela o payload nao serve para nada:"
                        + " o ripple continuaria dizendo \"levei dano\" sem dizer \"ali\".");

        assertTrue(comRipple.torso() > cheia.torso(),
                "O corpo fora da faixa parou de acender. O pulso de corpo inteiro foi a UNICA"
                        + " versao que alguem conseguiu ver -- \"no hit na aura toda passa um"
                        + " pulso azul\" --, e apagar o resto arrisca voltar ao estado em que o"
                        + " F6 mostrava o ripple subindo e a tela nao mostrava nada. O eco"
                        + " segura \"levei dano\"; a faixa acrescenta \"ali\".");
    }

    @Test
    @DisplayName("PERNAS acende as DUAS pernas -- a faixa nao inventa lateralidade")
    void pernasAcendemJuntas() {
        AuraDistribution comRipple = AuraDistribution.uniforme().comImpacto(
                AuraImpactState.iniciar(FaixaDoCorpo.PERNAS, 1.0F));

        assertEquals(comRipple.leftLeg(), comRipple.rightLeg(), 1.0E-4F,
                "Uma perna acendeu mais que a outra. A faixa PERNAS cobre as duas, e escolher"
                        + " uma seria o servidor inventando de que lado o golpe caiu -- que e"
                        + " exatamente o que mandar a faixa em vez da regiao existe para"
                        + " impedir.");
        assertTrue(comRipple.leftLeg() > comRipple.head(),
                "As pernas nao acenderam mais que a cabeca num golpe nas PERNAS.");
    }

    @Test
    @DisplayName("os BRACOS ficam no eco -- a faixa e derivada da ALTURA, e braco nao tem altura propria")
    void bracosFicamNoEco() {
        AuraDistribution comRipple = AuraDistribution.uniforme().comImpacto(
                AuraImpactState.iniciar(FaixaDoCorpo.TRONCO, 1.0F));

        assertEquals(comRipple.leftArm(), comRipple.head(), 1.0E-4F,
                "O braco passou a receber o realce da faixa. FaixaDoCorpo vem da ALTURA do"
                        + " golpe, e um braco ocupa a mesma altura do tronco: incluir os bracos"
                        + " no TRONCO acenderia quase toda a silhueta e devolveria o ripple ao"
                        + " corpo inteiro, com mais passos e a mesma leitura de antes.");
    }

    @Test
    @DisplayName("o ripple PRESERVA a concentracao: quem esta em Ko continua em Ko na tela")
    void rippleNaoAchataAConcentracao() {
        // Ko: quase tudo no braco direito.
        AuraDistribution ko = new AuraDistribution(0.1F, 0.2F, 0.1F, 1.0F, 0.1F, 0.1F);
        float razaoAntes = ko.rightArm() / ko.leftArm();

        AuraDistribution comRipple = ko.comImpacto(
                AuraImpactState.iniciar(FaixaDoCorpo.TRONCO, 0.5F));

        assertTrue(comRipple.torso() > ko.torso(), "O tronco nao acendeu.");
        assertTrue(comRipple.rightArm() > ko.rightArm(), "O braco concentrado nao acendeu.");
        assertEquals(razaoAntes, comRipple.rightArm() / comRipple.leftArm(), 1.0E-3F,
                "A concentracao ACHATOU durante o flash. Somar o mesmo valor a um braco em 1,0"
                        + " e a um tronco em 0,2 derruba a razao de 5:1 para 1,9:1 -- quem"
                        + " estivesse em Ko perderia a concentracao NA TELA sem ter perdido"
                        + " nada no jogo. Multiplicar acende tudo e preserva a razao. Os dois"
                        + " bracos estao no MESMO eco, entao a razao entre eles e o teste"
                        + " limpo disto.");
    }

    @Test
    @DisplayName("um soco DOBRA o alpha da aura -- senao o efeito nao se ve, e foi o que houve")
    void oSocoDobraOAlpha() {
        // O alpha da borda do Ten e 0,20; o renderer faz alpha * intensidade(regiao).
        final float ALPHA_DA_BORDA_DO_TEN = 0.20F;

        float forca = ForcaDeImpacto.de(1.0F, VIDA_MAXIMA);
        AuraImpactState soco = AuraImpactState.iniciar(FaixaDoCorpo.TRONCO, forca);

        float antes = ALPHA_DA_BORDA_DO_TEN;
        float depois = ALPHA_DA_BORDA_DO_TEN
                * AuraDistribution.uniforme().comImpacto(soco).torso();

        assertTrue(depois > antes * 1.8F,
                "No pico o soco levou o alpha de " + antes + " para " + depois + " -- menos que"
                        + " o dobro. A primeira versao movia 0,200 para 0,290, e o relato de jogo"
                        + " foi: \"o F6 mostra o ripple subindo, so nao da para ver no"
                        + " personagem\". Nove centesimos de alpha num involucro translucido,"
                        + " decaindo em 0,6 s, nao sao um flash.");
    }

    @Test
    @DisplayName("o PLATO segura o pico -- um pico de um quadro o olho descarta como ruido")
    void oPicoTemPlato() {
        AuraImpactState novo = AuraImpactState.iniciar(FaixaDoCorpo.TRONCO, 1.0F);
        assertEquals(1.0F, novo.progresso(), 1.0E-4F, "o ripple nao nasce no pico");
        assertEquals(1.0F, novo.avancar().avancar().progresso(), 1.0E-4F,
                "o pico durou menos que o plato: sem ele o efeito decai desde o PRIMEIRO"
                        + " quadro, e um pico de um quadro nao e flash, e cintilar.");
        assertTrue(novo.avancar().avancar().avancar().avancar().avancar().avancar()
                        .progresso() < 1.0F,
                "o plato nao acabou nunca: o ripple ficaria cheio ate sumir de repente.");
    }

    @Test
    @DisplayName("impacto morto devolve a MESMA distribuicao, sem copia")
    void impactoMortoNaoAloca() {
        AuraDistribution base = AuraDistribution.uniforme();
        AuraImpactState morto = new AuraImpactState(FaixaDoCorpo.TRONCO, 0, 0.5F);
        assertSame(base, base.comImpacto(morto),
                "Um impacto expirado alocou uma distribuicao nova. Isso roda por jogador por"
                        + " quadro, e lixo por quadro nao aparece como erro -- aparece como"
                        + " coletor trabalhando durante a luta.");
        assertSame(base, base.comImpacto(null), "Impacto ausente alocou.");
    }

    @Test
    @DisplayName("o ripple DECAI depois do plato")
    void rippleDecai() {
        AuraDistribution base = AuraDistribution.uniforme();
        AuraImpactState novo = AuraImpactState.iniciar(FaixaDoCorpo.CABECA, 0.8F);

        float noPico = base.comImpacto(novo).head();
        // Seis ticks: passa do plato, que segura o pico pelo primeiro terco.
        AuraImpactState velho = novo;
        for (int i = 0; i < 6; i++) {
            velho = velho.avancar();
        }
        float depois = base.comImpacto(velho).head();

        assertTrue(depois < noPico,
                "O ripple nao decaiu: " + noPico + " -> " + depois + ". Um realce que nao some"
                        + " deixa a aura acesa para sempre.");
        assertTrue(depois > base.head(), "O ripple morreu cedo demais.");
    }

    @Test
    @DisplayName("em ZETSU o ripple nao acende -- zero vezes qualquer coisa continua zero")
    void zetsuNaoAcende() {
        AuraDistribution zetsu = AuraDistribution.zetsu();
        AuraDistribution comRipple = zetsu.comImpacto(
                AuraImpactState.iniciar(FaixaDoCorpo.TRONCO, 1.0F));

        assertEquals(0.0F, comRipple.torso(), 1.0E-6F,
                "A aura de quem esta em Zetsu acendeu ao levar pancada. Zetsu e ausencia de"
                        + " aura visivel; um flash ali denunciaria quem escolheu se esconder --"
                        + " e seria o cliente inventando o que o servidor mandou suprimir.");
    }
}
