package com.darkcontinent.nenfoundation.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.darkcontinent.nenfoundation.nen.profile.RuntimeNenState;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NenRuntimeServiceTest {

    @BeforeEach
    @AfterEach
    void limparRegistro() {
        NenRuntimeService.encerrarTodasAsSessoes();
    }

    @Test
    @DisplayName("logout remove a sessao da colecao global sem atingir outro jogador")
    void logoutRemoveSessaoDaColecao() {
        UUID primeiro = UUID.randomUUID();
        UUID segundo = UUID.randomUUID();
        RuntimeNenState estadoDoSegundo = NenRuntimeService.iniciarSessao(segundo);
        NenRuntimeService.iniciarSessao(primeiro);
        assertEquals(2, NenRuntimeService.quantidadeDeSessoes());

        NenRuntimeService.encerrarSessao(primeiro);

        assertEquals(1, NenRuntimeService.quantidadeDeSessoes(),
                "jogador removido nao pode continuar na colecao global");
        assertThrows(IllegalStateException.class,
                () -> NenRuntimeService.estadoDe(primeiro));
        assertSame(estadoDoSegundo, NenRuntimeService.estadoDe(segundo));
    }

    @Test
    @DisplayName("respawn ou dimensao substitui o estado por outro neutro")
    void resetSubstituiEstadoSemDuplicarSessao() {
        UUID jogador = UUID.randomUUID();
        RuntimeNenState anterior = NenRuntimeService.iniciarSessao(jogador);
        // Simula que o pool anterior tinha estado (via pool direto, sem config)
        // O pool novo deve comecar zerado, mesmo que o anterior tivesse aura.
        // Nao ha mais definirAuraAtual(): o acesso direto ao pool basta para o teste.
        anterior.pool().resetarParaMaximo(
                new com.darkcontinent.nenfoundation.nen.profile.PersistentNenData(
                        1, false,
                        com.darkcontinent.nenfoundation.nen.category.NenCategory.UNDETERMINED,
                        false, 0.0D, 0.0D, 0.0D,
                        java.util.Map.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of()),
                com.darkcontinent.nenfoundation.nen.aura.AuraPool.Parametros.de(30.0D, 0.0D, 0.10D));

        RuntimeNenState reiniciado = NenRuntimeService.iniciarSessao(jogador);

        assertNotSame(anterior, reiniciado);
        assertEquals(0.0D, reiniciado.auraAtual());
        assertEquals(1, NenRuntimeService.quantidadeDeSessoes());
    }

    @Test
    @DisplayName("estado ausente nao e recriado silenciosamente pelo tick")
    void estadoAusenteFalhaVisivelmente() {
        UUID ausente = UUID.randomUUID();

        assertThrows(IllegalStateException.class,
                () -> NenRuntimeService.estadoDe(ausente));
        assertEquals(0, NenRuntimeService.quantidadeDeSessoes());
    }
}
