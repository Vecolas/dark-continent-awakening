package com.darkcontinent.nenfoundation.nen.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A conta que decide quanto dano alguem toma.
 *
 * <p>E a regra com maior chance de deixar o jogo trivial ou injusto, e nenhum
 * teste de gameplay vai perceber isso -- so jogar percebe. O que da para provar
 * aqui sao os limites: que ninguem fica imortal, que ninguem se cura apanhando,
 * e que a faixa atingida importa.
 */
class DefesaDeNenTest {

    private static final double TETO = 0.45D;

    // ------------------------------------------------- os limites duros

    @Test
    @DisplayName("nenhuma combinacao chega a imortalidade")
    void nuncaChegaACemPorCento() {
        // SEM TETO, numeros mal escolhidos numa sessao de balanceamento levam a
        // reducao a 100%. Isso nao da erro: da um jogador que nao morre mais, e
        // ninguem liga uma coisa na outra.
        float r = DefesaDeNen.reducao(999.0D, AlocacaoDeAura.uniforme(),
                FaixaDoCorpo.TRONCO, 1.0D);

        assertTrue(r < 1.0F, "a reducao chegou a " + r + " -- o jogador e imortal.");
        assertTrue(r <= DefesaDeNen.TETO_ABSOLUTO,
                "a reducao passou do teto absoluto do codigo: " + r + ". Nem a"
                        + " config pedindo 1.0 pode chegar la.");
    }

    @Test
    @DisplayName("apanhar nunca cura")
    void danoNuncaFicaNegativo() {
        // Dano negativo no Minecraft CURA. Uma reducao acima de 1 transformaria
        // a tecnica de defesa em regeneracao, e o relato seria "fico mais forte
        // quando me batem".
        assertEquals(0.0F, DefesaDeNen.danoDepoisDaAura(10.0F, 5.0F), 1.0e-6F,
                "reducao acima de 1 virou dano negativo");
        assertEquals(0.0F, DefesaDeNen.danoDepoisDaAura(10.0F, 1.0F), 1.0e-6F);
    }

    @Test
    @DisplayName("sem tecnica, sem protecao")
    void semTecnicaNaoReduz() {
        assertEquals(0.0F, DefesaDeNen.reducao(0.0D, AlocacaoDeAura.uniforme(),
                FaixaDoCorpo.TRONCO, TETO), 1.0e-6F);
        assertEquals(10.0F, DefesaDeNen.danoDepoisDaAura(10.0F, 0.0F), 1.0e-6F,
                "o dano mudou sem tecnica nenhuma ativa");
    }

