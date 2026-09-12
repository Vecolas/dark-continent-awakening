package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Sustenta que a carga e uma janela, nao um estado: quem esta parado nao atropela ninguem
 * e quem ja acertou nesta carga nao acerta de novo.
 */
class ChargeRulesTest {
    private static final ChargeRules REGRAS = new ChargeRules(4.0D, 16.0D, 60, 2.35D, 40);

    @Test
    void cargaNaoIniciaSemAlvoVisivelForaDeAlcanceOuComEsperaPendente() {
        assertTrue(REGRAS.podeIniciar(true, 8.0D, 0), "alvo visivel, no alcance e sem espera deveria iniciar");
        assertFalse(REGRAS.podeIniciar(false, 8.0D, 0), "sem linha de visao nao ha carga");
        assertFalse(REGRAS.podeIniciar(true, 3.9D, 0), "colado demais nao ha corrida para tomar");
        assertFalse(REGRAS.podeIniciar(true, 16.1D, 0), "longe demais a carga erraria");
        assertFalse(REGRAS.podeIniciar(true, 8.0D, 1), "espera restante ainda correndo bloqueia");
    }

    @Test
    void osLimitesDeDistanciaSaoInclusivos() {
        assertTrue(REGRAS.podeIniciar(true, 4.0D, 0), "exatamente a distancia minima ainda inicia");
        assertTrue(REGRAS.podeIniciar(true, 16.0D, 0), "exatamente a distancia maxima ainda inicia");
    }

    @Test
    void janelaDeDanoAbreUmaUnicaVezPorCarga() {
        assertTrue(REGRAS.janelaDeDano(AttackPhase.ACTIVE, false), "primeiro contato em ACTIVE machuca");
        assertFalse(REGRAS.janelaDeDano(AttackPhase.ACTIVE, true), "o segundo contato da mesma carga nao machuca");
    }

    @Test
    void foraDeActiveNaoExisteDanoDeCarga() {
        assertFalse(REGRAS.janelaDeDano(AttackPhase.IDLE, false));
        assertFalse(REGRAS.janelaDeDano(AttackPhase.WINDUP, false), "parado no windup nao causa dano de carga");
        assertFalse(REGRAS.janelaDeDano(AttackPhase.RECOVERY, false), "parado no recovery nao causa dano de carga");
        assertFalse(REGRAS.janelaDeDano(AttackPhase.COMPLETE, false));
    }

    @Test
    void atordoamentoExigeColisaoDuranteACorrida() {
        assertTrue(REGRAS.atordoa(AttackPhase.ACTIVE, true), "bater na parede correndo atordoa");
        assertFalse(REGRAS.atordoa(AttackPhase.ACTIVE, false), "sem colisao nao ha atordoamento");
        assertFalse(REGRAS.atordoa(AttackPhase.WINDUP, true), "encostar na parede antes de correr nao atordoa");
        assertFalse(REGRAS.atordoa(AttackPhase.RECOVERY, true), "encostar na parede depois de correr nao atordoa");
        assertFalse(REGRAS.atordoa(AttackPhase.IDLE, true));
    }

    @Test
    void regrasImpossiveisSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class,
                () -> new ChargeRules(4.0D, 16.0D, 60, 1.0D, 40),
                "multiplicador 1.0 seria uma carga que nao acelera");
        assertThrows(IllegalArgumentException.class,
                () -> new ChargeRules(4.0D, 16.0D, 60, 0.8D, 40));
        assertThrows(IllegalArgumentException.class,
                () -> new ChargeRules(16.0D, 16.0D, 60, 2.35D, 40),
                "alcance vazio nunca permitiria iniciar");
        assertThrows(IllegalArgumentException.class,
                () -> new ChargeRules(16.0D, 4.0D, 60, 2.35D, 40));
        assertThrows(IllegalArgumentException.class,
                () -> new ChargeRules(Double.NaN, 16.0D, 60, 2.35D, 40));
        assertThrows(IllegalArgumentException.class,
                () -> new ChargeRules(4.0D, Double.NaN, 60, 2.35D, 40));
        assertThrows(IllegalArgumentException.class,
                () -> new ChargeRules(4.0D, 16.0D, 60, Double.NaN, 40));
    }
}
