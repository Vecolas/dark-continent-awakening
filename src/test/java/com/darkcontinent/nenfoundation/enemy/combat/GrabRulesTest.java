package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Sustenta que ser engolido e uma JANELA com saida, nao uma sentenca.
 *
 * <p>QUEM LIGA, DESLIGA: o agarrao tem quatro razoes para terminar (tempo, dano
 * acumulado, vitima morta, predador morto) e todas moram em {@code solta}. Espalhadas
 * pelos pontos de saida da entidade, uma delas ia faltar -- e faltar nao da erro: da
 * um jogador preso para sempre dentro de um sapo que ja morreu.</p>
 */
class GrabRulesTest {
    /** Os mesmos numeros de {@code HunterExamProfiles.frogGrabRules()}. */
    private static final GrabRules REGRAS = new GrabRules(100, 20, 3.0F, 12.0F);

    @Test
    void oDanoPulsaNosMultiplosDoIntervaloENuncaNoTickZero() {
        assertFalse(REGRAS.aplicaDano(0),
                "pulsar no tick 0 cobraria um pulso no instante do agarrao, antes de existir janela");
        assertTrue(REGRAS.aplicaDano(20), "primeiro pulso no primeiro multiplo do intervalo");
        assertTrue(REGRAS.aplicaDano(40));
        assertTrue(REGRAS.aplicaDano(60));
        assertTrue(REGRAS.aplicaDano(80));
        assertTrue(REGRAS.aplicaDano(100), "o ultimo tick do agarrao ainda pulsa");
    }

    @Test
    void foraDosMultiplosDoIntervaloNaoHaPulso() {
        assertFalse(REGRAS.aplicaDano(1));
        assertFalse(REGRAS.aplicaDano(19), "um tick antes do pulso nao machuca");
        assertFalse(REGRAS.aplicaDano(21), "um tick depois do pulso nao machuca de novo");
        assertFalse(REGRAS.aplicaDano(39));
    }

    @Test
    void soltarPorTempoEInclusivoNoLimite() {
        assertFalse(REGRAS.soltaPorTempo(99), "um tick antes do maximo ainda segura");
        assertTrue(REGRAS.soltaPorTempo(100), "exatamente o tempo maximo ja solta");
        assertTrue(REGRAS.soltaPorTempo(101), "passar do maximo obviamente solta");
    }

    @Test
    void soltarPorDanoEInclusivoNoLimite() {
        assertFalse(REGRAS.soltaPorDano(11.9F), "dano abaixo do limiar nao liberta ninguem");
        assertTrue(REGRAS.soltaPorDano(12.0F), "exatamente o dano de escape ja liberta");
        assertTrue(REGRAS.soltaPorDano(30.0F), "bater muito alem do limiar tambem liberta");
    }

    /**
     * A prova de que nenhum caminho de saida foi esquecido: cada razao, sozinha,
     * basta para soltar; e nenhuma delas presente mantem o agarrao.
     */
    @Test
    void oAgarraoTerminaPorQualquerUmaDasQuatroRazoes() {
        assertTrue(REGRAS.solta(100, 0.0F, false, false),
                "razao 1: o tempo maximo acabou");
        assertTrue(REGRAS.solta(0, 12.0F, false, false),
                "razao 2: a vitima (ou um aliado) bateu o suficiente para escapar");
        assertTrue(REGRAS.solta(0, 0.0F, true, false),
                "razao 3: a vitima morreu -- nao se segura cadaver");
        assertTrue(REGRAS.solta(0, 0.0F, false, true),
                "razao 4: o predador morreu -- sem esta, o jogador fica preso dentro de um sapo morto");
    }

    @Test
    void semNenhumaDasQuatroRazoesOAgarraoContinua() {
        assertFalse(REGRAS.solta(0, 0.0F, false, false),
                "no instante do agarrao nada justifica soltar");
        assertFalse(REGRAS.solta(99, 11.9F, false, false),
                "faltando um tick e um dano de nada, a janela ainda esta aberta");
    }

    @Test
    void regrasImpossiveisSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class,
                () -> new GrabRules(100, 0, 3.0F, 12.0F),
                "intervalo 0 e divisao por zero ou dano todo tick: nunca e o intervalo pretendido");
        assertThrows(IllegalArgumentException.class,
                () -> new GrabRules(100, -20, 3.0F, 12.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new GrabRules(100, 20, 3.0F, 0.0F),
                "danoParaEscapar 0 soltaria a vitima antes de qualquer golpe, e a mordida nao existiria");
        assertThrows(IllegalArgumentException.class,
                () -> new GrabRules(100, 20, 3.0F, -1.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new GrabRules(100, 20, Float.NaN, 12.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new GrabRules(100, 20, 3.0F, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new GrabRules(100, 20, Float.POSITIVE_INFINITY, 12.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new GrabRules(0, 20, 3.0F, 12.0F),
                "agarrao de duracao zero e um estado que nunca chega a existir");
    }
}