    @Test
    @DisplayName("NaN e infinito nao viram protecao")
    void naoNumeroNaoProtege() {
        for (double ruim : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -1.0D}) {
            assertEquals(0.0F, DefesaDeNen.reducao(ruim, AlocacaoDeAura.uniforme(),
                    FaixaDoCorpo.TRONCO, TETO), 1.0e-6F, "aceitou protecao " + ruim);
        }
        assertEquals(0.0F, DefesaDeNen.reducao(0.5D, null, FaixaDoCorpo.TRONCO, TETO), 1.0e-6F);
        assertTrue(Float.isFinite(DefesaDeNen.danoDepoisDaAura(Float.NaN, 0.5F)));
    }

    // --------------------------------------------- a faixa importa

    @Test
    @DisplayName("a altura decide a faixa, e os limites sao exatos")
    void alturaDecideAFaixa() {
        assertEquals(FaixaDoCorpo.PERNAS, FaixaDoCorpo.porAltura(0.0D));
        assertEquals(FaixaDoCorpo.PERNAS, FaixaDoCorpo.porAltura(0.34D));
        assertEquals(FaixaDoCorpo.TRONCO, FaixaDoCorpo.porAltura(0.35D),
                "0.35 e o comeco do tronco; um `<` trocado por `<=` aqui move a"
                        + " fronteira de lugar sem ninguem notar.");
        assertEquals(FaixaDoCorpo.TRONCO, FaixaDoCorpo.porAltura(0.79D));
        assertEquals(FaixaDoCorpo.CABECA, FaixaDoCorpo.porAltura(0.80D));
        assertEquals(FaixaDoCorpo.CABECA, FaixaDoCorpo.porAltura(2.0D),
                "golpe vindo de muito alto ainda acerta a cabeca");
        assertEquals(FaixaDoCorpo.PERNAS, FaixaDoCorpo.porAltura(-5.0D),
                "golpe vindo de baixo do chao ainda acerta as pernas");
    }

    @Test
    @DisplayName("altura NaN cai no tronco, e nao numa faixa qualquer")
    void alturaNaNCaiNoTronco() {
        // NaN chega de divisao por altura zero, que acontece com entidade em
        // estado estranho. O padrao seguro e a faixa que quase tudo acerta.
        assertEquals(FaixaDoCorpo.TRONCO, FaixaDoCorpo.porAltura(Double.NaN));
    }

    @Test
    @DisplayName("com a aura espalhada, toda faixa protege igual")
    void repousoProtegeIgual() {
        // A NORMALIZACAO E O ASSUNTO DESTE TESTE. A alocacao soma 1.0 entre seis
        // regioes, entao cada uma vale 0,167 -- usar isso cru daria ao jogador
        // em repouso um sexto da protecao que a tecnica promete.
        for (FaixaDoCorpo faixa : FaixaDoCorpo.values()) {
            assertEquals(1.0F, faixa.auraDefendendo(AlocacaoDeAura.uniforme()), 1.0e-5F,
                    "a faixa " + faixa + " nao devolveu 1.0 em repouso");
        }
    }

    @Test
    @DisplayName("concentrar num lugar DESPROTEGE o resto -- o risco de Ko")
    void concentrarExpoeORestante() {
        // O canone: se voce poe quase tudo no punho, erra o golpe e leva um no
        // tronco, e catastrofico. Este teste e essa frase.
        AlocacaoDeAura ko = AlocacaoDeAura.concentrando(RegiaoDoCorpo.BRACO_DIREITO, 0.95F);

        float noTronco = DefesaDeNen.reducao(1.0D, ko, FaixaDoCorpo.TRONCO, TETO);
        float emRepouso = DefesaDeNen.reducao(1.0D, AlocacaoDeAura.uniforme(),
                FaixaDoCorpo.TRONCO, TETO);

        assertTrue(noTronco < emRepouso,
                "com 95% no braco, o tronco protegeu " + noTronco + " -- tanto"
                        + " quanto em repouso (" + emRepouso + "). Sem essa perda,"
                        + " Ko nao tem risco e vira Gyo com numero maior.");
        assertTrue(noTronco < 0.1F,
                "o tronco de quem esta em Ko ainda protege " + noTronco);
    }

    @Test
    @DisplayName("concentrar na cabeca protege a cabeca")
    void concentrarProtegeOndeEsta() {
        AlocacaoDeAura naCabeca = AlocacaoDeAura.concentrando(RegiaoDoCorpo.CABECA, 0.5F);
        float cabeca = DefesaDeNen.reducao(0.5D, naCabeca, FaixaDoCorpo.CABECA, TETO);
        float pernas = DefesaDeNen.reducao(0.5D, naCabeca, FaixaDoCorpo.PERNAS, TETO);

        assertTrue(cabeca > pernas,
                "concentrar na cabeca nao protegeu mais a cabeca (" + cabeca
                        + ") do que as pernas (" + pernas + ").");
    }

    @Test
    @DisplayName("as duas pernas contam como UMA faixa, e nao pelo dobro")
    void pernasNaoSomamEmDobro() {
        // Somar as duas faria as pernas protegerem o dobro do tronco em
        // repouso, sem que nada no desenho diga isso.
        assertEquals(FaixaDoCorpo.TRONCO.auraDefendendo(AlocacaoDeAura.uniforme()),
                FaixaDoCorpo.PERNAS.auraDefendendo(AlocacaoDeAura.uniforme()), 1.0e-5F,
                "em repouso, pernas e tronco deviam proteger igual");
    }

    @Test
    @DisplayName("mais protecao reduz mais, ate o teto")
    void maisProtecaoReduzMais() {
        float ten = DefesaDeNen.reducao(0.20D, AlocacaoDeAura.uniforme(),
                FaixaDoCorpo.TRONCO, TETO);
        float ken = DefesaDeNen.reducao(0.75D, AlocacaoDeAura.uniforme(),
                FaixaDoCorpo.TRONCO, TETO);

        assertTrue(ken > ten,
                "Ken (" + ken + ") nao protege mais que Ten (" + ten + "); a"
                        + " defesa pesada seria igual ao estado de repouso.");
        assertTrue(ken <= TETO + 1.0e-6F, "Ken passou do teto de config");
    }
}
