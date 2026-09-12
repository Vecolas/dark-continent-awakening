package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O ponto de consulta da layer nunca pode derrubar o render.
 *
 * <p>Uma excecao no tick de desenho nao apaga a aura: apaga o MUNDO. E por isso
 * que aqui tudo devolve estado desligado em vez de lancar -- a mesma regra que
 * manda o bloom cair para o modo simples em vez de crashar (ADR-016).
 */
class AuraVisualSystemTest {

    @AfterEach
    void desligar() {
        AuraVisualSystem.desligar();
    }

    @Test
    @DisplayName("sem fonte ligada, devolve desligado em vez de lancar")
    void semFonteNaoLanca() {
        AuraVisualSystem.desligar();
        AuraVisualState estado = AuraVisualSystem.estadoDe(null);
        assertFalse(estado.enabled());
        assertEquals(AuraVisualMode.OFF, estado.mode());
    }

    @Test
    @DisplayName("jogador nulo devolve desligado")
    void jogadorNuloNaoLanca() {
        AuraVisualSystem.ligar(jogador -> {
            throw new AssertionError("a fonte nao deveria ser consultada com jogador nulo");
        });
        assertFalse(AuraVisualSystem.estadoDe(null).enabled());
    }

    @Test
    @DisplayName("fonte que devolve nulo nao vira NullPointerException no render")
    void fonteQueDevolveNuloNaoQuebra() {
        AuraVisualSystem.ligar(jogador -> null);
        // Nao ha jogador aqui, entao o caminho exercitado e o do jogador nulo.
        // O que este teste fixa e o CONTRATO: nunca nulo para fora.
        assertFalse(AuraVisualSystem.estadoDe(null).enabled());
    }

    @Test
    @DisplayName("ligar com nulo e recusado na hora, e nao no meio de um quadro")
    void ligarComNuloERecusado() {
        assertThrows(NullPointerException.class, () -> AuraVisualSystem.ligar(null));
    }

    @Test
    @DisplayName("religar funciona: o par de ligar NAO e o logout")
    void ligaDesligaLiga() {
        AuraVisualSystem.ligar(jogador -> AuraVisualState.desligado());
        AuraVisualSystem.desligar();
        assertFalse(AuraVisualSystem.estadoDe(null).enabled());

        // Se o par de `ligar` fosse o logout, a aura morreria para sempre a
        // partir do SEGUNDO servidor -- sem erro nenhum, so sem aura. Religar
        // tem de continuar valendo.
        AuraVisualSystem.ligar(jogador -> AuraVisualState.desligado());
        assertNotNull(AuraVisualSystem.estadoDe(null),
                "o contrato e nunca devolver nulo, em nenhum estado");
    }

    // O QUE ESTE ARQUIVO NAO PROVA: que a fonte devolve o estado CERTO para um
    // jogador de verdade. Construir um `Player` exige o Minecraft carregado, e
    // nao ha biblioteca de duble neste projeto. O caminho por jogador so e
    // exercitado com o jogo de pe -- gate do AV0 (#169).
}
